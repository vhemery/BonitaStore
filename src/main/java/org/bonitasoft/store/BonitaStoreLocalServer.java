package org.bonitasoft.store;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.bonitasoft.engine.api.PageAPI;
import org.bonitasoft.engine.api.ProfileAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.page.Page;
import org.bonitasoft.engine.profile.Profile;
import org.bonitasoft.engine.profile.ProfileEntry;
import org.bonitasoft.engine.profile.ProfileNotFoundException;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.store.artifact.Artifact;
import org.bonitasoft.store.artifact.ArtifactCustomPage;
import org.bonitasoft.store.artifact.ArtifactLayout;
import org.bonitasoft.store.artifact.ArtifactProfile;
import org.bonitasoft.store.artifact.ArtifactRestApi;
import org.bonitasoft.store.artifact.ArtifactTheme;
import org.bonitasoft.store.artifact.Artifact.TypeArtifact;
import org.bonitasoft.store.toolbox.LoggerStore;
import org.bonitasoft.store.toolbox.LoggerStore.LOGLEVEL;

/* ******************************************************************************** */
/*                                                                                  */
/* BonitaStoreLocale */
/*                                                                                  */
/* Load the different artefact from the local Bonita Server. */
/*                                                                                  */
/* ******************************************************************************** */

public class BonitaStoreLocalServer extends BonitaStore {

    private static BEvent EVENT_NOT_IMPLEMENTED = new BEvent(BonitaStoreLocalServer.class.getName(), 1, Level.APPLICATIONERROR, "Not yet implemented", "The function is not yet implemented", "No valid return", "Wait the implementation");
    private static BEvent EVENT_BAD_API = new BEvent(BonitaStoreLocalServer.class.getName(), 1, Level.ERROR, "Bad API", "Bonita API can't be accessed", "No valid return", "Check the error");
    private static final BEvent EVENT_SEARCH_EXCEPTION = new BEvent(BonitaStoreLocalServer.class.getName(), 3, Level.ERROR, "Search exception", "The search in engine failed", "No valid return", "Check the log (maybe not the same BonitaVersion ?)");

    private static final BEvent EVENT_PROFILENOT_FOUND = new BEvent(BonitaStoreLocalServer.class.getName(), 4, Level.ERROR, "Profile not found", "A profile is register in a page, but can't be found by it's ID", "No valid return", "Check the log (maybe not the same BonitaVersion ?)");

    APISession apiSession;

    public BonitaStoreLocalServer(APISession apiSession) {
        this.apiSession = apiSession;
    }

    @Override
    public String getName() {
        return "BonitaStoreLocal";
    }

    public String getId() {
        return "BonitaStoreLocal";
    }

    @Override
    public boolean isManageDownload() {
        return false;
    }

    @Override
    public Map<String, Object> toMap() {
        return new HashMap<>();
    }

    @Override
    public BonitaStoreResult getListArtefacts(DetectionParameters detectionParameters, LoggerStore logBox) {
        BonitaStoreResult storeResult = new BonitaStoreResult("getListContent");

        // get list of pages
        if (detectionParameters.listTypeArtifact.contains(TypeArtifact.CUSTOMPAGE) || detectionParameters.listTypeArtifact.contains(TypeArtifact.LAYOUT) || detectionParameters.listTypeArtifact.contains(TypeArtifact.THEME) || detectionParameters.listTypeArtifact.contains(TypeArtifact.RESTAPI)) {
            Long profileId = null;
            PageAPI pageAPI;
            final ProfileAPI profileAPI;
            try {
                pageAPI = TenantAPIAccessor.getCustomPageAPI(apiSession);
                profileAPI = TenantAPIAccessor.getProfileAPI(apiSession);
            } catch (ServerAPIException | UnknownAPITypeException | BonitaHomeNotSetException e) {
                storeResult.addEvent(new BEvent(EVENT_BAD_API, ""));
                return storeResult;
            }
            try {

                SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 1000);
                final SearchResult<Page> searchResultPage = pageAPI.searchPages(searchOptionsBuilder.done());
                for (final Page page : searchResultPage.getResult()) {
                    Artifact artefact=null;
                    if ("page".equals(page.getContentType()) && detectionParameters.listTypeArtifact.contains(TypeArtifact.CUSTOMPAGE)) {
                        artefact = new ArtifactCustomPage(page, this);
                    }
                    if ("layout".equals(page.getContentType()) && detectionParameters.listTypeArtifact.contains(TypeArtifact.LAYOUT)) {
                        artefact = new ArtifactLayout(page, this);
                    }
                    if ("theme".equals(page.getContentType()) && detectionParameters.listTypeArtifact.contains(TypeArtifact.THEME)) {
                        artefact = new ArtifactTheme(page, this);
                    }
                    if ("apiExtension".equals(page.getContentType()) && detectionParameters.listTypeArtifact.contains(TypeArtifact.RESTAPI)) {
                        artefact = new ArtifactRestApi( page, this);
                    }
                    if (artefact!=null)
                        storeResult.listArtifacts.add(artefact);

                } // end page

                // search all profile, and populate the page
                searchOptionsBuilder = new SearchOptionsBuilder(0, 1000);
                SearchResult<ProfileEntry> searchResultProfile;

                searchResultProfile = profileAPI.searchProfileEntries(searchOptionsBuilder.done());

                for (final ProfileEntry profileEntry : searchResultProfile.getResult()) {
                    final String name = profileEntry.getPage();
                    profileId = profileEntry.getProfileId();
                    final Artifact artefactPage = storeResult.getArtefactByName(name);
                    if (artefactPage != null && artefactPage instanceof ArtifactCustomPage) {
                        final Profile profile = profileAPI.getProfile(profileId);
                        ArtifactProfile artefactProfile = new ArtifactProfile(profile, this);
                        ((ArtifactCustomPage) artefactPage).addOneProfile(artefactProfile);
                    }
                }
            } catch (final SearchException e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String exceptionDetails = sw.toString();
                storeResult.addEvent(new BEvent(EVENT_SEARCH_EXCEPTION, e, ""));
                logBox.log(LOGLEVEL.ERROR, "Exception " + e.getMessage() + " During Search " + exceptionDetails);

            } catch (final ProfileNotFoundException e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String exceptionDetails = sw.toString();
                storeResult.addEvent(new BEvent(EVENT_PROFILENOT_FOUND, e, "ProfileId:" + profileId));
                logBox.log(LOGLEVEL.ERROR, "Exception " + e.getMessage() + " During Profile Access " + exceptionDetails);
            }
        }

        return storeResult;
    }

    @Override
    public BonitaStoreResult downloadArtefact(Artifact artefactItem, UrlToDownload urlToDownload, LoggerStore logBox) {
        BonitaStoreResult storeResult = new BonitaStoreResult("getListContent");
        storeResult.addEvent(EVENT_NOT_IMPLEMENTED);
        return storeResult;
    }

    @Override
    public BonitaStoreResult ping(LoggerStore logBox) {
        return new BonitaStoreResult("ping");
    }

}
