/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.interfaces.launcher;

import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.tcf.core.streams.StreamsDataReceiver;

/**
 * Interface to be implemented by classes providing a remote process launcher.
 */
public interface IProcessLauncher extends IAdaptable {
	/**
	 * Property denoting the process image path. The process image path is the absolute remote path.
	 * <p>
	 * The property type is {@link String}.
	 */
	public static String PROP_PROCESS_PATH = "process.path"; //$NON-NLS-1$

	/**
	 * Property denoting the process image path of a monitored application. The process image path
	 * is the absolute remote path.
	 * <p>
	 * <b>Note:</b> Optional: The monitored process path property might be set if the process path
	 * contains the image path of a monitoring application, like mpatrol. This property influence
	 * the terminal title construction only.
	 * <p>
	 * The property type is {@link String}.
	 */
	public static String PROP_PROCESS_MONITORED_PATH = "process.monitored.path"; //$NON-NLS-1$

	/**
	 * Property denoting the process arguments.
	 * <p>
	 * <b>Note:</b> The arguments are passed as is to the launched remote process. In case of shell
	 * scripts, the client must assure that the first argument ($0) is the absolute process image
	 * path.
	 * <p>
	 * The property type is {@link String}[].
	 */
	public static String PROP_PROCESS_ARGS = "process.args"; //$NON-NLS-1$

	/**
	 * Property specify the process arguments should be used as is.
	 * if <code>False</code>, the process image name would be added as the first argument if not already set.
	 * The property type is {@link Boolean}.
	 */
	public static String PROP_USE_PROCESS_ARGS_AS_IS = "use.process.args.as.is"; //$NON-NLS-1$

	/**
	 * Property denoting the process working directory.
	 * <p>
	 * The property type is {@link String}.
	 */
	public static String PROP_PROCESS_CWD = "process.cwd"; //$NON-NLS-1$

	/**
	 * Property specify the process working directory should be used as is.
	 * if <code>False</code>, an empty cwd would be replaced by the path of the process image.
	 * The property type is {@link Boolean}.
	 */
	public static String PROP_USE_PROCESS_CWD_AS_IS = "use.process.cwd.as.is"; //$NON-NLS-1$

	/**
	 * Property denoting the process environment.
	 * <p>
	 * The property type is {@link Map}&lt; {@link String}, {@link String} &gt;.
	 */
	public static String PROP_PROCESS_ENV = "process.env"; //$NON-NLS-1$

	/**
	 * Property denoting if the process is launched attached or not.
	 * <p>
	 * The property type is {@link Boolean}.
	 */
	public static String PROP_PROCESS_ATTACH = "process.attach"; //$NON-NLS-1$

	/**
	 * Property denoting if the process is associated with an input/output console.
	 * <p>
	 * The property type is {@link Boolean}.
	 */
	public static String PROP_PROCESS_ASSOCIATE_CONSOLE = "process.associateConsole"; //$NON-NLS-1$

	/**
	 * Property denoting if the process is redirecting it's output to an file.
	 * <p>
	 * The property type is {@link String}.
	 */
	public static String PROP_PROCESS_OUTPUT_REDIRECT_TO_FILE = "process.redirectToFile"; //$NON-NLS-1$

	/**
	 * Property denoting a set of listeners listening to the process output.
	 * <p>
	 * The property type is an array of {@link StreamsDataReceiver#Listener}.
	 */
	public static String PROP_PROCESS_OUTPUT_LISTENER = "process.listener.output"; //$NON-NLS-1$

	/**
	 * Property denoting the full name of the target connection the launcher got invoked for.
	 * <p>
	 * The property type is {@link String}.
	 */
	public static String PROP_CONNECTION_NAME = "connection.name"; //$NON-NLS-1$

	/**
	 * Property denoting additional parameters for ProcessesV1.
	 * When set, parameters are only used from this map.
	 * <p>
	 * The property type is {@link Map<String,Object>}.
	 */
	public static String PROP_PROCESSESV1_PARAMS = "processesV1.params"; //$NON-NLS-1$

	/**
	 * Property denoting the communication channel to use.
	 * <p>
	 * The property type is {@link IChannel}.
	 */
	public static String PROP_CHANNEL = "process.channel"; //$NON-NLS-1$

	/**
	 * Launch a remote process defined by the given launch properties at the target specified by the
	 * given peer.
	 * <p>
	 * <b>Note:</b> If the target is supporting the extended process service (IProcessesV1), than the
	 * <code>IProcessesV1.START_*</code> launch parameter will passed on to the extended process service
	 * launch command if and as set in <code>params</code>.
	 *
	 * @param peer The peer. Must not be <code>null</code>.
	 * @param params The remote process properties. Must not be <code>null</code>.
	 * @param callback The callback or <code>null</code>.
	 */
	public void launch(IPeer peer, IPropertiesContainer properties, ICallback callback);

	/**
	 * Disposes the remote process launcher instance.
	 */
	public void dispose();

	/**
	 * Terminates the launched remote process (if still running).
	 */
	public void terminate();

	/**
	 * Cancels a remote process launch (if callback was not already sent)
	 */
	public void cancel();
}
