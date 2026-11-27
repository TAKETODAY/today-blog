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

import { Popover } from "antd"
import React from "react"
import Image from "@/components/Image";

export default props => {
  const { src, title, alt, width, placement, enlargedWidth, multiple, ...rest } = props
  return (
    <Popover
      title={title}
      placement={placement || 'rightTop'}
      content={<Image src={src} alt={alt || title} width={enlargedWidth || (width * (multiple || 3))}/>}
      {...rest}
    >
      <Image src={src} width={width} alt={alt || title} {...rest}/>
    </Popover>
  )
}
