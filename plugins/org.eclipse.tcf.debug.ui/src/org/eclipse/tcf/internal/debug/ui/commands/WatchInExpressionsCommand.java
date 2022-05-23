/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.commands;

import org.eclipse.debug.core.IExpressionManager;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.tcf.internal.debug.ui.Activator;
import org.eclipse.tcf.internal.debug.ui.model.IWatchInExpressions;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExecContext;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExpression;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeStackFrame;
import org.eclipse.tcf.util.TCFDataCache;
import org.eclipse.tcf.util.TCFTask;
import org.eclipse.ui.IWorkbenchPage;

public class WatchInExpressionsCommand extends AbstractActionDelegate {

    @Override
    protected void selectionChanged() {
        setEnabled(getNodes().length > 0);
    }

    @Override
    protected void run() {
        try {
            IWorkbenchPage page = getWindow().getActivePage();
            page.showView(IDebugUIConstants.ID_EXPRESSION_VIEW, null, IWorkbenchPage.VIEW_ACTIVATE);
            for (final TCFNode node : getNodes()) {
                final IExpressionManager manager = node.getModel().getExpressionManager();
                IExpression e = new TCFTask<IExpression>(node.getChannel()) {
                    public void run() {
                        try {
                            IExpression e = null;
                            if (node instanceof IWatchInExpressions) {
                                TCFDataCache<String> text_cache = ((IWatchInExpressions)node).getExpressionText();
                                if (!text_cache.validate(this)) return;
                                String text_data = text_cache.getData();
                                if (text_data != null) {
                                    for (final IExpression x : manager.getExpressions()) {
                                        if (text_data.equals(x.getExpressionText())) {
                                            done(null);
                                            return;
                                        }
                                    }
                                    e = manager.newWatchExpression(text_data);
                                }
                            }
                            done(e);
                        }
                        catch (Exception x) {
                            error(x);
                        }
                    }
                }.get();
                if (e != null) manager.addExpression(e);
            }
        }
        catch (Exception x) {
            Activator.log("Cannot open expressions view", x);
        }
    }

    private TCFNode[] getNodes() {
        TCFNode[] arr = getSelectedNodes();
        for (TCFNode n : arr) {
            if (n instanceof TCFNodeExpression) {
                if (((TCFNodeExpression)n).isEmpty()) return new TCFNode[0];
                if (IDebugUIConstants.ID_EXPRESSION_VIEW.equals(getPart().getSite().getId()) &&
                    (n.getParent() instanceof TCFNodeExecContext || n.getParent() instanceof TCFNodeStackFrame))
                return new TCFNode[0];
            }
            if (n instanceof IWatchInExpressions) continue;
            return new TCFNode[0];
        }
        return arr;
    }
}
