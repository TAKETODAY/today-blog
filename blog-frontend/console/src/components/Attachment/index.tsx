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

import React, { ReactNode, useEffect, useState } from 'react';
import { Button, Card, Input, List, Select } from 'antd';
import moment from "moment";
import styles from './style.less';
import { Attachment, FileType } from "./data";
import { IMAGE } from "@/utils";
import { UploadOutlined } from '@ant-design/icons';
import UploadForm from "./UploadForm";
import { getAttachment } from "@/components/Attachment/service";
import { Image } from '@/components'

const { Option } = Select;
const { Search } = Input;

interface AttachmentProps {
  children?: ReactNode | string;
  fileType?: FileType,
  itemClicked: { itemClicked(item: Attachment): void }["itemClicked"]
}

const defaultParams = { current: 1, size: 10, fileType: '', name: '' }
const grid = { gutter: 16, xs: 1, sm: 2, md: 4, lg: 6, xl: 6, xxl: 6, }

const images = {
  AUDIO: import('@/assets/images/file/mp3.png'),
  VIDEO: import('@/assets/images/file/avi.png'),
  OTHER: import('@/assets/images/file/more.png'),
  TEXT: import('@/assets/images/file/documents.png')
}

const defaultAttachment = {
  pages: 0,
  current: 1,
  data: [],
  total: 0,
  size: 10,
}

export default ({ itemClicked, fileType, children }: AttachmentProps) => {

  const [params, setParams] = useState({ ...defaultParams, fileType })
  const [loading, setLoading] = useState(true)
  const [attachment, setAttachment] = useState(defaultAttachment)
  const [uploadModalVisible, setUploadModalVisible] = useState<boolean>(false)

  useEffect(() => {
    fetchAttachment(params.current, params.size)
  }, [params])

  const fetchAttachment = (current: number, size?: number) => {
    setLoading(true)
    setAttachment(defaultAttachment)
    getAttachment({
      ...params,
      size,
      current,
    }).then(res => {
      setAttachment(res.data)
    }).finally(() => setLoading(false))
  }

  const renderImage = (item: Attachment) => {
    if (item.fileType === IMAGE) {
      return <Image alt={item.name} src={item.uri}/>
    }
    return <img alt={item.name || ''} src={images[item.fileType]}/>
  }

  const handleFileTypeChange = (fileType: FileType) => {
    setParams({ ...params, fileType })
  }

  const onSearch = (name: string) => {
    setParams({ ...params, name })
  }
  const onRefresh = () => {
    setParams({ ...params })
  }

  return (
      <>
        <Select defaultValue={fileType || ''}
                style={{ width: 120, marginBottom: 10 }}
                placeholder="附件分类" onChange={handleFileTypeChange}>
          <Option value="">全部</Option>
          <Option value="IMAGE">图片</Option>
          <Option value="VIDEO">视频</Option>
          <Option value="AUDIO">音频</Option>
          <Option value="TEXT">文本</Option>
          <Option value="OTHER">其他</Option>
        </Select>

        <Search placeholder="文件名"
                allowClear
                onSearch={onSearch}
                style={{ width: 200, margin: '0 10px' }}
        />
        <Button type="primary" icon={<UploadOutlined/>}
                onClick={() => setUploadModalVisible(true)}>上传附件</Button>

        <List
            grid={grid}
            loading={loading}
            pagination={{
              onChange: fetchAttachment,
              onShowSizeChange: fetchAttachment,
              total: attachment.total,
              pageSize: attachment.size,
              current: attachment.current,
            }}
            dataSource={attachment?.data}
            renderItem={(item: Attachment) => (
                <List.Item onClick={() => itemClicked(item)}>
                  <Card className={styles.card} hoverable cover={renderImage(item)}>
                    <div className={styles.cardItemContent}>
                      <span>{item.name || ''} </span>
                      <b>{moment(item.createAt).fromNow()}</b>
                    </div>
                  </Card>
                </List.Item>
            )}
        />

        {
          React.Children.map(children, child => {
            if (!React.isValidElement(child)) {
              return null
            }
            return React.cloneElement(child, {
              ...child.props, onRefresh
            })
          })
        }

        <UploadForm open={uploadModalVisible}
                    onRefresh={onRefresh}
                    onCancel={() => setUploadModalVisible(false)}/>
      </>
  )
}

