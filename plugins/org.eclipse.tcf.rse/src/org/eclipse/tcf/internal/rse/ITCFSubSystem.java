/*******************************************************************************
 * Copyright (c) 2007, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.rse;

import org.eclipse.rse.core.subsystems.ISubSystem;

/**
 * Subsystem can implement this interface to indicate that it can share TCF connection with
 * other subsystems on same host.
 */
public interface ITCFSubSystem extends ISubSystem {

}
