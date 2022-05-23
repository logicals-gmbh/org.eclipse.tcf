/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.model;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;

class StyledStringBuffer {

    /**
     * The font style constants.
     */
    public static final int
        NORMAL          = SWT.NORMAL,
        BOLD            = SWT.BOLD,
        ITALIC          = SWT.ITALIC,
        MONOSPACED      = 1 << 2;

    private final StringBuffer bf = new StringBuffer();
    private final ArrayList<Style> styles = new ArrayList<Style>();

    static class Style {
        int pos;
        int len;
        int font;
        RGB bg;
        RGB fg;
    }

    private boolean full_error_reports;

    void enableFullErrorReports(boolean full_error_reports) {
        this.full_error_reports = full_error_reports;
    }

    StyledStringBuffer append(int pos, int font, RGB bg, RGB fg) {
        Style x = new Style();
        x.pos = pos;
        x.len = bf.length() - pos;
        x.font = font;
        x.bg = bg;
        x.fg = fg;
        styles.add(x);
        return this;
    }

    StyledStringBuffer append(String s) {
        bf.append(s);
        return this;
    }

    StyledStringBuffer append(char ch) {
        bf.append(ch);
        return this;
    }

    StyledStringBuffer append(int i) {
        bf.append(i);
        return this;
    }

    StyledStringBuffer append(String s, int font) {
        Style x = new Style();
        x.pos = bf.length();
        x.len = s.length();
        x.font = font;
        styles.add(x);
        bf.append(s);
        return this;
    }

    StyledStringBuffer append(String s, int font, RGB bg, RGB fg) {
        Style x = new Style();
        x.pos = bf.length();
        x.len = s.length();
        x.font = font;
        x.bg = bg;
        x.fg = fg;
        styles.add(x);
        bf.append(s);
        return this;
    }

    StyledStringBuffer append(StyledStringBuffer s) {
        int offs = bf.length();
        for (Style y : s.styles) {
            Style x = new Style();
            x.pos = y.pos + offs;
            x.len = y.len;
            x.font = y.font;
            x.bg = y.bg;
            x.fg = y.fg;
            styles.add(x);
        }
        bf.append(s.bf);
        return this;
    }

    StyledStringBuffer append(Throwable x, RGB color) {
        if (x == null) return this;
        if (full_error_reports) {
            String[] a = ("Exception: " + TCFModel.getErrorMessage(x, true)).split("\n");
            for (String s : a) {
                int i = s.indexOf(':');
                if (i >= 0) {
                    append(s.substring(0, i + 1), SWT.BOLD, null, color);
                    append(s.substring(i + 1), SWT.ITALIC, null, color);
                }
                else {
                    append(s, SWT.ITALIC, null, color);
                }
                bf.append('\n');
            }
        }
        else {
            append("Exception: ", SWT.BOLD, null, color);
            append(TCFModel.getErrorMessage(x, false), SWT.ITALIC, null, color);
            bf.append('\n');
        }
        return this;
    }

    StringBuffer getStringBuffer() {
        return bf;
    }

    Collection<Style> getStyle() {
        return styles;
    }

    int length() {
        return bf.length();
    }

    @Override
    public String toString() {
        return bf.toString();
    }
}
