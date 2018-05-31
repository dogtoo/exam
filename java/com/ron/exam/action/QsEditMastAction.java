package com.ron.exam.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import com.ron.exam.util.StdCalendar;
import com.ron.exam.util.StopException;
import com.ron.exam.util.DbUtil;
import com.ron.exam.util.ExceptionUtil;
import com.ron.exam.util.OperLog;
import com.ron.exam.service.MemberSvc;
import com.ron.exam.service.MiscTool;
import com.ron.exam.service.ParamSvc;
import com.ron.exam.service.ProgData;
import com.ron.exam.service.QsSvc;
import com.ron.exam.service.UserData;

@EnableWebMvc
@Controller
public class QsEditMastAction {

	private static final String c_progId = "QsEditMast";
	
	/**
	 * 重新排列檔案的顯示順序，可以特別指定某一檔案插在指定位置
	 * @param dbu
	 * @param qsId 教案代碼
	 * @param fileName 指定插在特定位置的檔名
	 * @param showOrder 指定位置，0 表示排在最後面
	 */
	private void fileReorder(DbUtil dbu, String qsId, String fileName, int showOrder) throws SQLException {
		if (showOrder == 0)
			showOrder = Integer.MAX_VALUE;
		String sqlQryFile = " SELECT file_name FROM qsfile WHERE qs_id = ? AND ver_seq = ? ORDER BY show_order";
		String sqlUpdFile = " UPDATE qsfile SET show_order = ? WHERE qs_id = ? AND ver_seq = ? AND file_name = ?";
		int currVer = QsSvc.getQsVersion(dbu, qsId);
		int seq = 1;
		boolean added = false;
		ResultSet rsFile = dbu.queryList(sqlQryFile, qsId, currVer);
		while (rsFile.next()) {
			String name = rsFile.getString("file_name");
			if (seq == showOrder && !added) {
				added = true;
				if (!name.equals(fileName)) {
					dbu.executeList(sqlUpdFile, seq, qsId, currVer, fileName);
					seq++;
				}
			}
			else if (fileName.equals(name))
				continue;
			dbu.executeList(sqlUpdFile, seq, qsId, currVer, name);
			seq++;
		}
		if (!added)
			dbu.executeList(sqlUpdFile, seq, qsId, currVer, fileName);
		rsFile.close();
	}

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
	 * 建立教案編輯與審核中註記清單資料
	 * @param dbu
	 * @param qsId
	 * @param showSuspend
	 * @param onlySelf
	 * @return
	 * @throws SQLException
	 */
	private List<Map<String, Object>> buildNoteListData(DbUtil dbu, String qsId, boolean showSuspend, boolean onlySelf) throws SQLException {
		StringBuffer sqlQryNote = new StringBuffer(
			  " SELECT seq_no, cr_ver_seq, cr_date, cr_user_id, suspend, note_desc, content\n"
			+ "   FROM qsednt a\n"
			+ "  WHERE qs_id = ?\n");
		List<Object> params = new ArrayList<Object>();
		params.add(qsId);
		if (!showSuspend)
			sqlQryNote.append(" AND suspend <> 'Y'\n");
		if (onlySelf) {
			sqlQryNote.append(" AND cr_user_id = ?\n");
			params.add(UserData.getUserData().getUserId());
		}
		sqlQryNote.append(" ORDER BY seq_no DESC\n");
		ResultSet rsNote = dbu.queryArray(sqlQryNote.toString(), params.toArray());			
		List<Map<String, Object>> noteList = new ArrayList<Map<String, Object>>();
		while (rsNote.next()) {					
			Map<String, Object> note = new HashMap<String, Object>();
			note.put("seqShow", Integer.toString(rsNote.getInt("seq_no")) + ("Y".equals(rsNote.getString("suspend")) ? "*" : ""));
			note.put("noteDesc", DbUtil.nullToEmpty(rsNote.getString("note_desc")));
			note.put("crVerSeq", Integer.toString(rsNote.getInt("cr_ver_seq")));
			note.put("crDate", new StdCalendar(rsNote.getString("cr_date")).toDateString());
			note.put("userName", MemberSvc.getMemberNameOrId(dbu, rsNote.getString("cr_user_id")));
			note.put("seqNo", Integer.toString(rsNote.getInt("seq_no")));
			note.put("suspend", rsNote.getString("suspend"));
			note.put("content", DbUtil.nullToEmpty(rsNote.getString("content")));
			noteList.add(note);
		}													
		return noteList;
	}
	
	@RequestMapping(value = "/QsEditMast", method = RequestMethod.GET)
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

			res.put("departList", ParamSvc.buildSelectOptionByClass(dbu, ParamSvc.c_clsDepart, true));
			res.put("qsTargetList", ParamSvc.buildSelectOptionByClass(dbu, ParamSvc.c_clsQsTarget, true));
			res.put("qsClassList", ParamSvc.buildSelectOptionByClass(dbu, ParamSvc.c_clsQsClass, false));
			res.put("qsAbilityList", ParamSvc.buildSelectOptionByClass(dbu, ParamSvc.c_clsQsAbility, false));
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
		return "QsEditMast";
	}

	/**
	 * 查詢符合帳號、姓名條件的人員
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/QsEditMast_qryMemberList", method = RequestMethod.POST)
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

	@RequestMapping(value = "/QsEditMast_qryQsList", method = RequestMethod.POST)
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
				+ "  WHERE stat_flag = '" + QsSvc.c_qsMastFlagEdit + "'\n"
				+ "    AND EXISTS(SELECT * FROM qsuser b WHERE a.qs_id = b.qs_id AND b.role_flag = '" + QsSvc.c_qsRoleEdit + "' AND b.user_id = ? AND b.end_date > ?)\n");
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

	@RequestMapping(value = "/QsEditMast_qryQs", method = RequestMethod.POST)
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
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleEdit, QsSvc.c_qsMastFlagEdit);
			String qryType = req.get("qryType");
			if ("F".equals(qryType)) {
				res.put("fileList", buildFileListData(dbu, qsId));
				
				String sqlQryClass =
					  " SELECT class_id\n"
					+ "      , (SELECT param_name\n"
					+ "           FROM cmpard b\n"
					+ "          WHERE b.param_class = '" + ParamSvc.c_clsQsFileClass + "'\n"
					+ "            AND b.param_id = a.class_id) param_name\n"
					+ "   FROM qsclss a\n"
					+ "  WHERE qs_id = ?\n"
					+ "    AND class_type = 'F'\n"
					+ "  ORDER BY class_id\n";
				res.put("fileClassList", MiscTool.buildSelectDataBySql(dbu, sqlQryClass, "class_id", "param_name", true, qsId));
			}
			else if ("D".equals(qryType)) {
				String sqlQryQs =
					  " SELECT qs_name, qs_subject, focal_remark, edit_remark, curr_version\n"
					+ "   FROM qsmstr\n"
					+ "  WHERE qs_id = ?\n";
				Map<String, Object> rowQs = dbu.selectMapRowList(sqlQryQs, qsId);
				if (rowQs == null)
					throw new StopException("教案代碼 <" + qsId + "> 不存在");
				res.put("qsName", rowQs.get("qs_name"));
				res.put("qsSubject", rowQs.get("qs_subject"));
				res.put("focalRemark", rowQs.get("focal_remark"));
				res.put("editRemark", rowQs.get("edit_remark"));
				int currVer = (Integer) rowQs.get("curr_version");

				String sqlQryVer =
					  " SELECT cr_date, cr_user_id, ver_desc, edit_reason, remark\n"
					+ "   FROM qscnvr a\n"
					+ "  WHERE qs_id = ?\n"
					+ "    AND ver_seq = ?\n";
				Map<String, Object> rowVer = dbu.selectMapRowList(sqlQryVer, qsId, currVer);
				if (rowVer == null)
					throw new StopException("教案代碼 <" + qsId + "> 版本 <" + currVer + "> 不存在");
				res.put("currVer", Integer.toString(currVer));
				res.put("verInfo", new StdCalendar((String) rowVer.get("cr_date")).toDateString() + " " + MemberSvc.getMemberNameOrId(dbu, (String) rowVer.get("cr_user_id")));
				res.put("verDesc", DbUtil.nullToEmpty((String) rowVer.get("ver_desc")));
				res.put("hasReason", DbUtil.emptyToNull((String) rowVer.get("edit_reason")) != null);
				res.put("editReason", DbUtil.nullToEmpty((String) rowVer.get("edit_reason")));
				res.put("verRemark", DbUtil.nullToEmpty((String) rowVer.get("remark")));
			}
			else if ("N".equals(qryType)) {
				boolean showSuspend = "Y".equals(req.get("showSuspend"));
				boolean onlySelf = "Y".equals(req.get("onlySelf"));
				res.put("noteList", buildNoteListData(dbu, qsId, showSuspend, onlySelf));
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

	@RequestMapping(value = "/QsEditMast_validQs", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> validQs(@RequestParam Map<String, String> req, HttpServletRequest reqData) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");

			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("教案代碼不可以為空白");
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleEdit, QsSvc.c_qsMastFlagEdit);
			QsSvc.validateQs(reqData, dbu, qsId);
			dbu.doCommit();

			res.put("success", true);
			res.put("status", "送交審核完成");
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
	
	@RequestMapping(value = "/QsEditMast_qryFile", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryFile(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success", false);

			String qsId = req.get("qsId");
			String fileName = req.get("fileName");
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleEdit, QsSvc.c_qsMastFlagEdit);
			int currVer = QsSvc.getQsVersion(dbu, qsId);
			String sqlQryFile =
				  " SELECT file_desc, file_class, show_order, remark\n"
				+ "   FROM qsfile a\n"
				+ "  WHERE qs_id = ?\n"
				+ "    AND ver_seq = ?\n"
				+ "    AND file_name = ?\n";
			Map<String, Object> rowFile = dbu.selectMapRowList(sqlQryFile, qsId, currVer, fileName);
			if (rowFile == null)
				throw new StopException("無此檔案資料");
			res.put("fileDesc", rowFile.get("file_desc"));
			res.put("fileClass", rowFile.get("file_class"));
			res.put("showOrder", rowFile.get("show_order"));
			res.put("remark", DbUtil.nullToEmpty((String) rowFile.get("remark")));
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

	/**
	 * 上傳檔案，也就是新增檔案
	 * @param req
	 * @param uparg
	 * @return
	 */
	@RequestMapping(value = "/QsEditMast_upload", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> uploadFile(@RequestParam Map<String, String> req , @RequestParam("file") MultipartFile uparg) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			UserData ud = UserData.getUserData();
			String today = new StdCalendar().toDbDateString();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");

			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("教案代碼不可以為空白");
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleEdit, QsSvc.c_qsMastFlagEdit);
			String fileClass = req.get("fileClass");
			if (!fileClass.isEmpty() && !ParamSvc.doesParamExist(dbu, ParamSvc.c_clsQsFileClass, fileClass))
				throw new StopException("檔案類別代碼 <" + fileClass + "> 不存在");
			String showOrderStr = req.get("showOrder");
			int showOrder = 0;
			try {
				if (!showOrderStr.isEmpty())
					showOrder = Integer.parseInt(showOrderStr);
			}
			catch (NumberFormatException e) {
				throw new StopException("顯示順序不是正確的數字");
			}
			String remark = DbUtil.emptyToNull(req.get("remark"));
			String contentType = uparg.getContentType();
			String clientFileName = uparg.getOriginalFilename();
			File serverUploadFile;
			String serverFileName;

			// 儲存檔案
			try {
				File qsPath = new File(QsSvc.getQsVersionPath(dbu, qsId, 0));
				res.put("contentType", contentType);
				res.put("clientFileName", clientFileName);
				serverUploadFile = File.createTempFile("upload_", "_" + clientFileName, qsPath);
				serverFileName = serverUploadFile.getName();
				res.put("serverFileName", serverFileName);
				uparg.transferTo(serverUploadFile);
				res.put("contentLength", serverUploadFile.length());
			}
			catch (IOException e) {
				throw new StopException("儲存上傳檔案失敗: " + e.toString());
			}

			int currVer = QsSvc.getQsVersion(dbu, qsId);
			String sqlInsFile =
					  " INSERT INTO qsfile(qs_id, ver_seq, file_name, file_class, file_desc, file_size,\n"
					+ "	            file_type, show_order, cr_date, cr_user_id, remark)\n"
					+ " VALUES(?, ?, ?, ?, ?, ?,  ?, 0, ?, ?, ?)\n";
			dbu.executeList(sqlInsFile, qsId, currVer, serverFileName, fileClass, clientFileName, serverUploadFile.length(),
					contentType, today, ud.getUserId(), remark);
			fileReorder(dbu, qsId, serverFileName, showOrder);
			dbu.doCommit();

			OperLog operLog = new OperLog(c_progId, "addFile");
			operLog.add("qsId", qsId);
			operLog.add("verSeq", currVer);
			operLog.add("fileName", serverFileName);
			operLog.add("fileDesc", clientFileName);
			operLog.add("fileSize", serverUploadFile.length());
			operLog.add("fileType", contentType);
			operLog.add("fileClass", fileClass);
			operLog.add("crDatetime", today);
			operLog.add("crUser", ud.getUserId());
			operLog.add("showOrder", showOrder);
			operLog.add("remark", remark);
			operLog.write();
			
			res.put("fileList", buildFileListData(dbu, qsId));
			res.put("success", true);
			res.put("status", "上傳檔案完成");
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
	
	@RequestMapping(value = "/QsEditMast_modFile", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> modFile(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {		
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("教案代碼不可以為空白");	
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleEdit, QsSvc.c_qsMastFlagEdit);
			String fileName = req.get("fileName");
			String fileClass = req.get("fileClass");
			String showOrderStr = req.get("showOrder");
			int showOrder;
			try {
				showOrder = Integer.parseInt(showOrderStr);
			}
			catch (NumberFormatException e) {
				throw new StopException("顯示順序不是正確的數字");
			}
			String remark = DbUtil.emptyToNull((String) req.get("remark"));
			
			int currVer = QsSvc.getQsVersion(dbu, qsId);
			String sqlQryFile =
				  " SELECT file_class, show_order, remark\n"
				+ "   FROM qsfile\n"
				+ "  WHERE qs_id = ?\n"
				+ "    AND ver_seq = ?\n"
				+ "    AND file_name = ?\n";
			Map<String, Object> rowFile = dbu.selectMapRowList(sqlQryFile, qsId, currVer, fileName);
			if (rowFile == null)
				throw new StopException("檔案名稱 <" + fileName + "> 不存在");
			String fileClassOrg = (String) rowFile.get("file_class");
			int showOrderOrg = (Integer) rowFile.get("show_order");
			String remarkOrg = (String) rowFile.get("remark");

			String sqlUpdFile =
				  " UPDATE qsfile SET\n"
				+ "        file_class = ?\n"
				+ "      , remark = ?\n"
				+ "  WHERE qs_id = ?\n"
				+ "    AND ver_seq = ?\n"
				+ "    AND file_name = ?\n";
			dbu.executeList(sqlUpdFile, fileClass, remark, qsId, currVer, fileName);
			fileReorder(dbu, qsId, fileName, showOrder);
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "modFile");
            operLog.add("qsId", qsId);
            operLog.add("verSeq", currVer);
            operLog.add("fileName", fileName);
            operLog.add("fileClass", fileClassOrg, fileClass);
            operLog.add("showOrder", showOrderOrg, showOrder);
            operLog.add("remark", remarkOrg, remark);
            operLog.write();

            res.put("fileList", buildFileListData(dbu, qsId));
            res.put("success", true);
			res.put("status", "修改檔案完成");
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

	@RequestMapping(value = "/QsEditMast_delFile", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> delFile(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			
			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("教案代碼不可以為空白");
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleEdit, QsSvc.c_qsMastFlagEdit);
			String fileName = req.get("fileName");
			if (fileName.isEmpty())
				throw new StopException("檔案名稱不可以為空白");
			int currVer = QsSvc.getQsVersion(dbu, qsId);
			String sqlCntFile = " SELECT COUNT(*) FROM qsfile WHERE qs_id = ? AND ver_seq = ? AND file_name = ?";
			if (dbu.selectIntList(sqlCntFile, qsId, currVer, fileName) == 0)
				throw new StopException("指定檔案不存在");
			
			// 將檔案刪除
			File qsPath = new File(QsSvc.getQsVersionPath(dbu, qsId, currVer));
			new File(qsPath.getAbsolutePath(), fileName).delete();

			String sqlDelFile =
				  " DELETE FROM qsfile\n"
				+ "  WHERE qs_id = ?\n"
				+ "    AND ver_seq = ?\n"
				+ "    AND file_name = ?\n";
			dbu.executeList(sqlDelFile, qsId, currVer, fileName);
			fileReorder(dbu, qsId, "", 0);
			dbu.doCommit();
			            
            OperLog operLog = new OperLog(c_progId, "delFile");
            operLog.add("qsId", qsId);
            operLog.add("verSeq", currVer);
            operLog.add("fileName", fileName);
			
            res.put("fileList", buildFileListData(dbu, qsId));
            res.put("success", true);
			res.put("status", "刪除檔案完成");
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

	@RequestMapping(value = "/QsEditMast_download", method = RequestMethod.GET)
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
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleEdit, QsSvc.c_qsMastFlagEdit);
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
	
	@RequestMapping(value = "/QsEditMast_modQs", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> modQs(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {		
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("教案代碼不可以為空白");	
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleEdit, QsSvc.c_qsMastFlagEdit);
			String remark = DbUtil.emptyToNull((String) req.get("remark"));
				
			String sqlQryQs =
				  " SELECT edit_remark\n"
				+ "   FROM qsmstr\n"
				+ "  WHERE qs_id = ?\n";
			Map<String, Object> rowQs = dbu.selectMapRowList(sqlQryQs, qsId);
			if (rowQs == null)
				throw new StopException("教案代碼 <" + qsId + "> 不存在");
			String remarkOrg = (String) req.get("remark");

			String sqlUpdQs =
				  " UPDATE qsmstr SET\n"
				+ "        edit_remark = ?\n"
				+ "  WHERE qs_id = ?\n";
			dbu.executeList(sqlUpdQs, remark, qsId);
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "modQs");
            operLog.add("qsId", qsId);
            operLog.add("remark", remarkOrg, remark);
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
	
	@RequestMapping(value = "/QsEditMast_modVer", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> modVer(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {		
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("教案代碼不可以為空白");	
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleEdit, QsSvc.c_qsMastFlagEdit);
			int currVer = QsSvc.getQsVersion(dbu, qsId);
			String verDesc = DbUtil.emptyToNull(req.get("verDesc"));
			String remark = DbUtil.emptyToNull((String) req.get("remark"));
			
			String sqlQryVer =
				  " SELECT ver_desc, remark\n"
				+ "   FROM qscnvr\n"
				+ "  WHERE qs_id = ?\n"
				+ "    AND ver_seq = ?\n";
			Map<String, Object> rowVer = dbu.selectMapRowList(sqlQryVer, qsId, currVer);
			if (rowVer == null)
				throw new StopException("教案代碼 <" + qsId + "> 版本 <" + currVer + "> 不存在");
			String verDescOrg = (String) rowVer.get("ver_desc");
			String remarkOrg = (String) rowVer.get("remark");

			String sqlUpdVer =
				  " UPDATE qscnvr SET\n"
				+ "        ver_desc = ?\n"
				+ "      , remark = ?\n"
				+ "  WHERE qs_id = ?\n"
				+ "    AND ver_seq = ?\n";
			dbu.executeList(sqlUpdVer, verDesc, remark, qsId, currVer);
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "modVer");
            operLog.add("qsId", qsId);
            operLog.add("verSeq", currVer);
            operLog.add("verDesc", verDescOrg, verDesc);
            operLog.add("remark", remarkOrg, remark);
            operLog.write();

            res.put("success", true);
			res.put("status", "修改版本資料完成");
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
	
	@RequestMapping(value = "/QsEditMast_qryNoteList", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryNoteList(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {		
            res.put("success", false);
			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("教案代碼不可以為空白");	
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleEdit, QsSvc.c_qsMastFlagEdit);
			boolean showSuspend = "Y".equals(req.get("showSuspend"));
			boolean onlySelf = "Y".equals(req.get("onlySelf"));
            res.put("noteList", buildNoteListData(dbu, qsId, showSuspend, onlySelf));

            res.put("success", true);
			res.put("status", "修改註記資料完成");
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
	
	@RequestMapping(value = "/QsEditMast_addNote", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addNote(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {		
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("教案代碼不可以為空白");	
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleEdit, QsSvc.c_qsMastFlagEdit);
			int currVer = QsSvc.getQsVersion(dbu, qsId);
			String noteDesc = req.get("noteDesc");
			if (noteDesc.isEmpty())
				throw new StopException("註記說明不可以為空白");
			String content = DbUtil.emptyToNull((String) req.get("content"));
			boolean showSuspend = "Y".equals(req.get("showSuspend"));
			boolean onlySelf = "Y".equals(req.get("onlySelf"));
			
			String sqlQrySeq =
				  " SELECT COALESCE(MAX(seq_no), 0) + 1\n"
				+ "   FROM qsednt\n"
				+ "  WHERE qs_id = ?\n";
			int seqNo = dbu.selectIntList(sqlQrySeq, qsId);

			String sqlInsNote =
				  " INSERT INTO qsednt(qs_id, seq_no, cr_ver_seq, cr_date, cr_user_id, suspend, note_desc, content)\n"
				+ " VALUES(?, ?, ?, ?, ?, 'N', ?, ?)\n";
			String today = new StdCalendar().toDbDateString();
			while (true) {
				try {
					dbu.getSavePoint();
					dbu.executeList(sqlInsNote, qsId, seqNo, currVer, today, ud.getUserId(), noteDesc, content);
					dbu.relSavePoint();
					break;
				}
				catch (SQLException e) {
					dbu.rollbackSavePoint();
					if (DbUtil.c_codeDupKey.equals(e.getSQLState()))
						seqNo++;
					else
						throw e;
				}
			}
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "addNote");
            operLog.add("qsId", qsId);
            operLog.add("seqNo", seqNo);
            operLog.add("verSeq", currVer);
            operLog.add("crDate", today);
            operLog.add("noteDesc", noteDesc);
            operLog.add("content", content);
            operLog.write();

            res.put("noteList", buildNoteListData(dbu, qsId, showSuspend, onlySelf));
            res.put("success", true);
			res.put("status", "新增註記完成");
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
	
	@RequestMapping(value = "/QsEditMast_suspendNote", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> suspendNote(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {		
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("教案代碼不可以為空白");	
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleEdit, QsSvc.c_qsMastFlagEdit);
			int seqNo;
			try {
				seqNo = Integer.parseInt(req.get("seqNo"));
			}
			catch (NumberFormatException e) {
				throw new StopException("註記序號不正確");
			}
			String suspend = "Y".equals(req.get("suspend")) ? "Y" : "N";
			boolean showSuspend = "Y".equals(req.get("showSuspend"));
			boolean onlySelf = "Y".equals(req.get("onlySelf"));
			
			String sqlQryNote =
				  " SELECT suspend\n"
				+ "   FROM qsednt\n"
				+ "  WHERE qs_id = ?\n"
				+ "    AND seq_no = ?\n";
			Map<String, Object> rowNote = dbu.selectMapRowList(sqlQryNote, qsId, seqNo);
			if (rowNote == null)
				throw new StopException("教案代碼 <" + qsId + "> 註記序號 <" + seqNo + "> 不存在");
			String suspendOrg = (String) rowNote.get("suspend");

			String sqlUpdNote =
				  " UPDATE qsednt SET\n"
				+ "        suspend = ?\n"
				+ "  WHERE qs_id = ?\n"
				+ "    AND seq_no = ?\n";
			dbu.executeList(sqlUpdNote, suspend, qsId, seqNo);
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "modNote");
            operLog.add("qsId", qsId);
            operLog.add("seqNo", seqNo);
            operLog.add("suspend", suspendOrg, suspend);
            operLog.write();

            res.put("noteList", buildNoteListData(dbu, qsId, showSuspend, onlySelf));
            res.put("success", true);
			res.put("status", "修改註記資料完成");
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
