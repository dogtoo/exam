package com.ron.exam.action;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.ron.exam.service.ProgData;
import com.ron.exam.service.UserData;
import com.ron.exam.util.DbUtil;
import com.ron.exam.util.ExceptionUtil;
import com.ron.exam.util.StdCalendar;
import com.ron.exam.util.StopException;

@EnableWebMvc
@Controller
public class LoginAction {
	
//	private static final String c_progId = "Login";
	
	@RequestMapping(value = { "/", "/Login" }, method = RequestMethod.GET)
	public String execute(Model model, HttpSession sess) {
		Map<String, Object> res = new HashMap<String, Object>();

		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());
		
		DbUtil dbu = new DbUtil();
		try {
			if (ProgData.getProgData() == null) {
				ProgData pd = new ProgData(dbu);
				ProgData.createProgData(pd);
			}
		}
//		catch (StopException e) {
//			res.put("status", e.getMessage());
//		}
		catch (SQLException e) {
			res.put("status", ExceptionUtil.procExceptionMsg(e));
		}
		catch (Exception e) {
			res.put("status", ExceptionUtil.procExceptionMsg(e));
			Logger.getLogger(LoginAction.class).error(e);
		}
		dbu.relDbConn();
		
		Iterator<Entry<String, Object>> resi = res.entrySet().iterator();
		while (resi.hasNext()) {
			Entry<String, Object> rese = resi.next();
			model.addAttribute(rese.getKey(), rese.getValue());
		}
		return "Login";
	}

	@RequestMapping(value = "/Login_byUrl", method = RequestMethod.GET)
	public String byUrl(Model model, @RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());
		res.put("success", "N");

		DbUtil dbu = new DbUtil();
		try {
//			String data = req.get("data");

			res.put("success", "Y");
			res.put("status", "");
		}
//		catch (StopException e) {
//			res.put("status", e.getMessage());
//		}
//		catch (SQLException e) {
//			res.put("status", DbUtil.exceptionTranslation(e));
//		}
		catch (Exception e) {
			res.put("status", ExceptionUtil.procExceptionMsg(e));
			Logger.getLogger(LoginAction.class).error(e);
		}
		dbu.relDbConn();
		
		Iterator<Entry<String, Object>> resi = res.entrySet().iterator();
		while (resi.hasNext()) {
			Entry<String, Object> rese = resi.next();
			model.addAttribute(rese.getKey(), rese.getValue());
		}
		return "Login";
	}
	
	@RequestMapping(value = "/Login_login", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> login(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());
		res.put("success", false);

		DbUtil dbu = new DbUtil();
		try {
			if (dbu.getDbConn() == null)
				throw new StopException("無法連接資料庫");
			String userId = req.get("userId");
			if (userId.isEmpty())
				throw new StopException("請先輸入人員帳號");
			String userPass = req.get("userPass");
			if (userPass.isEmpty())
				throw new StopException("請先輸入登入密碼");

			UserData ud = new UserData(dbu, userId);
			if (!ud.doesExist())
				throw new StopException("登入失敗：帳號不存在");
			if (!ud.checkValid())
				throw new StopException("此帳號為失效帳號");
			String msg = ud.chkPass(userPass);
			if (msg.equals(UserData.c_passwordOk))
				ud.updateFailPass(true);
			else {
				ud.updateFailPass(false);
				throw new StopException("登入失敗：" + msg);
			}
			UserData.createUserData(ud);
			res.put("chgPass", ud.getNeedChgPass());
			
			res.put("success", true);
			res.put("status", "登入完成");
		}
		catch (StopException e) {
			res.put("status", e.getMessage());
		}
		catch (SQLException e) {
			res.put("status", DbUtil.exceptionTranslation(e));
		}
		catch (Exception e) {
			res.put("status", ExceptionUtil.procExceptionMsg(e));
			Logger.getLogger(LoginAction.class).error(e);
		}
		dbu.relDbConn();
		
		return res;
	}
}
