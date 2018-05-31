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
import com.ron.exam.service.ParamSvc;
import com.ron.exam.service.ProgData;
import com.ron.exam.service.UserData;

@EnableWebMvc
@Controller
public class SetParamAction {

	private static final String c_progId = "SetParam";
	
	@RequestMapping(value = "/SetParam", method = RequestMethod.GET)
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

			String sqlQryParam =
				  " SELECT param_class, param_name"
				+ "   FROM cmparm\n"
				+ "  WHERE add_del <> 'Y'\n"
				+ "  ORDER BY show_order\n";
			res.put("paramClassList", MiscTool.buildSelectOptionBySql(dbu, sqlQryParam, "param_class", "param_name", false));
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
		return "SetParam";
	}

	@RequestMapping(value = "/SetParam_qryParamList", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryParamList(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
//			UserData ud = UserData.getUserData();
			res.put("success", false);
			String paramClass = req.get("paramClass");
			if (paramClass.isEmpty())
				throw new StopException("請先指定參數類別代碼");
			String addDel = dbu.selectStringList("SELECT add_del FROM cmparm WHERE param_class = ?", paramClass);
			if (addDel == null)
				throw new StopException("指定的參數類別代碼 <" + paramClass + "> 不存在");
			if ("Y".equals(addDel))
				throw new StopException("指定的參數類別代碼 <" + paramClass + "> 不可設定");
			
			String sqlQryParam =
				  " SELECT param_id \"paramId\", param_name \"paramName\", COALESCE(curr_value, '') \"currValue\"\n"
				+ "      , COALESCE(init_value, '') \"initValue\"\n, editable, show_order \"showOrder\"\n"
				+ "      , (SELECT code_desc FROM cmcode b WHERE b.code_kind = 'PARMTYP' AND a.param_type = b.code_code) \"paramType\"\n"
				+ "   FROM cmpard a\n"
				+ "  WHERE param_class = ?\n"
				+ "  ORDER BY show_order\n";
			res.put("paramList", dbu.selectMapAllList(sqlQryParam, paramClass));
			dbu.doCommit();
			
			res.put("success", true);
			res.put("status", "查詢系統參數列表完成");
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

	@RequestMapping(value = "/SetParam_qryParam", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryParam(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
//			UserData ud = UserData.getUserData();
			res.put("success", false);
			String paramClass = req.get("paramClass");
			if (paramClass.isEmpty())
				throw new StopException("請先指定參數類別代碼");
			String sqlQryClass =
				  " SELECT param_name, add_del\n"
				+ "   FROM cmparm\n"
				+ "  WHERE param_class = ?\n";
			Map<String, Object> rowClass = dbu.selectMapRowList(sqlQryClass, paramClass);
			if (rowClass == null)
				throw new StopException("指定的參數類別代碼 <" + paramClass + "> 不存在");
			if ("Y".equals(rowClass.get("add_del")))
				throw new StopException("指定的參數類別代碼 <" + paramClass + "> 不可設定");
			res.put("paramClass", rowClass.get("param_name"));
			String paramId = req.get("paramId");
			if (paramClass.isEmpty())
				throw new StopException("請先指定參數代碼");
			
			String sqlQryParam =
				  " SELECT param_name, param_type, curr_value, init_value, remark\n"
				+ "      , (SELECT code_desc FROM cmcode b WHERE b.code_kind = 'PARMTYP' AND a.param_type = b.code_code) param_type_desc\n"
				+ "   FROM cmpard a\n"
				+ "  WHERE param_class = ?\n"
				+ "    AND param_id = ?\n";
			Map<String, Object> rowParam = dbu.selectMapRowList(sqlQryParam, paramClass, paramId);
			if (rowParam == null)
				throw new StopException("指定的參數代碼 <" + paramId + "> 不存在");
			res.put("paramName", rowParam.get("param_name"));
			res.put("paramType", rowParam.get("param_type"));
			res.put("paramTypeDesc", rowParam.get("param_type_desc"));
			res.put("currValue", rowParam.get("curr_value"));
			res.put("initValue", rowParam.get("init_value"));
			String remark = DbUtil.nullToEmpty((String) rowParam.get("remark"));
			remark = remark.replaceAll("<br>", "\n");
			res.put("remark", remark);
			dbu.doCommit();
			
			res.put("success", true);
			res.put("status", "查詢系統參數完成");
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

	@RequestMapping(value = "/SetParam_modParam", method = RequestMethod.POST)
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
			String sqlQryAddDel =
				  " SELECT add_del FROM cmparm WHERE param_class = ?\n";
			String addDel = dbu.selectStringList(sqlQryAddDel, paramClass);
			if (addDel == null)
				throw new StopException("指定的參數類別代碼 <" + paramClass + "> 不存在");
			if ("Y".equals(addDel))
				throw new StopException("指定的參數類別代碼 <" + paramClass + "> 不可設定");
			String paramId = req.get("paramId");
			String currValue = req.get("currValue");
			
			String sqlQryParam =
				  " SELECT editable, param_type, param_limit, curr_value, remark\n"
				+ "   FROM cmpard\n"
				+ "  WHERE param_class = ?\n"
				+ "    AND param_id = ?\n";
			Map<String, Object> rowParam = dbu.selectMapRowList(sqlQryParam, paramClass, paramId);
			if (rowParam == null)
				throw new StopException("指定的參數代碼 <" + paramId + "> 不存在");
			boolean editable = "Y".equals(rowParam.get("editable"));
			String paramType = (String) rowParam.get("param_type");
			String paramLimit = (String) rowParam.get("param_limit");
			String currValueOrg = (String) rowParam.get("curr_value");
			
			if (!editable)
				throw new StopException("此參數不可編輯");
			if (!ParamSvc.checkParamType(paramType, paramLimit, currValue))
				throw new StopException("參數格式或資料範圍不正確");
			
			String sqlUpdParam =
				  " UPDATE cmpard SET\n"
				+ "        curr_value = ?\n"
				+ "  WHERE param_class = ?\n"
				+ "    AND param_id = ?\n";
			dbu.executeList(sqlUpdParam, currValue, paramClass, paramId);
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "modParam");
			operLog.add("paramClass", paramClass);
			operLog.add("paramId", paramId);
			operLog.add("currValue", currValueOrg, currValue);
            operLog.write();;
			
            res.put("success", true);
			res.put("status", "修改系統參數完成");
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
