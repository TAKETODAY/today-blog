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
});


var Article = {
	id : null,
	title : null,
	image : null,
	summary: null,
	content: null,
	markdown: null,
	category: null,
	labels:[]
}

$(function () {
    inlineAttachment.editors.codemirror4.attach(easyMDE.codemirror, {
        uploadUrl: contextPath + "/api/attachments",
        urlText: function(url, result) {
//        	return "!["+result.data.alt+"]("+result.data.url+")"
            return '<img src="' + contextPath+ '/assets/images/loading.gif" alt=' + result.data.alt + ' data-original="' + result.data.url + ' data-action="zoom">'
        }
    });
});

function attachAction(editor) {
    var input = document.createElement("input")
    input.setAttribute("id","attach")
    layer.open({
        type: 2,
        title: "选择附件",
        shadeClose: true,
        shade: 0.5,
        maxmin: true,
        area: ['90%', '90%'],
        content: contextPath + '/admin/attachments/choose?id=attach&callback=attachCallback',
        scrollbar: false
    });
}

//browser without AMD, added to "window" on script load
//Note, there is no dash in "markdownit".
var md = window.markdownit({
	html: true,
	linkify: true,
	typographer: true,
	highlight: function (str, lang) {
		if (lang && hljs.getLanguage(lang)) {
			try {
				return hljs.highlight(lang, str).value;
			} catch (__) {}
    	}
    	return ''; // use external default escaping
	}
	
}).use(window.markdownitEmoji)

//var result = md.render('# markdown-it rulezz!');

var easyMDE = new EasyMDE({
    element: document.getElementById("editorarea"),
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
        Article.content = md.render(plainText)
        
        preview.setAttribute('id','editor-preview')
        
        preview.innerHTML = Article.content
        return Article.content
    },
    showIcons: ["code", "table","horizontal-rule"],
    status: ["autosave", "lines", "words"],
    tabSize: 4,
    promptURLs:true,
    toolbar: ["bold","italic","strikethrough","heading","|","code","quote","unordered-list","ordered-list","|",
              "link" ,"image", {
                name: "custom",
                action: attachAction,
                className: "fa fa-upload",
                title: "选择附件",
            },"|", "table","horizontal-rule","|","guide","undo","redo","|","preview","side-by-side","fullscreen","|",{
                title: "保存",
                name: "updateArticle",
                action: updateArticle,
                className: "fa fa-save",
            }
        ]
});

easyMDE.codemirror.on("change", function(){
    saveToLocal()
})
    
/**
 * callback for choose attachment
 * 
 * @param url
 * 			target selected url
 * @returns
 */
function attachCallback(url) {
    var cm = easyMDE.codemirror;
    var stat = easyMDE.getState(cm);
    var options = easyMDE.options;
    var startPoint = {},endPoint = {};
    Object.assign(startPoint, cm.getCursor('start'));
    Object.assign(endPoint, cm.getCursor('end'));
    
//    cm.replaceSelection("![]("+url+")");
    cm.replaceSelection('<img src="'+ contextPath+ '/assets/images/loading.gif" data-original="'+url+'" data-action="zoom">')

    cm.setSelection(startPoint, endPoint);
    cm.focus();
}

function saveToLocal() {
	Article.markdown = easyMDE.value()
	Article.title = $("#title").val()
	saveStorage("article_modify_md", Article)
}

var modify = location.pathname.indexOf("modify") > -1
var currentArticleId = undefined
if(modify) {
	let split = location.pathname.split("/")
	currentArticleId = split[split.length - 2]
	Article.id = currentArticleId;
	let article_modify = getStorage("article_modify_md")
	if(article_modify != null && article_modify.id == currentArticleId) {
		Article = article_modify
		full()
	} else {
		ajax("GET", contextPath + "/api/articles/"+ currentArticleId ,null, null, function(data) {
			if(data.success) {
				let article = data.data
				
				Article.title = article.title
				Article.image = article.image
				Article.labels = article.labels
				Article.summary = article.summary
				Article.content = article.content
				Article.markdown = article.markdown
				Article.category = article.category
				Article.password = article.password
				
				saveStorage("article_modify_md", Article)
				full()
			} else {
				if(data.code == 401) {
					$.toast("登录超时","cancel")
					setInterval(function () {
						location = location
					},3000)
					return
				}
				$.toast(data.message,"cancel")
			}
		})
	}
}

function full() {
	if (Article.markdown == undefined || Article.markdown == null) {//?markdown=false
		location = location + "?markdown=false"
	}
	$.showLoading("正在加载")
	$("#title").val(Article.title)
	createImage(Article.image)
	setTimeout(() => {
		try {
			easyMDE.value(Article.markdown)
			$.hideLoading()
		}catch(e) {
			console.log(e)
			$.hideLoading()
			$.toast("加载出错","cancel")
		}
	}, 50)
	getTags()
}

function update(){
	$.showLoading("正在更改")
	ajax("PUT", contextPath + "/api/articles/" + currentArticleId, Article, "json", function(data) {
		if(data.success){
			removeStorage("article_modify_md")
			$.toast(data.message)
		} else {
			if(data.code == 401) {
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
function getSummary(str) {
	str = str.replace(/<\/?[^>]+>/g, ''); // 去除HTML tag
	str = str.replace(/(\n)/g, '');
	str = str.replace(/(\t)/g, '');
	str = str.replace(/(\r)/g, '');
	str = str.replace(/&nbsp;/ig, ''); // 去掉&nbsp;
	return str.length > 256 ? str.substring(0,256) : str;
}

function savePassword(text) {
	Article.password = text
	saveToLocal()
	chooseCategory(function() {
		let markdown = easyMDE.value() 

		Article.content = md.render(markdown) //to html
		Article.summary = getSummary(Article.content)
//		encodeURIComponent(markdown)
		Article.markdown = markdown
		
		saveToLocal()
		update()
	})
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

function updateArticle() {
	if(Article.title==null||Article.title.trim()==""){
		$.toast("文章题目不能为空","cancel")
		return false
	}
	if(Article.category==null||Article.category.trim()==""){
		$.toast("文章类型不能为空","cancel");
		return false
	}
	
	inputPassword()
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
		saveStorage("article_modify_md", Article)
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
	
	let article_modify = getStorage("article_modify_md")
	if(article_modify != null) {
		let labels = article_modify.labels
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

