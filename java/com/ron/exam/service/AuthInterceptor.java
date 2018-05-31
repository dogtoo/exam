package com.ron.exam.service;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ron.exam.util.StdCalendar;

public class AuthInterceptor extends HandlerInterceptorAdapter {

	private static final String c_notLoginAllowProgs[] = { "Login", "Main" };
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		String url = request.getRequestURL().toString();
		int p = url.lastIndexOf('/');
		if (p < 0) {
			Logger.getLogger(AuthInterceptor.class.getName()).info("Invalid Request URL: " + url);
			return false;
		}
		String action = p + 1 == url.length() ? "Login" : url.substring(p + 1);
		p = action.indexOf('_');
		String progId = p >= 0 ? action.substring(0, p) : action;
		
		// 權限檢查
		boolean privOk = false;
		// 未登入依然可以使用的作業
		for (int i = 0; i < c_notLoginAllowProgs.length && !privOk; i++)
			if (c_notLoginAllowProgs[i].equals(progId))
				privOk = true;
		UserData ud = UserData.getUserData();
		if (!privOk && ud != null) {
			if (ud.getNeedChgPass())
				privOk = "ChgPass".equals(progId) || "Main".equals(progId);
			else {
				String privBase = ud.getPrivBase(progId);
				if (ProgData.c_privBaseMaintain.equals(privBase) || ProgData.c_privBaseQuery.equals(privBase))
					privOk = true;
			}
		}

		if (!privOk) {
			String msg;
			if (ud == null)
				msg = "連線已逾時，請重新連線";
			else if (ud.getNeedChgPass())
				msg = "請先更改密碼之後，再進行此作業";
			else
				msg = "此人員沒有使用本作業的權限";
			boolean json = false;
			if (request.getHeader("accept") != null) {
				String accepts[] = request.getHeader("accept").split("\\s*,\\s*");
				for (int i = 0; i < accepts.length && !json; i++)
					if (accepts[i].startsWith("application/json"))
						json = true;
			}
			if (json) {
				// ajax request, 需傳回 json 元件
				ObjectMapper jsonMap = new ObjectMapper();
				Map<String, Object> res = new HashMap<String, Object>();
				res.put("status", msg);
				res.put("statusTime", new StdCalendar().toTimesString());
				byte data[] = jsonMap.writeValueAsString(res).getBytes("UTF-8");
				response.setContentType("application/json; charset=UTF-8");
				response.setContentLength(data.length);
				response.getOutputStream().write(data);
			}
			else {
				// form request, 需傳回整頁的資料
				byte data[] = msg.getBytes("UTF-8");
				response.setContentType("text/plain; charset=UTF-8");
				response.setContentLength(data.length);
				response.getOutputStream().write(data);
			}
		}
	    return privOk;
	}
}
