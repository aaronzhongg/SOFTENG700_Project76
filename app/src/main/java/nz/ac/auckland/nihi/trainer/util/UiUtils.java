package nz.ac.auckland.nihi.trainer.util;

import java.lang.ref.SoftReference;
import java.util.Hashtable;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Contains utility methods to deal with fonts, pixel counts on-screen, etc.
 * 
 * @author Andrew Meads
 * 
 */
public class UiUtils {

	public static final String TAG = "UiUtil";

	/**
	 * Converts the given screen dimension in dp to the equivalent dimension in pixels.
	 * 
	 * @param dp
	 * @param context
	 * @return
	 */
	public static int toPx(int dp, Context context) {
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, dm);
		return (int) Math.round(px);
	}

	public static void setCustomFont(View textViewOrButton, Context ctx, AttributeSet attrs, int[] attributeSet,
			int fontId) {
		setCustomFont(textViewOrButton, ctx, attrs, attributeSet, 0, 0, fontId);
	}

	public static void setCustomFont(View textViewOrButton, Context ctx, AttributeSet attrs, int[] attributeSet,
			int defStyleAttr, int defStyleRes, int fontId) {
		TypedArray a = ctx.obtainStyledAttributes(attrs, attributeSet, defStyleAttr, defStyleRes);
		String customFont = a.getString(fontId);
		setCustomFont(textViewOrButton, ctx, customFont);
		a.recycle();
	}

	private static boolean setCustomFont(View textViewOrButton, Context ctx, String asset) {
		if (TextUtils.isEmpty(asset))
			return false;
		Typeface tf = null;
		try {
			tf = getFont(ctx, asset);
			if (textViewOrButton instanceof TextView) {
				((TextView) textViewOrButton).setTypeface(tf);
			} else {
				((Button) textViewOrButton).setTypeface(tf);
			}
		} catch (Exception e) {
			Log.e(TAG, "Could not get typeface: " + asset, e);
			return false;
		}

		return true;
	}

	private static final Hashtable<String, SoftReference<Typeface>> fontCache = new Hashtable<String, SoftReference<Typeface>>();

	public static Typeface getFont(Context c, String name) {
		synchronized (fontCache) {
			if (fontCache.get(name) != null) {
				SoftReference<Typeface> ref = fontCache.get(name);
				if (ref.get() != null) {
					return ref.get();
				}
			}

			Typeface typeface = Typeface.createFromAsset(c.getAssets(), "fonts/" + name);
			fontCache.put(name, new SoftReference<Typeface>(typeface));

			return typeface;
		}
	}

	/**
	 * Gets the ID of the view with the given name. You may include a package in the viewName; if you don't, the default
	 * one (the application's package) will be used.
	 * 
	 * e.g. android.R.id.pnlHello will be translated to android:id.pnlHello, whereas just pnlHello will be translated to
	 * my.app.package:id.pnlHello.
	 * 
	 * @param viewName
	 * @return
	 */
	public static int getViewID(String viewName, Context context) {
		String resType = "id";
		String identifier, pkg;
		if (viewName.contains(".id.")) {
			int i = viewName.lastIndexOf(".id.");
			pkg = viewName.substring(0, i);
			identifier = viewName.substring(i + 4);
		} else {
			identifier = viewName;
			pkg = context.getApplicationContext().getPackageName();
		}

		return context.getResources().getIdentifier(identifier, resType, pkg);
	}

	/**
	 * Gets the ID of the drawable with the given name. You may include a package in the drawableName; if you don't, the
	 * default one (the application's package) will be used.
	 * 
	 * e.g. android.R.drawable.my_icon will be translated to android:drawable.my_icon, whereas just my_icon will be
	 * translated to my.app.package:drawable.my_icon.
	 * 
	 * @param drawableName
	 * @return
	 */
	public static int getDrawableID(String drawableName, Context context) {
		String resType = "drawable";
		String identifier, pkg;
		if (drawableName.contains(".drawable.")) {
			int i = drawableName.lastIndexOf(".drawable.");
			pkg = drawableName.substring(0, i);
			identifier = drawableName.substring(i + 10);
		} else {
			identifier = drawableName;
			pkg = context.getApplicationContext().getPackageName();
		}

		return context.getResources().getIdentifier(identifier, resType, pkg);
	}

}