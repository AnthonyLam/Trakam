package com.trakam.activities;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.trakam.R;

public class CapturePreviewActivity extends Activity {

    public static final String TAG = CapturePreviewActivity.class.getCanonicalName();
    public static final String BITMAP_EXTRA = TAG + "_bitmap_extra";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.capture_preview_activity);

        final ImageView imageView = findViewById(R.id.imageView);
        final Bitmap bitmap = getIntent().getParcelableExtra(BITMAP_EXTRA);
        imageView.setImageBitmap(bitmap);
    }
}
