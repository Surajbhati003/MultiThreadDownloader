import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Merger {
    public static void mergeChunksIntoFile(String directory, int totalChunks, String outputFile) throws IOException {
        try (FileOutputStream output = new FileOutputStream(directory + "/" + outputFile)) {
            for (int partIndex = 0; partIndex < totalChunks; partIndex++) {
                String chunkFilePath = directory + "/part" + partIndex;
                try (InputStream chunkInputStream = Files.newInputStream(Paths.get(chunkFilePath))) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = chunkInputStream.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                }
                Files.delete(Paths.get(chunkFilePath)); 
            }
            System.out.println("File successfully merged into " + outputFile);
        }
    }
}
