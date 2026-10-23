/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2026 All Rights Reserved.
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

import { Pagination } from 'antd';
import 'antd/es/pagination/style';
import React, { useEffect, useState } from 'react';
import ArticleList from '../components/ArticleList';
import { applySEO, extractData, scrollTop } from 'core';
import { useDispatch } from "react-redux";
import { useLocation, useNavigate } from 'react-router-dom';
import { updateNavigations } from "../redux/actions";
import { articleService } from '../services';
import { setTitle } from "../utils/common"

function positiveInteger(value, fallback) {
  const number = Number(value);
  return Number.isSafeInteger(number) && number > 0 ? number : fallback;
}

export default function Home() {
  const dispatch = useDispatch();
  const location = useLocation();
  const navigate = useNavigate();
  const params = new URLSearchParams(location.search);
  const page = positiveInteger(params.get('page'), 1);
  const size = positiveInteger(params.get('size'), 10);
  const [articles, setArticles] = useState({ total: 0, current: page, size, data: [] });
  const [error, setError] = useState();

  useEffect(() => {
    setTitle("代码是我心中的一首诗");
    applySEO();
    dispatch(updateNavigations(null));
  }, [dispatch]);

  useEffect(() => {
    let active = true;
    //scrollTop();
    setError(undefined);
    setArticles(previous => ({ ...previous, data: undefined }));
    articleService.fetchHomeArticles(page, size)
      .then(extractData)
      .then(result => {
        if (active) {
          setArticles(result);
        }
      })
      .catch(error => {
        if (active) {
          setError({
            status: error.response ? error.response.status : 500,
            subTitle: error.message,
          });
        }
      });
    return () => {
      active = false;
    };
  }, [page, size]);

  function pageURL(page, size) {
    const params = new URLSearchParams(location.search);
    params.set('page', page);
    params.set('size', size);
    return `${location.pathname}?${params}`;
  }

  function changePage(page, size) {
    navigate(pageURL(page, size));
  }

  function renderPageItem(page, type, element) {
    if (type !== 'page' && type !== 'jump-prev' && type !== 'jump-next') {
      return element;
    }
    return React.cloneElement(element, {
      href: pageURL(page, size),
      rel: undefined,
      onClick: event => {
        if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || event.button !== 0) {
          event.stopPropagation();
          return;
        }
        event.preventDefault();
      },
    });
  }

  return <>
    <div className="shadow-box" id="test1">
      <div className="data-list-title">最新文章</div>
      <ArticleList articles={articles.data} error={error} title="最新文章" showPinned/>
      <div align='center' style={{ padding: '20px' }}>
        <Pagination
          showQuickJumper
          showSizeChanger
          total={articles.total}
          onChange={changePage}
          itemRender={renderPageItem}
          pageSize={size}
          current={page}
          showTotal={n => <><b className='red'>{n}</b>篇文章</>}
        />
      </div>
    </div>
  </>
}
