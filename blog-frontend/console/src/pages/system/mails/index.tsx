import { useRef, useState } from 'react';
import { message, Modal, Popconfirm } from 'antd';
import { PageContainer } from '@ant-design/pro-layout';
import ProTable, { ActionType, ProColumns } from '@ant-design/pro-table';
import { request } from '@umijs/max';
import { format } from '@/utils';
import { useSiteTranslation } from '@/utils/use-site-translation';

type MailItem = {
  id: number;
  to: string;
  subject: string;
  content: string;
  status: 'PENDING' | 'SENDING' | 'SENT' | 'FAILED';
  attempts: number;
  lastError?: string;
  createAt: string;
  lastAttemptAt?: string;
  sentAt?: string;
};

export default () => {
  const t = useSiteTranslation();
  const actionRef = useRef<ActionType>();
  const [selected, setSelected] = useState<MailItem>();

  const retry = async (record: MailItem) => {
    try {
      await request(`/api/console/mails/${record.id}/retry`, { method: 'POST' });
      message.success(t('mail.queuedForSending'));
      actionRef.current?.reload();
    } catch {
      message.error(t('mail.couldNotResendRefreshAnd'));
    }
  };

  const columns: ProColumns<MailItem>[] = [
    { title: t('mail.recipient'), dataIndex: 'to', width: 220 },
    { title: t('mail.subject'), dataIndex: 'subject', width: 300 },
    {
      title: t('mail.status'), dataIndex: 'status', width: 100,
      valueEnum: {
        PENDING: { text: t('mail.pending'), status: 'Default' },
        SENDING: { text: t('mail.sending'), status: 'Processing' },
        SENT: { text: t('mail.sent'), status: 'Success' },
        FAILED: { text: t('mail.failed'), status: 'Error' },
      },
    },
    { title: t('mail.attempts'), dataIndex: 'attempts', hideInSearch: true, width: 100 },
    { title: t('mail.failureReason'), dataIndex: 'lastError', hideInSearch: true, width: 300, ellipsis: true },
    { title: t('mail.created'), dataIndex: 'createAt', render: (_, item) => format(item.createAt), hideInSearch: true, width: 180 },
    { title: t('mail.lastAttempt'), dataIndex: 'lastAttemptAt', render: (_, item) => format(item.lastAttemptAt), hideInSearch: true, width: 180 },
    { title: t('mail.sentAt'), dataIndex: 'sentAt', render: (_, item) => format(item.sentAt), hideInSearch: true, width: 180 },
    {
      title: t('mail.actions'), valueType: 'option', width: 140, fixed: 'right',
      render: (_, record) => [
        <a key="detail" onClick={() => setSelected(record)}>{t('mail.view')}</a>,
        record.status === 'FAILED' && (
          <Popconfirm key="retry" title={t('mail.resendThisEmail')} onConfirm={() => retry(record)}>
            <a>{t('mail.resend')}</a>
          </Popconfirm>
        ),
      ],
    },
  ];

  return (
    <PageContainer>
      <ProTable<MailItem>
        rowKey="id"
        headerTitle={t('mail.emailDeliveryHistory')}
        actionRef={actionRef}
        columns={columns}
        scroll={{ x: 1800 }}
        request={async ({ current, pageSize, ...params }) => {
          const result = await request('/api/console/mails', {
            params: { ...params, page: current, size: pageSize },
          });
          return { ...result, current: result.current, pageSize: result.size };
        }}
      />
      <Modal title={selected?.subject} visible={!!selected} footer={null} width={800}
        onCancel={() => setSelected(undefined)}>
        {selected?.lastError && <p style={{ whiteSpace: 'pre-wrap' }}>{t('mail.failureReasonPrefix')}{selected.lastError}</p>}
        <iframe title={t('mail.emailContent')} sandbox="" srcDoc={selected?.content}
          style={{ width: '100%', height: 450, border: 0 }} />
      </Modal>
    </PageContainer>
  );
};
