package uk.ac.ebi.subs.fileupload.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(JUnit4.class)
public class FileTypeTest {

    private FileType fileType;

    @Test
    public void whenNotSupportedFileTypeGiven_ThenReturnNull() {
        String notSupportedFile = "funny.mp4";

        assertThat(FileType.getFileTypeByExtension(notSupportedFile), is(equalTo(null)));
    }

    @Test
    public void whenSupportedFileTypeGiven_ThenReturnTheFileType() {
        String supportedFile = "/this/is/the/path/zebrafish.fastq.gz";

        String expectedFileType = "FASTQ";

        assertThat(FileType.getFileTypeByExtension(supportedFile), is(equalTo(expectedFileType)));
    }
}
