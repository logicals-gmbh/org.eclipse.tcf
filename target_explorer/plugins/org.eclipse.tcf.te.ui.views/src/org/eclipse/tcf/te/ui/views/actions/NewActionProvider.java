/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * Tobias Schwarz (Wind River) - [368243] [UI] Allow dynamic new wizard contributions
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.tcf.te.ui.help.IContextHelpIds;
import org.eclipse.tcf.te.ui.interfaces.ImageConsts;
import org.eclipse.tcf.te.ui.views.interfaces.IUIConstants;
import org.eclipse.tcf.te.ui.views.nls.Messages;
import org.eclipse.tcf.te.ui.wizards.newWizard.NewWizardRegistry;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.actions.CommandAction;
import org.eclipse.ui.internal.navigator.wizards.WizardShortcutAction;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;
import org.eclipse.ui.navigator.WizardActionGroup;
import org.eclipse.ui.wizards.IWizardDescriptor;

/**
 * Action provider implementation providing the "New >" content menu
 * content.
 */
@SuppressWarnings("restriction")
public class NewActionProvider extends CommonActionProvider {
	// Reference to the action showing the "Other..." dialog (context menu)
	private CommandAction newWizardCommandAction = null;
	// Reference to the action showing the "Other..." dialog (toolbar)
	private CommandAction newWizardCommandActionToolbar = null;
	// Reference to the action group managing the context sensitive new wizards
	private WizardActionGroup newWizardActionGroup = null;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.CommonActionProvider#init(org.eclipse.ui.navigator.ICommonActionExtensionSite)
	 */
	@Override
	public void init(ICommonActionExtensionSite site) {
		super.init(site);

		if (site.getViewSite() instanceof ICommonViewerWorkbenchSite) {
			// To initialize the actions, the workbench window instance is required
			IWorkbenchWindow window = ((ICommonViewerWorkbenchSite)site.getViewSite()).getWorkbenchWindow();
			// Initialize the actions
			newWizardCommandAction = new CommandAction(window, "org.eclipse.tcf.te.ui.command.newWizards"); //$NON-NLS-1$
			newWizardCommandAction.setImageDescriptor(null);
			newWizardCommandAction.setDisabledImageDescriptor(null);
			newWizardCommandAction.setText(Messages.NewActionProvider_NewWizardCommandAction_label);
			newWizardCommandAction.setToolTipText(Messages.NewActionProvider_NewWizardCommandAction_tooltip);
            window.getWorkbench().getHelpSystem().setHelp(newWizardCommandAction, IContextHelpIds.NEW_TARGET_WIZARD);

			newWizardCommandActionToolbar = new CommandAction(window, "org.eclipse.tcf.te.ui.command.newWizards"); //$NON-NLS-1$
			newWizardCommandActionToolbar.setImageDescriptor(org.eclipse.tcf.te.ui.activator.UIPlugin.getImageDescriptor(ImageConsts.NEW_TARGET_WIZARD_ENABLED));
			newWizardCommandActionToolbar.setDisabledImageDescriptor(org.eclipse.tcf.te.ui.activator.UIPlugin.getImageDescriptor(ImageConsts.NEW_TARGET_WIZARD_DISABLED));
			newWizardCommandActionToolbar.setText(Messages.NewActionProvider_NewWizardCommandAction_label);
			newWizardCommandActionToolbar.setToolTipText(Messages.NewActionProvider_NewWizardCommandAction_tooltip);
            window.getWorkbench().getHelpSystem().setHelp(newWizardCommandActionToolbar, IContextHelpIds.NEW_TARGET_WIZARD);

			newWizardActionGroup = new WizardActionGroup(window,
														  NewWizardRegistry.getInstance(),
														  WizardActionGroup.TYPE_NEW,
														  site.getContentService());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#dispose()
	 */
	@Override
	public void dispose() {
		if (newWizardCommandAction != null) {
			newWizardCommandAction.dispose();
			newWizardCommandAction = null;
		}
		if (newWizardActionGroup != null) {
			newWizardActionGroup.dispose();
			newWizardActionGroup = null;
		}
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
	public void fillContextMenu(IMenuManager menu) {
		// If none of the actions got created, there is nothing to do here
		if (newWizardCommandAction == null && newWizardActionGroup == null) {
			return;
		}

		// Create the new sub menu
		IMenuManager newMenu = new MenuManager(Messages.NewActionProvider_NewMenu_label,
											   org.eclipse.tcf.te.ui.activator.UIPlugin.getImageDescriptor(ImageConsts.NEW_TARGET_WIZARD_ENABLED),
											   IUIConstants.ID_EXPLORER + ".menu.new"); //$NON-NLS-1$

		// Add the context sensitive wizards (commonWizard element)
		if (newWizardActionGroup != null) {
			newWizardActionGroup.setContext(getContext());
			newWizardActionGroup.fillContextMenu(newMenu);
		}


		IWorkbenchWindow window = ((ICommonViewerWorkbenchSite)getActionSite().getViewSite()).getWorkbenchWindow();
		for (IWizardDescriptor wizard : NewWizardRegistry.getInstance().getCommonWizards(getContext().getSelection())) {
			IAction action = new WizardShortcutAction(window, wizard);
			newMenu.add(action);
		}

		// Add the standard additions marker
		newMenu.add(new Separator(ICommonMenuConstants.GROUP_ADDITIONS));

		// Add the "Other..." dialog action
		if (newWizardCommandAction != null) {
			newMenu.add(new Separator());
			newMenu.add(newWizardCommandAction);
		}

		// The menu will be appended after the GROUP_NEW group.
		menu.insertAfter(ICommonMenuConstants.GROUP_NEW, newMenu);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.ActionGroup#fillActionBars(org.eclipse.ui.IActionBars)
	 */
	@Override
	public void fillActionBars(IActionBars actionBars) {
		// If none of the actions got created, there is nothing to do here
		if (newWizardCommandActionToolbar == null) {
			return;
		}

		// Get the toolbar manager
		IToolBarManager toolbar = actionBars.getToolBarManager();

		// Check for the newWizard action in the toolbar. If found,
		// drop out immediately to avoid adding the items to the toolbar
		// again and again
		if (toolbar.find("org.eclipse.tcf.te.ui.command.newWizards") != null) { //$NON-NLS-1$
			return;
		}

		// Add the items to the toolbar
		toolbar.insertAfter(ICommonMenuConstants.GROUP_NEW, newWizardCommandActionToolbar);
	}
}
