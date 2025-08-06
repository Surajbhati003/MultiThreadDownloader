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
        String fileUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRqEzSKHKnhS45Z7nEl040UNye83x9wQG08pw&s";
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
                System.out.println("Connection successful.");
                long totalFileSize = connection.getContentLengthLong();
                System.out.println("File size: " + totalFileSize + " bytes.");

                long segmentSize = totalFileSize / threadCount;

                Files.createDirectories(Paths.get(downloadDirectory));

                ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);

                for (int index = 0; index < threadCount; index++) {
                    long segmentStart = index * segmentSize;
                    long segmentEnd = (index == threadCount - 1) ? totalFileSize - 1 : segmentStart + segmentSize - 1;
                    threadPool.execute(new MultiDownload(url, segmentStart, segmentEnd, index, downloadDirectory));
                }

                threadPool.shutdown();
                while (!threadPool.isTerminated()) {}

                System.out.println("All segments downloaded.");
                Merger.mergeChunksIntoFile(downloadDirectory, threadCount, outputFileName);
            } else {
                System.out.println("Failed to connect: " + statusCode);
            }

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}