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
//var copyRow;
//var newRdId;
var queryTmplData = {};
$(function(){
    $("#rdmmList").datagrid({
        width:'98%',
        height:'98%',
        singleSelect:true,
        idField:'rdId',
        pagination: true,
        pagePosition: 'top',
        columns:[[
            {field:'id',hidden:true},
            {field:'rdId',title:'梯次代碼',width:100},
            {field:'rdDesc',title:'梯次說明',width:200},
            {field:'rdDate',title:'梯次日期',width:100, 
                formatter:function(value,row,index){
					return row.rdDate.substr(0,4) + '/' + row.rdDate.substr(5,2) + '/' + row.rdDate.substr(-2);                    
                }
            },
            {field:'begTime',title:'開始時間',width:100, 
                formatter:function(value,row,index){
					return row.begTime.substr(0,2) + ':' + row.begTime.substr(-2);                    
                }
            },
            {field:'qsCount',title:'試題數量',width:100},
            {field:'readTime',title:'每節讀題時間',width:110},
            {field:'examTime',title:'每節考試時間',width:110},
            {field:'stsR',hidden:true},
            {field:'stsS',hidden:true},
            {field:'stsE',hidden:true},
            {field:'rdStatusDesc',title:'梯次狀態',width:150,
            	formatter:function(value,row,index){
            	    //尚未建立考站
            	    var R = '';
            	    var S = '';
            	    var E = '';
            	    var CE = '';
            	    if (row.stsR != 'Y')
            	        R = '尚未建立考站';
            	    //尚未建立節次及考生 
            	    else if (row.stsS != 'Y')
            	    	S = '尚未建立節次及考生';
            	    //都建完後顯示「產生考試梯次表
            	    else if (row.stsE != 'Y') {
            			E = '<button class="easyui-linkbutton" onclick="parent.selProg(\'ScMntSect?rdId='+ row.rdId ;
            			var i = 0
            			$.each(queryTmplData, function(f,v){
            			    E += "&p_b_ScRunDown["+ (i++) +"]=" + f + ":" + v;
            			});
            			E += "&p_b_progId[0]=ScRunDown"
            		    E += '\');">產生考試梯次表 </button>';
            	    }
            		//考試梯次表已建
            		else if (row.stsE == 'Y')
            		    CE = '考試梯次表已建';
            		//考試中
            		//考試已結束 顯示空白
            		return R + S + E + CE;
            	}
            },
            {field:'rdStatus',hidden:true},
            {field:'rdProcess',title:'梯次處理',width:150
                , formatter:function(value,row,index){
                    //if (row.rdStatus == 'cr') {
                        var U = '<button class="easyui-linkbutton" onclick="editRd(\'U\', this)">編輯</button>';
                        var D = '<button class="easyui-linkbutton" onclick="rdEditDone(\'D\', this)">刪除</button>';
                        var C = '<button class="easyui-linkbutton" onclick="editRd(\'C\', this)">複製</button>';
                        return U + D + C;
                    //} 
                }
            },
            {field:'rdCreat',title:'建檔處理',width:150
                , formatter:function(value,row,index){
                    //if (row.rdStatus == 'cr') {
                        var AR = '<button class="easyui-linkbutton" onclick="editRdRoom(this)">考站</button>';
                        var AS = '<button class="easyui-linkbutton" onclick="editRdSect(this)">節次</button>';
                        var AE = '<button class="easyui-linkbutton" onclick="editRdExaminee(this)">考生</button>';
                        return AR + AS + AE;
                    //} 
                }
            	
            }
        ]]
    });
    
    //copyRow = $("#rdEditTable").find("tr.modPane.copy");
    //newRdId = $("#rdEditTable").find("tr.modPane.rdId");
    /*
    var td = $("#rdmmList").datagrid('getPanel').find('div.datagrid-header td[field="rdId"]');
    td.addClass('headCol1');
    */
});

//點選datagrid時取得row的index
function getRowIndex(target){
    var tr = $(target).closest('tr.datagrid-row');
    return parseInt(tr.attr('datagrid-row-index'));
}
//主畫面 考試梯次建檔
function qryRdList() {
    var data = {};
    data.rdId = $("#rdId").textbox('getText');
    data.rdDesc = $("#rdDesc").textbox('getText');
    data.rdDateS = $("#rdDateS").textbox('getText');
    data.rdDateE = $("#rdDateE").textbox('getText');
    queryTmplData = data;
    $.ajax({
        url: 'ScRunDown_qryRd',
        type: 'POST',
        data: data,
        dataType: 'json',
        success: function(res) {
            parent.showStatus(res);
            if (res.success) {
                $("#rdmmList").datagrid('loadData',res.rdmmList);
            }
            else {
                $("#rdmmList").datagrid('loadData',[]);
            }
        }
    });
}

function editRd(editType, target) {
    $("#rdEdit").dialog({ closed: false });
    //type = A新增
    //type = U更新 C複製， 帶入target資料，場次代號readonly
    var row = $("#rdmmList").datagrid('getRows')[getRowIndex(target)];
    $("#pRdId").textbox({'readonly':true});
    $("#pNewRdId").textbox({'readonly':true});
    if (editType == 'U' || editType == 'C') {
	    $("#pRdId").textbox('setText', row.rdId);
	    $("#pRdDesc").textbox('setText', row.rdDesc);
	    $("#pRdDate").datebox('setValue', row.rdDate);
	    $("#pBegTime").timespinner('setValue', row.begTime);
	    $("#pReadTime").textbox('setText', row.readTime);
	    $("#pExamTime").textbox('setText', row.examTime);
    }
    else if (editType == 'A') {
        $("#pRdId").textbox('setText', '');
	    $("#pRdDesc").textbox('setText', '');
	    $("#pRdDate").datebox('setValue', '');
	    $("#pBegTime").timespinner('setValue', '');
	    $("#pReadTime").textbox('setText', '');
	    $("#pExamTime").textbox('setText', '');
    }
    
    //$("#rdEditTable").find(".copy").remove();
    //$("#rdEditTable").find(".rdId").remove();
    if (editType == 'C') {
        //$("#rdEditTable").find(".copyTarget").after(copyRow.clone());
        //$("#rdEditTable").find(".rdIdTarget").after(newRdId.clone());
        $("#pNewRdId").textbox({
            'readonly':true, 
            'height':24
        });
        $("#pNewRdId").textbox('setText', '*');
        $("#pNewRdId").next().show();
        $("#pNewRdIdL").show();
        $("#pCopyL").show();
        $("#pCopy").show();
        $("#pCopyQs").prop('checked', true);
        $("#pCopyExaminer").prop('checked', true);
        $("#pCopyPatient").prop('checked', true);
        $("#pCopySect").prop('checked', true);
    }
    else
    {
        $("#pNewRdId").textbox('setText', '');
        $("#pNewRdId").next().hide();
        $("#pNewRdIdL").hide();
        $("#pCopyL").hide();
        $("#pCopy").hide();
        $("#pCopyQs").prop('checked', false);
        $("#pCopyExaminer").prop('checked', false);
        $("#pCopyPatient").prop('checked', false);
        $("#pCopySect").prop('checked', false);
    }
    //var test = $("#rdEditTable").find("tr.modPane.rdId");
}

function rdEditDone(editType, target) {
    //type = E新增、複製及更新
    //type = D刪除，需顯示題示
    var data;
    data = {
        'pRdId': $("#pRdId").textbox('getText'),
        'pRdDesc': $("#pRdDesc").textbox('getText'),
        'pRdDate': $("#pRdDate").textbox('getText'),
        'pBegTime': $("#pBegTime").textbox('getText'),
        'pReadTime': $("#pReadTime").textbox('getText'),
        'pExamTime': $("#pExamTime").textbox('getText'),
        'editType': editType
    };
    $.extend(data, queryTmplData);
    
    if (editType == 'D' && target) {
        var row = $("#rdmmList").datagrid('getRows')[getRowIndex(target)];
        data['pRdId'] = row.rdId;
    } 
    else {
        data['pNewRdId']     = $("#pNewRdId").textbox('getText');
        data['copyQs']       = $("#pCopyQs").prop("checked");
	    data['copyExaminer'] = $("#pCopyExaminer").prop("checked");
	    data['copyPatient']  = $("#pCopyPatient").prop("checked");
	    data['copySect']     = $("#pCopySect").prop("checked");
    }        

    if (editType == 'D') {
        $.messager.confirm({
    		title: '確認',
    		msg: '是否要刪除此樣板？',
    		ok: '刪除',
    		cancel: '取消',
    		fn: function (ok) {
    		    if (ok)
    		        rdEditAjaxDone(data);   
    		}
        });
    }
    else
    {
        rdEditAjaxDone(data);
    }
}

function rdEditAjaxDone(data) {
    $.ajax({
        url: 'ScRunDown_editRd',
        type: 'POST',
        data: data,
        dataType: 'json',
        success: function(res) {
            parent.showStatus(res);
            if (res.success) {
                $("#rdmmList").datagrid('loadData',res.rdmmList);
                $("#pNewRdId").textbox('setText', res.pNewRdId);
                
            }
        }
    });
}
//主畫面 考試梯次建檔 END

//編輯考站
function rdRoomEditRow(target){
	var index;
	if (typeof(target) == 'number')
		index = target;
	else
	    index = getRowIndex(target);
    $('#rdRoom').datagrid('beginEdit', index);
    var edRow = $("#rdRoom").datagrid('getEditors', index);
    var row = $("#rdRoom").datagrid('getRows')[index];
    var rdDate = $("#rdRoomRdDate").val();
    var rdId = $("#rdRoomRdId").val();
    
    var befCommBoxText = {};
    
    for (var i=0;i<edRow.length;i++)
    {
        var ed = edRow[i];
        if (ed.field == 'qsName') { //qsmstr + scqstm
            $(ed.target).combobox({
                valueField:'qsId',
                textField:'qsName',
                mode: 'remote',
                loader: function(param,success,error){
                    //var test = typeof(param);
                    data = {'param': param.q};
                    $.ajax({
                    	url: 'ScRunDown_qryQsList',
                        type: 'POST',
                        data: data,
                        dataType: 'json',
                        success: function(res) {
                        	if (res.success) {
	                            success(res.qsList);
	                            var ed = befCommBoxText['qsEd'];
	                            if ((!param.q || param.q.length < 0) && !ed.first) {
	                                $(ed.target).combobox('setText', befCommBoxText['qsName']);
	                                $(ed.target).combobox('setValue', befCommBoxText['qsId']);
	                            }
                        	}
                        	else
                        		return false;
                        }
                    });
                }
            });
            befCommBoxText['qsEd'] = ed;
            befCommBoxText['qsName'] = row.qsName;
            befCommBoxText['qsId'] = row.qsId;
        }
        else if (ed.field == 'roomName') { //EXROOM
            $(ed.target).combobox({
                valueField:'roomId',
                textField:'roomName',
                mode: 'remote',
                loader: function(param,success,error){
                	data = {'param': param.q, 'rdId': rdId, 'rdDate': rdDate};
                    $.ajax({
                        url: 'ScRunDown_qryRoomList',
                        type: 'POST',
                        data: data,
                        dataType: 'json',
                        success: function(res) {
                            if (res.success) {
                                success(res.roomList);
                                var ed = befCommBoxText['roomEd'];
                                if ((!param.q || param.q.length < 0) && !ed.first) {
                                	$(ed.target).combobox('setText', befCommBoxText['roomName']);
                                    $(ed.target).combobox('setValue', befCommBoxText['roomId']);
                                }
                            }
                            else
                                return false;
                        }
                    });
                }
            });
            befCommBoxText['roomEd'] = ed;
            befCommBoxText['roomName'] = row.roomName;
            befCommBoxText['roomId'] = row.roomId;
        }
        else if (ed.field == 'examinerName') { //cmuser
            $(ed.target).combobox({
                valueField:'examiner',
                textField:'examinerName',
                mode: 'remote',
                loader: function(param,success,error){
                	data = {'param': param.q, 'rdId': rdId, 'rdDate': rdDate, 'examiner': 'Y'};
                    $.ajax({
                        url: 'ScRunDown_qryUserList',
                        type: 'POST',
                        data: data,
                        dataType: 'json',
                        success: function(res) {
                            if (res.success) {
                                success(res.userList);
                                var ed = befCommBoxText['examinerEd'];
                                if ((!param.q || param.q.length < 0) && !ed.first) {
                                	$(ed.target).combobox('setText', befCommBoxText['examinerName']);
                                    $(ed.target).combobox('setValue', befCommBoxText['examiner']);
                                }
                            }
                            else
                                return false;
                        }
                    });
                }
            });
            befCommBoxText['examinerEd'] = ed;
            befCommBoxText['examinerName'] = row.examinerName;
            befCommBoxText['examiner'] = row.examiner;
        }
        else if (ed.field == 'patient1Name') { //mbdetl
            $(ed.target).combobox({
                valueField:'patient',
                textField:'patientName',
                mode: 'remote',
                loader: function(param,success,error){
                	data = {'param': param.q, 'rdId': rdId, 'rdDate': rdDate, 'examiner': 'N'};
                    $.ajax({
                        url: 'ScRunDown_qryUserList',
                        type: 'POST',
                        data: data,
                        dataType: 'json',
                        success: function(res) {
                            if (res.success) {
                                success(res.userList);
                                var ed = befCommBoxText['patient1Ed'];
                                if ((!param.q || param.q.length < 0) && !ed.first) {
                                	$(ed.target).combobox('setText', befCommBoxText['patient1Name']);
                                    $(ed.target).combobox('setValue', befCommBoxText['patient1']);
                                }
                            }
                            else
                                return false;
                        }
                    });
                }
            });
            befCommBoxText['patient1Ed'] = ed;
            befCommBoxText['patient1Name'] = row.patient1Name;
            befCommBoxText['patient1'] = row.patient1;
        }
        else if (ed.field == 'patient2Name') {
            $(ed.target).combobox({
                valueField:'patient',
                textField:'patientName',
                mode: 'remote',
                loader: function(param,success,error){
                	data = {'param': param.q, 'rdId': rdId, 'rdDate': rdDate, 'examiner': 'N'};
                    $.ajax({
                        url: 'ScRunDown_qryUserList',
                        type: 'POST',
                        data: data,
                        dataType: 'json',
                        success: function(res) {
                            if (res.success) {
                            	res.userList.unshift({'patientName': "　", 'patient': null})
                                success(res.userList);
                                var ed = befCommBoxText['patient2Ed'];
                                if ((!param.q || param.q.length < 0) && !ed.first) {
                                    $(ed.target).combobox('setText', befCommBoxText['patient2Name']);
                                    $(ed.target).combobox('setValue', befCommBoxText['patient2']);
                                }
                            }
                            else
                                return false;
                        }
                    });
                }
            });
            befCommBoxText['patient2Ed'] = ed;
            befCommBoxText['patient2Name'] = row.patient2Name;
            befCommBoxText['patient2'] = row.patient2;
        }
        else if (ed.field == 'patient3Name') {
            $(ed.target).combobox({
                valueField:'patient',
                textField:'patientName',
                mode: 'remote',
                loader: function(param,success,error){
                	data = {'param': param.q, 'rdId': rdId, 'rdDate': rdDate, 'examiner': 'N'};
                    $.ajax({
                        url: 'ScRunDown_qryUserList',
                        type: 'POST',
                        data: data,
                        dataType: 'json',
                        success: function(res) {
                            if (res.success) {
                            	res.userList.unshift({'patientName': "　", 'patient': null})
                                success(res.userList);
                                var ed = befCommBoxText['patient3Ed'];
                                if ((!param.q || param.q.length < 0) && !ed.first) {
                                    $(ed.target).combobox('setText', befCommBoxText['patient3Name']);
                                    $(ed.target).combobox('setValue', befCommBoxText['patient3']);
                                }
                            }
                            else
                                return false;
                        }
                    });
                }
            });
            befCommBoxText['patient3Ed'] = ed;
            befCommBoxText['patient3Name'] = row.patient3Name;
            befCommBoxText['patient3'] = row.patient3;
        }
    }
}
function rdRoomDeleteRow(target){
    $('#rdRoom').datagrid('deleteRow', getRowIndex(target));
    reLoadRdRoomSeq();
}
function rdRoomSaveRow(target){
    $('#rdRoom').datagrid('endEdit', getRowIndex(target));
    $('#rdRoom').datagrid('enableDnd');
}
function rdRoomCancelRow(target){
    $('#rdRoom').datagrid('cancelEdit', getRowIndex(target));
    reLoadRdRoomSeq();
}
//重排序號
function reLoadRdRoomSeq() {
	var pages = $('#rdRoom').datagrid('getRows');
    for (var i=0; i<pages.length; i++) {
        $('#rdRoom').datagrid('updateRow', {
            index: i,
            row: {
                roomSeq: i+1
            }
        });     
    }
}

//考站建檔 
function rdRoomEditDone(editType) {
    var rdRoomList = $("#rdRoom").datagrid('getRows');
    var rdId = $("#rdRoomRdId").val();
    var rdDate = $("#rdRoomRdDate").val();
    var data = {"pRdId": rdId,
	            "pRdDate": rdDate,
	            "pRdrmList":JSON.stringify(rdRoomList)};
    $.extend(data, queryTmplData);
    $.ajax({
        url: 'ScRunDown_editRdrm',
        type: 'POST',
        data: data,
        dataType: 'json',
        success: function(res) {
        	parent.showStatus(res);
        	if (res.success) {
        		alert('處理' + res.cnt + '筆資料');
        		$("#rdmmList").datagrid('loadData',res.rdmmList);
        	}
        }
    });
}

//考站視窗
function editRdRoom(target) {
    var row = $("#rdmmList").datagrid('getRows')[getRowIndex(target)];
    
    $("#rdRoomEdit").dialog({
        closed: false, 
        title: row.rdId + '考試站建檔'
    });
    
    $("#rdRoomRdId").val(row.rdId);
    $("#rdRoomRdDate").val(row.rdDate);
    
    $.ajax({
        url: 'ScRunDown_qryRdrm',
        type: 'POST',
        data: {'rdId': row.rdId},
        dataType: 'json',
        success: function(res) {
        	if (res.success)
                $('#rdRoom').datagrid('loadData',res.rdrmList);
        	else
        		$('#rdRoom').datagrid('loadData',[]);
        }
    });
    
    $("#rdRoom").datagrid({
        width:'770px',
        height:'300px',
        singleSelect:true,
        idField:'roomSeq',
        columns:[[
            {field:'roomSeq',title:'序號',width:40},
            {field:'qsName',title:'教案名稱',width:150,editor:{type:'combobox'}},
            {field:'qsId',hidden:true},
            {field:'roomName',title:'診間名稱',width:150,editor:{type:'combobox'}},
            {field:'roomId',hidden:true},
            {field:'examinerName',title:'考官',width:80,editor:{type:'combobox'}},
            {field:'examiner',hidden:true},
            {field:'patient1Name',title:'標準病人1',width:80,editor:{type:'combobox'}},
            {field:'patient1',hidden:true},
            {field:'patient2Name',title:'標準病人2',width:80,editor:{type:'combobox'}},
            {field:'patient2',hidden:true},
            {field:'patient3Name',title:'標準病人3',width:80,editor:{type:'combobox'}},
            {field:'patient3',hidden:true},
            {field:'action',title:'處理',width:100,align:'center',
				formatter:function(value,row,index){
					if (row.editing){
						var s = '<button class="easyui-linkbutton" onclick="rdRoomSaveRow(this)">儲存</a> ';
						var c = '<button class="easyui-linkbutton" onclick="rdRoomCancelRow(this)">取消</a>';
						return s+c;
					} else {
						var e = '<button class="easyui-linkbutton" onclick="rdRoomEditRow(this)">更新</a> ';
						var d = '<button class="easyui-linkbutton" onclick="rdRoomDeleteRow(this)">刪除</a>';
						return e+d;
					}
				}
			}
        ]],
        onEndEdit:function(index,row){
            var edRow = $(this).datagrid('getEditors', index);
            for(var i=0; i<edRow.length; i++){
                var ed = edRow[i];
                if (ed.field == 'qsName') {
                    row.qsName = $(ed.target).combobox('getText');
                    row.qsId = $(ed.target).combobox('getValue');                    
                }
                else if (ed.field == 'roomName') {
                    row.roomName = $(ed.target).combobox('getText');
                    row.roomId = $(ed.target).combobox('getValue');
                }
                else if (ed.field == 'examinerName') {
                    row.examinerName = $(ed.target).combobox('getText');
                    row.examiner = $(ed.target).combobox('getValue');
                }
                else if (ed.field == 'patient1Name') {
                    row.patient1Name = $(ed.target).combobox('getText');
                    row.patient1 = $(ed.target).combobox('getValue');
                }
                else if (ed.field == 'patient2Name') {
                    row.patient2Name = $(ed.target).combobox('getText');
                    row.patient2 = $(ed.target).combobox('getValue');
                }
                else if (ed.field == 'patient3Name') {
                    row.patient3Name = $(ed.target).combobox('getText');
                    row.patient3 = $(ed.target).combobox('getValue');
                }
            }
            
        },
        onBeforeEdit:function(index,row){
            row.editing = true;
            $(this).datagrid('refreshRow', index);
        },
        onAfterEdit:function(index,row){
            row.editing = false;
            $(this).datagrid('refreshRow', index);
        },
        onCancelEdit:function(index,row){
            row.editing = false;
            $(this).datagrid('refreshRow', index);
        },
        onLoadSuccess:function(){
            $(this).datagrid('enableDnd');
            reLoadRdRoomSeq();
        },
        onDrop:function(){
        	reLoadRdRoomSeq();
        }
    });
}
//增加考站編輯row
function addRdRoom() {
	var pages = $("#rdRoom").datagrid('getRows');
	for (var i=0; i<pages.length; i++) {
		$('#rdRoom').datagrid('cancelEdit', i);		
	}
	
	var row = $('#rdRoom').datagrid('getSelected');
	var index;
    if (row){
        index = $('#rdRoom').datagrid('getRowIndex', row) + 1;
    } else {
        index = 0;
    }
    $('#rdRoom').datagrid('insertRow', {
        index: index,
        row:{}
    });
    $('#rdRoom').datagrid('selectRow',index);
    reLoadRdRoomSeq();
    rdRoomEditRow(index);
}
//考站建檔 END

//考試節次建檔
//節次重排
function reLoadRdSectSeq() {
	var rows = $('#rdSect').datagrid('getRows');
	var sectRdIdx = 0;
    for (var i=0; i<rows.length; i++) {
    	var row = rows[i];
    	var sectSeq = 0;
    	if (row.sectType == 'REA' || row.sectType == 'EXA') {
    	    if (row.sectType == 'REA')
    			sectRdIdx++;
    		sectSeq = sectRdIdx;    		
    	}    		
        $('#rdSect').datagrid('updateRow', {
            index: i,
            row: {
                seqNo: i+1,
                sectSeq: sectSeq
            }
        });     
    }
}

function rdSectInputField(sectType) {
    $("#btRdSectEdit").attr('disabled', 'disabled');
    $("#edSectType").textbox('setText' ,'');
    $("#edSectTime").numberbox('setValue',0);
    $("#edFileName").textbox('setText' ,'');
    $("#edRelaTime").numberbox('setValue',0);
	$("#edSectType").textbox({'readonly':true});
    $("#edSectTime").textbox({'readonly':true});
	$("#edFileName").textbox({'readonly':true});
    $("#edRelaTime").textbox({'readonly':true});
    
    if (sectType == 'RES' || sectType == 'SUS' 
        || sectType == 'BET' || sectType == 'ENT' || sectType == 'MED') {
           $("#btRdSectEdit").removeAttr('disabled');
           $("#edSectType").textbox({'readonly':false});
       }
       if (sectType == 'RES' || sectType == 'SUS' )  
           $("#edSectTime").textbox({'readonly':false});
              
       if (sectType == 'MED' )
           $("#edFileName").textbox({'readonly':false});
       
       if (sectType == 'BET' || sectType == 'ENT' || sectType == 'MED')
       	$("#edRelaTime").textbox({'readonly':false});          
}

function rdSectAddRow() {
    var row = $('#rdSect').datagrid('getSelected');
    
    var insRow = 1;
	var index; 
    if (row){
        if (row.sectType == 'REA')
            insRow++;
        index = $('#rdSect').datagrid('getRowIndex', row) + insRow;
    } else {
        return;
    }
    $('#rdSect').datagrid('insertRow', {
        index: index,
        row:{}
    });
    $('#rdSect').datagrid('selectRow',index);
    
    rdSectInputField('xxxx');
	$("#edSectType").textbox({'readonly':false});
    reLoadRdSectSeq();
}

function rdSectEditRow() {
    var row = $('#rdSect').datagrid('getSelected');
    if (row){
        index = $('#rdSect').datagrid('getRowIndex', row);
    } else {
        return;
    }
    $('#rdSect').datagrid('updateRow', {
        index: index,
        row:{
            sectName: $("#edSectType").combobox('getText'),
            sectType: $("#edSectType").combobox('getValue'),
            sectTime: $("#edSectTime").numberbox('getValue'),
            fileName: $("#edFileName").textbox('getText'),
            relaTime: $("#edRelaTime").numberbox('getValue')
        }
    });
    reLoadRdSectSeq();
}

function rdSectDelRow(target) {
	index = getRowIndex(target);
    if (!index){
        return;
    }
	$('#rdSect').datagrid('deleteRow', index);
	reLoadRdSectSeq();
}

//節次建檔
function rdSectEditDone() {
	var rows = $("#rdSect").datagrid('getRows');
	var rdId = $("#rdSectRdId").val();
	var data = {"pRdId": rdId,
			    "sectClean": $("#sectClean").prop("checked"),
	            "pSectList":JSON.stringify(rows)};
	$.extend(data, queryTmplData);
	$.ajax({
	    url: 'ScRunDown_editSect',
	    type: 'POST',
	    data: data,
	    dataType: 'json',
	    success: function(res) {
	        parent.showStatus(res);
	        if (res.success) {
	            alert('處理' + res.cnt + '筆資料');
	            if ($("#sectClean").prop("checked"))
	            	$("#rdSect").datagrid('loadData', []);
            	$("#rdmmList").datagrid('loadData',res.rdmmList);
	        }
	    }
	});

	/*
	var save = 0;
	var tsCnt = 0;
	var ddmData = [];
	for (var i=0; i<rows.length; i++) {
		var httpMethod = 'POST';
		var row = rows[i];
		var id = '';
		if (row.id != null && row.id != '') {
			id = '/' + row.id;
			httpMethod = 'PUT';
		}
			 
		delete row['_selected'];
		row.rdId = $("#rdSectRdId").val(); 
		$.ajax({
	        url: 'http://localhost:80/SCRDSM' + id,
	        type: httpMethod,
	        data: JSON.stringify(row),
	        headers: {
	            'x-auth-token': localStorage.accessToken,
	            "Content-Type": "application/json"
	        },
	        dataType: 'json',
	        success: function(data) {
	        	save = save + 1;
	        	
	        }
	    });
	}
	alert('處理'+ ddmData.length + '筆資料。');
	*/
}

//顯示主畫面
function editRdSect(target) {
    var row = $("#rdmmList").datagrid('getRows')[getRowIndex(target)];
    
    $("#rdSectEdit").dialog({
        closed: false, 
        title: row.rdId + '梯 節次建檔'
    });
    
    $("#rdSectRdId").val(row.rdId);
    
    $("#rdSect").datagrid({
        width:'560px',
        height:'350px',
        singleSelect:true,
        idField:'seqNo',
        method:'POST',
        columns:[[
            {field:'seqNo',hidden:true},
            {field:'sectSeq',title:'節次',width:40},
            {field:'sectName',title:'時間類別',width:150},
            {field:'sectType',hidden:true},
            {field:'sectTime',title:'時間長度',width:80},
            {field:'fileName',title:'音效檔名',width:150},
            {field:'relaTime',title:'相對時間',width:80},
            {field:'execTime',hidden:true},
            {field:'process',title:'處理',width:50, 
                formatter:function(value,row,index){
					if (row.sectSeq == 0){
						var s = '<button class="easyui-linkbutton" onclick="rdSectDelRow(this)">刪除</a> ';
					}
						return s;
				}
            }
        ]],
        onLoadSuccess:function(){
            $(this).datagrid('clearSelections');
            $("#edSectType").textbox('setText' ,'');
            $("#edSectTime").textbox('setValue','');
            $("#edFileName").textbox('setText' ,'');
            $("#edRelaTime").textbox('setValue','');
        },
        onClickRow:function(index, row){
            rdSectInputField(row.sectType);
            $("#edSectType").textbox('setText',row.sectName);
            $("#edSectTime").textbox('setValue',row.sectTime);
            $("#edFileName").textbox('setText',row.fileName);
            $("#edRelaTime").textbox('setValue',row.relaTime);
        }
    });
    
    $.ajax({
        url: 'ScRunDown_qrySect',
        type: 'POST',
        data: {'rdId': row.rdId},
        dataType: 'json',
        success: function(res) {
            if (res.success)
                $('#rdSect').datagrid('loadData',res.sectList);
            else
                $('#rdSect').datagrid('loadData',[]);
        }
    });
    
    $("#edSectType").combobox({
    	valueField:'value',
        textField:'text',
    	url: 'ScRunDown_qrySectTypeList',
    	method: 'get', 
    	onSelect: function(rec) {
    	    rdSectInputField(rec.value);
    	}
    })
}
//考試節次END

//考生建檔
//節次重排
function reLoadRdExamineeSeq() {
	var pages = $('#rdExaminee').datagrid('getRows');
    for (var i=0; i<pages.length; i++) {
        $('#rdExaminee').datagrid('updateRow', {
            index: i,
            row: {
                roomSeq: i+1
            }
        });     
    }
}

function rdExamineeEditRow() {
    var edExamineeName = $("#edExaminee").combobox('getText');
    var edExaminee = $("#edExaminee").combobox('getValue');
    
    if (edExamineeName != '' && edExamineeName != null) {
        var row = $('#rdExaminee').datagrid('getSelected');
    	var index;
        if (row){
            index = $('#rdExaminee').datagrid('getRowIndex', row);
        } else {
            return;
        }
        
        $('#rdExaminee').datagrid('updateRow', {
            index: index,
            row:{
                examineeName: edExamineeName, 
                examinee: edExaminee
            }
        });
        $('#rdExaminee').datagrid('selectRow',index);
        $('#rdExaminee').datagrid('enableDnd');
        reLoadRdExamineeSeq();        
    }    
}

/*
function rdExamineeDelRow() {
	var row = $('#rdExaminee').datagrid('getSelected');
    if (row){
        index = $('#rdExaminee').datagrid('getRowIndex', row);
    } else {
        return;
    }
    $('#rdExaminee').datagrid('deleteRow', index);
    reLoadRdExamineeSeq();
}*/

function rdExamineeEditDone(){
	var rows = $("#rdExaminee").datagrid('getRows');
    var save = 0;
    
    for (var i=0; i<rows.length; i++) {
        var httpMethod = 'POST';
        var row = rows[i];
        var id = '';
        if (row.id != null && row.id != '') {
            id = '/' + row.id;
            httpMethod = 'PUT';
        }
        
        delete row['_selected'];
        row.rdId = $("#rdExamineeRdId").val(); 
        $.ajax({
            url: 'http://localhost:80/SCRDDM' + id,
            type: httpMethod,
            data: JSON.stringify(row),
            headers: {
                'x-auth-token': localStorage.accessToken,
                "Content-Type": "application/json"
            },
            dataType: 'json',
            success: function(data) {
                save = save + 1;
            }
        });
    }
    alert('處理'+ save + '筆資料。');
}

function editRdExaminee(target) {
    var row = $("#rdmmList").datagrid('getRows')[getRowIndex(target)];
    
    $("#rdExamineeEdit").dialog({
        closed: false, 
        title: row.rdId + '梯學生建檔'
    });
    
    $("#rdExamineeRdId").val(row.rdId); 
    
    $("#edExaminee").combobox({
        valueField:'examineeId',
        textField:'examineeName',
        mode: 'remote',
        loader: function(param,success,error){
            var url;
            if (param.q && param.q.length > 0)
                url = 'http://localhost:80/examinee?examineeName_like=' + param.q;
            else
                url = 'http://localhost:80/examinee';
            $.ajax({
                url: url,
                type: 'GET',
                headers: {
                    'x-auth-token': localStorage.accessToken,
                    "Content-Type": "application/json"
                },
                dataType: 'json',
                success: function(data) {
                    success(data);
                },
                error: function() { return false; }
            });
        }
    });
    
    $("#rdExaminee").datagrid({
        title:'首節次序',
        width:'250px',
        height:'350px',
        singleSelect:true,
        url:'http://localhost:80/SCRDDM?rdId=' + row.rdId + '&_sort=roomSeq&order=DESC',
        method:'GET',
        columns:[[
			{field:'roomSeq',title:'順序',width:80},
            {field:'examineeName',title:'考生名稱',width:150},
            {field:'examinee',hidden:true}
        ]],
        onLoadSuccess:function(){
            $(this).datagrid('enableDnd');
            reLoadRdExamineeSeq();
        },
        onDrop:function(){
            reLoadRdExamineeSeq();
        },
        onSelect:function(index, row) {
        	$("#edExaminee").combobox('setText', row.examineeName);
        	$("#edExaminee").combobox('setValue', row.examinee);
        }
    });
    
}


</script>
</head>
<div style="margin: 10px 0 0 10px;">
    <table class="cond">
        <tr>
            <td>梯次代碼</td>
            <td>梯次說明</td>
            <td>梯次起始日期</td>
            <td>梯次終止日期</td>
            <td rowspan="2">
                <button class="easyui-linkbutton" style="${queryHide}" onclick="qryRdList()">查詢梯次</button>
            </td>
            <td rowspan="2">
                <button class="easyui-linkbutton" style="${queryHide}" onclick="editRd('A')">新增梯次</button>
            </td>
        </tr>
        <tr>
            <td>
                <input id="rdId" class="easyui-textbox" style="width: 100px;" value="${rdId}"/>
            </td>
            <td>
                <input id="rdDesc" class="easyui-textbox" style="width: 100px;" value="${rdDesc}"/>
            </td>
            <td>
                <input id="rdDateS" class="easyui-datebox" style="width: 100px;" value="${rdDateS}"/>
            </td>
            <td>
                <input id="rdDateE" class="easyui-datebox" style="width: 100px;" value="${rdDateE}"/>
            </td>
        </tr>
    </table>
</div>
<div style="margin: 10px 0 0 10px;">
    <div style="clear: both; width: 1310px; height: 380px; overflow: auto;">
        <table id="rdmmList" class="listData">
        </table>
    </div>
</div>
<!-- 考試梯次建檔畫面 -->
<div id="rdEdit" class="easyui-dialog" style="width: 380px; height: 400px; display: none;"
    data-options="modal: true, title: '梯次建檔', closed: true">
    <div style="float: left;">
        <div style="clear: both;">
            <table id="rdEditTable" style="margin: 20px 0 0 20px;">
                <tr class="modPane rdIdTarget">
                    <td>梯次代碼</td>
                    <td>
                        <input id="pRdId" class="easyui-textbox" style="width: 150px;"/>
                    </td>
                </tr>
                <tr class="modPane">
                    <td>
                    	<labeal id="pNewRdIdL">新梯次代碼</labeal>
                   	</td>
                    <td>
                        <input id="pNewRdId" type="text" style="width: 150px;"/>
                    </td>
                </tr>
                <tr class="modPane">
                    <td>梯次說明</td>
                    <td>
                        <input id="pRdDesc" class="easyui-textbox" style="width: 200px;height:100px;"
                         data-options="multiline:true"/>
                    </td>
                </tr>
                <tr class="modPane">
                    <td>梯次日期</td>
                    <td>
                        <input id="pRdDate" class="easyui-datebox" style="width: 90px;"/>
                    </td>
                </tr>
                <tr class="modPane">
                    <td>開始時間</td>
                    <td>
                        <input id="pBegTime" class="easyui-timespinner" style="width: 90px;" value="09:00"/>
                    </td>
                </tr>
                <tr class="modPane">
                    <td>每節讀題時間(分)</td>
                    <td>
                        <input id="pReadTime" class="easyui-numberbox" style="width: 90px;"/>
                    </td>
                </tr>
                <tr class="modPane">
                    <td>每節考試時間(分)</td>
                    <td>
                        <input id="pExamTime" class="easyui-numberbox" style="width: 90px;"/>
                    </td>
                </tr>
                <tr class="modPane">
                	<td><label id="pCopyL">複製選項</label></td>
                	<td>
                		<div id="pCopy">
	                		<input id="pCopyQs" type="checkBox" checked/>教案
	                		<input id="pCopyExaminer" type="checkBox" checked/>考官
	                		<input id="pCopyPatient" type="checkBox" checked/>標準病人
	                		<input id="pCopySect" type="checkBox" checked/>節次
                		</div>
                	</td>
                </tr>
                <tr>
                    <td colspan="2" style="text-align: center;">
                        <button class="easyui-linkbutton" onclick="rdEditDone('E');">儲存考場</button>
                    </td>
                </tr>
            </table>
        </div>
    </div>
</div>
<!-- 梯次建檔畫面（結束） -->
<!-- 考試站建檔畫面  -->
<div id="rdRoomEdit" class="easyui-dialog" style="width: 820px; height: 500px; display: none;"
    data-options="modal: true, closed: true">
    <input id="rdRoomRdId" type="hidden" />
    <input id="rdRoomRdDate" type="hidden" />
    <div style="float: left;">
        <table style="margin: 20px 0 0 10px;">
            <tr>
                <td>
                    <button class="easyui-linkbutton" onclick="addRdRoom();">增加教案</button>
                </td>
            </tr>
            <tr>
                <td>
                    <table id="rdRoom"></table>
                </td>
            </tr>
            <tr>
                <td>
                    <button class="easyui-linkbutton" onclick="rdRoomEditDone('E');">儲存考堂</button>
                </td>
            </tr>
        </table>
    </div>
</div>
<!-- 考堂建檔畫面（結束）  -->
<!-- 考試節次建檔畫面 -->
<div id="rdSectEdit" class="easyui-dialog" style="width: 690px; height: 520px; display: none;"
    data-options="modal: true, closed: true">
    <input id="rdSectRdId" type="hidden" />
    <div style="float: left;">
        <table style="margin: 10px 0 0 10px;">
        	<tr>
        		<td>
        			
        		</td>
        		<td>
	        		<table>
	            		<tr>
	            			<td>時間類別</td>
	            			<td>長度</td>
	            			<td>音效檔名</td>
	            			<td>相對時間</td>
	            		</tr>
	            		<tr>
	            			<td><input id="edSectType" class="easyui-combobox" style="width: 150px;"/></td>
	            			<td><input id="edSectTime" class="easyui-numberbox" style="width: 80px;"/></td>
	            			<td><input id="edFileName" class="easyui-textbox" style="width: 150px;"/></td>
	            			<td><input id="edRelaTime" class="easyui-numberbox" style="width: 80px;"/></td>
	            			<td><button id="btRdSectEdit" class="easyui-linkbutton" onclick="rdSectEditRow()">確認</button></td>
	            		</tr>
	            	</table>
        		</td>
        	</tr>
            <tr>
            	<td valign="top">
            		<button class="easyui-linkbutton" onclick="rdSectAddRow()">插入類別</button>
            	</td>
                <td>
                    <table id="rdSect"></table>
                </td>
            </tr>
            <tr>
            	<td></td>
                <td>
                    <button class="easyui-linkbutton" onclick="rdSectEditDone();">儲存節次</button>
                    <input id="sectClean" type="checkBox" />清除節次
                </td>
            </tr>
        </table>
    </div>
</div>
<!-- 考試節次建檔畫面 （結束）-->
<!-- 考生及考試流程建檔畫面  -->
<div id="rdExamineeEdit" class="easyui-dialog" style="width: 400px; height: 500px; display: none;"
    data-options="modal: true, closed: true">
    <input id="rdExamineeRdId" type="hidden" />
    <div style="float: left;">
        <div style="clear: both;">
            <table style="margin: 20px 0 0 20px;">
                <tr>
                    <td>選擇考生</td>
                </tr>
                <tr>
                    <td>
                        <input id="edExaminee"/><button class="easyui-linkbutton" onclick="rdExamineeEditRow()">編輯</button>
                    </td>
                </tr>
                <tr>
                	<td>
                		<table id="rdExaminee"></table>
                	</td>
                </tr>
                <tr>
	                <td>
	                    <button class="easyui-linkbutton" onclick="rdExamineeEditDone('E');">儲存</button>
	                </td>
	            </tr>
            </table>
        </div>
    </div>
</div>
<!-- 考堂建檔畫面（結束）  -->
<input id="alterUserDel" type="hidden" />
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