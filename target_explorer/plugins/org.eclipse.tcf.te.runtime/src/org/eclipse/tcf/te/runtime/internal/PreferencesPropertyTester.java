/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.internal;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.Platform;
import org.eclipse.tcf.te.runtime.activator.CoreBundleActivator;
import org.eclipse.tcf.te.runtime.preferences.ScopedEclipsePreferences;

/**
 * Preferences property tester implementation.
 */
public class PreferencesPropertyTester extends PropertyTester {

	/* (non-Javadoc)
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		// The preferences property tester is not extending a specific object type.
		// The tester ignores the given receiver object.

		if ("preference".equals(property)) { //$NON-NLS-1$
			String bundleId = CoreBundleActivator.getUniqueIdentifier();
			String key = null;

			// Search the args for bundle id, the preference type and the preference key
			for (Object candidate : args) {
				// We cannot handle arguments other than strings
				if (!(candidate instanceof String)) continue;

				String arg = (String)candidate;

				// bundleId=<id>
				if (arg.toLowerCase().startsWith("bundleid")) { //$NON-NLS-1$
					String[] tokens = arg.split("=", 2); //$NON-NLS-1$
					// Check if the given bundle id really resolves to an installed bundle
					if (tokens.length == 2 && tokens[1] != null && Platform.getBundle(tokens[1].trim()) != null) {
						bundleId = tokens[1].trim();
					}
				}

				// key=<preference key>
				if (arg.toLowerCase().startsWith("key")) { //$NON-NLS-1$
					String[] tokens = arg.split("=", 2); //$NON-NLS-1$
					// Check for the key not being empty or null
					if (tokens.length == 2 && tokens[1] != null && !"".equals(tokens[1].trim())) { //$NON-NLS-1$
						key = tokens[1].trim();
					}
				}
			}

			// Lookup the preference
			if (key != null) {
				// Check the preference within the instance and default scope
				ScopedEclipsePreferences preferences = new ScopedEclipsePreferences(bundleId);
				// If the expected value is not specified or "null", check if the preference
				// key is set or not. Return "true" if the key is not set.
				if (expectedValue == null || "null".equals(expectedValue)) return !preferences.containsKey(key); //$NON-NLS-1$

				// Always check against the string value
				return expectedValue.toString().equals(preferences.getString(key));
			}
		}

		return false;
	}

}
