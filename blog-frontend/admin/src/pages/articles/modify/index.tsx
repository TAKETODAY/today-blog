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
import { BackTop, Button, Input, message, Popconfirm } from "antd";
import MarkdownEditor from "@/components/Editor";
import ImageChooserModal from "@/components/ImageChooserModal";
import { getStorage, handleHttpError, isEmpty, isNotEmpty, removeStorage, saveStorage } from '@/utils'
import articleService from '@/services/ArticleService'

import '@/assets/css/index.css'
import '@/assets/font-awesome/css/font-awesome.min.css'
import { PlusOutlined } from "@ant-design/icons";
import { AxiosResponse } from "axios";
import ArticleDrawer from "@/pages/articles/components/ArticleDrawer";
import { Post } from "@/pages/articles/components/article";
import { ArticleItem } from "@/pages/articles/data";
import { PageLoading } from "@ant-design/pro-layout";
import { Attachment } from "@/components/Attachment/data";
import Image from "@/components/Image";

export default (props: { match: { params: { id: string } } }) => {
  const { id } = props.match.params
  const articleCacheKey = "article_modify_md_" + id
  const [post, setPost] = useState<Post>(getStorage(articleCacheKey) || {})

  const [editor, setEditor] = useState()
  const [loading, setLoading] = useState<boolean>(true)
  const [modalVisible, setModalVisible] = useState(false)
  const [coverImage, setCoverImage] = useState(true)
  const [drawerVisible, setDrawerVisible] = useState(false)
  // @ts-ignore
  const [cover, setCover] = useState<string>(post.image)

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

  const savePost = (post: Post) => {
    setPost(post)
    saveStorage(articleCacheKey, post)
  }

  const showModal = (cover: boolean) => {
    setCoverImage(cover)
    setModalVisible(true)
  }

  const hideModal = () => setModalVisible(false)
  const showDrawer = () => setDrawerVisible(true)
  const hideDrawer = () => setDrawerVisible(false)
  const setImage = (cover: string | undefined) => savePost({ ...post, cover })
  const setTitle = (title: string) => savePost({ ...post, title })
  // const setContent = (content: string) => savePost({ ...post, content })
  const onChange = (markdown: string, content: string) => {
    savePost({ ...post, markdown, content })
  }

  const updateArticle = (values: any, onFinally: Function) => {
    const article = { ...post, ...values }
    savePost(article)

    articleService.update(article).then((res: AxiosResponse) => {
      message.success("更新成功")
      setDrawerVisible(false)
      removeStorage(articleCacheKey)
    }).catch(handleHttpError).finally(() => onFinally())
  }

  const imageCallback = (attachment: Attachment) => {
    if (coverImage) {
      setImage(attachment.uri)
    }
    else {
      // @ts-ignore
      const cm = editor.codemirror;
      const startPoint = {}, endPoint = {};
      Object.assign(startPoint, cm.getCursor('start'));
      Object.assign(endPoint, cm.getCursor('end'));

      cm.replaceSelection('<img src="/assets/images/loading.gif" data-original="' + attachment.uri + '">')
      cm.setSelection(startPoint, endPoint);
      cm.focus();
    }
    hideModal()
  }

  const editorOptions = {
    autosave: false,
    autofocus: false,
    renderingConfig: { html: true },
    toolbar: ["bold", "italic", "strikethrough", "heading", "|", "code", "quote", "unordered-list", "ordered-list", "|",
      "link", "image", {
        name: "custom",
        action: () => {
          showModal(false)
        },
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

  // @ts-ignore
  const setInputImage = (e) => {
    setCover(e.target.value)
  }
  if (loading) {
    return <>
      <PageLoading/>
    </>
  }

  const onValuesChange = (allValues: ArticleItem) => {
    savePost({ ...post, ...allValues })
  }

  return (<>
        <div className="container" style={{ marginTop: 22 }}>
          <div className="data_list">
            <div className="data_list_title" style={{ borderLeft: 'none' }}>修改博客</div>
            <div className="data" style={{ marginTop: 10 }}>
              <div className="WriteCover-wrapper">
                <div className="WriteCover-previewWrapper WriteCover-previewWrapper--empty">
                  {isNotEmpty(post.cover)
                      ? <Image src={post.cover}/>
                      : <label className="UploadPicture-wrapper" onClick={() => showModal(true)}>
                        <i className="fa fa-camera fa-3x WriteCover-uploadIcon"/>
                      </label>
                  }
                </div>
                <div className="linkInput" style={{ margin: '-32px 65px' }}>
                  <Popconfirm icon='' title={<Input value={cover} onInput={setInputImage} placeholder='填入链接地址'/>}
                              onConfirm={(e) => {
                                setImage(cover)
                              }}>
                    <Button type="dashed">链接</Button>
                  </Popconfirm>
                </div>
                <div className="deleteImage" style={{ margin: '-32px 0px' }}>
                  <Button type="primary" danger onClick={() => {
                    setImage(undefined)
                  }}>删除</Button>
                </div>
              </div>
              <div className="row">
                <div className="col-md-12">
                  <input value={post.title} autoComplete="off" maxLength={80} autoFocus={true}
                         className="article-title" placeholder="请输入标题"
                         onChange={(e) => {
                           setTitle(e.target.value)
                         }}/>

                  <div className="box box-primary">
                    <div className="box-body pad">
                      <div id="editor">
                        <MarkdownEditor setEditor={setEditor} value={post.markdown}
                                        onChange={onChange} options={editorOptions}/>
                      </div>
                      <div style={{ textAlign: 'center' }}>
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

        <ImageChooserModal visible={modalVisible} hideModal={hideModal} onSelect={imageCallback}/>
        <BackTop/>
        <ArticleDrawer article={post} onValuesChange={onValuesChange}
                       visible={drawerVisible} onSubmit={updateArticle} onClose={hideDrawer}/>
      </>
  )
}

// Drawer
