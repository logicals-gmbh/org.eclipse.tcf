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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.tcf.core.TransientPeer;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;

/**
 * LocalPeer object represents local end-point of TCF communication channel.
 * There should be exactly one such object in a TCF agent.
 * The object can be used to open a loop-back communication channel that allows
 * the agent to access its own services same way as remote services.
 * Note that "local" here is relative to the agent, and not same as in "local host".
 */
public class LocalPeer extends TransientPeer {

    private static Map<String,String> createAttributes() {
        Map<String, String> attrs = new HashMap<String, String>();
        attrs.put(ATTR_ID, "TCFLocal");
        attrs.put(IPeer.ATTR_SERVICE_MANGER_ID, ServiceManager.getID());
        attrs.put(IPeer.ATTR_AGENT_ID, Protocol.getAgentID());
        attrs.put(ATTR_NAME, "Local Peer");
        attrs.put(ATTR_OS_NAME, System.getProperty("os.name"));
        attrs.put(ATTR_TRANSPORT_NAME, "Loop");
        return attrs;
    }

    public LocalPeer() {
        super(createAttributes());
    }
}
