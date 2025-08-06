import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiDownloader {
    public static void main(String[] args) {
        Random rand = new Random();
        String fileUrl = "https://thumbs.dreamstime.com/b/spectacular-mountain-ranges-silhouettes-man-reaching-summit-enjoying-freedom-happy-winning-success-sunset-sunrise-standing-89912845.jpg";

        // --- SINGLE THREAD DOWNLOAD TEST ---
        String singleOutputPath = "./SingleDownload_" + rand.nextInt() + "/img1.jpg";
        SingleDownload.download(fileUrl, singleOutputPath);

        // --- MULTI THREAD DOWNLOAD TEST ---
        String downloadDirectory = "./Download_" + rand.nextInt();
        String outputFileName = "img1.jpg";
        int threadCount = 6;

        try {
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.connect();

            int statusCode = connection.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_OK) {
                long totalFileSize = connection.getContentLengthLong();
                long segmentSize = totalFileSize / threadCount;

                Files.createDirectories(Paths.get(downloadDirectory));

                ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
                long startTime = System.currentTimeMillis();

                for (int index = 0; index < threadCount; index++) {
                    long segmentStart = index * segmentSize;
                    long segmentEnd = (index == threadCount - 1) ? totalFileSize - 1 : segmentStart + segmentSize - 1;
                    threadPool.execute(new MultiDownload(url, segmentStart, segmentEnd, index, downloadDirectory));
                }

                threadPool.shutdown();
                while (!threadPool.isTerminated()) {}

                Merger.mergeChunksIntoFile(downloadDirectory, threadCount, outputFileName);

                long endTime = System.currentTimeMillis();
                System.out.println("Multi-threaded download completed in " + (endTime - startTime) + " ms");

            } else {
                System.out.println("Failed to connect: " + statusCode);
            }

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
