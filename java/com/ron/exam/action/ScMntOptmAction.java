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
public class ScMntOptmAction {

	private static final String c_progId = "ScMntOptm";
	
	private static class OptData {
		public String optClass;
		public String optId;
		public String optDesc;
		public int showOrder;
		public String noSel;
		public int scope;
		public int fract;
		public String optType;
		
		public static String stringify(List<OptData> optList) {
			StringBuffer str = new StringBuffer();
			for (int i = 0; i < optList.size(); i++) {
				OptData opt = optList.get(i);
				if (i > 0)
					str.append(',');
				str.append('<');
				str.append(opt.optClass);
				str.append(',');
				str.append(opt.optId);
				str.append(',');
				str.append(opt.optDesc);
				str.append(',');
				str.append(opt.showOrder);
				str.append(',');
				str.append(opt.noSel);
				str.append(',');
				str.append(opt.scope);
				str.append(',');
				str.append(opt.fract);
				str.append(',');
				str.append(opt.optType);
				str.append('>');
			}
			return str.toString();
		}
	}
	
	@RequestMapping(value = "/ScMntOptm", method = RequestMethod.GET)
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
			res.put("scFractList", CodeSvc.buildSelectOptionByKind(dbu, CodeSvc.c_kindScFractType, true));
			res.put("scOptTypeList", CodeSvc.buildSelectOptionByKind(dbu, CodeSvc.c_kindScOptType, true));
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
		return "ScMntOptm";
	}

	@RequestMapping(value = "/ScMntOptm_qryOptMList", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryOptMList(@RequestParam Map<String, String> req) {
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
			
			StringBuffer sqlQryOpt = new StringBuffer(
				  " SELECT opt_class, opt_desc, fract, opt_type\n"
			    + "      , (SELECT COUNT(*) FROM scoptm b WHERE a.opt_class = b.opt_class AND opt_id != '-') d_cnt\n");
			StringBuffer sqlCntOpt = new StringBuffer(" SELECT COUNT(*)");
			StringBuffer sqlCond = new StringBuffer(
				  "   FROM scoptm a\n"
				+ "  WHERE 1 = 1 \n"
				+ "    AND opt_id = '-'");
			
			List<Object> params = new ArrayList<Object>();
			
			//評分類別
			String qryOptClass = req.get("optClass");
			if (!qryOptClass.isEmpty()) {
				sqlCond.append(" AND opt_class like ?||'%'\n");
				params.add(qryOptClass);
			}
			
			//類別說明
			String qryOptDesc = req.get("optDesc");
			if (!qryOptDesc.isEmpty()) {
				sqlCond.append(" AND opt_desc like '%'||?||'%'\n");
				params.add(qryOptDesc);
			}
						
			//評分級別
			String qryFract = req.get("fract");
			if (!qryFract.isEmpty()) {
				sqlCond.append(" AND fract = ?\n");
				params.add(Integer.parseInt( qryFract) );
			}
			
			//選項區分
			String qryOptType = req.get("optType");
			if (!qryOptType.isEmpty()) {
				sqlCond.append(" AND opt_type = ?\n");
				params.add(qryOptType);
			}
			
			// 非換頁查詢時，需先查詢總筆數
			if ("N".equals(mode) || "T".equals(mode)) {
				sqlCntOpt.append(sqlCond);
				res.put("total", dbu.selectIntArray(sqlCntOpt.toString(), params.toArray()));
			}
			
			sqlQryOpt.append(sqlCond);
			StringBuffer sqlOrder = new StringBuffer();
			Map<String, String> orderMap = new HashMap<String, String>() {
				private static final long serialVersionUID = 1l;
				{	put("optClass:A", "opt_class ASC");
					put("optClass:D", "opt_class DESC");
					put("optDesc:A", "opt_desc ASC");
					put("optDesc:D", "opt_desc DESC");
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
			sqlQryOpt.append(sqlOrder);
			if (pageRow != 0) {
				sqlQryOpt.append(" OFFSET ? LIMIT ?\n");
				params.add(pageAt * pageRow);
				params.add(pageRow);
			}

			ResultSet rsOpt = dbu.queryArray(sqlQryOpt.toString(), params.toArray());
			Map<String, String> optTypeMap = CodeSvc.buildStringMapByKind(dbu, CodeSvc.c_kindScOptType);
			List<Map<String, Object>> optList = new ArrayList<Map<String, Object>>();
			while (rsOpt.next()) {
				Map<String, Object> opt = new HashMap<String, Object>();
				opt.put("optClass", rsOpt.getString("opt_class"));
				opt.put("optDesc", rsOpt.getString("opt_desc"));
				opt.put("fract", rsOpt.getString("fract"));
				String optType = rsOpt.getString("opt_type");
				opt.put("optType", optType);
				opt.put("optTypeStr", optTypeMap.containsKey( optType ) ? optTypeMap.get( optType ) : optType);
				opt.put("dCnt", rsOpt.getInt("d_cnt"));
				optList.add(opt);
			}
			rsOpt.close();
			res.put("optMList", optList);
			
			res.put("success", true);
			res.put("status", "查詢評分類別列表完成");
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

	@RequestMapping(value = "/ScMntOptm_qryOptDList", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryOptDList(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success",  false);
			
			StringBuffer sqlQryOpt = new StringBuffer(
				  " SELECT opt_class, opt_id, opt_desc, show_order, no_sel, score, fract\n");
			StringBuffer sqlCond = new StringBuffer(
				  "   FROM scoptm \n"
				+ "  WHERE 1 = 1 \n"
				+ "    AND opt_id !='-'\n"		  );
			
			List<Object> params = new ArrayList<Object>();
			
			//評分類別
			String qryOptClass = req.get("optClass");
			sqlCond.append(" AND opt_class = ?\n");
			params.add(qryOptClass);
			
			sqlCond.append(" ORDER BY show_order\n");
			sqlQryOpt.append( sqlCond );
//System.out.println("ScMntOptmAction:qryOptClass=" +qryOptClass + "\n"+ sqlQryOpt);
			ResultSet rsOpt = dbu.queryArray(sqlQryOpt.toString(), params.toArray());
			List<Map<String, Object>> optList = new ArrayList<Map<String, Object>>();
			while (rsOpt.next()) {
				Map<String, Object> opt = new HashMap<String, Object>();
				opt.put("optId", rsOpt.getString("opt_id"));
				opt.put("optDesc", rsOpt.getString("opt_desc"));
				opt.put("noSel", "Y".equals( rsOpt.getString("no_sel") ) ? "是" : "");
				opt.put("score", rsOpt.getString("score"));
				optList.add(opt);
			}
			rsOpt.close();
			res.put("optDList", optList);
			
			res.put("success", true);
			res.put("status", "查詢評分類別列表完成");
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

	@RequestMapping(value = "/ScMntOptm_addOptClass", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addOptClass(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");

			String msg;
			String optClass = req.get("optClass");
			String sqlCnt = " SELECT COUNT(*) FROM scoptm WHERE opt_class = ? and opt_id = '-'";
			if (dbu.selectIntList(sqlCnt, optClass) > 0)
				throw new StopException("評分類別代碼已存在 '" + optClass + "' 已存在");
			if ((msg = MiscTool.checkIdString(optClass, 3)) != null)
				throw new StopException("評分類別代碼" + msg);
			String optDesc = req.get("optDesc");
			if (optDesc.isEmpty())
				throw new StopException("類別說明不可以為空白");
			String optType = req.get("optType");
			if (optType.isEmpty())
				throw new StopException("選項區分不可以為空白");
			String fractStr = req.get("fract");
			if (fractStr.isEmpty())
				throw new StopException("評分級別不可以為空白");
			
			Map<String, String> optTypeMap = CodeSvc.buildStringMapByKind(dbu, CodeSvc.c_kindScOptType);
			Map<String, String> fractTypeMap = CodeSvc.buildStringMapByKind(dbu, CodeSvc.c_kindScFractType);
			
			if( !optTypeMap.containsKey( optType ) )
				throw new StopException("不正確的選項區分 '" + optType + "'");
			
			if( !fractTypeMap.containsKey( fractStr ) )
				throw new StopException("不正確的評分級別 '" + fractStr + "'");
			
			
			String sqlIns =
				  " INSERT INTO scoptm(opt_class, opt_id, opt_desc, show_order, score, fract, opt_type)\n"
				+ " VALUES(?, '-', ?, 0, 0, ?, ?)\n";
			dbu.executeList(sqlIns, optClass, optDesc, Integer.parseInt(fractStr), optType);
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "addOptClass");
			operLog.add("optClass", optClass);
			operLog.add("optDesc", optDesc);
			operLog.add("fract", fractStr);
			operLog.add("optType", optType);
			operLog.write();
			
            res.put("success", true);
			res.put("status", "新增評分類別完成");
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

	@RequestMapping(value = "/ScMntOptm_modOptClass", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> modOptClass(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			
			String optClass = req.get("optClass");
			String sqlCnt = " SELECT COUNT(*) FROM scoptm WHERE opt_class = ? and opt_id = '-'";
			if (dbu.selectIntList(sqlCnt, optClass) <= 0)
				throw new StopException("評分類別代碼不存在 '" + optClass + "' 不存在");
			
			String optDesc = req.get("optDesc");
			if (optDesc.isEmpty())
				throw new StopException("類別說明不可以為空白");
			String optType = req.get("optType");
			if (optType.isEmpty())
				throw new StopException("選項區分不可以為空白");
			String fractStr = req.get("fract");
			if (fractStr.isEmpty())
				throw new StopException("評分級別不可以為空白");
			
			Map<String, String> optTypeMap = CodeSvc.buildStringMapByKind(dbu, CodeSvc.c_kindScOptType);
			Map<String, String> fractTypeMap = CodeSvc.buildStringMapByKind(dbu, CodeSvc.c_kindScFractType);
			
			if( !optTypeMap.containsKey( optType ) )
				throw new StopException("不正確的選項區分 '" + optType + "'");
			
			if( !fractTypeMap.containsKey( fractStr ) )
				throw new StopException("不正確的評分級別 '" + fractStr + "'");
			
			
			// 2018/05/27 by sam 應該只允許改修改「選項說明」，使用者會比較好控管
			String sqlUpd =
				  " UPDATE scoptm SET\n"
				+ "        opt_desc = ?\n"
			//	+ "      , fract = ?\n"
			//	+ "      , opt_type = ?\n"
				+ "  WHERE opt_class = ?\n";
			//dbu.executeList(sqlUpd, optDesc, Integer.parseInt(fractStr), optType, optClass);
			dbu.executeList(sqlUpd, optDesc, optClass);
			
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "modOptClass");
			operLog.add("optClass", optClass);
			operLog.add("optDesc", optDesc);
			operLog.add("fract", fractStr);
			operLog.add("optType", optType);
			operLog.write();
			
            res.put("success", true);
			res.put("status", "修改評分類別完成");
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

	@RequestMapping(value = "/ScMntOptm_delOptClass", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> delOptClass(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");

			String optClass = req.get("optClass");
			if ("".equals(optClass))
				throw new StopException("請先指定評分類別");

			String sqlQstm = " SELECT QS_ID from scqstm WHERE total_opt_class=?\n";
			String qsId = dbu.selectStringList(sqlQstm, optClass);
			if ( qsId != null )
				throw new StopException("評分題目檔，教案代碼 '" + qsId + "' 已存在此評分類別代碼  '" + optClass + "'");
			
			String sqlQstd = " SELECT qs_id from scqstd WHERE opt_class=?\n";
			qsId = dbu.selectStringList(sqlQstd, optClass);
			if ( qsId != null )
				throw new StopException("評分項次檔，教案代碼 '" + qsId + "' 已存在此評分類別代碼  '" + optClass + "'");
			
			String sqlDel = " DELETE FROM scoptm WHERE opt_class = ?";
			dbu.executeList(sqlDel, optClass);
			
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "delOptClass");
			operLog.add("optClass", optClass);
            operLog.write();;
			
            res.put("success", true);
			res.put("status", "刪除評分類別完成");
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

	@RequestMapping(value = "/ScMntOptm_modOptItems", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> modOptItems(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			
			String optClass = req.get("optClass");
			String sqlM = " SELECT opt_class, fract, opt_type FROM scoptm WHERE opt_class = ? and opt_id = '-'";
			Map<String, Object> optm = dbu.selectMapRowList(sqlM, optClass);
			if (optm == null)
				throw new StopException("評分類別代碼不存在 '" + optClass + "' 不存在");
			
			String msg;
			List<OptData> optList = new ArrayList<OptData>();
			for (int i = 0; ; i++) {
				OptData opt = new OptData();
				opt.optClass = optClass;
				opt.showOrder = i + 1;
				opt.fract = (Integer)optm.get("fract");
				opt.optType = (String)optm.get("opt_type");
				
				String key = "optId[" + i + "]";
				if (!req.containsKey(key))
					break;
				opt.optId = req.get( key );				
				if ((msg = MiscTool.checkIdString(opt.optId, 3)) != null)
					throw new StopException("評分代碼 '" + opt.optId + "' " + msg);
				
				opt.optDesc = req.get("optDesc[" + i + "]");
				if (opt.optDesc == null || opt.optDesc.isEmpty())
					throw new StopException("評分代碼 '" + opt.optId + "' 評分項目說明不可以為空白！");
				opt.noSel = "是".equals( req.get("noSel[" + i + "]") ) ? "Y" : null;
				String scopeStr = req.get("score[" + i + "]");
				if( !scopeStr.matches("^\\d+$") )
					throw new StopException("評分代碼 '" + opt.optId + "' 答案分數須為數字！");
				opt.scope = new Integer( scopeStr );
				optList.add(opt);
			}
//System.out.println("OptData.stringify(optList)=" + OptData.stringify(optList));			
			String sqlDel = "DELETE FROM scoptm WHERE opt_class = ? AND opt_id != '-' ";
			dbu.executeList(sqlDel, optClass);
			
			
			String sqlIns =
					  " INSERT INTO scoptm(opt_class, opt_id, opt_desc, show_order, no_sel, score, fract, opt_type)\n"
					+ " VALUES(?, ?, ?, ?, ?, ?, ?, ?)\n";
			for(int i=0; i<optList.size(); i++){
				OptData opt = optList.get(i);
				dbu.executeList(sqlIns, opt.optClass, opt.optId, opt.optDesc, opt.showOrder, opt.noSel, opt.scope, opt.fract, opt.optType);
			}
			
			
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "modOptItems");
			operLog.add("optClass", optClass);
			operLog.add("optItems", OptData.stringify(optList));
			operLog.write();
			
            res.put("success", true);
			res.put("status", "儲存評分評分項目完成");
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
