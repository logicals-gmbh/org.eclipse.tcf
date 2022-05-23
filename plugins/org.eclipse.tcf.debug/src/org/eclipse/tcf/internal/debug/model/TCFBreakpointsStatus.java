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
package org.eclipse.tcf.internal.debug.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IBreakpoints;


public class TCFBreakpointsStatus {

    private final IBreakpoints service;
    private final Map<String,Map<String,Object>> breakpoints = new HashMap<String,Map<String,Object>>();
    private final Map<String,Map<String,Object>> status = new HashMap<String,Map<String,Object>>();
    private final Set<ITCFBreakpointListener> listeners = new HashSet<ITCFBreakpointListener>();

    private static final Map<String,Object> status_not_supported = new HashMap<String,Object>();

    static {
        status_not_supported.put(IBreakpoints.STATUS_ERROR, "Not supported");
    }

    TCFBreakpointsStatus(TCFLaunch launch) {
        assert Protocol.isDispatchThread();
        service = launch.getChannel().getRemoteService(IBreakpoints.class);
        if (service != null) {
            final IBreakpoints.BreakpointsListener listener = new IBreakpoints.BreakpointsListener() {

                public void breakpointStatusChanged(String id, Map<String,Object> m) {
                    assert Protocol.isDispatchThread();
                    if (status.get(id) == null) return;
                    status.put(id, m);
                    for (Iterator<ITCFBreakpointListener> i = listeners.iterator(); i.hasNext();) {
                        i.next().breakpointStatusChanged(id);
                    }
                }

                public void contextAdded(Map<String,Object>[] bps) {
                    for (Map<String,Object> bp : bps) {
                        String id = (String)bp.get(IBreakpoints.PROP_ID);
                        breakpoints.put(id, bp);
                        if (status.get(id) != null) continue;
                        status.put(id, new HashMap<String,Object>());
                        for (Iterator<ITCFBreakpointListener> i = listeners.iterator(); i.hasNext();) {
                            i.next().breakpointStatusChanged(id);
                        }
                    }
                }

                public void contextChanged(Map<String,Object>[] bps) {
                    for (Map<String,Object> bp : bps) {
                        String id = (String)bp.get(IBreakpoints.PROP_ID);
                        breakpoints.put(id, bp);
                        if (!status.containsKey(id)) continue;
                        for (Iterator<ITCFBreakpointListener> i = listeners.iterator(); i.hasNext();) {
                            i.next().breakpointChanged(id);
                        }
                    }
                }

                public void contextRemoved(String[] ids) {
                    for (String id : ids) {
                        breakpoints.remove(id);
                        if (!status.containsKey(id)) continue;
                        for (Iterator<ITCFBreakpointListener> i = listeners.iterator(); i.hasNext();) {
                            i.next().breakpointRemoved(id);
                        }
                        status.remove(id);
                    }
                }
            };
            service.addListener(listener);

            // query foreign breakpoints
            service.getIDs(new IBreakpoints.DoneGetIDs() {
                @SuppressWarnings("unchecked")
                public void doneGetIDs(IToken token, Exception error, String[] ids) {
                    if (error != null || ids == null) return;
                    for (final String id : ids) {
                        service.getProperties(id, new IBreakpoints.DoneGetProperties() {
                            public void doneGetProperties(IToken token, Exception error, Map<String,Object> props) {
                                if (error == null) {
                                    listener.contextAdded((Map<String,Object>[]) new Map[] { props });
                                    service.getStatus(id, new IBreakpoints.DoneGetStatus() {
                                        public void doneGetStatus(IToken token, Exception error, Map<String,Object> status) {
                                            if (error == null) listener.breakpointStatusChanged(id, status);
                                        }
                                    });
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    public Set<String> getStatusIDs() {
        return status.keySet();
    }

    public Map<String,Object> getStatus(String id) {
        assert id != null;
        assert Protocol.isDispatchThread();
        if (service == null) return status_not_supported;
        return status.get(id);
    }

    public Map<String,Object> getProperties(String id) {
        assert id != null;
        assert Protocol.isDispatchThread();
        return breakpoints.get(id);
    }

    public Map<String,Object> getStatus(IBreakpoint bp) {
        try {
            String id = TCFBreakpointsModel.getBreakpointID(bp);
            return getStatus(id);
        }
        catch (CoreException e) {
            return status_not_supported;
        }
    }

    public void addListener(ITCFBreakpointListener listener) {
        assert Protocol.isDispatchThread();
        listeners.add(listener);
    }

    public void removeListener(ITCFBreakpointListener listener) {
        assert Protocol.isDispatchThread();
        listeners.remove(listener);
    }
}
