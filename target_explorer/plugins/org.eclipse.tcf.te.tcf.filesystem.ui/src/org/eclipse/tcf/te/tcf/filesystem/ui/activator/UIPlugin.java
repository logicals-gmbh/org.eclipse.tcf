/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * William Chen (Wind River) - [345387] Open the remote files with a proper editor
 * William Chen (Wind River) - [345552] Edit the remote files with a proper editor
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.activator;

import java.net.URL;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tcf.te.tcf.filesystem.ui.interfaces.preferences.IPreferenceKeys;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.ImageConsts;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.autosave.SaveAllListener;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.autosave.SaveListener;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.operations.FsClipboard;
import org.eclipse.tcf.te.ui.jface.images.AbstractImageDescriptor;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class UIPlugin extends AbstractUIPlugin implements IPreferenceKeys {
	// The shared instance of this plug-in.
	private static UIPlugin plugin;
	// The listener which listens to command "SAVE" and synchronize the local file with the target.
	private IExecutionListener saveListener;
	// The listener which listens to command "SAVE ALL" and synchronize the local file with the target.
	private IExecutionListener saveAllListener;
	// The shared instance of Clipboard
	private FsClipboard clipboard;

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
		return "org.eclipse.tcf.te.tcf.filesystem.ui"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		clipboard = new FsClipboard();

		// Add the two execution listeners to command "SAVE" and "SAVE ALL".
		ICommandService commandService = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
		if (commandService != null) {
			saveListener = new SaveListener();
			Command saveCmd = commandService.getCommand(IWorkbenchCommandConstants.FILE_SAVE);
			saveCmd.addExecutionListener(saveListener);
			saveAllListener = new SaveAllListener();
			Command saveAllCmd = commandService.getCommand(IWorkbenchCommandConstants.FILE_SAVE_ALL);
			saveAllCmd.addExecutionListener(saveAllListener);
		}
	}

	/**
	 * Get the shared instance of clipboard
	 */
	public static FsClipboard getClipboard() {
		return plugin.clipboard;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		// Remove the two execution listeners.
		ICommandService commandService = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
		if (commandService != null) {
			Command saveCmd = commandService.getCommand(IWorkbenchCommandConstants.FILE_SAVE);
			saveCmd.removeExecutionListener(saveListener);
			Command saveAllCmd = commandService.getCommand(IWorkbenchCommandConstants.FILE_SAVE_ALL);
			saveAllCmd.removeExecutionListener(saveAllListener);
		}
		// Ignore SWTException here, the display might be disposed already.
		if (clipboard != null) {
			try {
				clipboard.dispose();
			}
			catch (SWTException e) { /* ignored on purpose */
			}
		}
		clipboard = null;
		plugin = null;
		super.stop(context);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#initializeImageRegistry(org.eclipse.jface.resource.ImageRegistry)
	 */
	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		URL url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OBJ + "folder.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.FOLDER, ImageDescriptor.createFromURL(url));

		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OBJ + "root.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.ROOT, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OBJ + "rootdrive.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.ROOT_DRIVE, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OBJ + "synch_synch.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.COMPARE_EDITOR, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OBJ32 + "replace_confirm.png"); //$NON-NLS-1$
		registry.put(ImageConsts.REPLACE_FOLDER_CONFIRM, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OBJ32 + "delete_readonly.png"); //$NON-NLS-1$
		registry.put(ImageConsts.DELETE_READONLY_CONFIRM, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OBJ32 + "banner.png"); //$NON-NLS-1$
		registry.put(ImageConsts.BANNER_IMAGE, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OBJ + "error.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.ERROR_IMAGE, ImageDescriptor.createFromURL(url));
		url = UIPlugin.getDefault().getBundle().getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_OBJ + "refresh.gif"); //$NON-NLS-1$
		registry.put(ImageConsts.REFRESH_IMAGE, ImageDescriptor.createFromURL(url));
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

	/**
	 * If the option of "autosaving" is set to on.
	 *
	 * @return true if it is auto saving or else false.
	 */
	public static boolean isAutoSaving() {
		IPreferenceStore preferenceStore = getDefault().getPreferenceStore();
		boolean autoSaving = preferenceStore.getBoolean(PREF_AUTOSAVING);
		return autoSaving;
	}

	/**
	 * If the option of "in-place editor" is set to on.
	 *
	 * @return true if it uses in-place editor when renaming files/folders.
	 */
	public static boolean isInPlaceEditor() {
		IPreferenceStore preferenceStore = getDefault().getPreferenceStore();
		boolean inPlaceEditor = preferenceStore.getBoolean(PREF_RENAMING_IN_PLACE_EDITOR);
		return inPlaceEditor;
	}

	/**
	 * If the option of "copy permissions" is set to on.
	 *
	 * @return true if it should copy source file permissions.
	 */
	public static boolean isCopyPermission() {
		IPreferenceStore preferenceStore = getDefault().getPreferenceStore();
		boolean copyPermission = preferenceStore.getBoolean(PREF_COPY_PERMISSION);
		return copyPermission;
	}

	/**
	 * If the option of "copy ownership" is set to on.
	 *
	 * @return true if it should copy source file ownership.
	 */
	public static boolean isCopyOwnership() {
		IPreferenceStore preferenceStore = getDefault().getPreferenceStore();
		boolean copyOwnership = preferenceStore.getBoolean(PREF_COPY_OWNERSHIP);
		return copyOwnership;
	}
}
