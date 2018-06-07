package com.ron.exam.action;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;
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
public class ScSetScoreAction {

	private static final String c_progId = "ScSetScore";
	
	@RequestMapping(value = "/ScSetScore", method = RequestMethod.GET)
	//public String execute(Model model, HttpSession sess) {
	public String execute(Model model, @RequestParam Map<String, String> req) {
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
			res.put("queryHide", ProgData.c_privBaseQuery.equals(ud.getPrivBase(c_progId)) ? "visibility: hidden;" : "");

			// 評分查詢 -> 梯次表 -> 評分表
			String rdId = req.get("rdId");
			String sectSeq = (req.get("sectSeq")!=null)? req.get("sectSeq") : "";
			String roomSeq = (req.get("roomSeq")!=null)? req.get("roomSeq") : "";
			String progId = null;
			JSONObject paneBackup = new JSONObject();
			// 紀錄跳頁的作業名稱，用List去存，最後一筆就會是呼叫本作業的作業名稱
			List<String> progIdList = new ArrayList<String>();
			for (int i = 0; ; i++) {
				String key = "p_b_progId[" + i + "]";
				if (!req.containsKey(key))
					break;
				progId = req.get(key);
				progIdList.add(progId);
				
				Map<String, String> p_b_ = new HashMap<String, String>();
                for (int p = 0; ; p++) {
                    String p_b_key = "p_b_"+ progId +"[" + p + "]";
                    if (!req.containsKey(p_b_key))
                        break;
                    String queryCol = req.get(p_b_key);
                    String[] col = queryCol.split(":", -1);
                    p_b_.put(col[0], col[1]);
                }
                paneBackup.put(progId, p_b_);
			}
			paneBackup.put(    "progId",  progIdList);
			model.addAttribute("bProgId", progId);
			model.addAttribute("rdId", rdId);
			
			//是別的畫面跳過來的要做預先查詢
			if (!sectSeq.equals("") && !roomSeq.equals("")) {
			    model.addAttribute("sectSeq", sectSeq); //對到考生
	            model.addAttribute("roomSeq", roomSeq); //對到教案
			    
    			//查考生 S
    			Map<String, String> qryExaminee = new HashMap<String, String>();
    			qryExaminee.put("rdId", rdId);
    			qryExaminee.put("sectSeq", sectSeq);
    			qryExaminee.put("roomSeq", roomSeq);
    			qryExaminee.put("qryType", "SCRDDM");
    			ScRunDownAction scRunDown = new ScRunDownAction();
    			List<Map<String,Object>> rowRddmList = new ArrayList<Map<String,Object>>();
    			rowRddmList = scRunDown.qryRdrmList(qryExaminee, dbu);
    			if (rowRddmList.size() == 0)
                    throw new StopException("查無考生資料");
                Map<String,Object> rowRddm = new HashMap<String,Object>();
                rowRddm = rowRddmList.get(0);
                String examineeText = (rowRddm.get("examineeName")!=null)? (String)rowRddm.get("examineeName") : "";
                String examinee = (rowRddm.get("examinee")!=null)? (String) rowRddm.get("examinee") : "";     
                model.addAttribute("examineeText", examineeText);
                model.addAttribute("examinee", examinee);
                //查考生 E
                
                //查教案
                Map<String, String> qryQsId = new HashMap<String, String>();
                qryQsId.put("rdId", rdId);
                qryQsId.put("roomSeq", roomSeq);
                qryQsId.put("qryType", "SCRDRM");
                List<Map<String,Object>> rowRdrmList = new ArrayList<Map<String,Object>>();
                rowRdrmList = scRunDown.qryRdrmList(qryQsId, dbu);
                if (rowRdrmList.size() == 0)
                    throw new StopException("查無教案資料");
                Map<String,Object> rowRdrm = new HashMap<String,Object>();
                rowRdrm = rowRdrmList.get(0);
                String qsName = (rowRdrm.get("qsName")!=null)? (String)rowRdrm.get("qsName") : "";
                String qsId = (rowRdrm.get("qsId")!=null)? (String) rowRdrm.get("qsId") : ""; 
                model.addAttribute("qsName", qsName);
                model.addAttribute("qsId", qsId);
			}

			if (progId != null && progId.equals("ScMntSect")) {
			    model.addAttribute("paneBackup", paneBackup.toString().replaceAll("\"", "'"));
            }    
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
		
		// 這啥? 將前面放到res的資料轉到真的會放到畫面上的model
		Iterator<Entry<String, Object>> resi = res.entrySet().iterator();
		while (resi.hasNext()) {
			Entry<String, Object> rese = resi.next();
			model.addAttribute(rese.getKey(), rese.getValue());
		}
		return "ScSetScore";
	}

	@RequestMapping(value = "/ScSetScore_qryRdList", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryRdList(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {	
			res.put("success",  false);
			String qryUserId = req.get("userId");

			// 梯次清單
			StringBuffer sqlQryRd = new StringBuffer(
					  " SELECT rd_id, rd_desc \n"
					+ "   FROM scrddm \n"
					+ "  WHERE rd_id IN (SELECT rd_id \n"
					+ "                    FROM scrddm \n"
					+ "                   WHERE examiner = ?) \n"
					//+ "    AND A.rd_date  = TO_CHAR(current_date,'YYYYMMDD') \n"
					);
			List<Object> params = new ArrayList<Object>();
			params.add(qryUserId);
			ResultSet rsRd = dbu.queryArray(sqlQryRd.toString(), params.toArray());
			List<Map<String, Object>> rdList = new ArrayList<Map<String, Object>>();
			while (rsRd.next()) {
				Map<String, Object> rd = new HashMap<String, Object>();
				rd.put("fRdId",   rsRd.getString("rd_id"));
				rd.put("fRdDesc", rsRd.getString("rd_desc"));
				rdList.add(rd);
			}
			rsRd.close();
			res.put("rdList", rdList);
			
			res.put("success", true);
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
	
	@RequestMapping(value = "/ScSetScore_qryExList", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryExList(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success",  false);
			//res.put("optIdList", CodeSvc.buildSelectRankData(dbu, "TAA", false, true));
			
			//String  qryBProgId = req.get("bProgId");
			String  qryRdId    = req.get("rdId");
			//Integer qryRoomSeq = 0;
			String  qryUserId  = req.get("userId");
			Object  qryCon;
			//if (qryBProgId != null) {
			//	qryRoomSeq = Integer.parseInt(req.get("roomSeq"));
			//}

			// 教案名稱
			StringBuffer sqlQryQsName = new StringBuffer(
					  " SELECT B.qs_name \n"
					+ "   FROM scrdrm A, qsmstr B \n"
				    + "  WHERE A.qs_id = B.qs_id \n"
					+ "    AND A.rd_id = ? \n"
				    + "    AND A.examiner = ? \n");
			/*
select * from scrddm where rd_id = '20180530001' and examiner='TEST2';
select * from scrdrm where rd_id = '20180530001' and room_seq = '2';

select b.qs_name  from scrddm a, qsmstr b where a.qs_id = b.qs_id and rd_id = '20180530001' and examiner='TEST2';
			// 不是評分查詢來的用考官查
			if (qryBProgId == null) {
				sqlQryQsName.append("    AND A.examiner = ? \n");
				qryCon = qryUserId;
			// 評分查詢來的用站別查
			} else {
				sqlQryQsName.append("    AND A.room_seq = ? \n");
				qryCon = qryRoomSeq;
			}
			*/
			//Map<String, Object> rowQs = dbu.selectMapRowList(sqlQryQsName.toString(), qryRdId, qryCon);
			//res.put("qsName", rowQs.get("qs_name"));

			// 考生清單 評分查詢來的就不查了
			//if (qryBProgId == null) {
				StringBuffer sqlQryEx = new StringBuffer(
						  " SELECT sect_seq, examinee, room_seq \n"
						+ "	  FROM scrddm \n"
					    + "  WHERE rd_id    = ? \n "
						+ "    AND examiner = ? \n"
					    + "  ORDER BY sect_seq \n");
				List<Object> params = new ArrayList<Object>();
				params.add(qryRdId);
				params.add(qryUserId);
				ResultSet rsEx = dbu.queryArray(sqlQryEx.toString(), params.toArray());
				List<Map<String, Object>> exList = new ArrayList<Map<String, Object>>();
				while (rsEx.next()) {
					Map<String, Object> ex = new HashMap<String, Object>();
					ex.put("fSectSeq",  rsEx.getString("sect_seq"));
					ex.put("fExaminee", rsEx.getString("examinee"));
					ex.put("fRoomSeq",  rsEx.getString("room_seq"));
					exList.add(ex);
				}
				rsEx.close();
				res.put("exList", exList);
			//}
			
			res.put("success", true);
			res.put("status", "查詢考生完成");
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
	
	// 帶出該梯次每節(學生)分數清單
	private List<Map<String, Object>> sectList(Map<String, String> req, DbUtil dbu) throws SQLException, Exception {
	    String  qryRdId    = req.get("rdId");
        Integer qryRoomSeq = Integer.parseInt(req.get("roomSeq"));
        Integer qrySectSeq = Integer.parseInt(req.get("sectSeq"));
        //String qryQsId     = "";
        List<Map<String, Object>> itemList = new ArrayList<Map<String, Object>>();
        
        // 項目清單
        StringBuffer sqlQryItem = new StringBuffer(
                  " SELECT A.qs_id, B.item_no, B.item_desc, B.opt_class, B.tip \n"
                + "   FROM scrdrm A, scqstd B \n"
                + "  WHERE A.qs_id    = B.qs_id \n"
                + "    AND A.rd_id    = ? \n"
                + "    AND A.room_seq = ? \n"
                + "  ORDER BY B.item_no ASC");
        List<Object> params = new ArrayList<Object>();
        params.add(qryRdId);
        params.add(qryRoomSeq);
        ResultSet rsItem = dbu.queryArray(sqlQryItem.toString(), params.toArray());
        Map<String, Map<String, Object>> itemMap = new HashMap<String, Map<String, Object>>();
        List<String> itemArray = new ArrayList<String>();
        while (rsItem.next()) {
            Map<String, Object> item = new HashMap<String, Object>();
            //qryQsId = rsItem.getString("qs_id");
            item.put("itemNo",   rsItem.getString("item_no"));
            item.put("itemDesc", rsItem.getString("item_desc"));
            item.put("optClass", rsItem.getString("opt_class"));
            item.put("tip",      rsItem.getString("tip"));
            itemMap.put(rsItem.getString("item_no"), item);
            itemArray.add(rsItem.getString("item_no"));
        }
        rsItem.close();
        //res.put("itemList", itemList);
        
        // 項目分數
        StringBuffer sqlQryScore = new StringBuffer(
                  " SELECT item_no, opt_id, exam_comm, exam_pic  \n"
                + "   FROM scscom \n"
                + "  WHERE rd_id    = ? \n"
                + "    AND room_seq = ? \n"
                + "    AND sect_seq = ? \n"
                + "  ORDER BY item_no");
        List<Object> params1 = new ArrayList<Object>();
        params1.add(qryRdId);
        params1.add(qryRoomSeq);
        params1.add(qrySectSeq);
        ResultSet rsScore = dbu.queryArray(sqlQryScore.toString(), params1.toArray());
        //int i = 0;
        while (rsScore.next()) {
            //itemList.get(i).put("optId", rsScore.getString("opt_id"));
            //i++;
            itemMap.get(rsScore.getString("item_no")).put("optId", rsScore.getString("opt_id"));
        }
        
        for (String itemNo : itemArray) {
            itemList.add(itemMap.get(itemNo));
        }
	    
	    return itemList;
	}
	
	//查詢同類型的級分資料
	@SuppressWarnings("unchecked")
    private Map<String, Object> qryOpt(String optClass, DbUtil dbu) throws SQLException, Exception {
	    Map<String, Object> opt = new HashMap<String, Object>();
	    
	    //String optClass = req.get("optClass").substring(0, 2);
	    
        StringBuffer sqlQryOpt = new StringBuffer(
                  " SELECT opt_class \"optClass\", opt_id \"optId\", opt_desc \"optDesc\", no_sel \"noSel\" \n"
                + "   FROM scoptm \n"
                + "  WHERE opt_class like ? || '%' \n"
                + "    AND opt_id <> '-' \n"
                + "  ORDER BY opt_class, show_order \n");
        List<Map<String, Object>> optListMap = new ArrayList<Map<String, Object>>();
        optListMap = dbu.selectMapAllList(sqlQryOpt.toString(), optClass);
        for (Map<String, Object> optMap : optListMap) {
            if (opt.containsKey(optMap.get("optClass"))) {
                ((List<Map<String, Object>>) opt.get(optMap.get("optClass"))).add(optMap);
            }
            else {
                List<Map<String, Object>> a = new ArrayList<Map<String, Object>>();
                a.add(optMap);
                opt.put((String) optMap.get("optClass"), a);
            }
        }
	    
	    return opt;
	}
	
	@RequestMapping(value = "/ScSetScore_qryScore", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryScore(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success",  false);
            
			// 節次分數
			List<Map<String, Object>> itemList = sectList(req, dbu);
			res.put("itemList", itemList);
			
			// 考試結果清單
			res.put("resList", CodeSvc.buildSelectDataByKind(dbu, "SCRESU", false)); 
			// 整體
	        /*
			
	        StringBuffer sqlQryTot = new StringBuffer(
	                  " SELECT opt_id, score, result \n"
	                + "   FROM scrddm \n"
	                + "  WHERE rd_id    = ? \n"
	                + "    AND room_seq = ? \n"
	                + "    AND sect_seq = ? \n");

	        Map<String, Object> rowTot = dbu.selectMapRowList(sqlQryTot.toString(), qryRdId, qryRoomSeq, qrySectSeq);
	        res.put("optId",  rowTot.get("opt_id"));
	        res.put("score",  rowTot.get("score"));
	        res.put("result", rowTot.get("result"));*/
	        
	        // 把同類型的級分資料轉給畫面
			String optClass = (String) itemList.get(0).get("optClass");
	        Map<String, Object> opt = qryOpt(optClass.substring(0, 2), dbu);
	        res.put("opt", opt);

			res.put("success", true);
			res.put("status", "查詢評分表完成");
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
	 * 儲存評分
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/ScSetScore_scoreEditDone", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addItem(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
            
            String rdId    = req.get("rdId");
			String roomSeq = req.get("roomSeq");
			String sectSeq = req.get("sectSeq");
			String optId   = req.get("optId");
			Integer score = 0;
			String result = "";
			
			// 執行
 			String sqlInsQsmstr =
 				  " UPDATE scrddm SET \n"
 				+ "      , score    = ? \n"
 				+ "      , result   = ? \n"
 				+ "      , opt_id   = ? \n"
 				+ "  WHERE rd_id    = ? \n"
 				+ "    AND room_seq = ? \n"
 				+ "    AND sect_seq = ? \n";
 			dbu.executeList(sqlInsQsmstr, score, result, optId, rdId, roomSeq, sectSeq);
 			dbu.doCommit();
		}
		catch (Exception e) {
			res.put("status", ExceptionUtil.procExceptionMsg(e));
		}
		dbu.relDbConn();
		
		return res;
	}
}