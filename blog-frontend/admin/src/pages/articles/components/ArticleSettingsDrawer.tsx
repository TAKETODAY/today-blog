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

import { useEffect, useState } from "react";
import { computeSummary, extractData, isEmpty, isNotEmpty, showHttpErrorMessageVoid } from "@/utils";
import { Button, Col, DatePicker, Drawer, Form, Input, Row, Select } from "antd";
import articleService from "@/services/ArticleService";
import { AxiosResponse } from "axios";
import { PostCategory, PostLabel } from "@/pages/articles/components/article";
import { ArticleItem } from "@/pages/articles/data";
import { ReloadOutlined } from "@ant-design/icons";
import ImageChooser from "@/components/ImageChooser"
import moment from "moment";

const { Option } = Select;
const { TextArea } = Input;

function getInitialLabels(article: ArticleItem) {
  if (isNotEmpty(article.labels)) {
    const first = article.labels[0]
    return (typeof first === 'string') ? [...article.labels]
        : article.labels?.map((label: PostLabel) => label.name)
  }
  return []
}

export default (props: any) => {
  const [form] = Form.useForm()
  const [publishing, setPublishing] = useState(false);
  const [labels, setLabels] = useState<PostLabel[]>([])
  const [categories, setCategories] = useState<PostCategory[]>([])
  const [labelsLoading, setLabelsLoading] = useState<boolean>(false)
  const [categoriesLoading, setCategoriesLoading] = useState<boolean>(false)

  const { visible, onClose, onSubmit, article, onValuesChange } = props

  useEffect(() => {
    // initial values
    const status = article.status ? article.status : "PUBLISHED"
    const summary = isNotEmpty(article.summary) ? article.summary : computeSummary(article.content)
    const createAt = moment(article.createAt)
    const labels = getInitialLabels(article)

    form.setFieldsValue({ ...article, createAt, summary, status, labels })
  }, [article])

  const loadLabels = (open: boolean) => {
    if (open && isEmpty(labels)) {
      setLabelsLoading(true)
      articleService.getAllLabels().then((res: AxiosResponse) => {
        setLabels(res.data)
      }).catch(showHttpErrorMessageVoid).finally(() => setLabelsLoading(false))
    }
  }

  const loadCategories = (open: boolean) => {
    if (open && isEmpty(categories)) {
      setCategoriesLoading(true)
      articleService.getAllCategories()
          .then(extractData)
          .then(setCategories)
          .catch(showHttpErrorMessageVoid)
          .finally(() => setCategoriesLoading(false))
    }
  }

  const onPublish = () => {
    form.validateFields().then(values => {
      setPublishing(true)
      onSubmit(values, () => setPublishing(false))
    })
  }

  const onUpdateValues = (_: ArticleItem, item: ArticleItem) => {
    console.debug("表单有变动=>\n变动：", _, "\n新值：", item)
    onValuesChange({ ...item })
  }

  return (
      <Drawer
          width={800}
          title="配置文章属性"
          onClose={onClose}
          open={visible}
          bodyStyle={{ paddingBottom: 80 }}
          footer={
            <div style={{ textAlign: "right" }}>
              <Button onClick={onClose} style={{ marginRight: 8 }}>取消</Button>
              <Button loading={publishing} onClick={onPublish} type="primary">确认发布</Button>
            </div>
          }
      >
        <Form form={form} layout="vertical" onValuesChange={onUpdateValues}>
          <Row gutter={16}>
            {/*文章标题*/}
            <Col span={24}>
              <Form.Item name="title" label="文章标题" rules={[{ required: true, message: "请填写文章标题！" }]}>
                <Input placeholder="文章标题" className="article-title"/>
              </Form.Item>
            </Col>
            {/*文章封面*/}
            <Col span={24}>
              <Form.Item name="cover" label="文章封面">
                <ImageChooser placeholder="文章封面" placement="right" width="80%"/>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            {/*文章分类*/}
            <Col span={12}>
              <Form.Item label="文章分类" name="category" rules={[{ required: true, message: "请选择一个文章分类" }]}>
                <Select
                    loading={categoriesLoading}
                    placeholder="请选择一个文章分类"
                    onDropdownVisibleChange={loadCategories}>
                  {categories.map((category: PostCategory) =>
                      <Option key={category.name} value={category.name} title={category.description}>
                        {category.name}
                      </Option>
                  )}
                </Select>
              </Form.Item>
            </Col>
            {/*文章标签*/}
            <Col span={12}>
              <Form.Item name="labels" label="文章标签" initialValue={article.labels}>
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
              <Form.Item name="password" label="访问密码" initialValue={article.password}>
                <Input placeholder="请输入访问密码"/>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="uri" label="文章地址" initialValue={article.uri}>
                <Input placeholder="请输入文章地址"/>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            {/*发布日期*/}
            <Col span={12}>
              <Form.Item name="createAt" label="发布日期">
                {/*@ts-ignore*/}
                <DatePicker showTime style={{ width: "100%" }} format="yyyy-MM-DD HH:mm:ss"/>
              </Form.Item>
            </Col>
            {/*文章状态*/}
            <Col span={12}>
              <Form.Item name="status" label="文章状态" rules={[{ required: true, message: "请选择一个文章状态" }]}>
                <Select placeholder="请选择文章状态">
                  <Option value="DRAFT">草稿</Option>
                  <Option value="RECYCLE">回收站</Option>
                  <Option value="PUBLISHED">已发布</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            {/*版权信息*/}
            <Col span={24}>
              <Form.Item name="copyright" label="版权信息" rules={[{ required: true, message: "请输入版权信息" }]}
                         initialValue="本文为作者原创文章，转载时请务必声明出处并添加指向此页面的链接。">
                <Input placeholder="请输入版权信息"/>
              </Form.Item>
            </Col>
          </Row>

          {/*文章摘要*/}
          <Row gutter={16}>
            <Col span={24}>
              <Form.Item name="summary" label="文章摘要">
                <TextArea rows={10} placeholder="请输入文章摘要"/>
              </Form.Item>
              <Button type="primary" onClick={
                () => {
                  const summary = computeSummary(article.content);
                  form.setFieldsValue({ summary })
                }}
              >
                <ReloadOutlined/> 自动生成摘要
              </Button>
            </Col>
          </Row>
        </Form>
      </Drawer>
  )
}
