package com.ron.exam.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import com.ron.exam.util.StdCalendar;
import com.ron.exam.util.StopException;
import com.ron.exam.util.DbUtil;
import com.ron.exam.util.ExceptionUtil;
import com.ron.exam.util.OperLog;
import com.ron.exam.service.MiscTool;
import com.ron.exam.service.ParamSvc;
import com.ron.exam.service.ProgData;
import com.ron.exam.service.QsSvc;
import com.ron.exam.service.UserData;

@EnableWebMvc
@Controller
public class QsMntTmplAction {

	private static final String c_progId = "QsMntTmpl";
	
	@RequestMapping(value = "/QsMntTmpl", method = RequestMethod.GET)
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
			
			res.put("departList", ParamSvc.buildSelectOptionByClass(dbu, ParamSvc.c_clsDepart, false)); 
			res.put("fileClassList", ParamSvc.buildSelectOptionByClass(dbu, ParamSvc.c_clsQsFileClass, false)); 
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
		return "QsMntTmpl";
	}
		
	/**
	 * 重新排列同科別所有樣版的顯示順序，可以特別指定某一樣版插在指定位置
	 * @param dbu
	 * @param tmplDepart 科別代碼
	 * @param tmplId 指定插在特定位置的樣版
	 * @param showOrder 指定位置，0 表示排在最後面
	 */
	private void tmplReorder(DbUtil dbu, String tmplDepart, String tmplId, int showOrder) throws SQLException {
		if (showOrder == 0)
			showOrder = Integer.MAX_VALUE;
		String sqlQryTmpl = " SELECT qstmp_id FROM qstpms WHERE depart_id = ? ORDER BY show_order";
		String sqlUpdTmpl = " UPDATE qstpms SET show_order = ? WHERE qstmp_id = ?";
		int seq = 1;
		boolean added = false;
		ResultSet rsTmpl = dbu.queryList(sqlQryTmpl, tmplDepart);
		while (rsTmpl.next()) {
			String id = rsTmpl.getString("qstmp_id");
			if (seq == showOrder && !added) {
				added = true;
				if (!id.equals(tmplId)) {
					dbu.executeList(sqlUpdTmpl, seq, tmplId);
					seq++;
				}
			}
			else if (tmplId.equals(id))
				continue;
			dbu.executeList(sqlUpdTmpl, seq, id);
			seq++;
		}
		if (!added)
			dbu.executeList(sqlUpdTmpl, seq, tmplId);
		rsTmpl.close();
	}
	
	/**
	 * 重新排列檔案的顯示順序，可以特別指定某一檔案插在指定位置
	 * @param dbu
	 * @param tmplId 樣版代碼
	 * @param fileName 指定插在特定位置的檔名
	 * @param showOrder 指定位置，0 表示排在最後面
	 */
	private void fileReorder(DbUtil dbu, String tmplId, String fileName, int showOrder) throws SQLException {
		if (showOrder == 0)
			showOrder = Integer.MAX_VALUE;
		String sqlQryFile = " SELECT file_name FROM qstpfl WHERE qstmp_id = ? ORDER BY show_order";
		String sqlUpdFile = " UPDATE qstpfl SET show_order = ? WHERE qstmp_id = ? AND file_name = ?";
		int seq = 1;
		boolean added = false;
		ResultSet rsTmpl = dbu.queryList(sqlQryFile, tmplId);
		while (rsTmpl.next()) {
			String name = rsTmpl.getString("file_name");
			if (seq == showOrder && !added) {
				added = true;
				if (!name.equals(fileName)) {
					dbu.executeList(sqlUpdFile, seq, tmplId, fileName);
					seq++;
				}
			}
			else if (fileName.equals(name))
				continue;
			dbu.executeList(sqlUpdFile, seq, tmplId, name);
			seq++;
		}
		if (!added)
			dbu.executeList(sqlUpdFile, seq, tmplId, fileName);
		rsTmpl.close();
	}

	/**
	 * 建立樣版中的檔案清單資料
	 * @param dbu
	 * @param tmplId
	 * @return
	 * @throws SQLException
	 */
	private List<Map<String, Object>> buildFileListData(DbUtil dbu, String tmplId) throws SQLException {
		String sqlQryFile =
				" SELECT file_name, file_desc, file_class, show_order, file_size, file_type\n"
			  + "   FROM qstpfl\n"
			  + "  WHERE qstmp_id = ?\n"
			  + "  ORDER BY show_order\n";
		Map<String, String> fileClassMap = ParamSvc.buildStringMapByClass(dbu, ParamSvc.c_clsQsFileClass);
		ResultSet rsFile = dbu.queryList(sqlQryFile, tmplId);			
		List<Map<String, Object>> fileList = new ArrayList<Map<String, Object>>();
		while (rsFile.next()) {					
			Map<String, Object> file = new HashMap<String, Object>();
			file.put("fileName", rsFile.getString("file_name"));
			String fileDesc = rsFile.getString("file_desc");
			int p = fileDesc.lastIndexOf('.');
			if (p >= 0)
				fileDesc = fileDesc.substring(0, p);
			file.put("fileDesc", fileDesc);
			file.put("fileClass", fileClassMap.get(rsFile.getString("file_class")));
			file.put("showOrder", Integer.toString(rsFile.getInt("show_order")));
			file.put("fileSize", MiscTool.convToCommaInt(rsFile.getInt("file_size")));
			file.put("fileType", rsFile.getString("file_type"));
			fileList.add(file);
		}													
		rsFile.close();
		return fileList;
	}

	@RequestMapping(value = "/QsMntTmpl_qryTmplList", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryTmplList(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success", false);
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
			
			StringBuffer sqlQryTmpl = new StringBuffer(
				  " SELECT qstmp_id, qstmp_name, depart_id, show_order\n");
			StringBuffer sqlCntTmpl = new StringBuffer(" SELECT COUNT(*)");
			StringBuffer sqlCond = new StringBuffer(
				  "   FROM qstpms \n"
				+ "  WHERE 1 = 1\n");
			
			List<Object> params = new ArrayList<Object>();
			String qryDepart = req.get("qryDepart");
			if (!qryDepart.isEmpty()) {
				sqlCond.append(" AND depart_id = ?\n");
				params.add(qryDepart);
			}
			//sqlCond.append(" ORDER BY show_order\n");
			
			// 非換頁查詢時，需先查詢總筆數
			if ("N".equals(mode) || "T".equals(mode)) {
				sqlCntTmpl.append(sqlCond);
				res.put("total", dbu.selectIntArray(sqlCntTmpl.toString(), params.toArray()));
			}
			
			sqlQryTmpl.append(sqlCond);
			
			if (pageRow != 0) {
				sqlQryTmpl.append(" OFFSET ? LIMIT ?\n");
				params.add(pageAt * pageRow);
				params.add(pageRow);
			}
			
			Map<String, String> departMap = ParamSvc.buildStringMapByClass(dbu, "DEPART");
			ResultSet rsTmpl = dbu.queryArray(sqlQryTmpl.toString(), params.toArray());
			List<Map<String, Object>> tmplList = new ArrayList<Map<String, Object>>();
			while (rsTmpl.next()) {
				Map<String, Object> tmpl = new HashMap<String, Object>();
				tmpl.put("tmplId", rsTmpl.getString("qstmp_id"));
				tmpl.put("tmplName", rsTmpl.getString("qstmp_Name"));
				tmpl.put("tmplDepart", departMap.get((rsTmpl.getString("depart_id"))));
				tmpl.put("showOrder", Integer.toString(rsTmpl.getInt("show_order")));
				tmplList.add(tmpl);
			}												
			
			rsTmpl.close();
			res.put("tmplList", tmplList);
			res.put("success", true);
			res.put("status", "查詢樣板列表完成");
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
	
	@RequestMapping(value = "/QsMntTmpl_qryTmpl", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryTmpl(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success", false);
			String tmplId = req.get("tmplId");
			String qryType = req.get("qryType");
			
			if ("T".equals(qryType)) {
				String sqlQryTmpl = " SELECT qstmp_name, file_path, depart_id, mod_date, show_order, remark"
						+ "   FROM qstpms a\n"
						+ "  WHERE qstmp_id = ?\n";
				Map<String, Object> rowTmpl = dbu.selectMapRowList(sqlQryTmpl, tmplId);
				if (rowTmpl == null)
					throw new StopException("指定樣板代碼 <" + tmplId + "> 不存在");
				res.put("tmplId", tmplId);
				res.put("tmplName", rowTmpl.get("qstmp_name"));
				res.put("showOrder", rowTmpl.get("show_order"));
				res.put("tmplDepart", rowTmpl.get("depart_id"));
				res.put("remark", DbUtil.nullToEmpty((String) rowTmpl.get("remark")));
			}
			
			if ("F".equals(qryType)) {
				res.put("fileList", buildFileListData(dbu, tmplId));
			}
			dbu.doCommit();
			
			res.put("success", true);
			res.put("status", "查詢樣板完成");
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
	
	@RequestMapping(value = "/QsMntTmpl_addTmpl", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addTmpl(@RequestParam Map<String, String> req) {
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
			String tmplId = req.get("tmplId");	
			if("".equals(tmplId)){
				//自動取代碼
				for(int i = 1; i < 100 && tmplId.isEmpty(); i++){
					String id = String.format("%s%02d", today, i);
					String sqlCntTmpl = " SELECT COUNT(*) FROM qstpms WHERE qstmp_id = ?";
					if (dbu.selectIntList(sqlCntTmpl, tmplId) == 0)
					tmplId = id;
				}
				if (tmplId.isEmpty())
					throw new StopException("無法自動取得樣版代碼");
			}			
			else {
				String sqlCntTmpl = " SELECT COUNT(*) FROM qstpms WHERE qstmp_id = ?";
				if (dbu.selectIntList(sqlCntTmpl, tmplId) > 0)
					throw new StopException("樣板代碼 " + tmplId + " 已存在");
			}						
			String tmplName = req.get("tmplName");
			String tmplDepart = req.get("tmplDepart");
			if (tmplDepart.isEmpty())
				throw new StopException("請先指定科別");
			String tmplremark = DbUtil.emptyToNull((String) req.get("tmplRemark"));
			
			String basePath = QsSvc.getTmplBasePath();			
			File tmplPath = File.createTempFile("tmpl_", "", new File(basePath));
			if (!tmplPath.delete())
				throw new StopException("無法刪除暫存檔案 " + tmplPath.getAbsolutePath());
			if (!tmplPath.mkdir())
				throw new StopException("無法建立樣版目錄 " + tmplPath.getAbsolutePath());		
			
			String sqlInsTmpl =
				  " INSERT INTO qstpms(qstmp_id, qstmp_name, file_path, depart_id, show_order,\n"
				+ "	            mod_date, mod_user_id, remark)\n"
				+ " VALUES(?, ?, ?, ?, 0,  ?, ?, ?)\n";
			dbu.executeList(sqlInsTmpl, tmplId, tmplName, tmplPath.getName(), tmplDepart,
				today, ud.getUserId(), tmplremark);
			tmplReorder(dbu, tmplDepart, tmplId, 0);
			
			dbu.doCommit();
		
			OperLog operLog = new OperLog(c_progId, "addTmpl");
            operLog.add("qstmpId", tmplId);
            operLog.add("tmplName", tmplName);
            operLog.add("tmplDepart", tmplDepart);
            operLog.add("tmplremark", tmplremark);
            operLog.write();

            res.put("success", true);
			res.put("status", "新增樣板完成");
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
	
	@RequestMapping(value = "/QsMntTmpl_modTmpl", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> modTmpl(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {		
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			String tmplIdOrg = req.get("tmplIdOrg");
			String tmplId = req.get("tmplId");
			if ("".equals(tmplIdOrg))
				throw new StopException("請先查詢樣版後再修改");
			if ("".equals(tmplId))
				throw new StopException("請先指定樣版代碼");
			String sqlCntTmpl = " SELECT COUNT(*) FROM qstpms WHERE qstmp_id = ?";
			if (dbu.selectIntList(sqlCntTmpl, tmplIdOrg) == 0)
				throw new StopException("原樣版代碼 <" + tmplIdOrg + "> 不存在");
			if (!tmplId.equals(tmplIdOrg) && dbu.selectIntList(sqlCntTmpl, tmplId) > 0)
				throw new StopException("樣版代碼 <" + tmplId + "> 已存在");
			String tmplDepart = req.get("tmplDepart");
			String tmplName = req.get("tmplName");
			String showOrderStr = req.get("showOrder");
			int showOrder;
			try {
				showOrder = Integer.parseInt(showOrderStr);
			}
			catch (NumberFormatException e) {
				throw new StopException("顯示順序不是正確的數字");
			}
			String tmplRemark = DbUtil.emptyToNull((String) req.get("tmplRemark"));
			
			String sqlQryTmpl =
				  " SELECT qstmp_name, depart_id, show_order, mod_date, mod_user_id, remark\n"
				+ "   FROM qstpms\n"
				+ "  WHERE qstmp_id = ?\n";
			ResultSet rsTmpl = dbu.queryList(sqlQryTmpl, tmplIdOrg);
			if (!rsTmpl.next())
				throw new StopException("原樣版代碼 <" + tmplIdOrg + "> 不存在");
			String tmplNameOrg = rsTmpl.getString("qstmp_name");
			String tmplDepartOrg = rsTmpl.getString("depart_id");
			int showOrderOrg = rsTmpl.getInt("show_order");
			String modDateOrg = rsTmpl.getString("mod_date");
			String modUserIdOrg = rsTmpl.getString("mod_user_id");
			String tmplRemarkOrg = rsTmpl.getString("remark");
			rsTmpl.close();

			String sqlUpdTmpl =
				  " UPDATE qstpms SET\n"
				+ "        qstmp_id = ?\n"
				+ "      , qstmp_name = ?\n"
				+ "      , depart_id = ?\n"
				+ "      , mod_date = ?\n"
				+ "      , mod_user_id = ?\n"
				+ "      , remark = ?\n"
				+ "  WHERE qstmp_id = ?\n";
			String today = new StdCalendar().toDbDateString();

			dbu.executeList(sqlUpdTmpl, tmplId, tmplName, tmplDepart, today, ud.getUserId(), tmplRemark, tmplIdOrg);
			tmplReorder(dbu, tmplDepart, tmplId, showOrder);
			if (!tmplDepartOrg.equals(tmplDepart))
				tmplReorder(dbu, tmplDepartOrg, "", 0);
			if (!tmplId.equals(tmplIdOrg)) {
				String sqlUpdFile =
					  " UPDATE qstpfl SET qstmp_id = ? WHERE qstmp_id = ?\n";
				dbu.executeList(sqlUpdFile, tmplId, tmplIdOrg);
			}
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "modTmpl");
            if (tmplId.equals(tmplIdOrg))
	            operLog.add("qstmpId", tmplId);
			else
				operLog.add("qstmpId", tmplIdOrg, tmplId);
            operLog.add("tmplName", tmplNameOrg, tmplName);
            operLog.add("tmplDepart", tmplDepartOrg, tmplDepart);
            operLog.add("modDate", modDateOrg, today);
            operLog.add("modUserId", modUserIdOrg, ud.getUserId());
            operLog.add("showOrder", showOrderOrg, showOrder);
            operLog.add("tmplremark", tmplRemarkOrg, tmplRemark);
            operLog.write();
			
            res.put("success", true);
			res.put("status", "修改樣板完成");
			
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
	
	@RequestMapping(value = "/QsMntTmpl_delTmpl", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> delTmpl(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			
			String tmplId = req.get("tmplId");
			if (tmplId.isEmpty())
				throw new StopException("請先指定樣板代碼");
			String sqlQryTmpl = " SELECT depart_id FROM qstpms WHERE qstmp_id = ?";
			String departId = dbu.selectStringList(sqlQryTmpl, tmplId);
			if (departId == null)
				throw new StopException("樣板代碼 <" + tmplId + "> 不存在");
			
			// 先移除檔案
			File tmplPath = new File(QsSvc.getTmplFilePath(dbu, tmplId));
			String sqlQryFile = " SELECT file_name FROM qstpfl WHERE qstmp_id = ?";
			ResultSet rsFile = dbu.queryList(sqlQryFile, tmplId);
			while (rsFile.next()) {
				String fileName = rsFile.getString("file_name");
				new File(tmplPath, fileName).delete();
			}			
			rsFile.close();
			tmplPath.delete();
			
			String sqlDelTmpl =
				  " DELETE FROM qstpms WHERE qstmp_id = ?";
			dbu.executeList(sqlDelTmpl, tmplId);
			
			String sqlDelFile =
				  " DELETE FROM qstpfl WHERE qstmp_id = ?";
			dbu.executeList(sqlDelFile, tmplId);
			tmplReorder(dbu, departId, "", 0);
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "delTmpl");
			operLog.add("qstmpId", tmplId);
            operLog.write();
			
            res.put("success", true);
			res.put("status", "刪除樣板完成");
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
	
	@RequestMapping(value = "/QsMntTmpl_qryFile", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryFile(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success", false);
			String tmplId = req.get("tmplId");
			String fileName = req.get("fileName");			
			String sqlQryFile =
				  " SELECT file_desc, file_class, show_order, remark\n"
				+ "   FROM qstpfl a\n"
				+ "  WHERE qstmp_id = ?\n"
				+ "    AND file_name = ?\n";
			Map<String, Object> rowFile = dbu.selectMapRowList(sqlQryFile, tmplId, fileName);
			if (rowFile == null)
				throw new StopException("無此檔案資料");
			res.put("fileDesc", rowFile.get("file_desc"));
			res.put("fileClass", rowFile.get("file_class"));
			res.put("showOrder", rowFile.get("show_order"));
			res.put("remark", DbUtil.nullToEmpty((String) rowFile.get("remark")));
			dbu.doCommit();
			
			res.put("success", true);
			res.put("status", "查詢檔案完成");
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
	 * 上傳檔案，也就是新增檔案
	 * @param req
	 * @param uparg
	 * @return
	 */
	@RequestMapping(value = "/QsMntTmpl_uploadFile", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> uploadFile(@RequestParam Map<String, String> req , @RequestParam("file") MultipartFile uparg) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();		
		try {
			UserData ud = UserData.getUserData();
			String today = new StdCalendar().toDbDateString();
			String tmplId = req.get("tmplId");
			if (tmplId.isEmpty())
				throw new StopException("樣版代碼不可以為空白");
			String fileClass = req.get("fileClass");
			if (fileClass.isEmpty())
				throw new StopException("請先選擇檔案類別");
			String showOrderStr = req.get("showOrder");
			int showOrder = 0;
			try {
				if (!showOrderStr.isEmpty())
					showOrder = Integer.parseInt(showOrderStr);
			}
			catch (NumberFormatException e) {
				throw new StopException("顯示順序不是正確的數字");
			}
			String remark = DbUtil.emptyToNull(req.get("remark"));
						
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			String contentType = uparg.getContentType();
			String clientFileName = uparg.getOriginalFilename();
			File serverUploadFile;
			String serverFileName;

			// 儲存檔案
			try {
				File tmplPath = new File(QsSvc.getTmplFilePath(dbu, tmplId));
				res.put("contentType", contentType);
				res.put("clientFileName", clientFileName);
				serverUploadFile = File.createTempFile("file_", "_" + clientFileName, tmplPath);
				serverFileName = serverUploadFile.getName();
				res.put("serverFileName", serverFileName);
				uparg.transferTo(serverUploadFile);
				res.put("contentLength", serverUploadFile.length());
			}
			catch (IOException e) {
				throw new StopException("儲存上傳檔案失敗: " + e.toString());
			}
			
			String sqlInsFile =
					  " INSERT INTO qstpfl(qstmp_id, file_name, file_class, file_desc, file_size,\n"
					+ "	            file_type, show_order, cr_datetime, cr_user_id, remark)\n"
					+ " VALUES(?, ?, ?, ?, ?,  ?, 0, ?, ?, ?)\n";
			dbu.executeList(sqlInsFile, tmplId, serverFileName, fileClass, clientFileName, serverUploadFile.length(),
					contentType, today, ud.getUserId(), remark);
			fileReorder(dbu, tmplId, serverFileName, showOrder);
			dbu.doCommit();

			OperLog operLog = new OperLog(c_progId, "addFile");
			operLog.add("tmplId", tmplId);
			operLog.add("fileName", res.get("serverFileName"));
			operLog.add("fileDesc", res.get("clientFileName"));
			operLog.add("fileSize", res.get("contentLength"));
			operLog.add("fileType", res.get("contentType"));
			operLog.add("fileClass", fileClass);
			operLog.add("crDatetime", today);
			operLog.add("crUser", ud.getUserId());
			operLog.add("showOrder", showOrder);
			operLog.add("remark", remark);
            operLog.write();
			
			res.put("fileList", buildFileListData(dbu, tmplId));
			res.put("success", true);
			res.put("status", "上傳檔案完成");
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
	
	@RequestMapping(value = "/QsMntTmpl_modFile", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> modFile(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {		
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			String tmplId = req.get("tmplId");
			if (tmplId.isEmpty())
				throw new StopException("樣版代碼不可以為空白");	
			String sqlCntTmpl = " SELECT COUNT(*) FROM qstpms WHERE qstmp_id = ?";
			if (dbu.selectIntList(sqlCntTmpl, tmplId) == 0){
				throw new StopException("樣版代碼 <" + tmplId + "> 不存在");
			}
			String fileName = req.get("fileName");
			String fileClass = req.get("fileClass");
			String showOrderStr = req.get("showOrder");
			int showOrder;
			try {
				showOrder = Integer.parseInt(showOrderStr);
			}
			catch (NumberFormatException e) {
				throw new StopException("顯示順序不是正確的數字");
			}
			String remark = DbUtil.emptyToNull((String) req.get("remark"));
			
			String sqlQryFile =
				  " SELECT file_class, show_order, remark\n"
				+ "   FROM qstpfl\n"
				+ "  WHERE qstmp_id = ?\n"
				+ "    AND file_name = ?\n";
			Map<String, Object> rowFile = dbu.selectMapRowList(sqlQryFile, tmplId, fileName);
			if (rowFile == null)
				throw new StopException("檔案名稱 <" + fileName + "> 不存在");
			String fileClassOrg = (String) rowFile.get("file_class");
			int showOrderOrg = (Integer) rowFile.get("show_order");
			String remarkOrg = (String) rowFile.get("remark");

			String sqlUpdFile =
				  " UPDATE qstpfl SET\n"
				+ "        file_class = ?\n"
				+ "      , remark = ?\n"
				+ "  WHERE qstmp_id = ?\n"
				+ "    AND file_name = ?\n";
			dbu.executeList(sqlUpdFile, fileClass, remark, tmplId, fileName);
			fileReorder(dbu, tmplId, fileName, showOrder);
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "modFile");
            operLog.add("qstmpId", tmplId);
            operLog.add("fileName", fileName);
            operLog.add("fileClass", fileClassOrg, fileClass);
            operLog.add("showOrder", showOrderOrg, showOrder);
            operLog.add("remark", remarkOrg, remark);
            operLog.write();

            res.put("fileList", buildFileListData(dbu, tmplId));
            res.put("success", true);
			res.put("status", "修改檔案完成");
			
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
	
	@RequestMapping(value = "/QsMntTmpl_delFile", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> delFile(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			
			String tmplId = req.get("tmplId");
			if (tmplId.isEmpty())
				throw new StopException("樣版代碼不可以為空白");
			String fileName = req.get("fileName");
			if (fileName.isEmpty())
				throw new StopException("檔案名稱不可以為空白");
			String sqlCntFile = " SELECT COUNT(*) FROM qstpfl WHERE qstmp_id = ? AND file_name = ?";
			if (dbu.selectIntList(sqlCntFile, tmplId, fileName) == 0)
				throw new StopException("指定檔案不存在");
			
			// 將檔案刪除
			File tmplPath = new File(QsSvc.getTmplFilePath(dbu, tmplId));
			new File(tmplPath.getAbsolutePath(), fileName).delete();

			String sqlDelFile =
				  " DELETE FROM qstpfl\n"
				+ "  WHERE qstmp_id = ?\n"
				+ "    AND file_name = ?\n";
			dbu.executeList(sqlDelFile, tmplId, fileName);
			fileReorder(dbu, tmplId, "", 0);
			dbu.doCommit();
			            
            OperLog operLog = new OperLog(c_progId, "delFile");
            operLog.add("qstmpId", tmplId);
            operLog.add("fileName", fileName);
			
            res.put("fileList", buildFileListData(dbu, tmplId));
            res.put("success", true);
			res.put("status", "刪除檔案完成");
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
	
	@RequestMapping(value = "/QsMntTmpl_download", method = RequestMethod.GET)
	public void download(@RequestParam Map<String, String> req, HttpServletResponse res) {
		String status = "";
		boolean success = false;

		String tmplId = "";
		String fileName = "";
		String open = "";
		String tmplPath = "";
		DbUtil dbu = new DbUtil();
		try {
			tmplId = req.get("tmplId");
			if (tmplId.isEmpty())
				throw new StopException("樣版代碼不可以為空白");
			fileName = req.get("fileName");
			if (fileName.isEmpty())
				throw new StopException("檔案名稱不可以為空白");
			open = req.get("open");
			
			String sqlQryFile =
				  " SELECT file_desc, file_type, file_size\n"
				+ "   FROM qstpfl a\n"
				+ "  WHERE qstmp_id = ?\n"
				+ "    AND file_name = ?\n";
			Map<String, Object> rowFile = dbu.selectMapRowList(sqlQryFile, tmplId, fileName);
			if (rowFile.isEmpty())
				throw new StopException("指定檔案紀錄不存在");
			tmplPath = QsSvc.getTmplFilePath(dbu, tmplId);
			File tmplFile = new File(tmplPath, fileName);
			if (!tmplFile.exists())
				throw new StopException("指定檔案不存在");

			res.setContentType((String) rowFile.get("file_type")); 
			res.setContentLength((Integer) rowFile.get("file_size"));
			if ("Y".equals(open))
				res.setHeader("Content-Disposition", "inline");
			else
				res.setHeader("Content-Disposition", "attachment; filename=" + (String) rowFile.get("file_desc"));
			InputStream is = new FileInputStream(tmplFile);
			FileCopyUtils.copy(is, res.getOutputStream());
			
			success = true;
		}
		catch (StopException e) {
			status = e.getMessage();
		}
		catch (SQLException e) {
			status = DbUtil.exceptionTranslation(e);
		}
		catch (Exception e) {
			status = e.toString();
		}
		dbu.relDbConn();
		
		if (!success) {
			try {
				UserData ud = UserData.getUserData();
				StringBuffer out = new StringBuffer();
				out.append("下載檔案失敗，原因: " + status + "\n");
				out.append("人員代碼: " + ud.getUserId() + "\n");
				out.append("人員姓名: " + ud.getUserName() + "\n");
				out.append("樣版代碼: " + tmplId + "\n");
				out.append("樣版目錄: " + tmplPath + "\n");
				out.append("檔案名稱: " + fileName + "\n");
				byte outdata[] = out.toString().getBytes("utf-8");
				res.setContentType("text/plain; charset=utf-8");
				res.setContentLength(outdata.length);
				res.getOutputStream().write(outdata);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
