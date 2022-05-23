/*******************************************************************************
 * Adapted from org.eclipse.rse.internal.services.ssh.ISshService
 * Copyright (c) 2006, 2011 Wind River Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Martin Oberhuber (Wind River) - initial API and implementation
 * Martin Oberhuber (Wind River) - [170910] Adopt RSE ITerminalService API for SSH
 * Intel Corporation             - [329654] Make all sub services operate against TCF connector service
 *******************************************************************************/
package org.eclipse.tcf.internal.rse;

/**
 * Markup Interface for services using the TCFConnectorService.
 *
 * By implementing this interface, services can be recognized
 * as operating against an TCFConnectorService. The interface
 * is used as the key in a table for looking up the connector
 * service when needed.
 */
public interface ITCFService {
}
