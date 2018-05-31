package com.ron.exam.service;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.FileUtils;
import com.ron.exam.util.DbUtil;
import com.ron.exam.util.StdCalendar;
import com.ron.exam.util.StopException;

public class QsSvc {

	public static final String c_qsMastFlagNew = "N";
	public static final String c_qsMastFlagEdit = "E";
	public static final String c_qsMastFlagValid = "V";
	public static final String c_qsMastFlagPass = "P";
	public static final String c_qsMastFlagOnline = "O";
	public static final String c_qsMastFlagAbandon = "A";
	
	public static final String c_qsRoleAdmin = "A";
	public static final String c_qsRoleEdit = "E";
	public static final String c_qsRoleValid = "V";
	public static final String c_qsRoleNote = "N";
	
	public static final String c_qsValidStatWait = "W";
	public static final String c_qsValidStatPass = "P";
	public static final String c_qsValidStatReject = "R";
	
	public static final int c_qsIdMaxLen = 20;
	
	public static String getTmplBasePath() throws NamingException {
		Context initCtx = new InitialContext();
		Context envCtx = (Context) initCtx.lookup("java:/comp/env");
		String qstmpPath = (String) envCtx.lookup("conf/question_template_file_path");
		return qstmpPath;
	}
	
	public static String getTmplFilePath(DbUtil dbu, String qstmpId) throws StopException, SQLException {
		String basePathStr;
		try {
			basePathStr = getTmplBasePath();
		}
		catch (NamingException e) {
			throw new StopException("無法取得教案樣版檔案目錄: " + e.toString());
		}
		String sqlQryPath = " SELECT file_path FROM qstpms WHERE qstmp_id = ?";
		String tmplPathStr = dbu.selectStringList(sqlQryPath, qstmpId);
		if (tmplPathStr == null)
			throw new StopException("教案樣版代碼 <" + qstmpId + "> 不存在");
		File tmplPath = new File(basePathStr, tmplPathStr);
		return tmplPath.getAbsolutePath();
	}
	
	public static String getQsBasePath() throws StopException {
		try {
			Context initCtx = new InitialContext();
			Context envCtx = (Context) initCtx.lookup("java:/comp/env");
			String qstmpPath = (String) envCtx.lookup("conf/question_file_path");
			return qstmpPath;
		}
		catch (NamingException e) {
			throw new StopException("無法在系統設定中取得教案根目錄 conf/question_file_path: " + e.toString());
		}
	}
	
	public static String getQsFilePath(DbUtil dbu, String qsId) throws StopException, SQLException {
		String sqlQryPath = " SELECT file_path FROM qsmstr WHERE qs_id = ?";
		String qsFilePath = dbu.selectStringList(sqlQryPath, qsId);
		if (qsFilePath == null)
			throw new StopException("教案代碼 <" + qsId + "> 不存在");
		return new File(getQsBasePath(), qsFilePath).getAbsolutePath();
	}
	
	/**
	 * 取得教案某一版本檔案所在的目錄
	 * @param dbu
	 * @param qsId
	 * @param verSeq == 0 表示查詢目前版本
	 * @return
	 * @throws StopException
	 * @throws SQLException
	 */
	public static String getQsVersionPath(DbUtil dbu, String qsId, int verSeq) throws StopException, SQLException {
		String qsFilePath = getQsFilePath(dbu, qsId);
		if (verSeq == 0)
			verSeq = getQsVersion(dbu, qsId);
		String sqlQryPath = " SELECT file_path FROM qscnvr WHERE qs_id = ? AND ver_seq = ?";
		String verFilePath = dbu.selectStringList(sqlQryPath, qsId, verSeq);
		if (verFilePath == null)
			throw new StopException("教案代碼 <" + qsId + "> 版本 <" + verSeq + "> 資料不存在");
		return new File(qsFilePath, verFilePath).getAbsolutePath();
	}
	
	/**
	 * 取得某一教案目前的版本
	 * @param dbu
	 * @param qsId
	 * @return
	 * @throws SQLException
	 */
	public static int getQsVersion(DbUtil dbu, String qsId) throws SQLException {
		String sqlQryVer = " SELECT curr_version FROM qsmstr WHERE qs_id = ?";
		return dbu.selectIntList(sqlQryVer, qsId);
	}
	
	/**
	 * 取得某一教案目前的狀態
	 * @param dbu
	 * @param qsId
	 * @return
	 * @throws SQLException
	 */
	public static String getQsStat(DbUtil dbu, String qsId) throws SQLException {
		String sqlQryVer = " SELECT stat_flag FROM qsmstr WHERE qs_id = ?";
		return dbu.selectStringList(sqlQryVer, qsId);
	}

	/**
	 * 依給定的 userCond，查詢所有符合條件的人員清單
	 * @param dbu
	 * @param res
	 * @param userCond
	 * @throws SQLException
	 */
	public static List<Map<String, Object>> queryUserList(DbUtil dbu, String userCond) throws SQLException {
		String sqlQryUser =
			  " SELECT user_id \"value\", user_name \"text\"\n"
			+ "   FROM mbdetl\n"
			+ "  WHERE user_id LIKE '%' || ? || '%'\n"
			+ "     OR user_name LIKE '%' || ? || '%'\n"
			+ "  ORDER BY user_id\n"
			+ "  LIMIT 20\n";
		return dbu.selectMapAllList(sqlQryUser, userCond.toUpperCase(), userCond);
	}
	
	/**
	 * 多重檢查教案狀態, 基本檢查教案是否存在, 檢查錯誤時以 StopException 回覆
	 * @param dbu
	 * @param qsId
	 * @param roleId 檢查目前登入人員是否為指定角色; 如此欄位 null 則不檢查, 可以包含多個字元
	 * @param statFlag 檢查教案是否為指定狀態; 如此欄位 null 則不檢查, 可以包含多個字元
	 * @throws SQLException
	 */
	public static void multiCheckQs(DbUtil dbu, String qsId, String roleId, String statFlag) throws StopException, SQLException {
		String sqlQryQs =
			  " SELECT stat_flag FROM qsmstr WHERE qs_id = ?";
		String qsFlag = dbu.selectStringList(sqlQryQs, qsId);
		if (qsFlag == null)
			throw new StopException("教案代碼 <" + qsId + "> 不存在");
		if (statFlag != null && statFlag.indexOf(qsFlag) < 0) {
			StringBuffer statDesc = new StringBuffer();
			for (int i = 0; i < statFlag.length(); i++) {
				if (i > 0)
					statDesc.append("、");
				statDesc.append(CodeSvc.queryCodeDesc(dbu, CodeSvc.c_kindQsMastFlag, statFlag.substring(i, i+ 1)));
			}
			throw new StopException("教案代碼 <" + qsId + "> 不是處於" + statDesc.toString() + "狀態");
		}
		if (roleId != null) {
			UserData ud = UserData.getUserData();
			String today = new StdCalendar().toDbDateString();
			List<String> roleIdList = new ArrayList<String>();
			for (int i = 0; i < roleId.length(); i++)
				roleIdList.add(roleId.substring(i, i + 1));
			StringBuffer sqlCntUser = new StringBuffer(
				  " SELECT COUNT(*) FROM qsuser WHERE qs_id = ? AND user_id = ? AND end_date > ? AND role_flag IN ");
			List<Object> params = new ArrayList<Object>();
			params.add(qsId);
			params.add(ud.getUserId());
			params.add(today);
			DbUtil.buildInSqlParam(sqlCntUser, params, roleIdList);
			if (dbu.selectIntArray(sqlCntUser.toString(), params.toArray()) == 0) {
				StringBuffer roleDesc = new StringBuffer();
				for (int i = 0; i < roleIdList.size(); i++) {
					if (i > 0)
						roleDesc.append("、");
					roleDesc.append(CodeSvc.queryCodeDesc(dbu, CodeSvc.c_kindQsUserRole, roleIdList.get(i)));
				}
				throw new StopException("您不是教案代碼 <" + qsId + "> 的" + roleDesc.toString() + "人員");
			}
		}
	}

	/**
	 * 從樣版創建第一版教案，並進入教案編輯模式
	 * @param req
	 * @param dbu
	 * @param qsId
	 * @throws StopException
	 * @throws SQLException
	 * @throws IOException
	 */
	public static void createQs(HttpServletRequest req, DbUtil dbu, String qsId) throws StopException, SQLException, IOException {
		String userId = UserData.getUserData().getUserId();
		String today = new StdCalendar().toDbDateString();
		// 建立教案目錄與修改教案記錄
		File qsPath = File.createTempFile("qs_", "", new File(getQsBasePath()));
		if (qsPath == null)
			throw new StopException("無法建立教案目錄");
		qsPath.delete();
		if (!qsPath.mkdir())
			throw new StopException("無法建立教案目錄");
		String sqlUpdMstr =
			  " UPDATE qsmstr SET\n"
			+ "        stat_flag = 'E'\n"
			+ "      , curr_version = 1\n"
			+ "      , file_path = ?\n"
			+ "  WHERE qs_id = ?\n";
		dbu.executeList(sqlUpdMstr, qsPath.getName(), qsId);

		// 建立版本目錄與記錄
		File verPath = File.createTempFile("ver1_", "", qsPath);
		verPath.delete();
		if (!verPath.mkdir())
			throw new StopException("無法建立教案版本目錄");
		String sqlInsVer =
			  " INSERT INTO qscnvr(qs_id, ver_seq, file_path, cr_date, cr_user_id, cnfrm_flag)\n"
			+ " VALUES(?, 1, ?, ?, ?, 'N')";
		dbu.executeList(sqlInsVer, qsId, verPath.getName(), today, userId);

		// 複製樣版檔案
		String sqlQryTmpid = " SELECT qstmp_id FROM qsmstr WHERE qs_id = ?";
		String qstmpId = dbu.selectStringList(sqlQryTmpid, qsId);
		if (qstmpId != null) {
			File tmplPath = new File(getTmplFilePath(dbu, qstmpId));
			String sqlQryTpfl =
				  " SELECT file_name, file_class, file_desc, file_size, file_type, show_order, remark\n"
				+ "   FROM qstpfl\n"
				+ "  WHERE qstmp_id = ?\n"
				+ "  ORDER BY show_order\n";
			String sqlInsFile =
				  " INSERT INTO qsfile(qs_id, ver_seq, file_name, file_class, file_desc, file_size, file_type,\n"
				+ "				show_order, cr_date, cr_user_id, remark)\n"
				+ " VALUES(?, 1, ?, ?, ?, ?, ?,  ?, ?, ?, ?)\n";
			ResultSet rsTpfl = dbu.queryList(sqlQryTpfl, qstmpId);
			while (rsTpfl.next()) {
				String fileName = rsTpfl.getString("file_name");
				String fileClass = rsTpfl.getString("file_class");
				String fileDesc = rsTpfl.getString("file_desc");
				int fileSize = rsTpfl.getInt("file_size");
				String fileType = rsTpfl.getString("file_type");
				int showOrder = rsTpfl.getInt("show_order");
				String fileRemark = rsTpfl.getString("remark");
				FileUtils.copyFile(new File(tmplPath, fileName), new File(verPath, fileName));
				dbu.executeList(sqlInsFile, qsId, fileName, fileClass, fileDesc, fileSize, fileType,
					showOrder, today, userId, fileRemark);
			}
			rsTpfl.close();
		}
		
		// 通知編輯與審核人員
		MailNotify mail = new MailNotify(req);
		mail.sendNewQsNotify(dbu, qsId);
	}

	/**
	 * 將教案置於審核模式，教案原本需為編輯模式
	 * @param HttpServletRequest
	 * @param dbu
	 * @param qsId
	 * @throws SQLException
	 */
	public static void validateQs(HttpServletRequest req, DbUtil dbu, String qsId) throws StopException, SQLException {
		UserData ud = UserData.getUserData();
		String today = new StdCalendar().toDbDateString();
		String sqlQryQs =
			  " SELECT curr_version, stat_flag FROM qsmstr WHERE qs_id = ?";
		Map<String, Object> rowQs = dbu.selectMapRowList(sqlQryQs, qsId);
		if (rowQs == null)
			throw new StopException("教案代碼 <" + qsId + "> 不存在");
		int currVer = (Integer) rowQs.get("curr_version");
		String statFlag = (String) rowQs.get("stat_flag");
		if (!c_qsMastFlagEdit.equals(statFlag))
			throw new StopException("教案目前不是編輯模式");
		String sqlCntMissFlcls =
			  " SELECT COUNT(*) FROM qsclss a\n"
			+ "  WHERE a.qs_id = ?\n"
			+ "    AND class_type = 'F'\n"
			+ "    AND class_id NOT IN (SELECT file_class\n"
			+ "                           FROM qsfile b, qsmstr c\n"
			+ "                          WHERE b.qs_id = a.qs_id\n"
			+ "                            AND c.qs_id = a.qs_id\n"
			+ "                            AND b.ver_seq = c.curr_version)\n";
		if (dbu.selectIntList(sqlCntMissFlcls, qsId) > 0)
			throw new StopException("教案尚有必要檔案類別沒有指定檔案");
		String sqlUpdVer =
			  " UPDATE qscnvr a SET\n"
			+ "        cnfrm_flag = 'Y'\n"
			+ "      , cnfrm_user_id = ?\n"
			+ "      , cnfrm_date = ?\n"
			+ "  WHERE qs_id = ?\n"
			+ "    AND ver_seq = ?\n";
		dbu.executeList(sqlUpdVer, ud.getUserId(), today, qsId, currVer);
		String sqlUpdMstr =
			  " UPDATE qsmstr SET\n"
			+ "        stat_flag = 'V'\n"
			+ "  WHERE qs_id = ?\n";
		dbu.executeList(sqlUpdMstr, qsId);

		// 通知審核人員
		MailNotify mail = new MailNotify(req);
		mail.sendQsEditDoneNotify(dbu, qsId);
	}
	
	/**
	 * 將教案退回編輯模式，教案版本會加一
	 * @param req
	 * @param pgu
	 * @param qsId
	 * @throws StopException
	 * @throws SQLException
	 * @throws IOException
	 */
	public static void rejectQs(HttpServletRequest req, DbUtil dbu, String qsId, String reason) throws StopException, SQLException, IOException {
		UserData ud = UserData.getUserData();
		String today = new StdCalendar().toDbDateString();
		String sqlQryQs =
			  " SELECT curr_version, stat_flag, file_path FROM qsmstr WHERE qs_id = ?";
		Map<String, Object> rowQs = dbu.selectMapRowList(sqlQryQs, qsId);
		if (rowQs == null)
			throw new StopException("教案代碼 <" + qsId + "> 不存在");
		int currVer = (Integer) rowQs.get("curr_version");
		String statFlag = (String) rowQs.get("stat_flag");
		String qsPath = (String) rowQs.get("file_path");
		if (!c_qsMastFlagValid.equals(statFlag))
			throw new StopException("教案目前不是審核模式");
		int nextVer = currVer + 1;

		// 建立教案新版本目錄
		File qsFilePathOld = new File(getQsVersionPath(dbu, qsId, currVer));
		File qsFilePathNew = File.createTempFile("ver" + nextVer + "_", "", new File(getQsBasePath(), qsPath));
		if (qsFilePathNew == null)
			throw new StopException("無法建立教案新版本目錄");
		qsFilePathNew.delete();
		if (!qsFilePathNew.mkdir())
			throw new StopException("無法建立教案新版本目錄");

		// 複製舊版本檔案
		String sqlQryFile =
			  " SELECT file_name, file_class, file_desc, file_size, file_type, show_order, cr_date, cr_user_id, remark\n"
			+ "   FROM qsfile\n"
			+ "  WHERE qs_id = ?\n"
			+ "    AND ver_seq = ?\n"
			+ "  ORDER BY show_order\n";
		String sqlInsFile =
			  " INSERT INTO qsfile(qs_id, ver_seq, file_name, file_class, file_desc, file_size, file_type,\n"
			+ "				show_order, cr_date, cr_user_id, remark)\n"
			+ " VALUES(?, ?, ?, ?, ?, ?, ?,  ?, ?, ?, ?)\n";
		ResultSet rsFile = dbu.queryList(sqlQryFile, qsId, currVer);
		while (rsFile.next()) {
			String fileName = rsFile.getString("file_name");
			String fileClass = rsFile.getString("file_class");
			String fileDesc = rsFile.getString("file_desc");
			int fileSize = rsFile.getInt("file_size");
			String fileType = rsFile.getString("file_type");
			int showOrder = rsFile.getInt("show_order");
			String crDate = rsFile.getString("cr_date");
			String crUserId = rsFile.getString("cr_user_id");
			String fileRemark = rsFile.getString("remark");
			FileUtils.copyFile(new File(qsFilePathOld, fileName), new File(qsFilePathNew, fileName));
			dbu.executeList(sqlInsFile, qsId, nextVer, fileName, fileClass, fileDesc, fileSize, fileType,
				showOrder, crDate, crUserId, fileRemark);
		}
		rsFile.close();

		// 建立版本目錄與記錄
		String sqlInsVer =
			  " INSERT INTO qscnvr(qs_id, ver_seq, file_path, cr_date, cr_user_id, cnfrm_flag, edit_reason)\n"
			+ " VALUES(?, ?, ?, ?, ?, 'N', ?)";
		dbu.executeList(sqlInsVer, qsId, nextVer, qsFilePathNew.getName(), today, ud.getUserId(), reason);
		String sqlUpdMstr =
			  " UPDATE qsmstr SET\n"
			+ "        stat_flag = ?\n"
			+ "      , curr_version = ?\n"
			+ "  WHERE qs_id = ?\n";
		dbu.executeList(sqlUpdMstr, c_qsMastFlagEdit, nextVer, qsId);

		// 通知編輯人員
		MailNotify mail = new MailNotify(req);
		mail.sendQsValidDoneNotify(dbu, qsId, false, nextVer);
	}
	
	/**
	 * 審核人員審核教案目前版本後，設定審核通過
	 * @param req
	 * @param dbu
	 * @param qsId
	 * @param reason
	 * @throws SQLException
	 */
	public static void passQs(HttpServletRequest req, DbUtil dbu, String qsId) throws StopException, SQLException, IOException {
		String today = new StdCalendar().toDbDateString();
		String sqlQryQs =
			  " SELECT curr_version, stat_flag FROM qsmstr WHERE qs_id = ?";
		Map<String, Object> rowQs = dbu.selectMapRowList(sqlQryQs, qsId);
		if (rowQs == null)
			throw new StopException("教案代碼 <" + qsId + "> 不存在");
		int currVer = (Integer) rowQs.get("curr_version");
		String statFlag = (String) rowQs.get("stat_flag");
		if (!c_qsMastFlagValid.equals(statFlag))
			throw new StopException("教案目前不是審核模式");
		
		// 修改教案狀態
		String sqlUpdMstr =
			  " UPDATE qsmstr SET\n"
			+ "        stat_flag = ?\n"
			+ "      , online_date = ?"
			+ "  WHERE qs_id = ?\n";
		dbu.executeList(sqlUpdMstr, c_qsMastFlagPass, today, qsId);					
		// 完成審核，則通知教案創建人
		MailNotify mail = new MailNotify(req);
		mail.sendQsValidDoneNotify(dbu, qsId, true, currVer);
	}

	/**
	 * 將教案改為公告模式，教案原本需為通過審核模式
	 * @param req
	 * @param pgu
	 * @param qsId
	 * @throws SQLException
	 */
	public static void onlineQs(HttpServletRequest req, DbUtil dbu, String qsId) throws StopException, SQLException {
		String today = new StdCalendar().toDbDateString();
		String sqlQryQs =
			  " SELECT stat_flag FROM qsmstr WHERE qs_id = ?";
		String statFlag = dbu.selectStringList(sqlQryQs, qsId);
		if (statFlag == null)
			throw new StopException("教案代碼 <" + qsId + "> 不存在");
		if (!c_qsMastFlagPass.equals(statFlag))
			throw new StopException("教案目前不是審核通過模式");
		String sqlUpdMstr =
			  " UPDATE qsmstr SET\n"
			+ "        stat_flag = ?\n"
			+ "      , online_date = ?"
			+ "  WHERE qs_id = ?\n";
		dbu.executeList(sqlUpdMstr, c_qsMastFlagOnline, today, qsId);
	}

	/**
	 * 將教案置於廢止模式，教案原本需為編輯、審核或公告模式
	 * @param req
	 * @param pgu
	 * @param qsId
	 * @throws SQLException
	 */
	public static void abandonQs(HttpServletRequest req, DbUtil dbu, String qsId) throws StopException, SQLException {
		String today = new StdCalendar().toDbDateString();
		String sqlQryQs =
			  " SELECT stat_flag FROM qsmstr WHERE qs_id = ?";
		String statFlag = dbu.selectStringList(sqlQryQs, qsId);
		if (statFlag == null)
			throw new StopException("教案代碼 <" + qsId + "> 不存在");
		if (!c_qsMastFlagEdit.equals(statFlag) && !c_qsMastFlagValid.equals(statFlag) &&
			!c_qsMastFlagPass.equals(statFlag) && !c_qsMastFlagOnline.equals(statFlag))
			throw new StopException("教案目前不是編輯、審核、審核通過或公告模式");
		String sqlUpdMstr =
			  " UPDATE qsmstr SET\n"
			+ "        stat_flag = ?\n"
			+ "      , abandon_date = ?\n"
			+ "  WHERE qs_id = ?\n";
		dbu.executeList(sqlUpdMstr, c_qsMastFlagAbandon, today, qsId);
	}
}
