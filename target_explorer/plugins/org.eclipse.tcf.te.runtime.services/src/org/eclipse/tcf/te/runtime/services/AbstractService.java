/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.services;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;

/**
 * Abstract service implementation.
 */
public abstract class AbstractService extends PlatformObject implements IService {

	private String id = null;

	/**
	 * Constructor.
	 */
	public AbstractService() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.services.IService#setId(java.lang.String)
	 */
	@Override
    public final void setId(String id) {
		if (this.id == null) this.id = id;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.services.IService#getId()
	 */
	@Override
    public final String getId() {
		return id;
	}

}
