package com.ron.exam.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.ron.exam.util.DbUtil;

public class MemberSvc {

	public static List<Map<String, Object>> queryMemberList(DbUtil dbu, String memberCond) throws SQLException {
		String sqlQryMember =
			  " SELECT user_id \"value\", user_name \"text\"\n"
			+ "   FROM mbdetl\n"
			+ "  WHERE user_id LIKE '%' || ? || '%'\n"
			+ "     OR user_name LIKE '%' || ? || '%'\n";
		return dbu.selectMapAllList(sqlQryMember, memberCond.toUpperCase(), memberCond);
	}
	
	/**
	 * 查詢帳號之使用者名稱
	 * @param dbu
	 * @param userId
	 * @return null: 帳號不存在
	 * @throws SQLException
	 */
	public static String getMemberName(DbUtil dbu, String userId) throws SQLException {
		String sqlQryMember = " SELECT user_name FROM mbdetl WHERE user_id = ?";
		ResultSet rsMember = dbu.queryList(sqlQryMember, userId);
		userId = "[" + userId + "]";
		String userName = null;
		if (rsMember.next())
			userName = DbUtil.nullToEmpty(rsMember.getString("user_name"));
		rsMember.close();
		return userName;
	}
	
	/**
	 * 查詢帳號之使用者名稱，如果不存在，則回傳 userId
	 * @param dbu
	 * @param userId
	 * @return null: 帳號不存在
	 * @throws SQLException
	 */
	public static String getMemberNameOrId(DbUtil dbu, String userId) throws SQLException {
		String sqlQryMember = " SELECT user_name FROM mbdetl WHERE user_id = ?";
		Map<String, Object> rowMember = dbu.selectMapRowList(sqlQryMember, userId);
		if (rowMember == null)
			return userId;
		String userName = (String) rowMember.get("user_name");
		if (userName == null || userName.isEmpty())
			return userId;
		return userName;
	}
}
