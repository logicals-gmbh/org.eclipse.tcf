/*******************************************************************************
 * Copyright (c) 2007-2020 Wind River Systems, Inc. and others.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.tcf.internal.core.ServiceManager;
import org.eclipse.tcf.internal.core.Token;
import org.eclipse.tcf.internal.core.TransportManager;
import org.eclipse.tcf.internal.services.local.LocatorService;
import org.eclipse.tcf.internal.services.remote.GenericProxy;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IErrorReport;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.IService;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.ILocator;

/**
 * Abstract implementation of IChannel interface.
 *
 * AbstractChannel implements communication link connecting two end points (peers).
 * The channel asynchronously transmits messages: commands, results and events.
 *
 * Clients can subclass AbstractChannel to support particular transport (wire) protocol.
 * Also, see StreamChannel for stream oriented transport protocols.
 */
public abstract class AbstractChannel implements IChannel {

    public interface TraceListener {

        public void onMessageReceived(char type, String token,
                String service, String name, byte[] data);

        public void onMessageSent(char type, String token,
                String service, String name, byte[] data);

        public void onChannelClosed(Throwable error);
    }

    public interface Proxy {

        public void onCommand(IToken token, String service, String name, byte[] data);

        public void onEvent(String service, String name, byte[] data);

        public void onChannelClosed(Throwable error);
    }

    /**
     * Represents a message sent through a channel between peers
     * @since 1.7
     */
    protected static class Message {
        /**
         * Type of message.
         * "C" for Commands.
         * "R" for Command Results.
         * "N" for Unknown Command Result.
         * "P" for Progress Result.
         * "E" for Events.
         */
        final char type;
        /**
         * Token associated with the Command
         */
        Token token;
        /**
         * Name of the service
         */
        String service;
        /**
         * In case of a Command ("C" Message) or an Event ("E" Message), the name of it
         */
        String name;
        /**
         * The array of bytes that accompanies the message
         */
        byte[] data;

        boolean is_sent;
        boolean is_canceled;

        Collection<TraceListener> trace;

        /**
         * Constructs a Message of the given type. Type could be 'C', 'R', 'N' 'P' or 'E'
         * @param type type of message
         */
        Message(char type) {
            this.type = type;
        }

        @Override
        public String toString() {
            try {
                StringBuffer bf = new StringBuffer();
                bf.append('[');;
                bf.append(type);
                if (token != null) {
                    bf.append(' ');
                    bf.append(token.getID());
                }
                if (service != null) {
                    bf.append(' ');
                    bf.append(service);
                }
                if (name != null) {
                    bf.append(' ');
                    bf.append(name);
                }
                if (data != null) {
                    int i = 0;
                    while (i < data.length) {
                        int j = i;
                        while (j < data.length && data[j] != 0) j++;
                        bf.append(' ');
                        bf.append(new String(data, i, j - i, "UTF8"));
                        if (j < data.length && data[j] == 0) j++;
                        i = j;
                    }
                }
                bf.append(']');
                return bf.toString();
            }
            catch (Exception x) {
                return x.toString();
            }
        }
    }

    private final LinkedList<Map<String,String>> redirect_queue = new LinkedList<Map<String,String>>();
    private final Map<Class<?>,IService> local_service_by_class = new HashMap<Class<?>,IService>();
    private final Map<Class<?>,IService> remote_service_by_class = new HashMap<Class<?>,IService>();
    private final Map<String,IService> local_service_by_name = new HashMap<String,IService>();
    private final Map<String,IService> remote_service_by_name = new HashMap<String,IService>();
    private final LinkedList<Message> out_queue = new LinkedList<Message>();
    private final Collection<IChannelListener> channel_listeners = new ArrayList<IChannelListener>();
    private final Map<String,IChannel.IEventListener[]> event_listeners = new HashMap<String,IChannel.IEventListener[]>();
    private final Map<String,IChannel.ICommandServer> command_servers = new HashMap<String,IChannel.ICommandServer>();
    private final LinkedList<IPeer> remote_peer_list = new LinkedList<IPeer>();
    private final Map<String,Message> out_tokens = new LinkedHashMap<String,Message>();
    private final Thread inp_thread;
    private final Thread out_thread;
    private boolean notifying_channel_opened;
    private boolean registered_with_trasport;
    private int state = STATE_OPENING;
    private IToken redirect_command;
    private final IPeer local_peer;
    private IPeer remote_peer;
    private Proxy proxy;
    private boolean zero_copy;

    private static final int pending_command_limit = 32;
    private int local_congestion_level = -100;
    private int remote_congestion_level = -100;
    private long local_congestion_time;
    private int local_congestion_cnt;
    private Collection<TraceListener> trace_listeners;

    /**
     * @since 1.2
     */
    protected static final boolean TRACE = Boolean.getBoolean("org.eclipse.tcf.core.tracing.channel");

    public static final int
        /**
         * End of Stream
         */
        EOS = -1,
        /**
         * End of Message
         */
        EOM = -2;

    protected AbstractChannel(IPeer remote_peer) {
        this(LocatorService.getLocalPeer(), remote_peer);
    }

    protected AbstractChannel(IPeer local_peer, IPeer remote_peer) {
        assert Protocol.isDispatchThread();
        this.remote_peer = remote_peer;
        this.local_peer = local_peer;
        remote_peer_list.add(remote_peer);

        /**
         * Thread used handles messages received through the channel
         */
        inp_thread = new Thread() {

            /**
             * Empty byte array used when returning a zero-length byte array on {@code readBytes}
             */
            final byte[] empty_byte_array = new byte[0];
            /**
             * Byte array used as temporary storage of bytes read on {@code readBytes}
             */
            byte[] buf = new byte[1024];
            /**
             * Byte array used as temporary storage of bytes read on {@code readString}
             */
            char[] cbf = new char[1024];

            /**
             * Byte array used to store the error
             */
            byte[] eos_err_report;

            /**
             * Throws an IOException when the input thread reads a malformed Message from the channel
             * @throws IOException with the message "Protocol syntax error"
             */
            private void error() throws IOException {
                throw new IOException("Protocol syntax error");
            }

            /**
             * Reads bytes from a channel
             * @param end the first byte character
             * @return a byte array containing all the bytes read
             * @throws IOException if it finds EOM or EOS reading from input stream
             */
            private byte[] readBytes(int end) throws IOException {
                int len = 0;
                for (;;) {
                    int ch = read();
                    if (ch <= 0) {
                        if (ch == end) break;
                        if (ch == EOM) throw new IOException("Unexpected end of message");
                        if (ch < 0) throw new IOException("Communication channel is closed by remote peer");
                    }
                    if (len >= buf.length) {
                        byte[] tmp = new byte[buf.length * 2];
                        System.arraycopy(buf, 0, tmp, 0, len);
                        buf = tmp;
                    }
                    buf[len++] = (byte)ch;
                }
                if (len == 0) return empty_byte_array;
                byte[] res = new byte[len];
                System.arraycopy(buf, 0, res, 0, len);
                return res;
            }

            /**
             * Reads complete strings made of bytes and return the Java string that it forms
             * @return string containing all the bytes read
             * @throws IOException if it finds EOM or EOS reading from input stream
             */
            private String readString() throws IOException {
                int len = 0;
                for (;;) {
                    int ch = read();
                    if (ch < 0) {
                        if (ch == EOM) throw new IOException("Unexpected end of message");
                        if (ch < 0) throw new IOException("Communication channel is closed by remote peer");
                    }
                    /*
                     * Check if ch is not part of the Basic Latin alphabet
                     */
                    if ((ch & 0x80) != 0) {
                        int n = 0;
                        if ((ch & 0xe0) == 0xc0) {
                            ch &= 0x1f;
                            n = 1;
                        }
                        else if ((ch & 0xf0) == 0xe0) {
                            ch &= 0x0f;
                            n = 2;
                        }
                        else if ((ch & 0xf8) == 0xf0) {
                            ch &= 0x07;
                            n = 3;
                        }
                        else if ((ch & 0xfc) == 0xf8) {
                            ch &= 0x03;
                            n = 4;
                        }
                        else if ((ch & 0xfe) == 0xfc) {
                            ch &= 0x01;
                            n = 5;
                        }
                        while (n > 0) {
                            int b = read();
                            if (b < 0) {
                                if (b == EOM) throw new IOException("Unexpected end of message");
                                if (b < 0) throw new IOException("Communication channel is closed by remote peer");
                            }
                            ch = (ch << 6) | (b & 0x3f);
                            n--;
                        }
                    }
                    if (ch == 0) break;
                    /*
                     * Duplicate size of array used to hold the bytes, after the size is bigger than original array
                     */
                    if (len >= cbf.length) {
                        char[] tmp = new char[cbf.length * 2];
                        System.arraycopy(cbf, 0, tmp, 0, len);
                        cbf = tmp;
                    }
                    cbf[len++] = (char)ch;
                }
                return new String(cbf, 0, len);
            }

            @Override
            public void run() {
                try {
                    while (true) {
                        int n = read();
                        if (n == EOM) continue;
                        if (n == EOS) {
                            try {
                                eos_err_report = readBytes(EOM);
                                if (eos_err_report.length == 0 || eos_err_report.length == 1 && eos_err_report[0] == 0) eos_err_report = null;
                            }
                            catch (Exception x) {
                            }
                            break;
                        }
                        final Message msg = new Message((char)n);
                        if (read() != 0) error();
                        switch (msg.type) {
                        case 'C':
                            msg.token = new Token(readBytes(0));
                            msg.service = readString();
                            msg.name = readString();
                            msg.data = readBytes(EOM);
                            break;
                        case 'P':
                        case 'R':
                        case 'N':
                            msg.token = new Token(readBytes(0));
                            msg.data = readBytes(EOM);
                            break;
                        case 'E':
                            msg.service = readString();
                            msg.name = readString();
                            msg.data = readBytes(EOM);
                            break;
                        case 'F':
                            msg.data = readBytes(EOM);
                            break;
                        default:
                            error();
                        }
                        /*
                         * Message handling is done in the dispatch thread
                         */
                        Protocol.invokeLater(new Runnable() {
                            public void run() {
                                handleInput(msg);
                            }
                        });
                        int delay = local_congestion_level;
                        if (delay > 0) sleep(delay);
                    }
                    Protocol.invokeLater(new Runnable() {
                        public void run() {
                            if (out_tokens.isEmpty() && eos_err_report == null && state != STATE_OPENING) {
                                close();
                            }
                            else {
                                IOException x = new IOException("Communication channel is closed by remote peer");
                                if (eos_err_report != null) {
                                    try {
                                        Object[] args = JSON.parseSequence(eos_err_report);
                                        if (args.length > 0 && args[0] != null) {
                                            x.initCause(new Exception(Command.toErrorString(args[0])));
                                        }
                                    }
                                    catch (IOException e) {
                                    }
                                }
                                terminate(x);
                            }
                        }
                    });
                }
                catch (final Throwable x) {
                    try {
                        Protocol.invokeLater(new Runnable() {
                            public void run() {
                                terminate(x);
                            }
                        });
                    }
                    catch (IllegalStateException y) {
                        // TCF event dispatcher has shut down
                    }
                }
            }
        };

        /**
         * Thread used to handle messages sent through the channel
         */
        out_thread = new Thread() {

            private final byte[] out_buf = new byte[0x4000];
            private int out_buf_pos;

            void writeBytes(byte[] buf)  throws IOException {
                if (buf.length > out_buf.length) {
                    write(out_buf, 0, out_buf_pos);
                    out_buf_pos = 0;
                    write(buf);
                }
                else {
                    int i = 0;
                    while (i < buf.length) {
                        if (out_buf_pos >= out_buf.length) {
                            write(out_buf);
                            out_buf_pos = 0;
                        }
                        int n = buf.length - i;
                        if (n > out_buf.length - out_buf_pos) n = out_buf.length - out_buf_pos;
                        System.arraycopy(buf, i, out_buf, out_buf_pos, n);
                        out_buf_pos += n;
                        i += n;
                    }
                }
            }

            void writeString(String s) throws IOException {
                int l = s.length();
                for (int i = 0; i < l; i++) {
                    if (out_buf_pos + 4 > out_buf.length) {
                        write(out_buf, 0, out_buf_pos);
                        out_buf_pos = 0;
                    }
                    int ch = s.charAt(i);
                    if (ch < 0x80) {
                        out_buf[out_buf_pos++] = (byte)ch;
                    }
                    else if (ch < 0x800) {
                        out_buf[out_buf_pos++] = (byte)((ch >> 6) | 0xc0);
                        out_buf[out_buf_pos++] = (byte)(ch & 0x3f | 0x80);
                    }
                    else if (ch < 0x10000) {
                        out_buf[out_buf_pos++] = (byte)((ch >> 12) | 0xe0);
                        out_buf[out_buf_pos++] = (byte)((ch >> 6) & 0x3f | 0x80);
                        out_buf[out_buf_pos++] = (byte)(ch & 0x3f | 0x80);
                    }
                    else {
                        out_buf[out_buf_pos++] = (byte)((ch >> 18) | 0xf0);
                        out_buf[out_buf_pos++] = (byte)((ch >> 12) & 0x3f | 0x80);
                        out_buf[out_buf_pos++] = (byte)((ch >> 6) & 0x3f | 0x80);
                        out_buf[out_buf_pos++] = (byte)(ch & 0x3f | 0x80);
                    }
                }
                if (out_buf_pos >= out_buf.length) {
                    write(out_buf);
                    out_buf_pos = 0;
                }
                out_buf[out_buf_pos++] = 0;
            }

            @Override
            public void run() {
                try {
                    while (true) {
                        Message msg = null;
                        boolean last = false;
                        synchronized (out_queue) {
                            while (out_queue.size() == 0) out_queue.wait();
                            msg = out_queue.removeFirst();
                            if (msg == null) break;
                            last = out_queue.isEmpty();
                            if (msg.is_canceled) {
                                if (last) flush();
                                continue;
                            }
                            msg.is_sent = true;
                        }
                        if (msg.trace != null) {
                            final Message m = msg;
                            Protocol.invokeLater(new Runnable() {
                                public void run() {
                                    for (TraceListener l : m.trace) {
                                        try {
                                            l.onMessageSent(m.type, m.token == null ? null : m.token.getID(),
                                                    m.service, m.name, m.data);
                                        }
                                        catch (Throwable x) {
                                            Protocol.log("Exception in channel listener", x);
                                        }
                                    }
                                }
                            });
                        }
                        out_buf_pos = 0;
                        out_buf[out_buf_pos++] = (byte)msg.type;
                        out_buf[out_buf_pos++] = 0;
                        if (msg.token != null) writeString(msg.token.getID());
                        if (msg.service != null) writeString(msg.service);
                        if (msg.name != null) writeString(msg.name);
                        if (msg.data != null) writeBytes(msg.data);
                        write(out_buf, 0, out_buf_pos);
                        write(EOM);
                        int delay = 0;
                        int level = remote_congestion_level;
                        if (level > 0) delay = level * 10;
                        if (last || delay > 0) flush();
                        if (delay > 0) sleep(delay);
                        else yield();
                    }
                    write(EOS);
                    write(EOM);
                    flush();
                }
                catch (final Throwable x) {
                    try {
                        Protocol.invokeLater(new Runnable() {
                            public void run() {
                                terminate(x);
                            }
                        });
                    }
                    catch (IllegalStateException y) {
                        // TCF event dispatcher has shut down
                    }
                }
            }
        };
        inp_thread.setName("TCF Channel Receiver");
        out_thread.setName("TCF Channel Transmitter");
    }

    protected void start() {
        assert Protocol.isDispatchThread();
        Protocol.invokeLater(new Runnable() {
            public void run() {
                try {
                    if (proxy != null) return;
                    if (state == STATE_CLOSED) return;
                    ServiceManager.onChannelCreated(AbstractChannel.this, local_service_by_name);
                    makeServiceByClassMap(local_service_by_name, local_service_by_class);
                    Object[] args = new Object[]{ local_service_by_name.keySet() };
                    sendEvent(Protocol.getLocator(), "Hello", JSON.toJSONSequence(args));
                }
                catch (IOException x) {
                    terminate(x);
                }
            }
        });
        inp_thread.start();
        out_thread.start();
    }

    /**
     * Redirect this channel to given peer using this channel remote peer locator service as a proxy.
     * @param peer_id - peer that will become new remote communication endpoint of this channel
     */
    public void redirect(String peer_id) {
        Map<String,String> map = new HashMap<String,String>();
        map.put(IPeer.ATTR_ID, peer_id);
        redirect(map);
    }

    /**
     * Redirect this channel to given peer using this channel remote peer locator service as a proxy.
     * @param peer_attrs - peer that will become new remote communication endpoint of this channel
     */
    public void redirect(final Map<String,String> peer_attrs) {
        assert Protocol.isDispatchThread();
        if (state == STATE_OPENING) {
            redirect_queue.add(peer_attrs);
        }
        else {
            assert state == STATE_OPEN;
            assert redirect_command == null;
            try {
                final ILocator l = (ILocator)remote_service_by_class.get(ILocator.class);
                if (l == null) throw new IOException("Cannot redirect channel: peer " +
                        remote_peer.getID() + " has no locator service");
                final String peer_id = peer_attrs.get(IPeer.ATTR_ID);
                if (peer_id != null && peer_attrs.size() == 1) {
                    final IPeer peer = l.getPeers().get(peer_id);
                    if (peer == null) {
                        // Peer not found, must wait for a while until peer is discovered or time out
                        final boolean[] found = new boolean[1];
                        Protocol.invokeLater(ILocator.DATA_RETENTION_PERIOD / 3, new Runnable() {
                            public void run() {
                                if (found[0]) return;
                                terminate(new Exception("Peer " + peer_id + " not found"));
                            }
                        });
                        l.addListener(new ILocator.LocatorListener() {
                            public void peerAdded(IPeer peer) {
                                if (peer.getID().equals(peer_id)) {
                                    found[0] = true;
                                    l.removeListener(this);
                                    if (state == STATE_OPENING) {
                                        state = STATE_OPEN;
                                        redirect(peer_id);
                                    }
                                }
                            }
                            public void peerChanged(IPeer peer) {
                            }

                            public void peerHeartBeat(String id) {
                            }

                            public void peerRemoved(String id) {
                            }
                        });
                    }
                    else {
                        redirect_command = l.redirect(peer_id, new ILocator.DoneRedirect() {
                            public void doneRedirect(IToken token, Exception x) {
                                assert redirect_command == token;
                                redirect_command = null;
                                if (state != STATE_OPENING) return;
                                if (x != null) terminate(x);
                                remote_peer = peer;
                                remote_peer_list.add(remote_peer);
                                remote_service_by_class.clear();
                                remote_service_by_name.clear();
                                event_listeners.clear();
                            }
                        });
                    }
                }
                else {
                    redirect_command = l.redirect(peer_attrs, new ILocator.DoneRedirect() {
                        public void doneRedirect(IToken token, Exception x) {
                            assert redirect_command == token;
                            redirect_command = null;
                            if (state != STATE_OPENING) return;
                            if (x != null) terminate(x);
                            final IPeer parent = remote_peer;
                            remote_peer = new TransientPeer(peer_attrs) {
                                public IChannel openChannel() {
                                    IChannel c = parent.openChannel();
                                    c.redirect(peer_attrs);
                                    return c;
                                }
                            };
                            remote_peer_list.add(remote_peer);
                            remote_service_by_class.clear();
                            remote_service_by_name.clear();
                            event_listeners.clear();
                        }
                    });
                }
                state = STATE_OPENING;
            }
            catch (Throwable x) {
                terminate(x);
            }
        }
    }

    private void makeServiceByClassMap(Map<String,IService> by_name, Map<Class<?>,IService> by_class) {
        for (IService service : by_name.values()) {
            for (Class<?> fs : service.getClass().getInterfaces()) {
                if (fs.equals(IService.class)) continue;
                if (!IService.class.isAssignableFrom(fs)) {
                    continue;
                }
                by_class.put(fs, service);
            }
        }
    }

    public final int getState() {
        return state;
    }

    public void addChannelListener(IChannelListener listener) {
        assert Protocol.isDispatchThread();
        assert listener != null;
        channel_listeners.add(listener);
    }

    public void removeChannelListener(IChannelListener listener) {
        assert Protocol.isDispatchThread();
        channel_listeners.remove(listener);
    }

    public void addTraceListener(TraceListener listener) {
        if (trace_listeners == null) {
            trace_listeners = new ArrayList<TraceListener>();
        }
        else {
            trace_listeners = new ArrayList<TraceListener>(trace_listeners);
        }
        trace_listeners.add(listener);
    }

    public void removeTraceListener(TraceListener listener) {
        trace_listeners = new ArrayList<TraceListener>(trace_listeners);
        trace_listeners.remove(listener);
        if (trace_listeners.isEmpty()) trace_listeners = null;
    }

    public void addEventListener(IService service, IChannel.IEventListener listener) {
        assert Protocol.isDispatchThread();
        IChannel.IEventListener[] list = event_listeners.get(service.getName());
        IChannel.IEventListener[] next = new IChannel.IEventListener[list == null ? 1 : list.length + 1];
        if (list != null) System.arraycopy(list, 0, next, 0, list.length);
        next[next.length - 1] = listener;
        event_listeners.put(service.getName(), next);
    }

    public void removeEventListener(IService service, IChannel.IEventListener listener) {
        assert Protocol.isDispatchThread();
        IChannel.IEventListener[] list = event_listeners.get(service.getName());
        for (int i = 0; i < list.length; i++) {
            if (list[i] == listener) {
                if (list.length == 1) {
                    event_listeners.remove(service.getName());
                }
                else {
                    IChannel.IEventListener[] next = new IChannel.IEventListener[list.length - 1];
                    System.arraycopy(list, 0, next, 0, i);
                    System.arraycopy(list, i + 1, next, i, next.length - i);
                    event_listeners.put(service.getName(), next);
                }
                return;
            }
        }
    }

    public void addCommandServer(IService service, IChannel.ICommandServer listener) {
        assert Protocol.isDispatchThread();
        if (command_servers.put(service.getName(), listener) != null) {
            throw new Error("Only one command server per service is allowed");
        }
    }

    public void removeCommandServer(IService service, IChannel.ICommandServer listener) {
        assert Protocol.isDispatchThread();
        if (command_servers.remove(service.getName()) != listener) {
            throw new Error("Invalid command server");
        }
    }

    public void close() {
        assert Protocol.isDispatchThread();
        if (state == STATE_CLOSED) {
            return;
        }
        try {
            sendEndOfStream(10000);
            close(null);
        }
        catch (Exception x) {
            close(x);
        }
    }

    public void terminate(Throwable error) {
        assert Protocol.isDispatchThread();
        if (state == STATE_CLOSED) {
            return;
        }
        try {
            sendEndOfStream(500);
            close(error);
        }
        catch (Exception x) {
            if (error == null) error = x;
            close(error);
        }
    }

    private void sendEndOfStream(long timeout) throws Exception {
        synchronized (out_queue) {
            out_queue.clear();
            out_queue.add(null);
            out_queue.notifyAll();
        }
        out_thread.join(timeout);
    }

    private void close(final Throwable error) {
        assert state != STATE_CLOSED;
        state = STATE_CLOSED;
        // Closing channel underlying streams can block for a long time,
        // so it needs to be done by a background thread.
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    AbstractChannel.this.stop();
                }
                catch (Exception x) {
                    Protocol.log("Cannot close channel streams", x);
                }
            }
        };
        thread.setName("TCF Channel Cleanup");
        thread.setDaemon(true);
        thread.start();
        if (error != null && remote_peer instanceof AbstractPeer) {
            ((AbstractPeer)remote_peer).onChannelTerminated();
        }
        if (registered_with_trasport) {
            registered_with_trasport = false;
            TransportManager.channelClosed(this, error);
        }
        if (proxy != null) {
            try {
                proxy.onChannelClosed(error);
            }
            catch (Throwable x) {
                Protocol.log("Exception in channel listener", x);
            }
        }
        Protocol.invokeLater(new Runnable() {
            public void run() {
                if (!out_tokens.isEmpty()) {
                    Exception x = null;
                    if (error instanceof Exception) x = (Exception)error;
                    else if (error != null) x = new Exception(error);
                    else x = new IOException("Channel is closed");
                    for (Message msg : out_tokens.values()) {
                        assert msg.token != null;
                        try {
                            String s = msg.toString();
                            if (s.length() > 72) s = s.substring(0, 72) + "...]";
                            IOException y = new IOException("Command " + s + " aborted");
                            y.initCause(x);
                            msg.token.getListener().terminated(msg.token, y);
                        }
                        catch (Throwable e) {
                            Protocol.log("Exception in command listener", e);
                        }
                    }
                    out_tokens.clear();
                }
                if (channel_listeners.size() > 0) {
                    for (IChannelListener l : channel_listeners.toArray(
                            new IChannelListener[channel_listeners.size()])) {
                        try {
                            l.onChannelClosed(error);
                        }
                        catch (Throwable x) {
                            Protocol.log("Exception in channel listener", x);
                        }
                    }
                }
                else if (error != null) {
                    Protocol.log("TCF channel terminated", error);
                }
                if (trace_listeners != null) {
                    for (TraceListener l : trace_listeners) {
                        try {
                            l.onChannelClosed(error);
                        }
                        catch (Throwable x) {
                            Protocol.log("Exception in channel listener", x);
                        }
                    }
                }
            }
        });
    }

    public int getCongestion() {
        assert Protocol.isDispatchThread();
        int level = out_tokens.size() * 100 / pending_command_limit - 100;
        if (remote_congestion_level > level) level = remote_congestion_level;
        if (level > 100) level = 100;
        return level;
    }

    public IPeer getLocalPeer() {
        assert Protocol.isDispatchThread();
        return local_peer;
    }

    public IPeer getRemotePeer() {
        assert Protocol.isDispatchThread();
        return remote_peer;
    }

    public List<IPeer> getRemotePeerList() {
        assert Protocol.isDispatchThread();
        return remote_peer_list;
    }

    public Collection<String> getLocalServices() {
        assert Protocol.isDispatchThread();
        assert state != STATE_OPENING;
        return local_service_by_name.keySet();
    }

    public Collection<String> getRemoteServices() {
        assert Protocol.isDispatchThread();
        assert state != STATE_OPENING;
        return remote_service_by_name.keySet();
    }

    @SuppressWarnings("unchecked")
    public <V extends IService> V getLocalService(Class<V> cls) {
        assert Protocol.isDispatchThread();
        assert state != STATE_OPENING;
        return (V)local_service_by_class.get(cls);
    }

    @SuppressWarnings("unchecked")
    public <V extends IService> V getRemoteService(Class<V> cls) {
        assert Protocol.isDispatchThread();
        assert state != STATE_OPENING;
        return (V)remote_service_by_class.get(cls);
    }

    public <V extends IService> void setServiceProxy(Class<V> service_interface, IService service_proxy) {
        String name = service_proxy.getName();
        if (remote_service_by_name.get(name) == null) throw new Error("Service not available");
        if (!notifying_channel_opened) throw new Error("setServiceProxe() can be called only from channel open call-back");
        if (!(remote_service_by_name.get(name) instanceof GenericProxy)) throw new Error("Proxy already set");
        if (remote_service_by_class.get(service_interface) != null) throw new Error("Proxy already set");
        remote_service_by_class.put(service_interface, service_proxy);
        remote_service_by_name.put(name, service_proxy);
    }

    public IService getLocalService(String service_name) {
        assert Protocol.isDispatchThread();
        assert state != STATE_OPENING;
        return local_service_by_name.get(service_name);
    }

    public IService getRemoteService(String service_name) {
        assert Protocol.isDispatchThread();
        assert state != STATE_OPENING;
        return remote_service_by_name.get(service_name);
    }

    public void setProxy(Proxy proxy, Collection<String> services) throws IOException {
        this.proxy = proxy;
        sendEvent(Protocol.getLocator(), "Hello", JSON.toJSONSequence(new Object[]{ services }));
        local_service_by_class.clear();
        local_service_by_name.clear();
    }

    private void addToOutQueue(Message msg) {
        msg.trace = trace_listeners;
        synchronized (out_queue) {
            out_queue.add(msg);
            out_queue.notifyAll();
        }
    }

    public IToken sendCommand(IService service, String name, byte[] args, ICommandListener listener) {
        assert Protocol.isDispatchThread();
        if (state == STATE_OPENING) throw new Error("Channel is waiting for Hello message");
        if (state == STATE_CLOSED) throw new Error("Channel is closed");
        final Message msg = new Message('C');
        msg.service = service.getName();
        msg.name = name;
        msg.data = args;
        Token token = new Token(listener) {
            @Override
            public boolean cancel() {
                assert msg.token == this;
                assert Protocol.isDispatchThread();
                if (state != STATE_OPEN) return false;
                synchronized (out_queue) {
                    if (msg.is_sent) return false;
                    msg.is_canceled = true;
                }
                out_tokens.remove(getID());
                return true;
            }
        };
        msg.token = token;
        out_tokens.put(token.getID(), msg);
        addToOutQueue(msg);
        return token;
    }

    /**
     * Send a command's progress response. Used for commands that can deliver partial results.
     * @param token token associated with this command/response
     * @param results array of bytes containing the data of the message
     */
    public void sendProgress(IToken token, byte[] results) {
        assert Protocol.isDispatchThread();
        if (state != STATE_OPEN) {
            throw new Error("Channel is closed");
        }
        Message msg = new Message('P');
        msg.data = results;
        msg.token = (Token)token;
        addToOutQueue(msg);
    }

    /**
     * Send a command's result response. There's exactly one result per command
     * @param token token associated with this command/response
     * @param results array of bytes containing the data of the message
     */
    public void sendResult(IToken token, byte[] results) {
        assert Protocol.isDispatchThread();
        if (state != STATE_OPEN) {
            throw new Error("Channel is closed");
        }
        Message msg = new Message('R');
        msg.data = results;
        msg.token = (Token)token;
        addToOutQueue(msg);
    }

    /**
     * Sends an "unrecognized command" message for the command corresponding to the given token
     * @param token token associated with this command/response
     */
    public void rejectCommand(IToken token) {
        assert Protocol.isDispatchThread();
        if (state != STATE_OPEN) {
            throw new Error("Channel is closed");
        }
        Message msg = new Message('N');
        msg.token = (Token)token;
        addToOutQueue(msg);
    }

    /**
     * Sends an event message with the given name, for the given service with the given arguments
     * @param service service this event belongs to
     * @param name event's name
     * @param args additional arguments of the event
     */
    public void sendEvent(IService service, String name, byte[] args) {
        assert Protocol.isDispatchThread();
        if (!(state == STATE_OPEN || state == STATE_OPENING && service instanceof ILocator)) {
            throw new Error("Channel is closed");
        }
        Message msg = new Message('E');
        msg.service = service.getName();
        msg.name = name;
        msg.data = args;
        addToOutQueue(msg);
    }

    public boolean isZeroCopySupported() {
        return zero_copy;
    }

    /**
     * Handles the message received from the channel
     * @param msg
     * @since 1.7
     */
    @SuppressWarnings("unchecked")
    protected void handleInput(Message msg) {
        assert Protocol.isDispatchThread();
        if (state == STATE_CLOSED) {
            return;
        }
        if (trace_listeners != null) {
            for (TraceListener l : trace_listeners) {
                try {
                    l.onMessageReceived(msg.type,
                            msg.token != null ? msg.token.getID() : null,
                            msg.service, msg.name, msg.data);
                }
                catch (Throwable x) {
                    Protocol.log("Exception in trace listener", x);
                }
            }
        }
        try {
            Message cmd = null;
            Token token = null;
            switch (msg.type) {
            case 'P':
            case 'R':
            case 'N':
                String token_id = msg.token.getID();
                cmd = msg.type == 'P' ? out_tokens.get(token_id) : out_tokens.remove(token_id);
                if (cmd == null) {
                    throw new Exception("Invalid token received: " + token_id);
                }
                token = cmd.token;
                break;
            }
            switch (msg.type) {
            case 'C':
                assert msg.service != null;
                assert msg.name != null;
                if (state == STATE_OPENING) {
                    throw new IOException("Received command " + msg.service + "." + msg.name + " before Hello message");
                }
                if (proxy != null) {
                    proxy.onCommand(msg.token, msg.service, msg.name, msg.data);
                }
                else {
                    token = msg.token;
                    IChannel.ICommandServer cmds = command_servers.get(msg.service);
                    if (cmds != null) {
                        cmds.command(token, msg.name, msg.data);
                    }
                    else {
                        rejectCommand(token);
                    }
                }
                break;
            case 'P':
                token.getListener().progress(token, msg.data);
                sendCongestionLevel();
                break;
            case 'R':
                token.getListener().result(token, msg.data);
                sendCongestionLevel();
                break;
            case 'N':
                {
                    String s = null;
                    if (remote_service_by_name.get(cmd.service) == null) {
                        s = "No such service: " + cmd.service;
                    }
                    else {
                        s = "Command is not recognized: " + cmd.service + "." + cmd.name;
                    }
                    token.getListener().terminated(token, new ErrorReport(s, IErrorReport.TCF_ERROR_INV_COMMAND));
                }
                break;
            case 'E':
                assert msg.service != null;
                assert msg.name != null;
                boolean hello = msg.service.equals(ILocator.NAME) && msg.name.equals("Hello");
                if (hello) {
                    remote_service_by_name.clear();
                    remote_service_by_class.clear();
                    ServiceManager.onChannelOpened(this, (Collection<String>)JSON.parseSequence(msg.data)[0], remote_service_by_name);
                    makeServiceByClassMap(remote_service_by_name, remote_service_by_class);
                    zero_copy = remote_service_by_name.containsKey("ZeroCopy");
                }
                if (proxy != null && state == STATE_OPEN) {
                    proxy.onEvent(msg.service, msg.name, msg.data);
                }
                else if (hello) {
                    assert state == STATE_OPENING;
                    state = STATE_OPEN;
                    assert redirect_command == null;
                    if (redirect_queue.size() > 0) {
                        redirect(redirect_queue.removeFirst());
                    }
                    else {
                        notifying_channel_opened = true;
                        if (!registered_with_trasport) {
                            TransportManager.channelOpened(this);
                            registered_with_trasport = true;
                        }
                        if (channel_listeners.size() > 0) {
                            for (IChannelListener l : channel_listeners.toArray(
                                    new IChannelListener[channel_listeners.size()])) {
                                try {
                                    l.onChannelOpened();
                                }
                                catch (Throwable x) {
                                    Protocol.log("Exception in channel listener", x);
                                }
                            }
                        }
                        else if (TRACE) {
                            Protocol.log("TCF channel opened but no one is listening.", null);
                        }
                        notifying_channel_opened = false;
                    }
                }
                else {
                    IChannel.IEventListener[] list = event_listeners.get(msg.service);
                    if (list != null) {
                        for (int i = 0; i < list.length; i++) {
                            list[i].event(msg.name, msg.data);
                        }
                    }
                    sendCongestionLevel();
                }
                break;
            case 'F':
                int len = msg.data.length;
                if (len > 0 && msg.data[len - 1] == 0) len--;
                remote_congestion_level = Integer.parseInt(new String(msg.data, 0, len, "ASCII"));
                for (IChannelListener l : channel_listeners.toArray(
                        new IChannelListener[channel_listeners.size()])) {
                    try {
                        l.congestionLevel(getCongestion());
                    }
                    catch (Throwable x) {
                        Protocol.log("Exception in channel listener", x);
                    }
                }
                break;
            default:
                assert false;
                break;
            }
        }
        catch (Throwable x) {
            terminate(x);
        }
    }

    /**
     *
     * @throws IOException
     */
    private void sendCongestionLevel() throws IOException {
        if (++local_congestion_cnt < 8) {
            return;
        }
        local_congestion_cnt = 0;
        if (state != STATE_OPEN) return;
        long time = System.currentTimeMillis();
        if (time - local_congestion_time < 500) {
            return;
        }
        assert Protocol.isDispatchThread();
        int level = Protocol.getCongestionLevel();
        if (level == local_congestion_level) {
            return;
        }
        int i = (level - local_congestion_level) / 8;
        if (i != 0) level = local_congestion_level + i;
        local_congestion_time = time;
        synchronized (out_queue) {
            Message msg = out_queue.isEmpty() ? null : out_queue.get(0);
            if (msg == null || msg.type != 'F') {
                msg = new Message('F');
                out_queue.add(0, msg);
                out_queue.notify();
            }
            StringBuilder buffer = new StringBuilder();
            buffer.append(local_congestion_level);
            buffer.append((char)0); // 0 terminate
            msg.data = buffer.toString().getBytes("ASCII");
            msg.trace = trace_listeners;
            local_congestion_level = level;
        }
    }

    /**
     * Read one byte from the channel input stream.
     * @return next data byte or EOS (-1) if end of stream is reached,
     * or EOM (-2) if end of message is reached.
     * @throws IOException
     */
    protected abstract int read() throws IOException;

    /**
     * Write one byte into the channel output stream.
     * The method argument can be one of two special values:
     *   EOS (-1) end of stream marker;
     *   EOM (-2) end of message marker.
     * The stream can put the byte into a buffer instead of transmitting it right away.
     * @param n - the data byte.
     * @throws IOException
     */
    protected abstract void write(int n) throws IOException;

    /**
     * Flush the channel output stream.
     * All buffered data should be transmitted immediately.
     * @throws IOException
     */
    protected abstract void flush() throws IOException;

    /**
     * Stop (close) channel underlying streams.
     * If a thread is blocked by read() or write(), it should be
     * resumed (or interrupted).
     * @throws IOException
     */
    protected abstract void stop() throws IOException;

    /**
     * Write array of bytes into the channel output stream.
     * The stream can put bytes into a buffer instead of transmitting it right away.
     * @param buf
     * @throws IOException
     */
    protected void write(byte[] buf) throws IOException {
        assert Thread.currentThread() == out_thread;
        for (int i = 0; i < buf.length; i++) {
            write(buf[i] & 0xff);
        }
    }

    /**
     * Write array of bytes into the channel output stream.
     * The stream can put bytes into a buffer instead of transmitting it right away.
     * @param buf
     * @param pos
     * @param len
     * @throws IOException
     * @since 1.3
     */
    protected void write(byte[] buf, int pos, int len) throws IOException {
        assert Thread.currentThread() == out_thread;
        for (int i = pos; i < pos + len; i++) {
            write(buf[i] & 0xff);
        }
    }
}
