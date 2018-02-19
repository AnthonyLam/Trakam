package com.trakam.util;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import java.lang.reflect.Field;

// Bug find/workaround credit: https://github.com/drakeet/ToastCompat#why
public class SafeToast {

    public static void show(@NonNull Context context, CharSequence text, int duration) {
        final Toast toast = Toast.makeText(context, text, duration);
        makeSafe(context, toast);
        toast.show();
    }

    public static void show(@NonNull Context context, @StringRes int resId, int duration) {
        final Toast toast = Toast.makeText(context, resId, duration);
        makeSafe(context, toast);
        toast.show();
    }

    static void makeSafe(@NonNull Context context, @NonNull Toast toast) {
        if (Build.VERSION.SDK_INT <= 25) {
            try {
                final Field field = View.class.getDeclaredField("mContext");
                field.setAccessible(true);
                field.set(toast.getView(), new ToastViewContextWrapper(context));
            } catch (Exception ignored) {
            }
        }
    }

    private static class ToastViewContextWrapper extends ContextWrapper {

        public ToastViewContextWrapper(Context base) {
            super(base);
        }

        @Override
        public Context getApplicationContext() {
            return new ToastViewApplicationContextWrapper(getBaseContext().getApplicationContext());
        }
    }

    private static class ToastViewApplicationContextWrapper extends ContextWrapper {

        public ToastViewApplicationContextWrapper(Context base) {
            super(base);
        }

        @Override
        public Object getSystemService(String name) {
            if (Context.WINDOW_SERVICE.equals(name)) {
                final WindowManager wm = (WindowManager) getBaseContext().getSystemService(name);
                return new ToastWindowManager(wm);
            } else {
                return super.getSystemService(name);
            }
        }
    }

    private static class ToastWindowManager implements WindowManager {

        private WindowManager mBase;

        ToastWindowManager(WindowManager base) {
            mBase = base;
        }

        @Override
        public Display getDefaultDisplay() {
            return mBase.getDefaultDisplay();
        }

        @Override
        public void removeViewImmediate(View view) {
            mBase.removeViewImmediate(view);
        }

        @Override
        public void addView(View view, ViewGroup.LayoutParams params) {
            try {
                mBase.addView(view, params);
            } catch (BadTokenException e) {
                MyLogger.logDebug(SafeToast.class, "Caught BadTokenException crash");
            }
        }

        @Override
        public void updateViewLayout(View view, ViewGroup.LayoutParams params) {
            mBase.updateViewLayout(view, params);
        }

        @Override
        public void removeView(View view) {
            mBase.removeView(view);
        }
    }

}