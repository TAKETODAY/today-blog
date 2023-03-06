/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

export interface ArticleItem {
  id: number;

  title: string;
  cover: string;
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

export interface CommentItem {
  id: number;
  content: string;
  articleId: number
  status: string;
  user: string;

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
