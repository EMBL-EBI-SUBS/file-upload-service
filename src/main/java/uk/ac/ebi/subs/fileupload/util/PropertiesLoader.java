package uk.ac.ebi.subs.fileupload.util;

import uk.ac.ebi.subs.fileupload.errors.FileNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesLoader {

    public static Properties loadProperties(String resourceFileName) {
        Properties configuration = new Properties();

        try (InputStream inputStream = PropertiesLoader.class
                .getClassLoader()
                .getResourceAsStream(resourceFileName);) {
            configuration.load(inputStream);
        } catch (IOException e) {
            throw new FileNotFoundException(String.format(FileNotFoundException.FILE_NOT_FOUND_MESSAGE, resourceFileName));
        }

        return configuration;
    }
}
