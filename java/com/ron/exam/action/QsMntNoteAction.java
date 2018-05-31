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
import com.ron.exam.service.MemberSvc;
import com.ron.exam.service.ParamSvc;
import com.ron.exam.service.ProgData;
import com.ron.exam.service.QsSvc;
import com.ron.exam.service.UserData;

@EnableWebMvc
@Controller
public class QsMntNoteAction {

	private static final String c_progId = "QsMntNote";
	
	private List<Map<String, Object>> buildNoteListData(DbUtil dbu, String qsId, String sortType, boolean expired) throws SQLException {
		StringBuffer sqlQryNote = new StringBuffer(
			  " SELECT seq_no, note_class, important_id, note_desc\n"
			+ "      , b.param_name note_class_desc\n"
			+ "   FROM qsnote a, cmpard b\n"
			+ "  WHERE qs_id = ?\n"
			+ "    AND b.param_class = '" + ParamSvc.c_clsQsNoteClass + "'\n"
			+ "    AND b.param_id = a.note_class\n");
		List<Object> params = new ArrayList<Object>();
		params.add(qsId);
		if (!expired) {
			sqlQryNote.append(" AND expire_date > ?\n");
			params.add(new StdCalendar().toDbDateString());
		}
		if ("I".equals(sortType))
			sqlQryNote.append(" ORDER BY important_id, seq_no DESC\n");
		else if ("C".equals(sortType))
			sqlQryNote.append(" ORDER BY b.show_order, seq_no DESC\n");
		else if ("S".equals(sortType))
			sqlQryNote.append(" ORDER BY seq_no DESC\n");
		ResultSet rsNote = dbu.queryArray(sqlQryNote.toString(), params.toArray());
		List<Map<String, Object>> noteList = new ArrayList<Map<String, Object>>();
		Map<String, String> importantMap = CodeSvc.buildStringMapByKind(dbu, CodeSvc.c_kindQsNoteImport);
		while (rsNote.next()) {
			Map<String, Object> note = new HashMap<String, Object>();
			note.put("seqNo", Integer.toString(rsNote.getInt("seq_no")));
			note.put("important", importantMap.get(rsNote.getString("important_id")));
			note.put("noteClass", rsNote.getString("note_class_desc"));
			note.put("noteDesc", rsNote.getString("note_desc"));
			noteList.add(note);
		}
		rsNote.close();
		return noteList;
	}
	
	@RequestMapping(value = "/QsMntNote", method = RequestMethod.GET)
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
			res.put("qsTargetList", ParamSvc.buildSelectOptionByClass(dbu, ParamSvc.c_clsQsTarget, true));
			res.put("qsClassList", ParamSvc.buildSelectOptionByClass(dbu, ParamSvc.c_clsQsClass, false));
			res.put("qsAbilityList", ParamSvc.buildSelectOptionByClass(dbu, ParamSvc.c_clsQsAbility, false));
			res.put("noteImportantList", CodeSvc.buildSelectOptionByKind(dbu, CodeSvc.c_kindQsNoteImport, false));
			res.put("noteClassList", ParamSvc.buildSelectOptionByClass(dbu, ParamSvc.c_clsQsNoteClass, false));
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
		return "QsMntNote";
	}

	/**
	 * 查詢符合帳號、姓名條件的人員
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/QsMntNote_qryMemberList", method = RequestMethod.POST)
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

	@RequestMapping(value = "/QsMntNote_qryQsList", method = RequestMethod.POST)
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

	@RequestMapping(value = "/QsMntNote_qryQs", method = RequestMethod.POST)
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
			if ("D".equals(qryType)) {
				String sqlQryQs =
					  " SELECT qs_name, qs_subject, online_remark, curr_version, stat_flag, depart_id, target_id\n"
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
				res.put("remark", rowQs.get("online_remark"));
				int currVer = (Integer) rowQs.get("curr_version");
				res.put("currVer", Integer.toString(currVer));
				res.put("statFlagDesc", rowQs.get("stat_flag_desc"));
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

	@RequestMapping(value = "/QsMntNote_qryNoteList", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryNoteList(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("教案代碼不可為空白");
			QsSvc.multiCheckQs(dbu, qsId, null, null);
			boolean expired = "Y".equals(req.get("expired"));
			String sortType = req.get("sort");

			res.put("noteList", buildNoteListData(dbu, qsId, sortType, expired));
			res.put("success", true);
			res.put("status", "查詢註記列表完成");
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

	@RequestMapping(value = "/QsMntNote_qryNote", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryNote(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("教案代碼不可為空白");
			String seqNoStr = req.get("seqNo");
			int seqNo;
			try {
				seqNo = Integer.parseInt(seqNoStr);
			}
			catch (NumberFormatException e) {
				throw new StopException("註記序號不是數值");
			}
			String sqlQryNote =
				  " SELECT note_class, important_id, cr_user_id, cr_date, expire_date, note_desc, content\n"
				+ "      , (SELECT user_name FROM mbdetl b WHERE a.cr_user_id = b.user_id) cr_user_name\n"
				+ "   FROM qsnote a\n"
				+ "  WHERE qs_id = ?\n"
				+ "    AND seq_no = ?\n";
			Map<String, Object> rowNote = dbu.selectMapRowList(sqlQryNote, qsId, seqNo);
			if (rowNote == null)
				throw new StopException("教案代碼 <" + qsId + "> 註記序號 <" + seqNo + "> 不存在");
			res.put("userName", rowNote.get("cr_user_name"));
			res.put("noteClass", rowNote.get("note_class"));
			res.put("importantId", rowNote.get("important_id"));
			res.put("crDate", new StdCalendar((String) rowNote.get("cr_date")).toDateString());
			res.put("expireDate", "99999999".equals(rowNote.get("expire_date")) ? "" : new StdCalendar((String) rowNote.get("expire_date")).toDateString());
			res.put("noteDesc", rowNote.get("note_desc"));
			res.put("content", DbUtil.nullToEmpty((String) rowNote.get("content")));

			res.put("success", true);
			res.put("status", "查詢註記完成");
		}
//		catch (StopException e) {
//			res.put("status", e.getMessage());
//		}
//		catch (SQLException e) {
//			res.put("status", DbUtil.exceptionTranslation(e));
//		}
		catch (Exception e) {
			res.put("status", ExceptionUtil.procExceptionMsg(e));
		}
		dbu.relDbConn();
		return res;
	}

	@RequestMapping(value = "/QsMntNote_modQs", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> modQs(@RequestParam Map<String, String> req) {
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
				throw new StopException("教案代碼不可以為空白");	
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleAdmin, QsSvc.c_qsMastFlagOnline);
			String onlineRemark = DbUtil.emptyToNull((String) req.get("remark"));
				
			String sqlQryQs =
				  " SELECT online_remark\n"
				+ "   FROM qsmstr\n"
				+ "  WHERE qs_id = ?\n";
			Map<String, Object> rowQs = dbu.selectMapRowList(sqlQryQs, qsId);
			if (rowQs == null)
				throw new StopException("教案代碼 <" + qsId + "> 不存在");
			String onlineRemarkOrg = (String) req.get("online_remark");

			String sqlUpdQs =
				  " UPDATE qsmstr SET\n"
				+ "        online_remark = ?\n"
				+ "  WHERE qs_id = ?\n";
			dbu.executeList(sqlUpdQs, onlineRemark, qsId);
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "modQs");
			operLog.add("qsId", qsId);
            operLog.add("onlineRemark", onlineRemarkOrg, onlineRemark);
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

	@RequestMapping(value = "/QsMntNote_addNote", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addNote(@RequestParam Map<String, String> req) {
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
				throw new StopException("教案代碼不可以為空白");	
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleAdmin + QsSvc.c_qsRoleNote, QsSvc.c_qsMastFlagOnline);
			String importantId = req.get("importantId");
			if (!CodeSvc.doesCodeExist(dbu, CodeSvc.c_kindQsNoteImport, importantId))
				throw new StopException("請先選擇註記重要性");
			String noteClass = req.get("noteClass");
			if (!ParamSvc.doesParamExist(dbu, ParamSvc.c_clsQsNoteClass, noteClass))
				throw new StopException("請先選擇註記類別");
			String beginDate = new StdCalendar().toDbDateString();
			String expireDate = req.get("expireDate");
			if (!"".equals(expireDate)) {
				if (!StdCalendar.isDateFormat(expireDate))
					throw new StopException("截止日期格式錯誤");
				expireDate = new StdCalendar(expireDate).toDbDateString();
			}
			else
				expireDate = "99999999";
			String userId = ud.getUserId();
			String noteDesc = req.get("noteDesc");
			if (noteDesc.isEmpty())
				throw new StopException("註記說明不可以為空白");
			String content = DbUtil.emptyToNull(req.get("content"));
			boolean expired = "Y".equals(req.get("expired"));
			String sortType = req.get("sort");
			
			String sqlQrySeq =
				  " SELECT COALESCE(MAX(seq_no), 0) + 1 FROM qsnote WHERE qs_id = ?";
			int seqNo = dbu.selectIntList(sqlQrySeq, qsId);
			while (true) {
				try {
					dbu.getSavePoint();
					String sqlInsNote =
						  " INSERT INTO qsnote(qs_id, seq_no, note_class, important_id, cr_date, cr_user_id,\n"
						+ "             expire_date, note_desc, content)\n"
						+ " VALUES(?, ?, ?, ?, ?, ?,  ?, ?, ?)\n";
					dbu.executeList(sqlInsNote, qsId, seqNo, noteClass, importantId, beginDate, userId,
							expireDate, noteDesc, content);
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
			
			OperLog operLog = new OperLog(c_progId, "addNote");
			operLog.add("qsId", qsId);
            operLog.add("seqNo", seqNo);
            operLog.add("importantId", importantId);
            operLog.add("noteClass", noteClass);
            operLog.add("crDate", beginDate);
            operLog.add("crUserId", userId);
            operLog.add("expireDate", expireDate);
            operLog.add("noteDesc", noteDesc);
            operLog.add("content", content);
            operLog.write();

			res.put("noteList", buildNoteListData(dbu, qsId, sortType, expired));
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

	@RequestMapping(value = "/QsMntNote_expNote", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> expNote(@RequestParam Map<String, String> req) {
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
				throw new StopException("教案代碼不可以為空白");	
			QsSvc.multiCheckQs(dbu, qsId, QsSvc.c_qsRoleAdmin + QsSvc.c_qsRoleNote, QsSvc.c_qsMastFlagOnline);
			String seqNoStr = req.get("seqNo");
			int seqNo;
			try {
				seqNo = Integer.parseInt(seqNoStr);
			}
			catch (NumberFormatException e) {
				throw new StopException("註記序號不是數值");
			}
			boolean expired = "Y".equals(req.get("expired"));
			String sortType = req.get("sort");

			String sqlQryNote =
				  " SELECT expire_date FROM qsnote WHERE qs_id = ? AND seq_no = ?\n";
			Map<String, Object> rowNote = dbu.selectMapRowList(sqlQryNote, qsId, seqNo);
			if (rowNote == null)
				throw new StopException("教案代碼 <" + qsId + "> 註記序號 <" + seqNo + "> 不存在");
			String expireDate = (String) rowNote.get("expire_date");
			String today = new StdCalendar().toDbDateString();
			if (expireDate.compareTo(today) <= 0)
				throw new StopException("此教案註記已經設定過期，不可再重複設定");
			
			String sqlUpdNote =
				  " UPDATE qsnote SET\n"
				+ "        expire_date = ?\n"
				+ "  WHERE qs_id = ?\n"
				+ "    AND seq_no = ?\n";
			dbu.executeList(sqlUpdNote, today, qsId, seqNo);
			dbu.doCommit();

			OperLog operLog = new OperLog(c_progId, "expireNote");
			operLog.add("qsId", qsId);
            operLog.add("expireDate", today);
            operLog.write();

			res.put("noteList", buildNoteListData(dbu, qsId, sortType, expired));
			res.put("success", true);
			res.put("status", "設定註記過期完成");
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
