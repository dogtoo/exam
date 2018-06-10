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
    data.rdId = $("#rdId").val();
    data.examinee = $("#examinee").val();
    
    if ($("#paneBackup").val() != null && $("#paneBackup").val() != "") {
	    var paneBackupS = $("#paneBackup").val().replace(/\'/g, '"');
	    if (paneBackupS != null) {
	        paneBackup = JSON.parse(paneBackupS);
	        paneBackup.progId.push('ScSetScore');
	        paneBackup.ScSetScore = data;
	    }
    }
    
    $("#qsName").textbox({
        readonly: true
    });
    
    $("#rdId").combogrid({
        panelWidth:301,
        editable:false,
        idField:'fRdId',
        textField:'fRdDesc',
        mode: 'remote',
        url: 'ScSetScore_qryRdList',
        type: 'POST',
        queryParams: {'userId': parent.$("#userId").val()},
        loader: function(param,success,error){
            
            if ($("#rdId").textbox('getValue') != null && $("#rdId").textbox('getValue') != "" 
             && $("#rdDesc").val() != null && $("#rdDesc").val() != "") {
                var res = [{'fRdId':$("#rdId").textbox('getText'), 'fRdDesc':$("#rdDesc").val()}];
                success(res);
                $("#rdId").textbox('readonly', true);
                $("#examineeText").textbox('readonly', true);
            }
            else {
                var opts = $(this).datagrid('options');
	            if (!opts.url) return false;
	            $.ajax({
	                url: opts.url,
	                type: opts.type,
	                data: param,
	                dataType: 'json',
	                success: function(res) {
	                    if (res.success) {
	                        success(res.rdList);
	                        var fRdId = res.rdList[0]['fRdId'];
	                    	$("#rdId").combogrid('setValue',fRdId);
	                    	$("#qsName").textbox('setValue',res.rdList[0]['fQsName']);
	                    	$("#qsId").val(res.rdList[0]['fQsId']);
	                    	$("#roomSeq").val(res.rdList[0]['fRoomSeq']);
	                    	var exam = {'rdId':fRdId, 'userId': parent.$("#userId").val()};
	                    	$("#examineeText").combogrid('grid').datagrid('reload', exam);
	                    }
	                    else {
	                    	$("#rdId").combogrid('setValue','');
                            $("#qsName").textbox('setValue','');
                            $("#qsId").val('');
                            $("#roomSeq").val('');
                            $("#examineeText").combogrid('setValue','');
	                    }
	                }
	            });
            }
        },
        onSelect: function(idx) {
        	var rec = $("#rdId").combogrid('grid').datagrid('getRows')[idx];
            $("#roomSeq").val(rec.fRoomSeq);
            var rs = $("#roomSeq").val();
            $("#qsId").val(rec.fQsId);
            var exam = {'rdId':rec.fRdId, 'userId': parent.$("#userId").val()};
            $("#examineeText").combogrid('grid').datagrid('reload', exam);
        },
        columns:[[
            {field:'fRdId',   title:'梯次', width: 100},
            {field:'fRdDesc', title:'梯次說明', width: 200},
            {field:'fQsId', hidden: true},
            {field:'fQsName', hidden: true},
            {field:'fRoomSeq', hidden: true}
        ]]
    });

    $("#examineeText").combogrid({
        panelWidth:241,
        editable:false,
        idField:'fExaminee',
        textField:'fExamineeName',
        valueField:'fExaminee',
        mode: 'remote',
        onClickRow: qryItemList,
        url: 'ScSetScore_qryExList',
        type: 'POST',
        queryParams: {'rdId':$("#rdId").textbox('getValue'), 'userId': parent.$("#userId").val()},
        loader: function(param,success,error){
        	if ($("#rdId").textbox('getValue') != null && $("#rdId").textbox('getValue') != "" 
        		&& $("#qsName").val() != null && $("#qsName").val() != ""
        	    && $("#examinee").val() != null && $("#examinee").val() != "") {
        		qryItemList();
        	}
        	else {
	        	var opts = $(this).datagrid('options');
	            if (!opts.url || !param.rdId) return false;
	            $.ajax({
	            	url: opts.url,
	                type: opts.type,
	                data: param,
	                dataType: 'json',
	                success: function(res) {
	                    if (res.success) {
	                        success(res.exList);
	                        var fRdId = res.exList[0]['fExaminee'];
	                        $("#examineeText").combogrid('setValue',fRdId);
	                        $("#sectSeq").val(res.exList[0]['fSectSeq']);
	                        
	                        $("#optId").textbox('setText',res.exList[0]['optId']);
	                        $("#score").textbox('setText',res.exList[0]['score']);
	                        $("#result").textbox('setText',res.exList[0]['result']);
	                        qryItemList();
	                    }
	                }
	            });
        	}
        },
        onSelect: function(idx) {
        	$("#sectSeq").val($("#examineeText").combogrid('grid').datagrid('getRows')[idx].fSectSeq);
        },
        columns:[[
            {field:'fSectSeq',  title:'節次', width:  40},
            {field:'fExaminee', hidden:true},
            {field:'fExamineeName', title:'考生', width: 100},
            {field:'fRoomSeq',  title:'站別', width:  40},
            {field:'fTime',     title:'時間', width:  60},
            {field:'score', hidden:true},
            {field:'result', hidden:true},
            {field:'optId', hidden:true},
            {field:'examComm', hidden:true},
            {field:'examPic', hidden:true},
        ]]
    });

    
});

/*
 * 查詢項次。
 */
function qryItemList(index,row) {
    var req = {'rdId': $("#rdId").textbox('getValue')
    		 , 'roomSeq': $("#roomSeq").val() 
    		 , 'sectSeq': $("#sectSeq").val()
    };
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
        data: req,
        dataType: 'json',
        success: function(res){
            parent.showStatus(res);
            if (res.success) {
                var optClassKey = Object.keys(res.opt)[0];
                opt = res.opt;
                $("#optId").textbox("setText", res.optId);
                $("#score").textbox("setText", res.score);
                $("#result").textbox("setText", res.result);
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
                        res.itemList[j]['optIdRadio' + res.opt[optClassKey][i].optId] = i;
                    }
                    cols[i+idx] = {field: 'optIdRadio' + res.opt[optClassKey][i].optId, title: res.opt[optClassKey][i].optDesc, width:  65,
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
    req.rdId = $('#rdId').combogrid('getValue');
    req.sectSeq = $('#sectSeq').val();
    req.roomSeq = $('#roomSeq').val();
    req.qsId = $("#qsId").val();
    
    var rows = $('#itemList').datagrid('getRows');
    var itemList = [{}];
    for (var i=0; i<rows.length; i++) {
    	var item = {};
    	item.itemNo = rows[i].itemNo;
    	item.optClass = rows[i].optClass;
    	item.optId = $('input[name=optIdRadio'+i+']:checked').val();
    	item.examComm = "";
    	item.examPic = "";
    	itemList[i] = item;
    }
    
    req.itemList = JSON.stringify(itemList);
    $.post("ScSetScore_scoreEditDone", req, function (res) {
        parent.showStatus(res);
        if (res.success) {
        	$("#optId").textbox("setText", res.optId);
            $("#score").textbox("setText", res.score);
            $("#result").textbox("setText", res.result);
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
                    <input id="rdDesc" type="hidden" value="${rdDesc}"/>
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
                    <input id="optId" class="easyui-textbox" style="width: 50px;" value="${optId}"/>
                </td>
                <td style="width: 110px; text-align: right;">
得分：
                    <input id="score" type='text' class="easyui-textbox" style="width: 50px;" data-options="readonly: true" value="${score}"/>
                </td>
                <td style="width: 170px; text-align: right;">
                    考試結果：
                    <input id="result" type='text' class="easyui-textbox" style="width: 80px;" data-options="readonly: true" value="${result}"/>
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