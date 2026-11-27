import { useEffect, useState } from 'react';
import { useModel } from '@umijs/max';
import { Alert, Button, Empty, message, Modal, Popconfirm, Tag } from 'antd';
import { DeleteOutlined, FileTextOutlined, FolderOpenOutlined, PlusOutlined } from '@ant-design/icons';
import { articleMessage as t } from '@/utils/article-i18n';
import { ArticleDraft, deleteDraft, EditorMode, importLegacyDrafts, listDrafts, newDraftId, saveDraft } from './storage';
import './DraftDialog.less';

interface Props {
  open: boolean;
  onClose: () => void;
  onSelect: (draft: ArticleDraft) => void;
  activeId?: string;
  onDeleted?: (id: string) => void;
}

export default function DraftDialog(props: Props) {
  const { initialState } = useModel('@@initialState');
  const userId = initialState?.currentUser?.id;
  if (userId == null) {
    return null;
  }
  return <Modal className="draft-dialog"
                title={<div className="draft-dialog-title">
                  <span className="draft-dialog-title-icon"><FolderOpenOutlined/></span>
                  <div>
                    <div className="draft-dialog-title-main">{t('localDrafts')}</div>
                    <div className="draft-dialog-title-hint">{t('localDraftHint')}</div>
                  </div>
                </div>}
                open={props.open} onCancel={props.onClose} width={720} footer={null} destroyOnClose>
    <DraftList key={userId} userId={userId} {...props}/>
  </Modal>;
}

function DraftList({ userId, open, onSelect, activeId, onDeleted }: Props & { userId: number }) {
  const [drafts, setDrafts] = useState<ArticleDraft[]>([]);
  const [error, setError] = useState(false);
  const [damaged, setDamaged] = useState(false);

  function reload() {
    try {
      const result = listDrafts(userId);
      setDrafts(result.drafts);
      setDamaged(result.damaged);
      setError(false);
    }
    catch {
      setError(true);
    }
  }

  useEffect(() => {
    if (!open) {
      return undefined;
    }
    reload();
    window.addEventListener('storage', reload);
    window.addEventListener('focus', reload);
    return () => {
      window.removeEventListener('storage', reload);
      window.removeEventListener('focus', reload);
    };
  }, [userId, open]);

  function remove(id: string) {
    try {
      deleteDraft(userId, id);
      reload();
      onDeleted?.(id);
    }
    catch {
      setError(true);
    }
  }

  function importOldDrafts() {
    try {
      if (importLegacyDrafts(userId)) {
        message.success(t('legacyDraftImported'));
      }
      else {
        message.warning(t('legacyDraftImportFailed'));
      }
      reload();
    }
    catch {
      setError(true);
    }
  }

  function create(mode: EditorMode) {
    const draft: ArticleDraft = { version: 1, id: newDraftId(), mode, savedAt: Date.now(), post: {} };
    try {
      saveDraft(userId, draft);
    }
    catch {
      message.warning(t('localDraftStatus.error'));
    }
    reload();
    onSelect(draft);
  }

  return <>
    <div className="draft-dialog-toolbar">
      <span className="draft-dialog-summary">{t('localDraftCount', { count: drafts.length })}</span>
      <div className="draft-dialog-create">
        <Button type="primary" icon={<PlusOutlined/>} onClick={() => create('markdown')}>{t('newLocalDraft')} · {t('markdown')}</Button>
        <Button onClick={() => create('richText')}>{t('newLocalDraft')} · {t('richText')}</Button>
      </div>
    </div>
    {error && <Alert type="error" showIcon message={t('localDraftStatus.error')}/>}
    {damaged && <Alert type="warning" showIcon message={t('localDraftDamaged')}/>}
    {drafts.length > 0 ? <ul className="draft-list" aria-label={t('localDrafts')}>
      {drafts.map(draft => <li key={draft.id} data-draft-id={draft.id} className="draft-list-item"
                              aria-current={draft.id === activeId ? 'true' : undefined}>
        <span className="draft-list-icon" aria-hidden="true"><FileTextOutlined/></span>
        <div className="draft-list-details">
          <div className="draft-list-title">
            <strong>{draft.post.title?.trim() || t('untitledDraft')}</strong>
            {draft.id === activeId && <Tag color="processing">{t('currentDraft')}</Tag>}
          </div>
          <div className="draft-list-meta">
            <span>{t(draft.mode)}</span>
            <span>{draft.articleId ? `${t('existingArticleDraft')} #${draft.articleId}` : t('newArticleDraft')}</span>
            <span>{new Date(draft.savedAt).toLocaleString()}</span>
          </div>
        </div>
        <div className="draft-list-actions">
          <Button className="draft-list-resume" onClick={() => onSelect(draft)}>{t('resumeDraft')}</Button>
          <Popconfirm title={t('confirmDeleteLocalDraft')} onConfirm={() => remove(draft.id)}>
            <Button className="draft-list-delete" type="text" icon={<DeleteOutlined/>} title={t('delete')} aria-label={t('delete')}/>
          </Popconfirm>
        </div>
      </li>)}
    </ul> : !error && <Empty className="draft-dialog-empty" image={Empty.PRESENTED_IMAGE_SIMPLE} description={t('noLocalDrafts')}/>}
    <div className="draft-dialog-footer">
      <Popconfirm title={t('confirmImportLegacyDraft')} onConfirm={importOldDrafts}>
        <Button type="link">{t('importLegacyDraft')}</Button>
      </Popconfirm>
    </div>
  </>;
}
