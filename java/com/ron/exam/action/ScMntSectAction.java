package com.ron.exam.action;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.ron.exam.util.DbUtil;
import com.ron.exam.util.ExceptionUtil;
import com.ron.exam.util.StdCalendar;
import com.ron.exam.util.StopException;

@EnableWebMvc
@Controller
public class ScMntSectAction {
    @RequestMapping(value = "/ScMntSect", method = RequestMethod.GET)
    public String execute(Model model, @RequestParam Map<String, String> req) {
        
                
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
        System.out.println(paneBackup.toString());
        model.addAttribute("paneBackup", paneBackup.toString().replaceAll("\"", "'"));
        return "ScMntSect";
    }
    
    //查詢梯次
    private List<Map<String, Object>> qryRddmList(Map<String, String> req, DbUtil dbu) throws SQLException, Exception {
        List<Map<String, Object>> rowRddms = new ArrayList<Map<String, Object>>();
        String rdId = req.get("rdId");
        String showType = req.get("showType");
        
        String sqlQryMaxRdId = "SELECT MAX(sect_seq), count(sect_seq) FROM scrddm WHERE rd_id = ?";
        List<Object> maxRdId = dbu.selectListRowList(sqlQryMaxRdId, rdId);
        int maxSect = Integer.parseInt(maxRdId.get(0).toString() + "");
        int maxRoom = Integer.parseInt(maxRdId.get(1).toString() + "");
        
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
            
        for (int i=0; i<maxSect; i++){
            List<Map<String, Object>> rowSect = dbu.selectMapAllList(sqlQryRd, rdId, (i+1));
            Map<String, Object> rowData = new HashMap<String, Object>();
            rowData.put("sectSeq", (i+1));
            int s = 1;
            for (Map<String, Object> sect : rowSect){
                rowData.put("roomSeq" + s, sect.get("name"));
                rowData.put("roomSeqVal" + s, sect.get("id"));
                s++;
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
                int s = 1;
                rowData.put("sectSeq", (i+2)); //這邊是做節次2之後的
                for (int idx: c) {
                    //System.out.println(rowRddms.get(0).get("roomSeq"+(idx+1) ));
                    rowData.put("roomSeq" + s, rowRddms.get(0).get("roomSeq"+(idx+1)));
                    rowData.put("roomSeqVal" + s, rowRddms.get(0).get("roomSeqVal"+(idx+1)));
                    s++;
                }
                rowRddms.add(rowData);
            }
        }
        System.out.println("rdId = " + rdId + " size = " + rowRddms.size());
        return rowRddms;
    }
    
    /**
     * 梯次查詢畫面
     * @param req
     * @return
     */
    @RequestMapping(value = "/ScRunDown_qryRddm", method = RequestMethod.POST)
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
}

