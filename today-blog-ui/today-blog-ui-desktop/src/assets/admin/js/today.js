$.fn.extend({
	animateCss : function(animationName, callback) {
		var animationEnd = (function(el) {
			var animations = {
				animation : 'animationend',
				OAnimation : 'oAnimationEnd',
				MozAnimation : 'mozAnimationEnd',
				WebkitAnimation : 'webkitAnimationEnd'
			};
			for ( var t in animations) {
				if (el.style[t] !== undefined) {
					return animations[t];
				}
			}
		})(document.createElement('div'));

		this.addClass('animated ' + animationName).one(animationEnd, function() {
			$(this).removeClass('animated ' + animationName);
			if (typeof callback === 'function')
				callback();
		});
		return this;
	}
})

/**
 * 适配移动端并初始化菜单
 */
$(document).ready(function() {
	try{
		$("#animated-header,#animated-content").animateCss("fadeIn");
	}catch(e) {}
	if ($(window).width() < 1024) {
		if ($('body').hasClass('layout-boxed')) {
			$('body').removeClass('layout-boxed');
		}
		if ($('body').hasClass('sidebar-collapse')) {
			$('body').removeClass('sidebar-collapse');
		}
	}
	initMenu();
});

document.addEventListener('pjax:complete', function() {
	try{
		$("#animated-header,#animated-content").animateCss("fadeIn");
	}catch(e) {}
	initMenu();
});

function initMenu() {
	var pathName = location.pathname
	$(".sidebar-menu").children().each(function() {
		var li = $(this)
		li.find('a').each(function() {
			var href = $(this).attr("href")
			if (pathName === href) {
				li.addClass("active")
				$(this).parent().addClass("active")
			} else {
				$(this).parent().removeClass("active")
			}
		})
	})
}

function showMsgReload(message, type) {
	showMsg(message, type, function() {
		location = location
	})
}

function confirm(content, d) {
	layer.confirm(content, {
		icon : 3,
		title : "系统提示",
		btn : [ '确认', '取消' ],
		btnclass : [ 'btn btn-primary', 'btn btn-danger' ],
	}, function(index) {
		layer.close(index);
		d(true);
	});
}

function showMsg(message, type, callback) {
	if (type == "error") {
		type = "cancel"
	}
	$.toast(message, type, callback)
}

function showMsgRefresh(message, type) {
	if (typeof type == "function") {
		showMsg(message, type)
	} else {
		showMsg(message, type, function() {
			pjax.loadUrl(location)
		})
	}
}

function showMsgParentRefresh(message, type) {
	showMsg(message, type, function() {
		parent.pjax.loadUrl(parent.location)
	})
}

function formatContent(a) {
	a = a.replace(/\r\n/g, '<br/>');
	a = a.replace(/\n/g, '<br/>');
	a = a.replace(/\s/g, ' ');
	return a;
}

function loadModal(url, title) {
	layer.open({
		type : 2,
		title : title,
		shadeClose : true,
		shade : 0.5,
		maxmin : true,
		area : [ '90%', '90%' ],
		content : url,
		scrollbar : false
	})
}

