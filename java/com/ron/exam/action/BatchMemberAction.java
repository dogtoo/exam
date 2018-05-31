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
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.DateUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import com.ron.exam.util.StdCalendar;
import com.ron.exam.util.StopException;
import com.ron.exam.util.XlsUtil;
import com.ron.exam.util.DbUtil;
import com.ron.exam.util.ExceptionUtil;
import com.ron.exam.util.OperLog;
import com.ron.exam.service.ParamSvc;
import com.ron.exam.service.ProgData;
import com.ron.exam.service.UserData;

@EnableWebMvc
@Controller
public class BatchMemberAction {

	private static final String c_progId = "BatchMember";
	private static final String c_sessUploadFileKey = "BatchMember_UploadFile";

	@RequestMapping(value = "/BatchMember", method = RequestMethod.GET)
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

			HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
			session.removeAttribute(c_sessUploadFileKey);
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
		return "BatchMember";
	}

	@RequestMapping(value = "/BatchMember_uploadFile", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> uploadFile(@RequestParam Map<String, String> req , @RequestParam("file") MultipartFile uparg) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");

			String uploadPath;
			try {
				Context initCtx = new InitialContext();
				uploadPath = (String) initCtx.lookup("java:/comp/env/conf/upload_path");	
			}
			catch (NamingException e) {
				throw new StopException("無法取得上傳路徑: " + e.toString());
			}
			File uploadPathFl = new File(uploadPath);
			if (!uploadPathFl.exists() || !uploadPathFl.isDirectory())
				throw new StopException("上傳路徑設定不正確");

			// 儲存檔案
			try {
				String contentType = uparg.getContentType();
				res.put("contentType", contentType);
				String clientFileName = uparg.getOriginalFilename();
				res.put("clientFileName", clientFileName);
				File serverUploadFile = File.createTempFile("upload_", "_" + clientFileName, new File(uploadPath));
				String serverFileName = serverUploadFile.getName();
				res.put("serverFileName", serverFileName);
				uparg.transferTo(serverUploadFile);
				res.put("contentLength", serverUploadFile.length());
				HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
				session.setAttribute(c_sessUploadFileKey, serverUploadFile);
			}
			catch (IOException e) {
				throw new StopException("儲存上傳檔案失敗: " + e.toString());
			}

			res.put("success", true);
			res.put("status", "上傳檔案完成");
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
	}

	private static final String c_xlsDataTitle[] = {
		"人員帳號", "人員編號", "姓名", "科系代碼", "電子郵件", "聯絡電話", "通信地址",
		"生效日期", "失效日期", "初始密碼", "選單代碼", "角色代碼列表", "備註"
	};
	
//	private static void buildCodeDesc(DbUtil dbu, HSSFSheet sheet, int rowAt[], int titleAt, String codeKind) throws SQLException {
//		HSSFRow row;
//		HSSFCell cell;
//		row = sheet.createRow(rowAt[0]++);
//		cell = row.createCell(0);
//		cell.setCellValue("欄位 <" + c_xlsDataTitle[titleAt] + "> 填入代碼說明");
//		String sqlQryCode =	" SELECT code_code, code_desc FROM cmcode WHERE code_kind = ? ORDER BY code_order";
//		ResultSet rsCode = dbu.queryList(sqlQryCode, codeKind);
//		while (rsCode.next()) {
//			String code = rsCode.getString("code_code");
//			String desc = rsCode.getString("code_desc");
//			row = sheet.createRow(rowAt[0]++);
//			cell = row.createCell(1);
//			cell.setCellValue(code);
//			cell = row.createCell(2);
//			cell.setCellValue(desc);
//		}
//		rsCode.close();
//	}

	private static void buildParamDesc(DbUtil dbu, HSSFSheet sheet, int rowAt[], int titleAt, String paramClass) throws SQLException {
		HSSFRow row;
		HSSFCell cell;
		row = sheet.createRow(rowAt[0]++);
		cell = row.createCell(0);
		cell.setCellValue("欄位 <" + c_xlsDataTitle[titleAt] + "> 填入代碼說明");
		String sqlQryParam =	" SELECT param_id, curr_value FROM cmparm WHERE param_class = ? AND param_id <> '-' ORDER BY show_order";
		ResultSet rsParam = dbu.queryList(sqlQryParam, paramClass);
		while (rsParam.next()) {
			String id = rsParam.getString("param_id");
			String value = rsParam.getString("curr_value");
			row = sheet.createRow(rowAt[0]++);
			cell = row.createCell(1);
			cell.setCellValue(id);
			cell = row.createCell(2);
			cell.setCellValue(value);
		}
		rsParam.close();
	}

	@RequestMapping(value = "/BatchMember_example", method = RequestMethod.GET)
	public void example(@RequestParam Map<String, String> req, HttpServletResponse res) {
		String status = "";

		DbUtil dbu = new DbUtil();
		try {
			HSSFWorkbook book = new HSSFWorkbook();
			HSSFSheet sheetData = book.createSheet("人員資料");
			HSSFSheet sheetDesc = book.createSheet("欄位說明");
			HSSFRow row;
			HSSFCell cell;
			
			// 產生人員資料頁內容
			row = sheetData.createRow(0);
			for (int i = 0; i < c_xlsDataTitle.length; i++) {
				cell = row.createCell(i);
				cell.setCellValue(c_xlsDataTitle[i]);
			}
			
			// 產生欄位說明頁內容
			String descTitle[] = new String[] { "欄位名稱", "可填入代碼", "代碼說明" };
			row = sheetDesc.createRow(0);
			for (int i = 0; i < descTitle.length; i++) {
				cell = row.createCell(i);
				cell.setCellValue(descTitle[i]);
			}
			int rowAt[] = new int[] { 1 };
			buildParamDesc(dbu, sheetDesc, rowAt, 3, "DEPART");
			row = sheetDesc.createRow(rowAt[0]++);
			cell = row.createCell(0);
			cell.setCellValue("欄位 <" + c_xlsDataTitle[7] + "> 填入格式說明");
			row = sheetDesc.createRow(rowAt[0]++);
			cell = row.createCell(1);
			cell.setCellValue("日期格式 YYYY/MM/DD，如果不指定，請填入半形的 -");
			row = sheetDesc.createRow(rowAt[0]++);
			cell = row.createCell(0);
			cell.setCellValue("欄位 <" + c_xlsDataTitle[8] + "> 填入格式說明");
			row = sheetDesc.createRow(rowAt[0]++);
			cell = row.createCell(1);
			cell.setCellValue("日期格式 YYYY/MM/DD，如果不指定，請填入半形的 -");

			row = sheetDesc.createRow(rowAt[0]++);
			cell = row.createCell(0);
			cell.setCellValue("欄位 <" + c_xlsDataTitle[10] + "> 填入代碼說明");
			String sqlQryMenu = " SELECT menu_id, menu_desc FROM cmmenu WHERE upper_id = '-' ORDER BY show_order";
			ResultSet rsMenu = dbu.queryList(sqlQryMenu);
			while (rsMenu.next()) {
				String id = rsMenu.getString("menu_id");
				String desc = rsMenu.getString("menu_desc");
				row = sheetDesc.createRow(rowAt[0]++);
				cell = row.createCell(1);
				cell.setCellValue(id);
				cell = row.createCell(2);
				cell.setCellValue(desc);
			}
			rsMenu.close();
			
			row = sheetDesc.createRow(rowAt[0]++);
			cell = row.createCell(0);
			cell.setCellValue("欄位 <" + c_xlsDataTitle[11] + "> 填入代碼說明");
			String sqlQryRole = " SELECT role_id, role_name, suspend FROM cmrole ORDER BY role_id";
			ResultSet rsRole = dbu.queryList(sqlQryRole);
			while (rsRole.next()) {
				String id = rsRole.getString("role_id");
				String name = DbUtil.nullToEmpty(rsRole.getString("role_name"));
				String suspend = rsRole.getString("suspend");
				row = sheetDesc.createRow(rowAt[0]++);
				cell = row.createCell(1);
				cell.setCellValue(id);
				if ("Y".equals(suspend))
					name += " [被暫停]";
				cell = row.createCell(2);
				cell.setCellValue(name);
			}
			rsRole.close();
			
			XlsUtil.generateXlsDownload(res, book, "MemberTemplate.xls");;
		}
//		catch (StopException e) {
//			status = e.getMessage();
//		}
		catch (SQLException e) {
			status = DbUtil.exceptionTranslation(e);
		}
		catch (Exception e) {
			status = e.toString();
		}
		dbu.relDbConn();
		
		XlsUtil.downloadError(res, status);
	}

	@RequestMapping(value = "/BatchMember_batch", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> batch(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		HSSFWorkbook book = null;
		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");

			HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
			File file = (File) session.getAttribute(c_sessUploadFileKey);
			session.setAttribute(c_sessUploadFileKey, null);
			if (file == null)
				throw new StopException("請先上傳檔案");
			InputStream is = new FileInputStream(file);
			book = new HSSFWorkbook(is);
			is.close();

			String xferMode = req.get("xferMode");
			boolean xferForce = "Y".equals(req.get("xferForce"));
			boolean delMode = "delMember".equals(xferMode) || "delLogin".equals(xferMode) || "delBoth".equals(xferMode);
			HSSFRow row;
			HSSFCell cell;

			HSSFSheet sheetData = book.getSheetAt(0);
			if (!"人員資料".equals(sheetData.getSheetName()))
				throw new StopException("第一工作表名稱不是 '人員資料'");
			if (sheetData.getFirstRowNum() != 0)
				throw new StopException("第一列不是標題列");
			row = sheetData.getRow(0);
			for (int i = 0; i < c_xlsDataTitle.length; i++) {
				cell = row.getCell(i);
				if (cell == null)
					throw new StopException("第 " + (i + 1) + " 欄位標題為空白");
				if (cell.getCellType() != HSSFCell.CELL_TYPE_STRING)
					throw new StopException("第 " + (i + 1) + " 欄位標題格式不正確");
				if (!c_xlsDataTitle[i].equals(cell.getStringCellValue()))
					throw new StopException("第 " + (i + 1) + " 欄位標題文字不正確");
			}
			
			StringBuffer msg = new StringBuffer();
			Map<String, String> departMap = ParamSvc.buildStringMapByClass(dbu, "DEPART");
			String sqlQryMenu = " SELECT menu_id, menu_desc FROM cmmenu WHERE upper_id = '-'";
			Map<String, String> menuMap = dbu.selectKeyStringList(sqlQryMenu, "menu_id", "menu_desc");
			String sqlQryRole = " SELECT role_id, role_name FROM cmrole";
			Map<String, String> roleMap = dbu.selectKeyStringList(sqlQryRole, "role_id", "role_name");
			List<Object> paramMember = new ArrayList<Object>();
			List<Object> paramUser = new ArrayList<Object>();
			StdCalendar date = new StdCalendar();
			List<String> roles = new ArrayList<String>();
			for (int rowAt = 2; rowAt - 1 <= sheetData.getLastRowNum(); rowAt++) {
				row = sheetData.getRow(rowAt - 1);
				if (row == null) {
					msg.append("第 " + rowAt + " 列為空白列\n");
					continue;
				}
				
				String userId = null;
				paramMember.clear();
				paramUser.clear();
				roles.clear();
				int orgMsgLen = msg.length();
				for (int colAt = 0; colAt < c_xlsDataTitle.length; colAt++) {
					String value = "";
					cell = row.getCell(colAt);
					if (cell != null) {
						int cellType = cell.getCellType();
						if (cellType == HSSFCell.CELL_TYPE_STRING)
							value = cell.getStringCellValue();
						else if (DateUtil.isCellDateFormatted(cell)) {
							StdCalendar d = new StdCalendar();
							d.setTimeInMillis(cell.getDateCellValue().getTime());
							value = d.toDateString();
						}
						else if (cellType == HSSFCell.CELL_TYPE_NUMERIC)
							value = Integer.toString((int) cell.getNumericCellValue());
						else {
							msg.append("第 " + rowAt + " 列<" + c_xlsDataTitle[colAt] + ">欄內容格式不是字串\n");
							continue;
						}
					}
					byte valueU8[] = value.getBytes("UTF-8");
					// 檢查並準備資料
					switch (colAt) {
						case 0:
							if (value.isEmpty())
								msg.append("第 " + rowAt + " 列<" + c_xlsDataTitle[colAt] + ">欄內容為空字串\n");
							if (value.length() > 20)
								msg.append("第 " + rowAt + " 列<" + c_xlsDataTitle[colAt] + ">欄內容長度超過\n");
							if (value.length() != valueU8.length)
								msg.append("第 " + rowAt + " 列<" + c_xlsDataTitle[colAt] + ">欄內容含不正確字元\n");
							userId = value.toUpperCase();
							paramMember.add(userId);
							paramUser.add(userId);
							break;
						case 1:
							if (delMode || "addLogin".equals(xferMode))
								break;
							if (valueU8.length > 30)
								msg.append("第 " + rowAt + " 列<" + c_xlsDataTitle[colAt] + ">欄內容長度超過\n");
							paramMember.add(value);
							break;
						case 2:
							if (delMode)
								break;
							if (valueU8.length > 50)
								msg.append("第 " + rowAt + " 列<" + c_xlsDataTitle[colAt] + ">欄內容長度超過\n");
							paramMember.add(value);
							paramUser.add(value);
							break;
						case 3:
							if (delMode || "addLogin".equals(xferMode))
								break;
							if (!departMap.containsKey(value))
								msg.append("第 " + rowAt + " 列<" + c_xlsDataTitle[colAt] + ">欄內容為無效代碼\n");
							paramMember.add(value);
							break;
						case 4:
							if (delMode || "addLogin".equals(xferMode))
								break;
							if (valueU8.length > 50)
								msg.append("第 " + rowAt + " 列<" + c_xlsDataTitle[colAt] + ">欄內容長度超過\n");
							paramMember.add(value);
							break;
						case 5:
							if (delMode || "addLogin".equals(xferMode))
								break;
							if (valueU8.length > 20)
								msg.append("第 " + rowAt + " 列<" + c_xlsDataTitle[colAt] + ">欄內容長度超過\n");
							paramMember.add(value);
							break;
						case 6:
							if (delMode || "addLogin".equals(xferMode))
								break;
							if (valueU8.length > 100)
								msg.append("第 " + rowAt + " 列<" + c_xlsDataTitle[colAt] + ">欄內容長度超過\n");
							paramMember.add(value);
							break;
						case 7:
							if (delMode)
								break;
							if (!"-".equals(value) && !StdCalendar.isDateFormat(value))
								msg.append("第 " + rowAt + " 列<" + c_xlsDataTitle[colAt] + ">欄內容非不指定或正確日期格式\n");
							if ("-".equals(value)) {
								paramMember.add("00000000");
								paramUser.add("00000000");
							}
							else {
								date.fromString(value);
								paramMember.add(date.toDbDateString());
								paramUser.add(date.toDbDateString());
							}
							break;
						case 8:
							if (delMode)
								break;
							if (!"-".equals(value) && !StdCalendar.isDateFormat(value))
								msg.append("第 " + rowAt + " 列<" + c_xlsDataTitle[colAt] + ">欄內容非不指定或正確日期格式\n");
							if ("-".equals(value)) {
								paramMember.add("99999999");
								paramUser.add("99999999");
							}
							else {
								date.fromString(value);
								paramMember.add(date.toDbDateString());
								paramUser.add(date.toDbDateString());
							}
							break;
						case 9:
							if (delMode || "addMember".equals(xferMode))
								break;
							if (value.isEmpty())
								msg.append("第 " + rowAt + " 列<" + c_xlsDataTitle[colAt] + ">欄內容為空字串\n");
							if (valueU8.length > 50)
								msg.append("第 " + rowAt + " 列<" + c_xlsDataTitle[colAt] + ">欄內容長度超過\n");
							paramUser.add(value);
							break;
						case 10:
							if (delMode || "addMember".equals(xferMode))
								break;
							if (!menuMap.containsKey(value))
								msg.append("第 " + rowAt + " 列<" + c_xlsDataTitle[colAt] + ">欄內容為無效代碼\n");
							paramUser.add(value);
							break;
						case 11:
							if (delMode || "addMember".equals(xferMode))
								break;
							if (!value.isEmpty()) {
								String roleIds[] = value.split("\\s*,\\s*");
								for (int i = 0; i < roleIds.length; i++) {
									if (roleMap.containsKey(roleIds[i]))
										roles.add(roleIds[i]);
									else
										msg.append("第 " + rowAt + " 列<" + c_xlsDataTitle[colAt] + ">欄內容中含無效角色代碼 '" + roleIds[i] + "'\n");
								}
							}
							break;
						case 12:
							if (delMode || "addMember".equals(xferMode))
								break;
							if (valueU8.length > 500)
								msg.append("第 " + rowAt + " 列<" + c_xlsDataTitle[colAt] + ">欄內容長度超過\n");
							paramUser.add(DbUtil.emptyToNull(value));
							break;
					}
				}
				
				// 有錯誤訊息就不處理此筆
				if (msg.length() != orgMsgLen)
					continue;
				// 實際動作
				if ("addMember".equals(xferMode) || "addBoth".equals(xferMode)) {
					String sqlCntMember =
						  " SELECT COUNT(*) FROM mbdetl WHERE user_id = ?";
					if (dbu.selectIntList(sqlCntMember, userId) > 0) {
						if (xferForce) {
							String sqlQryMember =
								  " SELECT user_name, user_no, user_name, depart_id, class_id, group_id,\n"
								+ "        semester_id, email, mobile_no, address, begin_date, end_date\n"
								+ "   FROM mbdetl\n"
								+ "  WHERE user_id = ?\n";
							ResultSet rsMember = dbu.queryList(sqlQryMember, userId);
							rsMember.next();
							String userNameOrg = rsMember.getString("user_name");
							String userNoOrg = rsMember.getString("user_no");
							String departIdOrg = rsMember.getString("depart_id");
							String emailOrg = rsMember.getString("email");
							String mobileNoOrg = rsMember.getString("mobile_no");
							String addressOrg = rsMember.getString("address");
							String beginDateOrg = rsMember.getString("begin_date");
							String endDateOrg = rsMember.getString("end_date");
							rsMember.close();
							String sqlUpdMember =
								  " UPDATE mbdetl SET\n"
								+ "        user_no = ?\n"
								+ "      , user_name = ?\n"
								+ "      , depart_id = ?\n"
								+ "      , email = ?\n"
								+ "      , mobile_no = ?\n"
								+ "      , address = ?\n"
								+ "      , begin_date = ?\n"
								+ "      , end_date = ?\n"
								+ "  WHERE user_id = ?\n";
							paramMember.add(paramMember.remove(0));
							dbu.executeArray(sqlUpdMember, paramMember.toArray());

							OperLog operLog = new OperLog(c_progId, "xferModMember");
							operLog.add("userId", userId);
				            operLog.add("userNo", userNoOrg, paramMember.get(1));
				            operLog.add("userName", userNameOrg, paramMember.get(2));
				            operLog.add("departId", departIdOrg, paramMember.get(3));
				            operLog.add("email", emailOrg, paramMember.get(7));
				            operLog.add("mobileNo", mobileNoOrg, paramMember.get(8));
				            operLog.add("address", addressOrg, paramMember.get(9));
				            operLog.add("beginDate", beginDateOrg, paramMember.get(10));
				            operLog.add("endDate", endDateOrg, paramMember.get(11));
				            operLog.write();;
						}
						else
							msg.append("第 " + rowAt + " 列人員資料檔已存在帳號 '" + userId + "' 資料\n");
					}
					else {
						String sqlInsMember =
							  " INSERT INTO mbdetl(user_id, user_no, user_name, depart_id,\n"
							+ "             email, mobile_no, address, begin_date, end_date)\n"
							+ " VALUES (?, ?, ?, ?,  ?, ?, ?, ?, ?)\n";
						try {
							dbu.executeArray(sqlInsMember, paramMember.toArray());

							OperLog operLog = new OperLog(c_progId, "xferAddMember");
				            operLog.add("userId", paramMember.get(0));
				            operLog.add("userNo", paramMember.get(1));
				            operLog.add("userName", paramMember.get(2));
				            operLog.add("departId", paramMember.get(3));
				            operLog.add("email", paramMember.get(4));
				            operLog.add("mobileNo", paramMember.get(5));
				            operLog.add("address", paramMember.get(6));
				            operLog.add("beginDate", paramMember.get(7));
				            operLog.add("endDate", paramMember.get(8));
				            operLog.write();
						}
						catch (SQLException e) {
							msg.append("第 " + rowAt + " 列新增人員資料檔時錯誤：" + e.toString() + "\n");
						}
					}
				}
				if ("addLogin".equals(xferMode) || "addBoth".equals(xferMode)) {
					String sqlCntUser =
						" SELECT COUNT(*) FROM cmuser WHERE user_id = ?";
					if (dbu.selectIntList(sqlCntUser, userId) > 0) {
						if (xferForce) {
							String sqlQryUser =
								  " SELECT user_name, init_pass, menu_id, begin_date, end_date, remark\n"
								+ "   FROM cmuser\n"
								+ "  WHERE user_id = ?\n";
							ResultSet rsUser = dbu.queryList(sqlQryUser, userId);
							rsUser.next();
							String userNameOrg = rsUser.getString("user_name");
							String initPassOrg = rsUser.getString("init_pass");
							String menuIdOrg = rsUser.getString("menu_id");
							String beginDateOrg = rsUser.getString("begin_date");
							String endDateOrg = rsUser.getString("end_date");
							String remarkOrg = rsUser.getString("remark");
							rsUser.close();
							String sqlUpdUser =
								  " UPDATE cmuser SET\n"
								+ "        user_name = ?\n"
								+ "      , begin_date = ?\n"
								+ "      , end_date = ?\n"
								+ "      , init_pass = ?\n"
								+ "      , menu_id = ?\n"
								+ "      , remark = ?\n"
								+ "  WHERE user_id = ?\n";
							paramUser.add(paramUser.remove(0));
							dbu.executeArray(sqlUpdUser, paramUser.toArray());

							String sqlQryUserRole =
								" SELECT role_id FROM cmusrr WHERE user_id = ? ORDER BY role_id";
							ResultSet rsRole = dbu.queryList(sqlQryUserRole, userId);
							StringBuffer userRoleOrg = new StringBuffer();
							while (rsRole.next()) {
								if (userRoleOrg.length() > 0)
									userRoleOrg.append(',');
								userRoleOrg.append(rsRole.getString("role_id"));
							}
							rsRole.close();
							String sqlDelUserRole =
								" DELETE FROM cmusrr WHERE user_id = ?";
							dbu.executeList(sqlDelUserRole, userId);
							for (int i = 0; i < roles.size(); i++) {
								String sqlInsUserRole =
									" INSERT INTO cmusrr(user_id, role_id) VALUES(?, ?)";
								dbu.executeList(sqlInsUserRole, userId, roles.get(i));
							}
							rsRole = dbu.queryList(sqlQryUserRole, userId);
							StringBuffer userRole = new StringBuffer();
							while (rsRole.next()) {
								if (userRole.length() > 0)
									userRole.append(',');
								userRole.append(rsRole.getString("role_id"));
							}
							rsRole.close();

							OperLog operLog = new OperLog(c_progId, "xferModUser");
				            operLog.add("userId", userId);
				            operLog.add("userName", userNameOrg, paramUser.get(0));
				            operLog.add("initPass", initPassOrg, paramUser.get(3));
				            operLog.add("menuId", menuIdOrg, paramUser.get(4));
				            operLog.add("beginDate", beginDateOrg, paramUser.get(1));
				            operLog.add("endDate", endDateOrg, paramUser.get(2));
				            operLog.add("remark", remarkOrg, paramUser.get(5));
				            operLog.add("roleList", userRoleOrg.toString(), userRole.toString());
				            operLog.write();
						}
						else
							msg.append("第 " + rowAt + " 列人員資料檔已存在帳號 '" + userId + "' 資料\n");
					}
					else {
						String sqlInsUser =
							  " INSERT INTO cmuser(user_id, user_name, begin_date, end_date, init_pass, fail_pass, menu_id, remark)\n"
							+ " VALUES (?, ?, ?, ?, ?, 0, ?, ?)\n";
						try {
							dbu.executeArray(sqlInsUser, paramUser.toArray());
							for (int i = 0; i < roles.size(); i++) {
								String sqlInsUserRole =
									" INSERT INTO cmusrr(user_id, role_id) VALUES(?, ?)";
								dbu.executeList(sqlInsUserRole, userId, roles.get(i));
							}
							String sqlQryUserRole =
								" SELECT role_id FROM cmusrr WHERE user_id = ? ORDER BY role_id";
							ResultSet rsRole = dbu.queryList(sqlQryUserRole, userId);
							StringBuffer userRole = new StringBuffer();
							while (rsRole.next()) {
								if (userRole.length() > 0)
									userRole.append(',');
								userRole.append(rsRole.getString("role_id"));
							}
							rsRole.close();

							OperLog operLog = new OperLog(c_progId, "xferAddUser");
				            operLog.add("userId", paramMember.get(0));
				            operLog.add("userName", paramMember.get(1));
				            operLog.add("initPass", paramUser.get(4));
				            operLog.add("menuId", paramUser.get(5));
				            operLog.add("beginDate", paramMember.get(2));
				            operLog.add("endDate", paramMember.get(3));
				            operLog.add("remark", paramUser.get(6));
				            operLog.add("roleList", userRole.toString());
				            operLog.write();
						}
						catch (SQLException e) {
							msg.append("第 " + rowAt + " 列新增人員資料檔時錯誤：" + e.toString() + "\n");
						}
					}
				}
				if ("delMember".equals(xferMode) || "delBoth".equals(xferMode)) {
					try {
						String relaMsg = MntMemberAction.chkUserIdInUse(dbu, userId);
						if (relaMsg != null)
							throw new StopException("第 " + rowAt + " 列" + relaMsg + "\n");
						String sqlDelMember = " DELETE FROM mbdetl WHERE user_id = ?";
						int r = dbu.executeList(sqlDelMember, userId);
						if (r == 0)
							msg.append("第 " + rowAt + " 列帳號不存在於人員資料檔\n");
						else {
							OperLog operLog = new OperLog(c_progId, "xferDelMember");
				            operLog.add("userId", userId);
				            operLog.write();
						}
					}
					catch (StopException e) {
						msg.append(e.getMessage());
					}
					catch (SQLException e) {
						msg.append("第 " + rowAt + " 列刪除人員資料檔時錯誤：" + e.toString() + "\n");
					}
				}
				if ("delLogin".equals(xferMode) || "delBoth".equals(xferMode)) {
					try {
						String sqlDelUser = " DELETE FROM cmuser WHERE user_id = ?";
						int r = dbu.executeList(sqlDelUser, userId);
						if (r == 0)
							msg.append("第 " + rowAt + " 列帳號不存在於登入資料檔\n");
						else {
							OperLog operLog = new OperLog(c_progId, "xferDelUser");
				            operLog.add("userId", userId);
				            operLog.write();
						}
					}
					catch (SQLException e) {
						msg.append("第 " + rowAt + " 列刪除登入資料檔時錯誤：" + e.toString() + "\n");
					}
				}
				if (msg.length() == orgMsgLen)
					dbu.doCommit();
				else
					dbu.doRollback();
			}
			if (msg.length() == 0)
				msg.append("資料內容正確無誤");
			res.put("xferResult", msg.toString());
			
            res.put("success", true);
			res.put("status", "批次處理人員完成");
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
		finally {
			try {
				if (book != null)
					book.close();
			}
			catch (Exception e) {
				res.put("status", ExceptionUtil.procExceptionMsg(e));
			}
		}
		dbu.relDbConn();
		
		return res;
	}
}
