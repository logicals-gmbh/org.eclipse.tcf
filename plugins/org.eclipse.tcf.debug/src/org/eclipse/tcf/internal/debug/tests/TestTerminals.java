/*******************************************************************************
 * Copyright (c) 2011-2020 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.tests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IProcesses;
import org.eclipse.tcf.services.IStreams;
import org.eclipse.tcf.services.ITerminals;
import org.eclipse.tcf.services.ITerminals.TerminalContext;

class TestTerminals implements ITCFTest {

    private final TCFTestSuite test_suite;
    private final ITerminals terminals;
    private final IProcesses processes;
    private final IStreams streams;
    private final Random rnd = new Random();
    private final HashSet<String> stream_ids = new HashSet<String>();
    private final StringBuffer stdout_buf = new StringBuffer();
    private final StringBuffer stderr_buf = new StringBuffer();
    private final HashSet<IToken> disconnect_cmds = new HashSet<IToken>();
    private final List<String> echo_tx = new ArrayList<String>();
    private final List<Integer> echo_rx = new ArrayList<Integer>();

    private final int echo_cnt = 50;

    private IStreams.StreamsListener streams_listener;
    private IStreams.DoneRead stdout_read;
    private IStreams.DoneRead stderr_read;
    private String encoding;
    private TerminalContext terminal;
    private TerminalContext get_ctx;
    private Map<String,String> environment;
    private boolean delay_done;
    private Collection<Map<String,Object>> signal_list;
    private IToken get_signals_cmd;
    private IToken signal_cmd;
    private boolean signal_sent;
    private int signal_code;
    private IToken unsubscribe_cmd;
    private boolean unsubscribe_done;
    private boolean exited;
    private boolean stdout_eos;
    private int timer = 0;

    private final ITerminals.TerminalsListener listener = new ITerminals.TerminalsListener() {

        public void exited(String id, int exit_code) {
            if (terminal != null && id.equals(terminal.getID())) {
                exited = true;
                if (!signal_sent) {
                    exit(new Exception("Terminal exited with code " + exit_code));
                }
                else {
                    run();
                }
            }
        }

        public void winSizeChanged(String id, int w, int h) {
        }
    };

    private final IStreams.DoneDisconnect disconnect_done = new IStreams.DoneDisconnect() {
        public void doneDisconnect(IToken token, Exception error) {
            assert disconnect_cmds.contains(token);
            disconnect_cmds.remove(token);
            if (error != null) exit(error);
            if (disconnect_cmds.size() == 0 && unsubscribe_done) exit(null);
        }
    };

    TestTerminals(TCFTestSuite test_suite, IChannel channel) {
        this.test_suite = test_suite;
        terminals = channel.getRemoteService(ITerminals.class);
        processes = channel.getRemoteService(IProcesses.class);
        streams = channel.getRemoteService(IStreams.class);
    }

    public void start() {
        if (terminals == null || streams == null) {
            test_suite.done(this, null);
        }
        else {
            terminals.addListener(listener);
            run();
        }
    }

    private void run() {
        if (environment == null && processes != null) {
            processes.getEnvironment(new IProcesses.DoneGetEnvironment() {
                public void doneGetEnvironment(IToken token, Exception error, Map<String, String> environment) {
                    if (error != null) {
                        exit(error);
                    }
                    else if (environment == null) {
                        exit(new Exception("Default process environment must not be null"));
                    }
                    else {
                        TestTerminals.this.environment = environment;
                        run();
                    }
                }
            });
            return;
        }
        if (streams_listener == null) {
            final IStreams.StreamsListener l = new IStreams.StreamsListener() {
                public void created(String stream_type, String stream_id, String context_id) {
                    if (!terminals.getName().equals(stream_type)) {
                        exit(new Exception("Invalid stream type in Streams.created event: " + stream_type));
                    }
                    else if (stream_id == null || stream_id.length() == 0 || stream_ids.contains(stream_id)) {
                        exit(new Exception("Invalid stream ID in Streams.created event: " + stream_id));
                    }
                    else if (terminal != null) {
                        if (stream_id.equals(terminal.getStdInID()) ||
                                stream_id.equals(terminal.getStdOutID()) ||
                                stream_id.equals(terminal.getStdErrID())) {
                            exit(new Exception("Invalid stream ID in Streams.created event: " + stream_id));
                        }
                        else {
                            disconnect_cmds.add(streams.disconnect(stream_id, disconnect_done));
                        }
                    }
                    else {
                        stream_ids.add(stream_id);
                    }
                }
                public void disposed(String stream_type, String stream_id) {
                }
            };
            streams.subscribe(terminals.getName(), l, new IStreams.DoneSubscribe() {
                public void doneSubscribe(IToken token, Exception error) {
                    if (error != null) {
                        exit(error);
                    }
                    else {
                        streams_listener = l;
                        run();
                    }
                }
            });
            return;
        }
        if (terminal == null) {
            String[] types = { "ansi", "vt100", null };
            String[] langs = { "en_US", "en_US.UTF-8", null };
            String[] env = null;
            if (environment != null && rnd.nextBoolean()) {
                int i = 0;
                env = new String[environment.size() + 1];
                for (String s : environment.keySet()) {
                    env[i++] = s + "=" + environment.get(s);
                }
                env[i++] = "TCF_FOO=BAR";
            }
            terminals.launch(types[rnd.nextInt(types.length)], langs[rnd.nextInt(langs.length)], env, new ITerminals.DoneLaunch() {
                public void doneLaunch(IToken token, Exception error, final TerminalContext terminal) {
                    if (error != null) {
                        exit(error);
                    }
                    else if (terminal == null) {
                        exit(new Exception("Terminal context must not be null"));
                    }
                    else if (terminal.getID() == null) {
                        exit(new Exception("Terminal context ID must not be null"));
                    }
                    else {
                        TestTerminals.this.terminal = terminal;
                        for (Iterator<String> i = stream_ids.iterator(); i.hasNext();) {
                            String stream_id = i.next();
                            if (stream_id.equals(terminal.getStdInID()) ||
                                    stream_id.equals(terminal.getStdOutID()) ||
                                    stream_id.equals(terminal.getStdErrID())) {
                                // keep connected
                            }
                            else {
                                i.remove();
                                disconnect_cmds.add(streams.disconnect(stream_id, disconnect_done));
                            }
                        }
                        Protocol.invokeLater(1000, new Runnable() {
                            public void run() {
                                if (!test_suite.isActive(TestTerminals.this)) return;
                                timer++;
                                if (test_suite.cancel) {
                                    exit(null);
                                }
                                else if (timer < 600) {
                                    Protocol.invokeLater(1000, this);
                                }
                                else if (!signal_sent) {
                                    if (signal_cmd == null) {
                                        exit(new Error("Timeout waiting for terminal reply. Context: " + terminal.getID()));
                                    }
                                    else {
                                        exit(new Error("Timeout waiting for terminal terminate command. Context: " + terminal.getID()));
                                    }
                                }
                                else if (!exited) {
                                    if (signal_code == 0) {
                                        exit(new Error("Timeout waiting for 'Terminals.exited' event after " +
                                                "Terminals.exit command. Context: " + terminal.getID()));
                                    }
                                    else {
                                        exit(new Error("Timeout waiting for 'Terminals.exited' event after " +
                                                "Processes.signal(" + signal_code + ") command. Context: " + terminal.getID()));
                                    }
                                }
                                else {
                                    exit(new Error("Timeout waiting for end-of-stream. Context: " + terminal.getID()));
                                }
                            }
                        });
                        run();
                    }
                }
            });
            return;
        }
        if (get_ctx == null) {
            terminals.getContext(terminal.getID(), new ITerminals.DoneGetContext() {
                public void doneGetContext(IToken token, Exception error, TerminalContext terminal) {
                    if (error != null) {
                        exit(error);
                    }
                    else if (terminal == null) {
                        exit(new Exception("Terminal context must not be null"));
                    }
                    else if (terminal.getID() == null) {
                        exit(new Exception("Terminal context ID must not be null"));
                    }
                    else if (!TestTerminals.this.terminal.getProperties().equals(terminal.getProperties())) {
                        exit(new Exception("Invalid result of Terminal.getContext"));
                    }
                    else {
                        TestTerminals.this.get_ctx = terminal;
                        run();
                    }
                }
            });
            return;
        }
        if (signal_list == null && processes != null && terminal.getProcessID() != null) {
            assert get_signals_cmd == null;
            get_signals_cmd = processes.getSignalList(terminal.getProcessID(), new IProcesses.DoneGetSignalList() {
                public void doneGetSignalList(IToken token, Exception error, Collection<Map<String,Object>> list) {
                    assert get_signals_cmd == token;
                    get_signals_cmd  = null;
                    if (error != null) {
                        exit(error);
                    }
                    else if (list == null) {
                        exit(new Exception("Signal list must not be null"));
                    }
                    else {
                        signal_list = list;
                        run();
                    }
                }
            });
            return;
        }
        if (encoding == null) {
            String lang = terminal.getEncoding();
            if (lang == null && environment != null) lang = environment.get("LC_ALL");
            if (lang == null && environment != null) lang = environment.get("LANG");
            if (lang == null) lang = "en_US.UTF-8";
            int i = lang.indexOf('.');
            int j = lang.indexOf('@');
            if (i < 0) {
                encoding = "UTF-8";
            }
            else if (j < i) {
                encoding = lang.substring(i + 1);
            }
            else {
                encoding = lang.substring(i + 1, j);
            }
        }
        if (stdout_read == null) {
            final String id = terminal.getStdOutID();
            if (id == null) {
                exit(new Exception("stdout stream ID is null"));
                return;
            }
            stdout_read = new IStreams.DoneRead() {
                public void doneRead(IToken token, Exception error, int lost_size, byte[] data, boolean eos) {
                    if (error != null) {
                        exit(error);
                    }
                    else if (lost_size > 0) {
                        exit(new Exception("Lost bytes in terminal stream"));
                    }
                    else {
                        try {
                            boolean run = false;
                            if (data != null) {
                                // System.out.println(new String(data, encoding));
                                stdout_buf.append(new String(data, encoding));
                                if (echo_tx.size() > echo_rx.size()) {
                                    String s = echo_tx.get(echo_rx.size());
                                    String p = s.substring(0, 12);
                                    int n = echo_rx.size() > 0 ? echo_rx.get(echo_rx.size() - 1) : 0;
                                    for (int j = 0; j < 2; j++) {
                                        String b = j == 0 ? "\r\n" : "\r";
                                        int i = stdout_buf.indexOf(b + p, n);
                                        if (i >= 0 && stdout_buf.length() >= i + b.length() + s.length() + 1) {
                                            echo_rx.add(i + b.length());
                                            run = true;
                                            timer = 0;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (!eos) {
                                streams.read(id, 0x1000, this);
                            }
                            else {
                                stdout_eos = true;
                                run = true;
                            }
                            if (run) run();
                        }
                        catch (Exception x) {
                            exit(x);
                        }
                    }
                }
            };
            streams.read(id, 0x1000, stdout_read);
        }
        if (stderr_read == null && terminal.getStdErrID() != null) {
            final String id = terminal.getStdErrID();
            stderr_read = new IStreams.DoneRead() {
                public void doneRead(IToken token, Exception error, int lost_size, byte[] data, boolean eos) {
                    if (error != null) {
                        exit(error);
                    }
                    else if (lost_size > 0) {
                        exit(new Exception("Lost bytes in terminal stream"));
                    }
                    else {
                        try {
                            if (data != null) {
                                // System.err.println(new String(data, encoding));
                                stderr_buf.append(new String(data, encoding));
                            }
                            if (!eos) streams.read(id, 0x1000, this);
                        }
                        catch (Exception x) {
                            exit(x);
                        }
                    }
                }
            };
            int n = rnd.nextInt(4) + 1;
            for (int i = 0; i < n; i++) {
                streams.read(id, 0x1000, stderr_read);
            }
        }
        if (!delay_done) {
            final int n = stdout_buf.length();
            Protocol.invokeLater(rnd.nextInt(250), new Runnable() {
                public void run() {
                    if (n > 1 && stdout_buf.length() == n &&
                            stdout_buf.charAt(n - 1) != '\n') delay_done = true;
                    TestTerminals.this.run();
                }
            });
            return;
        }
        if (echo_tx.size() < echo_cnt && echo_rx.size() == echo_tx.size()) {
            try {
                StringBuffer bf = new StringBuffer();
                for (int i = 0; i < 0x40; i++) {
                    bf.append((char)('A' + rnd.nextInt('Z' - 'A' + 1)));
                    bf.append((char)('a' + rnd.nextInt('Z' - 'A' + 1)));
                }
                String s = bf.toString();
                echo_tx.add(s);
                s = "echo " + s + '\n';
                byte[] buf = s.getBytes(encoding);
                streams.write(terminal.getStdInID(), buf, 0, buf.length, new IStreams.DoneWrite() {
                    public void doneWrite(IToken token, Exception error) {
                        if (error != null) {
                            exit(error);
                        }
                        else {
                            run();
                        }
                    }
                });
            }
            catch (Exception x) {
                exit(x);
            }
            return;
        }
        if (!exited && !stdout_eos && echo_rx.size() < echo_cnt) return;
        if (!signal_sent) {
            assert !exited;
            if (signal_cmd == null) {
                int code = 0;
                if (signal_list != null && rnd.nextBoolean()) {
                    for (Map<String,Object> m : signal_list) {
                        String nm = (String)m.get(IProcesses.SIG_NAME);
                        if (nm != null && nm.equals("SIGKILL")) {
                            Number n = (Number)m.get(IProcesses.SIG_CODE);
                            if (n != null) code = n.intValue();
                        }
                    }
                    if (code == 0) {
                        for (Map<String,Object> m : signal_list) {
                            String nm = (String)m.get(IProcesses.SIG_NAME);
                            if (nm != null && nm.equals("SIGTERM")) {
                                Number n = (Number)m.get(IProcesses.SIG_CODE);
                                if (n != null) code = n.intValue();
                            }
                        }
                    }
                }
                signal_code = code;
                if (code > 0) {
                    signal_cmd = processes.signal(terminal.getProcessID(), code, new IProcesses.DoneCommand() {
                        public void doneCommand(IToken token, Exception error) {
                            assert signal_cmd == token;
                            signal_cmd = null;
                            if (error != null) {
                                exit(error);
                            }
                            else {
                                timer = 0;
                                signal_sent = true;
                                run();
                            }
                        }
                    });
                }
                else {
                    signal_cmd = terminals.exit(terminal.getID(), new ITerminals.DoneCommand() {
                        public void doneCommand(IToken token, Exception error) {
                            assert signal_cmd == token;
                            signal_cmd = null;
                            if (error != null) {
                                exit(error);
                            }
                            else {
                                timer = 0;
                                signal_sent = true;
                                run();
                            }
                        }
                    });
                }
                timer = 0;
            }
            return;
        }
        if (exited && stdout_eos) {
            if (!unsubscribe_done) {
                if (unsubscribe_cmd == null) {
                    unsubscribe_cmd = streams.unsubscribe(terminals.getName(), streams_listener, new IStreams.DoneUnsubscribe() {
                        public void doneUnsubscribe(IToken token, Exception error) {
                            unsubscribe_done = true;
                            if (error != null) exit(error);
                            else run();
                        }
                    });
                }
                return;
            }
            else {
                for (String stream_id : stream_ids) {
                    disconnect_cmds.add(streams.disconnect(stream_id, disconnect_done));
                }
                stream_ids.clear();
                checkTerminalOutput(stdout_buf);
                checkTerminalOutput(stderr_buf);
                if (echo_rx.size() < echo_cnt) {
                    exit(new Exception("Terminal exited before test finished"));
                }
                else {
                    int n = 0;
                    for (int i : echo_rx) {
                        String s = echo_tx.get(n++);
                        String r = stdout_buf.substring(i, i + s.length());
                        if (!s.equals(r)) {
                            exit(new Exception("Invalid reply: " + r + "\nExpected: " + s));
                        }
                    }
                }
            }
        }
    }

    private void checkTerminalOutput(StringBuffer bf) {
        if (bf.indexOf("Cannot start") >= 0) {
            exit(new Exception("Unexpected terminal output:\n" + bf));
        }
    }

    private void exit(Throwable x) {
        if (!test_suite.isActive(this)) return;
        if (terminals != null) terminals.removeListener(listener);
        test_suite.done(this, x);
    }

    public boolean canResume(String id) {
        return true;
    }
}
