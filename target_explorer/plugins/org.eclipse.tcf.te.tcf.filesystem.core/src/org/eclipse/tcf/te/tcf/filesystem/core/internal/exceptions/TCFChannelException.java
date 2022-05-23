/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * William Chen (Wind River)- [345552] Edit the remote files with a proper editor
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.exceptions;

/**
 * TCF channel exception.
 */
public class TCFChannelException extends TCFException {
	private static final long serialVersionUID = 7414816212710485160L;

	/**
	 * Constructor.
	 *
	 * @param severity the severity; one of <code>OK</code>, <code>ERROR</code>, 
	 * <code>INFO</code>, <code>WARNING</code>,  or <code>CANCEL</code>
	 * @param message
	 *            The exception detail message or <code>null</code>.
	 */
	public TCFChannelException(int severity, String message) {
		super(severity, message);
	}

	/**
	 * Constructor.
	 *
	 * @param severity the severity; one of <code>OK</code>, <code>ERROR</code>, 
	 * <code>INFO</code>, <code>WARNING</code>,  or <code>CANCEL</code>
	 * @param message
	 *            The exception detail message or <code>null</code>.
	 * @param cause
	 *            The exception cause or <code>null</code>.
	 */
	public TCFChannelException(int severity, String message, Throwable cause){
		super(severity, message, cause);
	}
}
