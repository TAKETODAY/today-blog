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
      message.success(t('已加入发送队列', 'Queued for sending'));
      actionRef.current?.reload();
    } catch {
      message.error(t('重发失败，请刷新后重试', 'Could not resend. Refresh and try again.'));
    }
  };

  const columns: ProColumns<MailItem>[] = [
    { title: t('收件人', 'Recipient'), dataIndex: 'to', width: 220 },
    { title: t('主题', 'Subject'), dataIndex: 'subject', width: 300 },
    {
      title: t('状态', 'Status'), dataIndex: 'status', width: 100,
      valueEnum: {
        PENDING: { text: t('待发送', 'Pending'), status: 'Default' },
        SENDING: { text: t('发送中', 'Sending'), status: 'Processing' },
        SENT: { text: t('已发送', 'Sent'), status: 'Success' },
        FAILED: { text: t('发送失败', 'Failed'), status: 'Error' },
      },
    },
    { title: t('尝试次数', 'Attempts'), dataIndex: 'attempts', hideInSearch: true, width: 100 },
    { title: t('失败原因', 'Failure reason'), dataIndex: 'lastError', hideInSearch: true, width: 300, ellipsis: true },
    { title: t('创建时间', 'Created'), dataIndex: 'createAt', render: (_, item) => format(item.createAt), hideInSearch: true, width: 180 },
    { title: t('最近尝试', 'Last attempt'), dataIndex: 'lastAttemptAt', render: (_, item) => format(item.lastAttemptAt), hideInSearch: true, width: 180 },
    { title: t('发送成功时间', 'Sent at'), dataIndex: 'sentAt', render: (_, item) => format(item.sentAt), hideInSearch: true, width: 180 },
    {
      title: t('操作', 'Actions'), valueType: 'option', width: 140, fixed: 'right',
      render: (_, record) => [
        <a key="detail" onClick={() => setSelected(record)}>{t('查看', 'View')}</a>,
        record.status === 'FAILED' && (
          <Popconfirm key="retry" title={t('确定重新发送这封邮件吗？', 'Resend this email?')} onConfirm={() => retry(record)}>
            <a>{t('重发', 'Resend')}</a>
          </Popconfirm>
        ),
      ],
    },
  ];

  return (
    <PageContainer>
      <ProTable<MailItem>
        rowKey="id"
        headerTitle={t('邮件发送记录', 'Email delivery history')}
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
        {selected?.lastError && <p style={{ whiteSpace: 'pre-wrap' }}>{t('失败原因：', 'Failure reason: ')}{selected.lastError}</p>}
        <iframe title={t('邮件内容', 'Email content')} sandbox="" srcDoc={selected?.content}
          style={{ width: '100%', height: 450, border: 0 }} />
      </Modal>
    </PageContainer>
  );
};
