/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.statushandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.runtime.statushandler.interfaces.IStatusHandler;
import org.eclipse.tcf.te.runtime.statushandler.interfaces.IStatusHandlerConstants;
import org.eclipse.tcf.te.runtime.statushandler.internal.StatusHandlerBinding;
import org.eclipse.tcf.te.runtime.statushandler.internal.StatusHandlerBindingExtensionPointManager;
import org.eclipse.tcf.te.runtime.extensions.AbstractExtensionPointManager;
import org.eclipse.tcf.te.runtime.extensions.ExecutableExtensionProxy;

/**
 * Status handler manager implementation.
 */
public final class StatusHandlerManager extends AbstractExtensionPointManager<IStatusHandler> {

	/*
	 * Thread save singleton instance creation.
	 */
	private static class LazyInstance {
		public static StatusHandlerManager instance = new StatusHandlerManager();
	}

	/**
	 * Constructor.
	 */
	StatusHandlerManager() {
		super();
	}

	/**
	 * Returns the singleton instance of the extension point manager.
	 */
	public static StatusHandlerManager getInstance() {
		return LazyInstance.instance;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.extensions.AbstractExtensionPointManager#getExtensionPointId()
	 */
	@Override
	protected String getExtensionPointId() {
		return "org.eclipse.tcf.te.runtime.statushandler.handlers"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.extensions.AbstractExtensionPointManager#getConfigurationElementName()
	 */
	@Override
	protected String getConfigurationElementName() {
		return "handler"; //$NON-NLS-1$
	}

	/**
	 * Returns the list of all contributed status handler.
	 *
	 * @param unique If <code>true</code>, the method returns new instances for each
	 *               contributed status handler.
	 *
	 * @return The list of contributed status handler, or an empty array.
	 */
	public IStatusHandler[] getHandlers(boolean unique) {
		List<IStatusHandler> contributions = new ArrayList<IStatusHandler>();
		Collection<ExecutableExtensionProxy<IStatusHandler>> statusHandlers = getExtensions().values();
		for (ExecutableExtensionProxy<IStatusHandler> statusHandler : statusHandlers) {
			IStatusHandler instance = unique ? statusHandler.newInstance() : statusHandler.getInstance();
			if (instance != null && !contributions.contains(instance)) {
				contributions.add(instance);
			}
		}

		return contributions.toArray(new IStatusHandler[contributions.size()]);
	}

	/**
	 * Returns the status handler identified by its unique id. If no status
	 * handler with the specified id is registered, <code>null</code> is returned.
	 *
	 * @param id The unique id of the status handler or <code>null</code>
	 * @param unique If <code>true</code>, the method returns new instances of the status handler contribution.
	 *
	 * @return The status handler instance or <code>null</code>.
	 */
	public IStatusHandler getHandler(String id, boolean unique) {
		IStatusHandler contribution = null;
		if (getExtensions().containsKey(id)) {
			ExecutableExtensionProxy<IStatusHandler> proxy = getExtensions().get(id);
			// Get the extension instance
			contribution = unique ? proxy.newInstance() : proxy.getInstance();
		}

		return contribution;
	}

	/**
	 * Returns the status handler which are enabled for the given
	 * handler context.
	 *
	 * @param context The handler context. Must not be <code>null</code>.
	 * @return The list of status handler which are enabled or an empty array.
	 */
	public IStatusHandler[] getHandler(Object context) {
		Assert.isNotNull(context);

		List<IStatusHandler> handlers = new ArrayList<IStatusHandler>();

		// Get the list of applicable bindings
		StatusHandlerBinding[] bindings = StatusHandlerBindingExtensionPointManager.getInstance().getApplicableBindings(context);
		for (StatusHandlerBinding binding : bindings) {
			IStatusHandler handler = getHandler(binding.getHandlerId(), false);
			if (handler != null && !handlers.contains(handler)) handlers.add(handler);
		}

		// If no applicable status handler is found, always return the default status handler
		if (handlers.isEmpty()) {
			IStatusHandler handler = getHandler(IStatusHandlerConstants.ID_DEFAUT_HANDLER, false);
			Assert.isNotNull(handler);
			handlers.add(handler);
		}

		return handlers.toArray(new IStatusHandler[handlers.size()]);
	}
}
