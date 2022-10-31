export interface LoggingItem {
  id: (number | string);

  title: string;
  ip: string;
  user: string;
  content: string;
  type: string;
  result: string;

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
  list: LoggingItem[];
  pagination: Partial<TableListPagination>;
}
