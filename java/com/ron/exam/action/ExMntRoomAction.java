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
import com.ron.exam.service.MiscTool;
import com.ron.exam.service.ProgData;
import com.ron.exam.service.UserData;

@EnableWebMvc
@Controller
public class ExMntRoomAction {

	private static final String c_progId = "ExMntRoom";
	
	private static class EqpData {
		public String eqpType;
		public String eqpId;
		public String eqpName;
		public String fileName;
		public String eqpConfig;
		public String remark;
		
		public static String stringify(List<EqpData> eqpList) {
			StringBuffer str = new StringBuffer();
			for (int i = 0; i < eqpList.size(); i++) {
				EqpData eqp = eqpList.get(i);
				if (i > 0)
					str.append(',');
				str.append('<');
				str.append(eqp.eqpType);
				str.append(',');
				str.append(eqp.eqpId);
				str.append(',');
				str.append(eqp.eqpName);
				str.append(',');
				str.append(eqp.fileName);
				str.append(',');
				str.append('[');
				str.append(eqp.eqpConfig);
				str.append(']');
				str.append(',');
				str.append(eqp.remark);
				str.append('>');
			}
			return str.toString();
		}
	}
	
	@RequestMapping(value = "/ExMntRoom", method = RequestMethod.GET)
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

			res.put("eqpTypeList", CodeSvc.buildSelectOptionByKind(dbu, CodeSvc.c_kindExEquipType, false));
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
		
		Iterator<Entry<String, Object>> resi = res.entrySet().iterator();
		while (resi.hasNext()) {
			Entry<String, Object> rese = resi.next();
			model.addAttribute(rese.getKey(), rese.getValue());
		}
		return "ExMntRoom";
	}

	@RequestMapping(value = "/ExMntRoom_qryRoomList", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryRoomList(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success",  false);
			String mode = req.get("mode");
			int pageRow = Integer.MAX_VALUE;
			int pageAt = 0;
			try {
				pageRow = Integer.parseInt(req.get("pageRow"));
				if (pageRow <= 0)
					pageRow = Integer.MAX_VALUE;
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
			if ("N".equals(mode))
				pageAt = 0;
			
			StringBuffer sqlQryRoom = new StringBuffer(
				  " SELECT room_id, room_name, room_desc, suspend, remark\n");
			StringBuffer sqlCntRoom = new StringBuffer(" SELECT COUNT(*)");
			StringBuffer sqlCond = new StringBuffer(
				  "   FROM exroom \n"
				+ "  WHERE 1 = 1 \n");
			
			List<Object> params = new ArrayList<Object>();
			
			//診間代碼
			String qryRoomId = req.get("roomId");
			if (!qryRoomId.isEmpty()) {
				sqlCond.append(" AND room_id like ?||'%'\n");
				params.add(qryRoomId);
			}
			
			//診間名稱
			String qryRoomName = req.get("roomName");
			if (!qryRoomName.isEmpty()) {
				sqlCond.append(" AND room_name like ?||'%'\n");
				params.add(qryRoomName);
			}
						
			//診間說明
			String qryRoomDesc = req.get("roomDesc");
			if (!qryRoomDesc.isEmpty()) {
				sqlCond.append(" AND room_desc like '%'||?||'%'\n");
				params.add(qryRoomDesc);
			}
			
			// 非換頁查詢時，需先查詢總筆數
			if ("N".equals(mode) || "T".equals(mode)) {
				sqlCntRoom.append(sqlCond);
				res.put("total", dbu.selectIntArray(sqlCntRoom.toString(), params.toArray()));
			}
			
			sqlQryRoom.append(sqlCond);
			StringBuffer sqlOrder = new StringBuffer();
			Map<String, String> orderMap = new HashMap<String, String>() {
				private static final long serialVersionUID = 1l;
				{	put("roomId:A", "room_id ASC");
					put("roomId:D", "room_id DESC");
					put("roomName:A", "room_name ASC");
					put("roomName:D", "room_name DESC");
				}
			};
			for (int i = 0; ; i++) {
				String key = "order[" + i + "]";
				if (!req.containsKey(key))
					break;
				String order = req.get(key);
				if (!orderMap.containsKey(order))
					break;
				if (i == 0)
					sqlOrder.append(" ORDER BY ");
				else
					sqlOrder.append(", ");
				sqlOrder.append(orderMap.get(order));
			}
			sqlQryRoom.append(sqlOrder);
			if (pageRow != 0) {
				sqlQryRoom.append(" OFFSET ? LIMIT ?\n");
				params.add(pageAt * pageRow);
				params.add(pageRow);
			}

			ResultSet rsRoom = dbu.queryArray(sqlQryRoom.toString(), params.toArray());
			List<Map<String, Object>> qsList = new ArrayList<Map<String, Object>>();
			while (rsRoom.next()) {
				Map<String, Object> room = new HashMap<String, Object>();
				room.put("roomId", rsRoom.getString("room_id"));
				room.put("roomName", rsRoom.getString("room_name"));
				room.put("roomDesc", rsRoom.getString("room_desc"));
				room.put("suspend", "Y".equals(rsRoom.getString("suspend")) ? "是" : "");
				qsList.add(room);
			}
			rsRoom.close();
			res.put("roomList", qsList);
			
			res.put("success", true);
			res.put("status", "查詢診間列表完成");
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

	@RequestMapping(value = "/ExMntRoom_qryRoom", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> qryRoom(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
			res.put("success", false);
			
			String roomId = req.get("roomId");
			if (roomId != null) {
				String sqlQryRoom =
						  " SELECT room_id, room_name, room_desc, suspend, remark\n"
						+ "   FROM exroom \n"
						+ "  WHERE room_id = ?\n";

					Map<String, Object> rowQs = dbu.selectMapRowList(sqlQryRoom, roomId);
					if (rowQs == null)
						throw new StopException("診間代碼 <" + roomId + "> 不存在");
					res.put("roomName", rowQs.get("room_name"));
					res.put("roomDesc", rowQs.get("room_desc"));
					res.put("roomName", rowQs.get("room_name"));
					res.put("suspend", rowQs.get("suspend"));
					res.put("remark", rowQs.get("remark"));

					String sqlQryEqp = 
							" SELECT eqp_id, eqp_type, eqp_name, eqp_config, file_name, remark\n"
						  + "   FROM exreqm a WHERE room_id = ? ORDER BY eqp_id";
					Map<String, String> typeMap = CodeSvc.buildStringMapByKind(dbu, CodeSvc.c_kindExEquipType);
					List<Object> paramsu = new ArrayList<Object>();
					paramsu.add(roomId);
					ResultSet rsEqp = dbu.queryArray(sqlQryEqp.toString(), paramsu.toArray());			
					List<Map<String, Object>> eqpList = new ArrayList<Map<String, Object>>();
					while (rsEqp.next()) {					
						Map<String, Object> eqp = new HashMap<String, Object>();
						eqp.put("eqpTypeDesc",  typeMap.get(rsEqp.getString("eqp_type")));
						eqp.put("eqpId", rsEqp.getString("eqp_id"));
						eqp.put("eqpName", rsEqp.getString("eqp_name"));
						eqp.put("eqpConfig", rsEqp.getString("eqp_config"));
						eqp.put("fileName", rsEqp.getString("file_name"));
						eqp.put("eqpType", rsEqp.getString("eqp_type"));
						eqp.put("remark", rsEqp.getString("remark"));
						eqpList.add(eqp);
					}
					rsEqp.close();
					res.put("eqpList", eqpList);
			}
			
			dbu.doCommit();
			
			res.put("success", true);
			res.put("status", "查詢診間資料完成");
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

	@RequestMapping(value = "/ExMntRoom_addRoom", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addRoom(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");

			String msg;
			String roomId = req.get("roomId");
			String sqlCntRoom = " SELECT COUNT(*) FROM exroom WHERE room_id = ?";
			if (dbu.selectIntList(sqlCntRoom, roomId) > 0)
				throw new StopException("診間代碼 '" + roomId + "' 已存在");
			if ((msg = MiscTool.checkIdString(roomId, 10)) != null)
				throw new StopException("診間代碼" + msg);
			String roomName = req.get("roomName");
			if (roomName.isEmpty())
				throw new StopException("診間名稱不可以為空白");
			String roomDesc = DbUtil.emptyToNull(req.get("roomDesc"));
			String suspend = "Y".equals(req.get("suspend")) ? "Y" : null;
			String remark = DbUtil.emptyToNull(req.get("remark"));
			Map<String, String> eqpTypeMap = CodeSvc.buildStringMapByKind(dbu, CodeSvc.c_kindExEquipType);
			List<EqpData> eqpList = new ArrayList<EqpData>();
			for (int i = 0; ; i++) {
				EqpData eqp = new EqpData();
				String key = "eqpType[" + i + "]";
				if (!req.containsKey(key))
					break;
				eqp.eqpType = req.get(key);
				if (!eqpTypeMap.containsKey(eqp.eqpType))
					throw new StopException("不正確的設備型態 '" + eqp.eqpType + "'");
				eqp.eqpId = req.get("eqpId[" + i + "]");
				if ((msg = MiscTool.checkIdString(eqp.eqpId, 10)) != null)
					throw new StopException("設備代碼 '" + eqp.eqpId + "' " + msg);
				eqp.eqpName = req.get("eqpName[" + i + "]");
				if (eqp.eqpName == null || eqp.eqpName.isEmpty())
					throw new StopException("設備名稱不可以為空白");
				eqp.fileName = DbUtil.emptyToNull(req.get("fileName[" + i + "]"));
				eqp.eqpConfig = DbUtil.emptyToNull(req.get("eqpConfig[" + i + "]"));
				eqp.remark = DbUtil.emptyToNull(req.get("remark[" + i + "]"));
				eqpList.add(eqp);
			}
			
			String sqlInsRoom =
				  " INSERT INTO exroom(room_id, room_name, room_desc, suspend, remark)\n"
				+ " VALUES(?, ?, ?, ?, ?)\n";
			dbu.executeList(sqlInsRoom, roomId, roomName, roomDesc, suspend, remark);
			String sqlInsEqp =
					  " INSERT INTO exreqm(room_id, eqp_type, eqp_id, eqp_name, file_name, eqp_config, remark)\n"
					+ " VALUES(?, ?, ?, ?, ?, ?, ?)\n";
			for (int i = 0; i < eqpList.size(); i++) {
				EqpData eqp = eqpList.get(i);
				dbu.executeList(sqlInsEqp, roomId, eqp.eqpType, eqp.eqpId, eqp.eqpName, eqp.fileName, eqp.eqpConfig, eqp.remark);
			}
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "addRoom");
			operLog.add("roomId", roomId);
			operLog.add("roomName", roomName);
			operLog.add("roomDesc", roomDesc);
			operLog.add("suspend", suspend);
			operLog.add("remark", remark);
			operLog.add("eqpList", EqpData.stringify(eqpList));
            operLog.write();
			
            res.put("success", true);
			res.put("status", "新增診間完成");
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

	@RequestMapping(value = "/ExMntRoom_modRoom", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> modRoom(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");
			
			String roomIdOrg = req.get("roomIdOrg");
			if ("".equals(roomIdOrg))
				throw new StopException("請先查詢診間後再修改");
			String roomId = req.get("roomId");
			if ("".equals(roomId))
				throw new StopException("請先指定診間代碼");
			
			String msg;
			String sqlCntRoom = " SELECT COUNT(*) FROM exroom WHERE room_id = ?";
			if (dbu.selectIntList(sqlCntRoom, roomIdOrg) == 0)
				throw new StopException("原診間代碼 '" + roomIdOrg + "' 不存在");
			if (!roomId.equals(roomIdOrg) && dbu.selectIntList(sqlCntRoom, roomId) > 0)
				throw new StopException("診間代碼 '" + roomId + "' 已存在");
			if ((msg = MiscTool.checkIdString(roomId, 10)) != null)
				throw new StopException("診間代碼" + msg);
			
			String roomName = req.get("roomName");
			if (roomName.isEmpty())
				throw new StopException("診間名稱不可以為空白");
			String roomDesc = DbUtil.emptyToNull(req.get("roomDesc"));
			String suspend = "Y".equals(req.get("suspend")) ? "Y" : null;
			String remark = DbUtil.emptyToNull(req.get("remark"));
			Map<String, String> eqpTypeMap = CodeSvc.buildStringMapByKind(dbu, CodeSvc.c_kindExEquipType);
			List<EqpData> eqpList = new ArrayList<EqpData>();
			for (int i = 0; ; i++) {
				EqpData eqp = new EqpData();
				String key = "eqpType[" + i + "]";
				if (!req.containsKey(key))
					break;
				eqp.eqpType = req.get(key);
				if (!eqpTypeMap.containsKey(eqp.eqpType))
					throw new StopException("不正確的設備型態 '" + eqp.eqpType + "'");
				eqp.eqpId = req.get("eqpId[" + i + "]");
				if ((msg = MiscTool.checkIdString(eqp.eqpId, 10)) != null)
					throw new StopException("設備代碼 '" + eqp.eqpId + "' " + msg);
				eqp.eqpName = req.get("eqpName[" + i + "]");
				if (eqp.eqpName == null || eqp.eqpName.isEmpty())
					throw new StopException("設備名稱不可以為空白");
				eqp.fileName = DbUtil.emptyToNull(req.get("fileName[" + i + "]"));
				eqp.eqpConfig = DbUtil.emptyToNull(req.get("eqpConfig[" + i + "]"));
				eqp.remark = DbUtil.emptyToNull(req.get("remark[" + i + "]"));
				eqpList.add(eqp);
			}
			
			String sqlInsRoom =
				  " UPDATE exroom SET\n"
				+ "        room_id = ?\n"
				+ "      , room_name = ?\n"
				+ "      , room_desc = ?\n"
				+ "      , suspend = ?\n"
				+ "      , remark = ?\n"
				+ "  WHERE room_id = ?\n";
			dbu.executeList(sqlInsRoom, roomId, roomName, roomDesc, suspend, remark, roomIdOrg);

			String sqlDelEqp = "DELETE FROM exreqm WHERE room_id = ?";
			dbu.executeList(sqlDelEqp, roomIdOrg);
			String sqlInsEqp =
					  " INSERT INTO exreqm(room_id, eqp_type, eqp_id, eqp_name, file_name, eqp_config, remark)\n"
					+ " VALUES(?, ?, ?, ?, ?, ?, ?)\n";
			for (int i = 0; i < eqpList.size(); i++) {
				EqpData eqp = eqpList.get(i);
				dbu.executeList(sqlInsEqp, roomId, eqp.eqpType, eqp.eqpId, eqp.eqpName, eqp.fileName, eqp.eqpConfig, eqp.remark);
			}
			
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "modRoom");
			operLog.add("roomId", roomId);
			operLog.add("roomName", roomName);
			operLog.add("roomDesc", roomDesc);
			operLog.add("suspend", suspend);
			operLog.add("remark", remark);
			operLog.add("eqpList", EqpData.stringify(eqpList));
            operLog.write();
			
            res.put("success", true);
			res.put("status", "修改診間完成");
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

	@RequestMapping(value = "/ExMntRoom_delRoom", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> delRoom(@RequestParam Map<String, String> req) {
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("status", "");
		res.put("statusTime", new StdCalendar().toTimesString());

		DbUtil dbu = new DbUtil();
		try {
            res.put("success", false);
			UserData ud = UserData.getUserData();
			if (!ProgData.c_privBaseMaintain.equals(ud.getPrivBase(c_progId)))
				throw new StopException("您無權限執行此動作");

			String roomId = req.get("roomId");
			if ("".equals(roomId))
				throw new StopException("請先指定診間代碼");
			
			String sqlDelRoom = " DELETE FROM exroom WHERE room_id = ?";
			dbu.executeList(sqlDelRoom, roomId);
			
			String sqlDelEqp = " DELETE FROM exreqm WHERE room_id = ?";
			dbu.executeList(sqlDelEqp, roomId);
			
			dbu.doCommit();
			
			OperLog operLog = new OperLog(c_progId, "delRoom");
			operLog.add("roomId", roomId);
            operLog.write();;
			
            res.put("success", true);
			res.put("status", "刪除診間完成");
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
