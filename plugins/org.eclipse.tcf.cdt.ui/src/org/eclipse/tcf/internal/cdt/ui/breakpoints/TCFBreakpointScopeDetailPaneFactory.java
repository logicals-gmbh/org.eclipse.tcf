/*******************************************************************************
 * Copyright (c) 2008, 2013 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.cdt.ui.breakpoints;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.debug.ui.IDetailPane;
import org.eclipse.debug.ui.IDetailPaneFactory;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 *
 */
public class TCFBreakpointScopeDetailPaneFactory implements IDetailPaneFactory {

    public IDetailPane createDetailPane(String paneID) {
        assert paneID.equals(TCFBreakpointScopeDetailPane.ID);
        return new TCFBreakpointScopeDetailPane();
    }

    public String getDefaultDetailPane(IStructuredSelection selection) {
        return TCFBreakpointScopeDetailPane.ID;
    }

    public String getDetailPaneDescription(String paneID) {
        return TCFBreakpointScopeDetailPane.NAME;
    }

    public String getDetailPaneName(String paneID) {
        return TCFBreakpointScopeDetailPane.DESC;
    }

    public Set<String> getDetailPaneTypes(IStructuredSelection selection) {
        HashSet<String> set = new HashSet<String>();
        set.add(TCFBreakpointScopeDetailPane.ID);
        return set;
    }
}
