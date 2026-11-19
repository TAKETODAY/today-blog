import { Result, Skeleton } from 'antd';
import 'antd/es/result/style';
import 'antd/es/skeleton/style';
import React from 'react';
import { isEmpty, translate } from 'core';
import { connect } from "react-redux";
import { updateHttpErrorMessage } from "../redux/actions";
import HttpError from "./http/HttpError";


class ListGroup extends React.Component {

  render() {
    const { items, itemFunc, title, errors, errorKey, errorTitle } = this.props;
    const heading = Array.isArray(title) ? translate(...title) : title;
    // popularArticles
    const error = errors && errors[errorKey];
    if (error) {
      return <HttpError {...error} title={errorTitle || translate(`${heading}加载失败`, `Could not load ${heading}`)}/>
    }

    if (isEmpty(items)) {
      return <Skeleton active/>
    }
    return (
        <div className='list-group with-title'>
          <div className="list-group-title">{heading}</div>
          { itemFunc &&
          items.map((item, idx) => itemFunc(item, idx))
          }
        </div>
    )
  }
}

function popularArticlesError(state) {
  return { errors: state.http.errors }
}

export default connect(popularArticlesError, {
  updateHttpErrorMessage
})(ListGroup)
