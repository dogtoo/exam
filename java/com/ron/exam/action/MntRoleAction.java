package com.ron.exam.action;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpSession;
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
import com.ron.exam.service.ProgData;
import com.ron.exam.service.UserData;

@EnableWebMvc
@Controller
public class MntRoleAction {

	private static final String c_progId = "MntRole";
	
	@RequestMapping(value = "/MntRole", method = RequestMethod.GET)
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
		return "MntRole";
	}

	@RequestMapping(value = "/MntRole_qryRoleList", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryRoleList(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success", false);
			String mode = req.get("mode");

			int pageRow = 0;
			int pageAt = 0;
			try {
				pageRow = Integer.parseInt(req.get("pageRow"));
				if (pageRow < 0)
					pageRow = 0;
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
			
			if ("N".equals(mode)) {
				// N 模式需要一併查詢筆數
				String sqlCntRole = " SELECT COUNT(*) FROM cmrole";
				res.put("total", dbu.selectIntList(sqlCntRole));
			}
			
			StringBuffer sqlQryRole = new StringBuffer(
				  " SELECT role_id \"roleId\", role_name \"roleName\", suspend \"suspend\"\n"
				+ "   FROM cmrole\n"
				+ "  ORDER BY role_id\n");
			List<Object> params = new ArrayList<Object>();
			if (pageAt != 0 && pageRow != 0) {
				sqlQryRole.append(" OFFSET ?\n");
				params.add(pageAt * pageRow);
			}
			if (pageRow != 0) {
				sqlQryRole.append(" LIMIT ?\n");
				params.add(pageRow);
			}
			res.put("roleList", dbu.selectMapAllArray(sqlQryRole.toString(), params.toArray()));
			dbu.doCommit();
			
			res.put("success", true);
			res.put("status", "查詢角色列表完成");
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
		
		return res;
	}

	@RequestMapping(value = "/MntRole_qryProgList", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryProgList(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			String sqlQryProg =
				  " SELECT code_desc sys_desc, prog_id, prog_desc, def_priv_base, def_priv_aux\n"
				+ "      , all_priv_aux, priv_aux_desc\n"
				+ "   FROM cmprog a, cmcode b\n"
				+ "  WHERE b.code_kind = 'SYS'\n"
				+ "    AND a.sys_id = b.code_code\n"
				+ "    AND a.show_order >= 0\n"
				+ "  ORDER BY b.code_order, a.show_order\n";
			ResultSet rsProg = dbu.queryList(sqlQryProg);
			Map<String, String> privMap = CodeSvc.buildStringMapByKind(dbu, "PRIVBASE"); 
			List<Map<String, Object>> progList = new ArrayList<Map<String, Object>>();
			while (rsProg.next()) {
				Map<String, Object> prog = new HashMap<String, Object>();
				prog.put("sysDesc", rsProg.getString("sys_desc"));
				prog.put("progId", rsProg.getString("prog_id"));
				prog.put("progDesc", rsProg.getString("prog_desc"));
				prog.put("defPrivBase", rsProg.getString("def_priv_base"));
				prog.put("defPrivAux", DbUtil.nullToEmpty(rsProg.getString("def_priv_aux")));
				prog.put("allPrivAux", DbUtil.nullToEmpty(rsProg.getString("all_priv_aux")));
				prog.put("privAuxDesc", DbUtil.nullToEmpty(rsProg.getString("priv_aux_desc")));
				String privDesc = privMap.get(rsProg.getString("def_priv_base"));
				if (!DbUtil.nullToEmpty(rsProg.getString("all_priv_aux")).isEmpty())
					privDesc += " [" + rsProg.getString("all_priv_aux") + "]";
				prog.put("privDesc", privDesc);
				progList.add(prog);
			}
			rsProg.close();
			res.put("progList", progList);
			dbu.doCommit();
			
			res.put("status", "查詢作業列表完成");
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
		
		return res;
	}

	@RequestMapping(value = "/MntRole_qryRole", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryRole(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success", false);
			String roleId = req.get("roleId");
			
			String sqlQryRole = " SELECT role_name, suspend, remark FROM cmrole WHERE role_id = ?";
			Map<String, Object> rowRole = dbu.selectMapRowList(sqlQryRole, roleId);
			if (rowRole == null)
				throw new StopException("指定角色代碼 <" + roleId + "> 不存在");
			res.put("roleId", roleId);
			res.put("roleName", rowRole.get("role_name"));
			res.put("suspend", "Y".equals(rowRole.get("suspend")));
			res.put("remark", DbUtil.nullToEmpty((String) rowRole.get("remark")));
			
			String sqlQryPriv =
				  " SELECT c.code_desc \"sysDesc\", a.prog_id \"progId\", a.priv_base \"privBase\"\n"
				+ "      , a.priv_aux \"privAux\", COALESCE(b.all_priv_aux, '') \"allPrivAux\"\n"
				+ "      , b.prog_desc \"progDesc\"\n"
				+ "   FROM cmpriv a, cmprog b, cmcode c\n"
				+ "  WHERE a.acc_type = 'R'\n"
				+ "    AND a.acc_id = ?\n"
				+ "    AND a.prog_id = b.prog_id\n"
				+ "    AND c.code_kind = 'SYS'\n"
				+ "    AND b.sys_id = c.code_code\n"
				+ "  ORDER BY c.code_order, b.show_order\n";
			res.put("privList", dbu.selectMapAllList(sqlQryPriv, roleId));
			dbu.doCommit();
			
			res.put("success", true);
			res.put("status", "查詢角色完成");
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

	@RequestMapping(value = "/MntRole_addRole", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addRole(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");

			String roleId = req.get("roleId");
			if (roleId.isEmpty())
				throw new StopException("請先指定角色代碼");
			String sqlCntRole = " SELECT COUNT(*) FROM cmrole WHERE role_id = ?";
			if (dbu.selectIntList(sqlCntRole, roleId) > 0)
				throw new StopException("角色代碼 <" + roleId + "> 已存在");
			String roleName = req.get("roleName");
			if (roleName.isEmpty())
				throw new StopException("請先指定角色名稱");
			String suspend = "Y".equals(req.get("suspend")) ? "Y" : "N";
			String remark = DbUtil.emptyToNull((String) req.get("remark"));
			ProgData pd = ProgData.getProgData();
			List<String[]> privList = new ArrayList<String[]>();
			for (int i = 0; ; i++) {
				String key = "progId[" + i + "]";
				if (!req.containsKey(key))
					break;;
				String progId = req.get(key);
				key = "privBase[" + i + "]";
				if (!req.containsKey(key))
					break;
				String privBase = req.get(key);
				if (!ProgData.c_privBaseMaintain.equals(privBase) && !ProgData.c_privBaseQuery.equals(privBase) &&
					!ProgData.c_privBaseProhibit.equals(privBase))
					throw new StopException("不正確的基本權限設定值 <" + privBase + ">");
				key = "privAux[" + i + "]";
				if (!req.containsKey(key))
					break;
				String privAux = req.get(key);
				if (!pd.checkProgAuxAcceptable(progId, privAux))
					throw new StopException("不正確的基本權限設定值 <" + privBase + ">");
				privList.add(new String[] { progId, privBase, privAux });
			}

			String sqlInsRole =
				  " INSERT INTO cmrole(role_id, role_name, suspend, remark)\n"
				+ " VALUES(?, ?, ?, ?)";
			dbu.executeList(sqlInsRole, roleId, roleName, suspend, remark);
			StringBuffer privStr = new StringBuffer();
			for (int i = 0; i < privList.size(); i++) {
				String priv[] = privList.get(i);
				String progId = priv[0];
				String privBase = priv[1];
				String privAux = priv[2];
				String sqlInsPriv =
					  " INSERT INTO cmpriv(acc_type, acc_id, prog_id, priv_base, priv_aux)\n"
					+ " VALUES('R', ?, ?, ?, ?)\n";
				dbu.executeList(sqlInsPriv, roleId, progId, privBase, privAux);
				privStr.append("(" + progId + "," + privBase + "," + privAux + ")");
			}
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "addRole");
            operLog.add("roleId", roleId);
            operLog.add("roleName", roleName);
            operLog.add("suspend", suspend);
            operLog.add("remark", remark);
            operLog.add("privList", privStr);
            operLog.write();

            res.put("success", true);
			res.put("status", "新增角色完成");
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

	@RequestMapping(value = "/MntRole_modRole", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> modRole(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			
			String roleIdOrg = req.get("roleIdOrg");
			if (roleIdOrg.isEmpty())
				throw new StopException("請先指定原角色代碼");
			String roleId = req.get("roleId");
			if (roleId.isEmpty())
				throw new StopException("請先指定角色代碼");
			String sqlCntRole = " SELECT COUNT(*) FROM cmrole WHERE role_id = ?";
			if (dbu.selectIntList(sqlCntRole, roleIdOrg) == 0)
				throw new StopException("原角色代碼 <" + roleIdOrg + "> 不存在");
			if (!roleId.equals(roleIdOrg) && dbu.selectIntList(sqlCntRole, roleId) > 0)
				throw new StopException("角色代碼 <" + roleId + "> 已存在");
			String roleName = req.get("roleName");
			if (roleName.isEmpty())
				throw new StopException("請先指定角色名稱");
			String suspend = "Y".equals(req.get("suspend")) ? "Y" : "N";
			String remark = DbUtil.emptyToNull((String) req.get("remark"));
			ProgData pd = ProgData.getProgData();
			List<String[]> privList = new ArrayList<String[]>();
			for (int i = 0; ; i++) {
				String key = "progId[" + i + "]";
				if (!req.containsKey(key))
					break;;
				String progId = req.get(key);
				key = "privBase[" + i + "]";
				if (!req.containsKey(key))
					break;
				String privBase = req.get(key);
				if (!ProgData.c_privBaseMaintain.equals(privBase) && !ProgData.c_privBaseQuery.equals(privBase) &&
					!ProgData.c_privBaseProhibit.equals(privBase))
					throw new StopException("不正確的基本權限設定值 <" + privBase + ">");
				key = "privAux[" + i + "]";
				if (!req.containsKey(key))
					break;
				String privAux = req.get(key);
				if (!pd.checkProgAuxAcceptable(progId, privAux))
					throw new StopException("不正確的基本權限設定值 <" + privBase + ">");
				privList.add(new String[] { progId, privBase, privAux });
			}

			String sqlQryRole =
				  " SELECT role_name, suspend, remark\n"
				+ "   FROM cmrole\n"
				+ "  WHERE role_id = ?\n";
			Map<String, Object> rowRole = dbu.selectMapRowList(sqlQryRole, roleIdOrg);
			if (rowRole == null)
				throw new StopException("原角色代碼 <" + roleIdOrg + "> 不存在");
			String roleNameOrg = (String) rowRole.get("role_name");
			String suspendOrg = (String) rowRole.get("suspend");
			String remarkOrg = (String) rowRole.get("remark");
			String sqlQryPriv =
				  " SELECT a.prog_id, a.priv_base, a.priv_aux\n"
				+ "   FROM cmpriv a, cmprog b, cmcode c\n"
				+ "  WHERE a.acc_type = 'R'\n"
				+ "    AND a.acc_id = ?\n"
				+ "    AND a.prog_id = b.prog_id\n"
				+ "    AND c.code_kind = 'SYS'\n"
				+ "    AND b.sys_id = c.code_code\n"
				+ "  ORDER BY c.code_order, b.show_order\n";
			ResultSet rsPriv = dbu.queryList(sqlQryPriv, roleIdOrg);
			StringBuffer privStrOrg = new StringBuffer();
			while (rsPriv.next()) {
				String progId = rsPriv.getString("prog_id");
				String privBase = rsPriv.getString("priv_base");
				String privAux = rsPriv.getString("priv_aux");
				privStrOrg.append("(" + progId + "," + privBase + "," + privAux + ")");
			}
			rsPriv.close();

			String sqlUpdRole =
				  " UPDATE cmrole SET\n"
				+ "        role_id = ?\n"
				+ "      , role_name = ?\n"
				+ "      , suspend = ?\n"
				+ "      , remark = ?\n"
				+ "  WHERE role_id = ?\n";
			dbu.executeList(sqlUpdRole, roleId, roleName, suspend, remark, roleIdOrg);
			String sqlDelPriv = " DELETE FROM cmpriv WHERE acc_type = 'R' AND acc_id = ?";
			dbu.executeList(sqlDelPriv, roleIdOrg);
			StringBuffer privStr = new StringBuffer();
			for (int i = 0; i < privList.size(); i++) {
				String priv[] = privList.get(i);
				String progId = priv[0];
				String privBase = priv[1];
				String privAux = priv[2];
				String sqlInsPriv =
					  " INSERT INTO cmpriv(acc_type, acc_id, prog_id, priv_base, priv_aux)\n"
					+ " VALUES('R', ?, ?, ?, ?)\n";
				dbu.executeList(sqlInsPriv, roleId, progId, privBase, privAux);
				privStr.append("(" + progId + "," + privBase + "," + privAux + ")");
			}
			if (!roleId.equals(roleIdOrg)) {
				// 如果角色代碼變了，一並調整引用到的 role_id
				String sqlUpdUsrr =
					  " UPDATE cmusrr SET\n"
					+ "        role_id = ?\n"
					+ "  WHERE role_id = ?\n";
				dbu.executeList(sqlUpdUsrr, roleId, roleIdOrg);
			}
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "modRole");
			if (roleId.equals(roleIdOrg))
	            operLog.add("roleId", roleId);
			else
				operLog.add("roleId", roleIdOrg, roleId);
            operLog.add("roleName", roleNameOrg, roleName);
            operLog.add("suspend", suspendOrg, suspend);
            operLog.add("remark", remarkOrg, remark);
            operLog.add("privList", privStrOrg, privStr);
            operLog.write();;
			
            res.put("success", true);
			res.put("status", "修改角色完成");
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

	@RequestMapping(value = "/MntRole_setSuspend", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> setSuspend(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			
			String roleId = req.get("roleId");
			if (roleId.isEmpty())
				throw new StopException("請先指定角色代碼");
			String sqlCntRole = " SELECT COUNT(*) FROM cmrole WHERE role_id = ?";
			if (dbu.selectIntList(sqlCntRole, roleId) == 0)
				throw new StopException("角色代碼 <" + roleId + "> 不存在");
			String suspend = "Y".equals(req.get("suspend")) ? "Y" : "N";

			String sqlQrySuspend =
				  " SELECT suspend FROM cmrole WHERE role_id = ?";
			String suspendOrg = dbu.selectStringList(sqlQrySuspend, roleId);

			String sqlUpdRole =
				  " UPDATE cmrole SET\n"
				+ "        suspend = ?\n"
				+ "  WHERE role_id = ?\n";
			dbu.executeList(sqlUpdRole, suspend, roleId);
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "setSuspend");
            operLog.add("roleId", roleId);
            operLog.add("suspend", suspendOrg, suspend);
            operLog.write();;
			
            res.put("success", true);
			res.put("status", "設定角色是否停用完成");
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

	@RequestMapping(value = "/MntRole_delRole", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> delRole(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			
			String roleId = req.get("roleId");
			if (roleId.isEmpty())
				throw new StopException("請先指定角色代碼");
			String sqlCntRole = " SELECT COUNT(*) FROM cmrole WHERE role_id = ?";
			if (dbu.selectIntList(sqlCntRole, roleId) == 0)
				throw new StopException("角色代碼 <" + roleId + "> 不存在");
			
			String sqlDelRole =
				  " DELETE FROM cmrole WHERE role_id = ?";
			dbu.executeList(sqlDelRole, roleId);
			String sqlDelPriv =
				  " DELETE FROM cmpriv WHERE acc_type = 'R' AND acc_id = ?";
			dbu.executeList(sqlDelPriv, roleId);
			String sqlDelUser =
				  " DELETE FROM cmusrr WHERE role_id = ?";
			dbu.executeList(sqlDelUser, roleId);
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "delRole");
			operLog.add("roleId", roleId);
            operLog.write();
			
            res.put("success", true);
			res.put("status", "刪除角色完成");
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
