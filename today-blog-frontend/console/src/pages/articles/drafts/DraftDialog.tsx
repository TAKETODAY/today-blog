import { useEffect, useState } from 'react';
import { useModel } from '@umijs/max';
import { Alert, Button, message, Modal, Popconfirm, Space, Table, Tag } from 'antd';
import { articleMessage as t } from '@/utils/article-i18n';
import { ArticleDraft, deleteDraft, EditorMode, importLegacyDrafts, listDrafts, newDraftId, saveDraft } from './storage';

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
  return <Modal title={t('localDrafts')} open={props.open} onCancel={props.onClose}
                width={900} footer={null} destroyOnClose>
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
    <Space wrap style={{ marginBottom: 16 }}>
      <Button type="primary" onClick={() => create('markdown')}>{t('newLocalDraft')} · {t('markdown')}</Button>
      <Button onClick={() => create('richText')}>{t('newLocalDraft')} · {t('richText')}</Button>
      <Popconfirm title={t('confirmImportLegacyDraft')} onConfirm={importOldDrafts}>
        <Button>{t('importLegacyDraft')}</Button>
      </Popconfirm>
    </Space>
    <p>{t('localDraftHint')}</p>
    {error && <Alert type="error" showIcon message={t('localDraftStatus.error')}/>}
    {damaged && <Alert type="warning" showIcon message={t('localDraftDamaged')}/>}
    <Table<ArticleDraft> rowKey="id" dataSource={drafts} scroll={{ x: 650 }} columns={[
      { title: t('title'), render: (_, draft) => <Space wrap>
        {draft.post.title?.trim() || t('untitledDraft')}
        {draft.id === activeId && <Tag color="processing">{t('currentDraft')}</Tag>}
      </Space> },
      {
        title: t('article'), render: (_, draft) => <Tag>{draft.articleId
          ? `${t('existingArticleDraft')} #${draft.articleId}` : t('newArticleDraft')}</Tag>
      },
      { title: t('content'), render: (_, draft) => t(draft.mode) },
      { title: t('updatedAt'), render: (_, draft) => new Date(draft.savedAt).toLocaleString() },
      {
        title: t('actions'), render: (_, draft) => <Space>
          <Button type="link" onClick={() => onSelect(draft)}>{t('resumeDraft')}</Button>
          <Popconfirm title={t('confirmDeleteLocalDraft')} onConfirm={() => remove(draft.id)}>
            <Button type="link" danger>{t('delete')}</Button>
          </Popconfirm>
        </Space>
      },
    ]}/>
  </>;
}
