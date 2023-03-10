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

import { UploadOutlined } from '@ant-design/icons';
import { Button, message } from 'antd';
import Cropper from 'cropperjs';
import React from 'react';
import { Image } from '../';
import './index.css';

export default class ImageCropper extends React.Component {

  onCreate = (element) => {
    if (element) {
      this.cropper = new Cropper(element, {
        viewMode: 1,
        dragMode: 'move',
        preview: this.props.preview,
        aspectRatio: this.props.aspectRatio,
        cropBoxResizable: false,
        checkCrossOrigin: false,
      })
    }
  }

  inputChange = (event) => {
    let files = event.target.files;
    if (files && files.length) {
      const file = files[0];
      if (/^image\/\w+$/.test(file.type)) {
        const URL = window.URL || window.webkitURL
        let blobURL = URL.createObjectURL(file);
        this.cropper.replace(blobURL)
      }
      else {
        message.warning('请选择一张图片');
      }
    }
  }

  componentWillUnmount() {
    this.cropper &&
    this.cropper.destroy()
    delete this.cropper
  }

  /**
   getCroppedCanvas([options])
   width: the destination width of the output canvas.
   height: the destination height of the output canvas.
   minWidth: the minimum destination width of the output canvas, the default value is 0.
   minHeight: the minimum destination height of the output canvas, the default value is 0.
   maxWidth: the maximum destination width of the output canvas, the default value is Infinity.
   maxHeight: the maximum destination height of the output canvas, the default value is Infinity.
   fillColor: a color to fill any alpha values in the output canvas, the default value is transparent.
   imageSmoothingEnabled: set to change if images are smoothed (true, default) or not (false).
   imageSmoothingQuality: set the quality of image smoothing, one of "low" (default), "medium", or "high".
   */
  saveImage = () => {
    this.cropper && this.cropper.getCroppedCanvas({ maxWidth: 4096, maxHeight: 4096 }).toBlob((blob) => {

      const { onCut } = this.props
      onCut && onCut(blob)
    })
  }

  render() {
    const { aspectRatio, preview, children, ...rest } = this.props
    return (<>
      <div className="img-container">
        <Image {...rest} onCreate={this.onCreate}/>
      </div>
      <h3 style={{ marginBottom: 30 }}>预览</h3>
      <div className="docs-preview clearfix">
        {this.props.children}
      </div>
      <div className='page-footer'>
        <Button htmlType="submit" title="上传图片" style={{ textAlign: 'center' }}>
          <UploadOutlined/> 上传图片
          <input onChange={this.inputChange} type="file" accept="image/*"/>
        </Button>
        <Button style={{ marginLeft: '10px' }} type="primary" onClick={this.saveImage} title="确认保存">确认保存</Button>
      </div>
    </>)
  }
}
