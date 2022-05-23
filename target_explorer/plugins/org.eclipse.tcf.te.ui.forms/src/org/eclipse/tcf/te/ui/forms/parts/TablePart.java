/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.forms.parts;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * Table part implementation.
 */
public class TablePart extends AbstractStructuredViewerPart implements ISelectionChangedListener, IDoubleClickListener {
	// A flag to mark the table part "read-only"
	/* default */ boolean readOnly;

	/**
	 * Constructor.
	 *
	 * @param labels The list of label to apply to the created buttons in the given order. Must not be <code>null</code>.
	 */
	public TablePart(String[] labels) {
		super(labels);
	}

	/**
	 * Creates the table viewer instance.
	 *
	 * @param parent The parent composite. Must not be <code>null</code>.
	 * @param style The viewer style.
	 *
	 * @return The table viewer instance.
	 */
	protected TableViewer createTableViewer(Composite parent, int style) {
		return new TableViewer(parent, style);
	}

	/**
	 * Configures the table viewer instance.
	 *
	 * @param viewer The table viewer instance. Must not be <code>null</code<.
	 */
	protected void configureTableViewer(TableViewer viewer) {
		Assert.isNotNull(viewer);

		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent e) {
				TablePart.this.selectionChanged(e);
			}
		});
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent e) {
				TablePart.this.doubleClick(e);
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.forms.parts.AbstractStructuredViewerPart#createStructuredViewer(org.eclipse.swt.widgets.Composite, int, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	@Override
	protected StructuredViewer createStructuredViewer(Composite parent, int style, FormToolkit toolkit) {
		Assert.isNotNull(parent);

		// Adjust the style bits
		style |= SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER;

		TableViewer viewer = createTableViewer(parent, style);
		Assert.isNotNull(viewer);
		configureTableViewer(viewer);

		return viewer;
	}

	/**
	 * Returns the table viewer instance.
	 *
	 * @return The table viewer instance or <code>null</code>.
	 */
	protected TableViewer getTableViewer() {
		return (TableViewer)getViewer();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IDoubleClickListener#doubleClick(org.eclipse.jface.viewers.DoubleClickEvent)
	 */
	@Override
	public void doubleClick(DoubleClickEvent event) {
	}

	/**
	 * Set the table part read-only state.
	 *
	 * @param readOnly <code>True</code> to set the table part read-only.
	 */
	public final void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		updateButtons();
	}

	/**
	 * Returns the table part read-only state.
	 *
	 * @return <code>True</code> if the table part is read-only, <code>false</code> otherwise.
	 */
	public final boolean isReadOnly() {
		return readOnly;
	}

	/**
	 * Update the button enablements.
	 */
	protected void updateButtons() {
		// nothing to do
	}
}
