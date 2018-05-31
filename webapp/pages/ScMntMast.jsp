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
function qryQsList(mode) {
	var req = {};
	req.mode = mode;
	if (mode == "N") {
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
	}
	else {
		if (!window.queryCond1 || window.queryCond1._job != 'ScMntMast')
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
	$.post("ScMntMast_qryQsList", req, function (res) {
		if (mode != "S" && mode != "T")
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
					"    <input name='qsId'          type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.qsId + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='qsName'        type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.qsName + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='totalOptClass' type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.totalOptClass + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='totalScore'    type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.totalScore + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='passScore'     type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.passScore + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='borderline'    type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.borderline + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='fract'         type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.fract + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <input name='crStd'         type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.crStd + "' />\n" +
					"</td>\n" +
					"<td>\n" +
					"    <span style='width: " + widthList[wat++] + "px; display: inline-block; text-align: center;'>\n" +
					"        <button type='button' style='width: 75px;' ${queryHide} onclick='modQs(this);'>編輯教案</button>\n" +
					"        &nbsp;&nbsp;<button type='button' style='width: 75px;' ${queryHide} onclick='copQs(this);'>複製教案</button>\n" +
					"        &nbsp;&nbsp;<button type='button' style='width: 75px;' ${queryHide} onclick='modItem(this);'>編輯項目</button>\n" +
					"        &nbsp;&nbsp;<button type='button' style='width: 45px;' ${queryHide} onclick='delQs(this);'>刪除</button>\n" +
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
			$("#qsEdit .modPane"  ).css("visibility",    "hidden");
			$("#editQsId"         ).textbox("setValue",  "");
			$("#editQsName"       ).textbox("setValue",  "");
			$("#editTotalOptClass").combobox("loadData", res.totalOptClassList);
			$("#editTotalOptClass").combobox("setValue", "");
			$("#editTotalScore"   ).textbox("setValue",  "");
			$("#editPassScore"    ).textbox("setValue",  "");
			$("#editBorderline"   ).textbox("setValue",  "");
			$("#editFract"        ).textbox("setValue",  "");
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
			$("#editFract"        ).textbox("setValue",  res.fract);
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
			$("#qsEdit .modPane"  ).css("visibility",    "visible");
			$("#editQsIdOrg"      ).textbox("setValue",  req.qsId);
			$("#editQsId"         ).textbox("setValue",  "");
			$("#editQsName"       ).textbox("setValue",  "");
			$("#editTotalOptClass").combobox("loadData", res.totalOptClassList);
			$("#editTotalOptClass").combobox("setValue", res.totalOptClass);
			$("#editTotalScore"   ).textbox("setValue",  res.totalScore);
			$("#editPassScore"    ).textbox("setValue",  res.passScore);
			$("#editBorderline"   ).textbox("setValue",  res.borderline);
			$("#editFract"        ).textbox("setValue",  res.fract);
		}
	}, "json");
}

/*
 * 儲存教案(A:新增，M:修改)。
 */
function qsEditDone() {
	var req = {};
	req.qsId          = $("#editQsId"         ).textbox("getValue");
	req.qsName        = $("#editQsName"       ).textbox("getValue");
	req.totalOptClass = $("#editTotalOptClass").combobox("getValue");
	req.totalScore    = $("#editTotalScore"   ).textbox("getValue");
	req.passScore     = $("#editPassScore"    ).textbox("getValue");
	req.borderline    = $("#editBorderline"   ).textbox("getValue");
	req.fract         = $("#editFract"        ).textbox("getValue");//combobox("getValue");
	if ($("#qsEditMode").val() == "A") {
		$.post("ScMntMast_addQs", req, function (res) {
			parent.showStatus(res);
			if (res.success) {
				$("#qsEdit").dialog("close");
				qryQsList("T");
			}
		}, "json");
	}
	else if ($("#qsEditMode").val() == "M") {
		req.qsIdOrg = $("#editQsIdOrg").textbox("getValue");
		$.post("ScMntMast_modQs", req, function (res) {
			parent.showStatus(res);
			if (res.success) {
				$("#qsEdit").dialog("close");
				qryQsList("S");
			}
		}, "json");
	}
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
			$("#itemEdit .modPane").css("visibility","hidden");
			$("#itemEdit"    ).dialog("open");
			$("#editOptClass").combobox("loadData", res.optClassList);
			$("#editOptClass").combobox("setValue", res.optClass);
			
			$("#itemList").datagrid("loadData", res.itemList);
			$("#itemList").datagrid('enableDnd');
			
			//$("#editItemNoOrg").textbox("setValue",  "");
			$("#editItemNo"   ).textbox("setValue",  "");
			$("#editOptClass" ).combobox("setValue", "");
			$("#editItemDesc" ).textbox("setValue",  "");
			$("#editTip"      ).textbox("setValue",  "");

			$("#editQsIdH"    ).textbox("setValue",  jrow.find("[name='qsId']" ).val());
			
			//by nickel
			/*var rows = [{}];
			var cols = [{}];
			for (var y = 0; y < roomList.length; y++) {
				rows[y] = {'id':y+1};
				rows[y]['sectSeq'] = sectList[y].sectSeq;
				if (y==0)
					cols[0] = {field:'sectSeq',title:'節次/考場',height:}
				for(var x = 0; x < rooList.length; x++) {
					if (y==0)
						cols[x+1] = {field:'roomSeq' + (x+1) + "第" + "" + "場"};
				}
			}
			//by sam
			$("#itemList").datagrid("columns", ...);*/
			
			/*var jqsList = $("#itemList");
			var widthSum = 15;
			var widthList = jqsList.parent().prev().find("table.listHead td").map(function () { widthSum += $(this).width();    return $(this).width(); });
			jqsList.parent().css("width", widthSum.toString() + "px");
			jqsList.find("tr").remove();
			for (var i = 0; i < res.itemList.length; i++) {
			    var qs = res.itemList[i];
			    var wat = 0;
				var append = "<tr class='" + (i & 1 ? "odd" : "even") + "'>\n" +
				    "<td>\n" +
				    "    <input name='fItemNo'   type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.fItemNo   + "' onclick='clickItem(this); />\n" +
				    "</td>\n" +
				    "<td>\n" +
				    "    <input name='fItemDesc' type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.fItemDesc + "' onclick='clickItem(this); />\n" +
				    "</td>\n" +
				    "<td>\n" +
				    "    <input name='fTip'      type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.fTip      + "' onclick='clickItem(this); />\n" +
				    "</td>\n" +
				    "<td>\n" +
				    "    <input name='fOptClass' type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + qs.fOptClass + "' onclick='clickItem(this); />\n" +
				    "</td>\n" +
				    "</tr>\n";
				jqsList.append(append);
		    	var jrow = jqsList.find("tr:last");
			}*/
		}
	}, "json");
}

/*
 * 編輯項目-新增。
 */
/*function addItem() {
	if ( $("#editItemNo"  ).textbox("getValue" ).length == 0 ||
		 $("#editOptClass").combobox("getValue").length == 0 ||
		 $("#editItemDesc").textbox("getValue" ).length == 0 ){
		parent.showStatus({ statusTime: '', status: '項次編號、評分類別代碼、項目說明不可以為空白'});
		return;
	}
	
	if ( $("#editItemNo"  ).textbox("getValue" ) < "1" ){
			parent.showStatus({ statusTime: '', status: '項次編號不可以小於1'});
			return;
	}
	
	var res = {};
	res.fItemNo   = $("#editItemNo"  ).textbox("getValue");
	res.fOptClass = $("#editOptClass").combobox("getValue");
	res.fItemDesc = $("#editItemDesc").textbox("getValue");
	res.fTip      = $("#editTip"     ).textbox("getValue");

	var itemList  = $("#itemList"  ).datagrid("getData").rows;
	var addItemNo = $("#editItemNo").textbox("getValue");
	var found     = false;
	for (var i = 0; i < itemList.length && !found; i++)
		if (itemList[i].fItemNo == addItemNo)
			found = true;
	if (found)
		parent.showStatus({ statusTime: '', status: '項次編號不可以重複'});
	else {
		var jrow  = $(src).parent().parent().parent();
		$("#itemList tr.select").removeClass("select");
		jrow.addClass("select");
		var req      = {};
		req.qsId     = jrow.find("[name='qsId']" ).val();
		req.itemNo   = $("#editItemNo"  ).textbox("getValue");
		req.itemDesc = $("#editItemDesc").textbox("getValue");
		req.optClass = $("#editOptClass").combobox("getValue");
		req.tip      = $("#editTip"     ).textbox("getValue");
		$.post("ScMntMast_addItem", req, function (res) {
			parent.showStatus(res);
			if (res.success) {
				$("#itemList").dialog("close");
			}
		}, "json");
		
		parent.showStatus({ statusTime: '', status: ''});
		$("#itemList").datagrid("appendRow", res);
	}
	
	
	
	var jqsList = $("#itemList");
	var widthSum = 15;
	var widthList = jqsList.parent().prev().find("table.listHead td").map(function () { widthSum += $(this).width();    return $(this).width(); });
	jqsList.parent().css("width", widthSum.toString() + "px");
	jqsList.find("tr").remove();
	for (var i = 0; i < res.itemList.length; i++) {
	    var qs = res.itemList[i];
	    var wat = 0;
		var append = "<tr class='" + (i & 1 ? "odd" : "even") + "'>\n" +
		    "<td>\n" +
		    "    <input name='fItemNo'   type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + $("#editItemNo"  ).textbox("getValue")  + "' onclick='clickItem(this); />\n" +
		    "</td>\n" +
		    "<td>\n" +
		    "    <input name='fItemDesc' type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + $("#editItemDesc").textbox("getValue")  + "' onclick='clickItem(this); />\n" +
		    "</td>\n" +
		    "<td>\n" +
		    "    <input name='fTip'      type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + $("#editTip").textbox("getValue")       + "' onclick='clickItem(this); />\n" +
		    "</td>\n" +
		    "<td>\n" +
		    "    <input name='fOptClass' type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + $("#editOptClass").combobox("getValue") + "' onclick='clickItem(this); />\n" +
		    "</td>\n" +
		    "</tr>\n";
		jqsList.append(append);
    	var jrow = jqsList.find("tr:last");
	}
}

/*
 * 選取項目。
 */
function selItem(index,row) {
	//$("#editItemNoOrg").textbox("setValue",  row.fItemNo);
	$("#editItemNo"   ).textbox("setValue",  row.fItemNo);
	$("#editOptClass" ).combobox("setValue", row.fOptClass);
	$("#editItemDesc" ).textbox("setValue",  row.fItemDesc);
	$("#editTip"      ).textbox("setValue",  row.fTip);
}

/*
 * 編輯項目-新增、更新。
 */
function updItem() {
	var rows = $("#itemList").datagrid("getData").rows;
	var itemNo   = $("#editItemNo"  ).textbox("getValue");
	if (itemNo.length == 0)
		itemNo = rows.length + 1;
	var optClass = $("#editOptClass").combobox("getValue");//.combobox("getText");
	var itemDesc = $("#editItemDesc").textbox("getValue");
	var tip      = $("#editTip"     ).textbox("getValue");
	var nrow = { fItemNo: itemNo, fOptClass: optClass, fItemDesc: itemDesc, fTip: tip };
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

	$("#editItemNo"   ).textbox("setValue",  "");
	$("#editOptClass" ).combobox("setValue", "");
	$("#editItemDesc" ).textbox("setValue",  "");
	$("#editTip"      ).textbox("setValue",  "");
	
/*	if ( $("#editItemNoOrg"  ).textbox("getValue" ).length == 0) {
		parent.showStatus({ statusTime: '', status: '原項次編號為空白，請使用新增按鈕'});
		return;
	}
	if ( $("#editItemNo"  ).textbox("getValue" ).length == 0 ||
		 $("#editOptClass").combobox("getValue").length == 0 ||
		 $("#editItemDesc").textbox("getValue" ).length == 0 ){
		parent.showStatus({ statusTime: '', status: '項次編號、評分類別代碼、項目說明不可以為空白'});
		return;
	}
		
	if ( $("#editItemNo"  ).textbox("getValue" ) < "1" ){
			parent.showStatus({ statusTime: '', status: '項次編號不可以小於1'});
			return;
	}
		
	var res = {};
	res.fItemNo   = $("#editItemNo"  ).textbox("getValue");
	res.fOptClass = $("#editOptClass").combobox("getValue");
	res.fItemDesc = $("#editItemDesc").textbox("getValue");
	res.fTip      = $("#editTip"     ).textbox("getValue");

	var itemList  = $("#itemList"  ).datagrid("getData").rows;
	var addItemNo = $("#editItemNo").textbox("getValue");
	var found     = false;
	for (var i = 0; i < itemList.length && !found; i++)
		if (itemList[i].fItemNo == addItemNo)
			found = true;
	if (found)
		parent.showStatus({ statusTime: '', status: '項次編號不可以重複'});
	else {
		var jrow  = $(src).parent().parent().parent();
		$("#itemList tr.select").removeClass("select");
		jrow.addClass("select");
		var req       = {};
		req.qsId      = jrow.find("[name='qsId']" ).val();
		req.itemNoOrg = $("#editItemNoOrg").textbox("getValue");
		req.itemNo    = $("#editItemNo"   ).textbox("getValue");
		req.itemDesc  = $("#editItemDesc" ).textbox("getValue");
		req.optClass  = $("#editOptClass" ).combobox("getValue");
		req.tip       = $("#editTip"      ).textbox("getValue");
		$.post("ScMntMast_updItem", req, function (res) {
			parent.showStatus(res);
			if (res.success) {
				$("#itemList").dialog("close");
			}
		}, "json");
		
		parent.showStatus({ statusTime: '', status: ''});
		// 怎麼刪原本的
		$("#itemList").datagrid("appendRow", res);*/
}
	
function itemListFmt(value, row, index) {
	return "<button type='button' style='width: 60px;' onclick='itemListDel(this);' >刪除</button>"//;
	+
		"<input type='hidden' name='hItemNo' value='" + row.fItemNo + "' />" ;
}

function itemListDel(src) {
	var itemNo = $(src).next().val();
	var rows = $("#itemList").datagrid("getData").rows;
	for (var i = 0; i < rows.length; i++) {
		var row = rows[i];
		if (itemNo == row.fItemNo) {
			$("#itemList").datagrid("deleteRow", i);
			break;
		}
	}
}

/*
 * 儲存項目。
 */
function itemEditDone() {
	var req  = {};
	req.qsId = $("#editQsIdH").textbox("getValue");
	/*req.qsName        = $("#editQsName"       ).textbox("getValue");
	req.totalOptClass = $("#editTotalOptClass").textbox("getValue");//combobox("getValue");
	req.totalScore    = $("#editTotalScore"   ).textbox("getValue");
	req.passScore     = $("#editPassScore"    ).textbox("getValue");
	req.borderline    = $("#editBorderline"   ).textbox("getValue");
	req.fract         = $("#editFract"        ).textbox("getValue");//combobox("getValue");*/
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
			//qryQsList("T");
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
	var req = {};
	req.qsId = jrow.find("[name='qsId']").val();
	$.messager.confirm({
		title: '確認',
		msg: '是否要刪除教案資料？',
		ok: '刪除',
		cancel: '取消',
		fn: function (ok) {
			$("#qsList tr.select").removeClass("select");
			if (ok) {
				$.post("ScMntMast_delQs", req, function (res) {
					parent.showStatus(res);
					if (res.success) {
						qryQsList("T");
					}
				}, "json");
			}
		}
	});
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
	$("#qryQsId"     ).textbox("setValue", "");
	$("#qryQsName"   ).textbox("setValue", "");
	$("#qryDepartId" ).combobox("setValue", "");
	$("#qryTargetId" ).combobox("setValue", "");
	$("#qryQsClass"  ).combobox("clear");
	$("#qryQsAbility").combobox("clear");
	$("#order"       ).combobox("setValues", [ "qsId:A" ]);
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
				<button type="button" style="width: 100px;" onclick="qryQsList('N');">查詢教案</button>
			</td>
			<td rowspan="2">
				<button type="button" style="width: 100px;" ${queryHide} onclick="addQs();">新增教案</button>
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
			<tr class="modPane">
				<td>原教案代碼</td>
				<td>
					<input id="editQsIdOrg"       class="easyui-textbox" style="width: 250px;" data-options="readonly: true" />
				</td>
			</tr>
			<tr>
				<td>教案代碼</td>
				<td>
					<input id="editQsId"          class="easyui-textbox" style="width: 250px;" />
				</td>
			</tr>
			<tr class="modPane">
				<td>教案名稱</td>
				<td>
					<input id="editQsName"        class="easyui-textbox" style="width: 250px;" data-options="readonly: true" />
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
					<select id="editFract"         class="easyui-combobox" style="width: 250px;" data-options="editable: false, panelHeight: 'auto'">
						<option value="2">2</option>
						<option value="3">3</option>
						<option value="5">5</option>
					</select>
				</td>
			</tr>
			<tr>
				<td colspan="2" style="text-align: center;">
					<button id="editModBut" type="button" style="width: 100px;" onclick="qsEditDone();">儲存教案</button>
				</td>
			</tr>
		</table>
	</div>
</div>
<div id="itemEdit" class="easyui-dialog" style="width: 1242px; height: 460px; display: none;"
	data-options="modal: true, title: '編輯項目資料', closed: true, onClose: itemEditClose">
	<input id="itemEditMode" type="hidden" />
	<div style="float: left; margin: 10px 0 0 10px;">
		<table>
			<tr>
				<td>項目編號</td>
				<td>
					<input id="editItemNo"    class="easyui-textbox" style="width: 200px;" data-options="editable: false"/>
				</td>
			</tr>
			<tr>
				<td>評分類別代碼</td>
				<td>
					<input id="editOptClass"  class="easyui-combobox" style="width: 200px;" data-options="editable: false, panelHeight: 'auto'"/>
				</td>
			</tr>
			<tr>
				<td>項目說明</td>
				<td>
					<input id="editItemDesc"  class="easyui-textbox" style="width: 200px; height: 118px" data-options="multiline: true" />
				</td>
			</tr>
			<tr>
				<td>評分說明</td>
				<td>
					<input id="editTip"       class="easyui-textbox" style="width: 200px; height: 118px" data-options="multiline: true" />
				</td>
			</tr>
			<tr>
				<td colspan="2" style="text-align: center;">
					<button id="editModBut" type="button" style="width: 80px;" onclick="updItem();">新增 / 更新</button>
				</td>
			</tr>
			<tr class="modPane">
				<td>
					<input id="editQsIdH" class="easyui-textbox" />
				</td>
			</tr>
		</table>
	</div>
	<div style="float: left; margin: 10px 0 0 10px;">
		<table>
			<tr>
				<td>
					<div id="itemList" class="easyui-datagrid" style="width: 900px; height: 358px;"
						 data-options="
						    title: '項目列表', 
						    singleSelect: true, 
						    idField: 'key', 
						    onClickRow: selItem, 
						    onLoadSuccess: function(){
						    					$(this).datagrid('enableDnd');
						    			   },
						 	columns: [[
						 		{ field: 'fItemNo',   title: '編號',     halign: 'center', width: 40 },
						 		{ field: 'fItemDesc', title: '說明',     halign: 'center', width: 460},
						 		{ field: 'fOptClass', title: '評分級數', halign: 'center' },
						 		{ field: 'fTip',      title: '評分說明', halign: 'center'},
						 		{ field: 'fAction',   title: '處理動作', halign: 'center', formatter: itemListFmt},
						 	]] 
						 ">
					</div><!--, width: 80, hidden: true-->
					<!-- <div style="clear: both; height: 900px; overflow: auto;">
						<table id="itemList" class="listData">
						</table>
					</div> -->
				</td>
			</tr>
			<tr>
				<td style="text-align: right;">
					<button id="editModBut" type="button" style="width: 100px;" onclick="itemEditDone();">儲存教案</button>
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