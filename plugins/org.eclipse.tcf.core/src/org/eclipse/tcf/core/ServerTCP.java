/*******************************************************************************
 * Copyright (c) 2008, 2013 Wind River Systems, Inc. and others.
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
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.tcf.internal.core.ServiceManager;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;

/**
 * ServerTCP is a TCP server that is listening for incoming connection requests
 * and creates TCF communication channels over TCP sockets for such requests.
 *
 * Clients may create objects of this class to become a TCF server.
 */
public class ServerTCP extends ServerSocket {

    private static class ServerPeer extends AbstractPeer {
        ServerPeer(Map<String,String> attrs) {
            super(attrs);
        }
    }

    private final String name;
    private List<ServerPeer> peers;
    private Thread thread;

    public ServerTCP(String name, int port) throws IOException {
        super(port);
        this.name = name;
        peers = new ArrayList<ServerPeer>();
        Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
        while (e.hasMoreElements()) {
            NetworkInterface f = e.nextElement();
            Enumeration<InetAddress> n = f.getInetAddresses();
            while (n.hasMoreElements()) getServerPeer(n.nextElement());
        }
        thread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        final Socket socket = accept();
                        Protocol.invokeLater(new Runnable() {
                            public void run() {
                                try {
                                    new ChannelTCP(
                                            getServerPeer(socket.getLocalAddress()),
                                            getTransientPeer(socket.getInetAddress()),
                                            socket);
                                }
                                catch (final Throwable x) {
                                    Protocol.log("TCF Server: failed to create a channel", x);
                                }
                            }
                        });
                    }
                    catch (final Throwable x) {
                        Protocol.invokeLater(new Runnable() {
                            public void run() {
                                Protocol.log("TCF Server thread aborted", x);
                            }
                        });
                        break;
                    }
                }
            }
        };
        thread.setName(name);
        thread.setDaemon(true);
        thread.start();
    }

    private IPeer getServerPeer(InetAddress addr) {
        if (addr.isAnyLocalAddress()) return getTransientPeer(addr);
        if (addr.isMulticastAddress()) return getTransientPeer(addr);
        if (addr.isLinkLocalAddress()) return getTransientPeer(addr);
        String host = addr.getHostAddress();
        for (ServerPeer p : peers) {
            if (host.equals(p.getAttributes().get(IPeer.ATTR_IP_HOST))) return p;
        }
        String port = Integer.toString(getLocalPort());
        Map<String,String> attrs = new HashMap<String,String>();
        attrs.put(IPeer.ATTR_ID, "TCP:" + host + ":" + port);
        attrs.put(IPeer.ATTR_SERVICE_MANGER_ID, ServiceManager.getID());
        attrs.put(IPeer.ATTR_AGENT_ID, Protocol.getAgentID());
        attrs.put(IPeer.ATTR_NAME, name);
        attrs.put(IPeer.ATTR_OS_NAME, System.getProperty("os.name"));
        attrs.put(IPeer.ATTR_TRANSPORT_NAME, "TCP");
        attrs.put(IPeer.ATTR_IP_HOST, host);
        attrs.put(IPeer.ATTR_IP_PORT, port);
        attrs.put(IPeer.ATTR_PROXY, "");
        ServerPeer p = new ServerPeer(attrs);
        peers.add(p);
        return p;
    }

    private IPeer getTransientPeer(InetAddress addr) {
        String host = addr.getHostAddress();
        Map<String,String> attrs = new HashMap<String,String>();
        attrs.put(IPeer.ATTR_ID, "TCP:Transient:" + host + ":" + getLocalPort());
        attrs.put(IPeer.ATTR_TRANSPORT_NAME, "TCP");
        attrs.put(IPeer.ATTR_IP_HOST, host);
        return new TransientPeer(attrs);
    }

    @Override
    public void close() throws IOException {
        if (peers != null) {
            for (ServerPeer s : peers) s.dispose();
            peers = null;
        }
        super.close();
        if (thread != null) {
            try {
                thread.join();
                thread = null;
            }
            catch (InterruptedException e) {
                throw new InterruptedIOException();
            }
        }
    }
}
