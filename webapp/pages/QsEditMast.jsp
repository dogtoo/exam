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
	$("#qryQsClass").combobox("clear");
	$("#qryQsAbility").combobox("clear");
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
	$.post("QsEditMast_qryMemberList", req, function (res) {
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
		queryCond1._job = 'QsEditMast';
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
		if (!window.queryCond1 || window.queryCond1._job != 'QsEditMast')
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
	$.post("QsEditMast_qryQsList", req, function (res) {
		if (mode != "S" && mode != "T")
			parent.showStatus(res);
		if (res.success) {
			if ('total' in res)
				$("#pg").pagination({ total: res.total });
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
					"    <span style='width: " + widthList[wat++] + "px; display: inline-block; text-align: center;'>\n" +
					"        <button type='button' style='width: 70px;' ${queryHide} onclick='editQsFile(this);'>檔案</button>\n" +
					"        <button type='button' style='width: 70px;' ${queryHide} onclick='editQsDetail(this);'>內容</button>\n" +
					"        <button type='button' style='width: 70px;' ${queryHide} onclick='editQsNote(this);'>註記</button>\n" +
					"    </span>\n" +
					"</td>\n" +
					"</tr>\n";
				jqsList.append(append);
				var jrow = jqsList.find("tr:last");
			}
		}
	}, "json");
}

function editQsFile(src) {
	var jrow = $(src).parent().parent().parent();
	var req = {};
	req.qsId = jrow.find("[name='qsId']").val();
	req.qryType = "F";
	$.post("QsEditMast_qryQs", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			jrow.addClass("select");
			$("#qsEditFile").dialog("open");
			$("#fileQsId").val(req.qsId);
			$("#fileList").datagrid("loadData", res.fileList);
			$("#fileUpload").filebox("clear");
			$("#fileProgress").progressbar("setValue", 0);
			$("#fileClass").combobox("loadData", res.fileClassList);
			$("#fileShowOrder").textbox("setValue", "");
			$("#fileRemark").textbox("setValue", "");
			$("#uploadBut").prop("disabled", true);
			$("#modFileBut").prop("disabled", true);
		}
	}, "json");
}

function fileSelect() {
	$("#fileClass").combobox("clear");
	$("#fileShowOrder").textbox("setValue", "");
	$("#fileRemark").textbox("setValue", "");
	$("#uploadBut").prop("disabled", false);
	$("#modFileBut").prop("disabled", true);
} 

function fileListActFmt(value, row, index) {
	var disabled1 = "";
	var disabled2 = "";
	if (row.fileName == "")
		disabled1 = " disabled";
	if ("${queryHide}" == "" && row.fileName == "")
		disabled2 = " disabled";
	return "<button type='button' style='width: 60px;' onclick='fileListSel(this);'" + disabled1 + ">選擇</button>\n" +
		"&nbsp;<button type='button' style='width: 60px;' onclick='fileListOpen(this);'" + disabled1 + ">開啟</button>\n" +
		"&nbsp;<button type='button' style='width: 60px;' onclick='fileListDownload(this);'" + disabled1 + ">下載</button>\n" +
		"&nbsp;<button type='button' style='width: 60px;' onclick='fileListDel(this);' ${queryHide}" + disabled2 + ">刪除</button>\n" +
		"<input type='hidden' value='" + row.fileName + "' />\n";
}

function fileListSel(src) {
	var req = {};
	req.qsId = $("#fileQsId").val();
	req.fileName = $(src).next().next().next().next().val();
	$.post("QsEditMast_qryFile", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#fileServerName").val(req.fileName);
			$("#fileUpload").filebox("textbox").val(res.fileDesc);
			$("#fileClass").combobox("setValue", res.fileClass);
			$("#fileShowOrder").textbox("setValue", res.showOrder);
			$("#fileRemark").textbox("setValue", res.remark);			
			$("#uploadBut").prop("disabled", true);
			$("#modFileBut").prop("disabled", false);
		}
	}, "json");
}

function fileListOpen(src) {
	var url = "QsEditMast_download";
	url += "?qsId=" + encodeURIComponent($("#fileQsId").val());
	url += "&fileName=" + encodeURIComponent($(src).next().next().next().val());
	url += "&open=Y";
	window.open(url);
}

function fileListDownload(src) {
	var url = "QsEditMast_download";
	url += "?qsId=" + encodeURIComponent($("#fileQsId").val());
	url += "&fileName=" + encodeURIComponent($(src).next().next().val());
	url += "&open=N";
	window.open(url);
}

function fileListDel(src) {
	var req = {};
	req.qsId = $("#fileQsId").val();
	req.fileName = $(src).next().val();
	$.messager.confirm({
		title: '確認',
		msg: '是否要刪除此檔案？',
		ok: '刪除',
		cancel: '取消',
		fn: function (ok) {
			if (ok) {
				$.post("QsEditMast_delFile", req, function (res) {
					parent.showStatus(res);
					if (res.success) {
						$("#fileList").datagrid("loadData", res.fileList);
					}
				}, "json");
			}
		}
	});
}

function setUploadForm() {
	$("#upload").form({
		ajax: true, iframe: false,
		onProgress: function (pct) {
			$("#fileProgress").progressbar("setValue", pct);
		},
		success: function (res) {
			res = JSON.parse(res);
			parent.showStatus(res);
			if (res.success) {
				$("#fileList").datagrid("loadData", res.fileList);
				$("#fileUpload").filebox("clear");
				$("#fileProgress").progressbar("setValue", 0);
				$("#fileClass").combobox("clear");
				$("#fileShowOrder").textbox("setValue", "");
				$("#fileRemark").textbox("setValue", "");
				$("#uploadBut").prop("disabled", true);
			}
		}
	});
}

function modFile() {
	var req = {};
	req.qsId = $("#fileQsId").val();
	req.fileName = $("#fileServerName").val();
	req.fileClass = $("#fileClass").combobox("getValue");
	req.showOrder = $("#fileShowOrder").textbox("getValue");
	req.remark = $("#fileRemark").textbox("getValue");
	$.post("QsEditMast_modFile", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#fileClass").combobox("clear");
			$("#fileShowOrder").textbox("setValue", "");
			$("#fileRemark").textbox("setValue", "");
			$("#fileList").datagrid("loadData", res.fileList);
			$("#modFileBut").prop("disabled", true);
		}
	}, "json");
}

function editQsDetail(src) {
	var jrow = $(src).parent().parent().parent();
	var req = {};
	req.qsId = jrow.find("[name='qsId']").val();
	req.qryType = "D";
	$.post("QsEditMast_qryQs", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			jrow.addClass("select");
			$("#qsEditDetail").dialog("open");
			$("#detailQsId").textbox("setValue", req.qsId);
			$("#detailQsName").textbox("setValue", res.qsName);
			$("#detailQsSubject").textbox("setValue", res.qsSubject);
			$("#detailFocalRemark").textbox("setValue", res.focalRemark);
			$("#detailEditRemark").textbox("setValue", res.editRemark);
			$("#detailVersion").textbox("setValue", res.currVer);
			$("#detailVerInfo").textbox("setValue", res.verInfo);
			$("#detailVerDesc").textbox("setValue", res.verDesc);
			$("#reasonPane").css("visibility", res.hasReason ? "visible" : "hidden");
			$("#detailEditReason").textbox("setValue", res.editReason);
			$("#detailVerRemark").textbox("setValue", res.verRemark);
		}
	}, "json");
}

function qsEditClose() {
	$("#qsList tr.select").removeClass("select");
}

function modQs() {
	var req = {};
	req.qsId = $("#detailQsId").textbox("getValue");
	req.remark = $("#detailEditRemark").textbox("getValue");
	$.post("QsEditMast_modQs", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#qsEditDetail").dialog("close");
			qryQsList("S");
		}
	}, "json");
}

function modVer() {
	var req = {};
	req.qsId = $("#detailQsId").textbox("getValue");
	req.verDesc = $("#detailVerDesc").textbox("getValue");
	req.remark = $("#detailVerRemark").textbox("getValue");
	$.post("QsEditMast_modVer", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#qsEditDetail").dialog("close");
			qryQsList("S");
		}
	}, "json");
}

function validQs() {
	var req = {};
	req.qsId = $("#detailQsId").textbox("getValue");
	$.messager.confirm({
		title: '確認',
		msg: '是否要將此教案送交審核？',
		ok: '確認',
		cancel: '取消',
		fn: function (ok) {
			if (ok) {
				$.post("QsEditMast_validQs", req, function (res) {
					parent.showStatus(res);
					if (res.success) {
						$("#qsEditDetail").dialog("close");
						qryQsList("T");
					}
				}, "json");
			}
		}
	});
}

function editQsNote(src) {
	var jrow = $(src).parent().parent().parent();
	var req = {};
	req.qsId = jrow.find("[name='qsId']").val();
	req.showSuspend = "N";
	req.onlySelf = "N";
	req.qryType = "N";
	$.post("QsEditMast_qryQs", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			jrow.addClass("select");
			$("#qsEditNote").dialog("open");
			$("#noteQsId").val(req.qsId);
			$("#noteList").datagrid("loadData", res.noteList);
			$("#noteShowSuspend").switchbutton("uncheck");
			$("#noteOnlySelf").switchbutton("uncheck");
			$("#noteContentV").textbox("setValue", "");
			$("#noteDesc").textbox("setValue", "");
			$("#noteContent").textbox("setValue", "");
		}
	}, "json");
}

function noteListActFmt(value, row, index) {
	var disabled = "";
	var butText = row.suspend == "Y" ? "啟用" : "停用";
	return "<button type='button' style='width: 60px;' onclick='noteListSuspend(this);' ${queryHide}>" + butText + "</button>\n" +
		"<input type='hidden' value='" + row.seqNo + "' />\n" +
		"<input type='hidden' value='" + row.suspend + "' />\n";
}

function addNote() {
	var req = {};
	req.qsId = $("#noteQsId").val();
	req.noteDesc = $("#noteDesc").textbox("getValue");
	req.content = $("#noteContent").textbox("getValue");
	req.showSuspend = $("#noteShowSuspend").switchbutton("options").checked ? "Y" : "N";
	req.onlySelf = $("#noteOnlySelf").switchbutton("options").checked ? "Y" : "N";
	$.post("QsEditMast_addNote", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#noteList").datagrid("loadData", res.noteList);
			$("#noteContentV").textbox("setValue", "");
			$("#noteDesc").textbox("setValue", "");
			$("#noteContent").textbox("setValue", "");
		}
	}, "json");
}

function qryNoteList() {
	var req = {};
	req.qsId = $("#noteQsId").val();
	req.showSuspend = $("#noteShowSuspend").switchbutton("options").checked ? "Y" : "N";
	req.onlySelf = $("#noteOnlySelf").switchbutton("options").checked ? "Y" : "N";
	$.post("QsEditMast_qryNoteList", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#noteList").datagrid("loadData", res.noteList);
			$("#noteContentV").textbox("setValue", "");
		}
	}, "json");
}

function noteListSel(index, row) {
	$("#noteContentV").textbox("setValue", row.content);
}

function noteListSuspend(src) {
	var req = {};
	req.qsId = $("#noteQsId").val();
	req.seqNo = $(src).next().val();
	req.suspend = $(src).next().next().val() == "Y" ? "N" : "Y";
	req.showSuspend = $("#noteShowSuspend").switchbutton("options").checked ? "Y" : "N";
	req.onlySelf = $("#noteOnlySelf").switchbutton("options").checked ? "Y" : "N";
	$.post("QsEditMast_suspendNote", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#noteList").datagrid("loadData", res.noteList);
			$("#noteContentV").textbox("setValue", "");
		}
	}, "json");
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
				<td style="width: 220px;" class="headCol5">處理動作</td>
			</tr>
		</table>
	</div>
	<div style="clear: both; width: 1300px; height: 380px; overflow: auto;">
		<table id="qsList" class="listData">
		</table>
	</div>
</div>
<div id="qsEditFile" class="easyui-dialog" style="width: 1300px; height: 400px; display: none;"
	data-options="modal: true, title: '編輯教案檔案', closed: true, onClose: qsEditClose">
	<div style="float: left; margin: 10px 0 0 10px;">
		<div id="fileList" class="easyui-datagrid" style="width: 900px; height: 300px;" data-options="
			title: '教案包含檔案列表', singleSelect: true, columns: [[
				{ field: 'fileDesc', title: '檔案說明', halign: 'center', width: 150 },
				{ field: 'fileClass', title: '檔案類別', halign: 'center', width: 100 },
				{ field: 'showOrder', title: '順序', align: 'center', width: 50 },
				{ field: 'crDate', title: '上傳日期', align: 'center', width: 70 },
				{ field: 'userName', title: '上傳人員', halign: 'center', width: 80 },
				{ field: 'fileSize', title: '檔案大小', align: 'right', halign: 'center', width: 80 },
				{ field: 'fileType', title: '檔案型態', halign: 'center', width: 80 },
				{ field: 'action', title: '處理動作', align: 'center', width: 270, formatter: fileListActFmt },
			]] 
			">
		</div>
	</div>
	<div style="float: left; margin: 10px 0 0 10px;">
		<div style="margin: 0 0 0 0;">
			<input id="fileServerName" type="hidden" />
			<form id="upload" method="post" action="QsEditMast_upload" enctype="multipart/form-data">
				<input id="fileQsId" name="qsId" type="hidden" />
				<table>
					<tr>
						<td>上傳檔案</td>
						<td>
							<input id="fileUpload" name="file" class="easyui-filebox" style="width: 250px;" data-options="prompt: '請選擇上傳檔案', buttonText: '選擇檔案', onClickButton: fileSelect"/>
						</td>
					</tr>
					<tr>
						<td>上傳進度</td>
						<td>
							<div id="fileProgress" class="easyui-progressbar" style="width: 250px;" ></div>
						</td>
					</tr>
					<tr>
						<td>檔案類別</td>
						<td>
							<input id="fileClass" name="fileClass" class="easyui-combobox" style="width: 250px;" data-options="panelHeight: 'auto', editable: false"/>
						</td>
					</tr>
					<tr>
						<td>顯示順序</td>
						<td>
							<input id="fileShowOrder" name="showOrder" class="easyui-textbox" style="width: 250px;"/>
						</td>
					</tr>
					<tr>
						<td>檔案備註</td>
						<td colspan="2">
							<input id="fileRemark" name="remark" class="easyui-textbox" style="width: 250px; height: 100px" data-options="multiline: true" />
						</td>
					</tr>
					<tr>
						<td colspan="2" style="text-align: center;">
							<button id="uploadBut" type="submit" style="width: 100px;">上傳檔案</button>
							&nbsp;&nbsp;
							<button id="modFileBut" type="button" style="width: 100px;" onclick="modFile();">修改檔案</button>
						</td>
					</tr>
				</table>
			</form>
		</div>
	</div>
</div>
<div id="qsEditDetail" class="easyui-dialog" style="width: 800px; height: 400px; display: none;"
	data-options="modal: true, title: '編輯教案內容', closed: true, onClose: qsEditClose">
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
					<input id="detailQsName" class="easyui-textbox" style="width: 250px;" data-options="readonly: true" />
				</td>
			</tr>
			<tr>
				<td>主旨</td>
				<td>
					<input id="detailQsSubject" class="easyui-textbox" style="width: 250px; height: 50px;" data-options="multiline: true, readonly: true" />
				</td>
			</tr>
			<tr>
				<td>教案重點</td>
				<td>
					<input id="detailFocalRemark" class="easyui-textbox" style="width: 250px; height: 100px;" data-options="multiline: true, readonly: true" />
				</td>
			</tr>
			<tr>
				<td>教案備註</td>
				<td>
					<input id="detailEditRemark" class="easyui-textbox" style="width: 250px; height: 100px;" data-options="multiline: true" />
				</td>
			</tr>
			<tr>
				<td colspan="2" style="text-align: center;">
					<button type="button" style="width: 100px;" onclick="modQs();">修改教案</button>
				</td>
			</tr>
		</table>
	</div>
	<div style="float: left; margin: 10px 0 0 10px;">
		<table>
			<tr>
				<td>目前版本</td>
				<td>
					<input id="detailVersion" class="easyui-textbox" style="width: 250px;" data-options="readonly: true" />
				</td>
			</tr>
			<tr>
				<td>版本開始</td>
				<td>
					<input id="detailVerInfo" class="easyui-textbox" style="width: 250px;" data-options="readonly: true" />
				</td>
			</tr>
			<tr>
				<td>版本說明</td>
				<td>
					<input id="detailVerDesc" class="easyui-textbox" style="width: 250px; height: 50px;" data-options="multiline: true" />
				</td>
			</tr>
			<tr id="reasonPane">
				<td>重編輯原因</td>
				<td>
					<input id="detailEditReason" class="easyui-textbox" style="width: 250px; height: 100px;" data-options="multiline: true, readonly: true"/>
				</td>
			</tr>
			<tr>
				<td>版本備註</td>
				<td>
					<input id="detailVerRemark" class="easyui-textbox" style="width: 250px; height: 100px;" data-options="multiline: true" />
				</td>
			</tr>
			<tr>
				<td colspan="2" style="text-align: center;">
					<button type="button" style="width: 100px;" onclick="modVer();">修改版本</button>
				</td>
			</tr>
		</table>
	</div>
	<div style="float: left; margin: 10px 0 0 10px;">
		<div style="height: 320px;">&nbsp;</div>
		<div>
			<button type="button" style="width: 100px;" onclick="validQs();">送交審核</button>
		</div>
	</div>
</div>
<div id="qsEditNote" class="easyui-dialog" style="width: 1000px; height: 420px; display: none;"
	data-options="modal: true, title: '編輯教案註記', closed: true, onClose: qsEditClose">
	<div style="float: left; margin: 10px 0 0 10px;">
		<div id="noteList" class="easyui-datagrid" style="width: 550px; height: 330px;" data-options="
			title: '教案編輯審核期間註記列表', singleSelect: true, onSelect: noteListSel, columns: [[
				{ field: 'seqShow', title: '序號', align: 'center', width: 50 },
				{ field: 'noteDesc', title: '註記說明', halign: 'center', width: 200 },
				{ field: 'crVerSeq', title: '建立版本', align: 'center', width: 50 },
				{ field: 'crDate', title: '建立日期', align: 'center', width: 70 },
				{ field: 'userName', title: '建立人員', halign: 'center', width: 90 },
				{ field: 'action', title: '處理動作', align: 'center', width: 70, formatter: noteListActFmt },
				{ field: 'seqNo', hidden: true },
				{ field: 'suspend', hidden: true },
				{ field: 'content', hidden: true },
			]] 
			">
		</div>
	</div>
	<div style="float: left; margin: 10px 0 0 10px;">
		<input id="noteQsId" type="hidden" />
		<div style="margin: 0 0 0 0;">
			<table>
				<tr>
					<td>顯示</td>
					<td>
						<input id="noteShowSuspend" class="easyui-switchbutton" style="width: 150px;" data-options="onText: '顯示已停用註記', offText: '隱藏已停用註記', onChange: qryNoteList"/>
						&nbsp;&nbsp;
						<input id="noteOnlySelf" class="easyui-switchbutton" style="width: 120px;" data-options="onText: '僅顯示本人註記', offText: '顯示所有註記', onChange: qryNoteList"/>
					</td>
				</tr>
				<tr>
					<td>內容</td>
					<td>
						<input id="noteContentV" class="easyui-textbox" style="width: 280px; height: 100px;" data-options="multiline: true, editable: false"/>
					</td>
				</tr>
			</table>
		</div>
		<div style="margin: 50px 0 0 0;">
			<table>
				<tr>
					<td>說明</td>
					<td>
						<input id="noteDesc" class="easyui-textbox" style="width: 280px;" />
					</td>
				</tr>
				<tr>
					<td>內容</td>
					<td>
						<input id="noteContent" class="easyui-textbox" style="width: 280px; height: 100px;" data-options="multiline: true"/>
					</td>
				</tr>
				<tr>
					<td colspan="2" style="text-align: center;">
						<button type="button" style="width: 100px;" onclick="addNote();">建立註記</button>
					</td>
				</tr>
			</table>
		</div>
	</div>
</div>
<input type="hidden" id="progId" value="${progId}" />
<input type="hidden" id="privDesc" value="${privDesc}" />
<input type="hidden" id="progTitle" value="${progTitle}" />
<input type="hidden" id="status" value="${status}" />
<input type="hidden" id="statusTime" value="${statusTime}" />
</body>
<script type="text/javascript">
parent.showProg({ id: $("#progId").val(), priv: $("#privDesc").val(), title: $("#progTitle").val() });
parent.showStatus({ status: $("#status").val(), statusTime: $("#statusTime").val() });
setUploadForm();
</script>
</html>