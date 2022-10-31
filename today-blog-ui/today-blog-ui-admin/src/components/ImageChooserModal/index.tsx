import React from 'react';
import { Modal } from 'antd';
import AttachmentList from "@/components/Attachment";

export default (props: any) => {
  const { hideModal, visible, onSelect } = props

  return (<>
    <Modal title="选择图片文件" width={1300} visible={visible} onCancel={hideModal}>
      <AttachmentList itemClicked={onSelect}/>
    </Modal>
  </>)
}


