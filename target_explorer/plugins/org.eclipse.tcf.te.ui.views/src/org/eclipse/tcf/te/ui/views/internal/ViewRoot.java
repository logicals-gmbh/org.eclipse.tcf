/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.internal;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.tcf.te.ui.views.interfaces.IRoot;

/**
 * View root node implementation
 */
public class ViewRoot extends PlatformObject implements IRoot {

	/*
	 * Thread save singleton instance creation.
	 */
	private static class LazyInstance {
		public static ViewRoot instance = new ViewRoot();
	}

	/**
	 * Returns the singleton view root instance.
	 */
	public static ViewRoot getInstance() {
		return LazyInstance.instance;
	}

	/**
	 * Constructor.
	 */
	/* default */ ViewRoot() {
	}
}