/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.terminals.ui.connector;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.ITerminals;
import org.eclipse.tcf.te.runtime.interfaces.IDisposable;
import org.eclipse.tcf.te.tcf.terminals.core.launcher.TerminalsLauncher;
import org.eclipse.tm.internal.terminal.provisional.api.ISettingsStore;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalControl;
import org.eclipse.tm.internal.terminal.provisional.api.TerminalState;
import org.eclipse.tm.terminal.view.ui.streams.AbstractStreamsConnector;

/**
 * Terminals connector implementation.
 */
@SuppressWarnings("restriction")
public class TerminalsConnector extends AbstractStreamsConnector implements IDisposable {
	// Reference to the terminals settings
	private final TerminalsSettings settings;

	// Remember the last set window size
	private int width = -1;
	private int height = -1;

	/**
	 * Constructor.
	 */
	public TerminalsConnector() {
		this(new TerminalsSettings());
	}

	/**
	 * Constructor.
	 *
	 * @param settings The streams settings. Must not be <code>null</code>
	 */
	public TerminalsConnector(TerminalsSettings settings) {
		super();

		Assert.isNotNull(settings);
		this.settings = settings;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.terminal.provisional.api.provider.TerminalConnectorImpl#connect(org.eclipse.tcf.internal.terminal.provisional.api.ITerminalControl)
	 */
	@Override
	public void connect(ITerminalControl control) {
		Assert.isNotNull(control);
		super.connect(control);

		// connect the streams
		connectStreams(control, settings.getStdinStream(), settings.getStdoutStream(), settings.getStderrStream(), settings.isLocalEcho(), settings.getLineSeparator());

		// Set the terminal control state to CONNECTED
		control.setState(TerminalState.CONNECTED);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.terminal.provisional.api.provider.TerminalConnectorImpl#isLocalEcho()
	 */
	@Override
	public boolean isLocalEcho() {
		return settings.isLocalEcho();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IDisposable#dispose()
	 */
	@Override
	public void dispose() {
		disconnect();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.terminal.provisional.api.provider.TerminalConnectorImpl#doDisconnect()
	 */
	@Override
	public void doDisconnect() {
		// Dispose the streams
		super.doDisconnect();

		// Set the terminal control state to CLOSED.
		fControl.setState(TerminalState.CLOSED);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.terminal.provisional.api.provider.TerminalConnectorImpl#setTerminalSize(int, int)
	 */
	@Override
	public void setTerminalSize(final int newWidth, final int newHeight) {
		if (width == -1 || height == -1 || newWidth != width || newHeight != height) {
			if (fControl.getState() == TerminalState.CONNECTED && settings.getTerminalsLauncher() instanceof TerminalsLauncher) {
				final ITerminals service = ((TerminalsLauncher)settings.getTerminalsLauncher()).getSvcTerminals();
				final ITerminals.TerminalContext context = (ITerminals.TerminalContext)settings.getTerminalsLauncher().getAdapter(ITerminals.TerminalContext.class);
				if (service != null && context != null) {
					width = newWidth;
					height = newHeight;

					Protocol.invokeLater(new Runnable() {
						@Override
						public void run() {
							service.setWinSize(context.getID(), newWidth, newHeight, new ITerminals.DoneCommand() {
								@Override
								public void doneCommand(IToken token, Exception error) {
								}
							});
						}
					});
				}
			}
		}
	}

	// ***** Connector settings handling *****

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.terminal.provisional.api.provider.TerminalConnectorImpl#getSettingsSummary()
	 */
	@Override
	public String getSettingsSummary() {
		return ""; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.terminal.provisional.api.provider.TerminalConnectorImpl#load(org.eclipse.tcf.internal.terminal.provisional.api.ISettingsStore)
	 */
	@Override
	public void load(ISettingsStore store) {
		settings.load(store);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.terminal.provisional.api.provider.TerminalConnectorImpl#save(org.eclipse.tcf.internal.terminal.provisional.api.ISettingsStore)
	 */
	@Override
	public void save(ISettingsStore store) {
		settings.save(store);
	}
}
