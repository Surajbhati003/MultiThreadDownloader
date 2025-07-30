package com.velance.utils;

/**
 * Simple console progress bar
 */
public class ProgressBar {
    private final long totalBytes;
    private long lastPrintedProgress = -1;
    
    public ProgressBar(long totalBytes) {
        this.totalBytes = totalBytes;
    }
    
    public void update(long downloadedBytes) {
        if (totalBytes <= 0) return;
        
        long progress = (downloadedBytes * 100) / totalBytes;
        
        // Only print if progress has changed significantly
        if (progress != lastPrintedProgress) {
            System.out.print("\rProgress: " + progress + "% [" + FileUtils.formatBytes(downloadedBytes) + "/" + FileUtils.formatBytes(totalBytes) + "]");
            lastPrintedProgress = progress;
        }
    }
    
    public void finish() {
        System.out.println("\nDownload completed!");
    }
}
