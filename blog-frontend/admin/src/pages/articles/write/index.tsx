import React, { useState } from 'react';
import { BackTop, Button, Image, Input, message, Popconfirm } from "antd";
import MarkdownEditor from "@/components/Editor";
import ImageChooserModal from "@/components/ImageChooserModal";
import { fallbackImage, getStorage, handleHttpError, isNotEmpty, saveStorage } from '@/utils'
import articleService from '@/services/ArticleService'

import '@/assets/css/index.css'
import '@/assets/font-awesome/css/font-awesome.min.css'
import '@/assets/bootstrap3/css/bootstrap.css'
import { PlusOutlined } from "@ant-design/icons";
import { AxiosResponse } from "axios";
import ArticleDrawer from "@/pages/articles/components/ArticleDrawer";
import { ArticleItem } from "@/pages/articles/data";
import { Attachment } from "@/components/Attachment/data";


interface Post {
  title: string
  content: string
  markdown: string
  image: string | undefined
}

const articleCacheKey = "article_write_md"

export default () => {

  const [post, setPost] = useState<Post>(getStorage(articleCacheKey) || {})
  const [editor, setEditor] = useState()
  // @ts-ignore
  const [cover, setCover] = useState<string>(post.image)
  const [coverImage, setCoverImage] = useState(true)
  const [modalVisible, setModalVisible] = useState(false);
  const [drawerVisible, setDrawerVisible] = useState(false);

  const savePost = (post: Post) => {
    setPost(post)
    // @ts-ignore
    setCover(post.image)
    saveStorage(articleCacheKey, post)
  }

  const showModal = (cover: boolean) => {
    setCoverImage(cover)
    setModalVisible(true)
  }
  const hideModal = () => setModalVisible(false)
  const showDrawer = () => setDrawerVisible(true)
  const hideDrawer = () => setDrawerVisible(false)

  const setImage = (image: string | undefined) => savePost({ ...post, image })
  const setTitle = (title: string) => savePost({ ...post, title })
  // const setContent = (content: string) => savePost({ ...post, content })
  const onChange = (markdown: string, content: string) => {
    savePost({ ...post, markdown, content })
  }

  // @ts-ignore
  const setInputImage = (e) => {
    setCover(e.target.value)
  }

  const imageCallback = (attachment: Attachment) => {
    if (coverImage) {
      setImage(attachment.url)
    }
    else {
      // @ts-ignore
      const cm = editor.codemirror;
      const startPoint = {}, endPoint = {};
      Object.assign(startPoint, cm.getCursor('start'));
      Object.assign(endPoint, cm.getCursor('end'));

      cm.replaceSelection('<img src="/assets/images/loading.gif" data-original="' + attachment.url + '">')
      cm.setSelection(startPoint, endPoint);
      cm.focus();
    }
    hideModal()
  }

  const saveArticle = (values: any, onFinally: Function) => {
    const article = { ...post, ...values }
    savePost(article)

    articleService.create(article).then((res: AxiosResponse) => {
      message.success("??????????????????")
    }).catch(handleHttpError).finally(() => onFinally())
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
        title: "????????????",
      }, "|", "table", "horizontal-rule", "|", "guide", "undo", "redo", "|", "preview", "side-by-side", "fullscreen", "|", {
        title: "??????",
        name: "updateArticle",
        action: showDrawer,
        className: "fa fa-save",
      }
    ]
  }

  const onValuesChange = (allValues: ArticleItem) => {
    savePost({ ...post, ...allValues })
  }

  return (<>
        <div className="container" style={{ marginTop: 22 }}>
          <div className="data_list">
            <div className="data_list_title" style={{ borderLeft: 'none' }}>????????????</div>
            <div className="data" style={{ marginTop: 10 }}>
              <div className="WriteCover-wrapper">
                <div className="WriteCover-previewWrapper WriteCover-previewWrapper--empty">
                  {isNotEmpty(post.image)
                      ? <Image fallback={fallbackImage} src={post.image}/>
                      : <label className="UploadPicture-wrapper" onClick={() => showModal(true)}>
                        <i className="fa fa-camera fa-3x WriteCover-uploadIcon"/>
                      </label>
                  }
                </div>
                <div className="linkInput" style={{ margin: '-32px 65px' }}>
                  <Popconfirm icon='' title={<Input value={cover} onInput={setInputImage} placeholder='??????????????????'/>}
                              onConfirm={(e) => {
                                setImage(cover)
                              }}>
                    <Button type="dashed">??????</Button>
                  </Popconfirm>
                </div>
                <div className="deleteImage" style={{ margin: '-32px 0px' }}>
                  <Button type="primary" danger onClick={() => {
                    setImage(undefined)
                  }}>??????</Button>
                </div>
              </div>
              <div className="row">
                <div className="col-md-12">
                  <input value={post.title} autoComplete="off" maxLength={80} autoFocus={true}
                         className="article-title" placeholder="???????????????"
                         onChange={(e) => {
                           setTitle(e.target.value)
                         }}/>

                  <div className="box box-primary">
                    {/*<!-- Editor.md????????? -->*/}
                    <div className="box-body pad">
                      <div id="markdown-editor">
                        <MarkdownEditor setEditor={setEditor} value={post.markdown}
                                        onChange={onChange} options={editorOptions}/>
                      </div>
                      <div style={{ textAlign: 'center' }}>
                        <Button type="primary" onClick={showDrawer}>
                          <PlusOutlined/> ????????????
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
        <ArticleDrawer article={post} visible={drawerVisible} onClose={hideDrawer}
                       onValuesChange={onValuesChange} onSubmit={saveArticle}/>
      </>
  )
}
