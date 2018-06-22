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
                            
                            $("#optId").textbox('setText',res.exList[0]['foptId']);
                            $("#score").textbox('setText',res.exList[0]['fscore']);
                            $("#result").textbox('setText',res.exList[0]['fresult']);
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
            {field:'fscore', hidden:true},
            {field:'fresult', hidden:true},
            {field:'foptId', hidden:true},
            {field:'fexamComm', hidden:true},
            {field:'fexamPic', hidden:true},
        ]]
    });

    
});

//點選datagrid時取得row的index
function getRowIndex(target){
    var tr = $(target).closest('tr.datagrid-row');
    return parseInt(tr.attr('datagrid-row-index'));
}

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
                cols[idx] = {field: 'examComm',     title: '鍵盤輸入',     width:  65,
                                formatter:function(value, row, index){
                                    return '<button onclick=editComm("'+value+'")>輸入</button>';
                                }}; idx++;
                cols[idx] = {field: 'examPic',      title: '手寫輸入', width:  65,
                                formatter:function(value, row, index){
                                    //"'+row.optClass+'","'+row.itemNo+'","'+row.itemDesc+'","'+row.picUrl+'"
                                    //var rowJsonString = JSON.stringify(row);
                                    return '<button onclick=editPic(this)>輸入</button>';
                                }}; idx++;
                //cols[idx] = {field: 'examPic', hidden: true}; idx++;
                //cols[idx] = {field: 'examPic', hidden: true}; idx++;
                
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

function loadCanvas(reload) {
    if (!reload) {
	    $.each($("#canvasP0").find("canvas"), function(key, canvasP) {
	        canvasP.width = canvasWidth;
	        canvasP.height = canvasHeight;
	        if (key === 0) {
	            canvasP1.top = $(canvasP).offset().top;
	            canvasP1.left = $(canvasP).offset().left;
	        } else {
	            $(canvasP).offset({
	                top: canvasP1.top,
	                left: canvasP1.left
	            });
	        }
	    });
	    
	    $("#canvasP0").css({'width': canvasWidth+4,
	        'height': canvasHeight+4,
	        'border': '3px #cccccc dashed'});
	    $canvasP2 = $('#canvasP2');
	    $canvasP1 = $('#canvasP1');
    }
    $offset = $canvasP2.offset();
    
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
        $canvasP2.on('mousedown', function(e) {
            isMouseDown = true;
        });

        $canvasP2.on('mouseup', function() {
            isMouseDown = false;
            return;
        });
    }

    $canvasP2.on('mousemove', function(e) {
        lastPos.x = pos.x;
        lastPos.y = pos.y;
        pos.x = e.pageX - $offset.left;
        pos.y = e.pageY - $offset.top;
        if (lastPos.x === 0 && lastPos.y === 0) {
            lastPos.x = pos.x;
            lastPos.y = pos.y;
        }

        if (isMouseDown) {
            if (paintType === 'pen')
                paintLine(lastPos.x, lastPos.y, pos.x, pos.y);
            else if (paintType === 'eraser')
                clearLine(lastPos.x, lastPos.y, pos.x, pos.y);
        }
    });
}

function editPic(target) {
    $("#edPic").dialog("open");
    $("#baseColor").combobox('select', '#FF0000');
    $("#baseType").combobox('select', 'mouse-pointer.png');
    $("#basePaintWidth").combobox('select', 3);
    
	loadCanvas();
	var arg = {};
    if (target != null && target != '') {
        //項目輸入
    	arg = $("#itemList").datagrid('getRows')[getRowIndex(target)];
    	$("#dItemNo").val(arg.itemNo);
    	var itemDesc = "項目說明";
    	
    	for (var item = 0; item < 2; item++)
        {
            //設定起始座標
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
                iniText = arg.itemDesc;

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
                diffH = iDescParams.height + wordPadding.top + wordPadding.bottom - baseH; //因為有可能字太多所以要拉大框
			
            //項目的框
            $canvasP1.drawRect({
                strokeStyle: 'black',
                strokeWidth: 1,
                x: drawPointX, y: drawPointY,
                fromCenter: false,
                width: iDescW,
                height: baseH + diffH
            });

            drawPointX += iDescW;
            //顯示opt類別
            for (var cnt = 0; cnt < opt[arg.optClass].length; cnt++)
            {
            	var optText;
    	        if (item === 0)
    	        	optText = opt[arg.optClass][cnt].optDesc;
    	        else {
    	            if (opt[arg.optClass][cnt].noSel != 'Y')
    	        		optText = opt[arg.optClass][cnt].optId;
    	            else
    	                optText = '';
    	        }
            	//opt類別文字
                var param = {
                    fillStyle: '#000',
                    strokeWidth: 1,
                    x: drawPointX + wordPadding.top, y: drawPointY + wordPadding.left,
                    fontSize: fontsize,
                    fontFamily: 'Trebuchet MS, sans-serif',
                    maxWidth: optW - wordPadding.left - wordPadding.right,
                    fromCenter: false,
                    align: 'center',
                    text: optText
                };
                $canvasP1.drawText(param);
                
                //如果有分數就畫個圈
                if (arg.optId === opt[arg.optClass][cnt].optId && item !== 0) {
                    var p = $canvasP1.measureText(param);
    	            $canvasP1.drawArc({
    	                layer: true,
    	                strokeStyle: 'red',
    	                strokeWidth: 3,
    	                x: drawPointX + (p.width/2), y: drawPointY + (p.height/2),
    	                fromCenter: false,
    	                radius: p.width
    	            });
                }
            	
                //opt類別的框
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
    }
    else {
        //主題輸入
        var totOptClassList = $("#optId").combobox('getData');
        arg.examPic = '';//$("#examineeText").combogrid('get');
        var p = {};
        var drawPointX = baseX;
        var drawPointY = baseY;
        fontsize = 15;
        var param = {
            fillStyle: '#000',
            strokeWidth: 1,
            x: drawPointX, y: drawPointY,
            fontSize: fontsize,
            fontFamily: 'Trebuchet MS, sans-serif',
            //maxWidth: optW - wordPadding.left - wordPadding.right,
            fromCenter: false,
            align: 'center',
            text: '評分結果:'
        };
        p = $canvasP1.measureText(param);
        $canvasP1.drawText(param);
        drawPointX += p.width + 10;
        //PASS
        var r = $("#resList").val().replace(/\'/g, '"');
        var resList = JSON.parse(r)['resList'];
        for (var o = 0; o < resList.length; o++) {
            //前面要畫方框，跟字等高
            $canvasP1.drawRect({
                strokeStyle: '#000',
                strokeWidth: 1,
                x: drawPointX, y: drawPointY,
                fromCenter: false,
                width: fontsize,
                height: fontsize
            });			
            
            var p1 = {'x':0, 'y':0};
            var p2 = p1;
            var p3 = p1;
            
            p1.x = fontsize*Math.cos(15);
            
            if ($("#optId").textbox('getValue') === resList[o].value) {
                //打勾
                
                $('canvas').drawLine({
                    strokeStyle: 'red',
                    strokeWidth: 3,
                    x1: p1.x, y1: p1.y,
                    x2: p2.x, y2: p2.y,
                    x3: p3.x, y3: p3.y
                  });
            }
            drawPointX += fontsize + 3;
            
            optText = resList[o].text;
            var param = {
                fillStyle: '#000',
                strokeWidth: 1,
                x: drawPointX, y: drawPointY,
                fontSize: fontsize,
                fontFamily: 'Trebuchet MS, sans-serif',
                //maxWidth: optW - wordPadding.left - wordPadding.right,
                fromCenter: false,
                align: 'center',
                text: optText
            };
            p = $canvasP1.measureText(param);
            $canvasP1.drawText(param);
            
            drawPointX += p.width + 10;
        }
        
        drawPointX += 10
        
        //整體級分
        for (var o = 0; o < totOptClassList.length; o++) {
                        
            optText = totOptClassList[o].optDesc;
            var param = {
                fillStyle: '#000',
                strokeWidth: 1,
                x: drawPointX, y: drawPointY,
                fontSize: fontsize,
                fontFamily: 'Trebuchet MS, sans-serif',
                //maxWidth: optW - wordPadding.left - wordPadding.right,
                fromCenter: false,
                align: 'center',
                text: optText
            };
            p = $canvasP1.measureText(param);
            $canvasP1.drawText(param);
            
            if ($("#optId").combobox('getValue') === totOptClassList[o].optId) {
                $canvasP1.drawArc({
	                layer: true,
	                strokeStyle: 'red',
	                strokeWidth: 3,
	                x: drawPointX + 0, y: drawPointY - (p.height/2),
	                fromCenter: false,
	                radius: p.width/2
	            });
            }
            drawPointX += p.width + 10;
        }
    }    
    
    //下載上次存檔的圖
    if (arg.examPic) {
        $.ajax({
            type: 'POST',
            url: 'ScSetScore_getPic',
            data: {'examPic':arg.examPic},
            dataType: 'json',
            success: function(res){
            	parent.showStatus(res);
                if (res.success) {
                    $canvasP2.drawImage({
                    	source: 'data:image/png;base64,' + res.imageBase64,
                    	x: 0, y: 0,
                    	fromCenter: false
                    });
                }
            }        
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
                $("#itemList").datagrid('updateRow', {
                    index: $("#dItemNo").val() - 1,
                    row: {
                        examPic: res.examPic
                    }
                });
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
        //item.examPic = "";
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
<div id="edPic" class="easyui-dialog" style="width: 900px; height: 500px; display: none; overflow:hidden;"
    data-options="modal: true,
                  title: '編輯考官塗鴉', 
                  closed: true,
                  onClose : function() {
                      parent.document.body.style.overflow = 'auto';
                  },
                  onMove: function() {
                      if ($canvasP2 != null) {
                          $offset = $canvasP2.offset();
                          loadCanvas(true);
                      }
                  }">
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
                    <button onclick="$canvasP2.clearCanvas()">清除</button>
                    <input type="input" id="xy1" style="width:90px"/>
                    <input type="input" id="xy2" style="width:90px"/>
                    <button onclick="out()">存檔</button>
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
<input type="hidden" id="resList" value="${resList}" />
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
            if (rec.text === 'pen')
            	parent.document.body.style.overflow = "hidden";
            else
                parent.document.body.style.overflow = "auto";
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
var canvasWidth = 850;
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
var optW = 80;

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
//目標圖的位置
var $offset;

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

    $("#xy1").val("x1:"+ x1);
    $("#xy2").val("y1:"+ y1);

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
    $("#xy1").val("x1:"+ x1);
    $("#xy2").val("y1:"+ y1);
    
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