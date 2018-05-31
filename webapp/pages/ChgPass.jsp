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
#chgPass tr {
	height: 30px;
}
#chgPass td {
	height: 28px;
	line-height: 28px;
	vertical-align: middle;
	padding: 3px 5px 3px 5px;
}
#chgPass button {
	height: 28px;
	line-height: inherit;
	vertical-align: middle;
	font-size: inherit;
	color: inherit;
} 
#chgPass .textbox .textbox-text {
	font-size: inherit;
	color: inherit;
}
</style>
<script type="text/javascript">
function chgPass() {
	var req = {};
	req.orgPass = $("#orgPass").textbox("getValue");
	req.newPass = $("#newPass").textbox("getValue");
	req.chkPass = $("#chkPass").textbox("getValue");
	$.post("ChgPass_chgPass", req, function (res) {
		parent.showStatus(res);
	}, "json");
}
</script>
</head>
<body style="">
<div style="clear: none;">
	<table id="chgPass" style="margin: 100px auto;">
		<tr style="height: 35px;">
			<td nowrap style="text-align: right;">人員帳號:&nbsp;&nbsp;</td>
			<td>
				<input class='easyui-textbox' style="width: 150px; height: 30px; font-size: 14px;" data-options="readonly: true" value="${userId}" />
			</td>
		</tr>
		<tr style="height: 35px;">
			<td nowrap style="text-align: right;">原密碼:&nbsp;&nbsp;</td>
			<td style="${urlHide}">
				<input class='easyui-textbox' id="orgPass" style="width: 150px; height: 30px; font-size: 14px;" data-options="type: 'password'" />
			</td>
		</tr>
		<tr style="height: 35px;">
			<td nowrap style="text-align: right;">新密碼:&nbsp;&nbsp;</td>
			<td>
				<input class='easyui-textbox' id="newPass" style="width: 150px; height: 30px; font-size: 14px;" data-options="type: 'password'" />
			</td>
		</tr>
		<tr style="height: 35px;">
			<td nowarp style="text-align: right;">密碼確認:&nbsp;&nbsp;</td>
			<td>
				<input class='easyui-textbox' id="chkPass" style="width: 150px; height: 30px; font-size: 14px;" data-options="type: 'password'" />
			</td>
		</tr>
		<tr style="height: 35px;">
			<td nowrap style="text-align: right;">上次修改日期:&nbsp;&nbsp;</td>
			<td>
				<input class="easyui-textbox" style="width: 150px; height: 30px; font-size: 14px;" data-options="readonly: true" value="${chgPassDate}" />
			</td>
		</tr>
		<tr style="height: 35px;">
			<td colspan="2" style="text-align: center;">
				<button type="button" style="width: 150px;" ${queryHide} onclick="chgPass();">變更密碼</button>
			</td>			
		</tr>		
	</table>		
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