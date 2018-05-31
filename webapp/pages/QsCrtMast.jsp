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
#qryUserId + .textbox .textbox-text {
	text-transform: uppercase;
}
</style>
<script type="text/javascript">

/*
 * 變更排序。
 */
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

/*
 * 清除全部。
 */
function clearAll() {
	$("#qryQsId").textbox("setValue", "");
	$("#qryQsName").textbox("setValue", "");
	$("#qryStatFlag").switchbutton("uncheck");
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

/*
 * 輸入字母，自動查詢相關人員帳號。
 */
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



/*
 * 查詢教案。
 */
function qryQsList(mode) {
	var req = {};
	req.mode = mode;
	if (mode == "N") {
		$("#pg").pagination({ total: 1, pageNumber: 1 });
		var queryCond1 = {};
		queryCond1._job = 'QsCrtMast';
		queryCond1.qsId = $("#qryQsId").textbox("getValue");
		queryCond1.qsName = $("#qryQsName").textbox("getValue");
		queryCond1.statFlag = $("#qryStatFlag").switchbutton("options").checked ? "" : "N";
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
		if (!window.queryCond1 || window.queryCond1._job != 'QsCrtMast')
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
	$.post("QsCrtMast_qryQsList", req, function (res) {
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
					"    <input name='statFlag' type='text' style='width: " + widthList[wat++] + "px; text-align: center;' readonly value='" + qs.statFlag + "' />\n" +
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
					"    <span style='width: " + widthList[wat++] + "px; display: inline-block; text-align: center;'>\n" +
					"        <button type='button' style='width: 70px;' ${queryHide} onclick='modQs(this);'>編輯</button>\n" +
					"        &nbsp;&nbsp;<button type='button' style='width: 70px;' ${queryHide} onclick='delQs(this);'>刪除</button>\n" +
					"    </span>\n" +
					"</td>\n" +
					"</tr>\n";
				jqsList.append(append);
				var jrow = jqsList.find("tr:last");
			}
		}
	}, "json");
}

/*
 * 新增教案。
 */
function addQs() {
	var req = {};
	$.post("QsCrtMast_qryQs", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#qsEdit").dialog("open");
			$("#qsEditMode").val("A");
			$("#qsEdit .modPane").css("visibility", "hidden");
			$("#editQstmpId").combogrid("grid").datagrid("loadData", res.qstmpList);
			$("#editDepartId").combobox("loadData", res.departList);
			$("#editTargetId").combobox("loadData", res.qsTargetList);
			$("#editQsClass").datalist("loadData", res.qsClassList);
			$("#editQsAbility").datalist("loadData", res.qsAbilityList);
			$("#editFileClass").datalist("loadData", res.fileClassList);
			$("#editQsId").textbox("setValue", "");
			$("#editQsName").textbox("setValue", "");
			$("#editQsSubject").textbox("setValue", "");
			$("#editFocal").textbox("setValue", "");
			$("#editQstmpId").combogrid("setValue", "");
			$("#editDepartId").combobox("setValue", "");
			$("#editTargetId").combobox("setValue", "");
			$("#editFileMaxSize").textbox("setValue", "");
			$("#editRemark").textbox("setValue", "");
			$("#editQsClass").datalist("uncheckAll");
			$("#editQsAbility").datalist("uncheckAll");
			$("#editUserId").combobox("loadData", []);
			$("#editUserName").textbox("setValue", "");
			$("#editUserRole").combobox("setValue", "E");
			$("#editUserList").datagrid("loadData", []);
			$("#editModBut").prop("disabled", false);
			$("#editEditBut").prop("disabled", true);
		}
	}, "json");
}

/*
 * 點擊教案編輯button。
 */
function modQs(src) {
	var jrow = $(src).parent().parent().parent();
	var req = {};
	req.qsId = jrow.find("[name='qsId']").val();
	$.post("QsCrtMast_qryQs", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			jrow.addClass("select");			
			$("#qsEdit").dialog("open");
			$("#qsEditMode").val("M");
			$("#qsEdit .modPane").css("visibility", "visible");
			$("#editQstmpId").combogrid("grid").datagrid("loadData", res.qstmpList);
			$("#editDepartId").combobox("loadData", res.departList);
			$("#editTargetId").combobox("loadData", res.qsTargetList);
			$("#editQsClass").datalist("loadData", res.qsClassList);
			$("#editQsAbility").datalist("loadData", res.qsAbilityList);
			$("#editFileClass").datalist("loadData", res.fileClassList);
			$("#editQsIdOrg").textbox("setValue", req.qsId);
			$("#editQsId").textbox("setValue", req.qsId);
			$("#editQsName").textbox("setValue", res.qsName);
			$("#editQsSubject").textbox("setValue", res.qsSubject);
			$("#editFocal").textbox("setValue", res.focal);
			$("#editQstmpId").combogrid("setValue", res.qstmpId);
			$("#editDepartId").combobox("setValue", res.departId);
			$("#editTargetId").combobox("setValue", res.targetId);
			$("#editFileMaxSize").textbox("setValue", res.fileMaxSize);
			$("#editRemark").textbox("setValue", res.remark);
			$("#editQsClass").datalist("uncheckAll");
			for (var i = 0; i < res.useQsClass.length; i++) {
				$("#editQsClass").datalist("checkRow", $("#editQsClass").datalist("getRowIndex", res.useQsClass[i]));
			}
			$("#editQsAbility").datalist("uncheckAll");
			for (var i = 0; i < res.useQsAbility.length; i++){
				$("#editQsAbility").datalist("checkRow", $("#editQsAbility").datalist("getRowIndex", res.useQsAbility[i]));
			}
			$("#editFileClass").datalist("uncheckAll");
			for (var i = 0; i < res.useFileClass.length; i++){
				$("#editFileClass").datalist("checkRow", $("#editFileClass").datalist("getRowIndex", res.useFileClass[i]));
			}
			$("#editUserId").combobox("loadData", []);
			$("#editUserName").textbox("setValue", "");
			$("#editUserRole").combobox("setValue", "E");
			$("#editUserList").datagrid("loadData", res.userList);
			$("#editModBut").prop("disabled", !res.allowMod);
			$("#editEditBut").prop("disabled", !res.allowMod);
		}
	}, "json");
}

/*
 * 儲存教案(A:新增，M:修改)。
 */
function qsEditDone() {
	var req = {};
	req.qsId = $("#editQsId").textbox("getValue");
	req.qsName = $("#editQsName").textbox("getValue");
	req.qsSubject = $("#editQsSubject").textbox("getValue");
	req.focal = $("#editFocal").textbox("getValue");
	req.departId = $("#editDepartId").combobox("getValue");
	req.targetId = $("#editTargetId").combobox("getValue");
	req.qstmpId = $("#editQstmpId").combogrid("getValue");
	req.fileMaxSize = $("#editFileMaxSize").textbox("getValue");
	req.remark = $("#editRemark").textbox("getValue");
	var qsClassList = $("#editQsClass").datagrid("getChecked");
	for (var i = 0; i < qsClassList.length; i++)
		req["qsClassList[" + i + "]"] = qsClassList[i].value;
	var qsAbilityList = $("#editQsAbility").datagrid("getChecked");
	for (var i = 0; i < qsAbilityList.length; i++)
		req["qsAbilityList[" + i + "]"] = qsAbilityList[i].value;
	var fileClassList = $("#editFileClass").datagrid("getChecked");
	for (var i = 0; i < fileClassList.length; i++)
		req["fileClassList[" + i + "]"] = fileClassList[i].value;
	var userList = $("#editUserList").datagrid("getData").rows;
	for (var i = 0; i < userList.length; i++) {
		req["roleFlag[" + i + "]"] = userList[i].roleFlag;
		req["userId[" + i + "]"] = userList[i].userId;
	}
	if ($("#qsEditMode").val() == "A") {
		$.post("QsCrtMast_addQs", req, function (res) {
			parent.showStatus(res);
			if (res.success) {
				$("#qsEdit").dialog("close");
				qryQsList("T");
			}
		}, "json");
	}
	else if ($("#qsEditMode").val() == "M") {
		req.qsIdOrg = $("#editQsIdOrg").textbox("getValue");
		$.post("QsCrtMast_modQs", req, function (res) {
			parent.showStatus(res);
			if (res.success) {
				$("#qsEdit").dialog("close");
				qryQsList("S");
			}
		}, "json");
	}
}

/*
 * 將此新建教案送出至編輯模式
 */
function qsEditMode() {
	var req = {};
	req.qsId = $("#editQsIdOrg").textbox("getValue");
	$.messager.confirm({
		title: '確認',
		msg: '是否要將教案送出編輯？',
		ok: '送出',
		cancel: '取消',
		fn: function (ok) {
			if (ok) {
				$.post("QsCrtMast_editMode", req, function (res) {
					parent.showStatus(res);
					if (res.success) {
						$("#qsEdit").dialog("close");
						qryQsList("T");
					}
				}, "json");
			}
		}
	});
}

/*
 * 刪除教案。
 */
function delQs(src) {
	var jrow = $(src).parent().parent().parent();
	$("#qsList tr.select").removeClass("select");
	jrow.addClass("select");
	var req = {};
	req.qsId = jrow.find("[name='qsId']").val();
	$.messager.confirm({
		title: '確認',
		msg: '是否要刪除教案資料？',
		ok: '刪除',
		cancel: '取消',
		fn: function (ok) {
			$("#qsList tr.select").removeClass("select");
			if (ok) {
				$.post("QsCrtMast_delQs", req, function (res) {
					parent.showStatus(res);
					if (res.success) {
						qryQsList("T");
					}
				}, "json");
			}
		}
	});
}

function qsEditClose() {
	$("#qsList tr.select").removeClass("select");
}

/*
 * 列表新增人員。
 */
function addMember() {
	var req = {};
	req.qsId = $("#editQsId").textbox("getValue");
	req.userId = $("#editUserId").textbox("getValue");
	req.userRole = $("#editUserRole").combobox("getValue");
	$.post("QsCrtMast_addMemberCheck", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			var userList = $("#editUserList").datagrid("getData").rows;
			var roleFlag = $("#editUserRole").combobox("getValue");
			var found = false;
			for (var i = 0; i < userList.length && !found; i++)
				if (userList[i].roleFlag == roleFlag && userList[i].userId == req.userId)
					found = true;
			if (found)
				parent.showStatus({ statusTime: '', status: '人員已存在，不可以重複'});
			else {
				$("#editUserList").datagrid("appendRow", res.user);
			}
		}
	}, "json");
}

function editUserListActFmt(value, row, index) {
	if ("${queryHide}" != "")
		return "";
	return "<button type='button' style='width: 70px;' onclick='editUserListDel(this);'>刪除</button>\n" +
		"<input type='hidden' value='" + row.roleFlag + row.userId + "' />\n";
}

/*
 * 列表刪除人員。
 */
function editUserListDel(src) {
	var req = {};
	var key = $(src).next().val();
	var rowList = $("#editUserList").datagrid("getData").rows;
	var index;
	for (index = rowList.length - 1; index >= 0; index--)
		if (rowList[index].roleFlag + rowList[index].userId == key)
			break;
	if (index >= 0)
		$("#editUserList").datagrid("deleteRow", index);
}

</script>
</head>
<body>
<div style="margin: 10px 0 0 10px;">
	<table class="cond">
		<tr>
			<td>教案代碼</td>
			<td>教案名稱</td>
			<td>狀態</td>
			<td>科別</td>
			<td>對象</td>
			<td>測驗類別</td>
			<td>核心能力</td>
			<td>參與人員</td>
			<td>創建日期範圍</td>
			<td rowspan="2">
				<button type="button" style="width: 100px;" onclick="qryQsList('N');">查詢教案</button>
			</td>
			<td rowspan="2">
				<button type="button" style="width: 100px;" ${queryHide} onclick="addQs();">新增教案</button>
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
				<input id="qryStatFlag" class="easyui-switchbutton" style="width: 80px;" data-options="offText: '新建', onText: '全部'"/>
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
				<td style="width: 60px;" class="headCol4">狀態</td>
				<td style="width: 100px;" class="headCol5">
					<span class="orderField" onclick="chgOrder(this);">科別</span>
					<span class="orderMark" order="departId">&nbsp;</span>
				</td>
				<td style="width: 100px;" class="headCol6">
					<span class="orderField" onclick="chgOrder(this);">對象</span>
					<span class="orderMark" order="targetId">&nbsp;</span>
				</td>
				<td style="width: 100px;" class="headCol7">
					<span class="orderField" onclick="chgOrder(this);">創建日期</span>
					<span class="orderMark" order="crDate">&nbsp;</span>
				</td>
				<td style="width: 100px;" class="headCol6">管理人員</td>
				<td style="width: 160px;" class="headCol5">處理動作</td>
			</tr>
		</table>
	</div>
	<div style="clear: both; height: 380px; overflow: auto;">
		<table id="qsList" class="listData">
		</table>
	</div>
</div>
<div id="qsEdit" class="easyui-dialog" style="width: 1050px; height: 480px; display: none;"
	data-options="modal: true, title: '編輯教案資料', closed: true, onClose: qsEditClose">
	<input id="qsEditMode" type="hidden" />
	<div style="float: left; margin: 10px 0 0 10px;">
		<table>
			<tr class="modPane">
				<td>原教案代碼</td>
				<td>
					<input id="editQsIdOrg" class="easyui-textbox" style="width: 250px;" data-options="readonly: true" />
				</td>
			</tr>
			<tr>
				<td>教案代碼</td>
				<td>
					<input id="editQsId" class="easyui-textbox" style="width: 250px;" />
				</td>
			</tr>
			<tr>
				<td>教案名稱</td>
				<td>
					<input id="editQsName" class="easyui-textbox" style="width: 250px;" />
				</td>
			</tr>
			<tr>
				<td>主旨</td>
				<td>
					<input id="editQsSubject" class="easyui-textbox" style="width: 250px; height: 50px;" data-options="multiline: true" />
				</td>
			</tr>
			<tr>
				<td>編輯重點</td>
				<td>
					<input id="editFocal" class="easyui-textbox" style="width: 250px; height: 50px;" data-options="multiline: true" />
				</td>
			</tr>
			<tr>
				<td>選用樣版</td>
				<td>
					<div id="editQstmpId" class="easyui-combogrid" style="width: 250px;"
						 data-options="title: '教案樣版', singleSelect: true, panelWidth: 300, panelHeight: 180,
						 	idField: 'qstmpId', textField: 'qstmpName',
						 	columns: [[
						 		{ field: 'departName', title: '科別', width: 80 },
						 		{ field: 'qstmpName', title: '樣版名稱', width: 200 },
						 		{ field: 'qstmpId', hidden: true },
						 	]] 
						 ">
					</div>
				</td>
			</tr>
			<tr>
				<td>科別</td>
				<td>
					<input id="editDepartId" class="easyui-combobox" style="width: 250px;" data-options="editable: false, panelHeight: 'auto'"/>
				</td>
			</tr>
			<tr>
				<td>對象</td>
				<td>
					<input id="editTargetId" class="easyui-combobox" style="width: 250px;" data-options="editable: false, panelHeight: 'auto'"/>					
				</td>
			</tr>
			<tr>
				<td>檔案限制大小</td>
				<td>
					<input id="editFileMaxSize" class="easyui-textbox" style="width: 250px;"/>					
				</td>
			</tr>
			<tr>
				<td>備註</td>
				<td>
					<input id="editRemark" class="easyui-textbox" style="width: 250px; height: 80px;" data-options="multiline: true" />
				</td>
			</tr>
			<tr>
				<td colspan="2" style="text-align: center;">
					<button id="editModBut" type="button" style="width: 100px;" onclick="qsEditDone();">儲存教案</button>
					&nbsp;&nbsp;&nbsp;&nbsp;
					<button id="editEditBut" type="button" style="width: 100px;" onclick="qsEditMode();">送出編輯</button>
				</td>
			</tr>
		</table>
	</div>
	<div style="float: left; margin: 10px 0 0 10px;">
		<div id="editQsClass" class="easyui-datalist" style="width: 200px; height: 120px;"
			 data-options="title: '測驗類別', singleSelect: false, checkbox: true, idField: 'value'">
		</div>
		<div style="height: 2px;">&nbsp;</div>
		<div id="editQsAbility" class="easyui-datalist" style="width: 200px; height: 120px;"
			 data-options="title: '核心能力', singleSelect: false, checkbox: true, idField: 'value' ">
		</div>
		<div style="height: 2px;">&nbsp;</div>
		<div id="editFileClass" class="easyui-datalist" style="width: 200px; height: 120px;"
			 data-options="title: '必需檔案類別', singleSelect: false, checkbox: true, idField: 'value' ">
		</div>
	</div>
	<div style="float: left; margin: 10px 0 0 10px;">
		<div style="margin: 10px 0 0 10px;">
			帳號:
			<input id="editUserId" class="easyui-combobox" style="width: 150px;" data-options="loader: qryMemberList, mode: 'remote'"/>
			&nbsp;
			類別:
			<select id="editUserRole" class="easyui-combobox" style="width: 150px;" data-options="editable: false, panelHeight: 'auto'">
				<option value="E">編輯人員</option>
				<option value="V">審核人員</option>
				<option value="N">加註人員</option>
			</select>
			<button type="button" onclick="addMember();">加入人員</button>
		</div>
		<div style="margin: 10px 0 0 10px;">
			<div id="editUserList" class="easyui-datagrid" style="width: 450px; height: 350px;"
				 data-options="title: '人員列表', singleSelect: true, idField: 'key',
				 	columns: [[
				 		{ field: 'roleDesc', title: '類別', halign: 'center', width: 80 },
				 		{ field: 'userId', title: '帳號', halign: 'center', width: 100 },
				 		{ field: 'userName', title: '姓名', halign: 'center', width: 100 },
				 		{ field: 'date', title: '加入日期', halign: 'center', width: 80 },
				 		{ field: 'action', title: '動作', halign: 'center', width: 80, formatter: editUserListActFmt },
				 		{ field: 'roleFlag', hidden: true },
				 	]] 
				 ">
			</div>
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
</script>
</html>