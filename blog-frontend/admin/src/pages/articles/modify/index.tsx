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
import MarkdownEditor from "@/components/Editor";
import ImageChooserModal from "@/components/ImageChooserModal";
import { getStorage, handleHttpError, isEmpty, removeStorage, saveStorage, showHttpErrorMessage } from '@/utils'
import articleService from '@/services/ArticleService'

import '@/assets/css/index.css'
import '@/assets/font-awesome/css/font-awesome.min.css'
import { PlusOutlined } from "@ant-design/icons";
import { AxiosResponse } from "axios";
import ArticleDrawer from "@/pages/articles/components/ArticleSettingsDrawer";
import { Post } from "@/pages/articles/components/article";
import { ArticleItem } from "@/pages/articles/data";
import { PageLoading } from "@ant-design/pro-layout";
import { Attachment } from "@/components/Attachment/data";
import { useCDN } from "@/components/hooks";

export default (props: { match: { params: { id: string } } }) => {
  const { id } = props.match.params
  const articleCacheKey = "article_modify_md_" + id

  const cdn = useCDN()
  const [post, setPost] = useState<Post>(getStorage(articleCacheKey) || {})
  const [editor, setEditor] = useState()
  const [loading, setLoading] = useState<boolean>(true)
  const [modalVisible, setModalVisible] = useState(false)
  const [drawerVisible, setDrawerVisible] = useState(false)

  useEffect(() => {
    if (!post || isEmpty(Object.keys(post))) {
      articleService.getById(id).then((res: AxiosResponse) => {
        const { data } = res
        setPost(data)
      }).catch(handleHttpError).finally(() => setLoading(false))
    }
    else {
      setLoading(false)
    }
  }, [])

  const savePostToLocal = (post: any) => {
    setPost(post)
    saveStorage(articleCacheKey, post)
    console.log("本地缓存", post)
  }

  const showModal = () => {
    setModalVisible(true)
  }

  const hideModal = () => setModalVisible(false)
  const showDrawer = () => setDrawerVisible(true)
  const hideDrawer = () => setDrawerVisible(false)
  const setTitle = (title: string) => savePostToLocal({ ...post, title })

  const saveContent = (markdown: string, content: string) => {
    savePostToLocal({ ...post, markdown, content })
  }

  // 保存文章
  const onSubmit = (values: any, onFinally: () => void) => {
    const article = { ...post, ...values }
    savePostToLocal(article)

    articleService.create(article).then((_: AxiosResponse) => {
      setDrawerVisible(false)
      removeStorage(articleCacheKey)
      return message.success("更新成功")
    }).catch(showHttpErrorMessage)
        .finally(onFinally)
  }

  const imageCallback = (attachment: Attachment) => {
    // @ts-ignore
    const cm = editor.codemirror;
    const startPoint = {}, endPoint = {};
    Object.assign(startPoint, cm.getCursor('start'));
    Object.assign(endPoint, cm.getCursor('end'));

    cm.replaceSelection('<img src="/assets/images/loading.gif" data-original="'
        + cdn + attachment.uri + '" alt="' + attachment.name + '">')
    cm.setSelection(startPoint, endPoint);
    cm.focus();

    hideModal()
  }

  const editorOptions = {
    autosave: false,
    autofocus: false,
    renderingConfig: { html: true },
    toolbar: ["bold", "italic", "strikethrough", "heading", "|", "code", "quote", "unordered-list", "ordered-list", "|",
      "link", "image", {
        name: "custom",
        action: showModal,
        className: "fa fa-upload",
        title: "选择附件",
      }, "|", "table", "horizontal-rule", "|", "guide", "undo", "redo", "|", "preview", "side-by-side", "fullscreen", "|", {
        title: "保存",
        name: "updateArticle",
        action: showDrawer,
        className: "fa fa-save",
      }
    ]
  }

  if (loading) {
    return <>
      <PageLoading/>
    </>
  }

  const onValuesChange = (allValues: ArticleItem) => {
    savePostToLocal({ ...post, ...allValues })
  }

  return (<>
        <div className="container" style={{ marginTop: 22 }}>
          <div className="row clearfix">
            <div className="col-md-12" style={{ zIndex: 10, padding: 0 }}>
              <div className="data_list">
                <div className="data_list_title" style={{ borderLeft: 'none' }}>编辑文章</div>
                <div className="data" style={{ marginTop: 10 }}>
                  <div className="row">
                    <div className="col-md-12">
                      <input value={post.title} maxLength={80} autoFocus={true}
                             autoComplete="off" placeholder="请输入标题" className="article-title"
                             onChange={(e) => {
                               setTitle(e.target.value)
                             }}
                      />

                      <div className="box box-primary">
                        <div className="box-body pad">

                          <div id="markdown-editor">
                            <MarkdownEditor setEditor={setEditor} value={post.markdown}
                                            onChange={saveContent} options={editorOptions}/>
                          </div>

                          <div style={{ textAlign: 'center', padding: 10 }}>
                            <Button type="primary" onClick={showDrawer}>
                              <PlusOutlined/> 保存文章
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
          </div>
        </div>

        <BackTop/>

        {/*文章内容选择图片*/}
        <ImageChooserModal visible={modalVisible} hideModal={hideModal} onSelect={imageCallback}/>

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
