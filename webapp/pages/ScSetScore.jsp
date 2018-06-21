<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title></title>
<link rel="stylesheet" type="text/css" href="css/easyui.css" />
<link rel="stylesheet" type="text/css" href="css/exam.css" />
<script type="text/javascript" src="js/jquery-1.12.4.js"></script>
<script type="text/javascript" src="js/jcanvas.js"></script>
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
             , 'qsId': $("#qsId").val()
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
                $("#optId").combobox({data: res.totOptClassList});
                $("#optId").combobox("setValue", res.optId);
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
                cols[idx] = {field: 'comm',     title: '鍵盤輸入',     width:  65,
                                formatter:function(value, row, index){
                                    return '<button onclick="editComm()">輸入</button>';
                                }}; idx++;
                cols[idx] = {field: 'pic',      title: '手寫輸入', width:  65,
                                formatter:function(value, row, index){
                                    return '<button onclick=editPic("'+row.optClass+'","'+row.itemNo+'","'+row.itemDesc+'","'+row.picUrl+'")>輸入</button>';
                                }}; idx++;
                cols[idx] = {field: 'picUrl', hidden: true}; idx++;
                
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

function editPic(optClass, itemNo, itemValue, url) {
    $("#edPic").dialog("open");
    
    $("#dItemNo").val(itemNo);
    $("#baseColor").combobox('select', '#FF0000');
    $("#baseType").combobox('select', 'mouse-pointer.png');
    $("#basePaintWidth").combobox('select', 3);
    
    $.each($("#canvasP0").find("canvas"), function(key, canvasP) {
        canvasP.width = canvasWidth;
        canvasP.height = canvasHeight;
        if (key === 0) {
            canvasP1.top = $(canvasP).offset().top;
            canvasP1.left = $(canvasP).offset().left;
        } else {
            //$("#P2").offset({top:$("#P1").position().top,left:$("#P1").position().left});
            $(canvasP).offset({top: canvasP1.top,
                              left: canvasP1.left
            });
        }
    });
    
    $("#canvasP0").css({'width': canvasWidth+4,
        'height': canvasHeight+4,
        'border': '3px #cccccc dashed'});
    
    $canvasP2 = $('#canvasP2');
    $canvasP1 = $('#canvasP1');
    var $offset = $canvasP2.offset();
    
    var itemDesc = "項目說明";
    
    for (var item = 0; item < 2; item++)
    {
        //畫點
        var drawPointX = baseX;
        var drawPointY;
        var diffH;
        if (item == 0)
            drawPointY = baseY;
        else
            drawPointY += baseH + diffH;

        diffH = 0;
        var iniText;
        if (item === 0)
            iniText = itemDesc;
        else
            iniText = itemValue;

        //項目文字
        var param = {
            fillStyle: '#000',
            strokeWidth: 1,
            x: drawPointX + wordPadding.top, y: drawPointY + wordPadding.left,
            fontSize: fontsize,
            fontFamily: 'Trebuchet MS, sans-serif',
            maxWidth: iDescW - wordPadding.left - wordPadding.right,
            fromCenter: false,
            align: 'left',
            text: iniText
        };

        var iDescParams = $canvasP1.measureText(param);
        $canvasP1.drawText(param);

        if (iDescParams.height + wordPadding.top + wordPadding.bottom > baseH)
            diffH = iDescParams.height + wordPadding.top + wordPadding.bottom - baseH;

        $canvasP1.drawRect({
            strokeStyle: 'black',
            strokeWidth: 1,
            x: drawPointX, y: drawPointY,
            fromCenter: false,
            width: iDescW,
            height: baseH + diffH
        });

        drawPointX += iDescW;
        for (var cnt = 0; cnt < optCnt; cnt++)
        {
        	var optText;
	        if (item === 0)
	        	optText = opt[optClass][cnt].optDesc;
	        else
	        	optText = '';
        	//選項文字
            var param = {
                fillStyle: '#000',
                strokeWidth: 1,
                x: drawPointX + wordPadding.top, y: drawPointY + wordPadding.left,
                fontSize: fontsize,
                fontFamily: 'Trebuchet MS, sans-serif',
                maxWidth: iDescW - wordPadding.left - wordPadding.right,
                fromCenter: false,
                align: 'left',
                text: optText
            };
            $canvasP1.drawText(param);
        	
            $canvasP1.drawRect({
                strokeStyle: 'black',
                strokeWidth: 1,
                x: drawPointX, y: drawPointY,
                fromCenter: false,
                width: optW,
                height: baseH + diffH
            });
            drawPointX += optW;
        }

    }
    
    var hastouch = "ontouchstart" in window ? true : false;
    if (hastouch) {
        $canvasP2.on('touchstart', function(e) {
            isMouseDown = true;
        });

        $canvasP2.on('touchend', function() {
            isMouseDown = false;
            lastPos.x = 0;
            lastPos.y = 0;
            pos.x = 0;
            pos.y = 0;

            return;
        });

    } else {
        //On mousedown the painting functionality kicks in
        $canvasP2.on('mousedown', function(e) {
            isMouseDown = true;
        });

        //On mouseup the painting functionality stops
        $canvasP2.on('mouseup', function() {
            isMouseDown = false;
            return;
        });
    }

    $canvasP2.on('mousemove', function(e) {

        lastPos.x = pos.x;
        lastPos.y = pos.y;
        /*
        $("#xy1").val("e.pageX = " + e.pageX);
        $("#xy2").val("e.pagey = " + e.pageY);
        */
        pos.x = e.pageX - $offset.left;
        pos.y = e.pageY - $offset.top;
        if (lastPos.x === 0 && lastPos.y === 0) {
            lastPos.x = pos.x;
            lastPos.y = pos.y;
        }

        if (isMouseDown) {
        /*
            $("#xy1").val("x1 = " + lastPos.x + ",y1 = " + lastPos.y);
            $("#xy2").val("x2 = " + pos.x + ",y2 = " + pos.y);*/
            if (paintType === 'pen')
                paintLine(lastPos.x, lastPos.y, pos.x, pos.y);
            else if (paintType === 'eraser')
                clearLine(lastPos.x, lastPos.y, pos.x, pos.y);
        }
    });
    if (url) {
        $canvasP2.drawImage({
        	source: url
        });
    }
}

function out() {
	var image = $canvasP2.getCanvasImage('png');

	var req = {'rdId': $("#rdId").textbox('getValue')
             , 'roomSeq': $("#roomSeq").val() 
             , 'sectSeq': $("#sectSeq").val()
             , 'qsId': $("#qsId").val()
             , 'itemNo': $("#dItemNo").val()
             , 'image': image.replace("data:image/png;base64,", "")
    };
	
	$.ajax({
        type: 'POST',
        url: 'ScSetScore_editPicDone',
        data: req,
        dataType: 'json',
        success: function(res){
        	parent.showStatus(res);
            if (res.success) {
            
            }
        }
	});
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
    req.optId = $("#optId").combobox('getValue');
    
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
            //$("#optId").textbox("setText", res.optId);
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
                <td style="width: 90px; text-align: right;">
                    <button id="editModBut" type="button" style="width: 80px;" onclick="editComm();">鍵盤輸入</button>
                </td>
                <td style="width: 90px; text-align: right;">
                    <button id="editModBut" type="button" style="width: 80px;" onclick="editPic();">手寫輸入</button>
                </td>
                <!-- <td style="width: 300px;"></td> -->
                <td style="width: 140px; text-align: right;">
整體級數：
                    <input id="optId" class="easyui-combobox" style="width: 50px;" value="${optId}"/>
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
<div id="edPic" class="easyui-dialog" style="width: 900px; height: 500px; display: none;"
    data-options="modal: true, title: '編輯考官塗鴉', closed: true">
    <input id="dItemNo" type="hidden"/>
    <table>
        <tr>
            <td>
                <div id="canvasP0">
                    <div id="P1">
                        <canvas id="canvasP1"/>
                    </div>
                    <div id="P2">
                        <canvas id="canvasP2"/>
                    </div>
                </div>
            </td>
        </tr>
        <tr>
            <td>
                <div class="easyui-panel" style="width: 804px;padding:5px;">
                    <input id="baseColor" class="easyui-combobox" style="width:80px;"/>
                    <input id="baseType" class="easyui-combobox" style="width:80px;"/>
                    <input id="basePaintWidth" class="easyui-combobox" style="width:80px;"/>
                    <input type="input" id="xy1" style="width:90px"/>
                    <input type="input" id="xy2" style="width:90px"/>
                    <button onclick="out()">輸出</button>
                </div>
            </td>
        </tr>
    </table>
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

$("#optId").combobox({
    editable: false,
    panelHeight: 'auto',
    valueField:'optId',
    textField:'optDesc'
})


$(function(){
    //載入手繪的功能鈕
    $("#baseColor").combobox({
        showItemIcon: true,
        data: [
            {value:'#FF0000',text:'red'},
            {value:'#FFFF00',text:'yellow'},
            {value:'#0000CD',text:'mediumblue'},
            {value:'#000000',text:'black'}
        ],
        editable: false,
        panelHeight: 'auto',
        labelPosition: 'top',
        formatter:function(row){
            return '<div style="background-color:'+row.value+';width:30px;height:30px"></div><span class="item-text">'+row.text+'</span>';
        },
        onSelect: function(rec) {
            var r = $(this).next().find("input.textbox-text")[0];
            $(r).css("background-color",rec.value);
            paintColor=rec.value;
        }
    });

    $("#baseType").combobox({
        showItemIcon: true,
        data: [
            {value:'mouse-pointer.png',text:'cursor'},
            {value:'edit.png',text:'pen'},
            {value:'eraser.png',text:'eraser'}
        ],
        editable: false,
        panelHeight: 'auto',
        labelPosition: 'top',
        formatter:function(row){
            var imageFile = 'css/images/' + row.value;
            return '<img class="item-img" src="'+imageFile+'"/><span class="item-text">'+row.text+'</span>';
        },
        onSelect: function(rec) {
            var r = $(this).next().find("input.textbox-text")[0];
            var i = $(this).next().find("img")[0];
            $(i).remove();
            var imageFile = 'css/images/' + rec.value;
            $(r).before('<img class="item-img" src="'+imageFile+'"/>');
            paintType=rec.text;
        }
    });
    $("#basePaintWidth").combobox({
        showItemIcon: true,
        data: [
            {value:'3',text:'3'},
            {value:'5',text:'5'},
            {value:'6',text:'6'},
            {value:'9',text:'9'},
            {value:'11',text:'11'},
            {value:'13',text:'13'},
        ],
        editable: false,
        panelHeight: 'auto',
        labelPosition: 'top',
        formatter:function(row){
            var c = "#000000";
            return '<div style="background-color:'+c+';width:'+row.value+'px;height:'+row.value+'px;border-radius:999em;"></div><span class="item-text">'+row.text+'</span>';
        },
        onSelect: function(rec) {
            var c = "#000000";
            var r = $(this).next().find("input.textbox-text")[0];
            var d = $(this).next().find("div")[0];
            $(d).remove();
            $(r).before('<div style="height:'+r.clientHeight+'px;width:'+r.clientHeight+'px;float:left;">'
                      + '   <div style="margin: 0px auto;position: relative;transform:translateY('+((r.clientHeight-rec.value)/2-1)+'px);background-color:'+c+';width:'+rec.value+'px;height:'+rec.value+'px;border-radius:999em;"></div>'
                      + '</div>');
            paintWidth=rec.value;
        }
    });
});
    
//canvas 的基本值
var isMouseDown = false;
var pos = {
    x: 0,
    y: 0
};
var lastPos = {
    x: 0,
    y: 0
};
var canvasP1 = {
    top: 0,
    left: 0
};
var paintWidth = 3; //預設畫筆寬度
var paintColor = '#FF0000'; //預設畫筆顏色
var paintType = 'pen'; //預設游標 eraser pen
var canvasWidth = 800;
var canvasHeight = 350;

var fontsize = 20;

//itemDesc長度
var iDescW = 400;

//表格基點
var baseX = 30;
var baseY = 30;
//表格高度
var baseH = 80;

//答案選項長度
var optW = 100;
var optCnt = 3; //級分數量

//文字內距
var wordPadding = {
    top: 10,
    right: 10,
    bottom: 10,
    left: 10
};

//文字畫點
var wordPointX = 0;
var wordPointY = 0;

//用jCanvas的方式畫圖
var $canvasP2;
var $canvasP1;

//畫圖的功能
    function clearLine(x1, y1, x2, y2) {
        var radius = paintWidth / 2;

        $canvasP2.clearCanvas({
            x: x1, y: y1,
            radius: radius
        });

        var asin = radius*Math.sin(Math.atan((y2-y1)/(x2-x1)));
        var acos = radius*Math.cos(Math.atan((y2-y1)/(x2-x1)))
        var x3 = x1+asin;
        var y3 = y1-acos;
        var x4 = x1-asin;
        var y4 = y1+acos;
        var x5 = x2+asin;
        var y5 = y2-acos;
        var x6 = x2-asin;
        var y6 = y2+acos;

        $("#xy1").val(x3 +":"+ y3);
        $("#xy2").val(x4 +":"+ y4);

        $canvasP2.clearCanvas({
            x: x2, y: y2,
            radius: radius
        });

        if (x3 + y3 + x4 + y4 + x5 + y5 + x6 + y6 > 0)
            $canvasP2.clearCanvas({
                x3: x3, y3: y3,
                x4: x4, y4: y4,
                x5: x5, y5: y5,
                x6: x6, y6: y6
            });
    }

    function paintLine(x1, y1, x2, y2) {
        $canvasP2.drawLine({
            strokeStyle: paintColor,
            strokeWidth: paintWidth,
            rounded: true,
            strokeJoin: 'round',
            strokeCap: 'round',
            x1: x1,
            y1: y1,
            x2: x2,
            y2: y2
        });
    }
</script>
</html>