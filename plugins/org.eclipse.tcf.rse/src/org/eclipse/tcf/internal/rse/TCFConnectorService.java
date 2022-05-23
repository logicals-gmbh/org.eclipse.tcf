/*******************************************************************************
 * Copyright (c) 2007, 2013 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     Martin Oberhuber (Wind River) - [269682] Get port from RSE Property
 *     Uwe Stieber      (Wind River) - [271227] Fix compiler warnings in org.eclipse.tcf.rse
 *     Anna Dushistova  (MontaVista) - [285373] TCFConnectorService should send CommunicationsEvent.BEFORE_CONNECT and CommunicationsEvent.BEFORE_DISCONNECT
 *     Liping Ke        (Intel Corp.)- [326490] Add authentication to the TCF Connector Service and attach stream subs/unsubs method
 *     Jeff Johnston    (RedHat)     - [350752] TCFConnectorService doesn't recognize connections with SSL transport
 *******************************************************************************/
package org.eclipse.tcf.internal.rse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.model.IPropertySet;
import org.eclipse.rse.core.model.PropertyType;
import org.eclipse.rse.core.model.SystemSignonInformation;
import org.eclipse.rse.core.subsystems.CommunicationsEvent;
import org.eclipse.rse.services.files.RemoteFileException;
import org.eclipse.rse.ui.subsystems.StandardConnectorService;
import org.eclipse.tcf.core.TransientPeer;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.IService;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.ILocator;
import org.eclipse.tcf.services.IStreams;
import org.eclipse.tcf.services.ISysMonitor;
import org.eclipse.tcf.services.ITerminals;


public class TCFConnectorService extends StandardConnectorService implements ITCFSessionProvider{

    public static final String PROPERTY_SET_NAME = "TCF Connection Settings"; //$NON-NLS-1$
    public static final String PROPERTY_TRANSPORT_NAME = "Transport.Name"; //$NON-NLS-1$
    public static final String PROPERTY_LOGIN_REQUIRED = "Login.Required"; //$NON-NLS-1$
    public static final String PROPERTY_PWD_REQUIRED="Pwd.Required"; //$NON-NLS-1$
    public static final String PROPERTY_LOGIN_PROMPT = "Login.Prompt"; //$NON-NLS-1$
    public static final String PROPERTY_PASSWORD_PROMPT = "Password.Prompt"; //$NON-NLS-1$
    public static final String PROPERTY_COMMAND_PROMPT = "Command.Prompt"; //$NON-NLS-1$

    private IChannel channel;
    private Throwable channel_error;
    private final List<Runnable> wait_list = new ArrayList<Runnable>();

    private boolean poll_timer_started;

    private boolean streams_subscribed = false;
    private boolean streams_connecting = false;
    private final HashSet<String> stream_ids = new HashSet<String>();

    /* subscribe the stream service on this TCP connection */
    private final IStreams.StreamsListener streams_listener = new IStreams.StreamsListener() {
        public void created(String stream_type, String stream_id, String context_id) {
            if (streams_connecting) {
                stream_ids.add(stream_id);
            }
            else {
                getService(IStreams.class).disconnect(stream_id, new IStreams.DoneDisconnect() {
                    public void doneDisconnect(IToken token, Exception error) {
                        if (error != null) channel.terminate(error);
                    }
                });
            }
        }

        public void disposed(String stream_type, String stream_id) {
            stream_ids.remove(stream_id);
        }
    };


    public TCFConnectorService(IHost host, int port) {
        super(Messages.TCFConnectorService_Name, Messages.TCFConnectorService_Description, host, port);
        getTCFPropertySet();
    }

    public IPropertySet getTCFPropertySet() {
        IPropertySet tcfSet = getPropertySet(PROPERTY_SET_NAME);
        if (tcfSet == null) {
            tcfSet = createPropertySet(PROPERTY_SET_NAME, Messages.PropertySet_Description);
            //add default values if not set
            tcfSet.addProperty(PROPERTY_TRANSPORT_NAME, "TCP", PropertyType.getEnumPropertyType(new String[] {"TCP", "SSL"}));  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
            tcfSet.addProperty(PROPERTY_LOGIN_REQUIRED, "false", PropertyType.getEnumPropertyType(new String[] {"true", "false"}));  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
            tcfSet.addProperty(PROPERTY_PWD_REQUIRED, "false", PropertyType.getEnumPropertyType(new String[] {"true", "false"}));  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
            tcfSet.addProperty(PROPERTY_LOGIN_PROMPT, "ogin: ", PropertyType.getStringPropertyType()); //$NON-NLS-1$
            tcfSet.addProperty(PROPERTY_PASSWORD_PROMPT, "assword: ", PropertyType.getStringPropertyType()); //$NON-NLS-1$
            tcfSet.addProperty(PROPERTY_COMMAND_PROMPT, "#", PropertyType.getStringPropertyType()); //$NON-NLS-1$
        }
        return tcfSet;
    }

    /**
     * @return true if the associated connector service requires a password.
     */
    public final boolean requiresPassword() {
        return false;
    }

    @Override
    protected void internalConnect(final IProgressMonitor monitor) throws Exception {
        assert !Protocol.isDispatchThread();
        final Exception[] res = new Exception[1];
        // Fire comm event to signal state about to change
        fireCommunicationsEvent(CommunicationsEvent.BEFORE_CONNECT);
        monitor.beginTask("Connecting " + getHostName(), 1); //$NON-NLS-1$
        synchronized (res) {
            Protocol.invokeLater(new Runnable() {
                public void run() {
                    try {
                        if (!connectTCFChannel(res, monitor))
                            add_to_wait_list(this);
                    }
                    catch (Throwable x) {
                        synchronized (res) {
                            if (x instanceof Exception) res[0] = (Exception)x;
                            else res[0] = new Exception(x);
                            res.notify();
                        }
                    }
                }
            });
            res.wait();
        }
        if (res[0] != null) throw res[0];
        monitor.done();
    }

    @Override
    protected void internalDisconnect(final IProgressMonitor monitor) throws Exception {
        assert !Protocol.isDispatchThread();
        final Exception[] res = new Exception[1];
        // Fire comm event to signal state about to change
        fireCommunicationsEvent(CommunicationsEvent.BEFORE_DISCONNECT);
        monitor.beginTask("Disconnecting " + getHostName(), 1); //$NON-NLS-1$
        try {
            /* Disconnecting TCP channel */
            synchronized (res) {
                Protocol.invokeLater(new Runnable() {
                    public void run() {
                        if (!disconnectTCFChannel(res, monitor))
                            add_to_wait_list(this);
                    }
                });
                res.wait();
            }
            if (res[0] != null) throw res[0];

        }
        catch (Exception e) {
            e.printStackTrace();
            throw new RemoteFileException("Error creating Terminal", e); //$NON-NLS-1$
        }
        finally {
            monitor.done();
        }
    }

    public boolean isConnected() {
        final boolean res[] = new boolean[1];
        try {
            Protocol.invokeAndWait(new Runnable() {
                public void run() {
                    res[0] = channel != null && channel.getState() == IChannel.STATE_OPEN;
                }
            });
        }
        catch (IllegalStateException e) {
            res[0] = false;
        }
        return res[0];
    }

    private void add_to_wait_list(Runnable cb) {
        wait_list.add(cb);
        if (poll_timer_started) return;
        Protocol.invokeLater(1000, new Runnable() {
            public void run() {
                poll_timer_started = false;
                run_wait_list();
            }
        });
        poll_timer_started = true;
    }

    private void run_wait_list() {
        if (wait_list.isEmpty()) return;
        Runnable[] r = wait_list.toArray(new Runnable[wait_list.size()]);
        wait_list.clear();
        for (int i = 0; i < r.length; i++) r[i].run();
    }

    private boolean connectTCFChannel(Exception[] res, IProgressMonitor monitor) {
        if (channel != null) {
            switch (channel.getState()) {
            case IChannel.STATE_OPEN:
            case IChannel.STATE_CLOSED:
                synchronized (res) {
                    if (channel_error instanceof Exception) res[0] = (Exception)channel_error;
                    else if (channel_error != null) res[0] = new Exception(channel_error);
                    else res[0] = null;
                    res.notify();
                    return true;
                }
            }
        }
        if (monitor.isCanceled()) {
            synchronized (res) {
                res[0] = new Exception("Canceled"); //$NON-NLS-1$
                if (channel != null) channel.terminate(res[0]);
                res.notify();
                return true;
            }
        }
        if (channel == null) {
            String host = getHostName().toLowerCase();
            int port = getConnectPort();
            if (port <= 0) {
                // Default fallback
                port = TCFConnectorServiceManager.TCF_PORT;
            }
            IPeer peer = null;
            String port_str = Integer.toString(port);
            String transport = getTCFPropertySet().getPropertyValue(TCFConnectorService.PROPERTY_TRANSPORT_NAME);
            ILocator locator = Protocol.getLocator();
            for (IPeer p : locator.getPeers().values()) {
                Map<String, String> attrs = p.getAttributes();
                if (transport.equals(attrs.get(IPeer.ATTR_TRANSPORT_NAME)) && //$NON-NLS-1$
                        host.equalsIgnoreCase(attrs.get(IPeer.ATTR_IP_HOST)) &&
                        port_str.equals(attrs.get(IPeer.ATTR_IP_PORT))) {
                    peer = p;
                    break;
                }
            }
            if (peer == null) {
                Map<String, String> attrs = new HashMap<String, String>();
                attrs.put(IPeer.ATTR_ID, "RSE:" + host + ":" + port_str); //$NON-NLS-1$ //$NON-NLS-2$
                attrs.put(IPeer.ATTR_NAME, getName());
                attrs.put(IPeer.ATTR_TRANSPORT_NAME, transport);
                attrs.put(IPeer.ATTR_IP_HOST, host);
                attrs.put(IPeer.ATTR_IP_PORT, port_str);
                peer = new TransientPeer(attrs);
            }
            channel = peer.openChannel();
            channel.addChannelListener(new IChannel.IChannelListener() {

                public void onChannelOpened() {
                    assert channel != null;
                    run_wait_list();
                }

                public void congestionLevel(int level) {
                }

                public void onChannelClosed(Throwable error) {
                    assert channel != null;
                    channel.removeChannelListener(this);
                    channel_error = error;
                    if (wait_list.isEmpty()) {
                        fireCommunicationsEvent(CommunicationsEvent.CONNECTION_ERROR);
                    }
                    else {
                        run_wait_list();
                    }
                    channel = null;
                    channel_error = null;
                    streams_subscribed = false;
                    stream_ids.clear();
                }

            });
            assert channel.getState() == IChannel.STATE_OPENING;
        }
        return false;
    }

    private boolean disconnectTCFChannel(Exception[] res, IProgressMonitor monitor) {
        if (channel == null || channel.getState() == IChannel.STATE_CLOSED) {
            synchronized (res) {
                res[0] = null;
                res.notify();
                return true;
            }
        }
        if (monitor.isCanceled()) {
            synchronized (res) {
                res[0] = new Exception("Canceled"); //$NON-NLS-1$
                res.notify();
                return true;
            }
        }
        if (channel.getState() == IChannel.STATE_OPEN) channel.close();
        return false;
    }

    public <V extends IService> V getService(Class<V> service_interface) {
        if (channel == null || channel.getState() != IChannel.STATE_OPEN) throw new Error("Not connected"); //$NON-NLS-1$
        V m = channel.getRemoteService(service_interface);
        if (m == null) throw new Error("Remote peer does not support " + service_interface.getName() + " service"); //$NON-NLS-1$  //$NON-NLS-2$
        return m;
    }

    public ISysMonitor getSysMonitorService() {
        return getService(ISysMonitor.class);
    }

    public IFileSystem getFileSystemService() {
        return getService(IFileSystem.class);
    }

    public IChannel getChannel() {
        return channel;
    }

    public String getSessionHostName() {
        String hostName = "";
        IHost host = getHost();
        if (host != null) hostName = host.getHostName();
        return hostName;
    }

    public String getSessionUserId() {
        return getUserId();
    }

    public String getSessionPassword() {
        String password = "";
        SystemSignonInformation ssi = getSignonInformation();
        if (ssi != null) {
            password = ssi.getPassword();
        }
        return password;
    }

    public void onStreamsConnecting() {
        if (!streams_subscribed) {
            streams_subscribed = true;
            IStreams streams = getService(IStreams.class);
            if (streams != null) {
                streams.subscribe(ITerminals.NAME, streams_listener, new IStreams.DoneSubscribe() {
                    public void doneSubscribe(IToken token, Exception error) {
                        if (error != null) channel.terminate(error);
                    }
                });
            }
        }
        streams_connecting = true;
    }

    public void onStreamsID(String id) {
        stream_ids.remove(id);
    }

    public void onStreamsConnected() {
        streams_connecting = false;
        for (String id : stream_ids) {
            getService(IStreams.class).disconnect(id, new IStreams.DoneDisconnect() {
                public void doneDisconnect(IToken token, Exception error) {
                    if (error != null) channel.terminate(error);
                }
            });
        }
    }
}
