/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.async;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.runtime.callback.AsyncCallbackCollector;
import org.eclipse.ui.PlatformUI;

/**
 * Asynchronous callback collector callback invocation delegate implementation.
 * <p>
 * The delegate invokes callbacks within the UI thread.
 */
public class UICallbackInvocationDelegate implements AsyncCallbackCollector.ICallbackInvocationDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.core.async.AsyncCallbackCollector.ICallbackInvocationDelegate#invoke(java.lang.Runnable)
	 */
	@Override
	public void invoke(Runnable runnable) {
		Assert.isNotNull(runnable);
		if (PlatformUI.getWorkbench() != null && PlatformUI.getWorkbench().getDisplay() != null) {
			try {
				PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
	        }
	        catch (Exception e) {
	            // if display is disposed, silently ignore.
	        }
		}
	}
}
