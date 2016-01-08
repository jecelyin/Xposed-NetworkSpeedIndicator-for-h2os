package tw.fatminmin.xposed.networkspeedindicator.widget;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.Set;

import tw.fatminmin.xposed.networkspeedindicator.Common;
import tw.fatminmin.xposed.networkspeedindicator.logger.Log;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import de.robv.android.xposed.XSharedPreferences;

@SuppressLint("HandlerLeak")
public final class TrafficView extends TextView {

    public PositionCallback mPositionCallback = null;
    public View clock = null;
    
	private static final String TAG = TrafficView.class.getSimpleName();
	private static final DecimalFormat formatWithDecimal    = new DecimalFormat(" ##0.0");
	private static final DecimalFormat formatWithoutDecimal = new DecimalFormat(" ##0");
	
	private boolean mAttached;

	private long uploadSpeed;
	private long downloadSpeed;
	private long totalTxBytes;
	private long totalRxBytes;
	private long lastUpdateTime;
	private boolean justLaunched = true;
	private boolean loggedZero = false;
	private String networkType;
	private boolean networkState;

	private int prefPosition = Common.DEF_POSITION;
	private int prefForceUnit = Common.DEF_FORCE_UNIT;
	private int prefUnitMode = Common.DEF_UNIT_MODE;
	private float prefFontSize = Common.DEF_FONT_SIZE;
	private int prefSuffix = Common.DEF_SUFFIX;
	private int prefDisplay = Common.DEF_DISPLAY;
	private boolean prefSwapSpeeds = Common.DEF_SWAP_SPEEDS;
	private int prefUpdateInterval = Common.DEF_UPDATE_INTERVAL;
	private boolean prefFontColor = Common.DEF_FONT_COLOR;
	private int prefColor = Common.DEF_COLOR;
	private int prefHideBelow = Common.DEF_HIDE_BELOW;
	private boolean prefShowSuffix = Common.DEF_SHOW_SUFFIX;
	private Set<String> prefUnitFormat = Common.DEF_UNIT_FORMAT;
	private Set<String> prefNetworkType = Common.DEF_NETWORK_TYPE;
	private Set<String> prefNetworkSpeed = Common.DEF_NETWORK_SPEED;
	private Set<String> prefFontStyle = Common.DEF_FONT_STYLE;

	public TrafficView(final Context context) {
		super(context, null, 0);
		loadPreferences();
		updateConnectionInfo();
		updateViewVisibility();
		mAttached = false;
	}
	
	public final void refreshPosition() {
	    switch(prefPosition) {
        case 0:
            mPositionCallback.setLeft();
            break;
        case 1:
            mPositionCallback.setRight();
            break;
        case 2:
            mPositionCallback.setAbsoluteLeft();
            break;
        }
	}
	
	@SuppressLint("NewApi")
	private final void refreshColor() {
		if (prefFontColor) {
			setTextColor(prefColor);
		}
		else {
			if(clock != null) {
				if (clock instanceof TextView) {
					setTextColor(((TextView) clock).getCurrentTextColor());
				} else {
					//probably LinearLayout in VN ROM v14.1 (need to search child elements to find correct text color)
					Log.w(TAG, "clock is not a TextView, it is ", clock.getClass().getSimpleName());
					setTextColor(Common.ANDROID_SKY_BLUE);
				}
			}
			else {
				Log.i(TAG, "Gingerbread");
				setTextColor(Common.ANDROID_SKY_BLUE);
			}
		}
	}
	
	private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {

		@SuppressWarnings("unchecked")
		@Override
		public final void onReceive(final Context context, final Intent intent) {
			try {
				String action = intent.getAction();
				
				if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
					Log.i(TAG, "Connectivity changed");
					updateConnectionInfo();
					updateViewVisibility();
				}
				else if (action.equals(Common.ACTION_SETTINGS_CHANGED)) {
					Log.i(TAG, "Settings changed");
					Log.d(TAG, intent.getExtras().keySet().toArray());
					
					if (intent.hasExtra(Common.KEY_FORCE_UNIT)) {
						prefForceUnit = intent.getIntExtra(Common.KEY_FORCE_UNIT, Common.DEF_FORCE_UNIT);
					}
					if (intent.hasExtra(Common.KEY_UNIT_MODE)) {
						prefUnitMode = intent.getIntExtra(Common.KEY_UNIT_MODE, Common.DEF_UNIT_MODE);
					}
					if (intent.hasExtra(Common.KEY_UNIT_FORMAT)) {
					    prefUnitFormat = (Set<String>) intent.getSerializableExtra(Common.KEY_UNIT_FORMAT);
					}
					if (intent.hasExtra(Common.KEY_HIDE_BELOW)) {
						prefHideBelow = intent.getIntExtra(Common.KEY_HIDE_BELOW, Common.DEF_HIDE_BELOW);
					}
					if (intent.hasExtra(Common.KEY_SHOW_SUFFIX)) {
					    prefShowSuffix = intent.getBooleanExtra(Common.KEY_SHOW_SUFFIX, Common.DEF_SHOW_SUFFIX);
					}
					if (intent.hasExtra(Common.KEY_FONT_SIZE)) {
					    prefFontSize = intent.getFloatExtra(Common.KEY_FONT_SIZE, Common.DEF_FONT_SIZE);
					}
					if (intent.hasExtra(Common.KEY_POSITION)) {
					    prefPosition = intent.getIntExtra(Common.KEY_POSITION, Common.DEF_POSITION);
					    refreshPosition();
					}
					if (intent.hasExtra(Common.KEY_SUFFIX)) {
					    prefSuffix = intent.getIntExtra(Common.KEY_SUFFIX, Common.DEF_SUFFIX);
					}
					if (intent.hasExtra(Common.KEY_NETWORK_TYPE)) {
					    prefNetworkType = (Set<String>) intent.getSerializableExtra(Common.KEY_NETWORK_TYPE);
					}
					if (intent.hasExtra(Common.KEY_NETWORK_SPEED)) {
					    prefNetworkSpeed = (Set<String>) intent.getSerializableExtra(Common.KEY_NETWORK_SPEED);
					}
					if (intent.hasExtra(Common.KEY_DISPLAY)) {
				        prefDisplay = intent.getIntExtra(Common.KEY_DISPLAY, Common.DEF_DISPLAY);
				    }
					if (intent.hasExtra(Common.KEY_SWAP_SPEEDS)) {
						prefSwapSpeeds = intent.getBooleanExtra(Common.KEY_SWAP_SPEEDS, Common.DEF_SWAP_SPEEDS);
					}
					if (intent.hasExtra(Common.KEY_UPDATE_INTERVAL)) {
				        prefUpdateInterval = intent.getIntExtra(Common.KEY_UPDATE_INTERVAL, Common.DEF_UPDATE_INTERVAL);
				    }
					if (intent.hasExtra(Common.KEY_FONT_COLOR)) {
						prefFontColor = intent.getBooleanExtra(Common.KEY_FONT_COLOR, Common.DEF_FONT_COLOR);
					}
					if (intent.hasExtra(Common.KEY_COLOR)) {
						prefColor = intent.getIntExtra(Common.KEY_COLOR, Common.DEF_COLOR);
					}
					if (intent.hasExtra(Common.KEY_FONT_STYLE)) {
						prefFontStyle = (Set<String>) intent.getSerializableExtra(Common.KEY_FONT_STYLE);
					}
					if (intent.hasExtra(Common.KEY_ENABLE_LOG)) {
						Log.enableLogging = intent.getBooleanExtra(Common.KEY_ENABLE_LOG, Common.DEF_ENABLE_LOG);
					}
					
					updateViewVisibility();
				}
			} catch (Exception e) {
				Log.e(TAG, "onReceive failed: ", e);
				Common.throwException(e);
			}
		}
	};

	@SuppressLint("HandlerLeak")
    private final Handler mTrafficHandler = new Handler() {
		@Override
		public final void handleMessage(final Message msg) {
			try {
				// changing values must be fetched together and only once
				long lastUpdateTimeNew = SystemClock.elapsedRealtime();
				long totalTxBytesNew = getTotalBytes(TRANSMIT);
				long totalRxBytesNew = getTotalBytes(RECEIVE);
				
				long elapsedTime = lastUpdateTimeNew - lastUpdateTime;
				
				if (elapsedTime == 0) {
					Log.w(TAG, "Elapsed time is zero");
					uploadSpeed = 0;
					downloadSpeed = 0;
				} else {
					uploadSpeed = ((totalTxBytesNew - totalTxBytes) * 1000) / elapsedTime;
					downloadSpeed = ((totalRxBytesNew - totalRxBytes) * 1000) / elapsedTime;
				}

				if (loggedZero==false || uploadSpeed != 0 || downloadSpeed != 0) {
					Log.d(TAG,
						totalTxBytes, ",", totalTxBytesNew, ";",
						totalRxBytes, ",", totalRxBytesNew, ";",
						lastUpdateTime, ",", lastUpdateTimeNew, ";",
						uploadSpeed, ",", downloadSpeed
						);
					loggedZero = (uploadSpeed == 0 && downloadSpeed == 0);
				}
				
				totalTxBytes = totalTxBytesNew;
				totalRxBytes = totalRxBytesNew;
				lastUpdateTime = lastUpdateTimeNew;
				
				SpannableString spanString = new SpannableString(createText());
				
				if(prefFontStyle.contains("B")) {
					spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
				}
				if(prefFontStyle.contains("I")) {
					spanString.setSpan(new StyleSpan(Typeface.ITALIC), 0, spanString.length(), 0);
				}
				
				setText(spanString);
				setTextSize(TypedValue.COMPLEX_UNIT_SP, prefFontSize);
				refreshColor();
				
				update();
				
				super.handleMessage(msg);
			} catch (Exception e) {
				Log.e(TAG, "handleMessage failed: ", e);
				Common.throwException(e);
			}
		}
	};

	@Override
	protected final void onAttachedToWindow() {
		try {
			super.onAttachedToWindow();

			if (!mAttached) {
				mAttached = true;
				IntentFilter filter = new IntentFilter();
				filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
				filter.addAction(Common.ACTION_SETTINGS_CHANGED);
				getContext().registerReceiver(mIntentReceiver, filter, null, getHandler());
			}
			updateViewVisibility();
		} catch (Exception e) {
			Log.e(TAG, "onAttachedToWindow failed: ", e);
			Common.throwException(e);
		}
	}

	@Override
	protected final void onDetachedFromWindow() {
		try {
			super.onDetachedFromWindow();
			if (mAttached) {
				getContext().unregisterReceiver(mIntentReceiver);
				mAttached = false;
			}
		} catch (Exception e) {
			Log.e(TAG, "onDetachedFromWindow failed: ", e);
			Common.throwException(e);
		}
	}

	private final void updateConnectionInfo() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(
				Context.CONNECTIVITY_SERVICE);

		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		Log.d(TAG, networkInfo);
		
		if (networkInfo != null) {
			networkState = networkInfo.isAvailable();
			networkType = String.valueOf(networkInfo.getType());
			Log.i(TAG, "Network type: ", networkType);
		}
		else {
			networkState = false;
		}
		Log.d(TAG, "Network State: ", networkState);
	}

	private final void updateTraffic() {
		if (justLaunched) {
			//get the values for the first time
			lastUpdateTime = SystemClock.elapsedRealtime();
			totalTxBytes = getTotalBytes(TRANSMIT);
			totalRxBytes = getTotalBytes(RECEIVE);
			
			//don't get the values again
			justLaunched = false;
		}
		
		mTrafficHandler.sendEmptyMessage(0);
	}
	
	private static final int TRANSMIT = 0;
	private static final int RECEIVE = 1;
	
	private static final long getTotalBytes(final int traffic_direction) {
		final boolean tx = (traffic_direction == TRANSMIT);
		long totalBytes = -9; // not -1 because it conflicts with TrafficStats.UNSUPPORTED
		BufferedReader br = null;
		
		try {
			br = new BufferedReader(new FileReader("/sys/class/net/lo/statistics/" + (tx ? "tx" : "rx") + "_bytes"));
			
			// reading both together to reduce delay in between as much as possible
			totalBytes = tx ? TrafficStats.getTotalTxBytes() : TrafficStats.getTotalRxBytes();
			String line = br.readLine();
			
			long loBytes = Long.parseLong(line);
			
			Log.d(TAG, traffic_direction, " total: ", totalBytes, ", lo: ", loBytes);
			
			totalBytes = totalBytes - loBytes;
			
		} catch (Exception e) {
			Log.i(TAG, "Loopback exclusion failed: ", e);
			
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
		
		if (totalBytes == -9) {
			totalBytes = tx ? TrafficStats.getTotalTxBytes() : TrafficStats.getTotalRxBytes();
		}
		
		return totalBytes;
	}

	private final String createText() {
		String uploadSuffix, downloadSuffix;
		String strUploadValue, strDownloadValue;
		
		switch(prefSuffix) {
		default:
		case 0:
		    uploadSuffix = downloadSuffix = " ";
		    break;
		case 1:
			uploadSuffix = Common.BIG_UP_TRIANGLE;
			downloadSuffix = Common.BIG_DOWN_TRIANGLE;
		    break;
		case 2:
			uploadSuffix = Common.BIG_UP_HOLLOW_TRIANGLE;
			downloadSuffix = Common.BIG_DOWN_HOLLOW_TRIANGLE;
		    break;
		case 3:
			uploadSuffix = Common.SMALL_UP_TRIANGLE;
			downloadSuffix = Common.SMALL_DOWN_TRIANGLE;
		    break;
		case 4:
			uploadSuffix = Common.SMALL_UP_HOLLOW_TRIANGLE;
			downloadSuffix = Common.SMALL_DOWN_HOLLOW_TRIANGLE;
		    break;
		}
		
		boolean showUploadSpeed = prefNetworkSpeed.contains("U");
		boolean showDownloadSpeed = prefNetworkSpeed.contains("D");
		boolean showInExactPosition = (showUploadSpeed && showDownloadSpeed);
		
		if(showUploadSpeed) {
		    strUploadValue = formatSpeed(uploadSpeed, uploadSuffix);
		}
		else {
			strUploadValue = "";
		}
		
		if(showDownloadSpeed) {
		    strDownloadValue = formatSpeed(downloadSpeed, downloadSuffix);
		}
		else {
			strDownloadValue = "";
		}
		
		String delimiter = "";
		if(prefDisplay == 0) {
		    delimiter = "\n";
		}
		else {
		    delimiter = " ";
		    showInExactPosition = false; //irrelevant in one-line mode
		}
		
		String ret = "";
		boolean showBothSpeeds = strUploadValue.length() > 0 && strDownloadValue.length() > 0;
		
		if(showBothSpeeds==false && showInExactPosition==false) {
		    delimiter = "";
		}
		
		if (prefSwapSpeeds) {
			ret = strDownloadValue + delimiter + strUploadValue;
		} else {
			ret = strUploadValue + delimiter + strDownloadValue;
		}
		return ret;
	}
	
	private final String formatSpeed(final long transferSpeedBytes, final String transferSuffix) {
		float unitFactor;
		long transferSpeed = transferSpeedBytes;
		
		switch (prefUnitMode) {
		case 0: // Binary bits
			transferSpeed *= 8;
			//no break
		case 1: // Binary bytes
			unitFactor = 1024f;
			break;
		case 2: // Decimal bits
			transferSpeed *= 8;
			//no break
		default:
		case 3: // Decimal bytes
			unitFactor = 1000f;
			break;
		}
		
		int tempPrefUnit = prefForceUnit;
		float megaTransferSpeed = ((float) transferSpeed) / (unitFactor * unitFactor);
		float kiloTransferSpeed = ((float) transferSpeed) / unitFactor;
		
		float transferValue;
		DecimalFormat transferDecimalFormat;
		
		if (prefForceUnit == 0) { // Auto mode
			
			if (megaTransferSpeed >= 1) {
				tempPrefUnit = 3;
				
			} else if (kiloTransferSpeed >= 1) {
				tempPrefUnit = 2;
				
			} else {
				tempPrefUnit = 1;
			}
		}
		
		switch (tempPrefUnit) {
		case 3:
			transferValue = megaTransferSpeed;
			transferDecimalFormat = formatWithDecimal;
			break;
		case 2:
			transferValue = kiloTransferSpeed;
			transferDecimalFormat = formatWithoutDecimal;
			break;
		default:
		case 1:
			transferValue = transferSpeed;
			transferDecimalFormat = formatWithoutDecimal;
			break;
		}
		
		String strTransferValue;
		
		if (transferSpeedBytes < prefHideBelow) {
		    strTransferValue = "";
		}
		else {
		    strTransferValue = transferDecimalFormat.format(transferValue);
		}
		
		if(strTransferValue.length() > 0) {
		    strTransferValue += Common.formatUnit(prefUnitMode, tempPrefUnit, prefUnitFormat);
		    strTransferValue += transferSuffix;
		}
		else if (prefShowSuffix) {
			strTransferValue += transferSuffix;
		}
		
		return strTransferValue;
	}

	private final void update() {
		mTrafficHandler.removeCallbacks(mRunnable);
		mTrafficHandler.postDelayed(mRunnable, prefUpdateInterval);
	}

	private final Runnable mRunnable = new Runnable() {
		@Override
		public void run() {
			try {
				mTrafficHandler.sendEmptyMessage(0);
			} catch (Exception e) {
				Log.e(TAG, "run failed: ", e);
				Common.throwException(e);
			}
		}
	};
	
	private final void updateViewVisibility() {
		if (networkState && prefNetworkType.contains(networkType)) {
			if (mAttached) {
				updateTraffic();
			}
			setVisibility(View.VISIBLE);
		} else {
			setVisibility(View.GONE);
		}
		Log.d(TAG, "Visibility (visible: ", View.VISIBLE, "; gone: ", View.GONE, "): ", getVisibility());
	}

	private final void loadPreferences() {
		try {
			XSharedPreferences mPref = new XSharedPreferences(Common.PKG_NAME);
			
			// fetch all preferences first
			int localPrefForceUnit = Common.getPrefInt(mPref, Common.KEY_FORCE_UNIT, Common.DEF_FORCE_UNIT);
			int localPrefUnitMode = Common.getPrefInt(mPref, Common.KEY_UNIT_MODE, Common.DEF_UNIT_MODE);
			Set<String> localPrefUnitFormat = mPref.getStringSet(Common.KEY_UNIT_FORMAT, Common.DEF_UNIT_FORMAT);
			int localPrefHideBelow = Common.getPrefInt(mPref, Common.KEY_HIDE_BELOW, Common.DEF_HIDE_BELOW);
			boolean localPrefShowSuffix = mPref.getBoolean(Common.KEY_SHOW_SUFFIX, Common.DEF_SHOW_SUFFIX);
			float localPrefFontSize = Common.getPrefFloat(mPref, Common.KEY_FONT_SIZE, Common.DEF_FONT_SIZE);
			int localPrefPosition = Common.getPrefInt(mPref, Common.KEY_POSITION, Common.DEF_POSITION);
			int localPrefSuffix = Common.getPrefInt(mPref, Common.KEY_SUFFIX, Common.DEF_SUFFIX);
			Set<String> localPrefNetworkType = mPref.getStringSet(Common.KEY_NETWORK_TYPE, Common.DEF_NETWORK_TYPE);
			Set<String> localPrefNetworkSpeed = mPref.getStringSet(Common.KEY_NETWORK_SPEED, Common.DEF_NETWORK_SPEED);
			int localPrefDisplay = Common.getPrefInt(mPref, Common.KEY_DISPLAY, Common.DEF_DISPLAY);
			boolean localPrefSwapSpeeds = mPref.getBoolean(Common.KEY_SWAP_SPEEDS, Common.DEF_SWAP_SPEEDS);
			int localPrefUpdateInterval = Common.getPrefInt(mPref, Common.KEY_UPDATE_INTERVAL, Common.DEF_UPDATE_INTERVAL);
			boolean localPrefFontColor = mPref.getBoolean(Common.KEY_FONT_COLOR, Common.DEF_FONT_COLOR);
			int localPrefColor = mPref.getInt(Common.KEY_COLOR, Common.DEF_COLOR);
			Set<String> localPrefFontStyle = mPref.getStringSet(Common.KEY_FONT_STYLE, Common.DEF_FONT_STYLE);
			boolean localEnableLogging = mPref.getBoolean(Common.KEY_ENABLE_LOG, Common.DEF_ENABLE_LOG);
			
			// only when all are fetched, set them to fields
			prefForceUnit = localPrefForceUnit;
			prefUnitMode = localPrefUnitMode;
			prefUnitFormat = localPrefUnitFormat;
			prefHideBelow = localPrefHideBelow;
			prefShowSuffix = localPrefShowSuffix;
			prefFontSize = localPrefFontSize;
			prefPosition = localPrefPosition;
			prefSuffix = localPrefSuffix;
			prefNetworkType = localPrefNetworkType;
			prefNetworkSpeed = localPrefNetworkSpeed;
			prefDisplay = localPrefDisplay;
			prefSwapSpeeds = localPrefSwapSpeeds;
			prefUpdateInterval = localPrefUpdateInterval;
			prefFontColor = localPrefFontColor;
			prefColor = localPrefColor;
			prefFontStyle = localPrefFontStyle;
			Log.enableLogging = localEnableLogging;
			
		} catch (Exception e) {
			Log.e(TAG, "loadPreferences failure ignored, using defaults. Exception: ", e);
		}
	}
}
