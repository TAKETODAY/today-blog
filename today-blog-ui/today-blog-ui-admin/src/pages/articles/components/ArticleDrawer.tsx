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
      <Drawer title="配置文章属性" width={720} onClose={onClose} visible={visible} bodyStyle={{ paddingBottom: 80 }}
              footer={
                <div style={{ textAlign: "right" }}>
                  <Button onClick={onClose} style={{ marginRight: 8 }}>取消</Button>
                  <Button loading={publishing} onClick={onPublish} type="primary">确认发布</Button>
                </div>
              }
      >
        <Form layout="vertical" hideRequiredMark form={form} onValuesChange={onUpdateValues}>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="password" label="访问密码" initialValue={article.password}>
                <Input placeholder="请输入访问密码"/>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="url" label="文章地址">
                <Input placeholder="请输入文章地址"/>
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item initialValue={article.category} name="category" label="文章分类" rules={[{ required: true, message: "请选择一个文章分类" }]}>
                <Select placeholder="请选择一个文章分类" loading={categoriesLoading}
                        onDropdownVisibleChange={loadCategories}>
                  {categories.map((category: PostCategory) =>
                      <Option key={category.name} value={category.name} title={category.description}>{category.name}</Option>
                  )}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="labels" label="文章标签" initialValue={initialLabels}>
                <Select placeholder="请选择文章标签" loading={labelsLoading} mode="tags"
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
              <Form.Item name="status" label="文章状态" initialValue={status}
                         rules={[{ required: true, message: "请选择一个文章状态" }]}>
                <Select placeholder="请选择文章状态">
                  <Option value="DRAFT">草稿</Option>
                  <Option value="RECYCLE">回收站</Option>
                  <Option value="PUBLISHED">已发布</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="createTime" label="发布日期">
                <DatePicker showTime style={{ width: "100%" }}/>
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={24}>
              <Form.Item name="copyRight" label="版权信息" rules={[{ required: true, message: "请输入版权信息" }]}
                         initialValue="本文为作者原创文章，转载时请务必声明出处并添加指向此页面的链接。">
                <Input placeholder="请输入版权信息"/>
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={24}>
              <Form.Item name="summary" label="文章摘要" initialValue={summary}>
                <TextArea rows={4} placeholder="请输入文章摘要"/>
              </Form.Item>
              <Button type="primary" onClick={() => {
                const summary = computeSummary(article.content);
                form.setFieldsValue({ summary })
              }}>
                <ReloadOutlined /> 刷新摘要
              </Button>
            </Col>
          </Row>
        </Form>
      </Drawer>
  )
}
