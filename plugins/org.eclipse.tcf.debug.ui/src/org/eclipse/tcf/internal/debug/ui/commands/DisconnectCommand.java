/*******************************************************************************
 * Copyright (c) 2007, 2013 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.commands;

import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.commands.IDebugCommandRequest;
import org.eclipse.debug.core.commands.IDisconnectHandler;
import org.eclipse.debug.core.commands.IEnabledStateRequest;
import org.eclipse.tcf.internal.debug.model.TCFError;
import org.eclipse.tcf.internal.debug.ui.model.TCFModel;
import org.eclipse.tcf.internal.debug.ui.model.TCFRunnable;


public class DisconnectCommand implements IDisconnectHandler {

    private final TCFModel model;

    public DisconnectCommand(TCFModel model) {
        this.model = model;
    }

    public void canExecute(final IEnabledStateRequest monitor) {
        new TCFRunnable(model, monitor) {
            public void run() {
                monitor.setEnabled(model.getLaunch().canDisconnect());
                monitor.setStatus(Status.OK_STATUS);
                done();
            }
        };
    }

    public boolean execute(final IDebugCommandRequest monitor) {
        new TCFRunnable(model, monitor) {
            public void run() {
                try {
                    model.getLaunch().closeChannel();
                    monitor.setStatus(Status.OK_STATUS);
                }
                catch (Throwable x) {
                    monitor.setStatus(new TCFError(x));
                }
                done();
            }
        };
        return false;
    }
}
