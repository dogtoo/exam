package com.ron.exam.util;

	import java.util.*;
	import java.util.regex.*;

public class StdCalendar extends GregorianCalendar {
	private static final long serialVersionUID = 1L;

	private static final Pattern c_reDateTime = Pattern.compile("((\\d{1,4})/(\\d{1,2})(/(\\d{1,2}))?)?\\s*((\\d{1,2}):(\\d{1,2})(:(\\d{1,2})(\\.(\\d+))?)?)?\\s*");
	private static final Pattern c_reDbDateTimestamp = Pattern.compile("(\\d{4})(\\d{2})(\\d{2})(\\d{2})?(\\d{2})?(\\d{2})?(\\d{1,3})?");
	private static final Pattern c_reDigitDateTime = Pattern.compile("\\s*(\\d{6}|\\d{8}|\\d{12}|\\d{14}|\\d{17})\\s*");
	
	public static final StdCalendar nullDate = decode("00010101000000000");
	public static final StdCalendar minDate = decode("00010102000000000");
	public static final StdCalendar maxDate = decode("99991230000000000");
	
	/**
	 * 檢查輸入字串是否為 yyyy/mm/dd 的日期格式
	 * @param date
	 * @return
	 */
	public static boolean isDateFormat(String date) {
		Matcher ma = c_reDateTime.matcher(date);
		if (!ma.matches())
			return false;
		if (ma.group(1) == null || ma.group(4) == null)
			return false;
		StdCalendar cal = new StdCalendar(date);
		if (cal.get(Calendar.YEAR) != Integer.parseInt(ma.group(2)))
			return false;
		if (cal.get(Calendar.MONTH) != Integer.parseInt(ma.group(3)) - 1)
			return false;
		if (cal.get(Calendar.DAY_OF_MONTH) != Integer.parseInt(ma.group(5)))
			return false;
		return true;
	}
	
	/**
	 * 檢查輸入字串是否為 HH:MM 的時間格式
	 * @param time
	 * @return
	 */
	public static boolean isTimeFormat(String time) {
		Pattern reTime = Pattern.compile("(\\d{1,2}):(\\d{1,2})");
		Matcher ma = reTime.matcher(time);
		if (!ma.matches())
			return false;
		int hour = Integer.parseInt(ma.group(1));
		if (hour < 0 || hour > 23)
			return false;
		int min = Integer.parseInt(ma.group(2));
		if (min < 0 || min > 59)
			return false;
		return true;
	}
	
	public StdCalendar() {
		super();
	}
	
	public StdCalendar(long ms) {
		super();
		setTimeInMillis(ms);
	}
	
	public StdCalendar(String time) {
		super();
		if (time == null)
			setTimeInMillis(nullDate.getTimeInMillis());
		else if (c_reDigitDateTime.matcher(time).matches())
			fromDigitString(time);
		else if (c_reDateTime.matcher(time).matches())
			fromString(time);
		else if (c_reDbDateTimestamp.matcher(time).matches())
			fromDbString(time);
		else
			setTimeInMillis(nullDate.getTimeInMillis());
	}
	
	public StdCalendar addr(int field, int amount) {
		super.add(field, amount);
		return this;
	}
	
	private String toMS(int msDigit) {
		// 取得毫秒部份
		int ms = get(Calendar.MILLISECOND);
		if (msDigit == 0)
			return "";
		if (msDigit == 1)
			return String.format(".%01d", ms / 100);
		if (msDigit == 2)
			return String.format(".%02d", ms / 10);
		return String.format(".%03d", ms);
	}
	
	private void fromMS(String ms) {
		// 設定毫秒部份
		if (ms == null)
			return;
		// 最多接受三位
		String lms = ms.length() > 3 ? ms = ms.substring(0, 3) : ms;  
		int v;
		if (lms.length() == 1)
			v = Integer.parseInt(lms) * 100;
		else if (lms.length() == 2)
			v = Integer.parseInt(lms) * 10;
		else
			v = Integer.parseInt(lms);
		set(Calendar.MILLISECOND, v);
	}
	
	public String toDateString(int len) {
		// 取得西元年日期字串
		if (isNullDate())
			return null;
		if (len == 6)
			return String.format("%4d/%02d", get(Calendar.YEAR), get(Calendar.MONTH) + 1);
		return String.format("%4d/%02d/%02d", get(Calendar.YEAR), get(Calendar.MONTH) + 1, get(Calendar.DAY_OF_MONTH));
	}
	
	public String toDateString() {
		// 取得西元年日期字串
		return toDateString(8);
	}
	
	public String toDateStringM(int len) {
		// 取得民西元日期字串, 如果是 minDate 或 maxDate 則回傳空字串
		if (isMinDate() || isMaxDate())
			return "";
		return toDateString(len);
	}
	
	public String toDateStringM() {
		// 取得民西元日期字串, 如果是 minDate 或 maxDate 則回傳空字串
		if (isMinDate() || isMaxDate())
			return "";
		return toDateString();
	}
	
	public String toDateStringN(int len) {
		// 取得民西元日期字串, 如果是 nullDate 則回傳空字串
		if (isNullDate())
			return "";
		return toDateString(len);
	}
	
	public String toDateStringN() {
		// 取得民西元日期字串, 如果是 nullDate 則回傳空字串
		if (isNullDate())
			return "";
		return toDateString();
	}
	
	public String toHourString() {
		// 取得小時字串
		return String.format("%02d", get(Calendar.HOUR_OF_DAY));
	}
	
	public String toMinString() {
		// 取得分字串
		return String.format("%02d", get(Calendar.MINUTE));
	}
	
	public String toSecString() {
		// 取得秒字串
		return String.format("%02d", get(Calendar.SECOND));
	}
	
	public String toTimeString() {
		// 取得時分字串
		if (isNullDate())
			return null;
		return String.format("%02d:%02d", get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE));
	}
	
	public String toTimesString() {
		// 取得時分秒字串
		if (isNullDate())
			return null;
		return String.format("%02d:%02d:%02d", get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE), get(Calendar.SECOND));
	}
	
	public String toTimesString(int msDigit) {
		// 取得時分秒及秒下 msDigit 位數的字串
		if (isNullDate())
			return null;
		return toTimesString() + toMS(msDigit);
	}
	
	public String toDateTimeString() {
		// 取得西元年日期及時分字串
		if (isNullDate())
			return null;
		return toDateString() + " " + toTimeString();
	}
	
	public String toDateTimeStringM() {
		// 取得西元年日期及時分字串, 如果是 minDate 或 maxDate 則回傳空字串
		if (isMinDate() || isMaxDate())
			return "";
		if (isNullDate())
			return null;
		return toDateString() + " " + toTimeString();
	}
	
	public String toDateTimesString() {
		// 取得西元年日期及時分秒字串
		if (isNullDate())
			return null;
		return toDateString() + " " + toTimesString();
	}
	
	public String toDateTimesStringM() {
		// 取得西元年日期及時分秒字串, 如果是 minDate 或 maxDate 則回傳空字串
		if (isMinDate() || isMaxDate())
			return "";
		if (isNullDate())
			return null;
		return toDateString() + " " + toTimesString();
	}
	
	public String toDateTimesString(int msDigit) {
		// 取得西元年日期及時分秒與秒下 msDigit 字串
		if (isNullDate())
			return null;
		return toDateString() + " " + toTimesString(msDigit);
	}
	
	public String toDateTimesStringM(int msDigit) {
		// 取得西元年日期及時分秒與秒下 msDigit 字串, 如果是 minDate 或 maxDate 則回傳空字串
		if (isMinDate() || isMaxDate())
			return "";
		if (isNullDate())
			return null;
		return toDateString() + " " + toTimesString(msDigit);
	}

	public void fromString(String str) {
		// 從字串中取得西元年日期，時分秒與毫秒
		if (str == null || str.isEmpty()) {
			setTimeInMillis(nullDate.getTimeInMillis());
			return;
		}
		Matcher ma;
		ma = c_reDateTime.matcher(str);
		if (ma.matches()) {
			if (ma.group(1) != null) {
				// 有定義日期部份
				set(Calendar.YEAR, Integer.parseInt(ma.group(2)));
				set(Calendar.MONTH, Integer.parseInt(ma.group(3)) - 1);
				set(Calendar.DAY_OF_MONTH, ma.group(4) != null ? Integer.parseInt(ma.group(5)) : 1);
				// 預設時間為 0:0:0.0
				set(Calendar.AM_PM, Calendar.AM);
				set(Calendar.HOUR_OF_DAY, 0);
				set(Calendar.MINUTE, 0);
				set(Calendar.SECOND, 0);
				set(Calendar.MILLISECOND, 0);
			}
			if (ma.group(6) != null) {
				// 有設定時分部份
				set(Calendar.HOUR_OF_DAY, Integer.parseInt(ma.group(7)));
				set(Calendar.MINUTE, Integer.parseInt(ma.group(8)));
				// 預設秒為 0.0
				set(Calendar.SECOND, 0);
				set(Calendar.MILLISECOND, 0);
			}
			if (ma.group(9) != null) {
				// 有設定秒
				set(Calendar.SECOND, Integer.parseInt(ma.group(10)));
				set(Calendar.MILLISECOND, 0);
			}
			if (ma.group(11) != null) {
				// 有設定毫秒
				fromMS(ma.group(12));
			}
			return;
		}
		ma = c_reDigitDateTime.matcher(str);
		if (ma.matches()) {
			fromDigitString(str);
			return;
		}

		setTimeInMillis(nullDate.getTimeInMillis());
	}
	
	public String toDbDateString() {
		// 產生轉換至 database date 的標準字串
		String str = null;
		if (!isNullDate())
			str = String.format("%tY%tm%td", this, this, this);
		return str;
	}
	
	public String toDbTimesString(int msDigit) {
		// 產生轉換至 database time 的標準字串
		String str = null;
		if (!isNullDate()) {
			str = String.format("%tH%tM%tS", this, this, this);
			if (msDigit > 0)
				str += toMS(msDigit);
		}
		return str;
	}
	
	public String toDbTimesString() {
		// 產生轉換至 database time 的標準字串
		return toDbTimesString(0);
	}
	
	public String toDbTimeString() {
		// 產生轉換至 database time 的標準字串
		String str = null;
		if (!isNullDate())
			str = String.format("%tH%tM", this, this);
		return str;
	}
	
	public String toDbString() {
		// 產生轉換至 database date+time 的標準字串
		String str = null;
		if (!isNullDate())
			str = toDbDateString() + toDbTimesString();
		return str;
	}
	
	public String toDbString(int msDigit) {
		// 產生轉換至 datebase timestamp 的標準字串
		String str = null;
		if (!isNullDate())
			str = toDbDateString() + toDbTimesString(msDigit);
		return str;
	}
	
	public void fromDbString(String str) {
		// 從資料庫的標準字串轉入
		if (str == null) {
			setTimeInMillis(nullDate.getTimeInMillis());
			return;
		}
		Matcher ma = c_reDbDateTimestamp.matcher(str);
		if (ma.matches()) {
			set(Calendar.YEAR, Integer.parseInt(ma.group(1)));
			set(Calendar.MONTH, Integer.parseInt(ma.group(2)) - 1);
			set(Calendar.DAY_OF_MONTH, Integer.parseInt(ma.group(3)));
			set(Calendar.HOUR_OF_DAY, ma.group(4) != null ? Integer.parseInt(ma.group(4)) : 0);
			set(Calendar.MINUTE, ma.group(5) != null ? Integer.parseInt(ma.group(5)) : 0);
			set(Calendar.SECOND, ma.group(6) != null ? Integer.parseInt(ma.group(6)) : 0);
			if (ma.group(7) != null)
				fromMS(ma.group(7));
			else
				set(Calendar.MILLISECOND, 0);
		}
	}

	public String toDigitString(int len) {
		// 產生連續的數字格式字串
		if (isNullDate())
			return null;
		if (len == 6)
			// YYYYMM
			return String.format("%04d%02d", get(Calendar.YEAR), get(Calendar.MONTH) + 1);
		if (len == 8)
			// YYYYMMDD
			return String.format("%04d%02d%02d", get(Calendar.YEAR), get(Calendar.MONTH) + 1, get(Calendar.DAY_OF_MONTH));
		if (len == 12)
			// YYYYMMDDhhmm
			return String.format("%04d%02d%02d%02d%02d", get(Calendar.YEAR), get(Calendar.MONTH) + 1, get(Calendar.DAY_OF_MONTH),
				get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE));
		if (len == 14)
				// YYYYMMDDhhmmss
			return String.format("%04d%02d%02d%02d%02d%02d", get(Calendar.YEAR), get(Calendar.MONTH) + 1, get(Calendar.DAY_OF_MONTH),
				get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE), get(Calendar.SECOND));
		if (len == 16)
				// YYYYMMDDhhmmssff2
			return String.format("%04d%02d%02d%02d%02d%02d%02d", get(Calendar.YEAR), get(Calendar.MONTH) + 1, get(Calendar.DAY_OF_MONTH),
				get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE), get(Calendar.SECOND), get(Calendar.MILLISECOND) / 10);
		if (len == 17)
			// YYYYMMDDhhmmssff3
			return String.format("%04d%02d%02d%02d%02d%02d%03d", get(Calendar.YEAR), get(Calendar.MONTH) + 1, get(Calendar.DAY_OF_MONTH),
				get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE), get(Calendar.SECOND), get(Calendar.MILLISECOND));
		return "";
	}

	public String toDigitTimeString(int len) {
		// 產生連續的數字格式字串
		if (isNullDate())
			return null;
		if (len == 4)
			// hhmm
			return String.format("%02d%02d", get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE));
		if (len == 6)
				// hhmmss
			return String.format("%02d%02d%02d", get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE), get(Calendar.SECOND));
		if (len == 8)
				// hhmmssff2
			return String.format("%02d%02d%02d%02d", get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE), get(Calendar.SECOND), get(Calendar.MILLISECOND) / 10);
		if (len == 9)
			// hhmmssff3
			return String.format("%02d%02d%02d%03d", get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE), get(Calendar.SECOND), get(Calendar.MILLISECOND));
		return "";
	}

	public StdCalendar fromDigitString(String str) {
		// 從連續的數字格式解出日期
		if (str == null || str.isEmpty()) {
			setTimeInMillis(nullDate.getTimeInMillis());
			return this;
		}
		int len = str.length();
		try {
			switch (len) {
				case 6:
					// YYYYMM
					set(Calendar.YEAR, Integer.parseInt(str.substring(0, 4)));
					set(Calendar.MONTH, Integer.parseInt(str.substring(4, 6)) - 1);
					set(Calendar.DAY_OF_MONTH, 1);
					set(Calendar.HOUR_OF_DAY, 0);
					set(Calendar.MINUTE, 0);
					set(Calendar.SECOND, 0);
					set(Calendar.MILLISECOND, 0);
					break;
				case 8:
					// YYYYMMDD
					set(Calendar.YEAR, Integer.parseInt(str.substring(0, 4)));
					set(Calendar.MONTH, Integer.parseInt(str.substring(4, 6)) - 1);
					set(Calendar.DAY_OF_MONTH, Integer.parseInt(str.substring(6, 8)));
					set(Calendar.HOUR_OF_DAY, 0);
					set(Calendar.MINUTE, 0);
					set(Calendar.SECOND, 0);
					set(Calendar.MILLISECOND, 0);
					break;
				case 12:
					// YYYYMMDDhhmm
					set(Calendar.YEAR, Integer.parseInt(str.substring(0, 4)));
					set(Calendar.MONTH, Integer.parseInt(str.substring(4, 6)) - 1);
					set(Calendar.DAY_OF_MONTH, Integer.parseInt(str.substring(6, 8)));
					set(Calendar.HOUR_OF_DAY, Integer.parseInt(str.substring(8, 10)));
					set(Calendar.MINUTE, Integer.parseInt(str.substring(10, 12)));
					set(Calendar.SECOND, 0);
					set(Calendar.MILLISECOND, 0);
					break;
				case 14:
					// YYYYMMDDhhmmss
					set(Calendar.YEAR, Integer.parseInt(str.substring(0, 4)));
					set(Calendar.MONTH, Integer.parseInt(str.substring(4, 6)) - 1);
					set(Calendar.DAY_OF_MONTH, Integer.parseInt(str.substring(6, 8)));
					set(Calendar.HOUR_OF_DAY, Integer.parseInt(str.substring(8, 10)));
					set(Calendar.MINUTE, Integer.parseInt(str.substring(10, 12)));
					set(Calendar.SECOND, Integer.parseInt(str.substring(12, 14)));
					set(Calendar.MILLISECOND, 0);
					break;
				case 16:
					// YYYYMMDDhhmmssff2
					set(Calendar.YEAR, Integer.parseInt(str.substring(0, 4)));
					set(Calendar.MONTH, Integer.parseInt(str.substring(4, 6)) - 1);
					set(Calendar.DAY_OF_MONTH, Integer.parseInt(str.substring(6, 8)));
					set(Calendar.HOUR_OF_DAY, Integer.parseInt(str.substring(8, 10)));
					set(Calendar.MINUTE, Integer.parseInt(str.substring(10, 12)));
					set(Calendar.SECOND, Integer.parseInt(str.substring(12, 14)));
					set(Calendar.MILLISECOND, Integer.parseInt(str.substring(14, 16)) * 10);
					break;
				case 17:
					// YYYYMMDDhhmmssff3
					set(Calendar.YEAR, Integer.parseInt(str.substring(0, 4)));
					set(Calendar.MONTH, Integer.parseInt(str.substring(4, 6)) - 1);
					set(Calendar.DAY_OF_MONTH, Integer.parseInt(str.substring(6, 8)));
					set(Calendar.HOUR_OF_DAY, Integer.parseInt(str.substring(8, 10)));
					set(Calendar.MINUTE, Integer.parseInt(str.substring(10, 12)));
					set(Calendar.SECOND, Integer.parseInt(str.substring(12, 14)));
					set(Calendar.MILLISECOND, Integer.parseInt(str.substring(14, 17)));
					break;
				default:
					str = "";
					break;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return this;
	}
	
	public StdCalendar fromDigitTimeString(String str) {
		// 從連續的數字格式解出時間
		if (str == null || str.isEmpty()) {
			setTimeInMillis(nullDate.getTimeInMillis());
			return this;
		}
		int len = str.length();
		try {
			switch (len) {
				case 4:
					// hhmm
					set(Calendar.HOUR_OF_DAY, Integer.parseInt(str.substring(0, 2)));
					set(Calendar.MINUTE, Integer.parseInt(str.substring(2, 4)));
					set(Calendar.SECOND, 0);
					set(Calendar.MILLISECOND, 0);
					break;
				case 6:
					// hhmmss
					set(Calendar.HOUR_OF_DAY, Integer.parseInt(str.substring(0, 2)));
					set(Calendar.MINUTE, Integer.parseInt(str.substring(2, 4)));
					set(Calendar.SECOND, Integer.parseInt(str.substring(4, 6)));
					set(Calendar.MILLISECOND, 0);
					break;
				case 8:
					// hhmmssff2
					set(Calendar.HOUR_OF_DAY, Integer.parseInt(str.substring(0, 2)));
					set(Calendar.MINUTE, Integer.parseInt(str.substring(2, 4)));
					set(Calendar.SECOND, Integer.parseInt(str.substring(4, 6)));
					set(Calendar.MILLISECOND, Integer.parseInt(str.substring(6, 8)) * 10);
					break;
				case 9:
					// hhmmssff3
					set(Calendar.HOUR_OF_DAY, Integer.parseInt(str.substring(0, 2)));
					set(Calendar.MINUTE, Integer.parseInt(str.substring(2, 4)));
					set(Calendar.SECOND, Integer.parseInt(str.substring(4, 6)));
					set(Calendar.MILLISECOND, Integer.parseInt(str.substring(6, 9)));
					break;
				default:
					break;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return this;
	}

	public StdCalendar fromCalendar(Calendar src) {
		setTimeInMillis(src.getTimeInMillis());
		return this;
	}
	
	public static StdCalendar getMinDate() {
		// 取得最小日期的複本
		return (StdCalendar) minDate.clone();
	}
	
	public static StdCalendar getMaxDate() {
		// 取得最大日期的複本
		return (StdCalendar) maxDate.clone();
	}
	
	public static StdCalendar getNullDate() {
		// 取得空的日期
		return (StdCalendar) nullDate.clone();
	}
	
	public boolean isMinDate() {
		return equals(minDate);
	}
	
	public boolean isMaxDate() {
		return equals(maxDate);
	}
	
	public boolean isNullDate() {
		return equals(nullDate);
	}
	
	private static final StdCalendar decode(String str) {
		StdCalendar tc = new StdCalendar();
		tc.fromDigitString(str);
		return tc;
	}

	/**
	 * 檢查月日格式是否正確，並回傳月份及日期
	 * @param md md[0] 回傳月份, md[1] 回傳日期, md 為 null 則不回傳
	 * @param str 月份日期字川
	 * @return true: 格式正確, false: 格式不正確
	 */
	public static boolean parseMonthDayString(int md[], String str) {
		int c_monDays[] = { 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
		Pattern reMonthDay = Pattern.compile("(\\d{1,2})/(\\d{1,2})");
		Matcher ma = reMonthDay.matcher(str);
		if (!ma.matches())
			return false;
		int mon = Integer.parseInt(ma.group(1));
		int day = Integer.parseInt(ma.group(2));
		if (mon < 1 || mon > 12)
			return false;
		if (day < 1 || day > c_monDays[mon - 1])
			return false;
		if (md != null && md.length >= 2) {
			md[0] = mon;
			md[1] = day;
		}
		return true;
	}
}
