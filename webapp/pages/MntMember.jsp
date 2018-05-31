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
		qryMemberList();
}

function clearAll() {
	$("#qryUserId").textbox("setValue", "");
	$("#qryUserName").textbox("setValue", "");
	$("#qryDepartId").combobox("setValue", "");
	$("#qryUserNo").textbox("setValue", "");
	$("#qryMobileNo").textbox("setValue", "");
	$("#qryAddress").textbox("setValue", "");
	$("#qryEmail").textbox("setValue", "");
	$("#qryBeginMode").combobox("setValue", "");
	$("#qryEndMode").combobox("setValue", "");
	$("#order").combobox("setValues", [ "userId:A" ]);
	chgOrder();
	$("#pg").pagination({ total: 0 });
	$("#memberList tr").remove();
	delete window.queryCond1;
}

function qryMemberList(mode) {
	var req = {};
	req.mode = mode;
	if (mode == "N") {
		$("#pg").pagination({ total: 1, pageNumber: 1 });
		var queryCond1 = {};
		queryCond1._job = 'MntMember';
		queryCond1.userId = $("#qryUserId").textbox("getValue");
		queryCond1.userName = $("#qryUserName").textbox("getValue");
		queryCond1.departId = $("#qryDepartId").combobox("getValue");
		queryCond1.userNo = $("#qryUserNo").textbox("getValue");
		queryCond1.mobileNo = $("#qryMobileNo").textbox("getValue");
		queryCond1.address = $("#qryAddress").textbox("getValue");
		queryCond1.email = $("#qryEmail").textbox("getValue");
		queryCond1.beginMode = $("#qryBeginMode").combobox("getValue");
		queryCond1.endMode = $("#qryEndMode").combobox("getValue");
		window.queryCond1 = queryCond1;
	}
	else {
		if (!window.queryCond1 || window.queryCond1._job != 'MntMember')
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
	$.post("MntMember_qryMemberList", req, function (res) {
		if (mode != "S" && mode != "T")
			parent.showStatus(res);
		if (res.success) {
			if ('total' in res)
				$("#pg").pagination({ total: res.total });
			var queryHide = $("#queryHide").val();
			var jmemberList = $("#memberList");
			jmemberList.find("tr").remove();
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
					"    <input name='departName' type='text' style='width: 150px;' readonly value='" + user.departName + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='userNo' type='text' style='width: 100px;' readonly value='" + user.userNo + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='mobileNo' type='text' style='width: 100px;' readonly value='" + user.mobileNo + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='address' type='text' style='width: 150px;' readonly value='" + user.address + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='email' type='text' style='width: 200px;' readonly value='" + user.email + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='beginDate' type='text' style='width: 90px; text-align: center;' readonly value='" + user.beginDate + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='endDate' type='text' style='width: 90px; text-align: center;' readonly value='" + user.endDate + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <span style='width: 160px; display: inline-block; text-align: center;'>\n" +
					"        <button type='button' style='width: 70px;" + queryHide + "' onclick='modMember(this);'>編輯</button>\n" +
					"        &nbsp;&nbsp;<button type='button' style='width: 70px;" + queryHide + "' onclick='delMember(this);'>刪除</button>\n" +
					"    </span>\n" +
					"</td>\n" +
					"</tr>\n";
				jmemberList.append(append);
				var jrow = jmemberList.find("tr:last");
			}
		}
	}, "json");
}

function alterUserChg(chk) {
	$("div.alterUserPane").css("visibility", chk ? "visible" : "hidden");
}

function addMember() {
	var req = {};
	$.post("MntMember_qryMember", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#memberEdit").dialog({ closed: false });
			$("#memberEditMode").val("A");
			$("#memberEdit .modPane").css("visibility", "hidden");
			$("#editDepartId").combobox("loadData", res.departList);
			$("#editMenuId").combobox("loadData", res.menuList);
			$("#roleList").datagrid("loadData", res.roleList);
			$("#editUserId").textbox("setValue", "");
			$("#editUserName").textbox("setValue", "");
			$("#editDepartId").combobox("setValue", "");
			$("#editUserNo").textbox("setValue", "");
			$("#editMobileNo").textbox("setValue", "");
			$("#editAddress").textbox("setValue", "");
			$("#editEmail").textbox("setValue", "");
			$("#editBeginDate").datebox("setValue", "");
			$("#editEndDate").datebox("setValue", "");
			$("#alterUser").switchbutton({ checked: false });
			alterUserChg(false);
			$("#editInitPass").textbox("setValue", "");
			$("#editMenuId").combobox("setValue", "");
			$("#editRemark").textbox("setValue", "");
			$("#roleList").datagrid("uncheckAll");
		}
	}, "json");
}

function modMember(src) {
	var jrow = $(src).parent().parent().parent();
	var req = {};
	req.userId = jrow.find("[name='userId']").val();
	$.post("MntMember_qryMember", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			jrow.addClass("select");
			$("#memberEdit").dialog({ closed: false });
			$("#memberEditMode").val("M");
			$("#memberEdit .modPane").css("visibility", "visible");
			$("#editDepartId").combobox("loadData", res.departList);
			$("#editMenuId").combobox("loadData", res.menuList);
			$("#roleList").datagrid("loadData", res.roleList);
			$("#editUserIdOrg").textbox("setValue", req.userId);
			$("#editUserId").textbox("setValue", req.userId);
			$("#editUserName").textbox("setValue", res.userName);
			$("#editDepartId").combobox("setValue", res.departId);
			$("#editUserNo").textbox("setValue", res.userNo);
			$("#editMobileNo").textbox("setValue", res.mobileNo);
			$("#editAddress").textbox("setValue", res.address);
			$("#editEmail").textbox("setValue", res.email);
			$("#editBeginDate").datebox("setValue", res.beginDate);
			$("#editEndDate").datebox("setValue", res.endDate);
			$("#alterUser").switchbutton({ checked: false });
			alterUserChg(false);
			$("#editMenuId").combobox("setValue", res.menuId);
			$("#editInitPass").textbox("setValue", res.initPass);
			$("#editRemark").textbox("setValue", res.remark);
			var jroleList = $("#roleList");
			jroleList.datagrid("uncheckAll");
			for (var i = 0; i < res.userRoleList.length; i++)
				jroleList.datagrid("checkRow", jroleList.datagrid("getRowIndex", res.userRoleList[i]));
		}
	}, "json");
}

function memberEditClose() {
	$("#memberList tr.select").removeClass("select");
}

function memberEditDone() {
	var req = {};
	req.userId = $("#editUserId").textbox("getValue");
	req.userName = $("#editUserName").textbox("getValue");
	req.departId = $("#editDepartId").combobox("getValue");
	req.userNo = $("#editUserNo").textbox("getValue");
	req.mobileNo = $("#editMobileNo").textbox("getValue");
	req.address = $("#editAddress").textbox("getValue");
	req.email = $("#editEmail").textbox("getValue");
	req.beginDate = $("#editBeginDate").datebox("getValue");
	req.endDate = $("#editEndDate").datebox("getValue");
	req.remark = $("#editRemark").textbox("getValue");
	var roleList = $("#roleList").datagrid("getChecked");
	for (var i = 0; i < roleList.length; i++)
		req["roleId[" + i + "]"] = roleList[i].roleId;
	req.alterUser = $("#alterUser").switchbutton("options").checked ? "Y" : "N";
	req.menuId = $("#editMenuId").textbox("getValue");
	req.initPass = $("#editInitPass").textbox("getValue");
	if ($("#memberEditMode").val() == "A") {
		$.post("MntMember_addMember", req, function (res) {
			parent.showStatus(res);
			if (res.success) {
				$("#memberEdit").dialog({ closed: true });
				qryMemberList("T");
			}
		}, "json");
	}
	else if ($("#memberEditMode").val() == "M") {
		req.userIdOrg = $("#editUserIdOrg").textbox("getValue");
		$.post("MntMember_modMember", req, function (res) {
			parent.showStatus(res);
			if (res.success) {
				$("#memberEdit").dialog({ closed: true });
				qryMemberList("S");
			}
		}, "json");
	}
}

function delMember(src) {
	var jrow = $(src).parent().parent().parent();
	$("#memberList tr.select").removeClass("select");
	jrow.addClass("select");
	var req = {};
	req.userId = jrow.find("[name='userId']").val();
	setTimeout(function() {
		var jdiv = $("div.messager-body");
		jdiv.append("<span style='display: inline-block; width: 50px;'>&nbsp;</span><input id='alterUser1' style='width: 150px; position: relative; left: 50px;'/>");
		jdiv.find("#alterUser1").switchbutton({ onText: '一併刪除登入資料', offText: '不要變更登入資料',
			onChange: function (chk) { $("#alterUserDel").val(chk ? "Y" : "N"); }
		});
		$("#alterUserDel").val("N");
	}, 200);
	$.messager.confirm({
		title: '確認',
		msg: '是否要刪除此人員登入帳號？',
		ok: '刪除',
		cancel: '取消',
		fn: function (ok) {
			$("#memberList tr.select").removeClass("select");
			if (ok) {
				req.alterUser = $("#alterUserDel").val();
				$.post("MntMember_delMember", req, function (res) {
					parent.showStatus(res);
					if (res.success) {
						qryMemberList("T");
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
			<td>科系</td>
			<td>編號</td>
			<td>手機號碼</td>
			<td>通訊地址</td>
			<td>電子郵件</td>
			<td>生效日期</td>
			<td>失效日期</td>
			<td rowspan="2">
				<button type="button" style="width: 100px;" onclick="qryMemberList('N');">查詢帳號</button>
			</td>
			<td rowspan="2">
				<button type="button" style="width: 100px; ${queryHide}" onclick="addMember();">新增帳號</button>
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
				<select id="qryDepartId" class="easyui-combobox" data-options="editable: false, panelHeight: 'auto'" >
					${departList}
				</select>
			</td>
			<td>
				<input id="qryUserNo" class="easyui-textbox" style="width: 100px;" />
			</td>
			<td>
				<input id="qryMobileNo" class="easyui-textbox" style="width: 100px;" />
			</td>
			<td>
				<input id="qryAddress" class="easyui-textbox" style="width: 100px;" />
			</td>
			<td>
				<input id="qryEmail" class="easyui-textbox" style="width: 100px;" />
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
		</tr>
	</table>
</div>
<div style="margin: 10px 0 0 10px; height: 30px; ">
	<div style="float: left;">
		<div id="pg" class="easyui-pagination" style="width: 400px;" data-options="onSelectPage: qryMemberList, showRefresh: false, total: 0" ></div>
	</div>
	<div style="float: left; position: relative; top: 2px; margin-left: 20px;">
		排序：
		<select id="order" class="easyui-combobox" data-options="multiple: true, panelHeight: 'auto', onHidePanel: qryMemberList, onChange: chgOrder" style="width: 250px;">
			<option value="userId:A">帳號&#x25B2;</option>
			<option value="userId:D">帳號&#x25BC;</option>
			<option value="userName:A">姓名&#x25B2;</option>
			<option value="userName:D">姓名&#x25BC;</option>
			<option value="departId:A">科系&#x25B2;</option>
			<option value="departId:D">科系&#x25BC;</option>
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
<div style="margin: 10px 0 0 10px;">
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
					<span class="orderField" onclick="chgOrder(this);">科系</span>
					<span class="orderMark" order="departId">&nbsp;</span>
				</td>
				<td style="width: 100px;" class="headCol4">編號</td>
				<td style="width: 100px;" class="headCol5">手機號碼</td>
				<td style="width: 150px;" class="headCol6">通訊地址</td>
				<td style="width: 200px;" class="headCol7">電子郵件</td>
				<td style="width: 90px;" class="headCol6">
					<span class="orderField" onclick="chgOrder(this);">生效日期</span>
					<span class="orderMark" order="beginDate">&nbsp;</span>
				</td>
				<td style="width: 90px;" class="headCol5">
					<span class="orderField" onclick="chgOrder(this);">失效日期</span>
					<span class="orderMark" order="endDate">&nbsp;</span>
				</td>
				<td style="width: 160px;" class="headCol4">處理動作</td>
			</tr>
		</table>
	</div>
	<div style="clear: both; width: 1310px; height: 380px; overflow: auto;">
		<table id="memberList" class="listData">
		</table>
	</div>
</div>
<div id="memberEdit" class="easyui-dialog" style="width: 700px; height: 400px; display: none;"
	data-options="modal: true, title: '編輯人員資料', closed: true, onClose: memberEditClose">
	<input id="memberEditMode" type="hidden" />
	<div style="float: left;">
		<div style="clear: both;">
			<table style="margin: 20px 0 0 20px;">
				<tr class="modPane">
					<td>原帳號</td>
					<td>
						<input id="editUserIdOrg" class="easyui-textbox" style="width: 250px;" data-options="readonly: true" />
					</td>
				</tr>
				<tr>
					<td>帳號</td>
					<td>
						<input id="editUserId" class="easyui-textbox" style="width: 250px;" />
					</td>
				</tr>
				<tr>
					<td>姓名</td>
					<td>
						<input id="editUserName" class="easyui-textbox" style="width: 250px;" />
					</td>
				</tr>
				<tr>
					<td>科系</td>
					<td>
						<input id="editDepartId" class="easyui-combobox" style="width: 250px;" data-options="editable: false, panelHeight: 'auto'"/>
					</td>
				</tr>
				<tr>
					<td>編號</td>
					<td>
						<input id="editUserNo" class="easyui-textbox" style="width: 250px;"/>
					</td>
				</tr>
				<tr>
					<td>手機號碼</td>
					<td>
						<input id="editMobileNo" class="easyui-textbox" style="width: 250px;"/>
					</td>
				</tr>
				<tr>
					<td>通訊地址</td>
					<td>
						<input id="editAddress" class="easyui-textbox" style="width: 250px;"/>
					</td>
				</tr>
				<tr>
					<td>電子郵件</td>
					<td>
						<input id="editEmail" class="easyui-textbox" style="width: 250px;"/>
					</td>
				</tr>
				<tr>
					<td>生效日期</td>
					<td>
						<input id="editBeginDate" class="easyui-datebox" style="width: 250px;" />
					</td>
				</tr>
				<tr>
					<td>失效日期</td>
					<td>
						<input id="editEndDate" class="easyui-datebox" style="width: 250px;" />
					</td>
				</tr>
				<tr>
					<td colspan="2" style="text-align: center;">
						<button type="button" style="width: 100px;" onclick="memberEditDone();">儲存人員</button>
					</td>
			</table>
		</div>
	</div>
	<div style="float: left; margin: 0 0 0 30px;">
		<div style="clear: both; margin: 10px 0 0 0;">
			<input id="alterUser" class="easyui-switchbutton" style="width: 150px;" data-options="onText: '一併轉入登入帳號', offText: '不要變更登入帳號', onChange: alterUserChg" />
		</div>
		<div class="alterUserPane" style="clear: both; margin: 10px 0 0 0; visibility: hidden;">
			<table>
				<tr>
					<td>選單</td>
					<td>
						<input id="editMenuId" class="easyui-combobox" style="width: 200px;" data-options="editable: false, panelHeight: 'auto'"/>
					</td>
				</tr>
				<tr>
					<td>初始密碼</td>
					<td>
						<input id="editInitPass" class="easyui-textbox" style="width: 200px;" />
					</td>
				</tr>
				<tr>
					<td>備註</td>
					<td>
						<input id="editRemark" class="easyui-textbox" style="width: 200px; height: 60px;" data-options="multiline: true"/>
					</td>
				</tr>
			</table>
		</div>
		<div class="alterUserPane" style="clear: both; margin: 5px 0 0 0; visibility: hidden;">
			<div id="roleList" class="easyui-datagrid" style="width: 300px; height: 150px;" data-options="
				title: '選用角色列表', singleSelect: false, idField: 'roleId', columns: [[
					{ field: 'sel', title: '', checkbox: true },
					{ field: 'roleId', title: '角色代碼', halign: 'center', width: 100 },
					{ field: 'roleName', title: '角色名稱', halign: 'center', width: 150 },
				]]
			">
			</div>
		</div>
	</div>
</div>
<input id="alterUserDel" type="hidden" />
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