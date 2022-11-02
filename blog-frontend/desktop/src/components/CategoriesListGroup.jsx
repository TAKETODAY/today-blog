import React from 'react';
import { Link } from "react-router-dom";
import ListGroup from '../components/ListGroup';
import { arrayNotEquals } from '../utils';

const categoriesItemFunc = (category, idx) => {
  return (
      <Link key={ idx } to={ `/categories/${ category.name }` } className="list-group-item" title={ category.description }>
        &nbsp;&nbsp;&nbsp;{ category.name } ({ category.articleCount })
      </Link>
  )
}

export default class CategoriesListGroup extends React.Component {

  shouldComponentUpdate(nextProps, nextState) {
    return arrayNotEquals(this.props.items, nextProps.items)
  }

  render() {
    return (
        <ListGroup
            { ...this.props }
            title="分类"
            errorKey='categories'
            itemFunc={ categoriesItemFunc }
        />
    )
  }
}
