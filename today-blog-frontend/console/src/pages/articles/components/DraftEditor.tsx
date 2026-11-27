import { lazy, Suspense, useEffect, useRef, useState } from 'react';
import { history, useLocation, useModel, useParams } from '@umijs/max';
import { Alert, BackTop, Button, message, Modal, Space } from 'antd';
import { PageLoading } from '@ant-design/pro-layout';
import { PlusOutlined } from '@ant-design/icons';
import MarkdownEditor from '@/components/Editor';
import ImageChooserModal from '@/components/ImageChooserModal';
import { Attachment } from '@/components/Attachment/data';
import { useCDN } from '@/components/hooks';
import { showHttpErrorMessage } from '@/utils';
import { articleMessage as t } from '@/utils/article-i18n';
import articleService from '@/services/ArticleService';
import ArticleDrawer from './ArticleSettingsDrawer';
import { ArticleDraft, DraftPost, EditorMode, draftPost, newDraftId } from '../drafts/storage';
import DraftDialog from '../drafts/DraftDialog';
import { useDraft } from '../drafts/useDraft';
import '@/assets/css/index.css';

const RichTextEditor = lazy(() => import('./RichTextEditor'));

export default function DraftEditor({ mode: routeMode }: { mode: EditorMode }) {
  const { initialState } = useModel('@@initialState');
  const userId = initialState?.currentUser?.id;
  const { id: routeArticleId } = useParams<{ id: string }>();
  const { pathname, search } = useLocation();
  const params = new URLSearchParams(search);
  const id = params.get('draft');
  const selectedMode = params.get('mode');
  const mode = selectedMode === 'markdown' || selectedMode === 'richText' ? selectedMode : routeMode;
  const articleId = params.has('articleId') ? params.get('articleId') || undefined : routeArticleId;

  function selectDraft(draft: Pick<ArticleDraft, 'id' | 'mode' | 'articleId'>) {
    const next = new URLSearchParams(search);
    next.set('draft', draft.id);
    next.set('mode', draft.mode);
    next.set('articleId', draft.articleId || '');
    history.replace({ pathname, search: `?${next.toString()}` });
  }

  useEffect(() => {
    if (!id) {
      selectDraft({ id: newDraftId(), mode, articleId });
    }
  }, [id, mode, articleId]);

  if (userId == null || !id) {
    return <PageLoading/>;
  }
  return <EditorSession key={`${userId}:${mode}:${articleId || ''}:${id}`}
                       userId={userId} id={id} mode={mode} articleId={articleId} onSelect={selectDraft}/>;
}

function EditorSession({ userId, id, mode, articleId, onSelect }: {
  userId: number; id: string; mode: EditorMode; articleId?: string;
  onSelect: (draft: Pick<ArticleDraft, 'id' | 'mode' | 'articleId'>) => void;
}) {
  const { post, restored, initialize, change, flush, complete, discard, status, savedAt } = useDraft(userId, id, mode, articleId);
  const cdn = useCDN();
  const [editor, setEditor] = useState<any>();
  const [modalVisible, setModalVisible] = useState(false);
  const [drawerVisible, setDrawerVisible] = useState(false);
  const [draftsOpen, setDraftsOpen] = useState(false);
  const [loading, setLoading] = useState(Boolean(articleId && !restored));
  const [loadFailed, setLoadFailed] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const activeSession = useRef(true);

  useEffect(() => {
    activeSession.current = true;
    let active = true;
    if (articleId && !restored) {
      articleService.getById(articleId).then(({ data }) => {
        if (active) {
          initialize(draftPost(data));
        }
      }).catch(() => {
        if (active) {
          setLoadFailed(true);
        }
      }).finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    }
    return () => {
      active = false;
      activeSession.current = false;
    };
  }, []);

  function imageCallback(attachment: Attachment) {
    const cm = editor.codemirror;
    cm.replaceSelection('<img src="/assets/images/loading.gif" data-original="'
      + cdn + attachment.uri + '" alt="' + attachment.name + '">');
    cm.focus();
    setModalVisible(false);
  }

  const editorOptions = {
    autosave: false,
    autofocus: false,
    autoDownloadFontAwesome: true,
    renderingConfig: { html: true },
    toolbar: ['bold', 'italic', 'strikethrough', 'heading', '|', 'code', 'quote', 'unordered-list', 'ordered-list', '|',
      'link', 'image', {
        name: 'custom', action: () => setModalVisible(true), className: 'fa fa-upload', title: t('chooseAttachment'),
      }, '|', 'table', 'horizontal-rule', '|', 'guide', 'undo', 'redo', '|', 'preview', 'side-by-side', 'fullscreen', '|', {
        title: t('save'), name: 'updateArticle', action: () => setDrawerVisible(true), className: 'fa fa-save',
      }],
  };

  function onValuesChange(fields: any) {
    const values: DraftPost = { ...fields };
    if ('createAt' in fields) {
      values.createAt = fields.createAt?.toISOString() || null;
    }
    return change(values);
  }

  function openDrafts() {
    flush();
    setDraftsOpen(true);
  }

  function switchDraft(draft: ArticleDraft) {
    if (draft.id === id) {
      setDraftsOpen(false);
      return;
    }
    const select = () => {
      setDraftsOpen(false);
      onSelect(draft);
    };
    if (flush()) {
      select();
    }
    else {
      Modal.confirm({
        title: t('localDraftStatus.error'),
        content: t('leaveUnsavedDraft'),
        onOk: select,
      });
    }
  }

  function onDeleted(deletedId: string) {
    if (deletedId === id) {
      discard();
      setDraftsOpen(false);
      onSelect({ id: newDraftId(), mode });
    }
  }

  const draftsDialog = <DraftDialog open={draftsOpen} activeId={id}
                                    onClose={() => setDraftsOpen(false)} onSelect={switchDraft} onDeleted={onDeleted}/>;

  async function onSubmit(values: any, onFinally: () => void) {
    const article = onValuesChange(values);
    flush();
    setSubmitting(true);
    try {
      if (articleId) {
        await articleService.updateById(articleId, article);
      }
      else {
        await articleService.create(article);
      }
      if (activeSession.current) {
        setDrawerVisible(false);
      }
      message.success(t(articleId ? 'updateSuccess' : 'publishSuccess'));
      if (complete(article)) {
        message.success(t('localDraftCleaned'));
        if (activeSession.current) {
          history.replace('/blog/articles');
        }
      }
      else {
        message.warning(t('localDraftRetained'));
      }
    }
    catch (error) {
      showHttpErrorMessage(error);
    }
    finally {
      setSubmitting(false);
      onFinally();
    }
  }

  if (loading) {
    return <PageLoading/>;
  }
  if (loadFailed) {
    return <>
      <Alert type="error" message={t('localDraftLoadFailed')}
             action={<Button onClick={openDrafts}>{t('localDrafts')}</Button>}/>
      {draftsDialog}
    </>;
  }

  return <>
    <div className="container shadow-box" style={{ marginTop: 22 }}>
      <div className="data-list-title" style={{ borderLeft: 'none', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
        <span style={{ flexShrink: 0 }}>{t(articleId ? 'editArticle' : 'write')}</span>
        <Space style={{ fontWeight: 'normal' }}>
          <span role="status" style={{ color: status === 'error' ? '#cf1322' : undefined }}>
            {t(`localDraftStatus.${status}`)}{savedAt ? ` · ${new Date(savedAt).toLocaleString()}` : ''}
          </span>
          <Button disabled={submitting} onClick={openDrafts}>{t('localDrafts')}</Button>
        </Space>
      </div>
      <div className="data" style={{ marginTop: 10 }}>
        <input value={post.title || ''} maxLength={80} autoFocus autoComplete="off"
               placeholder={t('titlePlaceholder')} className="article-title"
               onChange={e => change({ title: e.target.value })} style={{ marginLeft: 10 }}/>
        {mode === 'markdown' ? <div id="markdown-editor">
          <MarkdownEditor setEditor={setEditor} value={post.markdown}
                          onChange={(markdown, content) => change({ markdown, content })} options={editorOptions}/>
        </div> : <div id="editor">
          <Suspense fallback={<PageLoading/>}>
            <RichTextEditor post={post} saveContent={content => change({ content })}/>
          </Suspense>
        </div>}
        <div style={{ textAlign: 'center', padding: 10 }}>
          <Button type="primary" loading={submitting} onClick={() => setDrawerVisible(true)}>
            <PlusOutlined/> {t('saveArticle')}
          </Button>
        </div>
      </div>
    </div>
    <BackTop/>
    {draftsDialog}
    {mode === 'markdown' && <ImageChooserModal visible={modalVisible} hideModal={() => setModalVisible(false)} onSelect={imageCallback}/>}
    <ArticleDrawer onValuesChange={onValuesChange} article={post} visible={drawerVisible}
                   onSubmit={onSubmit} onClose={() => setDrawerVisible(false)}/>
  </>;
}
