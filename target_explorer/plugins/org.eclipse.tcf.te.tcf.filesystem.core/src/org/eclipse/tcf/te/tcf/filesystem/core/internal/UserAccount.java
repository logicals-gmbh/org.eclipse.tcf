/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * William Chen (Wind River) - [352302]Opening a file in an editor depending on
 *                             the client's permissions.
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal;

import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IUserAccount;

/**
 * The data model of a user account.
 */
public class UserAccount implements IUserAccount {
	// The user's id.
	private int uid;
	// The user's group id.
	private int gid;
	// The user's effective id.
	private int euid;
	// The user's effective group id.
	private int egid;
	// The user's home directory.
	private String home;

	/**
	 * Create a user account with given data.
	 *
	 * @param uid
	 *            The user's id
	 * @param gid
	 *            The user's group id
	 * @param euid
	 *            The user's effective id.
	 * @param egid
	 *            The user's effective group id.
	 * @param home
	 *            The user's home directory.
	 */
	public UserAccount(int uid, int gid, int euid, int egid, String home) {
		this.uid = uid;
		this.gid = gid;
		this.euid = euid;
		this.egid = egid;
		this.home = home;
	}

	/**
	 * Get the user's id.
	 *
	 * @return The user's id.
	 */
	public int getUID() {
		return uid;
	}

	/**
	 * Get the user's group id.
	 *
	 * @return The user's group id.
	 */
	public int getGID() {
		return gid;
	}

	/**
	 * Get the user's effective id.
	 *
	 * @return The user's effective id.
	 */
	@Override
	public int getEUID() {
		return euid;
	}

	/**
	 * Get the user's effective group id.
	 *
	 * @return The user's effective group id.
	 */
	@Override
	public int getEGID() {
		return egid;
	}

	/**
	 * Get the user's home directory.
	 *
	 * @return The user's home directory.
	 */
	public String getHome() {
		return home;
	}
}
