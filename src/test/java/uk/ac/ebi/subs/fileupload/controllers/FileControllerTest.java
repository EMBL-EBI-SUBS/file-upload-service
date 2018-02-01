package uk.ac.ebi.subs.fileupload.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import uk.ac.ebi.subs.fileupload.errors.ErrorMessages;
import uk.ac.ebi.subs.fileupload.model.TUSFileInfo;
import uk.ac.ebi.subs.fileupload.repository.model.File;
import uk.ac.ebi.subs.fileupload.repository.repo.FileRepository;
import uk.ac.ebi.subs.fileupload.repository.util.FileHelper;
import uk.ac.ebi.subs.fileupload.services.FileService;
import uk.ac.ebi.subs.fileupload.services.SubmissionService;
import uk.ac.ebi.subs.fileupload.util.FileStatus;
import uk.ac.ebi.subs.fileupload.util.TusFileInfoHelper;

import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(value = {FileController.class, FileRepository.class})
public class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    private FileService fileService;

    @MockBean
    private SubmissionService submissionService;

    @MockBean
    private FileRepository fileRepository;

    private static final String INVALID_TOKEN = "invalid.token.value";
    private static final String INVALID_SUBMISSION_UUID = "12345";
    private static final String VALID_TOKEN = "valid.token.value";
    private static final String VALID_SUBMISSION_UUID = "12345";
    private static final String FILENAME = "test_file.cram";

    @Test
    public void whenDeleteFileWithInvalidToken_ThenDeletionFailsWithAuthenticationError() throws Exception {
        TUSFileInfo tusFileInfo = TusFileInfoHelper.generateTUSFileInfo(INVALID_TOKEN, VALID_SUBMISSION_UUID, FILENAME);
        File fileToDelete = FileHelper.convertTUSFileInfoToFile(tusFileInfo);
        String submissionId = fileToDelete.getSubmissionId();
        String jwtToken = tusFileInfo.getMetadata().getJwtToken();

        HttpClientErrorException httpClientErrorException = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);

        given(this.fileService.getFileByTusId(anyString())).willReturn(fileToDelete);
        given(this.submissionService.isModifiable(submissionId, jwtToken)).willThrow(httpClientErrorException);

        final String tusId = UUID.randomUUID().toString();
        tusFileInfo.setTusId(tusId);

        this.mockMvc.perform(delete("/file/{tusId}", tusFileInfo.getTusId())
                .header("Authorization", "Bearer " + jwtToken))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value(HttpStatus.UNAUTHORIZED.getReasonPhrase()))
                .andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.value()))
                .andExpect(jsonPath("$.errors[0]").value(ErrorMessages.UNAUTHORIZED_REQUEST));
    }

    @Test
    public void whenDeleteFileWithValidTokenButFileNotExists_ThenDeletionFailsWithFileNotFoundError() throws Exception {
        given(this.fileService.getFileByTusId(anyString())).willReturn(null);

        TUSFileInfo tusFileInfo = TusFileInfoHelper.generateTUSFileInfo(VALID_TOKEN, VALID_SUBMISSION_UUID, FILENAME);
        final String tusId = UUID.randomUUID().toString();
        tusFileInfo.setTusId(tusId);

        String jwtToken = tusFileInfo.getMetadata().getJwtToken();

        this.mockMvc.perform(delete("/file/{tusId}", tusFileInfo.getTusId())
                .header("Authorization", "Bearer " + jwtToken))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.errors[0]").value(String.format(ErrorMessages.FILE_DOCUMENT_NOT_FOUND, tusId)));
    }

    @Test
    public void whenDeleteFileWithValidTokenButSubmissionIsNotModifiable_ThenDeletionFailsWithSunmissionNotModifiableError() throws Exception{
        TUSFileInfo tusFileInfo = TusFileInfoHelper.generateTUSFileInfo(VALID_TOKEN, VALID_SUBMISSION_UUID, FILENAME);
        File fileToDelete = FileHelper.convertTUSFileInfoToFile(tusFileInfo);
        String submissionId = fileToDelete.getSubmissionId();
        String jwtToken = tusFileInfo.getMetadata().getJwtToken();

        given(this.fileService.getFileByTusId(anyString())).willReturn(fileToDelete);
        given(this.submissionService.isModifiable(submissionId, jwtToken)).willReturn(false);

        final String tusId = UUID.randomUUID().toString();
        tusFileInfo.setTusId(tusId);

        this.mockMvc.perform(delete("/file/{tusId}", tusFileInfo.getTusId())
                .header("Authorization", "Bearer " + jwtToken))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value(HttpStatus.CONFLICT.getReasonPhrase()))
                .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                .andExpect(jsonPath("$.errors[0]").value(String.format(
                        ErrorMessages.SUBMISSION_NOT_MODIFIABLE, submissionId)));
    }

    @Test
    public void whenDeleteFileWithValidParameters_ThenFileDeletionSucceed() throws Exception {
        TUSFileInfo tusFileInfo = TusFileInfoHelper.generateTUSFileInfo(VALID_TOKEN, VALID_SUBMISSION_UUID, FILENAME);
        File fileToDelete = FileHelper.convertTUSFileInfoToFile(tusFileInfo);
        String submissionId = fileToDelete.getSubmissionId();
        String jwtToken = tusFileInfo.getMetadata().getJwtToken();

        given(this.fileService.getFileByTusId(anyString())).willReturn(fileToDelete);
        fileToDelete.setStatus(FileStatus.MARK_FOR_DELETION);
        given(this.fileService.markFileForDeletion(any(File.class))).willReturn(fileToDelete);
        doNothing().when(this.fileService).deleteFileFromFileSystem(anyString());
        doNothing().when(this.fileService).removeDocumentMarkedForDeletion(any(File.class));
        given(this.submissionService.isModifiable(submissionId, jwtToken)).willReturn(true);

        final String tusId = UUID.randomUUID().toString();
        tusFileInfo.setTusId(tusId);

        this.mockMvc.perform(delete("/file/{tusId}", tusFileInfo.getTusId())
                .header("Authorization", "Bearer " + jwtToken))
                .andDo(print())
                .andExpect(status().isNoContent());
    }
}
