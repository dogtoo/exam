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
.panel-body {
	font-size: inherit;
	color: inherit;
}

#paramEdit table {
	margin: 15px 0 0 40px;
}

#paramEdit td {
	padding: 3px 5px 3px 5px;
}
</style>
<script type="text/javascript">
function paramClassChg(nv, ov) {
	var req = {};
	req.paramClass = nv;
	$.post("SetParam_qryParamList", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			var queryHide = $("#queryHide").val();
			var jparamList = $("#paramList");
			jparamList.find("tr").remove();
			for (var i = 0; i < res.paramList.length; i++) {
				var param = res.paramList[i];
				var append = "<tr class='" + (i & 1 ? "odd" : "even") + "'>\n" +
					"<td>\n" +
					"    <input name='paramClass' type='hidden' value='" + req.paramClass + "'>\n" +
					"    <input name='paramId' type='text' style='width: 150px;' readonly value='" + param.paramId + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input type='text' style='width: 200px;' readonly value='" + param.paramName + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input type='text' style='width: 80px; text-align: center;' readonly value='" + param.paramType + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input type='text' style='width: 200px;' readonly value='" + param.currValue + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input type='text' style='width: 200px;' readonly value='" + param.initValue + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input type='text' style='width: 50px; text-align: center;' readonly value='" + param.showOrder + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <div style='width: 80px; text-align: center;'>\n" +
					"    <button style='width: 70px;" + queryHide + "' onclick='paramMod(this);' " + (param.editable == "Y" ? "" : "disabled") + ">編輯</button>\n" +
					"    </div>\n" +
					"</td>\n" +
					"</tr>\n";
				jparamList.append(append);
			}
		}
	}, "json");
}

function paramMod(src) {
	var jrow = $(src).parent().parent().parent();
	jrow.addClass("select");
	var req = {};
	req.paramClass = jrow.find("[name='paramClass']").val();
	req.paramId = jrow.find("[name='paramId']").val();
	$.post("SetParam_qryParam", req, function(res) {
		parent.showStatus(res);
		if (res.success) {
			$("#paramEdit").dialog("open");
			$("#editParamClass").val(req.paramClass);
			$("#editParamClassName").textbox("setValue", res.paramClass);
			$("#editParamId").textbox("setValue", req.paramId);
			$("#editParamName").textbox("setValue", res.paramName);
			$("#editParamType").val(res.paramType);
			$("#editParamTypeDesc").textbox("setValue", res.paramTypeDesc);
			$("#editCurrValue").textbox("setValue", res.currValue);
			$("#editInitValue").textbox("setValue", res.initValue);
			$("#editRemark").textbox("setValue", res.remark);
		}
	}, "json");
}

function paramEditDone() {
	var req = {};
	req.paramClass = $("#editParamClass").val();
	req.paramId = $("#editParamId").textbox("getValue");
	req.currValue = $("#editCurrValue").textbox("getValue");
	$.post("SetParam_modParam", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#paramEdit").dialog("close");
			paramClassChg(req.paramClass);
		}
	}, "json");
}

function paramEditClose() {
	$("#paramList tr").removeClass("select");
}

</script>
</head>
<body>
<div style="margin: 5px 5px 0 10px;">
	<div style="margin: 5px 0 5px 0; height: 25px;">
		系統參數類別：
		<select id="paramClass" class="easyui-combobox" style="width: 200px;" data-options="editable: false, panelHeight: 'auto', onChange: paramClassChg" >
			${paramClassList}
		</select>
	</div>
	<div>
		<table class="listHead">
			<tr>
				<td class="headCol1" style="width: 150px;">參數代碼</td>
				<td class="headCol2" style="width: 200px;">參數名稱</td>
				<td class="headCol3" style="width: 80px;">參數型態</td>
				<td class="headCol4" style="width: 200px;">目前設定值</td>
				<td class="headCol5" style="width: 200px;">系統初始值</td>
				<td class="headCol6" style="width: 50px;">順序</td>
				<td class="headCol7" style="width: 80px;">編輯</td>
			</tr>
		</table>
	</div>
	<div style="width: 980px; height: 400px; overflow: auto; ">
		<table id="paramList" class="listData">
		</table>
	</div>
</div>
<div id="paramEdit" class="easyui-dialog" style="width: 400px; height: 400px; display: none;" data-options="title: '編輯參數', modal: true, closed: true, onClose: paramEditClose">
	<table>
		<tr>
			<td>參數類別</td>
			<td>
				<input id="editParamClass" type="hidden" />
				<input id="editParamClassName" class="easyui-textbox" style="width: 250px;" data-options="editable: false" />
			</td>
		</tr>
		<tr>
			<td>參數代碼</td>
			<td>
				<input id="editParamId" class="easyui-textbox" style="width: 250px;" data-options="editable: false" />
			</td>
		</tr>
		<tr>
			<td>參數名稱</td>
			<td>
				<input id="editParamName" class="easyui-textbox" style="width: 250px;" data-options="editable: false" />
			</td>
		</tr>
		<tr>
			<td>參數型態</td>
			<td>
				<input id="editParamType" type="hidden" />
				<input id="editParamTypeDesc" class="easyui-textbox" style="width: 250px;" data-options="editable: false" />
			</td>
		</tr>
		<tr>
			<td>目前值</td>
			<td>
				<input id="editCurrValue" class="easyui-textbox" style="width: 250px;" data-options="" />
			</td>
		</tr>
		<tr>
			<td>初始值</td>
			<td>
				<input id="editInitValue" class="easyui-textbox" style="width: 250px;" data-options="editable: false" />
			</td>
		</tr>
		<tr>
			<td>參數說明</td>
			<td>
				<input id="editRemark" class="easyui-textbox" style="width: 250px; height: 70px;" data-options="multiline: true, editable: false" />
			</td>
		</tr>
		<tr>
			<td colspan="2" style="text-align: center;">
				<button style="width: 150px;" onclick="paramEditDone();">編輯完成</button>
				</td>
			</tr>
		</table>
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