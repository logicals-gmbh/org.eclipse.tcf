/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.tcf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.tcf.core.TransientPeer;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.runtime.model.factory.Factory;
import org.eclipse.tcf.te.runtime.utils.Host;
import org.eclipse.tcf.te.runtime.utils.net.IPAddressUtil;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelUpdateService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tests.CoreTestCase;

/**
 * TCF test case implementation.
 * <p>
 * Launches a TCF agent at local host and make it available for a test case.
 */
public class TcfTestCase extends CoreTestCase {
	// The agent launcher instance
	private AgentLauncher launcher;
	// The peer instance
	protected IPeer peer;
	// The peer model instance
	protected IPeerNode peerNode;
	// The test agent location
	private IPath agentLocation;

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tests.CoreTestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
	    super.setUp();
	    launchAgent();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tests.CoreTestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		if (peerNode != null && peerNode.isConnectStateChangeActionAllowed(IConnectable.ACTION_DISCONNECT)) {
			Callback cb = new Callback();
			peerNode.changeConnectState(IConnectable.ACTION_DISCONNECT, cb, null);
			ExecutorsUtil.waitAndExecute(60000, cb.getDoneConditionTester(null));
		}

		if (launcher != null) launcher.dispose();
		if (peerNode != null) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					ModelManager.getPeerModel().getService(IPeerModelUpdateService.class).remove(peerNode);
				}
			};
			Protocol.invokeAndWait(runnable);
		}
		peer = null;
		peerNode = null;
	    super.tearDown();
	}

	/**
	 * Launches a TCF agent at local host.
	 */
	protected void launchAgent() {
		// Get the agent location
		IPath path = getAgentFile();

		Throwable error = null;
		String message = null;

		// If the agent is not marked executable on Linux, we have to change that
		if (Host.isLinuxHost() && !path.toFile().canExecute()) {
			try {
				Runtime.getRuntime().exec(new String[] { "chmod", "u+x", path.toString() }); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (IOException e) {
				error = e;
				message = e.getLocalizedMessage();
			}
		}
		assertNull("Failed to make the agent executable for the current user.", message); //$NON-NLS-1$

		error = null;
		message = null;

		assertTrue("Agent should be executable but is not.", path.toFile().canExecute()); //$NON-NLS-1$

		// Create the agent launcher
		launcher = new AgentLauncher(path);
		try {
			launcher.launch();
		} catch (Throwable e) {
			error = e;
			message = e.getLocalizedMessage();
		}
		assertNull("Failed to launch agent: " + message, error); //$NON-NLS-1$

		error = null;
		message = null;

		assertNotNull("Process handle not associated with launcher.", launcher.getProcess()); //$NON-NLS-1$
		assertNotNull("Process output reader not associated with launcher.", launcher.getOutputReader()); //$NON-NLS-1$

		Process process = launcher.getProcess();
		int exitCode = -1;
		try {
			exitCode = process.exitValue();
		} catch (IllegalThreadStateException e) {
			error = e;
			message = e.getLocalizedMessage();
		}
		assertNotNull("Agent process died with exit code " + exitCode, error); //$NON-NLS-1$

		error = null;
		message = null;

		// The agent is started with "-S" to write out the peer attributes in JSON format.
		String output = null;
		int counter = 10;
		while (counter > 0 && output == null) {
			// Try to read in the output
			output = launcher.getOutputReader().getOutput();
			if ("".equals(output)) { //$NON-NLS-1$
				output = null;
				waitAndDispatch(200);
			}
			counter--;
		}
		assertNotNull("Failed to read output from agent.", output); //$NON-NLS-1$

		// Find the "Server-Properties: ..." string within the output
		int start = output.indexOf("Server-Properties:"); //$NON-NLS-1$
		if (start != -1 && start > 0) {
			output = output.substring(start);
		}

		// Strip away "Server-Properties:"
		output = output.replace("Server-Properties:", " "); //$NON-NLS-1$ //$NON-NLS-2$
		output = output.trim();

		// Expectation is that the agent is printing the server properties as single line.
		// If we have still a newline in the string, ignore everything after it
		if (output.indexOf('\n') != -1) {
			output = output.substring(0, output.indexOf('\n'));
			output = output.trim();
		}

		// Read into an object
		Object object = parseOne(output);
		@SuppressWarnings("unchecked")
        final Map<String, String> attrs = new HashMap<String, String>((Map<String, String>)object);

		// Lookup the corresponding peer object
		final IPeerModel model = ModelManager.getPeerModel();
		assertNotNull("Failed to access locator model instance.", model); //$NON-NLS-1$

		// The expected peer id is "<transport>:<canonical IP>:<port>"
		String transport = attrs.get(IPeer.ATTR_TRANSPORT_NAME);
		assertNotNull("Unexpected return value 'null'.", transport); //$NON-NLS-1$
		String port = attrs.get(IPeer.ATTR_IP_PORT);
		assertNotNull("Unexpected return value 'null'.", port); //$NON-NLS-1$
		String ip = IPAddressUtil.getInstance().getIPv4LoopbackAddress();
		assertNotNull("Unexpected return value 'null'.", ip); //$NON-NLS-1$

		final String id = transport + ":" + ip + ":" + port; //$NON-NLS-1$ //$NON-NLS-2$
		final AtomicReference<IPeerNode> node = new AtomicReference<IPeerNode>();

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				node.set(model.getService(IPeerModelLookupService.class).lkupPeerModelById(id));
				// If the peer model is not found by id, try the agent id as fallback.
				if (node.get() == null) {
					String agentID = attrs.get(IPeer.ATTR_AGENT_ID);
					assertNotNull("Unexpected return value 'null'.", agentID); //$NON-NLS-1$
					IPeerNode[] candidates = model.getService(IPeerModelLookupService.class).lkupPeerModelByAgentId(agentID);
					if (candidates != null && candidates.length > 0) node.set(candidates[0]);
				}
			}
		};
		assertFalse("Test is running in TCF dispatch thread.", Protocol.isDispatchThread()); //$NON-NLS-1$
		Protocol.invokeAndWait(runnable);

		// If the peer model is still not found, we create a transient peer
		if (node.get() == null) {
			attrs.put(IPeer.ATTR_ID, id);
			attrs.put(IPeer.ATTR_IP_HOST, ip);
			attrs.put("SkipValueAdds", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			peer = new TransientPeer(attrs);
			peerNode = Factory.getInstance().newInstance(IPeerNode.class, new Object[] { model, peer });

			runnable = new Runnable() {
				@Override
				public void run() {
					model.getService(IPeerModelUpdateService.class).add(peerNode);
				}
			};
			Protocol.invokeAndWait(runnable);
		} else {
			peerNode = node.get();
			peer = peerNode.getPeer();
		}
		assertNotNull("Failed to determine the peer to use for the tests.", peer); //$NON-NLS-1$
	}

	protected IPath getAgentFile() {
	    IPath path = getAgentLocation();
		assertNotNull("Cannot determine TCF agent location.", path); //$NON-NLS-1$
		// Add the agent executable name
		path = path.append("agent"); //$NON-NLS-1$
		if (Host.isWindowsHost()) path = path.addFileExtension("exe"); //$NON-NLS-1$
		assertTrue("Invalid agent location: " + path.toString(), path.toFile().isFile()); //$NON-NLS-1$
	    return path;
    }

	/**
	 * Returns the agent location.
	 *
	 * @return The agent location or <code>null</code> if not found.
	 */
	protected IPath getAgentLocation() {
		if (agentLocation == null) {
			String agentPath = System.getProperty("tcf.agent.path"); //$NON-NLS-1$
			if (agentPath != null && !"".equals(agentPath.trim())) { //$NON-NLS-1$
				agentLocation = new Path(agentPath);
			} else {
				agentLocation = getDataLocation("agent", true, true); //$NON-NLS-1$
			}
		}
		return agentLocation;
	}

	/**
	 * Parses a object from the given encoded string.
	 *
	 * @param encoded The encoded string. Must not be <code>null</code>.
	 * @return The object
	 */
	private static Object parseOne(final String encoded) {
		assertNotNull(encoded);

		final AtomicReference<Object> object = new AtomicReference<Object>();
		final AtomicReference<String> message = new AtomicReference<String>();
		final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					object.set(JSON.parseOne(encoded.getBytes("UTF-8"))); //$NON-NLS-1$
				} catch (IOException e) {
					error.set(e);
					message.set(e.getLocalizedMessage());
				}
			}
		};
		assertFalse("Test is running in TCF dispatch thread.", Protocol.isDispatchThread()); //$NON-NLS-1$
		Protocol.invokeAndWait(runnable);

		assertNull("Failed to parse server properties: " + message.get(), error.get()); //$NON-NLS-1$
		assertTrue("Server properties object is not of expected type Map.", object.get() instanceof Map); //$NON-NLS-1$

		return object.get();
	}

	/**
	 * Returns the location of the helloWorld binary.
	 *
	 * @return The location of the helloWorld binary or <code>null/code>.
	 */
	protected IPath getHelloWorldLocation() {
		IPath path = getDataLocation("helloWorld", true, true); //$NON-NLS-1$
		if (path != null) {
			path = path.append("helloWorld"); //$NON-NLS-1$
			if (Host.isWindowsHost()) {
				path = path.addFileExtension("exe"); //$NON-NLS-1$
			}
		}

		return path;
	}

	/**
	 * Copy a file from source to destination.
	 *
	 * @param source The source file. Must not be <code>null</code>.
	 * @param dest The destination file. Must not be <code>null</code>.
	 *
	 * @throws IOException In case the copy fails.
	 */
	protected void copyFile(File source, File dest) throws IOException {
		assertNotNull(source);
		assertNotNull(dest);

		FileChannel inputChannel = null;
		FileChannel outputChannel = null;

		try {
			inputChannel = new FileInputStream(source).getChannel();
			outputChannel = new FileOutputStream(dest).getChannel();
			outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
		} finally {
			if (inputChannel != null) inputChannel.close();
			if (outputChannel != null) outputChannel.close();
		}
	}

}
