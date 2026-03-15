package ge.dola.talanti.media.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Component
public class LocalStorageProvider implements StorageProvider {

    private final Path rootLocation;

    public LocalStorageProvider(@Value("${app.storage.local.dir:uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir);
        try {
            Files.createDirectories(rootLocation);
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    @Override
    public String store(InputStream inputStream, String filename, String contentType) throws Exception {
        Path destinationFile = this.rootLocation.resolve(Paths.get(filename)).normalize().toAbsolutePath();
        Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        // In a real app, this should probably return a path that a dedicated ImageController serves
        return "/uploads/" + filename;
    }
}