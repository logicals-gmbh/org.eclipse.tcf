/*******************************************************************************
 * Copyright (c) 2011 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.controls;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.swt.widgets.Composite;

/**
 * Base implementation of a common control allowing to select
 * the content of the edit field control from a dialog. The dialog is associated
 * to the base edit browse text controls button.
 */
public class BaseJFaceDialogSelectionControl extends BaseEditBrowseTextControl {
	private Dialog dialogControl;

	/**
	 * Constructor.
	 *
	 * @param parentPage The parent page this control is embedded in.
	 *                   Might be <code>null</code> if the control is not associated with a page.
	 */
	public BaseJFaceDialogSelectionControl(IDialogPage parentPage) {
		super(parentPage);
		setIsGroup(true);
		setHasHistory(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl#isAdjustEditFieldControlWidthHint()
	 */
	@Override
	protected boolean isAdjustEditFieldControlWidthHint() {
		return true;
	}

	/**
	 * Returns the dialog control.
	 *
	 * @return The dialog control or <code>null</code> if the control has not been created yet.
	 */
	public Dialog getDialogControl() {
		return dialogControl;
	}

	/**
	 * The method is called to create the dialog control. Subclasses may override this method
	 * to create their own dialog control. The default implementation returns <code>null</code>.
	 *
	 * @param parent The parent control for the button control to create. Must not be <code>null</code>!
	 * @return The created button control.
	 */
	protected Dialog doCreateDialogControl(Composite parent) {
		Assert.isNotNull(parent);
		return null;
	}

	/**
	 * Configure the controls associated dialog before the dialogs is opened. Subclasses may use
	 * this hook to configure the controls associated dialog for their specific needs.
	 *
	 * @param dialog The dialog to configure. Must not be <code>null</code>!
	 */
	protected void configureDialogControl(Dialog dialog) {
		Assert.isNotNull(dialog);
	}

	/**
	 * Opens the given dialog and wait till the user pressed either OK or cancel. In
	 * case the user pressed OK and have selected a element within the dialog, the
	 * selected element is returned as string. In case the user canceled the dialog,
	 * the method returns <code>null</code>. The default implementation opens nothing
	 * and returns <code>null</code>!
	 *
	 * @param dialog The dialog to open. Must not be <code>null</code>.
	 * @return The selected element or <code>null</code>.
	 */
	protected String doOpenDialogControl(Dialog dialog) {
		Assert.isNotNull(dialog);
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl#onButtonControlSelected()
	 */
	@Override
	protected void onButtonControlSelected() {
		// create and configure the controls associated dialog
		dialogControl = doCreateDialogControl(getParentControl());
		configureDialogControl(dialogControl);

		// open the dialog and get the user selected element
		String selectedElement = doOpenDialogControl(dialogControl);
		// apply the selected element in case the user pressed OK.
		if (selectedElement != null) {
			doApplyElementFromDialogControl(selectedElement);
		}

		// finally, validate the control
		isValid();
	}

	/**
	 * Apply the selected element returned from the controls associated dialog to the
	 * control. The default implementation applies the given element as is to the edit field
	 * control. Subclasses may override this method to run additional logic just before
	 * applying the selected element to the control.
	 *
	 * @param selectedElement The selected element from that controls associated dialog. Must not be <code>null</code>.
	 */
	protected void doApplyElementFromDialogControl(String selectedElement) {
		Assert.isNotNull(selectedElement);
		setEditFieldControlText(selectedElement);
	}
}
