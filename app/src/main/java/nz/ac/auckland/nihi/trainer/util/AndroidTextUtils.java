package nz.ac.auckland.nihi.trainer.util;

import nz.ac.auckland.nihi.trainer.R.string;
import nz.ac.auckland.nihi.trainer.data.Symptom;
import nz.ac.auckland.nihi.trainer.data.SymptomStrength;
import android.content.Context;

/**
 * Contains methods to convert enums to text and such. In a separate class because we don't want the enums themselves to
 * depend on any android-specific classes, so that they may be shared with non-android components.
 * 
 * @author Andrew Meads
 * 
 */
public class AndroidTextUtils {

	private static final int[] suffixIds = new int[] { string.ordinal_th, string.ordinal_st, string.ordinal_nd,
			string.ordinal_rd, string.ordinal_th, string.ordinal_th, string.ordinal_th, string.ordinal_th,
			string.ordinal_th, string.ordinal_th };

	// private static final String[] suffixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th",
	// "th" };

	public static String padLeft(String original, String padding, int len) {
		while (original.length() < len) {
			original = padding + original;
		}
		return padding;
	}

	/**
	 * Converts the given integer to a string representation of that integer plus its suffix, i.e. "1st", "2nd", etc.
	 * 
	 * @param i
	 * @return
	 */
	public static String ordinal(Context context, int i) {
		return i + ordinalOnly(context, i);
	}

	/**
	 * Gets just the suffix for the given integer. For example, ordinalOnly(1) returns "st", as in "1st", while
	 * ordinalOnly(2) returns "nd", as in "2nd".
	 * 
	 * @param i
	 * @return
	 */
	public static String ordinalOnly(Context context, int i) {
		switch (i % 100) {
		case 11:
		case 12:
		case 13:
			return context.getString(string.ordinal_th);
		default:
			return context.getString(suffixIds[i % 10]);

		}
	}

	/**
	 * Gets a string representing the number of hours, minutes, and seconds represented by the given integer number of
	 * seconds. For example, calling this method with a value of 37 will return "00:00:37", while calling it with a
	 * value of 3600 will return "01:00:00".
	 * 
	 * @param relativeTimeInSeconds
	 * @return
	 */
	public static String getRelativeTimeStringSeconds(int relativeTimeInSeconds) {

		int seconds = relativeTimeInSeconds;

		int hours = seconds / 3600;
		seconds -= (hours * 3600);
		int minutes = seconds / 60;
		seconds -= (minutes * 60);

		StringBuilder builder = new StringBuilder();
		if (hours < 10) {
			builder.append('0');
		}
		builder.append(hours);
		builder.append(':');
		if (minutes < 10) {
			builder.append('0');
		}
		builder.append(minutes);
		builder.append(':');
		if (seconds < 10) {
			builder.append('0');
		}
		builder.append(seconds);

		return builder.toString();
	}

	/**
	 * Gets a string representing the number of hours, minutes, seconds, and milliseconds represented by the given
	 * integer number of milliseconds. For example, calling this method with a value of 250 will return "00:00:00.250",
	 * calling with a value of 1000 will return "00:00:01.000", and calling with a value of 3600000 will return
	 * "01:00:00.000".
	 * 
	 * @param relativeTimeInSeconds
	 * @return
	 */
	public static String getRelativeTimeStringMillis(long relativeTimeInMillis) {

		int millis = (int) relativeTimeInMillis % 1000;
		int seconds = (int) (relativeTimeInMillis / 1000) % 60;
		int minutes = (int) ((relativeTimeInMillis / (1000 * 60)) % 60);
		int hours = (int) ((relativeTimeInMillis / (1000 * 60 * 60)) % 24);
		String strMillis = "";
		if (millis < 100)
			strMillis += "0";
		if (millis < 10)
			strMillis += "0";
		strMillis += millis;
		String strSeconds = "" + seconds;
		if (seconds < 10)
			strSeconds = "0" + strSeconds;
		String strMinutes = "" + minutes;
		if (minutes < 10)
			strMinutes = "0" + strMinutes;
		String strHours = "" + hours;
		if (hours < 10)
			strHours = "0" + strHours;
		String strTime = strHours + ":" + strMinutes + ":" + strSeconds + "." + strMillis;

		return strTime;
	}

	/**
	 * Gets the text description of the given symptom strength.
	 * 
	 * @param context
	 * @param strength
	 * @return
	 */
	public static String getSymptomStrengthDescription(Context context, SymptomStrength strength) {
		switch (strength) {
		case Dummy:
			return context.getString(string.symptom_strength_dummy);
		case ExtremelySevere:
			return context.getString(string.symptom_strength_extreme);
		case Light:
			return context.getString(string.symptom_strength_light);
		case Moderate:
			return context.getString(string.symptom_strength_moderate);
		case Severe:
			return context.getString(string.symptom_strength_severe);

		default:
			return null;
		}
	}

	public static String getSymptomName(Context context, Symptom symptom) {
		switch (symptom) {
		case Angina:
			return context.getString(string.symptom_angina);
		case Dyspnoea:
			return context.getString(string.symptom_dyspnoea);
		case Syncope:
			return context.getString(string.symptom_syncope);

		default:
			return null;
		}
	}
}
