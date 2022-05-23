/*******************************************************************************
 * Copyright (c) 2011, 2016 Wind River Systems, Inc. and others.
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
import java.util.HashSet;

public class TCFChildrenLogExpressions extends TCFChildren {

    private final HashSet<String> scripts = new HashSet<String>();

    TCFChildrenLogExpressions(TCFNodeExecContext node) {
        super(node, 16);
    }

    void onSuspended() {
        for (TCFNode n : getNodes()) ((TCFNodeExpression)n).onSuspended(false);
        scripts.clear();
        reset();
    }

    void onRegisterValueChanged() {
        for (TCFNode n : getNodes()) ((TCFNodeExpression)n).onRegisterValueChanged();
    }

    void onMemoryChanged() {
        for (TCFNode n : getNodes()) ((TCFNodeExpression)n).onMemoryChanged();
    }

    void onMemoryMapChanged() {
        for (TCFNode n : getNodes()) ((TCFNodeExpression)n).onMemoryMapChanged();
    }

    public TCFNodeExpression findScript(String script) {
        for (TCFNode n : getNodes()) {
            TCFNodeExpression e = (TCFNodeExpression)n;
            if (script.equals(e.getScript())) return e;
        }
        return null;
    }

    public void addScript(String script) {
        if (scripts.add(script)) reset();
    }

    @Override
    protected boolean startDataRetrieval() {
        HashMap<String,TCFNode> data = new HashMap<String,TCFNode>();
        for (String script : scripts) {
            TCFNodeExpression expression_node = findScript(script);
            if (expression_node == null) {
                add(expression_node = new TCFNodeExpression(node, script, null, null, null, null, -1, false));
            }
            data.put(expression_node.id, expression_node);
        }
        set(null, null, data);
        return true;
    }
}
