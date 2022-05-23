/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.ui.internal.services;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener;
import org.eclipse.debug.core.model.IDisconnect;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.launch.core.lm.LaunchManager;
import org.eclipse.tcf.te.launch.core.lm.interfaces.ILaunchManagerDelegate;
import org.eclipse.tcf.te.launch.core.lm.interfaces.ILaunchSpecification;
import org.eclipse.tcf.te.launch.core.persistence.launchcontext.LaunchContextsPersistenceDelegate;
import org.eclipse.tcf.te.launch.core.selection.LaunchSelection;
import org.eclipse.tcf.te.launch.core.selection.RemoteSelectionContext;
import org.eclipse.tcf.te.launch.core.selection.interfaces.ILaunchSelection;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.runtime.services.AbstractService;
import org.eclipse.tcf.te.runtime.services.interfaces.IDebugService;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.launch.core.delegates.Launch;
import org.eclipse.tcf.te.tcf.launch.core.interfaces.ILaunchTypes;
import org.eclipse.tcf.te.ui.swt.DisplayUtil;

/**
 * Debug service implementations for TCF contexts.
 */
public class DebugService extends AbstractService implements IDebugService {
	// Reference to the launches listener
	private final ILaunchesListener listener;

	/**
     * Constructor
     */
    public DebugService() {
    	super();

    	// Create and register the launches listener instance
    	listener = new DebugServicesLaunchesListener();
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(listener);
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.runtime.services.interfaces.IDebugService#attach(java.lang.Object, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
     */
    @Override
    public void attach(final Object context, final IPropertiesContainer data, final IProgressMonitor monitor, final ICallback callback) {
		if (!Protocol.isDispatchThread()) {
			internalAttach(context, data, monitor, callback);
		}
		else {
			ExecutorsUtil.execute(new Runnable() {
				@Override
				public void run() {
					internalAttach(context, data, monitor, callback);
				}
			});
		}
	}

	@SuppressWarnings("restriction")
    protected void internalAttach(final Object context, final IPropertiesContainer data, final IProgressMonitor monitor, final ICallback callback) {
		Assert.isTrue(!Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		Assert.isNotNull(context);
		Assert.isNotNull(data);
		Assert.isNotNull(callback);

		if (context instanceof IModelNode) {
			ILaunchConfigurationType launchConfigType =	DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(ILaunchTypes.ATTACH);
			try {
				ILaunchSelection launchSelection = new LaunchSelection(ILaunchManager.DEBUG_MODE, new RemoteSelectionContext((IModelNode)context, true));
				ILaunchManagerDelegate delegate = LaunchManager.getInstance().getLaunchManagerDelegate(launchConfigType, ILaunchManager.DEBUG_MODE);
				if (delegate != null) {
					// create an empty launch configuration specification to initialize all attributes with their default defaults.
					ILaunchSpecification launchSpec = delegate.getLaunchSpecification(launchConfigType.getIdentifier(), launchSelection);
					for (String key : data.getProperties().keySet()) {
						launchSpec.addAttribute(key, data.getProperty(key));
					}
					delegate.validate(launchSpec);
					if (launchSpec != null && launchSpec.isValid()) {
						ILaunchConfiguration[] launchConfigs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(launchConfigType);
						launchConfigs = delegate.getMatchingLaunchConfigurations(launchSpec, launchConfigs);

						ILaunchConfiguration config = launchConfigs != null && launchConfigs.length > 0 ? launchConfigs[0] : null;

						boolean skip = false;
						ILaunch activeLaunch = null;
						if (config != null) {
							ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
							for (ILaunch launch : launches) {
								if (launch.getLaunchConfiguration() != null && launch.getLaunchConfiguration().getType().getIdentifier().equals(ILaunchTypes.ATTACH) && !launch.isTerminated()) {
									IModelNode[] contexts = LaunchContextsPersistenceDelegate.getLaunchContexts(launch.getLaunchConfiguration());
									if (contexts != null && contexts.length == 1 && contexts[0].equals(context)) {
										callback.setProperty("launch", launch); //$NON-NLS-1$
										activeLaunch = launch;
										skip = true;
										break;
									}
								}
							}
						}

						if (!skip) {
							final ILaunchConfiguration finConfig = LaunchManager.getInstance().createOrUpdateLaunchConfiguration(config, launchSpec);

							delegate.validate(ILaunchManager.DEBUG_MODE, finConfig);

							final ILaunchListener listener = new ILaunchListener() {
								@Override
								public void launchAdded(ILaunch launch) {
									if (launch != null && finConfig.equals(launch.getLaunchConfiguration())) {
										DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
										callback.setProperty("launch", launch); //$NON-NLS-1$
										if (launch instanceof Launch) {
											((Launch)launch).setCallback(callback);
										}
										else {
											callback.done(this, Status.OK_STATUS);
										}
									}
								}

								@Override
								public void launchChanged(ILaunch launch) {
								}

								@Override
								public void launchRemoved(ILaunch launch) {
								}
							};

							DebugPlugin.getDefault().getLaunchManager().addLaunchListener(listener);

							// DebugUITools.launch(...) must be called from within the UI thread.
							DisplayUtil.safeAsyncExec(new Runnable() {
								@Override
								public void run() {
									try {
										DebugUITools.buildAndLaunch(finConfig, ILaunchManager.DEBUG_MODE, monitor != null ? monitor : new NullProgressMonitor());
									}
									catch (Exception e) {
										callback.done(DebugService.this, StatusHelper.getStatus(e));
									}
								}
							});
						} else {
							org.eclipse.debug.internal.ui.DebugUIPlugin.getDefault().getPerspectiveManager().launchAdded(activeLaunch);
							callback.done(this, Status.OK_STATUS);
						}
					}
				}
			}
			catch (Exception e) {
				callback.done(this, StatusHelper.getStatus(e));
			}
		}
		else {
			callback.done(this, Status.OK_STATUS);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IDebugService#detach(java.lang.Object, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void detach(Object context, IPropertiesContainer data, final IProgressMonitor monitor, ICallback callback) {
		Assert.isNotNull(context);
		Assert.isNotNull(data);
		Assert.isNotNull(callback);

		if (context instanceof IModelNode) {
			ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
			for (ILaunch launch : launches) {
				try {
					if (launch.getLaunchConfiguration().getType().getIdentifier().equals(ILaunchTypes.ATTACH)) {
						IModelNode[] contexts = LaunchContextsPersistenceDelegate.getLaunchContexts(launch.getLaunchConfiguration());
						if (contexts != null && contexts.length == 1 && contexts[0].equals(context)) {
							if (launch instanceof IDisconnect && !((IDisconnect)launch).isDisconnected()) {
								((IDisconnect)launch).disconnect();
								data.setProperty(PROPERTY_DEBUGGER_DETACHED, true);
							}
							else if (launch instanceof Launch) {
								data.setProperty(PROPERTY_DEBUGGER_DETACHED, !((Launch)launch).isManualDisconnected());
							}
						}
					}
				} catch (Exception e) {
					if (e instanceof ExecutionException && "TCF task aborted".equals(e.getMessage()) //$NON-NLS-1$
							|| e.getCause() instanceof CancellationException) {
						// This disconnect of the debug launch timed out. We are going
						// to ignore this as we are detaching from the debugger anyway.
						callback.done(this, Status.OK_STATUS);
					} else {
						callback.done(this, StatusHelper.getStatus(e));
					}
					return;
				}
			}
		}
		callback.done(this, Status.OK_STATUS);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IDebugService#isLaunched(java.lang.Object)
	 */
	@Override
	public boolean isLaunched(Object context) {
		Assert.isNotNull(context);

		boolean isLaunched = false;

		if (context instanceof IModelNode) {
			ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
			for (ILaunch launch : launches) {
				if (launch == null) continue;
				try {
					if (launch.getLaunchConfiguration() != null && launch.getLaunchConfiguration().getType() != null && launch.getLaunchConfiguration().getType().getIdentifier() != null) {
						if (launch.getLaunchConfiguration().getType().getIdentifier().equals(ILaunchTypes.ATTACH) && !launch.isTerminated()) {
							IModelNode[] contexts = LaunchContextsPersistenceDelegate.getLaunchContexts(launch.getLaunchConfiguration());
							if (contexts != null && contexts.length == 1 && contexts[0].equals(context)) {
								isLaunched = true;
								break;
							}
						}
					}
				} catch (CoreException e) {
					if (Platform.inDebugMode()) e.printStackTrace();
				}
			}
		}

	    return isLaunched;
	}
}
