/*******************************************************************************
 * Copyright (c) 2009, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.launch.setup;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

class WizardLocalPage extends WizardPage implements Listener {

    WizardLocalPage(SetupWizardDialog wizard) {
        super("LocalPage");
        setTitle("Local TCF agent configuration");
    }

    public void handleEvent(Event event) {
        getContainer().updateButtons();
    }

    public void createControl(Composite parent) {
        Composite composite =  new Composite(parent, SWT.NULL);
        GridLayout gl = new GridLayout();
        gl.numColumns = 1;
        composite.setLayout(gl);

        new Label(composite, SWT.WRAP).setText("Under construction...");

        setControl(composite);
    }

    @Override
    public IWizardPage getNextPage() {
        return null;
    }
}
