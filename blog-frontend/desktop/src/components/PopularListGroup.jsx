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
import ListGroup from '../components/ListGroup';
import { arrayNotEquals } from 'core';
import { Link } from "react-router-dom";


const popularItemFunc = (article, idx) => {
  return (
      <Link
          key={ idx }
          style={ { whiteSpace: 'normal' } }
          className='list-group-item'
          to={ `/articles/${ article.uri }` }
          title={ article.title }>{ article.title }
      </Link>
  )
}

export default class PopularListGroup extends React.Component {

  shouldComponentUpdate(nextProps, nextState) {
    return arrayNotEquals(this.props.items, nextProps.items)
  }

  render() {
    return (
        <ListGroup
            { ...this.props }
            errorKey='popularArticles'
            title="最受欢迎的文章"
            itemFunc={ popularItemFunc }
        />
    )
  }
}
