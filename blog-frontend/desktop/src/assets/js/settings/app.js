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

// ????????????
function saveSession(key, obj) {
	var str = JSON.stringify(obj); // ???????????????????????????
	sessionStorage.setItem(key, str);
}
// ????????????
function getSession(key) {
	return JSON.parse(sessionStorage.getItem(key));
}
function clearSession() {
	sessionStorage.clear();
	localStorage.clear();
	$.toast("????????????");
}
function removeSession(key) {
	if (getSession(key) != null) {
		sessionStorage.removeItem(key);
		return true;
	}
	return false;
}
// ????????????
function saveStorage(key, obj) {
	var str = JSON.stringify(obj); // ???????????????????????????
	localStorage.setItem(key, str);
}
// ????????????
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
	subBtn.text("???????????? ...")
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
					subBtn.text("????????????")
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
						$.toast("????????????", "cancel")
						reload()
					} else {
						$.toast("???????????????", 'cancel')
						subBtn.text("????????????")
					}
				}
			})
		})
	} catch (e) {
		$.toast("????????????????????????????????????", 'cancel')
		reload()
	}
}

function saveAvatar(btn) {
	NProgress.start();
	let subBtn = $(btn)
	subBtn.text("???????????? ...")
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
					subBtn.text("????????????")
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
						$.toast("????????????", "cancel")
						reload()
					} else {
						$.toast("???????????????", 'cancel')
						subBtn.text("????????????")
					}
				}
			})
		})
	} catch (e) {
		$.toast("????????????????????????????????????", 'cancel')
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
		showMsg("??????????????????", "infoErrorField", name)
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
		showMsg("??????????????????", "emailErrorField", passwd)
		return false
	}
	if (email.value == null || email.value.length == 0) {
		showMsg("??????????????????", "emailErrorField", email)
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
		showMsg("????????????????????????", "passwordErrorField", prepasswd)
		return false
	}
	if (passwd.value == null || passwd.value.length == 0) {
		showMsg("??????????????????", "passwordErrorField", passwd)
		return false
	}
	if (repasswd.value == null || repasswd.value.length == 0) {
		showMsg("??????????????????", "passwordErrorField", repasswd)
		return false
	}
	if (repasswd.value != passwd.value) {
		showMsg("?????????????????????", "passwordErrorField", repasswd)
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
	error.innerHTML = "?????????" + message
	obj.focus()
}

function reload() {
	setInterval(function () {
		location = location
	}, 2000);
}

function Ajax(url, params, method) {
	NProgress.start()
	$.showLoading("????????????")
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
			$.toast("????????????", "cancel")
			reload()
		} else {
			$.toast("???????????????", "cancel")
		}
		NProgress.done();
	}
	xhr.onerror = function () {
		$.hideLoading()
		NProgress.done();
		$.toast("???????????????", "cancel")
	}
	xhr.send(params)
}

