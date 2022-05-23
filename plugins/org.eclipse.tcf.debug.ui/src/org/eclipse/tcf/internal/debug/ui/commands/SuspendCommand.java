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
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.commands.IDebugCommandRequest;
import org.eclipse.debug.core.commands.IEnabledStateRequest;
import org.eclipse.debug.core.commands.ISuspendHandler;
import org.eclipse.tcf.internal.debug.actions.TCFAction;
import org.eclipse.tcf.internal.debug.model.TCFContextState;
import org.eclipse.tcf.internal.debug.ui.Activator;
import org.eclipse.tcf.internal.debug.ui.model.TCFModel;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExecContext;
import org.eclipse.tcf.internal.debug.ui.model.TCFRunnable;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IRunControl;
import org.eclipse.tcf.util.TCFDataCache;


public class SuspendCommand implements ISuspendHandler {

    private final TCFModel model;

    public SuspendCommand(TCFModel model) {
        this.model = model;
    }

    public void canExecute(final IEnabledStateRequest monitor) {
        new TCFRunnable(model, monitor) {
            public void run() {
                if (done) return;
                Object[] elements = monitor.getElements();
                boolean res = false;
                for (int i = 0; i < elements.length; i++) {
                    TCFNode node = null;
                    if (elements[i] instanceof TCFNode) node = (TCFNode)elements[i];
                    else node = model.getRootNode();
                    while (node != null && !node.isDisposed()) {
                        IRunControl.RunControlContext ctx = null;
                        if (node instanceof TCFNodeExecContext) {
                            TCFDataCache<IRunControl.RunControlContext> cache = ((TCFNodeExecContext)node).getRunContext();
                            if (!cache.validate(this)) return;
                            ctx = cache.getData();
                        }
                        if (ctx == null) {
                            node = node.getParent();
                        }
                        else if (model.getActiveAction(ctx.getID()) != null) {
                            res = true;
                            break;
                        }
                        else {
                            if (!ctx.canSuspend()) break;
                            if (ctx.isContainer()) {
                                TCFNodeExecContext.ChildrenStateInfo s = new TCFNodeExecContext.ChildrenStateInfo();
                                if (!((TCFNodeExecContext)node).hasSuspendedChildren(s, this)) return;
                                if (s.running) res = true;
                            }
                            if (ctx.hasState()) {
                                TCFDataCache<TCFContextState> state_cache = ((TCFNodeExecContext)node).getState();
                                if (!state_cache.validate(this)) return;
                                TCFContextState state_data = state_cache.getData();
                                if (state_data != null && !state_data.is_suspended && ctx.canSuspend()) res = true;
                            }
                            break;
                        }
                    }
                }
                monitor.setEnabled(res);
                monitor.setStatus(Status.OK_STATUS);
                done();
            }
        };
    }

    public boolean execute(final IDebugCommandRequest monitor) {
        new TCFRunnable(model, monitor) {
            public void run() {
                if (done) return;
                Object[] elements = monitor.getElements();
                Set<IRunControl.RunControlContext> set = new HashSet<IRunControl.RunControlContext>();
                for (int i = 0; i < elements.length; i++) {
                    TCFNode node = null;
                    if (elements[i] instanceof TCFNode) node = (TCFNode)elements[i];
                    else node = model.getRootNode();
                    while (node != null && !node.isDisposed()) {
                        IRunControl.RunControlContext ctx = null;
                        if (node instanceof TCFNodeExecContext) {
                            TCFDataCache<IRunControl.RunControlContext> cache = ((TCFNodeExecContext)node).getRunContext();
                            if (!cache.validate(this)) return;
                            ctx = cache.getData();
                        }
                        if (ctx == null) {
                            node = node.getParent();
                        }
                        else {
                            set.add(ctx);
                            break;
                        }
                    }
                }
                final Set<IToken> cmds = new HashSet<IToken>();
                for (Iterator<IRunControl.RunControlContext> i = set.iterator(); i.hasNext();) {
                    IRunControl.RunControlContext ctx = i.next();
                    model.getLaunch().removeContextActions(ctx.getID());
                    final TCFAction action = model.getActiveAction(ctx.getID());
                    if (action != null) action.abort();
                    cmds.add(ctx.suspend(new IRunControl.DoneCommand() {
                        public void doneCommand(IToken token, Exception error) {
                            assert cmds.contains(token);
                            cmds.remove(token);
                            if (error != null && action == null) {
                                monitor.setStatus(new Status(IStatus.ERROR,
                                        Activator.PLUGIN_ID, IStatus.OK, "Cannot suspend: " + error.getLocalizedMessage(), error));
                            }
                            if (cmds.isEmpty()) done();
                        }
                    }));
                }
            }
        };
        return true;
    }
}
