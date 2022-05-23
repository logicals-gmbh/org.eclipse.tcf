/*******************************************************************************
 * Copyright (c) 2013, 2015 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Xilinx - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.profiler;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.debug.ui.contexts.IDebugContextService;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.tcf.internal.debug.launch.TCFSourceLookupParticipant;
import org.eclipse.tcf.internal.debug.model.TCFFunctionRef;
import org.eclipse.tcf.internal.debug.model.TCFLaunch;
import org.eclipse.tcf.internal.debug.model.TCFSourceRef;
import org.eclipse.tcf.internal.debug.ui.ImageCache;
import org.eclipse.tcf.internal.debug.ui.model.TCFModel;
import org.eclipse.tcf.internal.debug.ui.model.TCFModelManager;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExecContext;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IProfiler;
import org.eclipse.tcf.services.ISymbols;
import org.eclipse.tcf.util.TCFDataCache;
import org.eclipse.tcf.util.TCFTask;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.ViewPart;

public class ProfilerView extends ViewPart {

    private static final int FRAME_COUNT = 8;
    private static final String PARAM_VIEW_UPDATE_PERIOD = ProfilerSettingsDlg.PARAM_VIEW_UPDATE_PERIOD;
    private static final String PARAM_AGGREGATE = ProfilerSettingsDlg.PARAM_AGGREGATE;
    private static final String PARAM_STACK_TRACE = ProfilerSettingsDlg.PARAM_STACK_TRACE;

    private static class ProfileSample {
        int cnt;
        final BigInteger[] trace;

        ProfileSample(BigInteger[] trace) {
            this.trace = trace;
        }
    }

    private static class ProfileData {
        final String ctx;
        final Map<String,Object> params;
        final boolean stack_trace;

        boolean stopped;
        boolean unsupported;
        Throwable error;
        int sample_count;

        // Samples by frame
        final Map<BigInteger,List<ProfileSample>>[] map;

        int generation_inp;
        int generation_out;

        ProfileEntry[] entries;

        @SuppressWarnings("unchecked")
        ProfileData(String ctx, Map<String,Object> params) {
            this.ctx = ctx;
            this.params = new HashMap<String,Object>(params);
            Boolean b = (Boolean)params.get(PARAM_STACK_TRACE);
            stack_trace = b != null && b.booleanValue();
            int frame_cnt = 1;
            if (stack_trace) {
                Number n = (Number)params.get(IProfiler.PARAM_FRAME_CNT);
                if (n != null) frame_cnt = n.intValue();
            }
            map = new Map[frame_cnt];
            for (int i = 0; i < frame_cnt; i++) {
                map[i] = new HashMap<BigInteger,List<ProfileSample>>();
            }
        }
    }

    private static class ProfileEntry {
        final BigInteger addr;
        final Set<BigInteger> addr_list = new HashSet<BigInteger>();

        String name;
        String file_full;
        String file_base;
        int line;
        int count;
        float total;
        ProfileEntryRef[] up;
        ProfileEntryRef[] dw;

        boolean src_info_valid;
        boolean mark;

        ProfileEntry(BigInteger addr) {
            this.addr = addr;
        }
    }

    private static class ProfileEntryRef {
        final ProfileEntry pe;
        float total;

        ProfileEntryRef(ProfileEntry pe) {
            this.pe = pe;
        }
    }

    private class ProfileModel implements TCFModel.ProfilerDataListener {

        final Map<String,ProfileData> data = new HashMap<String,ProfileData>();

        @Override
        public void onDataReceived(String ctx, Map<String, Object>[] arr) {
            int cnt = 0;
            ProfileData p = data.get(ctx);
            if (p == null) return;
            if (p.stopped) return;
            try {
                for (Map<String,Object> props : arr) {
                    if (props == null) continue;
                    String format = (String)props.get(IProfiler.PROP_FORMAT);
                    if (format == null || !format.equals("StackTraces")) continue;
                    addSamples(p, props);
                    cnt++;
                }
            }
            catch (Throwable x) {
                p.error = x;
            }
            if (p.unsupported != (cnt == 0)) {
                p.unsupported = cnt == 0;
                updateView();
            }
        }
    }

    private final IDebugContextListener selection_listener = new IDebugContextListener() {
        @Override
        public void debugContextChanged(DebugContextEvent event) {
            selectionChanged(event.getContext());
        }
    };

    private final TCFModelManager.ModelManagerListener launch_listener = new TCFModelManager.ModelManagerListener() {
        @Override
        public void onConnected(TCFLaunch launch, TCFModel model) {
        }

        @Override
        public void onDisconnected(TCFLaunch launch, TCFModel model) {
            ProfileModel prf_model = models.remove(model);
            if (prf_model != null) model.removeProfilerDataListener(prf_model);
            updateView();
        }
    };

    private static class ProfileEntryComparator implements Comparator<ProfileEntry> {
        final int sorting;
        ProfileEntryComparator(int sorting) {
            this.sorting = sorting;
        }
        @Override
        public int compare(ProfileEntry x, ProfileEntry y) {
            int r = 0;
            switch (sorting) {
            case 0:
                r = x.addr.compareTo(y.addr);
                break;
            case 1:
                break;
            case 2:
                r = Float.compare(y.total, x.total);
                break;
            case 3:
                if (x.name == y.name) break;
                if (x.name == null) return -1;
                if (y.name == null) return +1;
                r = x.name.compareTo(y.name);
                break;
            case 4:
                if (x.file_base == y.file_base) break;
                if (x.file_base == null) return -1;
                if (y.file_base == null) return +1;
                r = x.file_base.compareTo(y.file_base);
                break;
            }
            if (r != 0) return r;
            if (x.count > y.count) return -1;
            if (x.count < y.count) return +1;
            return x.addr.compareTo(y.addr);
        }
    };

    private static class ProfileEntryRefComparator implements Comparator<ProfileEntryRef> {
        @Override
        public int compare(ProfileEntryRef x, ProfileEntryRef y) {
            int r = Float.compare(y.total, x.total);
            if (r == 0) r = x.pe.addr.compareTo(y.pe.addr);
            return r;
        }
    }

    private class Update implements Runnable {
        final int sorting;
        final TCFNode selection;
        final Map<BigInteger,ProfileEntry> entries = new HashMap<BigInteger,ProfileEntry>();
        final Map<BigInteger,BigInteger> addr_to_func_addr = new HashMap<BigInteger,BigInteger>();
        final Map<BigInteger,String> addr_to_func_id = new HashMap<BigInteger,String>();
        final TCFNodeExecContext node;
        final ProfileData prof_data;
        final TCFModel model;
        final boolean aggrerate;
        final int generation;
        TCFNodeExecContext mem_node;
        TCFDataCache<?> pending;
        boolean done;

        Update() {
            assert Protocol.isDispatchThread();
            selection = ProfilerView.this.selection;
            sorting = ProfilerView.this.sorting;
            ProfileData p = null;
            if (selection != null) {
                ProfileModel m = models.get(selection.getModel());
                if (m != null) p = m.data.get(selection.getID());
            }
            prof_data = p;
            if (p == null) {
                node = null;
                model = null;
                generation = 0;
                aggrerate = false;
            }
            else {
                node = (TCFNodeExecContext)selection;
                model = selection.getModel();
                generation = p.generation_inp;
                Boolean b = (Boolean)p.params.get(PARAM_AGGREGATE);
                aggrerate = b != null && b.booleanValue();
            }
            last_update = this;
        }

        private String getFuncID(BigInteger addr) {
            String func_id = addr_to_func_id.get(addr);
            if (func_id == null) {
                func_id = "";
                TCFDataCache<TCFFunctionRef> func_cache = mem_node.getFuncInfo(addr);
                if (func_cache != null) {
                    if (!func_cache.validate()) {
                        pending = func_cache;
                        return null;
                    }
                    TCFFunctionRef func_data = func_cache.getData();
                    if (func_data != null && func_data.symbol_id != null) {
                        func_id = func_data.symbol_id;
                    }
                }
                addr_to_func_id.put(addr, func_id);
            }
            return func_id;
        }

        private BigInteger getFuncAddress(BigInteger addr) {
            if (!aggrerate) return addr;
            BigInteger func_addr = addr_to_func_addr.get(addr);
            if (func_addr != null) return func_addr;
            String func_id = getFuncID(addr);
            if (func_id == null) return null;
            func_addr = addr;
            if (func_id.length() > 0) {
                TCFDataCache<ISymbols.Symbol> sym_cache = model.getSymbolInfoCache(func_id);
                if (!sym_cache.validate()) {
                    pending = sym_cache;
                    return null;
                }
                ISymbols.Symbol sym_data = sym_cache.getData();
                if (sym_data != null && sym_data.getAddress() != null) {
                    func_addr = JSON.toBigInteger(sym_data.getAddress());
                }
            }
            addr_to_func_addr.put(addr, func_addr);
            return func_addr;
        }

        private boolean getFuncName(ProfileEntry pe) {
            String func_id = getFuncID(pe.addr);
            if (func_id == null) return false;
            if (func_id.length() > 0) {
                TCFDataCache<ISymbols.Symbol> sym_cache = model.getSymbolInfoCache(func_id);
                if (!sym_cache.validate()) {
                    pending = sym_cache;
                    return false;
                }
                ISymbols.Symbol sym_data = sym_cache.getData();
                if (sym_data != null && sym_data.getName() != null) {
                    pe.name = sym_data.getName();
                }
            }
            return true;
        }

        private boolean getLineInfo(ProfileEntry pe) {
            TCFDataCache<TCFSourceRef> line_cache = mem_node.getLineInfo(pe.addr);
            if (line_cache == null) return true;
            if (!line_cache.validate()) {
                pending = line_cache;
                return false;
            }
            TCFSourceRef line_data = line_cache.getData();
            if (line_data != null && line_data.area != null) {
                pe.file_full = TCFSourceLookupParticipant.toFileName(line_data.area);
                if (pe.file_full != null) {
                    pe.file_base = pe.file_full;
                    int i = pe.file_base.lastIndexOf('/');
                    int j = pe.file_base.lastIndexOf('\\');
                    if (i > j) pe.file_base = pe.file_base.substring(i + 1);
                    if (i < j) pe.file_base = pe.file_base.substring(j + 1);
                    pe.line = line_data.area.start_line;
                }
            }
            return true;
        }

        private void linkEntry(ProfileEntry pe) {
            Set<ProfileEntry> set_up = new HashSet<ProfileEntry>();
            Set<ProfileEntry> set_dw = new HashSet<ProfileEntry>();
            for (int n = 0; n < prof_data.map.length; n++) {
                for (BigInteger addr : pe.addr_list) {
                    List<ProfileSample> s0 = prof_data.map[n].get(addr);
                    if (s0 != null) {
                        for (ProfileSample x : s0) {
                            assert addr.equals(x.trace[n]);
                            if (x.trace.length <= n + 1) continue;
                            BigInteger addr_up = getFuncAddress(x.trace[n + 1]);
                            ProfileEntry pe_up = entries.get(addr_up);
                            set_up.add(pe_up);
                        }
                    }
                    if (n == prof_data.map.length - 1) continue;
                    List<ProfileSample> s1 = prof_data.map[n + 1].get(addr);
                    if (s1 != null) {
                        for (ProfileSample x : s1) {
                            assert x.trace.length > n + 1;
                            assert addr.equals(x.trace[n + 1]);
                            BigInteger addr_dw = getFuncAddress(x.trace[n]);
                            ProfileEntry pe_dw = entries.get(addr_dw);
                            set_dw.add(pe_dw);
                        }
                    }
                }
            }
            if (set_up.size() > 0) {
                int n = 0;
                pe.up = new ProfileEntryRef[set_up.size()];
                for (ProfileEntry p : set_up) pe.up[n++] = new ProfileEntryRef(p);
            }
            if (set_dw.size() > 0) {
                int n = 0;
                pe.dw = new ProfileEntryRef[set_dw.size()];
                for (ProfileEntry p : set_dw) pe.dw[n++] = new ProfileEntryRef(p);
            }
        }

        private void addUpTotal(ProfileEntry pe, float cnt) {
            if (cnt <= 0.01f || pe.up == null) return;
            pe.mark = true;
            int n = 0;
            for (ProfileEntryRef up : pe.up) {
                if (!up.pe.mark) n++;
            }
            if (n != 0) {
                float m = cnt / n;
                for (ProfileEntryRef up : pe.up) {
                    if (up.pe.mark) continue;
                    addUpTotal(up.pe, m * 1.0001f);
                    up.pe.total += m;
                    up.total += m;
                    for (ProfileEntryRef dw : up.pe.dw) {
                        if (dw.pe == pe) {
                            dw.total += m;
                            break;
                        }
                    }
                }
            }
            pe.mark = false;
        }

        @Override
        public void run() {
            pending = null;
            mem_node = null;
            if (done) return;
            if (last_update != this) return;
            boolean capabilities_ok = false;
            if (selection instanceof TCFNodeExecContext) {
                TCFDataCache<Map<String,Object>> cache = selection.getModel().getProfilerCapabilities(selection.getID());
                if (!cache.validate(this)) return;
                Map<String,Object> map = cache.getData();
                capabilities_ok = map == null || map.containsKey("StackTraces");
            }
            if (prof_data != null && generation != prof_data.generation_out) {
                if (node.isDisposed()) {
                    entries.clear();
                }
                else {
                    TCFDataCache<TCFNodeExecContext> mem_cache = node.getMemoryNode();
                    if (!mem_cache.validate()) {
                        pending = mem_cache;
                    }
                    else {
                        mem_node = mem_cache.getData();
                    }
                    if (mem_node != null) {
                        for (int n = 0; n < prof_data.map.length; n++) {
                            for (BigInteger addr : prof_data.map[n].keySet()) {
                                BigInteger func_addr = getFuncAddress(addr);
                                if (func_addr == null) continue;
                                ProfileEntry pe = entries.get(func_addr);
                                if (pe == null) {
                                    pe = new ProfileEntry(func_addr);
                                    entries.put(pe.addr, pe);
                                }
                                if (!pe.addr_list.contains(addr)) {
                                    if (n == 0) {
                                        List<ProfileSample> s = prof_data.map[0].get(addr);
                                        for (ProfileSample x : s) pe.count += x.cnt;
                                    }
                                    pe.addr_list.add(addr);
                                }
                                if (!pe.src_info_valid) {
                                    pe.src_info_valid = true;
                                    if (!getFuncName(pe)) pe.src_info_valid = false;
                                    if (!getLineInfo(pe)) pe.src_info_valid = false;
                                }
                            }
                        }
                    }
                    if (pending != null) {
                        pending.wait(this);
                        return;
                    }
                    for (ProfileEntry pe : entries.values()) linkEntry(pe);
                    for (List<ProfileSample> lps : prof_data.map[0].values()) {
                        for (ProfileSample ps : lps) {
                            int n = 0;
                            ProfileEntry dw_pe = null;
                            ProfileEntry pe = null;
                            ProfileEntry up_pe = null;
                            assert(ps.trace.length <= prof_data.map.length);
                            while (n < ps.trace.length) {
                                float cnt = ps.cnt * (1.0f + n / 10000f);
                                if (pe == null) pe = entries.get(getFuncAddress(ps.trace[n]));
                                if (dw_pe != null) {
                                    for (ProfileEntryRef r : pe.dw) {
                                        if (r.pe == dw_pe) {
                                            r.total += cnt;
                                            break;
                                        }
                                    }
                                }
                                if (n < ps.trace.length - 1) {
                                    up_pe = entries.get(getFuncAddress(ps.trace[n + 1]));
                                    for (ProfileEntryRef r : pe.up) {
                                        if (r.pe == up_pe) {
                                            r.total += cnt;
                                            break;
                                        }
                                    }
                                }
                                else {
                                    addUpTotal(pe, cnt);
                                }
                                pe.total += cnt;
                                dw_pe = pe;
                                pe = up_pe;
                                n++;
                            }
                        }
                    }
                    for (ProfileEntry pe : entries.values()) {
                        if (pe.up != null) Arrays.sort(pe.up, new ProfileEntryRefComparator());
                        if (pe.dw != null) Arrays.sort(pe.dw, new ProfileEntryRefComparator());
                    }
                }
                prof_data.generation_out = generation;
                prof_data.entries = entries.values().toArray(new ProfileEntry[entries.size()]);
                assert pending == null;
            }
            if (prof_data != null && prof_data.entries != null) {
                Arrays.sort(prof_data.entries, new ProfileEntryComparator(sorting));
            }
            done = true;
            final boolean enable_start = capabilities_ok;
            final boolean enable_stop = node != null && !prof_data.stopped;
            final boolean stopped = node != null && prof_data.stopped;
            final boolean running = node != null && !stopped;
            final boolean unsupported = node != null && prof_data.unsupported;
            final boolean total_count = prof_data != null && prof_data.stack_trace;
            final int sample_count = prof_data == null ? 0 : prof_data.sample_count;
            final String error_msg = prof_data == null || prof_data.error == null ? null :
                TCFModel.getErrorMessage(prof_data.error, false);
            asyncExec(new Runnable() {
                @Override
                public void run() {
                    if (last_update != Update.this) return;
                    if (parent.isDisposed()) return;
                    action_start.setEnabled(enable_start);
                    action_stop.setEnabled(enable_stop);
                    profile_node = node;
                    disposeColors();
                    Object viewer_input = prof_data != null ? prof_data.entries : null;
                    if (viewer_main.getInput() != viewer_input) {
                        ISelection s = viewer_main.getSelection();
                        ProfilerView.this.total_count = total_count;
                        ProfilerView.this.sample_count = sample_count;
                        viewer_main.setInput(viewer_input);
                        List<ProfileEntry> l = new ArrayList<ProfileEntry>();
                        if (s instanceof IStructuredSelection && entries.size() > 0) {
                            IStructuredSelection ss = (IStructuredSelection)s;
                            for (Object obj : ss.toArray()) {
                                if (obj instanceof ProfileEntry) {
                                    ProfileEntry pe = (ProfileEntry)obj;
                                    pe = entries.get(pe.addr);
                                    if (pe != null) l.add(pe);
                                }
                            }
                        }
                        setSelection(l, false);
                    }
                    else {
                        // Sorting might be changed, need to refresh
                        viewer_main.refresh();
                    }
                    if (!enable_start) {
                        status.setText("Selected context does not support profiling");
                    }
                    else if (unsupported) {
                        status.setText("No suitable profiler found for selected context");
                    }
                    else if (stopped) {
                        status.setText("Profiler stopped. Press 'Start' button to restart profiling");
                    }
                    else if (!running) {
                        status.setText("Idle. Press 'Start' button to start profiling");
                    }
                    else if (error_msg != null) {
                        status.setText("Cannot upload profiling data: " + error_msg);
                    }
                    else {
                        status.setText("Profiler running. " + sample_count + " samples");
                    }
                }
            });
        }
    }

    private class ProfileContentProvider implements IStructuredContentProvider  {

        public Object[] getElements(Object input) {
            return (Object[])input;
        }

        public void inputChanged(Viewer viewer, Object old_input, Object new_input) {
        }

        public void dispose() {
        }
    }

    private class ProfileEntryLabelProvider extends LabelProvider implements ITableLabelProvider {

        public Image getColumnImage(Object element, int column) {
            return null;
        }

        public String getColumnText(Object element, int column) {
            ProfileEntry e = (ProfileEntry)element;
            switch (column) {
            case 0: return toHex(e.addr);
            case 1: return "";
            case 2: return "";
            case 3: return e.name;
            case 4: return e.file_base;
            case 5: return e.line == 0 ? null : Integer.toString(e.line);
            }
            return null;
        }
    }

    private class ProfileEntryRefLabelProvider extends LabelProvider implements ITableLabelProvider {

        public Image getColumnImage(Object element, int column) {
            return null;
        }

        public String getColumnText(Object element, int column) {
            ProfileEntryRef e = (ProfileEntryRef)element;
            switch (column) {
            case 0: return toHex(e.pe.addr);
            case 1: return "";
            case 2: return "";
            case 3: return e.pe.name;
            case 4: return e.pe.file_base;
            case 5: return e.pe.line == 0 ? null : Integer.toString(e.pe.line);
            }
            return null;
        }
    }

    private final Action action_start = new Action("Start", ImageCache.getImageDescriptor(ImageCache.IMG_THREAD_RUNNNIG)) {
        @Override
        public void run() {
            final TCFNode node = selection;
            if (node == null) return;
            Map<String,Object> conf = new TCFTask<Map<String,Object>>() {
                @Override
                public void run() {
                    done(getConfiguration(node));
                }
            }.getE();
            ProfilerSettingsDlg dlg = new ProfilerSettingsDlg(getSite().getShell(), conf);
            if (dlg.open() == Window.OK) {
                final Map<String,Object> params = dlg.data;
                Protocol.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        configuration.put(node.getID(), params);
                        Integer n = (Integer)params.get(PARAM_VIEW_UPDATE_PERIOD);
                        if (n != null) node.getModel().setProfilerReadDelay(n.intValue());
                        if (selection != node) return;
                        start(selection);
                    }
                });
            }
        }
    };

    private final Action action_stop = new Action("Stop", ImageCache.getImageDescriptor(ImageCache.IMG_THREAD_SUSPENDED)) {
        @Override
        public void run() {
            Protocol.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (selection == null) return;
                    stop(selection);
                }
            });
        }
    };

    private boolean disposed;
    private TCFNode selection;
    private int sorting;
    private Update last_update;
    private Composite parent;
    private Label status;
    private TableViewer viewer_main;
    private TableViewer viewer_up;
    private TableViewer viewer_dw;
    private Composite main_composite;
    private TCFNode profile_node;
    private int sample_count;
    private boolean total_count;
    private HashMap<RGB,Color> colors = new HashMap<RGB,Color>();

    private static final String[] column_ids = {
        "Address",
        "% Exclusive",
        "% Inclusive",
        "Function",
        "File",
        "Line"
    };

    private static final int[] column_size = {
        80,
        60,
        60,
        250,
        250,
        60
    };

    private final Map<String,Map<String,Object>> configuration = new HashMap<String,Map<String,Object>>();
    private final Map<TCFModel,ProfileModel> models = new HashMap<TCFModel,ProfileModel>();

    @Override
    public void createPartControl(Composite parent) {
        assert !disposed;
        this.parent = parent;

        Font font = parent.getFont();
        final Composite composite = new Composite(parent, SWT.NO_FOCUS | SWT.H_SCROLL);
        composite.setFont(font);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        main_composite = composite;

        final Composite table = createTable(composite);

        composite.setLayout(new Layout() {
            @Override
            protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
                Point p = table.computeSize(SWT.DEFAULT, hHint, flushCache);
                if (p.x < wHint) p.x = wHint;
                p.y = hHint;
                return p;
            }
            @Override
            protected void layout(Composite composite, boolean flushCache) {
                for (;;) {
                    Rectangle rc = composite.getClientArea();
                    Point p = table.computeSize(SWT.DEFAULT, rc.height, true);
                    if (p.x < rc.width) p.x = rc.width;
                    ScrollBar sb = composite.getHorizontalBar();
                    int pos = 0;
                    boolean vis = sb.getVisible();
                    if (p.x > rc.width) {
                        pos = sb.getSelection();
                        if (pos > p.x - rc.width) pos = p.x - rc.width;
                        sb.setValues(pos, 0, p.x, rc.width, 1, 1);
                    }
                    table.setBounds(-pos, rc.y, p.x, rc.height);
                    if (vis == (p.x > rc.width)) break;
                    sb.setVisible(!vis);
                }
            }
        });

        composite.getHorizontalBar().addListener(SWT.Selection, new Listener () {
            public void handleEvent(Event e) {
                Point location = table.getLocation();
                ScrollBar sb = composite.getHorizontalBar();
                int pos = sb.getSelection();
                table.setLocation(-pos, location.y);
            }
        });

        action_start.setEnabled(false);
        action_stop.setEnabled(false);
        IActionBars action_bars = getViewSite().getActionBars();
        IToolBarManager tool_bar = action_bars.getToolBarManager();
        tool_bar.add(action_start);
        tool_bar.add(action_stop);

        Protocol.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                TCFModelManager.getModelManager().addListener(launch_listener);
            }
        });

        IWorkbenchWindow window = getSite().getWorkbenchWindow();
        IDebugContextService dcs = DebugUITools.getDebugContextManager().getContextService(window);
        dcs.addDebugContextListener(selection_listener);
        ISelection active_context = dcs.getActiveContext();
        selectionChanged(active_context);
    }

    private Composite createTable(Composite parent) {
        Font font = parent.getFont();
        final Composite composite = new Composite(parent, SWT.NO_FOCUS);
        final FormLayout layout = new FormLayout();
        composite.setFont(font);
        composite.setLayout(layout);

        Composite main = createMainTable(composite);
        final Sash sash = new Sash(composite, SWT.HORIZONTAL);
        Composite details = createDetailsPane(composite);

        FormData form_data_main = new FormData();
        form_data_main.left = new FormAttachment(0, 0);
        form_data_main.right = new FormAttachment(100, 0);
        form_data_main.top = new FormAttachment(0, 0);
        form_data_main.bottom = new FormAttachment(sash, 0);
        main.setLayoutData(form_data_main);

        final int limit = 20, percent = 60;
        final FormData form_data_sash = new FormData();
        form_data_sash.left = new FormAttachment(0, 0);
        form_data_sash.right = new FormAttachment(100, 0);
        form_data_sash.top = new FormAttachment(percent, 0);
        sash.setLayoutData(form_data_sash);
        sash.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                Rectangle rect_sash = sash.getBounds();
                Rectangle rect_view = composite.getClientArea();
                int top = rect_view.height - rect_sash.height - limit;
                e.y = Math.max(Math.min(e.y, top), limit);
                if (e.y != rect_sash.y) {
                    form_data_sash.top = new FormAttachment(0, e.y);
                    composite.layout();
                }
            }
        });

        FormData form_data_details = new FormData();
        form_data_details.left = new FormAttachment(0, 0);
        form_data_details.right = new FormAttachment(100, 0);
        form_data_details.top = new FormAttachment(sash, 0);
        form_data_details.bottom = new FormAttachment(100, 0);
        details.setLayoutData(form_data_details);

        return composite;
    }

    private Composite createMainTable(Composite parent) {
        Font font = parent.getFont();
        Composite composite = new Composite(parent, SWT.NO_FOCUS | SWT.BORDER);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setFont(font);
        composite.setLayout(layout);
        status = new Label(composite, SWT.NONE);
        status.setFont(font);
        status.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        viewer_main = new TableViewer(composite, SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
        final Table table = viewer_main.getTable();
        table.setLayoutData(new GridData(GridData.FILL_BOTH));
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setFont(font);
        viewer_main.setContentProvider(new ProfileContentProvider());
        viewer_main.setLabelProvider(new ProfileEntryLabelProvider());
        viewer_main.setColumnProperties(column_ids);

        for (int i = 0; i < column_ids.length; i++) {
            createColumn(table, i);
        }

        table.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                disposeColors();
                if (e.item == null) return;
                ProfileEntry pe = (ProfileEntry)viewer_main.getElementAt(table.indexOf((TableItem)e.item));
                viewer_up.setInput(pe.up);
                viewer_dw.setInput(pe.dw);
                displaySource(pe);
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        table.addListener(SWT.PaintItem, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (event.index == 1 || event.index == 2 && total_count) {
                    int index = table.indexOf((TableItem)event.item);
                    ProfileEntry pe = ((ProfileEntry[])viewer_main.getInput())[index];
                    float count = event.index == 1 ? (float)pe.count : pe.total;
                    paintPercent(event, count);
                }
            }
        });

        return composite;
    }

    private Composite createDetailsPane(Composite parent) {
        Font font = parent.getFont();
        Composite composite = new Composite(parent, SWT.NO_FOCUS | SWT.BORDER);
        composite.setFont(font);

        final Label label_up = new Label(composite, SWT.NONE);
        label_up.setFont(font);
        label_up.setText("Called From");

        viewer_up = new TableViewer(composite, SWT.V_SCROLL | SWT.FULL_SELECTION);
        final Table table_up = viewer_up.getTable();
        table_up.setHeaderVisible(false);
        table_up.setLinesVisible(true);
        table_up.setFont(font);
        viewer_up.setContentProvider(new ProfileContentProvider());
        viewer_up.setLabelProvider(new ProfileEntryRefLabelProvider());
        viewer_up.setColumnProperties(column_ids);

        table_up.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (e.item == null) return;
                displaySource(((ProfileEntryRef)viewer_up.getElementAt(table_up.indexOf((TableItem)e.item))).pe);
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                if (e.item == null) return;
                displayEntry(((ProfileEntryRef)viewer_up.getElementAt(table_up.indexOf((TableItem)e.item))).pe);
            }
        });

        table_up.addListener(SWT.PaintItem, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (event.index == 2) {
                    int index = table_up.indexOf((TableItem)event.item);
                    ProfileEntryRef pe = ((ProfileEntryRef[])viewer_up.getInput())[index];
                    paintPercent(event, pe.total);
                }
            }
        });

        final Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setFont(font);

        final Label label_dw = new Label(composite, SWT.NONE);
        label_dw.setFont(font);
        label_dw.setText("Child Calls");

        viewer_dw = new TableViewer(composite, SWT.V_SCROLL | SWT.FULL_SELECTION);
        final Table table_dw = viewer_dw.getTable();
        table_dw.setHeaderVisible(false);
        table_dw.setLinesVisible(true);
        table_dw.setFont(font);
        viewer_dw.setContentProvider(new ProfileContentProvider());
        viewer_dw.setLabelProvider(new ProfileEntryRefLabelProvider());
        viewer_dw.setColumnProperties(column_ids);

        table_dw.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (e.item == null) return;
                displaySource(((ProfileEntryRef)viewer_dw.getElementAt(table_dw.indexOf((TableItem)e.item))).pe);
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                if (e.item == null) return;
                displayEntry(((ProfileEntryRef)viewer_dw.getElementAt(table_dw.indexOf((TableItem)e.item))).pe);
            }
        });

        table_dw.addListener(SWT.PaintItem, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (event.index == 2) {
                    int index = table_dw.indexOf((TableItem)event.item);
                    ProfileEntryRef pe = ((ProfileEntryRef[])viewer_dw.getInput())[index];
                    paintPercent(event, pe.total);
                }
            }
        });

        for (int i = 0; i < column_ids.length; i++) {
            createColumn(table_up, i);
            createColumn(table_dw, i);
        }

        composite.setLayout(new Layout() {
            @Override
            protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
                Point l_up = label_up.computeSize(wHint, SWT.DEFAULT);
                Point t_up = table_up.computeSize(wHint, SWT.DEFAULT);
                Point sep = separator.computeSize(wHint, SWT.DEFAULT);
                Point l_dw = label_dw.computeSize(wHint, SWT.DEFAULT);
                Point t_dw = table_dw.computeSize(wHint, SWT.DEFAULT);
                int w = 0;
                if (l_up.x > w) w = l_up.x;
                if (t_up.x > w) w = t_up.x;
                if (sep.x > w) w = sep.x;
                if (l_dw.x > w) w = l_dw.x;
                if (t_dw.x > w) w = t_dw.x;
                int h = l_up.y + t_up.y + sep.y + l_dw.y + t_dw.y;
                return new Point(w, h);
            }

            @Override
            protected void layout(Composite composite, boolean flushCache) {
                Rectangle rc = composite.getClientArea();
                Point l_up = label_up.computeSize(rc.width, SWT.DEFAULT);
                Point sep = separator.computeSize(rc.width, SWT.DEFAULT);
                Point l_dw = label_dw.computeSize(rc.width, SWT.DEFAULT);
                int h = (rc.height - l_up.y - sep.y - l_dw.y) / 2;
                if (h < 0) h = 0;
                int y = rc.y;
                label_up.setBounds(rc.x, y, rc.width, l_up.y);
                y += l_up.y;
                table_up.setBounds(rc.x, y, rc.width, h);
                y += h;
                separator.setBounds(rc.x, y, rc.width, sep.y);
                y += sep.y;
                label_dw.setBounds(rc.x, y, rc.width, l_dw.y);
                y += l_dw.y;
                table_dw.setBounds(rc.x, y, rc.width, h);
            }
        });

        return composite;
    }

    private void createColumn(final Table table, int i) {
        final int n = i;
        final TableColumn c = new TableColumn(table, SWT.NONE, i);
        c.setText(column_ids[i]);
        c.setWidth(column_size[i]);
        if (i != 5) {
            c.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    table.setSortDirection(SWT.DOWN);
                    table.setSortColumn(c);
                    sorting = n;
                    Protocol.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            updateView();
                        }
                    });
                }
                @Override
                public void widgetDefaultSelected(SelectionEvent e) {
                }
            });
        }
        if (table == viewer_main.getTable()) {
            c.addControlListener(new ControlListener() {
                @Override
                public void controlResized(ControlEvent e) {
                    int w = c.getWidth();
                    if (n == column_size.length - 1 && !System.getProperty("os.name", "").startsWith("Windows")) {
                        // Workaround:
                        // Linux GTK tries to outsmart user: last column is auto-resized when table size changes.
                        // This causes infinite recursion and stack overflow.
                        if (w > column_size[n]) w = column_size[n];
                    }
                    if (viewer_up.getTable().getColumn(n).getWidth() == w &&
                        viewer_dw.getTable().getColumn(n).getWidth() == w) return;
                    viewer_up.getTable().getColumn(n).setWidth(w);
                    viewer_dw.getTable().getColumn(n).setWidth(w);
                    main_composite.layout();
                }
                @Override
                public void controlMoved(ControlEvent e) {
                }
            });
            if (i == 2) {
                table.setSortDirection(SWT.DOWN);
                table.setSortColumn(c);
                sorting = i;
            }
        }
    }

    private String toHex(BigInteger n) {
        String s = n.toString(16);
        if (s.length() >= 8) return s;
        return "00000000".substring(s.length()) + s;
    }

    private void disposeColors() {
        for (Color c : colors.values()) c.dispose();
        colors.clear();
    }

    private Color getColor(RGB rgb) {
        Color c = colors.get(rgb);
        if (c == null) {
            c = new Color(parent.getDisplay(), rgb);
            colors.put(rgb, c);
        }
        return c;
    }

    private String toPercent(float x) {
        float f = x * 100 / sample_count;
        String s = String.format("%.3f", f);
        int l = s.indexOf('.');
        if (l >= 3) s = "100";
        else if (l >= 2) s = s.substring(0, 4);
        else if (s.charAt(0) == '0') s = s.substring(1);
        else s = s.substring(0, 4);
        return s;
    }

    private void paintPercent(Event event, float count) {
        Table table = (Table)event.widget;
        int cell_width = table.getColumn(event.index).getWidth() - 1;
        if (cell_width > 2) {
            GC gc = event.gc;
            float percent = count * 100 / sample_count;
            int width = (int)(cell_width * percent / 100);
            if (width > cell_width) width = cell_width;
            if (width >= 2) {
                Color bg = gc.getBackground();
                Color fg = gc.getForeground();
                gc.setBackground(getColor(new RGB(bg.getRed() / 3 + 170, bg.getGreen() / 3 + 170, bg.getBlue() / 3)));
                gc.setForeground(getColor(new RGB(bg.getRed() / 3 + 170, bg.getGreen() / 3, bg.getBlue() / 3)));
                gc.fillGradientRectangle(event.x, event.y, width, event.height, true);
                gc.drawRectangle(new Rectangle(event.x, event.y, width - 1, event.height - 1));
                gc.setBackground(bg);
                gc.setForeground(fg);
            }
            String text = toPercent(count);
            Point size = event.gc.textExtent(text);
            int offset = Math.max(0, (event.height - size.y) / 2);
            gc.drawText(text, event.x + 2, event.y + offset, true);
        }
    }

    private void selectionChanged(ISelection s) {
        if (s instanceof IStructuredSelection) {
            final Object obj = ((IStructuredSelection)s).getFirstElement();
            Protocol.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (obj instanceof TCFNode) {
                        selection = (TCFNode)obj;
                    }
                    else {
                        selection = null;
                    }
                    updateView();
                }
            });
        }
    }

    private void setSelection(List<ProfileEntry> l, boolean reveal) {
        viewer_main.setSelection(new StructuredSelection(l), reveal);
        if (l.size() == 0) {
            viewer_up.setInput(null);
            viewer_dw.setInput(null);
        }
        else {
            ProfileEntry pe = l.get(0);
            viewer_up.setInput(pe.up);
            viewer_dw.setInput(pe.dw);
        }
    }

    private void setSelection(ProfileEntry pe, boolean reveal) {
        List<ProfileEntry> l = new ArrayList<ProfileEntry>();
        if (pe != null) l.add(pe);
        setSelection(l, reveal);
    }

    private void displaySource(ProfileEntry pe) {
        if (profile_node == null) return;
        if (pe.file_full == null) return;
        profile_node.getModel().displaySource(profile_node.getID(), pe.file_full, pe.line);
    }

    private void displayEntry(ProfileEntry pe) {
        setSelection(pe, true);
    }

    @Override
    public void setFocus() {
        viewer_main.getControl().setFocus();
    }

    private void asyncExec(Runnable r) {
        try {
            parent.getDisplay().asyncExec(r);
        }
        catch (SWTException x) {
            // Widget is disposed
        }
    }

    private Map<String,Object> getConfiguration(TCFNode node) {
        String ctx = node.getID();
        Map<String,Object> params = configuration.get(ctx);
        if (params == null) {
            params = new HashMap<String,Object>();
            params.put(IProfiler.PARAM_FRAME_CNT, FRAME_COUNT);
            params.put(PARAM_AGGREGATE, Boolean.TRUE);
            params.put(PARAM_STACK_TRACE, Boolean.FALSE);
            configuration.put(ctx,  params);
        }
        params.put(PARAM_VIEW_UPDATE_PERIOD, node.getModel().getProfilerReadDelay());
        return params;
    }

    private void start(TCFNode node) {
        assert Protocol.isDispatchThread();
        configure(node, getConfiguration(node));
    }

    private void stop(TCFNode node) {
        assert Protocol.isDispatchThread();
        configure(node, null);
    }

    private void configure(final TCFNode node, final Map<String,Object> params) {
        TCFModel model = node.getModel();
        Map<String,Object> prf_cfg = model.getProfilerConfiguration(node.getID());
        if (params != null) {
            Object frame_cnt = null;
            Boolean stack_trace = (Boolean)params.get(PARAM_STACK_TRACE);
            if (stack_trace != null && stack_trace.booleanValue()) {
                frame_cnt = params.get(IProfiler.PARAM_FRAME_CNT);
            }
            if (frame_cnt == null) frame_cnt = Integer.valueOf(1);
            prf_cfg.put(IProfiler.PARAM_FRAME_CNT, frame_cnt);
        }
        else {
            prf_cfg.remove(IProfiler.PARAM_FRAME_CNT);
        }
        model.sendProfilerConfiguration(node.getID());
        ProfileModel prf_model = models.get(model);
        if (params == null) {
            ProfileData prf_data = null;
            if (prf_model != null) prf_data = prf_model.data.get(node.getID());
            if (prf_data != null) prf_data.stopped = true;
        }
        else {
            ProfileData d = new ProfileData(node.getID(), params);
            if (prf_model == null) {
                models.put(node.getModel(), prf_model = new ProfileModel());
                model.addProfilerDataListener(prf_model);
            }
            prf_model.data.put(d.ctx, d);
        }
        updateView();
    }

    private void addSamples(ProfileData p, Map<String,Object> props) {
        int size = 4;
        boolean big_endian = false;
        Number n = (Number)props.get(IProfiler.PROP_ADDR_SIZE);
        if (n != null) size = n.intValue();
        Boolean b = (Boolean)props.get(IProfiler.PROP_BIG_ENDIAN);
        if (b != null) big_endian = b.booleanValue();
        byte[] data = JSON.toByteArray(props.get(IProfiler.PROP_DATA));
        if (data == null || data.length == 0) return;
        int pos = 0;
        byte[] buf = new byte[size + 1];
        BigInteger[] trace = new BigInteger[p.map.length];
        for (;;) {
            int c = -1;
            int l = -1;
            int i = 0;
            while (pos + size <= data.length) {
                for (int j = 0; j < size; j++) {
                    buf[big_endian ? j + 1: size - j] = data[pos++];
                }
                if (i >= trace.length) continue;
                BigInteger a = new BigInteger(buf);
                if (c < 0) {
                    /* Count */
                    c = a.intValue();
                }
                else if (l < 0) {
                    /* Trace length */
                    l = a.intValue();
                }
                else {
                    /* Trace addresses */
                    trace[i++] = a;
                    if (i == l) break;
                }
            }
            if (l < 0) break;
            if (i > 0) addSample(p, trace, i, c);
        }
        updateView();
    }

    private void addSample(ProfileData p, BigInteger[] trace, int len, int cnt) {
        assert len > 0;
        p.sample_count += cnt;
        p.generation_inp++;
        ProfileSample ps = null;
        if (len > p.map.length) len = p.map.length;
        for (int f = 0; f < p.map.length && f < len; f++) {
            List<ProfileSample> lp = p.map[f].get(trace[f]);
            if (lp != null) {
                boolean ok = false;
                for (ProfileSample s : lp) {
                    if (len == s.trace.length) {
                        int i = 0;
                        while (i < len && trace[i].equals(s.trace[i])) i++;
                        if (i == len) {
                            assert ps == null || ps == s;
                            ps = s;
                            ok = true;
                        }
                    }
                }
                if (ok) continue;
            }
            else {
                p.map[f].put(trace[f], lp = new ArrayList<ProfileSample>());
            }
            if (ps == null) {
                BigInteger[] t = new BigInteger[len];
                for (int i = 0; i < len; i++) t[i] = trace[i];
                ps = new ProfileSample(t);
            }
            lp.add(ps);
        }
        ps.cnt += cnt;
    }

    private void updateView() {
        assert Protocol.isDispatchThread();
        Protocol.invokeLater(new Update());
    }

    @Override
    public void dispose() {
        disposed = true;
        Protocol.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                TCFModelManager.getModelManager().removeListener(launch_listener);
                models.clear();
            }
        });
        IWorkbenchWindow window = getSite().getWorkbenchWindow();
        IDebugContextService dcs = DebugUITools.getDebugContextManager().getContextService(window);
        dcs.removeDebugContextListener(selection_listener);
        disposeColors();
        super.dispose();
    }
}
