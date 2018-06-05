<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title></title>
<link rel="stylesheet" type="text/css" href="css/easyui.css" />
<link rel="stylesheet" type="text/css" href="css/exam.css" />
<link rel="stylesheet" type="text/css" href="css/icon.css" />
<script type="text/javascript" src="js/jquery-1.12.4.js"></script>
<script type="text/javascript" src="js/jquery.easyui.min.js"></script>
<script type="text/javascript" src="js/datagrid-dnd.js"></script>
<script type="text/javascript" src="js/datagrid-cellediting.js"></script>
<script type="text/javascript" src="js/jquery.edatagrid.js"></script>
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

$(function(){
	qryOptMList('N');
});

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
	$("#optDList").datagrid("loadData", []);
	delete window.queryCond1;
}

function qryOptDList(optClass) {
	var req = {};
	req.optClass = optClass;
	$.post("ScMntOptm_qryOptDList", req, function (res) {
		parent.showStatus(res);
		if ('optDList' in res) {
			var queryHide = $("#queryHide").val();
			$("#optDList").datagrid("loadData", res.optDList);
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
	$("#optDList").datagrid("loadData", []);
	
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

function edatagridAct(src){
	var act = src==1 ? 'saveRow' : src==2 ? 'cancelRow' : 'destroyRow' ;
	$('#optDList').edatagrid(act);
}

function optDListFmt(value, row, index) {
	return "<button type='button' style='width: 50px;' onclick='edatagridAct(1);' >確認</button> &nbsp;&nbsp;" +
    "<button type='button' style='width: 50px;' onclick='edatagridAct(2);' >取消</button> &nbsp;&nbsp;" +
    "<button type='button' style='width: 50px;' onclick='edatagridAct(3);' >刪除</button>" ;
}

function addOptItem(){
	if( $("#selectOptClass").val()=="" ){
		parent.showStatus({ status: '請先選擇評分類別！' });
		return;
	}
	
	$('#optDList').edatagrid('addRow');
}

function callModOptItems(req){
	$.post("ScMntOptm_modOptItems", req, function (res) {
		parent.showStatus(res);
		if(res.success){
			$( "#optMList input[value='"+req.optClass+"']" ).parent().parent().find("[name='dCnt']").val($("#optDList").datagrid("getData").rows.length)
		}
			
	}, "json");
}

function saveOptItem(){
	
	var req = {};
	req.optClass = $("#selectOptClass").val();
	var optDesc = $("#selectOptDesc").val();
	var fract = $("#selectFract").val();

	if( req.optClass=="" ){
		parent.showStatus({ status: '請先選擇評分類別！' });
		return;
	}
	
	alert( "optTypeStr=" + $( "#optMList input[value='"+req.optClass+"']" ).parent().parent().find("[name='dCnt']").val() );
	
	var optDList = $("#optDList").datagrid("getData").rows;
	for (var i = 0; i < optDList.length; i++) {
		if( $("#optDList").datagrid("isEditing", i)  ){
			parent.showStatus({ status: '評分選項尚在編輯狀態，請先執行確認！' });
			return;
		}

		var optD = optDList[i];
		//var optId = optD.optId.toUpperCase();
		req["optId[" + i + "]"] = optD.optId = optD.optId.toUpperCase();
		req["optDesc[" + i + "]"] = optD.optDesc;
		req["noSel[" + i + "]"] = optD.noSel;
		req["score[" + i + "]"] = optD.score;

		$("#optDList").datagrid("updateRow", { index: i, row: {optId:optD.optId, optDesc:optD.optDesc, noSel:optD.noSel, score:optD.scope} });
		
	}
	
	//$("#optDList").datagrid("updateRow", { index: 0, row: optDList[0] });
	//$("#optDList").datagrid("updateRow", { index: 1, row: optDList[1] });
	//$("#optDList").datagrid("updateRow", { index: 2, row: optDList[2] });
	
	
	if( optDList.length != fract ){
		$.messager.confirm({
			title: '儲存確認',
			msg: '評分級別(' + fract + ')與評分項目數量(' + optDList.length + ")不符，確定要儲存嗎？",
			ok: '儲存',
			cancel: '取消',
			fn: function (ok) {
				if (ok) {
					callModOptItems(req);
				}
			}
		});
	} else {
		callModOptItems(req);
	}
	
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
				<a class="easyui-linkbutton" style="width: 100px;" href="javascript:qryOptMList('N');">查詢評分類別</a>
			</td>
			<td rowspan="2">
				<a class="easyui-linkbutton" style="width: 100px; ${queryHide}" href="javascript:addOptM();">新增評分類別</a>
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

	<div style="width:646px; margin: 5px 0 5px 0; height: 25px;">
		評分選項編輯：
		<a href="javascript:addOptItem();" class="easyui-linkbutton" data-options="iconCls:'icon-add'" style="width: 80px;" ${queryHide} >加列</a>
		<a class="easyui-linkbutton" data-options="iconCls:'icon-save'" style="width: 120px;float: right;" ${queryHide} href="javascript:saveOptItem();">儲存評分項目</a>
	</div>

	<div style="width:650px;height:320px" >
		<table id="optDList" class="easyui-datagrid" title="評分選項列表"  data-options="
            singleSelect:true,
            fit:true,
            onLoadSuccess:function(){
	                //$(this).datagrid('enableCellEditing');
                	$(this).datagrid('enableDnd');
                	$(this).edatagrid({});
            	},
            destroyMsg:{
				norecord:{	// when no record is selected
					title:'警告',
					msg:'未選取項目'
				},
				confirm:{	// when select a row
					title:'刪除項目確認',
					msg:'確定要刪除這一個評分項目嗎？'
				}
				}
        	">
		    <thead>
		        <tr>
		            <th data-options="field:'optId',width:80,editor:{type:'textbox' ,options:{required:true,validType:{length:[1,3]}}}">評分代碼</th>
		            <th data-options="field:'optDesc',width:210,editor:{type:'textbox' ,options:{required:true,validType:{length:[1,50]}}}">評分說明</th>
		            <th data-options="field:'noSel',width:80,editor:{type:'combobox',options:{valueField:'id', textField:'text', editable:false,data:[{id:'是',text:'是'}, {id:'',text:'否'}]}}">不可選取</th>
		            <th data-options="field:'score',width:80,align:'right',editor:{type:'numberbox',options:{required:true, precision:0}}">答案分數</th>
		            <th data-options="field:'action',width:180,align:'center', formatter: optDListFmt">處理動作</th>
		        </tr>
		    </thead>
		</table>
	</div>		
		
	<div style="margin: 5px 0 5px 0;">編輯說明：</div>
	<div style="margin: 5px 0 5px 0;">1.雙擊評分項目啟動編輯模式</div>
	<div style="margin: 5px 0 5px 0;">2.拉動評分項目可調整顯示順序</div>
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