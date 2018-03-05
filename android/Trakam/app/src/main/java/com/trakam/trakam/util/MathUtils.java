package com.trakam.trakam.util;

import android.support.annotation.NonNull;

public final class MathUtils {

    private MathUtils() {
        // prevent instantiation
    }

    public static float max(@NonNull float[] values) {
        final int N = values.length;
        float result = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < N; i++) {
            final float val = values[i];
            if (val > result) {
                result = val;
            }
        }
        return result;
    }

    public static float min(@NonNull float[] values) {
        final int N = values.length;
        float result = Float.POSITIVE_INFINITY;
        for (int i = 0; i < N; i++) {
            final float val = values[i];
            if (val < result) {
                result = val;
            }
        }
        return result;
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static double log(int x, int base) {
        return Math.log(x) / Math.log(base);
    }

    public static float clamp(float val, float min, float max) {
        return (val > max) ? max : (val < min ? min : val);
    }

    public static int clamp(int val, int min, int max) {
        return (val > max) ? max : (val < min ? min : val);
    }

    public static float bytesToMegaBytes(long bytes) {
        return bytes / (1024.0f * 1024.0f);
    }

    public static long clamp(long val, long min, long max) {
        return (val > max) ? max : (val < min ? min : val);
    }

    public static double clamp(double val, double min, double max) {
        return (val > max) ? max : (val < min ? min : val);
    }

    public static boolean isPowerOfTwo(long x) {
        return (x != 0) && ((x & (x - 1)) == 0);
    }

    public static float randomFloat() {
        return (float) Math.random();
    }

    public static int compare(int lhs, int rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    public static int compare(long lhs, long rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    public static int compare(float lhs, float rhs) {
        return Float.compare(lhs, rhs);
    }

    public static int compare(double lhs, double rhs) {
        return Double.compare(lhs, rhs);
    }

    public static double celsiusToFahrenheit(double tempInCelsius) {
        return tempInCelsius * 1.8f + 32;
    }

    public static int parseInt(@NonNull String s) {
        return Integer.parseInt(s.trim());
    }

    public static float parseFloat(@NonNull String s) {
        return Float.parseFloat(s.trim());
    }
}
