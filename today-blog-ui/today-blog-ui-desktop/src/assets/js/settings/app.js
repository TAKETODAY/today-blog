var avatarCropper;
$(function () {
	(function () {
		var $image = $('#avatarImage>img'),
			$dataX = $('#dataX'),
			$dataY = $('#dataY'),
			$dataHeight = $('#dataHeight'),
			$dataWidth = $('#dataWidth'),
			$dataRotate = $('#dataRotate'),
			options = {
				aspectRatio: 1 / 1,
				preview: '.avatar',
				crop: function (data) {
					$dataX.val(Math.round(data.x));
					$dataY.val(Math.round(data.y));
					$dataHeight.val(Math.round(data.height));
					$dataWidth.val(Math.round(data.width));
					$dataRotate.val(Math.round(data.rotate));
				}
			};
		$image.cropper(options);
		avatarCropper = $image.data('cropper');
		// Import image
		var $inputImage = $('#inputAvatarImage'), URL = window.URL || window.webkitURL, blobURL;
		if (URL) {
			$inputImage.change(function () {
				var files = this.files, file;
				if (files && files.length) {
					file = files[0];
					if (/^image\/\w+$/.test(file.type)) {
						blobURL = URL.createObjectURL(file);
						$image.one('built.cropper', function () {
							URL.revokeObjectURL(blobURL); // Revoke when load complete
						}).cropper('reset', true).cropper('replace', blobURL);
						$inputImage.val('');
					} else {
						showMessage('Please choose an image file.');
					}
				}
			});
		} else {
			$inputImage.parent().remove();
		}
	}());
});

// background
var backgroundCropper;
$(function () {
	(function () {
		var $image = $('#backImage>img'), $dataX = $('#dataX'), $dataY = $('#dataY'), $dataHeight = $('#dataHeight'), $dataWidth = $('#dataWidth'), $dataRotate = $('#dataRotate'), options = {
			aspectRatio: 1200 / 600,
			preview: '.backImage',
			crop: function (data) {
				$dataX.val(Math.round(data.x));
				$dataY.val(Math.round(data.y));
				$dataHeight.val(Math.round(data.height));
				$dataWidth.val(Math.round(data.width));
				$dataRotate.val(Math.round(data.rotate));
			}
		};
		$image.cropper(options);
		backgroundCropper = $image.data('cropper');
		// Import image
		var $inputImage = $('#inputBackImage'), URL = window.URL || window.webkitURL, blobURL;
		if (URL) {
			$inputImage.change(function () {
				var files = this.files, file;
				if (files && files.length) {
					file = files[0];
					if (/^image\/\w+$/.test(file.type)) {
						blobURL = URL.createObjectURL(file);
						$image.one('built.cropper', function () {
							URL.revokeObjectURL(blobURL); // Revoke when load
							// complete
						}).cropper('reset', true).cropper('replace', blobURL);
						$inputImage.val('');
					} else {
						showMessage('Please choose an image file.');
					}
				}
			});
		} else {
			$inputImage.parent().remove();
		}
	}());
});

// 保存数据
function saveSession(key, obj) {
	var str = JSON.stringify(obj); // 将对象转换为字符串
	sessionStorage.setItem(key, str);
}
// 查找数据
function getSession(key) {
	return JSON.parse(sessionStorage.getItem(key));
}
function clearSession() {
	sessionStorage.clear();
	localStorage.clear();
	$.toast("清除成功");
}
function removeSession(key) {
	if (getSession(key) != null) {
		sessionStorage.removeItem(key);
		return true;
	}
	return false;
}
// 保存数据
function saveStorage(key, obj) {
	var str = JSON.stringify(obj); // 将对象转换为字符串
	localStorage.setItem(key, str);
}
// 查找数据
function getStorage(key) {
	return JSON.parse(localStorage.getItem(key));
}
function removeStorage(key) {
	if (getStorage(key) != null) {
		localStorage.removeItem(key);
		return true;
	}
	return false;
}

function saveBackground(btn) {
	NProgress.start();
	let subBtn = $(btn)
	//	console.log(subBtn)
	subBtn.text("正在修改 ...")
	try {
		backgroundCropper.getCroppedCanvas().toBlob((blob) => {
			const formData = new FormData()
			formData.append('background', blob)
			$.ajax({
				"url": contextPath + "/settings/background",
				"data": formData,
				"type": "POST",
				"dataType": "json",
				"contentType": false,
				"processData": false,
				"success": function (data) {
					NProgress.done();
					subBtn.text("保存背景")
					if (data.success) {
						$.toast(data.message)
						reload()
					} else {
						$.toast(data.message, 'cancel')
						if (data.code == 401) {
							reload()
						}
					}
				},
				"error": function (data) {
					NProgress.done();
					if (data.status == 401) {
						$.toast("登录超时", "cancel")
						reload()
					} else {
						$.toast("服务器繁忙", 'cancel')
						subBtn.text("保存背景")
					}
				}
			})
		})
	} catch (e) {
		$.toast("您的浏览器不支持当前操作", 'cancel')
		reload()
	}
}

function saveAvatar(btn) {
	NProgress.start();
	let subBtn = $(btn)
	subBtn.text("正在修改 ...")
	try {
		avatarCropper.getCroppedCanvas().toBlob((blob) => {
			const formData = new FormData()
			formData.append('image', blob)
			$.ajax({
				"url": contextPath + "/settings/avatar",
				"data": formData,
				"type": "POST",
				"dataType": "json",
				"contentType": false,
				"processData": false,
				"success": function (data) {
					NProgress.done();
					subBtn.text("保存头像")
					if (data.success) {
						$.toast(data.message)
						reload()
					} else {
						$.toast(data.message, 'cancel')
						if (data.code == 401) {
							reload()
						}
					}
				},
				"error": function (data) {
					NProgress.done();
					if (data.status == 401) {
						$.toast("登录超时", "cancel")
						reload()
					} else {
						$.toast("服务器繁忙", 'cancel')
						subBtn.text("保存头像")
					}
				}
			})
		})
	} catch (e) {
		$.toast("您的浏览器不支持当前操作", 'cancel')
		reload()
	}
}

/**
 * @param btn
 * @returns
 */
function updateInfo() {
	let name = document.getElementById("name")
	if (name.value == null || name.value.length == 0) {
		showMsg("姓名不能为空", "infoErrorField", name)
		return false
	}
	document.getElementById("infoErrorField").setAttribute("hidden", "")
	Ajax(contextPath + "/settings", new FormData(document.querySelector('#infoForm')), "PUT")
	return false
}

/**
 * @returns
 */
function updateEmail() {
	let email = document.getElementById("inputEmail")
	let passwd = document.getElementById("inputPassword")

	if (passwd.value == null || passwd.value.length == 0) {
		showMsg("密码不能为空", "emailErrorField", passwd)
		return false
	}
	if (email.value == null || email.value.length == 0) {
		showMsg("邮箱不能为空", "emailErrorField", email)
		return false
	}
	document.getElementById("emailErrorField").setAttribute("hidden", "")
	Ajax(contextPath + "/settings/email", new FormData(document.querySelector('#emailForm')), "PUT")
	return false
}

function updatePassword() {
	let repasswd = document.getElementById("rePassword")
	let prepasswd = document.getElementById("prePassword")
	let passwd = document.getElementById("Password")
	if (prepasswd.value == null || prepasswd.value.length == 0) {
		showMsg("请填写先前的密码", "passwordErrorField", prepasswd)
		return false
	}
	if (passwd.value == null || passwd.value.length == 0) {
		showMsg("请填写新密码", "passwordErrorField", passwd)
		return false
	}
	if (repasswd.value == null || repasswd.value.length == 0) {
		showMsg("请确认新密码", "passwordErrorField", repasswd)
		return false
	}
	if (repasswd.value != passwd.value) {
		showMsg("两次密码不一致", "passwordErrorField", repasswd)
		return false
	}
	document.getElementById("passwordErrorField").setAttribute("hidden", "")
	Ajax(contextPath + "/settings/password", new FormData(document.querySelector('#passwordForm')), "PUT")
	return false
}

/**
 * @param message
 * @param errorField
 * @param obj
 * @returns
 */
function showMsg(message, errorField, obj) {
	let error = document.getElementById(errorField)
	error.removeAttribute("hidden")
	error.innerHTML = "错误：" + message
	obj.focus()
}

function reload() {
	setInterval(function () {
		location = location
	}, 2000);
}

function Ajax(url, params, method) {
	NProgress.start()
	$.showLoading("正在修改")
	let xhr = new XMLHttpRequest()
	xhr.open(method, url)
	xhr.onload = function () {
		$.hideLoading()
		let data = JSON.parse(xhr.responseText);
		if (xhr.status == 200) {
			if (data.success) {
				$.toast(data.message)
			} else {
				$.toast(data.message, 'cancel')
				if (data.code == 401) {
					reload()
				}
			}
		} else if (xhr.status == 401) {
			$.toast("登录超时", "cancel")
			reload()
		} else {
			$.toast("服务器繁忙", "cancel")
		}
		NProgress.done();
	}
	xhr.onerror = function () {
		$.hideLoading()
		NProgress.done();
		$.toast("服务器繁忙", "cancel")
	}
	xhr.send(params)
}

