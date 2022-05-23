/*******************************************************************************
 * Copyright (c) 2008, 2014 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.util;

import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;

/**
 * Objects of this class are used to cache TCF remote data.
 * The cache is asynchronous state machine. The states are:
 *  1. Valid - cache is in sync with remote data, use getError() and getData() to get cached data;
 *  2. Invalid - cache is out of sync, start data retrieval by calling validate();
 *  3. Pending - cache is waiting result of a command that was sent to remote peer;
 *  4. Disposed - cache is disposed and cannot be used to store data.
 *
 * A cache instance can be created on any data type that needs to be cached.
 * Examples might be context children list, context properties, context state, memory data,
 * register data, symbol, variable, etc.
 * Clients of cache items can register for cache changes, but don't need to think about any particular events
 * since that is handled by the cache item itself.
 *
 * A typical cache client should implement Runnable interface.
 * The implementation of run() method should:
 *
 * Validate all cache items required for client task.
 * If anything is invalid then client should not alter any shared data structures,
 * should discard any intermediate results and register (wait) for changes of invalid cache instance(s) state.
 * When cache item state changes, client is invoked again and full validation is restarted.
 * Once everything is valid, client completes its task in a single dispatch cycle.
 *
 * Note: clients should never retain copies of remote data across dispatch cycles!
 * Such data would get out of sync and compromise data consistency.
 * All remote data and everything derived from remote data should be kept in cache items
 * that implement proper event handling and can keep data consistent across dispatch cycles.
 *
 * @param <V> - type of data to be stored in the cache.
 */
public abstract class TCFDataCache<V> implements Runnable {

    private final int WAITING_LIST_SIZE = 8;

    private Throwable error;
    private boolean valid;
    private boolean posted;
    private boolean disposed;
    private V data;

    protected final IChannel channel;
    protected IToken command;

    private Runnable[] waiting_list = null;
    private int waiting_cnt;

    public TCFDataCache(IChannel channel) {
        assert channel != null;
        this.channel = channel;
    }

    /**
     * @since 1.2
     */
    public void post() {
        if (posted) return;
        if (waiting_cnt == 0) return;
        Protocol.invokeLater(this);
        posted = true;
    }

    /**
     * @return true if cache contains up-to-date data or error.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * @return true if data retrieval command is in progress.
     */
    public boolean isPending() {
        return command != null;
    }

    /**
     * @return true if cache is disposed.
     */
    public boolean isDisposed() {
        return disposed;
    }

    /**
     * @return error object if data retrieval ended with an error, or null if retrieval was successful.
     * Note: It is prohibited to call this method when cache is not valid.
     */
    public Throwable getError() {
        assert valid;
        return error;
    }

    /**
     * @return cached data object.
     * Note: It is prohibited to call this method when cache is not valid.
     */
    public V getData() {
        assert Protocol.isDispatchThread();
        assert valid;
        return data;
    }

    /**
     * Notify waiting clients about cache state change and remove them from wait list.
     * It is responsibility of clients to check if the state change was one they are waiting for.
     * Clients are not intended to call this method, they can call post() if necessary.
     */
    public final void run() {
        assert Protocol.isDispatchThread();
        posted = false;
        if (waiting_cnt > 0) {
            int cnt = waiting_cnt;
            Runnable[] arr = waiting_list;
            waiting_list = null;
            waiting_cnt = 0;
            for (int i = 0; i < cnt; i++) {
                Runnable r = arr[i];
                if (r instanceof TCFDataCache<?>) {
                    TCFDataCache<?> c = (TCFDataCache<?>)r;
                    if (!c.posted && c.waiting_cnt > 0) {
                        Protocol.invokeLater(c);
                        c.posted = true;
                    }
                }
                else if (r instanceof TCFTask<?>) {
                    TCFTask<?> t = (TCFTask<?>)r;
                    if (!t.isDone()) t.run();
                }
                else {
                    r.run();
                }
                arr[i] = null;
            }
            if (waiting_list == null && arr.length <= WAITING_LIST_SIZE) waiting_list = arr;
        }
    }

    /**
     * Add a client call-back to cache wait list.
     * Client call-backs are activated when cache state changes.
     * Call-backs are removed from waiting list after that.
     * It is responsibility of clients to check if the state change was one they are waiting for.
     * @param cb - a call-back object
     */
    public void wait(Runnable cb) {
        assert Protocol.isDispatchThread();
        assert !disposed;
        assert !valid;
        if (cb != null && !isWaiting(cb)) {
            if (waiting_list == null) waiting_list = new Runnable[WAITING_LIST_SIZE];
            if (waiting_cnt >= waiting_list.length) {
                Runnable[] tmp = new Runnable[waiting_cnt * 2];
                System.arraycopy(waiting_list, 0, tmp, 0, waiting_list.length);
                waiting_list = tmp;
            }
            waiting_list[waiting_cnt++] = cb;
        }
    }

    /**
     * Return true if a client call-back is waiting for state changes of this cache item.
     * @param cb - a call-back object.
     * @return true if 'cb' is in the wait list.
     */
    public boolean isWaiting(Runnable cb) {
        for (int i = 0; i < waiting_cnt; i++) {
            if (waiting_list[i] == cb) return true;
        }
        return false;
    }

    /**
     * Initiate data retrieval if the cache is not valid.
     * @return true if the cache is already valid
     */
    public boolean validate() {
        assert Protocol.isDispatchThread();
        if (disposed || channel.getState() != IChannel.STATE_OPEN) {
            command = null;
            valid = true;
            error = null;
            data = null;
        }
        else {
            if (command != null) return false;
            if (!valid && !startDataRetrieval()) return false;
        }
        assert valid;
        assert command == null;
        post();
        return true;
    }

    /**
     * If the cache is not valid, initiate data retrieval and
     * add a client call-back to cache wait list.
     * Client call-backs are activated when cache state changes.
     * Call-backs are removed from waiting list after that.
     * It is responsibility of clients to check if the state change was one they are waiting for.
     * If the cache is valid do nothing and return true.
     * @param cb - a call-back object
     * @return true if the cache is already valid
     */
    public boolean validate(Runnable cb) {
        if (!validate()) {
            wait(cb);
            return false;
        }
        return true;
    }

    /**
     * Start cache pending state.
     * Pending state is when the cache is waiting for a TCF command to return results.
     * @param command - TCF command handle.
     */
    public void start(IToken command) {
        assert !valid;
        assert command != null;
        assert this.command == null;
        this.command = command;
    }

    /**
     * End cache pending state, but not mark the cache as valid.
     * @param command - TCF command handle.
     */
    public void done(IToken command) {
        if (this.command != command) return;
        assert !valid;
        this.command = null;
        post();
    }

    /**
     * End cache pending state and mark the cache as valid.
     * If 'token' != null, the data represent results from a completed command.
     * The data should be ignored if current cache pending command is not same as 'token'.
     * It can happen if the cache was reset or canceled during data retrieval.
     * @param token - pending command handle or null.
     * @param error - data retrieval error or null
     * @param data - up-to-date data object
     */
    public void set(IToken token, Throwable error, V data) {
        assert Protocol.isDispatchThread();
        if (token != null && command != token) return;
        command = null;
        if (!disposed) {
            assert !valid;
            if (channel.getState() != IChannel.STATE_OPEN) {
                error = null;
                data = null;
            }
            this.error = error;
            this.data = data;
            valid = true;
        }
        post();
    }

    /**
     * Force cache to become valid, cancel pending data retrieval if any.
     * @param data - up-to-date data object
     */
    public void reset(V data) {
        assert Protocol.isDispatchThread();
        if (command != null) {
            command.cancel();
            command = null;
        }
        if (!disposed) {
            this.data = data;
            error = null;
            valid = true;
        }
        post();
    }

    /**
     * Invalidate the cache.
     * If retrieval is in progress - let it continue.
     */
    public void reset() {
        assert Protocol.isDispatchThread();
        if (!disposed) {
            error = null;
            valid = false;
            data = null;
        }
        post();
    }

    /**
     * Invalidate the cache.
     * Cancel pending data retrieval if any.
     */
    public void cancel() {
        reset();
        if (command != null) {
            command.cancel();
            command = null;
        }
    }

    /**
     * Dispose the cache.
     * Cancel pending data retrieval if any.
     */
    public void dispose() {
        cancel();
        valid = true;
        disposed = true;
    }

    @Override
    public String toString() {
        StringBuffer bf = new StringBuffer();
        bf.append('[');
        if (valid) bf.append("valid,");
        if (disposed) bf.append("disposed,");
        if (posted) bf.append("posted,");
        if (error != null) bf.append("error,");
        bf.append("data=");
        bf.append(data == null ? "null" : data.toString());
        bf.append(']');
        return bf.toString();
    }

    /**
     * Sub-classes should override this method to implement actual data retrieval logic.
     * @return true is all done, false if retrieval is in progress.
     */
    protected abstract boolean startDataRetrieval();
}
