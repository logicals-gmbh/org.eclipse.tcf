/*******************************************************************************
 * Copyright (c) 2007, 2016 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.core;

import java.io.IOException;
import java.util.Map;

import org.eclipse.tcf.internal.core.RemotePeer;
import org.eclipse.tcf.internal.services.local.LocatorService;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.ILocator;
import org.eclipse.tcf.services.ILocator.LocatorListener;

/**
 * Abstract implementation of IPeer interface. Objects of this class are stored
 * in Locator service peer table. The class implements sending notification
 * events to Locator listeners. See TransientPeer for IPeer objects that are not
 * stored in the Locator table.
 */
public class AbstractPeer extends TransientPeer {

    private long last_heart_beat_time;

    /**
     * Constructs an AbstractPeer object using the given attributes, adds the
     * peer to the LocatorService Peer Table and sends a "peerAdded" event to
     * the TCF Channel
     *
     * @param attrs attributes maps with which to initialize the peer
     */
    public AbstractPeer(Map<String, String> attrs) {
        super(attrs);
        assert Protocol.isDispatchThread();
        String id = getID();
        assert id != null;
        Map<String, IPeer> peers = LocatorService.getLocator().getPeers();
        if (peers.get(id) instanceof RemotePeer) {
            ((RemotePeer) peers.get(id)).dispose();
        }
        assert peers.get(id) == null;
        peers.put(id, this);
        sendPeerAddedEvent();
    }

    /**
     * Removes the Peer from the Locator Service Peer table and sends a
     * "peerRemoved" event to the TCF Channel
     */
    public void dispose() {
        assert Protocol.isDispatchThread();
        String id = getID();
        assert id != null;
        Map<String, IPeer> peers = LocatorService.getLocator().getPeers();
        assert peers.get(id) == this;
        peers.remove(id);
        sendPeerRemovedEvent();
    }

    /**
     * Method called whenever a channel is terminated
     */
    void onChannelTerminated() {
        // A channel to this peer was terminated:
        // not delaying next heart beat helps client to recover much faster.
        last_heart_beat_time = 0;
    }

    /**
     * Updates Peer properties using the given attributes parameters
     * @param attrs - attributes map with which to update the peer
     */
    public void updateAttributes(Map<String, String> attrs) {
        long time = System.currentTimeMillis();
        if (!attrs.equals(ro_attrs)) {
            assert attrs.get(ATTR_ID).equals(rw_attrs.get(ATTR_ID));
            rw_attrs.clear();
            rw_attrs.putAll(attrs);
            for (LocatorListener l : LocatorService.getListeners()) {
                try {
                    l.peerChanged(this);
                }
                catch (Throwable x) {
                    Protocol.log("Unhandled exception in Locator listener", x);
                }
            }
            try {
                Object[] args = { rw_attrs };
                Protocol.sendEvent(ILocator.NAME, "peerChanged", JSON.toJSONSequence(args));
            }
            catch (IOException x) {
                Protocol.log("Locator: failed to send 'peerChanged' event", x);
            }
            last_heart_beat_time = time;
        }
        else if (last_heart_beat_time + ILocator.DATA_RETENTION_PERIOD / 4 < time) {
            for (LocatorListener l : LocatorService.getListeners()) {
                try {
                    l.peerHeartBeat(attrs.get(ATTR_ID));
                }
                catch (Throwable x) {
                    Protocol.log("Unhandled exception in Locator listener", x);
                }
            }
            try {
                Object[] args = { rw_attrs.get(ATTR_ID) };
                Protocol.sendEvent(ILocator.NAME, "peerHeartBeat", JSON.toJSONSequence(args));
            }
            catch (IOException x) {
                Protocol.log("Locator: failed to send 'peerHeartBeat' event", x);
            }
            last_heart_beat_time = time;
        }
    }

    /**
     * Sends a "peerAdded" event to the TCF Channel
     */
    private void sendPeerAddedEvent() {
        for (LocatorListener l : LocatorService.getListeners()) {
            try {
                l.peerAdded(this);
            }
            catch (Throwable x) {
                Protocol.log("Unhandled exception in Locator listener", x);
            }
        }
        try {
            Object[] args = { rw_attrs };
            Protocol.sendEvent(ILocator.NAME, "peerAdded", JSON.toJSONSequence(args));
        }
        catch (IOException x) {
            Protocol.log("Locator: failed to send 'peerAdded' event", x);
        }
        last_heart_beat_time = System.currentTimeMillis();
    }

    /**
     * Sends a "PeerRemoved" event to the TCF Channel
     */
    private void sendPeerRemovedEvent() {
        for (LocatorListener l : LocatorService.getListeners()) {
            try {
                l.peerRemoved(rw_attrs.get(ATTR_ID));
            }
            catch (Throwable x) {
                Protocol.log("Unhandled exception in Locator listener", x);
            }
        }
        try {
            Object[] args = { rw_attrs.get(ATTR_ID) };
            Protocol.sendEvent(ILocator.NAME, "peerRemoved", JSON.toJSONSequence(args));
        }
        catch (IOException x) {
            Protocol.log("Locator: failed to send 'peerRemoved' event", x);
        }
    }
}
