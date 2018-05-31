package com.ron.exam.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import javax.servlet.http.HttpSession;
import org.jasypt.util.password.BasicPasswordEncryptor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.ron.exam.util.DbUtil;
import com.ron.exam.util.StdCalendar;

public class UserData {

	private static final String c_userDataKey = "_USER_DATA_";

	public static final String c_passwordOk = "OK";
	private static final int c_passwordFailLock = 5;
	
	private class ProgPriv {
		private String privBase;
		private String privAux;
		private ProgPriv() {
			privBase = ProgData.c_privBaseProhibit;
			privAux = "";
		}
	};
	
	private boolean m_exists;
	private String m_userId;
	private String m_userPass;
	private String m_initPass;
	private String m_userName;
	private String m_menuId;
	private int m_failPass;
	private boolean m_needChgPass;
	private Calendar m_chgPassDate;
	private String m_beginDate;
	private String m_endDate;
	private Map<String, ProgPriv> m_privMap;
	private MenuSvc.MenuItem m_menu;
	private Map<String, String> m_privBaseDescMap;
	
	public UserData(DbUtil dbu, String userId) throws SQLException {
		
		m_exists = false;
		m_userId = null;
		m_userPass = null;
		m_initPass = null;
		m_userName = null;
		m_failPass = 0;
		m_needChgPass = false;
		m_chgPassDate = null;
		m_beginDate = null;
		m_endDate = null;
		m_privMap = new HashMap<String, ProgPriv>();
		m_menu = new MenuSvc.MenuItem();
		m_privBaseDescMap = CodeSvc.buildStringMapByKind(dbu, "PRIVBASE");
		
		String sqlQryUser =
			  " SELECT user_pass, init_pass, user_name, chg_pass_date, menu_id\n"
			+ "      , fail_pass, begin_date, end_date\n"
			+ "  FROM cmuser\n"
			+ "  WHERE user_id = ?\n";
		Map<String, Object> rowUser = dbu.selectMapRowList(sqlQryUser, userId);
		if (rowUser != null) {
			m_userId = userId;
			m_userPass = (String) rowUser.get("user_pass");
			m_initPass = (String) rowUser.get("init_pass");
			m_userName = (String) rowUser.get("user_name");
			m_menuId = (String) rowUser.get("menu_id");
			m_chgPassDate = new StdCalendar((String) rowUser.get("chg_pass_date"));
			m_failPass = (Integer) rowUser.get("fail_pass");
			m_beginDate = (String) rowUser.get("begin_date");
			m_endDate = (String) rowUser.get("end_date");
			m_exists = true;
			m_needChgPass = m_userPass == null;
		}
		
		MenuSvc.loadMenu(dbu, m_menuId, m_menu);

		// 查詢角色權限，角色可用的權限為相加方式處理
		String sqlQryUsrr =
			  " SELECT a.role_id\n"
			+ "   FROM cmusrr a, cmrole b\n"
			+ "  WHERE a.role_id = b.role_id\n"
			+ "    AND b.suspend <> 'Y'\n"
			+ "    AND user_id = ?";
		ResultSet rsUsrr = dbu.queryList(sqlQryUsrr, m_userId);
		while (rsUsrr.next()) {
			String roleId = rsUsrr.getString("role_id");
			String sqlQryRolePriv = 
				  " SELECT prog_id, priv_base, priv_aux\n"
				+ "   FROM cmpriv"
				+ "  WHERE acc_type = 'R'"
				+ "    AND acc_id = ?";
			ResultSet rsRolePriv = dbu.queryList(sqlQryRolePriv, roleId);
			while (rsRolePriv.next()) {
				String progId = rsRolePriv.getString("prog_id");
				String privBase = rsRolePriv.getString("priv_base");
				String privAux = DbUtil.nullToEmpty(rsRolePriv.getString("priv_aux"));
				if (!m_privMap.containsKey(progId))
					m_privMap.put(progId, new ProgPriv());
				ProgPriv priv = m_privMap.get(progId);
				priv.privBase = ProgData.mergePrivBase(priv.privBase, privBase);
				priv.privAux = ProgData.mergePrivAux(priv.privAux, privAux);
			}
			rsRolePriv.close();
		}
		rsUsrr.close();
		
		// 查詢帳號權限，帳號權限以覆蓋方式處理
		String sqlQryUserPriv =
			  " SELECT prog_id, priv_base, priv_aux"
			+ "   FROM cmpriv"
			+ "  WHERE acc_type = 'U'"
			+ "    AND acc_id = ?";
		ResultSet rsUserPriv = dbu.queryList(sqlQryUserPriv, m_userId);
		while (rsUserPriv.next()) {
			String progId = rsUserPriv.getString("prog_id");
			String privBase = rsUserPriv.getString("priv_base");
			String privAux = DbUtil.nullToEmpty(rsUserPriv.getString("priv_aux"));
			if (!m_privMap.containsKey(progId))
				m_privMap.put(progId, new ProgPriv());
			ProgPriv priv = m_privMap.get(progId);
			priv.privBase = privBase;
			priv.privAux = privAux;
		}
		rsUserPriv.close();
	}
	
	public static void createUserData(UserData ud) {
		HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
		session.setAttribute(c_userDataKey, ud);
	}
	
	public static UserData getUserData() {
		HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
		return (UserData) session.getAttribute(c_userDataKey);
	}
	
	public static void removeUserData() {
		HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
		session.removeAttribute(c_userDataKey);
	}
	
	public String chkPass(String pass) {
		if (m_failPass >= c_passwordFailLock)
			return "連續輸入錯誤密碼次數過多";
		if (m_userPass != null) {
			BasicPasswordEncryptor enc = new BasicPasswordEncryptor();
			if (enc.checkPassword(pass, m_userPass))
				return c_passwordOk;
			else
				return "密碼錯誤 （連續錯誤第" + (m_failPass + 1) + "次）";
		}
		else {
			if (pass.equals(m_initPass)) {
				return c_passwordOk;
			}
			else
				return "初始密碼錯誤 （連續錯誤第" + (m_failPass + 1) + "次）";
		}
	}
	
	public void chgPass(DbUtil dbu, String pass) throws SQLException, Exception {
		BasicPasswordEncryptor enc = new BasicPasswordEncryptor();
		m_userPass = enc.encryptPassword(pass);
		String sqlUpdPass =
			  " UPDATE cmuser SET"
			+ "        user_pass = ?"
			+ "      , chg_pass_date = ?"
			+ " WHERE user_id = ?";
		StdCalendar today = new StdCalendar();
		dbu.executeList(sqlUpdPass, m_userPass, today.toDbDateString(), m_userId);
		dbu.doCommit();
		m_chgPassDate = today;
		m_needChgPass = false;
	}
	
	public void updateFailPass(boolean reset) {
		DbUtil dbu = new DbUtil();
		try {
			if (reset) {
				m_failPass = 0;
				String sqlUpdUser =
					  " UPDATE cmuser SET"
					+ "        fail_pass = 0"
					+ " WHERE user_id = ?";
				dbu.executeList(sqlUpdUser, m_userId);
			}
			else {
				String sqlUpdUser =
					  " UPDATE cmuser SET"
					+ "        fail_pass = fail_pass + 1"
					+ " WHERE user_id = ?";
				dbu.executeList(sqlUpdUser, m_userId);
				String sqlQryUser = " SELECT fail_pass FROM cmuser WHERE user_id = ?";
				m_failPass = dbu.selectIntList(sqlQryUser, m_userId);
			}
			dbu.doCommit();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		dbu.relDbConn();
	}
	
	public boolean doesExist() {
		return m_exists;
	}
	
	public boolean checkValid() {
		String today = new StdCalendar().toDbDateString();
		if (today.compareTo(m_beginDate) < 0)
			return false;
		if (today.compareTo(m_endDate) > 0)
			return false;
		return true;
	}
	
	public String getUserId() {
		return m_userId;
	}
	
	public String getUserName() {
		return m_userName;
	}
	
	public StdCalendar getChgPassDate() {
		return (StdCalendar) m_chgPassDate.clone();
	}
	
	public int getFailPass() {
		return m_failPass;
	}
	
	public boolean getNeedChgPass() {
		return m_needChgPass;
	}
	
	public MenuSvc.MenuItem getMenu() {
		return m_menu;
	}

	public String getPrivBase(String progId) {
		if (m_privMap.containsKey(progId))
			return m_privMap.get(progId).privBase;
		return ProgData.getProgData().getProgPrivBase(progId);
	}	

	public String getPrivAux(String progId) {
		if (m_privMap.containsKey(progId))
			return m_privMap.get(progId).privAux;
		return ProgData.getProgData().getProgPrivAux(progId);
	}	
	
	public boolean checkPrivAux(String progId, String priv) {
		String privAux = getPrivAux(progId);
		if (privAux == null || privAux.isEmpty())
			return false;
		if (privAux.indexOf(priv) < 0)
			return false;
		return true;
		
	}
	
	public String getPrivDesc(String progId) {
		String desc = m_privBaseDescMap.get(getPrivBase(progId));
		String aux = getPrivAux(progId);
		if (!aux.isEmpty())
			desc += " [" + aux + "]";
		return desc;
	}
}
