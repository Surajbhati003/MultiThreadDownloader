# Multi-Thread Downloader

A robust Java application that demonstrates parallel file downloading using multiple threads. This project implements both single-threaded and multi-threaded approaches to file downloading, allowing for performance comparison.

## Features

- **Parallel Downloads**: Downloads file chunks simultaneously using multiple threads
- **Performance Comparison**: Includes both single-threaded and multi-threaded implementations
- **Robust Error Handling**: Implements retry mechanism for failed chunk downloads
- **Progress Tracking**: Shows download progress for each chunk
- **Automatic File Assembly**: Merges downloaded chunks into the final file
- **Configurable**: Easily adjust thread count and chunk sizes

## Technical Details

### Components

1. **MultiDownloader.java** (Main Class)
   - Orchestrates the download process
   - Manages thread pool for parallel downloads
   - Implements both single and multi-threaded download approaches
   - Handles file size calculation and chunk distribution
   - Default configuration: 8 threads

2. **MultiDownload.java** (Download Worker)
   - Implements `Callable<Boolean>` for thread execution
   - Handles individual chunk downloads
   - Features robust error handling with retry mechanism
   - Configurable timeout and retry parameters
   - Uses HTTP Range headers for chunk downloads

3. **Merger.java**
   - Handles the reassembly of downloaded chunks
   - Creates the final output file
   - Manages cleanup of temporary files

### Key Features

- **Error Handling**
  - Maximum retry attempts: 3
  - Retry delay: 2000ms
  - Connection timeout: 15000ms
  - Read timeout: 15000ms

- **Performance Optimization**
  - Buffer size: 4096 bytes
  - Configurable thread count
  - Automatic chunk size calculation

## Usage

1. Compile the Java files:
   ```bash
   javac MultiDownloader.java MultiDownload.java Merger.java
   ```

2. Run the program:
   ```bash
   java MultiDownloader
   ```

3. The program will:
   - First perform a single-threaded download
   - Then perform a multi-threaded download
   - Show performance comparison between both approaches
   - Save the downloaded file in the specified output directory

## Output Structure

- `single_thread_download.mp4`: Result of single-threaded download
- `multi_thread_download.mp4`: Result of multi-threaded download
- `temp_download_[timestamp]/`: Temporary directory for chunk storage
  - `part0` to `part[N-1]`: Individual downloaded chunks

## Performance Comparison

The multi-threaded approach typically shows significant performance improvements over the single-threaded approach, especially for:
- Large files
- Servers that support parallel connections
- Networks with good bandwidth

## Error Handling

The program includes robust error handling for:
- Network connectivity issues
- Server response errors
- Corrupt downloads
- Timeout scenarios
- Failed chunk downloads with retry mechanism

## System Requirements

- Java Runtime Environment (JRE) 8 or higher
- Internet connection
- Sufficient disk space for temporary files and final output

## Notes

- The program uses HTTP Range requests, ensure the server supports this feature
- Temporary files are automatically cleaned up after successful merging
- Progress is displayed in real-time during download

## Overview
This project is a multithreaded file downloader written in Java. It allows downloading large files efficiently by dividing the file into multiple chunks, downloading them in parallel, and then merging the chunks into a single file.

The project consists of three main components:
1. **Merger.java**: Handles merging of downloaded chunks into the final file.
2. **MultiDownload.java**: Represents a runnable task for downloading a specific chunk of the file.
3. **MultiDownloader.java**: The entry point of the program, orchestrating the download process using multiple threads.

---

## Files and Their Functions

### **1. Merger.java**
This class handles the merging of individual file chunks into the final output file. It also ensures cleanup of temporary chunk files after merging.

#### **Key Method: `mergeChunksIntoFile(String directory, int totalChunks, String outputFile)`**
- **Parameters**:
  - `directory`: Path to the directory containing the chunk files.
  - `totalChunks`: Total number of chunks to merge.
  - `outputFile`: Name of the final merged file.
- **Process**:
  - Opens the output file for writing.
  - Iterates over all chunk files (e.g., `part0`, `part1`, etc.).
  - Reads each chunk and writes its contents to the output file.
  - Deletes the chunk file after writing to save space.
- **Output**: Produces the final merged file and prints a success message.

---

### **2. MultiDownload.java**
This class defines a task for downloading a specific range of bytes (a chunk) from the file.

#### **Constructor:**
```java
public MultiDownload(URL resourceUrl, long rangeStart, long rangeEnd, int chunkIndex, String downloadDirectory)
