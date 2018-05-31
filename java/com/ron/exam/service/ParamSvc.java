package com.ron.exam.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.ron.exam.util.DbUtil;
import com.ron.exam.util.StdCalendar;

public class ParamSvc {
	
	public static final String c_clsDepart = "DEPART";
	public static final String c_clsQsClass = "QSCLS";
	public static final String c_clsQsAbility = "QSABL";
	public static final String c_clsQsTarget = "QSTGT";
	public static final String c_clsQsFileClass = "QSFLCLS";
	public static final String c_clsQsNoteClass = "QSNTCLS";
	
	public static boolean checkParamType(String paramType, String paramLimit, String value) {
		if ("S".equals(paramType)) {
			if (!paramLimit.isEmpty())
				return Pattern.matches(paramLimit, value);
			return true;
		}
		if ("I".equals(paramType)) {
			try {
				int v = Integer.parseInt(value);
				if (!paramLimit.isEmpty()) {
					Matcher ma1 = Pattern.compile("(>(-?\\d+))").matcher(paramLimit);
					if (ma1.find() && v <= Integer.parseInt(ma1.group(2)))
						return false;
					Matcher ma2 = Pattern.compile("(>=(-?\\d+))").matcher(paramLimit);
					if (ma2.find() && v < Integer.parseInt(ma2.group(2)))
						return false;
					Matcher ma3 = Pattern.compile("(<(-?\\d+))").matcher(paramLimit);
					if (ma3.find() && v >= Integer.parseInt(ma3.group(2)))
						return false;
					Matcher ma4 = Pattern.compile("(<=(-?\\d+))").matcher(paramLimit);
					if (ma4.find() && v > Integer.parseInt(ma4.group(2)))
						return false;
				}
			}
			catch (NumberFormatException e) {
				return false;
			}
			return true;
		}
		if ("F".equals(paramType)) {
			try {
				double v = Double.parseDouble(value);
				if (!paramLimit.isEmpty()) {
					Matcher ma1 = Pattern.compile("(>(-?(\\d+|\\d+\\.\\d*|\\d*\\.\\d+)))").matcher(paramLimit);
					if (ma1.find() && v <= Double.parseDouble(ma1.group(2)))
						return false;
					Matcher ma2 = Pattern.compile("(>=(-?(\\d+|\\d+\\.\\d*|\\d*\\.\\d+)))").matcher(paramLimit);
					if (ma2.find() && v < Double.parseDouble(ma2.group(2)))
						return false;
					Matcher ma3 = Pattern.compile("(<(-?(\\d+|\\d+\\.\\d*|\\d*\\.\\d+)))").matcher(paramLimit);
					if (ma3.find() && v >= Double.parseDouble(ma3.group(2)))
						return false;
					Matcher ma4 = Pattern.compile("(<=(-?(\\d+|\\d+\\.\\d*|\\d*\\.\\d+)))").matcher(paramLimit);
					if (ma4.find() && v > Double.parseDouble(ma4.group(2)))
						return false;
				}
			}
			catch (NumberFormatException e) {
				return false;
			}
			return true;
		}
		if ("B".equals(paramType)) {
			if ("Y".equals(value) || "N".equals(value))
				return true;
			return false;
		}
		if ("D".equals(paramType)) {
			if (StdCalendar.isDateFormat(value))
				return true;
			return false;
		}
		if ("T".equals(paramType)) {
			if (StdCalendar.isTimeFormat(value))
				return true;
			return false;
		}
		return false;
	}
	
	public static String buildSelectOptionByClass(DbUtil dbu, String paramClass, boolean blank) throws SQLException {
		String sqlQryParam =
			  " SELECT param_id, param_name\n"
			+ "   FROM cmpard\n"
			+ "  WHERE param_class = ?\n"
			+ "  ORDER BY show_order\n";
		ResultSet rsParam = dbu.queryList(sqlQryParam, paramClass);
		return MiscTool.buildSelectOptionByResult(rsParam, "param_id", "param_name", blank);
	}

	public static List<Map<String, Object>> buildSelectDataByClass(DbUtil dbu, String paramClass, boolean blank) throws SQLException {
		String sqlQryParam =
			  " SELECT param_id \"value\", param_name \"text\"\n"
			+ "   FROM cmpard\n"
			+ "  WHERE param_class = ?\n"
			+ "  ORDER BY show_order\n";
		List<Map<String, Object>> all = new ArrayList<Map<String, Object>>();
		if (blank) {
			Map<String, Object> empty = new HashMap<String, Object>();
			empty.put("value", "");
			empty.put("text", "");
			all.add(empty);
		}
		all.addAll(dbu.selectMapAllList(sqlQryParam, paramClass));
		return all;
	}

	public static Map<String, String> buildStringMapByClass(DbUtil dbu, String paramClass) throws SQLException {
		String sqlQryParam = " SELECT param_id, param_name FROM cmpard WHERE param_class = ? ORDER BY show_order";
		return dbu.selectKeyStringList(sqlQryParam, "param_id", "param_name", paramClass);
	}
	
	public static boolean doesParamExist(DbUtil dbu, String paramClass, String paramId) throws SQLException {
		String sqlCntParam = " SELECT COUNT(*) FROM cmpard WHERE param_class = ? AND param_id = ?";
		return dbu.selectIntList(sqlCntParam, paramClass, paramId) > 0;
	}
}
