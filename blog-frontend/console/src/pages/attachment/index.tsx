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

import { useState } from 'react';
import { message } from 'antd';
import { PageContainer } from '@ant-design/pro-layout';
import { update } from "./service";
import AttachmentForm from "@/pages/attachment/AttachmentForm";
import AttachmentList from "@/components/Attachment";
import { Attachment } from "@/components/Attachment/data";


/**
 * 更新节点
 * @param attach
 */
const handleUpdate = async (attach: Attachment) => {
  const hide = message.loading(`正在更新: ${attach.name}`)
  try {
    await update(attach)
    hide()
    message.success('更新信息成功', 1)
    return true
  }
  catch (error) {
    hide()
    message.error('更新信息失败请重试！')
    return false
  }
}

export default () => {
  const [updateAttachment, setUpdateAttachment] = useState({})
  const [updateModalVisible, setUpdateModalVisible] = useState<boolean>(false)

  return (
      <PageContainer>
        <div style={{ background: '#fff', padding: 10, borderRadius: 5 }}>

          <AttachmentList itemClicked={item => {
            setUpdateAttachment(item)
            setUpdateModalVisible(true)
          }}/>

          <AttachmentForm
              onSubmit={async (value: Attachment) => {
                if (await handleUpdate(value)) {
                  setUpdateAttachment({})
                  setUpdateModalVisible(false)
                }
              }}
              onCancel={() => {
                setUpdateAttachment({})
                setUpdateModalVisible(false)
              }}
              visible={updateModalVisible}
              values={updateAttachment}>

          </AttachmentForm>

        </div>
      </PageContainer>
  )
}

