/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.launch.ui.internal.pages;

import org.eclipse.tcf.te.launch.ui.nls.Messages;
import org.eclipse.tcf.te.ui.views.editor.pages.AbstractTreeViewerExplorerEditorPage;

/**
 * The editor page to explore the launches.
 */
public class LaunchExplorerEditorPage extends AbstractTreeViewerExplorerEditorPage {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.TreeViewerExplorerEditorPage#getViewerId()
	 */
	@Override
	protected String getViewerId() {
		return "org.eclipse.tcf.te.launch.ui.viewer.launches"; //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractCustomFormToolkitEditorPage#getFormTitle()
	 */
	@Override
    protected String getFormTitle() {
	    return Messages.LaunchExplorerEditorPage_PageTitle;
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.TreeViewerExplorerEditorPage#getDoubleClickCommandId()
	 */
	@Override
    protected String getDoubleClickCommandId() {
	    return "org.eclipse.ui.navigator.Open"; //$NON-NLS-1$
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractCustomFormToolkitEditorPage#getContextHelpId()
	 */
	@Override
    protected String getContextHelpId() {
	    return "org.eclipse.tcf.te.launch.ui.LaunchEditorPage"; //$NON-NLS-1$
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.TreeViewerExplorerEditorPage#getViewerInput()
	 */
	@Override
    protected Object getViewerInput() {
	    return getEditorInputNode();
    }
}
