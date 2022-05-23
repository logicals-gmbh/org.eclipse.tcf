/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.extensions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.te.runtime.activator.CoreBundleActivator;
import org.eclipse.tcf.te.runtime.nls.Messages;

/**
 * Abstract extension point manager implementation.
 */
public abstract class AbstractExtensionPointManager<V> {
	// Flag to mark the extension point manager initialized (extensions loaded).
	private boolean initialized = false;
	// The map of loaded extension listed by their unique id's
	private Map<String, ExecutableExtensionProxy<V>> extensionsMap = new LinkedHashMap<String, ExecutableExtensionProxy<V>>();
	// The extension point comparator
	private ExtensionPointComparator comparator = null;

	/**
	 * Constructor.
	 */
	public AbstractExtensionPointManager() {
	}

	/**
	 * Returns if or if not the extension point manager got initialized already.
	 * <p>
	 * Initialized means that the manager read the extensions for the managed extension point.
	 *
	 * @return <code>True</code> if already initialized, <code>false</code> otherwise.
	 */
	protected boolean isInitialized() {
		return initialized;
	}

	/**
	 * Sets if or if not the extension point manager is initialized.
	 * <p>
	 * Initialized means that the manager has read the extensions for the managed extension point.
	 *
	 * @return <code>True</code> to set the extension point manager is initialized, <code>false</code> otherwise.
	 */
	protected void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	/**
	 * Returns the map of managed extensions. If not loaded before,
	 * this methods trigger the loading of the extensions to the managed
	 * extension point.
	 *
	 * @return The map of extensions.
	 */
	protected Map<String, ExecutableExtensionProxy<V>> getExtensions() {
		// Load and store the extensions thread-safe!
		synchronized (extensionsMap) {
			if (!isInitialized()) { loadExtensions(); setInitialized(true); }
		}
		return extensionsMap;
	}

	/**
	 * Returns the extensions of the specified extension point sorted.
	 * <p>
	 * For the order of the extensions, see {@link ExtensionPointComparator}.
	 *
	 * @param point The extension point. Must not be <code>null</code>.
	 * @return The extensions in sorted order or an empty array if the extension point has no extensions.
	 */
	protected IExtension[] getExtensionsSorted(IExtensionPoint point) {
		Assert.isNotNull(point);

		List<IExtension> extensions = new ArrayList<IExtension>(Arrays.asList(point.getExtensions()));
		if (extensions.size() > 0) {
			Collections.sort(extensions, getExtensionPointComparator());
		}

		return extensions.toArray(new IExtension[extensions.size()]);
	}

	/**
	 * Returns the extension point comparator instance. If not available,
	 * {@link #doCreateExtensionPointComparator()} is called to create a new instance.
	 *
	 * @return The extension point comparator or <code>null</code> if the instance creation fails.
	 */
	protected final ExtensionPointComparator getExtensionPointComparator() {
		if (comparator == null) {
			comparator = doCreateExtensionPointComparator();
		}
		return comparator;
	}

	/**
	 * Creates a new extension point comparator instance.
	 *
	 * @return The extension point comparator instance.
	 */
	protected ExtensionPointComparator doCreateExtensionPointComparator() {
		return new ExtensionPointComparator();
	}

	/**
	 * Returns the extension point id to read. The method
	 * must return never <code>null</code>.
	 *
	 * @return The extension point id.
	 */
	protected abstract String getExtensionPointId();

	/**
	 * Returns the configuration element name. The method
	 * must return never <code>null</code>.
	 *
	 * @return The configuration element name.
	 */
	protected abstract String getConfigurationElementName();

	/**
	 * Creates the extension proxy instance.
	 *
	 * @param element The configuration element of the extension. Must not be <code>null</code>.
	 * @return The extension proxy instance.
	 *
	 * @throws CoreException If the extension proxy instantiation failed.
	 */
	protected ExecutableExtensionProxy<V> doCreateExtensionProxy(IConfigurationElement element) throws CoreException {
		Assert.isNotNull(element);
		return new ExecutableExtensionProxy<V>(element);
	}

	/**
	 * Store the given extension to the given extensions store. Checks if an extension with the same id does exist
	 * already and throws an exception in this case.
	 *
	 * @param extensions The extensions store. Must not be <code>null</code>.
	 * @param candidate The extension. Must not be <code>null</code>.
	 * @param element The configuration element. Must not be <code>null</code>.
	 *
	 * @throws CoreException In case a extension with the same id as the given extension already exist.
	 */
	protected void doStoreExtensionTo(Map<String, ExecutableExtensionProxy<V>> extensions, ExecutableExtensionProxy<V> candidate, IConfigurationElement element) throws CoreException {
		Assert.isNotNull(extensions);
		Assert.isNotNull(candidate);
		Assert.isNotNull(element);

		// If no extension with this id had been registered before, register now.
		if (!extensions.containsKey(candidate.getId())) {
			extensions.put(candidate.getId(), candidate);
		}
		else {
			throw new CoreException(new Status(IStatus.ERROR,
					CoreBundleActivator.getUniqueIdentifier(),
					0,
					NLS.bind(Messages.Extension_error_duplicateExtension, candidate.getId(), element.getContributor().getName()),
					null));
		}
	}

	/**
	 * Loads the extensions for the managed extension point.
	 */
	protected void loadExtensions() {
		// If already initialized, this method will do nothing.
		if (isInitialized())  return;

		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint point = registry.getExtensionPoint(getExtensionPointId());
		if (point != null) {
			IExtension[] extensions = getExtensionsSorted(point);
			for (IExtension extension : extensions) {
				IConfigurationElement[] elements = extension.getConfigurationElements();
				for (IConfigurationElement element : elements) {
					if (getConfigurationElementName().equals(element.getName())) {
						try {
							ExecutableExtensionProxy<V> candidate = doCreateExtensionProxy(element);
							if (candidate.getId() != null) {
								doStoreExtensionTo(extensionsMap, candidate, element);
							} else {
								throw new CoreException(new Status(IStatus.ERROR,
										CoreBundleActivator.getUniqueIdentifier(),
										0,
										NLS.bind(Messages.Extension_error_missingRequiredAttribute, "id", element.getAttribute("label")), //$NON-NLS-1$ //$NON-NLS-2$
										null));
							}
						} catch (CoreException e) {
							Platform.getLog(CoreBundleActivator.getContext().getBundle()).log(new Status(IStatus.ERROR,
									CoreBundleActivator.getUniqueIdentifier(),
									NLS.bind(Messages.Extension_error_invalidExtensionPoint, element.getDeclaringExtension().getUniqueIdentifier()), e));
						}
					}
				}
			}
		}
	}
}
