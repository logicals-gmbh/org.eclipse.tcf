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
package org.eclipse.tcf.internal.debug.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IMemoryMap;
import org.eclipse.tcf.services.IPathMap;

/**
 * TCF Test Suite implements stress testing of communication channels and capabilities of remote peer.
 * It is intended to be used before starting a debug session for a first time to make sure the selected
 * target is stable and reliable.
 */
public class TCFTestSuite {

    final static int NUM_CHANNELS = 4;

    private final TestListener listener;
    private final IChannel[] channels;
    private final Map<IChannel,RunControl> run_controls = new HashMap<IChannel, RunControl>();
    private final LinkedList<Runnable> pending_tests = new LinkedList<Runnable>();
    private final Collection<Throwable> errors = new ArrayList<Throwable>();
    private final Map<ITCFTest,IChannel> active_tests = new HashMap<ITCFTest,IChannel>();
    private final static HashMap<String,String> cancel_test_ids = new HashMap<String,String>();

    private int count_total;
    private int count_done;

    boolean cancel;
    boolean canceled;
    boolean target_lock;

    public interface TestListener {
        public void progress(String label, int done, int total);
        public void done(Collection<Throwable> errors);
    }

    public TCFTestSuite(final IPeer peer, final TestListener listener, final List<IPathMap.PathMapRule> path_map,
            final Map<String,ArrayList<IMemoryMap.MemoryRegion>> mem_map) throws IOException {
        this.listener = listener;
        pending_tests.add(new Runnable() {
            public void run() {
                listener.progress("Running Echo Test...", count_done++, count_total);
                for (IChannel channel : channels) {
                    active_tests.put(new TestEcho(TCFTestSuite.this, channel), channel);
                }
            }
        });
        pending_tests.add(new Runnable() {
            public void run() {
                listener.progress("Running Echo FP Test...", count_done++, count_total);
                for (IChannel channel : channels) {
                    active_tests.put(new TestEchoFP(TCFTestSuite.this, channel), channel);
                }
            }
        });
        pending_tests.add(new Runnable() {
            public void run() {
                listener.progress("Running Echo INT Test...", count_done++, count_total);
                for (IChannel channel : channels) {
                    active_tests.put(new TestEchoINT(TCFTestSuite.this, channel), channel);
                }
            }
        });
        pending_tests.add(new Runnable() {
            public void run() {
                listener.progress("Running Echo ERR Test...", count_done++, count_total);
                for (IChannel channel : channels) {
                    active_tests.put(new TestEchoERR(TCFTestSuite.this, channel), channel);
                }
            }
        });
        pending_tests.add(new Runnable() {
            public void run() {
                listener.progress("Running Path Map Test...", count_done++, count_total);
                for (IChannel channel : channels) {
                    active_tests.put(new TestPathMap(TCFTestSuite.this, channel, path_map), channel);
                }
            }
        });
        pending_tests.add(new Runnable() {
            public void run() {
                listener.progress("Running Debugger Attach/Terminate Test...", count_done++, count_total);
                for (IChannel channel : channels) {
                    active_tests.put(new TestAttachTerminate(TCFTestSuite.this, run_controls.get(channel), channel), channel);
                }
            }
        });
        pending_tests.add(new Runnable() {
            public void run() {
                listener.progress("Running Expressions Test...", count_done++, count_total);
                for (IChannel channel : channels) {
                    active_tests.put(new TestExpressions(TCFTestSuite.this, run_controls.get(channel), channel, mem_map), channel);
                }
            }
        });
        pending_tests.add(new Runnable() {
            public void run() {
                listener.progress("Running Streams Test...", count_done++, count_total);
                for (IChannel channel : channels) {
                    active_tests.put(new TestStreams(TCFTestSuite.this, channel), channel);
                }
            }
        });
        pending_tests.add(new Runnable() {
            public void run() {
                listener.progress("Running Sys monitor Test...", count_done++, count_total);
                for (IChannel channel : channels) {
                    active_tests.put(new TestSysMonitor(TCFTestSuite.this, channel), channel);
                }
            }
        });
        pending_tests.add(new Runnable() {
            public void run() {
                listener.progress("Running Terminals Test...", count_done++, count_total);
                for (IChannel channel : channels) {
                    active_tests.put(new TestTerminals(TCFTestSuite.this, channel), channel);
                }
            }
        });
        pending_tests.add(new Runnable() {
            public void run() {
                int i = 0;
                listener.progress("Running Run Control Test...", count_done++, count_total);
                for (IChannel channel : channels) {
                    active_tests.put(new TestRCBP1(TCFTestSuite.this, run_controls.get(channel),
                            channel, i++, path_map, mem_map), channel);
                }
            }
        });
        pending_tests.add(new Runnable() {
            public void run() {
                int i = 0;
                listener.progress("Running File System Test...", count_done++, count_total);
                for (IChannel channel : channels) {
                    active_tests.put(new TestFileSystem(TCFTestSuite.this, channel, i++), channel);
                }
            }
        });
        pending_tests.add(new Runnable() {
            public void run() {
                listener.progress("Running Interability Test...", count_done++, count_total);
                Random rnd = new Random();
                for (int i = 0; i < channels.length; i++) {
                    IChannel channel = channels[i];
                    ITCFTest test = null;
                    switch (rnd.nextInt(11)) {
                    case 0: test = new TestEcho(TCFTestSuite.this, channel); break;
                    case 1: test = new TestEchoERR(TCFTestSuite.this, channel); break;
                    case 2: test = new TestEchoFP(TCFTestSuite.this, channel); break;
                    case 3: test = new TestAttachTerminate(TCFTestSuite.this, run_controls.get(channel), channel); break;
                    case 4: test = new TestExpressions(TCFTestSuite.this, run_controls.get(channel), channel, mem_map); break;
                    case 5: test = new TestRCBP1(TCFTestSuite.this, run_controls.get(channel), channel, i, path_map, mem_map); break;
                    case 6: test = new TestFileSystem(TCFTestSuite.this, channel, i); break;
                    case 7: test = new TestPathMap(TCFTestSuite.this, channel, path_map); break;
                    case 8: test = new TestStreams(TCFTestSuite.this, channel); break;
                    case 9: test = new TestSysMonitor(TCFTestSuite.this, channel); break;
                    case 10: test = new TestTerminals(TCFTestSuite.this, channel); break;
                    }
                    active_tests.put(test, channel);
                }
            }
        });
        count_total = pending_tests.size() * (NUM_CHANNELS + 1);
        channels = new IChannel[NUM_CHANNELS];
        Protocol.invokeLater(new Runnable() {
            public void run() {
                try {
                    openChannels(peer);
                }
                catch (Throwable x) {
                    errors.add(x);
                    int cnt = 0;
                    for (int i = 0; i < channels.length; i++) {
                        if (channels[i] == null) continue;
                        if (channels[i].getState() != IChannel.STATE_CLOSED) channels[i].close();
                        cnt++;
                    }
                    if (cnt == 0) listener.done(errors);
                }
            }
        });
    }

    private void openChannels(IPeer peer) {
        listener.progress("Opening communication channels...", count_done, count_total);
        for (int i = 0; i < channels.length; i++) {
            final IChannel channel = channels[i] = peer.openChannel();
            channel.addChannelListener(new IChannel.IChannelListener() {

                public void onChannelOpened() {
                    for (int i = 0; i < channels.length; i++) {
                        if (channels[i] == null) return;
                        if (channels[i].getState() != IChannel.STATE_OPEN) return;
                    }
                    for (int i = 0; i < channels.length; i++) {
                        run_controls.put(channels[i], new RunControl(TCFTestSuite.this, channels[i], i));
                    }
                    runNextTest();
                }

                public void congestionLevel(int level) {
                }

                public void onChannelClosed(Throwable error) {
                    channel.removeChannelListener(this);
                    if (error == null && errors.isEmpty() && (!active_tests.isEmpty() || !pending_tests.isEmpty()) && !cancel) {
                        error = new IOException("Remote peer closed connection before all tests finished");
                    }
                    int cnt = 0;
                    for (int i = 0; i < channels.length; i++) {
                        if (channels[i] == channel) {
                            channels[i] = null;
                            if (error != null && errors.isEmpty()) errors.add(error);
                            for (Iterator<ITCFTest> n = active_tests.keySet().iterator(); n.hasNext();) {
                                if (active_tests.get(n.next()) == channel) n.remove();
                            }
                        }
                        if (channels[i] == null) continue;
                        if ((error != null || active_tests.isEmpty() && pending_tests.isEmpty()) &&
                                channels[i].getState() != IChannel.STATE_CLOSED) channels[i].close();
                        cnt++;
                    }
                    if (cnt == 0) listener.done(errors);
                }
            });
        }
    }

    public void cancel() {
        cancel = true;
        if (canceled) return;
        for (final ITCFTest t : active_tests.keySet()) {
            if (t instanceof TestRCBP1) {
                ((TestRCBP1)t).cancel(new Runnable() {
                    public void run() {
                        assert active_tests.get(t) == null;
                        cancel();
                    }
                });
                return;
            }
        }
        canceled = true;
        for (IChannel c : channels) {
            if (c != null && c.getState() != IChannel.STATE_CLOSED) c.close();
        }
    }

    public boolean isCanceled() {
        return canceled;
    }

    boolean isActive(ITCFTest test) {
        return active_tests.get(test) != null;
    }

    Collection<ITCFTest> getActiveTests() {
        return active_tests.keySet();
    }

    ITCFTest getActiveTest(IChannel channel) {
        for (Map.Entry<ITCFTest,IChannel> e : active_tests.entrySet()) {
            if (e.getValue() == channel) return e.getKey();
        }
        return null;
    }

    Map<String,String> getCanceledTests() {
        return cancel_test_ids;
    }

    boolean canResume(String id) {
        for (RunControl r : run_controls.values()) {
            if (!r.canResume(id)) return false;
        }
        for (ITCFTest t : active_tests.keySet()) {
            if (!t.canResume(id)) return false;
        }
        return true;
    }

    void done(ITCFTest test, Throwable error) {
        assert active_tests.get(test) != null;
        if (error != null && !canceled) errors.add(error);
        active_tests.remove(test);
        listener.progress(null, count_done++, count_total);
        if (active_tests.isEmpty()) runNextTest();
    }

    private void runNextTest() {
        while (active_tests.isEmpty()) {
            if (cancel || errors.size() > 0 || pending_tests.size() == 0) {
                for (IChannel channel : channels) {
                    if (channel != null && channel.getState() != IChannel.STATE_CLOSED) {
                        if (errors.size() > 0) channel.terminate(new Exception("Test failed"));
                        else channel.close();
                    }
                }
                return;
            }
            pending_tests.removeFirst().run();
            ITCFTest[] lst = active_tests.keySet().toArray(new ITCFTest[active_tests.size()]);
            for (ITCFTest test : lst) test.start();
        }
    }
}
