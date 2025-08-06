import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MultiDownload implements Runnable {

    private final URL resourceUrl;
    private final long rangeStart, rangeEnd;
    private final int chunkIndex;
    private final String downloadDirectory;

    public MultiDownload(URL resourceUrl, long rangeStart, long rangeEnd, int chunkIndex, String downloadDirectory) {
        this.resourceUrl = resourceUrl;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.chunkIndex = chunkIndex;
        this.downloadDirectory = downloadDirectory;
    }

    @Override
    public void run() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) resourceUrl.openConnection();
            connection.setInstanceFollowRedirects(true);
            String byteRange = "bytes=" + rangeStart + "-" + rangeEnd;
            connection.setRequestProperty("Range", byteRange);
            connection.connect();

            try (InputStream inputStream = connection.getInputStream(); FileOutputStream fileOutput = new FileOutputStream(downloadDirectory + "/part" + chunkIndex)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fileOutput.write(buffer, 0, bytesRead);
                }
                System.out.println("Chunk " + chunkIndex + " downloaded.");
            }
        } catch (IOException e) {
            System.err.println("Error downloading chunk " + chunkIndex + ": " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
