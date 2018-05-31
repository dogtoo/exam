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
	$.post("QsQryHist_qryMemberList", req, function (res) {
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
		queryCond1._job = 'QsQryHist';
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
		if (!window.queryCond1 || window.queryCond1._job != 'QsQryHist')
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
	$.post("QsQryHist_qryQsList", req, function (res) {
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
					"    <input name='depart' type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.depart + "' />\n" +
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
					"    <input name='verSeq' type='text' style='width: " + widthList[wat++] + "px; text-align: right;' readonly value='" + qs.currVersion + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='statFlag' type='text' style='width: " + widthList[wat++] + "px; text-align: center;' readonly value='" + qs.statFlag + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <span style='width: " + widthList[wat++] + "px; display: inline-block; text-align: center;'>\n" +
					"        <button type='button' style='width: 70px;' onclick='viewQsDetail(this);'>內容</button>\n" +
					"        <button type='button' style='width: 70px;' onclick='viewQsHistory(this);'>歷史</button>\n" +
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
	$.post("QsQryHist_qryQs", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			jrow.addClass("select");
			$("#qsViewDetail").dialog("open");
			$("#detailQsId").textbox("setValue", req.qsId);
			$("#detailQsName").textbox("setValue", res.qsName);
			$("#detailQsSubject").textbox("setValue", res.qsSubject);
			$("#detailDepart").textbox("setValue", res.depart);
			$("#detailTarget").textbox("setValue", res.target);
			$("#detailVerStat").textbox("setValue", res.verStat);
			$("#detailEditRemark").textbox("setValue", res.editRemark);
			$("#detailOnlineRemark").textbox("setValue", res.onlineRemark);
			var jviewQsClass = $("#detailQsClass");
			jviewQsClass.datagrid("loadData", JSON.parse($("#qsClassData").val()));
			for (var i = 0; i < res.useQsClassList.length; i++)
				jviewQsClass.datalist("checkRow", jviewQsClass.datalist("getRowIndex", res.useQsClassList[i]));
			var jviewQsAbility = $("#detailQsAbility");
			jviewQsAbility.datalist("loadData", JSON.parse($("#qsAbilityData").val()));
			for (var i = 0; i < res.useQsAbilityList.length; i++)
				jviewQsAbility.datalist("checkRow", jviewQsAbility.datalist("getRowIndex", res.useQsAbilityList[i]));
			$("#userList").datagrid("loadData", res.userList);
		}
	}, "json");
}

function viewQsHistory(src) {
	var jrow = $(src).parent().parent().parent();
	var req = {};
	req.qsId = jrow.find("[name='qsId']").val();
	req.qryType = "H";
	$.post("QsQryHist_qryQs", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			jrow.addClass("select");
			$("#qsViewHistory").dialog("open");
			$("#historyQsId").val(req.qsId);
			$("#historyVerSeq").combobox("loadData", res.verList);
			historyVerSeqChg();
		}
	}, "json");
}

function historyVerSeqChg(nval, oval) {
	$("#historyVerDate").textbox("setValue", "");
	$("#historyCrUserId").textbox("setValue", "");
	$("#historyCrUserName").textbox("setValue", "");
	$("#historyCnfrmDate").textbox("setValue", "");
	$("#historyCnfrmUserId").textbox("setValue", "");
	$("#historyCnfrmUserName").textbox("setValue", "");
	$("#historyVerDesc").textbox("setValue", "");
	$("#historyEditReason").textbox("setValue", "");
	$("#historyVerRemark").textbox("setValue", "");
	$("#fileList").datagrid("loadData", []);
	if (!nval) {
		$("#historyVerSeq").combobox("setValue", "");
		return;
	}
	var req = {};
	req.qsId = $("#historyQsId").val();
	req.verSeq = nval;
	$.post("QsQryHist_qryQsVer", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#historyVerDate").textbox("setValue", res.verDate);
			$("#historyCrUserId").textbox("setValue", res.crUserId);
			$("#historyCrUserName").textbox("setValue", res.crUserName);
			$("#historyCnfrmDate").textbox("setValue", res.cnfrmDate);
			$("#historyCnfrmUserId").textbox("setValue", res.cnfrmUserId);
			$("#historyCnfrmUserName").textbox("setValue", res.cnfrmUserName);
			$("#historyVerDesc").textbox("setValue", res.verDesc);
			$("#historyEditReason").textbox("setValue", res.editReason);
			$("#historyVerRemark").textbox("setValue", res.remark);
			$("#fileList").datagrid("loadData", res.fileList);
			fileListSel();
		}
	}, "json");
}

function fileListActFmt(value, row, index) {
	return "<button type='button' style='width: 55px;' onclick='fileListOpen(this);'>開啟</button>\n" +
		"&nbsp;<button type='button' style='width: 55px;' onclick='fileListDownload(this);'>下載</button>\n" +
		"<input type='hidden' value='" + row.fileName + "' />\n";
}

function fileListSel(index, row) {
	$("#historyFileName").val("");
	$("#historyFileDesc").textbox("setValue", "");
	$("#historyFileClass").textbox("setValue", "");
	$("#historyFileSize").textbox("setValue", "");
	$("#historyFileCrDate").textbox("setValue", "");
	$("#historyFileCrUserName").textbox("setValue", "");
	$("#historyFileType").textbox("setValue", "");
	$("#historyFileRemark").textbox("setValue", "");
	$("#historyValidFileUser").combobox("loadData", []);
	if (!row) {
		$("#historyValidFileUser").combobox("setValue", "");
		historyValidFileUserChg();
		return;
	}
	$("#historyFileName").val(row.fileName);
	var req = {};
	req.qsId = $("#historyQsId").val();
	req.verSeq = $("#historyVerSeq").combobox("getValue");
	req.fileName = $("#historyFileName").val();
	$.post("QsQryHist_qryQsVerFile", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#historyFileDesc").textbox("setValue", res.fileDesc);
			$("#historyFileClass").textbox("setValue", res.fileClass);
			$("#historyFileSize").textbox("setValue", res.fileSize);
			$("#historyFileCrDate").textbox("setValue", res.crDate);
			$("#historyFileCrUserName").textbox("setValue", res.crUserName);
			$("#historyFileType").textbox("setValue", res.fileType);
			$("#historyFileRemark").textbox("setValue", res.remark);
			$("#historyValidFileUser").combobox("loadData", res.userList);
		}
	}, "json");
}

function fileListOpen(src) {
	var fileName = $(src).next().next().val();
	var url = "QsQryHist_download";
	url += "?qsId=" + encodeURIComponent($("#historyQsId").val());
	url += "&verSeq=" + encodeURIComponent($("#historyVerSeq").combobox("getValue"));
	url += "&fileName=" + encodeURIComponent(fileName);
	url += "&open=Y";
	window.open(url);
}

function fileListDownload(src) {
	var fileName = $(src).next().val();
	var url = "QsQryHist_download";
	url += "?qsId=" + encodeURIComponent($("#historyQsId").val());
	url += "&verSeq=" + encodeURIComponent($("#historyVerSeq").combobox("getValue"));
	url += "&fileName=" + encodeURIComponent(fileName);
	url += "&open=N";
	window.open(url);
}

function historyValidFileUserChg(nval, oval) {
	$("#historyValidFileDate").textbox("setValue", "");
	$("#historyValidFileFlag").textbox("setValue", "");
	$("#historyValidFileReason").textbox("setValue", "");
	if (!nval) {
		$("#historyValidFileUser").combobox("setValue", "");
		return;
	}
	var req = {};
	req.qsId = $("#historyQsId").val();
	req.verSeq = $("#historyVerSeq").combobox("getValue");
	req.fileName = $("#historyFileName").val();
	req.userId = nval;
	$.post("QsQryHist_qryQsVerFileValid", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#historyValidFileDate").textbox("setValue", res.validDate);
			$("#historyValidFileFlag").textbox("setValue", res.validDesc);
			$("#historyValidFileReason").textbox("setValue", res.reason);
		}
	}, "json");
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
			<td>包含人員</td>
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
				<td style="width: 160px;" class="headCol4">處理動作</td>
			</tr>
		</table>
	</div>
	<div style="clear: both; width: 1220px; height: 380px; overflow: auto;">
		<table id="qsList" class="listData">
		</table>
	</div>
</div>
<div id="qsViewDetail" class="easyui-dialog" style="width: 1020px; height: 470px; display: none;"
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
					<input id="detailQsSubject" class="easyui-textbox" style="width: 250px; height: 80px;" data-options="multiline: true, readonly: true" />
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
				<td>編輯備註</td>
				<td>
					<input id="detailEditRemark" class="easyui-textbox" style="width: 250px; height: 70px;" data-options="multiline: true, readonly: true" />
				</td>
			</tr>
			<tr>
				<td>公告備註</td>
				<td>
					<input id="detailOnlineRemark" class="easyui-textbox" style="width: 250px; height: 70px;" data-options="multiline: true, readonly: true" />
				</td>
			</tr>
		</table>
	</div>
	<div style="float: left; margin: 10px 0 0 10px;">
		<div id="detailQsClass" class="easyui-datalist" style="width: 200px; height: 160px;"
			 data-options="title: '測驗類別', singleSelect: false, checkbox: true, idField: 'value', readonly: true ">
		</div>
		<div style="height: 20px;">&nbsp;</div>
		<div id="detailQsAbility" class="easyui-datalist" style="width: 200px; height: 160px;"
			 data-options="title: '核心能力', singleSelect: false, checkbox: true, idField: 'value', readonly: true ">
		</div>
	</div>
	<div style="float: left; margin: 10px 0 0 10px;">
		<div id="userList" class="easyui-datagrid" style="width: 430px; height: 360px;"
			 data-options="title: '人員列表', singleSelect: true,
			 	columns: [[
			 		{ field: 'roleDesc', title: '類別', align: 'center', width: 50 },
			 		{ field: 'userId', title: '帳號', halign: 'center', width: 100 },
			 		{ field: 'userName', title: '姓名', halign: 'center', width: 100 },
			 		{ field: 'beginDate', title: '加入日期', halign: 'center', width: 80 },
			 		{ field: 'endDate', title: '結束日期', halign: 'center', width: 80 },
			 		{ field: 'seqNo', hidden: true },
			 	]] 
			 ">
		</div>
	</div>
</div>
<div id="qsViewHistory" class="easyui-dialog" style="width: 1280px; height: 470px; display: none;"
	data-options="modal: true, title: '教案詳細內容', closed: true, onClose: qsViewClose">
	<input id="historyQsId" type="hidden" />
	<div style="float: left; margin: 10px 0 0 10px;">
		<div>
			<table>
				<tr>
					<td>版本</td>
					<td colspan="2">
						<input id="historyVerSeq" class="easyui-combobox" style="width: 210px;" data-options="editable: false, panelHeight: 'auto', onChange: historyVerSeqChg" />
					</td>
				</tr>
				<tr>
					<td>建立日期</td>
					<td colspan="2">
						<input id="historyVerDate" class="easyui-textbox" style="width: 210px;" data-options="readonly: true" />
					</td>
				</tr>
				<tr>
					<td>建立人員</td>
					<td>
						<input id="historyCrUserId" class="easyui-textbox" style="width: 100px;" data-options="readonly: true" />
					</td>
					<td>
						<input id="historyCrUserName" class="easyui-textbox" style="width: 100px;" data-options="readonly: true" />
					</td>
				</tr>
				<tr>
					<td>送審日期</td>
					<td colspan="2">
						<input id="historyCnfrmDate" class="easyui-textbox" style="width: 210px;" data-options="readonly: true" />
					</td>
				</tr>
				<tr>
					<td>送審人員</td>
					<td>
						<input id="historyCnfrmUserId" class="easyui-textbox" style="width: 100px;" data-options="readonly: true" />
					</td>
					<td>
						<input id="historyCnfrmUserName" class="easyui-textbox" style="width: 100px;" data-options="readonly: true" />
					</td>
				</tr>
				<tr>
					<td>說明</td>
					<td colspan="2">
						<input id="historyVerDesc" class="easyui-textbox" style="width: 210px;" data-options="readonly: true" />
					</td>
				</tr>
				<tr>
					<td>編輯原因</td>
					<td colspan="2">
						<input id="historyEditReason" class="easyui-textbox" style="width: 210px; height: 100px; " data-options="multiline: true, readonly: true" />
					</td>
				</tr>
				<tr>
					<td>備註</td>
					<td colspan="2">
						<input id="historyVerRemark" class="easyui-textbox" style="width: 210px; height: 100px; " data-options="multiline: true, readonly: true" />
					</td>
				</tr>
			</table>
		</div>
	</div>
	<div style="float: left; margin: 10px 0 0 10px;">
		<div style="margin: 0 0 0 0;">
			<div id="fileList" class="easyui-datagrid" style="width: 610px; height: 360px;" data-options="
				title: '教案包含檔案列表', singleSelect: true, onSelect: fileListSel, columns: [[
					{ field: 'fileDesc', title: '檔案名稱', halign: 'center', width: 170 },
					{ field: 'fileClass', title: '檔案類別', halign: 'center', width: 100 },
					{ field: 'fileSize', title: '檔案大小', align: 'right', halign: 'center', width: 90 },
					{ field: 'fileType', title: '檔案型態', halign: 'center', width: 100 },
					{ field: 'action', title: '處理動作', halign: 'center', width: 130, formatter: fileListActFmt },
					{ field: 'fileName', hidden: true }
				]] 
				">
			</div>
		</div>
	</div>
	<div style="float: left; margin: 10px 0 0 10px;">
		<input id="historyFileName" type="hidden" />
		<div>
			<table>
				<tr>
					<td>檔案名稱</td>
					<td colspan="3">
						<input id="historyFileDesc" class="easyui-textbox" style="width: 260px;" data-options="readonly: true" />
					</td>
				</tr>
				<tr>
					<td>檔案類別</td>
					<td>
						<input id="historyFileClass" class="easyui-textbox" style="width: 100px;" data-options="readonly: true" />
					</td>
					<td>檔案大小</td>
					<td>
						<input id="historyFileSize" class="easyui-textbox" style="width: 100px; text-align: right" data-options="readonly: true" />
					</td>
				</tr>
				<tr>
					<td>建立日期</td>
					<td>
						<input id="historyFileCrDate" class="easyui-textbox" style="width: 100px;" data-options="readonly: true" />
					</td>
					<td>建立人員</td>
					<td>
						<input id="historyFileCrUserName" class="easyui-textbox" style="width: 100px;" data-options="readonly: true" />
					</td>
				</tr>
				<tr>
					<td>檔案型態</td>
					<td colspan="3">
						<input id="historyFileType" class="easyui-textbox" style="width: 260px;" data-options="readonly: true" />
					</td>
				</tr>
				<tr>
					<td>備註</td>
					<td colspan="3">
						<input id="historyFileRemark" class="easyui-textbox" style="width: 260px; height: 80px;" data-options="multiline: true, readonly: true" />
					</td>
				</tr>
			</table>
		</div>
		<div style="margin: 40px 0 0 0;">
			<table>
				<tr>
					<td>審核人員</td>
					<td colspan="3">
						<input id="historyValidFileUser" class="easyui-combobox" style="width: 260px;" data-options="editable: false, panelHeight: 'auto', onChange: historyValidFileUserChg" />
					</td>
				</tr>
				<tr>
					<td>日期</td>
					<td>
						<input id="historyValidFileDate" class="easyui-textbox" style="width: 120px;" data-options="readonly: true" />
					</td>
					<td>結果</td>
					<td>
						<input id="historyValidFileFlag" class="easyui-textbox" style="width: 100px;" data-options="readonly: true" />
					</td>
				</tr>
				<tr>
					<td>審核意見</td>
					<td colspan="3">
						<input id="historyValidFileReason" class="easyui-textbox" style="width: 260px; height: 60px;" data-options="multiline: true, readonly: true" />
					</td>
				</tr>
			</table>
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