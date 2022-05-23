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
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * Abstract tree section implementation.
 */
public abstract class AbstractTreeSection extends AbstractStructuredViewerSection implements ISelectionChangedListener, IDoubleClickListener {

	/**
	 * Tree section tree part adapter implementation.
	 */
	protected class TreePartAdapter extends TreePart {

		/**
		 * Constructor.
		 *
		 * @param labels The list of label to apply to the created buttons in the given order. Must not be <code>null</code>.
		 */
		public TreePartAdapter(String[] labels) {
			super(labels);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.forms.parts.TablePart#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
		 */
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			getManagedForm().fireSelectionChanged(AbstractTreeSection.this, event.getSelection());
			AbstractTreeSection.this.selectionChanged(event);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.forms.parts.TablePart#doubleClick(org.eclipse.jface.viewers.DoubleClickEvent)
		 */
		@Override
		public void doubleClick(DoubleClickEvent event) {
			AbstractTreeSection.this.doubleClick(event);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.forms.parts.AbstractPartWithButtons#onButtonSelected(org.eclipse.swt.widgets.Button)
		 */
		@Override
		protected void onButtonSelected(Button button) {
			AbstractTreeSection.this.onButtonSelected(button);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.forms.parts.AbstractPartWithButtons#createButtonsPanel(org.eclipse.swt.widgets.Composite, org.eclipse.ui.forms.widgets.FormToolkit)
		 */
		@Override
		protected Composite createButtonsPanel(Composite parent, FormToolkit toolkit) {
			Composite panel = super.createButtonsPanel(parent, toolkit);
			initializeButtonsEnablement();
			if (parent.getData("filtered") != null) { //$NON-NLS-1$
				GridLayout layout = (GridLayout) panel.getLayout();
				layout.marginHeight = 28;
			}
			return panel;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.forms.parts.AbstractPartWithButtons#onButtonCreated(org.eclipse.swt.widgets.Button)
		 */
		@Override
		protected void onButtonCreated(Button button) {
		    AbstractTreeSection.this.onButtonCreated(button);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.forms.parts.TreePart#createTreeViewer(org.eclipse.swt.widgets.Composite, int)
		 */
		@Override
		protected TreeViewer createTreeViewer(Composite parent, int style) {
			return AbstractTreeSection.this.createTreeViewer(parent, style);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.forms.parts.TreePart#configureTreeViewer(org.eclipse.jface.viewers.TreeViewer)
		 */
		@Override
		protected void configureTreeViewer(TreeViewer viewer) {
			super.configureTreeViewer(viewer);
			AbstractTreeSection.this.configureTreeViewer(viewer);
		}
	}

	/**
	 * Constructor.
	 *
	 * @param form The parent managed form. Must not be <code>null</code>.
	 * @param parent The parent composite. Must not be <code>null</code>.
	 * @param style The section style.
	 * @param labels The list of label to apply to the created buttons in the given order. Must not be <code>null</code>.
	 */
	public AbstractTreeSection(IManagedForm form, Composite parent, int style, String[] labels) {
		this(form, parent, style, true, labels);
	}

	/**
	 * Constructor.
	 *
	 * @param form The parent managed form. Must not be <code>null</code>.
	 * @param parent The parent composite. Must not be <code>null</code>.
	 * @param style The section style.
	 * @param titleBar If <code>true</code>, the title bar style bit is added to <code>style</code>.
	 * @param labels The list of label to apply to the created buttons in the given order. Must not be <code>null</code>.
	 */
	public AbstractTreeSection(IManagedForm form, Composite parent, int style, boolean titleBar, String[] labels) {
		super(form, parent, style, titleBar, labels);

	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.forms.parts.AbstractStructuredViewerSection#createViewerPart(java.lang.String[])
	 */
	@Override
	protected AbstractStructuredViewerPart createViewerPart(String[] labels) {
		return new TreePartAdapter(labels);
	}

	/**
	 * Returns the tree part instance.
	 *
	 * @return The tree part instance.
	 */
	public TreePart getTreePart() {
		return (TreePart)getViewerPart();
	}

	/**
	 * Reveals the tree viewer instance to clients.
	 *
	 * @return The tree viewer instance or <code>null</code>.
	 */
	public TreeViewer getViewer() {
		StructuredViewer viewer =  getTreePart().getViewer();
		return viewer instanceof TreeViewer ? (TreeViewer)viewer : null;
	}

	/**
	 * Creates the tree viewer instance.
	 *
	 * @param parent The parent composite. Must not be <code>null</code>.
	 * @param style The viewer style.
	 *
	 * @return The tree viewer instance.
	 */
	protected TreeViewer createTreeViewer(Composite parent, int style) {
		return new TreeViewer(parent, style);
	}

	/**
	 * Configures the tree viewer instance.
	 *
	 * @param viewer The tree viewer instance. Must not be <code>null</code<.
	 */
	protected void configureTreeViewer(TreeViewer viewer) {
		Assert.isNotNull(viewer);
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
	 * Called from {@link TreePartAdapter#createButtonsPanel(Composite, FormToolkit)}.
	 *
	 * @param button The created button. Must not be <code>null</code>.
	 */
	protected void onButtonCreated(Button button) {
	}

	/**
	 * Initialize the enablement of the buttons in the buttons bar.
	 * <p>
	 * Called from {@link TreePartAdapter#createButtonsPanel(Composite, FormToolkit)}.
	 */
	protected void initializeButtonsEnablement() {
	}

	/**
	 * Indicates whether the sections parent page has become the active in the editor.
	 *
	 * @param active <code>True</code> if the parent page should be visible, <code>false</code> otherwise.
	 */
	public void setActive(boolean active) {
	}
}
