package com.odin.android.bioharness.prefs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Date;

import nz.ac.auckland.cs.android.prefs.IPreferences;

import org.bostonandroid.datepreference.DatePreference;

import android.content.Context;
import android.content.SharedPreferences;

import com.odin.android.bioharness.R.string;

public enum BioharnessPreferences implements IPreferences {

	/**
	 * The MAC address and name of the Bioharness device
	 */
	BioharnessDescription(string.pref_bhaddress_key, -1, BioharnessDescription.class);

	private final int keyId, suffixId;
	private final boolean isPrivate;
	private final Class<?> expectedType;

	private BioharnessPreferences(int keyId, int suffixId, Class<?> expectedType) {
		this(keyId, suffixId, false, expectedType);
	}

	private BioharnessPreferences(int keyId, int suffixId, boolean isPrivate, Class<?> expectedType) {
		this.keyId = keyId;
		this.suffixId = suffixId;
		this.isPrivate = isPrivate;
		this.expectedType = expectedType;
	}

	private SharedPreferences getPrefs(Context context) {
		return context.getSharedPreferences(context.getString(string.prefs_filename), Context.MODE_PRIVATE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nz.ac.auckland.nihi.trainer.prefs.IPreferences#isPrivate()
	 */
	@Override
	public boolean isPrivate() {
		return this.isPrivate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nz.ac.auckland.nihi.trainer.prefs.IPreferences#getKey(android.content.Context)
	 */
	@Override
	public String getKey(Context context) {
		return context.getString(keyId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nz.ac.auckland.nihi.trainer.prefs.IPreferences#getSuffix(android.content.Context)
	 */
	@Override
	public String getSuffix(Context context) {
		if (suffixId >= 0) {
			return context.getString(suffixId);
		} else {
			return "";
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nz.ac.auckland.nihi.trainer.prefs.IPreferences#getStringValue(android.content.Context,
	 * android.content.SharedPreferences)
	 */
	@Override
	public String getStringValue(Context context, String defaultValue) {
		String key = getKey(context);
		return getPrefs(context).getString(key, defaultValue);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nz.ac.auckland.nihi.trainer.prefs.IPreferences#getIntValue(android.content.Context,
	 * android.content.SharedPreferences)
	 */
	@Override
	public int getIntValue(Context context, int defaultValue) {
		return Integer.parseInt(getStringValue(context, "" + defaultValue));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nz.ac.auckland.nihi.trainer.prefs.IPreferences#getLongValue(android.content.Context,
	 * android.content.SharedPreferences)
	 */
	@Override
	public long getLongValue(Context context, long defaultValue) {
		return Long.parseLong(getStringValue(context, "" + defaultValue));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nz.ac.auckland.nihi.trainer.prefs.IPreferences#getBoolValue(android.content.Context,
	 * android.content.SharedPreferences)
	 */
	@Override
	public boolean getBoolValue(Context context, boolean defaultValue) {
		return Boolean.parseBoolean(getStringValue(context, "" + defaultValue));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nz.ac.auckland.nihi.trainer.prefs.IPreferences#getDateValue(android.content.Context,
	 * android.content.SharedPreferences)
	 */
	@Override
	public Date getDateValue(Context context, Date defaultValue) {
		String strVal = getStringValue(context, null);
		if (strVal == null) {
			return defaultValue;
		} else {
			try {
				return DatePreference.formatter().parse(strVal);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return new Date(0);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nz.ac.auckland.nihi.trainer.prefs.IPreferences#getEnumValue(android.content.Context,
	 * android.content.SharedPreferences, java.lang.Class)
	 */
	@Override
	public <T extends Enum<T>> T getEnumValue(Context context, Class<T> enumType, T defaultValue) {
		String strValue = getStringValue(context, null);
		if (strValue == null) {
			return defaultValue;
		}
		return (T) Enum.valueOf(enumType, strValue);
	}

	/**
	 * Tries to get the value of the property as an object of the given type. First looks for a static method defined in
	 * the given class called "fromString", taking one String as a parameter. If no such mehtod, then looks for a
	 * constructor taking a single String parameter.
	 * 
	 * @param context
	 * @param clazz
	 * @return
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T getValue(Context context, Class<T> clazz, T defaultValue) {

		String strValue = getStringValue(context, null);
		if (strValue == null) {
			return defaultValue;
		}

		try {
			Method mthd = clazz.getMethod("fromString", String.class);
			return (T) mthd.invoke(null, strValue);
		} catch (NoSuchMethodException e) {

			try {
				Constructor<T> ctor = clazz.getConstructor(String.class);
				return ctor.newInstance(strValue);
			} catch (Exception e1) {
				throw new RuntimeException(e1);
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nz.ac.auckland.nihi.trainer.prefs.IPreferences#setValue(android.content.Context,
	 * android.content.SharedPreferences, java.lang.String)
	 */
	@Override
	public void setValue(Context context, String value) {
		String key = getKey(context);
		getPrefs(context).edit().putString(key, value).apply();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nz.ac.auckland.nihi.trainer.prefs.IPreferences#setValue(android.content.Context,
	 * android.content.SharedPreferences, int)
	 */
	@Override
	public void setValue(Context context, int value) {
		String key = getKey(context);
		getPrefs(context).edit().putString(key, Integer.toString(value)).apply();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nz.ac.auckland.nihi.trainer.prefs.IPreferences#setValue(android.content.Context,
	 * android.content.SharedPreferences, long)
	 */
	@Override
	public void setValue(Context context, long value) {
		String key = getKey(context);
		getPrefs(context).edit().putString(key, Long.toString(value)).apply();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nz.ac.auckland.nihi.trainer.prefs.IPreferences#setValue(android.content.Context,
	 * android.content.SharedPreferences, boolean)
	 */
	@Override
	public void setValue(Context context, boolean value) {
		String key = getKey(context);
		getPrefs(context).edit().putString(key, Boolean.toString(value)).apply();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nz.ac.auckland.nihi.trainer.prefs.IPreferences#setValue(android.content.Context,
	 * android.content.SharedPreferences, java.util.Date)
	 */
	@Override
	public void setValue(Context context, Date value) {
		String strVal = DatePreference.formatter().format(value);
		setValue(context, strVal);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nz.ac.auckland.nihi.trainer.prefs.IPreferences#setValue(android.content.Context,
	 * android.content.SharedPreferences, java.lang.Object)
	 */
	@Override
	public void setValue(Context context, Object value) {
		String key = getKey(context);

		try {
			Method mthd = value.getClass().getMethod("writeToString");
			String strValue = (String) mthd.invoke(value);
			getPrefs(context).edit().putString(key, strValue).apply();
		} catch (Exception ex) {
			getPrefs(context).edit().putString(key, value.toString()).apply();
		}
	}

	@Override
	public void setValue(Context context, Enum<?> value) {
		setValue(context, value.toString());
	}

	@Override
	public Class<?> getExpectedType() {
		return this.expectedType;
	}

	@Override
	public void clearValue(Context context) {
		String key = getKey(context);
		getPrefs(context).edit().remove(key).apply();
	}

	@Override
	public boolean hasValue(Context context) {
		return getPrefs(context).contains(getKey(context));
	}

	private static final String TEST_HARNESS_PROPERTY_PREFIX = "bioharness.";

	public static final boolean setProperty(Context context, String key, String value) {
		if (key.startsWith(TEST_HARNESS_PROPERTY_PREFIX)) {
			for (BioharnessPreferences pref : values()) {
				if (pref.toString().equals(key.substring(TEST_HARNESS_PROPERTY_PREFIX.length()))) {
					pref.setValue(context, value);
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}
}
