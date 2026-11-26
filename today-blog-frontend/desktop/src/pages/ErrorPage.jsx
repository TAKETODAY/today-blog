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

import React from 'react';
import head from '../assets/images/404/head.png';
import { Button } from "antd";
import 'antd/es/button/style';
import { translate } from '../locales';

const errors = {
  '400': ['errors.400.title', 'errors.400.detail'],
  '403': ['errors.403.title', 'errors.403.detail'],
  '404': ['errors.404.title', 'errors.404.detail'],
  '405': ['errors.405.title', 'errors.405.detail'],
  '500': ['errors.500.title', 'errors.500.detail'],
}

export default props => {
  const status = Object.hasOwn(errors, props.status) ? props.status : '500';
  const [titleKey, detailKey] = errors[status];
  const title = `${status} ${translate(titleKey)}`;
  document.title = title;
  return (<>
    <div className="container" style={{ marginTop: '100px' }} align="center">
      <div className="img">
        <img src={head} alt=""/>
      </div>
      <h1 style={{ fontSize: '48px' }}>{title}</h1>
      <div id="msg" style={{ fontSize: '15px', marginBottom: 20 }}>{translate(detailKey)}</div>

      <Button onClick={() => window.location.replace('/')} style={{ marginRight: 10 }}>{translate('navigation.home')}</Button>

      <Button type="primary" onClick={() => window.history.back()}>{translate('errors.back')}</Button>
    </div>
  </>)
}
