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
package org.eclipse.tcf.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;

/**
 * ChannelPIPE is a IChannel implementation that works on top of named pipes as a transport.
 */
public class ChannelPIPE extends StreamChannel {

    private InputStream inp;
    private OutputStream out;
    private boolean started;
    private boolean closed;

    public ChannelPIPE(IPeer remote_peer, String name) {
        super(remote_peer);
        try {
            inp = new BufferedInputStream(new FileInputStream(name));
            byte[] buf = new byte[0x400];
            int rd = inp.read(buf);
            if (rd <= 0 || buf[rd - 1] != 0) throw new Exception("Invalid remote peer responce");
            out = new BufferedOutputStream(new FileOutputStream(new String(buf, 0, rd - 1, "UTF-8")));
            onConnected(null);
        }
        catch (Exception x) {
            onConnected(x);
        }
    }

    private void onConnected(final Throwable x) {
        Protocol.invokeLater(new Runnable() {
            public void run() {
                if (x != null) {
                    terminate(x);
                    closed = true;
                }
                if (closed) {
                    try {
                        if (out != null) out.close();
                        if (inp != null) inp.close();
                    }
                    catch (IOException y) {
                        Protocol.log("Cannot close pipe", y);
                    }
                }
                else {
                    started = true;
                    start();
                }
            }
        });
    }

    @Override
    protected final int get() throws IOException {
        try {
            if (closed) return -1;
            return inp.read();
        }
        catch (IOException x) {
            if (closed) return -1;
            throw x;
        }
    }

    @Override
    protected final int get(byte[] buf) throws IOException {
        try {
            if (closed) return -1;
            return inp.read(buf);
        }
        catch (IOException x) {
            if (closed) return -1;
            throw x;
        }
    }

    @Override
    protected final void put(int b) throws IOException {
        assert b >= 0 && b <= 0xff;
        if (closed) return;
        out.write(b);
    }

    @Override
    protected final void put(byte[] buf) throws IOException {
        if (closed) return;
        out.write(buf);
    }

    @Override
    protected final void flush() throws IOException {
        if (closed) return;
        out.flush();
    }

    @Override
    protected void stop() throws IOException {
        closed = true;
        if (started) {
            out.close();
            inp.close();
        }
    }
}
