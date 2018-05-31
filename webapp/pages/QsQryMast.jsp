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
		qryQsList();
}

function clearAll() {
	$("#qryQsId").textbox("setValue", "");
	$("#qryQsName").textbox("setValue", "");
	$("#qryDepartId").combobox("setValue", "");
	$("#qryTargetId").combobox("setValue", "");
	$("#qryQsClass").combobox("setValues", []);
	$("#qryQsAbility").combobox("setValues", []);
	$("#qryUserId").combobox("setValue", "");
	$("#qryBeginDate").datebox("setValue", "");
	$("#qryEndDate").datebox("setValue", "");
	$("#order").combobox("setValues", [ "qsId:A" ]);
	chgOrder();
	$("#pg").pagination({ total: 0 });
	$("#qsList tr").remove();
	delete window.queryCond1;
}

function qryMemberList(param, success, error) {
	if (!param.q || param.q.length < 1)
		return false;
	var req = {};
	req.memberCond = param.q;
	$.post("QsQryMast_qryMemberList", req, function (res) {
		if (res.success)
			success(res.memberList);
	}, "json");
}

function qryQsList(mode) {
	var req = {};
	req.mode = mode;
	if (mode == "N") {
		$("#pg").pagination({ total: 1, pageNumber: 1 });
		var queryCond1 = {};
		queryCond1._job = 'QsQryMast';
		queryCond1.qsId = $("#qryQsId").textbox("getValue");
		queryCond1.qsName = $("#qryQsName").textbox("getValue");
		queryCond1.departId = $("#qryDepartId").combobox("getValue");
		queryCond1.targetId = $("#qryTargetId").combobox("getValue");
		var qsClassList = $("#qryQsClass").combobox("getValues");
		for (var i = 0; i < qsClassList.length; i++)
			queryCond1["qryQsClass[" + i + "]"] = qsClassList[i];
		var qsAbilityList = $("#qryQsAbility").combobox("getValues");
		for (var i = 0; i < qsAbilityList.length; i++)
			queryCond1["qryQsAbility[" + i + "]"] = qsAbilityList[i];
		queryCond1.userId = $("#qryUserId").textbox("getValue");
		queryCond1.beginDate = $("#qryBeginDate").datebox("getValue");
		queryCond1.endDate = $("#qryEndDate").datebox("getValue");
		window.queryCond1 = queryCond1;
	}
	else {
		if (!window.queryCond1 || window.queryCond1._job != 'QsQryMast')
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
	$.post("QsQryMast_qryQsList", req, function (res) {
		if (mode != "S" && mode != "T")
			parent.showStatus(res);
		if (res.success) {
			if ('total' in res)
				$("#pg").pagination({ total: res.total });
			var queryHide = $("#queryHide").val();
			var jqsList = $("#qsList");
			var widthSum = 15;
			var widthList = jqsList.parent().prev().find("table.listHead td").map(function () { widthSum += $(this).width();	return $(this).width(); });
			jqsList.parent().css("width", widthSum.toString() + "px");
			jqsList.find("tr").remove();
			for (var i = 0; i < res.qsList.length; i++) {
				var qs = res.qsList[i];
				var wat = 0;
				var append = "<tr class='" + (i & 1 ? "odd" : "even") + "'>\n" +
					"<td>\n" +
					"    <input name='qsId' type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.qsId + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='qsName' type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.qsName + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='qsSubject' type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.qsSubject + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='departName' type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.departName + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='target' type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.target + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='crDate' type='text' style='width: " + widthList[wat++] + "px; text-align: center;' readonly value='" + qs.crDate + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='crUserName' type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.crUserName + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='verSeq' type='text' style='width: " + widthList[wat++] + "px; text-align: center;' readonly value='" + qs.verSeq + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='status' type='text' style='width: " + widthList[wat++] + "px; text-align: center;' readonly value='" + qs.statFlag + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <span style='width: " + widthList[wat++] + "px; display: inline-block; text-align: center;'>\n" +
					"        <button type='button' style='width: 55px;" + queryHide + "' onclick='viewQsDetail(this);'>內容</button>\n" +
					"        <button type='button' style='width: 55px;" + queryHide + "' onclick='viewQsUser(this);'>人員</button>\n" +
					"        <button type='button' style='width: 55px;" + queryHide + "' onclick='viewQsFile(this);'>檔案</button>\n" +
					"    </span>\n" +
					"</td>\n" +
					"</tr>\n";
				jqsList.append(append);
				var jrow = jqsList.find("tr:last");
			}
		}
	}, "json");
}

function viewQsDetail(src) {
	var jrow = $(src).parent().parent().parent();
	var req = {};
	req.qsId = jrow.find("[name='qsId']").val();
	req.qryType = "D";
	$.post("QsQryMast_qryQs", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			jrow.addClass("select");
			$("#qsViewDetail").dialog("open");
			$("#detailQsId").textbox("setValue", req.qsId);
			$("#detailQsName").textbox("setValue", res.qsName);
			$("#detailQsSubject").textbox("setValue", res.qsSubject);
			$("#detailDepart").textbox("setValue", res.departDesc);
			$("#detailTarget").textbox("setValue", res.targetDesc);
			$("#detailVerStat").textbox("setValue", res.currVer + '  ' + res.statFlagDesc);
			$("#detailRemark").textbox("setValue", res.remark);
			$("#detailQsClass").datagrid("loadData", JSON.parse($("#qsClassData").val()));
			for (var i = 0; i < res.useQsClass.length; i++)
				$("#detailQsClass").datalist("checkRow", $("#detailQsClass").datalist("getRowIndex", res.useQsClass[i]));
			$("#detailQsAbility").datagrid("loadData", JSON.parse($("#qsAbilityData").val()));
			for (var i = 0; i < res.useQsAbility.length; i++)
				$("#detailQsAbility").datalist("checkRow", $("#detailQsAbility").datalist("getRowIndex", res.useQsAbility[i]));
		}
	}, "json");
}

function viewQsUser(src) {
	var jrow = $(src).parent().parent().parent();
	var req = {};
	req.qsId = jrow.find("[name='qsId']").val();
	req.qryType = "U";
	$.post("QsQryMast_qryQs", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			jrow.addClass("select");
			$("#qsViewUser").dialog("open");
			$("#userList").datagrid("loadData", res.userList);
		}
	}, "json");
}

function viewQsFile(src) {
	var jrow = $(src).parent().parent().parent();
	var req = {};
	req.qsId = jrow.find("[name='qsId']").val();
	req.qryType = "F";
	$.post("QsQryMast_qryQs", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			jrow.addClass("select");
			$("#qsViewFile").dialog("open");
			$("#fileQsId").val(req.qsId);
			$("#fileList").datagrid("loadData", res.fileList);
		}
	}, "json");
}

function fileListActFmt(value, row, index) {
	if ($("#queryHide").val() != "")
		return "";
	return "<button type='button' style='width: 60px;' onclick='fileListOpen(this);'>開啟</button>\n" +
		"&nbsp;<button type='button' style='width: 60px;' onclick='fileListDownload(this);'>下載</button>\n" +
		"<input type='hidden' value='" + row.fileName + "' />\n";
}

function fileListOpen(src) {
	var fileName = $(src).next().next().val();
	var url = "QsQryMast_download";
	url += "?qsId=" + encodeURIComponent($("#fileQsId").val());
	url += "&fileName=" + encodeURIComponent(fileName);
	url += "&open=Y";
	window.open(url);
}

function fileListDownload(src) {
	var fileName = $(src).next().val();
	var url = "QsQryMast_download";
	url += "?qsId=" + encodeURIComponent($("#fileQsId").val());
	url += "&fileName=" + encodeURIComponent(fileName);
	url += "&open=N";
	window.open(url);
}

function qsViewClose() {
	$("#qsList tr.select").removeClass("select");
}
</script>
</head>
<body>
<div style="margin: 10px 0 0 10px;">
	<table class="cond">
		<tr>
			<td>教案代碼</td>
			<td>教案名稱</td>
			<td>科別</td>
			<td>對象</td>
			<td>測驗類別</td>
			<td>核心能力</td>
			<td>創建人員</td>
			<td>創建日期範圍</td>
			<td rowspan="2">
				<button type="button" style="width: 100px;" onclick="qryQsList('N');">查詢教案</button>
			</td>
		</tr>
		<tr>
			<td>
				<input id="qryQsId" class="easyui-textbox" style="width: 100px;" />
			</td>
			<td>
				<input id="qryQsName" class="easyui-textbox" style="width: 100px;" />
			</td>
			<td>
				<select id="qryDepartId" class="easyui-combobox" style="width: 100px;" data-options="editable: false, panelHeight: 'auto'" >
					${departList}
				</select>
			</td>
			<td>
				<select id="qryTargetId" class="easyui-combobox" style="width: 100px;" data-options="editable: false, panelHeight: 'auto'" >
					${qsTargetList}
				</select>
			</td>
			<td>
				<select id="qryQsClass" class="easyui-combobox" style="width: 100px;" data-options="editable: false, multiple: true, panelHeight: 'auto', value: ''" >
					${qsClassList}
				</select>
			</td>
			<td>
				<select id="qryQsAbility" class="easyui-combobox" style="width: 100px;" data-options="editable: false, multiple: true, panelHeight: 'auto', value: ''" >
					${qsAbilityList}
				</select>
			</td>
			<td>
				<input id="qryUserId" class="easyui-combobox" style="width: 100px;" data-options="loader: qryMemberList, mode: 'remote'"/>
			</td>
			<td>
				<input id="qryBeginDate" class="easyui-datebox" style="width: 100px;" />
				<input id="qryEndDate" class="easyui-datebox" style="width: 100px;" />
			</td>
		</tr>
	</table>
</div>
<div style="margin: 10px 0 0 10px; height: 30px; ">
	<div style="float: left;">
		<div id="pg" class="easyui-pagination" style="width: 400px;" data-options="onSelectPage: qryQsList, showRefresh: false, total: 0" ></div>
	</div>
	<div style="float: left; position: relative; top: 2px; margin-left: 20px;">
		排序：
		<select id="order" class="easyui-combobox" data-options="multiple: true, panelHeight: 'auto', onHidePanel: qryQsList, onChange: chgOrder" style="width: 250px;">
			<option value="qsId:A">代碼&#x25B2;</option>
			<option value="qsId:D">代碼&#x25BC;</option>
			<option value="qsName:A">名稱&#x25B2;</option>
			<option value="qsName:D">名稱&#x25BC;</option>
			<option value="departId:A">科別&#x25B2;</option>
			<option value="departId:D">科別&#x25BC;</option>
			<option value="targetId:A">對象&#x25B2;</option>
			<option value="targetId:D">對象&#x25BC;</option>
			<option value="crDate:A">日期&#x25B2;</option>
			<option value="crDate:D">日期&#x25BC;</option>
			${privOrder}
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
					<span class="orderField" onclick="chgOrder(this);">教案代碼</span>
					<span class="orderMark" order="qsId">&#x25B2;</span>
				</td>
				<td style="width: 250px;" class="headCol2">
					<span class="orderField" onclick="chgOrder(this);">教案名稱</span>
					<span class="orderMark" order="qsName">&nbsp;</span>
				</td>
				<td style="width: 250px;" class="headCol3">主旨</td>
				<td style="width: 100px;" class="headCol4">
					<span class="orderField" onclick="chgOrder(this);">科別</span>
					<span class="orderMark" order="departId">&nbsp;</span>
				</td>
				<td style="width: 100px;" class="headCol5">
					<span class="orderField" onclick="chgOrder(this);">對象</span>
					<span class="orderMark" order="targetId">&nbsp;</span>
				</td>
				<td style="width: 100px;" class="headCol6">
					<span class="orderField" onclick="chgOrder(this);">創建日期</span>
					<span class="orderMark" order="crDate">&nbsp;</span>
				</td>
				<td style="width: 100px;" class="headCol7">創建人員</td>
				<td style="width: 60px;" class="headCol6">版本</td>
				<td style="width: 60px;" class="headCol5">狀態</td>
				<td style="width: 180px;" class="headCol4">處理動作</td>
			</tr>
		</table>
	</div>
	<div style="clear: both; width: 1080px; height: 380px; overflow: auto;">
		<table id="qsList" class="listData">
		</table>
	</div>
</div>
<div id="qsViewDetail" class="easyui-dialog" style="width: 600px; height: 450px; display: none;"
	data-options="modal: true, title: '教案詳細內容', closed: true, onClose: qsViewClose">
	<div style="float: left; margin: 10px 0 0 10px;">
		<table>
			<tr>
				<td>教案代碼</td>
				<td>
					<input id="detailQsId" class="easyui-textbox" style="width: 250px;" data-options="readonly: true" />
				</td>
			</tr>
			<tr>
				<td>教案名稱</td>
				<td>
					<input id="detailQsName" class="easyui-textbox" style="width: 250px;" data-options="readonly: true"  />
				</td>
			</tr>
			<tr>
				<td>主旨</td>
				<td>
					<input id="detailQsSubject" class="easyui-textbox" style="width: 250px; height: 120px;" data-options="multiline: true, readonly: true" />
				</td>
			</tr>
			<tr>
				<td>科別</td>
				<td>
					<input id="detailDepart" class="easyui-textbox" style="width: 250px;" data-options="readonly: true" />
				</td>
			</tr>
			<tr>
				<td>對象</td>
				<td>
					<input id="detailTarget" class="easyui-textbox" style="width: 250px;" data-options="readonly: true" />
				</td>
			</tr>
			<tr>
				<td>版本及狀態</td>
				<td>
					<input id="detailVerStat" class="easyui-textbox" style="width: 250px;" data-options="readonly: true" />
				</td>
			</tr>
			<tr>
				<td>備註</td>
				<td>
					<input id="detailRemark" class="easyui-textbox" style="width: 250px; height: 120px;" data-options="multiline: true, readonly: true" />
				</td>
			</tr>
		</table>
	</div>
	<div style="float: left; margin: 10px 0 0 10px;">
		<div id="detailQsClass" class="easyui-datalist" style="width: 200px; height: 170px;"
			 data-options="title: '測驗類別', singleSelect: false, checkbox: true, idField: 'value', readonly: true ">
		</div>
		<div style="height: 10px;">&nbsp;</div>
		<div id="detailQsAbility" class="easyui-datalist" style="width: 200px; height: 170px;"
			 data-options="title: '核心能力', singleSelect: false, checkbox: true, idField: 'value', readonly: true ">
		</div>
	</div>
</div>
<div id="qsViewUser" class="easyui-dialog" style="width: 670px; height: 450px; display: none;"
	data-options="modal: true, title: '教案參與人員', closed: true, onClose: qsViewClose">
	<div style="float: left; margin: 10px 0 0 10px;">
		<div id="userList" class="easyui-datagrid" style="width: 630px; height: 360px;"
			 data-options="title: '人員列表', singleSelect: true,
			 	columns: [[
			 		{ field: 'seqNo', title: '序號', align: 'center', width: 50 },
			 		{ field: 'roleDesc', title: '類別', align: 'center', width: 60 },
			 		{ field: 'userId', title: '帳號', halign: 'center', width: 150 },
			 		{ field: 'userName', title: '姓名', halign: 'center', width: 150 },
			 		{ field: 'beginDate', title: '加入日期', halign: 'center', width: 100 },
			 		{ field: 'endDate', title: '退出日期', halign: 'center', width: 100 },
			 	]] 
			 ">
		</div>
	</div>
</div>
<div id="qsViewFile" class="easyui-dialog" style="width: 900px; height: 450px; display: none;"
	data-options="modal: true, title: '教案包含檔案', closed: true, onClose: qsViewClose">
	<input id="fileQsId" type="hidden" />
	<div style="float: left; margin: 10px 0 0 10px;">
		<div style="margin: 0 0 0 0;">
			<div id="fileList" class="easyui-datagrid" style="width: 840px; height: 360px;" data-options="
				title: '教案包含檔案列表', singleSelect: true, columns: [[
					{ field: 'fileDesc', title: '檔案名稱', halign: 'center', width: 200 },
					{ field: 'fileClass', title: '檔案類別', halign: 'center', width: 100 },
					{ field: 'fileSize', title: '檔案大小', align: 'right', halign: 'center', width: 100 },
					{ field: 'fileType', title: '檔案型態', halign: 'center', width: 200 },
					{ field: 'crDate', title: '創建日期', align: 'center', width: 80 },
					{ field: 'action', title: '處理動作', align: 'center', width: 140, formatter: fileListActFmt },
					{ field: 'fileName', hidden: true },
				]] 
				">
			</div>
		</div>
	</div>
</div>
<input type="hidden" id="qsClassData" value='${qsClassData}' />
<input type="hidden" id="qsAbilityData" value='${qsAbilityData}' />
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