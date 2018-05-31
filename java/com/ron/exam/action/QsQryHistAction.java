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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ron.exam.service.CodeSvc;
import com.ron.exam.service.MemberSvc;
import com.ron.exam.service.ParamSvc;
import com.ron.exam.service.ProgData;
import com.ron.exam.service.QsSvc;
import com.ron.exam.service.UserData;

@EnableWebMvc
@Controller
public class QsQryHistAction {

	private static final String c_progId = "QsQryHist";
	
	@RequestMapping(value = "/QsQryHist", method = RequestMethod.GET)
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

			res.put("departList", ParamSvc.buildSelectOptionByClass(dbu, "DEPART", true));
			res.put("qsTargetList", ParamSvc.buildSelectOptionByClass(dbu, "QSTGT", true));
			res.put("qsClassList", ParamSvc.buildSelectOptionByClass(dbu, "QSCLS", false));
			res.put("qsAbilityList", ParamSvc.buildSelectOptionByClass(dbu, "QSABL", false));

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
		return "QsQryHist";
	}

	/**
	 * 查詢符合帳號、姓名條件的人員
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/QsQryHist_qryMemberList", method = RequestMethod.POST)
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

	@RequestMapping(value = "/QsQryHist_qryUserList", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryUserList(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();

		DbUtil dbu = new DbUtil();
		try {
			res.put("userList", QsSvc.queryUserList(dbu, req.get("userCond")));
			dbu.doCommit();
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

	@RequestMapping(value = "/QsQryHist_qryQsList", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryQsList(@RequestParam Map<String, String> req) {
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
			
			StringBuffer sqlQryQs = new StringBuffer(
				  " SELECT qs_id, qs_name, qs_subject, depart_id, target_id, cr_date, cr_user_id\n"
				+ "      , curr_version, stat_flag\n"
				+ "      , (SELECT user_name FROM mbdetl b WHERE a.cr_user_id = b.user_id) cr_user_name\n");
			StringBuffer sqlCntQs = new StringBuffer(" SELECT COUNT(*)");
			StringBuffer sqlCond = new StringBuffer(
				  "   FROM qsmstr a\n"
				+ "  WHERE 1 = 1\n");
			List<Object> params = new ArrayList<Object>();
			String qryQsId = req.get("qsId");
			if (!qryQsId.isEmpty()) {
				sqlCond.append(" AND qs_id LIKE '%' || ? || '%'\n");
				params.add(qryQsId);
			}
			String qryQsName = req.get("qsName");
			if (!qryQsName.isEmpty()) {
				sqlCond.append(" AND qs_name LIKE '%' || ? || '%'\n");
				params.add(qryQsName);
			}
			String qryDepartId = req.get("departId");
			if (!qryDepartId.isEmpty()) {
				sqlCond.append(" AND depart_id = ?\n");
				params.add(qryDepartId);
			}
			String qryTargetId = req.get("targetId");
			if (!qryTargetId.isEmpty()) {
				sqlCond.append(" AND target_id = ?\n");
				params.add(qryTargetId);
			}
			List<String> qryClassList = new ArrayList<String>();
			for (int i = 0; ; i++) {
				String key = "qryQsClass[" + i + "]";
				if (!req.containsKey(key))
					break;
				qryClassList.add(req.get(key));
			}
			if (qryClassList.size() > 0) {
				sqlCond.append(" AND EXISTS(SELECT * FROM qsclss b WHERE a.qs_id = b.qs_id AND b.class_type = 'C' AND b.class_id IN ");
				DbUtil.buildInSqlParam(sqlCond, params, qryClassList);
				sqlCond.append(")\n");
			}
			List<String> qryAbilityList = new ArrayList<String>();
			for (int i = 0; ; i++) {
				String key = "qryQsAbility[" + i + "]";
				if (!req.containsKey(key))
					break;
				qryAbilityList.add(req.get(key));
			}
			if (qryAbilityList.size() > 0) {
				sqlCond.append(" AND EXISTS(SELECT * FROM qsclss b WHERE a.qs_id = b.qs_id AND b.class_type = 'A' AND b.class_id IN ");
				DbUtil.buildInSqlParam(sqlCond, params, qryAbilityList);
				sqlCond.append(")\n");
			}
			String qryUserId = req.get("userId");
			if (!qryUserId.isEmpty()) {
				sqlCond.append(" AND EXISTS(SELECT * FROM qsuser b WHERE a.qs_id = b.qs_id AND b.user_id = ?)\n");
				params.add(qryUserId);
			}
			String qryBeginDate = req.get("beginDate");
			if (!qryBeginDate.isEmpty()) {
				if (!StdCalendar.isDateFormat(qryBeginDate))
					throw new StopException("啟始日期格式不正確");
				qryBeginDate = new StdCalendar(qryBeginDate).toDbDateString();
				sqlCond.append(" AND cr_date >= ?\n");
				params.add(qryBeginDate);
			}
			String qryEndDate = req.get("endDate");
			if (!qryEndDate.isEmpty()) {
				if (!StdCalendar.isDateFormat(qryEndDate))
					throw new StopException("啟始日期格式不正確");
				qryEndDate = new StdCalendar(qryEndDate).toDbDateString();
				sqlCond.append(" AND cr_date <= ?\n");
				params.add(qryEndDate);
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
					put("qsName:A", "qs_name ASC");
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

			ResultSet rsQs = dbu.queryArray(sqlQryQs.toString(), params.toArray());
			List<Map<String, Object>> qsList = new ArrayList<Map<String, Object>>();
			Map<String, String> departMap = ParamSvc.buildStringMapByClass(dbu, "DEPART");
			Map<String, String> targetMap = ParamSvc.buildStringMapByClass(dbu, "QSTGT");
			Map<String, String> statusMap = CodeSvc.buildStringMapByKind(dbu, "QSMFLG");
			while (rsQs.next()) {
				Map<String, Object> qs = new HashMap<String, Object>();
				qs.put("qsId", rsQs.getString("qs_id"));
				qs.put("qsName", rsQs.getString("qs_name"));
				qs.put("qsSubject", DbUtil.nullToEmpty(rsQs.getString("qs_subject")));
				qs.put("depart", departMap.get(rsQs.getString("depart_id")));
				qs.put("target", targetMap.get(rsQs.getString("target_id")));
				qs.put("crDate", new StdCalendar(rsQs.getString("cr_date")).toDateString());
				qs.put("crUserName", DbUtil.nullToEmpty(rsQs.getString("cr_user_name")));
				qs.put("currVersion", Integer.toString(rsQs.getInt("curr_version")));
				qs.put("statFlag", statusMap.get(rsQs.getString("stat_flag")));
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

	@RequestMapping(value = "/QsQryHist_qryQs", method = RequestMethod.POST)
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
			String qryType = req.get("qryType");
			if ("D".equals(qryType)) {
				String sqlQryQs =
					  " SELECT qs_name, qs_subject, curr_version, stat_flag, edit_remark, online_remark\n"
					+ "      , (SELECT param_name FROM cmpard b WHERE b.param_class = 'DEPART' AND a.depart_id = b.param_id) depart\n"
					+ "      , (SELECT param_name FROM cmpard b WHERE b.param_class = 'QSTGT' AND a.depart_id = b.param_id) target\n"
					+ "      , (SELECT code_desc FROM cmcode b WHERE b.code_kind = 'QSMFLG' AND a.stat_flag = b.code_code) stat_desc\n"
					+ "   FROM qsmstr a\n"
					+ "  WHERE qs_id = ?\n";
				Map<String, Object> rowQs = dbu.selectMapRowList(sqlQryQs, qsId);
				if (rowQs == null)
					throw new StopException("教案代碼 <" + qsId + "> 不存在");
				res.put("qsName", (String) rowQs.get("qs_name"));
				res.put("qsSubject", (String) rowQs.get("qs_subject"));
				res.put("depart", (String) rowQs.get("depart"));
				res.put("target", (String) rowQs.get("target"));
				res.put("verStat", Integer.toString((Integer) rowQs.get("curr_version")) + " [" + (String) rowQs.get("stat_desc") + "]");
				res.put("editRemark", DbUtil.nullToEmpty((String) rowQs.get("edit_remark")));
				res.put("onlineRemark", DbUtil.nullToEmpty((String) rowQs.get("online_remark")));
				
				String sqlQryCls =
					  " SELECT class_id FROM qsclss WHERE qs_id = ? AND class_type = ? ORDER BY class_id";
				res.put("useQsClassList", dbu.selectStringAllList(sqlQryCls, qsId, "C"));
				res.put("useQsAbilityList", dbu.selectStringAllList(sqlQryCls, qsId, "A"));
				
				String sqlQryUser =
					  " SELECT seq_no, user_id, role_flag, begin_date, end_date\n"
					+ "      , (SELECT user_name FROM mbdetl b WHERE a.user_id = b.user_id) user_name\n"
					+ "  FROM qsuser a\n"
					+ " WHERE qs_id = ?\n"
					+ " ORDER BY seq_no\n";
				ResultSet rsUser = dbu.queryList(sqlQryUser, qsId);
				List<Map<String, Object>> userList = new ArrayList<Map<String, Object>>();
				Map<String, String> roleMap = CodeSvc.buildStringMapByKind(dbu, "QSUSRR");
				while (rsUser.next()) {
					Map<String, Object> user = new HashMap<String, Object>();
					user.put("seqNo", rsUser.getInt("seq_no"));
					user.put("userId", rsUser.getString("user_id"));
					user.put("userName", rsUser.getString("user_name"));
					user.put("roleDesc", roleMap.get(rsUser.getString("role_flag")));
					user.put("beginDate", new StdCalendar(rsUser.getString("begin_date")).toDateString());
					user.put("endDate", "99999999".equals(rsUser.getString("end_date")) ? "" : new StdCalendar(rsUser.getString("end_date")).toDateString());
					userList.add(user);
				}
				rsUser.close();
				res.put("userList", userList);
			}
			else if ("H".equals(qryType)) {	
				String sqlQryVer =
					  " SELECT ver_seq \"value\", ver_seq \"text\"\n"
					+ "   FROM qscnvr\n"
					+ "  WHERE qs_id = ?\n"
					+ "  ORDER BY ver_seq\n";
				res.put("verList", dbu.selectMapAllList(sqlQryVer, qsId));
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

	@RequestMapping(value = "/QsQryHist_qryQsVer", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryQsVer(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success", false);
			
			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("教案代碼不可以為空白");
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleAdmin + QsSvc.c_qsRoleEdit + QsSvc.c_qsRoleValid, null);
			int verSeq;
			try {
				verSeq = Integer.parseInt(req.get("verSeq"));
			}
			catch (NumberFormatException e) {
				throw new StopException("教案版本不是數值");
			}
			String sqlQryVer =
				  " SELECT ver_desc, cr_date, cr_user_id, cnfrm_user_id, cnfrm_date, edit_reason, remark\n"
				+ "      , (SELECT user_name FROM mbdetl b WHERE a.cr_user_id = b.user_id) cr_user_name\n"
				+ "      , (SELECT user_name FROM mbdetl b WHERE a.cnfrm_user_id = b.user_id) cnfrm_user_name\n"
				+ "   FROM qscnvr a\n"
				+ "  WHERE qs_id = ?\n"
				+ "    AND ver_seq = ?\n";
			Map<String, Object> rowVer = dbu.selectMapRowList(sqlQryVer, qsId, verSeq);
			if (rowVer == null)
				throw new StopException("指定版本不存在");
			res.put("verDate", new StdCalendar((String) rowVer.get("cr_date")).toDateString());
			res.put("crUserId", (String) rowVer.get("cr_user_id"));
			res.put("crUserName", (String) rowVer.get("cr_user_name"));
			res.put("cnfrmDate", new StdCalendar((String) rowVer.get("cnfrm_date")).toDateString());
			res.put("cnfrmUserId", DbUtil.nullToEmpty((String) rowVer.get("cr_user_id")));
			res.put("cnfrmUserName", DbUtil.nullToEmpty((String) rowVer.get("cr_user_name")));
			res.put("verDesc", (String) rowVer.get("ver_desc"));
			res.put("editReason", (String) rowVer.get("edit_reason"));
			res.put("remark", (String) rowVer.get("remark"));

			String sqlQryFile =
				  " SELECT file_name, file_desc, file_class, file_size, file_type\n"
				+ "   FROM qsfile a\n"
				+ "  WHERE qs_id = ?\n"
				+ "    AND ver_seq = ?\n"
				+ "  ORDER BY show_order\n";
			ResultSet rsFile = dbu.queryList(sqlQryFile, qsId, verSeq);
			Map<String, String> fileClassMap = ParamSvc.buildStringMapByClass(dbu, "QSFLCLS");
			List<Map<String, Object>> fileList = new ArrayList<Map<String, Object>>();
			while (rsFile.next()) {
				Map<String, Object> file = new HashMap<String, Object>();
				file.put("fileName", rsFile.getString("file_name"));
				file.put("fileDesc", rsFile.getString("file_desc"));
				file.put("fileClass", fileClassMap.get(rsFile.getString("file_class")));
				file.put("fileSize", String.format("%,d", rsFile.getInt("file_size")));
				file.put("fileType", rsFile.getString("file_type"));
				fileList.add(file);
			}
			rsFile.close();
			res.put("fileList", fileList);
			dbu.doCommit();
			
			res.put("success", true);
			res.put("status", "查詢版本資料完成");
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

	@RequestMapping(value = "/QsQryHist_qryQsVerFile", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryQsVerFile(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success", false);
			
			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("教案代碼不可以為空白");
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleAdmin + QsSvc.c_qsRoleEdit + QsSvc.c_qsRoleValid, null);
			int verSeq;
			try {
				verSeq = Integer.parseInt(req.get("verSeq"));
			}
			catch (NumberFormatException e) {
				throw new StopException("教案版本不是數值");
			}
			String fileName = req.get("fileName");
			if (fileName.isEmpty())
				throw new StopException("檔案名稱不可以為空白");
			String sqlQryFile =
				  " SELECT file_desc, file_class, file_size, file_type, cr_date, cr_user_id, remark\n"
				+ "      , (SELECT param_name FROM cmpard b WHERE b.param_class = 'QSFLCLS' AND a.file_class = b.param_id) file_class_desc\n"
				+ "      , (SELECT user_name FROM mbdetl b WHERE a.cr_user_id = b.user_id) cr_user_name\n"
				+ "   FROM qsfile a\n"
				+ "  WHERE qs_id = ?\n"
				+ "    AND ver_seq = ?\n"
				+ "    AND file_name = ?\n";
			Map<String, Object> rowFile = dbu.selectMapRowList(sqlQryFile, qsId, verSeq, fileName);
			if (rowFile == null)
				throw new StopException("指定的檔案不存在");
			res.put("fileDesc", (String) rowFile.get("file_desc"));
			res.put("fileClass", (String) rowFile.get("file_class_desc"));
			res.put("fileSize", String.format("%,d", (Integer) rowFile.get("file_size")));
			res.put("fileType", (String) rowFile.get("file_type"));
			res.put("crDate", new StdCalendar((String) rowFile.get("cr_date")).toDateString());
			res.put("crUserId", (String) rowFile.get("cr_user_id"));
			res.put("crUserName", (String) rowFile.get("cr_user_name"));
			res.put("remark", DbUtil.nullToEmpty((String) rowFile.get("remark")));

			String sqlQryUser =
				  " SELECT user_id \"value\"\n"
				+ "      , (SELECT user_name FROM mbdetl b WHERE a.user_id = b.user_id) \"text\"\n"
				+ "   FROM qsvlfl a\n"
				+ "  WHERE qs_id = ?\n"
				+ "    AND ver_seq = ?\n"
				+ "    AND file_name = ?\n"
				+ "  ORDER BY user_id\n";
			res.put("userList", dbu.selectMapAllList(sqlQryUser, qsId, verSeq, fileName));
			dbu.doCommit();
			
			res.put("success", true);
			res.put("status", "查詢檔案詳細資料完成");
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

	@RequestMapping(value = "/QsQryHist_qryQsVerFileValid", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryQsVerFileValid(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success", false);
			
			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("教案代碼不可以為空白");
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleAdmin + QsSvc.c_qsRoleEdit + QsSvc.c_qsRoleValid, null);
			int verSeq;
			try {
				verSeq = Integer.parseInt(req.get("verSeq"));
			}
			catch (NumberFormatException e) {
				throw new StopException("教案版本不是數值");
			}
			String fileName = req.get("fileName");
			if (fileName.isEmpty())
				throw new StopException("檔案名稱不可以為空白");
			String userId = req.get("userId");
			if (userId.isEmpty())
				throw new StopException("人員帳號不可以為空白");
			String sqlQryValid =
				  " SELECT valid_flag, valid_date, reason\n"
				+ "      , (SELECT code_desc FROM cmcode b WHERE b.code_kind = 'QSVFLG' AND a.valid_flag = b.code_code) valid_desc\n"
				+ "   FROM qsvlfl a\n"
				+ "  WHERE qs_id = ?\n"
				+ "    AND ver_seq = ?\n"
				+ "    AND file_name = ?\n"
				+ "    AND user_id = ?\n";
			Map<String, Object> rowValid = dbu.selectMapRowList(sqlQryValid, qsId, verSeq, fileName, userId);
			if (rowValid == null)
				throw new StopException("指定檔案審核資料不存在");
			res.put("validDate", new StdCalendar((String) rowValid.get("valid_date")).toDateString());
			res.put("validFlag", (String) rowValid.get("valid_flag"));
			res.put("validDesc", (String) rowValid.get("valid_desc"));
			res.put("reason", DbUtil.nullToEmpty((String) rowValid.get("reason")));
			dbu.doCommit();
			
			res.put("success", true);
			res.put("status", "查詢檔案審核資料完成");
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

	@RequestMapping(value = "/QsQryHist_download", method = RequestMethod.GET)
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
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleAdmin + QsSvc.c_qsRoleEdit + QsSvc.c_qsRoleValid, null);
			int verSeq;
			try {
				verSeq = Integer.parseInt(req.get("verSeq"));
			}
			catch (NumberFormatException e) {
				throw new StopException("教案版本不是數值");
			}
			fileName = req.get("fileName");
			if (fileName.isEmpty())
				throw new StopException("檔案名稱不可以為空白");
			open = req.get("open");
			
			String sqlQryFile =
				  " SELECT file_desc, file_type, file_size\n"
				+ "   FROM qsfile a\n"
				+ "  WHERE qs_id = ?\n"
				+ "    AND ver_seq = ?\n"
				+ "    AND file_name = ?\n";
			Map<String, Object> rowFile = dbu.selectMapRowList(sqlQryFile, qsId, verSeq, fileName);
			if (rowFile.isEmpty())
				throw new StopException("指定檔案紀錄不存在");
			qsPath = QsSvc.getQsVersionPath(dbu, qsId, verSeq);
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
}
