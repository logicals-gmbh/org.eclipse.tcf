/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.properties;

import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.tcf.te.runtime.activator.CoreBundleActivator;
import org.eclipse.tcf.te.runtime.events.ChangeEvent;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.interfaces.tracing.ITraceIds;

/**
 * A generic properties container implementation.
 * <p>
 * <b>Note:</b> The properties container implementation is not thread-safe. Clients requiring
 *              a thread-safe implementation should subclass the properties container and
 *              overwrite {@link #checkThreadAccess()}.
 */
public class PropertiesContainer extends PlatformObject implements IPropertiesContainer {
	// Used to have a simple check that the random generated UUID isn't
	// the same if objects of this type are created very rapidly.
	private volatile static UUID LAST_UUID_GENERATED = null;

	// The unique node id
	private final UUID uniqueId;

	// The flag to remember the notification enablement
	private boolean changeEventsEnabled = false;

	/**
	 * The custom properties map. The keys are always strings, the value might be any object.
	 */
	private Map<String, Object> properties = new HashMap<String, Object>();

	/**
	 * Constructor.
	 */
	public PropertiesContainer() {
		super();

		Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$

		// Initialize the unique node id.
		UUID uuid = UUID.randomUUID();
		while (LAST_UUID_GENERATED != null && LAST_UUID_GENERATED.equals(uuid)) {
			uuid = UUID.randomUUID();
		}
		LAST_UUID_GENERATED = uuid;
		uniqueId = LAST_UUID_GENERATED;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#getUUID()
	 */
	@Override
	public final UUID getUUID() {
		return uniqueId;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		boolean equals = super.equals(obj);
		if (!equals && obj instanceof PropertiesContainer) {
			return uniqueId.equals(((PropertiesContainer)obj).uniqueId);
		}
		return equals;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return uniqueId.hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder buffer = new StringBuilder();

		buffer.append("UUID=" + uniqueId.toString()); //$NON-NLS-1$

		// print the first level of the properties map only
		buffer.append(", properties={"); //$NON-NLS-1$
		for (String key : properties.keySet()) {
			buffer.append(key);
			buffer.append("="); //$NON-NLS-1$

			Object value = properties.get(key);
			if (value instanceof Map || value instanceof IPropertiesContainer) {
				buffer.append("{...}"); //$NON-NLS-1$
			} else {
				buffer.append(value);
			}

			buffer.append(", "); //$NON-NLS-1$
		}
		if (buffer.toString().endsWith(", ")) { //$NON-NLS-1$
			buffer.deleteCharAt(buffer.length() - 1);
			buffer.deleteCharAt(buffer.length() - 1);
		}
		buffer.append("}"); //$NON-NLS-1$

		return buffer.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.nodes.IPropertiesContainer#setChangeEventsEnabled(boolean)
	 */
	@Override
	public final boolean setChangeEventsEnabled(boolean enabled) {
		boolean changed = changeEventsEnabled != enabled;
		if (changed) changeEventsEnabled = enabled;
		return changed;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.nodes.IPropertiesContainer#changeEventsEnabled()
	 */
	@Override
	public final boolean changeEventsEnabled() {
		return changeEventsEnabled;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer#fireChangeEvent(java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void fireChangeEvent(String key, Object oldValue, Object newValue) {
		Assert.isNotNull(key);
		EventObject event = newEvent(this, key, oldValue, newValue);
		if (event != null) EventManager.getInstance().fireEvent(event);
	}

	/**
	 * Creates a new property change notification event object. The event object is initialized
	 * with the given parameter.
	 * <p>
	 * This method is typically called from {@link #fireChangeEvent(String, Object, Object)}.
	 * <code>Null</code> is returned if no event should be fired.
	 *
	 * @param source The source object. Must not be <code>null</code>.
	 * @param key The property key. Must not be <code>null</code>.
	 * @param oldValue The old properties value.
	 * @param newValue The new properties value.
	 *
	 * @return The new property change notification event instance or <code>null</code>.
	 */
	private final EventObject newEvent(Object source, String key, Object oldValue, Object newValue) {
		Assert.isNotNull(source);
		Assert.isNotNull(key);

		// Check if the event is dropped
		if (dropEvent(source, key, oldValue, newValue)) {
			// Log the event dropping if tracing is enabled
			if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_EVENTS)) {
				CoreBundleActivator.getTraceHandler().trace("Drop event notification (not created change event)\n\t\t" + //$NON-NLS-1$
								"for eventId  = " + key + ",\n\t\t" + //$NON-NLS-1$ //$NON-NLS-2$
								"currentValue = " + oldValue + ",\n\t\t" + //$NON-NLS-1$ //$NON-NLS-2$
								"newValue     = " + newValue, //$NON-NLS-1$
								0, ITraceIds.TRACE_EVENTS, IStatus.WARNING, this);
			}
			return null;
		}

		// Create the notification event instance.
		return newEventDelegate(source, key, oldValue, newValue);
	}

	/**
	 * Creates a new property change notification event object instance.
	 * <p>
	 * This method is typically called from {@link #newEvent(Object, String, Object, Object)}
	 * if notifications are enabled.
	 *
	 * @param source The source object. Must not be <code>null</code>.
	 * @param key The property key. Must not be <code>null</code>.
	 * @param oldValue The old properties value.
	 * @param newValue The new properties value.
	 *
	 * @return The new property change notification event instance or <code>null</code>.
	 */
	protected EventObject newEventDelegate(Object source, String key, Object oldValue, Object newValue) {
		Assert.isNotNull(source);
		Assert.isNotNull(key);
		return new ChangeEvent(source, key, oldValue, newValue);
	}

	/**
	 * Returns if or if not notifying the given property change has to be dropped.
	 *
	 * @param source The source object. Must not be <code>null</code>.
	 * @param key The property key. Must not be <code>null</code>.
	 * @param oldValue The old properties value.
	 * @param newValue The new properties value.
	 *
	 * @return <code>True</code> if dropping the property change notification, <code>false</code> if notifying the property change.
	 */
	protected boolean dropEvent(Object source, String key, Object oldValue, Object newValue) {
		Assert.isNotNull(source);
		Assert.isNotNull(key);
		return !changeEventsEnabled || key.endsWith(SILENT_PROPERTY);
	}

	/**
	 * Checks if the access to the properties container happens in
	 * a privileged thread.
	 * <p>
	 * The default implementation returns always <code>true</code>. Overwrite
	 * to implement thread-safe properties container access.
	 *
	 * @return <code>True</code> if the access to the properties container is allowed, <code>false</code> otherwise.
	 */
	protected boolean checkThreadAccess() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#getProperties()
	 */
	@Override
	public Map<String, Object> getProperties() {
		Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
		return Collections.unmodifiableMap(new HashMap<String, Object>(properties));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#getProperty(java.lang.String)
	 */
	@Override
	public Object getProperty(String key) {
		Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
		return properties.get(key);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#getBooleanProperty(java.lang.String)
	 */
	@Override
	public final boolean getBooleanProperty(String key) {
		Object value = getProperty(key);
		if (value instanceof Boolean) {
			return ((Boolean)value).booleanValue();
		}
		if (value instanceof String) {
			String val = ((String)value).trim();
			return "TRUE".equalsIgnoreCase(val) || "1".equals(val) || "Y".equalsIgnoreCase(val) ||  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							"JA".equalsIgnoreCase(val) || "YES".equalsIgnoreCase(val); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#getLongProperty(java.lang.String)
	 */
	@Override
	public final long getLongProperty(String key) {
		Object value = getProperty(key);
		try {
			if (value instanceof Long) {
				return ((Long)value).longValue();
			}
			if (value instanceof Number) {
				return ((Number)value).longValue();
			}
			if (value != null) {
				return Long.decode(value.toString()).longValue();
			}
		} catch (Exception e) {
			/* ignored on purpose */
		}

		return -1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#getIntProperty(java.lang.String)
	 */
	@Override
	public final int getIntProperty(String key) {
		Object value = getProperty(key);
		try {
			if (value instanceof Integer) {
				return ((Integer)value).intValue();
			}
			if (value instanceof Number) {
				return ((Number)value).intValue();
			}
			if (value != null) {
				return Integer.decode(value.toString()).intValue();
			}
		} catch (Exception e) {
			/* ignored on purpose */
		}

		return -1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#getStringProperty(java.lang.String)
	 */
	@Override
	public final String getStringProperty(String key) {
		Object value = getProperty(key);
		return value instanceof String ? (String)value :
					(value != null ? value.toString() : null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#getFloatProperty(java.lang.String)
	 */
	@Override
	public final float getFloatProperty(String key) {
		Object value = getProperty(key);
		try {
			if (value instanceof Float) {
				return ((Float)value).floatValue();
			}
			if (value instanceof Number) {
				return ((Number)value).floatValue();
			}
			if (value != null) {
				return Float.parseFloat(value.toString());
			}
		} catch (Exception e) {
			/* ignored on purpose */
		}

		return Float.NaN;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#getDoubleProperty(java.lang.String)
	 */
	@Override
	public final double getDoubleProperty(String key) {
		Object value = getProperty(key);
		try {
			if (value instanceof Double) {
				return ((Double)value).doubleValue();
			}
			if (value instanceof Number) {
				return ((Number)value).doubleValue();
			}
			if (value != null) {
				return Double.parseDouble(value.toString());
			}
		} catch (Exception e) {
			/* ignored on purpose */
		}

		return Double.NaN;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#setProperties(java.util.Map)
	 */
	@Override
	public void setProperties(Map<String, Object> properties) {
		Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(properties);

		// Change the properties only if they have changed really
		if (this.properties.equals(properties)) {
			return;
		}

		// Clear out all old properties
		this.properties.clear();
		// Apply everything from the given properties
		this.properties.putAll(properties);
		// And signal the change
		postSetProperties(properties);
	}

	/**
	 * Method hook called by {@link #setProperties(Map)} just before the
	 * method returns to the caller.
	 *
	 * @param properties The map of properties set. Must not be <code>null</code>.
	 */
	protected void postSetProperties(Map<String, ?> properties) {
		Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(properties);

		fireChangeEvent("properties", null, properties); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer#addProperties(java.util.Map)
	 */
	@Override
	public final void addProperties(Map<String, ?> properties) {
		Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(properties);

		// Apply everything from the given properties
		this.properties.putAll(properties);
		// And signal the change
		postSetProperties(properties);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#setProperty(java.lang.String, boolean)
	 */
	@Override
	public final boolean setProperty(String key, boolean value) {
		boolean oldValue = getBooleanProperty(key);
		if (oldValue != value) {
			return setProperty(key, Boolean.valueOf(value));
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#setProperty(java.lang.String, long)
	 */
	@Override
	public final boolean setProperty(String key, long value) {
		long oldValue = getLongProperty(key);
		if (oldValue != value) {
			return setProperty(key, Long.valueOf(value));
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#setProperty(java.lang.String, int)
	 */
	@Override
	public final boolean setProperty(String key, int value) {
		int oldValue = getIntProperty(key);
		if (oldValue != value) {
			return setProperty(key, Integer.valueOf(value));
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#setProperty(java.lang.String, float)
	 */
	@Override
	public final boolean setProperty(String key, float value) {
		float oldValue = getFloatProperty(key);
		if (oldValue != value) {
			return setProperty(key, Float.valueOf(value));
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#setProperty(java.lang.String, double)
	 */
	@Override
	public final boolean setProperty(String key, double value) {
		double oldValue = getDoubleProperty(key);
		if (oldValue != value) {
			return setProperty(key, Double.valueOf(value));
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#setProperty(java.lang.String, java.lang.Object)
	 */
	@Override
	public boolean setProperty(String key, Object value) {
		Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(key);

		Object oldValue = properties.get(key);
		if ((oldValue == null && value != null) || (oldValue != null && !oldValue.equals(value))) {
			if (value != null) {
				properties.put(key, value);
			} else {
				properties.remove(key);
			}
			postSetProperty(key, value, oldValue);
			return true;
		}
		return false;
	}

	/**
	 * Method hook called by {@link #setProperty(String, Object)} in case the value
	 * of the given property changed and just before the method returns to the caller.
	 *
	 * @param key The property key. Must not be <code>null</code>.
	 * @param value The property value.
	 * @param oldValue The old property value.
	 */
	public void postSetProperty(String key, Object value, Object oldValue) {
		Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(key);

		fireChangeEvent(key, oldValue, value);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#clearProperties()
	 */
	@Override
	public final void clearProperties() {
		Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
		properties.clear();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
		return properties.isEmpty();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer#containsKey(java.lang.String)
	 */
	@Override
	public boolean containsKey(String key) {
		Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(key);
	    return properties.containsKey(key);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#isProperty(java.lang.String, long)
	 */
	@Override
	public final boolean isProperty(String key, long value) {
		return getLongProperty(key) == value;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#isProperty(java.lang.String, boolean)
	 */
	@Override
	public final boolean isProperty(String key, boolean value) {
		return getBooleanProperty(key) == value;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#isProperty(java.lang.String, int)
	 */
	@Override
	public final boolean isProperty(String key, int value) {
		return getIntProperty(key) == value;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#isProperty(java.lang.String, float)
	 */
	@Override
	public final boolean isProperty(String key, float value) {
		return Math.abs(getFloatProperty(key) - value) < .0000001;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#isProperty(java.lang.String, double)
	 */
	@Override
	public final boolean isProperty(String key, double value) {
		return Math.abs(getDoubleProperty(key) - value) < .0000001;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#isPropertyIgnoreCase(java.lang.String, java.lang.String)
	 */
	@Override
	public final boolean isPropertyIgnoreCase(String key, String value) {
		String property = getStringProperty(key);
		return (property == null && value == null) || (property != null && property.equalsIgnoreCase(value));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IPropertiesContainer#isProperty(java.lang.String, java.lang.Object)
	 */
	@Override
	public final boolean isProperty(String key, Object value) {
		Object property = getProperty(key);
		return (property == null && value == null) || (property != null && property.equals(value));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.PlatformObject#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		Object adapted = super.getAdapter(adapter);
	    return adapted != null ? adapted : Platform.getAdapterManager().loadAdapter(this, adapter.getName());
	}
}
