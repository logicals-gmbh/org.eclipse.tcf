/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * William Chen (Wind River)- [345552] Edit the remote files with a proper editor
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.testers;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.tcf.te.tcf.filesystem.ui.activator.UIPlugin;

/**
 * Provide a tester to test if the current auto saving mode is on or off.
 *
 */
public class CachePropertyTester extends PropertyTester {
	/**
	 * Create a cache property tester.
	 */
	public CachePropertyTester() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if(property.equals("isAutoSavingOn")){ //$NON-NLS-1$
			return UIPlugin.isAutoSaving();
		}
		return false;
	}

}
