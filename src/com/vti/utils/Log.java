package com.vti.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Log {
	static final boolean LOG = true;
	
	public static String stack2string(Exception e) {
		try {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			return sw.toString();
		} catch (Exception e2) {
			return "bad stack2string, exception info. lost.";
		}
	}

	public static void i(String tag, String string) {
		if (LOG)
			android.util.Log.i(tag, string);
	}

	public static void e(String tag, String string) {
		if (LOG)
			android.util.Log.e(tag, string);
	}

	public static void d(String tag, String string) {
		if (LOG)
			android.util.Log.d(tag, string);
	}

	public static void v(String tag, String string) {
		if (LOG)
			android.util.Log.v(tag, string);
	}

	public static void w(String tag, String string) {
		if (LOG)
			android.util.Log.w(tag, string);
	}
}
