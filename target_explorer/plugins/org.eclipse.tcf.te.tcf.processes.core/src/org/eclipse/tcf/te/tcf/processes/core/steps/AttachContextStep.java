/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.steps;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IProcesses;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.StepperAttributeUtil;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.core.interfaces.steps.ITcfStepAttributes;
import org.eclipse.tcf.te.tcf.core.model.interfaces.IModel;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.steps.AbstractPeerNodeStep;
import org.eclipse.tcf.te.tcf.processes.core.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.steps.IProcessesStepAttributes;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;

/**
 * Attach context step implementation.
 */
public class AttachContextStep extends AbstractPeerNodeStep {

	/**
	 * Constructor.
	 */
	public AttachContextStep() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IExtendedStep#validateExecute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void validateExecute(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
		IChannel channel = (IChannel)StepperAttributeUtil.getProperty(ITcfStepAttributes.ATTR_CHANNEL, fullQualifiedId, data);
		if (channel == null || channel.getState() != IChannel.STATE_OPEN) {
			throw new CoreException(new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), "missing or closed channel")); //$NON-NLS-1$
		}

		IProcesses.ProcessContext processContext = (IProcesses.ProcessContext)StepperAttributeUtil.getProperty(IProcessesStepAttributes.ATTR_PROCESS_CONTEXT, fullQualifiedId, data);
		if (processContext == null) {
			throw new CoreException(new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), "missing process context")); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#execute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void execute(final IStepContext context, final IPropertiesContainer data, final IFullQualifiedId fullQualifiedId, final IProgressMonitor monitor, final ICallback callback) {
		if (Protocol.isDispatchThread()) {
			internalExecute(context, data, fullQualifiedId, monitor, callback);
		}
		else {
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					internalExecute(context, data, fullQualifiedId, monitor, callback);
				}
			});
		}
	}

	protected void internalExecute(IStepContext context, final IPropertiesContainer data, final IFullQualifiedId fullQualifiedId, final IProgressMonitor monitor, final ICallback callback) {
		final IChannel channel = (IChannel)StepperAttributeUtil.getProperty(ITcfStepAttributes.ATTR_CHANNEL, fullQualifiedId, data);
		final IProcesses.ProcessContext processContext = (IProcesses.ProcessContext)StepperAttributeUtil.getProperty(IProcessesStepAttributes.ATTR_PROCESS_CONTEXT, fullQualifiedId, data);
		final IProcessContextNode processContextNode = (IProcessContextNode)StepperAttributeUtil.getProperty(IProcessesStepAttributes.ATTR_PROCESS_CONTEXT_NODE, fullQualifiedId, data);
		final IProcesses processes = channel.getRemoteService(IProcesses.class);

		if (processes != null) {
			processContext.attach(new IProcesses.DoneCommand() {
				@Override
				public void doneCommand(IToken token, final Exception error) {
					if (processContextNode != null) {
						IModel model = processContextNode.getParent(IModel.class);
						Assert.isNotNull(model);
						model.getService(IModelRefreshService.class).refresh(processContextNode, new Callback() {
							@Override
                            protected void internalDone(Object caller, IStatus status) {
								callback(data, fullQualifiedId, callback, StatusHelper.getStatus(error), null);
							}
						});
					}
					else {
						callback(data, fullQualifiedId, callback, StatusHelper.getStatus(error), null);
					}
				}
			});
		}
		else {
			callback.done(this, new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), "missing processes service")); //$NON-NLS-1$
		}
	}
}
