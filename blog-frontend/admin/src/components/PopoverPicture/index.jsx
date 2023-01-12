import { Popover } from "antd"
import React from "react"

export default props => {
  const { src, title, alt, width, placement, enlargedWidth, multiple, ...rest } = props
  return (
    <Popover
      title={ title }
      placement={ placement || 'rightTop' }
      content={ <img src={ src } alt={ alt || title } width={ enlargedWidth || (width * (multiple || 3)) } /> }
      { ...rest }
    >
      <img src={ src } width={ width } alt={ alt || title } />
    </Popover>
  )
}
