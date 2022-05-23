/*******************************************************************************
 * Copyright (c) 2008, 2016 Wind River Systems, Inc. and others.
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
import org.eclipse.tcf.services.IExpressions;

public class TCFChildrenLocalVariables extends TCFChildren {

    private final TCFNodeStackFrame node;

    TCFChildrenLocalVariables(TCFNodeStackFrame node) {
        super(node, 128);
        this.node = node;
    }

    void onSuspended(boolean func_call) {
        if (!func_call) reset();
        for (TCFNode n : getNodes()) ((TCFNodeExpression)n).onSuspended(func_call);
    }

    void onRegisterValueChanged() {
        for (TCFNode n : getNodes()) ((TCFNodeExpression)n).onRegisterValueChanged();
    }

    void onMemoryChanged() {
        for (TCFNode n : getNodes()) ((TCFNodeExpression)n).onMemoryChanged();
    }

    void onMemoryMapChanged() {
        reset();
        for (TCFNode n : getNodes()) ((TCFNodeExpression)n).onMemoryMapChanged();
    }

    @Override
    protected boolean startDataRetrieval() {
        IExpressions exps = node.model.getLaunch().getService(IExpressions.class);
        if (exps == null || node.isEmulated()) {
            set(null, null, new HashMap<String,TCFNode>());
            return true;
        }
        TCFChildrenStackTrace stack_trace_cache = ((TCFNodeExecContext)node.parent).getStackTrace();
        if (!stack_trace_cache.validate(this)) return false; // node.getFrameNo() is not valid
        if (node.getFrameNo() < 0) {
            set(null, null, new HashMap<String,TCFNode>());
            return true;
        }
        assert command == null;
        command = exps.getChildren(node.id, new IExpressions.DoneGetChildren() {
            public void doneGetChildren(IToken token, Exception error, String[] contexts) {
                Map<String,TCFNode> data = null;
                if (command == token && error == null) {
                    int cnt = 0;
                    data = new HashMap<String,TCFNode>();
                    for (String id : contexts) {
                        TCFNodeExpression n = (TCFNodeExpression)node.model.getNode(id);
                        if (n == null) n = new TCFNodeExpression(node, null, null, null, id, null, -1, false);
                        assert n.id.equals(id);
                        assert n.parent == node;
                        n.setSortPosition(cnt++);
                        data.put(n.id, n);
                    }
                }
                set(token, error, data);
            }
        });
        return false;
    }
}
