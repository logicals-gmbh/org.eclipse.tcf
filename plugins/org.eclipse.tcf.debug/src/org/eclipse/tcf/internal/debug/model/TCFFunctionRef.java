/*******************************************************************************
 * Copyright (c) 2008, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.model;

import java.math.BigInteger;

/**
 * Objects of this class represent a mapping between an address and a function.
 */
public class TCFFunctionRef {
    public String context_id;
    public int address_size;
    public BigInteger address;
    public String symbol_id;
    public Throwable error;

    public String toString() {
        StringBuffer bf = new StringBuffer();
        bf.append('[');
        bf.append(context_id);
        bf.append(',');
        bf.append(address_size);
        bf.append(',');
        bf.append(address);
        bf.append(',');
        bf.append(symbol_id);
        bf.append(',');
        bf.append(error);
        bf.append(']');
        return bf.toString();
    }
}
