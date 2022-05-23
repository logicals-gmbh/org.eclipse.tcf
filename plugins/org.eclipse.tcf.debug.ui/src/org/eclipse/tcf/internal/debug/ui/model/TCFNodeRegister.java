/*******************************************************************************
 * Copyright (c) 2007, 2017 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenCountUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementEditor;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IHasChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ILabelUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.debug.ui.ITCFRegister;
import org.eclipse.tcf.internal.debug.actions.TCFAction;
import org.eclipse.tcf.internal.debug.model.TCFContextState;
import org.eclipse.tcf.internal.debug.ui.ColorCache;
import org.eclipse.tcf.internal.debug.ui.ImageCache;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.services.IRegisters;
import org.eclipse.tcf.services.IRunControl;
import org.eclipse.tcf.util.TCFDataCache;
import org.eclipse.tcf.util.TCFTask;


public class TCFNodeRegister extends TCFNode implements IElementEditor, IWatchInExpressions, IDetailsProvider, ITCFRegister {

    public static final String PROPERTY_REG_REPRESENTATION = "PROPERTY_REGISTER_REPRESENTATION";

    private final TCFChildrenRegisters children;
    private final TCFData<IRegisters.RegistersContext> context;
    private final TCFData<String> expression_text;
    private final TCFData<byte[]> value;
    private final boolean is_stack_frame_register;

    private byte[] prev_value;
    private byte[] next_value;

    private int index;

    TCFNodeRegister(final TCFNode parent, final String id) {
        super(parent, id);
        if (parent instanceof TCFNodeStackFrame) is_stack_frame_register = true;
        else if (parent instanceof TCFNodeRegister) is_stack_frame_register = ((TCFNodeRegister)parent).is_stack_frame_register;
        else is_stack_frame_register = false;
        children = new TCFChildrenRegisters(this);
        context = new TCFData<IRegisters.RegistersContext>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                IRegisters regs = launch.getService(IRegisters.class);
                command = regs.getContext(id, new IRegisters.DoneGetContext() {
                    public void doneGetContext(IToken token, Exception error, IRegisters.RegistersContext context) {
                        if (context != null) model.getContextMap().put(id, context);
                        set(token, error, context);
                    }
                });
                return false;
            }
        };
        expression_text = new TCFData<String>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                Throwable err = null;
                TCFNodeRegister n = TCFNodeRegister.this;
                ArrayList<String> names = new ArrayList<String>();
                for (;;) {
                    if (!n.context.validate(this)) return false;
                    IRegisters.RegistersContext ctx = n.context.getData();
                    if (ctx == null) {
                        err = n.context.getError();
                        break;
                    }
                    String s = ctx.getName();
                    if (s == null) break;
                    names.add(s);
                    if (!(n.parent instanceof TCFNodeRegister)) break;
                    n = (TCFNodeRegister)n.parent;
                }
                if (names.size() == 0 || err != null) {
                    set(null, err, null);
                }
                else {
                    StringBuffer bf = new StringBuffer();
                    boolean first = true;
                    int m = names.size();
                    while (m > 0) {
                        String s = names.get(--m);
                        boolean need_quotes = false;
                        int l = s.length();
                        for (int i = 0; i < l; i++) {
                            char ch = s.charAt(i);
                            if (ch >= 'A' && ch <= 'Z') continue;
                            if (ch >= 'a' && ch <= 'z') continue;
                            if (i > 0) {
                                if (ch >= '0' && ch <= '9') continue;
                                if (ch == '_') continue;
                            }
                            need_quotes = true;
                            break;
                        }
                        if (!first) bf.append('.');
                        if (need_quotes) bf.append("$\"");
                        if (first) bf.append('$');
                        bf.append(s);
                        if (need_quotes) bf.append('"');
                        first = false;
                    }
                    set(null, null, bf.toString());
                }
                return true;
            }
        };
        value = new TCFData<byte[]>(channel) {
            @Override
            protected boolean startDataRetrieval() {
                Boolean b = usePrevValue(this);
                if (b == null) return false;
                if (b) {
                    set(null, null, prev_value);
                    return true;
                }
                if (!context.validate(this)) return false;
                IRegisters.RegistersContext ctx = context.getData();
                if (ctx == null) {
                    set(null, context.getError(), null);
                    return true;
                }
                int[] bits = ctx.getBitNumbers();
                if (bits != null) {
                    // handle bit fields
                    TCFNodeRegister p = (TCFNodeRegister)parent;
                    if (!p.value.validate(this)) return false;
                    byte[] parent_value = p.value.getData();
                    byte[] bitfield_value = null;
                    if (parent_value != null) {
                        bitfield_value = new byte[(bits.length + 7) / 8];
                        for (int pos = 0; pos < bits.length; pos++) {
                            int bit = bits[pos];
                            if (bit / 8 >= parent_value.length) continue;
                            if ((parent_value[bit / 8] & (1 << (bit % 8))) == 0) continue;
                            bitfield_value[pos / 8] |= 1 << (pos % 8);
                        }
                    }
                    set(null, p.value.getError(), bitfield_value);
                    return true;
                }
                else {
                    if (ctx.getSize() <= 0) {
                        set(null, context.getError(), null);
                        return true;
                    }
                    final TCFDataCache<?> cache = this;
                    command = ctx.get(new IRegisters.DoneGet() {
                        public void doneGet(IToken token, Exception error, byte[] value) {
                            if (command != token) return;
                            command = null;
                            if (error != null) {
                                Boolean b = usePrevValue(cache);
                                if (b == null) return;
                                if (b) {
                                    set(null, null, prev_value);
                                    return;
                                }
                                if (value != null && value.length <= 0) value = null;
                            }
                            set(null, error, value);
                        }
                    });
                }
                return false;
            }
        };
    }

    public TCFDataCache<IRegisters.RegistersContext> getContext() {
        return context;
    }

    public TCFDataCache<byte[]> getValue() {
        return value;
    }

    public TCFChildren getChildren() {
        return children;
    }

    public TCFDataCache<String> getExpressionText() {
        return expression_text;
    }

    void setIndex(int index) {
        this.index = index;
    }

    private Boolean usePrevValue(Runnable done) {
        // Check if view should show old value.
        // Old value is shown if context is running or
        // stack trace does not contain register parent frame.
        // Return null if waiting for cache update.
        if (prev_value == null) return false;
        TCFNode p = parent;
        while (p instanceof TCFNodeRegister) p = p.parent;
        if (p instanceof TCFNodeStackFrame) {
            TCFNodeExecContext exe = (TCFNodeExecContext)p.parent;
            TCFAction action = model.getActiveAction(exe.id);
            if (action != null && action.showRunning()) return true;
            TCFDataCache<TCFContextState> state_cache = exe.getState();
            if (!state_cache.validate(done)) return null;
            TCFContextState state = state_cache.getData();
            if (state == null || !state.is_suspended) return true;
            TCFChildrenStackTrace stack_trace_cache = exe.getStackTrace();
            if (!stack_trace_cache.validate(done)) return null;
            if (stack_trace_cache.getData().get(p.id) == null) return true;
        }
        else if (p instanceof TCFNodeExecContext) {
            TCFNodeExecContext exe = (TCFNodeExecContext)p;
            TCFAction action = model.getActiveAction(exe.id);
            if (action != null && action.showRunning()) return true;
            TCFDataCache<IRunControl.RunControlContext> ctx_cache = exe.getRunContext();
            if (!ctx_cache.validate(done)) return null;
            IRunControl.RunControlContext ctx_data = ctx_cache.getData();
            if (ctx_data != null && ctx_data.hasState()) {
                Collection<String> access_types = ctx_data.getRegAccessTypes();
                if (access_types == null ||
                        !(access_types.contains(IRunControl.REG_ACCESS_RD_RUNNING) ||
                                access_types.contains(IRunControl.REG_ACCESS_RD_STOP))) {
                    TCFDataCache<TCFContextState> state_cache = exe.getState();
                    if (!state_cache.validate(done)) return null;
                    TCFContextState state_data = state_cache.getData();
                    if (state_data == null || !state_data.is_suspended) return true;
                }
            }
        }
        return false;
    }

    @Override
    void flushAllCaches() {
        prev_value = null;
        next_value = null;
        super.flushAllCaches();
    }

    public boolean getDetailText(StyledStringBuffer bf, Runnable done) {
        if (!context.validate(done)) return false;
        if (!value.validate(done)) return false;
        if (context.getError() != null) {
            bf.append(context.getError(), ColorCache.rgb_error);
            return true;
        }
        IRegisters.RegistersContext ctx = context.getData();
        if (ctx != null) {
            bf.append(ctx.getName(), SWT.BOLD);
            if (ctx.getDescription() != null) {
                bf.append(": ");
                bf.append(ctx.getDescription());
            }
            bf.append('\n');
        }
        if (value.getError() != null) {
            bf.append(value.getError(), ColorCache.rgb_error);
        }
        else {
            byte[] v = value.getData();
            if (v != null && v.length > 0) {
                bf.append("Hex: ", SWT.BOLD);
                bf.append(toNumberString(16), StyledStringBuffer.MONOSPACED);
                bf.append(", ");
                bf.append("Dec: ", SWT.BOLD);
                bf.append(toNumberString(10), StyledStringBuffer.MONOSPACED);
                bf.append(", ");
                bf.append("Oct: ", SWT.BOLD);
                bf.append(toNumberString(8), StyledStringBuffer.MONOSPACED);
                if ("PC".equals(ctx.getRole())) {
                    TCFNode p = parent;
                    while (p != null) {
                        if (p instanceof TCFNodeExecContext) {
                            TCFNodeExecContext exe = (TCFNodeExecContext)p;
                            BigInteger addr = TCFNumberFormat.toBigInteger(v, 0, v.length, ctx.isBigEndian(), false);
                            if (!exe.appendPointedObject(bf, addr, done)) return false;
                            break;
                        }
                        p = p.parent;
                    }
                }
                bf.append('\n');
                bf.append("Bin: ", SWT.BOLD);
                bf.append(toNumberString(2), StyledStringBuffer.MONOSPACED);
                bf.append('\n');
            }
        }
        if (ctx != null) {
            int l = bf.length();
            int[] bits = ctx.getBitNumbers();
            BigInteger addr = JSON.toBigInteger(ctx.getMemoryAddress());
            if (bits != null && addr == null && parent instanceof TCFNodeRegister) {
                if (!((TCFNodeRegister)parent).context.validate(done)) return false;
                IRegisters.RegistersContext parent_ctx = ((TCFNodeRegister)parent).context.getData();
                if (parent_ctx != null) addr = JSON.toBigInteger(parent_ctx.getMemoryAddress());
            }
            if (addr != null) {
                bf.append("Address: ", SWT.BOLD);
                bf.append("0x", StyledStringBuffer.MONOSPACED);
                bf.append(addr.toString(16), StyledStringBuffer.MONOSPACED);
            }
            if (bits != null) {
                if (bits.length > 0) {
                    if (l < bf.length()) bf.append(", ");
                    bf.append("Bits: ", SWT.BOLD);
                    bf.append("[");
                    int i = bits.length;
                    while (i > 0) {
                        if (i != bits.length) bf.append(",");
                        i--;
                        if (i > 2) {
                            int j = i;
                            while (j > 0 && bits[j - 1] == bits[j] - 1) j--;
                            if (i - j >= 3) {
                                bf.append(bits[i]);
                                bf.append("..");
                                bf.append(bits[j]);
                                i = j;
                                continue;
                            }
                        }
                        bf.append(bits[i]);
                    }
                    bf.append("]");
                }
            }
            else {
                BigInteger size = JSON.toBigInteger(ctx.getSize());
                if (size != null && size.compareTo(BigInteger.ZERO) > 0) {
                    if (l < bf.length()) bf.append(", ");
                    bf.append("Size: ", SWT.BOLD);
                    bf.append(size.toString(10), StyledStringBuffer.MONOSPACED);
                    bf.append(size.compareTo(BigInteger.ONE) == 0 ? " byte" : " bytes");
                }
            }
            if (ctx.isReadable()) {
                if (l < bf.length()) bf.append(", ");
                bf.append("readable");
            }
            if (ctx.isReadOnce()) {
                if (l < bf.length()) bf.append(", ");
                bf.append("read once");
            }
            if (ctx.isWriteable()) {
                if (l < bf.length()) bf.append(", ");
                bf.append("writable");
            }
            if (ctx.isWriteOnce()) {
                if (l < bf.length()) bf.append(", ");
                bf.append("write once");
            }
            if (ctx.hasSideEffects()) {
                if (l < bf.length()) bf.append(", ");
                bf.append("side effects");
            }
            if (l < bf.length()) bf.append('\n');
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean getChildren(IPresentationContext ctx, List<TCFNode> list, Runnable done) {
        AtomicBoolean b = new AtomicBoolean();
        if (!isRepresentationGroup(b, done)) return false;
        boolean rep_group = b.get();
        String rep_id = null;
        if (rep_group) {
            Map<String,String> map = (Map<String,String>)ctx.getProperty(
                    TCFNodeRegister.PROPERTY_REG_REPRESENTATION);
            if (map != null) rep_id = map.get(id);
        }
        for (TCFNode child : children.toArray()) {
            if (!rep_group || child.id.equals(rep_id)) list.add(child);
        }
        return true;
    }

    @Override
    protected boolean getData(IHasChildrenUpdate result, Runnable done) {
        List<TCFNode> list = new ArrayList<TCFNode>();
        if (!getChildren(result.getPresentationContext(), list, done)) return false;
        result.setHasChilren(list.size() > 0);
        return true;
    }

    @Override
    protected boolean getData(IChildrenCountUpdate result, Runnable done) {
        List<TCFNode> list = new ArrayList<TCFNode>();
        if (!getChildren(result.getPresentationContext(), list, done)) return false;
        result.setChildCount(list.size());
        return true;
    }

    @Override
    protected boolean getData(IChildrenUpdate result, Runnable done) {
        List<TCFNode> list = new ArrayList<TCFNode>();
        if (!getChildren(result.getPresentationContext(), list, done)) return false;
        int r_offset = result.getOffset();
        int r_length = result.getLength();
        for (int n = r_offset; n < r_offset + r_length && n < list.size(); n++) {
            result.setChild(list.get(n), n);
        }
        return true;
    }

    @Override
    protected boolean getData(ILabelUpdate result, Runnable done) {
        TCFDataCache<?> pending = null;
        if (!context.validate()) pending = context;
        if (!value.validate()) pending = value;
        if (pending != null) {
            pending.wait(done);
            return false;
        }
        String[] cols = result.getColumnIds();
        if (cols == null) {
            setLabel(result, -1, 16);
        }
        else {
            IRegisters.RegistersContext ctx = context.getData();
            for (int i = 0; i < cols.length; i++) {
                String c = cols[i];
                if (ctx == null) {
                    result.setForeground(ColorCache.rgb_error, i);
                    result.setLabel("N/A", i);
                }
                else if (c.equals(TCFColumnPresentationRegister.COL_NAME)) {
                    result.setLabel(ctx.getName(), i);
                }
                else if (c.equals(TCFColumnPresentationRegister.COL_HEX_VALUE)) {
                    setLabel(result, i, 16);
                }
                else if (c.equals(TCFColumnPresentationRegister.COL_DEC_VALUE)) {
                    setLabel(result, i, 10);
                }
                else if (c.equals(TCFColumnPresentationRegister.COL_DESCRIPTION)) {
                    result.setLabel(ctx.getDescription(), i);
                }
                else if (c.equals(TCFColumnPresentationRegister.COL_READBLE)) {
                    result.setLabel(bool(ctx.isReadable()), i);
                }
                else if (c.equals(TCFColumnPresentationRegister.COL_READ_ONCE)) {
                    result.setLabel(bool(ctx.isReadOnce()), i);
                }
                else if (c.equals(TCFColumnPresentationRegister.COL_WRITEABLE)) {
                    result.setLabel(bool(ctx.isWriteable()), i);
                }
                else if (c.equals(TCFColumnPresentationRegister.COL_WRITE_ONCE)) {
                    result.setLabel(bool(ctx.isWriteOnce()), i);
                }
                else if (c.equals(TCFColumnPresentationRegister.COL_SIDE_EFFECTS)) {
                    result.setLabel(bool(ctx.hasSideEffects()), i);
                }
                else if (c.equals(TCFColumnPresentationRegister.COL_VOLATILE)) {
                    result.setLabel(bool(ctx.isVolatile()), i);
                }
                else if (c.equals(TCFColumnPresentationRegister.COL_FLOAT)) {
                    result.setLabel(bool(ctx.isFloat()), i);
                }
                else if (c.equals(TCFColumnPresentationRegister.COL_MNEMONIC)) {
                    result.setLabel(getMnemonic(ctx), i);
                }
            }
        }
        next_value = value.getData();
        if (prev_value != null && next_value != null) {
            boolean changed = false;
            if (prev_value.length != next_value.length) {
                changed = true;
            }
            else {
                for (int i = 0; i < prev_value.length; i++) {
                    if (prev_value[i] != next_value[i]) changed = true;
                }
            }
            if (changed) {
                if (cols != null) {
                    for (int i = 1; i < cols.length; i++) {
                        result.setBackground(ColorCache.rgb_highlight, i);
                    }
                }
                else {
                    result.setForeground(ColorCache.rgb_no_columns_color_change, 0);
                }
            }
        }
        result.setImageDescriptor(ImageCache.getImageDescriptor(ImageCache.IMG_REGISTER), 0);
        return true;
    }

    private void setLabel(ILabelUpdate result, int col, int radix) {
        String name = null;
        IRegisters.RegistersContext ctx = context.getData();
        if (ctx != null) name = ctx.getName();
        Throwable error = context.getError();
        if (error == null) error = value.getError();
        byte[] data = value.getData();
        if (error != null && col >= 0) {
            result.setForeground(ColorCache.rgb_error, col);
            result.setLabel("N/A", col);
        }
        else if (data != null && error == null) {
            String s = toNumberString(radix);
            if (col >= 0) {
                result.setLabel(s, col);
            }
            else if (name != null) {
                result.setLabel(name + " = " + s, 0);
            }
        }
        else if (col < 0 && name != null) {
            result.setLabel(name, 0);
        }
    }

    @Override
    protected void getFontData(ILabelUpdate update, String view_id) {
        FontData fn = TCFModelFonts.getNormalFontData(view_id);
        String[] cols = update.getColumnIds();
        if (cols == null || cols.length == 0) {
            update.setFontData(fn, 0);
        }
        else {
            String[] ids = update.getColumnIds();
            for (int i = 0; i < cols.length; i++) {
                if (TCFColumnPresentationRegister.COL_HEX_VALUE.equals(ids[i]) ||
                        TCFColumnPresentationExpression.COL_DEC_VALUE.equals(ids[i])) {
                    update.setFontData(TCFModelFonts.getMonospacedFontData(view_id), i);
                }
                else {
                    update.setFontData(fn, i);
                }
            }
        }
    }

    private String toNumberString(int radix) {
        IRegisters.RegistersContext ctx = context.getData();
        byte[] data = value.getData();
        int[] bits = ctx.getBitNumbers();
        if (ctx == null || data == null) return "N/A";
        if (data.length == 0) return "";
        if (radix == 2) {
            StringBuffer bf = new StringBuffer();
            int i = data.length * 8;
            if (bits != null) i = bits.length;
            while (i > 0) {
                if (i % 4 == 0 && bf.length() > 0) bf.append(',');
                i--;
                int j = i / 8;
                if (ctx.isBigEndian()) j = data.length - j - 1;
                if ((data[j] & (1 << (i % 8))) != 0) {
                    bf.append('1');
                }
                else {
                    bf.append('0');
                }
            }
            return bf.toString();
        }
        if (radix == 10 && ctx.isFloat()) {
            String s = TCFNumberFormat.toFPString(data, 0, data.length, ctx.isBigEndian());
            if (s != null) return s;
        }
        BigInteger b = TCFNumberFormat.toBigInteger(data, ctx.isBigEndian(), false);
        String s = b.toString(radix);
        switch (radix) {
        case 8:
            if (!s.startsWith("0")) s = "0" + s;
            break;
        case 16:
            if (s.length() < (bits == null ? data.length * 2 : (bits.length + 3) / 4)) {
                StringBuffer bf = new StringBuffer();
                while (bf.length() + s.length() < data.length * 2) bf.append('0');
                bf.append(s);
                s = bf.toString();
            }
            break;
        }
        return s;
    }

    private String bool(boolean b) {
        return b ? "yes" : "no";
    }

    private String getMnemonic(IRegisters.RegistersContext ctx) {
        if (value.getData() != null) {
            IRegisters.NamedValue[] arr = ctx.getNamedValues();
            if (arr != null) {
                for (IRegisters.NamedValue n : arr) {
                    if (Arrays.equals(n.getValue(), value.getData())) return n.getName();
                }
            }
        }
        return "";
    }

    private void postStateChangedDelta() {
        for (TCFModelProxy p : model.getModelProxies()) {
            if (!IDebugUIConstants.ID_REGISTER_VIEW.equals(p.getPresentationContext().getId())) continue;
            p.addDelta(this, IModelDelta.STATE);
        }
    }

    void onValueChanged() {
        prev_value = next_value;
        value.reset();
        TCFNode n = parent;
        while (n != null) {
            if (n instanceof TCFNodeExecContext) {
                ((TCFNodeExecContext)n).onRegisterValueChanged();
                break;
            }
            else if (n instanceof TCFNodeRegister) {
                TCFNodeRegister r = (TCFNodeRegister)n;
                if (r.value.isValid() && r.value.getData() != null) {
                    r.value.reset();
                    r.postStateChangedDelta();
                }
            }
            n = n.parent;
        }
        children.onParentValueChanged();
        postStateChangedDelta();
    }

    void onParentValueChanged() {
        value.reset();
        children.onParentValueChanged();
        postStateChangedDelta();
    }

    void onSuspended(boolean func_call) {
        if (!func_call) {
            prev_value = next_value;
            value.reset();
            // Unlike thread registers, stack frame register list must be retrieved on every suspend
            if (is_stack_frame_register) children.reset();
            // No need to post delta: parent posted CONTENT
        }
        else if (value.isValid() && value.getError() != null) {
            value.reset();
        }
        children.onSuspended(func_call);
    }

    void onRegistersChanged() {
        children.onRegistersChanged();
        expression_text.reset();
        context.reset();
        value.reset();
        // No need to post delta: parent posted CONTENT
    }

    public CellEditor getCellEditor(IPresentationContext context, String column_id, Object element, Composite parent) {
        assert element == this;
        if (TCFColumnPresentationRegister.COL_HEX_VALUE.equals(column_id)) {
            return new TextCellEditor(parent);
        }
        if (TCFColumnPresentationRegister.COL_DEC_VALUE.equals(column_id)) {
            return new TextCellEditor(parent);
        }
        return null;
    }

    private static final ICellModifier cell_modifier = new ICellModifier() {
        private Object original_value;

        public boolean canModify(Object element, final String property) {
            final TCFNodeRegister node = (TCFNodeRegister)element;
            try {
                return new TCFTask<Boolean>() {
                    public void run() {
                        if (!node.context.validate(this)) return;
                        IRegisters.RegistersContext ctx = node.context.getData();
                        if (ctx != null && ctx.isWriteable()) {
                            if (!ctx.isReadable() || ctx.isReadOnce()) {
                                done(Boolean.TRUE);
                                return;
                            }
                            if (!node.value.validate(this)) return;
                            if (node.value.getError() == null) {
                                if (TCFColumnPresentationRegister.COL_HEX_VALUE.equals(property)) {
                                    done(TCFNumberFormat.isValidHexNumber(node.toNumberString(16)) == null);
                                    return;
                                }
                                if (TCFColumnPresentationRegister.COL_DEC_VALUE.equals(property)) {
                                    done(TCFNumberFormat.isValidDecNumber(true, node.toNumberString(10)) == null);
                                    return;
                                }
                            }
                        }
                        done(Boolean.FALSE);
                    }
                }.get(1, TimeUnit.SECONDS);
            }
            catch (Exception e) {
                return false;
            }
        }

        public Object getValue(Object element, final String property) {
            original_value = null;
            final TCFNodeRegister node = (TCFNodeRegister)element;
            try {
                original_value = new TCFTask<String>() {
                    public void run() {
                        if (!node.context.validate(this)) return;
                        IRegisters.RegistersContext ctx = node.context.getData();
                        if (!ctx.isReadable() || ctx.isReadOnce()) {
                            done("");
                            return;
                        }
                        if (!node.value.validate(this)) return;
                        if (node.value.getError() == null) {
                            if (TCFColumnPresentationRegister.COL_HEX_VALUE.equals(property)) {
                                done(node.toNumberString(16));
                                return;
                            }
                            if (TCFColumnPresentationRegister.COL_DEC_VALUE.equals(property)) {
                                done(node.toNumberString(10));
                                return;
                            }
                        }
                        done(null);
                    }
                }.get(1, TimeUnit.SECONDS);
                if ("".equals(original_value)) {
                    /* Write only register */
                    original_value = null;
                    return "0";
                }
                return original_value;
            }
            catch (Exception e) {
                return null;
            }
        }

        public void modify(Object element, final String property, final Object value) {
            if (value == null) return;
            if (original_value != null && original_value.equals(value)) return;
            final TCFNodeRegister node = (TCFNodeRegister)element;
            try {
                new TCFTask<Boolean>() {
                    public void run() {
                        try {
                            if (!node.context.validate(this)) return;
                            IRegisters.RegistersContext ctx = node.context.getData();
                            if (ctx != null && ctx.isWriteable()) {
                                byte[] bf = null;
                                boolean is_float = ctx.isFloat();
                                int size = ctx.getSize();
                                boolean big_endian = ctx.isBigEndian();
                                String input = (String)value;
                                String error = null;
                                int[] bits = ctx.getBitNumbers();
                                if (bits != null) size = (bits.length + 7) / 8;
                                if (TCFColumnPresentationRegister.COL_HEX_VALUE.equals(property)) {
                                    if (input.startsWith("0x")) input = input.substring(2);
                                    error = TCFNumberFormat.isValidHexNumber(input);
                                    if (error == null) bf = TCFNumberFormat.toByteArray(input, 16, false, size, false, big_endian);
                                }
                                else if (TCFColumnPresentationRegister.COL_DEC_VALUE.equals(property)) {
                                    error = TCFNumberFormat.isValidDecNumber(is_float, input);
                                    if (error == null) bf = TCFNumberFormat.toByteArray(input, 10, is_float, size, is_float, big_endian);
                                }
                                if (error != null) throw new Exception("Invalid value: " + value, new Exception(error));
                                if (bf != null) {
                                    // handle bit fields
                                    if (bits != null) {
                                        TCFNodeRegister p = (TCFNodeRegister)node.parent;
                                        if (!p.value.validate(this)) return;
                                        byte[] parent_value = p.value.getData();
                                        if (!p.context.validate(this)) return;
                                        IRegisters.RegistersContext parent_context = p.context.getData();

                                        if (parent_context != null && parent_value != null) {
                                            byte[] new_value = new byte[parent_value.length];
                                            System.arraycopy(parent_value, 0, new_value, 0, parent_value.length);
                                            for (int pos = 0; pos < bits.length; pos++) {
                                                int bit = bits[pos];
                                                if (bit / 8 >= new_value.length) continue;
                                                if ((bf[pos / 8] & (1 << (pos % 8))) == 0) {
                                                    new_value[bit / 8] &= ~(1 << (bit % 8));
                                                }
                                                else {
                                                    new_value[bit / 8] |= 1 << (bit % 8);
                                                }
                                            }
                                            parent_context.set(new_value, new IRegisters.DoneSet() {
                                                public void doneSet(IToken token, Exception error) {
                                                    TCFNodeRegister p = (TCFNodeRegister)node.parent;
                                                    if (error != null) {
                                                        p.model.showMessageBox("Cannot modify register value", error);
                                                        done(Boolean.FALSE);
                                                    }
                                                    else {
                                                        p.value.reset();
                                                        p.postStateChangedDelta();
                                                        done(Boolean.TRUE);
                                                    }
                                                }
                                            });
                                            return;
                                        }
                                    }
                                    else {
                                        ctx.set(bf, new IRegisters.DoneSet() {
                                            public void doneSet(IToken token, Exception error) {
                                                if (error != null) {
                                                    node.model.showMessageBox("Cannot modify register value", error);
                                                    done(Boolean.FALSE);
                                                }
                                                else {
                                                    node.value.reset();
                                                    node.postStateChangedDelta();
                                                    done(Boolean.TRUE);
                                                }
                                            }
                                        });
                                        return;
                                    }
                                }
                            }
                            done(Boolean.FALSE);
                        }
                        catch (Throwable x) {
                            node.model.showMessageBox("Cannot modify register value", x);
                            done(Boolean.FALSE);
                        }
                    }
                }.get(10, TimeUnit.SECONDS);
            }
            catch (TimeoutException e) {
                node.model.showMessageBox("Timeout modifying register value", new Exception("No response for 10 seconds."));
            }
            catch (Exception e) {
                node.model.showMessageBox("Error modifying register value", e);
            }
        }
    };

    public ICellModifier getCellModifier(IPresentationContext context, Object element) {
        assert element == this;
        return cell_modifier;
    }

    @Override
    public int compareTo(TCFNode n) {
        if (n instanceof TCFNodeRegister) {
            TCFNodeRegister r = (TCFNodeRegister)n;
            if (index < r.index) return -1;
            if (index > r.index) return +1;
        }
        return id.compareTo(n.id);
    }

    /**
     * Check if this register has multiple representations.
     */
    public boolean isRepresentationGroup(AtomicBoolean res, Runnable done) {
        res.set(false);
        HashSet<Integer> offsets = new HashSet<Integer>();
        if (!context.validate(done)) return false;
        if (!children.validate(done)) return false;
        IRegisters.RegistersContext reg_ctx = context.getData();
        if (reg_ctx == null) return true;
        if (reg_ctx.getSize() == 0) return true;
        for (TCFNode child_node : children.toArray()) {
            TCFNodeRegister child_reg = (TCFNodeRegister)child_node;
            if (!child_reg.context.validate(done)) return false;
            IRegisters.RegistersContext ctx = child_reg.context.getData();
            if (ctx == null) continue;
            int offs = ctx.getOffset();
            if (offs >= 0) {
                if (!offsets.add(Integer.valueOf(offs))) {
                    res.set(true);
                    return true;
                }
                continue;
            }
            // TODO: checking grand children should not be needed
            if (!child_reg.children.validate(done)) return false;
            for (TCFNode grand_child_node : child_reg.children.toArray()) {
                TCFNodeRegister grand_child_reg = (TCFNodeRegister)grand_child_node;
                if (!grand_child_reg.context.validate(done)) return false;
                ctx = grand_child_reg.context.getData();
                if (ctx == null) continue;
                offs = ctx.getOffset();
                if (offs >= 0) {
                    if (!offsets.add(Integer.valueOf(offs))) {
                        res.set(true);
                        return true;
                    }
                }
            }
        }
        return true;
    }
}
