<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title></title>
<link rel="stylesheet" type="text/css" href="css/easyui.css" />
<link rel="stylesheet" type="text/css" href="css/exam.css" />
<script type="text/javascript" src="js/jquery-1.12.4.js"></script>
<script type="text/javascript" src="js/jquery.easyui.min.js"></script>
<script type="text/javascript" src="js/easyui-lang-zh_TW.js"></script>
<style>
#qryRoomId + .textbox .textbox-text {
	text-transform: uppercase;
}
#editRoomId + .textbox .textbox-text {
	text-transform: uppercase;
}

#eqpId + .textbox .textbox-text {
	text-transform: uppercase;
}

</style>
<script type="text/javascript">
function chgOrder(nfld, ofld) {
	var jorder = $("#order");
	if (jorder.hasClass("inprog"))
		return;
	var sel = jorder.combobox("getValues");
	var fld;
	if (ofld) {
		if (nfld.length > ofld.length)
			fld = nfld[nfld.length - 1];
	}
	else if ($(nfld).hasClass("orderField")) {
		var fldName = $(nfld).next().attr("order");
		fld = fldName + ":A";
		for (var i = sel.length - 1; i >= 0; i--) {
			var p = sel[i].indexOf(':');
			if (sel[i].substring(0, p) == fldName) {
				fld = fldName + ":" + (sel[i].substring(p + 1) == "A" ? "D" : "A");
				break;
			}
		}
	}
	if (fld) {
		var fkey = fld.substring(0, fld.indexOf(':'));
		for (var i = sel.length - 1; i >= 0; i--) {
			skey = sel[i].substring(0, sel[i].indexOf(':'));
			if (skey == fkey)
				sel.splice(i, 1);
		}
		sel.unshift(fld);
	}
	var mark = {};
	for (var i = 0; i < sel.length; i++) {
		var p = sel[i].indexOf(':');
		var skey = sel[i].substring(0, p);
		mark[skey] = sel[i].substring(p + 1) == "A" ? "&#x25B2;" : "&#x25BC;";
	}
	var jmarks = $("table.listHead span.orderMark");
	for (var i = 0; i < jmarks.length; i++) {
		var jmark = $(jmarks[i]);
		var skey = jmark.attr("order");
		jmark.html(skey in mark ? mark[skey] : "&nbsp;");
	}
	jorder.addClass("inprog");
	jorder.combobox("setValues", sel);
	jorder.removeClass("inprog");
	if ($(nfld).hasClass("orderField"))
		qryRoomList();
}

function clearAll() {
	$("#qryRoomId").textbox("setValue", "");
	$("#qryRoomName").textbox("setValue", "");
	$("#qryRoomDesc").textbox("setValue", "");
	$("#order").combobox("setValues", [ "roomId:A" ]);
	chgOrder();
	$("#pg").pagination({ total: 0, pageSize: 10});
	$("#roomList tr").remove();
	delete window.queryCond1;
}


/*
 * 查詢診間
 */
function qryRoomList(mode) {
	var req = {};
	req.mode = mode;
	if (mode == "N") {
		$("#pg").pagination({ total: 1, pageNumber: 1 });
		var queryCond1 = {};
		queryCond1._job = 'ExMntRoom';
		queryCond1.roomId = $("#qryRoomId").textbox("getValue").toUpperCase();
		queryCond1.roomName = $("#qryRoomName").textbox("getValue");
		queryCond1.roomDesc = $("#qryRoomDesc").textbox("getValue");
		window.queryCond1 = queryCond1;
	}
	else {
		if (!window.queryCond1 || window.queryCond1._job != 'ExMntRoom')
			return;
	}
	for (var i in window.queryCond1)
		if (i != "_job")
			req[i] = window.queryCond1[i];
	var orders = $("#order").combobox("getValues");
	for (var i = 0; i < orders.length; i++)
		req["order[" + i + "]"] = orders[i];
	req.pageRow = $("#pg").pagination("options").pageSize;
	req.pageAt = $("#pg").pagination("options").pageNumber;
	$.post("ExMntRoom_qryRoomList", req, function (res) {
		if (mode != "S" && mode != "T")
			parent.showStatus(res);
		if (res.success) {
			if ('total' in res)
				$("#pg").pagination({ total: res.total });
			var queryHide = $("#queryHide").val();
			var jroomList = $("#roomList");
			jroomList.find("tr").remove();
			for (var i = 0; i < res.roomList.length; i++) {
				var ro = res.roomList[i];
				var append = "<tr class='" + (i & 1 ? "odd" : "even") + "'>\n" +
					"<td>\n" +
					"    <input name='roomId' type='text' style='width: 100px;' readonly value='" + ro.roomId + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='roomName' type='text' style='width: 250px;' readonly value='" + ro.roomName + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='roomDesc' type='text' style='width: 250px;' readonly value='" + ro.roomDesc + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='suspend' type='text' style='width: 100px;' readonly value='" + ro.suspend + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <span style='width: 160px; display: inline-block; text-align: center;'>\n" +
					"        <button type='button' style='width: 70px;" + queryHide + "' onclick='modRoom(this);'>編輯</button>\n" +
					"        &nbsp;&nbsp;<button type='button' style='width: 70px;" + queryHide + "' onclick='delRoom(this);'>刪除</button>\n" +
					"    </span>\n" +
					"</td>\n" +
					"</tr>\n";
				jroomList.append(append);
				var jrow = jroomList.find("tr:last");
			}
		}
	}, "json");
}

function addRoom() {
	var req = {};
	$.post("ExMntRoom_qryRoom", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#roomEdit").dialog("open");
			$("#roomEditMode").val("A");
			$("#roomEdit .modPane").css("visibility", "hidden");
			$("#editRoomId").textbox("setValue", "");
			$("#editRoomName").textbox("setValue", "");
			$("#editRoomDesc").textbox("setValue", "");
			$("#editSuspend").switchbutton("uncheck");
			$("#editRemark").textbox("setValue", "");
			$("#editEqpList").datagrid("loadData", []);
			$("#eqpType").combobox("select", $("#eqpType").combobox("getData")[0].value);
			$("#eqpId").textbox("setValue", "");
			$("#eqpName").textbox("setValue", "");
			$("#fileName").textbox("setValue", "");
			$("#eqpConfig").textbox("setValue", "");
			$("#eqpRemark").textbox("setValue", "");
		}
	}, "json");
}

function modRoom(src) {
	var jrow = $(src).parent().parent().parent();
	var req = {};
	req.roomId = jrow.find("[name='roomId']").val();
	$.post("ExMntRoom_qryRoom", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#roomEdit").dialog("open");
			$("#roomEditMode").val("M");
			$("#roomEdit .modPane").css("visibility", "visible");
			$("#editRoomIdOrg").textbox("setValue", req.roomId);
			$("#editRoomId").textbox("setValue", req.roomId);
			$("#editRoomName").textbox("setValue", res.roomName);
			$("#editRoomDesc").textbox("setValue", res.roomDesc);
			$("#editSuspend").switchbutton(res.suspend ? "check" : "uncheck");
			$("#editRemark").textbox("setValue", res.remark);
			$("#editEqpList").datagrid("loadData", res.eqpList);
			$("#eqpType").combobox("select", $("#eqpType").combobox("getData")[0].value);
			$("#eqpId").textbox("setValue", "");
			$("#eqpName").textbox("setValue", "");
			$("#fileName").textbox("setValue", "");
			$("#eqpConfig").textbox("setValue", "");
			$("#eqpRemark").textbox("setValue", "");
		}
	}, "json");
}

function roomEditClose() {
	$("#roomList tr.select").removeClass("select");
}

function roomEditDone() {
	var req = {};
	req.roomIdOrg = $("#editRoomIdOrg").textbox("getValue");
	req.roomId = $("#editRoomId").textbox("getValue").toUpperCase();
	req.roomName = $("#editRoomName").textbox("getValue");
	req.roomDesc = $("#editRoomDesc").textbox("getValue");
	req.suspend = $("#editSuspend").switchbutton("options").checked ? "Y" : "";
	req.remark = $("#editRemark").textbox("getValue");
	var eqpList = $("#editEqpList").datagrid("getData").rows;
	for (var i = 0; i < eqpList.length; i++) {
		var eqp = eqpList[i];
		req["eqpType[" + i + "]"] = eqp.eqpType;
		req["eqpId[" + i + "]"] = eqp.eqpId;
		req["eqpName[" + i + "]"] = eqp.eqpName;
		req["fileName[" + i + "]"] = eqp.fileName;
		req["eqpConfig[" + i + "]"] = eqp.eqpConfig;
		req["remark[" + i + "]"] = eqp.remark;
	}
	if ($("#roomEditMode").val() == "A") {
		$.post("ExMntRoom_addRoom", req, function (res) {
			parent.showStatus(res);
			if (res.success) {
				$("#roomEdit").dialog("close");
				qryRoomList("T");
			}
		}, "json");
	}
	else if ($("#roomEditMode").val() == "M") {
		req.roomIdOrg = $("#editRoomIdOrg").textbox("getValue");
		$.post("ExMntRoom_modRoom", req, function (res) {
			parent.showStatus(res);
			if (res.success) {
				$("#roomEdit").dialog("close");
				qryRoomList("S");
			}
		}, "json");
	}
}

function delRoom(src) {
	var jrow = $(src).parent().parent().parent();
	$("#roomList tr.select").removeClass("select");
	jrow.addClass("select");
	var req = {};
	req.roomId = jrow.find("[name='roomId']").val();
	$.messager.confirm({
		title: '確認',
		msg: '是否要刪除診間資料？',
		ok: '刪除',
		cancel: '取消',
		fn: function (ok) {
			$("#roomList tr.select").removeClass("select");
			if (ok) {
				$.post("ExMntRoom_delRoom", req, function (res) {
					parent.showStatus(res);
					if (res.success) {
						qryRoomList("T");
					}
				}, "json");
			}
		}
	});
}

function editEqpListFmt(value, row, index) {
	return "<button type='button' style='width: 120px;' onclick='editEqpListDel(this);' >刪除</button>" +
		"<input type='hidden' name='eqpType' value='" + row.eqpType + "' />" +
		"<input type='hidden' name='eqpId' value='" + row.eqpId + "' />";
}

function editEqpListDel(src) {
	var eqpType = $(src).next().val();
	var eqpId = $(src).next().next().val();
	var rows = $("#editEqpList").datagrid("getData").rows;
	for (var i = 0; i < rows.length; i++) {
		var row = rows[i];
		if (eqpType == row.eqpType && eqpId == row.eqpId) {
			$("#editEqpList").datagrid("deleteRow", i);
			break;
		}
	}
}

function addModEqp() {
	var eqpType = $("#eqpType").combobox("getValue");
	var eqpId = $("#eqpId").textbox("getValue").toUpperCase();
	if (eqpId == "") {
		parent.showStatus({ status: '設備代碼不可以為空白' });
		return
	}
	var eqpTypeDesc = $("#eqpType").combobox("getText");
	var eqpName = $("#eqpName").textbox("getValue");
	var fileName = $("#fileName").textbox("getValue");
	var eqpConfig = $("#eqpConfig").textbox("getValue");
	var remark = $("#eqpRemark").textbox("getValue");
	var nrow = { eqpType: eqpType, eqpId:eqpId, eqpName: eqpName, fileName: fileName, eqpTypeDesc: eqpTypeDesc, eqpConfig: eqpConfig, remark: remark };
	var rows = $("#editEqpList").datagrid("getData").rows;
	var at;
	for (at = rows.length - 1; at >= 0; at--) {
		var row = rows[at];
		if (eqpType == row.eqpType && eqpId == row.eqpId)
			break;
	}
	if (at >= 0)
		$("#editEqpList").datagrid("updateRow", { index: at, row: nrow });
	else
		$("#editEqpList").datagrid("appendRow", nrow);
}

function selEqpRow(index,row){
	$("#eqpType").combobox("setValue", row.eqpType);
	$("#eqpId").textbox("setValue", row.eqpId);
	$("#eqpName").textbox("setValue", row.eqpName);
	$("#fileName").textbox("setValue", row.fileName);
	$("#eqpConfig").textbox("setValue", row.eqpConfig);
	$("#eqpRemark").textbox("setValue", row.remark);	
}
</script>
</head>
<body>
<div style="margin: 10px 0 0 10px;">
	<table class="cond">
		<tr>
			<td>診間代碼</td>
			<td>診間名稱</td>
			<td>說明</td>
			<td rowspan="2">
				<button type="button" style="width: 100px;" onclick="qryRoomList('N');">查詢診間</button>
			</td>
			<td rowspan="2">
				<button type="button" style="width: 100px; ${queryHide}" onclick="addRoom();">新增診間</button>
			</td>
		</tr>
		<tr>
			<td>
				<input id="qryRoomId" class="easyui-textbox" style="width: 100px;" />
			</td>
			<td>
				<input id="qryRoomName" class="easyui-textbox" style="width: 100px;" />
			</td>
			<td>
				<input id="qryRoomDesc" class="easyui-textbox" style="width: 100px;" />
			</td>
		</tr>
	</table>
</div>
<div style="margin: 10px 0 0 10px; height: 30px; ">
	<div style="float: left;">
		<div id="pg" class="easyui-pagination" style="width: 400px;" data-options="onSelectPage: qryRoomList, showRefresh: false, total: 0" ></div>
	</div>
	<div style="float: left; position: relative; top: 2px; margin-left: 20px;">
		排序：
		<select id="order" class="easyui-combobox" data-options="multiple: true, panelHeight: 'auto', onHidePanel: qryRoomList, onChange: chgOrder" style="width: 150px;">
			<option value="roomId:A">代碼&#x25B2;</option>
			<option value="roomId:D">代碼&#x25BC;</option>
			<option value="roomName:A">名稱&#x25B2;</option>
			<option value="roomName:D">名稱&#x25BC;</option>
		</select>
	</div>
	<div style="float: left; position: relative; top: 5px; margin-left: 20px;">
		<span style="font-size: 30px; cursor: pointer;" onclick="clearAll();">&#x239A</span>
	</div>
</div>
<div style="margin: 10px 0 0 10px;">
	<div style="clear: both;">
		<table class="listHead">
			<tr>
				<td style="width: 100px;" class="headCol1">
					<span class="orderField" onclick="chgOrder(this);">診間代碼</span>
					<span class="orderMark" order="roomId">&#x25B2;</span>
				</td>
				<td style="width: 250px;" class="headCol2">
					<span class="orderField" onclick="chgOrder(this);">診間名稱</span>
					<span class="orderMark" order="roomName">&nbsp;</span>
				</td>
				<td style="width: 250px;" class="headCol3">說明</td>
				<td style="width: 100px;" class="headCol4">停用</td>
				<td style="width: 160px;" class="headCol5">處理動作</td>
			</tr>
		</table>
	</div>
	<div style="clear: both; width: 880px; height: 380px; overflow: auto; ">
		<table id="roomList" class="listData">
		</table>
	</div>
</div>
<div id="roomEdit" class="easyui-dialog" style="width: 1050px; height: 420px; display: none;"
	data-options="modal: true, title: '編輯診間資料', closed: true, onClose: roomEditClose">
	<input id="roomEditMode" type="hidden" />
	<div style="float: left; margin: 10px 0 0 10px;">
		<table>
			<tr class="modPane">
				<td>原診間代碼</td>
				<td>
					<input id="editRoomIdOrg" class="easyui-textbox" style="width: 150px;" data-options="readonly: true" />
				</td>
			</tr>
			<tr>
				<td>診間代碼</td>
				<td>
					<input id="editRoomId" class="easyui-textbox" style="width: 150px;" />
				</td>
			</tr>
			<tr>
				<td>診間名稱</td>
				<td>
					<input id="editRoomName" class="easyui-textbox" style="width: 150px;" />
				</td>
			</tr>
			<tr>
				<td>說明</td>
				<td>
					<input id="editRoomDesc" class="easyui-textbox" style="width: 150px;" />
				</td>
			</tr>
			<tr>
				<td>停用</td>
				<td>
					<input id="editSuspend" class="easyui-switchbutton" style="width: 100px;" data-options="onText: '暫停使用', offText: '正常使用'" />
				</td>
			</tr>
			<tr>
				<td>備註</td>
				<td>
					<input id="editRemark" class="easyui-textbox" style="width: 150px; height: 120px;" data-options="multiline: true" />
				</td>
			</tr>
			<tr>
				<td colspan="2" style="text-align: center;">
					<button type="button" style="width: 100px;" onclick="roomEditDone();">儲存診間</button>
				</td>
			</tr>
		</table>
	</div>
	<div style="float: left; margin: 10px 0 0 10px;">
		<div style="" >
			<div id="editEqpList" class="easyui-datagrid" style="width: 750px; height: 200px;"
				 data-options="title: '診間設備清單', singleSelect: true, onClickRow:selEqpRow, columns: [[
				 	{ field: 'eqpTypeDesc', title: '設備類別', align: 'center', width: 80 },
				 	{ field: 'eqpId', title: '設備代碼', halign: 'center', width: 80 },
				 	{ field: 'eqpName', title: '設備名稱', halign: 'center', width: 100 },
				 	{ field: 'eqpConfig', title: '設備設定資料', halign: 'center', width: 180 },
				 	{ field: 'fileName', title: '儲存檔名', halign: 'center', width: 150 },
				 	{ field: 'action', title: '處理動作', halign: 'center', width: 130, formatter: editEqpListFmt },
				 	{ field: 'eqpType', hidden: true },
				 	{ field: 'remark', hidden: true },
				 ]] 
				 ">
			</div>
		</div>
		<div>
			<table>
				<tr>
					<td>類別</td>
					<td>
						<select id="eqpType" class="easyui-combobox" style="width: 100px;" data-options="editable: false, panelHeight: 'auto'">
							${eqpTypeList}
						</select>
					</td>
					<td>代碼</td>
					<td>
						<input id="eqpId" class="easyui-textbox" style="width: 100px;" />
					</td>
					<td>名稱</td>
					<td>
						<input id="eqpName" class="easyui-textbox" style="width: 200px;" />
					</td>
					<td>檔名</td>
					<td>
						<input id="fileName" class="easyui-textbox" style="width: 200px;" />
					</td>
				</tr>
				<tr>
					<td>設定</td>
					<td colspan="7">
						<input id="eqpConfig" class="easyui-textbox" style="width: 700px;" />
					</td>
				</tr>
				<tr>
					<td>備註</td>
					<td colspan="7">
						<input id="eqpRemark" class="easyui-textbox" style="width: 700px; height: 50px;" data-options="multiline: true" />
					</td>
				</tr>
				<tr>
					<td colspan="8" style="text-align: center;">
						<button type="button" style="width: 200px;" onclick="addModEqp();">加入/修改設備</button>
					</td>
				</tr>
			</table>
		</div>
	</div>
</div>
<input type="hidden" id="progId" value="${progId}" />
<input type="hidden" id="privDesc" value="${privDesc}" />
<input type="hidden" id="progTitle" value="${progTitle}" />
<input type="hidden" id="queryHide" value="${queryHide}" />
<input type="hidden" id="status" value="${status}" />
<input type="hidden" id="statusTime" value="${statusTime}" />
</body>
<script type="text/javascript">
parent.showProg({ id: $("#progId").val(), priv: $("#privDesc").val(), title: $("#progTitle").val() });
parent.showStatus({ status: $("#status").val(), statusTime: $("#statusTime").val() });
</script>
</html>