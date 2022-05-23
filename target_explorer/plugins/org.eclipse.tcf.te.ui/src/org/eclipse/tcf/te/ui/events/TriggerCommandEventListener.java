/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.events;

import java.util.EventObject;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.tcf.te.runtime.events.TriggerCommandEvent;
import org.eclipse.tcf.te.ui.activator.UIPlugin;
import org.eclipse.tcf.te.ui.nls.Messages;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * Event listener implementation to handle trigger command events.
 */
public class TriggerCommandEventListener extends AbstractEventListener {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.events.IEventListener#eventFired(java.util.EventObject)
	 */
	@Override
	public void eventFired(EventObject event) {
		// Handling only TriggerCommandEvent's
		if (!(event instanceof TriggerCommandEvent)) return;

		TriggerCommandEvent commandEvent = (TriggerCommandEvent)event;

		ICommandService service = (ICommandService)PlatformUI.getWorkbench().getService(ICommandService.class);
		Command command = service != null ? service.getCommand(commandEvent.getCommandId()) : null;
		if (command != null && command.isDefined() && command.isEnabled()) {
			try {
				ParameterizedCommand pCmd = ParameterizedCommand.generateCommand(command, null);
				Assert.isNotNull(pCmd);
				IHandlerService handlerSvc = (IHandlerService)PlatformUI.getWorkbench().getService(IHandlerService.class);
				Assert.isNotNull(handlerSvc);
				IEvaluationContext ctx = handlerSvc.getCurrentState();
				if (commandEvent.getSource() instanceof ISelection) {
					ctx = new EvaluationContext(ctx, commandEvent.getSource());
					ctx.addVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME, commandEvent.getSource());
				}
				handlerSvc.executeCommandInContext(pCmd, null, ctx);
			} catch (Exception e) {
				// If the platform is in debug mode, we print the exception to the log view
				if (Platform.inDebugMode()) {
					IStatus status = new Status(IStatus.ERROR, UIPlugin.getUniqueIdentifier(),
												Messages.TriggerCommandEventListener_error_executionFailed, e);
					UIPlugin.getDefault().getLog().log(status);
				}
			}
		}
	}

}
