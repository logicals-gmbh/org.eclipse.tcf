/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.stepper.nls;

import org.eclipse.osgi.util.NLS;

/**
 * Stepper Runtime plugin externalized strings management.
 */
public class Messages extends NLS {

	// The plug-in resource bundle name
	private static final String BUNDLE_NAME = "org.eclipse.tcf.te.runtime.stepper.nls.Messages"; //$NON-NLS-1$

	/**
	 * Static constructor.
	 */
	static {
		// Load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	// **** Declare externalized string id's down here *****

	public static String AbstractStep_error_missingRequiredAttribute;
	public static String AbstractStep_warning_stepFinishedWithWarnings;

	public static String Stepper_error_stepGroup;
	public static String Stepper_error_step;
	public static String Stepper_error_referencedBaseGroup;
	public static String Stepper_error_referencedStepOrGroup;
	public static String Stepper_error_requiredStepOrGroup;
	public static String Stepper_error_requiredStep;
	public static String Stepper_error_initializeNotCalled;
	public static String Stepper_error_missingStepGroupId;
	public static String Stepper_error_missingStepGroup;
	public static String Stepper_error_missingSteps;
	public static String Stepper_multiStatus_finishedWithWarnings;
	public static String Stepper_multiStatus_finishedWithErrors;
	public static String Stepper_error_missingRequiredStep;
	public static String Stepper_error_requiredStepNotExecuted;

	public static String StepGroup_error_missingBaseStepGroup;
	public static String StepGroup_error_missingReferencedStep;
	public static String StepGroup_error_missingRequiredStep;
	public static String StepGroup_error_invalidRequiredStep;
	public static String StepGroup_error_multipleSingletonOccurrences;
	public static String StepGroup_error_step;
	public static String StepGroup_error_stepGroup;
	public static String StepGroup_error_requiredStep;
	public static String StepGroup_error_referencedBaseGroup;
	public static String StepGroup_error_referencedStepOrGroup;
	public static String StepGroup_error_requiredStepOrGroup;

	public static String StepExecutor_info_stepFailed;
	public static String StepExecutor_warning_stepFailed;
	public static String StepExecutor_error_stepFailed;
	public static String StepExecutor_stepFailed_debugInfo;
	public static String StepExecutor_warning_rollbackTimeout;

}
