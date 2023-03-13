/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

import React from 'react';
import { CKEditor } from '@ckeditor/ckeditor5-react';
// import ClassicEditor from '@/components/ckeditor';
import '@/components/ckeditor';
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
      editor={ClassicEditor}
      config={config}
      data={post.content}
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

