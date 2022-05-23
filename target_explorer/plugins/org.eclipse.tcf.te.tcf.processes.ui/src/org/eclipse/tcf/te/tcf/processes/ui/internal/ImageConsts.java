/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.internal;

/**
 * Image registry constants.
 */
public interface ImageConsts {
	// ***** The directory structure constants *****

	/**
	 * The root directory where to load the images from, relative to the bundle directory.
	 */
	public final static String IMAGE_DIR_ROOT = "icons/"; //$NON-NLS-1$

	/**
	 * The directory where to load model object images from, relative to the image root directory.
	 */
	public final static String IMAGE_DIR_OBJ = "obj16/"; //$NON-NLS-1$

	// ***** The image constants *****

	public static final String OBJ_Process = "OBJ_Process"; //$NON-NLS-1$

	public static final String OBJ_Process_Root = "OBJ_Process_Root"; //$NON-NLS-1$

	public static final String OBJ_Thread = "OBJ_Thread"; //$NON-NLS-1$

	public static final String PM_POLLING = "OBJ_Process_Polling"; //$NON-NLS-1$

	public static final String ATTACH = "OBJ_Attach"; //$NON-NLS-1$
	public static final String DETACH = "OBJ_Detach"; //$NON-NLS-1$
}
