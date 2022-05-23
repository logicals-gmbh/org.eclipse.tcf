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
package org.eclipse.tcf.internal.debug.ui.model;

import org.eclipse.debug.internal.ui.viewers.model.provisional.IColumnPresentation;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.jface.resource.ImageDescriptor;

public class TCFColumnPresentationExpression implements IColumnPresentation {

    public static final String PRESENTATION_ID = "Expressions";

    /**
     * Presentation column IDs.
     */
    public static final String
        COL_NAME = "Name",
        COL_TYPE = "Type",
        COL_HEX_VALUE = "HexValue",
        COL_DEC_VALUE = "DecValue",
        COL_VALUE = "Value";

    private static String[] cols_all = {
        COL_NAME,
        COL_TYPE,
        COL_DEC_VALUE,
        COL_HEX_VALUE,
        COL_VALUE,
    };

    private static String[] headers  = {
        "Name",
        "Type",
        "Decimal",
        "Hex",
        "Value",
    };

    private static String[] cols_ini = {
        COL_NAME,
        COL_TYPE,
        COL_VALUE,
    };

    public void dispose() {
    }

    public String[] getAvailableColumns() {
        return cols_all;
    }

    public String getHeader(String id) {
        for (int i = 0; i < cols_all.length; i++) {
            if (id.equals(cols_all[i])) return headers[i];
        }
        return null;
    }

    public String getId() {
        return PRESENTATION_ID;
    }

    public ImageDescriptor getImageDescriptor(String id) {
        return null;
    }

    public String[] getInitialColumns() {
        return cols_ini;
    }

    public void init(IPresentationContext context) {
    }

    public boolean isOptional() {
        return true;
    }
}
