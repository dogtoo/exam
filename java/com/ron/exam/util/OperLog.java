package com.ron.exam.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.naming.Context;
import javax.naming.InitialContext;
import com.ron.exam.service.UserData;

public class OperLog {
	
	private static final String c_operLogPath;
	
	static {
		String path = null;
		try {
			Context initCtx = new InitialContext();
			path = (String) initCtx.lookup("java:/comp/env/conf/operationLog_path");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		c_operLogPath = path;
	}
	
	private StringBuffer m_msg;
	private String m_userId;
	private String m_progId;
	private String m_oper;
	private String m_file;
	
	public OperLog(String userId, String progId, String oper, String file) {
		m_msg = new StringBuffer();
		m_userId = userId;
		m_progId = progId;
		m_oper = oper;
		m_file = file;
	}
	
	public OperLog(String progId, String oper) {
		this(UserData.getUserData().getUserId(), progId, oper, null);
	}
	
	public void add(String fld, Object oval, Object nval) {
		if (oval == null && nval == null)
			return;
		if (oval != null && nval != null && oval.getClass().equals(nval.getClass()) && oval.equals(nval))
			return;
		if (m_msg.length() > 0)
			m_msg.append(", ");
		m_msg.append(fld);
		m_msg.append("='");
		m_msg.append(oval != null ? oval : "{NULL}");
		m_msg.append("'=>'");
		m_msg.append(nval != null ? nval : "{NULL}");
		m_msg.append("'");
	}
	
	public void add(String fld, Object val) {
		if (m_msg.length() > 0)
			m_msg.append(", ");
		m_msg.append(fld);
		m_msg.append("='");
		m_msg.append(val != null ? val : "{NULL}");
		m_msg.append("'");
	}
	
	public void write() {
		try {
			Calendar now = new GregorianCalendar();
			if (m_file == null) {
				if (c_operLogPath == null)
					return;
				String logFn = String.format("operLog-%tY%tm%td.log", now, now, now);
				m_file = new File(c_operLogPath, logFn).getPath();
			}
			PrintStream ps = new PrintStream(new FileOutputStream(m_file, true));
			ps.println(String.format("%tY/%tm/%td %tH:%tM:%tS [%s] [%s:%s] - %s",
				now, now, now, now, now, now, m_userId, m_progId, m_oper, m_msg.toString()));
			ps.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
