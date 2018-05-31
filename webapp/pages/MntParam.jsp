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
function qryParamClassList() {
	var req = {};
	$.post("MntParam_qryParamClassList", req, function (res) {
		parent.showStatus(res);
		if ('paramClassList' in res) {
			var jparamClassList = $("#paramClassList");
			jparamClassList.find("tr").remove();
			for (var i = 0; i < res.paramClassList.length; i++) {
				var param = res.paramClassList[i];
				var append = "<tr class='" + (i & 1 ? "odd" : "even") + "'>\n" +
					"<td>\n" +
					"    <input name='paramClass' type='text' style='width: 100px;' readonly value='" + param.paramClass + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='paramName' type='text' style='width: 200px;' readonly value='" + param.paramName + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='paramName' type='text' style='width: 100px; text-align: right;' readonly value='" + param.idLen + "' />\n" +
					"</td>\n" +
					"</tr>\n";
				jparamClassList.append(append);
				var jrow = jparamClassList.find("tr:last");
				jrow.find("input").on("click", { tab: jparamClassList, row: jrow }, function(evt) {
					evt.data.tab.find("tr").removeClass("select");
					evt.data.row.addClass("select");
					var paramClass = evt.data.row.find("[name='paramClass']").val();
					var paramName = evt.data.row.find("[name='paramName']").val();
					$("#selectUserParamClass").val(paramClass);
					$("#selectUserParamName").val(paramName);
					qryParamList(paramClass);
				});
			}
		}
	}, "json");
}

function qryParamList(paramClass) {
	var req = {};
	req.paramClass = paramClass;
	$.post("MntParam_qryParamList", req, function (res) {
		parent.showStatus(res);
		if ('paramList' in res) {
			var queryHide = $("#queryHide").val();
			var jparamList = $("#paramList");
			jparamList.find("tr").remove();
			for (var i = 0; i < res.paramList.length; i++) {
				var param = res.paramList[i];
				var append = "<tr class='" + (i & 1 ? "odd" : "even") + "'>\n" +
					"<td>\n" +
					"    <input name='paramId' type='text' style='width: 100px;' readonly value='" + param.paramId + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='paramName' type='text' style='width: 200px;' readonly value='" + param.paramName + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='currValue' type='text' style='width: 200px;' readonly value='" + param.currValue + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='showOrder' type='text' style='width: 50px; text-align: right;' readonly value='" + param.showOrder + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <div style='width: 180px;'>\n" +
					"    &nbsp;<button style='width: 80px;' " + queryHide + " onclick='paramMod(this);'>編輯</button>\n" +
					"    &nbsp;<button style='width: 80px;' " + queryHide + " onclick='paramDel(this);'>刪除</button>\n" +
					"    </div>\n" +
					"</td>\n" +
					"</tr>\n";
				jparamList.append(append);
			}
		}
	}, "json");
}

function paramAdd() {
	var paramClass = $("#selectUserParamClass").val();
	var paramName = $("#selectUserParamName").val();
	if (paramClass == "") {
		parent.showStatus({ statusTime: "", status: "請先選擇類別代碼" });
		return;
	}
	$("#editParamClass").textbox("setValue", paramClass);
	$("#editParamClassName").textbox("setValue", paramName);
	$("#paramEdit_paramIdOrg").css("visibility", "hidden");
	$("#editParamId").textbox("setValue", "");
	$("#editParamName").textbox("setValue", "");
	$("#editCurrValue").textbox("setValue", "");
	$("#editShowOrder").textbox("setValue", "");
	$("#editRemark").textbox("setValue", "");
	$("#paramEdit").dialog("open");
	$("#paramEditMode").val("A");
}

function paramMod(src) {
	var paramClass = $("#selectUserParamClass").val();
	var paramName = $("#selectUserParamName").val();
	var jrow = $(src).parent().parent().parent();
	jrow.addClass("select");
	var paramId = jrow.find("[name='paramId']").val();
	$("#editParamClass").textbox("setValue", paramClass);
	$("#editParamClassName").textbox("setValue", paramName);
	$("#paramEdit_paramIdOrg").css("visibility", "visible");
	$("#editParamIdOrg").textbox("setValue", paramId);
	$("#editParamId").textbox("setValue", paramId);
	$("#editParamName").textbox("setValue", jrow.find("[name='paramName']").val());
	$("#editCurrValue").textbox("setValue", jrow.find("[name='currValue']").val());
	$("#editShowOrder").textbox("setValue", jrow.find("[name='showOrder']").val());
	$("#editRemark").textbox("setValue", jrow.find("[name='remark']").val());
	$("#paramEdit").dialog("open");
	$("#paramEditMode").val("M");
}

function paramEditDone() {
	var req = {};
	req.paramClass = $("#editParamClass").textbox("getValue");
	req.paramId = $("#editParamId").textbox("getValue");
	req.paramName = $("#editParamName").textbox("getValue");
	req.currValue = $("#editCurrValue").textbox("getValue");
	req.showOrder = $("#editShowOrder").textbox("getValue");
	req.remark = $("#editRemark").textbox("getValue");
	if ($("#paramEditMode").val() == "A") {
		$.post("MntParam_addParam", req, function (res) {
			parent.showStatus(res);
			if (res.success) {
				$("#paramEdit").dialog("close");
				qryParamList($("#selectUserParamClass").val());
			}
		}, "json");
	}
	else {
		req.paramIdOrg = $("#editParamIdOrg").textbox("getValue");
		$.post("MntParam_modParam", req, function (res) {
			parent.showStatus(res);
			if (res.success) {
				$("#paramEdit").dialog("close");
				qryParamList($("#selectUserParamClass").val());
			}
		}, "json");
	}
}

function paramEditClose() {
	$("#paramList tr").removeClass("select");
}

function paramDel(src) {
	var req = {};
	var jrow = $(src).parent().parent().parent();
	jrow.addClass("select");
	req.paramClass = $("#selectUserParamClass").val();
	req.paramId = jrow.find("[name='paramId']").val();
	$.messager.confirm({
		title: '確認',
		msg: '是否要刪除此筆參數？',
		ok: '刪除',
		cancel: '取消',
		fn: function (ok) {
			jrow.removeClass("select");
			if (ok) {
				$.post("MntParam_delParam", req, function (res) {
					parent.showStatus(res);
					if (res.success)
						qryParamList($("#selectUserParamClass").val());
				}, "json");
			}
		}
	});
}
</script>
</head>
<body>
<div style="float: left; margin: 5px 5px 0 10px;">
	<div style="margin: 5px 0 5px 0; height: 25px;">自定參數類別列表：</div>
	<div>
		<table class="listHead">
			<tr>
				<td class="headCol1" style="width: 100px;">類別代碼</td>
				<td class="headCol2" style="width: 200px;">類別說明</td>
				<td class="headCol3" style="width: 100px;">代碼字數</td>
			</tr>
		</table>
	</div>
	<div style="width: 416px; height: 400px; overflow: auto;">
		<table id="paramClassList" class="listData">
		</table>
	</div>
</div>
<input id="selectUserParamClass" type="hidden" />
<input id="selectUserParamName" type="hidden" />
<div style="float: left; margin: 5px 5px 0 30px;">
	<div style="margin: 5px 0 5px 0; height: 25px;">
		指定類別包含參數列表：
		<button style="width: 100px;" ${queryHide} onclick="paramAdd();">新增參數</button>
	</div>
	<div>
		<table class="listHead">
			<tr>
				<td class="headCol1" style="width: 100px;">參數代碼</td>
				<td class="headCol2" style="width: 200px;">參數說明</td>
				<td class="headCol3" style="width: 200px;">參數附加值</td>
				<td class="headCol4" style="width: 50px;">順序</td>
				<td class="headCol5" style="width: 180px;">編輯</td>
			</tr>
		</table>
	</div>
	<div style="width: 746px; height: 400px; overflow: auto;">
		<table id="paramList" class="listData">
		</table>
	</div>
</div>
<div id="paramEdit" class="easyui-dialog" style="width: 350px; height: 400px; display: none;" data-options="title: '編輯參數', modal: true, closed: true, onClose: paramEditClose">
	<input id="paramEditMode" type="hidden" />
	<table>
		<tr>
			<td>參數類別</td>
			<td>
				<input id="editParamClass" class="easyui-textbox" style="width: 150px;" data-options="readonly: true" />
			</td>
		</tr>
		<tr>
			<td>類別說明</td>
			<td>
				<input id="editParamClassName" class="easyui-textbox" style="width: 150px;" data-options="readonly: true" />
			</td>
		</tr>
		<tr id="paramEdit_paramIdOrg">
			<td>原參數代碼</td>
			<td>
				<input id="editParamIdOrg" class="easyui-textbox" style="width: 150px;" data-options="readonly: true" />
			</td>
		</tr>
		<tr>
			<td>參數代碼</td>
			<td>
				<input id="editParamId" class="easyui-textbox" style="width: 150px;" data-options="" />
			</td>
		</tr>
		<tr>
			<td>參數名稱</td>
			<td>
				<input id="editParamName" class="easyui-textbox" style="width: 150px;" data-options="" />
			</td>
		</tr>
		<tr>
			<td>參數附加值</td>
			<td>
				<input id="editCurrValue" class="easyui-textbox" style="width: 150px;" data-options="" />
			</td>
		</tr>
		<tr>
			<td>順序</td>
			<td>
				<input id="editShowOrder" class="easyui-textbox" style="width: 150px;" data-options="" />
			</td>
		</tr>
		<tr>
			<td>備註</td>
			<td>
				<input id="editRemark" class="easyui-textbox" style="width: 150px;" data-options="" />
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
qryParamClassList();
</script>
</html>