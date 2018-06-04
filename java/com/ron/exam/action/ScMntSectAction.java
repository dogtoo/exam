package com.ron.exam.action;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONString;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.ron.exam.service.ProgData;
import com.ron.exam.service.UserData;
import com.ron.exam.util.DbUtil;
import com.ron.exam.util.ExceptionUtil;
import com.ron.exam.util.StdCalendar;
import com.ron.exam.util.StopException;

@EnableWebMvc
@Controller
public class ScMntSectAction {
    
    private static final String c_progId = "ScMntSect";
    
    @RequestMapping(value = "/ScMntSect", method = RequestMethod.GET)
    public String execute(Model model, @RequestParam Map<String, String> req) {
        Map<String, Object> res = new HashMap<String, Object>();
        try {
            res.put("status", "");
            res.put("statusTime", new StdCalendar().toTimesString());
            
            UserData ud = UserData.getUserData();
            ProgData pd = ProgData.getProgData();
            res.put("progId", c_progId);
            res.put("privDesc", ud.getPrivDesc(c_progId));
            res.put("progTitle", pd.getProgTitle(c_progId));
            res.put("queryHide", ProgData.c_privBaseQuery.equals(ud.getPrivBase(c_progId)) ? "visibility: hidden;" : "");
            
            String rdId = req.get("rdId");
            String showType = req.get("showType");
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
            }
            paneBackup.put("progId", progIdList);
            
            model.addAttribute("bProgId", progId);
            model.addAttribute("rdId", rdId);
            
            if (showType != null && !showType.isEmpty())
                model.addAttribute("showType", showType);
            else if (progId != null && progId.equals("ScRunDown")) {
                model.addAttribute("showType", "S");
                Map<String, String> p_b_ScRunDown = new HashMap<String, String>();
                for (int i = 0; ; i++) {
                    String key = "p_b_ScRunDown[" + i + "]";
                    if (!req.containsKey(key))
                        break;
                    String queryCol = req.get(key);
                    String[] col = queryCol.split(":", -1);
                    p_b_ScRunDown.put(col[0], col[1]);
                }
                paneBackup.put("ScRunDown", p_b_ScRunDown);
            }            
            else
                model.addAttribute("showType", "R");
            //System.out.println(paneBackup.toString());
            model.addAttribute("paneBackup", paneBackup.toString().replaceAll("\"", "'"));
        }
        catch (Exception e) {
            res.put("status", ExceptionUtil.procExceptionMsg(e));
        }
        
        Iterator<Entry<String, Object>> resi = res.entrySet().iterator();
        while (resi.hasNext()) {
            Entry<String, Object> rese = resi.next();
            model.addAttribute(rese.getKey(), rese.getValue());
        }
        return "ScMntSect";
    }
    
    //查詢梯次
    private Map<String, Integer> getMaxRdCnt(Map<String, String> req, DbUtil dbu) throws SQLException, Exception {
        String rdId = req.get("rdId");
        String sqlQryMaxRdCnt = "SELECT MAX(sect_seq), MAX(room_seq) FROM scrddm WHERE rd_id = ?";
        List<Object> maxRdCntList = dbu.selectListRowList(sqlQryMaxRdCnt, rdId);
        Map<String, Integer> maxRdCnt = new HashMap<String, Integer>();
        maxRdCnt.put("maxSect", Integer.parseInt(maxRdCntList.get(0).toString() + ""));
        maxRdCnt.put("maxRoom", Integer.parseInt(maxRdCntList.get(1).toString() + ""));
        return maxRdCnt;
    }
    
    private List<Map<String, Object>> qryRddmList(Map<String, String> req, DbUtil dbu) throws SQLException, Exception {
        List<Map<String, Object>> rowRddms = new ArrayList<Map<String, Object>>();
        String rdId = req.get("rdId");
        String showType = req.get("showType");
        String editType = req.get("editType");
        Map<String, Integer> maxRdCnt = getMaxRdCnt(req, dbu);
        int maxSect = (Integer) maxRdCnt.get("maxSect");
        int maxRoom = (Integer) maxRdCnt.get("maxRoom");
        
        String sqlQryRd = "SELECT ";
        if (showType.equals("S")) {
            sqlQryRd +=   "       examinee \"id\" "
                     +    "     , (select user_name from mbdetl where user_id = a.examinee) \"name\" \n";
        }
        else if (showType.equals("T")) {
            sqlQryRd +=   "       examiner \"id\" "
                     +    "     , (select user_name from cmuser where user_id = a.examiner) \"name\" \n";
        }
        else if (showType.equals("R")) {
            sqlQryRd +=   "       room_id \"id\" "
                     +    "     , (select room_name from EXROOM where room_id = a.room_id ) \"name\" \n";
        }
            sqlQryRd +=   "  FROM scrddm a"
                     +    " WHERE rd_id = ? "
                     +    "   AND sect_seq = ?"
                     +    " ORDER BY sect_seq asc, room_seq asc;";
        
        List<Integer> a = new ArrayList<Integer>();
        List<Integer> b = new ArrayList<Integer>();
        
        Map<String, String> reqRd = new HashMap<String, String>();
        reqRd.put("rdId", rdId);
        ScRunDownAction ScRunDown = new ScRunDownAction();
        List<Map<String,Object>> rowRdrms = new ArrayList<Map<String,Object>>();
        if (editType != null && editType.equals("Y"))
            rowRdrms = ScRunDown.qryRdrmList(reqRd, dbu);
        
        for (int i=0; i<maxSect; i++){
            List<Map<String, Object>> rowSect = dbu.selectMapAllList(sqlQryRd, rdId, (i+1));
            Map<String, Object> rowData = new HashMap<String, Object>();
            rowData.put("sectSeq", "第 " + (i+1) + " 節");
            int r = 1;
            for (Map<String, Object> sect : rowSect){
                rowData.put("roomSeq" + r, sect.get("name"));
                rowData.put("roomSeqVal" + r, sect.get("id"));
                //rowData.put("roomSeqVal" + r, r);
                
                if (editType != null && editType.equals("Y")) {
                    Map<String, Object> colData = new HashMap<String, Object>();
                    colData.put("roomSeq" , rowRdrms.get(r-1).get("roomSeq"));
                    //colData.put("sectSeq" , rowRdrms.get(r-1).get("sectSeq"));
                    colData.put("roomId"  , rowRdrms.get(r-1).get("roomId"));
                    colData.put("examiner", rowRdrms.get(r-1).get("examiner"));
                    colData.put("patient1", rowRdrms.get(r-1).get("patient1"));
                    colData.put("patient2", rowRdrms.get(r-1).get("patient2"));
                    colData.put("patient3", rowRdrms.get(r-1).get("patient3"));
                    colData.put("examinee", sect.get("id"));
                    rowData.put("colData" + (r-1), colData);
                }
                r++;
            }      
            rowRddms.add(rowData);
            
        }
        for (int i=0; i<maxRoom; i++)
            b.add(i);
        
        if (maxSect < maxRoom) {
            
            for (int i = 0; i<maxRoom-maxSect; i++) {
                //把b的最後一個值放在a的最前面
                int b_l = b.get(b.size()-1);
                //System.out.println("b_l = " + b_l);
                a.add(0,b_l);
                //System.out.println("    a[] = " + a.toString());
                b.remove(b.size()-1);
                //System.out.println("    b[] = " + b.toString());
                List<Integer> c = new ArrayList<Integer>(a);
                c.addAll(b);
                //System.out.println("    c[] = " + c.toString());
                
                Map<String, Object> rowData = new HashMap<String, Object>();
                int r = 1;
                rowData.put("sectSeq", "第 " + (i+2) + " 節"); //這邊是做節次2之後的 
                for (int idx: c) {
                    //System.out.println(rowRddms.get(0).get("roomSeq"+(idx+1) ));
                    rowData.put("roomSeq" + r, rowRddms.get(0).get("roomSeq"+(idx+1)));
                    rowData.put("roomSeqVal" + r, rowRddms.get(0).get("roomSeqVal"+(idx+1)));
                    
                    if (editType != null && editType.equals("Y")) {
                        Map<String, Object> colData = new HashMap<String, Object>();
                        colData.put("roomSeq" , rowRdrms.get(r-1).get("roomSeq"));
                        //colData.put("sectSeq" , rowRdrms.get(r-1).get("sectSeq"));
                        colData.put("roomId"  , rowRdrms.get(r-1).get("roomId"));
                        colData.put("examiner", rowRdrms.get(r-1).get("examiner"));
                        colData.put("patient1", rowRdrms.get(r-1).get("patient1"));
                        colData.put("patient2", rowRdrms.get(r-1).get("patient2"));
                        colData.put("patient3", rowRdrms.get(r-1).get("patient3"));
                        colData.put("examinee", rowRddms.get(0).get("roomSeqVal"+(idx+1)));
                        rowData.put("colData" + (r-1), colData);
                    }
                    r++;
                }
                rowRddms.add(rowData);
            }
        }
        //System.out.println("rdId = " + rdId + " size = " + rowRddms.size());
        
        return rowRddms;
    }
    
    //建立梯次
    private Map<String, Integer> crRddm(Map<String, String> req, DbUtil dbu) throws SQLException, Exception {
        String rdId = req.get("rdId");
        req.put("showType", "S");
        int cnt = 0;
        String sqlDelRddm = "DELETE FROM scrddm WHERE rd_id = ? ";
        String sqlInsRddm = 
                  "INSERT INTO public.scrddm( "
                + "    rd_id, room_seq, sect_seq, room_id, examiner "
                + "  , patient1, patient2, patient3, examinee) "
                + "VALUES "
                + "   (?, ?, ?, ?, ?, ?, ?, ?, ?);";
        String upRdmmStsE = "UPDATE scrdmm SET sts_e = ? WHERE rd_id = ? \n";
        
        List<Map<String, Object>> rowRddms = qryRddmList(req, dbu);
        dbu.executeList(sqlDelRddm, rdId);
        for (Map<String, Object> row: rowRddms) {
            for (int r=0; r<rowRddms.size(); r++){
                @SuppressWarnings("unchecked")
                Map<String, Object> insData = (Map<String, Object>) row.get("colData"+r);
                int roomSeq = Integer.parseInt(insData.get("roomSeq") + "");
                int sectSeq = Integer.parseInt(row.get("sectSeq") + "");
                String roomId   = insData.get("roomId") + "";
                String examiner = insData.get("examiner") + "";
                String patient1 = insData.get("patient1") + "";
                String patient2 = (insData.get("patient2")!=null) ? insData.get("patient2") + "" : "";
                String patient3 = (insData.get("patient3")!=null) ? insData.get("patient3") + "" : "";
                String examinee = insData.get("examinee") + "";
                cnt += dbu.executeList(sqlInsRddm, rdId, roomSeq, sectSeq, roomId, examiner, patient1, patient2, patient3, examinee);
            }
        }
        
        Map<String, Integer> maxRdCnt = getMaxRdCnt(req, dbu);
        int maxSect = (Integer) maxRdCnt.get("maxSect");
        int maxRoom = (Integer) maxRdCnt.get("maxRoom");
        if (maxSect == maxRoom)
            dbu.executeList(upRdmmStsE, "Y", rdId);
        else if (maxSect == 1)
            dbu.executeList(upRdmmStsE, "S", rdId);
        else if (maxSect == 0)
            dbu.executeList(upRdmmStsE, "N", rdId);
        return maxRdCnt;
    }
    
  //考試表更新
    private int upRddm(Map<String, String> req, DbUtil dbu) throws SQLException, Exception {
        String rdId = req.get("rdId");
        int roomSeq = Integer.parseInt(req.get("roomSeq"));
        int sectSeq = Integer.parseInt(req.get("sectSeq"));
        
        String roomId = req.get("roomId");
        String examiner = req.get("examiner");
        String patient1 = req.get("patient1");
        String patient2 = req.get("patient2");
        String patient3 = req.get("patient3");

        String sqlUpRddm = 
                  "UPDATE scrddm \n"
                + "   SET room_id = ? \n"
                + "     , examiner = ? \n"
                + "     , patient1 = ?, patient2 = ?, patient3 = ? \n"
                + " WHERE rd_id=? \n"
                + "   AND room_seq = ? \n"
                + "   AND sect_seq >= ? ;\n";
        
        int sectCnt = 0;
        sectCnt = dbu.executeList(sqlUpRddm, roomId, examiner, patient1, patient2, patient3, rdId, roomSeq, sectSeq);
        //dbu.executeList(sqlDelRddm, rdId);
        
        return sectCnt;
    }
    
    /**
     * 考試表查詢畫面
     * @param req
     * @return
     */
    @RequestMapping(value = "/ScMntSect_qryRddm", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> qryRddm(@RequestParam Map<String, String> req) {
        Map<String, Object> res = new HashMap<String, Object>();
        res.put("status", "");
        res.put("statusTime", new StdCalendar().toTimesString());

        res.put("success", false);
        List<Map<String, Object>> rowRddms;
        DbUtil dbu = new DbUtil();
        try
        {           
            rowRddms = qryRddmList(req, dbu);
            if (rowRddms.size() == 0)
                throw new StopException("查無梯次表資料");
            Map<String, Integer> maxRdCnt = getMaxRdCnt(req, dbu);
            int maxSect = (Integer) maxRdCnt.get("maxSect");
            int maxRoom = (Integer) maxRdCnt.get("maxRoom");
            if (maxSect == maxRoom)
                res.put("rddmEditHide", "Y");
            else if (maxSect < maxRoom)
                res.put("rddmCrHide", "Y");
            
            res.put("maxRoom", maxRoom);
            res.put("rddmList", rowRddms);            
            res.put("success", true);
            res.put("status", "查詢梯次資料完成");
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
     * 考試表確認
     * @param req
     * @return
     */
    @RequestMapping(value = "/ScMntSect_crRddm", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> crRddm(@RequestParam Map<String, String> req) {
        Map<String, Object> res = new HashMap<String, Object>();
        res.put("status", "");
        res.put("statusTime", new StdCalendar().toTimesString());

        res.put("success", false);
        DbUtil dbu = new DbUtil();
        try
        {
            Map<String, Integer> maxRdCnt = crRddm(req, dbu);
            dbu.doCommit();
            int maxSect = (Integer) maxRdCnt.get("maxSect");
            int maxRoom = (Integer) maxRdCnt.get("maxRoom");
            
            if (maxSect == maxRoom)
                res.put("rddmEditHide", "Y");
            else if (maxSect < maxRoom)
                res.put("rddmCrHide", "Y");
            res.put("success", true);
            res.put("status", "考試表資料更新完成");
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
     * 考試表調整
     * @param req
     * @return
     */
    @RequestMapping(value = "/ScMntSect_upRddm", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> upRddm(@RequestParam Map<String, String> req) {
        Map<String, Object> res = new HashMap<String, Object>();
        res.put("status", "");
        res.put("statusTime", new StdCalendar().toTimesString());

        res.put("success", false);
        DbUtil dbu = new DbUtil();
        try
        {
            int sectCnt = upRddm(req, dbu);
            
            if (sectCnt == 0)
                throw new StopException("考試表更新無資料");
            
            dbu.doCommit();
            res.put("sectCnt", sectCnt);
            res.put("success", true);
            res.put("status", "考試表資料更新完成");
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
     * 查詢某節、站之資料
     * @param req
     * @return
     */
    @RequestMapping(value = "/ScMntSect_qryCellData", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> qryCellData(@RequestParam Map<String, String> req) { 
        Map<String, Object> res = new HashMap<String, Object>();
        DbUtil dbu = new DbUtil();
        try {
            ScRunDownAction scRunDown = new ScRunDownAction();
            List<Map<String,Object>> rowRdrmList = new ArrayList<Map<String,Object>>();
            rowRdrmList = scRunDown.qryRdrmList(req, dbu);
            if (rowRdrmList.size() == 0)
                throw new StopException("查無站次資料");
            Map<String,Object> rowRdrm = new HashMap<String,Object>();
            rowRdrm = rowRdrmList.get(0);
            res.put("roomId", (String) rowRdrm.get("roomId"));
            res.put("roomName", (String) rowRdrm.get("roomName"));
            res.put("examiner", (String) rowRdrm.get("examiner"));
            res.put("examinerName", (String) rowRdrm.get("examinerName"));
            res.put("patient1", (String) rowRdrm.get("patient1"));
            res.put("patient2", (String) rowRdrm.get("patient2"));
            res.put("patient3", (String) rowRdrm.get("patient3"));
            res.put("patient1Name", (String) rowRdrm.get("patient1Name"));
            res.put("patient2Name", (String) rowRdrm.get("patient2Name"));
            res.put("patient3Name", (String) rowRdrm.get("patient3Name"));
            res.put("success", true);
            res.put("status", "節次資料查詢完成");
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

