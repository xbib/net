package org.xbib.net.mime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Removing files based on this
 * <a href="https://www.oracle.com/technical-resources/articles/javase/finalization.html">article</a>
 */
final class WeakDataFile extends WeakReference<DataFile> {

    private static final Logger LOGGER = Logger.getLogger(WeakDataFile.class.getName());
    private static int TIMEOUT = 10; //milliseconds
    private static ReferenceQueue<DataFile> refQueue = new ReferenceQueue<DataFile>();
    private static Queue<WeakDataFile> refList = new ConcurrentLinkedQueue<>();
    private File file;
    private final RandomAccessFile raf;
    private static boolean hasCleanUpExecutor = false;
    static {
        int delay = 10;
        try {
            delay = Integer.getInteger("org.xbib.net.mime.delay", 10);
        } catch (SecurityException se) {
            if (LOGGER.isLoggable(Level.CONFIG)) {
                LOGGER.log(Level.CONFIG, "Cannot read ''{0}'' property, using defaults.",
                        new Object[] {"org.xbib.net.mime.delay"});
            }
        }
    }

    WeakDataFile(DataFile df, File file) throws IOException {
        super(df, refQueue);
        refList.add(this);
        this.file = file;
        raf = new RandomAccessFile(file, "rw");
        if (!hasCleanUpExecutor) {
            drainRefQueueBounded();
        }
    }

    synchronized void read(long pointer, byte[] buf, int offset, int length ) throws IOException {
        raf.seek(pointer);
        raf.readFully(buf, offset, length);
    }

    synchronized long writeTo(long pointer, byte[] data, int offset, int length) throws IOException {
        raf.seek(pointer);
        raf.write(data, offset, length);
        return raf.getFilePointer();    // Update pointer for next write
    }

    void close() throws IOException {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Deleting file = {0}", file.getName());
        }
        refList.remove(this);
        raf.close();
        boolean deleted = file.delete();
        if (!deleted) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, "File {0} was not deleted", file.getAbsolutePath());
            }
        }
    }

    void renameTo(File f) throws IOException {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Moving file={0} to={1}", new Object[]{file, f});
        }
        refList.remove(this);
        raf.close();
        Path target = Files.move(file.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
        boolean renamed = f.toPath().equals(target);
        if (!renamed) {
            if (LOGGER.isLoggable(Level.INFO)) {
                throw new IOException("File " + file.getAbsolutePath() +
                        " was not moved to " + f.getAbsolutePath());
            }
        }
        file = target.toFile();
    }

    static void drainRefQueueBounded() throws IOException {
        WeakDataFile weak;
        while (( weak = (WeakDataFile) refQueue.poll()) != null ) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Cleaning file = {0} from reference queue.", weak.file);
            }
            weak.close();
        }
    }
}
