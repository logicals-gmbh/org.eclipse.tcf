/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.search;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.ui.forms.FormLayoutFactory;
import org.eclipse.tcf.te.ui.interfaces.ISearchMatcher;
import org.eclipse.tcf.te.ui.utils.AbstractSearchable;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

/**
 * The base searchable that provides common methods for its subclasses.
 * 
 * @see FSModifiedSearchable
 * @see FSSizeSearchable
 */
public abstract class FSBaseSearchable extends AbstractSearchable implements ISearchMatcher {

	/**
	 * Create a collapseable section with the specified title and return the
	 * content composite.
	 * 
	 * @param parent The parent where the section is to be created.
	 * @param title The title of the section.
	 * @return The content composite.
	 */
	protected Composite createSection(Composite parent, String title) {
		Section section = new Section(parent, ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);
		section.setText(title);
		section.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		GridData layoutData = new GridData(GridData.FILL_HORIZONTAL);
		section.setLayoutData(layoutData);
		
		final Composite client = new Composite(section, SWT.NONE);
		client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		client.setBackground(section.getBackground());
		section.setClient(client);
		
		section.addExpansionListener(new IExpansionListener(){
			@Override
            public void expansionStateChanging(ExpansionEvent e) {
            }
			@Override
            public void expansionStateChanged(ExpansionEvent e) {
				Shell shell = client.getShell();
				boolean state = e.getState();
				int client_height = client.getSize().y;
				Point p = shell.getSize();
				p.y = state ? p.y + client_height : p.y - client_height;
				shell.setSize(p.x, p.y);
            }});
		return client;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchable#getMatcher()
	 */
	@Override
	public ISearchMatcher getMatcher() {
		return this;
	}
}
