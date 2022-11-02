import React, { useState } from 'react';
import { BackTop, Button, Image, Input, message, Popconfirm } from "antd";
import ImageChooserModal  from "@/components/ImageChooserModal";
import { fallbackImage, getStorage, handleHttpError, isNotEmpty, removeStorage, saveStorage } from '@/utils'
import articleService from '@/services/ArticleService'

import '@/assets/css/index.css'
import '@/assets/font-awesome/css/font-awesome.min.css'
import '@/assets/bootstrap3/css/bootstrap.css'
import { PlusOutlined } from "@ant-design/icons";
import { AxiosResponse } from "axios";
import ArticleDrawer from "@/pages/articles/components/ArticleDrawer";
import RichTextEditor from "@/pages/articles/components/RichTextEditor";
import { Post } from "@/pages/articles/components/article";
import { ArticleItem } from "@/pages/articles/data";
import { Attachment } from "@/components/Attachment/data";

const articleCacheKey = "article_write_rich"

export default (props: { match: { params: { id: string } } }) => {

  const [cover, setCover] = useState<string>()
  const [modalVisible, setModalVisible] = useState(false)
  const [drawerVisible, setDrawerVisible] = useState(false)
  const [post, setPost] = useState<Post>(getStorage(articleCacheKey) || {})

  const savePost = (post: Post) => {
    setPost(post)
    setCover(post.image)
    saveStorage(articleCacheKey, post)
  }

  const showModal = () => setModalVisible(true)
  const hideModal = () => setModalVisible(false)
  const showDrawer = () => setDrawerVisible(true)
  const hideDrawer = () => setDrawerVisible(false)
  const setImage = (image: string | undefined) => {
    savePost({ ...post, image })
  }

  const setTitle = (title: string) => savePost({ ...post, title })

  const onSelectImage = (attachment: Attachment) => {
    setImage(attachment.url)
    hideModal()
  }
  // @ts-ignore
  const setInputImage = (e) => {
    setCover(e.target.value)
  }

  const updateArticle = (values: any, onFinally: Function) => {
    const article = { ...post, ...values }
    savePost(article)

    articleService.create(article).then((res: AxiosResponse) => {
      message.success("文章发布成功")
      setDrawerVisible(false)
      removeStorage(articleCacheKey)
    }).catch(handleHttpError).finally(() => onFinally())
  }

  const saveContent = (content: string) => {
    savePost({ ...post, content })
  }

  const onValuesChange = (allValues: ArticleItem) => {
    savePost({ ...post, ...allValues })
  }

  return (<>
          <div className="container" style={{ marginTop: 22 }}>
            <div className="row clearfix">
              <div className="col-md-12" style={{ zIndex: 10, padding: 0 }}>
                <div className="data_list">
                  <div className="data_list_title" style={{ borderLeft: 'none' }}>发表博客</div>
                  <div className="data" style={{ marginTop: 10 }}>
                    <div className="WriteCover-wrapper">
                      <div className="WriteCover-previewWrapper WriteCover-previewWrapper--empty">
                        {isNotEmpty(post.image)
                            ? <Image fallback={fallbackImage} src={post.image}/>
                            : <label className="UploadPicture-wrapper" onClick={showModal}>
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
                              <RichTextEditor post={post} saveContent={saveContent}/>
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

          <ImageChooserModal visible={modalVisible} hideModal={hideModal} onSelect={onSelectImage}/>
          <BackTop/>
          <ArticleDrawer onValuesChange={onValuesChange} article={post}
                         visible={drawerVisible} onSubmit={updateArticle} onClose={hideDrawer}/>
      </>
  )
}

// Drawer
