package com.trakam.util;


import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.Toast;

public final class Utils {

    public static void makeToast(@NonNull Context context, String text) {
        makeToast(context, text, Toast.LENGTH_SHORT);
    }

    public static void makeToast(@NonNull Context context, String text, int length) {
        final Toast toast = Toast.makeText(context, text, length);
        SafeToast.makeSafe(context, toast);
        toast.show();
    }

}
