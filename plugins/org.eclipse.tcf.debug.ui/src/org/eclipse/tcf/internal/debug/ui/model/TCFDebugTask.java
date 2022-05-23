/*******************************************************************************
 * Copyright (c) 2007, 2016 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.model;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.tcf.internal.debug.ui.Activator;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.util.TCFTask;

/**
 * An extension of TCFTask class that adds support for throwing DebugException.
 */
public abstract class TCFDebugTask<V> extends TCFTask<V> {

    public TCFDebugTask() {
    }

    public TCFDebugTask(IChannel channel) {
        super(channel);
    }

    public synchronized V getD() throws DebugException {
        assert !Protocol.isDispatchThread();
        while (!isDone()) {
            try {
                wait();
            }
            catch (InterruptedException x) {
                throw new DebugException(new Status(
                        IStatus.ERROR, Activator.PLUGIN_ID, DebugException.REQUEST_FAILED,
                        "Debugger request interrupted", x));
            }
        }
        assert isDone();
        Throwable x = getError();
        if (x instanceof DebugException) throw (DebugException)x;
        if (x != null) throw new DebugException(new Status(
                IStatus.ERROR, Activator.PLUGIN_ID, DebugException.REQUEST_FAILED,
                "Debugger request failed.\n" + x.getMessage(), x));
        return getResult();
    }

    public void error(String msg) {
        error(new DebugException(new Status(
                IStatus.ERROR, Activator.PLUGIN_ID, DebugException.REQUEST_FAILED,
                msg, null)));
    }
}
