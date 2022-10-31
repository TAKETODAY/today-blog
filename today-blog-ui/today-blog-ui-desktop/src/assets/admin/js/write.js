
// 对Date的扩展，将 Date 转化为指定格式的String
// 月(M)、日(d)、小时(h)、分(m)、秒(s)、季度(q) 可以用 1-2 个占位符，
// 年(y)可以用 1-4 个占位符，毫秒(S)只能用 1 个占位符(是 1-3 位的数字)
// 例子：
// (new Date()).Format("yyyy-MM-dd hh:mm:ss.S") ==> 2006-07-02 08:09:04.423
// (new Date()).Format("yyyy-M-d h:m:s.S") ==> 2006-7-2 8:9:4.18
SyntaxHighlighter.all();
// 保存数据
function saveSession(key, obj){
	var str = JSON.stringify(obj); // 将对象转换为字符串
	sessionStorage.setItem(key,str);  
}
// 查找数据
function getSession(key){
	return JSON.parse(sessionStorage.getItem(key));
}
function clearSession(){
	sessionStorage.clear();
	localStorage.clear();
	$.toast("清除成功");
}
function removeSession(key){
	if(getSession(key) != null){
		sessionStorage.removeItem(key);
		return true;
	}
	return false;
}
// 保存数据
function saveStorage(key, obj){
	var str = JSON.stringify(obj); // 将对象转换为字符串
	localStorage.setItem(key,str);  
}
// 查找数据
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

var layer = layui.layer
var form
var laypage
layui.use(['layer', 'form'], function(){
    form = layui.form
    laypage = layui.laypage
    layer = layui.layer
})

var Article = {
	id	 : null,
	title : null,
	image : null,
	summary: null,
	content: null,
	category: null,
	password: null,
	labels:[]
}

UE.getEditor('editor') // for init editor

var localArticle = getStorage("article");
if(localArticle != null) {
	Article = localArticle
	full()
}

function full() {
	$.showLoading("正在加载")
	$("#title").val(Article.title)
	createImage(Article.image)
	setTimeout(() => {
		try {
			if (Article.content != null) {
				UE.getEditor('editor').setContent(Article.content, false, true)
			}
			$.hideLoading()
		}catch(e) {
			console.log(e)
			$.hideLoading()
			$.toast("加载出错","cancel")
		}
	}, 1000)
	getTags()
}

function saveToLocal() {
	Article.content = UE.getEditor('editor').getContent()
	Article.title= $("#title").val()
	saveStorage("article", Article)
}

function saveArticle(){
	$.modal({
		title: "给文章分类",
		text: "如果没有匹配的类别，可以创建<br/>" +
				"<select name='type' id='articleType' class='form-control' title='文章类别'>"+
					"<option value='0'>选择类别</option>"+
				"</select>",
		buttons: [
			{ text: "取消", className: "default"},
			{ text: "新建类别", onClick: function(){
					createType()
				}
			},
			{ text: "下一步", onClick: function(){
					Article.category = $("#articleType").val()
					saveToLocal()
					inputPassword()
				}
			},
		]
	});
	var data = getSession("articleType")
	if(data == null) {
		ajax("GET", contextPath+"/api/categories",null, null,function(data){
			if(data.success){
				createCategoryDom(data.data)
				saveSession("articleType",data)
				return
			}
			$.toast(data.message,"cancel")
		})
		return
	}
	createCategoryDom(data.data)
}

function savePassword(text) {
	Article.password = text
	saveToLocal()
	chooseCopyright()
}

function inputPassword() {
	$.prompt({
		title: "访问密码",
		text: "请输入文章访问密码",
        onOK: savePassword,
        onCancel: savePassword,
        input: Article.password
	});
}

//code:检查是否选择了类别
function chooseCopyright() {
	if(Article.category == null || Article.category == ""){
		$.toast("请选择类别!","cancel",function(){
			saveArticle()
		})
		return
	}
	$.modal({
		title: "关于文章版权",
		text: "文章是否原创？<br/>",
		buttons: [
			{ text: "原创", onClick: function(){
					$.confirm("您确定要发布文章吗?", "最后一步", function() {
						Article.copyRight = "版权声明：本文为作者原创文章，转载时请务必声明出处并添加指向此页面的链接。"
						saveToLocal()
						check()
			        }, function() {
			        	$.toast("取消发布","cancel")
			        });
				}
			},
			{ text: "转载", onClick: function(){ 
					reprintArticle(function(copyRight){
						Article.copyRight = "本文转载自：" + copyRight
						saveToLocal()
						check()
					});
				}
			},
		]
	});
}
// 文章来源
function reprintArticle(callback){
	$.prompt({
		title: "文章来源",
		text: "请输入文章地址",
        onOK: function(text) {
        	callback(text)
        },
        onCancel: function() {
        	chooseCopyright()
        },
        input: 'http://'
	});
}

function getSummary(str) {
	str = str.replace(/<\/?[^>]+>/g, ''); // 去除HTML tag
	str = str.replace(/(\n)/g, '');
	str = str.replace(/(\t)/g, '');
	str = str.replace(/(\r)/g, '');
	str = str.replace(/&nbsp;/ig, ''); // 去掉&nbsp;
	return str.length > 256 ? str.substring(0,256) : str;
}

/**
 * @returns
 */
function chooseCategory(success){
	$.modal({
		title: "给文章分类",
		text: "如果没有匹配的类别，可以创建<br/>" +
				"<select name='type' id='articleType' class='form-control' title='文章类别'>"+
					"<option value='0'>选择类别</option>"+
				"</select>",
		buttons: [
			{ text: "取消", className: "default"},
			{ text: "新建类别", onClick: function() {
					createType(success)
				}
			},
			{ text: "更新", onClick: function(){
					let category = $("#articleType").val()
					if (Article.category != category) {
						Article.category = category
					}
					success();
				}
			},
		]
	});
	var data = getSession("articleType")
	if(data == null) {
		ajax("GET", contextPath+"/api/categories", null, null, function(data){
			if(data.success){
				createCategoryDom(data.data)
				saveSession("articleType",data)
				return
			}
			$.toast(data.message, "cancel")
		})
		return
	}
	createCategoryDom(data.data)
}

function createCategoryDom(data){
	var html = ""
	$.each(data, function (index, type) {
		if (Article.category===type.name) {
			html +="<option value='"+type.name+"' selected>"+type.name+"</option>"
		} else {
			html +="<option value='"+type.name+"'>"+type.name+"</option>"
		}
	})
	$("#articleType").html(html)
}

// 创建新文章类型
function createType(success){
	$.prompt({
		title: "创建新类型",
		text: "请输入新类型，不要超过10个字符！",
        onOK: function(text) {
        	// 请求保存
        	saveType(text, success);
        },
        onCancel: function() {
        	chooseCategory(success);
        }
	});
}

function saveType(name) {
	$.showLoading("正在保存")
	ajax("POST", contextPath + "/api/categories/" + name, "list=true", null, function(data) {
		if(data.success) {
			$.toast(data.message)
			saveSession("articleType", data)
			chooseCategory(success)
		} else {
			$.toast(data.message, "cancel")
			setTimeout(() => {
				chooseCategory(success)
			}, 2000);
		}
	})
}

function check(){
	if(Article.title==null||Article.title.trim()==""){
		$.toast("文章题目不能为空","cancel")
		return false
	}
	if(Article.category==null||Article.category.trim()==""){
		$.toast("文章类型不能为空","cancel");
		return false
	}
	
	Article.content = UE.getEditor('editor').getContent()
	Article.summary = getSummary(Article.content)
	
	saveToLocal()
	save()
}

function save() {
	$.showLoading("正在发布")
	ajax("POST", contextPath + "/api/articles", Article, "json", function(data) {
		if(data.success) {
			removeStorage("article")
			$.toast(data.message)
		} else {
			if(data.code == 401){
				$.toast("登录超时","cancel")
				setInterval(function () {
					location = location
				},3000)
				return
			}
			$.toast(data.message,"cancel")
		}
	}, function(data){
		$.toast(data.message,"cancel")
	})
}


/////////////////////////////////////////////////////////////////

function attachImage() {
    var input = document.createElement("input")
    input.setAttribute("id","attachImage")
    layer.open({
        type: 2,
        title: "选择题图",
        shadeClose: true,
        shade: 0.5,
        maxmin: true,
        area: ['90%', '90%'],
        content: contextPath + '/admin/attachments/choose?id=attachImage&callback=createImage',
        scrollbar: false
    });
}

var UploadPicture_wrapper;

function createImage(data){
	if(data != null) {
		Article.image = data
		saveStorage("article", Article)
		if(UploadPicture_wrapper == null) {
			UploadPicture_wrapper = $(".WriteCover-previewWrapper").html()
		}
		let html = "<img src="+data+">"
		$(".WriteCover-previewWrapper").html(html)
	}
}

function deleteImage(){
	$(".WriteCover-previewWrapper").html(UploadPicture_wrapper)
	$("#uploadImage").change(function (){
		upload()
	})
	Article.image = null
	saveToLocal()
}

function Label(){
	var id = null
}

function getTags(){
	var tags = getSession("tags")
	if(tags != null){
		createTagDOM(tags)
		return;
	}
	ajax("GET", contextPath+"/api/tags", null, null, function(data) {
		saveSession("tags",data)
		createTagDOM(data)
	})
}

function createTagDOM(tags) {
	var html = "";
	$.each(tags.data, function (index, tag) {		
		html +="<option value='"+tag.id+"'>"+tag.name+"</option>";
	});
	createDOM(html, "tagSelect");
	$('#tagSelect').selectpicker({
		maxOptions : 8
	});
	
	let article = getStorage("article")
	if(article != null) {
		let labels = article.labels
		let values = new Set()
		if(labels.length != 0) {
			let children = document.querySelectorAll("#selectItem li")
			for (let li of children) {
				let listName = li.querySelector(".text").innerHTML
//				console.log("listName: " + listName)
				labels.forEach(label => {
					let labelName = getLabelName(label.id)
					if(labelName != null) {
						if(listName == labelName) {
							values.add(label.id)
						}
					}
				})
			}
			$('#tagSelect').selectpicker('val', Array.from(values))
		}
	}
	$("span.bs-caret").remove();
	$("button").removeClass("btn-default");// dropdown-toggle
	$("#selectButton,#saveArticleBtn,button.actions-btn").addClass("opposite");
}

function getLabelName(id) {
	
	let tags = getSession("tags")
	if(tags == null || (tags = tags.data).length == 0) {
		return null// tags == null
	}
	for(let tag in tags) {
		let id_ = tags[tag].id
//		console.log("TODAY BLOG","id: "+ id_ + "ID: "+ id)
		if(id_ == id) {
			return tags[tag].name
		}
	}
	return null
}

function selected(li) {
	let labels = $('#tagSelect').selectpicker('val')
	Article.labels = new Array(labels.length);
	for(var i = 0; i < labels.length;i++){
		let label = new Label()
		label.id = Number(labels[i])
		Article.labels[i] = label
	}
//	console.log(labels)
	saveToLocal()
}

function getLabelId(name) {
	let tags = getSession("tags")
	if(tags == null || (tags = tags.data).length == 0) {
		return null// tags == null
	}
	for(let tag in tags) {
		let name_ = tags[tag].name
		if(name_ == name) {
			return tags[tag].id
		}
	}
	return null
}

function saveTag() {
	let tag = $("#tagInput").val()
	if(tag == null || tag.trim() == "" ){
		$.toast("标签不能为空","cancel")
		return
	}
	
	ajax("POST", contextPath + "/api/tags/" + tag, null, null, function(data) {
		if(data.success) {
			$("#tagSelect").append("<option value='"+data.data.id+"'>"+data.data.name+"</option>");
			$('#tagSelect').selectpicker('refresh');
			removeSession("tags");
			$.toast(data.message)
		} else {
			$.toast(data.message,"cancel")
		}
	})
}

/**
 * input title image
 * @returns the image url
 */
function inputImage(){
	$.prompt({
		title: "文章题图地址",
		text: "请输入文章题图地址",
        onOK: function(text) {
        	createImage(text)
        },
        onCancel: function() {
        	$.toast("取消输入","cancel")
        },
        input: 'http://'
	});
}

function appendDOM(html, id){
	$("#"+id).append(html);
}
function createDOM(html, id){
	$("#"+id).html(html);
}

