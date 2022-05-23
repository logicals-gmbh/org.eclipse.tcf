/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others.
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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.tcf.core.Command;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.services.ISymbols;

public class SymbolsProxy implements ISymbols {

    private final IChannel channel;

    private class Context implements Symbol {

        private final byte[] value;
        private final Map<String,Object> props;

        Context(Map<String,Object> props) {
            this.props = props;
            value = JSON.toByteArray(props.get(PROP_VALUE));
        }

        public String getOwnerID() {
            return (String)props.get(PROP_OWNER_ID);
        }

        public int getUpdatePolicy() {
            Number n = (Number)props.get(PROP_UPDATE_POLICY);
            if (n == null) return 0;
            return n.intValue();
        }

        public Number getAddress() {
            return (Number)props.get(PROP_ADDRESS);
        }

        public String getBaseTypeID() {
            return (String)props.get(PROP_BASE_TYPE_ID);
        }

        public String getID() {
            return (String)props.get(PROP_ID);
        }

        public String getIndexTypeID() {
            return (String)props.get(PROP_INDEX_TYPE_ID);
        }

        public String getContainerID() {
            return (String)props.get(PROP_CONTAINER_ID);
        }

        public int getLength() {
            Number n = (Number)props.get(PROP_LENGTH);
            if (n == null) return 0;
            return n.intValue();
        }

        public Number getLowerBound() {
            return (Number)props.get(PROP_LOWER_BOUND);
        }

        public Number getUpperBound() {
            return (Number)props.get(PROP_UPPER_BOUND);
        }

        public Number getBitStride() {
            return (Number)props.get(PROP_BIT_STRIDE);
        }

        public String getName() {
            return (String)props.get(PROP_NAME);
        }

        public int getOffset() {
            Number n = (Number)props.get(PROP_OFFSET);
            if (n == null) return 0;
            return n.intValue();
        }

        public Map<String,Object> getProperties() {
            return props;
        }

        public int getSize() {
            Number n = (Number)props.get(PROP_SIZE);
            if (n == null) return 0;
            return n.intValue();
        }

        public SymbolClass getSymbolClass() {
            Number n = (Number)props.get(PROP_SYMBOL_CLASS);
            if (n != null) {
                switch (n.intValue()) {
                case 1: return SymbolClass.value;
                case 2: return SymbolClass.reference;
                case 3: return SymbolClass.function;
                case 4: return SymbolClass.type;
                case 5: return SymbolClass.comp_unit;
                case 6: return SymbolClass.block;
                case 7: return SymbolClass.namespace;
                case 8: return SymbolClass.variant_part;
                case 9: return SymbolClass.variant;
                }
            }
            return SymbolClass.unknown;
        }

        public TypeClass getTypeClass() {
            Number n = (Number)props.get(PROP_TYPE_CLASS);
            if (n != null) {
                switch (n.intValue()) {
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
            }
            return TypeClass.unknown;
        }

        public String getTypeID() {
            return (String)props.get(PROP_TYPE_ID);
        }

        public byte[] getValue() {
            return value;
        }

        public boolean isBigEndian() {
            Boolean b = (Boolean)props.get(PROP_BIG_ENDIAN);
            return b != null && b.booleanValue();
        }

        public String getRegisterID() {
            return (String)props.get(PROP_REGISTER);
        }

        public int getFlags() {
            Number n = (Number)props.get(PROP_FLAGS);
            if (n == null) return 0;
            return n.intValue();
        }

        public boolean getFlag(int flag) {
            Number n = (Number)props.get(PROP_FLAGS);
            if (n == null) return false;
            return (n.intValue() & flag) != 0;
        }
    }

    public SymbolsProxy(IChannel channel) {
        this.channel = channel;
    }

    public String getName() {
        return NAME;
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

    public IToken find(String context_id, Number ip, String name, final DoneFind done) {
        return new Command(channel, this, "find", new Object[]{ context_id, ip, name }) {
            @Override
            public void done(Exception error, Object[] args) {
                String id = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    id = (String)args[1];
                }
                done.doneFind(token, error, id);
            }
        }.token;
    }

    public IToken findByName(String context_id, Number ip, String name, final DoneFindAll done) {
        return new Command(channel, this, "findByName", new Object[]{ context_id, ip, name }) {
            @Override
            public void done(Exception error, Object[] args) {
                String[] ids = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    ids = toStringArray(args[1]);
                }
                done.doneFind(token, error, ids);
            }
        }.token;
    }

    public IToken findInScope(String context_id, Number ip, String scope_id, String name, final DoneFindAll done) {
        return new Command(channel, this, "findInScope", new Object[]{ context_id, ip, scope_id, name }) {
            @Override
            public void done(Exception error, Object[] args) {
                String[] ids = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    ids = toStringArray(args[1]);
                }
                done.doneFind(token, error, ids);
            }
        }.token;
    }

    public IToken findByAddr(String context_id, Number addr, final DoneFind done) {
        return new Command(channel, this, "findByAddr", new Object[]{ context_id, addr }) {
            @SuppressWarnings("rawtypes")
            @Override
            public void done(Exception error, Object[] args) {
                String id = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    if (args[1] instanceof Collection)
                        id = (String)((Collection) args[1]).iterator().next();
                    else
                        id = (String)args[1];
                }
                done.doneFind(token, error, id);
            }
        }.token;
    }

    public IToken findByAddr(String context_id, Number addr, final DoneFindAll done) {
        return new Command(channel, this, "findByAddr", new Object[]{ context_id, addr }) {
            @Override
            public void done(Exception error, Object[] args) {
                String[] ids = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    ids = toStringArray(args[1]);
                }
                done.doneFind(token, error, ids);
            }
        }.token;
    }

    public IToken list(String context_id, final DoneList done) {
        return new Command(channel, this, "list", new Object[]{ context_id }) {
            @Override
            public void done(Exception error, Object[] args) {
                String[] lst = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    lst = toStringArray(args[1]);
                }
                done.doneList(token, error, lst);
            }
        }.token;
    }

    public IToken getLocationInfo(String symbol_id, final DoneGetLocationInfo done) {
        return new Command(channel, this, "getLocationInfo", new Object[]{ symbol_id }) {
            @Override
            @SuppressWarnings("unchecked")
            public void done(Exception error, Object[] args) {
                Map<String,Object> props = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    props = (Map<String,Object>)args[1];
                }
                done.doneGetLocationInfo(token, error, props);
            }
        }.token;
    }

    public IToken findFrameInfo(String context_id, Number address, final DoneFindFrameInfo done) {
        return new Command(channel, this, "findFrameInfo", new Object[]{ context_id, address }) {
            @Override
            public void done(Exception error, Object[] args) {
                Number address = null;
                Number size = null;
                Object[] fp_cmds = null;
                Map<String,Object[]> reg_cmds = null;
                if (error == null) {
                    assert args.length == 5;
                    error = toError(args[0]);
                    address = (Number)args[1];
                    size = (Number)args[2];
                    fp_cmds = toObjectArray(args[3]);
                    reg_cmds = toStringMap(args[4]);
                }
                done.doneFindFrameInfo(token, error, address, size, fp_cmds, reg_cmds);
            }
        }.token;
    }

    public IToken getSymFileInfo(String context_id, Number address, final DoneGetSymFileInfo done) {
        return new Command(channel, this, "getSymFileInfo", new Object[]{ context_id, address }) {
            @SuppressWarnings("unchecked")
            @Override
            public void done(Exception error, Object[] args) {
                Map<String,Object> props = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    props = (Map<String,Object>)args[1];
                }
                done.doneGetSymFileInfo(token, error, props);
            }
        }.token;
    }

    @SuppressWarnings("unchecked")
    private String[] toStringArray(Object o) {
        if (o == null) return null;
        if (o instanceof String) return new String[]{ (String)o };
        Collection<String> c = (Collection<String>)o;
        return (String[])c.toArray(new String[c.size()]);
    }

    @SuppressWarnings("unchecked")
    private Object[] toObjectArray(Object o) {
        if (o == null) return null;
        Collection<Object> c = (Collection<Object>)o;
        return (Object[])c.toArray(new Object[c.size()]);
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object[]> toStringMap(Object o) {
        if (o == null) return null;
        Map<String,Object> c = (Map<String,Object>)o;
        HashMap<String,Object[]> m = new HashMap<String,Object[]>();
        for (Map.Entry<String,Object> e : c.entrySet()) {
            m.put(e.getKey(), toObjectArray(e.getValue()));
        }
        return m;
    }
}
