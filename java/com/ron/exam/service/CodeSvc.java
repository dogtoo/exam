package com.ron.exam.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.ron.exam.util.DbUtil;

public class CodeSvc {
	
	public static String c_kindYesNo = "YESNO";
	public static String c_kindWeekDay = "WDAY";
	public static String c_kindPrivBase = "PRIVBASE";
	public static String c_kindSystemCode = "SYS";
	public static String c_kindQsMastFlag = "QSMFLG";
	public static String c_kindQsValidFlag = "QSVFLG";
	public static String c_kindQsClassType = "QSCLST";
	public static String c_kindQsNoteImport = "QSNTIMP";
	public static String c_kindQsUserRole = "QSUSRR";
	public static String c_kindScOptionPlace = "SCOPTPLS";
	public static String c_kindExEquipType = "EXEQPTYP";

	public static String c_kindScResult = "SCRESU";
	public static String c_kindScSectType = "SCSECT";
	public static String c_kindScFractType = "SCFRACT";
	public static String c_kindScOptType = "SCOPTT";
	

	public static String buildSelectOptionByKind(DbUtil dbu, String codeKind, boolean blank) throws SQLException {
		String sqlQryCode =
			  " SELECT code_code, code_desc\n"
			+ "   FROM cmcode\n"
			+ "  WHERE code_kind = ?\n"
			+ "  ORDER BY code_order\n";
		ResultSet rsParam = dbu.queryList(sqlQryCode, codeKind);
		return MiscTool.buildSelectOptionByResult(rsParam, "code_code", "code_desc", blank);
	}

	public static List<Map<String, Object>> buildSelectDataByKind(DbUtil dbu, String codeKind, boolean blank) throws SQLException {
		String sqlQryCode =
			  " SELECT code_code \"value\", code_desc \"text\"\n"
			+ "   FROM cmcode\n"
			+ "  WHERE code_kind = ?\n"
			+ "  ORDER BY code_order\n";
		List<Map<String, Object>> all = new ArrayList<Map<String, Object>>();
		if (blank) {
			Map<String, Object> empty = new HashMap<String, Object>();
			empty.put("value", "");
			empty.put("text", "");
			all.add(empty);
		}
		all.addAll(dbu.selectMapAllList(sqlQryCode, codeKind));
		return all;
	}

	public static Map<String, String> buildStringMapByKind(DbUtil dbu, String codeKind) throws SQLException {
		Map<String, String> map = new HashMap<String, String>();
		String sqlQryCode =
			  " SELECT code_code, code_desc"
			+ "   FROM cmcode"
			+ "  WHERE code_kind = ?"
			+ "  ORDER BY code_code";
		ResultSet rs = dbu.queryList(sqlQryCode, codeKind);
		while (rs.next()) {
			map.put(rs.getString("code_code"), rs.getString("code_desc"));
		}
		rs.close();
		return map;
	}
	
	public static String queryCodeDesc(DbUtil dbu, String codeKind, String codeCode) throws SQLException {
		String sqlQryCode =
			  " SELECT code_desc FROM cmcode WHERE code_kind = ? AND code_code = ?";
		return dbu.selectStringList(sqlQryCode, codeKind, codeCode);
	}
	
	public static boolean doesCodeExist(DbUtil dbu, String codeKind, String codeCode) throws SQLException {
		String sqlCntCode =
			  " SELECT COUNT(*) FROM cmcode WHERE code_kind = ? AND code_code = ?";
		return dbu.selectIntList(sqlCntCode, codeKind, codeCode) > 0;
	}
	
}
