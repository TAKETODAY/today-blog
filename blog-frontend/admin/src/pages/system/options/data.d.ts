export interface ArticleItem {
  id: number;

  title: string;
  image: string;
  author: string;
  summary: string;
  content: string;
  markdown: string;
  status: string;
  labels: LabelItem[];

  category: string;
  pv: number; //浏览量

  lastModify: Date;
  createTime: Date;
}

export interface CategoryItem {

  name: string;
  order: number;
  articleCount: number
  description: string;

  lastModify: Date;
  createTime: Date;
}

export interface LabelItem {
  id: number;
  name: string;
}

export interface TableListPagination {
  total: number;
  pageSize: number;
  current: number;
}

export interface TableListData {
  list: ArticleItem[];
  pagination: Partial<TableListPagination>;
}
