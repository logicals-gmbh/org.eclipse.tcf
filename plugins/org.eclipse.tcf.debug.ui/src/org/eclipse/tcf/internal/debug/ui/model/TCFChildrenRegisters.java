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
package org.eclipse.tcf.internal.debug.ui.model;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IRegisters;


public class TCFChildrenRegisters extends TCFChildren {

    TCFChildrenRegisters(TCFNode node) {
        super(node, 128);
    }

    void onSuspended(boolean func_call) {
        for (TCFNode n : getNodes()) ((TCFNodeRegister)n).onSuspended(func_call);
    }

    void onParentValueChanged() {
        for (TCFNode n : getNodes()) ((TCFNodeRegister)n).onParentValueChanged();
    }

    void onRegistersChanged() {
        for (TCFNode n : getNodes()) ((TCFNodeRegister)n).onRegistersChanged();
        reset();
    }

    @Override
    void onNodeDisposed(String id) {
        super.onNodeDisposed(id);
        if (node instanceof TCFNodeExecContext) {
            // CPU register nodes are special case:
            // they have executable node as parent,
            // but they are also referenced as children of stack frames
            for (TCFNode n : ((TCFNodeExecContext)node).getStackTrace().getNodes()) {
                ((TCFNodeStackFrame)n).getRegisters().onNodeDisposed(id);
            }
        }
    }

    @Override
    protected boolean startDataRetrieval() {
        IRegisters regs = node.model.getLaunch().getService(IRegisters.class);
        if (regs == null) {
            set(null, null, new HashMap<String,TCFNode>());
            return true;
        }
        if (node instanceof TCFNodeStackFrame) {
            TCFChildrenStackTrace stack_trace_cache = ((TCFNodeExecContext)node.parent).getStackTrace();
            if (!stack_trace_cache.validate(this)) return false; // node.getFrameNo() is not valid
            final int frame_no = ((TCFNodeStackFrame)node).getFrameNo();
            if (frame_no < 0) {
                set(null, null, new HashMap<String,TCFNode>());
                return true;
            }
        }
        assert command == null;
        command = regs.getChildren(node.id, new IRegisters.DoneGetChildren() {
            public void doneGetChildren(IToken token, Exception error, String[] contexts) {
                Map<String,TCFNode> data = null;
                if (command == token && error == null) {
                    int index = 0;
                    data = new HashMap<String,TCFNode>();
                    for (String id : contexts) {
                        TCFNodeRegister n = (TCFNodeRegister)node.model.getNode(id);
                        if (n == null) n = new TCFNodeRegister(node, id);
                        n.setIndex(index++);
                        data.put(id, n);
                    }
                }
                set(token, error, data);
            }
        });
        return false;
    }
}
