<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>場次維護</title>
<link rel="stylesheet" type="text/css" href="css/easyui.css" />
<link rel="stylesheet" type="text/css" href="css/exam.css" />
<script type="text/javascript" src="js/jquery-1.12.4.js"></script>
<script type="text/javascript" src="js/jquery.easyui.min.js"></script>
<script type="text/javascript" src="js/datagrid-dnd.js"></script>
<script type="text/javascript" src="js/easyui-lang-zh_TW.js"></script>
<style>
#qryUserId + .textbox .textbox-text {
    text-transform: uppercase;
}
#editUserId + .textbox .textbox-text {
    text-transform: uppercase;
}
</style>
<script type="text/javascript">
var paneBackup = {};
$(function(){
    $("#rddmList").datagrid({
        width:'98%',
        height:'98%',
        singleSelect:true,
        idField:'rdId',
        columns:[[
            {field:'id',hidden:true},
            {field:'sectSeq',title:'節次/考場',width:100,height:100}
        ]]
    });
    
    //如果paneBackup有值，要把 paneBackup.progId.push('ScMntSect') 
    //                    paneBackup.ScMntSect = {rdId : ?, showType: ?}
    //把自已的作業名稱加上去讓下一個畫面知道返回是要回給誰，返回後所要帶的預設值
    var paneBackupS = $("#paneBackup").val().replace(/\'/g, '"');
    paneBackup = JSON.parse(paneBackupS);
    paneBackup.progId.push('ScMntSect');
    
    var data = {};
    data.rdId = $("#rdId").textbox('getText');
    data.showType = $("#showType").combobox('getValue');
    
    paneBackup.ScMntSect = data;
    $("#rddmCr").hide();
    $("#rddmEdit").hide();
    $("#rddmCr").css("visibility", "");
    $("#rddmEdit").css("visibility", "");
    /*
    var td = $("#rdmmList").datagrid('getPanel').find('div.datagrid-header td[field="rdId"]');
    td.addClass('headCol1');
    */
});

function backProgId(bProgId) {
	//要回到上一個畫面時要把 paneBackup.progId.pop()把最後一個刪除，也就是把自已刪掉
	//刪掉後再把paneBackup['progId'] 裡面的參數返迴為預設查詢
	paneBackup.progId.pop(); //[0]ScQry [1]ScMnt [2]ScSet(會被刪被)
	var url = bProgId + "?1=1"; //準備回到 ScMnt
	$.each(paneBackup[bProgId], function(f,v) {
		url += "&" + f + "=" + v;
	});//回到 ScMnt要執行的參數
	
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

function qryRddList() {
    var data = {};
    data.rdId = $("#rdId").textbox('getText');
    data.showType = $("#showType").combobox('getValue');
    $.ajax({
        url: 'ScRunDown_qryRddm',
        type: 'POST',
        data: data,
        dataType: 'json',
        success: function(res) {
            parent.showStatus(res);
            var rdId = $("#rdId").textbox('getText');
            if (res.success) {
                var cols = [{}];
                cols[0] = {field:'sectSeq',title:'節次/考場',width:100,height:100};
                var colLength = res.rddmList.length;
                for (var i=1; i<=colLength; i++) {
                	cols[i] = {field:'roomSeq'+i,title:'第' + i + '站',width:100,height:100,
                			formatter:function(value,row,index){
                				var s;
                				var roomSeqNo = Object.keys(row).filter(function(key) {return row[key] === value})[0].match(/\d+/);
                				if (paneBackup.progId[paneBackup.progId.length-1] == 'ScQryScore') {
                					s = '<button class="easyui-linkbutton" onclick="parent.selProg(\'ScQryScore?rdId=' + rdId 
                					                                                                        + "&sectSeq=" + row.sectSeq
                					                                                                        + "&roomSeq=" + roomSeqNo[0];
                					$.each(paneBackup.progId, function(f,v) {
                					    s += "&p_b_progId["+ f +"]=" + v;
                					    
                					    var i = 0;
                    					$.each(paneBackup[v], function(ff,vv){
                    					    s += "&p_b_" + v + "["+ (i++) +"]=" + ff + ":" + vv;
                    					});
                					})
                					
                							                                                         
                					s += '\');"> ' + value + ' </button>'
                				}
                			    else
                			    	s = value;
                                return s;                    
                            }};
                	
                }

                $("#rddmList").datagrid({
            		columns:[cols]
            	});
                
                $("#rddmList").datagrid('loadData',res.rddmList);
                if (res.rddmEditHide == 'Y'){
                    $("#rddmCr").hide();
	                $("#rddmEdit").show();
                }
                else if (res.rddmCrHide) {
                    $("#rddmCr").show();
	                $("#rddmEdit").hide();
                }
            }
            else {
                $("#rddmList").datagrid('loadData',[]);
                $("#rddmCr").hide();
                $("#rddmEdit").hide();
            }
        }
    });
    
    /*
	var roomList = [];
	var sectList = [];
	$.ajax({
        url: 'http://localhost:80/SCRDDM?rdId=' + $("#rdId").textbox('getText'),
        type: 'GET',
        headers: {
            'x-auth-token': localStorage.accessToken,
            "Content-Type": "application/json"
        },
        dataType: 'json',
        success: function(data) {
        	roomList = data;
        }
    });
	
	var rows = [{}];
	var cols = [{}];
	$.ajax({
        url: 'http://localhost:80/SCRDSM?sectType=TS&rdId=' + $("#rdId").textbox('getText') + '&_sort=sectSeq&order=DESC',
        type: 'GET',
        headers: {
            'x-auth-token': localStorage.accessToken,
            "Content-Type": "application/json"
        },
        dataType: 'json',
        success: function(data) {
        	sectList = data;
        	for (var y=0; y<roomList.length; y++) {
        	    rows[y] = {'id':y+1};
        	    rows[y]['sectSeq'] = sectList[y].sectSeq;
        	    if (y==0)
        	    	cols[0] = {field:'sectSeq',title:'節次/考場',width:100,height:100};
                for (var x=0; x<roomList.length; x++) {
                    if (y==0) {
                        cols[x+1] = {field:'roomSeq' + (x+1),title:'第' + (x+1) + '站',width:100,height:100};
                    }

                	if (y > 0)
                	    tt = roomList.length * y - 1; //
                	else
                		tt = 0
                    var mode = (x + tt) % roomList.length;
                    if ($("#showType").combobox('getValue') == 'S')
                		rows[y]['roomSeq' + (x+1)] = roomList[mode].examineeName;
                    else if ($("#showType").combobox('getValue') == 'T')
                        rows[y]['roomSeq' + (x+1)] = roomList[x].examinerName;
                    else if ($("#showType").combobox('getValue') == 'R')
                        rows[y]['roomSeq' + (x+1)] = roomList[x].roomName;
                }
            }
        	
        	$("#rddmList").datagrid({
        		columns:[cols],
        		data: rows
        	});
        }
    });
	*/
}

function crRdRoom() {
    //var data = $("#rddmList").datagrid('getRows');
    var rdId = $("#rdId").textbox('getText');
    var data = {"rdId":rdId, "editType":"Y"};
    $.ajax({
        url: 'ScRunDown_crRddm',
        type: 'POST',
        dataType: 'json',
        data: data,
        success: function(res) {
            parent.showStatus(res);
            if (res.success) {
                if (res.rddmEditHide == 'Y'){
                    $("#rddmCr").hide();
	                $("#rddmEdit").show();
                }
                else if (res.rddmCrHide) {
                    $("#rddmCr").show();
	                $("#rddmEdit").hide();
                }
            }
        }
    });
}

function editRdRoom(){
    $("#rdRoomEdit").dialog({ closed: false });
}
</script>
</head>
<body>
<div style="margin: 10px 0 0 10px;">
    <table class="cond">
        <tr>
            <td>梯次代碼</td>
            <td>顯示類別</td>
            <td rowspan="2">
                <button type="button" style="width: 100px;" onclick="qryRddList()">查詢梯次</button>
                <button type="button" id="rddmCr" style="width: 100px; visibility:hidden;" onclick="crRdRoom()">考試表確認</button>
                <button type="button" id="rddmEdit" style="width: 100px; visibility:hidden;" onclick="editRdRoom()">調整考場</button>
            </td>
            <td rowspan="2">
                <button type="button" style="width: 100px;" onclick="backProgId('${bProgId}')">返回</button>
            </td>
        </tr>
        <tr>
            <td><input id="rdId" class="easyui-textbox" style="width: 100px;" value="${rdId}"/></td>
            <td><input id="showType" class="easyui-combobox" style="width: 100px;" data-options="
                valueField: 'id',
                textField: 'text',
                data:[{'id':'S', 'text':'考生'},
                      {'id':'T', 'text':'考官'},
                      {'id':'R', 'text':'診間'}
                ]" value="${showType}"></td>
        </tr>
    </table>
</div>
<div style="margin: 10px 0 0 10px;">
    <div style="clear: both; width: 1310px; height: 380px; overflow: auto;">
        <table id="rddmList" class="listData">
        </table>
    </div>
</div>
<!-- 考場調整畫面 -->
<div id="rdRoomEdit" class="easyui-dialog" style="width: 400px; height: 350px; display: none;"
    data-options="modal: true, title: '梯次', closed: true">
    <div style="float: left;">
        <div style="clear: both;">
            <table id="rdRoomEditTable" style="margin: 20px 0 0 20px;">
                <tr class="modPane">
                    <td>梯次代碼</td>
                    <td>
                        <input id="pRdId" class="easyui-textbox" style="width: 150px;"/>
                    </td>
                </tr>
                <tr class="modPane">
                    <td>選擇節次</td>
                    <td>
                        <input id="pSectSeq" class="easyui-combobox" style="width: 200px;"/>
                    </td>
                </tr>
                <tr class="modPane">
                    <td>選擇考站</td>
                    <td>
                        <input id="pQsId" class="easyui-combobox" style="width: 90px;"/>
                    </td>
                </tr>
                <tr class="modPane">
                    <td>考官</td>
                    <td>
                        <input id="pExaminerId" class="easyui-combobox" style="width: 90px;"/>
                    </td>
                </tr>
                <tr class="modPane">
                    <td>診間</td>
                    <td>
                        <input id="pRoomId" class="easyui-combobox" style="width: 90px;"/>
                    </td>
                </tr>
                <tr class="modPane">
                    <td>標準病人</td>
                    <td>
                        <input id="pPatient1Id" class="easyui-combobox" style="width: 90px;"/>
                        <input id="pPatient2Id" class="easyui-combobox" style="width: 90px;"/>
                        <input id="pPatient3Id" class="easyui-combobox" style="width: 90px;"/>
                    </td>
                </tr>
                <tr>
                    <td colspan="2" style="text-align: center;">
                        <button type="button" style="width: 100px;" onclick="rdRoomEditDone('E');">儲存考場變更</button>
                    </td>
                </tr>
            </table>
        </div>
    </div>
</div>
<!-- 考場調整畫面（結束） -->

<input id="alterUserDel" type="hidden" />
<input type="hidden" id="progId" value="${progId}" />
<input type="hidden" id="privDesc" value="${privDesc}" />
<input type="hidden" id="progTitle" value="${progTitle}" />
<input type="hidden" id="queryHide" value="${queryHide}" />
<input type="hidden" id="status" value="${status}" />
<input type="hidden" id="statusTime" value="${statusTime}" />
<input type="hidden" id="paneBackup" value="${paneBackup}" />
</body>
<script type="text/javascript">
parent.showProg({ id: $("#progId").val(), priv: $("#privDesc").val(), title: $("#progTitle").val() });
parent.showStatus({ status: $("#status").val(), statusTime: $("#statusTime").val() });
</script>
</html>