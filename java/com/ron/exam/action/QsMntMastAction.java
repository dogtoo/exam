package com.ron.exam.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ron.exam.service.CodeSvc;
import com.ron.exam.service.MailNotify;
import com.ron.exam.service.MemberSvc;
import com.ron.exam.service.MiscTool;
import com.ron.exam.service.ParamSvc;
import com.ron.exam.service.ProgData;
import com.ron.exam.service.QsSvc;
import com.ron.exam.service.UserData;

@EnableWebMvc
@Controller
public class QsMntMastAction {

	private static final String c_progId = "QsMntMast";

	/**
	 * 建立教案目前編輯中版本的檔案清單資料
	 * @param dbu
	 * @param qsId
	 * @return
	 * @throws SQLException
	 */
	private List<Map<String, Object>> buildFileListData(DbUtil dbu, String qsId) throws SQLException {
		String sqlQryFlcls =
			  " SELECT class_id FROM qsclss WHERE qs_id = ? AND class_type = 'F' ORDER BY class_id\n";
		List<String> fileClsList = dbu.selectStringAllList(sqlQryFlcls, qsId);
		String sqlQryFile =
			  " SELECT file_name, file_desc, file_class, show_order, file_size, file_type, cr_user_id, cr_date\n"
			+ "      , (SELECT user_name FROM mbdetl b WHERE a.cr_user_id = b.user_id) cr_user_name\n"
			+ "   FROM qsfile a\n"
			+ "  WHERE qs_id = ?\n"
			+ "    AND ver_seq = (SELECT curr_version FROM qsmstr b WHERE a.qs_id = b.qs_id)\n"
			+ "  ORDER BY show_order\n";
		Map<String, String> fileClassMap = ParamSvc.buildStringMapByClass(dbu, ParamSvc.c_clsQsFileClass);
		ResultSet rsFile = dbu.queryList(sqlQryFile, qsId);			
		List<Map<String, Object>> fileList = new ArrayList<Map<String, Object>>();
		while (rsFile.next()) {					
			Map<String, Object> file = new HashMap<String, Object>();
			file.put("fileName", rsFile.getString("file_name"));
			String fileDesc = rsFile.getString("file_desc");
			int p = fileDesc.lastIndexOf('.');
			if (p >= 0)
				fileDesc = fileDesc.substring(0, p);
			file.put("fileDesc", fileDesc);
			file.put("fileClass", fileClassMap.get(rsFile.getString("file_class")));
			file.put("showOrder", Integer.toString(rsFile.getInt("show_order")));
			file.put("fileSize", MiscTool.convToCommaInt(rsFile.getInt("file_size")));
			file.put("fileType", rsFile.getString("file_type"));
			file.put("userName", DbUtil.nullToEmpty(rsFile.getString("cr_user_name")));
			file.put("crDate", new StdCalendar(rsFile.getString("cr_date")).toDateString());
			fileList.add(file);
			fileClsList.remove(rsFile.getString("file_class"));
		}													
		rsFile.close();
		for (int i = 0; i < fileClsList.size(); i++) {
			Map<String, Object> file = new HashMap<String, Object>();
			file.put("fileName", "");
			file.put("fileClass", fileClassMap.get(fileClsList.get(i)));
			fileList.add(file);
		}
		return fileList;
	}

	/**
	 * 建立教案的人員清單資料
	 * @param dbu
	 * @param qsId
	 * @return
	 * @throws SQLException
	 */
	private List<Map<String, Object>> buildUserListData(DbUtil dbu, String qsId) throws SQLException {
		String sqlQryAdmin =
			  " SELECT COUNT(*) FROM qsuser WHERE qs_id = ? AND role_flag = '" + QsSvc.c_qsRoleAdmin + "' AND end_date = '99999999'";
		boolean multiAdmin = dbu.selectIntList(sqlQryAdmin, qsId) > 1;
		String sqlQryUser =
			  " SELECT seq_no, user_id, role_flag, begin_date, end_date\n"
			+ "      , (SELECT user_name FROM mbdetl b WHERE a.user_id = b.user_id) user_name\n"
			+ "   FROM qsuser a\n"
			+ "  WHERE qs_id = ?\n"
			+ "  ORDER BY seq_no\n";
		Map<String, String> userRoleMap = CodeSvc.buildStringMapByKind(dbu, CodeSvc.c_kindQsUserRole);
		ResultSet rsUser = dbu.queryList(sqlQryUser, qsId);			
		List<Map<String, Object>> userList = new ArrayList<Map<String, Object>>();
		while (rsUser.next()) {					
			Map<String, Object> user = new HashMap<String, Object>();
			user.put("seqNo", Integer.toString(rsUser.getInt("seq_no")));
			user.put("userId", rsUser.getString("user_id"));
			user.put("userName", rsUser.getString("user_name"));
			user.put("roleDesc", userRoleMap.get(rsUser.getString("role_flag")));
			user.put("beginDate", new StdCalendar(rsUser.getString("begin_date")).toDateString());
			user.put("endDate", "99999999".equals(rsUser.getString("end_date")) ? "-" : new StdCalendar(rsUser.getString("end_date")).toDateString());
			user.put("canDel", "99999999".equals(rsUser.getString("end_date")) && (multiAdmin || !QsSvc.c_qsRoleAdmin.equals(rsUser.getString("role_flag"))));
			userList.add(user);
		}													
		rsUser.close();
		return userList;
	}
	
	@RequestMapping(value = "/QsMntMast", method = RequestMethod.GET)
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
			res.put("queryHide", ProgData.c_privBaseQuery.equals(ud.getPrivBase(c_progId)) ? "disabled" : "");

			res.put("departList", ParamSvc.buildSelectOptionByClass(dbu, ParamSvc.c_clsDepart, false));
			res.put("qsTargetList", ParamSvc.buildSelectOptionByClass(dbu, ParamSvc.c_clsQsTarget, false));
			res.put("qsClassList", ParamSvc.buildSelectOptionByClass(dbu, ParamSvc.c_clsQsClass, false));
			res.put("qsAbilityList", ParamSvc.buildSelectOptionByClass(dbu, ParamSvc.c_clsQsAbility, false));
			res.put("qsMastFlagList", CodeSvc.buildSelectOptionByKind(dbu, CodeSvc.c_kindQsMastFlag, true));
			res.put("qsRoleList", CodeSvc.buildSelectOptionByKind(dbu, CodeSvc.c_kindQsUserRole, false));
			ObjectMapper json = new ObjectMapper();
			res.put("qsClassData", json.writeValueAsString(ParamSvc.buildSelectDataByClass(dbu, ParamSvc.c_clsQsClass, false)));
			res.put("qsAbilityData", json.writeValueAsString(ParamSvc.buildSelectDataByClass(dbu, ParamSvc.c_clsQsAbility, false)));
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
		return "QsMntMast";
	}

	/**
	 * 查詢符合帳號、姓名條件的人員
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/QsMntMast_qryMemberList", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryMemberList(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success", false);
			res.put("memberList", MemberSvc.queryMemberList(dbu, req.get("memberCond")));
			dbu.doCommit();
			res.put("success", true);
			res.put("status", "查詢人員資料完成");
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

	/**
	 * 查詢教案列表
	 * 參數 mode:
	 * 		"N" 新查詢，需傳回筆數，查詢結果訊息要顯示
	 * 		"T" 用原本條件查詢，但仍需傳回筆數，查詢結果訊息不顯示
	 * 		"S" 用原本條件查詢，不需傳回筆數，查詢結果訊息不顯示
	 * 		"" 用原本條件查詢，不需傳回筆數，查詢結果訊息要顯示
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/QsMntMast_qryQsList", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryQsList(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success",  false);
			UserData ud = UserData.getUserData();
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
			
			StringBuffer sqlQryQs = new StringBuffer(
				  " SELECT qs_id, qs_name, qs_subject, depart_id, target_id, stat_flag, cr_date, cr_user_id\n"
				+ "      , qstmp_id, file_max_size, curr_version, edit_remark\n"
				+ "      , (SELECT user_name FROM mbdetl b WHERE a.cr_user_id = b.user_id) cr_user_name\n");
			StringBuffer sqlCntQs = new StringBuffer(" SELECT COUNT(*)");
			StringBuffer sqlCond = new StringBuffer(
				  "   FROM qsmstr a\n"
				+ "  WHERE EXISTS(SELECT * FROM qsuser b WHERE a.qs_id = b.qs_id AND b.role_flag = '" + QsSvc.c_qsRoleAdmin + "' AND b.user_id = ? AND b.end_date > ?)\n");
			List<Object> params = new ArrayList<Object>();
			params.add(ud.getUserId());
			params.add(new StdCalendar().toDbDateString());
			
			//教案代碼
			String qryQsId = req.get("qsId");
			if (!qryQsId.isEmpty()) {
				sqlCond.append(" AND qs_id LIKE '%' || ? || '%'\n");
				params.add(qryQsId);
			}
			
			//教案名稱
			String qryQsName = req.get("qsName");
			if (!qryQsName.isEmpty()) {
				sqlCond.append(" AND qs_name LIKE '%' || ? || '%'\n");
				params.add(qryQsName);
			}
			
			//科別
			String qryDepart = req.get("departId");
			if (!qryDepart.isEmpty()) {
				sqlCond.append(" AND depart_id = ?\n");
				params.add(qryDepart);
			}
			
			//對象
			String qryTargetId = req.get("targetId");
			if (!qryTargetId.isEmpty()) {
				sqlCond.append(" AND target_id = ?\n");
				params.add(qryTargetId);
			}
			
			//測驗類別
			List<String> qryQsClassList = new ArrayList<String>();
			for (int i = 0; ; i++) {
				String key = "qryQsClass[" + i + "]";
				if (!req.containsKey(key))
					break;
				qryQsClassList.add(req.get(key));
			}
			if (qryQsClassList.size() > 0) {
				sqlCond.append(" AND EXISTS(SELECT class_id FROM qsclss b WHERE a.qs_id = b.qs_id AND b.class_type = 'C' AND class_id IN ");
				DbUtil.buildInSqlParam(sqlCond, params, qryQsClassList);
				sqlCond.append(")\n");
			}
			
			//核心能力
			List<String> qryQsAbilityList = new ArrayList<String>();
			for (int i = 0; ; i++) {
				String key = "qryQsAbility[" + i + "]";
				if (!req.containsKey(key))
					break;
				qryQsAbilityList.add(req.get(key));
			}
			if (qryQsAbilityList.size() > 0) {
				sqlCond.append(" AND EXISTS(SELECT class_id FROM qsclss b WHERE a.qs_id = b.qs_id AND b.class_type = 'A' AND class_id IN ");
				DbUtil.buildInSqlParam(sqlCond, params, qryQsAbilityList);
				sqlCond.append(")\n");
			}
			
			// 教案狀態
			String qryMastFlag = req.get("mastFlag");
			if (!qryMastFlag.isEmpty()) {
				sqlCond.append(" AND stat_flag = ?\n");
				params.add(qryMastFlag);
			}
			
			//創建人員
			String qryUserId = req.get("userId");
			if (!qryUserId.isEmpty()) {
				sqlCond.append(" AND cr_user_id = ?\n");
				params.add(qryUserId);
			}
			
			//創建日期範圍
			String qryBeginDate = req.get("beginDate");
			if (!qryBeginDate.isEmpty()) {
				sqlCond.append(" AND cr_date >= ?\n");
				params.add(new StdCalendar(qryBeginDate).toDbDateString());
			}

			String qryEndDate = req.get("endDate");
			if (!qryEndDate.isEmpty()) {
				sqlCond.append(" AND cr_date <= ?\n");
				params.add(new StdCalendar(qryEndDate).toDbDateString());
			}
			
			// 非換頁查詢時，需先查詢總筆數
			if ("N".equals(mode) || "T".equals(mode)) {
				sqlCntQs.append(sqlCond);
				res.put("total", dbu.selectIntArray(sqlCntQs.toString(), params.toArray()));
			}
			sqlQryQs.append(sqlCond);
			StringBuffer sqlOrder = new StringBuffer();
			Map<String, String> orderMap = new HashMap<String, String>() {
				private static final long serialVersionUID = 1l;
				{	put("qsId:A", "qs_id ASC");
					put("qsId:D", "qs_id DESC");
					put("qsNamed:A", "qs_name ASC");
					put("qsName:D", "qs_name DESC");
					put("departId:A", "depart_id ASC");
					put("departId:D", "depart_id DESC");
					put("targetId:A", "target_id ASC");
					put("targetId:D", "target_id DESC");
					put("crDate:A", "cr_date ASC");
					put("crDate:D", "cr_date DESC");
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
			sqlQryQs.append(sqlOrder);
			if (pageRow != 0) {
				sqlQryQs.append(" OFFSET ? LIMIT ?\n");
				params.add(pageAt * pageRow);
				params.add(pageRow);
			}

			Map<String, String> departMap = ParamSvc.buildStringMapByClass(dbu, ParamSvc.c_clsDepart);
			Map<String, String> targetMap = ParamSvc.buildStringMapByClass(dbu, ParamSvc.c_clsQsTarget);
			Map<String, String> mastFlagMap = CodeSvc.buildStringMapByKind(dbu, CodeSvc.c_kindQsMastFlag);
			ResultSet rsQs = dbu.queryArray(sqlQryQs.toString(), params.toArray());
			List<Map<String, Object>> qsList = new ArrayList<Map<String, Object>>();
			while (rsQs.next()) {
				Map<String, Object> qs = new HashMap<String, Object>();
				qs.put("qsId", rsQs.getString("qs_id"));
				qs.put("qsName", rsQs.getString("qs_name"));
				qs.put("qsSubject", rsQs.getString("qs_subject"));
				qs.put("departName", departMap.get(rsQs.getString("depart_id")));
				qs.put("target", targetMap.get(rsQs.getString("target_id")));
				qs.put("crDate", new StdCalendar(rsQs.getString("cr_date")).toDateString());
				qs.put("crUserName", DbUtil.nullToEmpty(rsQs.getString("cr_user_name")));
				qs.put("verSeq", Integer.toString(rsQs.getInt("curr_version")));
				qs.put("mastFlag", mastFlagMap.get(rsQs.getString("stat_flag")));
				qsList.add(qs);
			}
			rsQs.close();
			res.put("qsList", qsList);
			
			res.put("success", true);
			res.put("status", "查詢教案列表完成");
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

	@RequestMapping(value = "/QsMntMast_qryQs", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryQs(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success", false);
			
			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("教案代碼不可以為空白");
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleAdmin, null);
			String qryType = req.get("qryType");
			if ("F".equals(qryType)) {
				res.put("fileList", buildFileListData(dbu, qsId));
			}
			else if ("D".equals(qryType)) {
				String sqlQryQs =
					  " SELECT qs_name, qs_subject, focal_remark, curr_version, stat_flag, depart_id, target_id\n"
					+ "      , (SELECT code_desc FROM cmcode b WHERE code_kind = '" + CodeSvc.c_kindQsMastFlag + "' AND code_code = a.stat_flag) stat_flag_desc\n"
					+ "   FROM qsmstr a\n"
					+ "  WHERE qs_id = ?\n";
				Map<String, Object> rowQs = dbu.selectMapRowList(sqlQryQs, qsId);
				if (rowQs == null)
					throw new StopException("教案代碼 <" + qsId + "> 不存在");
				res.put("qsName", rowQs.get("qs_name"));
				res.put("qsSubject", rowQs.get("qs_subject"));
				res.put("departId", rowQs.get("depart_id"));
				res.put("targetId", rowQs.get("target_id"));
				res.put("focalRemark", rowQs.get("focal_remark"));
				int currVer = (Integer) rowQs.get("curr_version");
				res.put("currVer", Integer.toString(currVer));
				res.put("statFlagDesc", rowQs.get("stat_flag_desc"));
				String statFlag = (String) rowQs.get("stat_flag");
				res.put("canEdit", QsSvc.c_qsMastFlagNew.equals(statFlag) || QsSvc.c_qsMastFlagValid.equals(statFlag));
				res.put("canValid", QsSvc.c_qsMastFlagEdit.equals(statFlag));
				res.put("canPass", QsSvc.c_qsMastFlagValid.equals(statFlag));
				res.put("canOnline", QsSvc.c_qsMastFlagPass.equals(statFlag));
				res.put("canAbandon", QsSvc.c_qsMastFlagEdit.equals(statFlag) || QsSvc.c_qsMastFlagValid.equals(statFlag) || QsSvc.c_qsMastFlagPass.equals(statFlag) || QsSvc.c_qsMastFlagOnline.equals(statFlag));
				res.put("canResend", QsSvc.c_qsMastFlagEdit.equals(statFlag) || QsSvc.c_qsMastFlagValid.equals(statFlag) || QsSvc.c_qsMastFlagPass.equals(statFlag));

				String sqlQryQsClass =
						  " SELECT class_id FROM qsclss WHERE qs_id = ? AND class_type = ?\n";
				res.put("useQsClass", dbu.selectStringAllList(sqlQryQsClass, qsId, "C"));
				res.put("useQsAbility", dbu.selectStringAllList(sqlQryQsClass, qsId, "A"));
			}
			else if ("U".equals(qryType)) {
				res.put("userList", buildUserListData(dbu, qsId));
			}
			
			dbu.doCommit();
			
			res.put("success", true);
			res.put("status", "查詢教案資料完成");
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
	
	@RequestMapping(value = "/QsMntMast_qryFile", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryFile(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success", false);

			String qsId = req.get("qsId");
			String fileName = req.get("fileName");
			String qryType = req.get("qryType");
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleAdmin, null);
			if ("D".equals(qryType)) {
				res.put("editStat", false);
				res.put("validStat", false);
				String sqlQryQs =
					  " SELECT curr_version, stat_flag FROM qsmstr WHERE qs_id = ?";
				Map<String, Object> rowQs = dbu.selectMapRowList(sqlQryQs, qsId);
				if (rowQs == null)
					throw new StopException("教案代碼 <" + qsId + "> 不存在");
				String statFlag = (String) rowQs.get("stat_flag");
				int currVer = (Integer) rowQs.get("curr_version");
				if ("V".equals(statFlag)) {
					res.put("validStat", true);
					String sqlQryFile = " SELECT file_desc FROM qsfile a WHERE qs_id = ? AND ver_seq = ? AND file_name = ?";
					String fileDesc = dbu.selectStringList(sqlQryFile, qsId, currVer, fileName);
					if (fileDesc == null)
						throw new StopException("檔案名稱 <" + fileName + "> 不存在");
					// 查詢有對此檔案評論的人員
					String sqlQryUser =
						  " SELECT user_id \"value\"\n"
						+ "      , (SELECT user_name FROM mbdetl b WHERE a.user_id = b.user_id) \"text\"\n"
						+ "   FROM qsvlfl a\n"
						+ "  WHERE qs_id = ?\n"
						+ "    AND ver_seq = ?\n"
						+ "    AND file_name = ?\n"
						+ "  ORDER BY user_id\n";
					res.put("userList", dbu.selectMapAllList(sqlQryUser, qsId, currVer, fileName));
					res.put("fileDesc", fileDesc);
				}
				else {
					res.put("editStat", true);
					String sqlQryFile =
						  " SELECT file_desc, file_class, remark\n"
						+ "   FROM qsfile a\n"
						+ "  WHERE qs_id = ?\n"
						+ "    AND ver_seq = ?\n"
						+ "    AND file_name = ?\n";
					Map<String, Object> rowFile = dbu.selectMapRowList(sqlQryFile, qsId, currVer, fileName);
					if (rowFile == null)
						throw new StopException("無此檔案資料");
					res.put("fileDesc", rowFile.get("file_desc"));
					res.put("fileClass", rowFile.get("file_class"));
					res.put("remark", DbUtil.nullToEmpty((String) rowFile.get("remark")));
				}
			}
			else if ("V".equals(qryType)) {
				// 查詢指定人員對此檔案評論的人員
				String userId = req.get("userId");
				if (userId.isEmpty())
					throw new StopException("人員帳號不可以為空白");
				int currVer = QsSvc.getQsVersion(dbu, qsId);
				String sqlQryFile =
					  " SELECT file_desc, file_class\n"
					+ "      , (SELECT param_name FROM cmpard b WHERE param_class = '" + ParamSvc.c_clsQsClass + "' AND param_id = a.file_class) file_class_desc\n"
					+ "   FROM qsfile a\n"
					+ "  WHERE qs_id = ?\n"
					+ "    AND ver_seq = ?\n"
					+ "    AND file_name = ?\n";
				Map<String, Object> rowFile = dbu.selectMapRowList(sqlQryFile, qsId, currVer, fileName);
				if (rowFile == null)
					throw new StopException("無此檔案資料");
				res.put("fileDesc", rowFile.get("file_desc"));
				res.put("fileClass", rowFile.get("file_class_desc"));
				String sqlQryValid =
					  " SELECT valid_flag, valid_date, reason\n"
					+ "      , (SELECT code_desc FROM cmcode b WHERE b.code_kind = '" + CodeSvc.c_kindQsValidFlag + "' AND a.valid_flag = b.code_code) valid_flag_desc\n"
					+ "   FROM qsvlfl a\n"
					+ "  WHERE qs_id = ?\n"
					+ "    AND ver_seq = ?\n"
					+ "    AND file_name = ?\n"
					+ "    AND user_id = ?\n";
				Map<String, Object> rowValid = dbu.selectMapRowList(sqlQryValid, qsId, currVer, fileName, userId);
				if (rowValid != null) {
					res.put("validDate", new StdCalendar((String) rowValid.get("valid_date")).toDateString());
					res.put("validFlag", rowValid.get("valid_flag_desc"));
					res.put("reason", rowValid.get("reason"));
					res.put("hasData", true);
				}
				else {
					res.put("validDate", "");
					res.put("validFlag", "W");
					res.put("reason", "");
					res.put("hasData", false);
				}
			}
			
			dbu.doCommit();
			
			res.put("success", true);
			res.put("status", "查詢檔案完成");
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

	@RequestMapping(value = "/QsMntMast_download", method = RequestMethod.GET)
	public void download(@RequestParam Map<String, String> req, HttpServletResponse res) {
		String status = "";
		boolean success = false;

		String qsId = "";
		String fileName = "";
		String open = "";
		String qsPath = "";
		DbUtil dbu = new DbUtil();
		try {
			qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("教案代碼不可以為空白");
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleAdmin, null);
			fileName = req.get("fileName");
			if (fileName.isEmpty())
				throw new StopException("檔案名稱不可以為空白");
			open = req.get("open");
			
			int currVer = QsSvc.getQsVersion(dbu, qsId);
			String sqlQryFile =
				  " SELECT file_desc, file_type, file_size\n"
				+ "   FROM qsfile a\n"
				+ "  WHERE qs_id = ?\n"
				+ "    AND ver_seq = ?\n"
				+ "    AND file_name = ?\n";
			Map<String, Object> rowFile = dbu.selectMapRowList(sqlQryFile, qsId, currVer, fileName);
			if (rowFile.isEmpty())
				throw new StopException("指定檔案紀錄不存在");
			qsPath = QsSvc.getQsVersionPath(dbu, qsId, currVer);
			File qsFile = new File(qsPath, fileName);
			if (!qsFile.exists())
				throw new StopException("指定檔案不存在");

			res.setContentType((String) rowFile.get("file_type")); 
			res.setContentLength((Integer) rowFile.get("file_size"));
			if ("Y".equals(open))
				res.setHeader("Content-Disposition", "inline");
			else
				res.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode((String) rowFile.get("file_desc"), "UTF-8"));
			InputStream is = new FileInputStream(qsFile);
			FileCopyUtils.copy(is, res.getOutputStream());
			
			success = true;
		}
		catch (StopException e) {
			status = e.getMessage();
		}
		catch (SQLException e) {
			status = DbUtil.exceptionTranslation(e);
		}
		catch (Exception e) {
			status = e.toString();
		}
		dbu.relDbConn();
		
		if (!success) {
			try {
				UserData ud = UserData.getUserData();
				StringBuffer out = new StringBuffer();
				out.append("下載檔案失敗，原因: " + status + "\n");
				out.append("人員代碼: " + ud.getUserId() + "\n");
				out.append("人員姓名: " + ud.getUserName() + "\n");
				out.append("教案代碼: " + qsId + "\n");
				out.append("目前版本: " + qsId + "\n");
				out.append("教案版本目錄: " + qsPath + "\n");
				out.append("檔案名稱: " + fileName + "\n");
				byte outdata[] = out.toString().getBytes("utf-8");
				res.setContentType("text/plain; charset=utf-8");
				res.setContentLength(outdata.length);
				res.getOutputStream().write(outdata);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@RequestMapping(value = "/QsMntMast_modQs", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> modQs(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");

			String qsIdOrg = req.get("qsIdOrg");
			if (qsIdOrg.isEmpty())
				throw new StopException("原教案代碼不可以為空白");	
			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("教案代碼不可以為空白");	
			if (!qsIdOrg.equals(qsId)) {
				String msg = MiscTool.checkIdString(qsId, QsSvc.c_qsIdMaxLen);
				if (msg != null)
					throw new StopException("教案代碼" + msg);
				String sqlCntQs = " SELECT COUNT(*) FROM qsmstr WHERE qd_id = ?";
				if (dbu.selectIntList(sqlCntQs, qsId) > 0)
					throw new StopException("新教案代碼 <" + qsId + "> 已存在");
			}
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleAdmin, null);
			String qsName = req.get("qsName");
			if (qsName.isEmpty())
				throw new StopException("教案名稱不可以為空白");
			String qsSubject = DbUtil.emptyToNull(req.get("qsSubject"));
			String departId = req.get("departId");
			if (!ParamSvc.doesParamExist(dbu, ParamSvc.c_clsDepart, departId))
				throw new StopException("科別代碼 <" + departId + "> 不存在");
			String targetId = req.get("targetId");
			if (!ParamSvc.doesParamExist(dbu, ParamSvc.c_clsQsTarget, targetId))
				throw new StopException("對象代碼 <" + departId + "> 不存在");
			String focalRemark = DbUtil.emptyToNull((String) req.get("focalRemark"));
			List<String> qsClassList = new ArrayList<String>();
			for (int i = 0; ; i++) {
				String key = "qsClass[" + i + "]";
				if (!req.containsKey(key))
					break;
				String value = req.get(key);
				qsClassList.add(value);
				if (!ParamSvc.doesParamExist(dbu, ParamSvc.c_clsQsClass, value))
					throw new StopException("教案類別代碼 <" + value + "> 不存在");
			}
			String qsClassStr = MiscTool.concatList(qsClassList, ",");
			List<String> qsAbilityList = new ArrayList<String>();
			for (int i = 0; ; i++) {
				String key = "qsAbility[" + i + "]";
				if (!req.containsKey(key))
					break;
				String value = req.get(key);
				qsAbilityList.add(value);
				if (!ParamSvc.doesParamExist(dbu, ParamSvc.c_clsQsAbility, value))
					throw new StopException("核心能力代碼 <" + value + "> 不存在");
			}
			String qsAbilityStr = MiscTool.concatList(qsAbilityList, ",");
				
			String sqlQryQs =
				  " SELECT qs_name, qs_subject, depart_id, target_id, focal_remark\n"
				+ "   FROM qsmstr\n"
				+ "  WHERE qs_id = ?\n";
			Map<String, Object> rowQs = dbu.selectMapRowList(sqlQryQs, qsIdOrg);
			if (rowQs == null)
				throw new StopException("教案代碼 <" + qsId + "> 不存在");
			String qsNameOrg = (String) rowQs.get("qs_name");
			String qsSubjectOrg = (String) rowQs.get("qs_subject");
			String departIdOrg = (String) rowQs.get("depart_id");
			String targetIdOrg = (String) rowQs.get("target_id");
			String focalRemarkOrg = (String) req.get("focal_remark");
			String sqlQryClass = " SELECT class_id FROM qsclss WHERE qs_id = ? AND class_type = ?\n";
			List<String> qsClassListOrg = dbu.selectStringAllList(sqlQryClass, qsId, "C");
			List<String> qsAbilityListOrg = dbu.selectStringAllList(sqlQryClass, qsId, "A");
			String qsClassStrOrg = MiscTool.concatList(qsClassListOrg, ",");
			String qsAbilityStrOrg = MiscTool.concatList(qsAbilityListOrg, ",");

			String sqlUpdQs =
				  " UPDATE qsmstr SET\n"
				+ "        qs_id = ?\n"
				+ "      , qs_name = ?\n"
				+ "      , qs_subject = ?\n"
				+ "      , depart_id = ?\n"
				+ "      , target_id = ?\n"
				+ "      , focal_remark = ?\n"
				+ "  WHERE qs_id = ?\n";
			dbu.executeList(sqlUpdQs, qsId, qsName, qsSubject, departId, targetId, focalRemark, qsIdOrg);
			if (!qsIdOrg.equals(qsId)) {
				String sqlUpdClass = " UPDATE qsclss SET qs_id = ? WHERE qs_id = ?";
				dbu.executeList(sqlUpdClass, qsId, qsIdOrg);
				String sqlUpdUser = " UPDATE qsuser SET qs_id = ? WHERE qs_id = ?";
				dbu.executeList(sqlUpdUser, qsId, qsIdOrg);
				String sqlUpdFile = " UPDATE qsfile SET qs_id = ? WHERE qs_id = ?";
				dbu.executeList(sqlUpdFile, qsId, qsIdOrg);
				String sqlUpdVer = " UPDATE qscnvr SET qs_id = ? WHERE qs_id = ?";
				dbu.executeList(sqlUpdVer, qsId, qsIdOrg);
				String sqlUpdEditNote = " UPDATE qsednt SET qs_id = ? WHERE qs_id = ?";
				dbu.executeList(sqlUpdEditNote, qsId, qsIdOrg);
				String sqlUpdNote = " UPDATE qsnote SET qs_id = ? WHERE qs_id = ?";
				dbu.executeList(sqlUpdNote, qsId, qsIdOrg);
				String sqlUpdValid = " UPDATE qsvlfl SET qs_id = ? WHERE qs_id = ?";
				dbu.executeList(sqlUpdValid, qsId, qsIdOrg);
			}
			String sqlDelClass = " DELETE FROM qsclss WHERE qs_id = ? AND class_type = ?";
			String sqlInsClass = " INSERT INTO qsclss(qs_id, class_type, class_id) VALUES(?, ?, ?)";
			if (!qsClassStr.equals(qsClassStrOrg.toString())) {
				dbu.executeList(sqlDelClass, qsId, "C");
				for (int i = 0; i < qsClassList.size(); i++)
					dbu.executeList(sqlInsClass, qsId, "C", qsClassList.get(i));
			}
			if (!qsAbilityStr.equals(qsAbilityStrOrg.toString())) {
				dbu.executeList(sqlDelClass, qsId, "A");
				for (int i = 0; i < qsAbilityList.size(); i++)
					dbu.executeList(sqlInsClass, qsId, "A", qsAbilityList.get(i));
			}
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "modQs");
			if (qsIdOrg.equals(qsId))
				operLog.add("qsId", qsId);
			else
				operLog.add("qsId", qsIdOrg, qsId);
            operLog.add("qsName", qsNameOrg, qsName);
            operLog.add("qsSubject", qsSubjectOrg, qsSubject);
            operLog.add("departId", departIdOrg, departId);
            operLog.add("targetId", targetIdOrg, targetId);
            operLog.add("focalRemark", focalRemarkOrg, focalRemark);
            operLog.add("qsClass", qsClassStrOrg, qsClassStr);
            operLog.add("qsAbility", qsAbilityStrOrg, qsAbilityStr);
            operLog.write();

			res.put("success", true);
			res.put("status", "修改教案完成");
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

	@RequestMapping(value = "/QsMntMast_qryMember", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryMember(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success", false);
			
			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("教案代碼不可以為空白");
			QsSvc.multiCheckQs(dbu, qsId, null, null);
			String seqNoStr = req.get("seqNo");
			int seqNo;
			try {
				seqNo = Integer.parseInt(seqNoStr);
			}
			catch (NumberFormatException e) {
				throw new StopException("人員序號不是正確的數值");
			}
			String sqlQryUser =
				  " SELECT user_id, role_flag, begin_date, end_date, remark\n"
				+ "      , (SELECT user_name FROM mbdetl b WHERE a.user_id = b.user_id) user_name\n"
				+ "   FROM qsuser a\n"
				+ "  WHERE qs_id = ?\n"
				+ "    AND seq_no = ?\n";
			Map<String, Object> rowUser = dbu.selectMapRowList(sqlQryUser, qsId, seqNo);
			if (rowUser == null)
				throw new StopException("人員資料不存在");
			res.put("userId", rowUser.get("user_id"));
			res.put("userName", DbUtil.nullToEmpty((String) rowUser.get("user_name")));
			res.put("roleFlag", rowUser.get("role_flag"));
			res.put("beginDate", new StdCalendar((String) rowUser.get("begin_date")).toDateString());
			res.put("endDate", "99999999".equals(rowUser.get("user_id")) ? "" : new StdCalendar((String) rowUser.get("end_date")).toDateString());
			res.put("remark", DbUtil.nullToEmpty((String) rowUser.get("remark")));
			
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

	@RequestMapping(value = "/QsMntMast_addMember", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addMember(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");

			String today = new StdCalendar().toDbDateString();
			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("教案代碼不可以為空白");
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleAdmin, null);
			String userId = req.get("userId").toUpperCase();
			if ("".equals(userId))
				throw new StopException("請先輸入參與人員帳號");
			String userName = MemberSvc.getMemberName(dbu, userId);
			if (userName == null)
				throw new StopException("參與人員帳號不存在");
			String roleFlag = req.get("roleFlag");
			String sqlCntUser = " SELECT COUNT(*) FROM qsuser WHERE qs_id = ? AND user_id = ? AND role_flag = ? AND begin_date <= ? AND end_date > ?";
			if (dbu.selectIntList(sqlCntUser, qsId, userId, roleFlag, today, today) > 0)
				throw new StopException("參與人員已存在");
			String remark = DbUtil.emptyToNull(req.get("remark"));
			
			String sqlQrySeq =
				  " SELECT COALESCE(MAX(seq_no), 0) + 1 FROM qsuser WHERE qs_id = ?";
			int seqNo = dbu.selectIntList(sqlQrySeq, qsId);
			String sqlInsUser =
				  " INSERT INTO qsuser(qs_id, seq_no, user_id, role_flag, begin_date, end_Date, remark)\n"
				+ " VALUES(?, ?, ?, ?, ?, '99999999', ?)\n";
			while (true) {
				try {
					dbu.getSavePoint();
					dbu.executeList(sqlInsUser, qsId, seqNo, userId, roleFlag, today, remark);
					dbu.relSavePoint();
					break;
				}
				catch (SQLException e) {
					seqNo++;
					if (DbUtil.c_codeDupKey.equals(e.getSQLState()))
						dbu.rollbackSavePoint();
					else
						throw e;
				}
			}
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "addMember");
			operLog.add("qsId", qsId);
			operLog.add("seqNo", seqNo);
			operLog.add("userId", userId);
			operLog.add("roleFlag", roleFlag);
			operLog.add("beginDate", today);
			operLog.add("remark", remark);
			
			res.put("userList", buildUserListData(dbu, qsId));

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

	@RequestMapping(value = "/QsMntMast_delMember", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> delMember(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");

			String today = new StdCalendar().toDbDateString();
			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("教案代碼不可以為空白");
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleAdmin, null);
			String seqNoStr = req.get("seqNo");
			int seqNo;
			try {
				seqNo = Integer.parseInt(seqNoStr);
			}
			catch (NumberFormatException e) {
				throw new StopException("人員序號不是正確的數值");
			}
			String sqlQryUser =
				  " SELECT user_id, end_date, role_flag\n"
				+ "   FROM qsuser\n"
				+ "  WHERE qs_id = ?\n"
				+ "    AND seq_no = ?\n";
			Map<String, Object> rowUser = dbu.selectMapRowList(sqlQryUser, qsId, seqNo);
			if (rowUser == null)
				throw new StopException("指定的教案代碼 <" + qsId + "> 及使用者序號 <" + seqNo + "> 不存在");
			String userId = (String) rowUser.get("user_id");
			String roleFlag = (String) rowUser.get("role_flag");
			if (!"99999999".equals(rowUser.get("end_date")))
				throw new StopException("指定人員已經停止，不可再停止");
			if (QsSvc.c_qsRoleAdmin.equals(roleFlag)) {
				String sqlQryAdmin =
					  " SELECT COUNT(*) FROM qsuser WHERE qs_id = ? AND role_flag = '" + QsSvc.c_qsRoleAdmin + "' AND end_date = '99999999'";
				if (dbu.selectIntList(sqlQryAdmin, qsId) <= 1)
					throw new StopException("不可刪除最後一個教案管理者");
			}
			
			String sqlUpdUser =
				  " UPDATE qsuser SET\n"
				+ "        end_date = ?\n"
				+ "  WHERE qs_id = ?\n"
				+ "    AND seq_no = ?\n";
			dbu.executeList(sqlUpdUser, today, qsId, seqNo);
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "delMember");
			operLog.add("qsId", qsId);
			operLog.add("seqNo", seqNo);
			operLog.add("userId", userId);
			operLog.add("roleFlag", roleFlag);
			operLog.add("endDate", today);

			res.put("userList", buildUserListData(dbu, qsId));
			
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

	@RequestMapping(value = "/QsMntMast_editQs", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> editQs(@RequestParam Map<String, String> req, HttpServletRequest reqData) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");

			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("請先指定教案編號");
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleAdmin, QsSvc.c_qsMastFlagNew + QsSvc.c_qsMastFlagValid);
			String statFlag = QsSvc.getQsStat(dbu, qsId);
			if (QsSvc.c_qsMastFlagNew.equals(statFlag)) {
				// 教案為新建狀態，轉入編輯模式
				String sqlQryMstr = " SELECT qstmp_id FROM qsmstr WHERE qs_id = ?";
				String qstmpId = dbu.selectStringList(sqlQryMstr, qsId);
				String sqlCntTmpl = " SELECT COUNT(*) FROM qstpms WHERE qstmp_id = ?";
				if (dbu.selectIntList(sqlCntTmpl, qstmpId) == 0)
					throw new StopException("指定的教案樣版 <" + qstmpId + "> 不存在");

				QsSvc.createQs(reqData, dbu, qsId);
				dbu.doCommit();
				
				OperLog operLog = new OperLog(c_progId, "editMode");
				operLog.add("qsId", qsId);
	            operLog.write();
			}
			else if (QsSvc.c_qsMastFlagValid.equals(statFlag)) {
				// 教案為審核狀態，退回編輯模式
				int currVer = QsSvc.getQsVersion(dbu, qsId);
				String reason = DbUtil.emptyToNull((String) req.get("rejectReason"));
				QsSvc.rejectQs(reqData, dbu, qsId, reason);
				dbu.doCommit();

				OperLog operLog = new OperLog(c_progId, "rejectQs");
				operLog.add("qsId", qsId);
				operLog.add("currVer", currVer);
				operLog.write();
			}

			res.put("success", true);
			res.put("status", "教案轉為編輯完成");
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

	@RequestMapping(value = "/QsMntMast_validQs", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> validQs(@RequestParam Map<String, String> req, HttpServletRequest reqData) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");

			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("請先指定教案編號");
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleAdmin, QsSvc.c_qsMastFlagEdit);

			QsSvc.validateQs(reqData, dbu, qsId);
			dbu.doCommit();
				
			OperLog operLog = new OperLog(c_progId, "validate");
			operLog.add("qsId", qsId);
            operLog.write();

			res.put("success", true);
			res.put("status", "教案轉為審核完成");
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

	@RequestMapping(value = "/QsMntMast_passQs", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> passQs(@RequestParam Map<String, String> req, HttpServletRequest reqData) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");

			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("請先指定教案編號");
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleAdmin, QsSvc.c_qsMastFlagValid);

			QsSvc.passQs(reqData, dbu, qsId);
			dbu.doCommit();
				
			OperLog operLog = new OperLog(c_progId, "pass");
			operLog.add("qsId", qsId);
            operLog.write();

			res.put("success", true);
			res.put("status", "教案審核通過完成");
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

	@RequestMapping(value = "/QsMntMast_onlineQs", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> onlineQs(@RequestParam Map<String, String> req, HttpServletRequest reqData) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");

			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("請先指定教案編號");
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleAdmin, QsSvc.c_qsMastFlagPass);

			QsSvc.onlineQs(reqData, dbu, qsId);
			dbu.doCommit();
				
			OperLog operLog = new OperLog(c_progId, "online");
			operLog.add("qsId", qsId);
            operLog.write();


			res.put("success", true);
			res.put("status", "教案公告完成");
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

	@RequestMapping(value = "/QsMntMast_abandonQs", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> abandonQs(@RequestParam Map<String, String> req, HttpServletRequest reqData) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");

			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("請先指定教案編號");
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleAdmin, QsSvc.c_qsMastFlagEdit + QsSvc.c_qsMastFlagValid + QsSvc.c_qsMastFlagPass + QsSvc.c_qsMastFlagOnline);

			QsSvc.abandonQs(reqData, dbu, qsId);
			dbu.doCommit();
				
			OperLog operLog = new OperLog(c_progId, "abandon");
			operLog.add("qsId", qsId);
            operLog.write();


			res.put("success", true);
			res.put("status", "教案廢止完成");
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

	@RequestMapping(value = "/QsMntMast_resendMail", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> resendMail(@RequestParam Map<String, String> req, HttpServletRequest reqData) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");

			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("請先指定教案編號");
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleAdmin, QsSvc.c_qsMastFlagEdit + QsSvc.c_qsMastFlagValid + QsSvc.c_qsMastFlagPass);
			String sqlQryQs =
				  " SELECT stat_flag, curr_version FROM qsmstr WHERE qs_id = ?";
			Map<String, Object> rowQs = dbu.selectMapRowList(sqlQryQs, qsId);
			if (rowQs == null)
				throw new StopException("教案代碼 <" + qsId + "> 不存在");
			String statFlag = (String) rowQs.get("stat_flag");
			int currVer = (Integer) rowQs.get("curr_version");

			MailNotify mail = new MailNotify(reqData);
			if (QsSvc.c_qsMastFlagEdit.equals(statFlag)) {
				if (currVer == 1)
					mail.sendNewQsNotify(dbu, qsId);
				else
					mail.sendQsValidDoneNotify(dbu, qsId, false, currVer);
			}
			else if (QsSvc.c_qsMastFlagValid.equals(statFlag)) {
				mail.sendQsEditDoneNotify(dbu, qsId);
			}
			else if (QsSvc.c_qsMastFlagPass.equals(statFlag)) {
				mail.sendQsValidDoneNotify(dbu, qsId, true, currVer);
			}
			QsSvc.validateQs(reqData, dbu, qsId);
			dbu.doCommit();
				
			OperLog operLog = new OperLog(c_progId, "validate");
			operLog.add("qsId", qsId);
            operLog.write();


			res.put("success", true);
			res.put("status", "重發教案通知完成");
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
