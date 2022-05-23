/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.launch.ui.internal.properties;

import org.eclipse.core.runtime.Assert;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tcf.te.launch.core.persistence.launchcontext.LaunchContextsPersistenceDelegate;
import org.eclipse.tcf.te.launch.ui.model.LaunchNode;
import org.eclipse.tcf.te.launch.ui.nls.Messages;
import org.eclipse.tcf.te.launch.ui.properties.BaseTitledSection;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

/**
 * The property section to display the properties of a launch configuration.
 */
public class LaunchContextPropertiesSection extends BaseTitledSection {
	private String launchContextValue = ""; //$NON-NLS-1$

	private Text launchContext;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#createControls(org.eclipse.swt.widgets.Composite, org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage)
	 */
	@Override
	public void createControls(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
		super.createControls(parent, aTabbedPropertySheetPage);

		launchContext = createTextField(null, Messages.ContextSelectorSection_label);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#setInput(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void setInput(IWorkbenchPart part, ISelection selection) {
		super.setInput(part, selection);
		Assert.isTrue(selection instanceof IStructuredSelection);
		Object input = ((IStructuredSelection) selection).getFirstElement();
		Assert.isTrue(input instanceof LaunchNode);
		ILaunchConfiguration node = ((LaunchNode)input).getLaunchConfiguration();

		IModelNode[] contexts = LaunchContextsPersistenceDelegate.getLaunchContexts(node);
		if (contexts != null && contexts.length > 0) {
			ILabelProvider labelProvider = (ILabelProvider)contexts[0].getAdapter(ILabelProvider.class);
			launchContextValue = labelProvider.getText(contexts[0]);
			if (labelProvider instanceof ILabelDecorator) {
				launchContextValue = ((ILabelDecorator)labelProvider).decorateText(launchContextValue, contexts[0]);
			}
			if (contexts.length > 1) {
				launchContextValue += ", ..."; //$NON-NLS-1$
			}
		}
		else {
			launchContextValue = ""; //$NON-NLS-1$
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#refresh()
	 */
	@Override
	public void refresh() {
		if (launchContext != null) launchContext.setText(launchContextValue);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#getText()
	 */
	@Override
	protected String getText() {
		return Messages.ContextSelectorSection_title;
	}
}
