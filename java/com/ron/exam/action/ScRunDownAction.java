package com.ron.exam.action;

import java.sql.SQLException;
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

import com.ron.exam.service.CodeSvc;
import com.ron.exam.service.ProgData;
import com.ron.exam.service.UserData;
import com.ron.exam.util.DbUtil;
import com.ron.exam.util.ExceptionUtil;
import com.ron.exam.util.StdCalendar;
import com.ron.exam.util.StopException;

@EnableWebMvc
@Controller
public class ScRunDownAction {
    
    private static final String c_progId = "ScRunDown";
    
    @RequestMapping(value = "/ScRunDown", method = RequestMethod.GET)
    public String execute(Model model, @RequestParam Map<String, String> req, HttpSession sess) {
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
            String rdDesc = req.get("rdDesc");
            String rdDateE = req.get("rdDateE");
            String rdDateS = req.get("rdDateS");

            model.addAttribute("rdId", rdId);
            model.addAttribute("rdDesc", rdDesc);
            model.addAttribute("rdDateE", rdDateE);
            model.addAttribute("rdDateS", rdDateS);
        }
        catch (Exception e) {
            res.put("status", ExceptionUtil.procExceptionMsg(e));
        }
        
        Iterator<Entry<String, Object>> resi = res.entrySet().iterator();
        while (resi.hasNext()) {
            Entry<String, Object> rese = resi.next();
            model.addAttribute(rese.getKey(), rese.getValue());
        }
        return "ScRunDown";
    }
    
    //教案下拉選單
    @RequestMapping(value = "/ScRunDown_qryQsList", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> qryQsList(@RequestParam Map<String, String> req) {
        Map<String, Object> res = new HashMap<String, Object>();
        
        DbUtil dbu = new DbUtil();
        res.put("success", false);
        try
        {
            String param = req.get("param");
            String sqlQs =
                "SELECT a.qs_id \"qsId\", a.qs_name \"qsName\"\n"
              + "  FROM qsmstr a, scqstm b \n"
              + " WHERE a.qs_id = b.qs_id \n"
              + "   AND (coalesce(?, '') = '' OR a.qs_id like ? || '%' OR a.qs_Name like ? || '%')\n";
            List<Map<String, Object>> qsList = dbu.selectMapAllList(sqlQs, param, param, param);
            if (qsList.size() == 0)
                throw new StopException("查無教案");
            
            res.put("qsList", qsList);
            res.put("success", true);
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
    
    //診間下拉選單
    @RequestMapping(value = "/ScRunDown_qryRoomList", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> qryRoomList(@RequestParam Map<String, String> req) {
        Map<String, Object> res = new HashMap<String, Object>();
        
        DbUtil dbu = new DbUtil();
        res.put("success", false);
        try
        {
            String param = req.get("param");
            //String rdId = req.get("rdId");
            //String rdDate = req.get("rdDate").replaceAll("/", "");
            String sqlRoom =
                  "select room_id \"roomId\", room_name \"roomName\" \n"
                + "  from EXROOM \n"
                + " where 1=1 \n"
                //排除同天非本梯次有在使用的診間
                //先不排除 Mark掉
                /*
                + " where room_id not in (select room_id "
                + "                         from scrdrm a, scrdmm b "
                + "                        where a.rd_id = b.rd_id "
                + "                          and b.rd_id != ?"
                + "                          and b.rd_date = ?) "*/
                + "   and (coalesce(?, '') = '' OR room_id like ? || '%' OR room_name like ? || '%'); \n";
            //List<Map<String, Object>> roomList = dbu.selectMapAllList(sqlRoom, rdId, rdDate, param, param, param);
            List<Map<String, Object>> roomList = dbu.selectMapAllList(sqlRoom, param, param, param);
            if (roomList.size() == 0)
                throw new StopException("查無診間");
            
            res.put("roomList", roomList);
            res.put("success", true);
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
    
    //考官、標準病人、學生下拉
    @RequestMapping(value = "/ScRunDown_qryUserList", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> qryUserList(@RequestParam Map<String, String> req) {
        Map<String, Object> res = new HashMap<String, Object>();
        
        DbUtil dbu = new DbUtil();
        res.put("success", false);
        try
        {
            String param = req.get("param");
            //String rdId = req.get("rdId");
            //String rdDate = req.get("rdDate").replaceAll("/", "");
            String examiner = req.get("examiner");
            String sqlUser;                           
            //排除同天非本梯次已有指派的人員
            List<Map<String, Object>> userList;
            if (examiner.equals("Y")) {
                sqlUser = "select user_id \"examiner\", user_name \"examinerName\" \n" 
                        + "  from cmuser \n"
                        + " where 1=1 \n"
                        //先不排除 Mark掉
                        /*
                        + " where user_id not in ( \n"
                        + "                       select examiner \n"
                        + "                         from scrdrm a, scrdmm b \n"
                        + "                        where a.rd_id = b.rd_id \n"
                        + "                          and b.rd_id != ? \n"
                        + "                          and b.rd_date = ?) \n"*/
                        + "   and (coalesce(?, '') = '' OR user_id like ? || '%' OR user_name like ? || '%'); \n";
                //userList = dbu.selectMapAllList(sqlUser, rdId, rdDate, param, param, param);
                userList = dbu.selectMapAllList(sqlUser, param, param, param);
            }
            else {
                if (examiner.equals("S"))
                    sqlUser = "select user_id \"examineeId\", user_name \"examineeName\" \n";
                else
                    sqlUser = "select user_id \"patient\", user_name \"patientName\" \n";
                
                sqlUser +="  from mbdetl \n"
                        + " where 1=1 \n"
                        //先不排除 Mark掉
                        /*
                        + " where user_id not in (select coalesce(patient1, ' ') \n"
                        + "                         from scrdrm a, scrdmm b \n"
                        + "                        where a.rd_id = b.rd_id \n"
                        + "                          and b.rd_id != ? \n"
                        + "                          and b.rd_date = ?) \n"
                        + "   and user_id not in (select coalesce(patient2, ' ') \n"
                        + "                         from scrdrm a, scrdmm b \n"
                        + "                        where a.rd_id = b.rd_id \n"
                        + "                          and b.rd_id != ? \n"
                        + "                          and b.rd_date = ?) \n"
                        + "   and user_id not in (select coalesce(patient3, ' ') \n"
                        + "                         from scrdrm a, scrdmm b \n"
                        + "                        where a.rd_id = b.rd_id \n"
                        + "                          and b.rd_id != ? \n"
                        + "                          and b.rd_date = ? ) \n"*/
                        + "   and (coalesce(?, '') = '' OR user_id like ? || '%' OR user_name like ? || '%'); \n";
                //userList = dbu.selectMapAllList(sqlUser, rdId, rdDate, rdId, rdDate, rdId, rdDate, param, param, param);
                userList = dbu.selectMapAllList(sqlUser, param, param, param);
            }
            
            if (userList.size() == 0)
                throw new StopException("查無人員");
            
            res.put("userList", userList);
            res.put("success", true);
        }
        catch (StopException e) {
            res.put("status", e.getMessage());
        }
        catch (SQLException e) {
            res.put("status", DbUtil.exceptionTranslation(e));
            System.out.println(e.toString());
        }
        catch (Exception e) {
            res.put("status", ExceptionUtil.procExceptionMsg(e));
            System.out.println(e.toString());
        }
        dbu.relDbConn();
        
        return res;
    }
    
    //時間類別下拉選單
    @RequestMapping(value = "/ScRunDown_qrySectTypeList", method = RequestMethod.GET)
    public @ResponseBody List<Map<String, Object>> qrySectTypeList(@RequestParam Map<String, String> req) {
        List<Map<String, Object>> res = new ArrayList<Map<String, Object>>();
        
        DbUtil dbu = new DbUtil();
        try
        {
            List<Map<String, Object>> sectTypeList = CodeSvc.buildSelectDataByKind(dbu, "SCSECT", false);
            for (Map<String, Object> sectType : sectTypeList) {
                if (!(sectType.get("value").equals("REA") || sectType.get("value").equals("EXA")))
                    res.add(sectType);                    
            }
        }
        catch (SQLException e) {
            System.out.println(e.toString());
        }
        catch (Exception e) {
            System.out.println(e.toString());
        }
        dbu.relDbConn();
        
        return res;
    }
    
    //查詢考站
    public List<Map<String, Object>> qryRdrmList(Map<String, String> req, DbUtil dbu) throws SQLException, Exception {
        List<Map<String, Object>> rowRdrms = new ArrayList<Map<String, Object>>();
        
        String rdId = req.get("rdId");
        int roomSeq = 0;
        int sectSeq = 0;
        String qryType = "";
        
        if (req.get("qryType") != null && req.get("qryType").equals("SCRDDM")) {
            roomSeq = Integer.parseInt(req.get("roomSeq"));
            sectSeq = Integer.parseInt(req.get("sectSeq"));
            qryType = req.get("qryType");
        }
        
        String sqlRdrm =
                 "SELECT a.rd_id \"rdId\", a.room_seq \"roomSeq\", a.room_id \"roomId\" \n"
               + "     , a.examiner, a.patient1, a.patient2, a.patient3 \n";
        if (qryType.equals("SCRDRM") || qryType.equals("")) { // ScMntSet會來查SCRDDM 因為跟 SCRDRM差不多欄位所以就共用一下
            sqlRdrm +=
                 "     , a.qs_id \"qsId\" \n"
               + "     , (select qs_name from qsmstr where qs_id = a.qs_id ) \"qsName\" \n ";
        }
        sqlRdrm +=
                 "     , (select room_name from EXROOM where room_id = a.room_id ) \"roomName\" \n"
               + "     , (select user_name from cmuser where user_id = a.examiner) \"examinerName\" \n"
               + "     , (select user_name from mbdetl where user_id = a.patient1) \"patient1Name\" \n"
               + "     , (select user_name from mbdetl where user_id = a.patient2) \"patient2Name\" \n"
               + "     , (select user_name from mbdetl where user_id = a.patient3) \"patient3Name\" \n";
        if (qryType.equals("SCRDRM") || qryType.equals("")) { 
            sqlRdrm +=
                 "  FROM SCRDRM a \n";
        }
        else {
            sqlRdrm +=
                 "  FROM SCRDDM a \n";
        }
        sqlRdrm +=
                 " WHERE rd_id = ? \n"
               + "   AND (coalesce(?, 0) = 0 OR room_seq = ?) \n";
        if (qryType.equals("SCRDDM")) {
            sqlRdrm += 
                 "   AND (coalesce(?, 0) = 0 OR sect_seq = ?) \n";
        }
        sqlRdrm +=
                 " ORDER BY room_seq ASC \n";
        if (qryType.equals("SCRDDM")) 
            rowRdrms = dbu.selectMapAllList(sqlRdrm, rdId, roomSeq, roomSeq, sectSeq, sectSeq);
        else
            rowRdrms = dbu.selectMapAllList(sqlRdrm, rdId, roomSeq, roomSeq);
        //System.out.println("rdId = " + rdId + " size = " + rowRdrms.size());
        return rowRdrms;
    }
    
    //檢查考官是否當天已指派 <--改成同梯次是否有重覆
    public boolean chkExaminer(DbUtil dbu, String examiner, String rdId, String rdDate) throws SQLException, Exception {
        boolean chk = false;
        /*
        String sqlChk = 
                "select count(*) \n"
              + "  from scrdrm a, scrdmm b \n"
              + " where a.rd_id = b.rd_id \n"
              + "   and b.rd_id != ? \n"
              + "   and a.examiner = ? \n"
              + "   and b.rd_date = ? \n";
        int cnt = dbu.selectIntList(sqlChk, rdId, examiner, rdDate.replaceAll("/", ""));
        System.out.println("chkExaminer : " + cnt);
        if (cnt > 0)
            chk = true;*/
        return chk;
    }
    //檢查診間是同梯次否有重覆
    public boolean chkRoom(String room, String rdDate) throws SQLException, Exception {
        boolean chk = false;
        
        return chk;
    }
    //檢查標準病人同梯次否有重覆
        public boolean chkPatient(String patient, String rdDate) throws SQLException, Exception {
            boolean chk = false;
            
            return chk;
    }
    //檢查考生同梯次否有重覆
    public boolean chkExaminee(String examinee, String rdDate) throws SQLException, Exception {
        boolean chk = false;
        
        return chk;
    }
    
    //異動考站
    private int updataRdrm(Map<String, String> req, DbUtil dbu) throws SQLException, Exception {
        int cnt = 0;
        int delCnt = 0;
        
        String delRdrm = "DELETE FROM public.scrdrm WHERE rd_id = ?";
        String instRdrm = "INSERT INTO public.scrdrm \n"
                        + "    (rd_id, room_seq, qs_id, room_id, examiner, patient1, patient2, patient3) \n"
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) \n";
        String upRdmmStsR = "UPDATE scrdmm SET sts_r = ? WHERE rd_id = ? \n";
        
        String rdId = req.get("pRdId");
        delCnt = dbu.executeList(delRdrm, rdId);
        
        int roomSeq ;
        String qsId, roomId, examiner, patient1, patient2, patient3;
        
        JSONArray rdrmJsonArr = new JSONArray(req.get("pRdrmList"));
        for (int i = 0; i < rdrmJsonArr.length(); i++) {
            JSONObject rdrm = rdrmJsonArr.getJSONObject(i);

            if (!rdrm.isNull("roomSeq") 
             && !rdrm.isNull("qsId") && !rdrm.getString("qsId").equals("")
             && !rdrm.isNull("roomId") && !rdrm.getString("roomId").equals("")
             && !rdrm.isNull("examiner") && !rdrm.getString("examiner").equals("")
             && !rdrm.isNull("patient1") && !rdrm.getString("patient1").equals("")) {
                roomSeq  = rdrm.getInt("roomSeq");
                qsId     = rdrm.getString("qsId");
                roomId   = rdrm.getString("roomId");
                examiner = rdrm.getString("examiner");
                
                patient1 = rdrm.getString("patient1");
                patient2 = (rdrm.isNull("patient2")) ? null : rdrm.getString("patient2");
                patient3 = (rdrm.isNull("patient3")) ? null : rdrm.getString("patient3");
                
                cnt += dbu.executeList(instRdrm, rdId, roomSeq, qsId, roomId, examiner, patient1, patient2, patient3);
            }
            
        }
        Map<String, String> queryRdId = new HashMap<String, String>();
        queryRdId.put("rdId", rdId);
        List<Map<String, Object>> rowRd = qryRdList(queryRdId, dbu);
        int qs_count = (Integer) rowRd.get(0).get("qsCount");
        
        if (cnt != qs_count || cnt < delCnt){
            dbu.executeList(upRdmmStsR, "N", rdId);
            if (cnt < delCnt)
                cnt = delCnt;
        }    
        else
            dbu.executeList(upRdmmStsR, "Y", rdId);

        return cnt;
    }
    
    //查詢節次
    private List<Map<String, Object>> qrySectList(Map<String, String> req, DbUtil dbu) throws SQLException, Exception {
        List<Map<String, Object>> rowSects = new ArrayList<Map<String, Object>>();
        
        String rdId = req.get("rdId");
        
        String sqlSect =
              "SELECT rd_id \"rdId\", seq_no \"seqNo\", sect_seq \"sectSeq\", sect_type \"sectType\" \n"
            + "     , sect_time \"sectTime\", file_name \"fileName\", rela_time \"relaTime\", exec_time \"execTime\" \n"
            + "     , (select code_desc from cmcode where code_kind = 'SCSECT' and code_code = sect_type) \"sectName\" \n"
            + "  FROM scrdsm \n"
            + " WHERE rd_id = ? \n"
            + " ORDER BY seq_no asc \n";
        rowSects = dbu.selectMapAllList(sqlSect, rdId);
        System.out.println("rdId = " + rdId + " size = " + rowSects.size());
        return rowSects;
    }
    
    //異動節次
    private int updataSect(Map<String, String> req, DbUtil dbu) throws SQLException, Exception {
        int cnt = 0;
        int delCnt = 0;
        
        String delSect = "DELETE FROM scrdsm WHERE rd_id = ?";
        String instSect = 
                  "INSERT INTO scrdsm( "
                + "    rd_id, seq_no, sect_seq, sect_type "
                + "  , sect_time, file_name, rela_time, exec_time) "
                + "VALUES "
                + "    (?, ?, ?, ?, ?, ?, ?, ?);";
        String upRdmmStsS = "UPDATE scrdmm SET sts_s = ? WHERE rd_id = ? \n";
        
        String rdId = req.get("pRdId");
        delCnt = dbu.executeList(delSect, rdId);
        
        int seqNo, sectSeq, sectTime, relaTime;
        String sectType, fileName, execTime;
        
        JSONArray sectJsonArr = new JSONArray(req.get("pSectList"));
        for (int i = 0; i < sectJsonArr.length(); i++) {
            JSONObject sect = sectJsonArr.getJSONObject(i);

            seqNo  = sect.getInt("seqNo");
            sectSeq  = sect.getInt("sectSeq");
            sectType   = sect.getString("sectType");
            sectTime  = (sect.isNull("sectTime")) ? 0 : sect.getInt("sectTime");
            fileName = (sect.isNull("fileName")) ? "" : sect.getString("fileName");
            relaTime  = (sect.isNull("relaTime")) ? 0 : sect.getInt("relaTime");
            execTime = (sect.isNull("execTime")) ? "" : sect.getString("execTime");
            cnt += dbu.executeList(instSect, rdId, seqNo, sectSeq, sectType, sectTime, fileName, relaTime, execTime);
        }
        
        dbu.executeList(upRdmmStsS, "Y", rdId);

        return cnt;
    }
    
    //查詢考生
    private List<Map<String, Object>> qryExamineeList(Map<String, String> req, DbUtil dbu) throws SQLException, Exception {
        List<Map<String, Object>> rowExaminees = new ArrayList<Map<String, Object>>();
        
        String rdId = req.get("rdId");
        
        String sqlRdrm =
                 "SELECT a.examinee \n"
               + "     , (select user_name from mbdetl where user_id = a.examinee) \"examineeName\" \n"
               + "  FROM scrddm a \n"
               + " WHERE rd_id = ? \n"
               + "   AND sect_seq = 1\n"
               + " ORDER BY room_seq ASC \n";
        rowExaminees = dbu.selectMapAllList(sqlRdrm, rdId);
        System.out.println("rdId = " + rdId + " size = " + rowExaminees.size());
        return rowExaminees;
    }
    
    //異動考生
    private int updataExaminee(Map<String, String> req, DbUtil dbu) throws SQLException, Exception {
        int cnt = 0;
        int delCnt = 0;
        
        String delSect = "DELETE FROM scrddm WHERE rd_id = ?";
        String instRddm = 
                "INSERT INTO public.scrddm( "
              + "    rd_id, room_seq, sect_seq, room_id, examiner, patient1, patient2, patient3, examinee) "
              + "VALUES "
              + "    (?, ?, ?, ?, ?, ?, ?, ?, ?);";
        String upRdmmStsS = "UPDATE scrdmm SET sts_e = ? WHERE rd_id = ? \n";
        
        String rdId = req.get("rdId");
        delCnt = dbu.executeList(delSect, rdId);
        
        List<Map<String, Object>> rowRdrms = qryRdrmList(req, dbu); //考站資料
                
        int roomSeq, sectSeq;
        String roomId, examiner, patient1, patient2, patient3, examinee;
        
        JSONArray examineeJsonArr = new JSONArray(req.get("pExamineeList"));
        for (int i = 0; i < examineeJsonArr.length(); i++) {
            JSONObject examineeObj = examineeJsonArr.getJSONObject(i);

            roomSeq = (Integer) rowRdrms.get(i).get("roomSeq");
            sectSeq = 1;
            roomId = (String) rowRdrms.get(i).get("roomId");
            examiner = (String) rowRdrms.get(i).get("examiner");
            patient1 = (String) rowRdrms.get(i).get("patient1");
            patient2 = (String) rowRdrms.get(i).get("patient2");
            patient3 = (String) rowRdrms.get(i).get("patient3");
            examinee = (!examineeObj.isNull("examinee")) ? examineeObj.getString("examinee") : "";
            dbu.executeList(instRddm, rdId, roomSeq, sectSeq, roomId, examiner, patient1, patient2, patient3, examinee);
            if (!examineeObj.isNull("examinee"))
                cnt++;
        }
        
        if (cnt == examineeJsonArr.length())
            dbu.executeList(upRdmmStsS, "S", rdId); //S 產生梯次表
        else
            dbu.executeList(upRdmmStsS, null, rdId);

        if (cnt < delCnt)
            cnt = delCnt;
        return cnt;
    }
    
    //查詢梯次
    private List<Map<String, Object>> qryRdList(Map<String, String> req, DbUtil dbu) throws SQLException, Exception {
        List<Map<String, Object>> rowRds;
        //UserData ud = UserData.getUserData();

        String rdId = req.get("rdId");
        String rdDesc = req.get("rdDesc");
        String rdDateS = req.get("rdDateS");
        String rdDateE = req.get("rdDateE");
        int page = (req.get("page") == null || req.get("page").equals("")) ? 1 : Integer.parseInt(req.get("page"));
        int rows = (req.get("rows") == null || req.get("rows").equals("")) ? 1 : Integer.parseInt(req.get("rows"));
        
        if (rdDateS != null) 
            rdDateS = rdDateS.replaceAll("/", "");
        if (rdDateE != null) 
            rdDateE = rdDateE.replaceAll("/", "");
        
        String sqlQryRd =
            " SELECT rd_id \"rdId\", rd_desc \"rdDesc\"\n"
          + "      , substr(rd_date, 1, 4) || '/' || substr(rd_date, 5, 2) || '/' || substr(rd_date, 7, 2) \"rdDate\"\n"
          + "      , substr(beg_time, 1, 2) || ':' || substr(beg_time, 3, 2) \"begTime\"\n"
          + "      , qs_count \"qsCount\", read_time \"readTime\", exam_time \"examTime\"\n"
          + "      , sts_r \"stsR\", sts_s \"stsS\", sts_e \"stsE\"\n"
          + "   FROM scrdmm a\n"
          + "  WHERE 1=1\n"
          + "    AND (coalesce(?, '') = '' OR rd_id = ?)\n"
          + "    AND (coalesce(?, '') = '' OR rd_desc LIKE '%' || ? || '%')\n"
          + "    AND (coalesce(?, '') = '' OR ? <= rd_date)\n"
          + "    AND (coalesce(?, '') = '' OR ? >= rd_date)\n"
          + "  ORDER BY rd_id\n"
          + "  LIMIT ? OFFSET ? \n";
        rowRds = dbu.selectMapAllList(sqlQryRd, rdId, rdId, rdDesc, rdDesc, rdDateS, rdDateS, rdDateE, rdDateE, rows, ((page-1) * rows));
        System.out.println("qryRdList : rdId = " + rdId + " size = " + rowRds.size());
        return rowRds;
    }
    
    /**
     * 查詢梯次
     * @param req
     * @return
     */
    @RequestMapping(value = "/ScRunDown_qryRd", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> qryRd(@RequestParam Map<String, String> req) {
        Map<String, Object> res = new HashMap<String, Object>();
        res.put("status", "");
        res.put("statusTime", new StdCalendar().toTimesString());

        res.put("success", false);
        List<Map<String, Object>> rowRds;
        DbUtil dbu = new DbUtil();
        try
        {            
            rowRds = qryRdList(req, dbu);
            if (rowRds.size() == 0)
                throw new StopException("查無梯次資料");
            res.put("rdmmList", rowRds);
            
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
     * 編輯梯次
     * @param req
     * @return
     */
    @RequestMapping(value = "/ScRunDown_editRd", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> editRd(@RequestParam Map<String, String> req) {
        Map<String, Object> res = new HashMap<String, Object>();
        res.put("status", "");
        res.put("statusTime", new StdCalendar().toTimesString());

        DbUtil dbu = new DbUtil();
        try {
            res.put("success", false);
            UserData ud = UserData.getUserData();
            if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
                throw new StopException("您無權限執行此動作");

            String rdId = req.get("pRdId");
            String procRdId = rdId;
            String editType = req.get("editType");
            String newRdId = req.get("pNewRdId"); //複製用
            
            if (editType.equals("E")) {
                String rdDesc = req.get("pRdDesc");
                String rdDate = req.get("pRdDate");
                String begTime  = req.get("pBegTime");
                int qsCount = Integer.parseInt(req.get("pQsCount"));
                int readTime = Integer.parseInt(req.get("pReadTime"));
                int examTime = Integer.parseInt(req.get("pExamTime"));
                String copyQs = req.get("copyQs");
                String copyExaminer = req.get("copyExaminer");
                String copyPatient = req.get("copyPatient");
                String copySect = req.get("copySect");
                
                if (rdDesc == null)
                    throw new StopException("請輸入梯次說明");
                if (rdDate == null) 
                    throw new StopException("請輸入梯次日期");
                else
                    rdDate = rdDate.replaceAll("/", "");
                if (begTime == null)
                    throw new StopException("請輸入開始時間");
                else
                    begTime = begTime.replaceAll(":", "");
                
                if (qsCount <= 0)
                    throw new StopException("試題數量");
                
                if (readTime <= 0)
                    throw new StopException("請輸入讀題時間（分）");
                if (examTime <= 0)
                    throw new StopException("請輸入考試時間（分）");
                if (newRdId != null && newRdId != "" && !newRdId.equals("*"))
                    throw new StopException("已複製請重新查詢");
                
                String sqlUpRd;
                if ((rdId == null || rdId.equals("")) || (newRdId != null && newRdId != "" && newRdId.equals("*"))) { 
                    String toDay =  new StdCalendar().toDateString().replaceAll("/", "");
                    String sqlQryMaxRd =
                        " SELECT substr(MAX(rd_id), 9, 3) rdSeq\n"
                      + "   FROM scrdmm \n"
                      + "  WHERE substr(rd_id, 1, 8) = ? \n";
                    String rdSeq = dbu.selectStringList(sqlQryMaxRd, toDay);
                    if (rdSeq == null)
                        rdSeq = "0";
                    int newRdSeq = Integer.parseInt(rdSeq) + 1;
                    procRdId = toDay + String.format("%03d", newRdSeq) ;
                    System.out.println("procRdId = " + procRdId);
                    newRdId = procRdId;
                    sqlUpRd =
                        "INSERT INTO scrdmm \n"
                      + "    (rd_desc, rd_date, beg_time, qs_count, read_time, exam_time, rd_id)\n" 
                      + "VALUES \n"
                      + "    (?, ?, ?, ?, ?, ?, ?);";
                }
                else
                {
                    sqlUpRd = 
                        "UPDATE public.scrdmm \n"
                      + "   SET rd_desc=? \n"
                      + "     , rd_date=?\n"
                      + "     , beg_time=?\n"
                      + "     , qs_count=?\n"
                      + "     , read_time=?\n"
                      + "     , exam_time=?\n"
                      + " WHERE rd_id = ?;";
                }
                dbu.executeList(sqlUpRd, rdDesc, rdDate, begTime, qsCount, readTime, examTime, procRdId);
            }
            else if (editType.equals("D")) {
                if (rdId == null || rdId.length() == 0)
                    throw new StopException("請指定梯次代碼");
                
                String sqlDelRd =
                        "DELETE FROM PUBLIC.SCRDMM"
                      + " WHERE rd_Id = ?";
                int cnt = dbu.executeList(sqlDelRd, rdId);
                if (cnt == 0)
                    throw new StopException("查無" + rdId + "梯次代碼");
            }
            dbu.doCommit();
            
            /*畫面的reload會呼叫，這裡就不用做了
            List<Map<String, Object>> rowRds = qryRdList(req, dbu);
            if (rowRds.size() == 0)
                throw new StopException("查無梯次資料");
            res.put("rdmmList", rowRds);*/
            res.put("pNewRdId", newRdId);
            
            res.put("success", true);
            if (editType.equals("E"))
                res.put("status", "梯次資料編輯完成");
            else if (editType.equals("D"))
                res.put("status", "梯次資料已刪除");
        }
        catch (StopException e) {
            res.put("status", e.getMessage());
        }
        catch (SQLException e) {
            res.put("status", DbUtil.exceptionTranslation(e));
            e.printStackTrace();
        }
        catch (Exception e) {
            res.put("status", ExceptionUtil.procExceptionMsg(e));
            e.printStackTrace();
        }
        dbu.relDbConn();
        
        return res;
    }
    
    /**
     * 查詢考站
     * @param req
     * @return
     */
    @RequestMapping(value = "/ScRunDown_qryRdrm", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> qryRdrm(@RequestParam Map<String, String> req) {
        Map<String, Object> res = new HashMap<String, Object>();
        res.put("status", "");
        res.put("statusTime", new StdCalendar().toTimesString());

        res.put("success", false);
        List<Map<String, Object>> rowRdrms;
        DbUtil dbu = new DbUtil();
        try
        {            
            rowRdrms = qryRdrmList(req, dbu);
            List<Map<String, Object>> rowRd = qryRdList(req, dbu);
            int qs_count = (Integer) rowRd.get(0).get("qsCount");
            
            if (rowRdrms.size() == 0){
                rowRdrms = new ArrayList<Map<String, Object>>();
                for (int i=1; i<=qs_count; i++) {
                    Map<String, Object> rdrm = new HashMap<String, Object>();
                    rdrm.put("roomSeq", i);
                    rowRdrms.add(rdrm);
                }
            }
            else if (rowRdrms.size() < qs_count) {
                List<Map<String, Object>> rowRdrmsTemp = new ArrayList<Map<String, Object>>();
                for (int i=1; i<=qs_count; i++) {
                    Map<String, Object> rdrm = new HashMap<String, Object>();
                    rdrm.put("roomSeq", i);
                    rowRdrmsTemp.add(rdrm);
                }
                for (int i=0; i<rowRdrms.size(); i++){
                    int target = ((Integer) rowRdrms.get(i).get("roomSeq") - 1);
                    /*
                    for (String key: rowRdrms.get(i).keySet()) {
                        rowRdrmsTemp.get(target).put(key, rowRdrms.get(i).get(key));
                    }*/
                    rowRdrmsTemp.get(target).putAll(rowRdrms.get(i));
                }
                rowRdrms = null;
                rowRdrms = rowRdrmsTemp;
            }
            res.put("rdrmList", rowRdrms);
            
            res.put("success", true);
            res.put("status", "查詢考站資料完成");
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
     * 異動考站
     * @param req
     * @return
     */
    @RequestMapping(value = "/ScRunDown_editRdrm", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> editRdrm(@RequestParam Map<String, String> req) {
        Map<String, Object> res = new HashMap<String, Object>();
        res.put("status", "");
        res.put("statusTime", new StdCalendar().toTimesString());

        res.put("success", false);
        int upRooms = 0;
        DbUtil dbu = new DbUtil();
        try
        {   
            upRooms = updataRdrm(req, dbu);
            if (upRooms== 0)
                throw new StopException("更新考站失敗");
            
            dbu.doCommit();
           
            /*畫面reload會來呼叫這裡就不用做了
            List<Map<String, Object>> rowRds = qryRdList(req, dbu);
            if (rowRds.size() == 0)
                throw new StopException("查無梯次資料");
            res.put("rdmmList", rowRds);*/
            res.put("cnt", upRooms);
            res.put("success", true);
            res.put("status", "考站資料更新完成");
        }
        catch (StopException e) {
            res.put("status", e.getMessage());
        }
        catch (SQLException e) {
            res.put("status", DbUtil.exceptionTranslation(e));
            System.out.println(e.toString());
        }
        catch (Exception e) {
            res.put("status", ExceptionUtil.procExceptionMsg(e));
            System.out.println(e.toString());
        }
        dbu.relDbConn();
        
        return res;
    }
    
    /**
     * 查詢節次
     * @param req
     * @return
     */
    @RequestMapping(value = "/ScRunDown_qrySect", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> qrySect(@RequestParam Map<String, String> req) {
        Map<String, Object> res = new HashMap<String, Object>();
        res.put("status", "");
        res.put("statusTime", new StdCalendar().toTimesString());

        res.put("success", false);
        List<Map<String, Object>> rowSects;
        
        DbUtil dbu = new DbUtil();
        try
        {            
            String rdId = req.get("rdId");
            rowSects = qrySectList(req, dbu);
            if (rowSects.size() == 0) {
                rowSects = new ArrayList<Map<String, Object>>();
                List<Map<String, Object>> rowRd = qryRdList(req, dbu);
                int qs_count = (Integer) rowRd.get(0).get("qsCount");
                int readTime = (Integer) rowRd.get(0).get("readTime");
                int examTime = (Integer) rowRd.get(0).get("examTime");
                String[] noShowType = {"REA","EXA"};
                
                for (int i=1; i<=qs_count; i++) {
                    int y = 1;
                    for (String sectType : noShowType) {
                        Map<String, Object> sect = new HashMap<String, Object>();
                        sect.put("rdId", rdId);
                        sect.put("sectSeq", i);
                        sect.put("seqNo", (i * 2) - y);
                        sect.put("sectType", sectType);
                        sect.put("sectName", CodeSvc.queryCodeDesc(dbu, "SCSECT", sectType));
                        if (sectType.equals("REA"))
                            sect.put("sectTime", readTime);
                        if (sectType.equals("EXA"))
                            sect.put("sectTime", examTime);
                        rowSects.add(sect);
                        y--;
                    }
                    
                }
            }
                
            res.put("sectList", rowSects);
            
            res.put("success", true);
            res.put("status", "查詢考站資料完成");
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
     * 異動節次
     * @param req
     * @return
     */
    @RequestMapping(value = "/ScRunDown_editSect", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> editSect(@RequestParam Map<String, String> req) {
        Map<String, Object> res = new HashMap<String, Object>();
        res.put("status", "");
        res.put("statusTime", new StdCalendar().toTimesString());

        res.put("success", false);
        int upSect = 0;
        DbUtil dbu = new DbUtil();
        try
        {   
            upSect = updataSect(req, dbu);
            if (upSect== 0)
                throw new StopException("更新節次失敗");
            
            dbu.doCommit();
           
            /* 畫面的reload會再來呼叫這裡就不用做了
            List<Map<String, Object>> rowRds = qryRdList(req, dbu);
            if (rowRds.size() == 0)
                throw new StopException("查無梯次資料");
            res.put("rdmmList", rowRds);*/
            res.put("cnt", upSect);
            res.put("success", true);
            res.put("status", "節次資料更新完成");
        }
        catch (StopException e) {
            res.put("status", e.getMessage());
        }
        catch (SQLException e) {
            res.put("status", DbUtil.exceptionTranslation(e));
            System.out.println(e.toString());
        }
        catch (Exception e) {
            res.put("status", ExceptionUtil.procExceptionMsg(e));
            System.out.println(e.toString());
        }
        dbu.relDbConn();
        
        return res;
    }
    
    /**
     * 查詢考生
     * @param req
     * @return
     */
    @RequestMapping(value = "/ScRunDown_qryExaminee", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> qryExaminee(@RequestParam Map<String, String> req) {
        Map<String, Object> res = new HashMap<String, Object>();
        res.put("status", "");
        res.put("statusTime", new StdCalendar().toTimesString());

        res.put("success", false);
        List<Map<String, Object>> rowExaminee;
        DbUtil dbu = new DbUtil();
        try
        {            
            rowExaminee = qryExamineeList(req, dbu);
            List<Map<String, Object>> rowRd = qryRdList(req, dbu);
            int qs_count = (Integer) rowRd.get(0).get("qsCount");
            
            if (rowExaminee.size() == 0){
                rowExaminee = new ArrayList<Map<String, Object>>();
                for (int i=1; i<=qs_count; i++) {
                    Map<String, Object> rdrm = new HashMap<String, Object>();
                    rdrm.put("roomSeq", i);
                    rowExaminee.add(rdrm);
                }
            }
            else if (rowExaminee.size() < qs_count) {
                List<Map<String, Object>> rowExamineeTemp = new ArrayList<Map<String, Object>>();
                for (int i=1; i<=qs_count; i++) {
                    Map<String, Object> rdrm = new HashMap<String, Object>();
                    rdrm.put("roomSeq", i);
                    rowExamineeTemp.add(rdrm);
                }
                for (int i=0; i<rowExaminee.size(); i++){
                    int target = ((Integer) rowExaminee.get(i).get("roomSeq") - 1);
                    /*
                    for (String key: rowRdrms.get(i).keySet()) {
                        rowRdrmsTemp.get(target).put(key, rowRdrms.get(i).get(key));
                    }*/
                    rowExamineeTemp.get(target).putAll(rowExaminee.get(i));
                }
                rowExaminee = null;
                rowExaminee = rowExamineeTemp;
            }
            res.put("examineeList", rowExaminee);
            
            res.put("success", true);
            res.put("status", "查詢考生資料完成");
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
     * 異動考生
     * @param req
     * @return
     */
    @RequestMapping(value = "/ScRunDown_editExaminee", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> editExaminee(@RequestParam Map<String, String> req) {
        Map<String, Object> res = new HashMap<String, Object>();
        res.put("status", "");
        res.put("statusTime", new StdCalendar().toTimesString());

        res.put("success", false);
        int upExaminee = 0;
        DbUtil dbu = new DbUtil();
        try
        {   
            upExaminee = updataExaminee(req, dbu);
            if (upExaminee== 0)
                throw new StopException("更新節次失敗");
            
            dbu.doCommit();

            res.put("cnt", upExaminee);
            res.put("success", true);
            res.put("status", "考生資料更新完成");
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
