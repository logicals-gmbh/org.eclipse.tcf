/*******************************************************************************
 * Copyright (c) 2007, 2015 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.services.remote;

import java.util.Collection;
import java.util.Map;

import org.eclipse.tcf.core.Command;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.ILineNumbers.CodeArea;
import org.eclipse.tcf.services.ILineNumbers;
import org.eclipse.tcf.services.IStackTrace;


public class StackTraceProxy implements IStackTrace {

    private final IChannel channel;

    private class Context implements StackTraceContext {

        private final Map<String,Object> props;

        Context(Map<String,Object> props) {
            this.props = props;
        }

        public Number getArgumentsAddress() {
            return (Number)props.get(PROP_ARGUMENTS_ADDRESS);
        }

        public int getArgumentsCount() {
            Number n = (Number)props.get(PROP_ARGUMENTS_COUNT);
            if (n == null) return 0;
            return n.intValue();
        }

        public Number getFrameAddress() {
            return (Number)props.get(PROP_FRAME_ADDRESS);
        }

        public String getID() {
            return (String)props.get(PROP_ID);
        }

        public String getName() {
            return (String)props.get(PROP_NAME);
        }

        public String getParentID() {
            return (String)props.get(PROP_PARENT_ID);
        }

        public Number getReturnAddress() {
            return (Number)props.get(PROP_RETURN_ADDRESS);
        }

        public Number getInstructionAddress() {
            return (Number)props.get(PROP_INSTRUCTION_ADDRESS);
        }

        public CodeArea getCodeArea() {
            @SuppressWarnings("unchecked")
            Map<String,Object> area = (Map<String,Object>)props.get(PROP_CODE_AREA);
            if (area == null) return null;
            return new ILineNumbers.CodeArea(area, null);
        }

        public String getFuncID() {
            return (String)props.get(PROP_FUNC_ID);
        }

        public Map<String, Object> getProperties() {
            return props;
        }
    }

    public StackTraceProxy(IChannel channel) {
        this.channel = channel;
    }

    public IToken getChildren(String parent_context_id, final DoneGetChildren done) {
        return new Command(channel, this, "getChildren", new Object[]{ parent_context_id }) {
            @Override
            public void done(Exception error, Object[] args) {
                String[] arr = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    arr = toStringArray(args[1]);
                }
                done.doneGetChildren(token, error, arr);
            }
        }.token;
    }

    public IToken getChildrenRange(String parent_context_id, int range_start, int range_end, final DoneGetChildren done) {
        return new Command(channel, this, "getChildrenRange", new Object[]{ parent_context_id, range_start, range_end }) {
            @Override
            public void done(Exception error, Object[] args) {
                String[] arr = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    arr = toStringArray(args[1]);
                }
                done.doneGetChildren(token, error, arr);
            }
        }.token;
    }

    public IToken getContext(String[] id, final DoneGetContext done) {
        return new Command(channel, this, "getContext", new Object[]{ id }) {
            @Override
            public void done(Exception error, Object[] args) {
                StackTraceContext[] arr = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[1]);
                    arr = toContextArray(args[0]);
                }
                done.doneGetContext(token, error, arr);
            }
        }.token;
    }

    public String getName() {
        return NAME;
    }

    @SuppressWarnings("unchecked")
    private StackTraceContext[] toContextArray(Object o) {
        if (o == null) return null;
        Collection<Map<String,Object>> c = (Collection<Map<String,Object>>)o;
        int n = 0;
        StackTraceContext[] ctx = new StackTraceContext[c.size()];
        for (Map<String,Object> m : c) {
            ctx[n++] = m != null ? new Context(m) : null;
        }
        return ctx;
    }

    @SuppressWarnings("unchecked")
    private String[] toStringArray(Object o) {
        if (o == null) return null;
        Collection<String> c = (Collection<String>)o;
        return (String[])c.toArray(new String[c.size()]);
    }
}
