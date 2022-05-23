/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.core.nls;

import org.eclipse.osgi.util.NLS;

/**
 * TCF Launch Core Plug-in externalized strings management.
 */
public class Messages extends NLS {

	// The plug-in resource bundle name
	private static final String BUNDLE_NAME = "org.eclipse.tcf.te.tcf.launch.core.nls.Messages"; //$NON-NLS-1$

	/**
	 * Static constructor.
	 */
	static {
		// Load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	// **** Declare externalized string id's down here *****

	public static String FileTransferItemValidator_missingFile;
	public static String FileTransferItemValidator_missingFileOrDirectory;
	public static String FileTransferItemValidator_notExistingFile;
	public static String FileTransferItemValidator_notExistingFileOrDirectory;
	public static String FileTransferItemValidator_invalidFile;
	public static String FileTransferItemValidator_invalidFileOrDirectory;

	public static String SetPathMapStep_error_missingChannel;
	public static String SetPathMapStep_error_missingLaunchConfig;
}
