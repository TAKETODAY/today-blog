import React from 'react';
import { CKEditor } from '@ckeditor/ckeditor5-react';
import ClassicEditor from '@/components/ckeditor';
import { message } from 'antd'

import UploadAdapterPlugin from './UploadAdapterPlugin'
import toolbar from './config-toolbar'

export default (props) => {
  const { post, saveContent } = props
  // const [isUploading, setIsUploading] = useState(false)

  const config = {
    toolbar,
    upload: {
      // setIsUploading,
      url: '/api/attachments',
      onUploadError: error => {
        return message.error(error.message)
      }
    },
    extraPlugins: [UploadAdapterPlugin],
    placeholder: '在此处输入或粘贴您的内容!',
  }

  return (
    <CKEditor
      editor={ ClassicEditor }
      config={ config }
      data={ post.content }
      onReady={
        editor => {
          // console.log('Editor is ready to use!', editor)

          // const wordCountPlugin = editor.plugins.get('WordCount');
          // const wordCountWrapper = document.getElementById('word-count');
          // wordCountWrapper.appendChild(wordCountPlugin.wordCountContainer);
        }
      }

      onChange={
        (event, editor) => {
          saveContent(editor.getData())
          // console.log('event: onChange', { event, editor });
        }
      }

      onBlur={
        (event, editor) => {
          // console.log('event: onBlur', { event, editor });
        }
      }

      onFocus={
        (event, editor) => {
          // console.log('event: onFocus', { event, editor });
        }
      }

    />
  )
}

