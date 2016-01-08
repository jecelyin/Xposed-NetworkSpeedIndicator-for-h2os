package tw.fatminmin.xposed.networkspeedindicator;

import java.util.HashSet;
import java.util.Set;

import tw.fatminmin.xposed.networkspeedindicator.logger.Log;
import android.content.SharedPreferences;
import android.graphics.Color;

public final class Common {

	private static final String TAG = Common.class.getSimpleName();
	public static final String PKG_NAME = "tw.fatminmin.xposed.networkspeedindicator";
	public static final String ACTION_SETTINGS_CHANGED = PKG_NAME + ".changed";
	
	public static final String KEY_HIDE_BELOW = "hide_below";
	public static final String KEY_SHOW_SUFFIX = "show_suffix";
	public static final String KEY_UNIT_MODE = "unit_mode";
	public static final String KEY_UNIT_FORMAT = "unit_format";
	public static final String KEY_FORCE_UNIT = "force_unit";
	public static final String KEY_NETWORK_TYPE = "network_type";
	public static final String KEY_NETWORK_SPEED = "network_speed";
	public static final String KEY_FONT_SIZE = "font_size";
	public static final String KEY_POSITION = "position";
	public static final String KEY_SUFFIX = "suffix";
	public static final String KEY_DISPLAY = "display";
	public static final String KEY_SWAP_SPEEDS = "swap_speeds";
	public static final String KEY_UPDATE_INTERVAL = "update_interval";
	public static final String KEY_FONT_COLOR = "font_color";
	public static final String KEY_COLOR = "color";
	public static final String KEY_FONT_STYLE = "font_style";
	public static final String KEY_ENABLE_LOG = "enable_logging_beta";
	
	public static final HashSet<String> DEF_NETWORK_TYPE = new HashSet<String>();
	static {
		DEF_NETWORK_TYPE.add("0");
		DEF_NETWORK_TYPE.add("1");
	}
	
	public static final HashSet<String> DEF_NETWORK_SPEED = new HashSet<String>();
	static {
		DEF_NETWORK_SPEED.add("U");
		DEF_NETWORK_SPEED.add("D");
	}
	
	public static final HashSet<String> DEF_UNIT_FORMAT = new HashSet<String>();
	static {
		DEF_UNIT_FORMAT.add("Sp");
		DEF_UNIT_FORMAT.add("KM");
		DEF_UNIT_FORMAT.add("Bb");
	}
	
	public static final int DEF_HIDE_BELOW = 0;
	public static final boolean DEF_SHOW_SUFFIX = false;
	public static final int DEF_UNIT_MODE = 3; //Decimal bytes
	public static final int DEF_FORCE_UNIT = 0;
	public static final float DEF_FONT_SIZE = 10;
	public static final int DEF_POSITION = 0;
	public static final int DEF_SUFFIX = 1;
	public static final int DEF_DISPLAY = 0;
	public static final boolean DEF_SWAP_SPEEDS = false;
	public static final int DEF_UPDATE_INTERVAL = 1000;
	public static final boolean DEF_FONT_COLOR = false;
	public static final int DEF_COLOR = Color.LTGRAY;
	public static final HashSet<String> DEF_FONT_STYLE = new HashSet<String>();
	public static final boolean DEF_ENABLE_LOG = true;
	
	public static final String BIG_UP_TRIANGLE = " \u25B2 ";
	public static final String BIG_DOWN_TRIANGLE = " \u25BC ";
	public static final String SMALL_UP_TRIANGLE = " \u25B4";
	public static final String SMALL_DOWN_TRIANGLE = " \u25BE ";
	
	public static final String BIG_UP_HOLLOW_TRIANGLE = " \u25B3 ";
	public static final String BIG_DOWN_HOLLOW_TRIANGLE = " \u25BD ";
	public static final String SMALL_UP_HOLLOW_TRIANGLE = " \u25B5";
	public static final String SMALL_DOWN_HOLLOW_TRIANGLE = " \u25BF ";
	
	public static final int ANDROID_SKY_BLUE = Color.parseColor("#33b5e5");

	public static final int getPrefInt(final SharedPreferences pref, final String key, final int def_value) {
		try {
			String value = pref.getString(key, String.valueOf(def_value));
			return Integer.parseInt(value);
		} catch (Exception e) {
			Log.w(TAG, "Key: ", key, ". Def: ", def_value, ". Exception ignored: ", e);
		}
		return def_value;
	}
	
	public static final float getPrefFloat(final SharedPreferences pref, final String key, final float def_value) {
		try {
			String value = pref.getString(key, String.valueOf(def_value));
			return Float.parseFloat(value);
		} catch (Exception e) {
			Log.w(TAG, "Key: ", key, ". Def: ", def_value, ". Exception ignored: ", e);
		}
		return def_value;
	}
	
	public static final String formatUnit(final int prefUnitMode, final int prefUnitFactor, final Set<String> prefUnitFormat) {
		boolean binaryMode = (prefUnitMode == 0 || prefUnitMode == 1);
		boolean bitMode = (prefUnitMode == 0 || prefUnitMode == 2);
		
		String factor = "K";
		if (prefUnitFactor == 3)
			factor = "M";
		else if (prefUnitFactor == 1)
			factor = "";
		
		StringBuilder unit = new StringBuilder();
		
		if (prefUnitFormat.contains("KM")) {
			unit.append(factor);
			if (binaryMode) {
				if (prefUnitFormat.contains("i") && factor.length() > 0) {
					unit.append("i");
				}
			}
		}
		
		if (prefUnitFormat.contains("Bb")) {
			if (bitMode)
				unit.append("b"); //Bits
			else
				unit.append("B"); //Bytes
		}
		
		if (prefUnitFormat.contains("p")) {
			if (bitMode)
				unit.append("p");
			else
				unit.append("/");
		}
		
		if (prefUnitFormat.contains("s")) {
			unit.append("s");
		}
		
		if (prefUnitFormat.contains("Sp")) {
			if (unit.length() > 0) {
				unit.insert(0, " ");
			}
		}
		
		return unit.toString();
	}
	
	public static final void throwException(final Exception e) {
		if ((e != null) && (e instanceof RuntimeException))
			throw (RuntimeException) e;
		else
			throw new RuntimeException(e);
	}

}
