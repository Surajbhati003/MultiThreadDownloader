# Multithreaded File Downloader

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
