import { computeSummary, handleHttpError, isEmpty, isNotEmpty } from "@/utils";
import { Button, Col, DatePicker, Drawer, Form, Input, Row, Select } from "antd";
import React, { useState } from "react";
import articleService from "@/services/ArticleService";
import { AxiosResponse } from "axios";
import { PostCategory, PostLabel } from "@/pages/articles/components/article";
import { ArticleItem } from "@/pages/articles/data";
import { ReloadOutlined } from "@ant-design/icons";

const { Option } = Select;
const { TextArea } = Input;

function getInitialLabels(article: { labels: any[]; }) {
  if (isNotEmpty(article.labels)) {
    const first = article.labels[0]
    return (typeof first === 'string') ? [...article.labels] : article.labels?.map((label: PostLabel) => label.name)
  }
  return []
}

export default (props: any) => {
  const { visible, onClose, onSubmit, article, onValuesChange } = props

  const [form] = Form.useForm()
  const [labels, setLabels] = useState<PostLabel[]>([])
  const [categories, setCategories] = useState<PostCategory[]>([])
  const [labelsLoading, setLabelsLoading] = useState<boolean>(false)
  const [categoriesLoading, setCategoriesLoading] = useState<boolean>(false)
  const [publishing, setPublishing] = useState(false);

  const loadLabels = (open: boolean) => {
    if (open && isEmpty(labels)) {
      setLabelsLoading(true)
      // @ts-ignore
      articleService.getAllLabels().then((res: AxiosResponse) => {
        setLabels(res.data)
      }).catch(handleHttpError).finally(() => setLabelsLoading(false))
    }
  }

  const loadCategories = (open: boolean) => {
    if (open && isEmpty(categories)) {
      setCategoriesLoading(true)
      // @ts-ignore
      articleService.getAllCategories().then((res: AxiosResponse) => {
        setCategories(res.data)
      }).catch(handleHttpError).finally(() => setCategoriesLoading(false))
    }
  }

  const onPublish = () => {
    form.validateFields().then(values => {
      setPublishing(true)
      onSubmit(values, () => setPublishing(false))
    })
  }

  // initial values
  const summary = isNotEmpty(article.summary) ? article.summary : computeSummary(article.content)
  const initialLabels = getInitialLabels(article)
  const status = article.status ? article.status : "PUBLISHED"

  const onUpdateValues = (_: ArticleItem, item: ArticleItem) => {
    onValuesChange(item)
  }

  // @ts-ignore
  return (
      <Drawer title="??????????????????" width={720} onClose={onClose} visible={visible} bodyStyle={{ paddingBottom: 80 }}
              footer={
                <div style={{ textAlign: "right" }}>
                  <Button onClick={onClose} style={{ marginRight: 8 }}>??????</Button>
                  <Button loading={publishing} onClick={onPublish} type="primary">????????????</Button>
                </div>
              }
      >
        <Form layout="vertical" hideRequiredMark form={form} onValuesChange={onUpdateValues}>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="password" label="????????????" initialValue={article.password}>
                <Input placeholder="?????????????????????"/>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="url" label="????????????">
                <Input placeholder="?????????????????????"/>
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item initialValue={article.category} name="category" label="????????????" rules={[{ required: true, message: "???????????????????????????" }]}>
                <Select placeholder="???????????????????????????" loading={categoriesLoading}
                        onDropdownVisibleChange={loadCategories}>
                  {categories.map((category: PostCategory) =>
                      <Option key={category.name} value={category.name} title={category.description}>{category.name}</Option>
                  )}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="labels" label="????????????" initialValue={initialLabels}>
                <Select placeholder="?????????????????????" loading={labelsLoading} mode="tags"
                        showArrow onDropdownVisibleChange={loadLabels}>
                  {labels.map((label: PostLabel) =>
                      <Option value={label.name} title={label.name}>{label.name}</Option>
                  )}
                </Select>
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="status" label="????????????" initialValue={status}
                         rules={[{ required: true, message: "???????????????????????????" }]}>
                <Select placeholder="?????????????????????">
                  <Option value="DRAFT">??????</Option>
                  <Option value="RECYCLE">?????????</Option>
                  <Option value="PUBLISHED">?????????</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="createTime" label="????????????">
                <DatePicker showTime style={{ width: "100%" }}/>
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={24}>
              <Form.Item name="copyRight" label="????????????" rules={[{ required: true, message: "?????????????????????" }]}
                         initialValue="????????????????????????????????????????????????????????????????????????????????????????????????">
                <Input placeholder="?????????????????????"/>
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={24}>
              <Form.Item name="summary" label="????????????" initialValue={summary}>
                <TextArea rows={4} placeholder="?????????????????????"/>
              </Form.Item>
              <Button type="primary" onClick={() => {
                const summary = computeSummary(article.content);
                form.setFieldsValue({ summary })
              }}>
                <ReloadOutlined /> ????????????
              </Button>
            </Col>
          </Row>
        </Form>
      </Drawer>
  )
}
