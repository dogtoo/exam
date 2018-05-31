package com.ron.exam.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import com.ron.exam.service.MenuSvc;
import com.ron.exam.service.UserData;
import com.ron.exam.util.DbUtil;
import com.ron.exam.util.ExceptionUtil;
import com.ron.exam.util.StdCalendar;
import com.ron.exam.util.StopException;

@EnableWebMvc
@Controller
public class MainAction {

//	private static final String c_progId = "Main";
	
	@RequestMapping(value = "/Main", method = RequestMethod.GET)
	public String execute(Model model) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());
		
		try {
			UserData ud = UserData.getUserData();
			res.put("userId", ud.getUserId());
			res.put("userName", ud.getUserName());

			try {
				Context initCtx = new InitialContext();
				res.put("titleAppend", (String) initCtx.lookup("java:/comp/env/conf/title_append"));
			}
			catch (NamingException e) {
				res.put("titleAppend", "");
			}
		}
		catch (Exception e) {
			res.put("status", ExceptionUtil.procExceptionMsg(e));
		}

		Iterator<Entry<String, Object>> resi = res.entrySet().iterator();
		while (resi.hasNext()) {
			Entry<String, Object> rese = resi.next();
			model.addAttribute(rese.getKey(), rese.getValue());
		}
		return "Main";
	}

	
	private void buildMenu(MenuSvc.MenuItem parent, Map<String, Object> menu) {
		menu.put("id", parent.menuId);
		menu.put("desc", parent.menuDesc);
		menu.put("prog", parent.progId);
		if (parent.children != null) {
			List<Map<String, Object>> subMenuList = new ArrayList<Map<String, Object>>();
			menu.put("subs", subMenuList);
			for (int i = 0; i < parent.children.size(); i++) {
				Map<String, Object> subMenu = new HashMap<String, Object>();
				subMenuList.add(subMenu);
				buildMenu(parent.children.get(i), subMenu);
			}
		}
	}
	
	@RequestMapping(value = "/Main_qryProg", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryProg(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			UserData ud = UserData.getUserData();
			Map<String, Object> menu = new HashMap<String, Object>();
			buildMenu(ud.getMenu(), menu);
			res.put("menu", menu);
			res.put("chgPass", ud.getNeedChgPass());
			res.put("status", "查詢程式權限完成");
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
	
	@RequestMapping(value = "/Main_logout", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> logout(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());
		res.put("success", false);

		DbUtil dbu = new DbUtil();
		try {
			UserData ud = UserData.getUserData();
			if (ud == null)
				throw new StopException("未完成登入，登出無效");
			UserData.removeUserData();
			
			res.put("success", true);
			res.put("status", "登出完成");
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
}
