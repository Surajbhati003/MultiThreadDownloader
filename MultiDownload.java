import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable; // Import Callable

public class MultiDownload implements Callable<Boolean> { // Implement Callable<Boolean>
    private final URL resourceUrl;
    private final long rangeStart, rangeEnd;
    private final int chunkIndex;
    private final String downloadDirectory;

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    public MultiDownload(URL resourceUrl, long rangeStart, long rangeEnd, int chunkIndex, String downloadDirectory) {
        this.resourceUrl = resourceUrl;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.chunkIndex = chunkIndex;
        this.downloadDirectory = downloadDirectory;
    }

    @Override
    public Boolean call() { // Changed from run() to call()
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) resourceUrl.openConnection();
                connection.setInstanceFollowRedirects(true);
                String byteRange = "bytes=" + rangeStart + "-" + rangeEnd;
                connection.setRequestProperty("Range", byteRange);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    try (InputStream inputStream = connection.getInputStream();
                         FileOutputStream fileOutput = new FileOutputStream(downloadDirectory + "/part" + chunkIndex)) {

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            fileOutput.write(buffer, 0, bytesRead);
                        }
                        System.out.println("Chunk " + chunkIndex + " downloaded successfully.");
                        return true; // --- SUCCESS: Return true ---
                    }
                } else {
                     System.err.println("Chunk " + chunkIndex + " received non-OK HTTP response: " + responseCode);
                }

            } catch (IOException e) {
                System.err.println("Attempt " + (attempt + 1) + " failed for chunk " + chunkIndex + ": " + e.getMessage());
                if (attempt + 1 == MAX_RETRIES) {
                    break; 
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    System.err.println("Retry delay interrupted for chunk " + chunkIndex);
                    return false; // --- FAILURE: Return false ---
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        System.err.println("All retries failed for chunk " + chunkIndex + ".");
        return false; // --- FAILURE: Return false ---
    }
}
