function ajax(method, url, data, success, error) {
	NProgress.start();
	let xhr = new XMLHttpRequest()
	xhr.open(method, url)
	xhr.onload = function() {
		NProgress.done();
		$.hideLoading()
		let result = JSON.parse(xhr.responseText)
		if(xhr.status == 200) {
			success(result)
		} else if (xhr.status == 401) {
			$.toast("登录超时", "cancel");
			setTimeout(() => {
				location = location;
			}, 2000);
		} else {
			if (error != null) {
				error(result)
			} else {
				$.toast("服务器繁忙","cancel");
			}
		}
	}
	xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xhr.send(data)
}

layui.use(['layer', 'form'], function(){
 var $ = layui.jquery
  ,layer = layui.layer
  ,form = layui.form
  ,laypage = layui.laypage;
  
  // 触发事件
  var active = {
    test: function(){
      layer.open({
        type: 1,
        title:'填写表单',
        offset: ['30%', '38%'],
        resize: false,
        content: [
        '<ul class="layui-form" style="margin: 10px;">',
            '<li class="layui-form-item">',
                '<label for="keepNavigation">保留导航栏</label>',
                '<select class="form-control" id="keepNavigation" name="keepNavigation">',
                    '<option value="true" <#if post.keepNavigation>selected</#if>>保留</option>',
                    '<option value="false" <#if !post.keepNavigation>selected</#if>>不保留</option>',
                '<select>',
            '</li>',
            '<li class="layui-form-item" style="text-align:center;">',
                '<button type="submit" lay-submit lay-filter="*" class="layui-btn">提交</button>',
            '</li>',
        '</ul>'].join('')
        ,success: function(layero){
          layero.find('.layui-layer-content').css('overflow', 'visible');
          form.render().on('submit(*)', function(data){
            layer.message(JSON.stringify(data.field));
          });
        }
      });
    }
  };
  $('.savePost').on('click', function(){
    var type = $(this).data('type');
    active[type] ? active[type].call(this) : '';
  });
});
  
    MathJax.Hub.Config({
        showProcessingMessages: false,
        messageStyle: "none",
        tex2jax: {
            inlineMath: [ ['$','$'], ["\\(","\\)"] ],
            displayMath: [ ['$$','$$'], ["\\[","\\]"] ],
            skipTags: ['script', 'noscript', 'style', 'textarea', 'pre','code','a']
        }
    });

    var QUEUE = MathJax.Hub.queue;

    /**
	 * 加载编辑器
	 */
    var easyMDE = new EasyMDE({
        element: document.getElementById("editorarea"),
        autoDownloadFontAwesome: false,
        autofocus: true,
        autosave: {
            enabled: true,
            uniqueId: "editor-temp-page-<#if post??>${post.id}<#else>1</#if>",
            delay: 10000
        },
        renderingConfig: {
            codeSyntaxHighlighting: true
        },
        previewRender: function(plainText) {
            var preview = document.getElementsByClassName("editor-preview-side")[0];
            preview.innerHTML = this.parent.markdown(plainText);
            preview.setAttribute('id','editor-preview');
            MathJax.Hub.Queue(["Typeset",MathJax.Hub,"editor-preview"]);
            return preview.innerHTML;
        },
        showIcons: ["code", "table"],
        status: ["autosave", "lines", "words"],
        tabSize: 4
    });

    /**
	 * 方法来自https://gitee.com/supperzh/zb-blog/blob/master/src/main/resources/templates/article/publish.html#L255
	 */
    $(function () {
        inlineAttachment.editors.codemirror4.attach(easyMDE.codemirror, {
            uploadUrl: "${contextPath}/admin/attachments/upload"
        });
    });
    
    function UrlOnBlurAuto() {
        var newPostUrl = $('#newPostUrl');
        if(newPostUrl.val()===""){
            halo.showMsg("<@message code='admin.editor.js.no-url' />",'info',2000);
            return;
        }
        $.get('${contextPath}/admin/posts/checkUrl',{'postUrl': newPostUrl.val()},function (data) {
            if(data.code === 0){
                halo.showMsg(data.message,'error',2000);
                return;
            }else{
                $('#postUrl').html(newPostUrl.val());
                $('#btn_change_postUrl').hide();
                $('#btn_input_postUrl').show();
            }
        },'JSON');
    }
    $('#btn_input_postUrl').click(function () {
        $('#postUrl').html("<input type='text' id='newPostUrl' onblur='UrlOnBlurAuto()' value=''>");
        $(this).hide();
        $('#btn_change_postUrl').show();
    });
    
    function push(status) {
        var postTitle = $("#postTitle");
        var postUrl = $("#postUrl");
        if(!postTitle.val()) {
            halo.showMsg("<@message code='admin.editor.js.no-title' />",'info',2000);
            return;
        }
        if(!postUrl.html()){
            halo.showMsg("<@message code='admin.editor.js.no-url' />",'info',2000);
            return;
        }
    
        $.post('${contextPath}/admin/posts/new/push',{
            'title': postTitle.val(),
            'url' : postUrl.html().toString(),
            'markdown': easyMDE.value()
        },function (data) {
            if(data.code===1){
                // 清除自动保存的内容
                easyMDE.toTextArea();
                easyMDE = null;
                halo.showMsgAndRedirect(data.message,'success',1000,location,"true}");
            }else{
                halo.showMsg(data.message,'error',2000);
            }
        },'JSON');
    }

    function removeThumbnail(){
        $("#selectImg").attr("src","${contextPath}/static/halo-frontend/images/thumbnail/thumbnail.png");
    }




