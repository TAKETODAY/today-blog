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
  copyright: string
  id: number
  cover: string
  updateAt: string
  createAt: string
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
