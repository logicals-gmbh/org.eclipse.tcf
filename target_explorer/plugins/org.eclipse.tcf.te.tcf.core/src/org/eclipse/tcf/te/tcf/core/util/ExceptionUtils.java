/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.util;

import java.util.concurrent.ExecutionException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.util.TCFTask;

/**
 * Exception utilities helper implementations.
 */
public final class ExceptionUtils {

	/**
	 * The {@link TCFTask} implementation is wrapping the error cause into an
	 * {@link ExecutionException} with the default error message &quot;TCF
	 * task aborted&quot;. As this message is not very informative to the user,
	 * unpack the given exception to reveal the real error cause to the clients.
	 *
	 * @param e The source exception. Must not be <code>null</code>.
	 * returns Exception The real error cause.
	 */
	public static Exception checkAndUnwrapException(Exception e) {
		Assert.isNotNull(e);

		// If the incoming exception is a ExecutionException and has set
		// the default error message text, get the embedded cause.
		if (e instanceof ExecutionException && "TCF task aborted".equals(e.getMessage())) { //$NON-NLS-1$
			// Get the cause
			if (e.getCause() instanceof Exception) return (Exception)e.getCause();
		}

		return e;
	}
}
