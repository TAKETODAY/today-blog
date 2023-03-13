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

import { useEffect, useState } from 'react';
import { BackTop, Button, message } from "antd";
import { extractData, getStorage, isEmpty, removeStorage, saveStorage, showHttpErrorMessage, showHttpErrorMessageVoid } from '@/utils'
import articleService from '@/services/ArticleService'

import '@/assets/css/index.css'

import { PlusOutlined } from "@ant-design/icons";
import { AxiosResponse } from "axios";
import ArticleDrawer from "../components/ArticleSettingsDrawer";
import RichTextEditor from "../components/RichTextEditor";
import { ArticleItem } from "../data";
import { PageLoading } from "@ant-design/pro-layout";

export default (props: { match: { params: { id: string } } }) => {
  const { id } = props.match.params
  const articleCacheKey = "article_modify_rich_" + id

  const [loading, setLoading] = useState<boolean>(true)
  const [drawerVisible, setDrawerVisible] = useState(false)
  const [post, setPost] = useState<ArticleItem>(getStorage(articleCacheKey) || {})

  const savePostToLocal = (post: ArticleItem) => {
    setPost(post)
    saveStorage(articleCacheKey, post)
    console.log("本地缓存", post)
  }

  const showDrawer = () => setDrawerVisible(true)
  const hideDrawer = () => setDrawerVisible(false)

  const setTitle = (title: string) => savePostToLocal({ ...post, title })

  const updateArticle = (values: any, onFinally: () => void) => {
    const article = { ...post, ...values }
    savePostToLocal(article)
    articleService.updateById(id, article).then((res: AxiosResponse) => {
      setDrawerVisible(false)
      removeStorage(articleCacheKey)
      return message.success("更新成功")
    }).catch(showHttpErrorMessage)
        .finally(onFinally)
  }

  const saveContent = (content: string) => {
    savePostToLocal({ ...post, content })
  }

  const onValuesChange = (allValues: ArticleItem) => {
    savePostToLocal({ ...post, ...allValues })
  }

  useEffect(() => {
    if (!post || isEmpty(Object.keys(post))) {
      console.log("getById")
      articleService.getById(id)
          .then(extractData)
          .then(savePostToLocal)
          .catch(showHttpErrorMessageVoid)
          .finally(() => setLoading(false))
    }
    else {
      console.log("setLoading false")
      setLoading(false)
    }
  }, [])

  if (loading) {
    return <>
      <PageLoading/>
    </>
  }

  return (<>
        <div className="container" style={{ marginTop: 22 }}>
          <div className="data_list">
            <div className="data_list_title" style={{ borderLeft: 'none' }}>修改文章</div>
            <div className="data" style={{ marginTop: 10 }}>

              <div className="row">
                <div className="col-md-12">
                  <input
                      value={post.title || ""}
                      autoComplete="off"
                      maxLength={80}
                      autoFocus={true}
                      className="article-title"
                      placeholder="请输入标题"
                      onChange={(e) => {
                        setTitle(e.target.value)
                      }}
                  />

                  <div className="box box-primary">
                    <div className="box-body">
                      <div id="editor">
                        <RichTextEditor post={post} saveContent={saveContent}/>
                      </div>
                      <div style={{ textAlign: 'center', padding: 10 }}>
                        <Button type="primary" onClick={showDrawer}>
                          <PlusOutlined/> 更新文章
                        </Button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          {/*  <!--/RIGHT END-->*/}
        </div>

        <BackTop/>

        <ArticleDrawer
            onValuesChange={onValuesChange}
            article={post}
            visible={drawerVisible}
            onSubmit={updateArticle}
            onClose={hideDrawer}
        />

      </>
  )
}
