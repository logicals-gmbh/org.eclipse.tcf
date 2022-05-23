/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.core.interfaces;

/**
 * Launch configuration type id declarations.
 * <p>
 * Keep in sync with the launch configuration type id's used within the plugin.xml.
 */
public interface ILaunchTypes {

	/**
	 * Launch configuration type id: Remote Application
	 */
	public final String REMOTE_APPLICATION = "org.eclipse.tcf.te.tcf.launch.type.remote.app"; //$NON-NLS-1$

	/**
	 * Launch configuration type id: Attach
	 */
	public final String ATTACH = "org.eclipse.tcf.te.tcf.launch.type.attach"; //$NON-NLS-1$
}
