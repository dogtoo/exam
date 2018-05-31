package com.ron.exam.util;

import java.sql.*;
import java.util.*;
import javax.sql.*;
import javax.naming.*;
import org.apache.log4j.Logger;

public class DbUtil {
	
	public static final String c_codeDupKey = "23505";

	private static final String c_dbcpKey = "jdbc/exam";
	private static final String c_driverClass = "org.postgresql.Driver";

	public static enum LogLevel {
		debug, info, warn, error, fatal, none
	};
	
	private String m_jdbcUrl;
	private String m_jdbcUser;
	private String m_jdbcPass;
	private Connection m_conn;
	private Savepoint m_savepoint;
	private Logger m_log;
	private LogLevel m_logLevel;
	private String allocTrace;
	
	public DbUtil() {
		m_jdbcUrl = null;
		m_conn = null;
		m_log = Logger.getLogger(getClass());
		m_logLevel = LogLevel.error;
	}
	
	public DbUtil(String url, String user, String pass) {
		m_jdbcUrl = url;
		m_jdbcUser = user;
		m_jdbcPass = pass;
		m_conn = null;
		m_log = Logger.getLogger(getClass());
		m_logLevel = LogLevel.error;
	}
	
	protected void finalize() throws Throwable {
		super.finalize();
		if (m_conn != null && allocTrace != null) {
			System.out.println("未釋放連線:\n" + allocTrace);
			m_log.info("未釋放連線:\n" + allocTrace);
		}
	}
	
	public Connection getDbConn() {
		// 從 JDBC connection pool 中取得資料庫連線
		if (m_conn != null)
			return m_conn;
		try {
			if (m_jdbcUrl == null) {
				Context initCtx = new InitialContext();
				Context envCtx = (Context) initCtx.lookup("java:/comp/env");
				DataSource ds = (DataSource) envCtx.lookup(c_dbcpKey);
				m_conn = ds.getConnection();
			}
			else {
				Class.forName(c_driverClass);
				m_conn = DriverManager.getConnection(m_jdbcUrl, m_jdbcUser, m_jdbcPass);
			}
			m_conn.setAutoCommit(false);
			StackTraceElement trace[] = Thread.currentThread().getStackTrace();
			StringBuffer traceStr = new StringBuffer();
			for (int i = 0; i < trace.length && i < 10; i++) {
				traceStr.append(trace[i].toString());
				traceStr.append('\n');
			}
			allocTrace = traceStr.toString();
		}
		catch (Exception e) {
			if (m_logLevel.ordinal() >= LogLevel.error.ordinal())
				m_log.error("PgUtil.getDbConn error: " + e.getMessage(), e);
		}
		return m_conn;
	}

	public void relDbConn() {
		try {
			if (m_conn != null)
				m_conn.close();
		}
		catch (SQLException e) {
			if (m_logLevel.ordinal() >= LogLevel.error.ordinal())
				m_log.error("relDbConn error: " + e.getMessage(), e);
		}
		m_conn = null;
	}
	
	public Savepoint getSavePoint() throws SQLException {
		m_savepoint = getDbConn().setSavepoint();
		return m_savepoint;
	}
	
	public void relSavePoint(Savepoint savepoint) throws SQLException {
		getDbConn().releaseSavepoint(savepoint);
	}
	
	public void relSavePoint() throws SQLException {
		getDbConn().releaseSavepoint(m_savepoint);
	}
	
	public void rollbackSavePoint(Savepoint savepoint) throws SQLException {
		getDbConn().rollback(savepoint);
	}
	
	public void rollbackSavePoint() throws SQLException {
		getDbConn().rollback(m_savepoint);
	}
	
	public void doCommit() {
		try {
			if (m_conn != null)
				m_conn.commit();
		}
		catch (SQLException e) {
			m_log.error("commit error: " + e.getMessage(), e);
		}
	}
	
	public void doRollback() {
		try {
			if (m_conn != null)
				m_conn.rollback();
		}
		catch (SQLException e) {
			m_log.error("rollback error: " + e.getMessage(), e);
		}
	}
	
	public void setLogger(Logger log) {
		if (log != null)
			m_log = log;
	}

	public LogLevel getLogLevel() {
		return m_logLevel;
	}
	
	public void setLogLevel(LogLevel level) {
		m_logLevel = level;
	}
	
	public CallableStatement buildCall(Connection conn, String sql) throws SQLException {
		if (m_logLevel.ordinal() >= LogLevel.debug.ordinal())
			m_log.debug("sql: " + sql);
		CallableStatement st = conn.prepareCall(sql);
		return st;
	}

	public CallableStatement buildCall(String sql) throws SQLException {
		return buildCall(getDbConn(), sql);
	}

	public PreparedStatement buildStatement(Connection conn, String sql) throws SQLException {
		if (m_logLevel.ordinal() >= LogLevel.debug.ordinal())
			m_log.debug("sql: " + sql);
		PreparedStatement st = conn.prepareStatement(sql);
		return st;
	}

	public PreparedStatement buildStatement(String sql) throws SQLException {
		return buildStatement(getDbConn(), sql);
	}

	public void bindParameter(PreparedStatement st, int pos, Object val) throws SQLException {
		if (m_logLevel.ordinal() >= LogLevel.debug.ordinal())
			m_log.debug("  bind " + pos + ": " + val + " (" + (val != null ? val.getClass() : "") + ")");
		st.setObject(pos, val);
	}
	
	public void bindParameterArray(PreparedStatement st, Object[] args) throws SQLException {
		for (int i = 0; i < args.length; i++)
			bindParameter(st, i + 1, args[i]);
	}

	public void bindParameterList(PreparedStatement st, Object... args) throws SQLException {
		bindParameterArray(st, args);
	}

	public ResultSet queryArray(Connection conn, String sql, Object[] args) throws SQLException {
		PreparedStatement st = buildStatement(conn, sql);
		bindParameterArray(st, args);
		return st.executeQuery();
	}

	public ResultSet queryArray(String sql, Object[] args) throws SQLException {
		return queryArray(getDbConn(), sql, args);
	}

	public ResultSet queryList(Connection conn, String sql, Object... args) throws SQLException {
		return queryArray(conn, sql, args);
	}

	public ResultSet queryList(String sql, Object... args) throws SQLException {
		return queryArray(getDbConn(), sql, args);
	}

	public int selectIntArray(Connection conn, String sql, Object[] args) throws SQLException {
		ResultSet rs = queryArray(conn, sql, args);
		int cnt = 0;
		if (rs.next())
			cnt = rs.getInt(1);
		rs.close();
		return cnt;
	}

	public int selectIntArray(String sql, Object[] args) throws SQLException {
		return selectIntArray(getDbConn(), sql, args);
	}

	public int selectIntList(Connection conn, String sql, Object... args) throws SQLException {
		return selectIntArray(conn, sql, args);
	}

	public int selectIntList(String sql, Object... args) throws SQLException {
		return selectIntArray(getDbConn(), sql, args);
	}

	public String selectStringArray(Connection conn, String sql, Object[] args) throws SQLException {
		ResultSet rs = queryArray(conn, sql, args);
		String result = null;
		if (rs.next())
			result = rs.getString(1);
		rs.close();
		return result;
	}

	public String selectStringArray(String sql, Object[] args) throws SQLException {
		return selectStringArray(getDbConn(), sql, args);
	}

	public String selectStringList(Connection conn, String sql, Object... args) throws SQLException {
		return selectStringArray(conn, sql, args);
	}

	public String selectStringList(String sql, Object... args) throws SQLException {
		return selectStringArray(getDbConn(), sql, args);
	}

	public Object selectObjectArray(Connection conn, String sql, Object[] args) throws SQLException {
		ResultSet rs = queryArray(conn, sql, args);
		Object result = null;
		if (rs.next())
			result = rs.getObject(1);
		rs.close();
		return result;
	}

	public Object selectObjectArray(String sql, Object[] args) throws SQLException {
		return selectObjectArray(getDbConn(), sql, args);
	}

	public Object selectObjectList(Connection conn, String sql, Object... args) throws SQLException {
		return selectObjectArray(conn, sql, args);
	}

	public Object selectObjectList(String sql, Object... args) throws SQLException {
		return selectObjectArray(getDbConn(), sql, args);
	}

	public List<Object> selectListRowArray(Connection conn, String sql, Object[] args) throws SQLException {
		ResultSet rs = queryArray(conn, sql, args);
		int cnt = rs.getMetaData().getColumnCount();
		List<Object> row = null;
		if (rs.next()) {
			row = new ArrayList<Object>();
			for (int i = 1; i <= cnt; i++)
				row.add(rs.getObject(i));
		}
		rs.close();
		return row;
	}

	public List<Object> selectListRowArray(String sql, Object[] args) throws SQLException {
		return selectListRowArray(getDbConn(), sql, args);
	}

	public List<Object> selectListRowList(Connection conn, String sql, Object... args) throws SQLException {
		return selectListRowArray(conn, sql, args);
	}

	public List<Object> selectListRowList(String sql, Object... args) throws SQLException {
		return selectListRowArray(getDbConn(), sql, args);
	}

	public Map<String, Object> selectMapRowArray(Connection conn, String sql, Object[] args) throws SQLException {
		ResultSet rs = queryArray(conn, sql, args);
		ResultSetMetaData rsm = rs.getMetaData();
		Map<String, Object> row = null;
		if (rs.next()) {
			row = new HashMap<String, Object>();
			int cnt = rsm.getColumnCount();
			for (int i = 1; i <= cnt; i++)
				row.put(rsm.getColumnLabel(i), rs.getObject(i));
		}
		rs.close();
		return row;
	}

	public Map<String, Object> selectMapRowArray(String sql, Object[] args) throws SQLException {
		return selectMapRowArray(getDbConn(), sql, args);
	}

	public Map<String, Object> selectMapRowList(Connection conn, String sql, Object... args) throws SQLException {
		return selectMapRowArray(conn, sql, args);
	}

	public Map<String, Object> selectMapRowList(String sql, Object... args) throws SQLException {
		return selectMapRowArray(getDbConn(), sql, args);
	}

	public List<String> selectStringAllArray(Connection conn, String sql, Object[] args) throws SQLException {
		ResultSet rs = queryArray(conn, sql, args);
		List<String> all = new ArrayList<String>();
		while (rs.next()) {
			all.add(rs.getString(1));
		}
		rs.close();
		return all;
	}

	public List<String> selectStringAllArray(String sql, Object[] args) throws SQLException {
		return selectStringAllArray(getDbConn(), sql, args);
	}

	public List<String> selectStringAllList(Connection conn, String sql, Object... args) throws SQLException {
		return selectStringAllArray(conn, sql, args);
	}

	public List<String> selectStringAllList(String sql, Object... args) throws SQLException {
		return selectStringAllArray(getDbConn(), sql, args);
	}

	public Map<String, Object> selectKeyMapArray(Connection conn, String sql, String keyField, String valueField, Object[] args) throws SQLException {
		ResultSet rs = queryArray(conn, sql, args);
		Map<String, Object> all = new HashMap<String, Object>();
		while (rs.next()) {
			all.put(rs.getString(keyField), valueField != null ? rs.getObject(valueField) : null);
		}
		rs.close();
		return all;
	}

	public Map<String, Object> selectKeyMapArray(String sql, String keyField, String valueField, Object[] args) throws SQLException {
		return selectKeyMapArray(getDbConn(), sql, keyField, valueField, args);
	}

	public Map<String, Object> selectKeyMapList(Connection conn, String sql, String keyField, String valueField, Object... args) throws SQLException {
		return selectKeyMapArray(conn, sql, keyField, valueField, args);
	}

	public Map<String, Object> selectKeyMapList(String sql, String keyField, String valueField, Object... args) throws SQLException {
		return selectKeyMapArray(getDbConn(), sql, keyField, valueField, args);
	}

	public Map<String, String> selectKeyStringArray(Connection conn, String sql, String keyField, String valueField, Object[] args) throws SQLException {
		ResultSet rs = queryArray(conn, sql, args);
		Map<String, String> all = new HashMap<String, String>();
		while (rs.next()) {
			all.put(rs.getString(keyField), rs.getString(valueField));
		}
		rs.close();
		return all;
	}

	public Map<String, String> selectKeyStringArray(String sql, String keyField, String valueField, Object[] args) throws SQLException {
		return selectKeyStringArray(getDbConn(), sql, keyField, valueField, args);
	}

	public Map<String, String> selectKeyStringList(Connection conn, String sql, String keyField, String valueField, Object... args) throws SQLException {
		return selectKeyStringArray(conn, sql, keyField, valueField, args);
	}

	public Map<String, String> selectKeyStringList(String sql, String keyField, String valueField, Object... args) throws SQLException {
		return selectKeyStringArray(getDbConn(), sql, keyField, valueField, args);
	}

	public List<List<Object>> selectListAllArray(Connection conn, String sql, Object[] args) throws SQLException {
		ResultSet rs = queryArray(conn, sql, args);
		int cnt = rs.getMetaData().getColumnCount();
		List<List<Object>> all = new ArrayList<List<Object>>();
		while (rs.next()) {
			List<Object> row = new ArrayList<Object>();
			for (int i = 1; i <= cnt; i++)
				row.add(rs.getObject(i));
			all.add(row);
		}
		rs.close();
		return all;
	}

	public List<List<Object>> selectListAllArray(String sql, Object[] args) throws SQLException {
		return selectListAllArray(getDbConn(), sql, args);
	}

	public List<List<Object>> selectListAllList(Connection conn, String sql, Object... args) throws SQLException {
		return selectListAllArray(conn, sql, args);
	}

	public List<List<Object>> selectListAllList(String sql, Object... args) throws SQLException {
		return selectListAllArray(getDbConn(), sql, args);
	}

	public List<Map<String, Object>> selectMapAllArray(Connection conn, String sql, Object[] args) throws SQLException {
		ResultSet rs = queryArray(conn, sql, args);
		ResultSetMetaData rsm = rs.getMetaData();
		List<Map<String, Object>> all = new ArrayList<Map<String, Object>>();
		int cnt = rsm.getColumnCount();
		while (rs.next()) {
			Map<String, Object> row = new HashMap<String, Object>();
			for (int i = 1; i <= cnt; i++)
				row.put(rsm.getColumnLabel(i), rs.getObject(i));
			all.add(row);
		}
		rs.close();
		return all;
	}

	public List<Map<String, Object>> selectMapAllArray(String sql, Object[] args) throws SQLException {
		return selectMapAllArray(getDbConn(), sql, args);
	}

	public List<Map<String, Object>> selectMapAllList(Connection conn, String sql, Object... args) throws SQLException {
		return selectMapAllArray(conn, sql, args);
	}

	public List<Map<String, Object>> selectMapAllList(String sql, Object... args) throws SQLException {
		return selectMapAllArray(getDbConn(), sql, args);
	}

	public int executeArray(Connection conn, String sql, Object[] args) throws SQLException {
		PreparedStatement st = conn.prepareStatement(sql);
		bindParameterArray(st, args);
		int rc = st.executeUpdate();
		st.close();
		return rc;
	}

	public int executeArray(String sql, Object[] args) throws SQLException {
		return executeArray(getDbConn(), sql, args);
	}

	public int executeList(Connection conn, String sql, Object... args) throws SQLException {
		return executeArray(conn, sql, args);
	}

	public int executeList(String sql, Object... args) throws SQLException {
		return executeArray(getDbConn(), sql, args);
	}

    public String getSeq(Connection conn, String seqName, int seqLen) throws SQLException {
        String seqNo = "";
        String sql = "{ ? = call get_seq(?, ?) }";
        CallableStatement st = buildCall(sql);
		st.registerOutParameter(1, Types.VARCHAR);
		st.setString(2, seqName);
		st.setInt(3, seqLen);
		st.execute();
		seqNo = st.getString(1);
		st.close();
        return seqNo;
    }

    public String getSeq(String seqName, int seqLen) throws SQLException {
    	return getSeq(getDbConn(), seqName, seqLen);
    }
    
    public static void buildInSqlParam(StringBuffer sql, List<Object> params, List<String> data) {
    	sql.append("(");
    	for (int i = 0; i < data.size(); i++) {
    		if (i > 0)
    			sql.append(", ");
    		sql.append("?");
    		params.add(data.get(i));
    	}
    	sql.append(")");
    }

	public static String nullToEmpty(String val) {
		if (val == null)
			return "";
		return val;
	}

	public static String emptyToNull(String val) {
		// 如果字串內容為空字串，則以 null 取代
		if (val == null)
			return null;
		if (val.length() == 0)
			return null;
		return val;
	}
	
	public static String exceptionTranslation(SQLException e) {
		String result = e.toString();
		return result;
	}
}