package alcatel.tess.hometop.gateways.utils;

// Jdk
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

/**
 * This appender extends Log4jAppender with daily rolling file capabilities. When a logfile has
 * to be rolled over, it is moved to the zip dir with the format "logfile-DATE-INDEX.tmp", where
 * DATE is the current date, INDEX is an integer starting from 1. The tmp file is then
 * compressed asynchronously. If the logfile must be rolled over while a previous log
 * compression is still running, then the INDEX log file is incremented by one.
 */
public class DailyLog4jAppender extends Log4jAppender {
    /**
     * Timer used to trigger log rotate at the specified timer.
     */
    private ScheduledFuture<?> _rollingTimer;
    
    /**
     * The Timer above is regularly rescheduled every minutes, in case there is a local time shift.
     */
    private ScheduledFuture<?> _rollingTimerScheduler;

    /**
     * don't store zip files olders than max storage days.
     */
    private int _maxStorageDays;

    /**
     * Don't log if our appender is closed.
     */
    private boolean _closed;

    /**
     * We roll over at a configurable time (every day).
     */
    private RollingCalendar _dailyRotationCalendar;

    /**
     * The time of the day when we force log rotation (every day), in order to avoid compressing to much files at midnight.
     */
    private String _dailyRotationTimePattern = "23:00";

    /**
     * Pattern used to create rotated log files.
     */
    private final SimpleDateFormat _sdf = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * next time we have to force a log rotation (avoid compressing to much files at midnight).
     */
    private long _nextCheck;

    /**
     * Timer used to flush logs if buffering is used.
     */
    private ScheduledFuture<?> _flusherFuture;

    /**
     * Scheduler used to manage various timers.
     */
    private final static ScheduledExecutorService _scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * Sets the max days until compressed files remains.
     * 
     * @param maxStorageDays
     */
    public void setMaxStorageDays(int maxStorageDays) {
        _maxStorageDays = maxStorageDays;
    }

    /**
     * Sets the storage date (format is Hour:Minute). The log file will be rolled over every day
     * at the specified time. But log is always rotated at midnight, if necessary.
     * 
     * @param dailyRotationTimePattern the storage date (format is Hour:Minute)
     */
    public void setRotationTime(String dailyRotationTimePattern) {
        _dailyRotationTimePattern = dailyRotationTimePattern;
    }

    @Override
    public void activateOptions() {
        // create or open the log file
        try {
            openFile(true);
        }

        catch (IOException e) {
            stdout("Can not open file " + _file, e);
        }

        // Create a calendar used to force log rotate at a specified day time (hour:minute).
        _dailyRotationCalendar = new RollingCalendar(_dailyRotationTimePattern);

        // Create a shutdown hook used to close the logfile when the jvm exits.
        _shutdownHook = new ShutdownHook();
        Runtime.getRuntime().addShutdownHook(_shutdownHook);

        // Check if buffering mode is enabled or not
        if (_bufsize > 0) {
            _autoFlush = false;
            _flusherFuture = _scheduler.scheduleWithFixedDelay(new FlusherTask(), 5, 5, TimeUnit.SECONDS);
        }
        else {
            _autoFlush = true;
        }
        
        // Check if some tmp files needs to be gziped. This may happen if the previous jvm execution has crashed.
        checkPendingTmpFiles();
        
        // Check if we missed a previous rotate in past days (in case the jvm was stopped before a previous rotate time).
        long now = System.currentTimeMillis();
        long prevCheck =  _dailyRotationCalendar.getNextCheckMillis(new Date(_file.lastModified()));
        if (now >= prevCheck) {
            rollOver(false, _file.lastModified());
        }
        
        // Compute next rotate check time.
        _nextCheck = _dailyRotationCalendar.getNextCheckMillis(new Date(now));
        
        // Arms the timer that will trigger rolling at the specified timeout time.
        _rollingTimerScheduler = _scheduler.scheduleAtFixedRate(new RollingTimeoutScheduler(), 0, 1, TimeUnit.MINUTES);
    }

    /**
     * Check if some pending tmp files are remaining.
     */
    private void checkPendingTmpFiles() {
        File[] files = getFilesFromZipDir(_file.getName(), ".tmp");
        if (files != null) {
            for (File tmp : files) {
                Runnable task = new CompressTask(tmp);
                task.run(); // do that synchronously.
            }
        }
    }

    @Override
    public void close() {
        close(false);
    }

    public synchronized void close(boolean jvmShuttingDown) {
        if (_closed) {
            return;
        }
        _closed = true;
        try {
            if (!jvmShuttingDown) {
                Runtime.getRuntime().removeShutdownHook(_shutdownHook);
            }
        }
        catch (IllegalStateException e) {
            // the jvm is shutting down ...
        }

        if (_flusherFuture != null) {
            _flusherFuture.cancel(false);
        }
        
        if (_rollingTimerScheduler != null) {
            _rollingTimerScheduler.cancel(false);                         
        }
        
        if (_rollingTimer != null) {
            _rollingTimer.cancel(false);
        }

        closeFile();
    }

    /**
     * Already synchronized on this.
     */
    @Override
    protected void doAppend(String msg) throws Exception {
        // Check if log file has not been closed
        if (_closed) {
            return;
        }

        checkOpened();

        // Before actually logging, check whether it is time to roll over.
        checkRollOver(true);

        // Now, check if max logfile size is reached, and possibly force a log rotate.
        if (_maxSize > 0 && (_size > _maxSize)) {
            debug("logfile size exceeded max size (%d). Forcing log rotate.", _size);
            rollOver(true, System.currentTimeMillis());
        }

        // And log the current message.
        byte[] bmsg = msg.getBytes();
        try {
            _out.write(bmsg);
            _size += bmsg.length;
        }

        catch (IOException e) {
            closeFile();
            openFile(true);
            _out.write(bmsg);
        }
    }

    // ----------------------------- Private methods ---------------------------------------------
    
    // Method called either from doAppend or from RollingTimeout task.
    private void checkRollOver(boolean async) {
        long now = System.currentTimeMillis();
        if (now >= _nextCheck) {
            _nextCheck = _dailyRotationCalendar.getNextCheckMillis(new Date(now));
            rollOver(async, now);
        }
    }

    private void rollOver(boolean async, long rotateTime) {
        try {
            debug("rolling over ...");

            // Move the current log file to the zip dir.
            File tmp = moveLogFileToZipDir(rotateTime);

            // Schedule a compression task 
            CompressTask task = new CompressTask(tmp);
            if (async) {
                _scheduler.execute(task);
            }
            else {
                task.run();
            }
        }

        catch (Throwable t) {
            stdout("could not compress rollover file", t);
        }
    }

    private File moveLogFileToZipDir(long rotateTime) throws IOException {
        if (_zipDir.exists() == false) {
            _zipDir.mkdirs();
        }
        
        String rotateDay = _sdf.format(rotateTime);
        int index = getLastIndexFromZipDir(_file.getName() + "." + rotateDay, ".gz") + 1; // if no tmp file exist, index=0

        StringBuilder tmpFile = new StringBuilder();
        tmpFile.append(_zipDir.toString());
        tmpFile.append(File.separator);
        tmpFile.append(_file.getName());
        tmpFile.append(".");
        tmpFile.append(rotateDay);
        tmpFile.append(".");
        tmpFile.append(Integer.toString(index));
        tmpFile.append(".tmp");
        File out = new File(tmpFile.toString());

        debug("moving log file to zip dir: [%s]", out.getPath());

        // close/flush the current logfile.
        closeFile();

        // Move the current msg.log file into the source file.
        _file.renameTo(out);
        if (out.exists() == false) {
            throw new IOException("Could not rename " + _file + " to " + out);
        }

        // Re-open logfile for next logging event
        openFile(false /* don't append */);
        return out;
    }

    /**
     * Gets the list of zip files starting with a given prefix, and ending with a given suffix.
     * The list is sorted according to the index of the file. (Remember that all files under the
     * zip dir has the format "logfile-DATE-INDEX.tmp" or logfile-DATE-INDEX.tmp).
     * 
     * @param prefix we'll look for files which start with the given prefix
     * @param suffix we'll look for files which end with the given suffix
     * @return the list of files found from the zip dir, which satisfies the supplied
     *         prefix/suffix.
     */
    private File[] getFilesFromZipDir(final String prefix, final String suffix) {
        File[] files = _zipDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (prefix != null && !name.startsWith(prefix)) {
                    return false;
                }

                if (suffix != null && !name.endsWith(suffix)) {
                    return false;
                }
                return true;
            }
        });

        if (files != null) {
            Arrays.sort(files, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    int n1 = getNumber(f1);
                    int n2 = getNumber(f2);
                    return n1 == n2 ? 0 : ((n1 < n2) ? -1 : 1);
                }
            });
        }
        return files;
    }

    /**
     * Returns the number of a zip file or a tmp file (msg.log-yyyy-MM-DD.<NUMBER>.gz, or
     * msg.log-yyyy-MM-DD.<NUMBER>.tmp)
     * 
     * @param f the zip file
     * @return the number of the zip file (NUMBER)
     */
    private int getNumber(File f) {
        int number = 0;
        int dot = 0, dot2 = 0;
        try {
            dot = f.getName().lastIndexOf(".");
            dot2 = f.getName().lastIndexOf(".", dot - 1);
            String s = f.getName().substring(dot2 + 1, dot);
            number = Integer.parseInt(s);
        }
        catch (Throwable e) {
            stdout("Could not get Number part from zip file: " + f.getName() + ", dot=" + dot + ", dot2=" + dot2, e);
        }
        return number;
    }

    /**
     * Returns the highest index of files, given a prefix and a suffix.
     * If no files is found, -1 is returned.
     * 
     * @param prefix
     * @param suffix
     * @return
     */
    private int getLastIndexFromZipDir(String prefix, String suffix) {
        File[] files = getFilesFromZipDir(prefix, suffix);

        int index = -1;
        if (files != null) {
            for (File f : files) {
                index = Math.max(index, getNumber(f));
            }
        }
        return index;
    }

    private class ShutdownHook extends Thread {
        public void run() {
            close(true);
        }
    }

    private class FlusherTask implements Runnable {
        public void run() {
            flush(false /* don't sync */);
        }
    }

    /**
     *  Computes the start of the next interval.  
     */
    class RollingCalendar {
        private final Calendar _calendar = Calendar.getInstance();
        private final int _hour, _minute;

        RollingCalendar(String rotationTimePattern) {
            rotationTimePattern = rotationTimePattern.trim();
            int separator = rotationTimePattern.indexOf(":");
            if (separator == -1) {
                throw new IllegalArgumentException("Invalid for rotationTime paramater: " + rotationTimePattern
                    + " (correct format should be HH:mm)");
            }

            try {
                _hour = Integer.parseInt(rotationTimePattern.substring(0, separator));
                _minute = Integer.parseInt(rotationTimePattern.substring(separator + 1));
            }
            catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid for rotationTime paramater: " + rotationTimePattern
                    + " (correct format should be HH:mm)");
            }
        }

        public long getNextCheckMillis(Date now) {
            return getNextCheckDate(now).getTime();
        }

        public Date getNextCheckDate(Date now) {
            _calendar.setTime(now);
            _calendar.set(Calendar.SECOND, 0);
            _calendar.set(Calendar.MILLISECOND, 0);
            if (_hour < _calendar.get(Calendar.HOUR_OF_DAY)
                || (_hour == _calendar.get(Calendar.HOUR_OF_DAY) && _minute <= _calendar.get(Calendar.MINUTE)))
            {
                _calendar.add(Calendar.DATE, 1);
            }
            _calendar.set(Calendar.HOUR_OF_DAY, _hour);
            _calendar.set(Calendar.MINUTE, _minute);
            return _calendar.getTime();
        }
    }
    
    /**
     * RotationTimeout task, used to fire a log rotate at the daily rolling time, it no activity occurs.
     */
    private class RollingTimeout implements Runnable {
        public void run() {
            synchronized (DailyLog4jAppender.this) {
                debug("Rolling Timeout.");
                checkRollOver(false);
            }
        }        
    }
    
    /**
     * Periodically reschedule the RollingTimeout task (in order to avoid wrong timeout date in case there is a time shift).
     */
    private class RollingTimeoutScheduler implements Runnable {
        public void run() {
            synchronized (DailyLog4jAppender.this) {
                long delta = _nextCheck - System.currentTimeMillis();
                if (_rollingTimer != null) {
                    _rollingTimer.cancel(false);
                }
                if (delta >= 0) {
                    _rollingTimer = _scheduler.schedule(new RollingTimeout(), delta, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    /**
     * Compress a tmp file under the zip directory.
     */
    private class CompressTask implements Runnable {
        final File _tmpFile;

        CompressTask(File tmpFile) {
            _tmpFile = tmpFile;
        }

        public void run() {
            try {
                // Before compressing, clean too old gz files.
                cleanOldFiles();

                // Compress the tmp file.
                InputStream in = null;
                OutputStream out = null;
                final File gzFile = new File(_tmpFile.getPath().replace(".tmp", ".gz"));
                debug("compressing file %s to %s", _tmpFile, gzFile);

                gzFile.delete();
                try {
                    in = new BufferedInputStream(new FileInputStream(_tmpFile));
                    out = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(gzFile)));

                    byte buf[] = new byte[4096];
                    int n = 0;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                    }
                }

                catch (IOException e) {
                    stdout("Could not compress " + _tmpFile + ": " + e.toString());
                    gzFile.delete();
                }

                finally {
                    close(in);
                    close(out);
                    _tmpFile.delete();
                }
            }

            catch (Throwable t) {
                stdout("Could not compress log file", t);
            }
        }

        private void close(InputStream in) {
            try {
                if (in != null) {
                    in.close();
                }
            }
            catch (IOException ignored) {
            }
        }

        private void close(OutputStream out) {
            if (out != null) {
                try {
                    out.close();
                }
                catch (IOException ignored) {
                }
            }
        }

        private void cleanOldFiles() {
            // cleanup files which storage date is older than _maxStorageDays
            if (_maxStorageDays > 0) {
                final long maxStorageDaysMillis = System.currentTimeMillis()
                    - (_maxStorageDays * (60L * 60L * 24L * 1000L));

                File[] files = getFilesFromZipDir(_file.getName(), ".gz");
                if (files != null) {
                    for (File f : files) {
                        if (f.lastModified() < maxStorageDaysMillis) {
                            debug("deleting tool old zip file [%s]", f);
                            f.delete();
                        }
                    }
                }
            }

            // if number of files exceeds maxZipFiles, then remove old files.
            if (_maxZipFiles > 0) {
                File[] files = getFilesFromZipDir(_file.getName(), ".gz");
                // Sort the files by their timestamp
                if (files != null) {
                    Arrays.sort(files, new Comparator<File>() {
                        public int compare(File f1, File f2) {
                            long t1 = f1.lastModified();
                            long t2 = f2.lastModified();
                            return t1 == t2 ? 0 : ((t1 < t2) ? -1 : 1);
                        }
                    });
                }

                int max = _maxZipFiles - 1; // We don't have yet created our new zip file
                if (files != null && files.length > max) {
                    int n = files.length - max;
                    for (int i = 0; n > 0 && i < n; i++) {
                        debug("deleting file [%s] (too many zipped files)", files[i]);
                        files[i].delete();
                    }
                }
            }
        }
    }
}
