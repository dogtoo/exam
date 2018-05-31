package com.ron.exam.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ron.exam.util.DbUtil;

public class MiscTool {

	/**
	 * 以 SQL 查詢結果 ResultSet 建立可放置於 html 的 select 元件 option 列表
	 * @param rs
	 * @param vfld
	 * @param tfld
	 * @param blank
	 * @return
	 * @throws SQLException
	 */
	public static String buildSelectOptionByResult(ResultSet rs, String vfld, String tfld, boolean blank) throws SQLException {
		StringBuffer str = new StringBuffer();
		if (blank)
			str.append("<option value=''>&nbsp;</option>\n");
		while (rs.next()) {
			str.append("<option value='" + rs.getString(vfld) + "'>" + rs.getString(tfld) + "</option>\n");
		}
		rs.close();
		return str.toString();
	}

	/**
	 * 以 SQL 建立可放置於 html 的 select 元件 option 列表
	 * @param dbu
	 * @param sql
	 * @param vfld
	 * @param tfld
	 * @param blank
	 * @param args
	 * @return
	 * @throws SQLException
	 */
	public static String buildSelectOptionBySql(DbUtil dbu, String sql, String vfld, String tfld, boolean blank, Object... args) throws SQLException {
		return buildSelectOptionByResult(dbu.queryArray(sql, args), vfld, tfld, blank);
	}

	/**
	 * 以 SQL 查詢結果 ResultSet 建立可透過 JSON 傳回 javascript 的 select option 列表
	 * @param rs
	 * @param vfld
	 * @param tfld
	 * @param blank
	 * @return
	 * @throws SQLException
	 */
	public static List<Map<String, String>> buildSelectDataByResult(ResultSet rs, String vfld, String tfld, boolean blank) throws SQLException {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		if (blank) {
			Map<String, String> map = new HashMap<String, String>();
			map.put("value", "");
			map.put("text", "");
			list.add(map);
		}
		while (rs.next()) {
			Map<String, String> map = new HashMap<String, String>();
			map.put("value", rs.getString(vfld));
			map.put("text", rs.getString(tfld));
			list.add(map);
		}
		rs.close();
		return list;
	}

	/**
	 * 以 SQL 建立可透過 JSON 傳回 javascript 的 select option 列表
	 * @param rs
	 * @param vfld
	 * @param tfld
	 * @param blank
	 * @param args
	 * @return
	 * @throws SQLException
	 */
	public static List<Map<String, String>> buildSelectDataBySql(DbUtil dbu, String sql, String vfld, String tfld, boolean blank, Object... args) throws SQLException {
		return buildSelectDataByResult(dbu.queryArray(sql, args), vfld, tfld, blank);
	}
	
	/**
	 * 檢查代碼欄位是否由合理字元組成（大小寫英文字母、數字或底線）
	 * @param id
	 * @param maxLength 最大長度
	 * @return
	 */
	public static String checkIdString(String id, int maxLength) {
		if (id == null || id.isEmpty())
			return "不可以為空白";
		if (id.length() > maxLength)
			return "長度不可以超過 " + maxLength + " 字元";
		for (int i = 0; i < id.length(); i++)
			if ("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_".indexOf(id.charAt(i)) < 0)
				return "只能由大小寫英文字母、數字或底線符號組成";
		return null;
	}
	
	public static String concatList(List<String> list, String sep) {
		StringBuffer str = new StringBuffer();
		for (int i = 0; i < list.size(); i++) {
			if (i > 0)
				str.append(sep);
			str.append(list.get(i));
		}
		return str.toString();
	}
	
	/**
	 * 將數字轉為三位一節的表示方式
	 * @param number
	 * @return
	 */
	public static String convToCommaInt(int number) {
		String org = Integer.toString(number);
		String num = "";
		for (int len = org.length(); len > 0; len -= 3) {
			if (!num.isEmpty())
				num = "," + num;
			num = (len <= 3 ? org.substring(0, len) : org.substring(len - 3, len)) + num;
		}
		return num;
	}
	
	public static int convFromCommaInt(String number) {
		if (number == null || number.isEmpty())
			return 0;
		StringBuffer str = new StringBuffer();
		for (int i = 0; i < number.length(); i++)
			if (number.charAt(i) != ',')
				str.append(number.charAt(i));
		return Integer.parseInt(str.toString());
	}
}
