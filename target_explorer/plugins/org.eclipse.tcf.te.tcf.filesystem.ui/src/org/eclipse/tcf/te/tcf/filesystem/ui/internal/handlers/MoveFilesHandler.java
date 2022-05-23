/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.handlers;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.dialogs.FSFolderSelectionDialog;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.operations.UiExecutor;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.ui.handlers.HandlerUtil;
/**
 * The handler that moves the selected files or folders to a destination folder.
 */
public class MoveFilesHandler extends AbstractHandler {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShellChecked(event);
		FSFolderSelectionDialog dialog = new FSFolderSelectionDialog(shell);
		IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
		List<IFSTreeNode> nodes = selection.toList();
		IPeerNode peer = nodes.get(0).getPeerNode();
		dialog.setInput(peer);
		dialog.setMovedNodes(nodes);
		if (dialog.open() == Window.OK) {
			Object obj = dialog.getFirstResult();
			Assert.isTrue(obj instanceof IFSTreeNode);
			IFSTreeNode dest = (IFSTreeNode) obj;
			UiExecutor.execute(dest.operationDropMove(nodes, new MoveCopyCallback()));
		}
		return null;
	}
}
