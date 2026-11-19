/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

import { Avatar, Card, Col, Row, Skeleton, Statistic, Table, Tag } from 'antd';
import React, { useEffect } from 'react';

import { connect, Dispatch, Link } from '@umijs/max';
import { PageHeaderWrapper } from '@ant-design/pro-layout';
import { ModalState } from './model';
import EditableLinkGroup, { EditableLink } from './components/EditableLinkGroup';
import styles from './style.less';
import { DashboardComment, DashboardLog, DashboardStatistics } from './data.d';
import { format, fromNow, getCommentStatusDesc, isEmpty } from "@/utils";
import Statistics from "@/pages/workplace/components/Statistics";
import Image from "@/components/Image";
import { useUserSession } from "@/components/hooks";
import { ArticleLink } from "@/components/Article";
import { useSiteTranslation } from '@/utils/use-site-translation';

type Translate = ReturnType<typeof useSiteTranslation>;

const links = (t: Translate): EditableLink[] => [
  {
    title: t('写文章', 'Write an article'),
    href: '/articles/write',
    target: '_blank'
  },
  {
    title: t('全部文章', 'All articles'),
    href: '/blog/articles',
  },
  {
    title: t('文章分类', 'Categories'),
    href: '/blog/categories',
  },
  {
    title: t('文章标签', 'Tags'),
    href: '/blog/labels',
  },
  {
    title: t('文章评论', 'Comments'),
    href: '/blog/comments',
  },
  {
    title: t('用户管理', 'Users'),
    href: '/user/list',
  },
  {
    title: t('附件管理', 'Attachments'),
    href: '/attachment/list',
  },
  {
    title: t('变量管理', 'Site options'),
    href: '/system/options',
  },
];

interface WorkplaceProps {
  statistics: DashboardStatistics;
  dispatch: Dispatch;
  loading: boolean;
}

const PageHeaderContent: React.FC<{}> = () => {
  const t = useSiteTranslation();
  const [currentUser] = useUserSession()
  if (isEmpty(currentUser)) {
    return <Skeleton avatar paragraph={{ rows: 1 }} active/>;
  }
  return (
      <div className={styles.pageHeaderContent}>
        <div className={styles.avatar}>
          <Avatar size="large" icon={<Image alt={t('头像', 'Avatar')} src={currentUser.avatar}/>}/>
        </div>
        <div className={styles.content}>
          <div className={styles.contentTitle}>
            {t(`您好，${currentUser.name}`, `Hello, ${currentUser.name}`)}
          </div>
          <div>
            {currentUser.email} | {currentUser.introduce}
          </div>
        </div>
      </div>
  );
};

const ExtraContent: React.FC<{ statistics: DashboardStatistics }> = ({ statistics }) => {
  const t = useSiteTranslation();
  if (isEmpty(statistics)) {
    return <Skeleton paragraph={{ rows: 1 }} active/>;
  }
  return (
      <div className={styles.extraContent}>
        <div className={styles.statItem}>
          <Statistic title={t('文章数', 'Articles')} value={statistics.articleCount}/>
        </div>
        <div className={styles.statItem}>
          <Statistic title={t('评论数', 'Comments')} value={statistics.commentCount}/>
        </div>
        <div className={styles.statItem}>
          <Statistic title={t('附件数', 'Attachments')} value={statistics.attachmentCount}/>
        </div>
        <div className={styles.statItem}>
          <Statistic title={t('上次启动时间', 'Last startup')} value={format(statistics.lastStartup)}/>
        </div>
      </div>
  )
}

const loggingColumns = (t: Translate) => [
  {
    title: t('事件', 'Event'),
    key: 'title',
    width: 120,
    render: (log: DashboardLog) => {
      return <Tag title={log.title}>{log.title}</Tag>
    }
  },
  {
    title: t('内容', 'Details'),
    dataIndex: 'content',
    key: 'content',
    width: 260,
  },
  {
    title: t('IP 地址', 'IP address'),
    dataIndex: 'ip',
    key: 'ip',
    width: 120,
  },
  {
    title: t('时间', 'Time'),
    dataType: 'datetime',
    width: 160,
    render: (log: DashboardLog) => {
      return format(log.createAt)
    }
  },
]

const commentColumns = (t: Translate) => [
  {
    title: t('评论内容', 'Comment'),
    render: (comment: DashboardComment) => {
      return <div dangerouslySetInnerHTML={{ __html: comment.content }}/>
    }
  },
  {
    title: t('状态', 'Status'),
    width: 80,
    render: (comment: DashboardComment) => {
      const statusDesc = getCommentStatusDesc(comment.status)
      return <Tag title={statusDesc}>{statusDesc}</Tag>
    }
  },
  {
    title: t('时间', 'Time'),
    width: 240,
    render: (comment: DashboardComment) => {
      return format(comment.createAt)
    }
  },
]


const Workplace = (props: WorkplaceProps) => {
  const t = useSiteTranslation();
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
                  title={t('最新文章', 'Latest articles')}
                  bordered={false}
                  extra={<Link to="/blog/articles">{t('全部文章', 'All articles')}</Link>}
                  loading={loading}
                  bodyStyle={{ padding: 0 }}
            >
              {statistics?.articles?.map((article) => (
                  <Card.Grid className={styles.projectGrid} key={article.id}>
                    <Card bodyStyle={{ padding: 0 }} bordered={false} key={article.id}>
                      <Card.Meta
                          title={
                            <div className={styles.cardTitle}>
                              <Avatar size="small" src={article.cover}/>
                              <ArticleLink target='_blank' article={article}/>
                            </div>
                          }
                          description={article.summary}
                      />
                      <div className={styles.projectItemContent}>
                        <ArticleLink target='_blank' article={article}>{t('浏览', 'View')}</ArticleLink>
                        <span className={styles.datetime} title={format(article.updateAt)}>
                            {fromNow(article.updateAt)}
                          </span>
                      </div>
                    </Card>
                  </Card.Grid>
              ))}
            </Card>
            <Card bodyStyle={{ padding: 0 }}
                  bordered={false}
                  className={styles.activeCard}
                  title={t('最近日志', 'Recent logs')}
                  loading={loading}
            >
              <Table scroll={{ x: 600, y: 600 }}
                     columns={loggingColumns(t)} rowKey='id' dataSource={statistics?.logs} pagination={false}/>
            </Card>
          </Col>
          <Col xl={8} lg={24} md={24} sm={24} xs={24}>
            <Card style={{ marginBottom: 24 }}
                  title={t('快速开始 / 便捷导航', 'Quick links')}
                  bordered={false}
                  bodyStyle={{ padding: 0 }}
            >
              <EditableLinkGroup links={links(t)} linkElement={Link}/>
            </Card>
            <Card bodyStyle={{ padding: 0 }}
                  bordered={false}
                  title={t('最近评论', 'Recent comments')}
                  loading={loading}
            >
              <div className={styles.members}>
                <Table scroll={{ x: 600, y: 600 }} columns={commentColumns(t)} rowKey='id'
                       dataSource={statistics?.comments} pagination={false}/>
              </div>
            </Card>
          </Col>
        </Row>

        <Row gutter={24} style={{ marginTop: 24 }}>
          <Col xl={16} lg={24} md={24} sm={24} xs={24}>
            <Card style={{ marginBottom: 24 }}
                  title={t('统计分析', 'Analytics')}
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
