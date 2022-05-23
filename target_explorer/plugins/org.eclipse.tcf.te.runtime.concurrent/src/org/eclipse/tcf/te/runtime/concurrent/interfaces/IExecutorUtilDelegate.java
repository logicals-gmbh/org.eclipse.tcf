/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.concurrent.interfaces;

import org.eclipse.tcf.te.runtime.interfaces.extensions.IExecutableExtension;

/**
 * Executor utility delegate interface declaration.
 */
public interface IExecutorUtilDelegate extends IExecutableExtension {

	/**
	 * Returns if or if not the current thread is an executor thread handled by
	 * this executor utility wait and dispatch delegate.
	 *
	 * @return <code>True</code> if the current thread is handled,
	 *         <code>false</code> otherwise.
	 */
	public boolean isHandledExecutorThread();

	/**
	 * Reads an event from the handled executors event queue, dispatches it
	 * appropriately, and returns <code>true</code> if there is potentially more
	 * work to do, or <code>false</code> if the caller can sleep until another
	 * event is placed on the event queue.
	 *
	 * @return <code>True</code> if there is potentially more work to do,
	 *         <code>false</code> otherwise.
	 */
	public boolean readAndDispatch();
}
