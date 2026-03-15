package ge.dola.talanti.media.storage;

import java.io.InputStream;

public interface StorageProvider {
    /**
     * Stores the file and returns the public URL or unique identifier.
     */
    String store(InputStream inputStream, String filename, String contentType) throws Exception;
}

