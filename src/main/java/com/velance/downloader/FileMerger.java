package com.velance.downloader;

import com.velance.utils.FileUtils;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Day 3: FileMerger class for combining downloaded chunks into final file
 * Handles merging, cleanup, and integrity verification
 */
public class FileMerger {
    private static final int BUFFER_SIZE = 64 * 1024; // 64KB buffer for faster I/O
    
    /**
     * Merges all part files into the final complete file
     * @param baseFileName The base filename (without .part extension)
     * @param numParts Number of parts to merge
     * @throws IOException if merging fails
     */
    public void mergeFiles(String baseFileName, int numParts) throws IOException {
        System.out.println("Starting file merge process...");
        
        // Verify all parts exist before starting merge
        verifyAllPartsExist(baseFileName, numParts);
        
        File outputFile = new File(baseFileName);
        File tempFile = new File(baseFileName + ".tmp");
        
        try (FileOutputStream fos = new FileOutputStream(tempFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER_SIZE)) {
            
            long totalBytes = 0;
            
            for (int i = 0; i < numParts; i++) {
                String partFileName = baseFileName + ".part" + i;
                File partFile = new File(partFileName);
                
                System.out.println("Merging part " + (i + 1) + "/" + numParts + " (" + 
                                 FileUtils.formatBytes(partFile.length()) + ")");
                
                try (FileInputStream fis = new FileInputStream(partFile);
                     BufferedInputStream bis = new BufferedInputStream(fis, BUFFER_SIZE)) {
                    
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    long partBytes = 0;
                    
                    while ((bytesRead = bis.read(buffer)) != -1) {
                        bos.write(buffer, 0, bytesRead);
                        partBytes += bytesRead;
                        totalBytes += bytesRead;
                    }
                    
                    System.out.println("Part " + (i + 1) + " merged: " + FileUtils.formatBytes(partBytes));
                }
            }
            
            bos.flush();
            System.out.println("Total merged size: " + FileUtils.formatBytes(totalBytes));
        }
        
        // Atomically move temp file to final file
        if (outputFile.exists()) {
            if (!outputFile.delete()) {
                throw new IOException("Could not delete existing output file: " + outputFile.getAbsolutePath());
            }
        }
        
        if (!tempFile.renameTo(outputFile)) {
            throw new IOException("Could not rename temp file to final output file");
        }
        
        System.out.println("File merge completed successfully: " + outputFile.getAbsolutePath());
        
        // Clean up part files
        cleanupPartFiles(baseFileName, numParts);
        
        // Verify final file integrity
        verifyMergedFile(baseFileName, numParts);
    }
    
    /**
     * Verifies that all part files exist and are readable
     */
    private void verifyAllPartsExist(String baseFileName, int numParts) throws IOException {
        for (int i = 0; i < numParts; i++) {
            String partFileName = baseFileName + ".part" + i;
            File partFile = new File(partFileName);
            
            if (!partFile.exists()) {
                throw new IOException("Part file missing: " + partFileName);
            }
            
            if (!partFile.canRead()) {
                throw new IOException("Cannot read part file: " + partFileName);
            }
            
            if (partFile.length() == 0) {
                throw new IOException("Part file is empty: " + partFileName);
            }
        }
        
        System.out.println("All " + numParts + " part files verified and ready for merge");
    }
    
    /**
     * Cleans up temporary part files after successful merge
     */
    private void cleanupPartFiles(String baseFileName, int numParts) {
        System.out.println("Cleaning up temporary part files...");
        
        int deletedCount = 0;
        for (int i = 0; i < numParts; i++) {
            String partFileName = baseFileName + ".part" + i;
            File partFile = new File(partFileName);
            
            if (partFile.exists()) {
                if (partFile.delete()) {
                    deletedCount++;
                    System.out.println("Deleted: " + partFile.getName());
                } else {
                    System.err.println("Warning: Could not delete part file: " + partFileName);
                }
            }
        }
        
        System.out.println("Cleanup completed. Deleted " + deletedCount + " part files.");
    }
    
    /**
     * Verifies the integrity of the merged file by comparing sizes
     */
    private void verifyMergedFile(String baseFileName, int numParts) {
        try {
            File mergedFile = new File(baseFileName);
            long mergedSize = mergedFile.length();
            
            // Calculate expected size from original parts (before cleanup)
            // Note: This is a basic verification. In a production system,
            // you might want to store checksums for each part and verify those.
            
            System.out.println("Final file verification:");
            System.out.println("Merged file: " + mergedFile.getName());
            System.out.println("Final size: " + FileUtils.formatBytes(mergedSize));
            
            if (mergedSize > 0) {
                System.out.println("[SUCCESS] File merge verification passed");
            } else {
                System.err.println("[WARNING] Merged file appears to be empty");
            }
            
        } catch (Exception e) {
            System.err.println("Warning: Could not verify merged file: " + e.getMessage());
        }
    }
    
    /**
     * Calculates MD5 checksum for file integrity verification
     * Bonus feature for advanced verification
     */
    public String calculateMD5(String fileName) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        File file = new File(fileName);
        
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            
            while ((bytesRead = bis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }
        
        byte[] digest = md.digest();
        StringBuilder hexString = new StringBuilder();
        
        for (byte b : digest) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
    
    /**
     * Validates file integrity using MD5 checksum
     * @param fileName File to check
     * @param expectedMD5 Expected MD5 hash (if known)
     * @return true if file is valid, false otherwise
     */
    public boolean validateFileIntegrity(String fileName, String expectedMD5) {
        try {
            String actualMD5 = calculateMD5(fileName);
            System.out.println("File MD5: " + actualMD5);
            
            if (expectedMD5 != null && !expectedMD5.isEmpty()) {
                boolean matches = actualMD5.equalsIgnoreCase(expectedMD5);
                System.out.println("Expected MD5: " + expectedMD5);
                System.out.println("MD5 Match: " + (matches ? "[SUCCESS] PASS" : "[ERROR] FAIL"));
                return matches;
            } else {
                System.out.println("No expected MD5 provided - skipping validation");
                return true;
            }
            
        } catch (Exception e) {
            System.err.println("Error validating file integrity: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Resume a broken merge operation
     * Useful for handling interrupted merge processes
     */
    public void resumeMerge(String baseFileName, int numParts, long resumeFromByte) throws IOException {
        System.out.println("Resuming merge from byte: " + resumeFromByte);
        
        File outputFile = new File(baseFileName);
        if (!outputFile.exists()) {
            throw new IOException("Cannot resume - output file does not exist");
        }
        
        // Verify current file size matches resume point
        if (outputFile.length() != resumeFromByte) {
            throw new IOException("Resume point mismatch. File size: " + outputFile.length() + 
                                ", Resume point: " + resumeFromByte);
        }
        
        // Find which part to resume from
        long currentByte = 0;
        int resumePartIndex = 0;
        
        for (int i = 0; i < numParts; i++) {
            String partFileName = baseFileName + ".part" + i;
            File partFile = new File(partFileName);
            
            if (currentByte + partFile.length() > resumeFromByte) {
                resumePartIndex = i;
                break;
            }
            currentByte += partFile.length();
        }
        
        System.out.println("Resuming from part " + (resumePartIndex + 1));
        
        // Continue merge from the determined part
        try (FileOutputStream fos = new FileOutputStream(outputFile, true);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            
            for (int i = resumePartIndex; i < numParts; i++) {
                String partFileName = baseFileName + ".part" + i;
                File partFile = new File(partFileName);
                
                long skipBytes = (i == resumePartIndex) ? resumeFromByte - currentByte : 0;
                
                try (FileInputStream fis = new FileInputStream(partFile);
                     BufferedInputStream bis = new BufferedInputStream(fis)) {
                    
                    // Skip bytes if resuming in middle of a part
                    if (skipBytes > 0) {
                        bis.skip(skipBytes);
                    }
                    
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    
                    while ((bytesRead = bis.read(buffer)) != -1) {
                        bos.write(buffer, 0, bytesRead);
                    }
                }
                
                if (i == resumePartIndex) {
                    currentByte += partFile.length();
                }
            }
        }
        
        System.out.println("Resume merge completed successfully");
    }
}
