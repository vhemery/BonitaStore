package org.bonitasoft.store.artifactdeploy;

import org.bonitasoft.engine.page.Page;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.store.BonitaStoreAccessor;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.toolbox.LoggerStore;

public class DeployStrategyPage extends DeployStrategy {

    @Override
    public DeployOperation detectDeployment(Artifact artefact, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DeployOperation deploy(Artifact artefact, BonitaStoreAccessor bonitaAccessor, LoggerStore logBox) {
        DeployOperation deployOperation = new DeployOperation();
        // 

        Page currentPage = null;
        try {
            currentPage = bonitaAccessor.pageAPI.getPageByName(artefact.getName());
            // getPageByName does not work : search manually

            if (currentPage != null) {
                bonitaAccessor.pageAPI.updatePageContent(currentPage.getId(), artefact.getContent().toByteArray());
            } else {
                Page page = bonitaAccessor.pageAPI.createPage(artefact.getName(), artefact.getContent().toByteArray());
            }
            deployOperation.deploymentStatus = DeploymentStatus.DEPLOYED;
        } catch (Exception e) {
            deployOperation.deploymentStatus = DeploymentStatus.DEPLOYEDFAILED;
            deployOperation.listEvents.add(new BEvent(EventErrorAtDeployment, e, "Page [" + artefact.getName() + "]"));
        }
        return deployOperation;
    }

}