export interface UserItem {
  id: number;

  status: string;
  name: string;
  email: string;
  site: string;
  type: string;
  image: string;

  background: string;
  notification: string;
  isBlogger: string;

  defaultPassword: boolean;
}

export interface TableListPagination {
  total: number;
  pageSize: number;
  current: number;
}

export interface TableListData {
  list: UserItem[];
  pagination: Partial<TableListPagination>;
}
