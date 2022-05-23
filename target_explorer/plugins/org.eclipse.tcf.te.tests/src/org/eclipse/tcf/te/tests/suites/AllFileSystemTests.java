/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.suites;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.tests.tcf.filesystem.adapters.AdaptersTests;
import org.eclipse.tcf.te.tests.tcf.filesystem.controls.ControlsTests;
import org.eclipse.tcf.te.tests.tcf.filesystem.dnd.DnDTests;
import org.eclipse.tcf.te.tests.tcf.filesystem.filters.FiltersTests;
import org.eclipse.tcf.te.tests.tcf.filesystem.operations.OperationTests;
import org.eclipse.tcf.te.tests.tcf.filesystem.testers.TestersTests;
import org.eclipse.tcf.te.tests.tcf.filesystem.url.URLTests;
import org.eclipse.tcf.te.tests.tcf.filesystem.utils.UtilTests;

/**
 * Links all file system tests.
 */
public class AllFileSystemTests {

	/**
	 * Main method called if the tests are running as part of the nightly
	 * Workbench wheel. Use only the <code>junit.textui.TestRunner</code>
	 * here to execute the tests!
	 *
	 * @param args The command line arguments passed.
	 */
	public static void main (String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	/**
	 * Static method called by the several possible test runners to fetch
	 * the test(s) to run.
	 * Do not rename this method, otherwise tests will not be called anymore!
	 *
	 * @return Any object of type <code>Test</code> containing the test to run.
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite("All File System Tests"); //$NON-NLS-1$
		addTests(suite);
		return suite;
	}

	/**
	 * Adds all related tests to the given test suite.
	 *
	 * @param suite The test suite. Must not be <code>null</code>.
	 */
	public static void addTests(TestSuite suite) {
		Assert.isNotNull(suite);

		suite.addTest(OperationTests.suite());
		suite.addTest(URLTests.suite());
		suite.addTest(UtilTests.suite());
		suite.addTest(AdaptersTests.suite());
		suite.addTest(ControlsTests.suite());
		suite.addTest(FiltersTests.suite());
		suite.addTest(TestersTests.suite());
		suite.addTest(DnDTests.suite());
	}
}
