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
#editUserId + .textbox .textbox-text {
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
		qryUserList();
}

function clearAll() {
	$("#qryUserId").textbox("setValue", "");
	$("#qryUserName").textbox("setValue", "");
	$("#qryBeginMode").combobox("setValue", "");
	$("#qryEndMode").combobox("setValue", "");
	$("#qryMenuId").combobox("setValue", "");
	$("#qryRoleId").combobox("setValue", "");
	$("#order").combobox("setValues", [ "userId:A" ]);
	chgOrder();
	$("#pg").pagination({ total: 0 });
	$("#userList tr").remove();
	delete window.queryCond1;
}

function qryUserList(mode) {
	var req = {};
	req.mode = mode;
	if (mode == "N") {
		$("#pg").pagination({ total: 1, pageNumber: 1 });
		var queryCond1 = {};
		queryCond1._job = 'MntUser';
		queryCond1.userId = $("#qryUserId").textbox("getValue");
		queryCond1.userName = $("#qryUserName").textbox("getValue");
		queryCond1.beginMode = $("#qryBeginMode").combobox("getValue");
		queryCond1.endMode = $("#qryEndMode").combobox("getValue");
		queryCond1.menuId = $("#qryMenuId").combobox("getValue");
		var roleIdList = $("#qryRoleId").combobox("getValues");
		for (var i = 0; i < roleIdList.length; i++)
			queryCond1["roleId[" + i + "]"] = roleIdList[i];
		window.queryCond1 = queryCond1;
	}
	else {
		if (!window.queryCond1 || window.queryCond1._job != 'MntUser')
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
	$.post("MntUser_qryUserList", req, function (res) {
		if (mode != "S" && mode != "T")
			parent.showStatus(res);
		if (res.success) {
			if ('total' in res)
				$("#pg").pagination({ total: res.total });
			var queryHide = $("#queryHide").val();
			var juserList = $("#userList");
			juserList.find("tr").remove();
			for (var i = 0; i < res.userList.length; i++) {
				var user = res.userList[i];
				var append = "<tr class='" + (i & 1 ? "odd" : "even") + "'>\n" +
					"<td>\n" +
					"    <input name='userId' type='text' style='width: 100px;' readonly value='" + user.userId + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='userName' type='text' style='width: 100px;' readonly value='" + user.userName + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='menuDesc' type='text' style='width: 150px;' readonly value='" + user.menuDesc + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='beginDate' type='text' style='width: 90px; text-align: center;' readonly value='" + user.beginDate + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='endDate' type='text' style='width: 90px; text-align: center;' readonly value='" + user.endDate + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='roleName' type='text' style='width: 200px;' readonly value='" + user.roleName + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='status' type='text' style='width: 100px;' readonly value='" + user.status + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <span style='width: 160px; display: inline-block; text-align: center;'>\n" +
					"        <button type='button' style='width: 70px;" + queryHide + "' onclick='modUser(this);'>編輯</button>\n" +
					"        &nbsp;&nbsp;<button type='button' style='width: 70px;" + queryHide + "' onclick='delUser(this);'>刪除</button>\n" +
					"    </span>\n" +
					"</td>\n" +
					"</tr>\n";
				juserList.append(append);
				var jrow = juserList.find("tr:last");
			}
		}
	}, "json");
}

function progListActFmt(value, row, index) {
	return "<button type='button' onclick='progItemAdd(this);'>增加</button>\n" +
		"<input type='hidden' value='" + row.progId + "' />\n";
}

function progItemAdd(src) {
	var progId = $(src).next().val();
	var rowList = $("#progList").datagrid("getData").rows;
	var index;
	for (index = rowList.length - 1; index >= 0; index--)
		if (rowList[index].progId == progId)
			break;
	if (index >= 0) {
		var row = rowList[index];
		var rowList = $("#privList").datagrid("getData").rows;
		for (var i = 0; i < rowList.length; i++)
			if (rowList[i].progId == progId) {
				parent.showStatus({ statusTime: '', status: '程式代碼　<' + progId + '> 已存在，不可重複' });
				return;
			}
		var nrow = { sysDesc: row.sysDesc, progId: row.progId, progDesc: row.progDesc, privBase: row.defPrivBase,
			privAux: row.defPrivAux, allPrivAux: row.allPrivAux
		};
		$("#privList").datagrid("appendRow", nrow);
	}
}

function progListSel(index, row) {
	if (row.privAuxDesc) {
		$("#privAuxDescPane").css("visibility", "visible");
		var desc = row.privAuxDesc.replace("<br>", "\n");
		$("#privAuxDesc").textbox("setValue", desc);
	}
	else
		$("#privAuxDescPane").css("visibility", "hidden");
}

function privListBaseFmt(value, row, index) {
	return "<select style='color: inherit; width: 50px;' onchange='privItemBaseChg(this);'>\n" +
		"<option value='M'" + (value == "M" ? " selected" : "") + ">維護</option>\n" +
		"<option value='Q'" + (value == "Q" ? " selected" : "") + ">查詢</option>\n" +
		"<option value='P'" + (value == "P" ? " selected" : "") + ">禁止</option>\n" +
		"</select>\n" +
		"<input type='hidden' value='" + row.progId + "'/>\n";
}

function privItemBaseChg(src) {
	var progId = $(src).next().val();
	var rowList = $("#privList").datagrid("getData").rows;
	var index;
	for (index = rowList.length - 1; index >= 0; index--)
		if (rowList[index].progId == progId)
			break;
	if (index >= 0) {
		var row = rowList[index];
		row.privBase = $(src).val();
		$("#privList").datagrid("updateRow", { index: index, row: row });
	}
}

function privListAuxFmt(value, row, index) {
	return "<input type='text' style='width: 30px; border: 0 none white; color: inherit; background-color: inherit; text-align: center;'\n" +
		" onchange='privItemAuxChg(this);' value='" + value + "'>\n" +
		"<input type='hidden' value='" + row.progId + "'/>\n";
}

function privItemAuxChg(src) {
	var progId = $(src).next().val();
	var rowList = $("#privList").datagrid("getData").rows;
	var index;
	for (index = rowList.length - 1; index >= 0; index--)
		if (rowList[index].progId == progId)
			break;
	if (index >= 0) {
		var row = rowList[index];
		var aux = $(src).val();
		for (var i = aux.length - 1; i >= 0; i--)
			if (row.allPrivAux.indexOf(aux.substring(i, i + 1)) < 0)
				aux = aux.substring(0, i) + aux.substring(i + 1);
		row.privAux = aux;
		$("#privList").datagrid("updateRow", { index: index, row: row });
		$(src).val(aux);
	}
}

function privListActFmt(value, row, index) {
	return "<button type='button' onclick='privItemDel(this);'>刪除</button>\n" +
		"<input type='hidden' value='" + row.progId + "'/>\n";
}

function privItemDel(src) {
	var progId = $(src).next().val();
	var rowList = $("#privList").datagrid("getData").rows;
	var index;
	for (index = rowList.length - 1; index >= 0; index--)
		if (rowList[index].progId == progId)
			break;
	if (index >= 0)
		$("#privList").datagrid("deleteRow", index);
}

function addUser() {
	var req = {};
	$.post("MntUser_qryUser", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#userEdit").dialog({ closed: false });
			$("#userEditMode").val("A");
			$("#userEdit .modPane").css("visibility", "hidden");
			$("#editMenuId").combobox("loadData", res.menuList);
			$("#roleList").datagrid("loadData", res.roleList);
			$("#progList").datagrid("loadData", res.progList);
			$("#editUserId").textbox("setValue", "");
			$("#editUserName").textbox("setValue", "");
			$("#editMenuId").combobox("setValue", "");
			$("#editInitPass").textbox("setValue", "");
			$("#editBeginDate").datebox("setValue", "");
			$("#editEndDate").datebox("setValue", "");
			$("#editRemark").textbox("setValue", "");
			$("#privList").datagrid("loadData", []);
			$("#roleList").datagrid("uncheckAll");
		}
	}, "json");
}

function modUser(src) {
	var jrow = $(src).parent().parent().parent();
	var req = {};
	req.userId = jrow.find("[name='userId']").val();
	$.post("MntUser_qryUser", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			jrow.addClass("select");
			$("#userEdit").dialog({ closed: false });
			$("#userEditMode").val("M");
			$("#userEdit .modPane").css("visibility", "visible");
			$("#editMenuId").combobox("loadData", res.menuList);
			$("#roleList").datagrid("loadData", res.roleList);
			$("#progList").datagrid("loadData", res.progList);
			$("#editUserIdOrg").textbox("setValue", req.userId);
			$("#editUserId").textbox("setValue", req.userId);
			$("#editUserName").textbox("setValue", res.userName);
			$("#editMenuId").combobox("setValue", res.menuId);
			$("#editInitPass").textbox("setValue", res.initPass);
			$("#editPassStat").textbox("setValue", res.passStat);
			$("#editPassReset").switchbutton({ disabled: !res.hasPass });
			$("#editLockStat").textbox("setValue", res.lockStat);
			$("#editLockReset").switchbutton({ disabled: !res.lock });
			$("#editBeginDate").datebox("setValue", res.beginDate);
			$("#editEndDate").datebox("setValue", res.endDate);
			$("#editRemark").textbox("setValue", res.remark);
			var jroleList = $("#roleList");
			jroleList.datagrid("uncheckAll");
			for (var i = 0; i < res.userRoleList.length; i++)
				jroleList.datagrid("checkRow", jroleList.datagrid("getRowIndex", res.userRoleList[i]));
			$("#privList").datagrid("loadData", res.privList);
		}
	}, "json");
}

function userEditClose() {
	$("#userList tr.select").removeClass("select");
	$("#privAuxDescPane").css("visibility", "hidden");
}

function editUserDone() {
	var req = {};
	req.userId = $("#editUserId").textbox("getValue");
	req.userName = $("#editUserName").textbox("getValue");
	req.menuId = $("#editMenuId").textbox("getValue");
	req.initPass = $("#editInitPass").textbox("getValue");
	req.beginDate = $("#editBeginDate").datebox("getValue");
	req.endDate = $("#editEndDate").datebox("getValue");
	req.remark = $("#editRemark").textbox("getValue");
	var roleList = $("#roleList").datagrid("getChecked");
	for (var i = 0; i < roleList.length; i++)
		req["roleId[" + i + "]"] = roleList[i].roleId;
	var privList = $("#privList").datagrid("getData").rows;
	for (var i = 0; i < privList.length; i++) {
		req["progId[" + i + "]"] = privList[i].progId;
		req["privBase[" + i + "]"] = privList[i].privBase;
		req["privAux[" + i + "]"] = privList[i].privAux;
	}
	if ($("#userEditMode").val() == "A") {
		$.post("MntUser_addUser", req, function (res) {
			parent.showStatus(res);
			if (res.success) {
				$("#userEdit").dialog({ closed: true });
				qryUserList("T");
			}
		}, "json");
	}
	else if ($("#userEditMode").val() == "M") {
		req.userIdOrg = $("#editUserIdOrg").textbox("getValue");
		req.passReset = $("#editPassReset").switchbutton("options").checked ? "Y" : "N";
		req.lockReset = $("#editLockReset").switchbutton("options").checked ? "Y" : "N";
		$.post("MntUser_modUser", req, function (res) {
			parent.showStatus(res);
			if (res.success) {
				$("#userEdit").dialog({ closed: true });
				qryUserList("S");
			}
		}, "json");
	}
}

function delUser(src) {
	var jrow = $(src).parent().parent().parent();
	$("#userList tr.select").removeClass("select");
	jrow.addClass("select");
	var req = {};
	req.userId = jrow.find("[name='userId']").val();
	$.messager.confirm({
		title: '確認',
		msg: '是否要刪除此人員登入帳號？',
		ok: '刪除',
		cancel: '取消',
		fn: function (ok) {
			$("#userList tr.select").removeClass("select");
			if (ok) {
				$.post("MntUser_delUser", req, function (res) {
					parent.showStatus(res);
					if (res.success) {
						qryUserList("T");
					}
				}, "json");
			}
		}
	});
}
</script>
</head>
<body>
<div style="margin: 10px 0 0 10px;">
	<table class="cond">
		<tr>
			<td>帳號</td>
			<td>姓名</td>
			<td>生效日期</td>
			<td>失效日期</td>
			<td>使用選單</td>
			<td>包含角色</td>
			<td rowspan="2">
				<button type="button" style="width: 100px;" onclick="qryUserList('N');">查詢帳號</button>
			</td>
			<td rowspan="2">
				<button type="button" style="width: 100px; ${queryHide}" onclick="addUser();">新增帳號</button>
			</td>
		</tr>
		<tr>
			<td>
				<input id="qryUserId" class="easyui-textbox" style="width: 100px;" />
			</td>
			<td>
				<input id="qryUserName" class="easyui-textbox" style="width: 100px;" />
			</td>
			<td>
				<select id="qryBeginMode" class="easyui-combobox" style="width: 80px;" data-options="editable: false, panelHeight: 'auto'">
					<option value="">不限制</option>
					<option value="N">未指定</option>
					<option value="V">有指定</option>
					<option value="B">未生效</option>
					<option value="A">已生效</option>
				</select>
			</td>
			<td>
				<select id="qryEndMode" class="easyui-combobox" style="width: 80px;" data-options="editable: false, panelHeight: 'auto'">
					<option value="">不限制</option>
					<option value="N">未指定</option>
					<option value="V">有指定</option>
					<option value="B">未失效</option>
					<option value="A">已失效</option>
				</select>
			</td>
			<td>
				<select id="qryMenuId" class="easyui-combobox" data-options="editable: false, panelHeight: 'auto'">
					${menuList}
				</select>
			</td>
			<td>
				<select id="qryRoleId" class="easyui-combobox" data-options="editable: false, panelHeight: 'auto', multiple: true, value: ''">
					${roleList}
				</select>
			</td>
		</tr>
	</table>
</div>
<div style="margin: 10px 0 0 10px; height: 30px; ">
	<div style="float: left;">
		<div id="pg" class="easyui-pagination" style="width: 400px;" data-options="onSelectPage: qryUserList, showRefresh: false, total: 0" ></div>
	</div>
	<div style="float: left; position: relative; top: 2px; margin-left: 20px;">
		排序：
		<select id="order" class="easyui-combobox" data-options="multiple: true, panelHeight: 'auto', onHidePanel: qryUserList, onChange: chgOrder" style="width: 250px;">
			<option value="userId:A">帳號&#x25B2;</option>
			<option value="userId:D">帳號&#x25BC;</option>
			<option value="userName:A">姓名&#x25B2;</option>
			<option value="userName:D">姓名&#x25BC;</option>
			<option value="menuId:A">選單&#x25B2;</option>
			<option value="menuId:D">選單&#x25BC;</option>
			<option value="beginDate:A">生效&#x25B2;</option>
			<option value="beginDate:D">生效&#x25BC;</option>
			<option value="endDate:A">失效&#x25B2;</option>
			<option value="endDate:D">失效&#x25BC;</option>
			${privOrder}
		</select>
	</div>
	<div style="float: left; position: relative; top: 5px; margin-left: 20px;">
		<span style="font-size: 30px; cursor: pointer;" onclick="clearAll();">&#x239A</span>
	</div>
</div>
<div style="margin: 10px 0 0 20px;">
	<div style="clear: both;">
		<table class="listHead">
			<tr>
				<td style="width: 100px;" class="headCol1">
					<span class="orderField" onclick="chgOrder(this);">帳號</span>
					<span class="orderMark" order="userId">&#x25B2;</span>
				</td>
				<td style="width: 100px;" class="headCol2">
					<span class="orderField" onclick="chgOrder(this);">姓名</span>
					<span class="orderMark" order="userName">&nbsp;</span>
				</td>
				<td style="width: 150px;" class="headCol3">
					<span class="orderField" onclick="chgOrder(this);">選單</span>
					<span class="orderMark" order="menuId">&nbsp;</span>
				</td>
				<td style="width: 90px;" class="headCol4">
					<span class="orderField" onclick="chgOrder(this);">生效日期</span>
					<span class="orderMark" order="beginDate">&nbsp;</span>
				</td>
				<td style="width: 90px;" class="headCol5">
					<span class="orderField" onclick="chgOrder(this);">失效日期</span>
					<span class="orderMark" order="endDate">&nbsp;</span>
				</td>
				<td style="width: 200px;" class="headCol6">選用角色</td>
				<td style="width: 100px;" class="headCol7">狀態</td>
				<td style="width: 160px;" class="headCol6">處理動作</td>
			</tr>
		</table>
	</div>
	<div style="clear: both; width: 1020px; height: 380px; overflow: auto;">
		<table id="userList" class="listData">
		</table>
	</div>
</div>
<div id="userEdit" class="easyui-dialog" style="width: 1100px; height: 500px; display: none;"
	data-options="modal: true, title: '編輯人員資料', closed: true, onClose: userEditClose">
	<input id="userEditMode" type="hidden" />
	<div style="float: left;">
		<div style="clear: both;">
			<table style="margin: 20px 0 0 20px;">
				<tr class="modPane">
					<td>原帳號</td>
					<td>
						<input id="editUserIdOrg" class="easyui-textbox" style="width: 150px;" data-options="readonly: true" />
					</td>
				</tr>
				<tr>
					<td>帳號</td>
					<td>
						<input id="editUserId" class="easyui-textbox" style="width: 150px;" />
					</td>
				</tr>
				<tr>
					<td>姓名</td>
					<td>
						<input id="editUserName" class="easyui-textbox" style="width: 150px;" />
					</td>
				</tr>
				<tr>
					<td>選單</td>
					<td>
						<input id="editMenuId" class="easyui-combobox" style="width: 150px;" data-options="editable: false, panelHeight: 'auto'"/>
					</td>
				</tr>
				<tr>
					<td>初始密碼</td>
					<td>
						<input id="editInitPass" class="easyui-textbox" style="width: 150px;" />
					</td>
				</tr>
				<tr class="modPane">
					<td>密碼狀態</td>
					<td>
						<input id="editPassStat" class="easyui-textbox" style="width: 60px;" data-options="readonly: true" />
						<input id="editPassReset" class="easyui-switchbutton" style="width: 80px;" data-options="onText: '初始密碼', offText: '自定密碼'" />
					</td>
				</tr>
				<tr class="modPane">
					<td>鎖定狀態</td>
					<td>
						<input id="editLockStat" class="easyui-textbox" style="width: 60px;" data-options="readonly: true" />
						<input id="editLockReset" class="easyui-switchbutton" style="width: 80px;" data-options="onText: '解除鎖定', offText: '維持狀態'" />
					</td>
				</tr>
				<tr>
					<td>生效日期</td>
					<td>
						<input id="editBeginDate" class="easyui-datebox" style="width: 150px;" />
					</td>
				</tr>
				<tr>
					<td>失效日期</td>
					<td>
						<input id="editEndDate" class="easyui-datebox" style="width: 150px;" />
					</td>
				</tr>
				<tr>
					<td>備註</td>
					<td>
						<input id="editRemark" class="easyui-textbox" style="width: 150px; height: 100px;" data-options="multiline: true"/>
					</td>
				</tr>
				<tr>
					<td colspan="2" style="text-align: center;">
						<button type="button" style="width: 100px;" onclick="editUserDone();">儲存人員</button>
					</td>
			</table>
		</div>
	</div>
	<div style="float: left; margin: 0 0 0 10px;">
		<div style="clear: both; margin: 10px 0 0 0;">
			<div id="roleList" class="easyui-datagrid" style="width: 300px; height: 200px;" data-options="
				title: '選用角色列表', singleSelect: false, idField: 'roleId', columns: [[
					{ field: 'sel', title: '', checkbox: true },
					{ field: 'roleId', title: '角色代碼', halign: 'center', width: 100 },
					{ field: 'roleName', title: '角色名稱', halign: 'center', width: 150 },
				]]
			">
			</div>
		</div>
		<div id="privAuxDescPane" style="margin: 30px 0 0 0; visibility: hidden;">
			<div>附加權限說明</div>
			<div style="margin: 10px 0 0 0;">
				<input id="privAuxDesc" class="easyui-textbox" style="width: 300px; height: 150px;" data-options="multiline: true, readonly: true" />
			</div>
		</div>
	</div>
	<div style="float: left; margin: 0 0 0 10px;">
		<div style="clear: both; margin: 10px 0 0 0;">
			<div id="privList" class="easyui-datagrid" style="width: 500px; height: 200px;" data-options="
				title: '人員權限清單', singleSelect: true, columns: [[
					{ field: 'sysDesc', title: '系統別', align: 'center', width: 50 },
					{ field: 'progId', title: '作業代碼', halign: 'center', width: 100 },
					{ field: 'progDesc', title: '作業說明', halign: 'center', width: 170 },
					{ field: 'privBase', title: '基本', align: 'center', width: 60, formatter: privListBaseFmt },
					{ field: 'privAux', title: '附加', align: 'center', width: 40, formatter: privListAuxFmt },
					{ field: 'action', title: '動作', align: 'center', width: 70, formatter: privListActFmt },
					{ field: 'allPrivAux', hidden: true },
				]]
			">
			</div>
		</div>
		<div style="clear: both; margin: 10px 0 0 0;">
			<div id="progList" class="easyui-datagrid" style="width: 500px; height: 200px;" data-options="
				title: '作業列表', singleSelect: true, columns: [[
					{ field: 'sysDesc', title: '系統別', align: 'center', width: 50 },
					{ field: 'progId', title: '作業代碼', halign: 'center', width: 100 },
					{ field: 'progDesc', title: '作業說明', halign: 'center', width: 180 },
					{ field: 'privDesc', title: '權限設定', halign: 'center', width: 80 },
					{ field: 'action', title: '動作', align: 'center', width: 70, formatter: progListActFmt },
					{ field: 'defPrivBase', hidden: true },
					{ field: 'defPrivAux', hidden: true },
					{ field: 'allPrivAux', hidden: true },
					{ field: 'privAuxDesc', hidden: true },
				]], onSelect: progListSel
			">
			</div>
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