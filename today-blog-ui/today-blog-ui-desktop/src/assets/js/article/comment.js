$.ajaxSetup({
  cache: true
});
//对Date的扩展，将 Date 转化为指定格式的String
//月(M)、日(d)、小时(h)、分(m)、秒(s)、季度(q) 可以用 1-2 个占位符，
//年(y)可以用 1-4 个占位符，毫秒(S)只能用 1 个占位符(是 1-3 位的数字)
//例子：
//(new Date()).Format("yyyy-MM-dd hh:mm:ss.S") ==> 2006-07-02 08:09:04.423
//(new Date()).Format("yyyy-M-d h:m:s.S") ==> 2006-7-2 8:9:4.18
Date.prototype.format = function(fmt){
	var o = {
			"M+" : this.getMonth()+1,                 // 月份
			"d+" : this.getDate(),                    // 日
			"h+" : this.getHours(),                   // 小时
			"m+" : this.getMinutes(),                 // 分
			"s+" : this.getSeconds(),                 // 秒
			"q+" : Math.floor((this.getMonth()+3)/3), // 季度
			"S"  : this.getMilliseconds()             // 毫秒
	};
	if(/(y+)/.test(fmt))
		fmt=fmt.replace(RegExp.$1, (this.getFullYear()+"").substr(4 - RegExp.$1.length));
	for(var k in o)
		if(new RegExp("("+ k +")").test(fmt))
			fmt = fmt.replace(RegExp.$1, (RegExp.$1.length==1) ? (o[k]) : (("00"+ o[k]).substr((""+ o[k]).length)));
	return fmt;
}
function changeImage(img){
	img.src = img.src + "?c="+Math.random()
}
//自动get新验证码
function getRandCode(){
	$("#randImage").attr("src", contextPath+"/captcha?c="+Math.random())
}
function saveSession(key, obj) {
	sessionStorage.setItem(key, JSON.stringify(obj));  
}
function getSession(key) {
	return JSON.parse(sessionStorage.getItem(key));
}
function clearCache() {
	sessionStorage.clear();
	localStorage.clear();
	$.toast("清除成功");
}
function removeSession(key) {
	if(getSession(key) != null){
		sessionStorage.removeItem(key);
		return true;
	}
	return false;
}
function saveStorage(key, obj) {
	var str = JSON.stringify(obj); // 将对象转换为字符串
	localStorage.setItem(key,str);  
}
function getStorage(key){
	return JSON.parse(localStorage.getItem(key));
}
function removeStorage(key){
	if(getStorage(key) != null){
		localStorage.removeItem(key);
		return true;
	}
	return false;
}
function getCommentDOM(data) {
	let html =""
	if(data.data == null || data.data.length==0){
		return "<p style='text-align: center;'>暂无评论</p>"
	}
	$.each(data.data, function (index, comment) {
		let user = comment.user
html = html +	"<div class='comment' id='comment_" + comment.id + "'>"
			+		"<div class='comment_user_info'>"
			+			"<a href='"+user.site+"'>"
			+				"<img class='comment_avatar' alt='" + user.name + "' src='"+user.image+"' title='"+user.name+"'/>" 
			+			"</a>"
			+			"<div class='user_info'>"
			+				"<a href='" + user.site + "' target='_blank'>"
			+					"<span class='user_name'>" + user.name + "</span>"
			+				"</a>"
			+				"<span data-toggle='tooltip' data-placement='top' title='" + new Date(comment.id).format("yyyy-MM-dd hh:mm:ss") + "' style='cursor: pointer'>"
			+					"<time>" + formatTime(comment.id) + "</time>"
			+				"</span>"
			+			"</div>"
			+			"<div class='user_description'>" + user.introduce + "</div>"
			+		"</div>"
			+		"<div class='comment_content'>"
			+			"<div class='content'>" + comment.content + "</div>"
			+			"<div class='replyOper'>"
			+				"<a href='#comment_area' class='replyBtn' title='回复 " + user.name + "' data-image='" + user.image+ "' data-introduce='" + user.introduce + "' data-site='" + user.site + "' data-name='"+user.name+"' data-id='"+comment.id+"'>"
			+					"<i class='fa fa-reply'>"
			+						"<span> 回复 </span>"
			+					"</i>"
			+				"</a>"
			+			"</div>"
			+		"</div>"
			+ getReplies(comment, user)
			+	"</div>"
	})
	return html
}

function getReplies(comment, user) {
	if (comment.replies.length == 0) {
		return "";
	}
	let html = ""
	+		"<div class='sub_comment' >"
	$.each(comment.replies, function (index, reply) {
		let replyUser = reply.user
html = html	+	"<div class='sub_comment_item' id='comment_" + reply.id + "'>"
	+				"<div class='comment_user_info'>"
	+					"<a href='"+replyUser.site+"'>"
	+						"<img class='comment_avatar' alt='"+replyUser.name+"' src='"+replyUser.image+"' title='"+replyUser.name+"'>"
	+					"</a>"
	+					"<div class='user_info'>"
	+						"<a href='"+replyUser.site+"' target='_blank'>"
	+							"<span class='user_name'>"+replyUser.name+"</span>"
	+						"</a>回复"
	+						"<a href='"+ user.site +"' target='_blank'>"
	+							"<span class='user_name'>"+ user.name +"</span>"
	+						"</a>"
	+						"<span data-toggle='tooltip' data-placement='top' title='" + new Date(reply.id).format("yyyy-MM-dd hh:mm:ss") + "' style='cursor: pointer'>"
	+							"<time>" + formatTime(reply.id) + "</time>"
	+						"</span>"
	+					"</div>"
	+					"<div class='user_description'>" + replyUser.introduce + "</div>"
	+				"</div>"
	+				"<div class='sub_comment_content'>"
	+					"<div class='content'>"+(reply.content)+"</div>"
	+					"<div class='replyOper'>"
	+						"<a href='#comment_area' class='replyBtn' title='回复 " + replyUser.name + "' data-image='" + replyUser.image+ "' data-introduce='" + replyUser.introduce + "' data-site='" + replyUser.site + "' data-id='"+reply.id+"' data-name='"+replyUser.name+"'>"
	+							"<i class='fa fa-reply'>"
	+								"<span> 回复 </span>"
	+							"</i>"
	+						"</a>"
	+					"</div>"
	+				"</div>"
	+ getReplies(reply, replyUser)
	+			"</div>"})
html = html +"</div>"
	return html
}

// has loaded comments
var commentIsLoaded = false
// bowser's height
var windowHeight = $(window).height()
// ready
function gethtml() {
	return 	"<div class='comment' id='comment_x'>"
	+			"<div class='comment_user_info'>"
	+				"<a class='blank'>"
	+ 					"<div></div>"
	+ 				"</a>"
	+ 				"<div class='user_info'>"
	+					"<div></div>"
	+				"</div>"
	+				"<div class='user_address'>"
	+					"<div></div>"
	+				"</div>"
	+			"</div>"
	+			"<div class='comment_content'>"
	+				"<div class='content'>"
	+					"<div></div>"
	+					"<div></div>"
	+				"</div>"
	+ 			"</div>"
	+		"</div>"
}

function formatTime(ct) {
	
	ct = (new Date().getTime() - ct) / 1000
	
	if (ct > 31104000) {
		return Number.parseInt(ct / 31104000) + "年前";
	}
	if (ct > 2592000) {
		return Number.parseInt(ct / 2592000) + "个月前";
	}
	if (ct > 172800) {
		return Number.parseInt(ct / 86400) + "天前";
	}
	if (ct > 86400) {
		return "昨天";
	}
	if (ct > 3600) {
		return Number.parseInt(ct / 3600) + "小时前";
	}
	if (ct > 60) {
		return Number.parseInt(ct / 60) + "分钟前";
	}
	if (ct > 0) {
		return Number.parseInt(ct) + "秒前";
	}
	return "刚刚";
}

function comments() {
	if(commentIsLoaded) {// loaded
		return
	}
	//current position to top length
	var scroll = $(this).scrollTop()
	// element to top length
	var comments = $("#comments").offset().top
	// exist comment area
	if(($('div .comment')[0])) {
		return
	}
	if((windowHeight + scroll) < (comments + 150)){
		return
	}
	loadComment(1)
}

function buildReply(current) {
	let user = current.user
	let html = "<i class='fa fa-reply fa-flip-horizontal'></i>"
			 + 	"<div class='comment_user_info' style='display: inline;margin-left: 5px;'>"
			 + 		"<a href='" + user.site + "'>"
			 + 			"<img class='comment_avatar' alt='" + user.name + "' src='" + user.image + "' title='" + user.name + "' style='border-radius: 6px; width: 40px;'>"
			 +		"</a>"
			 + 		"<div class='user_info'>"
			 +			"<a href='" + user.site + "' target='_blank'>" 
			 +				"<span class='user_name'>" + user.name + "</span>" 
			 +			"</a>" 
			 +		"</div>" 
			 +		"<div class='user_description'>" + user.introduce + "</div>" 
			 + 	"</div>"
			 +  "<div style='float: right;margin-right: -10px;'>"
			 +		"<button onclick='cancel()' class='btn btn-primary'>取消评论</button>"
			 +  "</div>"
			 
	$("#comment_area .data_list_title").html(html)
}

function cancel() {
	$("#comment_area .data_list_title").html("<i class='fa fa-edit'></i> 发表评论")
	current.commentId = 0
	current.user.site = ""
	current.user.name = ""
	current.user.image = ""
	current.user.introduce = ""
	removeStorage("current")
}

function loadScripts() {

	$.getScript(contextPath + "/assets/plugin/easymde/easymde.min.js", function( data, textStatus, jqxhr) {
		easyMDE = new EasyMDE({
		    element: document.getElementById("content"),
		    autoDownloadFontAwesome: false,
		    autofocus: true,
		    renderingConfig: {
		        codeSyntaxHighlighting: true,
		        markedOptions: {
		            gfm:true
		        }
		    },
		    previewRender: function(plainText) {
		        var preview = document.getElementsByClassName("editor-preview-side")[0]
		        let content = md.render(plainText)
		        preview.setAttribute('id', 'editor-preview')
		        
		        preview.innerHTML = content
		        return content
		    },
		    status: ["autosave", "lines", "words"],
		    tabSize: 4,
		    promptURLs:true,
		    toolbar: ["bold","italic","strikethrough","heading","|","code","quote","unordered-list","ordered-list","|",
		              "link","image" ,"table","horizontal-rule","|","preview","undo","redo","|","guide","|"]
		});
		let comment = getStorage("comment"); 
		if(comment != null) {
			easyMDE.value(comment)
		}
		let currentData = getStorage("current");//{"commentId":1560163859921,"articleId":1560163530909,"user":{"site":"https://taketoday.cn","name":"TODAY","image":"/upload/2019/6/2/88fed8f1blob.png","introduce":"I TAKE TODAY"}}
		if (currentData != null && currentData.articleId == current.articleId) {
			current = currentData
			buildReply(current)
		}
		easyMDE.codemirror.on("change", function(){
		    saveStorage("comment", easyMDE.value());
		})
		try{$.hideLoading()} catch (e) {}
	})
}

var editorLoaded = false
var commentTextarea = document.querySelector("textarea#content")
if (commentTextarea != null) {
	commentTextarea.onfocus = function() {
		if (!editorLoaded) {
			$.showLoading("正在加载")
			loadScripts()
			editorLoaded = true
			//$.toast("加载成功")
		}
	}
}

$(function() {
	if($(window).width() < 768) {
		$("#left").attr("style","padding: 0;")
	}
    let hash = location.hash
    if (hash !== '') {
        loadComment(1, function() {
            let id = $(location.hash)
            if (id.length != 0) {
                $('html, body').animate({
                    scrollTop: id.offset().top - 80
                }, 500)
                setTimeout(() => {
                	id.fadeOut()
                	id.fadeIn()
				}, 500);
            }
            $.hideLoading()
        })
    }
    setTimeout(() => {
        $.post(contextPath + "/api/articles/" + current.articleId + "/pv")
    }, 1500);
})

function loadComment(page, callback) {
	$.showLoading("正在加载评论")
	$("#commentDatas").html(gethtml() + gethtml())
	ajax("GET", contextPath + "/api/articles/" + current.articleId + "/comments?page=" + page, null, null, function(data) {
		if(data.success) {
			if(data.data == null || data.data.length == 0){
				$.toast("暂无评论")
			} else {
				$("#pagination").remove()
				//$.toast("评论加载成功")
				appendDOM(getPaginationHtml("", data.size, data.num, data.current), "comments")
			}
			$("#commentDatas").html(getCommentDOM(data))
			$('[data-toggle="tooltip"]').tooltip()
			if (callback != null && typeof callback == "function") {
				callback()
			}
		} else {
			$("#commentDatas").html("<p style='text-align: center;'>服务器繁忙，请稍后<a onclick='loadComment(1)'>重试</a></p>")
			$.toast("加载失败","cancel")
		}
		commentIsLoaded = true
	}, function() {
		$("#commentDatas").html("<p style='text-align: center;'>服务器繁忙，请稍后<a onclick='loadComment(1)'>重试</a></p>")
		$.toast("服务器繁忙","cancel")
		commentIsLoaded = false
	})
}

function rand(n, m){
    return Math.floor(Math.random()*(m-n+1)+n);
}

function login() {
	setInterval(function() {
		location = contextPath + "/login?forward="+encodeURIComponent(location.pathname.replace(contextPath,"")+"#comment_area")
	}, 2000)
}

/**
 * @returns
 */
$("body").on("click", ".replyBtn", function(e) {
	
	current.commentId = $(this).data("id")
	current.user.name = $(this).data("name")
	current.user.site = $(this).data("site")
	current.user.image = $(this).data("image")
	current.user.introduce = $(this).data("introduce")
	
	$('html, body').animate({ // to comment area
		scrollTop: $($.attr(this, 'href')).offset().top - 70
	}, 500)
	
	saveStorage("current", current)
	if (isLoggedIn) {
		buildReply(current)
		easyMDE.codemirror.focus()
	} else {
		login()
	}
    return false
})

function saveComment() {
	let plainText 	= easyMDE.value()
	if(plainText == null || plainText.trim() == ''){
		$.toast("请输入评论内容", "cancel")
		return false
	}
	let randCode 	= $("#randCode").val()
	if(randCode == null || randCode.trim() == ''){
		$.toast("请填写验证码", "cancel")
		return false
	}
	if(current.articleId == 0) {
		$.toast("浏览器错误","cancel")
		return false
	}
	let content = md.render(plainText) // get content
	
	let params = {
		randCode : randCode,
		data : {
			content : content,
			articleId : current.articleId,
			commentId : current.commentId
		}
	}
	$.showLoading("正在发表评论")
	ajax("POST", contextPath + "/api/comments", JSON.stringify(params), "json", function(data) {
		if(data.success) {
			removeStorage("current")
			removeStorage("comment")
			$.toast(data.message)
		} else {
			if (data.code == 401) {
				$.toast("登录超时","cancel")
			} else {
				$.toast(data.message,"cancel")
			}
		}
		getRandCode()
	}, function() {
		$.toast("服务器繁忙稍后重试","cancel")
		getRandCode()
	})
}

// add a scroll event
$(window).scroll(function() {
	comments()
})

function goto(type, page) {
	loadComment(page)
}

function appendDOM(html, id){
	$("#"+id).append(html)
}

function createDOM(html, id){
	$("#"+id).html(html)
}

function getPaginationHtml(type,size,pageCount, pageNow){
//	var pageCount=(totalNum%6)==0?(totalNum/6):(Math.ceil(totalNum/6))
	var html ="<div align='center' id='pagination'>"
			+	"<ul class='pagination' style='margin-top: 10px'>"
	html	+=	"<li>"
			+ 		"<a>每页" + size + "条记录</a>"
			+ 	"</li>"
			+ 	"<li class='disabled'>"
			+ 		"<a>"+pageNow+"/"+pageCount+"</a>"
			+ 	"</li>"
	//首页按钮
	if(pageNow==1){
		html+=	"<li class='disabled'>"
			+ 		"<a class='disabled'>首页</a>"
			+ 	"</li>"
	}else{
		html+=	"<li>"
			+ 		"<a href=\"javascript:goto('"+type+"',1)\">首页</a>"
			+ 	"</li>"
	}
	
	if(pageNow>1){
		html+=	"<li><a href=\"javascript:goto(\'"+type+"\',"+(pageNow-1)+")\"><<</a></li>"			
	}else{
		html+= "<li class='disabled'>"
			+ 		"<a><<</a>"
			+ 	"</li>"
	}
	//小于5
	if(pageCount <= 5) {
		for (var i = 1; i < pageCount + 1; i++) {
			html+=	"<li"+(pageNow==i?" class='active'":"")+">"
				+ 		"<a href=\"javascript:goto('"+type+"',"+i+")\">"+i+"</a>" 
				+	"</li>"
		}
	}else if(pageCount>5&&pageNow<=2){
		html+=	"<li"+(pageNow==1?" class='active'":"")+">"
			+		"<a href=\"javascript:goto('"+type+"',1)\">1</a>"
			+ 	"</li>"
			+ 	"<li"+(pageNow==2?" class='active'":"")+">"
			+ 		"<a href=\"javascript:goto('"+type+"',2)\">2</a>"
			+	"</li>"
			+ 	"<li>"
			+ 		"<a href=\"javascript:goto('"+type+"',3)\">3</a>" 
			+ 	"</li>"
			+ 	"<li>"
			+ 		"<a href=\"javascript:goto('"+type+"',4)\">4</a>" 
			+ 	"</li>"
			+ 	"<li>"
			+ 		"<a href=\"javascript:goto('"+type+"',5)\">5</a>" 
			+ 	"</li>"
	}else if(pageNow<pageCount-2){
		html+=	"<li>"
			+ 		"<a href=\"javascript:goto('"+type+"',"+(pageNow-2)+")\">"+(pageNow-2)+"</a>" 
			+ 	"</li>"
			+ 	"<li>"
			+ 		"<a href=\"javascript:goto('"+type+"',"+(pageNow-1)+")\">"+(pageNow-1)+"</a>" 
			+ 	"</li>"
			+ 	"<li class='active'>"
			+ 		"<a href=\"javascript:goto('"+type+"',"+(pageNow)+")\">"+(pageNow)+"</a>" 
			+ 	"</li>"
			+ 	"<li>"
			+ 		"<a href=\"javascript:goto('"+type+"',"+(pageNow+1)+")\">"+(pageNow+1)+"</a>" 
			+ 	"</li>"
			+ 	"<li>"
			+ 		"<a href=\"javascript:goto('"+type+"',"+(pageNow+2)+")\">"+(pageNow+2)+"</a>" 
			+ 	"</li>"
	}else if(pageNow>=pageCount-2){
		html+=	"<li>"
			+		"<a href=\"javascript:goto('"+type+"',"+(pageCount-4)+")\">"+(pageCount-4)+"</a>"
			+ 	"</li>"
			+ 	"<li>"
			+ 		"<a href=\"javascript:goto('"+type+"',"+(pageCount-3)+")\">"+(pageCount-3)+"</a>" 
			+ 	"</li>"
			+ 	"<li"+((pageNow==pageCount-2)?" class='active'":"")+">" 
			+ 		"<a href=\"javascript:goto('"+type+"',"+(pageCount-2)+")\">"+(pageCount-2)+"</a>" 
			+ 	"</li>"
			+ 	"<li"+((pageNow==pageCount-1)?" class='active'":"")+">" 
			+ 		"<a href=\"javascript:goto('"+type+"',"+(pageCount-1)+")\">"+(pageCount-1)+"</a>" 
			+ 	"</li>"
			+ 	"<li"+(pageNow==pageCount?" class='active'":"")+">" 
			+ 		"<a href=\"javascript:goto('"+type+"',"+(pageCount)+")\">"+(pageCount)+"</a>" 
			+ 	"</li>"
	}
	if(pageNow==pageCount){
		html+=	"<li class='disabled'>"
			+ 		"<a>>></a>"
			+ 	"</li>"
	}else{//"+(pageNow==pageCount?" class='disabled'":"")+"
		html+=	"<li>" 
			+		"<a href=\"javascript:goto('"+type+"',"+(pageNow+1)+")\">>></a>"
			+ 	"</li>"
	}
	if(pageNow==pageCount){
		html+=	"<li class='disabled'>" 
			+ 		"<a class='disabled'>尾页</a>" 
			+ 	"</li>"
	}else{
		html+=	"<li>"
			+		"<a href=\"javascript:goto('"+type+"',"+pageCount+")\">尾页</a>"
			+ 	"</li>"
	}
	return html+"</ul></div>"
}
