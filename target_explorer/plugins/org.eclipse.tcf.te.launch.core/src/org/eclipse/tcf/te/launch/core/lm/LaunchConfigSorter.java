/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.launch.core.lm;

import org.eclipse.core.runtime.Assert;
import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * Implementation of the Comparable interface for comparing launch configuration by their ranking.
 */
public class LaunchConfigSorter implements Comparable<LaunchConfigSorter> {
	private final int ranking;
	private final ILaunchConfiguration config;

	/**
	 * Constructor.
	 *
	 * @param config The launch configuration. Must not be <code>null</code>
	 * @param ranking The launch configuration ranking.
	 */
	public LaunchConfigSorter(ILaunchConfiguration config, int ranking) {
		Assert.isNotNull(config);

		this.config = config;
		this.ranking = ranking;
	}

	/**
	 * Returns the launch configuration.
	 *
	 * @return The launch configuration.
	 */
	public ILaunchConfiguration getConfig() {
		return config;
	}

	/**
	 * Returns the launch configuration ranking.
	 *
	 * @return The launch configuration ranking.
	 */
	public int getRanking() {
		return ranking;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
	    return ranking;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LaunchConfigSorter) {
			return ranking == ((LaunchConfigSorter)obj).ranking;
		}
	    return super.equals(obj);
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
    public int compareTo(LaunchConfigSorter other) {
		if (other.getRanking() > ranking) {
			return 1;
		} else if (other.getRanking() == ranking) {
			return 0;
		}

		return -1;
	}
}
