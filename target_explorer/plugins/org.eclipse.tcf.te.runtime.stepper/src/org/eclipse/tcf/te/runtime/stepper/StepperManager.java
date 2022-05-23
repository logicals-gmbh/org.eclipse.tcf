/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.stepper;

import org.eclipse.tcf.te.runtime.stepper.extensions.manager.StepExtensionPointManager;
import org.eclipse.tcf.te.runtime.stepper.extensions.manager.StepGroupExtensionPointManager;

/**
 * Central manager providing access to the stepper, steps and step groups
 * contributed via extension points.
 */
public final class StepperManager {
	// References to the extension point managers
	private final StepExtensionPointManager stepExtManager = new StepExtensionPointManager();
	private final StepGroupExtensionPointManager stepGroupExtManager = new StepGroupExtensionPointManager();

	/*
	 * Thread save singleton instance creation.
	 */
	private static class LazyInstance {
		public static StepperManager instance = new StepperManager();
	}

	/**
	 * Constructor.
	 */
	StepperManager() {
		super();
	}

	/**
	 * Returns the singleton instance of the manager.
	 */
	public static StepperManager getInstance() {
		return LazyInstance.instance;
	}

	/**
	 * Returns the step extension point manager instance.
	 *
	 * @return The step extension point manager instance.
	 */
	public StepExtensionPointManager getStepExtManager() {
		return stepExtManager;
	}

	/**
	 * Returns the step group extension point manager instance.
	 *
	 * @return The step group extension point manager instance.
	 */
	public StepGroupExtensionPointManager getStepGroupExtManager() {
		return stepGroupExtManager;
	}
}
