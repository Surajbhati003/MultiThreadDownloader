# Write the README.md file with comprehensive documentation
readme_content = """# Multithreaded File Downloader

A high-performance, multithreaded file downloader implemented in Java that can download large files by splitting them into chunks and downloading them concurrently using multiple threads.

## 🚀 Features

### Core Features
- **Multithreaded Downloads**: Downloads files using multiple threads for maximum speed
- **Single-threaded Fallback**: Automatically falls back to single-threaded download if server doesn't support ranges
- **Resume Support**: Can resume interrupted downloads (bonus feature)
- **Progress Tracking**: Real-time progress bar with speed and ETA calculations
- **File Integrity**: MD5 checksum validation for downloaded files
- **Smart Chunk Merging**: Efficiently merges downloaded chunks into final file
- **Error Handling**: Comprehensive error handling with retry mechanisms

### Advanced Features
- **Thread Pool Management**: Uses ExecutorService for optimal thread management
- **Download Manager**: Advanced download management with resource limiting
- **GUI Interface**: Optional Swing-based graphical user interface
- **Console Interface**: Command-line interface for headless usage
- **File Utilities**: Comprehensive file handling utilities
- **Progress Visualization**: Multiple progress bar implementations

## 📁 Project Structure

```
MultithreadedDownloader/
├── src/main/java/com/velance/
│   ├── downloader/
│   │   ├── Downloader.java          # Main downloader class
│   │   ├── DownloadThread.java      # Individual download thread
│   │   ├── FileMerger.java          # File merging utility
│   │   └── DownloadManager.java     # Advanced download manager
│   ├── utils/
│   │   ├── ProgressBar.java         # Progress visualization
│   │   └── FileUtils.java           # File utility functions
│   └── gui/
│       └── DownloadGUI.java         # Swing GUI interface
├── README.md
└── pom.xml                          # Maven configuration
```

## 🛠️ Requirements

- Java 8 or higher
- Maven 3.6+ (for building)
- Internet connection for downloads
- Minimum 512MB RAM (recommended: 1GB+)

## 🏗️ Building the Project

### Using Maven

```bash
# Clone or extract the project
cd MultithreadedDownloader

# Compile the project
mvn clean compile

# Package into JAR
mvn clean package

# Run with Maven
mvn exec:java -Dexec.mainClass="com.velance.downloader.Downloader"
```

### Using Command Line

```bash
# Navigate to source directory
cd src/main/java

# Compile all Java files
javac -d ../../../target/classes com/velance/*/*.java

# Run the main class
java -cp ../../../target/classes com.velance.downloader.Downloader
```

## 🚀 Usage

### Command Line Interface

```bash
# Basic usage
java -cp target/classes com.velance.downloader.Downloader

# The program will prompt for:
# - Download URL
# - Download directory (optional, defaults to current directory)
# - Number of threads (optional, defaults to 4)
```

### GUI Interface

```bash
# Launch the GUI
java -cp target/classes com.velance.gui.DownloadGUI
```

### Programmatic Usage

```java
import com.velance.downloader.DownloadManager;

// Create download manager with 8 threads
DownloadManager manager = new DownloadManager(8, "/path/to/downloads");

// Start download
boolean success = manager.downloadFile("https://example.com/largefile.zip");

if (success) {
    System.out.println("Download completed successfully!");
} else {
    System.out.println("Download failed!");
}

// Clean up
manager.shutdown();
```

## 📋 4-Day Development Plan

### Day 1: Core Setup & Basic Downloader ✅
- [x] Project structure setup
- [x] Basic single-threaded downloader
- [x] HTTP connection handling
- [x] File I/O operations
- [x] Basic error handling

**Deliverables:**
- `Downloader.java` - Main downloader class
- Basic console interface
- Single-threaded download functionality

### Day 2: Add Multithreading ✅
- [x] HTTP Range header support
- [x] File chunking logic
- [x] Thread creation and management
- [x] Concurrent download implementation
- [x] Thread synchronization

**Deliverables:**
- `DownloadThread.java` - Individual download threads
- Multithreaded download capability
- Progress tracking across threads

### Day 3: Merge + Optimize + Handle Errors ✅
- [x] File merging implementation
- [x] Error handling and retry logic
- [x] Thread pool management
- [x] Resource optimization
- [x] File integrity verification

**Deliverables:**
- `FileMerger.java` - File merging utility
- `DownloadManager.java` - Advanced thread pool management
- Comprehensive error handling
- Resume functionality (bonus)

### Day 4: UI + Polish + Test ✅
- [x] Swing GUI implementation
- [x] Progress visualization
- [x] File utilities
- [x] Documentation
- [x] Testing with large files

**Deliverables:**
- `DownloadGUI.java` - Complete GUI interface
- `ProgressBar.java` - Progress visualization
- `FileUtils.java` - Utility functions
- Complete documentation

## 🔧 Configuration

### Thread Configuration
- **Minimum threads**: 1
- **Maximum threads**: 16 (configurable)
- **Default threads**: 4
- **Recommended**: 4-8 threads for most use cases

### Performance Tuning
```java
// In DownloadManager constructor
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    maxConnections,                    // Core pool size
    maxConnections,                    // Maximum pool size
    60L,                              // Keep alive time
    TimeUnit.SECONDS,                 // Time unit
    new LinkedBlockingQueue<>(),      // Work queue
    new DownloadThreadFactory(),      // Thread factory
    new ThreadPoolExecutor.CallerRunsPolicy() // Rejection policy
);
```

### Buffer Sizes
- **Download buffer**: 32KB (32,768 bytes)
- **File I/O buffer**: 32KB for optimal performance
- **Progress update interval**: 250ms

## 🧪 Testing

### Test with Sample URLs

```bash
# Small file (for quick testing)
https://httpbin.org/bytes/1048576  # 1MB test file

# Medium file
https://releases.ubuntu.com/20.04/ubuntu-20.04.6-desktop-amd64.iso  # ~4GB

# Large file (if available)
# Any large file URL that supports HTTP Range requests
```

### Performance Testing
1. Test with different thread counts (1, 2, 4, 8, 16)
2. Monitor CPU and memory usage
3. Test with various file sizes
4. Test network interruption recovery

## 🛡️ Error Handling

### Automatic Retry
- **Max retries**: 3 per thread
- **Retry delay**: 1 second (exponential backoff)
- **Timeout handling**: 30 second read timeout

### Supported Error Scenarios
- Network interruptions
- Server timeouts
- Partial file downloads
- Disk space issues
- Permission errors
- Invalid URLs

## 📊 Performance Metrics

### Typical Performance Gains
- **Single thread**: Baseline performance
- **4 threads**: 2-4x faster (depending on server and network)
- **8 threads**: 3-6x faster (optimal for most scenarios)
- **16 threads**: May hit server or network limits

### Memory Usage
- **Base memory**: ~50MB
- **Per thread**: ~2-4MB additional
- **File buffers**: ~32KB per thread
- **Progress tracking**: Minimal overhead

## 🔍 Troubleshooting

### Common Issues

**1. Server doesn't support range requests**
- Solution: Application automatically falls back to single-threaded download

**2. Download stuck at 99%**
- Cause: Network interruption during final merge
- Solution: Check logs, restart download if necessary

**3. "Permission denied" errors**
- Cause: Insufficient write permissions
- Solution: Choose different download directory or run with appropriate permissions

**4. OutOfMemoryError**
- Cause: Too many threads or very large files
- Solution: Reduce thread count or increase JVM heap size with `-Xmx2g`

### Debug Mode
Enable verbose logging by setting system property:
```bash
java -Ddownloader.debug=true -cp target/classes com.velance.downloader.Downloader
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Java HttpURLConnection documentation
- ExecutorService and ThreadPoolExecutor best practices
- HTTP Range request specifications (RFC 7233)
- Swing GUI development patterns

## 📚 Additional Resources

### Learning Materials
- [Java Concurrency in Practice](https://jcip.net/)
- [HTTP Range Requests - MDN](https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests)
- [Java ExecutorService Tutorial](https://docs.oracle.com/javase/tutorial/essential/concurrency/executors.html)

### Useful Commands

```bash
# Monitor download progress
tail -f download.log

# Check server range support
curl -I "https://example.com/file.zip"

# Test with specific range
curl -H "Range: bytes=0-1023" "https://example.com/file.zip"
```

---

**Built with ❤️ by Velance for learning multithreaded programming in Java**

*This project demonstrates core concepts of concurrent programming, network I/O, file handling, and GUI development in Java.*"""

# Write the README.md file
with open("MultithreadedDownloader/README.md", "w", encoding="utf-8") as f:
    f.write(readme_content)

print("README.md created successfully!")