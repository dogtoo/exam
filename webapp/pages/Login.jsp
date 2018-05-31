<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Ron Examination</title>
<link rel="stylesheet" type="text/css" href="css/reset.css" />
<link rel="stylesheet" type="text/css" href="css/easyui.css" />
<link rel="stylesheet" type="text/css" href="css/icon.css" />
<style>
#login table {
	width: 380px;
	font-size: 14px;
}
#login td {
	padding: 10px 0 10px 0;
	color: #404040;
}
#login button {
	color: #404040;
	width: 90px;
	height: 28px;
	font-size: 14px;
	line-height: 14px;
	vertical-align: middle;
}
#userId + .textbox .textbox-text {
	text-transform: uppercase;
}
</style>
<script type="text/javascript" src="js/jquery-1.12.4.js"></script>
<script type="text/javascript" src="js/jquery.easyui.min.js"></script>
<script type="text/javascript" src="js/easyui-lang-zh_TW.js"></script>
<script type="text/javascript">
function login() {
	var req = {};
	req.userId = $("#userId").textbox("getValue").toUpperCase();
	req.userPass = $("#userPass").passwordbox("getValue");
	$.post("Login_login", req, function (res) {
		if (res.success) {
			window.location.href = "Main";
		}
		else {
			$.messager.alert("登入失敗", res.status, "error");
		}
	}, "json");
}

function inputField(evt) {
	if (evt.keyCode == 13)
		login();
}
//function recaptcha(data) {
//	$("#recaptcha").val(data);
//}
</script>
</head>
<body>
<div style="width: 1340px; height: 670px; marin: 0 auto;" >
	<div style="position: relative; top: 200px; left: 350px; width: 550px;" >
		<div style="margin-left: 100px; width: 500px;">
			<img src="images/ron_logo.png" style="width: 200px; height: 50px; "/>
			<span style="font-size: 30px; vertical-align: bottom;">試場維運系統</span>
		</div>
		<div style="margin-left: 180px; margin-top: 20px;">
			<table id="login">
				<tr>
					<td>人員帳號：</td>
					<td><input id="userId" class="easyui-textbox" style="width: 150px;" /></td>
				</tr>
				<tr>
					<td>人員密碼：</td>
					<td><input id="userPass" class="easyui-passwordbox" style="width: 150px;" /></td>
				</tr>
				<tr>
					<td colspan="2" style="text-align: center;">
						<button id="login" type="button" onclick="login();">登入系統</button>
					</td>
				</tr>
			</table>
		</div>
		<div style="margin-top: 60px; margin-left: 100px; width: 500px;">
			<span style="color: #a0a0a0; font-size: 12px;">
				榮恩國際有限公司版權所有 &#xa9; 2017 Ron International Co, Ltd. All Rights Reserved.<br>
			</span>
			<span style="color: #a0a0a0; font-size: 12px; display: inline-block; margin-left: 200px;">
				building: 2018-01-26
			</span>
		</div>
	</div>
</div>
<input type="hidden" id="success" value="${success}" />
<input type="hidden" id="status" value="${status}" />
<input type="hidden" id="statusTime" value="${statusTime}" />
</body>
<script type="text/javascript">
if ($("#success").val() == "Y")
	window.location.href = "Main";
if ($("#status").val() != "")
	$.messager.alert("登入失敗", $("#status").val(), "error");
setTimeout(function () {
	$("#userId").textbox("textbox").bind("keypress", null, inputField);
	$("#userPass").passwordbox("textbox").bind("keypress", null, inputField);
	$("#userId").textbox("textbox").focus();
}, 500);
</script>
</html>
