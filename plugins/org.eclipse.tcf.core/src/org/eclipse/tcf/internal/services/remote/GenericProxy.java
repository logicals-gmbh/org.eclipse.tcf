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
package org.eclipse.tcf.internal.services.remote;

import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IService;

/**
 * Objects of GenericProxy class represent remote services, which don't
 * have a proxy class defined for them.
 * Clients still can use such services, but framework will not provide
 * service specific utility methods for message formatting and parsing.
 */
public class GenericProxy implements IService {

    private final IChannel channel;
    private final String name;

    public GenericProxy(IChannel channel, String name) {
        this.channel = channel;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public IChannel getChannel() {
        return channel;
    }
}
