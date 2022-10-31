export interface VisitDataType {
  x: string;
  y: number;
}

export interface SearchDataType {
  index: number;
  keyword: string;
  count: number;
  range: number;
  status: number;
}

export interface GeographicType {
  province: {
    label: string;
    key: string;
  };
  city: {
    label: string;
    key: string;
  };
}

export interface NoticeType {
  id: string;
  title: string;
  logo: string;
  description: string;
  updatedAt: string;
  member: string;
  href: string;
  memberLink: string;
}

export interface Member {
  avatar: string;
  name: string;
  id: string;
}


interface DashboardComment {
  articleId: number
  content: string
  id: number
  replies: DashboardComment[]
  status: string
}

interface DashboardLog {
  content: string
  id: number
  ip: string
  result: string
  title: string
  type: string
  user: string
}

interface DashboardArticle {
  category: string
  content: string
  copyRight: string
  id: number
  image: string
  lastModify: number
  markdown: string
  status: string
  pv: number
  summary: string
  title: string
}

interface DashboardStatistics {
  logs: DashboardLog[]
  articles: DashboardArticle[]
  comments: DashboardComment[]
  lastStartup: number
  commentCount: number
  articleCount: number
  attachmentCount: number
}
