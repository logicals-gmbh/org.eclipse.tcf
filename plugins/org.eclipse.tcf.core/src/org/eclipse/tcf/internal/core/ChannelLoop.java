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

import java.io.IOException;
import java.io.InterruptedIOException;

import org.eclipse.tcf.core.StreamChannel;
import org.eclipse.tcf.protocol.IPeer;

/**
 * Simulates a loopback connection, which feeds a received signal back to
 * the sender
 *
 */
public class ChannelLoop extends StreamChannel {

    private final byte[] buf = new byte[0x1000];
    private int buf_inp;
    private int buf_out;
    private boolean waiting;
    private boolean closed;

    ChannelLoop(IPeer peer) {
        super(peer);
        start();
    }

    @Override
    protected synchronized int get() throws IOException {
        try {
            while (buf_inp == buf_out) {
                if (closed) return -1;
                waiting = true;
                wait();
            }
            int b = buf[buf_out] & 0xff;
            buf_out = (buf_out + 1) % buf.length;
            if (waiting) {
                waiting = false;
                notifyAll();
            }
            return b;
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
    }

    @Override
    protected synchronized void put(int b) throws IOException {
        assert b >=0  && b <= 0xff;
        try {
            for (;;) {
                int nxt_inp = (buf_inp + 1) % buf.length;
                if (nxt_inp != buf_out) {
                    buf[buf_inp] = (byte)b;
                    buf_inp = nxt_inp;
                    break;
                }
                if (closed) return;
                waiting = true;
                wait();
            }
            if (waiting) {
                waiting = false;
                notifyAll();
            }
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
    }

    @Override
    protected void flush() throws IOException {
    }

    @Override
    protected synchronized void stop() throws IOException {
        closed = true;
        if (waiting) {
            waiting = false;
            notifyAll();
        }
    }
}
