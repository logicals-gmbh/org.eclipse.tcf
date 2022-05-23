/*******************************************************************************
 * Copyright (c) 2008, 2017 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.actions;

import org.eclipse.tcf.internal.debug.model.TCFLaunch;
import org.eclipse.tcf.protocol.Protocol;

/**
 * TCFAction class represents user request to perform some action(s) on
 * a remote context, for example, step over line command.
 * Such action might require multiple data exchanges with remote target.
 * Actions for a particular context should be executed sequentially -
 * it does not make sense to execute two step commands concurrently.
 * If user requests actions faster then they are executed,
 * actions are placed into a FIFO queue.
 *
 * Clients are expected to implement run() method to perform the action job.
 * When the job is done, client code should call done() method.
 */
public abstract class TCFAction implements Runnable {

    public static final String STEP_BREAKPOINT_PREFIX = "Step.";

    protected final TCFLaunch launch;
    protected final String ctx_id;

    protected boolean aborted;

    public TCFAction(TCFLaunch launch, String ctx_id) {
        assert Protocol.isDispatchThread();
        this.launch = launch;
        this.ctx_id = ctx_id;
        launch.addContextAction(this);
    }

    public void abort() {
        aborted = true;
    }

    public String getContextID() {
        return ctx_id;
    }

    public int getPriority() {
        return 0;
    }

    public boolean showRunning() {
        return false;
    }

    public void setActionResult(String id, String result) {
        launch.setContextActionResult(id, result);
    }

    public void done() {
        assert Protocol.isDispatchThread();
        launch.removeContextAction(this);
    }
}
