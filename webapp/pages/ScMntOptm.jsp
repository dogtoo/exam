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
#qryOptClass + .textbox .textbox-text {
	text-transform: uppercase;
}
#editOptClassM + .textbox .textbox-text {
	text-transform: uppercase;
}

#editOptIdD + .textbox .textbox-text {
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
		qryOptMList();
}

function clearAll() {
	$("#qryOptClass").textbox("setValue", "");
	$("#qryOptDesc").textbox("setValue", "");
	$("#qryFract").combobox("setValue", "");
	$("#order").combobox("setValues", [ "optClass:A" ]);
	chgOrder();
	$("#pg").pagination({ total: 0});
	$("#optMList tr").remove();
	delete window.queryCond1;
}

function qryOptDList(optClass) {
	var req = {};
	req.optClass = optClass;
	$.post("ScMntOptm_qryOptDList", req, function (res) {
		parent.showStatus(res);
		if ('optDList' in res) {
			var queryHide = $("#queryHide").val();
			var joptDList = $("#optDList");
			joptDList.find("tr").remove();
			for (var i = 0; i < res.optDList.length; i++) {
				var optD = res.optDList[i];
				var append = "<tr class='" + (i & 1 ? "odd" : "even") + "'>\n" +
					"<td>\n" +
					"    <input name='optId' type='text' style='width: 100px;' readonly value='" + optD.optId + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='optDesc' type='text' style='width: 150px;' readonly value='" + optD.optDesc + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='noSel' type='text' style='width: 100px;' readonly value='" + ("Y"==optD.noSel ? "是" : "") + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='score' type='text' style='width: 100px; text-align: right;' readonly value='" + optD.score + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <div style='width: 180px;'>\n" +
					"    &nbsp;<button style='width: 80px;' " + queryHide + " onclick='optDMod(this);'>編輯</button>\n" +
					"    &nbsp;<button style='width: 80px;' " + queryHide + " onclick='optDDel(this);'>刪除</button>\n" +
					"    </div>\n" +
					"</td>\n" +
					"</tr>\n";
					joptDList.append(append);
			}
		}
	}, "json");
}

/*
 * 查詢選項類別
 */
function qryOptMList(mode) {
	var req = {};
	req.mode = mode;
	if (mode == "N") {
		$("#pg").pagination({ total: 1, pageNumber: 1 });
		var queryCond1 = {};
		queryCond1._job = 'ScMntOptm';
		queryCond1.optClass = $("#qryOptClass").textbox("getValue").toUpperCase();
		queryCond1.optDesc = $("#qryOptDesc").textbox("getValue");
		queryCond1.fract =$("#qryFract").combobox("getValue");
		queryCond1.optType =$("#qryOptType").combobox("getValue");
		
		window.queryCond1 = queryCond1;
	}
	else {
		if (!window.queryCond1 || window.queryCond1._job != 'ScMntOptm')
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
	$.post("ScMntOptm_qryOptMList", req, function (res) {
		if (mode != "S" && mode != "T")
			parent.showStatus(res);
		if (res.success) {
			if ('total' in res)
				$("#pg").pagination({ total: res.total });
			var queryHide = $("#queryHide").val();
			var joptMList = $("#optMList");
			joptMList.find("tr").remove();
			for (var i = 0; i < res.optMList.length; i++) {
				var ro = res.optMList[i];
				var append = "<tr class='" + (i & 1 ? "odd" : "even") + "'>\n" +
					"<td>\n" +
					"    <input name='optClass' type='text' style='width: 100px;' readonly value='" + ro.optClass + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='optDesc' type='text' style='width: 150px;' readonly value='" + ro.optDesc + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='fract' type='text' style='width: 80px;' readonly value='" + ro.fract + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='optType' type='hidden' value='" + ro.optType + "' />\n" +
					"    <input name='dCnt' type='hidden' value='" + ro.dCnt + "' />\n" +
					"    <input name='optTypeStr' type='text' style='width: 80px;' readonly value='" + ro.optTypeStr + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <span style='width: 160px; display: inline-block; text-align: center;'>\n" +
					"        <button type='button' style='width: 70px;" + queryHide + "' onclick='modOptM(this);'>編輯</button>\n" +
					"        &nbsp;&nbsp;<button type='button' style='width: 70px;" + queryHide + "' onclick='delOptClass(this);'>刪除</button>\n" +
					"    </span>\n" +
					"</td>\n" +
					"</tr>\n";
				joptMList.append(append);
				var jrow = joptMList.find("tr:last");
				jrow.find("input").on("click", { tab: joptMList, row: jrow }, function(evt) {
					evt.data.tab.find("tr").removeClass("select");
					evt.data.row.addClass("select");
					var optClass = evt.data.row.find("[name='optClass']").val();
					var optDesc = evt.data.row.find("[name='optDesc']").val();
					var fract = evt.data.row.find("[name='fract']").val();
//alert("on click class : optClass=" + optClass+", optDesc="+optDesc+", fract="+fract);
					$("#selectOptClass").val(optClass);
					$("#selectOptDesc").val(optDesc);
					$("#selectFract").val(fract);
					qryOptDList(optClass);
				});
			}
		}
	}, "json");
}
	
function addOptM() {
	$("#optMEdit").dialog("open");
	$("#optMEdit").dialog("setTitle", "新增評分類別");
	$("#optMEditMode").val("A");
	
	$("#editOptTypeM").combobox("readonly", false);
	$("#editOptTypeM").combobox("setValue", "");
	$("#editOptClassM").textbox("readonly", false);
	$("#editOptClassM").textbox("setValue", "");
	$("#editOptDescM").textbox("setValue", "");
	$("#editFractM").combobox("readonly", false);
	$("#editFractM").combobox("setValue", "");
}

function modOptM(src) {
	var jrow = $(src).parent().parent().parent();
	var req = {};
	req.optClass = jrow.find("[name='optClass']").val();
	req.optDesc = jrow.find("[name='optDesc']").val();
	req.fract = jrow.find("[name='fract']").val();
	req.optType = jrow.find("[name='optType']").val();

	$("#optMEdit").dialog("open");
	$("#optMEdit").dialog("setTitle", "編輯評分類別");
	$("#optMEditMode").val("M");
	
	$("#editOptTypeM").combobox("readonly", true);
	$("#editOptTypeM").combobox("setValue", req.optType);
	$("#editOptClassM").textbox("readonly", true);
	$("#editOptClassM").textbox("setValue", req.optClass);
	$("#editOptDescM").textbox("setValue", req.optDesc);
	$("#editFractM").combobox("readonly", true);
	$("#editFractM").combobox("setValue", req.fract);
}

function optMEditClose() {
	$("#optMList tr.select").removeClass("select");
}

function optMEditDone() {
	var req = {};
	req.optType = $("#editOptTypeM").combobox("getValue");
	req.optClass = $("#editOptClassM").textbox("getValue").toUpperCase();
	req.optDesc = $("#editOptDescM").textbox("getValue");
	req.fract = $("#editFractM").combobox("getValue");
	if ($("#optMEditMode").val() == "A") {
		$.post("ScMntOptm_addOptClass", req, function (res) {
			parent.showStatus(res);
			if (res.success) {
				$("#optMEdit").dialog("close");
				qryOptMList("T");
			}
		}, "json");
	}
	else if ($("#optMEditMode").val() == "M") {
		$.post("ScMntOptm_modOptClass", req, function (res) {
			parent.showStatus(res);
			if (res.success) {
				$("#optMEdit").dialog("close");
				qryOptMList("S");
			}
		}, "json");
	}
}

function delOptClass(src) {
	var jrow = $(src).parent().parent().parent();
	$("#optMList tr.select").removeClass("select");
	jrow.addClass("select");
	var req = {};
	req.optClass = jrow.find("[name='optClass']").val();
	var dCnt = jrow.find("[name='dCnt']").val();
	var str = (dCnt>0 ? "此評分類別已存在評分項目，" : "") + '是否要刪除評分類別？';
	$.messager.confirm({
		title: '確認',
		msg: str,
		ok: '刪除',
		cancel: '取消',
		fn: function (ok) {
			$("#optMList tr.select").removeClass("select");
			if (ok) {
				$.post("ScMntOptm_delOptClass", req, function (res) {
					parent.showStatus(res);
					if (res.success) {
						qryOptMList("T");
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
			<td>評分類別</td>
			<td>類別說明</td>
			<td>評分級別</td>
			<td>選項區分</td>
			<td rowspan="2">
				<button type="button" style="width: 100px;" onclick="qryOptMList('N');">查詢評分類別</button>
			</td>
			<td rowspan="2">
				<button type="button" style="width: 100px; ${queryHide}" onclick="addOptM();">新增評分類別</button>
			</td>
		</tr>
		<tr>
			<td>
				<input id="qryOptClass" class="easyui-textbox" style="width: 100px;" />
			</td>
			<td>
				<input id="qryOptDesc" class="easyui-textbox" style="width: 100px;" />
			</td>
			<td>
				<select id="qryFract" class="easyui-combobox" style="width: 80px;" data-options="editable: false, panelHeight: 'auto'" >
					${scFractList}
				</select>
			</td>
			<td>
				<select id="qryOptType" class="easyui-combobox" style="width: 80px;" data-options="editable: false, panelHeight: 'auto'" >
					${scOptTypeList}
				</select>
			</td>
		</tr>
	</table>
</div>

<div style="float: left; margin: 5px 5px 0 10px;">
	<div style="margin: 10px 0 0 10px; height: 30px; ">
		<div style="float: left;">
			<div id="pg" class="easyui-pagination" style="width: 250px;" data-options="layout: ['first','prev','links','next','last'], onSelectPage: qryOptMList, showRefresh: false, total: 0" ></div>
		</div>
		<div style="float: left; position: relative; top: 2px; margin-left: 20px;">
			排序：
			<select id="order" class="easyui-combobox" data-options="multiple: true, panelHeight: 'auto', onHidePanel: qryOptMList, onChange: chgOrder" style="width: 100px;">
				<option value="optClass:A">類別&#x25B2;</option>
				<option value="optClass:D">類別&#x25BC;</option>
				<option value="optDesc:A">說明&#x25B2;</option>
				<option value="optDesc:D">說明&#x25BC;</option>
			</select>
		</div>
		<div style="float: left; position: relative; top: 5px; margin-left: 20px;">
			<span style="font-size: 30px; cursor: pointer;" onclick="clearAll();">&#x239A</span>
		</div>
	</div>
	
	<div style="clear: both;">
		<table class="listHead">
			<tr>
				<td style="width: 100px;" class="headCol1">
					<span class="orderField" onclick="chgOrder(this);">類別代碼</span>
					<span class="orderMark" order="optClass">&#x25B2;</span>
				</td>
				<td style="width: 150px;" class="headCol2">
					<span class="orderField" onclick="chgOrder(this);">類別說明</span>
					<span class="orderMark" order="optDesc">&nbsp;</span>
				</td>
				<td style="width: 80px;" class="headCol3">級別</td>
				<td style="width: 80px;" class="headCol4">選項區分</td>
				<td style="width: 160px;" class="headCol5">處理動作</td>
			</tr>
		</table>
	</div>
	<div style="clear: both; width: 580px; height: 380px; overflow: auto; ">
		<table id="optMList" class="listData">
		</table>
	</div>
</div>
<input id="selectOptClass" type="hidden" />
<input id="selectOptDesc" type="hidden" />
<input id="selectFract" type="hidden" />
<div style="float: left; margin: 5px 5px 0 30px;">
	<div style="margin: 5px 0 5px 0; height: 25px;">
		評分選項列表：
		<button style="width: 100px;" ${queryHide} onclick="optDAdd();">新增評分項目</button>
	</div>
	<div>
		<table class="listHead">
			<tr>
				<td class="headCol1" style="width: 100px;">評分代碼</td>
				<td class="headCol2" style="width: 150px;">評分說明</td>
				<td class="headCol3" style="width: 100px;">不可選取</td>
				<td class="headCol4" style="width: 100px;">答案分數</td>
				<td class="headCol5" style="width: 180px;">編輯</td>
			</tr>
		</table>
	</div>
	<div style="width: 646px; height: 400px; overflow: auto;">
		<table id="optDList" class="listData">
		</table>
	</div>
</div>

<div id="optMEdit" class="easyui-dialog" style="width: 240px; height: 200px; display: none;"
	data-options="modal: true, title: '編輯評分類別', closed: true, onClose: optMEditClose">
	<input id="optMEditMode" type="hidden" />
	<div style="float: left; margin: 10px 0 0 10px;">
		<table>
			<tr>
				<td>選項區分</td>
				<td>
					<select id="editOptTypeM" class="easyui-combobox" style="width: 150px;" data-options="editable: false, panelHeight: 'auto'" >
						${scOptTypeList}
					</select>
				</td>
			</tr>
			<tr>
				<td>類別代碼</td>
				<td>
					<input id="editOptClassM" class="easyui-textbox" style="width: 150px;" />
				</td>
			</tr>
			<tr>
				<td>類別說明</td>
				<td>
					<input id="editOptDescM" class="easyui-textbox" style="width: 150px;" />
				</td>
			</tr>
			<tr>
				<td>評分級別</td>
				<td>
				<select id="editFractM" class="easyui-combobox" style="width: 150px;" data-options="editable: false, panelHeight: 'auto'" >
					${scFractList}
				</select>
			</td>
			</tr>
			<tr>
				<td colspan="2" style="text-align: center;">
					<button type="button" style="width: 100px;" onclick="optMEditDone();">儲存類別</button>
				</td>
			</tr>
		</table>
	</div>
</div>
<div id="optDEdit" class="easyui-dialog" style="width: 350px; height: 420px; display: none;"
	data-options="modal: true, title: '編輯評分項目', closed: true, onClose: optMEditClose">
	<input id="optDEditMode" type="hidden" />
	<div style="float: left; margin: 10px 0 0 10px;">
		<table>
			<tr>
				<td>評分類別</td>
				<td>
					<input id="editOptClassD" class="easyui-textbox" style="width: 150px;" data-options="readonly: true"/>
				</td>
			</tr>
			<tr>
				<td>評分級別</td>
				<td>
					<input id="editFractD" class="easyui-textbox" style="width: 150px;" data-options="readonly: true" />
				</td>
			</tr>
			<tr>
				<td>評分代碼</td>
				<td>
					<input id="editOptIdD" class="easyui-textbox" style="width: 150px;" />
				</td>
			</tr>
			<tr>
				<td>評分說明</td>
				<td>
					<input id="editOptDescD" class="easyui-textbox" style="width: 150px;" />
				</td>
			</tr>
			<tr>
				<td>選取類別</td>
				<td>
					<input id="editNoSelD" class="easyui-switchbutton" style="width: 100px;" data-options="onText: '不可選取', offText: '可以選取'" />
				</td>
			</tr>
			<tr>
				<td>答案分數</td>
				<td>
					<input id="editScore" class="easyui-textbox" style="width: 150px;" />
				</td>
			</tr>
			<tr>
				<td colspan="2" style="text-align: center;">
					<button type="button" style="width: 100px;" onclick="optDEditDone();">儲存評分項目</button>
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