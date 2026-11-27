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
import { articleMessage as t } from '@/utils/article-i18n';
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
          title={t('settings')}
          onClose={onClose}
          open={visible}
          bodyStyle={{ paddingBottom: 80 }}
          footer={
            <div style={{ textAlign: "right" }}>
              <Button onClick={onClose} style={{ marginRight: 8 }}>{t('cancel')}</Button>
              <Button loading={publishing} onClick={onPublish} type="primary">{t('confirmPublishAction')}</Button>
            </div>
          }
      >
        <Form form={form} layout="vertical" onValuesChange={onUpdateValues}>
          <Row gutter={16}>
            {/*文章标题*/}
            <Col span={24}>
              <Form.Item name="title" label={t('articleTitle')} rules={[{ required: true, message: t('titleRequired') }]}>
                <Input placeholder={t('articleTitle')} className="article-title"/>
              </Form.Item>
            </Col>
            {/*文章封面*/}
            <Col span={24}>
              <Form.Item name="cover" label={t('articleCover')}>
                <ImageChooser placeholder={t('articleCover')} placement="right" width="80%"/>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            {/*文章分类*/}
            <Col span={12}>
              <Form.Item label={t('articleCategory')} name="category" rules={[{ required: true, message: t('chooseCategory') }]}>
                <Select
                    loading={categoriesLoading}
                    placeholder={t('chooseCategory')}
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
              <Form.Item name="labels" label={t('labels')} initialValue={article.labels}>
                <Select placeholder={t('chooseLabels')} loading={labelsLoading} mode="tags"
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
              <Form.Item name="password" label={t('accessPassword')} initialValue={article.password}>
                <Input placeholder={t('passwordPlaceholder')}/>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="uri" label={t('uri')} initialValue={article.uri}>
                <Input placeholder={t('uriPlaceholder')}/>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            {/*发布日期*/}
            <Col span={12}>
              <Form.Item name="createAt" label={t('createdAt')}>
                {/*@ts-ignore*/}
                <DatePicker showTime style={{ width: "100%" }} format="yyyy-MM-DD HH:mm:ss"/>
              </Form.Item>
            </Col>
            {/*文章状态*/}
            <Col span={12}>
              <Form.Item name="status" label={t('articleStatus')} rules={[{ required: true, message: t('chooseStatus') }]}>
                <Select placeholder={t('chooseStatus')}>
                  <Option value="DRAFT">{t('draft')}</Option>
                  <Option value="RECYCLE">{t('recycleBin')}</Option>
                  <Option value="PUBLISHED">{t('published')}</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            {/*版权信息*/}
            <Col span={24}>
              <Form.Item name="copyright" label={t('copyright')} rules={[{ required: true, message: t('copyrightPlaceholder') }]}
                         initialValue="本文为作者原创文章，转载时请务必声明出处并添加指向此页面的链接。">
                <Input placeholder={t('copyrightPlaceholder')}/>
              </Form.Item>
            </Col>
          </Row>

          {/*文章摘要*/}
          <Row gutter={16}>
            <Col span={24}>
              <Form.Item name="summary" label={t('summary')}>
                <TextArea rows={10} placeholder={t('summaryPlaceholder')}/>
              </Form.Item>
              <Button type="primary" onClick={
                () => {
                  const summary = computeSummary(article.content);
                  form.setFieldsValue({ summary })
                }}
              >
                <ReloadOutlined/> {t('generateSummary')}
              </Button>
            </Col>
          </Row>
        </Form>
      </Drawer>
  )
}
