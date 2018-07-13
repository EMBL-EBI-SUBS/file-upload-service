package uk.ac.ebi.subs.fileupload.util;

import java.util.HashMap;
import java.util.Map;

public class FileType {

    private static Map<String, String> extensionToType = new HashMap<>();

    static {
        extensionToType.put("fastq.gz", "FASTQ");
        extensionToType.put("fq.gz", "FASTQ");
        extensionToType.put("vcf", "VCF");
        extensionToType.put("vcf.gz", "VCF");
    }

    public static String getFileTypeByExtension(String filename) {
        for (String extension : extensionToType.keySet()) {
            if (filename.endsWith(extension)) {
                return extensionToType.get(extension);
            }
        }

        return null;
    }
}
