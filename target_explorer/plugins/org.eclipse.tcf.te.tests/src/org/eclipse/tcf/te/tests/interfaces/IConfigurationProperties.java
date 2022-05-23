/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.interfaces;

import org.eclipse.tcf.te.tests.activator.UIPlugin;

/**
 * Public test configuration property id's.
 */
public interface IConfigurationProperties {

	/**
	 * If set to <code>true</code>, the test framework will maximize the
	 * Target Explorer tree view before starting the test.
	 * <p>
	 * Default value is <b><code>false</code></b>.
	 */
	public static final String MAXIMIZE_VIEW = UIPlugin.getUniqueIdentifier() + ".maximizeView"; //$NON-NLS-1$

	/**
	 * Set to the perspective id to switch to before starting the test.
	 * <p>
	 * Default value is <b><code>org.eclipse.tcf.te.ui.perspective</code></b>.
	 */
	public static final String TARGET_PERSPECTIVE = UIPlugin.getUniqueIdentifier() + ".targetPerspective"; //$NON-NLS-1$

	/**
	 * Set to the view id to open before starting the test.
	 * <p>
	 * Default value is <b><code>org.eclipse.tcf.te.ui.views.View</code></b>.
	 */
	public static final String TARGET_VIEW = UIPlugin.getUniqueIdentifier() + ".targetView"; //$NON-NLS-1$
}
