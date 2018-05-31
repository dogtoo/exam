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
import com.ron.exam.service.MiscTool;
import com.ron.exam.service.ParamSvc;
import com.ron.exam.service.ProgData;
import com.ron.exam.service.QsSvc;
import com.ron.exam.service.UserData;

@EnableWebMvc
@Controller
public class QsQryMastAction {

	private static final String c_progId = "QsQryMast";
	
	@RequestMapping(value = "/QsQryMast", method = RequestMethod.GET)
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
		return "QsQryMast";
	}

	/**
	 * 查詢符合帳號、姓名條件的人員
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/QsQryMast_qryMemberList", method = RequestMethod.POST)
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

	@RequestMapping(value = "/QsQryMast_qryQsList", method = RequestMethod.POST)
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
				+ "  WHERE stat_flag = '" + QsSvc.c_qsMastFlagOnline + "'\n"
				+ "    AND EXISTS(SELECT * FROM qsuser b WHERE a.qs_id = b.qs_id AND b.role_flag IN ('" + QsSvc.c_qsRoleNote + "', '" + QsSvc.c_qsRoleAdmin + "') AND b.user_id = ? AND b.end_date > ?)\n");
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
			Map<String, String> statFlagMap = CodeSvc.buildStringMapByKind(dbu, CodeSvc.c_kindQsMastFlag);
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
				qs.put("statFlag", statFlagMap.get(rsQs.getString("stat_flag")));
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

	@RequestMapping(value = "/QsQryMast_qryQs", method = RequestMethod.POST)
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
			QsSvc.multiCheckQs(dbu, qsId, null, QsSvc.c_qsMastFlagOnline);
			String qryType = req.get("qryType");
			if ("D".equals(qryType)) {
				String sqlQryQs =
					  " SELECT qs_name, qs_subject, online_remark, curr_version, stat_flag, depart_id, target_id\n"
					+ "      , (SELECT param_name FROM cmpard b WHERE param_class = '" + ParamSvc.c_clsDepart + "' AND param_id = a.depart_id) depart_desc\n"
					+ "      , (SELECT param_name FROM cmpard b WHERE param_class = '" + ParamSvc.c_clsQsTarget + "' AND param_id = a.target_id) target_desc\n"
					+ "      , (SELECT code_desc FROM cmcode b WHERE code_kind = '" + CodeSvc.c_kindQsMastFlag + "' AND code_code = a.stat_flag) stat_flag_desc\n"
					+ "   FROM qsmstr a\n"
					+ "  WHERE qs_id = ?\n";
				Map<String, Object> rowQs = dbu.selectMapRowList(sqlQryQs, qsId);
				if (rowQs == null)
					throw new StopException("教案代碼 <" + qsId + "> 不存在");
				res.put("qsName", rowQs.get("qs_name"));
				res.put("qsSubject", rowQs.get("qs_subject"));
				res.put("departDesc", rowQs.get("depart_desc"));
				res.put("targetDesc", rowQs.get("target_desc"));
				res.put("remark", rowQs.get("online_remark"));
				int currVer = (Integer) rowQs.get("curr_version");
				res.put("currVer", Integer.toString(currVer));
				res.put("statFlagDesc", rowQs.get("stat_flag_desc"));

				String sqlQryQsClass =
					  " SELECT class_id FROM qsclss WHERE qs_id = ? AND class_type = ?\n";
				res.put("useQsClass", dbu.selectStringAllList(sqlQryQsClass, qsId, "C"));
				res.put("useQsAbility", dbu.selectStringAllList(sqlQryQsClass, qsId, "A"));
			}
			else if ("U".equals(qryType)) {
				String sqlQryUser =
					  " SELECT seq_no, user_id, role_flag, begin_date, end_date\n"
					+ "      , (SELECT user_name FROM mbdetl b WHERE a.user_id = b.user_id) user_name\n"
					+ "   FROM qsuser a\n"
					+ "  WHERE qs_id = ?\n"
					+ "  ORDER BY seq_no\n";
				ResultSet rsUser = dbu.queryList(sqlQryUser, qsId);
				List<Map<String, Object>> userList = new ArrayList<Map<String, Object>>();
				Map<String, String> roleMap = CodeSvc.buildStringMapByKind(dbu, CodeSvc.c_kindQsUserRole);
				while (rsUser.next()) {
					Map<String, Object> user = new HashMap<String, Object>();
					user.put("seqNo", Integer.toString(rsUser.getInt("seq_no")));
					user.put("roleDesc", roleMap.get(rsUser.getString("role_flag")));
					user.put("userId", rsUser.getString("user_id"));
					user.put("userName", DbUtil.nullToEmpty(rsUser.getString("user_name")));
					user.put("beginDate", new StdCalendar(rsUser.getInt("begin_date")).toDateString());
					user.put("endDate", "99999999".equals(rsUser.getString("end_date")) ? "" : new StdCalendar(rsUser.getInt("end_date")).toDateString());
					userList.add(user);
				}
				rsUser.close();
				res.put("userList", userList);
			}
			else if ("F".equals(qryType)) {
				String sqlQryFile =
					  " SELECT file_name, file_desc, file_class, cr_date, file_size, file_type\n"
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
					file.put("fileSize", MiscTool.convToCommaInt(rsFile.getInt("file_size")));
					file.put("fileType", rsFile.getString("file_type"));
					file.put("crDate", new StdCalendar(rsFile.getString("cr_date")).toDateString());
					fileList.add(file);
				}													
				rsFile.close();
				res.put("fileList", fileList);
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
	@RequestMapping(value = "/QsQryMast_download", method = RequestMethod.GET)
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
			QsSvc.multiCheckQs(dbu, qsId, null, QsSvc.c_qsMastFlagOnline);
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
}
