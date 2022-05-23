/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.launch.ui.handler;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.tcf.te.launch.ui.internal.pages.LaunchExplorerEditorPage;
import org.eclipse.tcf.te.launch.ui.model.LaunchModel;
import org.eclipse.tcf.te.launch.ui.model.LaunchNode;
import org.eclipse.tcf.te.ui.interfaces.handler.IEditorHandlerDelegate;
import org.eclipse.tcf.te.ui.swt.DisplayUtil;
import org.eclipse.tcf.te.ui.trees.TreeControl;
import org.eclipse.tcf.te.ui.views.editor.Editor;
import org.eclipse.tcf.te.ui.views.editor.EditorInput;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;

/**
 * Launch properties command handler implementation.
 */
public class EditorHandlerDelegate implements IEditorHandlerDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.handler.IEditorHandlerDelegate#getEditorInput(java.lang.Object)
	 */
	@Override
	public IEditorInput getEditorInput(Object element) {
		if (element instanceof LaunchModel) {
			return new EditorInput(((LaunchModel)element).getModelRoot());
		}
		if (element instanceof LaunchNode) {
			return new EditorInput(((LaunchNode)element).getModel().getModelRoot());
		}
	    return new EditorInput(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.handler.IEditorHandlerDelegate#postOpenProperties(org.eclipse.ui.IEditorPart, java.lang.Object)
	 */
	@Override
	public void postOpenEditor(IEditorPart editor, final Object element) {
		if (editor instanceof FormEditor) {
			final FormEditor formEditor = (FormEditor)editor;
			DisplayUtil.safeAsyncExec(new Runnable() {
				@Override
				public void run() {
					IFormPage page = formEditor.setActivePage("org.eclipse.tcf.te.launch.ui.LaunchEditorPage"); //$NON-NLS-1$
					// If the element is a context node, select the node
					if (page != null && element instanceof LaunchModel || element instanceof LaunchNode) {
						TreeControl treeControl = ((LaunchExplorerEditorPage) page).getTreeControl();
						if (treeControl != null) {
							Viewer viewer = treeControl.getViewer();
							if (viewer != null) {
								viewer.setSelection(new StructuredSelection(element), true);
							}
						}
					}
					else if (page == null && formEditor instanceof Editor) {
						((Editor)formEditor).setActivePage(0);
					}
				}
			});
		}
	}
}
