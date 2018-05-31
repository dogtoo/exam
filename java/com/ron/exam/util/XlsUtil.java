package com.ron.exam.util;

import java.io.*;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import com.ron.exam.service.UserData;

public class XlsUtil {

	public static void generateXlsDownload(HttpServletResponse res, HSSFWorkbook book, String filename) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		book.write(bos);
		res.setContentType("application/vnd.ms-excel");
		res.setContentLength(bos.size());
		if (filename != null)
			res.setHeader("Content-Disposition", "attachment; filename=" + filename);
		else
			res.setHeader("Content-Disposition", "inline");
		book.write(res.getOutputStream());
	}
	
	public static void downloadError(HttpServletResponse res, String status) {
		try {
			UserData ud = UserData.getUserData();
			StringBuffer out = new StringBuffer();
			out.append("下載檔案失敗，原因: " + status + "\n");
			out.append("人員代碼: " + ud.getUserId() + "\n");
			out.append("人員姓名: " + ud.getUserName() + "\n");
			byte outdata[] = out.toString().getBytes("utf-8");
			res.setContentType("text/plain; charset=utf-8");
			res.setContentLength(outdata.length);
			res.getOutputStream().write(outdata);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
