function rand(n, m){
    return Math.floor(Math.random()*(m-n+1)+n);
}

var label = ["default","primary","success","info", "warning","danger"]

$(document).ready(function() {
	$(".label").each(function() {
		$(this).addClass("label-"+label[rand(0,5)]);
	});
	$("#count").change(function() {
		SelectChange();
	});
	let tagcloud = getSession("tagcloud")
	if(tagcloud == null) {
		$.ajax({
			"url": contextPath + "/api/tags",
			"type":"GET",
			"dataType":"json",
			"success":function(data) {
				saveSession("tagcloud", data)
				createTagsDOM(data)
			}
		});
	} else {
		createTagsDOM(tagcloud)
	}
	
	let popularArticles = getSession("popularArticles")
	if(popularArticles == null){
		$.ajax({
			"url": contextPath + "/api/articles/popular",
			"type":"GET",
			"dataType":"json",
			"success":function(data) {
				saveSession("popularArticles", data)
				createPopularArticlesDOM(data)
			}
		});
		return;
	}
	createPopularArticlesDOM(popularArticles)
});

function createTagsDOM(data){
	let html ="<div class=\"data_list\" id=\"tagcloud\">"
		+		"<div class=\"data_list_title\">标签云</div>"
		$.each(data.data, function (index, tag) {
			html += "<a href=\""+contextPath+"/tags/"+ tag.name +"\"class=\"label label-"+label[rand(0,5)]+"\" title=\""+tag.name+"\">" + tag.name + "</a>"
		})
		html += "</div>"
		$("#right").append(html)
}

function createPopularArticlesDOM(data){
	let html ="<div class=\"list-group with-title\">"
		 +		"<div class=\"list-group-title\">最受欢迎的文章</div>"
	$.each(data, function (index, article) {
		html += 	"<a class='list-group-item' href=\""+contextPath+"/articles/"+ article.id +"\" title=\""+article.title+"\">"+article.title+"</a>"
	})
	html 	+=   "</div>"
		 	+"</div>"
	$("#right").append(html)
}

//保存数据  
function saveSession(key, obj){
	var str = JSON.stringify(obj); // 将对象转换为字符串
	sessionStorage.setItem(key,str);  
}
//查找数据 
function getSession(key){
	return JSON.parse(sessionStorage.getItem(key));
}
function removeSession(key){
	if(getSession(key) != null){
		sessionStorage.removeItem(key);
		return true;
	}
	return false;
}

String.prototype.endWith=function(endStr){
    var d=this.length-endStr.length;
    return (d>=0&&this.lastIndexOf(endStr)==d)
}
function SelectChange() {
    var selectCount= $("#count").val();
    var url	= window.location+"";
    if((url.indexOf(".action"))>-1){
    	url = (url.split(".action"))[0]+"/";
    }
    if((url.indexOf(".jsp"))>-1){
    	url = (url.split(".jsp"))[0]+"/";
    }
    if((url.indexOf("?page="))>-1){
    	url = (url.split("?page="))[0];
    }
    if((url.indexOf("size"))>-1){
    	url = (url.split("?size="))[0];
    }
    if((url.indexOf("&size"))>-1){
    	url = (url.split("&size="))[0];
    }
    if(url.endWith("/")){
    	window.location = url+"?size="+selectCount;
    	return;
    }
    if((url.indexOf("?q="))>-1){
    	window.location = url+"&size="+selectCount;
    	return;
    }
    window.location = url+"?size="+selectCount;
}

$(".go").click(function(){
	goto();
});

function goto(){
	var	page = $(".goto").val();
	if(page.trim()==""||page==null){
		window.alert("请填好要跳转的页数然后再点击");
	}else{
		var href = $("div>.pagination>li #first_page").attr('href');
		var url = window.location + "";
		if((url.indexOf("?page="))>-1){
	    	url = (url.split("?page="))[0];
	    }
		if((url.indexOf("&page="))>-1){
			url = (url.split("&page="))[0];
		}
		if((url.indexOf("?size=")) > -1) {
			window.location = url + "&page=" + page.trim()
			return;
		}
		if((url.indexOf("?q="))>-1){
			window.location = url + "&page=" + page.trim()
	    	return;
	    }
		window.location = url + "?page=" + page.trim()
	}
}

$(".data_list .datas .data .summary").click(function(){
	var id= $(this).parent().attr("id");
	window.location=contextPath+"/articles/"+id;
});

$(function(){
	$(".goto").keypress(function (e) {
		if (e.keyCode == 13){
			goto();
		}
	})
});
