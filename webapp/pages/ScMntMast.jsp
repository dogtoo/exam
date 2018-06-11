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
<script type="text/javascript" src="js/datagrid-dnd.js"></script>

<style>
#qryUserId + .textbox .textbox-text {
	text-transform: uppercase;
}
</style>
<script type="text/javascript">

/*
 * 查詢教案。
 */
function qryQsList() {
	var req = {};
	$("#pg").pagination({ total: 1, pageNumber: 1 });
	var queryCond1      = {};
	queryCond1._job     = 'ScMntMast';
	queryCond1.qsId     = $("#qryQsId"    ).textbox("getValue");
	queryCond1.qsName   = $("#qryQsName"  ).textbox("getValue");
	queryCond1.departId = $("#qryDepartId").combobox("getValue");
	queryCond1.targetId = $("#qryTargetId").combobox("getValue");
	var qsClassList     = $("#qryQsClass" ).combobox("getValues");
	for (var i = 0; i < qsClassList.length; i++)
		queryCond1["qryQsClass[" + i + "]"] = qsClassList[i];
	var qsAbilityList   = $("#qryQsAbility").combobox("getValues");
	for (var i = 0; i < qsAbilityList.length; i++)
		queryCond1["qryQsAbility[" + i + "]"] = qsAbilityList[i];
	window.queryCond1 = queryCond1;

	for (var i in window.queryCond1)
		if (i != "_job")
			req[i] = window.queryCond1[i];
	var orders = $("#order").combobox("getValues");
	for (var i = 0; i < orders.length; i++)
		req["order[" + i + "]"] = orders[i];
	req.pageRow = $("#pg").pagination("options").pageSize;
	req.pageAt = $("#pg").pagination("options").pageNumber;
	$.post("ScMntMast_qryQsList", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			if ('total' in res)
				$("#pg").pagination({ total: res.total });
			var jqsList = $("#qsList");
			var widthSum = 15;
			var widthList = jqsList.parent().prev().find("table.listHead td").map(function () { widthSum += $(this).width();	return $(this).width(); });
			jqsList.parent().css("width", widthSum.toString() + "px");
			jqsList.find("tr").remove();
			for (var i = 0; i < res.qsList.length; i++) {
				var qs = res.qsList[i];
				var wat = 0;
				var append = "<tr class='" + (i & 1 ? "odd" : "even") + "'>\n" +
					"<td>\n" +
					"    <input name='qsId'          type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.qsId          + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='qsName'        type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.qsName        + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='totalOptClass' type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.totalOptClass + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='totalScore'    type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.totalScore    + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='passScore'     type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.passScore     + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='borderline'    type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.borderline    + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='fract'         type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.fract         + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='crStd'         type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.crStd         + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <span style='width: " + widthList[wat++] + "px; display: inline-block; text-align: center;'>\n" +
					"                    <button type='button' style='width: 75px;' onclick='modQs(this);'  >編輯教案</button>\n" +
					"        &nbsp;&nbsp;<button type='button' style='width: 75px;' onclick='copQs(this);'  >複製教案</button>\n" +
					"        &nbsp;&nbsp;<button type='button' style='width: 75px;' onclick='modItem(this);'>編輯項目</button>\n" +
					"        &nbsp;&nbsp;<button type='button' style='width: 45px;' onclick='delQs(this);'  >刪除</button>\n" +
					"    </span>\n" +
					"</td>\n" +
					"</tr>\n";
				jqsList.append(append);
				var jrow = jqsList.find("tr:last");
			}
		}
	}, "json");
}

/*
 * 新增教案。
 */
function addQs() {
	var req = {};
	$.post("ScMntMast_qryQs", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#qsEdit"           ).dialog("open");
			$("#qsEditMode"       ).val("A");
			//$("#qsEdit .modPane"  ).css("visibility",    "hidden");
			$("#pCopyModelL").hide();
			$("#pCopyModel").hide();
			$("#editQsId"         ).textbox("setValue",  "");
			$("#editQsName"       ).textbox("setValue",  "");
			$("#editTotalOptClass").combobox("loadData", res.totalOptClassList);
			$("#editTotalOptClass").combobox("setValue", "");
			$("#editTotalScore"   ).textbox("setValue",  "");
			$("#editPassScore"    ).textbox("setValue",  "");
			$("#editBorderline"   ).textbox("setValue",  "");
			$("#editFract"        ).combobox("loadData", res.fractList);
			$("#editFract"        ).combobox("setValue", "");
		}
	}, "json");
}

/*
 * 編輯教案。
 */
function modQs(src) {
	var jrow = $(src).parent().parent().parent();
	var req = {};
	req.qsId = jrow.find("[name='qsId']").val();
	$.post("ScMntMast_qryQs", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			jrow.addClass("select");
			$("#qsEdit"           ).dialog("open");
			$("#qsEditMode"       ).val("M");
			$("#qsEdit .modPane"  ).css("visibility",    "visible");
			$("#editQsIdOrg"      ).textbox("setValue",  req.qsId);
			$("#editQsId"         ).textbox("setValue",  req.qsId);
			$("#editQsName"       ).textbox("setValue",  res.qsName);
			$("#editTotalOptClass").combobox("loadData", res.totalOptClassList);
			$("#editTotalOptClass").combobox("setValue", res.totalOptClass);
			$("#editTotalScore"   ).textbox("setValue",  res.totalScore);
			$("#editPassScore"    ).textbox("setValue",  res.passScore);
			$("#editBorderline"   ).textbox("setValue",  res.borderline);
			$("#editFract"        ).combobox("loadData", res.fractList);
			$("#editFract"        ).combobox("setValue", res.fract);
		}
	}, "json");
}

/*
 * 複製教案。
 */
function copQs(src) {
	var jrow = $(src).parent().parent().parent();
	var req = {};
	req.qsId = jrow.find("[name='qsId']").val();
	$.post("ScMntMast_qryQs", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			jrow.addClass("select");
			$("#qsEdit"           ).dialog("open");
			$("#qsEditMode"       ).val("A");
			//$("#qsEdit .modPane"  ).css("visibility",    "visible");
			$("pCopyModelL").show();			
			$("pCopyModel").show();
			$("#editQsIdOrg"      ).textbox("setValue",  req.qsId);
			$("#editQsId"         ).textbox("setValue",  "");
			$("#editQsName"       ).textbox("setValue",  "");
			$("#editTotalOptClass").combobox("loadData", res.totalOptClassList);
			$("#editTotalOptClass").combobox("setValue", res.totalOptClass);
			$("#editTotalScore"   ).textbox("setValue",  res.totalScore);
			$("#editPassScore"    ).textbox("setValue",  res.passScore);
			$("#editBorderline"   ).textbox("setValue",  res.borderline);
			$("#editFract"        ).combobox("loadData", res.fractList);
			$("#editFract"        ).combobox("setValue", res.fract);
		}
	}, "json");
}

/*
 * 儲存教案(A:新增，M:修改)。
 */
function qsEditDone() {
	var req = {};
	req.qsId          = $("#editQsId"         ).textbox( "getValue");
	req.totalOptClass = $("#editTotalOptClass").combobox("getValue");
	req.totalScore    = $("#editTotalScore"   ).textbox( "getValue");
	req.passScore     = $("#editPassScore"    ).textbox( "getValue");
	req.borderline    = $("#editBorderline"   ).textbox( "getValue");
	req.fract         = $("#editFract"        ).combobox("getValue");
	var url;
	if ($("#qsEditMode").val() == "A")
		url = "ScMntMast_addQs";
	else if ($("#qsEditMode").val() == "M")
		url = "ScMntMast_modQs";
	$.post(url, req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#qsEdit").dialog("close");
			qryQsList();
		}
	}, "json");
}

/*
 * 關閉編輯教案資料視窗。
 */
function qsEditClose() {
	$("#qsList tr.select").removeClass("select");
}

/*
 * 編輯項目。
 */
function modItem(src) {	
	var jrow  = $(src).parent().parent().parent();
	$("#itemList tr.select").removeClass("select");
	jrow.addClass("select");
	var req   = {};
	req.qsId  = jrow.find("[name='qsId']" ).val();
	req.fract = jrow.find("[name='fract']").val();
	$.post("ScMntMast_qryItem", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#itemEdit .modPane").css(     "visibility", "hidden");
			$("#itemEdit"         ).dialog(  "open");
			$("#editItemNo"       ).textbox( "setValue",   "");
			$("#editOptClass"     ).combobox("loadData",   res.optClassList);
			$("#editOptClass"     ).combobox("setValue",   "");
			$("#editItemDesc"     ).textbox( "setValue",   "");
			$("#editTip"          ).textbox( "setValue",   "");
			$("#hEditQsId"        ).textbox( "setValue",   jrow.find("[name='qsId']" ).val());
			
			var optDesc3 = ["完全做到", "部份做到", "沒有做到"];
			var optDesc5 = [5,4,3,2,1];
			
			var cols = [];
			var idx  = 0;
			cols[idx] = {field: 'fItemNo',   title: '編號',     width:  40};
			idx++;
			cols[idx] = {field: 'fItemDesc', title: '項目說明',     width: 460};
			idx++;
			cols[idx] = {field: 'fOptClass', hidden:true};
			idx++;
			var optLen = res.itemList[0].fOptClass.substr(1,1);
			var optTitle;
			if (optLen == 3 )
				optTitle = optDesc3;
			else if (optLen == 5)
				optTitle = optDesc5;
		    var i = 0;
		    for (; i < optLen ; i++) {
		    	cols[i+idx] = {field: 'optClass' + i, title: optTitle[i], width:  65};
		    }
		    idx = idx + i;
			cols[idx] = {field: 'fTip',      hidden:true};
			idx++;
			cols[idx] = {field: 'fAction',   title: '處理動作', width:  65,
			            	formatter:function(value, row, index){
			            		return "<button onclick='delItem(this)'>刪除</button>"
			        		         + "<input type='hidden' name='hItemNo' value='" + row.fItemNo + "' />" ;
			            	}
			            };
		    
		    $("#itemList").datagrid({
		        width:  '100%',
		        height: '100%',
		        singleSelect:true,
		        idField:'fItemNo',
		        //pagination: true,
		        pagePosition: 'top',
		        columns: [cols],
		        onStopDrag: function(){
		            reLoadItemSeq();
		        },
		        onClickRow: function(index, row){
		        	selItem(index, row);
		        }
		    });
			
			$("#itemList").datagrid("loadData", res.itemList);
			$("#itemList").datagrid('enableDnd');
		}
	}, "json");
}

/*
 * 重排項目編號。
 */
function reLoadItemSeq() {
	var pages = $('#itemList').datagrid('getRows');
    for (var i = 0; i < pages.length; i++) {
        $('#itemList').datagrid('updateRow', {
            index: i,
            row: {
            	fItemNo: i + 1
            }
        });     
    }
}

/*
 * 選取項目。
 */
function selItem(index, row) {
	$("#editItemNo"  ).textbox("setValue",  row.fItemNo);
	$("#editOptClass").combobox("setValue", row.fOptClass);
	$("#editItemDesc").textbox("setValue",  row.fItemDesc);
	$("#editTip"     ).textbox("setValue",  row.fTip);
	$("#adUpItem"    ).html("更新");
}

/*
 * 編輯項目-新增/更新。
 */
function updItem() {
	var rows     = $("#itemList").datagrid("getData").rows;
	var itemNo   = $("#editItemNo"  ).textbox("getValue");
	if (itemNo.length == 0)
		itemNo   = rows.length + 1;
	var optClass = $("#editOptClass").combobox("getValue");//.combobox("getText");
	var itemDesc = $("#editItemDesc").textbox("getValue");
	var tip      = $("#editTip"     ).textbox("getValue");
	var nrow     = { fItemNo: itemNo, fOptClass: optClass, fItemDesc: itemDesc, fTip: tip };
	var at;
	for (at = rows.length - 1; at >= 0; at--) {
		var row = rows[at];
		if (itemNo == row.fItemNo)
			break;
	}

	if (at >= 0)
		$("#itemList").datagrid("updateRow", { index: at, row: nrow });
	else
		$("#itemList").datagrid("appendRow", nrow);
	
	$("#itemList").datagrid('enableDnd');
}

/*
 * 編輯項目-重置。
 */
function clearItem() {
	$("#editItemNo"  ).textbox("setValue",  "");
	$("#editOptClass").combobox("setValue", "");
	$("#editItemDesc").textbox("setValue",  "");
	$("#editTip"     ).textbox("setValue",  "");
	$("#adUpItem"    ).html("新增");
}

/*
 * 編輯項目-刪除。
 */
function delItem(src) {
	var itemNo = $(src).next().val();
	$('#itemList').datagrid('deleteRow', itemNo - 1);
	reLoadItemSeq();
}

/*
 * 儲存項目。
 */
function itemEditDone() {
	var req      = {};
	req.qsId     = $("#hEditQsId").textbox("getValue");
	var itemList = $("#itemList").datagrid("getData").rows;
	for (var i = 0; i < itemList.length; i++) {
		var item = itemList[i];
		req["itemNo["   + i + "]"] = item.fItemNo;
		req["optClass[" + i + "]"] = item.fOptClass;
		req["itemDesc[" + i + "]"] = item.fItemDesc;
		req["tip["      + i + "]"] = item.fTip;
	}
	$.post("ScMntMast_addItem", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#itemEdit").dialog("close");
			qryQsList();
		}
	}, "json");
}

/*
 * 關閉編輯項目資料視窗。
 */
function itemEditClose() {
	$("#qsList tr.select").removeClass("select");
}

/*
 * 刪除教案。
 */
function delQs(src) {
	var jrow = $(src).parent().parent().parent();
	$("#qsList tr.select").removeClass("select");
	jrow.addClass("select");
	var req  = {};
	req.qsId = jrow.find("[name='qsId']").val();
	$.messager.confirm({
		title:  '確認',
		msg:    '是否要刪除教案資料？',
		ok:     '刪除',
		cancel: '取消',
		onClose: delClose,
		fn: function (ok) {
			$("#qsList tr.select").removeClass("select");
			if (ok) {
				$.post("ScMntMast_delQs", req, function (res) {
					parent.showStatus(res);
					if (res.success) {
						qryQsList();
					}
				}, "json");
			}
			
			$("#editItemNo"  ).textbox("setValue",  "");
			$("#editOptClass").combobox("setValue", "");
			$("#editItemDesc").textbox("setValue",  "");
			$("#editTip"     ).textbox("setValue",  "");
		}
	});
}

/*
 * 關閉刪除教案視窗。
 */
function delClose() {
	$("#qsList tr.select").removeClass("select");
}

/************************************* 以下公用 *************************************/

/*
 * 變更排序。
 */
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
		qryQsList();
}

/*
 * 清除全部。
 */
function clearAll() {
	$("#qryQsId"     ).textbox(   "setValue",  "");
	$("#qryQsName"   ).textbox(   "setValue",  "");
	$("#qryDepartId" ).combobox(  "setValue",  "");
	$("#qryTargetId" ).combobox(  "setValue",  "");
	$("#qryQsClass"  ).combobox(  "clear");
	$("#qryQsAbility").combobox(  "clear");
	$("#order"       ).combobox(  "setValues", [ "qsId:A" ]);
	chgOrder();
	$("#pg"          ).pagination({ total: 0 });
	$("#qsList tr"   ).remove();
	delete window.queryCond1;
}
</script>
</head>
<body>
<div style="margin: 10px 0 0 10px;">
	<table class="cond">
		<tr>
			<td>教案代碼</td>
			<td>教案名稱</td>
			<td>科別</td>
			<td>對象</td>
			<td>測驗類別</td>
			<td>核心能力</td>
			<td rowspan="2">
				<button type="button" style="width: 100px;" onclick="qryQsList();">查詢教案</button>
			</td>
			<td rowspan="2">
				<button type="button" style="width: 100px;" onclick="addQs();">新增教案</button>
			</td>
		</tr>
		<tr>
			<td>
				<input id="qryQsId"       class="easyui-textbox"  style="width: 100px;" />
			</td>
			<td>
				<input id="qryQsName"     class="easyui-textbox"  style="width: 100px;" />
			</td>
			<td>
				<select id="qryDepartId"  class="easyui-combobox" style="width: 100px;" data-options="editable: false, panelHeight: 'auto'" >
					${departList}
				</select>
			</td>
			<td>
				<select id="qryTargetId"  class="easyui-combobox" style="width: 100px;" data-options="editable: false, panelHeight: 'auto'" >
					${qsTargetList}
				</select>
			</td>
			<td>
				<select id="qryQsClass"   class="easyui-combobox" style="width: 100px;" data-options="editable: false, multiple: true, panelHeight: 'auto', value: ''" >
					${qsClassList}
				</select>
			</td>
			<td>
				<select id="qryQsAbility" class="easyui-combobox" style="width: 140px;" data-options="editable: false, multiple: true, panelHeight: 'auto', value: ''" >
					${qsAbilityList}
				</select>
			</td>
		</tr>
	</table>
</div>
<div style="margin: 10px 0 0 10px; height: 30px; ">
	<div style="float: left;">
		<div id="pg" class="easyui-pagination" style="width: 400px;" data-options="onSelectPage: qryQsList, showRefresh: false, total: 0" ></div>
	</div>
	<div style="float: left; position: relative; top: 2px; margin-left: 20px;">
		排序：
		<select id="order" class="easyui-combobox" data-options="multiple: true, panelHeight: 'auto', onHidePanel: qryQsList, onChange: chgOrder" style="width: 250px;">
			<option value="qsId:A"         >代碼&#x25B2;</option>
			<option value="qsId:D"         >代碼&#x25BC;</option>
			<option value="qsName:A"       >名稱&#x25B2;</option>
			<option value="qsName:D"       >名稱&#x25BC;</option>
			<option value="totalOptClass:A">整體評分&#x25B2;</option>
			<option value="totalOptClass:D">整體評分&#x25BC;</option>
			<option value="totalScore:A"   >滿分&#x25B2;</option>
			<option value="totalScore:D"   >滿分&#x25BC;</option>
			<option value="passScore:A"    >通過&#x25B2;</option>
			<option value="passScore:D"    >通過&#x25BC;</option>
			<option value="borderline:A"   >邊界&#x25B2;</option>
			<option value="borderline:D"   >邊界&#x25BC;</option>
			<option value="fract:A"        >項次評分&#x25B2;</option>
			<option value="fract:D"        >項次評分&#x25BC;</option>
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
					<span class="orderField" onclick="chgOrder(this);">教案代碼</span>
					<span class="orderMark" order="qsId">&#x25B2;</span>
				</td>
				<td style="width: 250px;" class="headCol2">
					<span class="orderField" onclick="chgOrder(this);">教案名稱</span>
					<span class="orderMark" order="qsName">&nbsp;</span>
				</td>
				<td style="width: 120px;" class="headCol3">
					<span class="orderField" onclick="chgOrder(this);">整體評分分級</span>
					<span class="orderMark" order="totalOptClass">&nbsp;</span>
				</td>
				<td style="width: 100px;" class="headCol4">
					<span class="orderField" onclick="chgOrder(this);">滿分分數</span>
					<span class="orderMark" order="totalScore">&nbsp;</span>
				</td>
				<td style="width: 100px;" class="headCol5">
					<span class="orderField" onclick="chgOrder(this);">通過分數</span>
					<span class="orderMark" order="passScore">&nbsp;</span>
				</td>
				<td style="width: 100px;" class="headCol6">
					<span class="orderField" onclick="chgOrder(this);">邊界分數</span>
					<span class="orderMark" order="borderline">&nbsp;</span>
				</td>
				<td style="width: 120px;" class="headCol7">
					<span class="orderField" onclick="chgOrder(this);">項次評分級別</span>
					<span class="orderMark" order="fract">&nbsp;</span>
				</td>
				<td style="width: 100px;" class="headCol6">項目已建檔</td>
				<td style="width: 310px;" class="headCol5">處理動作</td>
			</tr>
		</table>
	</div>
	<div style="clear: both; height: 380px; overflow: auto;">
		<table id="qsList" class="listData">
		</table>
	</div>
</div>
<div id="qsEdit" class="easyui-dialog" style="width: 370px; height: 300px; display: none;"
	data-options="modal: true, title: '編輯教案資料', closed: true, onClose: qsEditClose">
	<input id="qsEditMode" type="hidden" />
	<div style="float: left; margin: 10px 0 0 10px;">
		<table>
			<tr>
				<td><label id="pCopyModelL">原教案代碼</label></td>
				<td>
				    <div id="pCopyModel">
					<input id="editQsIdOrg" class="easyui-textbox" style="width: 250px;" data-options="readonly: true" />
					</div>
				</td>
			</tr>
			<tr>
				<td>教案代碼</td>
				<td>
					<input id="editQsId"          class="easyui-textbox" style="width: 250px;" />
				</td>
			</tr>
			<tr>
				<td>教案名稱</td>
				<td>
					<input id="editQsName"        class="easyui-textbox" style="width: 250px;"/>
				</td>
			</tr>
			<tr>
				<td>整體評分分級</td>
				<td>
					<input id="editTotalOptClass" class="easyui-combobox" style="width: 250px;" data-options="editable: false, panelHeight: 'auto'"/>
				</td>
			</tr>
			<tr>
				<td>滿分分數</td>
				<td>
					<input id="editTotalScore"    class="easyui-textbox" style="width: 250px;"/>
				</td>
			</tr>
			<tr>
				<td>通過分數</td>
				<td>
					<input id="editPassScore"     class="easyui-textbox" style="width: 250px;"/>
				</td>
			</tr>
			<tr>
				<td>邊界分數</td>
				<td>
					<input id="editBorderline"    class="easyui-textbox" style="width: 250px;"/>
				</td>
			</tr>
			<tr>
				<td>項目評分級別</td>
				<td>
					<input id="editFract" class="easyui-combobox" style="width: 250px;"/>
				</td>
			</tr>
			<tr>
				<td colspan="2" style="text-align: center;">
					<button id="editQsMod" type="button" style="width: 100px;" onclick="qsEditDone();">儲存教案</button>
				</td>
			</tr>
		</table>
	</div>
</div>
<!-- <div id="itemEdit" class="easyui-dialog" style="width: 1242px; height: 460px; display: none;" -->
<div id="itemEdit" class="easyui-dialog" style="width: 940px; height: 460px; display: none;"
	data-options="modal: true, title: '編輯項目資料', closed: true, onClose: itemEditClose">
	<input id="itemEditMode" type="hidden" />
	<div style="float: left; margin: 10px 0 0 10px;">
		<table>
			<tr>
				<td>項目編號</td>
				<td>
					<input id="editItemNo"    class="easyui-textbox"  style="width: 200px;" data-options="editable: false"/>
				</td>
				<td>評分類別代碼</td>
				<td>
					<input id="editOptClass"  class="easyui-combobox" style="width: 200px;" data-options="editable: false, panelHeight: 'auto'"/>
				</td>
				<td style="text-align: right;">
					<button id="adUpItem" type="button" style="width: 60px;" onclick="updItem();">新增</button>
					<button id="clItem"   type="button" style="width: 60px;" onclick="clearItem();">重置</button>
				</td>
			</tr>
			<tr>
				<td>項目說明</td>
				<td colspan="4">
					<input id="editItemDesc"  class="easyui-textbox"  style="width: 848px;" />
				</td>
			</tr>
			<tr>
				<td>評分說明</td>
				<td colspan="4">
					<input id="editTip"       class="easyui-textbox"  style="width: 848px; height: 55px" data-options="multiline: true" />
					<input id="hEditQsId"     class="easyui-textbox" />
				</td>
			</tr>
		</table>
	</div>
	<div style="float: left; margin: 10px 0 0 10px;">
		<table>
			<tr>
				<td>
					<div style="clear: both; width: 900px; height: 250px; overflow: auto;">
				        <table id="itemList" class="listData">
				        </table>
				    </div>
				</td>
			</tr>
			<tr>
				<td style="text-align: right;">
					<button id="editItemMod" type="button" style="width: 100px;" onclick="itemEditDone();">儲存項目</button>
				</td>
			</tr>
		</table>
	</div>
</div>
<input type="hidden" id="progId"     value="${progId}" />
<input type="hidden" id="privDesc"   value="${privDesc}" />
<input type="hidden" id="progTitle"  value="${progTitle}" />
<input type="hidden" id="status"     value="${status}" />
<input type="hidden" id="statusTime" value="${statusTime}" />
</body>
<script type="text/javascript">
    parent.showProg({ id: $("#progId").val(), priv: $("#privDesc").val(), title: $("#progTitle").val() });
    parent.showStatus({ status: $("#status").val(), statusTime: $("#statusTime").val() });
</script>
</html>