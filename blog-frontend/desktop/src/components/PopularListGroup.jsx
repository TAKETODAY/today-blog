import React from 'react';
import ListGroup from '../components/ListGroup';
import { arrayNotEquals } from '../utils';
import { Link } from "react-router-dom";


const popularItemFunc = (article, idx) => {
  return (
      <Link
          key={ idx }
          style={ { whiteSpace: 'normal' } }
          className='list-group-item'
          to={ `/articles/${ article.id }` }
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
