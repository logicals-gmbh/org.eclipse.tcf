/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * William Chen (Wind River) - [345384] Provide property pages for remote file system nodes
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.nls;

import java.lang.reflect.Field;

import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.te.runtime.services.ServiceUtils;
import org.eclipse.tcf.te.tcf.filesystem.ui.interfaces.IFileSystemUIDelegate;

/**
 * File System plug-in externalized strings management.
 */
public class Messages extends NLS {

	// The plug-in resource bundle name
	private static final String BUNDLE_NAME = "org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages"; //$NON-NLS-1$

	/**
	 * Static constructor.
	 */
	static {
		// Load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	/**
	 * Returns if or if not this NLS manager contains a constant for
	 * the given externalized strings key.
	 *
	 * @param key The externalized strings key or <code>null</code>.
	 * @return <code>True</code> if a constant for the given key exists, <code>false</code> otherwise.
	 */
	public static boolean hasString(String key) {
		if (key != null) {
			try {
				Field field = Messages.class.getDeclaredField(key);
				return field != null;
			} catch (NoSuchFieldException e) { /* ignored on purpose */ }
		}

		return false;
	}

	/**
	 * Returns the corresponding string for the given externalized strings
	 * key or <code>null</code> if the key does not exist.
	 *
	 * @param key The externalized strings key or <code>null</code>.
	 * @return The corresponding string or <code>null</code>.
	 */
	public static String getString(String key) {
		if (key != null) {
			try {
				Field field = Messages.class.getDeclaredField(key);
				return (String)field.get(null);
			} catch (Exception e) { /* ignored on purpose */ }
		}

		return null;
	}

	/**
	 * Returns the corresponding string for the given externalized strings key via
	 * the {@link IFileSystemUIDelegate}.
	 *
	 * @param context The context or <code>null</code>.
	 * @param key The externalized strings key or <code>null</code>.
	 *
	 * @return The corresponding string or <code>null</code>.
	 */
	public static String getStringDelegated(Object context, String key) {
		if (key != null) {
			IFileSystemUIDelegate delegate = ServiceUtils.getUIServiceDelegate(context, context, IFileSystemUIDelegate.class);
			return delegate != null ? delegate.getMessage(key) : null;
		}

		return null;
	}

	public static String FSFolderSelectionDialog_MoveDialogMessage;
	public static String FSFolderSelectionDialog_MoveDialogTitle;
	public static String FSFolderSelectionDialog_Refresh_menu;
	public static String FSFolderSelectionDialog_RefreshAll_menu;
	public static String FSFolderSelectionDialog_validate_message;
	public static String FSFolderSelectionDialog_notWritable_error;
	public static String FSFolderSelectionDialog_notWritable_warning;

	public static String FSOpenFileDialog_message;
	public static String FSOpenFileDialog_title;
	public static String FSDelete_ConfirmDelete;
	public static String FSDelete_ConfirmMessage;
	public static String FSDelete_ButtonCancel;
	public static String FSDelete_ButtonNo;
	public static String FSDelete_ButtonYes;
	public static String FSDelete_ButtonYes2All;
	public static String DateValidator_DateInvalidNumber;
	public static String DateValidator_DateOutofRange;
	public static String DateValidator_InfoFormat;
	public static String DateValidator_InfoPrompt;
	public static String DateValidator_InvalidDate;
	public static String DateValidator_MonthInvalidNumber;
	public static String DateValidator_MonthOutofRange;
	public static String DateValidator_YearInvalidNumber;
	public static String DateValidator_YearOutofRange;
	public static String DeleteFilesHandler_DeleteMultipleFilesConfirmation;
	public static String DeleteFilesHandler_DeleteOneFileConfirmation;
	public static String DeleteFilesHandler_ConfirmDialogTitle;
	public static String DownloadFilesHandler_folderDlg_message;
	public static String DownloadFilesHandler_folderDlg_text;
	public static String FSRenamingAssistant_NameAlreadyExists;
	public static String FSRenamingAssistant_SpecifyNonEmptyName;
	public static String FSRenamingAssistant_UnixIllegalCharacters;
	public static String FSRenamingAssistant_WinIllegalCharacters;
	public static String LocalTypedElement_SavingFile;
	public static String MergeEditorInput_LocalFile;
	public static String MergeEditorInput_RemoteFile;
	public static String MergeEditorInput_CompareLeftAndRight;
	public static String MergeEditorInput_CompareWithLocalCache;
	public static String MergeInput_CopyNotSupported;
	public static String RemoteTypedElement_GettingRemoteContent;
	public static String RemoteTypedElement_DowloadingFile;
	public static String FSDropTargetListener_ConfirmMoveTitle;
	public static String FSDropTargetListener_MovingWarningMultiple;
	public static String FSDropTargetListener_MovingWarningSingle;
	public static String FSExplorerEditorPage_PageTitle;
	public static String FSGeneralSearchable_FileType;
	public static String FSGeneralSearchable_Find;
	public static String FSGeneralSearchable_GeneralOptionText;
	public static String FSGeneralSearchable_SearchHiddenFiles;
	public static String FSGeneralSearchable_SearchSystemFiles;
	public static String FSModifiedSearchable_DontRemember;
	public static String FSModifiedSearchable_LastWeek;
	public static String FSModifiedSearchable_PastMonth;
	public static String FSModifiedSearchable_PastYear;
	public static String FSModifiedSearchable_SpecifyDates;
	public static String FSModifiedSearchable_ToDate;
	public static String FSModifiedSearchable_WhenModified;
	public static String FSUpload_Cancel;
	public static String FSUpload_No;
	public static String FSUpload_OverwriteConfirmation;
	public static String FSUpload_OverwriteTitle;
	public static String FSUpload_Yes;
	public static String FSUpload_YesToAll;
	public static String FSOperation_ConfirmDialogCancel;
	public static String FSOperation_ConfirmDialogNo;
	public static String FSOperation_ConfirmDialogYes;
	public static String FSOperation_ConfirmDialogYesToAll;
	public static String FSOperation_ConfirmFileReplace;
	public static String FSOperation_ConfirmFileReplaceMessage;
	public static String FSOperation_ConfirmFolderReplaceMessage;
	public static String FSOperation_ConfirmFolderReplaceTitle;
	public static String OpenFileHandler_OpeningBinaryNotSupported;
	public static String OpenFileHandler_Warning;
	public static String OpenWithMenu_ChooseEditorForOpening;
	public static String OpenWithMenu_DefaultEditor;
	public static String OpenWithMenu_NoEditorFound;
	public static String OpenWithMenu_OpenWith;
	public static String FSRename_RenameFileFolderTitle;
	public static String FSSizeSearchable_DontRemember;
	public static String FSSizeSearchable_KBS;
	public static String FSSizeSearchable_Large;
	public static String FSSizeSearchable_Medium;
	public static String FSSizeSearchable_Small;
	public static String FSSizeSearchable_SpecifySize;
	public static String FSSizeSearchable_ToText;
	public static String FSSizeSearchable_WhatSize;
	public static String FSTreeNodeSearchable_FilesAndFolders;
	public static String FSTreeNodeSearchable_FilesOnly;
	public static String FSTreeNodeSearchable_FindFilesAndFolders;
	public static String FSTreeNodeSearchable_FindMessage;
	public static String FSTreeNodeSearchable_FoldersOnly;
	public static String FSTreeNodeSearchable_SearchingTargets;
	public static String FSTreeNodeSearchable_SelectedFileSystem;
	public static String RenameFilesHandler_TitleRename;
	public static String RenameFilesHandler_TitleRenameFile;
	public static String RenameFilesHandler_TitleRenameFolder;
	public static String RenameFilesHandler_PromptNewName;
	public static String RenameFilesHandler_RenamePromptMessage;
	public static String PreferencePage_AutoSavingText;
	public static String PreferencePage_CopyOwnershipText;
	public static String PreferencePage_CopyPermissionText;
	public static String PreferencePage_PersistExpanded;
	public static String PreferencePage_RenamingOptionText;
	public static String AdvancedAttributesDialog_FileBanner;
	public static String AdvancedAttributesDialog_FolderBanner;
	public static String AdvancedAttributesDialog_CompressEncrypt;
	public static String AdvancedAttributesDialog_ArchiveIndex;
	public static String AdvancedAttributesDialog_IndexFile;
	public static String AdvancedAttributesDialog_IndexFolder;
	public static String AdvancedAttributesDialog_FileArchive;
	public static String AdvancedAttributesDialog_FolderArchive;
	public static String AdvancedAttributesDialog_Encrypt;
	public static String AdvancedAttributesDialog_Compress;
	public static String AdvancedAttributesDialog_ShellTitle;
	public static String GeneralInformationPage_Accessed;
	public static String GeneralInformationPage_Advanced;
	public static String GeneralInformationPage_Attributes;
	public static String GeneralInformationPage_Computer;
	public static String GeneralInformationPage_FileSizeInfo;
	public static String GeneralInformationPage_Hidden;
	public static String GeneralInformationPage_Location;
	public static String GeneralInformationPage_Modified;
	public static String GeneralInformationPage_Name;
	public static String GeneralInformationPage_ReadOnly;
	public static String GeneralInformationPage_Size;
	public static String GeneralInformationPage_Type;
	public static String GeneralInformationPage_PermissionText;
	public static String PermissionsGroup_Executable;
	public static String PermissionsGroup_GroupPermissions;
	public static String PermissionsGroup_OtherPermissions;
	public static String PermissionsGroup_Readable;
	public static String PermissionsGroup_UserPermissions;
	public static String PermissionsGroup_Writable;
	public static String BasicFolderSection_BasicInfoText;
	public static String LinuxPermissionsSection_Permissions;
	public static String WindowsAttributesSection_Attributes;
	public static String FolderValidator_DirNotExist;
	public static String FolderValidator_NotWritable;
	public static String FolderValidator_SpecifyFolder;
	public static String NameValidator_InfoPrompt;
	public static String NameValidator_SpecifyFolder;
	public static String NewFileWizard_NewFileWizardTitle;
	public static String NewFileWizardPage_NewFileWizardPageDescription;
	public static String NewFileWizardPage_NewFileWizardPageNameLabel;
	public static String NewFileWizardPage_NewFileWizardPageTitle;
	public static String NewFolderWizard_NewFolderWizardTitle;
	public static String NewFolderWizardPage_NewFolderWizardPageDescription;
	public static String NewFolderWizardPage_NewFolderWizardPageNameLabel;
	public static String NewFolderWizardPage_NewFolderWizardPageTitle;
	public static String NewNodeWizardPage_PromptFolderLabel;
	public static String SaveAllListener_message_uploadFile;
	public static String SaveAllListener_message_uploadFiles;
	public static String SizeValidator_ErrorIncorrectFormat;
	public static String SizeValidator_ErrorSizeOutofRange;
	public static String SizeValidator_InfoPrompt;
	public static String TargetSelectionPage_Description;
	public static String TargetSelectionPage_Targets;
	public static String TargetSelectionPage_Title;
	public static String ToggleRevealOnConnectContributionItem_text;
	public static String TreeViewerSearchDialog_LblCancelText;
	public static String TreeViewerSearchDialog_GrpOptionsText;
	public static String TreeViewerSearchDialog_BtnCaseText;
	public static String TreeViewerSearchDialog_BtnPreciseText;

	public static String ContentProvider_notConnected;
	public static String UiExecutor_errorRunningOperation;

	public static String FsClipboardTransfer_errorMessage;
}
