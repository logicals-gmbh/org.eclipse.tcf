/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.activator;

import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tcf.te.runtime.model.MessageModelNode;
import org.eclipse.tcf.te.runtime.preferences.ScopedEclipsePreferences;
import org.eclipse.tcf.te.runtime.tracing.TraceHandler;
import org.eclipse.tcf.te.ui.interfaces.ImageConsts;
import org.eclipse.tcf.te.ui.jface.images.AbstractImageDescriptor;
import org.eclipse.tcf.te.ui.trees.ViewerStateManager;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class UIPlugin extends AbstractUIPlugin {
	// The shared instance
	private static UIPlugin plugin;
	// The scoped preferences instance
	private static volatile ScopedEclipsePreferences scopedPreferences;
	// The trace handler instance
	private static volatile TraceHandler traceHandler;

	/**
	 * The constructor
	 */
	public UIPlugin() {
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static UIPlugin getDefault() {
		return plugin;
	}

	/**
	 * Convenience method which returns the unique identifier of this plugin.
	 */
	public static String getUniqueIdentifier() {
		if (getDefault() != null && getDefault().getBundle() != null) {
			return getDefault().getBundle().getSymbolicName();
		}
		return "org.eclipse.tcf.te.ui"; //$NON-NLS-1$
	}

	/**
	 * Return the scoped preferences for this plugin.
	 */
	public static ScopedEclipsePreferences getScopedPreferences() {
		if (scopedPreferences == null) {
			scopedPreferences = new ScopedEclipsePreferences(getUniqueIdentifier());
		}
		return scopedPreferences;
	}

	/**
	 * Returns the bundles trace handler.
	 *
	 * @return The bundles trace handler.
	 */
	public static TraceHandler getTraceHandler() {
		if (traceHandler == null) {
			traceHandler = new TraceHandler(getUniqueIdentifier());
		}
		return traceHandler;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		// Load the tree viewer's action.
		ViewerStateManager.getInstance().loadViewerStates();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		// Save the tree viewer's action.
		ViewerStateManager.getInstance().storeViewerStates();
		plugin = null;
		scopedPreferences = null;
		traceHandler = null;
		super.stop(context);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#initializeImageRegistry(org.eclipse.jface.resource.ImageRegistry)
	 */
	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		URL url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_WIZBAN + "newtarget_wiz.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.NEW_TARGET_WIZARD, ImageDescriptor.createFromURL(url));

		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_DLCL + "newtarget_wiz.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.NEW_TARGET_WIZARD_DISABLED, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_ELCL + "newtarget_wiz.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.NEW_TARGET_WIZARD_ENABLED, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_DLCL + "filter_ps.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.VIEWER_FILTER_CONFIG_DISABLED, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_ELCL + "filter_ps.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.VIEWER_FILTER_CONFIG_ENABLED, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_ELCL + "collapseall.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.VIEWER_COLLAPSE_ALL, ImageDescriptor.createFromURL(url));

		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_ELCL + "connect.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.ACTION_CONNECT, ImageDescriptor.createFromURL(url));

		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OVR + "gold_ovr.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.GOLD_OVR, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OVR + "green_ovr.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.GREEN_OVR, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OVR + "grey_ovr.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.GREY_OVR, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OVR + "red_ovr.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.RED_OVR, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OVR + "filtering_ovr.png"); //$NON-NLS-1$
		registry.put(ImageConsts.FILTERING_OVR, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OVR + "redX_ovr.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.RED_X_OVR, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OVR + "busy.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.BUSY_OVR, ImageDescriptor.createFromURL(url));

		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OBJ + "pending.gif"); //$NON-NLS-1$
		registry.put(MessageModelNode.OBJECT_MESSAGE_PENDING_ID, ImageDescriptor.createFromURL(url));
	}

	/**
	 * Loads the image registered under the specified key from the image
	 * registry and returns the <code>Image</code> object instance.
	 *
	 * @param key The key the image is registered with.
	 * @return The <code>Image</code> object instance or <code>null</code>.
	 */
	public static Image getImage(String key) {
		return getDefault().getImageRegistry().get(key);
	}

	/**
	 * Loads the image registered under the specified key from the image
	 * registry and returns the <code>ImageDescriptor</code> object instance.
	 *
	 * @param key The key the image is registered with.
	 * @return The <code>ImageDescriptor</code> object instance or <code>null</code>.
	 */
	public static ImageDescriptor getImageDescriptor(String key) {
		return getDefault().getImageRegistry().getDescriptor(key);
	}

	/**
	 * Loads the image given by the specified image descriptor from the image
	 * registry. If the image has been loaded ones before already, the cached
	 * <code>Image</code> object instance is returned. Otherwise, the <code>
	 * Image</code> object instance will be created and cached before returned.
	 *
	 * @param descriptor The image descriptor.
	 * @return The corresponding <code>Image</code> object instance or <code>null</code>.
	 */
	public static Image getSharedImage(AbstractImageDescriptor descriptor) {
		ImageRegistry registry = getDefault().getImageRegistry();

		String imageKey = descriptor.getDecriptorKey();
		Image image = registry.get(imageKey);
		if (image == null) {
			registry.put(imageKey, descriptor);
			image = registry.get(imageKey);
		}

		return image;
	}
}
