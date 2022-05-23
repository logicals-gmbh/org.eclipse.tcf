/*******************************************************************************
 * Copyright (c) 2007, 2012 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.commands;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.commands.IDebugCommandHandler;
import org.eclipse.debug.core.commands.IDebugCommandRequest;
import org.eclipse.debug.core.commands.IEnabledStateRequest;
import org.eclipse.tcf.internal.debug.model.TCFContextState;
import org.eclipse.tcf.internal.debug.ui.model.TCFModel;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExecContext;
import org.eclipse.tcf.internal.debug.ui.model.TCFRunnable;
import org.eclipse.tcf.services.IRunControl;
import org.eclipse.tcf.util.TCFDataCache;

abstract class StepCommand implements IDebugCommandHandler {

    private static final int MAX_ACTION_CNT = 4;

    protected final TCFModel model;

    public StepCommand(TCFModel model) {
        this.model = model;
    }

    protected abstract boolean canExecute(IRunControl.RunControlContext ctx);

    protected abstract void execute(IDebugCommandRequest monitor,
            IRunControl.RunControlContext ctx, boolean src_step, Runnable done);

    private boolean getContextSet(boolean exec, Object[] elements, Set<IRunControl.RunControlContext> set, Runnable done) {
        for (int i = 0; i < elements.length; i++) {
            TCFNode node = null;
            if (elements[i] instanceof TCFNode) node = (TCFNode)elements[i];
            else node = model.getRootNode();
            while (node != null && !node.isDisposed()) {
                IRunControl.RunControlContext ctx = null;
                if (node instanceof TCFNodeExecContext) {
                    TCFDataCache<IRunControl.RunControlContext> cache = ((TCFNodeExecContext)node).getRunContext();
                    if (!cache.validate(done)) return false;
                    ctx = cache.getData();
                }
                if (ctx == null) {
                    node = node.getParent();
                }
                else {
                    if (!canExecute(ctx)) break;
                    int action_cnt = model.getLaunch().getContextActionsCount(ctx.getID());
                    if (exec && action_cnt >= MAX_ACTION_CNT) break;
                    if (action_cnt > 0) {
                        set.add(ctx);
                    }
                    else {
                        if (ctx.isContainer()) {
                            TCFNodeExecContext.ChildrenStateInfo s = new TCFNodeExecContext.ChildrenStateInfo();
                            if (!((TCFNodeExecContext)node).hasSuspendedChildren(s, done)) return false;
                            if (s.suspended || s.not_active) set.add(ctx);
                        }
                        if (ctx.hasState()) {
                            TCFDataCache<TCFContextState> state_cache = ((TCFNodeExecContext)node).getState();
                            if (!state_cache.validate(done)) return false;
                            TCFContextState state_data = state_cache.getData();
                            if (state_data != null && state_data.is_suspended) set.add(ctx);
                        }
                    }
                    break;
                }
            }
        }
        return true;
    }

    public final void canExecute(final IEnabledStateRequest monitor) {
        new TCFRunnable(model, monitor) {
            public void run() {
                if (done) return;
                if (!monitor.isCanceled()) {
                    Set<IRunControl.RunControlContext> set = new HashSet<IRunControl.RunControlContext>();
                    if (!getContextSet(false, monitor.getElements(), set, this)) return;
                    monitor.setEnabled(set.size() > 0);
                    monitor.setStatus(Status.OK_STATUS);
                }
                done();
            }
        };
    }

    public final boolean execute(final IDebugCommandRequest monitor) {
        new TCFRunnable(model, monitor) {
            public void run() {
                if (done) return;
                Set<IRunControl.RunControlContext> set = new HashSet<IRunControl.RunControlContext>();
                if (!getContextSet(true, monitor.getElements(), set, this)) return;
                if (set.size() == 0) {
                    monitor.setStatus(Status.OK_STATUS);
                    monitor.done();
                }
                else {
                    final Set<Runnable> wait_list = new HashSet<Runnable>();
                    for (IRunControl.RunControlContext ctx : set) {
                        Runnable done = new Runnable() {
                            public void run() {
                                wait_list.remove(this);
                                if (wait_list.isEmpty()) monitor.done();
                            }
                        };
                        wait_list.add(done);
                        execute(monitor, ctx, !model.isInstructionSteppingEnabled(), done);
                    }
                }
            }
        };
        return true;
    }
}
