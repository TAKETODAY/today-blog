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

import { useState } from 'react';
import { BackTop, Button, message } from "antd";
import { getStorage, removeStorage, saveStorage, showHttpErrorMessage } from '@/utils'
import articleService from '@/services/ArticleService'

import '@/assets/css/index.css'
import { PlusOutlined } from "@ant-design/icons";
import { AxiosResponse } from "axios";
import ArticleDrawer from "@/pages/articles/components/ArticleSettingsDrawer";
import RichTextEditor from "@/pages/articles/components/RichTextEditor";
import { ArticleItem } from "@/pages/articles/data";

const articleCacheKey = "article_write_rich"

export default () => {

  const [drawerVisible, setDrawerVisible] = useState(false)
  const [post, setPost] = useState<ArticleItem>(getStorage(articleCacheKey) || {})

  const savePostToLocal = (post: ArticleItem) => {
    setPost(post)
    saveStorage(articleCacheKey, post)
    // console.log("本地缓存", post)
  }

  const showDrawer = () => setDrawerVisible(true)
  const hideDrawer = () => setDrawerVisible(false)
  const setTitle = (title: string) => savePostToLocal({ ...post, title })

  // 保存文章
  const onSubmit = (values: any, onFinally: () => void) => {
    const article = { ...post, ...values }
    savePostToLocal(article)

    articleService.create(article).then((res: AxiosResponse) => {
      setDrawerVisible(false)
      removeStorage(articleCacheKey)
      return message.success("文章发布成功")
    }).catch(showHttpErrorMessage)
        .finally(onFinally)
  }

  const saveContent = (content: string) => {
    savePostToLocal({ ...post, content })
  }

  const onValuesChange = (allValues: ArticleItem) => {
    savePostToLocal({ ...post, ...allValues })
  }

  return (<>
        <div className="container clearfix data_list" style={{ marginTop: 22 }}>
          <div className="data_list_title" style={{ borderLeft: 'none' }}>发表文章</div>
          <div className="data" style={{ marginTop: 10 }}>
            <input value={post.title} maxLength={80} autoFocus={true}
                   autoComplete="off" placeholder="请输入标题" className="article-title"
                   onChange={(e) => {
                     setTitle(e.target.value)
                   }}
                   style={{ marginBottom: 10 }}
            />

            <div id="editor">
              <RichTextEditor post={post} saveContent={saveContent}/>
            </div>

            <div style={{ textAlign: 'center', padding: 10 }}>
              <Button type="primary" onClick={showDrawer}>
                <PlusOutlined/> 保存文章
              </Button>
            </div>
          </div>
        </div>

        <BackTop/>

        <ArticleDrawer
            onValuesChange={onValuesChange}
            article={post}
            visible={drawerVisible}
            onSubmit={onSubmit}
            onClose={hideDrawer}
        />

      </>
  )
}
