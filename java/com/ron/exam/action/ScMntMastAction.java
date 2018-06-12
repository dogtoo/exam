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
public class ScMntMastAction {

	private static final String c_progId = "ScMntMast";
	
	@RequestMapping(value = "/ScMntMast", method = RequestMethod.GET)
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

			res.put("departList",    ParamSvc.buildSelectOptionByClass(dbu, ParamSvc.c_clsDepart,    true));
			res.put("qsTargetList",  ParamSvc.buildSelectOptionByClass(dbu, ParamSvc.c_clsQsTarget,  true));
			res.put("qsClassList",   ParamSvc.buildSelectOptionByClass(dbu, ParamSvc.c_clsQsClass,   false));
			res.put("qsAbilityList", ParamSvc.buildSelectOptionByClass(dbu, ParamSvc.c_clsQsAbility, false));
		
			StringBuffer sqlQryOpt = new StringBuffer(
					  " SELECT opt_class, opt_id, opt_desc, no_sel, score \n"
				    + "   FROM scoptm \n"
				    + "  WHERE opt_class LIKE 'M%' \n"
				    + "    AND opt_id <> '-' \n "
				    + "   ORDER BY opt_class, show_order \n");
			List<Object> params = new ArrayList<Object>();
			ResultSet rsOpt = dbu.queryArray(sqlQryOpt.toString(), params.toArray());
			List<Map<String, Object>> optList = new ArrayList<Map<String, Object>>();
			while (rsOpt.next()) {
				Map<String, Object> opt = new HashMap<String, Object>();
				opt.put("optClass", rsOpt.getString("opt_class"));
				opt.put("optId",    rsOpt.getString("opt_id"));
				opt.put("optDesc",  rsOpt.getString("opt_desc"));
				opt.put("noSel",    rsOpt.getString("no_sel"));
				opt.put("score",    rsOpt.getString("score"));
				optList.add(opt);
			}
			rsOpt.close();
// 怎麼回給畫面 0.0
//			res.put("optList", optList);
			model.addAttribute("optList",    optList);
		}
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
		return "ScMntMast";
	}
	
	private List<Map<String, Object>> qryOptClassList(String optType, DbUtil dbu) throws SQLException, Exception{
	    String sqlQryOptClass = 
	            "SELECT opt_class \"optClass\", opt_id \"optId\", opt_desc \"optDesc\" \n"
	          + "  FROM scoptm \n";
    	if (optType.equals("T")) {
    	    sqlQryOptClass +=
    	        " WHERE opt_id = '-' \n"
    	      + "   AND opt_class Like ? || '%' \n";
    	}
    	else if (optType.length() == 3)
    	{
    	    sqlQryOptClass +=
    	        " WHERE opt_id != '-' \n"
    	      + "   AND opt_class = ? \n";
    	}
	    List<Map<String, Object>> optClassList = dbu.selectMapAllList(sqlQryOptClass, optType);
	    return optClassList;
	}
	
	private List<Map<String, Object>> qryOptClassList(int optType, DbUtil dbu) throws SQLException, Exception{
        String sqlQryOptClass = 
                "SELECT opt_class \"optClass\", opt_id \"optId\", opt_desc \"optDesc\" \n"
              + "  FROM scoptm \n"
              + " WHERE opt_id = '-' \n"
              + "   AND fract = ? \n"
              + "   AND opt_type = 'ITE'";
        List<Map<String, Object>> optClassList = dbu.selectMapAllList(sqlQryOptClass, optType);
        return optClassList;
    }
	
	private List<Map<String, Object>> qryQsList(Map<String, String> req, DbUtil dbu) throws SQLException, Exception{
	    
	    int pageRow = Integer.MAX_VALUE;
        int pageAt  = 0;
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
	    
	    // 查詢條件
        StringBuffer sqlQryQs = new StringBuffer(
              " SELECT B.qs_id, B.qs_name, A.total_opt_class, A.total_score, A.pass_score, A.borderline, A.fract, \n"
            + "        ( SELECT COUNT(*) FROM scqstd WHERE qs_id = A.qs_id) crStd \n");
        //StringBuffer sqlCntQs = new StringBuffer(" SELECT COUNT(*)");
        StringBuffer sqlCond = new StringBuffer(
              "   FROM qsmstr B left join scqstm A \n"
            + "     ON A.qs_id = B.qs_id \n"
            + "  WHERE 1=1 \n");
        List<Object> params = new ArrayList<Object>();
        
        // 教案代碼
        String qryQsId = (req.get("qsId") != null)? req.get("qsId") : "";
        if (!qryQsId.isEmpty()) {
            sqlCond.append(" AND B.qs_id LIKE '%' || ? || '%'\n");
            params.add(qryQsId);
        }
        
        // 教案名稱
        String qryQsName = (req.get("qsName") != null)? req.get("qsName") : "";
        if (!qryQsName.isEmpty()) {
            sqlCond.append(" AND B.qs_name LIKE '%' || ? || '%'\n");
            params.add(qryQsName);
        }
                    
        // 科別
        String qryDepart = (req.get("departId")!=null)? req.get("departId") : "";
        if (!qryDepart.isEmpty()) {
            sqlCond.append(" AND B.depart_id = ?\n");
            params.add(qryDepart);
        }
        
        // 對象
        String qryTargetId = (req.get("targetId")!=null)? req.get("targetId") : "";
        if (!qryTargetId.isEmpty()) {
            sqlCond.append(" AND B.target_id = ?\n");
            params.add(qryTargetId);
        }
        
        // 測驗類別
        List<String> qryQsClassList = new ArrayList<String>();
        for (int i = 0; ; i++) {
            String key = "qryQsClass[" + i + "]";
            if (!req.containsKey(key))
                break;
            qryQsClassList.add(req.get(key));
        }
        if (qryQsClassList.size() > 0) {
            sqlCond.append(" AND EXISTS(SELECT class_id FROM qsclss C WHERE a.qs_id = C.qs_id AND C.class_type = 'C' AND class_id IN ");
            DbUtil.buildInSqlParam(sqlCond, params, qryQsClassList);
            sqlCond.append(")\n");
        }
        
        // 核心能力
        List<String> qryAbilityList = new ArrayList<String>();
        for (int i = 0; ; i++) {
            String key = "qryQsAbility[" + i + "]";
            if (!req.containsKey(key))
                break;
            qryAbilityList.add(req.get(key));
        }
        if (qryAbilityList.size() > 0) {
            sqlCond.append(" AND EXISTS(SELECT class_id FROM qsclss C WHERE a.qs_id = C.qs_id AND C.class_type = 'A' AND class_id IN ");
            DbUtil.buildInSqlParam(sqlCond, params, qryAbilityList);
            sqlCond.append(")\n");
        }

        //sqlCntQs.append(sqlCond);
        //res.put("total", dbu.selectIntArray(sqlCntQs.toString(), params.toArray()));
        sqlQryQs.append(sqlCond);
        
        // 排序
        StringBuffer sqlOrder = new StringBuffer();
        Map<String, String> orderMap = new HashMap<String, String>() {
            private static final long serialVersionUID = 1l;
            {   put("qsId:A",          "qs_id ASC");
                put("qsId:D",          "A.qs_id DESC");
                put("qsNamed:A",       "B.qs_name ASC");
                put("qsName:D",        "B.qs_name DESC");
                put("totalOptClass:A", "A.total_opt_class ASC");
                put("totalOptClass:D", "A.total_opt_class DESC");
                put("totalScore:A",    "A.total_score ASC");
                put("totalScore:D",    "A.total_score DESC");
                put("passScore:A",     "A.pass_score ASC");
                put("passScore:D",     "A.pass_score DESC");
                put("borderline:A",    "A.borderline ASC");
                put("borderline:D",    "A.borderline DESC");
                put("fract:A",         "A.fract ASC");
                put("fract:D",         "A.fract DESC");
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

        // 結果
        ResultSet rsQs = dbu.queryArray(sqlQryQs.toString(), params.toArray());
        List<Map<String, Object>> qsList = new ArrayList<Map<String, Object>>();
        while (rsQs.next()) {
            Map<String, Object> qs = new HashMap<String, Object>();
            qs.put("qsId",          (rsQs.getString("qs_id")!=null)?rsQs.getString("qs_id"):"");
            qs.put("qsName",        (rsQs.getString("qs_name")!=null)?rsQs.getString("qs_name"):"");
            qs.put("totalOptClass", (rsQs.getString("total_opt_class")!=null)?rsQs.getString("total_opt_class"):"");
            qs.put("totalScore",    (rsQs.getString("total_score")!=null)?rsQs.getString("total_score"):"");
            qs.put("passScore",     (rsQs.getString("pass_score")!=null)?rsQs.getString("pass_score"):"");
            qs.put("borderline",    (rsQs.getString("borderline")!=null)?rsQs.getString("borderline"):"");
            qs.put("fract",         (rsQs.getString("fract")!=null)?rsQs.getString("fract"):"");
            qs.put("crStd",         Integer.parseInt(rsQs.getString("crStd")) > 0 ? "Y" :"N");
            qsList.add(qs);
        }
        rsQs.close();
        
        return qsList;
	}

	/**
	 * 查詢教案列表
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/ScMntMast_qryQs", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryQs(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {	
			res.put("success",  false);
			
			res.put("totalOptClassList", qryOptClassList("T", dbu));
            res.put("fractList", CodeSvc.buildSelectDataByKind(dbu, CodeSvc.c_kindScFractType, true));
			
			List<Map<String, Object>> qsList = qryQsList(req,dbu);
			res.put("qsList", qsList);
			res.put("total", qsList.size());
			res.put("success", true);
			res.put("status", "查詢題目列表完成");
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
	/*
	@RequestMapping(value = "/ScMntMast_qryQs", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryQs(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success", false);
			res.put("totalOptClassList", qryOptClassList("T", dbu));
			res.put("fractList", qryOptClassList("M", dbu));

			String qsId = req.get("qsId");
			if (qsId != null) {
				StringBuffer sqlQryMstr = new StringBuffer(
					  " SELECT A.qs_id, B.qs_name, A.total_opt_class, A.total_score, A.pass_score, A.borderline, A.fract \n"
					+ "   FROM scqstm A, qsmstr B \n"
				    + "  WHERE A.qs_id = B.qs_id ");
				
				List<Object> params = new ArrayList<Object>();
				
				//教案代碼
				String qryQsId = req.get("qsId");
				if (!qryQsId.isEmpty()) {
					sqlQryMstr.append(" AND A.qs_id LIKE '%' || ? || '%'\n");
					params.add(qryQsId);
				}
				
				String sqlQry = sqlQryMstr.toString();
				Map<String, Object> rowQs = dbu.selectMapRowList(sqlQry, qsId);
				if (rowQs == null)
					throw new StopException("教案代碼 <" + qsId + "> 不存在");
				res.put("qsId",          rowQs.get("qs_id"));
				res.put("qsName",        rowQs.get("qs_name"));
				res.put("totalOptClass", rowQs.get("total_opt_class"));
				res.put("totalScore",    rowQs.get("total_score"));
				res.put("passScore",     rowQs.get("pass_score"));
				res.put("borderline",    rowQs.get("borderline"));
				res.put("fract",         rowQs.get("fract"));
			}
			
			res.put("success", true);
			res.put("status", "查詢題目表頭完成");
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
	
	/**
	 * 新增教案
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/ScMntMast_addQs", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addQs(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
            
            // 基本檢查
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			
			String qsId = req.get("qsId");
			if (qsId.isEmpty())
				throw new StopException("教案代碼不可以為空白");
			String msg = MiscTool.checkIdString(qsId, QsSvc.c_qsIdMaxLen);
			if (msg != null)
				throw new StopException("教案代碼" + msg);	
			String sqlCntQsmstr = " SELECT COUNT(*) FROM qsmstr WHERE qs_id = ?";
			if (dbu.selectIntList(sqlCntQsmstr, qsId) == 0)
				throw new StopException("教案 " + qsId + " 不存在於教案檔");
			String sqlCntScqstm = " SELECT COUNT(*) FROM scqstm WHERE qs_id = ?";
			if (dbu.selectIntList(sqlCntScqstm, qsId) > 0)
				throw new StopException("教案 " + qsId + " 已存在於題目檔");

			String totalOptClass = req.get("totalOptClass");
			if (totalOptClass.isEmpty())
				throw new StopException("整體評分分級不可以為空白");
			if (req.get("totalScore").length() == 0)
				throw new StopException("滿分分數不可以為空白");
			Integer totalScore = Integer.parseInt(req.get("totalScore"));
			if (req.get("passScore").length() == 0)
				throw new StopException("通過分數不可以為空白");
			Integer passScore = Integer.parseInt(req.get("passScore"));
			if (req.get("borderline").length() == 0)
				throw new StopException("邊界分數不可以為空白");
			Integer borderline = Integer.parseInt(req.get("borderline"));
			if (req.get("fract").length() == 0)
				throw new StopException("項次評分級別不可以為空白");
			Integer fract = Integer.parseInt(req.get("fract"));
			
			// 執行
			String sqlInsQsmstr =
				  " INSERT INTO scqstm(qs_id, total_opt_class, total_score, pass_score, borderline, fract) \n"
				+ " VALUES(?, ?, ?, ?, ?, ?)\n";
			dbu.executeList(sqlInsQsmstr, qsId, totalOptClass, totalScore, passScore, borderline, fract);
			dbu.doCommit();

			OperLog operLog = new OperLog(c_progId, "addQs");
			operLog.add("qsId",          qsId);
			operLog.add("totalOptClass", totalOptClass);
			operLog.add("totalScore",    totalScore);
			operLog.add("passScore",     passScore);
			operLog.add("borderline",    borderline);
			operLog.add("fract",         fract);
            operLog.write();
			
            res.put("success", true);
			res.put("status", "新增題目表頭完成");
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
	 * 複製、編輯教案
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/ScMntMast_modQs", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> modQs(@RequestParam Map<String, String> req) {
        Map<String, Object> res = new HashMap<String, Object>();
        res.put("status", "");
        res.put("statusTime", new StdCalendar().toTimesString());

        DbUtil dbu = new DbUtil();
        try {
            res.put("success", false);
            
            // 基本檢查
            UserData ud = UserData.getUserData();
            if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
                throw new StopException("您無權限執行此動作");
            
            String qsId = req.get("qsId");
            if ("".equals(qsId))
                throw new StopException("教案代碼不可以為空白");
            
            String totalOptClass = req.get("totalOptClass");
            if (totalOptClass.isEmpty())
                throw new StopException("整體評分分級不可以為空白");
            if (req.get("totalScore").length() == 0)
                throw new StopException("滿分分數不可以為空白");
            Integer totalScore = Integer.parseInt(req.get("totalScore"));
            if (req.get("passScore").length() == 0)
                throw new StopException("通過分數不可以為空白");
            Integer passScore = Integer.parseInt(req.get("passScore"));
            if (req.get("borderline").length() == 0)
                throw new StopException("邊界分數不可以為空白");
            Integer borderline = Integer.parseInt(req.get("borderline"));
            if (req.get("fract").length() == 0)
                throw new StopException("項次評分級別不可以為空白");
            Integer fract = Integer.parseInt(req.get("fract"));
            
            
            String sqlDelQsmstr =
                    "DELETE FROM scqstm WHERE qs_id = ?";
            dbu.executeList(sqlDelQsmstr, qsId);
            
            String sqlInsQsmstr =
                    " INSERT INTO scqstm(qs_id, total_opt_class, total_score, pass_score, borderline, fract) \n"
                  + " VALUES(?, ?, ?, ?, ?, ?)\n";
            dbu.executeList(sqlInsQsmstr, qsId, totalOptClass, totalScore, passScore, borderline, fract);
            dbu.doCommit();
              
            res.put("success", true);
            res.put("status", "教案資料編輯完成");
        }
        catch (StopException e) {
            res.put("status", e.getMessage());
        }
        catch (Exception e) {
            res.put("status", ExceptionUtil.procExceptionMsg(e));
        }
        dbu.relDbConn();
        
        return res;
    }
	/*
	@RequestMapping(value = "/ScMntMast_modQs", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> modQs(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
            
            // 基本檢查
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			
			String qsIdOrg = req.get("qsIdOrg");
			if ("".equals(qsIdOrg))
				throw new StopException("請先查詢教案後再編輯");
			String qsId = req.get("qsId");
			if ("".equals(qsId))
				throw new StopException("教案代碼不可以為空白");
			if (!qsIdOrg.equals(qsId)) {
				String msg = MiscTool.checkIdString(qsId, QsSvc.c_qsIdMaxLen);
				if (msg != null)
					throw new StopException("教案代碼" + msg);	
			}
			String sqlCntQsmstr = " SELECT COUNT(*) FROM qsmstr WHERE qs_id = ?";
			if (dbu.selectIntList(sqlCntQsmstr, qsId) == 0)
				throw new StopException("教案 " + qsId + " 不存在於教案檔");
			String sqlQryQs =
					  " SELECT total_opt_class, total_score, pass_score, borderline, fract \n"
					+ "   FROM scqstm\n"
					+ "  WHERE qs_id = ?\n";
			Map<String, Object> rowQs = dbu.selectMapRowList(sqlQryQs, qsIdOrg);
			if (rowQs == null)
				throw new StopException("教案 " + qsId + " 不存在於題目檔");
			String totalOptClassOrg = (String) rowQs.get("total_opt_class");
			String totalScoreOrg    = rowQs.get("total_score").toString();
			String passScoreOrg     = rowQs.get("pass_score").toString();
			String borderlineOrg    = rowQs.get("borderline").toString();
			String fractOrg         = rowQs.get("fract").toString();
			String totalOptClass = req.get("totalOptClass");
			if (totalOptClass.isEmpty())
				throw new StopException("整體評分分級不可以為空白");
			if (req.get("totalScore").length() == 0)
				throw new StopException("滿分分數不可以為空白");
			Integer totalScore = Integer.parseInt(req.get("totalScore"));
			if (req.get("passScore").length() == 0)
				throw new StopException("通過分數不可以為空白");
			Integer passScore = Integer.parseInt(req.get("passScore"));
			if (req.get("borderline").length() == 0)
				throw new StopException("邊界分數不可以為空白");
			Integer borderline = Integer.parseInt(req.get("borderline"));
			if (req.get("fract").length() == 0)
				throw new StopException("項次評分級別不可以為空白");
			Integer fract = Integer.parseInt(req.get("fract"));
			
			// 執行
			String sqlUpdMstr =
				  " UPDATE scqstm SET\n"
				+ "        qs_id           = ?\n"
				+ "      , total_opt_class = ?\n"
				+ "      , total_score     = ?\n"
				+ "      , pass_score      = ?\n"
				+ "      , borderline      = ?\n"
				+ "      , fract           = ?\n"
				+ "  WHERE qs_id           = ?\n";
			dbu.executeList(sqlUpdMstr, qsId, totalOptClass, totalScore, passScore, borderline, fract, qsIdOrg);
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "modQs");
			if (qsId.equals(qsIdOrg))
	            operLog.add("qsId", qsId);
			else
				operLog.add("qsId", qsIdOrg, qsId);
            operLog.add("totalOptClass", totalOptClassOrg, totalOptClass);
            operLog.add("totalScore",    totalScoreOrg,    totalScore);
            operLog.add("passScore",     passScoreOrg,     passScore);
            operLog.add("borderline",    borderlineOrg,    borderline);
            operLog.add("fract",         fractOrg,         fract);            
            operLog.write();
			
            res.put("success", true);
			res.put("status", "修改題目表頭完成");
		}
		catch (StopException e) {
			res.put("status", e.getMessage());
		}
		catch (Exception e) {
			res.put("status", ExceptionUtil.procExceptionMsg(e));
		}
		dbu.relDbConn();
		
		return res;
	}
	*/

	/**
	 * 查詢項目內容
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/ScMntMast_qryItem", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryItem(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success", false);
			//res.put("optClassList", CodeSvc.buildSelectRankData(dbu, "M" + req.get("fract"), true, true));
			res.put("optClassList", qryOptClassList(Integer.parseInt(req.get("fract")), dbu));
			
			String qsId = req.get("qsId");
			if (qsId != null) {
				StringBuffer sqlQryItem = new StringBuffer (
						  " SELECT item_no, item_desc, opt_class, tip \n"
						+ "   FROM scqstd \n");
//						+ "  WHERE qs_id = ? \n");
				
				// 教案代碼
				String qryQsId = req.get("qsId");
				List<Object> params = new ArrayList<Object>();
				if (!qryQsId.isEmpty()) {
					sqlQryItem.append(" WHERE qs_id = ? \n");
					params.add(qryQsId);
				}
				
				ResultSet rsQs = dbu.queryArray(sqlQryItem.toString(), params.toArray());
				List<Map<String, Object>> itemList = new ArrayList<Map<String, Object>>();
				while (rsQs.next()) {
					Map<String, Object> qs = new HashMap<String, Object>();
					qs.put("fItemNo",   rsQs.getString("item_no"));
					qs.put("fItemDesc", rsQs.getString("item_desc"));
					qs.put("fTip",      rsQs.getString("tip"));
					qs.put("fOptClass", rsQs.getString("opt_class"));
					itemList.add(qs);
				}
				rsQs.close();
				res.put("itemList", itemList);
			}
			
			res.put("success", true);
			res.put("status", "查詢題目表頭完成");
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
	 * 新增項目
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/ScMntMast_addItem", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addItem(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
            
            // 基本檢查
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			
			String qsId     = req.get("qsId");
			/*String itemNo   = req.get("itemNo");
			String itemDesc = req.get("itemDesc");
			String optClass = req.get("optClass");
			String tip      = req.get("tip");
			if (qsId.isEmpty())
				throw new StopException("教案代碼不可以為空白");
			String msg = MiscTool.checkIdString(qsId, QsSvc.c_qsIdMaxLen);
			if (msg != null)
				throw new StopException("教案代碼" + msg);	
			String sqlCntQsmstr = " SELECT COUNT(*) FROM qsmstr WHERE qs_id = ?";
			if (dbu.selectIntList(sqlCntQsmstr, qsId) == 0)
				throw new StopException("教案 " + qsId + " 不存在於教案檔");
			String sqlCntScqstm = " SELECT COUNT(*) FROM scqstm WHERE qs_id = ?";
			if (dbu.selectIntList(sqlCntScqstm, qsId) == 0)
				throw new StopException("教案 " + qsId + " 不存在於題目檔");
			String sqlCntScqstd = " SELECT COUNT(*) FROM scqstm WHERE qs_id = ? AND item_no = ?";
			if (dbu.selectIntList(sqlCntScqstd, qsId, itemNo) == 0)
				throw new StopException("項次 " + itemNo + " 已存在");

			if (itemDesc.isEmpty())
				throw new StopException("說明不可以為空白");
			if (optClass.isEmpty())
				throw new StopException("評分級數不可以為空白");*/
			
			List<itemData> itemList = new ArrayList<itemData>();
			for (int i = 0; ; i++) {
				itemData item = new itemData();
				String key = "itemNo[" + i + "]";
				if (!req.containsKey(key))
					break;
				item.itemNo   = req.get(key);
				item.optClass = req.get("optClass[" + i + "]");
				item.itemDesc = req.get("itemDesc[" + i + "]");
				item.tip      = req.get("tip[" + i + "]");
				itemList.add(item);
			}

			// 執行
			String sqlDelD = " DELETE FROM scqstd WHERE qs_id = ?";
			dbu.executeList(sqlDelD, qsId);
			String sqlInsQsmstr =
				  " INSERT INTO scqstd(qs_id, item_no, item_desc, opt_class, tip) \n"
				+ " VALUES (?, ?, ?, ?, ?) \n ";
			for (int i = 0; i < itemList.size(); i++) {
				itemData item = itemList.get(i);
				dbu.executeList(sqlInsQsmstr, qsId, Integer.parseInt(item.itemNo), item.itemDesc, item.optClass, item.tip);
			}
			dbu.doCommit();

			/*OperLog operLog = new OperLog(c_progId, "addItem");
			operLog.add("qsId",     qsId);
			operLog.add("itemNo",   itemNo);
			operLog.add("itemDesc", itemDesc);
			operLog.add("optClass", optClass);
			operLog.add("tip",      tip);
            operLog.write();*/
			
            res.put("success", true);
			res.put("status", "新增項次完成");
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
	
	private static class itemData {
		public String itemNo;
		public String optClass;
		public String itemDesc;
		public String tip;
		
		/*public static String stringify(List<itemData> itemList) {
			StringBuffer str = new StringBuffer();
			for (int i = 0; i < itemList.size(); i++) {
				itemData item = itemList.get(i);
				if (i > 0)
					str.append(',');
				str.append('<');
				str.append(item.itemNo);
				str.append(',');
				str.append(item.optClass);
				str.append(',');
				str.append(item.itemDesc);
				str.append(',');
				str.append(item.tip);
				str.append('>');
			}
			return str.toString();
		}*/
	}
	
	/**
	 * 更新項目
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/ScMntMast_updItem", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> updItem(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
            
            // 基本檢查
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			
			String qsId      = req.get("qsId");
			String itemNoOrg = req.get("itemNoOrg");
			String itemNo    = req.get("itemNo");
			String itemDesc  = req.get("itemDesc");
			String optClass  = req.get("optClass");
			String tip       = req.get("tip");
			if (qsId.isEmpty())
				throw new StopException("教案代碼不可以為空白");
			String msg = MiscTool.checkIdString(qsId, QsSvc.c_qsIdMaxLen);
			if (msg != null)
				throw new StopException("教案代碼" + msg);
			if (itemNoOrg.isEmpty())
				throw new StopException("原項目代碼不可以為空白");
			if (itemNo.isEmpty())
				throw new StopException("項目代碼不可以為空白");
			String sqlCntQsmstr = " SELECT COUNT(*) FROM qsmstr WHERE qs_id = ?";
			if (dbu.selectIntList(sqlCntQsmstr, qsId) == 0)
				throw new StopException("教案 " + qsId + " 不存在於教案檔");
			String sqlCntScqstm = " SELECT COUNT(*) FROM scqstm WHERE qs_id = ?";
			if (dbu.selectIntList(sqlCntScqstm, qsId) == 0)
				throw new StopException("教案 " + qsId + " 不存在於題目檔");
			String sqlCntScqstd = " SELECT COUNT(*) FROM scqstm WHERE qs_id = ? AND item_no = ?";
			if (dbu.selectIntList(sqlCntScqstd, qsId, itemNo) == 0)
				throw new StopException("項次 " + itemNo + " 已存在");

			if (itemDesc.isEmpty())
				throw new StopException("說明不可以為空白");
			if (optClass.isEmpty())
				throw new StopException("評分級數不可以為空白");
			
			// 執行
			String sqlInsQsmstr =
				  " UPDATE scqstd SET \n"
				+ "      , item_no   = ? \n"
				+ "      , item_desc = ? \n"
				+ "      , opt_class = ? \n"
				+ "      , tip       = ? \n"
				+ "  WHERE qs_id     = ? \n"
				+ "    AND item_no   = ? \n";
			dbu.executeList(sqlInsQsmstr, itemNo, itemDesc, optClass, tip, qsId, itemNoOrg);
			dbu.doCommit();

			OperLog operLog = new OperLog(c_progId, "updItem");
			if (itemNo.equals(itemNoOrg))
	            operLog.add("itemNo", itemNo);
			else
				operLog.add("itemNo", itemNoOrg, itemNo);
//			operLog.add("qsId",     qsId);
//			operLog.add("itemDesc", itemDescOrg, itemDesc);
//            operLog.add("optClass", optClassOrg, optClass);
//            operLog.add("tip",      tipOrg,      tip); 
//            operLog.write();
			
            res.put("success", true);
			res.put("status", "新增項次完成");
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
	 * 刪除教案
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/ScMntMast_delQs", method = RequestMethod.POST)
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
			
			String sqlDelM = " DELETE FROM scqstm WHERE qs_id = ?";
			dbu.executeList(sqlDelM, qsId);
			String sqlDelD = " DELETE FROM scqstd WHERE qs_id = ?";
			dbu.executeList(sqlDelD, qsId);

			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "delQs");
			operLog.add("qsId", qsId);
            operLog.write();
			
            res.put("success", true);
			res.put("status", "刪除題目完成");
		}
		catch (StopException e) {
			res.put("status", e.getMessage());
		}
		catch (Exception e) {
			res.put("status", ExceptionUtil.procExceptionMsg(e));
		}
		dbu.relDbConn();
		
		return res;
	}
}
