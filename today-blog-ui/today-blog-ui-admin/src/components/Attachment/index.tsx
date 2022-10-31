import React, { useEffect, useState } from 'react';
import { Button, Card, Input, List, Select } from 'antd';
import moment from "moment";
import styles from './style.less';
import { Attachment } from "./data";
import { IMAGE } from "@/utils";
import { UploadOutlined } from '@ant-design/icons';
import UploadForm from "./UploadForm";
import { getAttachment } from "@/components/Attachment/service";

const { Option } = Select;
const { Search } = Input;

interface AttachmentProps {
  itemClicked: { itemClicked(item: Attachment): void }["itemClicked"]
}

const defaultParams = { current: 1, size: 10, fileType: '', name: '' }
const grid = { gutter: 16, xs: 1, sm: 2, md: 4, lg: 6, xl: 6, xxl: 6, }

const images = {
  AUDIO: require('@/assets/images/file/mp3.png'),
  VIDEO: require('@/assets/images/file/avi.png'),
  OTHER: require('@/assets/images/file/more.png'),
  TEXT: require('@/assets/images/file/documents.png')
}
const defaultAttachment = {
  all: 0,
  current: 1,
  data: [],
  num: 0,
  size: 10,
}

export default ({ itemClicked }: AttachmentProps) => {

  const [loading, setLoading] = useState(true)
  const [params, setParams] = useState(defaultParams)
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

  const renderItem = (item: Attachment) => {
    return <>
      <Card className={styles.card} hoverable
            cover={<img alt={item.name} src={item.fileType === IMAGE ? item.url : images[item.fileType]}/>}>
        <div className={styles.cardItemContent}>
          <span>{item.name} </span>
          <b>{moment(item.id).fromNow()}</b>
        </div>
      </Card>
    </>;
  }

  const handleChange = (fileType: string) => {
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
        <Select style={{ width: 120, marginBottom: 10 }} placeholder='附件分类' onChange={handleChange}>
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
              total: attachment.all,
              pageSize: attachment.size,
              current: attachment.current,
            }}
            dataSource={attachment?.data}
            renderItem={item => (
                <List.Item onClick={() => itemClicked(item)}>
                  {
                    renderItem(item)
                  }
                </List.Item>
            )}
        />
        <UploadForm updateModalVisible={uploadModalVisible}
                    onRefresh={onRefresh} onCancel={() => setUploadModalVisible(false)}/>
      </>
  )
}

