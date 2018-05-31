package com.ron.exam.action;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
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
import com.ron.exam.service.ProgData;
import com.ron.exam.service.UserData;
import com.ron.exam.util.OperLog;
import com.ron.exam.util.DbUtil;
import com.ron.exam.util.ExceptionUtil;
import com.ron.exam.util.StdCalendar;
import com.ron.exam.util.StopException;

@EnableWebMvc
@Controller
public class ChgPassAction {

	private static final String c_progId = "ChgPass";
	
	@RequestMapping(value = "/ChgPass", method = RequestMethod.GET)
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
			res.put("queryHide", ProgData.c_privBaseQuery.equals(ud.getPrivBase(c_progId)) ? "disabled" : "");
			
			res.put("userId", ud.getUserId());
			res.put("chgPassDate", ud.getChgPassDate() != null ? ud.getChgPassDate().toDateString() : "");
		}
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
		return "ChgPass";
	}
	
	@RequestMapping(value = "/ChgPass_chgPass", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> chgPass(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			String orgPass = req.get("orgPass");
			if (orgPass.isEmpty())
				throw new StopException("原密碼不可以為空白");
			String newPass = req.get("newPass");
			if (newPass.isEmpty())
				throw new StopException("新密碼不可以為空白");
			String chkPass = req.get("chkPass");
			if (chkPass.isEmpty())
				throw new StopException("檢查密碼不可以為空白");
			if (!newPass.equals(chkPass))
				throw new StopException("新密碼與檢查密碼不同");
			String msg = ud.chkPass(orgPass);
			if (!UserData.c_passwordOk.equals(msg))
				throw new StopException(msg);
			
			// 設定密碼
			ud.chgPass(dbu, newPass);
			
			dbu.doCommit();
			
			OperLog oper = new OperLog(c_progId, "chgPass");
			oper.add("userId", ud.getUserId());
			oper.add("passwdDate", ud.getChgPassDate());
			oper.write();
			
			res.put("status", "修改密碼完成");
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
