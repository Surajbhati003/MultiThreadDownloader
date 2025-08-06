import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SingleDownload {
    public static void download(String fileUrl, String outputPath) {
        try {
            long startTime = System.currentTimeMillis();

            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            Files.createDirectories(Paths.get(outputPath).getParent());

            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(outputPath)) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            long endTime = System.currentTimeMillis();
            System.out.println("Single-threaded download completed in " + (endTime - startTime) + " ms");

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
