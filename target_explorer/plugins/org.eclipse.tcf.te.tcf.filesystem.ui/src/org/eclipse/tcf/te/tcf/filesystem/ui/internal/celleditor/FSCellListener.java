/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.celleditor;

import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.tcf.te.tcf.filesystem.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.ImageConsts;

/**
 * FSCellListener is an <code>ICellEditorListener</code> that listens to the modification event and displays
 * error messages in a tool tip when the new name entered is not valid.
 */
public class FSCellListener implements ICellEditorListener {
	// The cell editor used to enter the new name for renaming.
	private TextCellEditor editor;
	// The tool tip used to display the error message.
	private DefaultToolTip tooltip;

	/**
	 * Create an FSCellListener using the specified cell editor.
	 * 
	 * @param editor The cell editor
	 */
	public FSCellListener(TextCellEditor editor) {
		this.editor = editor;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellEditorListener#applyEditorValue()
	 */
	@Override
	public void applyEditorValue() {
		disposeToolTip();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellEditorListener#cancelEditor()
	 */
	@Override
	public void cancelEditor() {
		disposeToolTip();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellEditorListener#editorValueChanged(boolean, boolean)
	 */
	@Override
	public void editorValueChanged(boolean oldValidState, boolean newValidState) {
		if (!newValidState) {
			// If it is an invalid input, then display a tool tip showing the error.
			if (tooltip == null) {
				tooltip = new DefaultToolTip(editor.getControl(), ToolTip.RECREATE, true);
				tooltip.setImage(UIPlugin.getImage(ImageConsts.ERROR_IMAGE));
			}
			tooltip.setText(editor.getErrorMessage());
			Control control = editor.getControl();
			Point pOnScr = control.getSize();
			pOnScr.x = 0;
			tooltip.show(pOnScr);
		}
		else {
			// Dispose the tool tip if it is valid.
			disposeToolTip();
		}
	}

	/**
	 * Dispose the tool tip used to display error message.
	 */
	private void disposeToolTip() {
		if (tooltip != null) {
			tooltip.hide();
			tooltip = null;
		}
	}
}
