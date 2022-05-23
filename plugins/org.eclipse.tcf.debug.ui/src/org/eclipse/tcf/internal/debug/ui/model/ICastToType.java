/*******************************************************************************
 * Copyright (c) 2009, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.model;

import org.eclipse.tcf.services.ISymbols;
import org.eclipse.tcf.util.TCFDataCache;

public interface ICastToType {

    void onCastToTypeChanged();

    TCFDataCache<ISymbols.Symbol> getType();
}
