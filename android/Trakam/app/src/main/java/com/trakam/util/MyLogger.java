package com.trakam.util;


import android.util.Log;

public final class MyLogger {

    private static final String TAG = "SSM";

    private static final int MAX_MSG_LENGTH = 4000;

    private MyLogger() {
        // prevent instantiation
    }

    public static void logInfo(Class<?> clazz, String message) {
        logInfo(clazz.getSimpleName(), message);
    }

    public static void logDebug(Class<?> clazz, String message) {
        logDebug(clazz.getSimpleName(), message);
    }

    public static void logWarning(Class<?> clazz, String message) {
        logWarning(clazz.getSimpleName(), message);
    }

    public static void logError(Class<?> clazz, String message) {
        logError(clazz.getSimpleName(), message);
    }

    public static void logWTF(Class<?> clazz, String message) {
        logWTF(clazz.getSimpleName(), message);
    }

    public static void logVerbose(Class<?> clazz, String message) {
        logVerbose(clazz.getSimpleName(), message);
    }

    public static void logInfo(String tag, String message) {
        log(LogType.INFO, tag, message);
    }

    public static void logDebug(String tag, String message) {
        log(LogType.DEBUG, tag, message);
    }

    public static void logWarning(String tag, String message) {
        log(LogType.WARNING, tag, message);
    }

    public static void logError(String tag, String message) {
        log(LogType.ERROR, tag, message);
    }

    public static void logWTF(String tag, String message) {
        log(LogType.WTF, tag, message);
    }

    public static void logVerbose(String tag, String message) {
        log(LogType.VERBOSE, tag, message);
    }

    private static void log(int type, String tag, String message) {
        message = String.format("[%s] %s", tag, message);
        if (message.length() > MAX_MSG_LENGTH) {
            message = message.substring(0, MAX_MSG_LENGTH);
        }
        switch (type) {
            case LogType.INFO: {
                Log.i(TAG, message);
            }
            break;
            case LogType.DEBUG: {
                Log.d(TAG, message);
            }
            break;
            case LogType.WARNING: {
                Log.w(TAG, message);
            }
            break;
            case LogType.ERROR: {
                Log.e(TAG, message);
            }
            break;
            case LogType.VERBOSE: {
                Log.v(TAG, message);
            }
            break;
            case LogType.WTF: {
                Log.wtf(TAG, message);
            }
            break;
        }
    }

    private interface LogType {

        int INFO = 0;
        int DEBUG = 1;
        int WARNING = 2;
        int ERROR = 3;
        int VERBOSE = 4;
        int WTF = 5;
    }
}
