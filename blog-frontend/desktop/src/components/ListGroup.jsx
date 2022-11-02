import { Result, Skeleton } from 'antd';
import React from 'react';
import { arrayNotEquals, isEmpty } from '../utils';
import { connect } from "react-redux";
import { updateHttpErrorMessage } from "../redux/actions";
import HttpError from "./http/HttpError";


class ListGroup extends React.Component {

  shouldComponentUpdate(nextProps, nextState) {
    return arrayNotEquals(this.props.items, nextProps.items)
        || this.props.title !== nextProps.title
        || this.props.itemFunc !== nextProps.itemFunc
  }

  render() {
    const { items, itemFunc, title, errors, errorKey, errorTitle } = this.props;
    // popularArticles
    const error = errors && errors[errorKey];
    if (error) {
      return <HttpError { ...error } title={ errorTitle ? errorTitle : title + "加载失败" }/>
    }

    if (isEmpty(items)) {
      return <Skeleton active/>
    }
    return (
        <div className='list-group with-title'>
          <div className="list-group-title">{ title }</div>
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

