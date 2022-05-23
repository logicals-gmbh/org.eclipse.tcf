/*******************************************************************************
 * Copyright (c) 2010, 2015 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.actions;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.tcf.internal.debug.model.TCFContextState;
import org.eclipse.tcf.internal.debug.model.TCFLaunch;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IBreakpoints;
import org.eclipse.tcf.services.IRunControl;
import org.eclipse.tcf.services.IStackTrace;
import org.eclipse.tcf.services.IRunControl.RunControlContext;
import org.eclipse.tcf.util.TCFDataCache;

@Deprecated
public abstract class TCFActionStepOut extends TCFAction implements IRunControl.RunControlListener {

    private final boolean step_back;
    private final IRunControl rc = launch.getService(IRunControl.class);
    private final IBreakpoints bps = launch.getService(IBreakpoints.class);

    private IRunControl.RunControlContext ctx;
    private TCFDataCache<TCFContextState> state;
    private int step_cnt;
    private Map<String,Object> bp;

    protected boolean exited;

    public TCFActionStepOut(TCFLaunch launch, IRunControl.RunControlContext ctx, boolean step_back) {
        super(launch, ctx.getID());
        this.ctx = ctx;
        this.step_back = step_back;
    }

    protected abstract TCFDataCache<TCFContextState> getContextState();
    protected abstract TCFDataCache<?> getStackTrace();
    protected abstract TCFDataCache<IStackTrace.StackTraceContext> getStackFrame();
    protected abstract int getStackFrameIndex();

    public void run() {
        if (exited) return;
        try {
            runAction();
        }
        catch (Throwable x) {
            exit(x);
        }
    }

    private void runAction() {
        if (aborted) {
            exit(null);
            return;
        }
        if (state == null) {
            rc.addListener(this);
            state = getContextState();
            if (state == null) {
                exit(new Exception("Invalid context ID"));
                return;
            }
        }
        if (!state.validate(this)) return;
        if (state.getData() == null || !state.getData().is_suspended) {
            Throwable error = state.getError();
            if (error == null) error = new Exception("Context is not suspended");
            exit(error);
            return;
        }
        int mode = step_back ? IRunControl.RM_REVERSE_STEP_OUT : IRunControl.RM_STEP_OUT;
        if (ctx.canResume(mode)) {
            if (step_cnt > 0) {
                exit(null);
                return;
            }
            ctx.resume(mode, 1, new IRunControl.DoneCommand() {
                public void doneCommand(IToken token, Exception error) {
                    if (error != null) exit(error);
                }
            });
            step_cnt++;
            return;
        }
        TCFDataCache<?> stack_trace = getStackTrace();
        if (!stack_trace.validate(this)) return;
        int frame_index = getStackFrameIndex();
        if (step_cnt > 0) {
            TCFContextState state_data = state.getData();
            boolean ok = isMyBreakpoint(state_data) || IRunControl.REASON_STEP.equals(state_data.suspend_reason);
            if (!ok) exit(null, state_data.suspend_reason);
            else if (frame_index < 0) exit(null);
            if (exited) return;
        }
        if (bps != null && ctx.canResume(step_back ? IRunControl.RM_REVERSE_RESUME : IRunControl.RM_RESUME)) {
            if (bp == null) {
                TCFDataCache<IStackTrace.StackTraceContext> frame = getStackFrame();
                if (!frame.validate(this)) return;
                Number addr = null;
                if (frame.getData() != null) addr = frame.getData().getReturnAddress();
                if (addr == null) {
                    exit(new Exception("Unknown stack frame return address"));
                    return;
                }
                if (step_back) {
                    BigInteger n = JSON.toBigInteger(addr);
                    addr = n.subtract(BigInteger.valueOf(1));
                }
                String id = STEP_BREAKPOINT_PREFIX + ctx.getID();
                bp = new HashMap<String,Object>();
                bp.put(IBreakpoints.PROP_ID, id);
                bp.put(IBreakpoints.PROP_LOCATION, addr.toString());
                bp.put(IBreakpoints.PROP_CONDITION, "$thread==\"" + ctx.getID() + "\"");
                bp.put(IBreakpoints.PROP_ENABLED, Boolean.TRUE);
                bps.add(bp, new IBreakpoints.DoneCommand() {
                    public void doneCommand(IToken token, Exception error) {
                        if (error != null) exit(error);
                    }
                });
            }
            ctx.resume(step_back ? IRunControl.RM_REVERSE_RESUME : IRunControl.RM_RESUME, 1, new IRunControl.DoneCommand() {
                public void doneCommand(IToken token, Exception error) {
                    if (error != null) exit(error);
                }
            });
            step_cnt++;
            return;
        }
        exit(new Exception("Step out is not supported"));
    }

    protected void exit(Throwable error) {
        exit(error, "Step Out");
    }

    protected void exit(Throwable error, String reason) {
        if (exited) return;
        if (bp != null) {
            bps.remove(new String[]{ (String)bp.get(IBreakpoints.PROP_ID) }, new IBreakpoints.DoneCommand() {
                public void doneCommand(IToken token, Exception error) {
                }
            });
        }
        rc.removeListener(this);
        exited = true;
        if (error == null) setActionResult(getContextID(), reason);
        else launch.removeContextActions(getContextID());
        done();
    }

    public void containerResumed(String[] context_ids) {
    }

    public void containerSuspended(String context, String pc,
            String reason, Map<String, Object> params,
            String[] suspended_ids) {
        for (String id : suspended_ids) {
            if (!id.equals(context)) contextSuspended(id, null, null, null);
        }
        contextSuspended(context, pc, reason, params);
    }

    public void contextAdded(RunControlContext[] contexts) {
    }

    public void contextChanged(RunControlContext[] contexts) {
        for (RunControlContext c : contexts) {
            if (c.getID().equals(ctx.getID())) ctx = c;
        }
    }

    public void contextException(String context, String msg) {
        if (context.equals(ctx.getID())) exit(new Exception(msg));
    }

    public void contextRemoved(String[] context_ids) {
        for (String context : context_ids) {
            if (context.equals(ctx.getID())) exit(null);
        }
    }

    public void contextResumed(String context) {
    }

    public void contextSuspended(String context, String pc, String reason, Map<String,Object> params) {
        if (!context.equals(ctx.getID())) return;
        Protocol.invokeLater(this);
    }

    private boolean isMyBreakpoint(TCFContextState state_data) {
        if (bp == null) return false;
        if (!IRunControl.REASON_BREAKPOINT.equals(state_data.suspend_reason)) return false;
        if (state_data.suspend_params != null) {
            Object ids = state_data.suspend_params.get(IRunControl.STATE_BREAKPOINT_IDS);
            if (ids != null) {
                @SuppressWarnings("unchecked")
                Collection<String> c = (Collection<String>)ids;
                if (c.contains(bp.get(IBreakpoints.PROP_ID))) return true;
            }
        }
        if (state_data.suspend_pc == null) return false;
        BigInteger x = new BigInteger(state_data.suspend_pc);
        BigInteger y = new BigInteger((String)bp.get(IBreakpoints.PROP_LOCATION));
        return x.equals(y);
    }
}
