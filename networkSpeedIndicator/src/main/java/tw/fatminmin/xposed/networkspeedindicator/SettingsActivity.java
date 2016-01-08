package tw.fatminmin.xposed.networkspeedindicator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;

import net.margaritov.preference.colorpicker.ColorPickerPreference;
import tw.fatminmin.xposed.networkspeedindicator.logger.Log;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;

import com.h6ah4i.android.compat.preference.MultiSelectListPreferenceCompat;

public final class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	private static final String TAG = SettingsActivity.class.getSimpleName();
	private SharedPreferences mPrefs;
	private final Set<String> networkTypeEntries = new LinkedHashSet<String>();
	private final Set<String> networkTypeValues = new LinkedHashSet<String>();
	private int prefUnitMode;
	private int prefForceUnit;

	@SuppressWarnings("deprecation")
	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		try {
			super.onCreate(savedInstanceState);
			
			getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
			mPrefs = getPreferenceManager().getSharedPreferences();
			
			addPreferencesFromResource(R.xml.settings);
			
			refreshNetworkTypes();
			refreshPreferences(mPrefs, null);
			
		} catch (Exception e) {
			Log.e(TAG, "onCreate failed: ", e);
			Common.throwException(e);
		}
	}

	@Override
	protected final void onResume() {
		try {
			super.onResume();
			
			@SuppressWarnings("deprecation")
			PreferenceGroup settings = (PreferenceGroup) findPreference("settings");
			setAllSummary(settings);
			
			mPrefs.registerOnSharedPreferenceChangeListener(this);
			
		} catch (Exception e) {
			Log.e(TAG, "onResume failed: ", e);
			Common.throwException(e);
		}
	}

	@Override
	protected final void onPause() {
		try {
			mPrefs.unregisterOnSharedPreferenceChangeListener(this);
			super.onPause();
		} catch (Exception e) {
			Log.e(TAG, "onPause failed: ", e);
			Common.throwException(e);
		}
	}

	private final void setAllSummary(final PreferenceGroup group) {
		for(int i = 0; i < group.getPreferenceCount(); i++) {
			if(group.getPreference(i) instanceof PreferenceGroup) {
				setAllSummary((PreferenceGroup) group.getPreference(i));
			}
			else {
				setSummary(group.getPreference(i));
			}
		}
	}

	private final void refreshNetworkTypes() {
		// Get the network types supported by device
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo[] allNetInfo = cm.getAllNetworkInfo();
		
		networkTypeEntries.clear();
		networkTypeValues.clear();
		
		if (allNetInfo == null) {
			Log.e(TAG, "Array containing all network info is null!");
		}
		else {
			List<String> resNetworkTypeEntries  = Arrays.asList(getResources().getStringArray(R.array.networktype_entries));
			List<String> resNetworkTypeValues   = Arrays.asList(getResources().getStringArray(R.array.networktype_values));
			
			for (NetworkInfo netInfo : allNetInfo) {
				if (netInfo == null) {
					Log.w(TAG, "Network info object is null.");
				} else {
					String netInfoType = String.valueOf(netInfo.getType());
					int index = resNetworkTypeValues.indexOf(netInfoType);
					if (index >= 0 && index < resNetworkTypeEntries.size()) {
						networkTypeEntries.add(resNetworkTypeEntries.get(index));
						networkTypeValues .add(resNetworkTypeValues .get(index));
					}
				}
			}
		}
	}
	
    private final void setSummary(final Preference preference) {
	    if (preference instanceof ListPreference) {
            ListPreference listPref = (ListPreference) preference;
            preference.setSummary(createListPrefSummary(listPref));
        }
        else if(preference instanceof EditTextPreference) {
            EditTextPreference editPref = (EditTextPreference) preference;
            preference.setSummary(createEditTextSummary(editPref));
        }
        else if(preference instanceof MultiSelectListPreferenceCompat) {
        	MultiSelectListPreferenceCompat mulPref = (MultiSelectListPreferenceCompat) preference;
        	preference.setSummary(createMultiSelectSummary(mulPref));
        }
        else if(preference instanceof ColorPickerPreference) {
        	ColorPickerPreference colorPicker = (ColorPickerPreference) preference;
        	preference.setSummary(createColorPickerSummary(colorPicker));
        }
	}
    
    private final String createListPrefSummary(final ListPreference listPref) {
    	String summaryText = listPref.getEntry().toString();
    	
    	if (Common.KEY_UNIT_MODE.equals(listPref.getKey())) {
    		try {
    			int listValue = Integer.parseInt(listPref.getValue());
    			String[] summaryTexts = getResources().getStringArray(R.array.unit_mode_summary);
    			summaryText += String.format(" (%s)", summaryTexts[listValue]);
    		} catch (Exception e) {
    			//reset
    			summaryText = listPref.getEntry().toString();
    		}
    	}
    	
    	return summaryText;
    }
    
    private final String createEditTextSummary(final EditTextPreference editPref) {
    	String summaryText = editPref.getText();
    	
    	if (Common.KEY_UPDATE_INTERVAL.equals(editPref.getKey())) {
    		try {
    			summaryText = String.valueOf(Integer.parseInt(summaryText));
    		} catch (Exception e) {
    			summaryText = String.valueOf(Common.DEF_UPDATE_INTERVAL);
    		}
    		summaryText = formatWithUnit(summaryText, getString(R.string.unit_update_interval));
    	}
    	else if (Common.KEY_HIDE_BELOW.equals(editPref.getKey())) {
    		int value;
    		try {
    			value = Integer.parseInt(summaryText);
    		} catch (Exception e) {
    			value = Common.DEF_HIDE_BELOW;
    		}
    		if (value <= 0) {
    			summaryText = getString(R.string.sum0_hide_below);
    		} else if (value == 1) {
    			summaryText = getString(R.string.sum1_hide_below);
    		} else {
    			summaryText = formatWithUnit(String.valueOf(value), getString(R.string.unit_hide_below));
    		}
    	}
    	else if (Common.KEY_FONT_SIZE.equals(editPref.getKey())) {
    		try {
    			summaryText = String.valueOf(Float.parseFloat(summaryText));
    		} catch (Exception e) {
    			summaryText = String.valueOf(Common.DEF_FONT_SIZE);
    		}
    		summaryText = formatWithUnit(summaryText, getString(R.string.unit_font_size));
    	}
    	
    	return summaryText;
    }
    
    private static final String formatWithUnit(final String value, final String unit) {
		if (unit.contains("%s")) {
			return String.format(unit, value);
		} else {
			return value + " " + unit;
		}
	}
    
    private final String createMultiSelectSummary(final MultiSelectListPreferenceCompat mulPref) {
    	Set<String> valueSet = mulPref.getValues();
    	
    	if (Common.KEY_UNIT_FORMAT.equals(mulPref.getKey())) {
    		String formattedUnit = Common.formatUnit(prefUnitMode, prefForceUnit, valueSet);
    		
    		if (formattedUnit.length() == 0) {
    			return getString(R.string.unit_format_hidden);
    		} else {
    			return getString(R.string.unit_format_fmt_as) + " #" + formattedUnit;
    		}
    	}
    	
    	if (valueSet.size() == 0) {
    		return getString(R.string.summary_none);
    	}
    	
    	TreeMap<Integer, String> selections = new TreeMap<Integer, String>();
    	for (String value : valueSet) {
    		int index = mulPref.findIndexOfValue(value);
    		if (index < 0 || index >= mulPref.getEntries().length) {
    			Log.w(TAG, "Found multi select value without entry: ", value);
    		} else {
	    		String entry = (String) mulPref.getEntries()[index];
	    		selections.put(index, entry);
    		}
    	}
    	
    	String summary = "";
    	for (String entry : selections.values()) {
    		if(summary.length() > 0) {
    			summary += ", ";
    		}
    		summary += entry;
    	}
    	return summary;
    }
	
	private String createColorPickerSummary(final ColorPickerPreference colorPicker) {
		
		SharedPreferences prefs = colorPicker.getSharedPreferences();
		String key = colorPicker.getKey();
		
		String summary;
		
		if (prefs.contains(key)) {
			int iColor = prefs.getInt(key, 0);
			summary = ColorPickerPreference.convertToARGB(iColor).toUpperCase(Locale.getDefault());
			summary = formatWithUnit(summary, getString(R.string.unit_font_color));
		} else {
			summary = getString(R.string.summary_none);
		}
		return summary;
	}

    @SuppressWarnings("deprecation")
	@Override
	public final void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
		try {
			Intent intent = new Intent();
			Log.i(TAG, "onSharedPreferenceChanged ", key);
			
			refreshPreferences(prefs, key);
			setSummary(findPreference(key));
			
			intent.setAction(Common.ACTION_SETTINGS_CHANGED);
			
			if (key.equals(Common.KEY_FORCE_UNIT)) {
				intent.putExtra(key, Common.getPrefInt(prefs, key, Common.DEF_FORCE_UNIT));
			}
			else if (key.equals(Common.KEY_UNIT_MODE)) {
				intent.putExtra(key, Common.getPrefInt(prefs, key, Common.DEF_UNIT_MODE));
			}
			else if (key.equals(Common.KEY_HIDE_BELOW)) {
				intent.putExtra(key, Common.getPrefInt(prefs, key, Common.DEF_HIDE_BELOW));
			}
			else if (key.equals(Common.KEY_SHOW_SUFFIX)) {
				intent.putExtra(key, prefs.getBoolean(key, Common.DEF_SHOW_SUFFIX));
			}
			else if (key.equals(Common.KEY_FONT_SIZE)) {
			    intent.putExtra(key, Common.getPrefFloat(prefs, key, Common.DEF_FONT_SIZE));
			}
			else if (key.equals(Common.KEY_POSITION)) {
			    intent.putExtra(key, Common.getPrefInt(prefs, key, Common.DEF_POSITION));
			}
			else if (key.equals(Common.KEY_SUFFIX)) {
			    intent.putExtra(key, Common.getPrefInt(prefs, key, Common.DEF_SUFFIX));
			}
			else if (key.equals(Common.KEY_DISPLAY)) {
			    intent.putExtra(key, Common.getPrefInt(prefs, key, Common.DEF_DISPLAY));
			}
			else if (key.equals(Common.KEY_SWAP_SPEEDS)) {
				intent.putExtra(key, prefs.getBoolean(key, Common.DEF_SWAP_SPEEDS));
			}
			else if (key.equals(Common.KEY_UPDATE_INTERVAL)) {
			    intent.putExtra(key, Common.getPrefInt(prefs, key, Common.DEF_UPDATE_INTERVAL));
			}
			else if (key.equals(Common.KEY_FONT_COLOR)) {
				intent.putExtra(key, prefs.getBoolean(key, Common.DEF_FONT_COLOR));
			}
			else if (key.equals(Common.KEY_COLOR)) {
				intent.putExtra(key, prefs.getInt(key, Common.DEF_COLOR));
			}
			else if (key.equals(Common.KEY_ENABLE_LOG)) {
				intent.putExtra(key, prefs.getBoolean(key, Common.DEF_ENABLE_LOG));
			}
			else if (key.equals(Common.KEY_NETWORK_TYPE)
					|| key.equals(Common.KEY_NETWORK_SPEED)
					|| key.equals(Common.KEY_UNIT_FORMAT)
					|| key.equals(Common.KEY_FONT_STYLE)) {
				MultiSelectListPreferenceCompat mulPref = (MultiSelectListPreferenceCompat) findPreference(key);
				HashSet<String> value = (HashSet<String>) mulPref.getValues(); 
				intent.putExtra(key, value);
			}
			else {
				intent.setAction(null);
			}
			
			if (intent.getAction() != null) {
				sendBroadcast(intent);
			}
		} catch (Exception e) {
			Log.e(TAG, "onSharedPreferenceChanged failed: ", e);
			Common.throwException(e);
		}
	}
	
	@SuppressWarnings("deprecation")
	private final void refreshPreferences(final SharedPreferences prefs, final String key) {
		// When key is null, refresh everything.
		// When a key is provided, refresh only for that key.
		
		if (key==null) { //only first time
			MultiSelectListPreferenceCompat mulPref = (MultiSelectListPreferenceCompat) findPreference(Common.KEY_NETWORK_TYPE);
			mulPref.setEntries(networkTypeEntries.toArray(new String[]{}));
			mulPref.setEntryValues(networkTypeValues.toArray(new String[]{}));
		}
		
		if (Common.KEY_FORCE_UNIT.equals(key)) {
			getUnitSettings(prefs);
			setSummary(findPreference(Common.KEY_UNIT_FORMAT));
		}
		
		if (Common.KEY_UNIT_FORMAT.equals(key)) {
			getUnitSettings(prefs);
		}
    	
		if (key==null
				|| key.equals(Common.KEY_HIDE_BELOW)
				|| key.equals(Common.KEY_SUFFIX)) {
	    	int prefHideBelow = Common.getPrefInt(prefs, Common.KEY_HIDE_BELOW, Common.DEF_HIDE_BELOW);
	    	int prefSuffix = Common.getPrefInt(prefs, Common.KEY_SUFFIX, Common.DEF_SUFFIX);
	    	findPreference(Common.KEY_SHOW_SUFFIX).setEnabled(prefHideBelow > 0 && prefSuffix != 0);
		}
    	
		if (key==null || key.equals(Common.KEY_UNIT_MODE)) {
			// Dynamically change the entry texts of "Unit" preference
			getUnitSettings(prefs);
			int resId;
			
			switch (prefUnitMode) {
			case 0:
				resId = R.array.unit_entries_binary_bits;
				break;
			case 1:
				resId = R.array.unit_entries_binary_bytes;
				break;
			default: case 2:
				resId = R.array.unit_entries_decimal_bits;
				break;
			case 3:
				resId = R.array.unit_entries_decimal_bytes;
				break;
			}
			
			String[] unitEntries = getResources().getStringArray(resId);
			Preference prefUnit = findPreference(Common.KEY_FORCE_UNIT);
			((ListPreference) prefUnit).setEntries(unitEntries);
			
			if (key != null) { // key-specific refresh
				setSummary(prefUnit);
				setSummary(findPreference(Common.KEY_UNIT_FORMAT));
			}
		}
		
		if (key==null
				|| key.equals(Common.KEY_ENABLE_LOG)) {
			Log.enableLogging = prefs.getBoolean(Common.KEY_ENABLE_LOG, Common.DEF_ENABLE_LOG);
		}
	}
	
	private final void getUnitSettings(final SharedPreferences prefs) {
		prefUnitMode = Common.getPrefInt(prefs, Common.KEY_UNIT_MODE, Common.DEF_UNIT_MODE);
		prefForceUnit = Common.getPrefInt(prefs, Common.KEY_FORCE_UNIT, Common.DEF_FORCE_UNIT);
	}
}
