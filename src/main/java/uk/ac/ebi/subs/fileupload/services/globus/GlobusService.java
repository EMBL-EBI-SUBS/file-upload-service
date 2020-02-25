package uk.ac.ebi.subs.fileupload.services.globus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.repository.model.fileupload.GlobusShare;
import uk.ac.ebi.subs.repository.repos.fileupload.GlobusShareRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class GlobusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobusService.class);

    @Autowired
    private GlobusShareRepository globusShareRepository;

    @Autowired
    private MongoOperations mongoOperations;

    @Autowired
    private GlobusApiClient globusApiClient;

    @Value("${file-upload.globus.baseUploadDirectory}")
    private String baseUploadDir;

    @Value("${file-upload.globus.hostEndpoint.baseDirectory}")
    private String hostEndpointBaseDir;

    public String getShareLink(String owner) {
        return String.format("https://app.globus.org/file-manager?origin_id=%s", getOrCreateGlobusShare(owner));
    }

    public void registerSubmission(String owner, String submissionId) {

        GlobusShare gs = null;
        do {
            gs = getOrCreateGlobusShare(owner);

            //If the share document got deleted after the execution of previous line,
            //following operation will return null . . .
            gs = mongoOperations.findAndModify(
                    Query.query(Criteria.where("owner").is(owner)),
                    new Update().addToSet("openSubmissionIds", submissionId),
                    FindAndModifyOptions.options().returnNew(true).upsert(false),
                    GlobusShare.class);

            //. . . and then, repeat.
        } while (gs == null);
    }

    public void unregisterSubmission(String owner, String submissionId) {
        GlobusShare gs = globusShareRepository.findOne(owner);
        if (gs == null
                || gs.getOpenSubmissionIds().stream().filter(subId -> subId.equals(submissionId)).findFirst().isEmpty()) {
            return;
        }

        mongoOperations.findAndModify(
                Query.query(Criteria.where("owner").is(owner)),
                new Update().pull("openSubmissionIds", submissionId),
                FindAndModifyOptions.options().returnNew(true).upsert(false),
                GlobusShare.class);

        gs = mongoOperations.findAndRemove(
                Query.query(Criteria.where("owner").is(owner).and("openSubmissionIds").size(0)),
                GlobusShare.class);

        if (gs != null) {
            globusApiClient.deleteEndpoint(gs.getSharedEndpointId());
        }
    }

    /**
     * Globus only allows a limited number of shares. In order to use shares efficiently we try to reuse them if requested again for the same owner.
     * To synchronize share creation we rely on Spring data's unique key mechanism. So a share document is created and used
     * as a way to synchronize share creation across multiple requests for the same owner.<br/>
     * Forexample, if the owner is a user and if he/she attempts to create multiple submissions simultaneously.
     * Then we will only create one share for this user and expect him/her to use it for all the submissions he/she is hoping to make while the share exists.
     *
     * @param owner Owner of the share.
     * @return
     */
    private GlobusShare getOrCreateGlobusShare(String owner) {
        GlobusShare gs = null;

        //Wait for 1 second on each iteration of this loop until the share becomes available. 1sec X 30 = 30 seconds wait period.
        for (int i = 0; i < 30; i++) {
            do {
                //See if a share exists already.
                gs = globusShareRepository.findOne(owner);
                if (gs == null) {
                    //Attempt to create a new one if it does not.
                    //Following method attempts to create a unique share against the given owner. If multiple threads call this method
                    //then all except the first one that managed to create a share will fail and will return with a 'null' value.
                    gs = createGlobusShare(owner);
                    if (gs != null) {
                        return gs;
                    }

                    //gs = null means that some other thread managed to create a share.
                    //Reset the outer wait loop and read that share document in the next iteration.
                    i = 0;
                }
            } while (gs == null);

            //Share link becomes available after the creation of the document. Return it if its available.
            if (gs.getSharedEndpointId() != null) {
                break;
            }

            //Or wait . . .
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.error("Error waiting for Globus share availability.", e);
            }

            //. . . and repeat the whole process again as its quite possible that the share might have been deleted during the wait.
        }

        if (gs.getSharedEndpointId() == null) {
            throw new RuntimeException("Share availability waiting period expired. owner : " + owner);
        }

        return gs;
    }

    private GlobusShare createGlobusShare(String owner) {
        GlobusShare gs = new GlobusShare();
        gs.setOwner(owner);

        try {
            gs = globusShareRepository.save(gs);
            createUploadDirectory(gs);
            return createShareLink(gs);
        } catch (DuplicateKeyException ex) {
            LOGGER.info("Cannot create a another share document for the given owner : {}", owner);
        }

        return null;
    }

    private void createUploadDirectory(GlobusShare gs) {
        Path targetPath = Paths.get(baseUploadDir, gs.getOwner());
        if (Files.notExists(targetPath)) {
            try {
                Files.createDirectories(targetPath);
            } catch (Exception ex) {
                //delete the share object to make retry possible.
                globusShareRepository.delete(gs.getId());

                throw new RuntimeException(ex);
            }
        }
    }

    private GlobusShare createShareLink(GlobusShare gs) {
        String sharedEndpointId = null;
        try {
            sharedEndpointId = globusApiClient.createShare(
                    hostEndpointBaseDir + gs.getOwner(), gs.getOwner(), "");
        } catch (Exception ex) {
            //delete the share object to make retry possible.
            globusShareRepository.delete(gs.getId());

            throw ex;
        }

        gs.setSharedEndpointId(sharedEndpointId);

        return globusShareRepository.save(gs);
    }
}
