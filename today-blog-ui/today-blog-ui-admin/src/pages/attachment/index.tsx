import React, { useState } from 'react';
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

