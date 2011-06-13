/**
 * This file is licensed under the University of Illinois/NCSA Open Source License. See LICENSE.TXT for details.
 */
package edu.illinois.bundleupdater;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class Updater implements IStartup {

	private static final String UPDATE_SITE_URI = "file:<path to where the git repository is cloned>/BundleUpdater/edu.illinois.bundleupdater.updatesite/";

	private URI getUpdateSiteURI() {
		try {
			return new URI(UPDATE_SITE_URI);
		} catch (URISyntaxException e) {
			Activator.getDefault().logErrorStatus("Invalid update site URI", e);
		}
		return null;
	}

	@Override
	public void earlyStartup() {
		BundleContext context = Activator.getContext();
		ServiceReference serviceReference = context
				.getServiceReference(IProvisioningAgentProvider.SERVICE_NAME);
		if (serviceReference == null)
			return;

		IProvisioningAgentProvider agentProvider = (IProvisioningAgentProvider) context
				.getService(serviceReference);
		try {
			final IProvisioningAgent agent = agentProvider.createAgent(null);

			IMetadataRepositoryManager metadataRepositoryManager = (IMetadataRepositoryManager) agent
					.getService(IMetadataRepositoryManager.SERVICE_NAME);
			IArtifactRepositoryManager artifactRepositoryManager = (IArtifactRepositoryManager) agent
					.getService(IArtifactRepositoryManager.SERVICE_NAME);

			metadataRepositoryManager.addRepository(getUpdateSiteURI());
			artifactRepositoryManager.addRepository(getUpdateSiteURI());

			IMetadataRepository metadataRepository = metadataRepositoryManager
					.loadRepository(getUpdateSiteURI(),
							new NullProgressMonitor());

			Collection<IInstallableUnit> iusToUpdate = metadataRepository
					.query(QueryUtil.createLatestIUQuery(),
							new NullProgressMonitor()).toUnmodifiableSet();

			System.err.println("iusToUpdate=" + iusToUpdate);

			// The following operation object works for updating all installed
			// plug-ins.

			// final UpdateOperation updateOperation = new UpdateOperation(
			// new ProvisioningSession(agent));

			final UpdateOperation updateOperation = new UpdateOperation(
					new ProvisioningSession(agent), iusToUpdate);

			IStatus modalResolution = updateOperation
					.resolveModal(new NullProgressMonitor());

			System.err.println("modalResolution=" + modalResolution.toString());

			if (modalResolution.isOK()) {
				Display.getDefault().asyncExec(new Runnable() {

					@Override
					public void run() {
						ProvisioningUI.getDefaultUI().openUpdateWizard(false,
								updateOperation, null);
					}
				});

			}
		} catch (ProvisionException e) {
			Activator.getDefault().logErrorStatus(
					"Provisioning exception while checking for updates", e);
		}
	}

}
