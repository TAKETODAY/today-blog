import { useRef, useState } from 'react';
import { message, Modal, Popconfirm } from 'antd';
import { PageContainer } from '@ant-design/pro-layout';
import ProTable, { ActionType, ProColumns } from '@ant-design/pro-table';
import { request } from '@umijs/max';

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
  const actionRef = useRef<ActionType>();
  const [selected, setSelected] = useState<MailItem>();

  const retry = async (record: MailItem) => {
    try {
      await request(`/api/console/mails/${record.id}/retry`, { method: 'POST' });
      message.success('已加入发送队列');
      actionRef.current?.reload();
    } catch {
      message.error('重发失败，请刷新后重试');
    }
  };

  const columns: ProColumns<MailItem>[] = [
    { title: '收件人', dataIndex: 'to', width: 220 },
    { title: '主题', dataIndex: 'subject', width: 300 },
    {
      title: '状态', dataIndex: 'status', width: 100,
      valueEnum: {
        PENDING: { text: '待发送', status: 'Default' },
        SENDING: { text: '发送中', status: 'Processing' },
        SENT: { text: '已发送', status: 'Success' },
        FAILED: { text: '发送失败', status: 'Error' },
      },
    },
    { title: '尝试次数', dataIndex: 'attempts', hideInSearch: true, width: 100 },
    { title: '失败原因', dataIndex: 'lastError', hideInSearch: true, width: 300, ellipsis: true },
    { title: '创建时间', dataIndex: 'createAt', valueType: 'dateTime', hideInSearch: true, width: 180 },
    { title: '最近尝试', dataIndex: 'lastAttemptAt', valueType: 'dateTime', hideInSearch: true, width: 180 },
    { title: '发送成功时间', dataIndex: 'sentAt', valueType: 'dateTime', hideInSearch: true, width: 180 },
    {
      title: '操作', valueType: 'option', width: 140, fixed: 'right',
      render: (_, record) => [
        <a key="detail" onClick={() => setSelected(record)}>查看</a>,
        record.status === 'FAILED' && (
          <Popconfirm key="retry" title="确定重新发送这封邮件吗？" onConfirm={() => retry(record)}>
            <a>重发</a>
          </Popconfirm>
        ),
      ],
    },
  ];

  return (
    <PageContainer>
      <ProTable<MailItem>
        rowKey="id"
        headerTitle="邮件发送记录"
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
        {selected?.lastError && <p style={{ whiteSpace: 'pre-wrap' }}>失败原因：{selected.lastError}</p>}
        <iframe title="邮件内容" sandbox="" srcDoc={selected?.content}
          style={{ width: '100%', height: 450, border: 0 }} />
      </Modal>
    </PageContainer>
  );
};
