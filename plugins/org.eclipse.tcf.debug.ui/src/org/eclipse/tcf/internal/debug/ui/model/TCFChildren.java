/*******************************************************************************
 * Copyright (c) 2007, 2012 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenUpdate;
import org.eclipse.tcf.debug.ui.ITCFChildren;
import org.eclipse.tcf.debug.ui.ITCFObject;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.util.TCFDataCache;

/**
 * TCFChildren is a concrete type of TCF data cache that is used to cache a list of children.
 */
public abstract class TCFChildren extends TCFDataCache<Map<String,TCFNode>> implements ITCFChildren {

    private final int pool_margin;
    private final Map<String,TCFNode> node_pool = new LinkedHashMap<String,TCFNode>(32, 0.75f, true);

    protected final TCFNode node;

    private static final TCFNode[] EMPTY_NODE_ARRAY = new TCFNode[0];

    private TCFNode[] array;
    private Map<String,ITCFObject> obj_map;

    TCFChildren(TCFNode node) {
        super(node.channel);
        this.node = node;
        pool_margin = 0;
        node.addDataCache(this);
    }

    TCFChildren(TCFNode node, int pool_margin) {
        super(node.channel);
        this.node = node;
        this.pool_margin = pool_margin;
        node.addDataCache(this);
    }

    /**
     * Dispose the cache and all nodes in the nodes pool.
     */
    @Override
    public void dispose() {
        assert !isDisposed();
        node.removeDataCache(this);
        for (TCFNode n : node_pool.values()) n.dispose();
        node_pool.clear();
        super.dispose();
    }

    /**
     * Remove a node from cache.
     * The method is called every time a node is disposed.
     * @param id - node ID
     */
    void onNodeDisposed(String id) {
        node_pool.remove(id);
        if (isValid()) {
            array = null;
            obj_map = null;
            Map<String,TCFNode> data = getData();
            if (data != null) data.remove(id);
        }
    }

    private void addToPool(Map<String,TCFNode> data) {
        assert !isDisposed();
        for (TCFNode n : data.values()) {
            assert data.get(n.id) == n;
            assert n.parent == node;
            node_pool.put(n.id, n);
        }
        if (node_pool.size() > data.size() + pool_margin) {
            String[] arr = node_pool.keySet().toArray(new String[node_pool.size()]);
            for (String id : arr) {
                if (data.get(id) == null) {
                    node_pool.get(id).dispose();
                    if (node_pool.size() <= data.size() + pool_margin) break;
                }
            }
        }
    }

    /**
     * End cache pending state.
     * @param token - pending command handle.
     * @param error - data retrieval error or null
     * @param data - up-to-date map of children nodes
     */
    @Override
    public void set(IToken token, Throwable error, Map<String,TCFNode> data) {
        array = null;
        obj_map = null;
        if (isDisposed()) {
            // A command can return data after the cache element has been disposed.
            // Just ignore the data in such case.
            super.set(token, null, null);
            assert node_pool.isEmpty();
        }
        else if (data != null) {
            super.set(token, error, data);
            addToPool(data);
        }
        else {
            super.set(token, error, new HashMap<String,TCFNode>());
        }
    }

    /**
     * Set given data to the cache, mark cache as valid, cancel any pending data retrieval.
     * @param data - up-to-date data to store in the cache, null means empty collection of nodes.
     */
    @Override
    public void reset(Map<String,TCFNode> data) {
        assert !isDisposed();
        array = null;
        obj_map = null;
        if (data != null) {
            super.reset(data);
            addToPool(data);
        }
        else {
            super.reset(new HashMap<String,TCFNode>());
        }
    }

    /**
     * Invalidate the cache. If retrieval is in progress - let it continue.
     */
    @Override
    public void reset() {
        super.reset();
        array = null;
        obj_map = null;
    }

    /**
     * Force cache to invalid state, cancel pending data retrieval if any.
     */
    @Override
    public void cancel() {
        super.cancel();
        array = null;
        obj_map = null;
    }

    /**
     * Add a node to collection of children.
     * @param n - a node.
     */
    void add(TCFNode n) {
        assert !isDisposed();
        assert !n.isDisposed();
        assert node_pool.get(n.id) == null;
        assert n.parent == node;
        node_pool.put(n.id, n);
        if (isValid()) {
            array = null;
            obj_map = null;
            Map<String,TCFNode> data = getData();
            if (data != null) data.put(n.id, n);
        }
    }

    /**
     * Return collection of all nodes, including current children as well as
     * currently unused nodes from the pool.
     * To get only current children use getData() method.
     * @return Collection of nodes.
     */
    Collection<TCFNode> getNodes() {
        return node_pool.values();
    }

    /**
     * Return current number of children.
     * The cache must be valid for the method to work.
     * @return number of children.
     */
    public int size() {
        assert isValid();
        Map<String,TCFNode> data = getData();
        return data == null ? 0 : data.size();
    }

    /**
     * Return current children nodes as a sorted array.
     * @return array of nodes.
     */
    public TCFNode[] toArray() {
        assert isValid();
        if (array != null) return array;
        Map<String,TCFNode> data = getData();
        if (data == null || data.size() == 0) return array = EMPTY_NODE_ARRAY;
        array = data.values().toArray(new TCFNode[data.size()]);
        Arrays.sort(array);
        return array;
    }

    /**
     * Return current children nodes as a map of ITCFObject.
     * @return map of ITCFObject.
     */
    public Map<String,ITCFObject> getChildren() {
        assert isValid();
        if (obj_map != null) return obj_map;
        Map<String,TCFNode> data = getData();
        obj_map = new HashMap<String,ITCFObject>();
        if (data == null) return obj_map;
        for (TCFNode n : data.values()) obj_map.put(n.id, n);
        return obj_map;
    }

    /**
     * Return current children nodes in IChildrenUpdate object.
     * @param update - children update request object.
     * @param done - a call-back object, it is called when cache state changes.
     * @return true if all done, false if data request is pending.
     */
    boolean getData(IChildrenUpdate update, Runnable done) {
        if (!validate(done)) return false;
        TCFNode[] arr = toArray();
        int offset = 0;
        int r_offset = update.getOffset();
        int r_length = update.getLength();
        for (TCFNode n : arr) {
            if (offset >= r_offset && offset < r_offset + r_length) {
                update.setChild(n, offset);
            }
            offset++;
        }
        return true;
    }
}
