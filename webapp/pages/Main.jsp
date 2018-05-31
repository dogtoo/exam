<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>試場維運系統</title>
<link rel="stylesheet" type="text/css" href="css/reset.css" />
<link rel="stylesheet" type="text/css" href="css/easyui.css" />
<link rel="stylesheet" type="text/css" href="css/icon.css" />
<style>
body {
	color: #404040;
}

#info td {
	padding: 1px 3px 1px 3px;
	color: inherit;
}

#info .textbox .textbox-text {
	color: inherit;
}

.menuArea {
	float: left;
	background-color: #344b6b;
}

.menuTop {
	font-size: 16px;
	color: #ffffff;
	cursor: pointer;
	padding: 8px 25px 0 15px;
}

.menuDrop {
	display: none;
	padding: 0 5px 0 5px;
	border: 3px double white;
	margin: 10px 10px 10px 10px;
}

.menuItem {
	font-size: 16px;
	color: #ffffff;
	padding: 10px 0px 10px 0px;
	cursor: pointer;
}

.menuArea:hover .menuDrop {
	display: block;
}

.menuArea:hover .menuTop {
	color: #80ff80;
}

.menuItem:hover {
	color: #80ff80;
}

#statusPanel .textbox .textbox-text {
	color: inherit;
}
</style>
<script type="text/javascript" src="js/jquery-1.12.4.js"></script>
<script type="text/javascript" src="js/jquery.easyui.min.js"></script>
<script type="text/javascript" src="js/easyui-lang-zh_TW.js"></script>
<script type="text/javascript" src="js/detect-zoom.js"></script>
<script type="text/javascript">
function showMenu() {
	var req = {};
	$.post("Main_qryProg", req, function (res) {
		showStatus(res);
		var jmenu = $("#menuBar");
		jmenu.find("div").remove();
		var menuRoot = res.menu;
		for (var i = 0; i < menuRoot.subs.length; i++) {
			var menuTop = menuRoot.subs[i];
			var append =
				"<div class='menuArea'>\n" +
				"	<div class='menuTop'>" + menuTop.desc + "</div>\n";
				"</div>\n";
			jmenu.append(append);
			var jarea = jmenu.find(".menuArea:last");
			var append = "<div class='menuDrop'></div>\n"
			jarea.append(append);
			var jdrop = jarea.find(".menuDrop");
			for (var j = 0; j < menuTop.subs.length; j++) {
				var menu = menuTop.subs[j];
				var prog = menu.prog;
				var append = "<div class='menuItem' onclick=\"selProg('" + prog + "');\">" + menu.desc + "</div>\n";
				jdrop.append(append);
			}
		}
	}, "json");
}

function selProg(progId) {
	$("#progPane").attr("src", progId);
}

function logout() {
	var req = {};
	$.post("Main_logout", req, function (res) {
		showStatus(res);
		window.location.href = 'Login';
	}, "json");
}

function showProg(prog) {
	$("#progId").textbox("setValue", prog.id);
	$("#privDesc").textbox("setValue", prog.priv);
	$("#progTitle").textbox("setValue", prog.title);
}

function showStatus(res) {
	$("#statusTime").textbox("setValue", 'statusTime' in res ? res.statusTime : "");
	$("#status").textbox("setValue", 'status' in res ? res.status : "");
}

function showSize() {
	var browserWidth = Math.max(
		document.body.scrollWidth,
		document.documentElement.scrollWidth,
		document.body.offsetWidth,
		document.documentElement.offsetWidth,
		document.documentElement.clientWidth
	);
	var browserHeight = Math.max(
		document.body.scrollHeight,
		document.documentElement.scrollHeight,
		document.body.offsetHeight,
		document.documentElement.offsetHeight,
		document.documentElement.clientHeight
	);
	var zoom = detectZoom.zoom();
	var frameWidth = $("body div:first").width();
	var frameHeight = $("body div:first").height();
	var rateWidth = zoom * browserWidth / frameWidth;
	var rateHeight = zoom * browserHeight / frameHeight;
	var rate = rateWidth < rateHeight ? rateWidth : rateHeight;
	rate = Math.floor(rate * 10) * 10;
	if (rate > 100) {
		$.messager.show({
			title: '顯示建議',
			msg: '建議瀏覽器顯示比例: ' + rate + '%',
			timeout: 5000,
			showType: 'slide'
		});
	}
}
</script>
</head>
<body style="font-size: 14px; ">
<div style="width: 1340px; height: 670px; margin: 0 auto;">
	<div style="height: 80px;">
		<div style="float: left; width: 240px; height: 80px;">
			<img src="images/ron_logo.png" style="width: 230px; height: 60px; margin: 10px 10px 10px 10px;" />
		</div>
		<div style="float: left; width: 820px; height: 80px;">
			<div style="height: 50px; margin-top: 16px; font-size: 48px; color: #344b6b; text-align: center;">
				試場維運系統${titleAppend}
			</div>
		</div>
		<div style="float: left; width: 280px; height: 80px;">
			<table id="info">
				<tr>
					<td nowrap>登入人員</td>
					<td><input id="userId" class="easyui-textbox" style="width: 100px;" data-options="readonly: true" value="${userId}"/></td>
					<td><input id="userName" class="easyui-textbox" style="width: 100px;" data-options="readonly: true" value="${userName}"/></td>
				</tr>
				<tr>
					<td>作業代碼</td>
					<td><input id="progId" class="easyui-textbox" style="width: 100px;" data-options="readonly: true" /></td>
					<td><input id="privDesc" class="easyui-textbox" style="width: 100px;" data-options="readonly: true" /></td>
				</tr>
				<tr>
					<td>作業名稱</td>
					<td colspan="2">
						<input id="progTitle" class="easyui-textbox" style="width: 205px;" data-options="readonly: true" />
					</td>
				</tr>
			</table>
		</div>
	</div>
	<div style="clear: left; width: 1340px; height: 565px; position: relative;">
		<div style="position: absolute; top: 30px; width: 1338px; height: 535px;
			border-style: solid; border-top-width: 0; border-left-width: 1px; border-right-width: 1px; border-bottom-width: 1px; border-color: #e0e0e0;">
			<iframe id="progPane" style="width: 1338px; height: 535px; overflow: auto;" >
			</iframe>
		</div>

		<div style="position: absolute; top: 0px; left: 0px; width: 1340px; height: 30px; background-color: #344b6b;">
		</div>
		<div id="menuBar" style="position: absolute; top: 0px; left: 20px; width: 1260px;"></div>
		<div class="menuItem" style="position: absolute; left: 1280px;" onclick="logout();">登出</div>
	</div>
	<div id="statusPanel" style="height: 21px; margin-top: 2px;">
		&nbsp;訊息：
		<input id="statusTime" class="easyui-textbox" readonly style="width: 60px; height: 20px;" value="${statusTime}"/>
		<input id="status" class="easyui-textbox" readonly style="width: 1200px; height: 20px;" value="${status}"/>
	</div>
</div>
</body>
<script type="text/javascript">
showMenu();
showSize();
</script>

</html>