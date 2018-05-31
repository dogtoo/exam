package com.ron.exam.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import org.apache.http.client.utils.URIBuilder;
import com.ron.exam.util.DbUtil;
import com.ron.exam.util.StdCalendar;
import com.ron.exam.util.StopException;

public class MailNotify {

	public static final String c_newQsEditorNotify = "new_qs_editor_notify";
	public static final String c_newQsValidNotify = "new_qs_auditor_notify";
	public static final String c_qsEditDoneNotify = "qs_edit_done_notify";
	public static final String c_qsEditDoneAdminNotify = "qs_edit_done_admin_notify";
	public static final String c_qsValidDoneRejectNotify = "qs_audit_done_reject_notify";
	public static final String c_qsValidDoneRejectAdminNotify = "qs_audit_done_reject_admin_notify";
	public static final String c_qsValidDonePassAdminNotify = "qs_audit_done_pass_admin_notify";
	public static final String c_qsValidDonePassEditorNotify = "qs_audit_done_pass_editor_notify";
	
	public static final String c_repkeyQsId = "QsId";
	public static final String c_repkeyQsName = "QsName";
	public static final String c_repkeyQsStatDesc = "QsStatDesc";
	public static final String c_repkeyQsCurrVer = "QsCurrVer";
	public static final String c_repkeyQsAdminId = "QsAdminId";
	public static final String c_repkeyQsAdminName = "QsAdminName";
	public static final String c_repkeyQsEditorId = "QsEditorId";
	public static final String c_repkeyQsEditorName = "QsEditorName";
	public static final String c_repkeyQsValidId = "QsAuditorId";
	public static final String c_repkeyQsValidName = "QsAuditorName";
	public static final String c_repkeyQsReason = "QsReason";
	public static final String c_repkeyQsLink = "QsLink";
	/*
	 * 可替代文字說明
	 * ${QsAdminId}			教案創建人帳號
	 * ${QsAdminName}		教案創建人姓名
	 * ${QsEditorId}		教案編輯人帳號
	 * ${QsEditorName}		教案編輯人姓名
	 * ${QsAuditorId}		教案審核人帳號
	 * ${QsAuditorName}		教案審核人姓名
	 * ${QsId}				教案代碼
	 * ${QsName}			教案名稱
	 * ${QSStatDesc}		教案狀態
	 * ${QSCurrVer}			教案目前版本
	 * ${QsReason}			教案退回編輯原因
	 * ${QsLink}			教案網址
	 */
	
	private HttpServletRequest m_req;
	private String m_mailTmplPath;
	private String m_mailSmtpHost;
	private String m_mailSmtpPort;
	private boolean m_mailSmtpTls;
	private String m_mailSender;
	private String m_mailUser;
	private String m_mailAuth;
	private String m_mimeType;
	private Map<String, String> m_replaceMap;
	
	public MailNotify(HttpServletRequest req) throws StopException {
		// conf/mail_smtp_host 格式可為以下三者之一
		// 1. {host} 只設定主機
		// 2. {host}/{sender} 設定主機及寄件人
		// 3. {host}/{sender}/{password} 設定主機、寄件人及登入密碼
		try {
			Context initCtx = new InitialContext();
			Context envCtx = (Context) initCtx.lookup("java:/comp/env");
			m_mailTmplPath = (String) envCtx.lookup("conf/mail_template_path");
			String smtp = (String) envCtx.lookup("conf/mail_smtp_host");
			Pattern reSmtp = Pattern.compile("([^\\[/:]+?)(:(\\w+))?(\\[([\\w,]+)\\])?(/(([^/@]+?)@[^/]+?)?)?(/(.+))?");
			Matcher maSmtp = reSmtp.matcher(smtp);
			if (!maSmtp.matches())
				throw new StopException("設定的 mail_smtp_host <" + smtp + "> 格式不正確");
			m_mailSmtpHost = maSmtp.group(1);
			if (maSmtp.group(2) != null)
				m_mailSmtpPort = maSmtp.group(3);
			if (maSmtp.group(4) != null) {
				String auxes[] = maSmtp.group(5).split(",");
				for (String aux: auxes) {
					if ("tls".equals(aux))
						m_mailSmtpTls = true;
				}
			}
			if (maSmtp.group(6) != null) {
				m_mailSender = maSmtp.group(7);
				m_mailUser = maSmtp.group(8);
			}
			if (maSmtp.group(9) != null)
				m_mailAuth = maSmtp.group(10);
		}
		catch (NamingException e) {
			e.printStackTrace();
			throw new StopException(e.toString());
		}
		m_req = req;
		m_replaceMap = new HashMap<String, String>();
	}
	
	public void setReplaceMap(List<String> data) {
		m_replaceMap.clear();
		for (int i = 0; i + 1 < data.size(); i += 2) {
			m_replaceMap.put(data.get(i), data.get(i + 1));
		}
	}
	
	private String getDirectLink(String progId, String qsId) {
		String path = m_req.getContextPath();
		path += "/Login_direct";
		URIBuilder url = new URIBuilder().setScheme("http").setHost(m_req.getServerName()).setPort(m_req.getServerPort())
			.setPath(path).addParameter("acotArg", progId + "," + qsId);
		String link = "<a href='";
		try {
			link += url.build().toString();
		}
		catch (Exception e) {
			link += "#";
		}
		link += "'>點此進入</a>";
		return link;
	}
	
	private void loadMailContent(String fileName, StringBuffer subject, StringBuffer content) throws StopException {
		try {
			Reader rd = null;
			if (rd == null) {
				File fl = new File(m_mailTmplPath, fileName + ".html");
				if (fl.isFile() && fl.canRead()) {
					rd = new FileReader(fl);
					m_mimeType = "text/html; charset=UTF-8";
				}
			}
			if (rd == null) {
				File fl = new File(m_mailTmplPath, fileName + ".txt");
				if (fl.isFile() && fl.canRead()) {
					rd = new FileReader(fl);
					m_mimeType = "text/plain; charset=UTF-8";
				}
			}
			if (rd == null)
				throw new StopException("無法載入郵件樣版檔案 <" + fileName + ">");
			String line;
			Pattern reKey = Pattern.compile("(\\$\\{(\\w+)\\})");
			BufferedReader rdr = new BufferedReader(rd);
			boolean first = true;
			while ((line = rdr.readLine()) != null) {
				StringBuffer target;
				if (first && line.startsWith("Subject:")) {
					target = subject;
					line = line.substring(8);
				}
				else
					target = content;
				first = false;
				int at = 0;
				while (at >= 0) {
					Matcher maKey = reKey.matcher(line);
					if (maKey.find(at)) {
						int found = maKey.start();
						target.append(line.substring(at,  found));
						if (m_replaceMap.containsKey(maKey.group(2)))
							target.append(m_replaceMap.get(maKey.group(2)));
						at = found + maKey.group(1).length();
					}
					else {
						target.append(line.substring(at));
						at = -1;
					}
				}
				content.append('\n');
			}
			rd.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new StopException("郵件樣版檔案 <" + fileName + "> 不存在");
		}
		catch (IOException e) {
			e.printStackTrace();
			throw new StopException("載入郵件樣版檔案 <" + fileName + "> 時錯誤: " + e.toString());
		}
	}
	
	private void sendMail(String recpAddr, String template) throws StopException {
		Properties props = new Properties();
		if (m_mailSmtpTls) {
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.ssl.trust", "*");
		}
		props.put("mail.smtp.host", m_mailSmtpHost);
		if (m_mailSmtpPort != null)
			props.put("mail.smtp.port", m_mailSmtpPort);
		Session session = null;
		if (m_mailAuth != null) {
			props.put("mail.smtp.auth", "true");
			session = Session.getInstance(props, new Authenticator() {
				private String m_user;
				private String m_pass;
				Authenticator init(String user, String pass) {
					m_user = user;
					m_pass = pass;
					return this;
				}
				protected PasswordAuthentication getPasswordAuthentication() {
		               return new PasswordAuthentication(m_user, m_pass);
		            }
			}.init(m_mailUser, m_mailAuth));
		}
		else
			session = Session.getInstance(props);
		StringBuffer subject = new StringBuffer();
		StringBuffer content = new StringBuffer();
		loadMailContent(template, subject, content);
		try {
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(m_mailSender));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(recpAddr));
			message.setSubject(subject.toString());
			message.setContent(content.toString(), m_mimeType);
			Transport.send(message);
		}
		catch (MessagingException e) {
			e.printStackTrace();
			throw new StopException("寄送郵件時錯誤: " + e.toString());
		}
	}
	
	private List<String> getMasterData(DbUtil dbu, String qsId) throws StopException, SQLException{
		List<String> dataList = new ArrayList<String>();
		String sqlQryMstr =
			  " SELECT qs_name, cr_user_id, stat_flag, curr_version\n"
			+ "      , (SELECT user_name FROM mbdetl b WHERE a.cr_user_id = b.user_id) cr_user_name\n"
			+ "   FROM qsmstr a\n"
			+ "  WHERE qs_id = ?\n";
		Map<String, Object> rowMstr = dbu.selectMapRowList(sqlQryMstr, qsId);
		if (rowMstr == null)
			throw new StopException("教案代碼 <" + qsId + "> 不存在");
		Map<String, String> statFlagDesc = CodeSvc.buildStringMapByKind(dbu, CodeSvc.c_kindQsMastFlag);
		dataList.add(c_repkeyQsId); 
		dataList.add(qsId); 
		dataList.add(c_repkeyQsName); 
		dataList.add(DbUtil.nullToEmpty((String) rowMstr.get("qs_name"))); 
		dataList.add(c_repkeyQsStatDesc); 
		dataList.add(statFlagDesc.get((String) rowMstr.get("stat_flag"))); 
		dataList.add(c_repkeyQsCurrVer); 
		dataList.add(Integer.toString((Integer) rowMstr.get("curr_version"))); 
		dataList.add(c_repkeyQsAdminId); 
		dataList.add((String) rowMstr.get("cr_user_id")); 
		dataList.add(c_repkeyQsAdminName); 
		dataList.add(DbUtil.nullToEmpty((String) rowMstr.get("cr_user_name"))); 
		return dataList;
	}
	
	/**
	 * 通知新建立教案中的編輯與審核人員
	 * @param dbu
	 * @param qsId
	 */
	public void sendNewQsNotify(DbUtil dbu, String qsId) throws StopException, SQLException {
		List<String> dataList = getMasterData(dbu, qsId);
		String sqlQryUser =
			  " SELECT role_flag, user_id\n"
			+ "      , (SELECT user_name FROM mbdetl b WHERE a.user_id = b.user_id) user_name\n"
			+ "      , (SELECT email FROM mbdetl b WHERE a.user_id = b.user_id) email\n"
			+ "   FROM qsuser a\n"
			+ "  WHERE qs_id = ?\n"
			+ "    AND role_flag IN ('E', 'V')\n"
			+ "    AND end_date >= ?\n";
		String today = new StdCalendar().toDbDateString();
		ResultSet rsUser = dbu.queryList(sqlQryUser, qsId, today);
		while (rsUser.next()) {
			String roleFlag = rsUser.getString("role_flag");
			String userId = rsUser.getString("user_id");
			String userName = DbUtil.nullToEmpty(rsUser.getString("user_name"));
			String email = DbUtil.nullToEmpty(rsUser.getString("email"));
			if (email.isEmpty())
				continue;
			List<String> data = new ArrayList<String>(dataList);
			if ("E".equals(roleFlag)) {
				data.add(c_repkeyQsEditorId);
				data.add(userId);
				data.add(c_repkeyQsEditorName);
				data.add(userName);
				data.add(c_repkeyQsLink);
				data.add(getDirectLink("QsEditMast", qsId));
				setReplaceMap(data);
				sendMail(email, c_newQsEditorNotify);
			}
			else if ("V".equals(roleFlag)) {
				data.add(c_repkeyQsValidId);
				data.add(userId);
				data.add(c_repkeyQsValidName);
				data.add(userName);
				data.add(c_repkeyQsLink);
				data.add(getDirectLink("QsValidMast", qsId));
				setReplaceMap(data);
				sendMail(email, c_newQsValidNotify);
			}
		}
		rsUser.close();
	}
	
	/**
	 * 教案完成編輯，通知審核人員
	 * @param dbu
	 * @param qsId
	 */
	public void sendQsEditDoneNotify(DbUtil dbu, String qsId) throws StopException, SQLException {
		List<String> dataList = getMasterData(dbu, qsId);
		String sqlQryUser =
			  " SELECT role_flag, user_id\n"
			+ "      , (SELECT user_name FROM mbdetl b WHERE a.user_id = b.user_id) user_name\n"
			+ "      , (SELECT email FROM mbdetl b WHERE a.user_id = b.user_id) email\n"
			+ "   FROM qsuser a\n"
			+ "  WHERE qs_id = ?\n"
			+ "    AND role_flag IN ('A', 'V')\n"
			+ "    AND end_date >= ?\n";
		String today = new StdCalendar().toDbDateString();
		ResultSet rsUser = dbu.queryList(sqlQryUser, qsId, today);
		while (rsUser.next()) {
			String roleFlag = rsUser.getString("role_flag");
			String userId = rsUser.getString("user_id");
			String userName = DbUtil.nullToEmpty(rsUser.getString("user_name"));
			String email = DbUtil.nullToEmpty(rsUser.getString("email"));
			if (email.isEmpty())
				continue;
			List<String> data = new ArrayList<String>(dataList);
			if ("V".equals(roleFlag)) {
				data.add(c_repkeyQsValidId);
				data.add(userId);
				data.add(c_repkeyQsValidName);
				data.add(userName);
				data.add(c_repkeyQsLink);
				data.add(getDirectLink("QsValidMast", qsId));
				setReplaceMap(data);
				sendMail(email, c_qsEditDoneNotify);
			}
			else if("A".equals(roleFlag)){
				data.add(c_repkeyQsAdminId);
				data.add(userId);
				data.add(c_repkeyQsAdminName);
				data.add(userName);
				data.add(c_repkeyQsLink);
				data.add(getDirectLink("QsValidMast", qsId));
				setReplaceMap(data);
				sendMail(email, c_qsEditDoneAdminNotify);
			}
			
		}
		rsUser.close();
	}
	
	/**
	 * 通知完成審核，視審核結果通知創建人或編輯人員
	 * @param dbu
	 * @param qsId
	 */
	public void sendQsValidDoneNotify(DbUtil dbu, String qsId, boolean pass, int verSeq) throws StopException, SQLException {
		List<String> dataList = getMasterData(dbu, qsId);
		String sqlQryUser =
			  " SELECT role_flag, user_id\n"
			+ "      , (SELECT user_name FROM mbdetl b WHERE a.user_id = b.user_id) user_name\n"
			+ "      , (SELECT email FROM mbdetl b WHERE a.user_id = b.user_id) email\n"
			+ "   FROM qsuser a\n"
			+ "  WHERE qs_id = ?\n"
			+ "    AND role_flag IN ('E', 'A')\n"
			+ "    AND end_date >= ?\n";
		String today = new StdCalendar().toDbDateString();
		ResultSet rsUser = dbu.queryList(sqlQryUser, qsId, today);
		while (rsUser.next()) {
			String roleFlag = rsUser.getString("role_flag");
			String userId = rsUser.getString("user_id");
			String userName = DbUtil.nullToEmpty(rsUser.getString("user_name"));
			String email = DbUtil.nullToEmpty(rsUser.getString("email"));
			if (email.isEmpty())
				continue;
			List<String> data = new ArrayList<String>(dataList);
			if (!pass) {
				// 審核不通過，轉為編輯，取得所有不通過的審核意見結合。
				String sqlQryReject =
						  " SELECT edit_reason\n"
						+ "   FROM qscnvr\n"
						+ "  WHERE qs_id = ?\n"
						+ "    AND ver_seq = ?\n";
				ResultSet rsReject = dbu.queryList(sqlQryReject, qsId, verSeq);
				StringBuffer rejectReason = new StringBuffer();
				while (rsReject.next()) {
					rejectReason.append(DbUtil.nullToEmpty(rsReject.getString("edit_reason")));
					rejectReason.append('\n');
				}
				rsReject.close();
				String rejReason = rejectReason.length() <= 1000 ? rejectReason.toString() : rejectReason.toString().substring(0, 1000);
				data.add(c_repkeyQsReason);
				data.add(rejReason);
				if("E".equals(roleFlag)){
					data.add(c_repkeyQsEditorId);
					data.add(userId);
					data.add(c_repkeyQsEditorName);
					data.add(userName);
					data.add(c_repkeyQsLink);
					data.add(getDirectLink("QsEditMast", qsId));
					setReplaceMap(data);
					sendMail(email, c_qsValidDoneRejectNotify);
				}
				else if("A".equals(roleFlag)) {
					data.add(c_repkeyQsAdminId);
					data.add(userId);
					data.add(c_repkeyQsAdminName);
					data.add(userName);
					data.add(c_repkeyQsLink);
					data.add(getDirectLink("QsEditMast", qsId));
					setReplaceMap(data);
					sendMail(email, c_qsValidDoneRejectAdminNotify);
				}
			}
			else if ("E".equals(roleFlag) && pass) {
				data.add(c_repkeyQsEditorId);
				data.add(userId);
				data.add(c_repkeyQsEditorName);
				data.add(userName);
				setReplaceMap(data);
				sendMail(email, c_qsValidDonePassEditorNotify);
			}
			else if ("A".equals(roleFlag) && pass) {
				data.add(c_repkeyQsAdminId);
				data.add(userId);
				data.add(c_repkeyQsAdminName);
				data.add(userName);
				data.add(c_repkeyQsLink);
				data.add(getDirectLink("QsMntMast", qsId));
				setReplaceMap(data);
				sendMail(email, c_qsValidDonePassAdminNotify);
			}
		}
		rsUser.close();
	}
}
