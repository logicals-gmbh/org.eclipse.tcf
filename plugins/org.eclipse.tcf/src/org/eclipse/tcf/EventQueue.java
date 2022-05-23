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
package org.eclipse.tcf;

import java.util.LinkedList;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.tcf.protocol.IEventQueue;
import org.eclipse.tcf.protocol.Protocol;


/**
 * Implementation of Target Communication Framework event queue.
 * This implementation is intended for Eclipse environment.
 */
class EventQueue implements IEventQueue, Runnable {

    private final LinkedList<Runnable> queue = new LinkedList<Runnable>();
    private final Thread thread;
    private boolean waiting;
    private boolean shutdown;
    private int job_cnt;

    EventQueue() {
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.setName("TCF Event Dispatcher"); //$NON-NLS-1$
        // Need to monitor jobs to detect congestion
        Job.getJobManager().addJobChangeListener(new IJobChangeListener() {

            public void aboutToRun(IJobChangeEvent event) {
            }

            public void awake(IJobChangeEvent event) {
            }

            public void done(IJobChangeEvent event) {
                job_cnt--;
            }

            public void running(IJobChangeEvent event) {
            }

            public void scheduled(IJobChangeEvent event) {
                job_cnt++;
            }

            public void sleeping(IJobChangeEvent event) {
            }
        });
    }

    void start() {
        thread.start();
    }

    void shutdown() {
        try {
            synchronized (this) {
                shutdown = true;
                if (waiting) {
                    waiting = false;
                    notifyAll();
                }
            }
            thread.join();
        }
        catch (InterruptedException e) {
        }
        catch (Throwable e) {
            Protocol.log("Failed to shutdown TCF event dispatch thread", e); //$NON-NLS-1$
        }
    }

    public void run() {
        for (;;) {
            try {
                Runnable r = null;
                synchronized (this) {
                    while (queue.size() == 0) {
                        if (shutdown) return;
                        waiting = true;
                        wait();
                    }
                    r = queue.removeFirst();
                }
                r.run();
            }
            catch (Throwable x) {
                Protocol.log("Unhandled exception in TCF event dispatch", x); //$NON-NLS-1$
            }
        }
    }

    public synchronized void invokeLater(final Runnable r) {
        assert r != null;
        if (shutdown) throw new IllegalStateException("TCF event dispatcher has shut down"); //$NON-NLS-1$
        queue.add(r);
        if (waiting) {
            waiting = false;
            notifyAll();
        }
    }

    public boolean isDispatchThread() {
        return Thread.currentThread() == thread;
    }

    public synchronized int getCongestion() {
        if (Job.getJobManager().isIdle()) job_cnt = 0;
        int l0 = job_cnt / 10 - 100;
        int l1 = queue.size() / 10 - 100;
        if (l1 > l0) l0 = l1;
        if (l0 > 100) l0 = 100;
        return l0;
    }
}
