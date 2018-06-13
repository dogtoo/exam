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
import com.ron.exam.service.ParamSvc;
import com.ron.exam.service.ProgData;
import com.ron.exam.service.UserData;

@EnableWebMvc
@Controller
public class MntMemberAction {

	private static final String c_progId = "MntMember";
	
	public static String chkUserIdInUse(DbUtil dbu, String userIdOrg) throws SQLException {
		String sqlCntQsCnvr = " SELECT COUNT(*) FROM qscnvr WHERE cr_user_id = ? OR cnfrm_user_id = ?";
		if (dbu.selectIntList(sqlCntQsCnvr, userIdOrg, userIdOrg) > 0)
			return "原帳號在教案版本資料使用中，不可以刪除";
		String sqlCntQsFile = " SELECT COUNT(*) FROM qsfile WHERE cr_user_id = ?";
		if (dbu.selectIntList(sqlCntQsFile, userIdOrg) > 0)
			return "原帳號在教案檔案資料使用中，不可以刪除";
		String sqlCntQsMstr = " SELECT COUNT(*) FROM qsmstr WHERE cr_user_id = ?";
		if (dbu.selectIntList(sqlCntQsMstr, userIdOrg) > 0)
			return "原帳號在教案創建人資料使用中，不可以刪除";
		String sqlCntQsNote = " SELECT COUNT(*) FROM qsnote WHERE cr_user_id = ?";
		if (dbu.selectIntList(sqlCntQsNote, userIdOrg) > 0)
			return "原帳號在教案註記資料使用中，不可以刪除";
		String sqlCntTmplFile = " SELECT COUNT(*) FROM qstpfl WHERE cr_user_id = ?";
		if (dbu.selectIntList(sqlCntTmplFile, userIdOrg) > 0)
			return "原帳號在教案樣版檔案資料使用中，不可以刪除";
		String sqlCntQsTmpl = " SELECT COUNT(*) FROM qstpms WHERE mod_user_id = ?";
		if (dbu.selectIntList(sqlCntQsTmpl, userIdOrg) > 0)
			return "原帳號在教案樣版資料使用中，不可以刪除";
		String sqlCntQsUser = " SELECT COUNT(*) FROM qsuser WHERE user_id = ?";
		if (dbu.selectIntList(sqlCntQsUser, userIdOrg) > 0)
			return "原帳號在教案人員檔案資料使用中，不可以刪除";
		String sqlCntQsValid = " SELECT COUNT(*) FROM qsvlfl WHERE user_id = ?";
		if (dbu.selectIntList(sqlCntQsValid, userIdOrg) > 0)
			return "原帳號在教案審核檔案資料使用中，不可以刪除";
		return null;
	}
	
	public void updateUserId(DbUtil dbu, String userId, String userIdOrg) throws SQLException {
		String sqlCntQsCnvr1 = " UPDATE qscnvr SET cr_user_id = ? WHERE cr_user_id = ?";
		dbu.executeList(sqlCntQsCnvr1, userId, userIdOrg);
		String sqlCntQsCnvr2 = " UPDATE qscnvr SET cnfrm_user_id = ? WHERE cnfrm_user_id = ?";
		dbu.executeList(sqlCntQsCnvr2, userId, userIdOrg);
		String sqlCntQsFile = " UPDATE qsfile SET cr_user_id = ? WHERE cr_user_id = ?";
		dbu.executeList(sqlCntQsFile, userId, userIdOrg);
		String sqlCntQsMstr = " UPDATE qsmstr SET cr_user_id = ? WHERE cr_user_id = ?";
		dbu.executeList(sqlCntQsMstr, userId, userIdOrg);
		String sqlCntQsNote = " UPDATE qsnote SET cr_user_id = ? WHERE cr_user_id = ?";
		dbu.executeList(sqlCntQsNote, userId, userIdOrg);
		String sqlCntTmplFile = " UPDATE qstpfl SET cr_user_id = ? WHERE cr_user_id = ?";
		dbu.executeList(sqlCntTmplFile, userId, userIdOrg);
		String sqlCntQsTmpl = " UPDATE qstpms SET mod_user_id = ? WHERE mod_user_id = ?";
		dbu.executeList(sqlCntQsTmpl, userId, userIdOrg);
		String sqlCntQsUser = " UPDATE qsuser SET user_id = ? WHERE user_id = ?";
		dbu.executeList(sqlCntQsUser, userId, userIdOrg);
		String sqlCntQsValid = " UPDATE qsvlfl SET user_id = ? WHERE user_id = ?";
		dbu.executeList(sqlCntQsValid, userId, userIdOrg);
	}
	
	@RequestMapping(value = "/MntMember", method = RequestMethod.GET)
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

			res.put("departList", ParamSvc.buildSelectOptionByClass(dbu, ParamSvc.c_clsDepart, true));
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
		return "MntMember";
	}

	@RequestMapping(value = "/MntMember_qryMemberList", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryMemberList(@RequestParam Map<String, String> req) {
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
				  " SELECT user_id, user_name, depart_id, user_no, mobile_no, address, email, begin_date, end_date\n");
			StringBuffer sqlCntUser = new StringBuffer(" SELECT COUNT(*)");
			StringBuffer sqlCond = new StringBuffer(
				  "   FROM mbdetl a\n"
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
			String qryDepartId = req.get("departId");
			if (!qryDepartId.isEmpty()) {
				sqlCond.append(" AND depart_id = ?\n");
				params.add(qryDepartId);
			}
			String qryUserNo = req.get("userNo");
			if (!qryUserNo.isEmpty()) {
				sqlCond.append(" AND user_no LIKE '%' || ? || '%'\n");
				params.add(qryUserNo);
			}
			String qryMobileNo = req.get("mobileNo");
			if (!qryMobileNo.isEmpty()) {
				sqlCond.append(" AND mobile_no LIKE '%' || ? || '%'\n");
				params.add(qryMobileNo);
			}
			String qryAddress = req.get("address");
			if (!qryAddress.isEmpty()) {
				sqlCond.append(" AND address LIKE '%' || ? || '%'\n");
				params.add(qryAddress);
			}
			String qryEmail = req.get("email");
			if (!qryEmail.isEmpty()) {
				sqlCond.append(" AND email LIKE '%' || ? || '%'\n");
				params.add(qryEmail);
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
					put("departId:A", "depart_id ASC");
					put("departId:D", "depart_id DESC");
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

			Map<String, String> departMap = ParamSvc.buildStringMapByClass(dbu, "DEPART");
			ResultSet rsUser = dbu.queryArray(sqlQryUser.toString(), params.toArray());
			List<Map<String, Object>> userList = new ArrayList<Map<String, Object>>();
			while (rsUser.next()) {
				Map<String, Object> user = new HashMap<String, Object>();
				user.put("userId", rsUser.getString("user_id"));
				user.put("userName", rsUser.getString("user_name"));
				user.put("departName", departMap.get(rsUser.getString("depart_id")));
				user.put("userNo", DbUtil.nullToEmpty(rsUser.getString("user_no")));
				user.put("mobileNo", DbUtil.nullToEmpty(rsUser.getString("mobile_no")));
				user.put("address", DbUtil.nullToEmpty(rsUser.getString("address")));
				user.put("email", DbUtil.nullToEmpty(rsUser.getString("email")));
				user.put("beginDate", "00000000".equals(rsUser.getString("begin_date")) ? "" : new StdCalendar(rsUser.getString("begin_date")).toDateString());
				user.put("endDate", "99999999".equals(rsUser.getString("end_date")) ? "" : new StdCalendar(rsUser.getString("end_date")).toDateString());
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

	@RequestMapping(value = "/MntMember_qryMember", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryMember(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success", false);
			
			res.put("departList", ParamSvc.buildSelectDataByClass(dbu, "DEPART", false));
			res.put("mbTypeList", CodeSvc.buildSelectDataByKind(dbu, "MBTYPE", false));
			
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

			String userId = req.get("userId");
			if (userId != null) {
				String sqlQryMember =
					  " SELECT user_name, depart_id, user_no, mobile_no, address, email, begin_date, end_date\n"
					+ "   FROM mbdetl\n"
					+ "  WHERE user_id = ?\n";
				Map<String, Object> rowMember = dbu.selectMapRowList(sqlQryMember, userId);
				if (rowMember == null)
					throw new StopException("人員帳號 <" + userId + "> 不存在");
				res.put("userName", (String) rowMember.get("user_name"));
				res.put("departId", (String) rowMember.get("depart_id"));
				res.put("userNo", (String) rowMember.get("user_no"));
				res.put("mobileNo", (String) rowMember.get("mobile_no"));
				res.put("address", (String) rowMember.get("address"));
				res.put("email", (String) rowMember.get("email"));
				res.put("beginDate", "00000000".equals(rowMember.get("begin_date")) ? "" : new StdCalendar((String) rowMember.get("begin_date")).toDateString());
				res.put("endDate", "99999999".equals(rowMember.get("end_date")) ? "" : new StdCalendar((String) rowMember.get("end_date")).toDateString());

				String sqlQryUser =
					  " SELECT init_pass, menu_id, remark\n"
					+ "   FROM cmuser\n"
					+ "  WHERE user_id = ?\n";
				Map<String, Object> rowUser = dbu.selectMapRowList(sqlQryUser, userId);
				if (rowUser != null) {
					res.put("initPass", (String) rowMember.get("init_pass"));
					res.put("menuId", (String) rowMember.get("menu_id"));
					res.put("remark", (String) rowMember.get("remark"));
				}

				String sqlQryUserRole =
					  " SELECT role_id FROM cmusrr WHERE user_id = ?\n";
				res.put("userRoleList", dbu.selectStringAllList(sqlQryUserRole, userId));
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

	@RequestMapping(value = "/MntMember_addMember", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addMember(@RequestParam Map<String, String> req) {
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
			String sqlCntMember = " SELECT COUNT(*) FROM mbdetl WHERE user_id = ?";
			if (dbu.selectIntList(sqlCntMember, userId) > 0)
				throw new StopException("帳號 <" + userId + "> 已存在");
			String userName = req.get("userName");
			if (userName.isEmpty())
				throw new StopException("姓名不可以為空白");
			String departId = req.get("departId");
			String sqlCntDepart = "SELECT COUNT(*) FROM cmparm WHERE param_class = ? AND param_id = ?";
			if (dbu.selectIntList(sqlCntDepart, ParamSvc.c_clsDepart, departId) == 0)
				throw new StopException("科系代碼 <" + departId + "> 不存在");
			String userNo = DbUtil.emptyToNull(req.get("userNo"));
			String mobileNo = DbUtil.emptyToNull(req.get("mobileNo"));
			String address = DbUtil.emptyToNull(req.get("address"));
			String email = DbUtil.emptyToNull(req.get("email"));
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
			boolean alterUser = "Y".equals(req.get("alterUser"));
			String initPass = req.get("initPass");
			String menuId = req.get("menuId");
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
			if (alterUser) {
				String sqlCntUser = " SELECT COUNT(*) FROM cmuser WHERE user_id = ?";
				if (dbu.selectIntList(sqlCntUser, userId) > 0)
					throw new StopException("登入帳號 <" + userId + "> 已存在");
				if (initPass.isEmpty())
					throw new StopException("初始密碼不可以為空白");
				String sqlCntMenu = "SELECT COUNT(*) FROM cmmenu WHERE menu_id = ? AND upper_id = '-'";
				if (dbu.selectIntList(sqlCntMenu, menuId) == 0)
					throw new StopException("選單代碼 <" + menuId + "> 不存在");
			}
			
			String sqlInsMember =
				  " INSERT INTO mbdetl(user_id, user_name, depart_id, user_no, mobile_no,\n"
				+ "        address, email, begin_date, end_date)\n"
				+ " VALUES(?, ?, ?, ?, ?,  ?, ?, ?, ?)\n";
			dbu.executeList(sqlInsMember, userId, userName, departId, userNo, mobileNo,
				address, email, beginDate, endDate);
			if (alterUser) {
				String sqlInsUser =
					  " INSERT INTO cmuser(user_id, user_name, menu_id, init_pass, user_pass,\n"
					+ "        fail_pass, chg_pass_date, begin_date, end_date, remark)\n"
					+ " VALUES(?, ?, ?, ?, null,  0, null, ?, ?, ?)\n";
				dbu.executeList(sqlInsUser, userId, userName, menuId, initPass, beginDate, endDate, remark);
				String sqlInsRole = " INSERT INTO cmusrr(user_id, role_id) VALUES(?, ?)";
				for (int i = 0; i < roleList.size(); i++)
					dbu.executeList(sqlInsRole, userId, roleList.get(i));
			}
			String roleStr = MiscTool.concatList(roleList, ",");
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "addMember");
			operLog.add("userId", userId);
            operLog.add("userName", userName);
            operLog.add("departId", departId);
            operLog.add("userNo", userNo);
            operLog.add("mobileNo", mobileNo);
            operLog.add("address", address);
            operLog.add("email", email);
            operLog.add("beginDate", beginDate);
            operLog.add("endDate", endDate);
            if (alterUser) {
				operLog.add("menuId", menuId);
				operLog.add("initPass", initPass);
	            operLog.add("remark", remark);
	            operLog.add("roleStr", roleStr);
            }
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

	@RequestMapping(value = "/MntMember_modMember", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> modMemberr(@RequestParam Map<String, String> req) {
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
			String sqlCntMember = " SELECT COUNT(*) FROM cmuser WHERE user_id = ?";
			if (dbu.selectIntList(sqlCntMember, userIdOrg) == 0)
				throw new StopException("原帳號 <" + userIdOrg + "> 不存在");
			if (!userId.equals(userIdOrg) && dbu.selectIntList(sqlCntMember, userId) > 0)
				throw new StopException("帳號 <" + userId + "> 已存在");
			String userName = req.get("userName");
			if (userName.isEmpty())
				throw new StopException("姓名不可以為空白");
			String departId = req.get("departId");
			String sqlCntDepart = "SELECT COUNT(*) FROM cmparm WHERE param_class = ? AND param_id = ?";
			if (dbu.selectIntList(sqlCntDepart, ParamSvc.c_clsDepart, departId) == 0)
				throw new StopException("科系代碼 <" + departId + "> 不存在");
			String userNo = DbUtil.emptyToNull(req.get("userNo"));
			String mobileNo = DbUtil.emptyToNull(req.get("mobileNo"));
			String address = DbUtil.emptyToNull(req.get("address"));
			String email = DbUtil.emptyToNull(req.get("email"));
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
			boolean alterUser = "Y".equals(req.get("alterUser"));
			String initPass = req.get("initPass");
			String menuId = req.get("menuId");
			String remark = DbUtil.emptyToNull(req.get("remark"));
			List<String> roleList = new ArrayList<String>();
			String roleStr = "";
			if (alterUser) {
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
				String sqlCntUser = " SELECT COUNT(*) FROM cmuser WHERE user_id = ?";
				if (dbu.selectIntList(sqlCntUser, userIdOrg) == 0)
					throw new StopException("原登入帳號 <" + userIdOrg + "> 不存在");
				if (!userId.equals(userIdOrg) && dbu.selectIntList(sqlCntUser, userId) > 0)
					throw new StopException("登入帳號 <" + userId + "> 已存在");
				if (initPass.isEmpty())
					throw new StopException("初始密碼不可以為空白");
				String sqlCntMenu = "SELECT COUNT(*) FROM cmmenu WHERE menu_id = ? AND upper_id = '-'";
				if (dbu.selectIntList(sqlCntMenu, menuId) == 0)
					throw new StopException("選單代碼 <" + menuId + "> 不存在");
				roleStr = MiscTool.concatList(roleList, ",");
			}
			
			String sqlQryMember =
				  " SELECT user_name, depart_id, user_no, mobile_no, address, email, begin_date, end_date\n"
				+ "   FROM mbdetl\n"
				+ "  WHERE user_id = ?\n";
			Map<String, Object> rowMember = dbu.selectMapRowList(sqlQryMember, userIdOrg);
			if (rowMember == null)
				throw new StopException("原帳號 <" + userIdOrg + "> 不存在");
			String userNameOrg = (String) rowMember.get("user_name");
			String departIdOrg = (String) rowMember.get("depart_id");
			String userNoOrg = (String) rowMember.get("user_no");
			String mobileNoOrg = (String) rowMember.get("mobile_no");
			String addressOrg = (String) rowMember.get("address");
			String emailOrg = (String) rowMember.get("email");
			String beginDateOrg = (String) rowMember.get("begin_date");
			String endDateOrg = (String) rowMember.get("end_date");
			String menuIdOrg = null;
			String initPassOrg = null;
			String remarkOrg = null;
			String roleStrOrg = "";
			if (alterUser) {
				String sqlQryUser =
					  " SELECT menu_id, init_pass, remark\n"
					+ "   FROM cmuser\n"
					+ "  WHERE user_id = ?\n";
				Map<String, Object> rowUser = dbu.selectMapRowList(sqlQryUser, userIdOrg);
				if (rowUser == null)
					throw new StopException("原登入帳號 <" + userIdOrg + "> 不存在");
				menuIdOrg = (String) rowUser.get("menu_id");
				initPassOrg = (String) rowUser.get("init_pass");
				remarkOrg = (String) rowUser.get("remark");
				String sqlQryRole = " SELECT role_id FROM cmusrr WHERE user_id = ? ORDER BY role_id";
				roleStrOrg = MiscTool.concatList(dbu.selectStringAllList(sqlQryRole, userIdOrg), ",");
			}
			
			String sqlUpdMember =
				  " UPDATE mbdetl SET\n"
				+ "        user_id = ?\n"
				+ "      , user_name = ?\n"
				+ "      , depart_id = ?\n"
				+ "      , user_no = ?\n"
				+ "      , mobile_no = ?\n"
				+ "      , address = ?\n"
				+ "      , email = ?\n"
				+ "      , begin_date = ?\n"
				+ "      , end_date = ?\n"
				+ "  WHERE user_id = ?\n";
			dbu.executeList(sqlUpdMember, userId, userName, departId, userNo, mobileNo, address, email,
				beginDate, endDate, userIdOrg);
			if (!userIdOrg.equals(userId)) {
				// 變更 userId 一併修改相關資料
				updateUserId(dbu, userId, userIdOrg);
			}
			if (alterUser) {
				String sqlUpdUser =
					  " UPDATE cmuser SET\n"
					+ "        user_id = ?\n"
					+ "      , user_name = ?\n"
					+ "      , menu_id = ?\n"
					+ "      , init_pass = ?\n"
					+ "      , begin_date = ?\n"
					+ "      , end_date = ?\n"
					+ "      , remark = ?\n"
					+ "  WHERE user_id = ?\n";
				dbu.executeList(sqlUpdUser, userId, userName, menuId, initPass, beginDate, endDate, remark, userIdOrg);
				String sqlDelRole = " DELETE FROM cmusrr WHERE user_id = ?";
				dbu.executeList(sqlDelRole, userIdOrg);
				String sqlInsRole = " INSERT INTO cmusrr(user_id, role_id) VALUES(?, ?)";
				for (int i = 0; i < roleList.size(); i++)
					dbu.executeList(sqlInsRole, userId, roleList.get(i));
			}
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "modMember");
			if (userId.equals(userIdOrg))
				operLog.add("userId", userId);
			else
				operLog.add("userId", userIdOrg, userId);
            operLog.add("userName", userNameOrg, userName);
			operLog.add("departId", departIdOrg, departId);
            operLog.add("userNo", userNoOrg, userNo);
            operLog.add("mobileNo", mobileNoOrg, mobileNo);
            operLog.add("address", addressOrg, address);
            operLog.add("email", emailOrg, email);
            operLog.add("beginDate", beginDateOrg, beginDate);
            operLog.add("endDate", endDateOrg, endDate);
            if (alterUser) {
				operLog.add("menuId", menuIdOrg, menuId);
				operLog.add("initPass", initPassOrg, initPass);
	            operLog.add("remark", remarkOrg, remark);
	            operLog.add("roleStr", roleStrOrg, roleStr);
            }
            operLog.write();
			
            res.put("success", true);
			res.put("status", "修改人員完成");
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

	@RequestMapping(value = "/MntMember_delMember", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> delMember(@RequestParam Map<String, String> req) {
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
			String sqlCntMember = " SELECT COUNT(*) FROM mbdetl WHERE user_id = ?";
			if (dbu.selectIntList(sqlCntMember, userId) == 0)
				throw new StopException("帳號 <" + userId + "> 不存在");
			String relaMsg = chkUserIdInUse(dbu, userId);
			if (relaMsg != null)
				throw new StopException(relaMsg);
			boolean alterUser = "Y".equals(req.get("alterUser"));
			if (alterUser) {
				String sqlCntUser = " SELECT COUNT(*) FROM cmuser WHERE user_id = ?";
				if (dbu.selectIntList(sqlCntUser, userId) == 0)
					throw new StopException("登入帳號 <" + userId + "> 不存在");
			}

			String sqlDelMember =
				  " DELETE FROM mbdetl WHERE user_id = ?";
			dbu.executeList(sqlDelMember, userId);
			if (alterUser) {
				String sqlDelUser =
					  " DELETE FROM cmuser WHERE user_id = ?";
				dbu.executeList(sqlDelUser, userId);
				String sqlDelRole =
					  " DELETE FROM cmusrr WHERE user_id = ?";
				dbu.executeList(sqlDelRole, userId);
				String sqlDelPriv =
					  " DELETE FROM cmpriv WHERE acc_type = 'U' AND acc_id = ?";
				dbu.executeList(sqlDelPriv, userId);
			}
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "delMember");
			operLog.add("userId", userId);
            operLog.write();;
			
            res.put("success", true);
			res.put("status", "刪除人員完成");
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
}
