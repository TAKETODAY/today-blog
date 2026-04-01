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
import { Image } from 'antd';
import { isTrue } from "core";
import loading from 'src/assets/images/loading.gif'
import fallback from 'src/assets/images/fallback.png'
import { connect } from "react-redux";

// const imageServer = 'http://localhost:8080'

function getSource(src, options) {
  if (src) {
    if (src.startsWith('//') || src.startsWith('http://') || src.startsWith('https://')) {
      return src
    }
    const imageServer = options['site.cdn']
    return `${imageServer}${src}`
  }
  return src
}

class ImageComponent extends React.Component {

  render() {
    const {
      src,
      onCreate,
      alt = 'pic',
      original = true,
      options,
      ...rest
    } = this.props

    if (isTrue(original)) {
      return <img {...rest}
                  alt={alt}
                  src={getSource(src, options)}
                  ref={onCreate}
      />
    }
    return (<>
      <Image {...rest}
             alt={alt}
             src={getSource(src, options)}
             ref={onCreate}
             placeholder={<img src={loading} alt='加载中'/>}
             fallback={fallback}
      />
    </>)
  }
}


export default connect(
  (state) => {
    return {
      options: state.options,
    }
  },
  {}
)(ImageComponent)
