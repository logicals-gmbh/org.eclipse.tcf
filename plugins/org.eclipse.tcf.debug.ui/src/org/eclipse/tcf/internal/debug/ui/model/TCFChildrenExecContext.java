/*******************************************************************************
 * Copyright (c) 2007, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.model;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IMemory;
import org.eclipse.tcf.services.IRunControl;
import org.eclipse.tcf.util.TCFDataCache;


/**
 * This class is used to maintain a dynamic list of both executable contexts and memory spaces
 * that are children of a given parent context. The job is slightly complicated by necessity
 * to merge results from two independent services.
 */
public class TCFChildrenExecContext extends TCFChildren {

    private final TCFChildren mem_children;
    private final TCFChildren run_children;

    TCFChildrenExecContext(final TCFNode node) {
        super(node);
        mem_children = new TCFChildren(node) {
            @Override
            protected boolean startDataRetrieval() {
                IMemory mem = node.model.getLaunch().getService(IMemory.class);
                if (mem == null) {
                    set(null, null, new HashMap<String,TCFNode>());
                    return true;
                }
                assert command == null;
                command = mem.getChildren(node.id, new IMemory.DoneGetChildren() {
                    public void doneGetChildren(IToken token, Exception error, String[] contexts) {
                        Map<String,TCFNode> data = null;
                        if (command == token && error == null) {
                            int cnt = 0;
                            data = new HashMap<String,TCFNode>();
                            for (String id : contexts) {
                                TCFNode n = node.model.getNode(id);
                                if (n == null) n = new TCFNodeExecContext(node, id);
                                ((TCFNodeExecContext)n).setMemSeqNo(cnt++);
                                assert n.parent == node;
                                data.put(id, n);
                            }
                        }
                        set(token, error, data);
                    }
                });
                return false;
            }
        };
        run_children = new TCFChildren(node) {
            @Override
            protected boolean startDataRetrieval() {
                IRunControl run = node.model.getLaunch().getService(IRunControl.class);
                if (run == null) {
                    set(null, null, new HashMap<String,TCFNode>());
                    return true;
                }
                assert command == null;
                command = run.getChildren(node.id, new IRunControl.DoneGetChildren() {
                    public void doneGetChildren(IToken token, Exception error, String[] contexts) {
                        Map<String,TCFNode> data = null;
                        if (command == token && error == null) {
                            int cnt = 0;
                            data = new HashMap<String,TCFNode>();
                            for (String id : contexts) {
                                TCFNode n = node.model.getNode(id);
                                if (n == null) n = new TCFNodeExecContext(node, id);
                                ((TCFNodeExecContext)n).setExeSeqNo(cnt++);
                                assert n.parent == node;
                                data.put(id, n);
                            }
                        }
                        set(token, error, data);
                    }
                });
                return false;
            }
        };
    }

    @Override
    protected boolean startDataRetrieval() {
        TCFDataCache<?> pending = null;
        if (!mem_children.validate()) pending = mem_children;
        if (!run_children.validate()) pending = run_children;
        if (pending != null) {
            pending.wait(this);
            return false;
        }
        Throwable error = mem_children.getError();
        if (error == null) error = run_children.getError();
        Map<String,TCFNode> data = new HashMap<String,TCFNode>();
        Map<String,TCFNode> m1 = mem_children.getData();
        Map<String,TCFNode> m2 = run_children.getData();
        if (m1 != null) data.putAll(m1);
        if (m2 != null) data.putAll(m2);
        set(null, error, data);
        return true;
    }

    void onContextAdded(IRunControl.RunControlContext context) {
        // To preserve children order need to reset children list.
        reset();
        run_children.reset();
        mem_children.reset();
        assert !node.isDisposed();
        String id = context.getID();
        TCFNodeExecContext n = (TCFNodeExecContext)node.model.getNode(id);
        if (n == null) {
            n = new TCFNodeExecContext(node, id);
            n.postContextAddedDelta();
            add(n);
        }
        else {
            n.postAllChangedDelta();
        }
        run_children.add(n);
        n.setRunContext(context);
    }

    void onContextAdded(IMemory.MemoryContext context) {
        // To preserve children order need to reset children list.
        reset();
        run_children.reset();
        mem_children.reset();
        assert !node.isDisposed();
        String id = context.getID();
        TCFNodeExecContext n = (TCFNodeExecContext)node.model.getNode(id);
        if (n == null) {
            n = new TCFNodeExecContext(node, id);
            n.postContextAddedDelta();
            add(n);
        }
        else {
            n.postAllChangedDelta();
        }
        mem_children.add(n);
        n.setMemoryContext(context);
    }

    void onAncestorContextChanged() {
        for (TCFNode n : getNodes()) ((TCFNodeExecContext)n).onAncestorContextChanged();
    }
}
