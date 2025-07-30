package com.velance.downloader;

import java.util.Scanner;

/**
 * Main entry point for the console version of the downloader
 */
public class Downloader {
    
    public static void main(String[] args) {
        System.out.println("Multithreaded File Downloader");
        System.out.println("=============================");
        
        Scanner scanner = new Scanner(System.in);
        
        try {
            System.out.print("Enter download URL: ");
            String url = scanner.nextLine().trim();
            
            if (url.isEmpty()) {
                System.err.println("URL cannot be empty!");
                return;
            }
            
            System.out.print("Enter number of threads (default 4): ");
            String threadsInput = scanner.nextLine().trim();
            int threads = threadsInput.isEmpty() ? 4 : Integer.parseInt(threadsInput);
            
            System.out.print("Enter download directory (default current): ");
            String directory = scanner.nextLine().trim();
            if (directory.isEmpty()) {
                directory = ".";
            }
            
            DownloadManager manager = new DownloadManager(threads, directory);
            
            System.out.println("\nStarting download...");
            boolean success = manager.downloadFile(url);
            
            if (success) {
                System.out.println("[SUCCESS] Download completed successfully!");
            } else {
                System.out.println("[ERROR] Download failed!");
            }
            
            manager.shutdown();
            
        } catch (NumberFormatException e) {
            System.err.println("Invalid number of threads!");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}
