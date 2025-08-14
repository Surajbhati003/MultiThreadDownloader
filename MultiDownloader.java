import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.Comparator;
import java.util.Random;

public class MultiDownloader {
    @SuppressWarnings("deprecation")
    public static void main(String[] args) {
        String fileUrl = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4";
        int threadCount = 8;
        
        Random rand = new Random();
        String runDirectoryName = "./Download_Run_" + rand.nextInt();
        Path runDirectoryPath = Paths.get(runDirectoryName);
        
        // --- RESTORED: SINGLE-THREADED DOWNLOAD FOR COMPARISON ---
        System.out.println("--- Starting Single-Threaded Download Test ---");
        // We'll give it a unique name to avoid conflicts
        String singleOutputFileName = "single_thread_download.mp4";
        // The SingleDownload class handles creating its own directory and file
        SingleDownload.download(fileUrl, "./" + singleOutputFileName);
        System.out.println("--- Single-Threaded Download Finished ---\n");


        // --- MULTI-THREADED DOWNLOAD ---
        System.out.println("--- Starting Multi-Threaded Download Test ---");
        String multiOutputFileName = "multi_thread_download.mp4";
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String downloadDirectory = "./temp_download_" + timestamp;

        try {
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.connect();
            int statusCode = connection.getResponseCode();

            if (statusCode == HttpURLConnection.HTTP_OK) {
                long totalFileSize = connection.getContentLengthLong();
                System.out.println("File Size: " + totalFileSize / (1024 * 1024) + " MB");
                long segmentSize = totalFileSize / threadCount;

                Files.createDirectories(Paths.get(downloadDirectory));
                ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
                List<Future<Boolean>> futures = new ArrayList<>();

                long startTime = System.currentTimeMillis();

                for (int index = 0; index < threadCount; index++) {
                    long segmentStart = index * segmentSize;
                    long segmentEnd = (index == threadCount - 1) ? totalFileSize - 1 : segmentStart + segmentSize - 1;
                    
                    // --- START: MODIFICATION TO SIMULATE FAILURE ---
                    URL taskUrl = url;
                    // Let's intentionally fail the download for chunk number 2
                    // if (index == 2) {
                    //     System.out.println("\n>>> INTENTIONALLY SIMULATING A FAILURE FOR CHUNK 2 <<<\n");
                    //     // This invalid URL will cause an IOException
                    //     taskUrl = new URL("https://this-is-a-bad-url.invalid/file.jpg");
                    // }
                    // --- END: MODIFICATION ---

                    MultiDownload task = new MultiDownload(taskUrl, segmentStart, segmentEnd, index, downloadDirectory);
                    futures.add(threadPool.submit(task));
                }

                threadPool.shutdown();

                boolean allChunksDownloaded = true;
                for (Future<Boolean> future : futures) {
                    if (!future.get()) {
                        allChunksDownloaded = false;
                        break; 
                    }
                }

                if (allChunksDownloaded) {
                    System.out.println("All chunks downloaded successfully. Merging files...");
                    Merger.mergeChunksIntoFile(downloadDirectory, threadCount, multiOutputFileName);
                    long endTime = System.currentTimeMillis();
                    System.out.println("Multi-threaded download completed in " + (endTime - startTime) + " ms");


                } else {
                    System.err.println("One or more chunks failed to download. Merging aborted.");
                }

            } else {
                System.out.println("Failed to connect: " + statusCode);
            }
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("--- Multi-Threaded Download Finished ---");
    }
}
