package tw.fatminmin.xposed.networkspeedindicator.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.os.Environment;

public class Log {
	
	private Log() {
		//prevent instantiation
	}
	
	private static final String TAG = Log.class.getSimpleName();
	private static final String logNull = "null";
	private static final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	private static final SimpleDateFormat sdfTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
	private static PrintWriter pw = null;
	
	public static boolean enableLogging = true;
	
	private static final String concatArray(final Object... msg) {
		if (msg == null || msg.length == 0) {
			return logNull;
		}
		if (msg.length == 1) {
			return msg[0]==null?logNull:msg[0].toString();
		}
		StringBuilder sb = new StringBuilder();
		for (Object o : msg) {
			sb.append(o==null?logNull:o.toString());
		}
		return sb.toString();
	}
	
	private static final Throwable getThrowable(final Object... msg) {
		if (msg == null)
			return null;
		for (Object o : msg) {
			if (o!=null && o instanceof Throwable)
				return (Throwable) o;
		}
		return null;
	}
	
	private static final void writeToFile(final char level, final String msg, final Throwable thr) {
		try {
			if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
				return;
			if (pw == null || pw.checkError()) {
				try {
					pw.close();
				} catch (Exception ignore) { }
				
				String foldername = Environment.getExternalStorageDirectory() + "/NetworkSpeedIndicator";
				File folder = new File(foldername);
				if (!folder.exists())
					folder.mkdir();
				if (!folder.isDirectory())
					return;
				
				String filename = foldername + "/" + sdfDate.format(new Date()) + ".log";
				pw = new PrintWriter(new FileWriter(filename, true));
			}
			pw.print(level);
			pw.print('/');
			pw.print(sdfTime.format(new Date()));
			pw.print('/');
			pw.println(msg);
			if (thr != null)
				thr.printStackTrace(pw);
			pw.flush();
		} catch (Exception e) {
			try {
				pw.close();
			} catch (Exception ignore) { }
			pw = null;
			android.util.Log.e(TAG, "Stack trace: ", e);
		}
	}
	
	public static final void d(final String tag, final Object... msg) {
		if (enableLogging) {
			String concatMsg = concatArray(msg);
			android.util.Log.d(tag, concatMsg);
			writeToFile('D', concatMsg, null);
		}
	}
	
	public static final void e(final String tag, final Object... msg) {
		// always log errors
		String concatMsg = concatArray(msg);
		android.util.Log.e(tag, concatMsg);
		
		Throwable thr = getThrowable(msg);
		if (thr != null) {
			android.util.Log.e(tag, "Stack trace: ", thr);
		}
		
		writeToFile('E', concatMsg, thr);
	}
	
	public static final void i(final String tag, final Object... msg) {
		if (enableLogging) {
			String concatMsg = concatArray(msg);
			android.util.Log.i(tag, concatMsg);
			writeToFile('I', concatMsg, null);
		}
	}
	
	public static final void v(final String tag, final Object... msg) {
		if (enableLogging) {
			String concatMsg = concatArray(msg);
			android.util.Log.v(tag, concatMsg);
			writeToFile('V', concatMsg, null);
		}
	}
	
	public static final void w(final String tag, final Object... msg) {
		if (enableLogging) {
			String concatMsg = concatArray(msg);
			android.util.Log.w(tag, concatMsg);
			writeToFile('W', concatMsg, null);
		}
	}
	
}
