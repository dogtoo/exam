package com.ron.exam.action;

import java.sql.*;
import java.util.HashMap;
import java.util.Iterator;
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
import com.ron.exam.service.MiscTool;
import com.ron.exam.service.ProgData;
import com.ron.exam.service.UserData;

@EnableWebMvc
@Controller
public class MntParamAction {

	private static final String c_progId = "MntParam";
	
	/**
	 * 修改參數代碼時，一併修改相關的資料
	 * @param dbu
	 * @param paramClass
	 * @param paramId
	 * @param paramIdOrg
	 * @throws SQLException
	 */
	private void updateRelatedTable(DbUtil dbu, String paramClass, String paramId, String paramIdOrg) throws SQLException {
		if ("DEPART".equals(paramClass)) {
			String sqlUpdQsmstr = " UPDATE qsmstr SET depart_id = ? WHERE depart_id = ?";
			dbu.executeList(sqlUpdQsmstr, paramId, paramIdOrg);
			String sqlUpdQstmpl = " UPDATE qstpms SET depart_id = ? WHERE depart_id = ?";
			dbu.executeList(sqlUpdQstmpl, paramId, paramIdOrg);
			String sqlUpdMember = " UPDATE mbdetl SET depart_id = ? WHERE depart_id = ?";
			dbu.executeList(sqlUpdMember, paramId, paramIdOrg);
		}
		else if ("QSCLS".equals(paramClass)) {
			String sqlUpdQsClass = " UPDATE qsclss SET class_id = ? WHERE class_type = 'C' AND class_id = ?";
			dbu.executeList(sqlUpdQsClass, paramId, paramIdOrg);
		}
		else if ("QSABL".equals(paramClass)) {
			String sqlUpdQsClass = " UPDATE qsclss SET class_id = ? WHERE class_type = 'A' AND class_id = ?";
			dbu.executeList(sqlUpdQsClass, paramId, paramIdOrg);
		}
		else if ("QSTGT".equals(paramClass)) {
			String sqlUpdQsMstr = " UPDATE qsmstr SET target_id = ? WHERE target_id = ?";
			dbu.executeList(sqlUpdQsMstr, paramId, paramIdOrg);
		}
		else if ("QSFLCLS".equals(paramClass)) {
			String sqlUpdQsfile = " UPDATE qsfile SET file_class = ? WHERE file_class = ?";
			dbu.executeList(sqlUpdQsfile, paramId, paramIdOrg);
			String sqlUpdQstpfl = " UPDATE qstpfl SET file_class = ? WHERE file_class = ?";
			dbu.executeList(sqlUpdQstpfl, paramId, paramIdOrg);
		}
		else if ("QSNTCLS".equals(paramClass)) {
			String sqlUpdQsClass = " UPDATE qsnote SET note_class = ? WHERE note_class = ?";
			dbu.executeList(sqlUpdQsClass, paramId, paramIdOrg);
		}
	}

	/**
	 * 檢查參數是否被其他資料引用
	 * @param dbu
	 * @param paramClass
	 * @param paramIdOrg
	 * @return true: 使用中; false: 未被使用
	 * @throws SQLException
	 */
	private boolean useByRelatedTable(DbUtil dbu, String paramClass, String paramIdOrg) throws SQLException {
		if ("DEPART".equals(paramClass)) {
			String sqlQryQsmstr = " SELECT COUNT(*) FROM qsmstr WHERE depart_id = ?";
			if (dbu.selectIntList(sqlQryQsmstr, paramIdOrg) > 0)
				return true;
			String sqlQryQstmpl = " SELECT COUNT(*) FROM qstpms WHERE depart_id = ?";
			if (dbu.selectIntList(sqlQryQstmpl, paramIdOrg) > 0)
				return true;
			String sqlQryMember = " SELECT COUNT(*) FROM mbdetl WHERE depart_id = ?";
			if (dbu.selectIntList(sqlQryMember, paramIdOrg) > 0)
				return true;
		}
		else if ("QSCLS".equals(paramClass)) {
			String sqlQryQsClass = " SELECT COUNT(*) FROM qsclss WHERE class_type = 'C' AND class_id = ?";
			if (dbu.selectIntList(sqlQryQsClass, paramIdOrg) > 0)
				return true;
		}
		else if ("QSABL".equals(paramClass)) {
			String sqlQryQsClass = " SELECT COUNT(*) FROM qsclss WHERE class_type = 'Ａ' AND class_id = ?";
			if (dbu.selectIntList(sqlQryQsClass, paramIdOrg) > 0)
				return true;
		}
		else if ("QSTGT".equals(paramClass)) {
			String sqlQryQsmstr = " SELECT COUNT(*) FROM qsmstr WHERE target_id = ?";
			if (dbu.selectIntList(sqlQryQsmstr, paramIdOrg) > 0)
				return true;
		}
		else if ("QSFLCLS".equals(paramClass)) {
			String sqlQryQsfile = " SELECT COUNT(*) FROM qsfile WHERE file_class = ?";
			if (dbu.selectIntList(sqlQryQsfile, paramIdOrg) > 0)
				return true;
			String sqlQryQstpfl = " SELECT COUNT(*) FROM qstpfl WHERE file_class = ?";
			if (dbu.selectIntList(sqlQryQstpfl, paramIdOrg) > 0)
				return true;
		}
		else if ("QSNTCLS".equals(paramClass)) {
			String sqlQryQsClass = " SELECT COUNT(*) FROM qsnote WHERE note_class = ?";
			if (dbu.selectIntList(sqlQryQsClass, paramIdOrg) > 0)
				return true;
		}
		return false;
	}
	
	/**
	 * 重新排列同類參數內的所有參數項次，可以特別指定某一參數插在指定位置
	 * @param dbu
	 * @param paramClass
	 * @param paramId 指定插在特定位置的參數
	 * @param showOrder 指定位置，0 表示排在最後面
	 */
	private void paramReorder(DbUtil dbu, String paramClass, String paramId, int showOrder) throws SQLException {
		if (showOrder == 0)
			showOrder = Integer.MAX_VALUE;
		String sqlQryParam = " SELECT param_id FROM cmpard WHERE param_class = ? AND param_id <> '-' ORDER BY show_order";
		String sqlUpdParam = " UPDATE cmpard SET show_order = ? WHERE param_class = ? AND param_id = ?";
		int seq = 1;
		boolean added = false;
		ResultSet rsParam = dbu.queryList(sqlQryParam, paramClass);
		while (rsParam.next()) {
			String id = rsParam.getString("param_id");
			if (seq == showOrder && !added) {
				added = true;
				if (!id.equals(paramId)) {
					dbu.executeList(sqlUpdParam, seq, paramClass, paramId);
					seq++;
				}
			}
			else if (paramId.equals(id))
				continue;
			dbu.executeList(sqlUpdParam, seq, paramClass, id);
			seq++;
		}
		if (!added)
			dbu.executeList(sqlUpdParam, seq, paramClass, paramId);
		rsParam.close();
	}
	
	@RequestMapping(value = "/MntParam", method = RequestMethod.GET)
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
		
		Iterator<Entry<String, Object>> resi = res.entrySet().iterator();
		while (resi.hasNext()) {
			Entry<String, Object> rese = resi.next();
			model.addAttribute(rese.getKey(), rese.getValue());
		}
		return "MntParam";
	}

	@RequestMapping(value = "/MntParam_qryParamClassList", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryParamClassList(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
//			UserData ud = UserData.getUserData();
			String sqlQryParam =
				  " SELECT param_class \"paramClass\", param_name \"paramName\", id_len \"idLen\"\n"
				+ "   FROM cmparm\n"
				+ "  WHERE add_del = 'Y'\n"
				+ "  ORDER BY show_order\n";
			res.put("paramClassList", dbu.selectMapAllList(sqlQryParam));
			dbu.doCommit();
			
			res.put("status", "查詢自定參數類別列表完成");
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

	@RequestMapping(value = "/MntParam_qryParamList", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryParamList(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
//			UserData ud = UserData.getUserData();
			String paramClass = req.get("paramClass");
			if (paramClass.isEmpty())
				throw new StopException("請先指定參數類別代碼");
			String sqlQryParam =
				  " SELECT param_id \"paramId\", param_name \"paramName\", COALESCE(curr_value, '') \"currValue\"\n"
				+ "      , show_order \"showOrder\"\n"
				+ "   FROM cmpard\n"
				+ "  WHERE param_class = ?\n"
				+ "  ORDER BY show_order\n";
			res.put("paramList", dbu.selectMapAllList(sqlQryParam, paramClass));
			dbu.doCommit();
			
			res.put("status", "查詢自定參數列表完成");
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

	@RequestMapping(value = "/MntParam_addParam", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addParam(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			
			String paramClass = req.get("paramClass");
			if ("".equals("paramClass"))
				throw new StopException("請先指定參數類別");
			String sqlQryParamClass = 
				  " SELECT add_del, id_len\n"
				+ "   FROM cmparm\n"
				+ "  WHERE param_class = ?\n";
			Map<String, Object> rowClass = dbu.selectMapRowList(sqlQryParamClass, paramClass);
			if (rowClass == null)
				throw new StopException("參數類別 <" + paramClass + "> 不存在");
			if (!"Y".equals(rowClass.get("add_del")))
				throw new StopException("參數類別代碼 <" + paramClass + "> 不可以新增或刪除");
			int idLen = (Integer) rowClass.get("id_len");
			String paramId = req.get("paramId");
			String msg;
			if ((msg = MiscTool.checkIdString(paramId, idLen)) != null)
				throw new StopException("參數代碼不正確: " + msg);
			String sqlCntParam = " SELECT COUNT(*) FROM cmpard WHERE param_class = ? AND param_id = ?";
			if (dbu.selectIntList(sqlCntParam, paramClass, paramId) > 0)
				throw new StopException("參數代碼 <" + paramId + "> 已存在");
			String paramName = req.get("paramName");
			if (paramName.isEmpty())
				throw new StopException("參數名稱不可以為空白");
			String currValue = DbUtil.emptyToNull(req.get("currValue"));
			int showOrder = 0;
			try {
				if (!req.get("showOrder").isEmpty())
					showOrder = Integer.parseInt(req.get("showOrder"));
			}
			catch (NumberFormatException e) {
				throw new StopException("順序內容必需為數值");
			}
			String remark = DbUtil.emptyToNull(req.get("remark"));

			String sqlInsParam =
				  " INSERT INTO cmpard(param_class, param_id, param_name, editable, show_order,\n"
				+ "        param_type, param_limit, curr_value, init_value, remark)\n"
				+ " VALUES(?, ?, ?, '', 0,  '', '', ?, null, ?)";
			dbu.executeList(sqlInsParam, paramClass, paramId, paramName, currValue, remark);
			paramReorder(dbu, paramClass, paramId, showOrder);
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "addParam");
			operLog.add("paramClass", paramClass);
			operLog.add("paramId", paramId);
			operLog.add("paramName", paramName);
			operLog.add("currValue", currValue);
			operLog.add("showOrder", showOrder);
			operLog.add("remark", remark);
            operLog.write();

            res.put("success", true);
			res.put("status", "新增自定參數完成");
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

	@RequestMapping(value = "/MntParam_modParam", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> modParam(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			
			String paramClass = req.get("paramClass");
			if ("".equals("paramClass"))
				throw new StopException("請先指定參數類別");
			String sqlQryParamClass = 
				  " SELECT add_del, id_len\n"
				+ "   FROM cmparm\n"
				+ "  WHERE param_class = ?\n";
			Map<String, Object> rowClass = dbu.selectMapRowList(sqlQryParamClass, paramClass);
			if (rowClass == null)
				throw new StopException("參數類別 <" + paramClass + "> 不存在");
			if (!"Y".equals(rowClass.get("add_del")))
				throw new StopException("參數類別代碼 <" + paramClass + "> 不可以新增或刪除");
			int idLen = (Integer) rowClass.get("id_len");
			String paramIdOrg = req.get("paramIdOrg");
			String paramId = req.get("paramId");
			String msg;
			if ((msg = MiscTool.checkIdString(paramId, idLen)) != null)
				throw new StopException("參數代碼不正確: " + msg);
			String sqlCntParam = " SELECT COUNT(*) FROM cmpard WHERE param_class = ? AND param_id = ?";
			if (dbu.selectIntList(sqlCntParam, paramClass, paramIdOrg) == 0)
				throw new StopException("原參數代碼 <" + paramIdOrg + "> 不存在");
			if (!paramIdOrg.equals(paramId) && dbu.selectIntList(sqlCntParam, paramClass, paramId) > 0)
				throw new StopException("參數代碼 <" + paramId + "> 已存在");
			String paramName = req.get("paramName");
			if (paramName.isEmpty())
				throw new StopException("參數名稱不可以為空白");
			String currValue = DbUtil.emptyToNull(req.get("currValue"));
			int showOrder = 0;
			try {
				showOrder = Integer.parseInt(req.get("showOrder"));
			}
			catch (NumberFormatException e) {
				throw new StopException("順序內容必需為數值");
			}
			String remark = DbUtil.emptyToNull(req.get("remark"));

			String sqlQryParam =
				  " SELECT param_name, curr_value, show_order, remark\n"
				+ "   FROM cmpard\n"
				+ "  WHERE param_class = ?\n"
				+ "    AND param_id = ?\n";
			Map<String, Object> rowParam = dbu.selectMapRowList(sqlQryParam, paramClass, paramIdOrg);
			if (rowParam == null)
				throw new StopException("原參數代碼 <" + paramIdOrg + "> 不存在");
			String paramNameOrg = (String) rowParam.get("param_name");
			String currValueOrg = (String) rowParam.get("curr_value");
			int showOrderOrg = (Integer) rowParam.get("show_order");
			String remarkOrg = (String) rowParam.get("remark");
			
			String sqlUpdParam =
				  " UPDATE cmpard SET\n"
				+ "        param_id = ?\n"
				+ "      , param_name = ?\n"
				+ "      , curr_value = ?\n"
				+ "      , show_order = ?\n"
				+ "      , remark = ?\n"
				+ "  WHERE param_class = ?\n"
				+ "    AND param_id = ?\n";
			dbu.executeList(sqlUpdParam, paramId, paramName, currValue, showOrder, remark, paramClass, paramIdOrg);
			paramReorder(dbu, paramClass, paramId, showOrder);
			if (!paramId.equals(paramIdOrg)) 
				updateRelatedTable(dbu, paramClass, paramId, paramIdOrg);
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "modParam");
			operLog.add("paramClass", paramClass);
			if (paramId.equals(paramIdOrg))
				operLog.add("paramId", paramId);
			else
				operLog.add("paramId", paramIdOrg, paramId);
			operLog.add("paramName", paramNameOrg, paramName);
			operLog.add("currValue", currValueOrg, currValue);
			operLog.add("showOrder", showOrderOrg, showOrder);
			operLog.add("remark", remarkOrg, remark);
            operLog.write();;
			
            res.put("success", true);
			res.put("status", "修改自定參數完成");
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


	@RequestMapping(value = "/MntParam_delParam", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> delParam(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			
			String paramClass = req.get("paramClass");
			if ("".equals("paramClass"))
				throw new StopException("請先指定參數類別");
			String sqlQryParamClass = 
				  " SELECT add_del, id_len\n"
				+ "   FROM cmparm\n"
				+ "  WHERE param_class = ?\n";
			Map<String, Object> rowClass = dbu.selectMapRowList(sqlQryParamClass, paramClass);
			if (rowClass == null)
				throw new StopException("參數類別 <" + paramClass + "> 不存在");
			if (!"Y".equals(rowClass.get("add_del")))
				throw new StopException("參數類別代碼 <" + paramClass + "> 不可以新增或刪除");
			String paramId = req.get("paramId");
			if (paramId.isEmpty())
				throw new StopException("參數代碼不可以為空白");
			String sqlCntParam = " SELECT COUNT(*) FROM cmpard WHERE param_class = ? AND param_id = ?";
			if (dbu.selectIntList(sqlCntParam, paramClass, paramId) == 0)
				throw new StopException("參數代碼 <" + paramId + "> 不存在");
			if (useByRelatedTable(dbu, paramClass, paramId))
				throw new StopException("此參數仍在使用中，不可以刪除");

			String sqlDelParam =
				  " DELETE FROM cmpard\n"
				+ "  WHERE param_class = ?\n"
				+ "    AND param_id = ?\n";
			dbu.executeList(sqlDelParam, paramClass, paramId);
			paramReorder(dbu, paramClass, paramId, 0);
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "delParam");
			operLog.add("paramClass", paramClass);
			operLog.add("paramId", paramId);
            operLog.write();
			
            res.put("success", true);
			res.put("status", "刪除自定參數完成");
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
