/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.launch.ui.internal;

/**
 * Image registry constants.
 */
public interface ImageConsts {

	// ***** The directory structure constants *****

	/**
	 * The root directory where to load the images from, relative to
	 * the bundle directory.
	 */
	public final static String IMAGE_DIR_ROOT = "icons/"; //$NON-NLS-1$

	/**
	 * The directory where to load colored local toolbar images from,
	 * relative to the image root directory.
	 */
	public final static String  IMAGE_DIR_CLCL = "clcl16/"; //$NON-NLS-1$

	/**
	 * The directory where to load disabled local toolbar images from,
	 * relative to the image root directory.
	 */
	public final static String  IMAGE_DIR_DLCL = "dlcl16/"; //$NON-NLS-1$

	/**
	 * The directory where to load enabled local toolbar images from,
	 * relative to the image root directory.
	 */
	public final static String  IMAGE_DIR_ELCL = "elcl16/"; //$NON-NLS-1$

	/**
	 * The directory where to load model object images from, relative to the image root directory.
	 */
	public final static String IMAGE_DIR_OBJ = "obj16/"; //$NON-NLS-1$

	/**
	 * The directory where to load object overlay images from,
	 * relative to the image root directory.
	 */
	public final static String  IMAGE_DIR_OVR = "ovr16/"; //$NON-NLS-1$


	// ***** The image constants *****

	/**
	 * The key to access the launches tree root image.
	 */
	public static final String OBJ_Launches_Root = "OBJ_Launches_Root"; //$NON-NLS-1$

	/**
	 * The key to access the target object red X overlay image.
	 */
	public static final String RED_X_OVR = "RedXOverlay"; //$NON-NLS-1$
}
