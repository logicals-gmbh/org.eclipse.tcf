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
package org.eclipse.tcf.internal.core;

import java.util.Map;

import org.eclipse.tcf.core.AbstractPeer;

/**
 * RemotePeer objects represent TCF agents that Locator service discovered on local network.
 * This includes both local host agents and remote host agents.
 * Note that "remote peer" means any peer accessible over network,
 * it does not imply the agent is running on a "remote host".
 * If an agent binds multiple network interfaces or multiple ports, it can be represented by
 * multiple RemotePeer objects - one per each network address/port combination.
 * RemotePeer objects life cycle is managed by Locator service.
 */
public class RemotePeer extends AbstractPeer {

    private long last_update_time;

    /**
     * Constructs a Remote Peer and initializes it with the given attributes
     * @param attrs attributes map used to initialize the Peer Properties
     */
    public RemotePeer(Map<String,String> attrs) {
        super(attrs);
        last_update_time = System.currentTimeMillis();
    }

    @Override
    public void updateAttributes(Map<String,String> attrs) {
        super.updateAttributes(attrs);
        last_update_time = System.currentTimeMillis();
    }

    public long getLastUpdateTime() {
        return last_update_time;
    }
}
