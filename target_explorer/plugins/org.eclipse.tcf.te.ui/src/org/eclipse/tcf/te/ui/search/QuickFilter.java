/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.search;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.tcf.te.core.interfaces.IPropertyChangeProvider;
import org.eclipse.tcf.te.ui.interfaces.ISchedulableEvent;

/**
 * A quick filter is a viewer filter that selects elements, which has the specified name pattern,
 * under a certain tree path. Other elements outside of this tree path is ignored.
 */
public class QuickFilter extends TablePatternFilter implements PropertyChangeListener {
	// The tree viewer to filter.
	private TreeViewer viewer;
	// The root path to select from.
	private TreePath root;
	// If the current filtering is global
	private boolean global;

	/**
	 * Create a quick filter for the specified viewer.
	 */
	public QuickFilter(TreeViewer viewer, TreePath root) {
		super((ILabelProvider) viewer.getLabelProvider());
		this.viewer = viewer;
		this.root = root;
		this.addPropertyChangeListener(this);
	}

	/**
	 * Show the pop up dialog for the specified root path.
	 *
	 * @param global If the filter is a global one.
	 */
	public void showFilterPopup(boolean global) {
		this.global = global;
		if (!isFiltering()) {
			viewer.addFilter(this);
		}
		QuickFilterPopup popup = new QuickFilterPopup(viewer, this);
		Point location = computePopupLocation();
		popup.open();
		popup.getShell().setLocation(location);
	}

	/**
	 * Compute the best location of the pop up dialog.
	 *
	 * @return The best location of the pop up dialog.
	 */
	private Point computePopupLocation() {
		Point location = null;
		if (!global) {
			TreeItem[] items = viewer.getTree().getSelection();
			if (items != null && items.length > 0) {
				for (TreeItem item : items) {
					viewer.getTree().showItem(item);
				}
				TreeItem item = items[0];
				Rectangle bounds = item.getBounds();
				location = new Point(bounds.x, bounds.y - bounds.height);
			}
			else {
				location = new Point(0, -viewer.getTree().getItemHeight());
			}
		}
		else {
			location = new Point(0, -viewer.getTree().getItemHeight());
		}
		location = viewer.getTree().toDisplay(location);
		return location;
	}

	/**
	 * Adjust the position of the pop up when the tree viewer has changed.
	 *
	 * @param popshell The shell of the pop up dialog.
	 */
	void adjustPopup(Shell popshell) {
		if (!global) {
			Point location = computePopupLocation();
			Point shellLocation = popshell.getLocation();
			if (shellLocation != null && !shellLocation.equals(location)) {
				popshell.setLocation(location);
			}
		}
	}

	/**
	 * Subclass PropertyChangeEvent and implement ISchedulable to provide an event which should be
	 * scheduled when the key stroke pauses for a certain time.
	 */
	private static class QuickFilterEvent extends PropertyChangeEvent implements ISchedulableEvent {
		private static final long serialVersionUID = 1L;
		// Remember the last time of a property change event caused by a key stroke
		private static long last_enqueue;
		// Maximum delay before the event should be scheduled.
		private static final long MAXIMUM_DELAY = 300L;
		// The effective tree viewer;
		private TreeViewer viewer;

		/**
		 * Constructor inherited.
		 *
		 * @param source
		 * @param propertyName
		 * @param oldValue
		 * @param newValue
		 */
		public QuickFilterEvent(TreeViewer viewer, Object source, String propertyName, Object oldValue, Object newValue) {
			super(source, propertyName, oldValue, newValue);
			this.viewer = viewer;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.interfaces.ISchedulable#eventQueued()
		 */
		@Override
		public synchronized void eventQueued() {
			last_enqueue = System.currentTimeMillis();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.interfaces.ISchedulable#isSchedulable()
		 */
		@Override
		public boolean isSchedulable() {
			return System.currentTimeMillis() - last_enqueue > MAXIMUM_DELAY;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.interfaces.ISchedulableEvent#isApplicable(org.eclipse.jface.viewers.TreeViewer)
		 */
		@Override
		public boolean isApplicable(TreeViewer viewer) {
			return this.viewer == viewer;
		}
	}

	/* (non-Javadoc)
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		Object element = root.getLastSegment();
		element = element == null ? viewer.getInput() : element;
		if (element != null) {
			IPropertyChangeProvider provider = getPropertyChangeProvider(element);
			if (provider != null) {
				provider.firePropertyChange(new QuickFilterEvent(viewer, element, evt.getPropertyName(), evt.getOldValue(), evt.getNewValue()));
			} else {
				viewer.refresh(element, true);
			}
		}
	}

	/**
	 * Get an adapter of IPropertyChangeProvider from the specified element.
	 *
	 * @param element The element to get the adapter from.
	 * @return The element's adapter or null if does not adapt to IPropertyChangeProvider.
	 */
	private IPropertyChangeProvider getPropertyChangeProvider(Object element) {
		IPropertyChangeProvider provider = null;
		if (element instanceof IPropertyChangeProvider) {
			provider = (IPropertyChangeProvider) element;
		}
		if (provider == null && element instanceof IAdaptable) {
			provider = (IPropertyChangeProvider) ((IAdaptable) element).getAdapter(IPropertyChangeProvider.class);
		}
		if (provider == null && element != null) {
			provider = (IPropertyChangeProvider) Platform.getAdapterManager().getAdapter(element, IPropertyChangeProvider.class);
		}
		return provider;
	}

	/**
	 * Reset the tree viewer to the original view by removing this filter.
	 */
	public void resetViewer() {
		viewer.removeFilter(this);
		@SuppressWarnings("unchecked")
        Map<TreePath, QuickFilter> filters = (Map<TreePath, QuickFilter>) viewer.getData("quick.filter"); //$NON-NLS-1$
		if (filters != null) {
			filters.remove(root);
		}
		setPattern(null);
	}

	/**
	 * If the current viewer is being filtered.
	 *
	 * @return true if it has this filter.
	 */
	private boolean isFiltering() {
		ViewerFilter[] filters = viewer.getFilters();
		if (filters != null) {
			for (ViewerFilter filter : filters) {
				if (filter == this) {
					return matcher != null;
				}
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.internal.utils.TablePatternFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		Assert.isNotNull(parentElement);
		return !shouldSelect(parentElement) || super.select(viewer, parentElement, element);
	}

	/**
	 * If the current parent element should be selected for matching.
	 *
	 * @param parentElement The parent element.
	 * @return true if it should continue matching.
	 */
	private boolean shouldSelect(Object parentElement) {
		Object rootElement = parentElement instanceof TreePath ? root : (root.getSegmentCount() == 0 ? viewer
		                .getInput() : root.getLastSegment());
		return parentElement.equals(rootElement);
	}

	/**
	 * If the element is being filtered.
	 *
	 * @param path The element to be checked.
	 * @return true if it is filtering.
	 */
	public boolean isFiltering(TreePath path) {
		return isFiltering() && matcher != null && root.equals(path);
	}
}
