/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.stepper.interfaces;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tcf.te.runtime.interfaces.IConditionTester;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;


/**
 * A stepper.
 * <p>
 * Stepper are executing a set of steps and/or step groups for the given context(s). The stepper is
 * responsible for handling any exceptions which occurred during step execution.
 * <p>
 * <b>Note:</b> Stepper are synchronous where steps are asynchronous.
 * <p>
 * Stepper must run in worker threads.
 */
public interface IStepper {

	public static final String ID_TYPE_STEP_CONTEXT_ID = "StepContext"; //$NON-NLS-1$
	public static final String ID_TYPE_STEPPER_ID = "Stepper"; //$NON-NLS-1$
	public static final String ID_TYPE_STEP_GROUP_ID = "StepGroup"; //$NON-NLS-1$
	public static final String ID_TYPE_STEP_ID = "Step"; //$NON-NLS-1$

	/**
	 * Condition Tester to test for finished execution of the associated stepper.
	 */
	public static class ExecutionFinishedConditionTester implements IConditionTester {
		private final IStepper stepper;

		/**
		 * Constructor.
		 *
		 * @param stepper The stepper. Must not be <code>null</code>.
		 */
		public ExecutionFinishedConditionTester(IStepper stepper) {
			Assert.isNotNull(stepper);
			this.stepper = stepper;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.runtime.interfaces.IConditionTester#cleanup()
		 */
		@Override
		public void cleanup() {
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.runtime.interfaces.IConditionTester#isConditionFulfilled()
		 */
		@Override
		public boolean isConditionFulfilled() {
			return stepper.isFinished();
		}
	}

	/**
	 * Returns the unique id of the extension. The returned
	 * id must be never <code>null</code> or an empty string.
	 *
	 * @return The unique id.
	 */
	public String getId();

	/**
	 * Returns the label or UI name of the extension.
	 *
	 * @return The label or UI name. An empty string if not set.
	 */
	public String getLabel();

	/**
	 * Initialize the stepper for a run. This method must be called before <i><code>execute()</code>
	 * </i>. Once the stepper finished the execution, the initialization is reseted and must be
	 * renewed before <i><code>execute()</code></i> can be called again.
	 *
	 * @param context The step context. Must not be <code>null</code>.
	 * @param stepGroupId The step group to execute. Must not be <code>null</code>.
	 * @param data The data to transfer data between steps. Must not be <code>null</code>.
	 *             Can also be used to get data from the launch after it has finished.
	 * @param monitor The progress monitor or <code>null</code>.
	 *
	 * @throws IllegalStateException If called if the stepper is in initialized state already.
	 */
	public void initialize(IStepContext context, String stepGroupId, IPropertiesContainer data, IProgressMonitor monitor) throws IllegalStateException;

	/**
	 * Returns if or if not the stepper got initialized for a new run.
	 * <p>
	 * The <i><code>execute()</code></i> method cannot be called if the stepper is not correctly
	 * initialized for each run. The initialized state can be set only by calling the <i>
	 * <code>initialize(...)</code></i> method. <i> <code>cleanup()</code></i> will reset the
	 * initialized state back to uninitialized.
	 *
	 * @return <code>True</code> if initialized, <code>false</code> otherwise.
	 */
	public boolean isInitialized();

	/**
	 * Executes the configured steps. The method is synchronous and must return only if all steps
	 * finished or an exception occurred.
	 * <p>
	 * Steps are assumed to be asynchronous. The stepper implementor must wait for callback(s) to be
	 * invoked by the step implementor(s) before the sequence can continue.
	 * <p>
	 * <b>Note:</b> Waiting for the step callback must not block the UI thread.
	 *
	 * @throws CoreException In case the execution fails or is canceled.
	 */
	public void execute() throws CoreException;

	/**
	 * Returns if or if not the stepper finished the execution.
	 *
	 * @return <code>True</code> if the execution is finished, <code>false</code> otherwise.
	 */
	public boolean isFinished();

	/**
	 * Cleanup and reset the stepper into a defined state.
	 */
	public void cleanup();
}
