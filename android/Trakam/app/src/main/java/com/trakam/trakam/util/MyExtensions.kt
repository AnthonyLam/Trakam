package com.trakam.trakam.util

import android.app.Fragment
import android.app.FragmentManager
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Build
import android.preference.PreferenceManager
import android.support.annotation.AttrRes
import android.support.annotation.IdRes
import android.support.annotation.LayoutRes
import android.util.TypedValue
import android.view.*
import android.widget.Toast

//// BEGIN Context

private val typefaceCache = mutableMapOf<String, Typeface>()

// Bug find/workaround credit: https://github.com/drakeet/ToastCompat#why
fun Context.showToast(msg: CharSequence, length: Int = Toast.LENGTH_SHORT) {
    val toast = Toast.makeText(this, msg, length)
    if (Build.VERSION.SDK_INT <= 25) {
        try {
            val field = View::class.java.getDeclaredField("mContext")
            field.isAccessible = true
            field.set(toast.view, ToastViewContextWrapper(this))
        } catch (e: Exception) {
        }
    }
    toast.show()
}

private class ToastViewContextWrapper(base: Context) : ContextWrapper(base) {
    override fun getApplicationContext(): Context =
            ToastViewApplicationContextWrapper(baseContext.applicationContext)
}

private class ToastViewApplicationContextWrapper(base: Context) : ContextWrapper(base) {
    override fun getSystemService(name: String?): Any {
        return if (name == Context.WINDOW_SERVICE) {
            ToastWindowManager(baseContext.getSystemService(name) as WindowManager)
        } else {
            super.getSystemService(name)
        }
    }
}

private class ToastWindowManager(val base: WindowManager) : WindowManager {
    override fun getDefaultDisplay(): Display = base.defaultDisplay

    override fun addView(view: View?, params: ViewGroup.LayoutParams?) {
        try {
            base.addView(view, params)
        } catch (e: WindowManager.BadTokenException) {
            MyLogger.logError("Toast", "caught BadTokenException crash")
        }
    }

    override fun updateViewLayout(view: View?, params: ViewGroup.LayoutParams?) =
            base.updateViewLayout(view, params)

    override fun removeView(view: View?) = base.removeView(view)

    override fun removeViewImmediate(view: View?) = base.removeViewImmediate(view)
}

fun Context.getTypeface(name: String): Typeface? {
    val assetPath = "fonts/$name.ttf"
    var typeface = typefaceCache[assetPath]
    if (typeface == null) {
        typeface = Typeface.createFromAsset(assets, assetPath)
        typefaceCache[assetPath] = typeface
    }
    return typeface
}

fun Context.dipToPix(value: Float): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)
}

fun Context.getDefaultSharedPreferences(): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(this)

fun Context.getAttrColor(@AttrRes attr: Int): Int {
    val typedValue = TypedValue()
    return if (theme.resolveAttribute(attr, typedValue, true)) {
        typedValue.data
    } else {
        throw IllegalStateException("Color attr not found")
    }
}

//// END Context


//// BEGIN Fragment

fun Fragment.inflateLayout(@LayoutRes layoutResId: Int): View = LayoutInflater.from(activity)
        .inflate(layoutResId, null, false)

//// END Fragment


//// BEGIN FragmentManager

fun FragmentManager.replace(@IdRes container: Int, fragment: Fragment, tag: String?) {
    beginTransaction().replace(container, fragment, tag).commit()
}

fun FragmentManager.add(@IdRes container: Int, fragment: Fragment, tag: String?) {
    beginTransaction().add(container, fragment, tag).commit()
}

fun FragmentManager.findFragmentByTag(tag: String?, block: (Fragment) -> Unit) {
    val fragment = findFragmentByTag(tag)
    if (fragment != null) {
        block(fragment)
    }
}

//// End FragmentManager

