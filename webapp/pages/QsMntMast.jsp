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
	$("#qryMastFlag").combobox("setValue", "");
	$("#qryUserId").combobox("clear");
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
	$.post("QsMntMast_qryMemberList", req, function (res) {
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
		queryCond1._job = 'QsMntMast';
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
		queryCond1.mastFlag = $("#qryMastFlag").combobox("getValue");
		queryCond1.userId = $("#qryUserId").textbox("getValue");
		queryCond1.beginDate = $("#qryBeginDate").datebox("getValue");
		queryCond1.endDate = $("#qryEndDate").datebox("getValue");
		window.queryCond1 = queryCond1;
	}
	else {
		if (!window.queryCond1 || window.queryCond1._job != 'QsMntMast')
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
	$.post("QsMntMast_qryQsList", req, function (res) {
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
					"    <input name='mastFlag' type='text' style='width: " + widthList[wat++] + "px; text-align: center;' readonly value='" + qs.mastFlag + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <span style='width: " + widthList[wat++] + "px; display: inline-block; text-align: center;'>\n" +
					"        <button type='button' style='width: 70px;' " + queryHide + " onclick='editQsFile(this);'>檔案</button>\n" +
					"        &nbsp;" +
					"        <button type='button' style='width: 70px;' " + queryHide + " onclick='editQsDetail(this);'>內容</button>\n" +
					"        &nbsp;" +
					"        <button type='button' style='width: 70px;' " + queryHide + " onclick='editQsUser(this);'>人員</button>\n" +
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
	$.post("QsMntMast_qryQs", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			jrow.addClass("select");
			$("#qsEditFile").dialog("open");
			$("#fileQsId").val(req.qsId);
			$("#fileList").datagrid("loadData", res.fileList);
			$("#qsEditFileEdit").css("display", "none");
			$("#qsEditFileValid").css("display", "none");
		}
	}, "json");
}

function fileListSelect() {
	$("#qsEditFileEdit").css("display", "none");
	$("#qsEditFileValid").css("display", "none");
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
		"<input type='hidden' value='" + row.fileName + "' />\n";
}

function fileListSel(src) {
	var req = {};
	req.qsId = $("#fileQsId").val();
	req.fileName = $(src).next().next().next().val();
	req.qryType = "D";
	$.post("QsMntMast_qryFile", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#fileServerName").val(req.fileName);
			$("#qsEditFileEdit").css("display", "none");
			$("#qsEditFileValid").css("display", "none");
			if (res.editStat) {
				$("#qsEditFileEdit").css("display", "table");
				$("#fileEditDesc").textbox("setValue", res.fileDesc);			
				$("#fileEditClass").textbox("setValue", res.fileClass);
				$("#fileEditRemark").textbox("setValue", res.remark);			
			}
			else if (res.validStat) {
				$("#qsEditFileValid").css("display", "table");
				$("#fileValidDesc").textbox("setValue", res.fileDesc);
				$("#fileValidUser").addClass("inProg");
				$("#fileValidUser").combobox("loadData", res.userList);
				$("#fileValidUser").combobox("clear");
				$("#fileValidUser").removeClass("inProg");
				$("#fileValidDate").textbox("setValue", "");
				$("#fileValidFlag").textbox("setValue", "");
				$("#fileValidReason").textbox("setValue", "");
			}
		}
	}, "json");
}

function fileListOpen(src) {
	var url = "QsMntMast_download";
	url += "?qsId=" + encodeURIComponent($("#fileQsId").val());
	url += "&fileName=" + encodeURIComponent($(src).next().next().val());
	url += "&open=Y";
	window.open(url);
}

function fileListDownload(src) {
	var url = "QsMntMast_download";
	url += "?qsId=" + encodeURIComponent($("#fileQsId").val());
	url += "&fileName=" + encodeURIComponent($(src).next().val());
	url += "&open=N";
	window.open(url);
}

function fileValidUserSel(rec) {
	if ($("#fileValidUser").hasClass("inProg"))
		return;
	var req = {};
	req.qsId = $("#fileQsId").val();
	req.fileName = $("#fileServerName").val();
	req.userId = rec.value;
	req.qryType = "V";
	$.post("QsMntMast_qryFile", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#fileValidDate").textbox("setValue", res.validDate);
			$("#fileValidFlag").textbox("setValue", res.validFlag);
			$("#fileValidReason").textbox("setValue", res.reason);
		}
	}, "json");
}

function qsEditClose() {
	$("#qsList tr.select").removeClass("select");
}

function editQsDetail(src) {
	var jrow = $(src).parent().parent().parent();
	var req = {};
	req.qsId = jrow.find("[name='qsId']").val();
	req.qryType = "D";
	$.post("QsMntMast_qryQs", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			jrow.addClass("select");
			$("#qsEditDetail").dialog("open");
			$("#detailQsIdOrg").textbox("setValue", req.qsId);
			$("#detailQsId").textbox("setValue", req.qsId);
			$("#detailQsName").textbox("setValue", res.qsName);
			$("#detailVerStat").textbox("setValue", res.currVer + " " + res.statFlagDesc);
			$("#detailQsSubject").textbox("setValue", res.qsSubject);
			$("#detailDepartId").combobox("setValue", res.departId);
			$("#detailTargetId").combobox("setValue", res.targetId);
			$("#detailFocal").textbox("setValue", res.focalRemark);
			$("#detailQsClass").datalist("loadData", JSON.parse($("#detailQsClassData").val()));
			$("#detailQsClass").datalist("uncheckAll");
			for (var i = 0; i < res.useQsClass.length; i++)
				$("#detailQsClass").datalist("checkRow", $("#detailQsClass").datalist("getRowIndex", res.useQsClass[i]));
			$("#detailQsAbility").datalist("loadData", JSON.parse($("#detailQsAbilityData").val()));
			$("#detailQsAbility").datalist("uncheckAll");
			for (var i = 0; i < res.useQsAbility.length; i++)
				$("#detailQsAbility").datalist("checkRow", $("#detailQsAbility").datalist("getRowIndex", res.useQsAbility[i]));
			$("#setEditBut").prop("disabled", !res.canEdit);
			$("#setValidBut").prop("disabled", !res.canValid);
			$("#setPassBut").prop("disabled", !res.canPass);
			$("#setOnlineBut").prop("disabled", !res.canOnline);
			$("#setAbandonBut").prop("disabled", !res.canAbandon);
			$("#resendMailBut").prop("disabled", !res.canResend);
		}
	}, "json");
}

function modQs() {
	var req = {};
	req.qsIdOrg = $("#detailQsIdOrg").textbox("getValue");
	req.qsId = $("#detailQsId").textbox("getValue");
	req.qsName = $("#detailQsName").textbox("getValue");
	req.qsSubject = $("#detailQsSubject").textbox("getValue");
	req.departId = $("#detailDepartId").combobox("getValue");
	req.targetId = $("#detailTargetId").combobox("getValue");
	req.focalRemark = $("#detailFocal").textbox("getValue");
	var qsClassList = $("#detailQsClass").datalist("getChecked");
	for (var i = 0; i < qsClassList.length; i++)
		req["qsClass[" + i + "]"] = qsClassList[i].value;
	var qsAbilityList = $("#detailQsAbility").datalist("getChecked");
	for (var i = 0; i < qsAbilityList.length; i++)
		req["qsAbility[" + i + "]"] = qsAbilityList[i].value;
	$.post("QsMntMast_modQs", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#qsEditDetail").dialog("close");
			qryQsList("S");
		}
	}, "json");
}

function setEditQs() {
	var req = {};
	req.qsId = $("#detailQsIdOrg").textbox("getValue");
	$.messager.confirm({
		title: '確認',
		msg: '是否要將此教案設定為編輯模式？',
		ok: '設定',
		cancel: '取消',
		fn: function (ok) {
			if (ok) {
				$.post("QsMntMast_editQs", req, function (res) {
					parent.showStatus(res);
					if (res.success) {
						$("#editVerStat").textbox("setValue", res.verStat);
						if (res.success) {
							$("#qsEditDetail").dialog("close");
							qryQsList("S");
						}
					}
				});
			}
		}
	});
}

function setValidQs() {
	var req = {};
	req.qsId = $("#detailQsIdOrg").textbox("getValue");
	$.messager.confirm({
		title: '確認',
		msg: '是否要將此教案設定為審核模式？',
		ok: '設定',
		cancel: '取消',
		fn: function (ok) {
			if (ok) {
				$.post("QsMntMast_validQs", req, function (res) {
					parent.showStatus(res);
					if (res.success) {
						$("#qsEditDetail").dialog("close");
						qryQsList("S");
					}
				});
			}
		}
	});
}

function setPassQs() {
	var req = {};
	req.qsId = $("#detailQsIdOrg").textbox("getValue");
	$.messager.confirm({
		title: '確認',
		msg: '是否要將此教案設定為審核通過模式？',
		ok: '設定',
		cancel: '取消',
		fn: function (ok) {
			if (ok) {
				$.post("QsMntMast_passQs", req, function (res) {
					parent.showStatus(res);
					if (res.success) {
						$("#qsEditDetail").dialog("close");
						qryQsList("S");
					}
				});
			}
		}
	});
}

function setOnlineQs() {
	var req = {};
	req.qsId = $("#detailQsIdOrg").textbox("getValue");
	$.messager.confirm({
		title: '確認',
		msg: '是否要將此教案設定為上線模式？',
		ok: '設定',
		cancel: '取消',
		fn: function (ok) {
			if (ok) {
				$.post("QsMntMast_onlineQs", req, function (res) {
					parent.showStatus(res);
					if (res.success) {
						$("#qsEditDetail").dialog("close");
						qryQsList("S");
					}
				});
			}
		}
	});
}

function setAbandonQs() {
	var req = {};
	req.qsId = $("#detailQsIdOrg").textbox("getValue");
	$.messager.confirm({
		title: '確認',
		msg: '是否要將此教案設定為廢止模式？',
		ok: '設定',
		cancel: '取消',
		fn: function (ok) {
			$.post("QsMntMast_abandonQs", req, function (res) {
				parent.showStatus(res);
				if (res.success) {
					$("#qsEditDetail").dialog("close");
					qryQsList("S");
				}
			});
		}
	});
}

function resendMail() {
	var req = {};
	req.qsId = $("#detailQsIdOrg").textbox("getValue");
	$.messager.confirm({
		title: '確認',
		msg: '是否要重送此教案的郵件通知？',
		ok: '重送',
		cancel: '取消',
		fn: function (ok) {
			$.post("QsMntMast_resendMail", req, function (res) {
				parent.showStatus(res);
				if (res.success) {
					$("#qsEditDetail").dialog("close");
					qryQsList("S");
				}
			});
		}
	});
}

function editQsUser(src) {
	var jrow = $(src).parent().parent().parent();
	var req = {};
	req.qsId = jrow.find("[name='qsId']").val();
	req.qryType = "U";
	$.post("QsMntMast_qryQs", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			jrow.addClass("select");
			$("#qsEditUser").dialog("open");
			$("#userQsId").val(req.qsId);
			$("#userList").datagrid("loadData", res.userList);
			editQsUserMode("A");
		}
	}, "json");
}

function editQsUserMode(mode) {
	if (mode == "A") {
		$("#userName").combobox({readonly: false});
		$("#userRoleFlag").combobox({readonly: false});
		$("#userRemark").textbox({readonly: false});
		$("#userAddBut").css("display", "inline-block");
		$("#userClearBut").css("display", "none");
	}
	else if (mode == "V") {
		$("#userName").combobox({readonly: true});
		$("#userRoleFlag").combobox({readonly: true});
		$("#userRemark").textbox({readonly: true});
		$("#userAddBut").css("display", "none");
		$("#userClearBut").css("display", "inline-block");
	}
}


function userListActFmt(value, row, index) {
	var queryHide = $("#queryHide").val() != "" || !row.canDel ? "disabled" : "";
	return "<button type='button' style='width: 70px;' " + queryHide + " onclick='userListDel(this);'>停止</button>\n" +
		"<input type='hidden' value='" + row.seqNo + "' />\n";
}

function userListSel(index, row) {
	var req = {};
	req.qsId = $("#userQsId").val();
	req.seqNo = row.seqNo;
	$.post("QsMntMast_qryMember", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			editQsUserMode("V");
			$("#userSeqNo").textbox("setValue", req.seqNo);
			$("#userId").textbox("setValue", res.userId);
			$("#userName").textbox("setValue", res.userName);
			$("#userRoleFlag").combobox("setValue", res.roleFlag);
			$("#userBeginDate").textbox("setValue", res.beginDate);
			$("#userEndDate").textbox("setValue", res.endDate);
			$("#userRemark").textbox("setValue", res.remark);
		}
	}, "json");
}

function userListDel(src) {
	var req = {};
	req.qsId = $("#userQsId").val();
	req.seqNo = $(src).next().val();
	$.messager.confirm({
		title: '確認',
		msg: '是否要停止此教案參與人員？',
		ok: '刪除',
		cancel: '取消',
		fn: function (ok) {
			if (ok) {
				$.post("QsMntMast_delMember", req, function (res) {
					parent.showStatus(res);
					if (res.success) {
						$("#userList").datagrid("loadData", res.userList);
						clearMember();
					}
				}, "json");
			}
		}
	});
}

function addMember() {
	var req = {};
	req.qsId = $("#userQsId").val();
	req.userId = $("#userName").combobox("getValue");
	req.roleFlag = $("#userRoleFlag").combobox("getValue");
	req.remark = $("#userRemark").textbox("getValue");
	$.post("QsMntMast_addMember", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#userList").datagrid("loadData", res.userList);
			clearMember();
		}
	}, "json");
}

function clearMember() {
	editQsUserMode("A");
	$("#userSeqNo").textbox("setValue", "");
	$("#userId").textbox("setValue", "");
	$("#userName").textbox("setValue", "");
	$("#userRoleFlag").combobox("clear");
	$("#userBeginDate").textbox("setValue", "");
	$("#userEndDate").textbox("setValue", "");
	$("#userRemark").textbox("setValue", "");
	$("#userList").datagrid("uncheckAll");
}

function qsEditSave() {
	var req = {};
	req.qsIdOrg = $("#editQsIdOrg").textbox("getValue");
	req.qsId = $("#editQsId").textbox("getValue");
	req.qsName = $("#editQsName").textbox("getValue");
	req.qsSubject = $("#editQsSubject").textbox("getValue");
	req.departId = $("#editDepartId").combobox("getValue");
	req.targetId = $("#editTargetId").combobox("getValue");
	req.qstmpId = $("#editQstmpId").combogrid("getValue");
	req.remark = $("#editRemark").textbox("getValue");
	var qsClassList = $("#qsClassList").datagrid("getChecked");
	for (var i = 0; i < qsClassList.length; i++)
		req["qsClassList[" + i + "]"] = qsClassList[i].roleId;
	var qsAbilityList = $("#qsAbilityList").datagrid("getChecked");
	for (var i = 0; i < qsAbilityList.length; i++)
		req["qsAbilityList[" + i + "]"] = qsAbilityList[i].roleId;
	$.post("QsMntMast_modQs", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
		}
	}, "json");
	
}

function qryMember() {
	var req = {};
	req.userId = $("#editUserId").textbox("getValue");
	req.userName = $("#editUserName").textbox("getValue");
	$.post("QsMnttMast_qryMember", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("editUserId").textbox("setValue", res.userId);
			$("editUserName").textbox("setValue", res.userName);
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
			<td>教案狀態</td>
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
					<option value=""></option>
					${departList}
				</select>
			</td>
			<td>
				<select id="qryTargetId" class="easyui-combobox" style="width: 100px;" data-options="editable: false, panelHeight: 'auto'" >
					<option value=""></option>
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
				<select id="qryMastFlag" class="easyui-combobox" style="width: 100px;" data-options="editable: false, panelHeight: 'auto'" >
					${qsMastFlagList}
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
				<td style="width: 200px;" class="headCol2">
					<span class="orderField" onclick="chgOrder(this);">教案名稱</span>
					<span class="orderMark" order="qsName">&nbsp;</span>
				</td>
				<td style="width: 250px;" class="headCol3">主旨</td>
				<td style="width: 80px;" class="headCol4">
					<span class="orderField" onclick="chgOrder(this);">科別</span>
					<span class="orderMark" order="departId">&nbsp;</span>
				</td>
				<td style="width: 80px;" class="headCol5">
					<span class="orderField" onclick="chgOrder(this);">對象</span>
					<span class="orderMark" order="targetId">&nbsp;</span>
				</td>
				<td style="width: 100px;" class="headCol6">
					<span class="orderField" onclick="chgOrder(this);">創建日期</span>
					<span class="orderMark" order="crDate">&nbsp;</span>
				</td>
				<td style="width: 100px;" class="headCol7">創建人員</td>
				<td style="width: 40px;" class="headCol6">版本</td>
				<td style="width: 60px;" class="headCol5">狀態</td>
				<td style="width: 240px;" class="headCol4">處理動作</td>
			</tr>
		</table>
	</div>
	<div style="clear: both; width: 1080px; height: 380px; overflow: auto;">
		<table id="qsList" class="listData">
		</table>
	</div>
</div>
<div id="qsEditFile" class="easyui-dialog" style="width: 1200px; height: 400px; display: none;"
	data-options="modal: true, title: '查看教案檔案', closed: true, onClose: qsEditClose">
	<input id="fileQsId" type="hidden" />
	<div style="float: left; margin: 10px 0 0 10px;">
		<div id="fileList" class="easyui-datagrid" style="width: 830px; height: 300px;" data-options="
			title: '教案包含檔案列表', singleSelect: true, onSelect: fileListSelect, columns: [[
				{ field: 'fileDesc', title: '檔案說明', halign: 'center', width: 150 },
				{ field: 'fileClass', title: '檔案類別', halign: 'center', width: 100 },
				{ field: 'showOrder', title: '順序', align: 'center', width: 50 },
				{ field: 'crDate', title: '上傳日期', align: 'center', width: 70 },
				{ field: 'userName', title: '上傳人員', halign: 'center', width: 80 },
				{ field: 'fileSize', title: '檔案大小', align: 'right', halign: 'center', width: 80 },
				{ field: 'fileType', title: '檔案型態', halign: 'center', width: 80 },
				{ field: 'action', title: '處理動作', align: 'center', width: 200, formatter: fileListActFmt },
			]] 
			">
		</div>
	</div>
	<div style="float: left; margin: 10px 0 0 10px;">
		<div style="margin: 0 0 0 0;">
			<input id="fileServerName" type="hidden" />
			<table id="qsEditFileEdit" style="display: none;">
				<tr>
					<td>檔案說明</td>
					<td>
						<input id="fileEditDesc" class="easyui-textbox" style="width: 250px;" data-options="readonly: true"/>
					</td>
				</tr>
				<tr>
					<td>檔案類別</td>
					<td>
						<input id="fileEditClass" class="easyui-textbox" style="width: 250px;" data-options="readonly: true"/>
					</td>
				</tr>
				<tr>
					<td>檔案備註</td>
					<td>
						<input id="fileEditRemark" name="remark" class="easyui-textbox" style="width: 250px; height: 100px" data-options="multiline: true, readonly: true" />
					</td>
				</tr>
			</table>
			<table id="qsEditFileValid" style="display: none;">
				<tr>
					<td>檔案說明</td>
					<td>
						<input id="fileValidDesc" class="easyui-textbox" style="width: 250px;" data-options="editable: false, panelHeight: 'auto'" />
					</td>
				</tr>
				<tr>
					<td>審核人員</td>
					<td>
						<input id="fileValidUser" class="easyui-combobox" style="width: 250px;" data-options="editable: false, panelHeight: 'auto', onSelect: fileValidUserSel" />
					</td>
				</tr>
				<tr>
					<td>審核日期</td>
					<td>
						<input id="fileValidDate" class="easyui-textbox" style="width: 250px;" data-options="readonly: true" />
					</td>
				</tr>
				<tr>
					<td>審核結果</td>
					<td>
						<input id="fileValidFlag" class="easyui-textbox" style="width: 250px;" data-options="readonly: true" />
					</td>
				</tr>
				<tr>
					<td>評論</td>
					<td>
						<input id="fileValidReason" class="easyui-textbox" style="width: 250px; height: 150px;" data-options="multiline: true, readonly: true" />
					</td>
				</tr>
			</table>
		</div>
	</div>
</div>
<div id="qsEditDetail" class="easyui-dialog" style="width: 770px; height: 460px; display: none;"
	data-options="modal: true, title: '維護教案', closed: true, onClose: qsEditClose">
	<div style="float: left; margin: 10px 0 0 10px;">
		<table>
			<tr>
				<td>原教案代碼</td>
				<td>
					<input id="detailQsIdOrg" class="easyui-textbox" style="width: 250px;" data-options="readonly: true" />
				</td>
			</tr>
			<tr>
				<td>版本及狀態</td>
				<td>
					<input id="deatilVerStat" class="easyui-textbox" style="width: 250px;" data-options="readonly: true" />
				</td>
			</tr>
			<tr>
				<td>教案代碼</td>
				<td>
					<input id="detailQsId" class="easyui-textbox" style="width: 250px;" />
				</td>
			</tr>
			<tr>
				<td>教案名稱</td>
				<td>
					<input id="detailQsName" class="easyui-textbox" style="width: 250px;" />
				</td>
			</tr>
			<tr>
				<td>主旨</td>
				<td>
					<input id="detailQsSubject" class="easyui-textbox" style="width: 250px; height: 100px;" data-options="multiline: true" />
				</td>
			</tr>
			<tr>
				<td>科別</td>
				<td>
					<select id="detailDepartId" class="easyui-combobox" style="width: 250px;" data-options="panelHeight: 'auto'">
						${departList}
					</select>
				</td>
			</tr>
			<tr>
				<td>對象</td>
				<td>
					<select id="detailTargetId" class="easyui-combobox" style="width: 250px;" data-options="panelHeight: 'auto'">
						${qsTargetList}
					</select>
				</td>
			</tr>
			<tr>
				<td>重點</td>
				<td>
					<input id="detailFocal" class="easyui-textbox" style="width: 250px; height: 80px;" data-options="multiline: true" />
				</td>
			</tr>
			<tr>
				<td colspan="2" style="text-align: center;">
					<button type="button" style="width: 150px;" onclick="modQs();">修改教案</button>
				</td>
			</tr>
		</table>
	</div>
	<div style="float: left; margin: 10px 0 0 10px;">
		<div id="detailQsClass" class="easyui-datalist" style="width: 200px; height: 170px;"
			 data-options="title: '測驗類別', singleSelect: false, checkbox: true, idField: 'value'">
		</div>
		<input id="detailQsClassData" type="hidden" value='${qsClassData}' />
		<div style="height: 10px;">&nbsp;</div>
		<div id="detailQsAbility" class="easyui-datalist" style="width: 200px; height: 170px;"
			 data-options="title: '核心能力', singleSelect: false, checkbox: true, idField: 'value'">
		</div>
		<input id="detailQsAbilityData" type="hidden" value='${qsAbilityData}' />
	</div>
	<div style="float: left; margin: 10px 0 0 50px;">
		<div style="margin: 20px 0 0 0;">
			<button id="setEditBut" type="button" style="width: 120px; margin: 0 0 0 0;" onclick="setEditQs();">轉為編輯模式</button>
		</div>
		<div style="margin: 40px 0 0 0;">
			<button id="setValidBut" type="button" style="width: 120px; margin: 0 0 0 0;" onclick="setValidQs();">轉為審核模式</button>
		</div>
		<div style="margin: 40px 0 0 0;">
			<button id="setPassBut" type="button" style="width: 120px; margin: 0 0 0 0;" onclick="setPassQs();">轉為通過模式</button>
		</div>
		<div style="margin: 40px 0 0 0;">
			<button id="setOnlineBut" type="button" style="width: 120px; margin: 0 0 0 0;" onclick="setOnlineQs();">轉為公告模式</button>
		</div>
		<div style="margin: 40px 0 0 0;">
			<button id="setAbandonBut" type="button" style="width: 120px; margin: 0 0 0 0;" onclick="setAbandonQs();">轉為廢止模式</button>
		</div>
		<div style="margin: 40px 0 0 0;">
			<button id="resendMailBut" type="button" style="width: 120px; margin: 0 0 0 0;" onclick="resendMail();">重發通知訊息</button>
		</div>
	</div>
</div>
<div id="qsEditUser" class="easyui-dialog" style="width: 1000px; height: 400px; display: none;"
	data-options="modal: true, title: '維護教案參與人員', closed: true, onClose: qsEditClose">
	<input id="userQsId" type="hidden"/>
	<div style="float: left; margin: 10px 0 0 10px;">
		<div id="userList" class="easyui-datagrid" style="width: 600px; height: 300px;"
			 data-options="title: '人員列表', singleSelect: true, onSelect: userListSel,
			 	columns: [[
			 		{ field: 'seqNo', title: '序號', align: 'center', width: 50 },
			 		{ field: 'userId', title: '帳號', halign: 'center', width: 120 },
			 		{ field: 'userName', title: '姓名', halign: 'center', width: 120 },
			 		{ field: 'roleDesc', title: '類別', align: 'center', width: 50 },
			 		{ field: 'beginDate', title: '加入日期', align: 'center', width: 80 },
			 		{ field: 'endDate', title: '停止日期', align: 'center', width: 80 },
			 		{ field: 'action', title: '動作', halign: 'center', width: 80, formatter: userListActFmt },
			 		{ field: 'canDel', hidden: true },
			 	]] 
			 ">
		</div>
	</div>
	<div style="float: left; margin: 10px 0 0 10px;">
		<table>
			<tr>
				<td>序號</td>
				<td>
					<input id="userSeqNo" class="easyui-textbox" style="width: 250px;" data-options="readonly: true"/>
				</td>
			</tr>
			<tr>
				<td>帳號</td>
				<td>
					<input id="userId" class="easyui-textbox" style="width: 250px;" data-options="readonly: true"/>
				</td>
			</tr>
			<tr>
				<td>姓名</td>
				<td>
					<input id="userName" class="easyui-combobox" style="width: 250px;" data-options="loader: qryMemberList, mode: 'remote'"/>
				</td>
			</tr>
			<tr>
				<td>角色</td>
				<td>
					<select id="userRoleFlag" class="easyui-combobox" style="width: 250px;" data-options="editable: false, panelHeight: 'auto'">
						${qsRoleList}
					</select>
				</td>
			</tr>
			<tr>
				<td>開始</td>
				<td>
					<input id="userBeginDate" class="easyui-textbox" style="width: 250px;" data-options="readonly: true"/>
				</td>
			</tr>
			<tr>
				<td>結束</td>
				<td>
					<input id="userEndDate" class="easyui-textbox" style="width: 250px;" data-options="readonly: true"/>
				</td>
			</tr>
			<tr>
				<td>備註</td>
				<td>
					<input id="userRemark" class="easyui-textbox" style="width: 250px; height: 100px;" data-options="multiline: true" />
				</td>
			</tr>
			<tr>
				<td>&nbsp;</td>
				<td>&nbsp;</td>
			</tr>
			<tr>
				<td colspan="2" style="text-align: center;">
					<button id="userAddBut" type="button" style="width: 120px;" onclick="addMember();">新增人員</button>
					&nbsp;&nbsp;
					<button id="userClearBut" type="button" style="width: 120px;" onclick="clearMember();">清除資料</button>
				</td>
			</tr>
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