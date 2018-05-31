package com.ron.exam.service;

import java.sql.*;
import java.util.*;
import com.ron.exam.util.DbUtil;

public class MenuSvc {

	public static class MenuItem {
		public String menuId;
		public String menuDesc;
		public String progId;			// 如果沒有指定呼叫程式，則為 "-"
		public List<MenuItem> children;	// 如果沒有子選單，則為 null
	}
	
	private static void loadMenuTree(DbUtil dbu, String menuId, MenuItem menu) throws SQLException {
		String sqlQryMenu =
			  " SELECT menu_id, menu_desc, prog_id\n"
			+ "   FROM cmmenu\n"
			+ "  WHERE upper_id = ?\n"
			+ "  ORDER BY show_order\n";
		ResultSet rsMenu = dbu.queryList(sqlQryMenu, menuId);
		while (rsMenu.next()) {
			if (menu.children == null)
				menu.children = new ArrayList<MenuItem>();
			MenuItem child = new MenuItem();
			child.menuId = rsMenu.getString("menu_id");
			child.menuDesc = rsMenu.getString("menu_desc");
			child.progId = rsMenu.getString("prog_id");
			menu.children.add(child);
			loadMenuTree(dbu, child.menuId, child);
		}
		rsMenu.close();
	}
	
	public static void loadMenu(DbUtil dbu, String menuId, MenuItem menu) throws SQLException {
		menu.children = null;
		String sqlQryMenu =
			  " SELECT menu_id, menu_desc, prog_id\n"
			+ "   FROM cmmenu\n"
			+ "  WHERE menu_id = ?\n";
		Map<String, Object> rowMenu = dbu.selectMapRowList(sqlQryMenu, menuId);
		if (rowMenu != null) {
			menu.menuId = (String) rowMenu.get("menu_id");
			menu.menuDesc = (String) rowMenu.get("menu_desc");
			menu.progId = (String) rowMenu.get("prog_id");
			loadMenuTree(dbu, menuId, menu);
		}
	}
}
