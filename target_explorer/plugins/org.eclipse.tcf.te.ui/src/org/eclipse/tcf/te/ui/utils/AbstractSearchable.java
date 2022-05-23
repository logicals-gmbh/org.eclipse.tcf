/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.utils;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.te.ui.interfaces.IOptionListener;
import org.eclipse.tcf.te.ui.interfaces.ISearchable;
import org.eclipse.tcf.te.ui.search.TreeViewerSearchDialog;

/**
 * The base class that implements ISearchable and provide basic implementation method
 * for adding and removing listeners.
 */
public abstract class AbstractSearchable extends EventManager implements ISearchable {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchable#addOptionListener(org.eclipse.tcf.te.ui.interfaces.IOptionListener)
	 */
	@Override
    public void addOptionListener(IOptionListener listener) {
		super.addListenerObject(listener);
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchable#removeOptionListener(org.eclipse.tcf.te.ui.interfaces.IOptionListener)
	 */
	@Override
    public void removeOptionListener(IOptionListener listener) {
		super.removeListenerObject(listener);
    }

	/**
	 * Fire an option changed event to the listeners to notify
	 * the current option input has changed.
	 */
	protected void fireOptionChanged() {
		Object[] listeners = super.getListeners();
		for(Object listener : listeners) {
			((IOptionListener)listener).optionChanged(null);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchable#createCommonPart(org.eclipse.tcf.te.ui.internal.utils.TreeViewerSearchDialog, org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createCommonPart(TreeViewerSearchDialog dialog, Composite parent) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchable#createAdvancedPart(org.eclipse.tcf.te.ui.internal.utils.TreeViewerSearchDialog, org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createAdvancedPart(TreeViewerSearchDialog dialog, Composite parent) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchable#isInputValid()
	 */
	@Override
    public boolean isInputValid() {
	    return true;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchable#getSearchTitle(java.lang.Object)
	 */
	@Override
	public String getSearchTitle(Object rootElement) {
	    return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchable#getSearchMessage(java.lang.Object)
	 */
	@Override
	public String getSearchMessage(Object rootElement) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchable#getCustomMessage(java.lang.Object, java.lang.String)
	 */
	@Override
	public String getCustomMessage(Object rootElement, String key) {
	    return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchable#restoreValues(org.eclipse.jface.dialogs.IDialogSettings)
	 */
	@Override
    public void restoreValues(IDialogSettings settings) {
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchable#persistValues(org.eclipse.jface.dialogs.IDialogSettings)
	 */
	@Override
    public void persistValues(IDialogSettings settings) {
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchable#getElementText(java.lang.Object)
	 */
	@Override
	public String getElementText(Object element) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchable#getPreferredSize()
	 */
	@Override
    public Point getPreferredSize() {
	    return null;
    }
}
