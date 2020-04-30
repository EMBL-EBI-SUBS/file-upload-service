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
import uk.ac.ebi.subs.data.fileupload.FileStatus;
import uk.ac.ebi.subs.fileupload.services.EventHandlerService;
import uk.ac.ebi.subs.fileupload.util.FileSource;
import uk.ac.ebi.subs.fileupload.util.Utils;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.fileupload.File;
import uk.ac.ebi.subs.repository.model.fileupload.GlobusShare;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.fileupload.FileRepository;
import uk.ac.ebi.subs.repository.repos.fileupload.GlobusShareRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class GlobusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobusService.class);

    @Autowired
    private GlobusShareRepository globusShareRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private EventHandlerService eventHandlerService;

    @Autowired
    private MongoOperations mongoOperations;

    @Autowired
    private GlobusApiClient globusApiClient;

    @Value("${file-upload.sourceBasePath}")
    private String sourceBasePath;
    @Value("${file-upload.targetBasePath}")
    private String targetBasePath;

    @Value("${file-upload.globus.shareUrlStringFormat}")
    private String shareUrlFormat;

    @Value("${file-upload.globus.baseUploadDirectory}")
    private String baseUploadDir;
    @Value("${file-upload.globus.hostEndpoint.baseDirectory}")
    private String hostEndpointBaseDir;

    @Value("${file-upload.filePrefixForLocalProcessing}")
    private String filePrefixForLocalProcessing;

    public String getShareLink(String owner, String submissionId) {
        return getOrCreateGlobusShare(owner, submissionId).getShareLink();
    }

    public void processUploadedFiles(String owner, String submissionId, List<String> files) {
        files.stream()
                .filter(file -> file != null && !file.isBlank()
                            && fileRepository.findByFilenameAndSubmissionId(file, submissionId) == null)
                .map(file -> Paths.get(baseUploadDir, owner, file))
                .filter(filePath -> Files.exists(filePath))
                .map(filePath -> new java.io.File(filePath.toUri()))
                .map(file -> createFileObject(owner, submissionId, file))
                .forEach(fileObj -> {
                    try {
                        fileRepository.save(fileObj);

                        moveFile(fileObj);

                        fileObj.setUploadPath(fileObj.getTargetPath());
                        fileObj.setStatus(FileStatus.READY_FOR_CHECKSUM);
                        fileRepository.save(fileObj);

                        File postReferenceValidationFile = eventHandlerService.validateFileReference(fileObj.getGeneratedTusId());

                        processFile(postReferenceValidationFile);
                    } catch (Exception ex) {
                        throw new RuntimeException("Error processing uploaded file. Owner : " + owner +
                                ", SubmissionID : " + submissionId + ", FileTusID : " + fileObj.getGeneratedTusId(), ex);
                    }
                });
    }

    public void unregisterSubmission(String owner, String submissionId) {
        GlobusShare gs = globusShareRepository.findOne(owner);
        if (gs == null
                || gs.getRegisteredSubmissionIds().stream().filter(subId -> subId.equals(submissionId)).findFirst().isEmpty()) {
            return;
        }

        mongoOperations.findAndModify(
                Query.query(Criteria.where("owner").is(owner)),
                new Update().pull("registeredSubmissionIds", submissionId),
                FindAndModifyOptions.options().returnNew(true).upsert(false),
                GlobusShare.class);

        gs = mongoOperations.findAndRemove(
                Query.query(Criteria.where("owner").is(owner).and("registeredSubmissionIds").size(0)),
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
    private GlobusShare getOrCreateGlobusShare(String owner, String submissionId) {
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
                    gs = createGlobusShare(owner, submissionId);
                    if (gs != null) {
                        return gs;
                    }

                    //gs = null means that some other thread managed to create a share.
                    //Reset the outer wait loop and read that share document in the next iteration.
                    i = 0;
                }
            } while (gs == null);

            //Share link becomes available after the creation of the document.
            if (gs.getSharedEndpointId() != null) {
                //Register submission with the share.
                gs = mongoOperations.findAndModify(
                        Query.query(Criteria.where("owner").is(owner)),
                        new Update().addToSet("registeredSubmissionIds", submissionId),
                        FindAndModifyOptions.options().returnNew(true).upsert(false),
                        GlobusShare.class);

                //If the share got removed before the above operation . . .
                if (gs == null) {
                    LOGGER.debug("Share no longer available for registration. Owner : {}, SubmissionID : {}",
                            owner, submissionId);
                    //. . . reset the waiting period and repeat.
                    i = 0;
                } else {
                    break;
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.error("Error waiting for Globus share availability. Owner : {}, SubmissionID : {}",
                        owner, submissionId, e);
            }
        }

        if (gs.getSharedEndpointId() == null) {
            throw new RuntimeException("Share availability waiting period expired. owner : " + owner + ", submissionId : " + submissionId);
        }

        return gs;
    }

    private GlobusShare createGlobusShare(String owner, String submissionId) {
        LOGGER.debug("Creating globus share. owner : {}, submissionId : {}", owner, submissionId);

        GlobusShare gs = new GlobusShare();
        gs.setOwner(owner);
        gs.getRegisteredSubmissionIds().add(submissionId);

        try {
            gs = globusShareRepository.save(gs);
            createUploadDirectory(gs);
            return createShareLink(gs);
        } catch (DuplicateKeyException ex) {
            LOGGER.info("Cannot create a another share document for the given owner : {}, submissionId : {}", owner, submissionId);
        }

        return null;
    }

    private void createUploadDirectory(GlobusShare gs) {
        LOGGER.debug("Creating upload directory for owner : {}", gs.getOwner());

        Path targetPath = Paths.get(baseUploadDir, gs.getOwner());
        if (Files.notExists(targetPath)) {
            try {
                LOGGER.debug("Creating upload directory for owner : {}, directory : {}", gs.getOwner(), targetPath.toString());
                Files.createDirectories(targetPath);
                LOGGER.debug("Setting permissions on upload directory for owner : {}, directory : {}", gs.getOwner(), targetPath.toString());

                ProcessBuilder pb = new ProcessBuilder("/usr/bin/chmod", "g+w", targetPath.toString());
                Process proc = pb.start();
                if (proc.waitFor() != 0) {
                    throw new RuntimeException("Error setting permissions on upload directory. Owner : " + gs.getOwner() + ", Directory : " + targetPath.toString());
                }
            } catch (Exception ex) {
                LOGGER.debug("Error creating upload directory for owner : {}. Deleting share document.", gs.getOwner());

                //delete the share object to make retry possible.
                globusShareRepository.delete(gs.getId());

                throw new RuntimeException("Error creating upload directory for owner : " + gs.getOwner(), ex);
            }
        } else {
            LOGGER.debug("Upload directory already exists for owner : {}", gs.getOwner());
        }
    }

    private GlobusShare createShareLink(GlobusShare gs) {
        LOGGER.debug("Creating share link for owner : {}", gs.getOwner());

        String sharedEndpointId = null;
        try {
            sharedEndpointId = globusApiClient.createShare(
                    hostEndpointBaseDir + "/" + gs.getOwner(), UUID.randomUUID().toString(), "");
        } catch (Exception ex) {
            LOGGER.debug("Error creating globus share for owner : {}. Deleting share document.", gs.getOwner());

            //delete the share object to make retry possible.
            globusShareRepository.delete(gs.getId());

            throw new RuntimeException("Error creating globus share for owner : " + gs.getOwner(), ex);
        }

        gs.setSharedEndpointId(sharedEndpointId);
        gs.setShareLink(String.format(shareUrlFormat, sharedEndpointId));

        LOGGER.debug("Updating share document with new endpoint and share link. Owner : {}", gs.getOwner());

        return globusShareRepository.save(gs);
    }

    private File createFileObject(String owner, String submissionId, java.io.File file) {
        Submission submission = submissionRepository.findOne(submissionId);

        File fileObj = new File();
        fileObj.setCreatedBy(submission.getSubmitter().getName());
        fileObj.setFilename(file.getName());
        fileObj.setGeneratedTusId(UUID.randomUUID().toString());
        fileObj.setId(fileObj.getGeneratedTusId());
        fileObj.setSource(FileSource.GLOBUS.toString());
        fileObj.setStatus(FileStatus.UPLOADED);
        fileObj.setSubmissionId(submissionId);
        fileObj.setTotalSize(file.length());
        fileObj.setUploadedSize(file.length());
        fileObj.setUploadStartDate(LocalDateTime.now());
        fileObj.setUploadFinishDate(fileObj.getUploadStartDate());
        fileObj.setUploadPath(file.getAbsolutePath());
        fileObj.setTargetPath(assembleTargetPath(owner, submissionId, file.getAbsolutePath()));

        return fileObj;
    }

    private String assembleTargetPath(String owner, String submissionId, String sourceFilePath) {
        String genDirsPath = Utils.generateFolderName(submissionId);

        //Considering the possibility that files might have been uploaded into sub folders,
        //preserve the paths in the assembled target path.
        String startsWithOwner = sourceFilePath.substring(sourceFilePath.indexOf(owner));
        String withoutOwner = startsWithOwner.substring(startsWithOwner.indexOf("/") + 1);

        return Paths.get(sourceBasePath, targetBasePath, genDirsPath, withoutOwner).toString();
    }

    private void moveFile(File file) {
        try {
            Files.createDirectories(Paths.get(file.getTargetPath()).getParent());
            Files.move(Paths.get(file.getUploadPath()), Paths.get(file.getTargetPath()), StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException("Error while moving file : " + file.toString(), e);
        }
    }

    private void processFile(File file) {
        if (!file.getFilename().startsWith(filePrefixForLocalProcessing)) {
            eventHandlerService.executeFileProcessingOnCluster(file);
        } else {
            eventHandlerService.executeFileProcessingOnVM(file);
        }
    }
}
