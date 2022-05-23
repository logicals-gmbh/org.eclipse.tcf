/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.internal.utils;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.te.tcf.core.activator.CoreBundleActivator;


/**
 * Logging utilities helper implementations.
 */
public final class LogUtils {

	/**
	 * Log the given message for the given channel.
	 *
	 * @param channel The channel. Must not be <code>null</code>.
	 * @param message The message to log. Must not be <code>null</code>.
	 * @param slotId The Eclipse debug slot id which must be enabled to log the message. Must not be <code>null</code>.
	 * @param clazz The invoking class or <code>null</code>.
	 */
	public static void logMessageForChannel(IChannel channel, String message, String slotId, Object clazz) {
		Assert.isNotNull(channel);
		Assert.isNotNull(message);
		Assert.isNotNull(slotId);

		// Trace the message
		String fullMessage = channel.toString() + ": " + message; //$NON-NLS-1$

		if (Boolean.parseBoolean(Platform.getDebugOption(CoreBundleActivator.getUniqueIdentifier() + "/" + slotId))) { //$NON-NLS-1$
			IStatus status = new Status(IStatus.INFO, CoreBundleActivator.getUniqueIdentifier(), fullMessage.trim());
			Platform.getLog(CoreBundleActivator.getContext().getBundle()).log(status);
		}
	}
}
