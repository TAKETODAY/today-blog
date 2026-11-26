import { uploadFile } from "@/utils"

const UploadAdapterPlugin = (editor) => {
  console.log("加载上传插件")
  const uploadConfig = editor.config.get('upload')
  if (uploadConfig) {
    editor.plugins.get('FileRepository').createUploadAdapter = loader => {
      // console.log(loader)
      return new UploadAdapter(loader, uploadConfig);
    }
  }
}

class UploadAdapter {
  constructor(loader, uploadConfig) {
    this.loader = loader;
    this.uploadConfig = uploadConfig
  }

  upload() {
    const loader = this.loader
    const { url, setIsUploading, onUploadError, onUploadProgress } = this.uploadConfig

    const applyUploadStatus = (status) => {
      setIsUploading && setIsUploading(status)
    }

    return loader.file.then(file => {
      return new Promise((resolve, reject) => {
        applyUploadStatus(true);
        uploadFile(url, file, evt => {
          console.log(evt)
          if (evt.lengthComputable) {
            loader.uploadTotal = evt.total;
            loader.uploaded = evt.loaded;
            onUploadProgress && onUploadProgress(evt)
          }
        })
          .then(res => {
            resolve({ default: res.data.uri });
          })
          .catch(err => {
            if (onUploadError) {
              reject()
              return onUploadError(err)
            }
            else {
              return reject(err)
            }
          })
          .finally(() => applyUploadStatus(false))
      });
    });
  }

}

export default UploadAdapterPlugin;
