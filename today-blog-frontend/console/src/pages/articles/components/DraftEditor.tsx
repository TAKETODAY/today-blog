import { lazy, Suspense, useEffect, useRef, useState } from 'react';
import { history, useLocation, useModel, useParams } from '@umijs/max';
import { Alert, BackTop, Button, message, Modal } from 'antd';
import { PageLoading } from '@ant-design/pro-layout';
import { FolderOpenOutlined, SaveOutlined } from '@ant-design/icons';
import MarkdownEditor from '@/components/Editor';
import ImageChooserModal from '@/components/ImageChooserModal';
import { Attachment } from '@/components/Attachment/data';
import { useOptions } from '@/components/hooks';
import { showHttpErrorMessage } from '@/utils';
import { articleMessage as t } from '@/utils/article-i18n';
import articleService from '@/services/ArticleService';
import ArticleDrawer from './ArticleSettingsDrawer';
import { ArticleDraft, DraftPost, EditorMode, draftPost, newDraftId } from '../drafts/storage';
import DraftDialog from '../drafts/DraftDialog';
import { useDraft } from '../drafts/useDraft';
import { articleBaseUrl, renderArticleMarkdown } from 'core/article-markdown';
import '@/assets/css/index.css';
import './DraftEditor.less';

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
  const [options] = useOptions();
  const siteHost = options['site.host'];
  const [editor, setEditor] = useState<any>();
  const [modalVisible, setModalVisible] = useState(false);
  const [drawerVisible, setDrawerVisible] = useState(false);
  const [draftsOpen, setDraftsOpen] = useState(false);
  const [loading, setLoading] = useState(Boolean(articleId && !restored));
  const [loadFailed, setLoadFailed] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const activeSession = useRef(true);
  const latestPost = useRef(post);
  latestPost.current = post;

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
    const image = document.createElement('img');
    image.setAttribute('src', `${options['site.cdn'] || ''}${attachment.uri}`);
    image.setAttribute('alt', attachment.name);
    cm.replaceSelection(image.outerHTML);
    cm.focus();
    setModalVisible(false);
  }

  const editorOptions = {
    autosave: false,
    autofocus: false,
    autoDownloadFontAwesome: false,
    placeholder: t('writingPlaceholder'),
    renderMarkdown: (markdown: string) => renderArticleMarkdown(markdown,
      articleBaseUrl(siteHost, latestPost.current.uri || articleId)),
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
    const article = onValuesChange(mode === 'markdown' ? {
      ...values,
      content: renderArticleMarkdown(post.markdown, articleBaseUrl(siteHost, values.uri || post.uri || articleId)),
    } : { ...values, markdown: '' });
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
    <div className="article-editor-shell">
      <header className="article-editor-header">
        <div className="article-editor-heading">
          <h1>{t(articleId ? 'editArticle' : 'write')}</h1>
          <span className="article-editor-mode">{t(mode)}</span>
        </div>
        <div className="article-editor-header-actions">
          <span className={`article-editor-save-status is-${status}`} role="status" aria-live="polite">
            <span className="article-editor-status-dot"/>
            <span>{t(`localDraftStatus.${status}`)}</span>
            {status === 'saved' && savedAt !== undefined && <span className="article-editor-save-time">{new Date(savedAt).toLocaleString()}</span>}
          </span>
          <div className="article-editor-buttons">
            <Button className="article-editor-drafts-button" icon={<FolderOpenOutlined/>} disabled={submitting} onClick={openDrafts}>
              {t('localDrafts')}
            </Button>
            <Button type="primary" icon={<SaveOutlined/>} loading={submitting} onClick={() => setDrawerVisible(true)}>
              {t('saveArticle')}
            </Button>
          </div>
        </div>
      </header>
      <div className="article-editor-content">
        <input value={post.title || ''} maxLength={80} autoFocus autoComplete="off"
               aria-label={t('articleTitle')} placeholder={t('titlePlaceholder')} className="article-editor-title"
               onChange={e => change({ title: e.target.value })}/>
        {mode === 'markdown' ? <div id="markdown-editor" className="article-editor-stage">
          <MarkdownEditor setEditor={setEditor} value={post.markdown}
                          onChange={(markdown, content) => change({ markdown, content })} options={editorOptions}/>
        </div> : <div id="editor" className="article-editor-stage">
          <Suspense fallback={<PageLoading/>}>
            <RichTextEditor post={post} saveContent={content => change({ content })}/>
          </Suspense>
        </div>}
        <footer className="article-editor-footer">
          {t('localDraftHint')}
        </footer>
      </div>
    </div>
    <BackTop/>
    {draftsDialog}
    {mode === 'markdown' && <ImageChooserModal visible={modalVisible} hideModal={() => setModalVisible(false)} onSelect={imageCallback}/>}
    <ArticleDrawer onValuesChange={onValuesChange} article={post} visible={drawerVisible}
                   onSubmit={onSubmit} onClose={() => setDrawerVisible(false)}/>
  </>;
}
