package com.ron.exam.service;

import java.sql.*;
import java.util.*;

import javax.servlet.http.HttpSession;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.ron.exam.util.DbUtil;

public class ProgData {
	
	private static final String c_progDataKey = "_PROG_DATA_";

	public static final String c_privBaseProhibit = "P";
	public static final String c_privBaseQuery = "Q";
	public static final String c_privBaseMaintain = "M";
	
	private static class ProgInfo {
		public String progId;
		public String progTitle;
		public String progMenu;
		public String progDesc;
		public String sysId;
		public String privBase;
		public String privAux;
		public String allPrivAux;
	}
	
	private Map<String, ProgInfo> m_progMap;
	private List<String> m_progList;
	
	public ProgData(DbUtil dbu) throws SQLException {
		m_progList = new ArrayList<String>();
		m_progMap = new HashMap<String, ProgData.ProgInfo>();
		String sqlQryProg =
			  " SELECT prog_id, prog_title, prog_menu, prog_desc, sys_id, def_priv_base, def_priv_aux, all_priv_aux\n"
			+ "   FROM cmprog\n"
			+ "  ORDER BY sys_id, show_order";
		ResultSet rsProg = dbu.queryList(sqlQryProg);
		while (rsProg.next()) {
			ProgInfo info = new ProgInfo();
			info.progId = rsProg.getString("prog_id");
			info.progTitle = rsProg.getString("prog_title");
			info.progMenu = rsProg.getString("prog_menu");
			info.progDesc = rsProg.getString("prog_desc");
			info.sysId = rsProg.getString("sys_id");
			info.privBase = rsProg.getString("def_priv_base");
			info.privAux = DbUtil.nullToEmpty(rsProg.getString("def_priv_aux"));
			info.allPrivAux = DbUtil.nullToEmpty(rsProg.getString("all_priv_aux"));
			m_progMap.put(info.progId, info);
			m_progList.add(info.progId);
		}
		rsProg.close();
	}
	
	public static void createProgData(ProgData pd) {
		HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
		session.setAttribute(c_progDataKey, pd);
	}
	
	public static ProgData getProgData() {
		HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
		return (ProgData) session.getAttribute(c_progDataKey);
	}
	
	public static void removeProgData() {
		HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
		session.removeAttribute(c_progDataKey);
	}

	
	public String getProgTitle(String progId) {
		if (!m_progMap.containsKey(progId))
			return "<未定義程式標題>";
		return m_progMap.get(progId).progTitle;
	}
	
	public String getProgMenu(String progId) {
		if (!m_progMap.containsKey(progId))
			return "<未定義程式>";
		return m_progMap.get(progId).progMenu;
	}
	
	public String getProgDesc(String progId) {
		if (!m_progMap.containsKey(progId))
			return "<未定義程式說明>";
		return m_progMap.get(progId).progDesc;
	}
	
	public String getProgSysId(String progId) {
		if (!m_progMap.containsKey(progId))
			return "";
		return m_progMap.get(progId).sysId;
	}
	
	public String getProgPrivBase(String progId) {
		if (!m_progMap.containsKey(progId))
			return c_privBaseProhibit;
		return m_progMap.get(progId).privBase;
	}
	
	public String getProgPrivAux(String progId) {
		if (!m_progMap.containsKey(progId))
			return "";
		return m_progMap.get(progId).privAux;
	}
	
	/**
	 * 檢查程式附加權限字串是否可接受
	 * @param dbu
	 * @param progId
	 * @param privAux
	 * @return true: 可接受；false: 不接受
	 */
	public boolean checkProgAuxAcceptable(String progId, String privAux) {
		if (privAux == null || privAux.isEmpty())
			return true;
		if (!m_progMap.containsKey(progId))
			return false;
		String allProgPriv = m_progMap.get(progId).allPrivAux;
		boolean rc = true;
		for (int i = 0; i < privAux.length() && rc; i++) {
			char priv = privAux.charAt(i);
			if (privAux.indexOf(priv, i + 1) >= 0)	// 檢查是否重複定義
				rc = false;
			if (allProgPriv.indexOf(priv) < 0)		// 檢查是否定義於 all_priv_aux
				rc = false;
		}
		return rc;
	}
	
	/**
	 * 結合程式基本權限，取最大權限（最寬鬆）
	 * @param privBase1
	 * @param privBase2
	 */
	public static String mergePrivBase(String privBase1, String privBase2) {
		if (c_privBaseMaintain.equals(privBase1) || c_privBaseMaintain.equals(privBase2))
			return c_privBaseMaintain;
		if (c_privBaseQuery.equals(privBase1) || c_privBaseQuery.equals(privBase2))
			return c_privBaseQuery;
		return c_privBaseProhibit;
	}
	
	/**
	 * 結合程式附加權限，取最多附加權限（聯集）
	 * @param privAux1
	 * @param privAux2
	 */
	public static String mergePrivAux(String privAux1, String privAux2) {
		String privAux = "";
		for (int i = 0; i < privAux1.length(); i++)
			if (privAux.indexOf(privAux1.charAt(i)) < 0)
				privAux += privAux1.charAt(i);
		for (int i = 0; i < privAux2.length(); i++)
			if (privAux.indexOf(privAux2.charAt(i)) < 0)
				privAux += privAux2.charAt(i);
		return privAux;
	}
}
