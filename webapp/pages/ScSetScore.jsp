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
var paneBackup = {};
var opt = {};
$(function(){
	//把自已的作業名稱加上去讓下一個畫面知道返回是要回給誰，返回後所要帶的預設值
    var data = {};
    data.rdId = $("#rdIdH").val();
    data.examinee = $("#examinee").val();
	
	var paneBackupS = $("#paneBackup").val().replace(/\'/g, '"');
	if (paneBackupS != null) {
	    paneBackup = JSON.parse(paneBackupS);
	    paneBackup.progId.push('ScSetScore');
	    paneBackup.ScSetScore = data;
	}
	
	$("#qsName").textbox({
	    readonly: true
	});
	
    $("#rdId").combogrid({
    	panelWidth:301,
    	idField:'fRdId',
    	textField:'fRdName',
    	mode: 'remote',
        loader: function(param,success,error){
            data = {'rdId': param.q, 'userId': parent.$("#userId").val()};
            $.ajax({
                url: 'ScSetScore_qryExList',
                type: 'POST',
                data: data,
                dataType: 'json',
                success: function(res) {
                    if (res.success)
                        success(res.exList);
                    else
                        return false;
                    $("#examiner").val('');
                }
            });
        },
        columns:[[
            {field:'fRdId',   title:'梯次', width: 100},
            {field:'fRdDesc', title:'梯次說明', width: 200},
        ]]
    });
    
    $("#examineeText").combogrid({
        panelWidth:241,
        idField:'fExaminee',
        textField:'fExaminee',
        onClickRow: qryItemList,
        columns:[[
            {field:'fSectSeq',  title:'節次', width:  40},
            {field:'fExaminee', title:'考生', width: 100},
            {field:'fRoomSeq',  title:'站別', width:  40},
            {field:'fTime',     title:'時間', width:  60},
        ]]
	});
    
    qryItemList();
});
/*
 * 查詢梯次。
 */
function qryRdList() {
	var req = {};
	req.userId = parent.$("#userId").val();
	req.rdId = $("#rdId").val();
	req.examinee
	$.post("ScSetScore_qryRdList", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			if ( $("#bProgId").val().length != 0 ) {
				$("#backBtn" ).show();
				$("#rdId"    ).combogrid("setValue", $("#bRdId"    ).val());
				$("#examinee").combogrid("setValue", $("#bExaminee").val());
				$("#rdId"    ).combogrid("disable",  true);
				$("#examinee").combogrid("disable",  true);
				$("#optId"   ).combogrid("disable",  true);
				qryExList();
			} else {
				$("#backBtn").hide();
				$("#rdId").combogrid("grid").datagrid("loadData", res.rdList);
			}
		}
	}, "json");
	
	// 評分查詢 -> 梯次表 -> 評分表
	/*if ( $("#bProgId").val().length != 0 ) {
		$("#backBtn" ).show();
		$("#rdId"    ).combogrid("setValue", $("#bRdId"    ).val());
		$("#examinee").combogrid("setValue", $("#bExaminee").val());
		$("#rdId"    ).combogrid("disable",  true);
		$("#examinee").combogrid("disable",  true);
		$("#optId"   ).combogrid("disable",  true);
		qryExList();
	} else {
		$("#backBtn").hide();
		var req = {};
		req.userId = parent.$("#userId").val();
		$.post("ScSetScore_qryRdList", req, function (res) {
			parent.showStatus(res);
			if (res.success) {
				$("#rdId").combogrid("grid").datagrid("loadData", res.rdList);
			}
		}, "json");
	}*/
}

/*
 * 查詢教案名稱、考生。
 */
function qryExList(index,row) {
	var req = {};
	// 評分查詢 -> 梯次表 -> 評分表
	if ( $("#bProgId").val().length != 0 ) {
		req.bProgId = $("#bProgId" ).val();
		req.rdId    = $("#bRdId"   ).val();
		req.roomSeq = $("#bRoomSeq").val();
	} else {
		req.rdId    = row.fRdId;
		req.userId  = parent.$("#userId").val();
	}
	$.post("ScSetScore_qryExList", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
			$("#qsName"  ).textbox(  "setValue", res.qsName);
			
			// 評分查詢 -> 梯次表 -> 評分表
			if ( $("#bProgId").val().length != 0 )
				qryItemList();
			else
				$("#examinee").combogrid("grid").datagrid("loadData", res.exList);
		}
	}, "json");
}

/*
 * 查詢項次。
 */
function qryItemList(index,row) {
	var req = {};
	// 評分查詢 -> 梯次表 -> 評分表
	/*
	if ( $("#bProgId").val().length != 0 ) {
		req.rdId    = $("#bRdId").val();
		req.roomSeq = $("#bRoomSeq").val();
		req.sectSeq = $("#bSectSeq").val();
	} else {
		req.rdId    = $('#rdId').combogrid('getValue');
		req.roomSeq = row.fRoomSeq;
		req.sectSeq = row.fSectSeq;
	}*/
	
	$.ajax({
        type: 'POST',
        url: 'ScSetScore_qryScore',
        data: {'rdId': $("#rdIdH").val(), 'roomSeq': $("#roomSeq").val() ,'sectSeq': $("#sectSeq").val()},
        dataType: 'json',
        success: function(res){
            parent.showStatus(res);
            if (res.success) {
                var optClassKey = Object.keys(res.opt)[0];
                opt = res.opt;
                var cols = [];
            	var idx  = 0;
            	cols[idx] = {field: 'itemNo',   title: '編號',     width:  80}; idx++;
            	cols[idx] = {field: 'itemDesc', title: '項目說明',     width: 690}; idx++;
            	cols[idx] = {field: 'tip',   title: '評分說明', width:  65,
            			    	formatter:function(value, row, index){
            			    		return '<button onclick="clickTip(this)">檢視</button>'
            			    			 + "<input type='hidden' name='tip' value='" + row.tip + "' />" ;
            			    	}}; idx++;
                cols[idx] = {field: 'optClass', hidden: true}; idx++;
                for (var i=0; i < res.opt[optClassKey].length ; i++) {
                    for (var j=0; j<res.itemList.length; j++) {
                    	res.itemList[j]['optClass' + res.opt[optClassKey][i].optId] = i;
                    }
                	cols[i+idx] = {field: 'optClass' + res.opt[optClassKey][i].optId, title: res.opt[optClassKey][i].optDesc, width:  65,
                	        formatter:function(value, row, index){
                	            var r = '';
                	            if (!opt[row.optClass][value].noSel) {
                	                if (opt[row.optClass][value].optId == row.optId)
                	                	r = '<input type="radio" name="optIdRadio'+index+'" value="'+opt[row.optClass][value].optId+'" checked>';
                	                else
                	                    r = '<input type="radio" name="optIdRadio'+index+'" value="'+opt[row.optClass][value].optId+'">';
                	            }
        			    		return r ;
        			    	}};
                }
                idx = idx + res.opt[optClassKey].length;
                
                cols[idx] = {field: 'optId',   hidden: true}; idx++;
                cols[idx] = {field: 'comm',     title: '註記',     width:  65,
            			    	formatter:function(value, row, index){
            			    		return '<button onclick="editComm()">編輯</button>';
            			    	}}; idx++;
                cols[idx] = {field: 'pic',      title: '考官塗鴉', width:  65,
            			    	formatter:function(value, row, index){
            			    		return '<button onclick="editPic()">編輯</button>';
            			    	}}; idx++;
                
                $("#itemList").datagrid({
                    width:  '100%',
                    height: '100%',
                    singleSelect:true,
                    idField:'fItemNo',
                    columns: [cols],
                    data: res.itemList
                });
                
            }
        }
     
    });
	
}

function editComm() {
	$("#edComm").dialog("open");
}

function editPic() {
	$("#edPic").dialog("open");
}

function clickTip(src) {
	var tip = $(src).next().val();
	document.getElementById('tipContent').innerText = tip;
	$("#tip").dialog("open");
}

function scoreEditDone() {
	var req = {};
	// 評分查詢 -> 梯次表 -> 評分表
	if ( $("#bProgId").val().length != 0 ) {
		req.rdId    = $("#bRdId").val();
		req.roomSeq = $("#bRoomSeq").val();
		req.sectSeq = $("#bSectSeq").val();
	} else {
		req.rdId    = $('#rdId').combogrid('getValue');
		req.roomSeq = row.fRoomSeq;
		req.sectSeq = row.fSectSeq;
	}
	
	//註記
	//考官塗鴉
	req.optId = $('#optId').combogrid('getValue');
	//項目
	
	$.post("ScSetScore_scoreEditDone", req, function (res) {
		parent.showStatus(res);
		if (res.success) {
		}
	}, "json");
}
function backProgId(bProgId) {
	//要回到上一個畫面時要把 paneBackup.progId.pop()把最後一個刪除，也就是把自已刪掉
	//刪掉後再把paneBackup['progId'] 裡面的參數返迴為預設查詢
	paneBackup.progId.pop(); //[0]ScQry [1]ScMnt [2]ScSet(會被刪掉)
	var url = bProgId + "?1=1"; //準備回到 ScMnt
	$.each(paneBackup[bProgId], function(f,v) {
		url += "&" + f + "=" + v;
	});//回到 ScMnt要執行的參數
	delete paneBackup[bProgId];//參數放好後也把放在p b up的刪掉
	$.each(paneBackup.progId, function(f,v) { //要幫ScMnt打包 [0]ScQry
	    if (v != bProgId) {
		    url += "&p_b_progId["+ f +"]=" + v;
		    
		    var i = 0;
			$.each(paneBackup[v], function(ff,vv){
			    url += "&p_b_" + v + "["+ (i++) +"]=" + ff + ":" + vv;
			});
	    }
	})

	parent.selProg(url);
}
</script>
</head>
<body>
<div style="margin: 5px 5px 0 10px;">
	<div style="margin: 5px 0 5px 0; height: 25px;">
		<table class="listHead">
			<tr>
				<td>
					梯次：
					<input id="rdId" class="easyui-combogrid" style="width:105px;" value="${rdId}"/>
					<input id="rdIdH" type="hidden" value="${rdId}"/>
				</td>
				<td style="width: 180px; text-align: right;">
					教案：
				    <input id="qsName" type='text' class="easyui-textbox" style="width: 100px;" value="${qsName}"/>
				    <input id="qsId" type="hidden" value="${qsId}"/>
				    <input id="roomSeq" type="hidden" value="${roomSeq}"/>
				</td>
				<td style="width: 180px; text-align: right;">
					考生：
					<input id="examineeText" class="easyui-combogrid" style="width:120px;" value="${examineeText}"/>
				    <input type="hidden" id="examinee" value="${examinee}"/>
				    <input id="sectSeq" type="hidden" value="${sectSeq}"/>
				</td>
				<td style="width: 60px; text-align: right;">
					<button id="editModBut" type="button" style="width: 50px;" onclick="editComm();">註記</button>
				</td>
				<td style="width: 90px; text-align: right;">
					<button id="editModBut" type="button" style="width: 80px;" onclick="editPic();">考官塗鴉</button>
				</td>
				<!-- <td style="width: 300px;"></td> -->
				<td style="width: 140px; text-align: right;">
					整體級數：
					<input id="optId" type='text' class="easyui-combobox" style="width: 50px;" data-options="editable: false, panelHeight: 'auto'"/>
				</td>
				<td style="width: 110px; text-align: right;">
					得分：
					<input id="score" type='text' class="easyui-textbox" style="width: 50px;" data-options="readonly: true"/>
				</td>
				<td style="width: 170px; text-align: right;">
					考試結果：
					<input id="result" type='text' class="easyui-combobox" style="width: 80px;" data-options="readonly: true"/>
				</td>
				<td style="width: 100px; text-align: right;">
					<button id="editModBut" type="button" style="width: 80px;" onclick="scoreEditDone();">儲存評分</button>
				</td>
				<td>
					<button type="button" id="backBtn" style="width: 80px;" onclick="backProgId('${bProgId}')">返回</button>
				</td>
			</tr>
		</table>
	</div>
	<div style="width: 1300px; height: 400px; overflow: auto; ">
		<table id="itemList" class="listData">
		</table>
	</div>
</div>

<div id="edComm" class="easyui-dialog" style="width: 500px; height: 300px; display: none;"
	data-options="modal: true, title: '編輯註記', closed: true">
	<div style="float: left; margin: 10px 0 0 10px;">
		<table>
			<tr></tr>
		</table>
	</div>
</div>
<div id="edPic" class="easyui-dialog" style="width: 900px; height: 400px; display: none;"
	data-options="modal: true, title: '編輯考官塗鴉', closed: true">
</div>
<div id="tip" class="easyui-dialog" style="width: 890px; height: 300px; display: none;"
	data-options="modal: true, title: '評分說明', closed: true">
	<div style="float: left; margin: 10px 0 0 10px;">
		<table>
			<tr>
				<td>
					<!-- 
					<input id="tipContent" class="easyui-textbox"  style="width: 848px; height: 240px" data-options="editable: false" />
					 -->
					 <label id="tipContent"></label>
				</td>
			</tr>
		</table>
	</div>
</div>

<input type="hidden" id="progId"     value="${progId}" />
<input type="hidden" id="privDesc"   value="${privDesc}" />
<input type="hidden" id="progTitle"  value="${progTitle}" />
<input type="hidden" id="queryHide"  value="${queryHide}" />
<input type="hidden" id="status"     value="${status}" />
<input type="hidden" id="statusTime" value="${statusTime}" />
<!-- 應該不是這樣做 
<input type="hidden" id="bProgId"    value="${bProgId}" />
<input type="hidden" id="bRdId"      value="${bRdId}" />
<input type="hidden" id="bSectSeq"   value="${bSectSeq}" />
<input type="hidden" id="bRoomSeq"   value="${bRoomSeq}" />
<input type="hidden" id="bExaminee"  value="${bExaminee}" />
 -->
<input type="hidden" id="paneBackup" value="${paneBackup}" />
</body>
<script type="text/javascript">
parent.showProg({ id: $("#progId").val(), priv: $("#privDesc").val(), title: $("#progTitle").val() });
parent.showStatus({ status: $("#status").val(), statusTime: $("#statusTime").val() });

//qryRdList();
</script>
</html>