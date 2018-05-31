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
import com.ron.exam.service.ProgData;
import com.ron.exam.service.UserData;

@EnableWebMvc
@Controller
public class MntMenuAction {

	private static final String c_progId = "MntMenu";
	
	/**
	 * 重新排列同層選單內的所有項目項次，可以特別指定某一項目插在指定位置
	 * @param dbu
	 * @param upperId
	 * @param menuId 指定插在特定位置的項目
	 * @param showOrder 指定位置，0 表示排在最後面
	 */
	private void menuReorder(DbUtil dbu, String upperId, String menuId, int showOrder) throws SQLException {
		if (showOrder == 0)
			showOrder = Integer.MAX_VALUE;
		String sqlQryMenu = " SELECT menu_id FROM cmmenu WHERE upper_id = ? ORDER BY show_order";
		String sqlUpdMenu = " UPDATE cmmenu SET show_order = ? WHERE menu_id = ?";
		int seq = 1;
		boolean added = false;
		ResultSet rsMenu = dbu.queryList(sqlQryMenu, upperId);
		while (rsMenu.next()) {
			String id = rsMenu.getString("menu_id");
			if (seq == showOrder && !added) {
				added = true;
				if (!id.equals(menuId)) {
					dbu.executeList(sqlUpdMenu, seq, menuId);
					seq++;
				}
			}
			else if (menuId.equals(id))
				continue;
			dbu.executeList(sqlUpdMenu, seq, id);
			seq++;
		}
		if (!added)
			dbu.executeList(sqlUpdMenu, seq, menuId);
		rsMenu.close();
	}
	
	@RequestMapping(value = "/MntMenu", method = RequestMethod.GET)
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
		return "MntMenu";
	}

	private List<Map<String, Object>> buildMenu(DbUtil dbu, String upperId) throws SQLException {
		List<Map<String, Object>> menu = null;
		String sqlQryMenu =
			  " SELECT menu_id, menu_desc, prog_id, show_order, remark\n"
			+ "      , (SELECT prog_desc FROM cmprog b WHERE a.prog_id = b.prog_id) prog_desc\n"
			+ "   FROM cmmenu a\n"
			+ "  WHERE upper_id = ?\n"
			+ "  ORDER BY show_order\n";
		ResultSet rsMenu = dbu.queryList(sqlQryMenu, upperId);
		while (rsMenu.next()) {
			if (menu == null)
				menu = new ArrayList<Map<String, Object>>();
			Map<String, Object> item = new HashMap<String, Object>();
			item.put("menuId", rsMenu.getString("menu_id"));
			item.put("menuDesc", rsMenu.getString("menu_desc"));
			item.put("progId", "-".equals(rsMenu.getString("prog_id")) ? "" : rsMenu.getString("prog_id"));
			item.put("progDesc", rsMenu.getString("prog_desc"));
			item.put("showOrder", rsMenu.getInt("show_order"));
			item.put("remark", DbUtil.nullToEmpty(rsMenu.getString("remark")));
			List<Map<String, Object>> children = buildMenu(dbu, rsMenu.getString("menu_id"));
			if (children != null)
				item.put("children", children);
			menu.add(item);
		}
		return menu;
	}
	
	@RequestMapping(value = "/MntMenu_qryMenuList", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryMenuList(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			List<Map<String, Object>> menu = buildMenu(dbu, "-");
			if (menu == null)
				menu = new ArrayList<Map<String, Object>>();
			res.put("menuList", menu);
			dbu.doCommit();
			
			res.put("status", "查詢選單列表完成");
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

	@RequestMapping(value = "/MntMenu_qryProgList", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryProgList(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			String sqlQrySys =
				  " SELECT code_code \"progId\", code_desc \"sysDesc\", '' \"progDesc\"\n"
				+ "   FROM cmcode\n"
				+ "  WHERE code_kind = 'SYS'\n"
				+ "  ORDER BY code_order\n";
			List<Map<String, Object>> progList = dbu.selectMapAllList(sqlQrySys);
			for (int i = 0; i < progList.size(); i++) {
				Map<String, Object> prog = progList.get(i);
				String sqlQryProg =
					  " SELECT prog_id \"progId\", prog_desc \"progDesc\", prog_menu \"progMenu\", prog_id \"sysDesc\"\n"
					+ "   FROM cmprog\n"
					+ "  WHERE sys_id = ?\n"
					+ "  ORDER BY show_order\n";
				prog.put("children", dbu.selectMapAllList(sqlQryProg, prog.get("progId")));
				prog.put("progId", "*" + prog.get("progId"));
			}
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

	@RequestMapping(value = "/MntMenu_addMenu", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addMenu(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");

			String menuId = req.get("menuId");
			String upperId = req.get("upperId");
			if (menuId.isEmpty())
				throw new StopException("請先指定選單代碼");
			if ("-".equals(menuId))
				throw new StopException("不正確的選單代碼");
			if (upperId.isEmpty())
				throw new StopException("請先指定上層選單代碼");
			String sqlCntMenu = " SELECT COUNT(*) FROM cmmenu WHERE menu_id = ?";
			if (dbu.selectIntList(sqlCntMenu, menuId) > 0)
				throw new StopException("選單代碼 <" + menuId + "> 已存在");
			if (!"-".equals(upperId) && dbu.selectIntList(sqlCntMenu, upperId) == 0)
				throw new StopException("上層選單代碼 <" + upperId + "> 不存在");
			String progId = req.get("progId");
			if (!"-".equals(progId)) {
				String sqlCntProg = " SELECT COUNT(*) FROM cmprog WHERE prog_id = ?";
				if (dbu.selectIntList(sqlCntProg, progId) == 0)
					throw new StopException("程式代碼 <" + progId + "> 不存在");
			}

			int showOrder = 0;
			try {
				if (!req.get("showOrder").isEmpty())
					showOrder = Integer.parseInt(req.get("showOrder"));
			}
			catch (NumberFormatException e) {
				throw new StopException("選單順序必須為數字");
			}
			String menuDesc = req.get("menuDesc");
			if (menuDesc.isEmpty())
				throw new StopException("請輸入選單說明");
			String remark = DbUtil.emptyToNull(req.get("remark"));

			String sqlInsMenu =
				  " INSERT INTO cmmenu(menu_id, menu_desc, prog_id, upper_id, show_order, remark)\n"
				+ " VALUES(?, ?, ?, ?, 0, ?)";
			dbu.executeList(sqlInsMenu, menuId, menuDesc, progId, upperId, remark);
			menuReorder(dbu, upperId, menuId, showOrder);
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "addMenu");
            operLog.add("menuId", menuId);
            operLog.add("menuDesc", menuDesc);
            operLog.add("progId", progId);
            operLog.add("upperId", upperId);
            operLog.add("showOrder", showOrder);
            operLog.add("remark", remark);
            operLog.write();

            res.put("success", true);
			res.put("status", "新增選單完成");
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

	@RequestMapping(value = "/MntMenu_modMenu", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> modParam(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			
			String menuIdOrg = req.get("menuIdOrg");
			String menuId = req.get("menuId");
			if ("-".equals(menuId))
				throw new StopException("不正確的選單代碼");
			String sqlCntMenu = " SELECT COUNT(*) FROM cmmenu WHERE menu_id = ?";
			if (dbu.selectIntList(sqlCntMenu, menuIdOrg) == 0)
				throw new StopException("原選單代碼 <" + menuIdOrg + "> 不存在");
			if (!menuId.equals(menuIdOrg) && dbu.selectIntList(sqlCntMenu, menuId) > 0)
				throw new StopException("選單代碼 <" + menuId + "> 已存在");
			String progId = req.get("progId");
			if (!"-".equals(progId)) {
				String sqlCntProg = " SELECT COUNT(*) FROM cmprog WHERE prog_id = ?";
				if (dbu.selectIntList(sqlCntProg, progId) == 0)
					throw new StopException("程式代碼 <" + progId + "> 不存在");
			}
			int showOrder = 0;
			try {
				showOrder = Integer.parseInt(req.get("showOrder"));
			}
			catch (NumberFormatException e) {
				throw new StopException("無效的顯示順序");
			}
			String menuDesc = req.get("menuDesc");
			if (menuDesc.isEmpty())
				throw new StopException("請輸入選單說明");
			String remark = DbUtil.emptyToNull(req.get("remark"));
			
			String sqlQryMenu =
				  " SELECT menu_desc, prog_id, upper_id, show_order, remark\n"
				+ "   FROM cmmenu\n"
				+ "  WHERE menu_id = ?\n";
			Map<String, Object> rowMenu = dbu.selectMapRowList(sqlQryMenu, menuIdOrg);
			if (rowMenu == null)
				throw new StopException("原選單代碼 <" + menuIdOrg + "> 不存在");
			String menuDescOrg = (String) rowMenu.get("menu_desc");
			String progIdOrg = (String) rowMenu.get("prog_id");
			String upperIdOrg = (String) rowMenu.get("upper_id");
			int showOrderOrg = (Integer) rowMenu.get("show_order");
			String remarkOrg = (String) rowMenu.get("remark");
			
			String sqlUpdMenu =
				  " UPDATE cmmenu SET\n"
				+ "        menu_id = ?\n"
				+ "      , menu_desc = ?\n"
				+ "      , prog_id = ?\n"
				+ "      , remark = ?\n"
				+ "  WHERE menu_id = ?\n";
			dbu.executeList(sqlUpdMenu, menuId, menuDesc, progId, remark, menuIdOrg);
			menuReorder(dbu, upperIdOrg, menuId, showOrder);
			if (!menuId.equals(menuIdOrg)) {
				// 如果選單代碼變了，一並調整引用到的 upper_id
				String sqlUpdUpper =
					  " UPDATE cmmenu SET\n"
					+ "        upper_id = ?\n"
					+ "  WHERE upper_id = ?\n";
				dbu.executeList(sqlUpdUpper, menuId, menuIdOrg);
				
				if ("-".equals(upperIdOrg)) {
					String sqlUpdUser =
						  " UPDATE cmuser SET\n"
						+ "        menu_id = ?\n"
						+ "  WHERE menu_id = ?\n";
					dbu.executeList(sqlUpdUser, menuId, menuIdOrg);
				}
			}
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "modMenu");
			if (menuId.equals(menuIdOrg))
				operLog.add("menuId", menuId);
			else
				operLog.add("menuId", menuIdOrg, menuId);
            operLog.add("menuDesc", menuDescOrg, menuDesc);
            operLog.add("progId", progIdOrg, progId);
            operLog.add("showOrder", showOrderOrg, showOrder);
            operLog.add("remark", remarkOrg, remark);
            operLog.write();
			
            res.put("success", true);
			res.put("status", "修改選單完成");
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


	@RequestMapping(value = "/MntMenu_delMenu", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> delParam(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			
			String menuId = req.get("menuId");
			String sqlQryMenu = " SELECT upper_id FROM cmmenu WHERE menu_id = ?";
			String upperId = dbu.selectStringList(sqlQryMenu, menuId);
			if (upperId == null)
				throw new StopException("選單代碼 <" + menuId + "> 不存在");
			String sqlCntMenuUp = " SELECT COUNT(*) FROM cmmenu WHERE upper_id = ?";
			if (dbu.selectIntList(sqlCntMenuUp, menuId) > 0)
				throw new StopException("選單代碼 <" + menuId + "> 被其它選單使用中，不可刪除");
			String sqlCntUser = " SELECT COUNT(*) FROM cmuser WHERE menu_id = ?";
			if ("-".equals(upperId) && dbu.selectIntList(sqlCntUser, menuId) > 0)
				throw new StopException("選單代碼 <" + menuId + "> 正被帳號使用中，不可刪除");

			String sqlDelMenu =
				  " DELETE FROM cmmenu WHERE menu_id = ?";
			dbu.executeList(sqlDelMenu, menuId);
			menuReorder(dbu, upperId, menuId, 0);
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "delMenu");
			operLog.add("menuId", menuId);
            operLog.write();;
			
            res.put("success", true);
			res.put("status", "刪除選單完成");
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
