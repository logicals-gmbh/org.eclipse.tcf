/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.internal;

import java.util.EventObject;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.ui.views.events.ViewerContentChangeEvent;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.INavigatorContentService;

/**
 * View common viewer implementation.
 */
public class ViewViewer extends CommonViewer {
	// Flag to mark the viewer silent. In silent mode, no
	// ViewerContentChangeEvents are send.
	private boolean silent = false;

	// Remember the last double click selection event state mask
	private int lastDoubleClickSelectionEventStateMask = 0;

	/**
	 * Constructor.
	 *
	 * @param view
	 *            The common navigator based parent view. Must not be <code>null</code>.
	 * @param viewerId
	 *            An id tied to the extensions that is used to focus specific
	 *            content to a particular instance of the Common Navigator
	 * @param parent
	 *            A Composite parent to contain the actual SWT widget
	 * @param style
	 *            A style mask that will be used to create the TreeViewer
	 *            Composite.
	 */
	public ViewViewer(String viewerId, Composite parent, int style) {
		super(viewerId, parent, style);
	}

	/**
	 * Fire the given event if the viewer is not in silent mode.
	 *
	 * @param event The event or <code>null</code>.
	 */
	private void fireEvent(EventObject event) {
		if (!silent && event != null) {
		    EventManager.getInstance().fireEvent(event);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.navigator.CommonViewer#init()
	 */
	@Override
    protected void init() {
		setUseHashlookup(true);
		INavigatorContentService contentService = getNavigatorContentService();
		setContentProvider(contentService.createCommonContentProvider());
		setLabelProvider(new ViewViewerDecoratingLabelProvider(this, contentService.createCommonLabelProvider()));
		initDragAndDrop();
    }

	/**
	 * Sets the viewers event firing silent mode.
	 *
	 * @param silent <code>True</code> to stop firing change events, <code>false</code> otherwise.
	 * @return <code>True</code> if the silent mode changed.
	 */
	public boolean setSilentMode(boolean silent) {
		boolean changed = this.silent != silent;
		this.silent = silent;
		return changed;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.CommonViewer#add(java.lang.Object, java.lang.Object[])
	 */
	@Override
	public void add(Object parentElement, Object[] childElements) {
	    super.add(parentElement, childElements);

	    ViewerContentChangeEvent event = new ViewerContentChangeEvent(this, ViewerContentChangeEvent.ADD);
	    fireEvent(event);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.CommonViewer#remove(java.lang.Object[])
	 */
	@Override
	public void remove(Object[] elements) {
	    super.remove(elements);

	    ViewerContentChangeEvent event = new ViewerContentChangeEvent(this, ViewerContentChangeEvent.REMOVE);
	    fireEvent(event);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.AbstractTreeViewer#remove(java.lang.Object, java.lang.Object[])
	 */
	@Override
	public void remove(Object parent, Object[] elements) {
	    super.remove(parent, elements);

	    ViewerContentChangeEvent event = new ViewerContentChangeEvent(this, ViewerContentChangeEvent.REMOVE);
	    fireEvent(event);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.CommonViewer#refresh(java.lang.Object, boolean)
	 */
	@Override
	public void refresh(Object element, boolean updateLabels) {
	    super.refresh(element, updateLabels);

	    ViewerContentChangeEvent event = new ViewerContentChangeEvent(this, ViewerContentChangeEvent.REFRESH);
	    fireEvent(event);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.CommonViewer#handleDoubleSelect(org.eclipse.swt.events.SelectionEvent)
	 */
	@Override
	protected void handleDoubleSelect(SelectionEvent event) {
		lastDoubleClickSelectionEventStateMask = event != null ? event.stateMask : 0;
	    super.handleDoubleSelect(event);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StructuredViewer#fireDoubleClick(org.eclipse.jface.viewers.DoubleClickEvent)
	 */
	@Override
	protected void fireDoubleClick(DoubleClickEvent event) {
		boolean altPressed = (lastDoubleClickSelectionEventStateMask & SWT.ALT) != 0;
		if (altPressed) {
			event = new AltDoubleClickEvent((Viewer)event.getSource(), event.getSelection());
		}
	    super.fireDoubleClick(event);
	}
}
