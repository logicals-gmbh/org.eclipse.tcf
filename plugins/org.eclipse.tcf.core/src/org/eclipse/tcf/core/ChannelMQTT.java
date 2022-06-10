// Copyright (C) logi.cals GmbH. All rights reserved.
package org.eclipse.tcf.core;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import org.eclipse.tcf.internal.core.Token;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.ILocator;

/**
 * ChannelMQTT implements TCF channel over MQTT protocol.
 *
 * @since 1.7.2
 */
public class ChannelMQTT extends AbstractChannel implements MqttCallback {

    private static final int INIT_TIMEOUT = 2000;
    public static final int ESC = 3;
    private static int id_cnt = 0;
    private final String id = UUID.randomUUID().toString() + "-" + Integer.toHexString(id_cnt++);
    private String tcfTopic;
    private AtomicBoolean stoppedLock = new AtomicBoolean(false);
    private boolean stopped;
    private byte[] wr_buf = new byte[0x1000];
    private int wr_cnt;
    private int bin_data_size;
    private int inp_buf_pos;
    private int inp_buf_len;
    private InputStream inp;
    private MqttClient mqttClient = null;
    private final MqttCallback callback;
    byte[] read_payload = new byte[0];
    private AtomicBoolean initialized = new AtomicBoolean();

    public ChannelMQTT(final IPeer remote_peer) {
        super(remote_peer);
        callback = this;
        final MemoryPersistence persistence = new MemoryPersistence();
        final String broker = remote_peer.getAttributes().get(MqttPeer.ATTR_BROKER_ID);
        tcfTopic = remote_peer.getAttributes().get(MqttPeer.ATTR_TCF_TOPIC);
        try {
            tcfTopic = remote_peer.getAttributes().get(MqttPeer.ATTR_TCF_TOPIC);
            mqttClient = new MqttClient(broker, id, persistence);
            mqttClient.setCallback(callback);
            final MqttConnectionOptions connOpts = new MqttConnectionOptions();
            connOpts.setUserName(getUsername(remote_peer));
            connOpts.setPassword(getPassword(remote_peer));
            connOpts.setCleanStart(true);
            mqttClient.connect(connOpts);

            MqttSubscription opts = new MqttSubscription(getTcfTopic(), 1);
            opts.setNoLocal(true);
            mqttClient.subscribe(new MqttSubscription[] { opts });
        }
        catch (MqttException exception) {
            // TODO Auto-generated catch block
            exception.printStackTrace();
        }
    }

    public void init()
    {
        Protocol.invokeLater(new Runnable() {
            @Override
            public void run() {
                getIsInitialized().set(true);
                start();
            }
        });
    }

    private byte[] getPassword(final IPeer remote_peer) {
        String password = remote_peer.getAttributes().get(MqttPeer.ATTR_PASSWORD);
        return password.isEmpty() ? null : password.getBytes();
    }

    private String getUsername(final IPeer remote_peer) {
        String userName = remote_peer.getUserName();
        return userName.isEmpty() ? null : userName;
    }

    @Override
    protected void write(final int n) throws IOException {
        if (n < 0) {
            if (wr_cnt > 0) {
                try {
                    int i = 0;
                    final char type = (char) wr_buf[i++];
                    checkEndOfString(i++);
                    switch (type) {
                    case 'C':
                        sendCommand(i);
                        break;
                    case 'E':
                        sendEvent(i);
                        break;
                    }
                }
                catch (final Throwable x) {
                    if (x instanceof FileNotFoundException) {
                        throw new IOException("Page not found: " + x.getMessage());
                    }
                    if (x instanceof IOException) {
                        throw (IOException) x;
                    }
                    throw new IOException(x);
                }
                finally {
                    wr_cnt = 0;
                }
            }
            return;
        }
        if (wr_cnt >= wr_buf.length) {
            final byte[] t = new byte[wr_cnt * 2];
            System.arraycopy(wr_buf, 0, t, 0, wr_cnt);
            wr_buf = t;
        }
        wr_buf[wr_cnt++] = (byte) n;
    }

    @Override
    protected final void write(final byte[] buf) throws IOException {
        write(buf, 0, buf.length);
    }

    @Override
    protected final void write(final byte[] buf, final int pos, final int len) throws IOException {
        if (wr_cnt + len > wr_buf.length) {
            final byte[] t = new byte[(wr_cnt + len) * 2];
            System.arraycopy(wr_buf, 0, t, 0, wr_cnt);
            wr_buf = t;
        }
        System.arraycopy(buf, pos, wr_buf, wr_cnt, len);
        wr_cnt += len;
    }

    @Override
    protected void flush() throws IOException {
    }

    @Override
    protected void stop() throws IOException {
        assert !isStopped();
        setStopped(true);
        getIsInitialized().set(false);

        if (mqttClient == null) {
            return;
        }

        try {
            mqttClient.unsubscribe(getTcfTopic());
            mqttClient.disconnect();
            mqttClient.close();
        }
        catch (final MqttException exception) {
            Protocol.log("Error closing MQTT connection", exception);
        }
    }

    private String getTcfTopic() {
        return tcfTopic;
    }

    private void checkEndOfString(final int i) throws Exception {
        if (i >= wr_cnt || wr_buf[i] != 0) {
            throw new IOException("Invalid message format");
        }
    }

    private String getArgs(int i) throws Exception {
        if (i >= wr_cnt) {
            return null;
        }
        final StringBuffer args = new StringBuffer();
        while (i < wr_cnt) {
            if (args.length() > 0) {
                args.append('\u0000');
            }
            while (wr_buf[i] != 0) {
                final char ch = (char) (wr_buf[i++] & 0xff);
                args.append(ch);
            }
            checkEndOfString(i++);
        }
        return args.toString();
    }

    private void sendCommand(int i) throws Exception {
        int p = i;
        while (i < wr_cnt && wr_buf[i] != 0) {
            i++;
        }
        final byte[] t = new byte[i - p];
        System.arraycopy(wr_buf, p, t, 0, t.length);
        final Token token = new Token(t);
        checkEndOfString(i++);

        p = i;
        while (i < wr_cnt && wr_buf[i] != 0) {
            i++;
        }
        final String service = new String(wr_buf, p, i - p, "UTF-8");
        checkEndOfString(i++);

        p = i;
        while (i < wr_cnt && wr_buf[i] != 0) {
            i++;
        }
        final String command = new String(wr_buf, p, i - p, "UTF-8");
        checkEndOfString(i++);

        sendRequest(token, service, command, getArgs(i));
    }

    private void sendEvent(int i) throws Exception {
        int p = i;
        while (i < wr_cnt && wr_buf[i] != 0) {
            i++;
        }
        final String service = new String(wr_buf, p, i - p, "UTF-8");
        checkEndOfString(i++);

        p = i;
        while (i < wr_cnt && wr_buf[i] != 0) {
            i++;
        }
        final String command = new String(wr_buf, p, i - p, "UTF-8");
        checkEndOfString(i++);

        if (command.equalsIgnoreCase("hello")) {
            sendRequest(null, service, command, getArgs(i));
        }
        else {
            sendRequest(null, service, command, getArgs(i));
        }
    }

    private void sendRequest(final Token token, final String service, final String command, final String args) throws Exception {
        assert !isStopped();

        final String type = token == null ? "E" : "C";
        final String content = type + "\u0000" + (token != null ? token + "\u0000" : "") + service + "\u0000" + command + "\u0000" + args
                + "\u0000\u0003\u0001";

        final MqttMessage message = new MqttMessage(content.getBytes());
        message.setQos(1);
        mqttClient.publish(getTcfTopic(), message);
    }

    @Override
    public void messageArrived(final String topic, final MqttMessage message) throws MqttException {
        final long startTime = System.currentTimeMillis();
        long currentTime = System.currentTimeMillis();

        while(!getIsInitialized().get() && (currentTime - startTime < INIT_TIMEOUT))
        {
            try {
                Thread.sleep(1000);
                currentTime = System.currentTimeMillis();
            }
            catch (InterruptedException exception) {
                terminate(exception);
                Protocol.log("Cannot consume MQTT message", exception);
            }
        }
        Protocol.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    handleReply(message.getPayload());
                }
                catch (final Exception x) {
                    terminate(x);
                    Protocol.log("Cannot execute MQTT request", x);
                }
            }

            /**
             * Empty byte array used when returning a zero-length byte array on
             * {@code readBytes}
             */
            final byte[] empty_byte_array = new byte[0];

            /**
             * Byte array used as temporary storage of bytes read on
             * {@code readBytes}
             */
            byte[] buf = new byte[1024];

            /**
             * Byte array used as temporary storage of bytes read on
             * {@code readString}
             */
            char[] cbf = new char[1024];

            private final byte[] inp_buf = new byte[0x4000];

            /**
             * Reads bytes from a channel
             *
             * @param end
             *            the first byte character
             * @return a byte array containing all the bytes read
             * @throws IOException
             *             if it finds EOM or EOS reading from input stream
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
                    buf[len++] = (byte) ch;
                }
                if (len == 0) return empty_byte_array;
                byte[] res = new byte[len];
                System.arraycopy(buf, 0, res, 0, len);
                return res;
            }

            /**
             * Reads complete strings made of bytes and return the Java string
             * that it forms
             *
             * @return string containing all the bytes read
             * @throws IOException
             *             if it finds EOM or EOS reading from input stream
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
                     * Duplicate size of array used to hold the bytes, after the
                     * size is bigger than original array
                     */
                    if (len >= cbf.length) {
                        char[] tmp = new char[cbf.length * 2];
                        System.arraycopy(cbf, 0, tmp, 0, len);
                        cbf = tmp;
                    }
                    cbf[len++] = (char) ch;
                }
                return new String(cbf, 0, len);
            }

            private int read() throws IOException {

                for (;;) {
                    while (inp_buf_pos >= inp_buf_len) {
                        inp_buf_len = get(inp_buf);
                        inp_buf_pos = 0;
                        if (inp_buf_len < 0) return EOS;
                    }
                    int res = inp_buf[inp_buf_pos++] & 0xff;
                    if (bin_data_size > 0) {
                        bin_data_size--;
                        return res;
                    }
                    if (res != ESC) return res;
                    while (inp_buf_pos >= inp_buf_len) {
                        inp_buf_len = get(inp_buf);
                        inp_buf_pos = 0;
                        if (inp_buf_len < 0) return EOS;
                    }
                    int n = inp_buf[inp_buf_pos++] & 0xff;
                    switch (n) {
                    case 0:
                        return ESC;
                    case 1:
                        return EOM;
                    case 2:
                        return EOS;
                    case 3:
                        for (int i = 0;; i += 7) {
                            while (inp_buf_pos >= inp_buf_len) {
                                inp_buf_len = get(inp_buf);
                                inp_buf_pos = 0;
                                if (inp_buf_len < 0) return EOS;
                            }
                            int m = inp_buf[inp_buf_pos++] & 0xff;
                            bin_data_size |= (m & 0x7f) << i;
                            if ((m & 0x80) == 0) break;
                        }
                        break;
                    default:
                        throw new IOException("Invalid escape sequence: " + ESC + " " + n);
                    }
                }
            }

            private int get(byte[] buf) throws IOException {
                /* Default implementation - it is expected to be overridden */
                int i = 0;
                while (i < buf.length) {
                    int b = get();
                    if (b < 0) {
                        if (i == 0) return -1;
                        break;
                    }
                    buf[i++] = (byte) b;
                    if (i >= bin_data_size) break;
                }
                return i;
            }

            protected final int get() throws IOException {
                try {
                    if (isStopped()) return -1;
                    return inp.read();
                }
                catch (IOException x) {
                    if (isStopped()) return -1;
                    throw x;
                }
            }

            private void handleReply(byte[] tcfMessage) throws Exception {
                buf = new byte[tcfMessage.length];
                int pos = 0;
                try {
                    final ByteArrayInputStream buffer = new ByteArrayInputStream(tcfMessage);
                    while (pos < buf.length) {
                        final int rd = buffer.read(buf, pos, buf.length - pos);
                        if (rd < 0) {
                            break;
                        }
                        pos += rd;
                    }
                    inp = new ByteArrayInputStream(tcfMessage);
                    buffer.close();
                }
                catch (final IOException ioException) {
                    throw new Exception(ioException);
                }

                int n = read();

                final Message msg = new Message((char) n);
                if (read() != 0) {
                    // TODO error handling
                }
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
                    if (msg.service.equals(ILocator.NAME) && msg.name.equals("Hello") && getState() != STATE_OPENING) {
                        return;
                    }
                    break;
                case 'F':
                    msg.data = readBytes(EOM);
                    break;
                default:
                    throw new Exception("Invalid MQTT reply");
                }
                handleInput(msg);
            }
        });
    }

    @Override
    protected int read() throws IOException {

        while (!isStopped());
        return -1;
    }

    public boolean isStopped() {
        synchronized (stoppedLock) {
            return stopped;
        }
    }

    public void setStopped(boolean stopped) {
        synchronized (stoppedLock) {
            this.stopped = stopped;
        }
    }


    public AtomicBoolean getIsInitialized() {
        return initialized;
    }

    @Override
    public void authPacketArrived(int arg0, MqttProperties arg1)
    {
    }

    @Override
    public void connectComplete(boolean arg0, String arg1)
    {
    }

    @Override
    public void deliveryComplete(IMqttToken arg0)
    {
    }

    @Override
    public void disconnected(MqttDisconnectResponse e)
    {
        Protocol.log("Disconnected from MQTT broker: " + e.getReasonString(), e.getException());
        synchronized (stoppedLock) {
            this.stopped = true;
        }
    }

    @Override
    public void mqttErrorOccurred(MqttException e)
    {
        Protocol.log("MQTT error occurred: " + e.getLocalizedMessage(), e);
    }
}
