/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.statushandler.internal;

import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.tcf.te.runtime.extensions.ExecutableExtension;

/**
 * Status handler binding implementation.
 */
public class StatusHandlerBinding extends ExecutableExtension {
	// The mandatory handler identifier
	private String handlerId;
	// The converted expression
	private Expression expression;

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.extensions.ExecutableExtension#doSetInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	@Override
	public void doSetInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
	    super.doSetInitializationData(config, propertyName, data);

		// Initialize the handler id field by reading the <handler> extension attribute.
		// Throws an exception if the id is empty or null.
		handlerId = config != null ? config.getAttribute("handlerId") : null; //$NON-NLS-1$
		if (handlerId == null || "".equals(handlerId.trim())) { //$NON-NLS-1$
			throw createMissingMandatoryAttributeException("handlerId", config.getContributor().getName()); //$NON-NLS-1$
		}

		// Read the sub elements of the extension
		IConfigurationElement[] children = config != null ? config.getChildren() : null;
		// The "enablement" element is the only expected one
		if (children != null && children.length > 0) {
			expression = ExpressionConverter.getDefault().perform(children[0]);
		}
	}

	/**
	 * Returns the status handler id which is associated with this binding.
	 *
	 * @return The status handler id.
	 */
	public String getHandlerId() {
		return handlerId;
	}

	/**
	 * Returns the enablement expression which is associated with this binding.
	 *
	 * @return The enablement expression or <code>null</code>.
	 */
	public Expression getEnablement() {
		return expression;
	}
}
