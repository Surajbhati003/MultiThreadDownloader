package com.velance.downloader;

import java.io.*;
import java.net.*;

/**
 * Day 2: DownloadThread class for handling individual chunk downloads
 * Implements Runnable for multithreaded downloading
 */
public class DownloadThread extends Thread {
    private final String url;
    private final String partFileName;
    private final long startByte;
    private final long endByte;
    private final int threadId;
    
    private volatile long bytesDownloaded = 0;
    private volatile boolean completed = false;
    private volatile boolean hasError = false;
    private volatile String errorMessage = "";
    
    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;
    
    public DownloadThread(String url, String partFileName, long startByte, long endByte, int threadId) {
        this.url = url;
        this.partFileName = partFileName;
        this.startByte = startByte;
        this.endByte = endByte;
        this.threadId = threadId;
        this.setName("DownloadThread-" + threadId);
    }
    
    @Override
    public void run() {
        int retryCount = 0;
        while (retryCount < MAX_RETRIES && !completed && !hasError) {
            try {
                downloadChunk();
                completed = true;
                System.out.println("Thread " + threadId + " completed successfully");
            } catch (IOException e) {
                retryCount++;
                hasError = true;
                errorMessage = e.getMessage();
                
                if (retryCount < MAX_RETRIES) {
                    System.out.println("Thread " + threadId + " failed, retrying... (" + retryCount + "/" + MAX_RETRIES + ")");
                    hasError = false; // Reset error flag for retry
                    
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        errorMessage = "Thread interrupted during retry";
                        break;
                    }
                } else {
                    System.err.println("Thread " + threadId + " failed after " + MAX_RETRIES + " retries: " + errorMessage);
                }
            }
        }
    }
    
    /**
     * Downloads the assigned chunk of the file
     */
    private void downloadChunk() throws IOException {
        URL downloadUrl = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) downloadUrl.openConnection();
        
        // Set range header for partial content
        conn.setRequestProperty("Range", "bytes=" + (startByte + bytesDownloaded) + "-" + endByte);
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000); // 10 seconds timeout
        conn.setReadTimeout(10000);    // 10 seconds read timeout
        conn.connect();
        
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Server returned HTTP response code: " + responseCode);
        }
        
        // Calculate expected bytes to download
        long expectedBytes = endByte - startByte + 1 - bytesDownloaded;
        
        // Check if resuming a partial download
        File partFile = new File(partFileName);
        boolean resuming = partFile.exists() && partFile.length() > 0;
        
        try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
             FileOutputStream out = new FileOutputStream(partFileName, resuming);
             BufferedOutputStream bout = new BufferedOutputStream(out)) {
            
            if (resuming) {
                bytesDownloaded = partFile.length();
                System.out.println("Thread " + threadId + " resuming from byte " + bytesDownloaded);
            }
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long totalRead = 0;
            
            while ((bytesRead = in.read(buffer)) != -1 && totalRead < expectedBytes) {
                // Ensure we don't read more than expected
                int bytesToWrite = (int) Math.min(bytesRead, expectedBytes - totalRead);
                bout.write(buffer, 0, bytesToWrite);
                
                totalRead += bytesToWrite;
                bytesDownloaded += bytesToWrite;
                
                // Break if we've downloaded all expected bytes
                if (totalRead >= expectedBytes) {
                    break;
                }
                
                // Check for thread interruption
                if (Thread.currentThread().isInterrupted()) {
                    throw new IOException("Thread interrupted");
                }
            }
            
            bout.flush();
        }
        
        conn.disconnect();
        
        // Verify that we downloaded the expected amount
        File finalFile = new File(partFileName);
        long actualFileSize = finalFile.length();
        long expectedFileSize = endByte - startByte + 1;
        
        if (actualFileSize != expectedFileSize) {
            throw new IOException("Downloaded file size mismatch. Expected: " + expectedFileSize + ", Actual: " + actualFileSize);
        }
        
        System.out.println("Thread " + threadId + " downloaded " + actualFileSize + " bytes successfully");
    }
    
    // Getters for monitoring progress
    public long getBytesDownloaded() {
        return bytesDownloaded;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public boolean hasError() {
        return hasError;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public int getThreadId() {
        return threadId;
    }
    
    public long getTotalBytes() {
        return endByte - startByte + 1;
    }
    
    public double getProgress() {
        long totalBytes = getTotalBytes();
        return totalBytes > 0 ? (double) bytesDownloaded / totalBytes * 100.0 : 0.0;
    }
    
    public String getPartFileName() {
        return partFileName;
    }
    
    public long getStartByte() {
        return startByte;
    }
    
    public long getEndByte() {
        return endByte;
    }
}
