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
</style>
<script type="text/javascript">

/*
 * 查詢樣板。
 */
function qryTmplList(mode) {
	var req = {};
	if (mode == "N"){
		req.mode = mode;
		$("#pg").pagination({ total: 1, pageNumber: 1 });
		var queryCond1 = {};
		queryCond1._job = 'QsMntTmpl';
		queryCond1.qryDepart = $("#qryDepart").combobox("getValue");
		window.queryCond1 = queryCond1;
	}
	else{
		req.mode = "";
		if (!window.queryCond1 || window.queryCond1._job != 'QsMntTmpl')
			return;
	}
	for (var i in window.queryCond1)
		if (i != "_job")
			req[i] = window.queryCond1[i];
	req.pageRow = $("#pg").pagination("options").pageSize;
	req.pageAt = $("#pg").pagination("options").pageNumber;
	$.post("QsMntTmpl_qryTmplList", req, function (res){
		if (mode != "S" && mode != "T")
			parent.showStatus(res);
		if (res.success){
			if ('total' in res)
				$("#pg").pagination({ total: res.total });
			var jtmplList = $("#tmplList");
			var widthSum = 15;
			var widthList = jtmplList.parent().prev().find("table.listHead td").map(function () { widthSum += $(this).width();	return $(this).width(); });
			jtmplList.parent().css("width", widthSum.toString() + "px");
			jtmplList.find("tr").remove();
			for (var i = 0; i < res.tmplList.length; i++){
				var tmpl = res.tmplList[i];
				var wat = 0;
				var append = "<tr class='" + (i & 1 ? "odd" : "even") + "'>\n" +
				"<td>\n" +
				"    <input name='tmplId' type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + tmpl.tmplId + "' />\n" +
				"</td>\n" +
				"<td>\n" +
				"    <input name='tmplDepart' type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + tmpl.tmplDepart + "' />\n" +
				"</td>\n" +
				"<td>\n" +
				"    <input name='showOrder' type='text' style='width: " + widthList[wat++] + "px; text-align: center;' readonly value='" + tmpl.showOrder + "' />\n" +
				"</td>\n" +
				"<td>\n" +
				"    <input name='tmplName' type='text' style='width: " + widthList[wat++] + "px;' readonly value='" + tmpl.tmplName + "' />\n" +
				"</td>\n" +
				"<td>\n" +
				"    <span style='width: " + widthList[wat++] + "px; display: inline-block; text-align: center;'>\n" +
				"        <button type='button' style='width: 70px;' ${queryHide} onclick='modTmpl(this);'>編輯</button>\n" +
				"        &nbsp;&nbsp;<button type='button' style='width: 70px;' ${queryHide} onclick='modTmplFile(this);'>檔案</button>\n" +
				"        &nbsp;&nbsp;<button type='button' style='width: 70px;' ${queryHide} onclick='delTmpl(this);'>刪除</button>\n" +
				"    </span>\n" +
				"</td>\n" +
				"</tr>\n";
			jtmplList.append(append);
			var jrow = jtmplList.find("tr:last");
			}
		}
	}, "json");
}

/*
 * 新增樣板。
 */
function addTmpl() {
	$("#tmplList tr.select").removeClass("select");
	$("#tmplEdit").dialog("open");
	$("#tmplEditMode").val("A");
	$("#editTmplIdOrgPane").css("visibility", "hidden");
	$("#editTmplId").textbox("setValue", "");
	$("#editTmplName").textbox("setValue", "");
	$("#editTmplDepart").combobox("setValue", "");
	$("#editTmplOrder").textbox("setValue", "");
	$("#editTmplRemark").textbox("setValue", "");
	$("#editFileRemark").textbox("setValue", "");
	$("#fileList").datagrid("loadData", []);
}

/*
 * 點擊樣版編輯button。
 */
function modTmpl(src){
	var jrow = $(src).parent().parent().parent();
	var req = {};
	req.tmplId = jrow.find("[name='tmplId']").val();
	req.qryType = "T";
	$.post("QsMntTmpl_qryTmpl", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#tmplList tr.select").removeClass("select");
			jrow.addClass("select");
			$("#tmplEdit").dialog("open");
			$("#tmplEditMode").val("M");
			$("#editTmplIdOrgPane").css("visibility", "visible");
			$("#editTmplIdOrg").textbox("setValue", res.tmplId);
			$("#editTmplId").textbox("setValue", res.tmplId);
			$("#editTmplName").textbox("setValue", res.tmplName);
			$("#editTmplDepart").combobox("setValue", res.tmplDepart);
			$("#editTmplOrder").textbox("setValue", res.showOrder);
			$("#editTmplRemark").textbox("setValue", res.remark);
			$("#fileList").datagrid("loadData", res.fileList);
			$("#editFileRemark").textbox("setValue", "");
		}
	}, "json");
}

/*
 * 儲存樣板button(A:新增; M:修改)。
 */
function editTmplDone(){
	var req = {};
	req.tmplId = $("#editTmplId").textbox("getValue");
	req.tmplName = $("#editTmplName").textbox("getValue");
	req.tmplDepart = $("#editTmplDepart").combobox("getValue");
	req.showOrder = $("#editTmplOrder").textbox("getValue");
	req.tmplRemark = $("#editTmplRemark").textbox("getValue");
	if ($("#tmplEditMode").val() == "A") {
		$.post("QsMntTmpl_addTmpl", req, function (res) {
			parent.showStatus(res);
			if (res.success) {
				$("#tmplEdit").dialog("close");
				qryTmplList("T");
			}
		}, "json");
	}
	else if ($("#tmplEditMode").val() == "M") {
		req.tmplIdOrg = $("#editTmplIdOrg").textbox("getValue");
		$.post("QsMntTmpl_modTmpl", req, function (res) {
			parent.showStatus(res);
			if (res.success) {
				$("#tmplEdit").dialog("close");
				$("#editTmplIdOrgPane").css("visibility", "hidden");
				$("#fileList tr.select").removeClass("select");
				qryTmplList("S");
			}
		}, "json");
	}
}

function tmplClose() {
	$("#tmplList tr.select").removeClass("select");
}

/*
 * 點擊樣版檔案button。
 */
function modTmplFile(src){
	var jrow = $(src).parent().parent().parent();
	var req = {};
	req.tmplId = jrow.find("[name='tmplId']").val();
	req.qryType = "F";
	$.post("QsMntTmpl_qryTmpl", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#tmplList tr.select").removeClass("select");
			jrow.addClass("select");
			$("#fileTmplId").val(req.tmplId);
			$("#tmplFile").dialog("open");
			$("#fileList").datagrid("loadData", res.fileList);
			$("#fileUpload").filebox("clear");
			$("#editFileClass").combobox("clear");
			$("#editFileOrder").textbox("setValue", "");
			$("#editFileRemark").textbox("setValue", "");
			$("#uploadBut").prop("disabled", true);
			$("#modFileBut").prop("disabled", true);
		}
	}, "json");
}

function tmplFileClose() {
	$("#tmplList tr.select").removeClass("select");
}

/*
 * 刪除樣板。
 */
function delTmpl(src) {
	var jrow = $(src).parent().parent().parent();
	var tmplId = jrow.find("[name='tmplId']").val();
	$("#tmplList tr.select").removeClass("select");
	jrow.addClass("select");
	$("#editTmplIdOrgPane").css("visibility", "hidden");
	var req = {};
	req.tmplId = tmplId;
	$.messager.confirm({
		title: '確認',
		msg: '是否要刪除此樣板？',
		ok: '刪除',
		cancel: '取消',
		fn: function (ok) {
			$("#tmplList tr.select").removeClass("select");
			if (ok) {
				$.post("QsMntTmpl_delTmpl", req, function (res) {
					parent.showStatus(res);
					if (res.success) {
						qryTmplList("S");
					}
				}, "json");
			}
		}
	});
}

/*
 * 每一個檔案增加下載button。
 */
function fileListActFmt(value, row, index) {
	return "<button type='button' style='width: 60px;' onclick='fileListSel(this);'>選擇</button>\n" +
		"&nbsp;&nbsp;<button type='button' style='width: 60px;' onclick='fileListOpen(this);'>開啟</button>\n" +
		"&nbsp;&nbsp;<button type='button' style='width: 60px;' onclick='fileListDownload(this);'>下載</button>\n" +
		"&nbsp;&nbsp;<button type='button' style='width: 60px;' onclick='fileListDel(this);'>刪除</button>\n" +
		"<input type='hidden' value='" + row.fileName + "'/>\n";
}

/*
 * 查詢檔案
 */
function fileListSel(src) {
	var req = {};
	req.tmplId = $("#fileTmplId").val();
	req.fileName = $(src).next().next().next().next().val();
	$.post("QsMntTmpl_qryFile", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#fileServerName").val(req.fileName);
			$("#fileUpload").filebox("textbox").val(res.fileDesc);
			$("#editFileClass").combobox("setValue", res.fileClass);
			$("#editFileOrder").textbox("setValue", res.showOrder);
			$("#editFileRemark").textbox("setValue", res.remark);			
			$("#uploadBut").prop("disabled", true);
			$("#modFileBut").prop("disabled", false);
		}
	}, "json");
}
	
/*
 * 檔案開啟
 */
function fileListOpen(src) {
	var url = "QsMntTmpl_download";
	url += "?tmplId=" + encodeURIComponent($("#fileTmplId").val());
	url += "&fileName=" + encodeURIComponent($(src).next().next().next().val());
	url += "&open=Y";
	window.open(url);
}

/*
 * 檔案下載
 */
function fileListDownload(src) {
	var url = "QsMntTmpl_download";
	url += "?tmplId=" + encodeURIComponent($("#fileTmplId").val());
	url += "&fileName=" + encodeURIComponent($(src).next().next().val());
	url += "&open=N";
	window.open(url);
}

/*
 * 刪除檔案
 */
function fileListDel(src) {
	var req = {};
	req.tmplId = $("#fileTmplId").val();
	req.fileName = $(src).next().val();
	$.messager.confirm({
		title: '確認',
		msg: '是否要刪除此檔案？',
		ok: '刪除',
		cancel: '取消',
		fn: function (ok) {
			if (ok) {
				$.post("QsMntTmpl_delFile", req, function (res) {
					parent.showStatus(res);
					if (res.success) {
						$("#fileList").datagrid("loadData", res.fileList);
					}
				}, "json");
			}
		}
	});
}


/*
 * 上傳檔案。
 */
function setUploadForm() {
	$("#upload").on("submit", function () {
		$("#fileClass").val($("#editFileClass").combobox("getValue"));
		$("#fileShowOrder").val($("#editFileOrder").textbox("getValue"));
		$("#fileRemark").val($("#editFileRemark").textbox("getValue"));
	});
	$("#upload").form({
		ajax: true, iframe: false,
		onProgress: function (pct) {
			$("#fileProgress").progressbar("setValue", pct);
		},
		success: function (res) {
			res = JSON.parse(res);
			parent.showStatus(res);
			if (res.success) {
				$("#fileList").datagrid("loadData", res.fileList);
				$("#fileUpload").filebox("clear");
				$("#fileProgress").progressbar("setValue", 0);
				$("#editFileClass").combobox("clear");
				$("#editFileOrder").textbox("setValue", "");
				$("#editFileRemark").textbox("setValue", "");
				$("#uploadBut").prop("disabled", true);
			}
		}
	});
}

function fileSelect() {
	$("#editFileClass").combobox("clear");
	$("#editFileOrder").textbox("setValue", "");
	$("#editFileRemark").textbox("setValue", "");
	$("#uploadBut").prop("disabled", false);
	$("#modFileBut").prop("disabled", true);
} 

/*
 * 修改檔案。
 */
function modFile() {
	var req = {};
	req.tmplId = $("#fileTmplId").val();
	req.fileName = $("#fileServerName").val();
	req.fileClass = $("#editFileClass").combobox("getValue");
	req.showOrder = $("#editFileOrder").textbox("getValue");
	req.remark = $("#editFileRemark").textbox("getValue");
	$.post("QsMntTmpl_modFile", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#editFileClass").combobox("clear");
			$("#editFileOrder").textbox("setValue", "");
			$("#editFileRemark").textbox("setValue", "");
			$("#fileList").datagrid("loadData", res.fileList);
			$("#modFileBut").prop("disabled", true);
		}
	}, "json");
}

</script>
</head>
<body>
<div style="margin: 10px 0 0 300px;">
	<div style="float: none; height: 35px;">
		<table>
			<tr style="height: 35px;">
				<td nowrap style="text-align: right;">科別:&nbsp;&nbsp;</td>
				<td>
					<select id="qryDepart" class="easyui-combobox" data-options="editable: false, panelHeight: 'auto'">
						<option value="">&nbsp;</option>
						${departList}
					</select>
				</td>
				<td style="width: 270px;">
					<button type="button" style="width: 100px;" onclick="qryTmplList('N');">查詢樣板</button>
				</td>
				<td>
					<button type="button" style="width: 100px;" onclick="addTmpl();" ${queryHide}>新增樣板</button>
				</td>
			</tr>
		</table>	
	</div>
	<div style="clear: both;">
		<div style="float: left;">
			<div id="pg" class="easyui-pagination" style="width: 380px;" data-options="onSelectPage: qryTmplList, showRefresh: false, total: 0" ></div>
		</div>	
	</div>
	<div style="clear: both;">
		<table class="listHead">
			<tr>
				<td style="width: 100px;" class="headCol1">代碼</td>
				<td style="width: 100px;" class="headCol2">科別</td>
				<td style="width: 60px;" class="headCol3">順序</td>
				<td style="width: 160px;" class="headCol4">樣板名稱</td>
				<td style="width: 240px;" class="headCol5">處理動作</td>
			</tr>
		</table>
	</div>
	<div style="clear: both; height: 420px; overflow: auto; ">
		<table id="tmplList" class="listData">
		</table>
	</div>
</div>
<div id="tmplEdit" class="easyui-dialog" style="width: 280px; height: 340px; display: none;" data-options="title: '編輯樣版', modal: true, closed: true, onClose: tmplClose">
	<input id="tmplEditMode" type="hidden" />
	<div style="float: left; height: 280px; margin: 10px 0 0 10px;">
		<table>
			<tr id="editTmplIdOrgPane">
				<td>原樣板代碼</td>
				<td>
					<input id="editTmplIdOrg" class="easyui-textbox" style="width: 150px;" data-options="readonly: true" />
				</td>
			</tr>
			<tr>
				<td>樣板代碼</td>
				<td>
					<input id="editTmplId" class="easyui-textbox" style="width: 150px;" />
				</td>
			</tr>
			<tr>
				<td>樣板名稱</td>
				<td>
					<input id="editTmplName" class="easyui-textbox" style="width: 150px;" />
				</td>
			</tr>
			<tr>
				<td>科別</td>
				<td>
					<select id="editTmplDepart" class="easyui-combobox" data-options="editable: false, panelHeight: 'auto'">
						${departList}
					</select>
				</td>
			</tr>
			<tr>
				<td>順序</td>
				<td>
					<input id="editTmplOrder" class="easyui-textbox" style="width: 150px;" />
				</td>
			</tr>
			<tr>
				<td>備註</td>
				<td>
					<input id="editTmplRemark" class="easyui-textbox" style="width: 150px; height: 100px;" data-options="multiline: true"/>
				</td>
			</tr>
			<tr>
				<td colspan="2" style="text-align: center;">
					<button type="button" style="width: 100px;" onclick="editTmplDone();">儲存樣板</button>
				</td>
			</tr>
		</table>
	</div>
</div>
<div id="tmplFile" class="easyui-dialog" style="width: 1000px; height: 360px; display: none;" data-options="title: '編輯樣版', modal: true, closed: true, onClose: tmplFileClose">
	<div style="float: left; margin: 10px 0 0 10px;">
		<div id="fileList" class="easyui-datagrid" style="width: 760px; height: 270px;" data-options="
			title: '樣板檔案清單', singleSelect: true, columns: [[
				{ field: 'fileDesc', title: '檔名', align: 'left', width: 120 },				
				{ field: 'fileClass', title: '類別', halign: 'center', width: 100 },
				{ field: 'showOrder', title: '順序', align: 'center', width: 60 },
				{ field: 'fileSize', title: '大小', align: 'right', halign: 'center', width: 80 },
				{ field: 'fileType', title: '型態', align: 'center', width: 100 },
				{ field: 'action', title: '處理動作', align: 'center', width: 280, formatter: fileListActFmt },
				{ field: 'fileName', hidden: true },
			]]
		">
		</div>
	</div>
	<div style="float: left; margin: 10px 0 0 10px;">
		<input id="fileServerName" name="fileServerName" type="hidden" />
		<form id="upload" method="post" action="QsMntTmpl_uploadFile" enctype="multipart/form-data">
			<input id="fileTmplId" name="tmplId" type="hidden" />
			<input id="fileClass" name="fileClass" type="hidden" />					
			<input id="fileShowOrder" name="showOrder" type="hidden" />					
			<input id="fileRemark" name="remark" type="hidden" />					
			<div style="margin: 10px 0 0 0;">
				<input id="fileUpload" name="file" class="easyui-filebox" style="width: 200px;" data-options="
						prompt: '請選擇上傳檔案', buttonText: '選擇檔案', onClickButton: fileSelect"/>
			</div>
			<div style="margin: 10px 0 0 0;">
				<div id="fileProgress" class="easyui-progressbar" style="width: 200px;" ></div>
			</div>
			<div style="margin: 10px 0 0 0;">
				類別:
				<select id="editFileClass" class="easyui-combobox" style="width: 160px;" data-options="panelHeight: 'auto', editable: false">
					${fileClassList}
				</select>
			</div>
			<div style="margin: 10px 0 0 0;">
				順序:
				<input id="editFileOrder" class="easyui-textbox" style="width: 160px;" />
			</div>
			<div style="margin: 10px 0 0 0;">
				備註:
				<input id="editFileRemark" class="easyui-textbox" style="width: 160px; height: 100px;" data-options="multiline: true"/>
			</div>
			<div style="margin: 10px 0 0 0;">
				<button id="uploadBut" type="submit" style="width: 100px;">上傳檔案</button>
				<button id="modFileBut" type="button" style="width: 100px;" onclick="modFile();">修改檔案</button>
			</div>
		</form>
	</div>
</div>
<input type="hidden" id="progId" value="${progId}" />
<input type="hidden" id="privDesc" value="${privDesc}" />
<input type="hidden" id="progTitle" value="${progTitle}" />
<input type="hidden" id="status" value="${status}" />
<input type="hidden" id="statusTime" value="${statusTime}" />
</body>
<script type="text/javascript">
parent.showProg({ id: $("#progId").val(), priv: $("#privDesc").val(), title: $("#progTitle").val() });
parent.showStatus({ status: $("#status").val(), statusTime: $("#statusTime").val() });
setUploadForm();
</script>
</html>