package com.vidyo.vidyoconnector;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.ionic.starter.BuildConfig;

public class Logger {

    private static final String LOGS_FOLDER = "VidyoConnectorLogs";
    private static final String LOG_FILE = "VidyoConnectorLog.log";

    public enum LogType {
        ERROR, INFO, WARNING, DEBUG
    }

    private static final boolean ENABLED = BuildConfig.DEBUG;

    private static final String TAG = "VidyoConnector";

    public static void e(String error) {
        log(getCallerClassName(), error, LogType.ERROR);
    }

    public static void e(String error, Object... format) {
        log(getCallerClassName(), String.format(error, format), LogType.ERROR);
    }

    public static void i(String info) {
        log(getCallerClassName(), info, LogType.INFO);
    }

    public static void i(String info, Object... format) {
        log(getCallerClassName(), String.format(info, format), LogType.INFO);
    }

    public static void d(String debug) {
        log(getCallerClassName(), debug, LogType.DEBUG);
    }

    public static void d(String debug, Object... format) {
        log(getCallerClassName(), String.format(debug, format), LogType.DEBUG);
    }

    public static void w(String warning) {
        log(getCallerClassName(), warning, LogType.WARNING);
    }

    public static void w(String warning, Object... format) {
        log(getCallerClassName(), String.format(warning, format), LogType.WARNING);
    }

    private static void log(String cls, String message, LogType logType) {
        StringBuilder builder = new StringBuilder();
        if (cls != null) {
            builder.append(cls);
            builder.append(": ");
        }

        if (message != null) {
            builder.append(message);
        }

        String data = builder.toString();

        if (ENABLED) {
            switch (logType) {
                case DEBUG:
                    Log.d(TAG, data);
                    break;
                case ERROR:
                    Log.e(TAG, data);
                    break;
                case WARNING:
                    Log.w(TAG, data);
                    break;
                case INFO:
                    Log.i(TAG, data);
                    break;
            }
        }
    }

    private static String getCallerClassName() {
        try {
            StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
            for (int i = 1; i < stElements.length; i++) {
                StackTraceElement ste = stElements[i];
                if (!ste.getClassName().equals(Logger.class.getName()) && ste.getClassName().indexOf("java.lang.Thread") != 0) {
                    return parseClassName(ste) + ": " + ste.getMethodName();
                }
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    private static String parseClassName(StackTraceElement stackTraceElement) {
        String className = stackTraceElement.getClassName();
        int dotIndex = className.lastIndexOf(".");
        return dotIndex > 0 ? className.substring(dotIndex) : className;
    }

    /**
     * Log file is create individually for every session
     *
     * @param context {@link Context}
     * @return log file path
     */
    public static String configLogFile(Context context) {
        File cacheDir = context.getCacheDir();
        File logDir = new File(cacheDir, LOGS_FOLDER);
        deleteRecursive(logDir);

        File logFile = new File(logDir, LOG_FILE);
        logFile.mkdirs();

        String[] logFiles = logDir.list();
        if (logFiles != null)
            for (String file : logFiles) Logger.i("Cached log file: " + file);

        return logFile.getAbsolutePath();
    }

    private static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }
}