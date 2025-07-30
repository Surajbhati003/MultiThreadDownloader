package com.velance.downloader;

import com.velance.utils.FileUtils;
import com.velance.utils.ProgressBar;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;

/**
 * Day 3-4: Advanced DownloadManager using ExecutorService and ThreadPoolExecutor
 * Provides better thread management, progress tracking, and error handling
 */
public class DownloadManager {
    private final ExecutorService executorService;
    private final int maxConnections;
    private final String downloadDirectory;
    
    // Progress tracking
    private final AtomicLong totalBytesDownloaded = new AtomicLong(0);
    private volatile long totalFileSize = 0;
    private volatile boolean downloadInProgress = false;
    
    // Download metadata
    private String currentUrl;
    private String currentFileName;
    private List<Future<DownloadResult>> downloadTasks;
    
    public DownloadManager(int maxConnections, String downloadDirectory) {
        this.maxConnections = Math.max(1, Math.min(maxConnections, 16)); // Limit between 1-16
        this.downloadDirectory = downloadDirectory;
        
        // Create thread pool with custom naming and proper shutdown handling
        this.executorService = new ThreadPoolExecutor(
            maxConnections,                           // corePoolSize
            maxConnections,                           // maximumPoolSize  
            60L,                                      // keepAliveTime
            TimeUnit.SECONDS,                         // time unit
            new LinkedBlockingQueue<>(),              // work queue
            new DownloadThreadFactory(),              // thread factory
            new ThreadPoolExecutor.CallerRunsPolicy() // rejection policy
        );
        
        // Ensure proper cleanup on JVM shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }
    
    /**
     * Main method to start a download with progress monitoring
     */
    public boolean downloadFile(String url) {
        if (downloadInProgress) {
            System.err.println("Another download is already in progress!");
            return false;
        }
        
        try {
            downloadInProgress = true;
            currentUrl = url;
            currentFileName = extractFileName(url);
            
            System.out.println("=== Starting Download ===");
            System.out.println("URL: " + url);
            System.out.println("File: " + currentFileName);
            System.out.println("Threads: " + maxConnections);
            
            // Check server capabilities
            ServerInfo serverInfo = analyzeServer(url);
            if (!serverInfo.isAccessible) {
                System.err.println("Server is not accessible!");
                return false;
            }
            
            totalFileSize = serverInfo.fileSize;
            System.out.println("File size: " + FileUtils.formatBytes(totalFileSize));
            System.out.println("Range support: " + (serverInfo.supportsRanges ? "Yes" : "No"));
            
            boolean success;
            if (serverInfo.supportsRanges && maxConnections > 1 && totalFileSize > 1024 * 1024) { // Use multithreading for files > 1MB
                success = downloadWithThreadPool(serverInfo);
            } else {
                success = downloadSingleThread(serverInfo);
            }
            
            return success;
            
        } catch (Exception e) {
            System.err.println("Download failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            downloadInProgress = false;
            totalBytesDownloaded.set(0);
        }
    }
    
    /**
     * Download using thread pool for maximum efficiency
     */
    private boolean downloadWithThreadPool(ServerInfo serverInfo) throws Exception {
        System.out.println("Using multithreaded download...");
        
        long chunkSize = totalFileSize / maxConnections;
        downloadTasks = new ArrayList<>();
        
        String outputPath = downloadDirectory + File.separator + currentFileName;
        
        // Submit download tasks
        for (int i = 0; i < maxConnections; i++) {
            long startByte = i * chunkSize;
            long endByte = (i == maxConnections - 1) ? totalFileSize - 1 : (startByte + chunkSize - 1);
            
            String partFile = outputPath + ".part" + i;
            DownloadTask task = new DownloadTask(currentUrl, partFile, startByte, endByte, i, totalBytesDownloaded);
            
            Future<DownloadResult> future = executorService.submit(task);
            downloadTasks.add(future);
        }
        
        // Monitor progress
        monitorProgress();
        
        // Wait for all tasks to complete and check results
        boolean allSuccessful = true;
        List<DownloadResult> results = new ArrayList<>();
        
        for (Future<DownloadResult> future : downloadTasks) {
            try {
                DownloadResult result = future.get(300, TimeUnit.SECONDS); // 5 minute timeout per chunk
                results.add(result);
                
                if (!result.success) {
                    allSuccessful = false;
                    System.err.println("Task failed: " + result.errorMessage);
                }
            } catch (TimeoutException e) {
                future.cancel(true);
                allSuccessful = false;
                System.err.println("Task timed out");
            } catch (Exception e) {
                allSuccessful = false;
                System.err.println("Task execution failed: " + e.getMessage());
            }
        }
        
        if (allSuccessful) {
            System.out.println("\nAll chunks downloaded successfully!");
            
            // Merge files
            FileMerger merger = new FileMerger();
            merger.mergeFiles(outputPath, maxConnections);
            
            System.out.println("[SUCCESS] Download completed: " + outputPath);
            return true;
        } else {
            System.err.println("[ERROR] Download failed - not all chunks completed successfully");
            return false;
        }
    }
    
    /**
     * Fallback single-threaded download
     */
    private boolean downloadSingleThread(ServerInfo serverInfo) throws Exception {
        System.out.println("Using single-threaded download...");
        
        String outputPath = downloadDirectory + File.separator + currentFileName;
        
        URL url = new URL(currentUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        conn.connect();
        
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("Server returned: " + conn.getResponseCode());
        }
        
        try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream(), 32768);
             FileOutputStream out = new FileOutputStream(outputPath);
             BufferedOutputStream bout = new BufferedOutputStream(out, 32768)) {
            
            byte[] buffer = new byte[32768];
            int bytesRead;
            ProgressBar progressBar = new ProgressBar(totalFileSize);
            
            while ((bytesRead = in.read(buffer)) != -1) {
                bout.write(buffer, 0, bytesRead);
                long downloaded = totalBytesDownloaded.addAndGet(bytesRead);
                progressBar.update(downloaded);
            }
            
            progressBar.finish();
        }
        
        conn.disconnect();
        System.out.println("[SUCCESS] Download completed: " + outputPath);
        return true;
    }
    
    /**
     * Monitor download progress across all threads
     */
    private void monitorProgress() {
        Thread progressMonitor = new Thread(() -> {
            ProgressBar progressBar = new ProgressBar(totalFileSize);
            
            while (downloadInProgress && !Thread.currentThread().isInterrupted()) {
                long downloaded = totalBytesDownloaded.get();
                progressBar.update(downloaded);
                
                if (downloaded >= totalFileSize) {
                    break;
                }
                
                try {
                    Thread.sleep(250); // Update every 250ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            progressBar.finish();
        });
        
        progressMonitor.setName("ProgressMonitor");
        progressMonitor.setDaemon(true);
        progressMonitor.start();
    }
    
    /**
     * Analyze server capabilities
     */
    private ServerInfo analyzeServer(String url) throws IOException {
        ServerInfo info = new ServerInfo();
        
        URL downloadUrl = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) downloadUrl.openConnection();
        conn.setRequestMethod("HEAD");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.connect();
        
        try {
            int responseCode = conn.getResponseCode();
            info.isAccessible = (responseCode == HttpURLConnection.HTTP_OK);
            
            if (info.isAccessible) {
                info.fileSize = conn.getContentLengthLong();
                String acceptRanges = conn.getHeaderField("Accept-Ranges");
                info.supportsRanges = "bytes".equalsIgnoreCase(acceptRanges);
                
                // Additional server info
                info.serverType = conn.getHeaderField("Server");
                info.contentType = conn.getHeaderField("Content-Type");
                info.lastModified = conn.getHeaderField("Last-Modified");
            }
        } finally {
            conn.disconnect();
        }
        
        return info;
    }
    
    /**
     * Cancel current download
     */
    public void cancelDownload() {
        System.out.println("Cancelling download...");
        downloadInProgress = false;
        
        if (downloadTasks != null) {
            for (Future<DownloadResult> future : downloadTasks) {
                future.cancel(true);
            }
        }
        
        System.out.println("Download cancelled");
    }
    
    /**
     * Get current download progress as percentage
     */
    public double getProgress() {
        if (totalFileSize <= 0) return 0.0;
        return (double) totalBytesDownloaded.get() / totalFileSize * 100.0;
    }
    
    /**
     * Check if download is currently in progress
     */
    public boolean isDownloadInProgress() {
        return downloadInProgress;
    }
    
    /**
     * Get download speed in bytes per second
     */
    public long getDownloadSpeed() {
        // This is a simplified implementation
        // In a real application, you'd track bytes over time
        return totalBytesDownloaded.get();
    }
    
    /**
     * Extract filename from URL
     */
    private String extractFileName(String url) {
        try {
            String path = new URL(url).getPath();
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            
            if (fileName.isEmpty() || !fileName.contains(".")) {
                fileName = "download_" + System.currentTimeMillis();
            }
            
            return fileName;
        } catch (Exception e) {
            return "download_" + System.currentTimeMillis();
        }
    }
    
    /**
     * Shutdown the download manager
     */
    public void shutdown() {
        System.out.println("Shutting down DownloadManager...");
        
        if (downloadInProgress) {
            cancelDownload();
        }
        
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("DownloadManager shutdown complete");
    }
    
    // Inner classes and data structures
    
    /**
     * Custom thread factory for naming download threads
     */
    private static class DownloadThreadFactory implements ThreadFactory {
        private int threadNumber = 1;
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "DownloadWorker-" + threadNumber++);
            t.setDaemon(false);
            return t;
        }
    }
    
    /**
     * Server information holder
     */
    private static class ServerInfo {
        boolean isAccessible = false;
        boolean supportsRanges = false;
        long fileSize = 0;
        String serverType;
        String contentType;
        String lastModified;
    }
    
    /**
     * Download task result
     */
    private static class DownloadResult {
        boolean success;
        String errorMessage;
        long bytesDownloaded;
        int threadId;
        
        DownloadResult(boolean success, String errorMessage, long bytesDownloaded, int threadId) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.bytesDownloaded = bytesDownloaded;
            this.threadId = threadId;
        }
    }
    
    /**
     * Individual download task for thread pool execution
     */
    private static class DownloadTask implements Callable<DownloadResult> {
        private final String url;
        private final String partFileName;
        private final long startByte;
        private final long endByte;
        private final int threadId;
        private final AtomicLong totalProgress;
        
        DownloadTask(String url, String partFileName, long startByte, long endByte, int threadId, AtomicLong totalProgress) {
            this.url = url;
            this.partFileName = partFileName;
            this.startByte = startByte;
            this.endByte = endByte;
            this.threadId = threadId;
            this.totalProgress = totalProgress;
        }
        
        @Override
        public DownloadResult call() {
            try {
                long bytesDownloaded = downloadChunk();
                return new DownloadResult(true, null, bytesDownloaded, threadId);
            } catch (Exception e) {
                return new DownloadResult(false, e.getMessage(), 0, threadId);
            }
        }
        
        private long downloadChunk() throws IOException {
            URL downloadUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) downloadUrl.openConnection();
            
            conn.setRequestProperty("Range", "bytes=" + startByte + "-" + endByte);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.connect();
            
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + responseCode);
            }
            
            long expectedBytes = endByte - startByte + 1;
            long bytesDownloaded = 0;
            
            try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream(), 32768);
                 FileOutputStream out = new FileOutputStream(partFileName);
                 BufferedOutputStream bout = new BufferedOutputStream(out, 32768)) {
                
                byte[] buffer = new byte[32768];
                int bytesRead;
                
                while ((bytesRead = in.read(buffer)) != -1 && bytesDownloaded < expectedBytes) {
                    int bytesToWrite = (int) Math.min(bytesRead, expectedBytes - bytesDownloaded);
                    bout.write(buffer, 0, bytesToWrite);
                    
                    bytesDownloaded += bytesToWrite;
                    totalProgress.addAndGet(bytesToWrite);
                    
                    // Check for cancellation
                    if (Thread.currentThread().isInterrupted()) {
                        throw new IOException("Download cancelled");
                    }
                }
            } finally {
                conn.disconnect();
            }
            
            return bytesDownloaded;
        }
    }
}
