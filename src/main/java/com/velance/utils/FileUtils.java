package com.velance.utils;

import java.text.DecimalFormat;

/**
 * Utility class for file operations
 */
public class FileUtils {
    
    /**
     * Formats bytes into human readable format
     * @param bytes Number of bytes
     * @return Formatted string (e.g., "1.5 MB", "256 KB")
     */
    public static String formatBytes(long bytes) {
        if (bytes == 0) return "0 B";
        
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(size) + " " + units[unitIndex];
    }
}
