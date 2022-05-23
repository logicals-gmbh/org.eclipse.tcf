/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.launch.core.nls;

import org.eclipse.osgi.util.NLS;

/**
 * Launch Core Plug-in externalized strings management.
 */
public class Messages extends NLS {

	// The plug-in resource bundle name
	private static final String BUNDLE_NAME = "org.eclipse.tcf.te.launch.core.nls.Messages"; //$NON-NLS-1$

	/**
	 * Static constructor.
	 */
	static {
		// Load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	// **** Declare externalized string id's down here *****

	public static String DefaultLaunchManagerDelegate_defaultLaunchName;

	public static String LaunchManager_error_invalidExtensionPoint;
	public static String LaunchManager_error_noLaunchConfigType;
	public static String LaunchManager_error_failedToCreateConfig;
	public static String LaunchManager_error_failedToUpdateConfig;
	public static String LaunchManager_error_deleteLaunchConfig;

	public static String LaunchConfigurationDelegate_error_failedToGetStepper;
	public static String LaunchConfigurationDelegate_error_failedToCloneStepper;
	public static String LaunchConfigurationDelegate_error_inaccessibleReferencedProject;

	public static String AbstractLaunchConfigurationDelegate_scoped_incremental_build;

	public static String ReferencedProjectItemValidator_missingProject;
	public static String ReferencedProjectItemValidator_notExistingProject;
	public static String ReferencedProjectItemValidator_closedProject;
}
