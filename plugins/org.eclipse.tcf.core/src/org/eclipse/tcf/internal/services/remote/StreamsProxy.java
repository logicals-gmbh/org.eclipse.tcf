/*******************************************************************************
 * Copyright (c) 2009, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.services.remote;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.tcf.core.Command;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.services.IStreams;

public class StreamsProxy implements IStreams {

    private final IChannel channel;
    private final Map<String,IChannel.IEventListener> listeners =
        new HashMap<String,IChannel.IEventListener>();

    public StreamsProxy(IChannel channel) {
        this.channel = channel;
    }

    public IToken connect(String stream_id, final DoneConnect done) {
        return new Command(channel, this, "connect", new Object[]{ stream_id }) {
            @Override
            public void done(Exception error, Object[] args) {
                if (error == null) {
                    assert args.length == 1;
                    error = toError(args[0]);
                }
                done.doneConnect(token, error);
            }
        }.token;
    }

    public IToken disconnect(String stream_id, final DoneDisconnect done) {
        return new Command(channel, this, "disconnect", new Object[]{ stream_id }) {
            @Override
            public void done(Exception error, Object[] args) {
                if (error == null) {
                    assert args.length == 1;
                    error = toError(args[0]);
                }
                done.doneDisconnect(token, error);
            }
        }.token;
    }

    public IToken eos(String stream_id, final DoneEOS done) {
        return new Command(channel, this, "eos", new Object[]{ stream_id }) {
            @Override
            public void done(Exception error, Object[] args) {
                if (error == null) {
                    assert args.length == 1;
                    error = toError(args[0]);
                }
                done.doneEOS(token, error);
            }
        }.token;
    }

    public IToken read(String stream_id, int size, final DoneRead done) {
        return new Command(channel, this, "read", new Object[]{ stream_id, size }) {
            @Override
            public void done(Exception error, Object[] args) {
                int lost_size = 0;
                byte data[] = null;
                boolean eos = false;
                if (error == null) {
                    assert args.length == 4;
                    data = JSON.toByteArray(args[0]);
                    error = toError(args[1]);
                    lost_size = ((Number)args[2]).intValue();
                    eos = ((Boolean)args[3]).booleanValue();
                }
                done.doneRead(token, error, lost_size, data, eos);
            }
        }.token;
    }

    public IToken subscribe(final String stream_type, final StreamsListener listener, final DoneSubscribe done) {
        return new Command(channel, this, "subscribe", new Object[]{ stream_type }) {
            @Override
            public void done(Exception error, Object[] args) {
                if (error == null) {
                    assert args.length == 1;
                    error = toError(args[0]);
                }
                if (error == null) {
                    IChannel.IEventListener l = new IChannel.IEventListener() {

                        public void event(String name, byte[] data) {
                            try {
                                assert listeners.get(stream_type) == this;
                                Object[] args = JSON.parseSequence(data);
                                if (stream_type.equals(args[0])) {
                                    if (name.equals("created")) {
                                        if (args.length == 3) {
                                            listener.created((String)args[0], (String)args[1], (String)args[2]);
                                        }
                                        else {
                                            assert args.length == 2;
                                            listener.created((String)args[0], (String)args[1], null);
                                        }
                                    }
                                    else if (name.equals("disposed")) {
                                        assert args.length == 2;
                                        listener.disposed((String)args[0], (String)args[1]);
                                    }
                                    else {
                                        throw new IOException("Streams service: unknown event: " + name);
                                    }
                                }
                            }
                            catch (Throwable x) {
                                channel.terminate(x);
                            }
                        }
                    };
                    assert listeners.get(stream_type) == null;
                    listeners.put(stream_type, l);
                    channel.addEventListener(StreamsProxy.this, l);
                }
                done.doneSubscribe(token, error);
            }
        }.token;
    }

    public IToken unsubscribe(final String stream_type, final StreamsListener listener, final DoneUnsubscribe done) {
        return new Command(channel, this, "unsubscribe", new Object[]{ stream_type }) {
            @Override
            public void done(Exception error, Object[] args) {
                if (error == null) {
                    assert args.length == 1;
                    error = toError(args[0]);
                }
                if (error == null) {
                    IChannel.IEventListener l = listeners.remove(stream_type);
                    if (l != null) channel.removeEventListener(StreamsProxy.this, l);
                }
                done.doneUnsubscribe(token, error);
            }
        }.token;
    }

    public IToken write(String stream_id, byte[] buf, int offset, int size, final DoneWrite done) {
        return new Command(channel, this, "write", new Object[]{ stream_id, size, new JSON.Binary(buf, offset, size) }) {
            @Override
            public void done(Exception error, Object[] args) {
                if (error == null) {
                    assert args.length == 1;
                    error = toError(args[0]);
                }
                done.doneWrite(token, error);
            }
        }.token;
    }

    public String getName() {
        return NAME;
    }
}
