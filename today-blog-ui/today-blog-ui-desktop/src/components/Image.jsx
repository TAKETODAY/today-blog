import React from 'react';
import { Image } from 'antd';
import { isTrue } from "../utils";
import loading from 'src/assets/images/loading.gif'
import fallback from 'src/assets/images/fallback.png'

// const imageServer = 'http://localhost:8080'
const imageServer = ''

function getSource(src) {
  if (src) {
    if (src.startsWith('//') || src.startsWith('http://') || src.startsWith('https://')) {
      return src
    }
    return `${ imageServer }${ src }`
  }
  return src
}

export default class ImageComponent extends React.Component {

  render() {
    const {
      src,
      onCreate,
      alt = 'pic',
      original = true,
      ...rest
    } = this.props

    if (isTrue(original)) {
      return <img { ...rest }
                  alt={ alt }
                  src={ getSource(src) }
                  ref={ onCreate }
      />
    }
    return (<>
      <Image { ...rest }
             alt={ alt }
             src={ getSource(src) }
             ref={ onCreate }
             placeholder={ <img src={ loading } alt='加载中'/> }
             fallback={ fallback }
      />
    </>)
  }
}
