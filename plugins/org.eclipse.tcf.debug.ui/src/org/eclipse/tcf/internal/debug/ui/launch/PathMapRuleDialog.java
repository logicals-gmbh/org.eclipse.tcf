/*******************************************************************************
 * Copyright (c) 2010-2020 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.launch;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tcf.internal.debug.ui.ImageCache;
import org.eclipse.tcf.internal.debug.ui.model.TCFModel;
import org.eclipse.tcf.services.IPathMap;

public class PathMapRuleDialog extends TitleAreaDialog {

    private final IPathMap.PathMapRule pathMapRule;
    private final boolean enable_editing;
    private final boolean showContextQuery;
    private final Image image;

    private Text source_text;
    private Text destination_text;
    private Text context_query_text;
    private Button destination_button_dir;
    private Button destination_button_file;

    public PathMapRuleDialog(Shell parent, Image image, IPathMap.PathMapRule pathMapRule, boolean enable_editing, boolean showContextQuery) {
        super(parent);
        this.image = image != null ? image : ImageCache.getImage(ImageCache.IMG_PATH);
        this.pathMapRule = pathMapRule;
        this.enable_editing = enable_editing;
        this.showContextQuery = showContextQuery;
        setHelpAvailable(false);
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("File Path Map Rule"); //$NON-NLS-1$
        shell.setImage(image);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        updateButtons();
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle("Add or edit source and destination path mapping rule"); //$NON-NLS-1$
        setMessage("Source and destination are absolute path fragments.\nThe rule is applied if the source path fragment matches or is a prefix of a path to map."); //$NON-NLS-1$

        Composite composite = (Composite)super.createDialogArea(parent);
        createFileNameFields(composite);
        setData();
        composite.setSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        //PlatformUI.getWorkbench().getHelpSystem().setHelp(getShell(), "org.eclipse.tcf.debug.ui.add_path_map_rule"); //$NON-NLS-1$
        return composite;
    }

    private void createFileNameFields(Composite parent) {
        Font font = parent.getFont();
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(4, false);
        composite.setFont(font);
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label source_label = new Label(composite, SWT.NONE);
        source_label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        source_label.setFont(font);
        source_label.setText("Source:"); //$NON-NLS-1$

        source_text = new Text(composite, SWT.SINGLE | SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = convertWidthInCharsToPixels(40);
        gd.horizontalSpan = 3;
        source_text.setLayoutData(gd);
        source_text.setFont(font);
        if (!enable_editing) {
            source_text.setEditable(false);
            source_text.setBackground(parent.getBackground());
        }

        source_text.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateButtons();
            }
        });

        Label destination_label = new Label(composite, SWT.NONE);
        destination_label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        destination_label.setFont(font);
        destination_label.setText("Destination:"); //$NON-NLS-1$

        destination_text = new Text(composite, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = convertWidthInCharsToPixels(40);
        destination_text.setLayoutData(gd);
        destination_text.setFont(font);
        if (!enable_editing) {
            destination_text.setEditable(false);
            destination_text.setBackground(parent.getBackground());
        }

        destination_text.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateButtons();
            }
        });

        destination_button_dir = new Button(composite, SWT.PUSH);
        destination_button_dir.setImage(ImageCache.getImage(ImageCache.IMG_FOLDER));
        destination_button_dir.setFont(font);
        destination_button_dir.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        destination_button_dir.setEnabled(enable_editing);
        destination_button_dir.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.NONE);
                dialog.setFilterPath(destination_text.getText());
                String path = dialog.open();
                if (path != null) destination_text.setText(path);
            }
        });

        destination_button_file = new Button(composite, SWT.PUSH);
        destination_button_file.setImage(ImageCache.getImage(ImageCache.IMG_FILE));
        destination_button_file.setFont(font);
        destination_button_file.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        destination_button_file.setEnabled(enable_editing);
        destination_button_file.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(getShell(), SWT.NONE);
                dialog.setFilterPath(destination_text.getText());
                String path = dialog.open();
                if (path != null) destination_text.setText(path);
            }
        });

        if (showContextQuery) {
            Label context_query_label = new Label(composite, SWT.NONE);
            context_query_label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            context_query_label.setFont(font);
            context_query_label.setText("Context Query:"); //$NON-NLS-1$

            context_query_text = new Text(composite, SWT.SINGLE | SWT.BORDER);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.widthHint = 200;
            gd.horizontalSpan = 2;
            context_query_text.setLayoutData(gd);
            context_query_text.setFont(font);
            if (!enable_editing) {
                context_query_text.setEditable(false);
                context_query_text.setBackground(parent.getBackground());
            }

            context_query_text.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    updateButtons();
                }
            });
        }
    }

    private void setData() {
        source_text.setText(pathMapRule.getSource() != null ? pathMapRule.getSource() : ""); //$NON-NLS-1$
        destination_text.setText(pathMapRule.getDestination() != null ? pathMapRule.getDestination() : ""); //$NON-NLS-1$
        if (context_query_text != null)
            context_query_text.setText(pathMapRule.getContextQuery() != null ? pathMapRule.getContextQuery() : ""); //$NON-NLS-1$
        updateButtons();
    }

    private void getData() {
        if (source_text.getText().trim().length() > 0)
            pathMapRule.getProperties().put(IPathMap.PROP_SOURCE, source_text.getText());
        else
            pathMapRule.getProperties().remove(IPathMap.PROP_SOURCE);

        if (destination_text.getText().trim().length() > 0)
            pathMapRule.getProperties().put(IPathMap.PROP_DESTINATION, destination_text.getText());
        else
            pathMapRule.getProperties().remove(IPathMap.PROP_DESTINATION);

        if (context_query_text != null && context_query_text.getText().trim().length() > 0)
            pathMapRule.getProperties().put(IPathMap.PROP_CONTEXT_QUERY, context_query_text.getText());
        else
            pathMapRule.getProperties().remove(IPathMap.PROP_CONTEXT_QUERY);
    }

    private void updateButtons() {
        Button btn = getButton(IDialogConstants.OK_ID);
        if (btn != null && source_text != null) btn.setEnabled(!enable_editing || source_text.getText().trim().length() > 0);
    }

    @Override
    protected void okPressed() {
        if (enable_editing) {
            try {
                getData();
            }
            catch (Throwable x) {
                MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
                mb.setText("Invalid data"); //$NON-NLS-1$
                mb.setMessage(TCFModel.getErrorMessage(x, true));
                mb.open();
                return;
            }
        }
        super.okPressed();
    }
}
