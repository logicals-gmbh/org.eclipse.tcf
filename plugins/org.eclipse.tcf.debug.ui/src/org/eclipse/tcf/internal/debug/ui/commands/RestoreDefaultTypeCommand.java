/*******************************************************************************
 * Copyright (c) 2009, 2012 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.commands;

import org.eclipse.tcf.internal.debug.ui.model.ICastToType;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;
import org.eclipse.tcf.protocol.Protocol;

public class RestoreDefaultTypeCommand extends AbstractActionDelegate {

    @Override
    protected void run() {
        final TCFNode node = getCastToTypeNode();
        if (node == null) return;
        Protocol.invokeLater(new Runnable() {
            public void run() {
                node.getModel().setCastToType(node.getID(), null);
            }
        });
    }

    @Override
    protected void selectionChanged() {
        TCFNode node = getCastToTypeNode();
        setEnabled(node != null && node.getModel().getCastToType(node.getID()) != null);
    }

    private TCFNode getCastToTypeNode() {
        TCFNode node = getSelectedNode();
        if (node instanceof ICastToType) return node;
        return null;
    }
}
