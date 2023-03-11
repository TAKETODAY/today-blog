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

import { fallbackImage, isNotEmpty } from "@/utils"
import { Button, Input, message, Popconfirm } from "antd"
import React, { useEffect, useState } from "react"
import { ImageChooserModal, Image } from "@/components"
import { PlusOutlined } from "@ant-design/icons"

import "./style.less"

export default props => {
  // hooks
  const [imageToShow, setSelected] = useState()
  const [inputCoverUrl, setInputCoverUrl] = useState()
  const [chooserVisible, setChooserVisible] = useState(false)

  // props
  const { onChange, value, placeholder, ...rest } = props

  useEffect(() => {
    setSelected(value)
  }, [value])

  const showSelector = () => setChooserVisible(true)
  const hideSelector = () => setChooserVisible(false)

  // 将该组件的封面的状态通知给外部组件
  const coverChange = coverURI => {
    setSelected(coverURI) // 显示区域的图片
    onChange && onChange(coverURI)
  }

  // 当删除按钮被点击时，清空封面
  const onDeleteImage = () => {
    coverChange(null) // 将该组件的状态通知给外部
    setInputCoverUrl(null) // 输入框的地址也要清空
  }

  // 手动输入了照片并点击了确认按钮
  const setInputImage = e => {
    coverChange(inputCoverUrl) // 将手动输入的状态通知给外部
  }

  return (
    <div className="main-wrapper">
      <div className="preview-wrapper">
        {isNotEmpty(imageToShow)
          ? <Image src={imageToShow}
                   onError={
                     e => {
                       onDeleteImage()
                       message.warn("输入的图片有误")
                     }
                   }
          /> :
          <div className="select-wrapper" onClick={showSelector} title={`点击选择${placeholder}`}>
            <PlusOutlined/> 点击选择{placeholder}
          </div>
        }
      </div>
      <div className="linkInput">
        <Popconfirm
          icon=""
          title={<Input placeholder="填入链接地址"
                        value={inputCoverUrl}
                        onInput={
                          e => setInputCoverUrl(e.target.value)
                        }
          />}
          onConfirm={setInputImage}>
          <Button type="dashed" title="手动输入外部或内部链接">链接</Button>
        </Popconfirm>
      </div>

      <div className="deleteImage">
        <Button danger disabled={imageToShow == null} type="primary" onClick={onDeleteImage}>删除</Button>
      </div>

      <ImageChooserModal
        visible={chooserVisible}
        onCancel={hideSelector}
        onSelect={
          attachment => {
            coverChange(attachment.uri)
            hideSelector()
          }
        }
        {...rest}
      />

    </div>
  )
}
