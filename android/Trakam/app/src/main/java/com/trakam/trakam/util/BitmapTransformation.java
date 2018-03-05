package com.trakam.trakam.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import com.squareup.picasso.Transformation;

import java.lang.ref.WeakReference;

public class BitmapTransformation implements Transformation {

    private static final float BLUR_RADIUS = 5.0f;

    private WeakReference<Context> mContextRef;
    private float mBorderWidth;
    private int mBorderColor;
    private boolean mBorderEnabled;
    private boolean mBlurEnabled;
    private float mBlurRadius;
    private boolean mCircularCrop;

    private BitmapTransformation(Context context) {
        mContextRef = new WeakReference<>(context);
        mBorderColor = Color.WHITE;
        mBorderWidth = 5.0f;
        mBorderEnabled = false;
        mBlurEnabled = false;
        mBlurRadius = BLUR_RADIUS;
        mCircularCrop = true;
    }

    private Bitmap blur(Context context, Bitmap source, float radius) {
        final int width = source.getWidth();
        final int height = source.getHeight();
        final RenderScript rs = RenderScript.create(context);
        final Allocation in = Allocation.createFromBitmap(rs, source);
        final Allocation out = Allocation.createTyped(rs, in.getType());
        final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        script.setRadius(radius);

        final Bitmap dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        script.setRadius(radius);
        script.setInput(in);
        script.forEach(out);
        out.copyTo(dest);

        return dest;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        final Context context = mContextRef.get();
        if (mBlurEnabled && context != null) {
            final int width = source.getWidth();
            final int height = source.getHeight();
            final Bitmap scaledDown = Bitmap.createScaledBitmap(source, width / 8, height / 8, true);
            source.recycle();

            final float radius = MathUtils.clamp(mBlurRadius, 0.0f, 25.0f);
            source = blur(context, scaledDown, radius);
            scaledDown.recycle();
        }

        final int width = source.getWidth();
        final int height = source.getHeight();

        final Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(result);

        final Paint paint = new Paint();
        paint.setAntiAlias(true);

        final float cx = width / 2.0f;
        final float cy = height / 2.0f;
        final float radius = Math.max(width, height) / 2.0f;

        final BitmapShader shader = new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(shader);
        if (mCircularCrop) {
            canvas.drawCircle(cx, cy, radius, paint);
        } else {
            canvas.drawRect(0, 0, width, height, paint);
        }

        if (mBorderEnabled) {
            if (mBorderWidth <= 0.0f) {
                throw new IllegalStateException("border width = " + mBorderWidth);
            }

            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(mBorderWidth);
            paint.setColor(mBorderColor);
            canvas.drawCircle(cx, cy, radius - mBorderWidth / 2.0f, paint);
        }

        source.recycle();

        return result;
    }

    @Override
    public String key() {
        return String.valueOf(mBorderColor) + String.valueOf(mBorderWidth) + String.valueOf(mBorderEnabled) +
                String.valueOf(mBlurEnabled) + String.valueOf(mBlurRadius) + String.valueOf(mCircularCrop);
    }

    public static class Builder {

        private BitmapTransformation mTransformation;

        public Builder(Context context) {
            mTransformation = new BitmapTransformation(context);
        }

        public Builder setBorderEnabled(boolean enabled) {
            mTransformation.mBorderEnabled = enabled;
            return this;
        }

        public Builder setBorderWidth(float borderWidth) {
            mTransformation.mBorderWidth = borderWidth;
            return this;
        }

        public Builder setBorderColor(int color) {
            mTransformation.mBorderColor = color;
            return this;
        }

        public Builder blur() {
            mTransformation.mBlurEnabled = true;
            return this;
        }

        public Builder blurRadius(float blurRadius) {
            mTransformation.mBlurRadius = blurRadius;
            return this;
        }

        public Builder circularCrop(boolean enabled) {
            mTransformation.mCircularCrop = enabled;
            return this;
        }

        public BitmapTransformation build() {
            return mTransformation;
        }
    }
}
