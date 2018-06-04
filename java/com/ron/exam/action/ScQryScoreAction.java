package com.ron.exam.action;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;
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
import com.ron.exam.service.MiscTool;
import com.ron.exam.service.ParamSvc;
import com.ron.exam.service.ProgData;
import com.ron.exam.service.QsSvc;
import com.ron.exam.service.UserData;

@EnableWebMvc
@Controller
public class ScQryScoreAction {

	private static final String c_progId = "ScQryScore";
	
	@RequestMapping(value = "/ScQryScore", method = RequestMethod.GET)
	public String execute(Model model, HttpSession sess) {
		Map<String, Object> res = new HashMap<String, Object>();
		DbUtil dbu = new DbUtil();
		try {
			res.put("status", "");
			res.put("statusTime", new StdCalendar().toTimesString());
			
			UserData ud = UserData.getUserData();
			ProgData pd = ProgData.getProgData();
			res.put("progId",    c_progId);
			res.put("privDesc",  ud.getPrivDesc(c_progId));
			res.put("progTitle", pd.getProgTitle(c_progId));
			res.put("queryHide", ProgData.c_privBaseQuery.equals(ud.getPrivBase(c_progId)) ? "disabled" : "");

			res.put("departList",    ParamSvc.buildSelectOptionByClass(dbu, ParamSvc.c_clsDepart, true));
			res.put("qsTargetList",  ParamSvc.buildSelectOptionByClass(dbu, ParamSvc.c_clsQsTarget, true));
			res.put("qsClassList",   ParamSvc.buildSelectOptionByClass(dbu, ParamSvc.c_clsQsClass, false));
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
		return "ScQryScore";
	}

	/**
	 * 查詢教案列表
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/ScQryScore_qryRdList", method = RequestMethod.POST)
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
			
			StringBuffer sqlQryQs = new StringBuffer( " SELECT rd_id, rd_desc, rd_date, beg_time \n" );
			StringBuffer sqlCntQs = new StringBuffer(" SELECT COUNT(*)");
			StringBuffer sqlCond  = new StringBuffer("   FROM scrdmm \n"
												   + "  WHERE 1 = 1 \n");
			List<Object> params = new ArrayList<Object>();
			
			// 梯次代碼
			String qryRdId = req.get("rdId");
			if (!qryRdId.isEmpty()) {
				sqlCond.append("    AND rd_id LIKE '%' || ? || '%' \n");
				params.add(qryRdId);
			}

			// 梯次名稱
			String qryRdDesc = req.get("rdDesc");
			if (!qryRdDesc.isEmpty()) {
				sqlCond.append("    AND rd_desc LIKE '%' || ? || '%' \n");
				params.add(qryRdDesc);
			}
			
			// 考試日期範圍
			String qryRdBDate = req.get("rdBDate");
			String qryRdEDate = req.get("rdEDate");
			if (!qryRdBDate.isEmpty() && !qryRdEDate.isEmpty()) {
				sqlCond.append("    AND rd_date BETWEEN ? AND ? \n");
				params.add(new StdCalendar(qryRdBDate).toDbDateString());
				params.add(new StdCalendar(qryRdEDate).toDbDateString());
			}
			
			StringBuffer sqlCon   = new StringBuffer();
			// 考官
			String qryExaminer = req.get("examiner");
			if (!qryExaminer.isEmpty()) {
				sqlCon.append("                     AND examiner LIKE '%' || ? || '%' \n");
				params.add(qryExaminer);
			}

			// 考生
			String qryExaminee = req.get("examinee");
			if (!qryExaminee.isEmpty()) {
				sqlCon.append("                     AND examinee LIKE '%' || ? || '%' \n");
				params.add(qryExaminee);
			}
	
			// scrddm集合
			if (sqlCon.length() > 0) {
				sqlCond.append("    AND rd_id IN (SELECT rd_id \n");
				sqlCond.append("                    FROM scrddm \n");
				sqlCond.append("                   WHERE 1 = 1 \n");
				sqlCond.append(sqlCon);
				sqlCond.append("                 ) \n");
			}
			
			// 教案
			String qryQsId = req.get("qsId");
			if (!qryQsId.isEmpty()) {
				sqlCond.append("    AND rd_id IN (SELECT rd_id \n");
				sqlCond.append("                    FROM scrdrm \n");
				sqlCond.append("                   WHERE qs_id LIKE '%' || ? || '%') \n");
				params.add(qryQsId);
			}
			
			sqlQryQs.append(sqlCond);

			// 非換頁查詢時，需先查詢總筆數
			if ("N".equals(mode) || "T".equals(mode)) {
				sqlCntQs.append(sqlCond);
				res.put("total", dbu.selectIntArray(sqlCntQs.toString(), params.toArray()));
			}
			
			StringBuffer sqlOrder = new StringBuffer();
			Map<String, String> orderMap = new HashMap<String, String>() {
				private static final long serialVersionUID = 1l;
				{	put("rdId:A",   "rd_id ASC");
					put("rdId:D",   "rd_id DESC");
					put("rdDate:A", "rd_date ASC");
					put("rdDate:D", "rd_date DESC");
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
			List<Map<String, Object>> rdList = new ArrayList<Map<String, Object>>();
			while (rsQs.next()) {
				Map<String, Object> qs = new HashMap<String, Object>();
				qs.put("rdId",    rsQs.getString("rd_id"));
				qs.put("rdDesc",  rsQs.getString("rd_desc"));
				qs.put("rdDate",  new StdCalendar(rsQs.getString("rd_date")).toDateString());
				qs.put("begTime", rsQs.getString("beg_time"));
				rdList.add(qs);
			}
			rsQs.close();
			res.put("rdList", rdList);
			
			res.put("success", true);
			res.put("status", "查詢評分列表完成");
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
	 * 查詢單一教案內容
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/ScQryScore_qryQs", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryQs(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success", false);
			UserData ud = UserData.getUserData();
						
			res.put("departList", ParamSvc.buildSelectDataByClass(dbu, ParamSvc.c_clsDepart, true));
			res.put("qsTargetList", ParamSvc.buildSelectDataByClass(dbu, ParamSvc.c_clsQsTarget, true));
			res.put("qsClassList", ParamSvc.buildSelectDataByClass(dbu, ParamSvc.c_clsQsClass, false));
			res.put("qsAbilityList", ParamSvc.buildSelectDataByClass(dbu, ParamSvc.c_clsQsAbility, false));		
			res.put("fileClassList", ParamSvc.buildSelectDataByClass(dbu, ParamSvc.c_clsQsFileClass, false));		
			
			String sqlQryTmplAll =
				  " SELECT qstmp_name \"qstmpName\", qstmp_id \"qstmpId\"\n"
				+ "      , (SELECT param_name FROM cmpard b WHERE b.param_class = 'DEPART' AND b.param_id = a.depart_id) \"departName\"\n" 
				+ "   FROM qstpms a\n"
				+ "  ORDER BY depart_id\n";
			res.put("qstmpList", dbu.selectMapAllList(sqlQryTmplAll));
			
			String qsId = req.get("qsId");
			if (qsId != null) {
				String sqlQryMstr =
					  " SELECT qs_name, qs_subject, depart_id, target_id, stat_flag, cr_date, cr_user_id\n"
					+ "      , qstmp_id, file_max_size, focal_remark, edit_remark\n"
					+ "      , (SELECT user_name FROM mbdetl b WHERE a.cr_user_id = b.user_id) cr_user_name\n"
					+ "   FROM qsmstr a\n"
					+ "  WHERE qs_id = ?\n";
				Map<String, Object> rowQs = dbu.selectMapRowList(sqlQryMstr, qsId);
				if (rowQs == null)
					throw new StopException("教案代碼 <" + qsId + "> 不存在");
				res.put("qsName", rowQs.get("qs_name"));
				res.put("qsSubject", rowQs.get("qs_subject"));
				res.put("focal", rowQs.get("focal_remark"));
				res.put("departId", rowQs.get("depart_id"));
				res.put("targetId", rowQs.get("target_id"));
				res.put("fileMaxSize", rowQs.get("file_max_size"));
				res.put("qstmpId", rowQs.get("qstmp_id"));
				res.put("remark", rowQs.get("edit_remark"));
				res.put("allowMod", ud.getUserId().equals(rowQs.get("cr_user_id")) && "N".equals(rowQs.get("stat_flag")));
								
				String sqlQryQsClass =
						  " SELECT class_id FROM qsclss WHERE qs_id = ? AND class_type = ?\n";
				res.put("useQsClass", dbu.selectStringAllList(sqlQryQsClass, qsId, "C"));
				res.put("useQsAbility", dbu.selectStringAllList(sqlQryQsClass, qsId, "A"));
				res.put("useFileClass", dbu.selectStringAllList(sqlQryQsClass, qsId, "F"));
				
				String sqlQryUser = 
						" SELECT role_flag, user_id, begin_date\n"
					  + " ,(SELECT user_name FROM mbdetl b WHERE a.user_id = b.user_id) user_name\n"
					  + "   FROM qsuser a WHERE qs_id = ? AND role_flag IN ('A', 'E', 'V', 'N') ORDER BY seq_no";
				Map<String, String> roleMap = CodeSvc.buildStringMapByKind(dbu, "QSUSRR");
				List<Object> paramsu = new ArrayList<Object>();
				paramsu.add(qsId);
				ResultSet rsUser = dbu.queryArray(sqlQryUser.toString(), paramsu.toArray());			
				List<Map<String, Object>> userList = new ArrayList<Map<String, Object>>();
				while (rsUser.next()) {					
					Map<String, Object> user = new HashMap<String, Object>();
					user.put("roleDesc",  roleMap.get(rsUser.getString("role_flag")));
					user.put("userId", rsUser.getString("user_id"));
					user.put("userName", rsUser.getString("user_name"));
					user.put("date", rsUser.getString("begin_date"));
					user.put("roleFlag", rsUser.getString("role_flag"));
					userList.add(user);
				}
				rsUser.close();
				res.put("userList", userList);						
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
	
	/**
	 * 新增教案
	 * @param req
	 * @return
	 */
	/*@RequestMapping(value = "/ScQryScore_addQs", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addQs(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
            
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			
			String today = new StdCalendar().toDbDateString();
			String qsId = req.get("qsId");
			if ("".equals(qsId)) {
				// 自動取代碼
				for (int i = 1; i < 100 && qsId.isEmpty(); i++) {
					String id = String.format("%s%02d", today, i);
					String sqlCntQsmstr = " SELECT COUNT(*) FROM qsmstr WHERE qs_id = ?";
					if (dbu.selectIntList(sqlCntQsmstr, id) == 0)
						qsId = id;
				}
				if (qsId.isEmpty())
					throw new StopException("無法自動取得教案代碼");
				res.put("qsId", qsId);
			}
			else {
				String msg = MiscTool.checkIdString(qsId, QsSvc.c_qsIdMaxLen);
				if (msg != null)
					throw new StopException("教案代碼" + msg);	
				String sqlCntQsmstr = " SELECT COUNT(*) FROM qsmstr WHERE qs_id = ?";
				if (dbu.selectIntList(sqlCntQsmstr, qsId) > 0)
					throw new StopException("教案代碼 " + qsId + " 已存在");
			}

			String qsName = req.get("qsName");
			if (qsName.isEmpty())
				throw new StopException("教案名稱不可以為空白");
			String qsSubject = DbUtil.emptyToNull(req.get("qsSubject"));
			String focal = req.get("focal");
			String departId = req.get("departId");
			if ("".equals(departId))
				throw new StopException("請先指定科別");
			String targetId = req.get("targetId");
			if (!ParamSvc.checkParamExists(dbu, ParamSvc.c_clsDepart, departId))
				throw new StopException("科別代碼 <" + departId + "> 不存在");
			if ("".equals(targetId))
				throw new StopException("請先指定對象");
			if (!ParamSvc.checkParamExists(dbu, ParamSvc.c_clsQsTarget, targetId))
				throw new StopException("對象代碼 <" + departId + "> 不存在");
			String qstmpId = req.get("qstmpId");
			String remark = req.get("remark");
			int fileMaxSize = 0;
			try {
				fileMaxSize = MiscTool.convFromCommaInt(req.get(("fileMaxSize")));
			}
			catch (NumberFormatException e) {
				throw new StopException("檔案大小限制需填入數字(bytes)，或是 0 (不限制大小)");
			}
			List<String> qsClassList = new ArrayList<String>();
			for (int i = 0; ; i++) {
				String key = "qsClassList[" + i + "]";
				if (!req.containsKey(key))
					break;
				String value = req.get(key);
				qsClassList.add(value);
				if (!ParamSvc.checkParamExists(dbu, ParamSvc.c_clsQsClass, value))
					throw new StopException("教案類別代碼 <" + value + "> 不存在");
			}
			List<String> qsAbilityList = new ArrayList<String>();
			for (int i = 0; ; i++) {
				String key = "qsAbilityList[" + i + "]";
				if (!req.containsKey(key))
					break;
				String value = req.get(key);
				qsAbilityList.add(value);
				if (!ParamSvc.checkParamExists(dbu, ParamSvc.c_clsQsAbility, value))
					throw new StopException("核心能力代碼 <" + value + "> 不存在");
			}
			List<String> fileClassList = new ArrayList<String>();
			for (int i = 0; ; i++) {
				String key = "fileClassList[" + i + "]";
				if (!req.containsKey(key))
					break;
				String value = req.get(key);
				fileClassList.add(value);
				if (!ParamSvc.checkParamExists(dbu, ParamSvc.c_clsQsFileClass, value))
					throw new StopException("檔案類別代碼 <" + value + "> 不存在");
			}
			List<String[]> userList = new ArrayList<String[]>();
			userList.add(new String[] { "A", ud.getUserId() });
			for (int i = 0; ; i++) {				
				String key = "roleFlag[" + i + "]";
				if (!req.containsKey(key))					
					break;
				String roleFlag = req.get(key);
				if ("A".equals(roleFlag))
					continue;
				key = "userId[" + i + "]";
				if (!req.containsKey(key))					
					break;
				String userId = req.get(key);
				userList.add(new String[] { roleFlag, userId });
			}
			StringBuffer userStr = new StringBuffer();
			for (int i = 0; i < userList.size(); i++) {				
				if (userStr.length() > 0)
					userStr.append(',');
				userStr.append(userList.get(i)[0]);
				userStr.append(':');
				userStr.append(userList.get(i)[1]);
			}
			
			String sqlInsQsmstr =
				  " INSERT INTO qsmstr(qs_id, qs_name, qs_subject, depart_id, target_id, stat_flag, curr_version,\n"
				+ "	            cr_date, cr_user_id, qstmp_id, file_max_size, focal_remark, edit_remark)\n"
				+ " VALUES(?, ?, ?, ?, ?, 'N', 0,  ?, ?, ?, ?, ?, ?)\n";
			dbu.executeList(sqlInsQsmstr, qsId, qsName, qsSubject, departId, targetId,
				today, ud.getUserId(), qstmpId, fileMaxSize, focal, remark);
			
			String sqlInsQsclss =
				  " INSERT INTO qsclss(qs_id, class_type, class_id)\n"
				+ " VALUES(?, ?, ?)\n";
			for (int i = 0; i < qsClassList.size(); i++)
				dbu.executeList(sqlInsQsclss, qsId, "C", qsClassList.get(i));
			for (int i = 0; i < qsAbilityList.size(); i++)
				dbu.executeList(sqlInsQsclss, qsId, "A", qsAbilityList.get(i));
			String sqlInsQsuser =
				  " INSERT INTO qsuser(qs_id, seq_no, user_id, role_flag, begin_date, end_date)\n"
				+ " VALUES(?, ?, ?, 'A', ?, '99999999')\n";
			dbu.executeList(sqlInsQsuser, qsId, 1, ud.getUserId(), today);
			dbu.doCommit();
			
			String qsClassStr = MiscTool.concatList(qsClassList, ",");
			String qsAbilityStr = MiscTool.concatList(qsAbilityList, ",");
			String fileClassStr = MiscTool.concatList(fileClassList, ",");
			
			OperLog operLog = new OperLog(c_progId, "addQs");
			operLog.add("qsId", qsId);
			operLog.add("qsName", qsName);
			operLog.add("qsSubject", qsSubject);
			operLog.add("departId", departId);
			operLog.add("targetId", targetId);
			operLog.add("qstmpId", qstmpId);
			operLog.add("qsClass", qsClassStr);
			operLog.add("qsAbility", qsAbilityStr);
			operLog.add("fileClass", fileClassStr);
			operLog.add("crDate", today);
			operLog.add("crUser", ud.getUserId());
			operLog.add("focalRemark", focal);
			operLog.add("onlineRemark", remark);
            operLog.write();
			
            res.put("success", true);
			res.put("status", "新增教案完成");
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
	}*/

	/*@RequestMapping(value = "/ScQryScore_modQs", method = RequestMethod.POST)
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
			
			String qsIdOrg = req.get("qsIdOrg");
			if ("".equals(qsIdOrg))
				throw new StopException("請先查詢教案後再修改");
			String qsId = req.get("qsId");
			if ("".equals(qsId))
				throw new StopException("請先指定教案代碼");
			if (!qsIdOrg.equals(qsId)) {
				String msg = MiscTool.checkIdString(qsId, QsSvc.c_qsIdMaxLen);
				if (msg != null)
					throw new StopException("教案代碼" + msg);	
			}
			String sqlCntMstr = " SELECT COUNT(*) FROM qsmstr WHERE qs_id = ?";
			if (dbu.selectIntList(sqlCntMstr, qsIdOrg) == 0)
				throw new StopException("原教案代碼 <" + qsIdOrg + "> 不存在");
			if (!qsId.equals(qsIdOrg) && dbu.selectIntList(sqlCntMstr, qsId) > 0)
				throw new StopException("教案代碼 <" + qsId + "> 已存在");
			String qsName = req.get("qsName");
			if (qsName.isEmpty())
				throw new StopException("教案名稱不可以為空白");
			String qsSubject = DbUtil.emptyToNull(req.get("qsSubject"));
			String focal = DbUtil.emptyToNull(req.get("focal"));
			String departId = req.get("departId");
			if ("".equals(departId))
				throw new StopException("請先指定科別");
			if (!ParamSvc.checkParamExists(dbu, ParamSvc.c_clsDepart, departId))
				throw new StopException("科別代碼 <" + departId + "> 不存在");
			String targetId = req.get("targetId");
			if ("".equals(targetId))
				throw new StopException("請先指定對象");
			if (!ParamSvc.checkParamExists(dbu, ParamSvc.c_clsQsTarget, targetId))
				throw new StopException("對象代碼 <" + departId + "> 不存在");
			String qstmpId = req.get("qstmpId");
			String remark = DbUtil.emptyToNull(req.get("remark"));
			int fileMaxSize = 0;
			try {
				fileMaxSize = MiscTool.convFromCommaInt(req.get(("fileMaxSize")));
			}
			catch (NumberFormatException e) {
				throw new StopException("檔案大小限制需填入數字(bytes)，或是 0 (不限制大小)");
			}
			List<String> qsClassList = new ArrayList<String>();
			for (int i = 0; ; i++) {
				String key = "qsClassList[" + i + "]";
				if (!req.containsKey(key))
					break;
				String value = req.get(key);
				qsClassList.add(value);
				if (!ParamSvc.checkParamExists(dbu, ParamSvc.c_clsQsClass, value))
					throw new StopException("教案類別代碼 <" + value + "> 不存在");
			}
			List<String> qsAbilityList = new ArrayList<String>();
			for (int i = 0; ; i++) {
				String key = "qsAbilityList[" + i + "]";
				if (!req.containsKey(key))
					break;
				String value = req.get(key);
				qsAbilityList.add(value);
				if (!ParamSvc.checkParamExists(dbu, ParamSvc.c_clsQsAbility, value))
					throw new StopException("核心能力代碼 <" + value + "> 不存在");
			}
			List<String> fileClassList = new ArrayList<String>();
			for (int i = 0; ; i++) {
				String key = "fileClassList[" + i + "]";
				if (!req.containsKey(key))
					break;
				String value = req.get(key);
				fileClassList.add(value);
				if (!ParamSvc.checkParamExists(dbu, ParamSvc.c_clsQsFileClass, value))
					throw new StopException("檔案類別代碼 <" + value + "> 不存在");
			}
			List<String[]> userList = new ArrayList<String[]>();
			userList.add(new String[] { "A", ud.getUserId() });
			for (int i = 0; ; i++) {				
				String key = "roleFlag[" + i + "]";
				if (!req.containsKey(key))					
					break;
				String roleFlag = req.get(key);
				if ("A".equals(roleFlag))
					continue;
				key = "userId[" + i + "]";
				if (!req.containsKey(key))					
					break;
				String userId = req.get(key);
				userList.add(new String[] { roleFlag, userId });
			}
			StringBuffer userStr = new StringBuffer();
			for (int i = 0; i < userList.size(); i++) {				
				if (userStr.length() > 0)
					userStr.append(',');
				userStr.append(userList.get(i)[0]);
				userStr.append(':');
				userStr.append(userList.get(i)[1]);
			}
			
			String qsClassStr = MiscTool.concatList(qsClassList, ",");
			String qsAbilityStr = MiscTool.concatList(qsAbilityList, ",");
			String fileClassStr = MiscTool.concatList(fileClassList, ",");
		
			String sqlQryMstr =
				  " SELECT qs_name, qs_subject, depart_id, target_id, stat_flag, cr_date, cr_user_id\n"
				+ "      , qstmp_id, file_max_size, focal_remark, edit_remark\n"
				+ "   FROM qsmstr a\n"
				+ "  WHERE qs_id = ?\n";
			Map<String, Object> rowMstr = dbu.selectMapRowList(sqlQryMstr, qsIdOrg);
			if (rowMstr == null)
				throw new StopException("教案代碼 <" + qsIdOrg + "> 不存在");
			String qsNameOrg = DbUtil.nullToEmpty((String) rowMstr.get("qs_name"));
			String qsSubjectOrg = DbUtil.nullToEmpty((String) rowMstr.get("qs_subject"));
			String departIdOrg = (String) rowMstr.get("depart_id");
			String targetIdOrg = (String) rowMstr.get("target_id");
			String qsStatOrg = (String) rowMstr.get("stat_flag");
			String focalOrg = DbUtil.nullToEmpty((String) rowMstr.get("focal_remark"));
			String remarkOrg = DbUtil.nullToEmpty((String) rowMstr.get("edit_remark"));
			int fileMaxSizeOrg = (Integer) rowMstr.get("file_max_size");
			String qstmpIdOrg = (String) rowMstr.get("qstmp_id");
			if (!"N".equals(qsStatOrg))
				throw new StopException("只能修改新建的教案（尚未開始編輯）");
			if (!ud.getUserId().equals(rowMstr.get("cr_user_id")))
				throw new StopException("您不是此教案的管理者，不可以修改");
			String sqlQryClass = " SELECT class_id FROM qsclss WHERE qs_id = ? AND class_type = ?\n";
			List<String> qsClassListOrg = dbu.selectStringAllList(sqlQryClass, qsId, "C");
			List<String> qsAbilityListOrg = dbu.selectStringAllList(sqlQryClass, qsId, "A");
			List<String> fileClassListOrg = dbu.selectStringAllList(sqlQryClass, qsId, "F");
			String qsClassStrOrg = MiscTool.concatList(qsClassListOrg, ",");
			String qsAbilityStrOrg = MiscTool.concatList(qsAbilityListOrg, ",");
			String fileClassStrOrg = MiscTool.concatList(fileClassListOrg, ",");
			List<String[]> userListOrg = new ArrayList<String[]>();
			StringBuffer userStrOrg = new StringBuffer();
			String sqlQryUser = " SELECT role_flag, user_id FROM qsuser WHERE qs_id = ? AND role_flag IN ('A', 'E', 'V', 'N') ORDER BY seq_no";
			ResultSet rsUser = dbu.queryList(sqlQryUser, qsId);
			while (rsUser.next()) {
				userListOrg.add(new String[] { rsUser.getString("role_flag"), rsUser.getString("user_id") });
				if (userStrOrg.length() > 0)
					userStrOrg.append(',');
				userStrOrg.append(rsUser.getString("role_flag"));
				userStrOrg.append(':');
				userStrOrg.append(rsUser.getString("user_id"));
			}
			rsUser.close();
			
			String sqlUpdMstr =
				  " UPDATE qsmstr SET\n"
				+ "        qs_id = ?\n"
				+ "      , qs_name = ?\n"
				+ "      , qs_subject = ?\n"
				+ "      , depart_id = ?\n"
				+ "      , target_id = ?\n"
				+ "      , qstmp_id = ?\n"
				+ "      , file_max_size = ?\n"
				+ "      , focal_remark = ?\n"
				+ "      , edit_remark = ?\n"
				+ "  WHERE qs_id = ?\n";
			dbu.executeList(sqlUpdMstr, qsId, qsName, qsSubject, departId, targetId, qstmpId, fileMaxSize, focal, remark, qsIdOrg);
			if (!qsId.equals(qsIdOrg)) {
				String sqlUpdClss = " UPDATE qsclss SET qs_id = ? WHERE qs_id = ?";
				dbu.executeList(sqlUpdClss, qsId, qsIdOrg);
				String sqlUpdUser = " UPDATE qsuser SET qs_id = ? WHERE qs_id = ?";
				dbu.executeList(sqlUpdUser, qsId, qsIdOrg);
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
			if (!fileClassStr.equals(fileClassStrOrg.toString())) {
				dbu.executeList(sqlDelClass, qsId, "F");
				for (int i = 0; i < fileClassList.size(); i++)
					dbu.executeList(sqlInsClass, qsId, "F", fileClassList.get(i));
			}
			String sqlDelUser = " DELETE FROM qsuser WHERE qs_id = ?";
			String sqlInsUser =
				  " INSERT INTO qsuser(qs_id, seq_no, user_id, role_flag, begin_date, end_date)\n"
				+ " VALUES(?, ?, ?, ?, ?, '99999999')\n";
			if (!userStr.equals(userStrOrg)) {
				dbu.executeList(sqlDelUser, qsId);
				String today = new StdCalendar().toDbDateString();
				for (int i = 0; i < userList.size(); i++) {
					String roleFlag = userList.get(i)[0];
					String userId = userList.get(i)[1];
					dbu.executeList(sqlInsUser, qsId, i + 1, userId, roleFlag, today);
				}
			}
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "modQs");
			if (qsId.equals(qsIdOrg))
	            operLog.add("qsId", qsId);
			else
				operLog.add("qsId", qsIdOrg, qsId);
            operLog.add("qsName", qsNameOrg, qsName);
            operLog.add("qsSubject", qsSubjectOrg, qsSubject);
            operLog.add("departId", departIdOrg, departId);
            operLog.add("targetId", targetIdOrg, targetId);
            operLog.add("qstmpId", qstmpIdOrg, qstmpId);
            operLog.add("fileMaxSize", fileMaxSizeOrg, fileMaxSize);
            operLog.add("focalRemark", focalOrg, focal);
            operLog.add("onlineRemark", remarkOrg, remark);
            operLog.add("qsClass", qsClassStrOrg, qsClassStr);
            operLog.add("qsAbility", qsAbilityStrOrg, qsAbilityStr);	            
            operLog.add("fileClass", fileClassStrOrg, fileClassStr);	            
            operLog.add("userList", userStrOrg, userStr);	            
            operLog.write();
			
            res.put("success", true);
			res.put("status", "修改教案完成");
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
	}*/

	/*@RequestMapping(value = "/ScQryScore_delQs", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> delQs(@RequestParam Map<String, String> req) {
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
			String sqlQryMstr = " SELECT stat_flag FROM qsmstr WHERE qs_id = ?";
			String statFlag = dbu.selectStringList(sqlQryMstr, qsId);
			if (!"N".equals(statFlag))
				throw new StopException("只能刪除新建的教案（尚未開始編輯）");
			
			String sqlDelMstr = " DELETE FROM qsmstr WHERE qs_id = ?";
			dbu.executeList(sqlDelMstr, qsId);
			String sqlUpdClss = " DELETE FROM qsclss WHERE qs_id = ?";
			dbu.executeList(sqlUpdClss, qsId);
			String sqlDelUser = " DELETE FROM qsuser WHERE qs_id = ?";
			dbu.executeList(sqlDelUser, qsId);

			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "delQs");
			operLog.add("qsId", qsId);
            operLog.write();
			
            res.put("success", true);
			res.put("status", "刪除教案完成");
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
	}*/
	
	/*@RequestMapping(value = "/ScQryScore_addMemberCheck", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addMemberCheck(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success", false);
			
			String today = new StdCalendar().toDbDateString();
			String userId = req.get("userId").toUpperCase();
			if ("".equals(userId))
				throw new StopException("請先輸入參與人員帳號");
			String userName = MemberSvc.getMemberName(dbu, req.get("userId").toUpperCase());
			if (userName == null)
				throw new StopException("參與人員帳號不存在");
			String userRole = req.get("userRole");
			String sqlCntUser = " SELECT COUNT(*) FROM qsuser WHERE qs_id = ? AND user_id = ? AND role_flag = ?";
			if (dbu.selectIntList(sqlCntUser, req.get("qsId"), userId, userRole) > 0)
				throw new StopException("參與人員已存在");
			
			Map<String, String> roleMap = CodeSvc.buildStringMapByKind(dbu, "QSUSRR");
			Map<String, Object> user = new HashMap<String, Object>();
			user.put("roleDesc",  roleMap.get(userRole));
			user.put("userId", userId);
			user.put("userName", userName);
			user.put("date", today);
			user.put("roleFlag", userRole);
			
			res.put("user", user);
			dbu.doCommit();
			
			res.put("success", true);
			res.put("status", "新增人員資料完成");
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
	}	*/
	
	/**
	 * 教案送出編輯模式
	 * @param req
	 * @return
	 */
	/*@RequestMapping(value = "/ScQryScore_editMode", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> editMode(@RequestParam Map<String, String> req, HttpServletRequest reqData) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success", false);
			UserData ud = UserData.getUserData();
			
			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("請先指定教案編號");
			String sqlQryMstr = " SELECT stat_flag, cr_user_id, qstmp_id FROM qsmstr WHERE qs_id = ?";
			Map<String, Object> rowMstr = dbu.selectMapRowList(sqlQryMstr, qsId);
			if (rowMstr == null)
				throw new StopException("教案代碼 <" + qsId + "> 不存在");
			if (!"N".equals(rowMstr.get("stat_flag")))
				throw new StopException("只能開始編輯新建的教案（尚未開始編輯）");
			if (!ud.getUserId().equals(rowMstr.get("cr_user_id")))
				throw new StopException("您不是教案管理者，不可將此教案送出編輯");
			String sqlCntTmpl = " SELECT COUNT(*) FROM qstpms WHERE qstmp_id = ?";
			if (dbu.selectIntList(sqlCntTmpl, rowMstr.get("qstmp_id")) == 0)
				throw new StopException("指定的教案樣版 <" + rowMstr.get("qstmp_id") + "> 不存在");

			QsSvc.createQs(reqData, dbu, qsId);
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "editMode");
			operLog.add("qsId", qsId);
            operLog.write();

			res.put("success", true);
            res.put("status", "開始編輯教案完成");
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
	}	*/
}
