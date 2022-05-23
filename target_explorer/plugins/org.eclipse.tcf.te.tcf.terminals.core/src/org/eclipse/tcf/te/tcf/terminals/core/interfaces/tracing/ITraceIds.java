/*******************************************************************************
 * Copyright (c) 2011 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.terminals.core.interfaces.tracing;

/**
 * Core plug-in trace slot identifiers.
 */
public interface ITraceIds {

	/**
	 * If activated, tracing information about the remote terminals launcher is printed out.
	 */
	public static final String TRACE_TERMINALS_LAUNCHER = "trace/terminalsLauncher"; //$NON-NLS-1$

	/**
	 * If activated, tracing information about the remote terminals listener is printed out.
	 */
	public static final String TRACE_TERMINALS_LISTENER = "trace/terminalsListener"; //$NON-NLS-1$

	/**
	 * If activated, tracing information about the remote terminals streams listener is printed out.
	 */
	public static final String TRACE_STREAMS_LISTENER = "trace/streamsListener"; //$NON-NLS-1$
}
