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
function uploadFile() {
	$("#upload").form({
		ajax: true, iframe: false,
		onProgress: function (pct) {
			$("#progress").progressbar("setValue", pct);
		},
		success: function (res) {
			res = JSON.parse(res);
			parent.showStatus(res);
			$("#xferResult").textbox("setValue", "");
		}
	});
}

function example() {
	var url = "BatchMember_example";
	window.open(url);
}

function batch() {
	req = {};
	req.xferMode = $("#xferMode").combobox("getValue");
	req.xferForce = $("#xferForce").switchbutton("options").checked ? "Y" : "N";
	$.post("BatchMember_batch", req, function (res) {
		parent.showStatus(res);
		$("#file").filebox("clear");
		$("#progress").progressbar("setValue", 0);
		if (res.success)
			$("#xferResult").textbox("setValue", res.xferResult);
		else
			$("#xferResult").textbox("setValue", "");
	}, "json");
}
</script>
</head>
<body>
<div style="float: left; margin: 0 0 0 10px;">
	<div style="margin: 20px 0 0 0;">
		<form id="upload" method="post" action="BatchMember_uploadFile" enctype="multipart/form-data">
			<div>上傳整批人員處理之 Excel 檔案</div>
			<div style="margin: 10px 0 0 0;">
				<input id="file" name="file" class="easyui-filebox" style="width: 250px;" data-options="
					prompt: '請選擇上傳檔案', buttonText: '選擇檔案'"/>
			</div>
			<div style="margin: 10px 0 0 0;">
				<div id="progress" class="easyui-progressbar" style="width: 250px;" ></div>
			</div>
			<div style="margin: 10px 0 0 0;">
				<button type="submit" style="margin: 0 0 0 20px;">上傳檔案</button>
				<button type="button" style="margin: 0 0 0 50px;" onclick="example();">下載樣本</button>
			</div>
		</form>
	</div>
	<div style="margin: 50px 0 0 0;">
		<div>整批人員資料處理動作:</div>
		<div style="padding: 10px 0 0 0;">
			<select id="xferMode" class="easyui-combobox" style="width: 250px;" data-options="editable: false, panelHeight: 'auto'" >
				<option value="test">測試檔案內容</option>
				<option value="addMember">僅轉入人員資料</option>
				<option value="addLogin">僅轉入登入帳號</option>
				<option value="addBoth">轉入人員及帳號</option>
				<option value="delMember">刪除人員資料</option>
				<option value="delLogin">刪除登入帳號</option>
				<option value="delBoth">刪除人員及帳號</option>
			</select>
		</div>
		<div style="padding: 10px 0 0 0;">
			<input id="xferForce" class="easyui-switchbutton" style="width: 150px;" data-options="onText: '修改已存在帳號', offText: '不異動存在帳號'" />
			&nbsp;&nbsp;&nbsp;&nbsp;
			<button id="batch" type="button" onclick="batch();">批次處理</button>
		</div>
	</div>
</div>
<div style="float: left; margin: 0 0 0 10px;">
	<div style="margin: 10px 0 5px 0;">
		處理結果
	</div>
	<input id="xferResult" class="easyui-textbox" style="width: 1000px; height: 480px;" data-options="readonly: true, multiline: true" />
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
uploadFile();
</script>
</html>