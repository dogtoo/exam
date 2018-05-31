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
	$.post("QsMntNote_qryMemberList", req, function (res) {
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
		queryCond1._job = 'QsMntNote';
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
		if (!window.queryCond1 || window.queryCond1._job != 'QsMntNote')
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
	$.post("QsMntNote_qryQsList", req, function (res) {
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
					"    <input name='crDate' type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.crDate + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='crUserName' type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.crUserName + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <span style='width: " + widthList[wat++] + "px; display: inline-block; text-align: center;'>\n" +
					"        <button type='button' style='width: 70px;' " + queryHide + " onclick='editQsDetail(this);'>內容</button>\n" +
					"        &nbsp;\n" +
					"        <button type='button' style='width: 70px;' " + queryHide + " onclick='editQsNote(this);'>註記</button>\n" +
					"    </span>\n" +
					"</td>\n" +
					"</tr>\n";
				jqsList.append(append);
				var jrow = jqsList.find("tr:last");
			}
		}
	}, "json");
}

function editQsDetail(src) {
	var jrow = $(src).parent().parent().parent();
	var req = {};
	req.qsId = jrow.find("[name='qsId']").val();
	req.qryType = "D";
	$.post("QsMntNote_qryQs", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			jrow.addClass("select");
			$("#qsEditDetail").dialog("open");
			$("#detailQsId").textbox("setValue", req.qsId);
			$("#detailQsName").textbox("setValue", res.qsName);
			$("#detailQsSubject").textbox("setValue", res.qsSubject);
			$("#detailVerStat").textbox("setValue", res.currVer + "  " + res.statFlagDesc);
			$("#detailRemark").textbox("setValue", res.remark);
		}
	}, "json");
}

function modQs() {
	var req = {};
	req.qsId = $("#detailQsId").textbox("getValue");
	req.remark = $("#detailRemark").textbox("getValue");
	$.post("QsMntNote_modQs", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#qsEditDetail").dialog("close");
			qryQsList("S");
		}
	}, "json");
	
}

function editQsNote(src) {
	var jrow = $(src).parent().parent().parent();
	$("#noteQsId").val(jrow.find("[name='qsId']").val());
	$("#noteQryExpired").switchbutton("uncheck");
	$("#noteQrySort").combobox("setValue", "I");
	jrow.addClass("select");
	$("#qsEditNote").dialog("open");
	qryNoteList();
}

function qryNoteList(arg) {
	var req = {};
	req.qsId = $("#noteQsId").val();
	req.expired = (this && $(this).prop("id") == "noteQryExpired" ? arg : $("#noteQryExpired").switchbutton("options").checked) ? "Y" : "N";
	req.sort = this && $(this).prop("id") == "noteQrySort" ? arg.value : $("#noteQrySort").combobox("getValue");
	$.post("QsMntNote_qryNoteList", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#noteList").datagrid("loadData", res.noteList);
			clearNote('A');
		}			
	}, "json");
}

function clearNote(mode) {
	if (mode == "A") {
		$("#noteImportant").combobox({ readonly: false });
		$("#noteClass").combobox({ readonly: false });
		$("#noteDesc").textbox({ readonly: false });
		$("#noteContent").textbox({ readonly: false });
		$("#addNoteBut").prop("disabled", false);
	}
	else if (mode == "V") {
		$("#noteImportant").combobox({ readonly: true });
		$("#noteClass").combobox({ readonly: true });
		$("#noteDesc").textbox({ readonly: true });
		$("#noteContent").textbox({ readonly: true });
		$("#addNoteBut").prop("disabled", true);
	}
	$("#noteSeqNo").textbox("setValue", "");
	$("#noteUserName").textbox("setValue", "");
	$("#noteImportant").combobox("setValue", "");
	$("#noteClass").combobox("setValue", "");
	$("#noteCrDate").textbox("setValue", "");
	$("#noteExpireDate").datebox("setValue", "");
	$("#noteDesc").textbox("setValue", "");
	$("#noteContent").textbox("setValue", "");
}

function noteListActFmt(value, row, index) {
	if ($("#queryHide").val() != "")
		return "";
	return "<button type='button' style='width: 70px;' onclick='noteListDetail(this);'>詳細</button>\n" +
		"&nbsp;<button type='button' style='width: 70px;' onclick='noteListExpire(this);'>過期</button>\n" +
		"<input type='hidden' value='" + row.seqNo + "' />\n";
}

function noteListDetail(src) {
	var req = {};
	req.qsId = $("#noteQsId").val();
	req.seqNo = $(src).next().next().val();
	$.post("QsMntNote_qryNote", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			clearNote("V");
			$("#noteSeqNo").textbox("setValue", req.seqNo);
			$("#noteUserName").textbox("setValue", res.userName);
			$("#noteImportant").combobox("setValue", res.importantId);
			$("#noteClass").combobox("setValue", res.noteClass);
			$("#noteCrDate").textbox("setValue", res.crDate);
			$("#noteExpireDate").datebox("setValue", res.expireDate);
			$("#noteDesc").textbox("setValue", res.noteDesc);
			$("#noteContent").textbox("setValue", res.content);
		}			
	}, "json");
}

function noteListExpire(src) {
	var req = {};
	req.qsId = $("#noteQsId").val();
	req.seqNo = $(src).next().val();
	req.expired = $("#noteQryExpired").switchbutton("options").checked ? "Y" : "N";
	req.sort = $("#noteQrySort").combobox("getValue");
	$.messager.confirm({
		title: '確認',
		msg: '是否要將此註記設定為已過期？',
		ok: '設定',
		cancel: '取消',
		fn: function (ok) {
			if (ok) {
				$.post("QsMntNote_expNote", req, function (res) {
					parent.showStatus(res);
					if (res.success) {
						$("#noteList").datagrid("loadData", res.noteList);
						clearNote('A');
					}			
				}, "json");
			}
		}
	});
}

function addNote() {
	var req = {};
	req.qsId = $("#noteQsId").val();
	req.importantId = $("#noteImportant").combobox("getValue");
	req.noteClass = $("#noteClass").combobox("getValue");
	req.expireDate = $("#noteExpireDate").textbox("getValue");
	req.noteDesc = $("#noteDesc").textbox("getValue");
	req.content = $("#noteContent").textbox("getValue");
	req.expired = $("#noteQryExpired").switchbutton("options").checked ? "Y" : "N";
	req.sort = $("#noteQrySort").combobox("getValue");
	$.post("QsMntNote_addNote", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#noteList").datagrid("loadData", res.noteList);
			clearNote('A');
		}
	})
}

function qsEditClose() {
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
				<td style="width: 160px;" class="headCol4">處理動作</td>
			</tr>
		</table>
	</div>
	<div style="clear: both; width: 1080px; height: 380px; overflow: auto;">
		<table id="qsList" class="listData">
		</table>
	</div>
</div>
<div id="qsEditDetail" class="easyui-dialog" style="width: 400px; height: 440px; display: none;"
	data-options="modal: true, title: '維護教案內容', closed: true, onClose: qsEditClose">
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
					<input id="detailQsSubject" class="easyui-textbox" style="width: 250px; height: 100px;" data-options="multiline: true, readonly: true" />
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
					<input id="detailRemark" class="easyui-textbox" style="width: 250px; height: 150px;" data-options="multiline: true" />
				</td>
			</tr>
			<tr>
				<td colspan="2" style="text-align: center;">
					<button type="button" style="width: 150px;" onclick="modQs();">修改教案備註</button>
				</td>
			</tr>
		</table>
	</div>
</div>
<div id="qsEditNote" class="easyui-dialog" style="width: 960px; height: 440px; display: none;"
	data-options="modal: true, title: '維護教案註記', closed: true, onClose: qsEditClose">
	<input id="noteQsId" type="hidden" />
	<div style="float: left; margin: 10px 0 0 10px;">
		<div style="margin: 10px 0 0 10px;">
			<input id="noteQryExpired" class="easyui-switchbutton" style="width: 120px;" data-options="onText: '包含過期註記', offText: '僅查有效註記', onChange: qryNoteList" />
			&nbsp;&nbsp;&nbsp;&nbsp;
			排序:
			<select id="noteQrySort" class="easyui-combobox" data-options="editable: false, panelHeight: 'auto', onSelect: qryNoteList">
				<option value="I">重要性</option>
				<option value="C">類別</option>
				<option value="S">序號</option>
			</select>
			&nbsp;&nbsp;&nbsp;&nbsp;
			<div style="float: right; margin: 5px 10px 0 0;">
				<span style="font-size: 30px; cursor: pointer;" onclick="clearNote('A');">&#x239A</span>
			</div>
		</div>
		<div style="margin: 10px 0 0 10px;">
			<div id="noteList" class="easyui-datagrid" style="width: 530px; height: 300px;"
				 data-options="title: '教案註記列表', singleSelect: true,
				 	columns: [[
				 		{ field: 'seqNo', title: '序號', align: 'center', width: 60 },
				 		{ field: 'important', title: '重要性', align: 'center', width: 60 },
				 		{ field: 'noteClass', title: '類別', align: 'center', width: 80 },
				 		{ field: 'noteDesc', title: '說明', halign: 'center', width: 150 },
				 		{ field: 'action', title: '動作', align: 'center', width: 160, formatter: noteListActFmt },
				 	]] 
				 ">
			</div>
		</div>
	</div>
	<div style="float: left; margin: 10px 0 0 10px;">
		<div style="margin: 0 0 0 0;">
			<table>
				<tr>
					<td>序號</td>
					<td>
						<input id="noteSeqNo" class="easyui-textbox" style="width: 100px;" data-options="readonly: true"/>
					</td>
					<td>建立人員</td>
					<td>
						<input id="noteUserName" class="easyui-textbox" style="width: 100px;" data-options="readonly: true"/>
					</td>
				</tr>
				<tr>
					<td>重要性</td>
					<td>
						<select id="noteImportant" class="easyui-combobox" style="width: 100px;" data-options="editable: false, panelHeight: 'auto'" >
							${noteImportantList}
						</select>
					</td>
					<td>註記類別</td>
					<td>
						<select id="noteClass" class="easyui-combobox" style="width: 100px;" data-options="editable: false, panelHeight: 'auto'">
							${noteClassList}
						</select>
					</td>
				</tr>
				<tr>
					<td>建立日期</td>
					<td>
						<input id="noteCrDate" class="easyui-textbox" style="width: 100px;" data-options="readonly: true"/>
					</td>
					<td>過期日期</td>
					<td>
						<input id="noteExpireDate" class="easyui-datebox" style="width: 100px;"/>
					</td>
				</tr>
				<tr>
					<td>註記說明</td>
					<td colspan="3">
						<input id="noteDesc" class="easyui-textbox" style="width: 280px;"/>
					</td>
				</tr>
				<tr>
					<td>註記內容</td>
					<td colspan="3">
						<input id="noteContent" class="easyui-textbox" style="width: 280px; height: 120px;" data-options="multiline: true, readonly: true"/>
					</td>
				</tr>
				<tr>
					<td>
						&nbsp;
					</td>
				</tr>
				<tr>
					<td colspan="4" style="text-align: center;">
						<button type="button" id="addNoteBut" style="width: 150px;" onclick="addNote();">新增註記</button>
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