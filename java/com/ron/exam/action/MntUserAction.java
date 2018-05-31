package com.ron.exam.action;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import com.ron.exam.util.StdCalendar;
import com.ron.exam.util.StopException;
import com.ron.exam.util.DbUtil;
import com.ron.exam.util.ExceptionUtil;
import com.ron.exam.util.OperLog;
import com.ron.exam.service.CodeSvc;
import com.ron.exam.service.MiscTool;
import com.ron.exam.service.ProgData;
import com.ron.exam.service.UserData;

@EnableWebMvc
@Controller
public class MntUserAction {

	private static final String c_progId = "MntUser";
	
	@RequestMapping(value = "/MntUser", method = RequestMethod.GET)
	public String execute(Model model, HttpSession sess) {
		Map<String, Object> res = new HashMap<String, Object>();
		DbUtil dbu = new DbUtil();
		try {
			res.put("status", "");
			res.put("statusTime", new StdCalendar().toTimesString());
			
			UserData ud = UserData.getUserData();
			ProgData pd = ProgData.getProgData();
			res.put("progId", c_progId);
			res.put("privDesc", ud.getPrivDesc(c_progId));
			res.put("progTitle", pd.getProgTitle(c_progId));
			res.put("queryHide", ProgData.c_privBaseQuery.equals(ud.getPrivBase(c_progId)) ? "visibility: hidden;" : "");
			
			String sqlQryMenu =
				  " SELECT menu_id, menu_desc FROM cmmenu WHERE upper_id = '-' ORDER BY show_order";
			res.put("menuList", MiscTool.buildSelectOptionBySql(dbu, sqlQryMenu, "menu_id", "menu_desc", true));
			String sqlQryRole =
				  " SELECT role_id, role_name FROM cmrole ORDER BY role_id";
			res.put("roleList", MiscTool.buildSelectOptionBySql(dbu, sqlQryRole, "role_id", "role_name", false));
		}
//		catch (StopException e) {
//			res.put("status", e.getMessage());
//		}
		catch (SQLException e) {
			res.put("status", DbUtil.exceptionTranslation(e));
		}
		catch (Exception e) {
			res.put("status", ExceptionUtil.procExceptionMsg(e));
		}
		dbu.relDbConn();
		
		Iterator<Entry<String, Object>> resi = res.entrySet().iterator();
		while (resi.hasNext()) {
			Entry<String, Object> rese = resi.next();
			model.addAttribute(rese.getKey(), rese.getValue());
		}
		return "MntUser";
	}

	@RequestMapping(value = "/MntUser_qryUserList", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryUserList(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success",  false);
			String mode = req.get("mode");
			int pageRow = Integer.MAX_VALUE;
			int pageAt = 0;
			try {
				pageRow = Integer.parseInt(req.get("pageRow"));
				if (pageRow <= 0)
					pageRow = Integer.MAX_VALUE;
			}
			catch (Exception e) {
			}
			try {
				pageAt = Integer.parseInt(req.get("pageAt")) - 1;
				if (pageAt < 0)
					pageAt = 0;
			}
			catch (Exception e) {
			}
			if ("N".equals(mode))
				pageAt = 0;
			
			StringBuffer sqlQryUser = new StringBuffer(
				  " SELECT user_id, user_name, menu_id, user_pass, fail_pass, begin_date, end_date\n");
			StringBuffer sqlCntUser = new StringBuffer(" SELECT COUNT(*)");
			StringBuffer sqlCond = new StringBuffer(
				  "   FROM cmuser a\n"
				+ "  WHERE 1 = 1\n");
			List<Object> params = new ArrayList<Object>();
			String qryUserId = req.get("userId").toUpperCase();
			if (!qryUserId.isEmpty()) {
				sqlCond.append(" AND user_id LIKE '%' || ? || '%'\n");
				params.add(qryUserId);
			}
			String qryUserName = req.get("userName");
			if (!qryUserName.isEmpty()) {
				sqlCond.append(" AND user_name LIKE '%' || ? || '%'\n");
				params.add(qryUserName);
			}
			String qryMenuId = req.get("menuId");
			if (!qryMenuId.isEmpty()) {
				sqlCond.append(" AND menu_id = ?\n");
				params.add(qryMenuId);
			}
			String qryBeginMode = req.get("beginMode");
			if ("N".equals(qryBeginMode))
				sqlCond.append(" AND begin_date = '00000000'\n");
			else if ("V".equals(qryBeginMode))
				sqlCond.append(" AND begin_date <> '00000000'\n");
			else if ("B".equals(qryBeginMode)) {
				sqlCond.append(" AND begin_date > ?\n");
				params.add(new StdCalendar().toDbDateString());
			}
			else if ("A".equals(qryBeginMode)) {
				sqlCond.append(" AND begin_date <= ?\n");
				params.add(new StdCalendar().toDbDateString());
			}
			String qryEndMode = req.get("endMode");
			if ("N".equals(qryEndMode))
				sqlCond.append(" AND end_date = '99999999'\n");
			else if ("V".equals(qryEndMode))
				sqlCond.append(" AND end_date <> '99999999'\n");
			else if ("B".equals(qryEndMode)) {
				sqlCond.append(" AND end_date >= ?\n");
				params.add(new StdCalendar().toDbDateString());
			}
			else if ("A".equals(qryEndMode)) {
				sqlCond.append(" AND end_date < ?\n");
				params.add(new StdCalendar().toDbDateString());
			}
			List<String> roleList = new ArrayList<String>();
			for (int i = 0; ; i++) {
				String key = "roleId[" + i + "]";
				if (!req.containsKey(key))
					break;
				roleList.add(req.get(key));
			}
			if (roleList.size() > 0) {
				sqlCond.append(" AND EXISTS(SELECT * FROM cmusrr b WHERE a.user_id = b.user_id AND role_id IN ");
				DbUtil.buildInSqlParam(sqlCond, params, roleList);
				sqlCond.append(")\n");
			}
			
			// 非換頁查詢時，需先查詢總筆數
			if ("N".equals(mode) || "T".equals(mode)) {
				sqlCntUser.append(sqlCond);
				res.put("total", dbu.selectIntArray(sqlCntUser.toString(), params.toArray()));
			}
			
			sqlQryUser.append(sqlCond);
			StringBuffer sqlOrder = new StringBuffer();
			Map<String, String> orderMap = new HashMap<String, String>() {
				private static final long serialVersionUID = 1l;
				{	put("userId:A", "user_id ASC");
					put("userId:D", "user_id DESC");
					put("userName:A", "user_name ASC");
					put("userName:D", "user_name DESC");
					put("menuId:A", "menu_id ASC");
					put("menuId:D", "menu_id DESC");
					put("beginDate:A", "begin_date ASC");
					put("beginDate:D", "begin_date DESC");
					put("endDate:A", "end_date ASC");
					put("endDate:D", "end_date DESC");
				}
			};
			for (int i = 0; ; i++) {
				String key = "order[" + i + "]";
				if (!req.containsKey(key))
					break;
				String order = req.get(key);
				if (!orderMap.containsKey(order))
					break;
				if (i == 0)
					sqlOrder.append(" ORDER BY ");
				else
					sqlOrder.append(", ");
				sqlOrder.append(orderMap.get(order));
			}
			sqlQryUser.append(sqlOrder);
			if (pageRow != 0) {
				sqlQryUser.append(" OFFSET ? LIMIT ?\n");
				params.add(pageAt * pageRow);
				params.add(pageRow);
			}

			Map<String, String> menuMap = dbu.selectKeyStringList("SELECT menu_id, menu_desc FROM cmmenu WHERE upper_id = '-'", "menu_id", "menu_desc");
			Map<String, String> roleMap = dbu.selectKeyStringList("SELECT role_id, role_name FROM cmrole", "role_id", "role_name");
			ResultSet rsUser = dbu.queryArray(sqlQryUser.toString(), params.toArray());
			List<Map<String, Object>> userList = new ArrayList<Map<String, Object>>();
			while (rsUser.next()) {
				Map<String, Object> user = new HashMap<String, Object>();
				user.put("userId", rsUser.getString("user_id"));
				user.put("userName", rsUser.getString("user_name"));
				user.put("menuDesc", menuMap.get(rsUser.getString("menu_id")));
				user.put("beginDate", "00000000".equals(rsUser.getString("begin_date")) ? "" : new StdCalendar(rsUser.getString("begin_date")).toDateString());
				user.put("endDate", "99999999".equals(rsUser.getString("end_date")) ? "" : new StdCalendar(rsUser.getString("end_date")).toDateString());
				String sqlQryRole = " SELECT role_id FROM cmusrr WHERE user_id = ? ORDER BY role_id";
				ResultSet rsRole = dbu.queryList(sqlQryRole, rsUser.getString("user_id"));
				StringBuffer roleName = new StringBuffer();
				while (rsRole.next()) {
					if (roleName.length() > 0)
						roleName.append(',');
					roleName.append(roleMap.get(rsRole.getString("role_id")));
				}
				rsRole.close();
				user.put("roleName", roleName.toString());
				String userStatus = "";
				if (rsUser.getString("user_pass") == null)
					userStatus += "初密 ";
				if (rsUser.getInt("fail_pass") >= 5)
					userStatus += "鎖住 ";
				user.put("status", userStatus);
				userList.add(user);
			}
			rsUser.close();
			res.put("userList", userList);
			
			res.put("success", true);
			res.put("status", "查詢人員列表完成");
		}
//		catch (StopException e) {
//			res.put("status", e.getMessage());
//		}
		catch (SQLException e) {
			res.put("status", DbUtil.exceptionTranslation(e));
		}
		catch (Exception e) {
			res.put("status", ExceptionUtil.procExceptionMsg(e));
		}
		dbu.relDbConn();
		
		return res;
	}

	@RequestMapping(value = "/MntUser_qryUser", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryUser(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success", false);
			
			String sqlQryMenu =
				  " SELECT menu_id \"value\", menu_desc \"text\"\n"
				+ "   FROM cmmenu\n"
				+ "  WHERE upper_id = '-'\n"
				+ "  ORDER BY show_order\n";
			res.put("menuList", dbu.selectMapAllList(sqlQryMenu));
			
			String sqlQryRole =
				  " SELECT false \"sel\", role_id \"roleId\", role_name \"roleName\"\n"
				+ "   FROM cmrole\n"
				+ "  ORDER BY role_id\n";
			res.put("roleList", dbu.selectMapAllList(sqlQryRole));

			String sqlQryProg =
				  " SELECT code_desc sys_desc, prog_id, prog_desc, def_priv_base, def_priv_aux\n"
				+ "      , all_priv_aux, priv_aux_desc\n"
				+ "   FROM cmprog a, cmcode b\n"
				+ "  WHERE b.code_kind = 'SYS'\n"
				+ "    AND a.sys_id = b.code_code\n"
				+ "    AND a.show_order >= 0\n"
				+ "  ORDER BY b.code_order, a.show_order\n";
			ResultSet rsProg = dbu.queryList(sqlQryProg);
			Map<String, String> privMap = CodeSvc.buildStringMapByKind(dbu, "PRIVBASE"); 
			List<Map<String, Object>> progList = new ArrayList<Map<String, Object>>();
			while (rsProg.next()) {
				Map<String, Object> prog = new HashMap<String, Object>();
				prog.put("sysDesc", rsProg.getString("sys_desc"));
				prog.put("progId", rsProg.getString("prog_id"));
				prog.put("progDesc", rsProg.getString("prog_desc"));
				prog.put("defPrivBase", rsProg.getString("def_priv_base"));
				prog.put("defPrivAux", DbUtil.nullToEmpty(rsProg.getString("def_priv_aux")));
				prog.put("allPrivAux", DbUtil.nullToEmpty(rsProg.getString("all_priv_aux")));
				prog.put("privAuxDesc", DbUtil.nullToEmpty(rsProg.getString("priv_aux_desc")));
				String privDesc = privMap.get(rsProg.getString("def_priv_base"));
				if (!DbUtil.nullToEmpty(rsProg.getString("all_priv_aux")).isEmpty())
					privDesc += " [" + rsProg.getString("all_priv_aux") + "]";
				prog.put("privDesc", privDesc);
				progList.add(prog);
			}
			rsProg.close();
			res.put("progList", progList);

			String userId = req.get("userId");
			if (userId != null) {
				String sqlQryUser =
					  " SELECT user_name, menu_id, init_pass, user_pass, fail_pass, begin_date, end_date, remark\n"
					+ "   FROM cmuser\n"
					+ "  WHERE user_id = ?\n";
				Map<String, Object> rowUser = dbu.selectMapRowList(sqlQryUser, userId);
				if (rowUser == null)
					throw new StopException("人員帳號 <" + userId + "> 不存在");
				res.put("userName", (String) rowUser.get("user_name"));
				res.put("menuId", (String) rowUser.get("menu_id"));
				res.put("initPass", (String) rowUser.get("init_pass"));
				res.put("passStat", rowUser.get("user_pass") == null ? "初始密碼" : "自定密碼");
				res.put("hasPass", rowUser.get("user_pass") != null);
				res.put("lockStat", (Integer) rowUser.get("fail_pass") >= 5 ? "已鎖住" : "未鎖住");
				res.put("lock", (Integer) rowUser.get("fail_pass") >= 5);
				res.put("beginDate", "00000000".equals(rowUser.get("begin_date")) ? "" : new StdCalendar((String) rowUser.get("begin_date")).toDateString());
				res.put("endDate", "99999999".equals(rowUser.get("end_date")) ? "" : new StdCalendar((String) rowUser.get("end_date")).toDateString());
				res.put("remark", (String) rowUser.get("remark"));

				String sqlQryUserRole =
					  " SELECT role_id FROM cmusrr WHERE user_id = ?\n";
				res.put("userRoleList", dbu.selectStringAllList(sqlQryUserRole, userId));

				String sqlQryPriv =
					  " SELECT c.code_desc \"sysDesc\", a.prog_id \"progId\", a.priv_base \"privBase\"\n"
					+ "      , a.priv_aux \"privAux\", COALESCE(b.all_priv_aux, '') \"allPrivAux\"\n"
					+ "      , b.prog_desc \"progDesc\"\n"
					+ "   FROM cmpriv a, cmprog b, cmcode c\n"
					+ "  WHERE a.acc_type = 'U'\n"
					+ "    AND a.acc_id = ?\n"
					+ "    AND a.prog_id = b.prog_id\n"
					+ "    AND c.code_kind = 'SYS'\n"
					+ "    AND b.sys_id = c.code_code\n"
					+ "  ORDER BY c.code_order, b.show_order\n";
				res.put("privList", dbu.selectMapAllList(sqlQryPriv, userId));
			}
			
			dbu.doCommit();
			
			res.put("success", true);
			res.put("status", "查詢人員資料完成");
		}
		catch (StopException e) {
			res.put("status", e.getMessage());
		}
		catch (SQLException e) {
			res.put("status", DbUtil.exceptionTranslation(e));
		}
		catch (Exception e) {
			res.put("status", ExceptionUtil.procExceptionMsg(e));
		}
		dbu.relDbConn();
		
		return res;
	}

	@RequestMapping(value = "/MntUser_addUser", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addUser(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");

			String userId = req.get("userId").toUpperCase();
			String sqlCntUser = " SELECT COUNT(*) FROM cmuser WHERE user_id = ?";
			if (dbu.selectIntList(sqlCntUser, userId) > 0)
				throw new StopException("帳號 <" + userId + "> 已存在");
			String userName = req.get("userName");
			if (userName.isEmpty())
				throw new StopException("姓名不可以為空白");
			String menuId = req.get("menuId");
			String sqlCntMenu = "SELECT COUNT(*) FROM cmmenu WHERE menu_id = ? AND upper_id = '-'";
			if (dbu.selectIntList(sqlCntMenu, menuId) == 0)
				throw new StopException("選單代碼 <" + menuId + "> 不存在");
			String initPass = req.get("initPass");
			if (initPass.isEmpty())
				throw new StopException("初始密碼不可以為空白");
			String beginDate = req.get("beginDate");
			if (!beginDate.isEmpty() && !StdCalendar.isDateFormat(beginDate))
				throw new StopException("不正確的生效日期");
			beginDate = beginDate.isEmpty() ? "00000000" : new StdCalendar(beginDate).toDbDateString();
			String endDate = req.get("endDate");
			if (!endDate.isEmpty() && !StdCalendar.isDateFormat(endDate))
				throw new StopException("不正確的失效日期");
			endDate = endDate.isEmpty() ? "99999999" : new StdCalendar(endDate).toDbDateString();
			if (beginDate.compareTo(endDate) > 0)
				throw new StopException("生效日期不可在失效日期之後");
			String remark = DbUtil.emptyToNull(req.get("remark"));
			List<String> roleList = new ArrayList<String>();
			for (int i = 0; ; i++) {
				String key = "roleId[" + i + "]";
				if (!req.containsKey(key))
					break;
				String roleId = req.get(key);
				String sqlCntRole = "SELECT COUNT(*) FROM cmrole WHERE role_id = ?";
				if (dbu.selectIntList(sqlCntRole, roleId) == 0)
					throw new StopException("角色代碼 <" + roleId + "> 不存在");
				roleList.add(roleId);
			}
			ProgData pd = ProgData.getProgData();
			List<String[]> privList = new ArrayList<String[]>();
			for (int i = 0; ; i++) {
				String key = "progId[" + i + "]";
				if (!req.containsKey(key))
					break;;
				String progId = req.get(key);
				key = "privBase[" + i + "]";
				if (!req.containsKey(key))
					break;
				String privBase = req.get(key);
				if (!ProgData.c_privBaseMaintain.equals(privBase) && !ProgData.c_privBaseQuery.equals(privBase) &&
					!ProgData.c_privBaseProhibit.equals(privBase))
					throw new StopException("不正確的基本權限設定值 <" + privBase + ">");
				key = "privAux[" + i + "]";
				if (!req.containsKey(key))
					break;
				String privAux = req.get(key);
				if (!pd.checkProgAuxAcceptable(progId, privAux))
					throw new StopException("不正確的基本權限設定值 <" + privBase + ">");
				privList.add(new String[] { progId, privBase, privAux });
			}
			
			String sqlInsUser =
				  " INSERT INTO cmuser(user_id, user_name, menu_id, init_pass, user_pass,\n"
				+ "        fail_pass, chg_pass_date, begin_date, end_date, remark)\n"
				+ " VALUES(?, ?, ?, ?, null,  0, null, ?, ?, ?)\n";
			dbu.executeList(sqlInsUser, userId, userName, menuId, initPass, beginDate, endDate, remark);
			String sqlInsRole = " INSERT INTO cmusrr(user_id, role_id) VALUES(?, ?)";
			for (int i = 0; i < roleList.size(); i++)
				dbu.executeList(sqlInsRole, userId, roleList.get(i));
			String roleStr = MiscTool.concatList(roleList, ",");
			StringBuffer privStr = new StringBuffer();
			for (int i = 0; i < privList.size(); i++) {
				String priv[] = privList.get(i);
				String progId = priv[0];
				String privBase = priv[1];
				String privAux = priv[2];
				String sqlInsPriv =
					  " INSERT INTO cmpriv(acc_type, acc_id, prog_id, priv_base, priv_aux)\n"
					+ " VALUES('U', ?, ?, ?, ?)\n";
				dbu.executeList(sqlInsPriv, userId, progId, privBase, privAux);
				privStr.append("(" + progId + "," + privBase + "," + privAux + ")");
			}
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "addUser");
			operLog.add("userId", userId);
            operLog.add("userName", userName);
			operLog.add("menuId", menuId);
			operLog.add("initPass", initPass);
            operLog.add("beginDate", beginDate);
            operLog.add("endDate", endDate);
            operLog.add("roleStr", roleStr);
            operLog.add("privStr", privStr);
            operLog.add("remark", remark);
            operLog.write();
			
            res.put("success", true);
			res.put("status", "新增人員完成");
		}
		catch (StopException e) {
			res.put("status", e.getMessage());
		}
		catch (SQLException e) {
			res.put("status", DbUtil.exceptionTranslation(e));
		}
		catch (Exception e) {
			res.put("status", ExceptionUtil.procExceptionMsg(e));
		}
		dbu.relDbConn();
		
		return res;
	}

	@RequestMapping(value = "/MntUser_modUser", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> modUser(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			
			String userIdOrg = req.get("userIdOrg").toUpperCase();
			String userId = req.get("userId").toUpperCase();
			String sqlCntUser = " SELECT COUNT(*) FROM cmuser WHERE user_id = ?";
			if (dbu.selectIntList(sqlCntUser, userIdOrg) == 0)
				throw new StopException("原帳號 <" + userIdOrg + "> 不存在");
			if (!userId.equals(userIdOrg) && dbu.selectIntList(sqlCntUser, userId) > 0)
				throw new StopException("帳號 <" + userId + "> 已存在");
			String userName = req.get("userName");
			if (userName.isEmpty())
				throw new StopException("姓名不可以為空白");
			String menuId = req.get("menuId");
			String sqlCntMenu = "SELECT COUNT(*) FROM cmmenu WHERE menu_id = ? AND upper_id = '-'";
			if (dbu.selectIntList(sqlCntMenu, menuId) == 0)
				throw new StopException("選單代碼 <" + menuId + "> 不存在");
			String initPass = req.get("initPass");
			if (initPass.isEmpty())
				throw new StopException("初始密碼不可以為空白");
			boolean passReset = "Y".equals(req.get("passReset"));
			boolean lockReset = "Y".equals(req.get("lockReset"));
			String beginDate = req.get("beginDate");
			if (!beginDate.isEmpty() && !StdCalendar.isDateFormat(beginDate))
				throw new StopException("不正確的生效日期");
			beginDate = beginDate.isEmpty() ? "00000000" : new StdCalendar(beginDate).toDbDateString();
			String endDate = req.get("endDate");
			if (!endDate.isEmpty() && !StdCalendar.isDateFormat(endDate))
				throw new StopException("不正確的失效日期");
			endDate = endDate.isEmpty() ? "99999999" : new StdCalendar(endDate).toDbDateString();
			if (beginDate.compareTo(endDate) > 0)
				throw new StopException("生效日期不可在失效日期之後");
			String remark = DbUtil.emptyToNull(req.get("remark"));
			List<String> roleList = new ArrayList<String>();
			for (int i = 0; ; i++) {
				String key = "roleId[" + i + "]";
				if (!req.containsKey(key))
					break;
				String roleId = req.get(key);
				String sqlCntRole = "SELECT COUNT(*) FROM cmrole WHERE role_id = ?";
				if (dbu.selectIntList(sqlCntRole, roleId) == 0)
					throw new StopException("角色代碼 <" + roleId + "> 不存在");
				roleList.add(roleId);
			}
			ProgData pd = ProgData.getProgData();
			List<String[]> privList = new ArrayList<String[]>();
			for (int i = 0; ; i++) {
				String key = "progId[" + i + "]";
				if (!req.containsKey(key))
					break;;
				String progId = req.get(key);
				key = "privBase[" + i + "]";
				if (!req.containsKey(key))
					break;
				String privBase = req.get(key);
				if (!ProgData.c_privBaseMaintain.equals(privBase) && !ProgData.c_privBaseQuery.equals(privBase) &&
					!ProgData.c_privBaseProhibit.equals(privBase))
					throw new StopException("不正確的基本權限設定值 <" + privBase + ">");
				key = "privAux[" + i + "]";
				if (!req.containsKey(key))
					break;
				String privAux = req.get(key);
				if (!pd.checkProgAuxAcceptable(progId, privAux))
					throw new StopException("不正確的基本權限設定值 <" + privBase + ">");
				privList.add(new String[] { progId, privBase, privAux });
			}
			
			String sqlQryUser =
				  " SELECT user_name, menu_id, init_pass, user_pass, fail_pass, chg_pass_date\n"
				+ "      , begin_date, end_date, remark\n"
				+ "   FROM cmuser\n"
				+ "  WHERE user_id = ?\n";
			Map<String, Object> rowUser = dbu.selectMapRowList(sqlQryUser, userIdOrg);
			if (rowUser == null)
				throw new StopException("原帳號 <" + userIdOrg + "> 不存在");
			String userNameOrg = (String) rowUser.get("user_name");
			String menuIdOrg = (String) rowUser.get("menu_id");
			String initPassOrg = (String) rowUser.get("init_pass");
			String userPassOrg = (String) rowUser.get("user_pass");
			int failPassOrg = (Integer) rowUser.get("fail_pass");
			String chgPassDateOrg = (String) rowUser.get("chg_pass_date");
			String beginDateOrg = (String) rowUser.get("begin_date");
			String endDateOrg = (String) rowUser.get("end_date");
			String remarkOrg = (String) rowUser.get("remark");
			String sqlQryRole = " SELECT role_id FROM cmusrr WHERE user_id = ? ORDER BY role_id";
			String roleStrOrg = MiscTool.concatList(dbu.selectStringAllList(sqlQryRole, userIdOrg), ",");
			String sqlQryPriv =
				  " SELECT a.prog_id, a.priv_base, a.priv_aux\n"
				+ "   FROM cmpriv a, cmprog b, cmcode c\n"
				+ "  WHERE a.acc_type = 'U'\n"
				+ "    AND a.acc_id = ?\n"
				+ "    AND a.prog_id = b.prog_id\n"
				+ "    AND c.code_kind = 'SYS'\n"
				+ "    AND b.sys_id = c.code_code\n"
				+ "  ORDER BY c.code_order, b.show_order\n";
			ResultSet rsPriv = dbu.queryList(sqlQryPriv, userIdOrg);
			StringBuffer privStrOrg = new StringBuffer();
			while (rsPriv.next()) {
				String progId = rsPriv.getString("prog_id");
				String privBase = rsPriv.getString("priv_base");
				String privAux = rsPriv.getString("priv_aux");
				privStrOrg.append("(" + progId + "," + privBase + "," + privAux + ")");
			}
			rsPriv.close();
			// 處理重置密碼
			String userPass = userPassOrg;
			String chgPassDate = chgPassDateOrg;
			int failPass = failPassOrg;
			if (passReset)
				userPass = chgPassDate = null;
			// 處理重置鎖定
			if (lockReset)
				failPass = 0;
			
			String sqlUpdUser =
				  " UPDATE cmuser SET\n"
				+ "        user_id = ?\n"
				+ "      , user_name = ?\n"
				+ "      , menu_id = ?\n"
				+ "      , init_pass = ?\n"
				+ "      , user_pass = ?\n"
				+ "      , fail_pass = ?\n"
				+ "      , chg_pass_date = ?\n"
				+ "      , begin_date = ?\n"
				+ "      , end_date = ?\n"
				+ "      , remark = ?\n"
				+ "  WHERE user_id = ?\n";
			dbu.executeList(sqlUpdUser, userId, userName, menuId, initPass, userPass, failPass,
				chgPassDate, beginDate, endDate, remark, userIdOrg);
			String sqlDelRole = " DELETE FROM cmusrr WHERE user_id = ?";
			dbu.executeList(sqlDelRole, userIdOrg);
			String sqlInsRole = " INSERT INTO cmusrr(user_id, role_id) VALUES(?, ?)";
			for (int i = 0; i < roleList.size(); i++)
				dbu.executeList(sqlInsRole, userId, roleList.get(i));
			String roleStr = MiscTool.concatList(roleList, ",");
			String sqlDelPriv = " DELETE FROM cmpriv WHERE acc_type = 'U' AND acc_id = ?";
			dbu.executeList(sqlDelPriv, userIdOrg);
			StringBuffer privStr = new StringBuffer();
			for (int i = 0; i < privList.size(); i++) {
				String priv[] = privList.get(i);
				String progId = priv[0];
				String privBase = priv[1];
				String privAux = priv[2];
				String sqlInsPriv =
					  " INSERT INTO cmpriv(acc_type, acc_id, prog_id, priv_base, priv_aux)\n"
					+ " VALUES('U', ?, ?, ?, ?)\n";
				dbu.executeList(sqlInsPriv, userId, progId, privBase, privAux);
				privStr.append("(" + progId + "," + privBase + "," + privAux + ")");
			}
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "modUser");
			if (userId.equals(userIdOrg))
				operLog.add("userId", userId);
			else
				operLog.add("userId", userIdOrg, userId);
            operLog.add("userName", userNameOrg, userName);
			operLog.add("menuId", menuIdOrg, menuId);
			operLog.add("initPass", initPassOrg, initPass);
            operLog.add("userPass", userPassOrg, userPass);
            operLog.add("failPass", failPassOrg, failPass);
            operLog.add("beginDate", beginDateOrg, beginDate);
            operLog.add("endDate", endDateOrg, endDate);
            operLog.add("roleStr", roleStrOrg, roleStr);
            operLog.add("privStr", privStrOrg, privStr);
            operLog.add("remark", remarkOrg, remark);
            operLog.write();
			
            res.put("success", true);
			res.put("status", "修改人員完成");
		}
		catch (StopException e) {
			res.put("status", e.getMessage());
		}
//		catch (SQLException e) {
//			res.put("status", DbUtil.exceptionTranslation(e));
//		}
		catch (Exception e) {
			res.put("status", ExceptionUtil.procExceptionMsg(e));
		}
		dbu.relDbConn();
		
		return res;
	}


	@RequestMapping(value = "/MntUser_delUser", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> delUser(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			
			String userId = req.get("userId").toUpperCase();
			String sqlCntUser = " SELECT COUNT(*) FROM cmuser WHERE user_id = ?";
			if (dbu.selectIntList(sqlCntUser, userId) == 0)
				throw new StopException("帳號 <" + userId + "> 不存在");

			String sqlDelUser =
				  " DELETE FROM cmuser WHERE user_id = ?";
			dbu.executeList(sqlDelUser, userId);
			String sqlDelRole =
				  " DELETE FROM cmusrr WHERE user_id = ?";
			dbu.executeList(sqlDelRole, userId);
			String sqlDelPriv =
				  " DELETE FROM cmpriv WHERE acc_type = 'U' AND acc_id = ?";
			dbu.executeList(sqlDelPriv, userId);
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "delUser");
			operLog.add("userId", userId);
            operLog.write();;
			
            res.put("success", true);
			res.put("status", "刪除人員完成");
		}
		catch (StopException e) {
			res.put("status", e.getMessage());
		}
//		catch (SQLException e) {
//			res.put("status", DbUtil.exceptionTranslation(e));
//		}
		catch (Exception e) {
			res.put("status", ExceptionUtil.procExceptionMsg(e));
		}
		dbu.relDbConn();
		
		return res;
	}
}
