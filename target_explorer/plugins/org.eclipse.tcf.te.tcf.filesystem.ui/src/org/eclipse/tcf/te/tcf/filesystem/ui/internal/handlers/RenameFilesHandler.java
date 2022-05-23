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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.celleditor.FSCellValidator;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.operations.UiExecutor;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;
import org.eclipse.tcf.te.ui.dialogs.RenameDialog;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * The handler that renames the selected file or folder.
 */
public class RenameFilesHandler extends AbstractHandler {
	// The currently focused viewer.
	private static TreeViewer currentViewer;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection sel = (IStructuredSelection) HandlerUtil.getCurrentSelectionChecked(event);
		if (!sel.isEmpty()) {
			IFSTreeNode node = (IFSTreeNode) sel.getFirstElement();
			boolean inPlaceEditor = UIPlugin.isInPlaceEditor();
			if (inPlaceEditor) {
				// If it is configured to use in-place editor, then invoke the editor.
				if (currentViewer != null) {
					Control control = currentViewer.getControl();
					if (!control.isDisposed()) {
						currentViewer.editElement(node, 0);
					}
				}
			}
			else {
				Shell shell = HandlerUtil.getActiveShellChecked(event);
				RenameDialog dialog = createRenameDialog(shell, node);
				int ok = dialog.open();
				if (ok == Window.OK) {
					// Do the renaming.
					UiExecutor.execute(node.operationRename(dialog.getNewName()));
				}
			}
		}
		return null;
	}

	/**
	 * Create a renaming dialog for the specified file/folder node.
	 *
	 * @param shell The parent shell.
	 * @param node The file/folder node.
	 * @return The renaming dialog.
	 */
	private RenameDialog createRenameDialog(Shell shell, IFSTreeNode node) {
		String[] names = getUsedNames(node);
		String title;
		if (node.isFile()) {
			title = Messages.RenameFilesHandler_TitleRenameFile;
		}
		else if (node.isDirectory()) {
			title = Messages.RenameFilesHandler_TitleRenameFolder;
		}
		else {
			title = Messages.RenameFilesHandler_TitleRename;
		}
		String formatRegex;
		if (node.isWindowsNode()) {
			formatRegex = FSCellValidator.WIN_FILENAME_REGEX;
		}
		else {
			formatRegex = FSCellValidator.UNIX_FILENAME_REGEX;
		}
		String error;
		if (node.isWindowsNode()) {
			error = Messages.FSRenamingAssistant_WinIllegalCharacters;
		}
		else {
			error = Messages.FSRenamingAssistant_UnixIllegalCharacters;
		}
		String prompt = Messages.RenameFilesHandler_RenamePromptMessage;
		String usedError = Messages.FSRenamingAssistant_NameAlreadyExists;
		String label = Messages.RenameFilesHandler_PromptNewName;
		return new RenameDialog(shell, title, null, prompt, usedError, error, label, node.getName(), formatRegex, names, null);
	}

	/**
	 * Get the used names in the specified folder.
	 *
	 * @param folder The folder.
	 * @return Used names.
	 */
	private String[] getUsedNames(IFSTreeNode folder) {
		IFSTreeNode[] nodes = folder.getParent().getChildren();
		if (nodes == null)
			return new String[0];

		List<String> usedNames = new ArrayList<String>();
		for (IFSTreeNode node : nodes) {
			usedNames.add(node.getName());
		}
		return usedNames.toArray(new String[usedNames.size()]);
	}

	/**
	 * Set the currently focused tree viewer. Called by Target Explorer and FSTreeControl to set the
	 * current viewer.
	 *
	 * @param viewer The currently focused tree viewer.
	 */
	public static void setCurrentViewer(TreeViewer viewer) {
		currentViewer = viewer;
	}
}
