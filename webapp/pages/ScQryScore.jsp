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

var queryTmplData = {};

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
		qryRdList();
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
 * 查詢梯次。
 */
function qryRdList(mode) {
	var req = {};
	req.mode = mode;
	if (mode == "N") {
		$("#pg").pagination({ total: 1, pageNumber: 1 });
		var queryCond1 = {};
		queryCond1._job     = 'ScQryScore';
		queryCond1.rdId     = $("#qryRdId"    ).textbox("getValue");
		queryCond1.rdDesc   = $("#qryRdDesc"  ).textbox("getValue");
		queryCond1.rdBDate  = $("#qryRdBDate" ).datebox("getValue");
		queryCond1.rdEDate  = $("#qryRdEDate" ).datebox("getValue");
		queryCond1.qsId     = $("#qryQsId"    ).textbox("getValue");
		queryCond1.examiner = $("#qryExaminer").textbox("getValue");
		queryCond1.examinee = $("#qryExaminee").textbox("getValue");
		window.queryCond1 = queryCond1;
		queryTmplData = queryCond1;
	}
	//else {
	//	if (!window.queryCond1 || window.queryCond1._job != 'ScMntMast')
	//		return;
	//}
	
	for (var i in window.queryCond1)
		if (i != "_job")
			req[i] = window.queryCond1[i];
	var orders = $("#order").combobox("getValues");
	for (var i = 0; i < orders.length; i++)
		req["order[" + i + "]"] = orders[i];
	req.pageRow = $("#pg").pagination("options").pageSize;
	req.pageAt = $("#pg").pagination("options").pageNumber;
	$.post("ScQryScore_qryRdList", req, function (res) {
		if (mode != "S" && mode != "T")
			parent.showStatus(res);
		if (res.success) {
			if ('total' in res)
				$("#pg").pagination({ total: res.total });
			var jqsList = $("#rdList");
			var widthSum = 15;
			var widthList = jqsList.parent().prev().find("table.listHead td").map(function () { widthSum += $(this).width();	return $(this).width(); });
			jqsList.parent().css("width", widthSum.toString() + "px");
			jqsList.find("tr").remove();
			
			for (var i = 0; i < res.rdList.length; i++) {
				var rd = res.rdList[i];
				var wat = 0;
				var url = 'ScMntSect?p_b_progId[0]=ScQryScore&rdId=' + rd.rdId + '&showType=S';
				var j = 0;
    			$.each(queryTmplData, function(f,v){
    				url += "&p_b_ScQryScore["+ (j++) +"]=" + f + ":" + v;
    			});
				var append = "<tr class='" + (i & 1 ? "odd" : "even") + "'>\n" +
					"<td>\n" +
					"    <input name='rdId'    type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + rd.rdId    + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='rdDesc'  type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + rd.rdDesc  + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='rdDate'  type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + rd.rdDate  + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='begTime' type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + rd.begTime + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <span style='width: " + widthList[wat++] + "px; display: inline-block; text-align: center;'>\n" +
					//'        <button class="easyui-linkbutton" onclick="parent.selProg(\'ScMntSect?progId='+ res.progId+'&paneBackup='+queryCond1+'\');">檢視</button>'+
					//'        <button class="easyui-linkbutton" onclick="parent.selProg(\'ScSetScore?progId=' + $("#progId").val() + '&paneBackup=' + JSON.stringify(paneBackup) + '\');">檢視</button>'+
					'        <button class="easyui-linkbutton" onclick="parent.selProg(\'' + url + '\');">檢視</button>' +
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
	$.post("ScMntItem_qryQs", req, function (res) {
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
	$("#qsEdit").dialog("open");
/*	var jrow = $(src).parent().parent().parent();
	var req = {};
	req.qsId = jrow.find("[name='qsId']").val();
	$.post("ScQryScore_qryQs", req, function (res) {
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
				$("#item").textbox("setValue", req.qsId);
				$("#itemDes").textbox("setValue", req.qsId);
				$("#itemScr").textbox("setValue", res.qsName);
			$("#editQsSubject").textbox("setValue", res.qsSubject);
			$("#editFocal").textbox("setValue", res.focal);
				$("#scr").combogrid("setValue", res.qstmpId);
				$("#desc").combobox("setValue", res.departId);
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
	}, "json");*/
	/*var res = {};
	itemCnt += 1;
	res.item    = itemCnt;
	res.itemDes = $("#editDes").textbox("getValue");
	res.itemScr = $("#editGra").textbox("getValue");
	$("#itemList").datagrid("appendRow", res);

	if ( descCnt == 0 ) {
		var res2 = {};
		descCnt += 1;
		res2.scr  = descCnt;
		res2.desc = "說明" + descCnt;
		$("#descList").datagrid("appendRow", res2);
		descCnt += 1;
		res2.scr  = descCnt;
		res2.desc = "說明" + descCnt;
		$("#descList").datagrid("appendRow", res2);
		descCnt += 1;
		res2.scr  = descCnt;
		res2.desc = "說明" + descCnt;
		$("#descList").datagrid("appendRow", res2);
		descCnt += 1;
		res2.scr  = descCnt;
		res2.desc = "說明" + descCnt;
		$("#descList").datagrid("appendRow", res2);
		descCnt += 1;
		res2.scr  = descCnt;
		res2.desc = "說明" + descCnt;
		$("#descList").datagrid("appendRow", res2);
	}*/
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
		$.post("ScMntMast_addQs", req, function (res) {
			parent.showStatus(res);
			if (res.success) {
				$("#qsEdit").dialog("close");
				qryRdList("T");
			}
		}, "json");
	}
	else if ($("#qsEditMode").val() == "M") {
		req.qsIdOrg = $("#editQsIdOrg").textbox("getValue");
		$.post("ScMntMast_modQs", req, function (res) {
			parent.showStatus(res);
			if (res.success) {
				$("#qsEdit").dialog("close");
				qryRdList("S");
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
				$.post("ScMntMast_editMode", req, function (res) {
					parent.showStatus(res);
					if (res.success) {
						$("#qsEdit").dialog("close");
						qryRdList("T");
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
				$.post("ScMntMast_delQs", req, function (res) {
					parent.showStatus(res);
					if (res.success) {
						qryRdList("T");
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
	$.post("ScMntMast_addMemberCheck", req, function (res) {
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

function editComm() {
	$("#edComm").dialog("open");
}

function editPic() {
	$("#edPic").dialog("open");
}

</script>
</head>
<body>
<div style="margin: 10px 0 0 10px;">
	<table class="cond">
		<tr>
			<td>梯次代碼</td>
			<td>梯次名稱</td>
			<td colspan="2" align="center">考試日期範圍</td>
			<td>教案</td>
			<td>考官</td>
			<td>考生</td>
			<td rowspan="2">
				<button type="button" style="width: 100px;" onclick="qryRdList('N');">查詢評分</button>
			</td>
		</tr>
		<tr>
				<td>
					<input id="qryRdId"     class="easyui-textbox"  style="width: 100px;" />
				</td>
				<td>
					<input id="qryRdDesc"   class="easyui-textbox"  style="width: 100px;" />
				</td>
				<td>
					<input id="qryRdBDate"  class="easyui-datebox"  style="width: 100px;" />
				</td>
				<td>
					<input id="qryRdEDate"  class="easyui-datebox"  style="width: 100px;" />
				</td>
				<td>
					<input id="qryQsId"     class="easyui-textbox"  style="width: 100px;" />
				</td>
				<td>
					<input id="qryExaminer" class="easyui-textbox"  style="width: 100px;" />
				</td>
				<td>
					<input id="qryExaminee" class="easyui-textbox"  style="width: 100px;" />
				</td>
		</tr>
	</table>
</div>
<div style="margin: 10px 0 0 10px; height: 30px; ">
	<div style="float: left;">
		<div id="pg" class="easyui-pagination" style="width: 400px;" data-options="onSelectPage: qryRdList, showRefresh: false, total: 0" ></div>
	</div>
	<div style="float: left; position: relative; top: 2px; margin-left: 20px;">
		排序：
		<select id="order" class="easyui-combobox" data-options="multiple: true, panelHeight: 'auto', onHidePanel: qryRdList, onChange: chgOrder" style="width: 250px;">
			<option value="rdId:A"  >代碼&#x25B2;</option>
			<option value="rdId:D"  >代碼&#x25BC;</option>
			<option value="rdDate:A">日期&#x25B2;</option>
			<option value="rdDate:D">日期&#x25BC;</option>
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
					<span class="orderField" onclick="chgOrder(this);">梯次代碼</span>
					<span class="orderMark" order="rdId">&#x25B2;</span>
				</td>
				<td style="width: 220px;" class="headCol2">
					<span class="orderField" onclick="chgOrder(this);">梯次名稱</span>
				</td>
				<td style="width: 100px;" class="headCol3">
					<span class="orderField" onclick="chgOrder(this);">考試日期</span>
					<span class="orderMark" order="rdDate">&nbsp;</span>
				</td>
				<td style="width: 100px;" class="headCol4">
					<span class="orderField" onclick="chgOrder(this);">開始時間</span>
					<span class="orderMark" order="begTime">&nbsp;</span>
				</td>
				<td style="width: 80px;" class="headCol4">處理動作</td>
			</tr>
		</table>
	</div>
	<div style="clear: both; height: 380px; overflow: auto;">
		<table id="rdList" class="listData">
		</table>
	</div>
</div>
<div id="qsEdit" class="easyui-dialog" style="width: 990px; height: 460px; display: none;"
	data-options="modal: true, title: '檢視評分表', closed: true, onClose: qsEditClose">
	<input id="qsEditMode" type="hidden" />
	<div style="float: left; margin: 10px 0 0 10px;">
		<table>
			<tr class="modPane">
				<td>場次代碼</td>
				<td>
					<input id="rdId" class="easyui-textbox" style="width: 100px;" />
				</td>
				<td>&nbsp;&nbsp;場次名稱</td>
				<td>
					<input id="rdDesc" class="easyui-textbox" style="width: 243px;" />
				</td>
				<td>&nbsp;&nbsp;節次/堂次</td>
				<td>
					<input id="sectSeq" class="easyui-textbox" style="width: 30px;" />/
					<input id="roomSeq" class="easyui-textbox" style="width: 30px;" />
				</td>
				<td>&nbsp;&nbsp;考官</td>
				<td>
					<input id="examiner" class="easyui-textbox" style="width: 100px;" />
				</td>
				<td>&nbsp;&nbsp;考生</td>
				<td>
					<input id="examinee" class="easyui-textbox" style="width: 100px;" />
				</td>
			</tr>
			<tr>
				<td>教案代碼</td>
				<td>
					<input id="qsId" class="easyui-textbox" style="width: 100px;" />
				</td>
				<td>&nbsp;&nbsp;教案說明</td>
				<td>
					<input id="qsName" class="easyui-textbox" style="width: 243px;" />
				</td>
				<td>&nbsp;&nbsp;病人1</td>
				<td>
					<input id="patient1" class="easyui-textbox" style="width: 100px;" />
				</td>
				<td>&nbsp;&nbsp;病人2</td>
				<td>
					<input id="patient2" class="easyui-textbox" style="width: 100px;" />
				</td>
				<td>&nbsp;&nbsp;病人3</td>
				<td>
					<input id="patient3" class="easyui-textbox" style="width: 100px;" />
				</td>
			</tr>
			<tr>
				<td colspan="9" rowspan="14">
					<div style="margin: 10px 0 0 0;">
						<div id="itemList" class="easyui-datagrid" style="width: 770px; height: 300px;"
							 data-options="title: '項目列表', singleSelect: true, idField: 'key',
							 	columns: [[
							 		{ field: 'itemNo', title: '項目序號', halign: 'center', width: 80 },
							 		{ field: 'itemDesc', title: '項目描述', halign: 'center', width: 320 },
							 		{ field: 'optId', title: '級分', halign: 'center', width: 40 },
							 		{ field: 'examComm', title: '註記', halign: 'center', width: 40 },
							 		{ field: 'examPic', title: '考官塗鴉', halign: 'center', width: 40 },
							 	]]">
						</div>
					</div>
				</td>
			</tr>
			<tr>
				<td>考試結果</td>
			</tr>
			<tr>
				<td>
					<select id="examResult" class="easyui-combobox" style="width: 100px;" data-options="editable: false, panelHeight: 'auto'">
                        <option value="P">Pass</option>
                        <option value="B">Borderline</option>
                        <option value="F">Fail</option>
                    </select>
                </td>
            </tr>
            <tr></tr>
			<tr>
				<td>整體分數</td>
			</tr>
			<tr>
				<td>
					<select id="examOptId" class="easyui-combobox" style="width: 100px;" data-options="editable: false, panelHeight: 'auto'">
                        <option value="1">1</option>
                        <option value="2">2</option>
                        <option value="3">3</option>
                        <option value="4">4</option>
                        <option value="5">5</option>
                        <option value="6">6</option>
                        <option value="7">7</option>
                        <option value="8">8</option>
                        <option value="9">9</option>
                        <option value="10">10</option>
                    </select>
                 </td>
			</tr>
			<tr></tr>
			<tr>
				<td>註記</td>
			</tr>
			<tr>
				<td>
					<button type="button" style="width: 100px;" onclick="editComm();">檢視</button>
				</td>
			</tr>
			<tr></tr>
			<tr>
				<td>考官塗鴉</td>
			</tr>
			<tr>
				<td>
					<button type="button" style="width: 100px;" onclick="editPic();">檢視</button>
				</td>
			</tr>
			<tr></tr>
			<tr>
                <td>
                    <button id="editModBut" type="button" style="width: 100px;" onclick="qsEditDone();">儲存評分</button>
                </td>
            </tr>
		</table>
	</div>
	
</div>

<div id="edComm" class="easyui-dialog" style="width: 500px; height: 300px; display: none;"
	data-options="modal: true, title: '檢視註記', closed: true">
	<div style="float: left; margin: 10px 0 0 10px;">
		<table>
			<tr></tr>
		</table>
	</div>
</div>
<div id="edPic" class="easyui-dialog" style="width: 900px; height: 400px; display: none;"
	data-options="modal: true, title: '檢視考官塗鴉', closed: true">
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