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

import { Skeleton } from 'antd';
import React from 'react';
import { getRandLabel, isEmpty } from '../utils';
import { Link } from "react-router-dom";
import { connect } from "react-redux";
import { updateHttpErrorMessage } from "../redux/actions";
import HttpError from "./http/HttpError";

class Labels extends React.Component {
  //
  // shouldComponentUpdate(nextProps, nextState) {
  //   return this.props.labels !== nextProps.labels
  // }

  render() {
    const { labels, error } = this.props;
    if (error) {
      return <HttpError { ...error } title="标签加载失败"/>
    }

    if (isEmpty(labels)) {
      return <Skeleton active/>
    }
    return (
        <div className="shadow-box" id="tagcloud">
          <div className="data-list-title">标签云</div>
          <div style={ { paddingTop: 10 } }>
            {
              labels.map((label, idx) => {
                return <Link key={ idx } to={ `/tags/${ label.name }` } className={ getRandLabel() } title={ label.name }>{ label.name }</Link>
              })
            }
          </div>
        </div>
    )
  }
}


function labelsError(state) {
  return { error: state.http.errors?.tags }
}

export default connect(labelsError, {
  updateHttpErrorMessage
})(Labels)

