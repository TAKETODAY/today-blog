import { Avatar, Card, Col, Row, Skeleton, Statistic, Table, Tag } from 'antd';
import React, { useEffect } from 'react';

import { connect, Dispatch, Link } from 'umi';
import { PageHeaderWrapper } from '@ant-design/pro-layout';
import moment from 'moment';
import { ModalState } from './model';
import EditableLinkGroup, { EditableLink } from './components/EditableLinkGroup';
import styles from './style.less';
import { DashboardComment, DashboardLog, DashboardStatistics } from './data.d';
import { getCommentStatusDesc, isEmpty } from "@/utils";
import Statistics from "@/pages/workplace/components/Statistics";
import { useModel } from "@@/plugin-model/useModel";

const links: EditableLink[] = [
  {
    title: '写文章',
    href: '/articles/write',
    target: '_blank'
  },
  {
    title: '全部文章',
    href: '/blog/articles',
  },
  {
    title: '文章标签',
    href: '/blog/labels',
  },
  {
    title: '文章索引',
    href: '/blog/search',
  },
  {
    title: '文章分类',
    href: '/blog/categories',
  },
  {
    title: '用户管理',
    href: '/user',
  },
];

interface WorkplaceProps {
  statistics: DashboardStatistics;
  dispatch: Dispatch;
  loading: boolean;
}

const PageHeaderContent: React.FC<{}> = () => {
  const { initialState } = useModel("@@initialState")
  const { currentUser } = initialState;
  if (isEmpty(currentUser)) {
    return <Skeleton avatar paragraph={{ rows: 1 }} active/>;
  }
  return (
      <div className={styles.pageHeaderContent}>
        <div className={styles.avatar}>
          <Avatar size="large" src={currentUser.image}/>
        </div>
        <div className={styles.content}>
          <div className={styles.contentTitle}>
            您好，{currentUser.name}，祝你开心每一天！
          </div>
          <div>
            {currentUser.email} | {currentUser.introduce}
          </div>
        </div>
      </div>
  );
};

const ExtraContent: React.FC<{ statistics: DashboardStatistics }> = ({ statistics }) => {
  if (isEmpty(statistics)) {
    return <Skeleton paragraph={{ rows: 1 }} active/>;
  }
  return (
      <div className={styles.extraContent}>
        <div className={styles.statItem}>
          <Statistic title="文章数" value={statistics.articleCount}/>
        </div>
        <div className={styles.statItem}>
          <Statistic title="评论数" value={statistics.commentCount}/>
        </div>
        <div className={styles.statItem}>
          <Statistic title="附件数" value={statistics.attachmentCount}/>
        </div>
        <div className={styles.statItem}>
          <Statistic title="上次启动时间" value={moment(statistics.lastStartup).format("lll")}/>
        </div>
      </div>
  )
}

const loggingColumns = [
  {
    title: '事件',
    key: 'title',
    render: (log: DashboardLog) => {
      return <Tag title={log.title}>{log.title}</Tag>
    }
  },
  {
    title: '内容',
    dataIndex: 'content',
    key: 'content',
  },
  {
    title: 'IP地址',
    dataIndex: 'ip',
    key: 'ip',
  },
  {
    title: '时间',
    dataIndex: 'id',
    dataType: 'datetime',
    key: 'id',
    render: (log: DashboardLog) => {
      return moment(log.id).format("lll")
    }
  },
]

const commentColumns = [
  {
    title: '评论内容',
    render: (comment: DashboardComment) => {
      return <div dangerouslySetInnerHTML={{ __html: comment.content }}/>
    }
  },
  {
    title: '状态',
    width: 80,
    render: (comment: DashboardComment) => {
      const statusDesc = getCommentStatusDesc(comment.status)
      return <Tag title={statusDesc}>{statusDesc}</Tag>
    }
  },
  {
    title: '时间',
    width: 180,
    render: (comment: DashboardComment) => {
      return moment(comment.id).format("lll")
    }
  },
]


const Workplace = (props: WorkplaceProps) => {
  const { loading, statistics } = props

  useEffect(() => {
    const { dispatch } = props;
    dispatch({
      type: 'dashboardAndworkplace/init',
    });
    return () => {
      dispatch({
        type: 'dashboardAndworkplace/clear',
      });
    }
  }, [])

  return (
      <PageHeaderWrapper content={<PageHeaderContent/>}
                         extraContent={<ExtraContent statistics={statistics}/>}>

        <Row gutter={24}>
          <Col xl={16} lg={24} md={24} sm={24} xs={24}>
            <Card className={styles.projectList}
                  style={{ marginBottom: 24 }}
                  title="最新文章"
                  bordered={false}
                  extra={<Link to="/blog/articles">全部文章</Link>}
                  loading={loading}
                  bodyStyle={{ padding: 0 }}
            >
              {statistics?.articles?.map((article) => (
                  <Card.Grid className={styles.projectGrid} key={article.id}>
                    <Card bodyStyle={{ padding: 0 }} bordered={false} key={article.id}>
                      <Card.Meta
                          title={
                            <div className={styles.cardTitle}>
                              <Avatar size="small" src={article.image}/>
                              <a target='_blank' href={`/articles/${article.id}`}>{article.title}</a>
                            </div>
                          }
                          description={article.summary}
                      />
                      <div className={styles.projectItemContent}>
                        <a target='_blank' href={`/articles/${article.id}`}>浏览</a>
                        <span className={styles.datetime} title={moment(article.lastModify).format("lll")}>
                            {moment(article.lastModify).fromNow()}
                          </span>
                      </div>
                    </Card>
                  </Card.Grid>
              ))}
            </Card>
            <Card bodyStyle={{ padding: 0 }}
                  bordered={false}
                  className={styles.activeCard}
                  title="最近日志"
                  loading={loading}
            >
              <Table columns={loggingColumns} rowKey='id' dataSource={statistics?.logs} pagination={false}/>
            </Card>
          </Col>
          <Col xl={8} lg={24} md={24} sm={24} xs={24}>
            <Card style={{ marginBottom: 24 }}
                  title="快速开始 / 便捷导航"
                  bordered={false}
                  bodyStyle={{ padding: 0 }}
            >
              <EditableLinkGroup links={links} linkElement={Link}/>
            </Card>
            <Card bodyStyle={{ padding: 0 }}
                  bordered={false}
                  title="最近评论"
                  loading={loading}
            >
              <div className={styles.members}>
                <Table scroll={{ x: 600, y: 600 }} columns={commentColumns} rowKey='id'
                       dataSource={statistics?.comments} pagination={false}/>
              </div>
            </Card>
          </Col>
        </Row>

        <Row gutter={24} style={{ marginTop: 24 }}>
          <Col xl={16} lg={24} md={24} sm={24} xs={24}>
            <Card style={{ marginBottom: 24 }}
                  title="统计分析"
                  bordered={false}
                  loading={loading}
                  bodyStyle={{ padding: 0 }}
            >
              <Statistics/>
            </Card>
          </Col>
        </Row>
      </PageHeaderWrapper>
  );
}

export default connect(
    ({
       dashboardAndworkplace: { statistics },
       loading,
     }: {
      dashboardAndworkplace: ModalState;
      loading: {
        effects: {
          [key: string]: boolean;
        };
      };
    }) => ({
      statistics,
      loading: loading.effects['dashboardAndworkplace/fetchStatistics'],
    }),
)(Workplace);
