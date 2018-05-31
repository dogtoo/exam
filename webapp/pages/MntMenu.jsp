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
#addPanel {
	margin: 0 auto;
}
#addPanel td {
	padding: 2px 5px 2px 5px;
}
#modPanel td {
	padding: 2px 5px 2px 5px;
}
</style>
<script type="text/javascript">
function qryMenuList(mode) {
	function travelMenu(list, menuState, mode) {
		for (var i = 0; i < list.length; i++) {
			if (mode == 'S')
				menuState[list[i].menuId] = list[i].state;
			else if (mode == 'G')
				list[i].state = menuState[list[i].menuId];
			if (list[i].children)
				travelMenu(list[i].children, menuState);
		}
	}
	
	var menuState = {};
	travelMenu($("#menuList").treegrid("getRoots"), menuState, "S");
	var req = {};
	$.post("MntMenu_qryMenuList", req, function (res) {
		if (mode != "S")
			parent.showStatus(res);
		if ('menuList' in res) {
			travelMenu(res.menuList, menuState, "G");
			$("#menuList").treegrid("loadData", res.menuList);
			if (mode == 'C')
				$("#menuList").treegrid("collapseAll");
		}
	}, "json");
}

function menuItemActFmt(value, row, index) {
	if ($("#queryHide").val() != "")
		return "";
	return "<button type='button' style='width: 70px;' onclick='modMenu(this);'>編輯</button>\n" +
		"&nbsp;&nbsp;<button type='button' style='width: 70px;' onclick='delMenu(this);'>刪除</button>\n" +
		"<input type='hidden' value='" + row.menuId + "' />\n";
}

function qryProgList() {
	var req = {};
	$.post("MntMenu_qryProgList", req, function (res) {
		parent.showStatus(res);
		if ('progList' in res) {
			$("#addProgList").treegrid("loadData", res.progList);
			$("#addProgList").treegrid("collapseAll");
		}
	}, "json");
}

function addProgItemSel(row) {
	if (row.progId.substr(0, 1) != '*') {
		$("#addProgId").textbox("setValue", row.progId);
		$("#addMenuDesc").textbox("setValue", row.progMenu);
		$("#addProgUse").prop("checked", true);
	}
}

function modProgItemSel(row) {
	if (row.progId.substr(0, 1) != '*') {
		$("#modProgId").textbox("setValue", row.progId);
		$("#modMenuDesc").textbox("setValue", row.progMenu);
		$("#modProgUse").prop("checked", true);
	}
}

function addMenu() {
	var upperId = "-";
	if (!$("#addMenuType").switchbutton("options").checked) {
		var row = $("#menuList").treegrid("getSelected");
		if (!row) {
			parent.showStatus({ statusTime: '', status: '請先選擇一個上層選單' });
			return;
		}
		upperId = row.menuId;
	}
	var req = {};
	req.upperId = upperId;
	req.menuId = $("#addMenuId").textbox("getValue");
	req.menuDesc = $("#addMenuDesc").textbox("getValue");
	req.progId = $("#addProgUse").prop("checked") ? $("#addProgId").textbox("getValue") : "-";
	req.showOrder = $("#addShowOrder").textbox("getValue");
	req.remark = $("#addRemark").textbox("getValue");
	$.post("MntMenu_addMenu", req, function (res) {
		parent.showStatus(res);
		if (res.success)
			qryMenuList("S");
	}, "json");
}

function modMenu(src) {
	function findRow(list, menuId) {
		var row;
		for (var i = 0; i < list.length && !row; i++) {
			if (list[i].menuId == menuId)
				row = list[i];
			else if (list[i].children)
				row = findRow(list[i].children, menuId);
		}
		return row;
	}
	var menuId = $(src).next().next().val();
	var row = findRow($("#menuList").treegrid("getData"), menuId);
	if (!row)
		return;
	$("#menuEdit").dialog({ closed: false });
	$("#modMenuIdOrg").textbox("setValue", row.menuId);
	$("#modMenuId").textbox("setValue", row.menuId);
	$("#modMenuDesc").textbox("setValue", row.menuDesc);
	$("#modProgUse").prop("checked", row.progId != "");
	$("#modProgId").textbox("setValue", row.progId);
	$("#modShowOrder").textbox("setValue", row.showOrder);
	$("#modRemark").textbox("setValue", row.remark);
	$("#modProgList").treegrid("loadData", $("#addProgList").treegrid("getData"));
	$("#modProgList").treegrid("collapseAll");
}

function modMenuDone() {
	var req = {};
	req.menuIdOrg = $("#modMenuIdOrg").textbox("getValue");
	req.menuId = $("#modMenuId").textbox("getValue");
	req.menuDesc = $("#modMenuDesc").textbox("getValue");
	req.progId = $("#modProgUse").prop("checked") ? $("#modProgId").textbox("getValue") : "-";
	req.showOrder = $("#modShowOrder").textbox("getValue");
	req.remark = $("#modRemark").textbox("getValue");
	$.post("MntMenu_modMenu", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#menuEdit").dialog({ closed: true });
			qryMenuList("S");
		}
	}, "json");
}

function delMenu(src) {
	var menuId = $(src).next().val();
	var req = {};
	req.menuId = menuId;
	$.messager.confirm({
		title: '確認',
		msg: '是否要刪除此筆選單？',
		ok: '刪除',
		cancel: '取消',
		fn: function (ok) {
			if (ok) {
				$.post("MntMenu_delMenu", req, function (res) {
					parent.showStatus(res);
					if (res.success)
						qryMenuList("S");
				}, "json");
			}
		}
	});
}
</script>
</head>
<body>
<div style="float: left; margin: 10px 0 0 10px;">
	<div id="menuList" class="easyui-treegrid" style="width: 880px; height: 500px;"	data-options="
		title: '系統選單列表', columns: [[
			{ field: 'menuDesc', title: '選單說明', halign: 'center', width: 250 }, 
			{ field: 'menuId', title: '選單代碼', halign: 'center', width: 100 }, 
			{ field: 'progId', title: '作業代碼', halign: 'center', width: 100 }, 
			{ field: 'progDesc', title: '作業說明', halign: 'center', width: 200 }, 
			{ field: 'showOrder', title: '順序', halign: 'center', align: 'center', width: 50 }, 
			{ field: 'remark', hidden: true }, 
			{ field: 'action', title: '處理動作', halign: 'center', width: 160, formatter: menuItemActFmt }, 
		]],
		idField: 'menuId', treeField: 'menuDesc'
	">
		
	</div>
</div>
<div style="float: left; margin: 10px 0 0 30px;">
	<div style="clear: both;">
		<div id="addProgList" class="easyui-treegrid" style="width: 380px; height: 300px;" data-options="
			title: '作業清單', columns: [[
				{ field: 'sysDesc', title: '系統/作業代碼', halign: 'center', width: 150 },
				{ field: 'progDesc', title: '作業說明', halign: 'center', width: 200 },
				{ field: 'progId', hidden: true },
				{ field: 'progMenu', hidden: true },
			]],
			idField: 'progId', treeField: 'sysDesc', onSelect: addProgItemSel
		"></div>
	</div>
	<div style="margin-top: 20px; ${queryHide}">
		<div style="float: left;">
			<table id="addPanel">
				<tr>
					<td>選單代碼</td>
					<td>
						<input id="addMenuId" class="easyui-textbox" style="width: 150px;" />
					</td>
				</tr>
				<tr>
					<td>選單說明</td>
					<td>
						<input id="addMenuDesc" class="easyui-textbox" style="width: 150px;" />
					</td>
				</tr>
				<tr>
					<td>
						<input id="addProgUse" type="checkbox" />
						作業
					</td>
					<td>
						<input id="addProgId" class="easyui-textbox" style="width: 150px;" data-options="readonly: true" />
					</td>
				</tr>
				<tr>
					<td>
						選單順序
					</td>
					<td>
						<input id="addShowOrder" class="easyui-textbox" style="width: 150px;" data-options="" />
					</td>
				</tr>
				<tr>
					<td>
						選單類別
					</td>
					<td>
						<input id="addMenuType" class="easyui-switchbutton" style="width: 150px;" data-options="onText: '最上層選單', offText: '選定項目的子選單'" />
					</td>
				</tr>
				<tr>
					<td colspan="2" style="text-align: center;">
						<button type="button" style=" width: 150px;" onclick="addMenu();">新增選單</button>
					</td>
				</tr>
			</table>
		</div>
		<div style="float: left; margin-left: 5px; ${queryHide}">
			<div style="padding: 8px 0 8px 0;">選單備註</div>
			<input id="addRemark" class="easyui-textbox" style="width: 140px; height: 140px;" data-options="multiline: true" />
		</div>
	</div>
</div>
<div id="menuEdit" class="easyui-dialog" style="width: 700px; height: 400px; display: none;" data-options="title: '編輯選單', modal: true, closed: true" >
	<div style="float: left; margin: 40px 0 0 10px;">
		<table id="modPanel">
			<tr>
				<td>原選單代碼</td>
				<td>
					<input id="modMenuIdOrg" class="easyui-textbox" style="width: 150px;" data-options="readonly: true"/>
				</td>
			</tr>
			<tr>
				<td>選單代碼</td>
				<td>
					<input id="modMenuId" class="easyui-textbox" style="width: 150px;" />
				</td>
			</tr>
			<tr>
				<td>選單說明</td>
				<td>
					<input id="modMenuDesc" class="easyui-textbox" style="width: 150px;" />
				</td>
			</tr>
			<tr>
				<td>
					<input id="modProgUse" type="checkbox" />
					作業
				</td>
				<td>
					<input id="modProgId" class="easyui-textbox" style="width: 150px;" data-options="readonly: true" />
				</td>
			</tr>
			<tr>
				<td>
					選單順序
				</td>
				<td>
					<input id="modShowOrder" class="easyui-textbox" style="width: 150px;" data-options="" />
				</td>
			</tr>
			<tr>
				<td>
					選單備註
				</td>
				<td>
					<input id="modRemark" class="easyui-textbox" style="width: 150px; height: 100px;" data-options="multiline: true" />
				</td>
			</tr>
			<tr>
				<td colspan="2" style="text-align: center;">
					<button type="button" style=" width: 150px;" onclick="modMenuDone();">修改選單</button>
				</td>
			</tr>
		</table>
	</div>
	<div style="float: left; margin: 10px 0 0 30px;">
		<div id="modProgList" class="easyui-treegrid" style="width: 380px; height: 300px;" data-options="
			title: '作業清單', columns: [[
				{ field: 'sysDesc', title: '系統/作業代碼', halign: 'center', width: 150 },
				{ field: 'progDesc', title: '作業說明', halign: 'center', width: 200 },
				{ field: 'progId', hidden: true },
				{ field: 'progMenu', hidden: true },
			]],
			idField: 'progId', treeField: 'sysDesc', onSelect: modProgItemSel
		"></div>
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
setTimeout(function () { qryMenuList('C'); qryProgList(); }, 500);
</script>
</html>