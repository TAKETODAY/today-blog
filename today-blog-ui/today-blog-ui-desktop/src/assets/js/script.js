//back to top	
$(document).ready(function(){
	// context path
    $("#scrollToTop").hide()
    // when scroll to 100px
    $(function () {
        $(window).scroll(function(){
            if ($(window).scrollTop()>100){
                $("#scrollToTop").fadeIn(500)
            } else {
                $("#scrollToTop").fadeOut(500)
            }
        })
        // click to top
        $("#scrollToTop").click(function(){
            $('body,html').animate({scrollTop:0},500)
            return false
        })
    })
	setTimeout(() => {
        $.post(contextPath + "/pv?referer="+encodeURIComponent(document.referrer))
    }, 1500);
})

var Share = {
	site  : "https://taketoday.cn",
	desc  : "TODAY BLOG 代码是我心中的一首诗",
	image : "https://cdn.taketoday.cn/logo.png",
	summary : "TODAY BLOG 是记录我学习的博客。主要分享自己的心得体会,学习经验、建站经验、资源分享、知识分享、杂谈生活.",
}
var shareUrl = [
    "http://connect.qq.com/widget/shareqq/index.html?" +
    "url="+Share.site+
    "&title="+Share.desc+
    "&desc="+Share.desc+
    "&summary="+Share.summary+
    "&site="+Share.site+
    "&pics="+Share.image,
    
    "https://sns.qzone.qq.com/cgi-bin/qzshare/cgi_qzshare_onekey?" +
    "url=https://taketoday.cn" +
    "&title="+Share.desc+
    "&desc="+Share.desc+
    "&summary="+Share.summary+
    "&site="+Share.site+
    "&pics="+Share.image,
    
    "http://service.weibo.com/share/share.php?" +
    "url="+Share.site+
    "&title="+Share.desc+
    "&pic="+Share.image+
    "&searchPic=true"
]

document.onclick = function(e) {
	let s = (e.target|| e.srcElement).id
    if(s == "qq") {
        window.open(shareUrl[0])
    } else if(s == "qqZone"){
        window.open(shareUrl[1])
    } else if(s == "weiBo"){
        window.open(shareUrl[2])
    } else if(s =="menuShare"){
    	$.modal({
            title: "分享",
            text: "选择分享平台",
            buttons: [
              { text: "QQ好友", onClick: function(){window.open(shareUrl[0])}},
              { text: "QQ空间", onClick: function(){window.open(shareUrl[1])}},
              { text: "微博", onClick: function(){window.open(shareUrl[2])}},
              { text: "取消分享"}
            ]
    	})
    }
}
function logout() {
	$.confirm({
		text:"您确定要退出吗?",
		title: "确认退出?",
		onOK:function() {
			location = contextPath + "/logout"
		},
		onCancel:function(){
			return false
		}
	})
}

var defaultOptions = {
	method : "GET",
	url : "/index",
	data : null,
	type : "application/x-www-form-urlencoded",
	success : function(result) {},
	error : function(result, status) {},
	catched: null
}

function json(options) {
	let params = defaultOptions
	Object.assign(params, options)
	ajax(params.method, params.url, params.data, params.type, params.success, params.error, params.catched)
}

function ajax(method, url, data, type, success, error, catched) {
	NProgress.start()
	try {
		let xhr = new XMLHttpRequest()
		xhr.open(method, url)
		xhr.onload = function() {
			try{NProgress.done();$.hideLoading()} catch (e) {}
			let result = JSON.parse(xhr.responseText)
			if(xhr.status == 200) {
				success(result)
			} else if (xhr.status == 401 || (result != null && result.code == 401)) {
				$.toast("登录超时", "cancel")
				setTimeout(() => {
					location = location
				}, 2000);
			} else {
				if (error != null && typeof error == "function") {
					error(result)
				} else if (result.message != null){
					$.toast(result.message, "cancel")
				} else {
					$.toast("服务器繁忙","cancel")
				}
			}
		}
		if (type == null || type == undefined) {
			type = 'application/x-www-form-urlencoded'
		} else if (type == "json") {
			type = 'application/json;charset=UTF-8'
		}
		if(!(data instanceof FormData)) {
			if (typeof data == "object" && data != null) {
				data = JSON.stringify(data)
			}
			xhr.setRequestHeader('Content-type', type)
		}
		xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest')
		if (data == null) 
			xhr.send()
		else 
			xhr.send(data)
	} catch (e) {
		if (catched == null) {
			if (error == null)
				throw e
			error(e)
		}
		catched(e)
	}
}

$("#search").click(function(){
	$(this).html("")
})
let timer
$("#searchInput").keyup(function(e){
	let code = e.keyCode
	if (!((code>=65&&code<=90)||(code>=48&&code<=57)||(code>=96&&code<=105)||(code==8||code==32))) {
		return
	}
    if(timer) {
    	clearTimeout(timer)
    }
    timer = setTimeout(() => {
    	$("#search").html("")
    	let load ="<li style='text-align:center;'><img src='" + contextPath + "/assets/images/loading.gif' width='25'></li>"
    	let q = $("#searchInput").val()
    	if(q.length < 3) {
    		return
    	}
    	$("#search").html(load)
    	ajax("GET", contextPath+"/api/search/"+q, null, null, function(data) {
    		if(data.code == 500) {
    			$.toast(data.message,"cancel");
    		}
    		let html = ""
    		if(data.hits == null || data.hits.total == 0){
    			html = "<li><a>没有关于<em>" + q + "</em>的结果</a></li>"
    		} else {
    			$.each(data.hits.hits, function (index, article) {
    				html += "<li><a href='"+contextPath+"/articles/"+article._source.id+"'>"+((article.highlight==null)?article._source.title:article.highlight.title[0])+"</a></li>"
    			})
    			html +="<a href='"+contextPath+"/search?q="+q+"' class='without-shadow opposite btn' style='border:none;width:100%;'>查看更多结果</a>"	
    		}
    		$("#searchForm").addClass("open")
    		$("#searchForm>div").attr("aria-expanded","true")
    		$("#search").html(html)
    	}, function() {
    		$("#search").html("<li><a>搜索<em>"+q+"</em>出错</a></li>")
    	})
    }, 300)
})
$("body").on("keypress", "#searchInput", function(e){
	let q = $("#searchInput").val()
	if (e.keyCode == 13){
		if(q != null && q.trim()!=""){
			location = contextPath+"/search?q="+q
		}
	}
})
$("#searchBtn").click(function(){
	let q = $("#searchInput").val()
	if(q != null && q.trim()!=""){
		location = contextPath+"/search?q="+q
   	}
})
console.log("%cTODAY BLOG%c\n代码是我心中的一首诗\n\nCopyright © TODAY & 2017 - 2020 All Rights Reserved.\n","font-size:96px;text-shadow: 1px 1px 1px rgba(0,0,0,.2);","font-size:12px;color:rgba(0,0,0,.38);")
