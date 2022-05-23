/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.launch.core.delegates;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.te.launch.core.activator.CoreBundleActivator;
import org.eclipse.tcf.te.launch.core.bindings.LaunchConfigTypeBindingsManager;
import org.eclipse.tcf.te.launch.core.interfaces.IReferencedProjectItem;
import org.eclipse.tcf.te.launch.core.lm.interfaces.ICommonLaunchAttributes;
import org.eclipse.tcf.te.launch.core.lm.interfaces.ILaunchManagerDelegate;
import org.eclipse.tcf.te.launch.core.nls.Messages;
import org.eclipse.tcf.te.launch.core.persistence.projects.ReferencedProjectsPersistenceDelegate;
import org.eclipse.tcf.te.runtime.interfaces.ISharedConstants;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepAttributes;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepper;
import org.eclipse.tcf.te.runtime.stepper.interfaces.tracing.ITraceIds;
import org.eclipse.tcf.te.runtime.stepper.stepper.Stepper;

/**
 * Default launch configuration delegate implementation.
 * <p>
 * The launch configuration delegate implements the bridge between the native Eclipse launch
 * configuration framework and the stepper engine. The delegate is standard for all
 * launch configurations which supports extensible and modularized launching.
 * <p>
 * <b>Implementation Details</b>:<br>
 * <ul>
 * <li>The launch configuration delegate signals the completion of the launch sequence via
 * the custom {@link ILaunch} attribute {@link ICommonLaunchAttributes#ILAUNCH_ATTRIBUTE_LAUNCH_SEQUENCE_COMPLETED}.</li>
 * <li>The launch configuration delegates enforces the removal of the launch from the Eclipse
 * debug platforms launch manager if the progress monitor is set to canceled or an status with
 * severity {@link IStatus#CANCEL} had been thrown by the stepper implementation.</li>
 * <li>The launch configuration delegate creates launches of type {@link Launch}.</li>
 * </ul>
 */
public class LaunchConfigurationDelegate extends org.eclipse.debug.core.model.LaunchConfigurationDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.LaunchConfigurationDelegate#buildProjects(org.eclipse.core.resources.IProject[], org.eclipse.core.runtime.IProgressMonitor)
	 * <p>
	 * This method is a copy of the super implementation, except it does not lock the workspace to perform the build.
	 * This is required to support "Edit while build is ongoing".
	 */
	@Override
	protected void buildProjects(final IProject[] projects, IProgressMonitor monitor) throws CoreException {
		IWorkspaceRunnable build = new IWorkspaceRunnable(){
			@Override
			public void run(IProgressMonitor pm) throws CoreException {
				SubMonitor localmonitor = SubMonitor.convert(pm, Messages.AbstractLaunchConfigurationDelegate_scoped_incremental_build, projects.length);
				try {
					for (IProject project : projects) {
						if (localmonitor.isCanceled()) {
							throw new OperationCanceledException();
						}
						project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, localmonitor.newChild(1));
					}
				} finally {
					localmonitor.done();
				}
			}
		};
		ResourcesPlugin.getWorkspace().run(build, null, IWorkspace.AVOID_UPDATE, monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(configuration);
		Assert.isNotNull(mode);
		Assert.isNotNull(launch);

		// Note: This method is typically called within a worker thread from the launch configuration framework.

		long startTime = System.currentTimeMillis();
		ILaunchConfigurationWorkingCopy launchConfig = configuration.getWorkingCopy();
		launchConfig.setAttribute(ICommonLaunchAttributes.ATTR_LAST_LAUNCHED, Long.toString(startTime));
		launchConfig.doSave();

		CoreBundleActivator.getTraceHandler().trace("LaunchConfigurationDelegate#launch: *** ENTERED" //$NON-NLS-1$
						+ " (" + configuration.getName() + ")", //$NON-NLS-1$ //$NON-NLS-2$
						0, ITraceIds.TRACE_STEPPING, IStatus.WARNING, this);
		CoreBundleActivator.getTraceHandler().trace(" [" + ISharedConstants.TIME_FORMAT.format(new Date(startTime)) + "]" //$NON-NLS-1$ //$NON-NLS-2$
						+ " ***", //$NON-NLS-1$
						0, ITraceIds.PROFILE_STEPPING, IStatus.WARNING, this);

		// Reset the attribute to tell if the launch sequence has been completed. Clients cannot
		// use ILaunch.isTerminated() as this is passing through to a possibly associated IProcess
		// object. We need to know when the launch itself has finished the job, not when the process
		// might have died.
		launch.setAttribute(ICommonLaunchAttributes.ILAUNCH_ATTRIBUTE_LAUNCH_SEQUENCE_COMPLETED, Boolean.FALSE.toString());

		// The stepper instance to be used
		IStepper stepper = new Stepper(launchConfig.getName());
		IStatus status = null;

		try {
			// Get the launch properties container
			IPropertiesContainer properties = (IPropertiesContainer)launch.getAdapter(IPropertiesContainer.class);
			Assert.isNotNull(properties);

			// Initialize the stepper
			String stepGroupId = LaunchConfigTypeBindingsManager.getInstance().getStepGroupId(
							launchConfig.getType().getIdentifier(),
							launch.getLaunchMode());

			IStepContext context = (IStepContext)launch.getAdapter(IStepContext.class);
			properties.setProperty(IStepAttributes.PROP_SKIP_LAST_RUN_HISTORY, true);
			stepper.initialize(context, stepGroupId, properties, monitor);

			// Execute
			stepper.execute();

		} catch (CoreException e) {
			// We have to catch the CoreException here as we do want to open the
			// launch configurations dialog on ERROR only.
			status = e.getStatus();

			ILaunchManagerDelegate delegate = LaunchConfigTypeBindingsManager.getInstance().getLaunchManagerDelegate(configuration.getType().getIdentifier(), mode);

			if (status == null || (status.getSeverity() == IStatus.ERROR && delegate.showLaunchDialog(ILaunchManagerDelegate.SITUATION_AFTER_LAUNCH_FAILED))) {
				// just pass on the exception as is
				throw e;
			}

			// Try to get a handler for the status
			IStatusHandler handler = DebugPlugin.getDefault().getStatusHandler(status);
			// If we cannot get a handler, pass on the exception
			if (handler == null) {
				throw e;
			}

			// Invoke the handler directly and drop the exception
			handler.handleStatus(status, this);

			// Mimic the original launch configuration behavior if an exception occurred
			// by removing the launch, if empty, and setting the progress monitor canceled.
			// @see LaunchConfiguration#launch(String, IProgressMonitor, boolean, boolean), line #768
			if (!launch.hasChildren()) {
				DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
				if (monitor != null) {
					monitor.setCanceled(true);
				}
			}
		} finally {
			// Mimic the original launch configuration behavior if monitor is set
			// canceled and make sure that the launch get's removed.
			if (monitor == null || monitor.isCanceled()) {
				DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
			}

			// Cleanup the stepper
			stepper.cleanup();

			// Set the launch completed here. Doesn't matter if the launch might completed with error or not.
			launch.setAttribute(ICommonLaunchAttributes.ILAUNCH_ATTRIBUTE_LAUNCH_SEQUENCE_COMPLETED, Boolean.TRUE.toString());
			onLaunchFinished(launch, status != null ? status : Status.OK_STATUS);

			long endTime = System.currentTimeMillis();
			CoreBundleActivator.getTraceHandler().trace("LaunchConfigurationDelegate#launch: *** DONE" //$NON-NLS-1$
							+ " (" + configuration.getName() + ")", //$NON-NLS-1$ //$NON-NLS-2$
							0, ITraceIds.TRACE_STEPPING, IStatus.WARNING, this);
			CoreBundleActivator.getTraceHandler().trace(" [" + ISharedConstants.TIME_FORMAT.format(new Date(endTime)) //$NON-NLS-1$
							+ " , delay = " + (endTime - startTime) + " ms]" //$NON-NLS-1$ //$NON-NLS-2$
							+ " ***", //$NON-NLS-1$
							0, ITraceIds.PROFILE_STEPPING, IStatus.WARNING, this);

		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.LaunchConfigurationDelegate#getLaunch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String)
	 */
	@Override
	public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
		return new Launch(configuration, mode, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.LaunchConfigurationDelegate#getProjectsForProblemSearch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String)
	 */
	@Override
	protected IProject[] getProjectsForProblemSearch(ILaunchConfiguration configuration, String mode) throws CoreException {
		// Return the same list of projects as we have to build
		return getBuildOrder(configuration, mode);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.LaunchConfigurationDelegate#getBuildOrder(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String)
	 */
	@Override
	protected IProject[] getBuildOrder(ILaunchConfiguration configuration, String mode) throws CoreException {
		// The result list. Return always an empty list at least. If returning null,
		// the super implementation will perform a incremental workspace build.
		List<IProject> projects = new ArrayList<IProject>();

		// Return the projects to build within the order the user configured within
		// the corresponding UI launch tab.
		IReferencedProjectItem[] items = ReferencedProjectsPersistenceDelegate.getReferencedProjects(configuration);
		for (IReferencedProjectItem item : items) {
			if (item.isProperty(IReferencedProjectItem.PROPERTY_ENABLED, true) && item.getStringProperty(IReferencedProjectItem.PROPERTY_PROJECT_NAME) != null) {
				IProject project = findProjectResource(item.getStringProperty(IReferencedProjectItem.PROPERTY_PROJECT_NAME));
				projects.add(project);
			}
		}

		// If the list of projects is not empty, we have to check for duplicates
		// and possible sub projects. As we cannot pre check if a project must
		// be build, we have to avoid building the same project again and again.
		checkForDuplicatesAndSubProjects(projects);

		return projects.toArray(new IProject[projects.size()]);
	}

	/**
	 * Lookup the corresponding project resource for the given project name.
	 * If the referenced project cannot be found or is closed, a {@link CoreException} will be thrown.
	 *
	 * @param projectName The project name. Must not be <code>null</code>.
	 * @return The project resource or <code>null</code>.
	 */
	protected IProject findProjectResource(String projectName) throws CoreException {
		Assert.isNotNull(projectName);

		// Project resources are stored with the workspace root
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(projectName);
		if (project != null && !project.isAccessible()) {
			// Not accessible -> means the project does either not exist or
			// is closed.
			throw new CoreException(new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(),
							NLS.bind(Messages.LaunchConfigurationDelegate_error_inaccessibleReferencedProject, projectName)
							));
		}

		return project;
	}

	/**
	 * Check for duplicates and sub projects in the given list of project
	 * resources.
	 *
	 * @param projects The list of project resources. Must not be <code>null</code>.
	 */
	protected void checkForDuplicatesAndSubProjects(List<IProject> projects) {
		Assert.isNotNull(projects);

		// The list of already processed project names
		List<String> processedProjectNames = new ArrayList<String>();

		// Loop the project list and determine the duplicates. Use an
		// iterator here as we manipulate the list directly.
		ListIterator<IProject> iterator = projects.listIterator();
		while (iterator.hasNext()) {
			IProject project = iterator.next();
			// If the project name is within the list of already processed
			// projects, the project will be deleted from the list.
			if (processedProjectNames.contains(project.getName())) {
				iterator.remove();
				continue;
			}

			// Add the project name to the list of processed project names
			processedProjectNames.add(project.getName());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.LaunchConfigurationDelegate#getBreakpoints(org.eclipse.debug.core.ILaunchConfiguration)
	 *
	 * Note: Redefined to be public accessible. Needed to access the breakpoints without
	 *       duplicating the super implementation.
	 */
	@Override
	public IBreakpoint[] getBreakpoints(ILaunchConfiguration configuration) {
		return super.getBreakpoints(configuration);
	}

	protected void onLaunchFinished(ILaunch launch, IStatus status) {

	}
}
