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

import { Drawer, Modal } from 'antd';
import { AttachmentList } from "@/components";

export default (props: any) => {
  const { onCancel, visible, onSelect, type, modal, width, ...rest } = props

  if (modal) {
    return (<>
      <Modal title="选择图片文件" width={width || 1300} open={visible} onCancel={onCancel} {...rest}>
        <AttachmentList itemClicked={onSelect} fileType="IMAGE"/>
      </Modal>
    </>)
  }

  return (
      <>
        <Drawer title="选择图片文件" width={width || 1300} open={visible} onClose={onCancel} {...rest}>
          <AttachmentList itemClicked={onSelect} fileType="IMAGE"/>
        </Drawer>
      </>
  )
}



