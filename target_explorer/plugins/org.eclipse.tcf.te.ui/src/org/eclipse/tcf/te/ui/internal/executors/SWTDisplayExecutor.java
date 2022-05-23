/*******************************************************************************
 * Copyright (c) 2011 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.internal.executors;

import org.eclipse.swt.widgets.Display;
import org.eclipse.tcf.te.ui.activator.UIPlugin;
import org.eclipse.tcf.te.runtime.concurrent.interfaces.IExecutor;
import org.eclipse.tcf.te.runtime.concurrent.interfaces.INestableExecutor;
import org.eclipse.tcf.te.runtime.concurrent.interfaces.ISingleThreadedExecutor;
import org.eclipse.tcf.te.runtime.extensions.ExecutableExtension;
import org.eclipse.ui.PlatformUI;

/**
 * SWT display executor implementation utilizing the platform display.
 */
public class SWTDisplayExecutor extends ExecutableExtension implements IExecutor, ISingleThreadedExecutor, INestableExecutor {

	/* (non-Javadoc)
	 * @see java.util.concurrent.Executor#execute(java.lang.Runnable)
	 */
	@Override
	public void execute(Runnable command) {
		// Try the platform display first
		if (PlatformUI.isWorkbenchRunning()
				&& PlatformUI.getWorkbench() != null && PlatformUI.getWorkbench().getDisplay() != null
				&& !PlatformUI.getWorkbench().getDisplay().isDisposed()) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(command);
		} else {
			// Fallback to the display associated with the current thread
			Display display = Display.findDisplay(Thread.currentThread());
			// If there is a display associated with the current thread,
			// execute the runnable using that display instance.
			if (display != null && !display.isDisposed()) {
				display.asyncExec(command);
			} else {
				// There is no display to execute the runnable at.
				// Drop execution and write a trace message if enabled
				UIPlugin.getTraceHandler().trace("DROPPED display command invocation. No display instance found.!", 1, this); //$NON-NLS-1$
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.concurrent.interfaces.ISingleThreadedExecutor#isExecutorThread()
	 */
	@Override
	public boolean isExecutorThread() {
		return isExecutorThread(Thread.currentThread());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.concurrent.interfaces.ISingleThreadedExecutor#isExecutorThread(java.lang.Thread)
	 */
	@Override
	public boolean isExecutorThread(Thread thread) {
		if (thread != null) {
			// Try the platform display first
			if (PlatformUI.isWorkbenchRunning()
					&& PlatformUI.getWorkbench() != null && PlatformUI.getWorkbench().getDisplay() != null
					&& !PlatformUI.getWorkbench().getDisplay().isDisposed()) {
				return thread.equals(PlatformUI.getWorkbench().getDisplay().getThread());
			}

			// Fallback to the display associated with the current thread
			Display display = Display.findDisplay(thread);
			if (display != null && !display.isDisposed()) {
				return thread.equals(display.getThread());
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.concurrent.interfaces.INestableExecutor#getMaxDepth()
	 */
	@Override
	public int getMaxDepth() {
		return 1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.concurrent.interfaces.INestableExecutor#readAndExecute()
	 */
	@Override
	public boolean readAndExecute() {
		// Try the platform display first
		if (PlatformUI.isWorkbenchRunning()
				&& PlatformUI.getWorkbench() != null && PlatformUI.getWorkbench().getDisplay() != null
				&& !PlatformUI.getWorkbench().getDisplay().isDisposed()) {
			return PlatformUI.getWorkbench().getDisplay().readAndDispatch();
		}

		// Fallback to the display associated with the current thread
		Display display = Display.getCurrent();
		if (display != null && !display.isDisposed()) {
			return display.readAndDispatch();
		}
		return false;
	}
}
