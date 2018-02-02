package uk.ac.ebi.subs.fileupload.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.fileupload.repository.model.File;
import uk.ac.ebi.subs.fileupload.repository.repo.FileRepository;
import uk.ac.ebi.subs.fileupload.repository.util.FileHelper;
import uk.ac.ebi.subs.fileupload.util.TusFileInfoHelper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;

@RunWith(SpringRunner.class)
public class EventHandlerServiceTest {

    private static final String EXISTING_FILE_NAME = "already_uploaded_file.cram";
    private static final String NEW_FILE_NAME = "new_file.cram";
    private static final String SUBMISSION_ID = "12ab34cd56ef";
    private static final String JWT_TOKEN = "dummy.jwt.token";

    private File persistedFile;

    @MockBean
    private FileRepository fileRepository;

    @MockBean
    private ValidationService validationService;

    private EventHandlerService eventHandlerService;

    @Before
    public void setup() {
        persistedFile = FileHelper.convertTUSFileInfoToFile(
                TusFileInfoHelper.generateTUSFileInfo(JWT_TOKEN, SUBMISSION_ID, EXISTING_FILE_NAME));

        eventHandlerService = new DefaultEventHandlerService(validationService, fileRepository);
    }

    @Test
    public void whenFileAlreadyUploadedToAGivenSubmission_ThenUploadAgainFailsValidation() {
        given(this.fileRepository.findByFilenameAndSubmissionId(EXISTING_FILE_NAME, SUBMISSION_ID))
                .willReturn(persistedFile);

        boolean isDuplicatedFile = eventHandlerService.isFileDuplicated(EXISTING_FILE_NAME, SUBMISSION_ID);

        assertTrue(isDuplicatedFile);
    }

    @Test
    public void whenFileNotYetUploadedToAGivenSubmission_ThenUploadItWillSucceed() {
        given(this.fileRepository.findByFilenameAndSubmissionId(NEW_FILE_NAME, SUBMISSION_ID))
                .willReturn(null);

        boolean isDuplicatedFile = eventHandlerService.isFileDuplicated(NEW_FILE_NAME, SUBMISSION_ID);

        assertFalse(isDuplicatedFile);
    }
}

