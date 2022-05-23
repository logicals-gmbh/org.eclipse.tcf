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
package org.eclipse.tcf.internal.services.local;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import org.eclipse.tcf.internal.core.Token;
import org.eclipse.tcf.core.Command;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IErrorReport;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IDiagnostics;


public class DiagnosticsService implements IDiagnostics {

    private final IChannel channel;

    private class CommandServer implements IChannel.ICommandServer {

        public void command(IToken token, String name, byte[] data) {
            try {
                command(token, name, JSON.parseSequence(data));
            }
            catch (Throwable x) {
                channel.terminate(x);
            }
        }

        private void command(IToken token, String name, Object[] args) throws Exception {
            if (name.equals("echo")) {
                if (args.length != 1) throw new Exception("Invalid number of arguments");
                String s = (String)args[0];
                channel.sendResult(token, JSON.toJSONSequence(new Object[]{ s }));
            }
            else if (name.equals("echoFP")) {
                if (args.length != 1) throw new Exception("Invalid number of arguments");
                Number n = (Number)args[0];
                channel.sendResult(token, JSON.toJSONSequence(new Object[]{ n }));
            }
            else if (name.equals("echoINT")) {
                if (args.length != 2) throw new Exception("Invalid number of arguments");
                Number n = (Number)args[1];
                channel.sendResult(token, JSON.toJSONSequence(new Object[]{ n }));
            }
            else if (name.equals("echoERR")) {
                if (args.length != 1) throw new Exception("Invalid number of arguments");
                @SuppressWarnings("unchecked")
                Map<String,Object> err = (Map<String,Object>)args[0];
                channel.sendResult(token, JSON.toJSONSequence(new Object[]{ err, Command.toErrorString(err) }));
            }
            else if (name.equals("getTestList")) {
                if (args.length != 0) throw new Exception("Invalid number of arguments");
                channel.sendResult(token, JSON.toJSONSequence(new Object[]{ null, new String[0] }));
            }
            else {
                channel.rejectCommand(token);
            }
        }
    }

    public DiagnosticsService(IChannel channel) {
        this.channel = channel;
        channel.addCommandServer(this, new CommandServer());
    }

    public String getName() {
        return NAME;
    }

    public IToken echo(final String s, final DoneEcho done) {
        final IToken token = new Token();
        Protocol.invokeLater(new Runnable() {
            public void run() {
                done.doneEcho(token, null, s);
            }
        });
        return token;
    }

    public IToken echoFP(final BigDecimal n, final DoneEchoFP done) {
        final IToken token = new Token();
        Protocol.invokeLater(new Runnable() {
            public void run() {
                done.doneEchoFP(token, null, n);
            }
        });
        return token;
    }

    public IToken echoINT(int t, final BigInteger n, final DoneEchoINT done) {
        final IToken token = new Token();
        Protocol.invokeLater(new Runnable() {
            public void run() {
                done.doneEchoINT(token, null, n);
            }
        });
        return token;
    }

    public IToken echoERR(final Throwable err, final DoneEchoERR done) {
        final IToken token = new Token();
        Protocol.invokeLater(new Runnable() {
            public void run() {
                if (err instanceof IErrorReport) {
                    done.doneEchoERR(token, null, err, Command.toErrorString(((IErrorReport)err).getAttributes()));
                }
                else {
                    done.doneEchoERR(token, null, err, err.getMessage());
                }
            }
        });
        return token;
    }

    public IToken getTestList(final DoneGetTestList done) {
        final IToken token = new Token();
        Protocol.invokeLater(new Runnable() {
            public void run() {
                done.doneGetTestList(token, null, new String[0]);
            }
        });
        return token;
    }

    public IToken runTest(final String s, final DoneRunTest done) {
        final IToken token = new Token();
        Protocol.invokeLater(new Runnable() {
            public void run() {
                done.doneRunTest(token, new Exception("Test suite not found: " + s), null);
            }
        });
        return token;
    }

    public IToken cancelTest(String context_id, final DoneCancelTest done) {
        final IToken token = new Token();
        Protocol.invokeLater(new Runnable() {
            public void run() {
                done.doneCancelTest(token, null);
            }
        });
        return token;
    }

    public IToken getSymbol(String context_id, String symbol_name, final DoneGetSymbol done) {
        final IToken token = new Token();
        Protocol.invokeLater(new Runnable() {
            public void run() {
                done.doneGetSymbol(token, new Exception("Invalid context"), null);
            }
        });
        return token;
    }

    public IToken createTestStreams(int inp_buf_size, int out_buf_size, final DoneCreateTestStreams done) {
        final IToken token = new Token();
        Protocol.invokeLater(new Runnable() {
            public void run() {
                done.doneCreateTestStreams(token, new Exception("Not implemented"), null, null);
            }
        });
        return token;
    }

    public IToken disposeTestStream(String id, final DoneDisposeTestStream done) {
        final IToken token = new Token();
        Protocol.invokeLater(new Runnable() {
            public void run() {
                done.doneDisposeTestStream(token, new Exception("Invalid context"));
            }
        });
        return token;
    }

    public IToken not_implemented_command(final DoneNotImplementedCommand done) {
        final IToken token = new Token();
        Protocol.invokeLater(new Runnable() {
            public void run() {
                done.doneNotImplementedCommand(token, new Exception("Not implemented"));
            }
        });
        return token;
    }
}
