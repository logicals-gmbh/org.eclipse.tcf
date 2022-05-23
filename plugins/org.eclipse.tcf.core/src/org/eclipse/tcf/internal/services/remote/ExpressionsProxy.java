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
package org.eclipse.tcf.internal.services.remote;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.tcf.core.Command;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.services.IExpressions;
import org.eclipse.tcf.services.ISymbols.TypeClass;

public class ExpressionsProxy implements IExpressions {

    private final IChannel channel;
    private final Map<ExpressionsListener,IChannel.IEventListener> listeners =
        new HashMap<ExpressionsListener,IChannel.IEventListener>();

    private class Context implements Expression {

        private final Map<String,Object> props;

        Context(Map<String,Object> props) {
            this.props = props;
        }

        public boolean canAssign() {
            Boolean n = (Boolean)props.get(PROP_CAN_ASSIGN);
            if (n == null) return false;
            return n.booleanValue();
        }

        public boolean hasFuncCall() {
            Boolean n = (Boolean)props.get(PROP_HAS_FUNC_CALL);
            if (n == null) return false;
            return n.booleanValue();
        }

        public int getBits() {
            Number n = (Number)props.get(PROP_BITS);
            if (n == null) return 0;
            return n.intValue();
        }

        public String getExpression() {
            return (String)props.get(PROP_EXPRESSION);
        }

        public String getID() {
            return (String)props.get(PROP_ID);
        }

        public String getParentID() {
            return (String)props.get(PROP_PARENT_ID);
        }

        public String getSymbolID() {
            return (String)props.get(PROP_SYMBOL_ID);
        }

        public String getLanguage() {
            return (String)props.get(PROP_LANGUAGE);
        }

        public int getSize() {
            Number n = (Number)props.get(PROP_SIZE);
            if (n == null) return 0;
            return n.intValue();
        }

        public String getTypeID() {
            return (String)props.get(PROP_TYPE);
        }

        public TypeClass getTypeClass() {
            Number n = (Number)props.get(PROP_CLASS);
            if (n != null) return toTypeClass(n.intValue());
            return TypeClass.unknown;
        }

        public Map<String, Object> getProperties() {
            return props;
        }
    }

    private class ContextValue implements Value {

        private final byte[] value;
        private final Map<String,Object> props;

        ContextValue(byte[] value, Map<String,Object> props) {
            if (props == null) props = new HashMap<String,Object>();
            this.value = value;
            this.props = props;
        }

        public Map<String, Object> getProperties() {
            return props;
        }

        public String getTypeID() {
            return (String)props.get(VAL_TYPE);
        }

        public byte[] getValue() {
            return value;
        }

        public String getRegisterID() {
            return (String)props.get(VAL_REGISTER);
        }

        public String getSymbolID() {
            return (String)props.get(VAL_SYMBOL);
        }

        public Number getAddress() {
            return (Number)props.get(VAL_ADDRESS);
        }

        public TypeClass getTypeClass() {
            Number n = (Number)props.get(VAL_CLASS);
            if (n != null) return toTypeClass(n.intValue());
            return TypeClass.unknown;
        }

        public boolean isBigEndian() {
            Boolean n = (Boolean)props.get(VAL_BIG_ENDIAN);
            if (n == null) return false;
            return n.booleanValue();
        }

        public boolean isImplicitPointer() {
            Boolean n = (Boolean)props.get(VAL_IMPLICIT_POINTER);
            if (n == null) return false;
            return n.booleanValue();
        }
    }

    public ExpressionsProxy(IChannel channel) {
        this.channel = channel;
    }

    public IToken assign(String id, byte[] value, final DoneAssign done) {
        return new Command(channel, this, "assign", new Object[]{ id, new JSON.Binary(value, 0, value.length) }) {
            @Override
            public void done(Exception error, Object[] args) {
                if (error == null) {
                    assert args.length == 1;
                    error = toError(args[0]);
                }
                done.doneAssign(token, error);
            }
        }.token;
    }

    public IToken create(String parent_id, String language, String expression, final DoneCreate done) {
        return new Command(channel, this, "create", new Object[]{ parent_id, language, expression }) {
            @Override
            @SuppressWarnings("unchecked")
            public void done(Exception error, Object[] args) {
                Context ctx = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    if (args[1] != null) ctx = new Context((Map<String,Object>)args[1]);
                }
                done.doneCreate(token, error, ctx);
            }
        }.token;
    }

    public IToken createInScope(Map<String,Object> scope, String expression, final DoneCreate done) {
        return new Command(channel, this, "createInScope", new Object[]{ scope, expression }) {
            @Override
            @SuppressWarnings("unchecked")
            public void done(Exception error, Object[] args) {
                Context ctx = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    if (args[1] != null) ctx = new Context((Map<String,Object>)args[1]);
                }
                done.doneCreate(token, error, ctx);
            }
        }.token;
    }

    public IToken dispose(String id, final DoneDispose done) {
        return new Command(channel, this, "dispose", new Object[]{ id }) {
            @Override
            public void done(Exception error, Object[] args) {
                if (error == null) {
                    assert args.length == 1;
                    error = toError(args[0]);
                }
                done.doneDispose(token, error);
            }
        }.token;
    }

    public IToken evaluate(String id, final DoneEvaluate done) {
        return new Command(channel, this, "evaluate", new Object[]{ id }) {
            @SuppressWarnings("unchecked")
            @Override
            public void done(Exception error, Object[] args) {
                byte[] value = null;
                Map<String,Object> props = null;
                if (error == null) {
                    assert args.length == 3;
                    value = JSON.toByteArray(args[0]);
                    error = toError(args[1]);
                    props = (Map<String,Object>)args[2];
                }
                done.doneEvaluate(token, error, new ContextValue(value, props));
            }
        }.token;
    }

    public IToken getChildren(String parent_context_id, final DoneGetChildren done) {
        return new Command(channel, this, "getChildren", new Object[]{ parent_context_id }) {
            @Override
            public void done(Exception error, Object[] args) {
                String[] lst = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    lst = toStringArray(args[1]);
                }
                done.doneGetChildren(token, error, lst);
            }
        }.token;
    }

    public IToken getContext(String id, final DoneGetContext done) {
        return new Command(channel, this, "getContext", new Object[]{ id }) {
            @SuppressWarnings("unchecked")
            @Override
            public void done(Exception error, Object[] args) {
                Context ctx = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    if (args[1] != null) ctx = new Context((Map<String,Object>)args[1]);
                }
                done.doneGetContext(token, error, ctx);
            }
        }.token;
    }

    public String getName() {
        return NAME;
    }

    public void addListener(final ExpressionsListener listener) {
        IChannel.IEventListener l = new IChannel.IEventListener() {

            public void event(String name, byte[] data) {
                try {
                    Object[] args = JSON.parseSequence(data);
                    if (name.equals("valueChanged")) {
                        assert args.length == 1;
                        listener.valueChanged((String)args[0]);
                    }
                    else {
                        throw new IOException("Expressions service: unknown event: " + name);
                    }
                }
                catch (Throwable x) {
                    channel.terminate(x);
                }
            }
        };
        channel.addEventListener(this, l);
        listeners.put(listener, l);
    }

    public void removeListener(ExpressionsListener listener) {
        IChannel.IEventListener l = listeners.remove(listener);
        if (l != null) channel.removeEventListener(this, l);
    }

    @SuppressWarnings("unchecked")
    private String[] toStringArray(Object o) {
        if (o == null) return null;
        Collection<String> c = (Collection<String>)o;
        return (String[])c.toArray(new String[c.size()]);
    }

    private static TypeClass toTypeClass(int n) {
        switch (n) {
        case 1: return TypeClass.cardinal;
        case 2: return TypeClass.integer;
        case 3: return TypeClass.real;
        case 4: return TypeClass.pointer;
        case 5: return TypeClass.array;
        case 6: return TypeClass.composite;
        case 7: return TypeClass.enumeration;
        case 8: return TypeClass.function;
        case 9: return TypeClass.member_pointer;
        case 10: return TypeClass.complex;
        }
        return TypeClass.unknown;
    }
}
