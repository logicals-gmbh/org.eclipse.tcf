/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.interfaces;

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
     * The directory where to load wizard banner images from,
     * relative to the image root directory.
     */
    public final static String IMAGE_DIR_WIZBAN = "wizban/"; //$NON-NLS-1$

    /**
     * The directory where to load object overlay images from,
     * relative to the image root directory.
     */
    public final static String  IMAGE_DIR_OVR = "ovr16/"; //$NON-NLS-1$

	/**
	 * The directory where to load model object images from, relative to the image root directory.
	 */
	public final static String IMAGE_DIR_OBJ = "obj16/"; //$NON-NLS-1$

    // ***** The image constants *****

    /**
     * The key to access the New target wizard banner image.
     */
    public static final String NEW_TARGET_WIZARD = "NewTargetWizard"; //$NON-NLS-1$

    /**
     * The key to access the New target wizard image (enabled).
     */
    public static final String  NEW_TARGET_WIZARD_ENABLED = "NewTargetWizard_enabled"; //$NON-NLS-1$

    /**
     * The key to access the New target wizard image (disabled).
     */
    public static final String  NEW_TARGET_WIZARD_DISABLED = "NewTargetWizard_disabled"; //$NON-NLS-1$

    /**
     * The key to access the viewer filter configuration image (enabled).
     */
    public static final String  VIEWER_FILTER_CONFIG_ENABLED = "ViewerFilterConfig_enabled"; //$NON-NLS-1$

    /**
     * The key to access the viewer filter configuration image (enabled).
     */
    public static final String  VIEWER_COLLAPSE_ALL = "ViewerCollapseAll_enabled"; //$NON-NLS-1$

    /**
     * The key to access the viewer filter configuration image (disabled).
     */
    public static final String  VIEWER_FILTER_CONFIG_DISABLED = "ViewerFilterConfig_disabled"; //$NON-NLS-1$

    /**
     * The key to access the target object gold overlay image.
     */
    public static final String GOLD_OVR = "GoldOverlay"; //$NON-NLS-1$

    /**
     * The key to access the target object green overlay image.
     */
    public static final String GREEN_OVR = "GreenOverlay"; //$NON-NLS-1$

    /**
     * The key to access the target object grey overlay image.
     */
    public static final String GREY_OVR = "GreyOverlay"; //$NON-NLS-1$

    /**
     * The key to access the target object red overlay image.
     */
    public static final String RED_OVR = "RedOverlay"; //$NON-NLS-1$

    /**
     * The key to access the filtering decoration image.
     */
    public static final String FILTERING_OVR = "FilteringOverlay"; //$NON-NLS-1$

    /**
     * The key to access the target object red X overlay image.
     */
    public static final String RED_X_OVR = "RedXOverlay"; //$NON-NLS-1$

    /**
     * The key to access the target object busy action overlay image.
     */
    public static final String BUSY_OVR = "BusyOverlay"; //$NON-NLS-1$

	/**
	 * The key to access the connect action image.
	 */
	public static final String ACTION_CONNECT = "ActionConnect"; //$NON-NLS-1$
}
