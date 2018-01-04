package uk.ac.ebi.subs.fileupload.repository.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.subs.fileupload.repository.model.File;
import uk.ac.ebi.subs.fileupload.util.FileStatus;

/**
 * This is a MongoDB data repository for {@link File}s.
 * There are some additional finder methods defined.
 */
@Repository
public interface FileRepository extends MongoRepository<File, String> {

    File findByTusId(String tusId);
    Page<File> findBySubmissionId(String submissionId, Pageable pageable);
    File findByFilenameAndSubmissionId(String filename, String submissionId);
    Page<File> findByUser(String User, Pageable pageable);
    Page<File> findByStatus(FileStatus status, Pageable pageable);
}
