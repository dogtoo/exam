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
function qryRoleList(mode) {
	var req = {};
	req.mode = mode;
	req.pageRow = $("#pg").pagination("options").pageSize;
	req.pageAt = $("#pg").pagination("options").pageNumber;
	$.post("MntRole_qryRoleList", req, function (res) {
		if (mode != "S")
			parent.showStatus(res);
		if (res.success) {
			if ('total' in res)
				$("#pg").pagination({ total: res.total });
			var queryHide = $("#queryHide").val();
			var jroleList = $("#roleList");
			jroleList.find("tr").remove();
			for (var i = 0; i < res.roleList.length; i++) {
				var role = res.roleList[i];
				var append = "<tr class='" + (i & 1 ? "odd" : "even") + "'>\n" +
					"<td>\n" +
					"    <input name='roleId' type='text' style='width: 100px;' readonly value='" + role.roleId + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='roleName' type='text' style='width: 150px;' readonly value='" + role.roleName + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <span style='width: 80px; display: inline-block; text-align: center;'><input name='suspend'/></span>\n" +
					"</td>\n" +
					"<td>\n" +
					"    <span style='width: 160px; display: inline-block; text-align: center;'>\n" +
					"        <button type='button' style='width: 70px;' onclick='modRole(this);'>編輯</button>\n" +
					"        &nbsp;&nbsp;<button type='button' style='width: 70px;" + queryHide + "' onclick='delRole(this);'>刪除</button>\n" +
					"    </span>\n" +
					"</td>\n" +
					"</tr>\n";
				jroleList.append(append);
				var jrow = jroleList.find("tr:last");
				jrow.find("[name='suspend']").switchbutton({ onText: '停用', offText: '使用', checked: (role.suspend == 'Y' ),
					onChange: function(chk) { chgSuspend(this, chk); }, disabled: (queryHide != "") });
			}
		}
	}, "json");
}

function qryProgList() {
	var req = {};
	$.post("MntRole_qryProgList", req, function (res) {
		parent.showStatus(res);
		if ('progList' in res) {
			$("#progList").datagrid("loadData", res.progList);
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

function chgSuspend(src, chk) {
	if ($(src).hasClass("inProg"))
		return;
	var jrow = $(src);
	while (jrow.length > 0 && jrow.prop("tagName") != "TR")
		jrow = jrow.parent();
	if (jrow.length == 0)
		return;
	$("#roleList tr.select").removeClass("select");
	jrow.addClass("select");
	var roleId = jrow.find("[name='roleId']").val();
	$.messager.confirm({
		title: '確認',
		msg: '是否要設定此角色為' + (chk ? '暫停' : '使用') + '？',
		ok: '確認',
		cancel: '取消',
		fn: function (ok) {
			$("#roleList tr.select").removeClass("select");
			if (ok) {
				var req = {};
				req.roleId = roleId;
				req.suspend = chk ? "Y" : "N";
				$.post("MntRole_setSuspend", req, function (res) {
					parent.showStatus(res);
					if (res.success) {
						qryRoleList("S");
					}
				}, "json");
			}
			else {
				$(src).addClass("inProg");
				jrow.find("[switchbuttonname='suspend']").switchbutton({ checked: !chk });
				$(src).removeClass("inProg");
			}
		}
	});
}

function addRole() {
	$("#roleEdit").css("visibility", "visible");
	$("#roleEditMode").val("A");
	$("#editRoleIdOrgPane").css("visibility", "hidden");
	$("#editRoleId").textbox("setValue", "");
	$("#editRoleName").textbox("setValue", "");
	$("#editSuspend").switchbutton({ checked: false });
	$("#editRemark").textbox("setValue", "");
	$("#privList").datagrid("loadData", []);
	$("#roleList tr.select").removeClass("select");
}

function modRole(src) {
	var jrow = $(src).parent().parent().parent();
	var roleId = jrow.find("[name='roleId']").val();
	var req = {};
	req.roleId = roleId;
	$.post("MntRole_qryRole", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#roleList tr.select").removeClass("select");
			jrow.addClass("select");
			$("#roleEdit").css("visibility", "visible");
			$("#roleEditMode").val("M");
			$("#editRoleIdOrgPane").css("visibility", "visible");
			$("#editRoleIdOrg").textbox("setValue", res.roleId);
			$("#editRoleId").textbox("setValue", res.roleId);
			$("#editRoleName").textbox("setValue", res.roleName);
			$("#editSuspend").switchbutton({ checked: res.suspend });
			$("#editRemark").textbox("setValue", res.remark);
			$("#privList").datagrid("loadData", res.privList);
		}
	}, "json");
}

function editRoleDone() {
	var req = {};
	req.roleId = $("#editRoleId").textbox("getValue");
	req.roleName = $("#editRoleName").textbox("getValue");
	req.suspend = $("#editSuspend").switchbutton("options").checked ? "Y" : "N";
	req.remark = $("#editRemark").textbox("getValue");
	var privList = $("#privList").datagrid("getData").rows;
	for (var i = 0; i < privList.length; i++) {
		req["progId[" + i + "]"] = privList[i].progId;
		req["privBase[" + i + "]"] = privList[i].privBase;
		req["privAux[" + i + "]"] = privList[i].privAux;
	}
	if ($("#roleEditMode").val() == "A") {
		$.post("MntRole_addRole", req, function (res) {
			parent.showStatus(res);
			if (res.success) {
				$("#roleEdit").css("visibility", "hidden");
				$("#privAuxDescPane").css("visibility", "hidden");
				qryRoleList("S");
			}
		}, "json");
	}
	else if ($("#roleEditMode").val() == "M") {
		req.roleIdOrg = $("#editRoleIdOrg").textbox("getValue");
		$.post("MntRole_modRole", req, function (res) {
			parent.showStatus(res);
			if (res.success) {
				$("#roleEdit").css("visibility", "hidden");
				$("#editRoleIdOrgPane").css("visibility", "hidden");
				$("#privAuxDescPane").css("visibility", "hidden");
				$("#roleList tr.select").removeClass("select");
				qryRoleList("S");
			}
		}, "json");
	}
}

function delRole(src) {
	var jrow = $(src).parent().parent().parent();
	var roleId = jrow.find("[name='roleId']").val();
	$("#roleList tr.select").removeClass("select");
	jrow.addClass("select");
	$("#roleEdit").css("visibility", "hidden");
	$("#editRoleIdOrgPane").css("visibility", "hidden");
	$("#privAuxDescPane").css("visibility", "hidden");
	var req = {};
	req.roleId = roleId;
	$.messager.confirm({
		title: '確認',
		msg: '是否要刪除此筆角色？',
		ok: '刪除',
		cancel: '取消',
		fn: function (ok) {
			$("#roleList tr.select").removeClass("select");
			if (ok) {
				$.post("MntRole_delRole", req, function (res) {
					parent.showStatus(res);
					if (res.success) {
						qryRoleList("S");
					}
				}, "json");
			}
		}
	});
}
</script>
</head>
<body>
<div style="float: left; margin: 10px 0 0 10px;">
	<div style="clear: both;">
		<div style="float: left;">
			<div id="pg" class="easyui-pagination" style="width: 380px;" data-options="onSelectPage: qryRoleList, showRefresh: false, total: 0" ></div>
		</div>
		<div style="float: left; margin-left: 20px;">
			<button type="button" style="${queryHide}" onclick="addRole();">新增角色</button>
		</div>
	</div>
	<div style="clear: both;">
		<table class="listHead">
			<tr>
				<td style="width: 100px;" class="headCol1">角色代碼</td>
				<td style="width: 150px;" class="headCol2">角色名稱</td>
				<td style="width: 80px;" class="headCol3">是否停用</td>
				<td style="width: 160px;" class="headCol4">處理動作</td>
			</tr>
		</table>
	</div>
	<div style="clear: both; width: 510px; height: 440px; overflow: auto;">
		<table id="roleList" class="listData">
		</table>
	</div>
</div>
<div id="roleEdit" style="float: left; margin: 10px 0 0 10px; visibility: hidden;">
	<input id="roleEditMode" type="hidden" />
	<div style="float: left;">
		<div style="clear: both; height: 260px;">
			<table>
				<tr id="editRoleIdOrgPane">
					<td>原角色代碼</td>
					<td>
						<input id="editRoleIdOrg" class="easyui-textbox" style="width: 150px;" data-options="readonly: true" />
					</td>
				</tr>
				<tr>
					<td>角色代碼</td>
					<td>
						<input id="editRoleId" class="easyui-textbox" style="width: 150px;" />
					</td>
				</tr>
				<tr>
					<td>角色名稱</td>
					<td>
						<input id="editRoleName" class="easyui-textbox" style="width: 150px;" />
					</td>
				</tr>
				<tr>
					<td>暫停使用</td>
					<td>
						<input id="editSuspend" class="easyui-switchbutton" style="width: 80px;" data-options="onText: '停用', offText: '使用'"/>
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
						<button type="button" style="width: 100px; ${queryHide}" onclick="editRoleDone();">儲存角色</button>
					</td>
			</table>
		</div>
		<div id="privAuxDescPane" style="margin: 30px 0 0 0; visibility: hidden;">
			<div>附加權限說明</div>
			<div style="margin: 10px 0 0 0;">
				<input id="privAuxDesc" class="easyui-textbox" style="width: 230px; height: 150px;" data-options="multiline: true, readonly: true" />
			</div>
		</div>
	</div>
	<div style="float: left; margin: 0 0 0 10px;">
		<div style="clear: both;">
			<div id="privList" class="easyui-datagrid" style="width: 500px; height: 250px;" data-options="
				title: '角色權限清單', singleSelect: true, columns: [[
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
			<div id="progList" class="easyui-datagrid" style="width: 500px; height: 250px;" data-options="
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
setTimeout(function () { qryRoleList('N'); qryProgList(); }, 500);
</script>
</html>