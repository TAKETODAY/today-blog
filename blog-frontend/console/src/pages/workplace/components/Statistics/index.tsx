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

import React, { useEffect, useState } from 'react';
import FieldStatistics from "../FieldStatistics";
import PageViewStatistics from "../PageViewStatistics";
import { getCacheable } from "@/utils";

type StatisticsProps = {}

const statisticsMap = [
  {
    api: '/api/statistics/os',
    name: { x: '操作系统种类', y: '数量', series: '数量' },
    data: {}
  },
  {
    api: '/api/statistics/user',
    name: { x: '用户', y: '数量', series: '数量' },
    data: {}
  },
  {
    api: '/api/statistics/browser',
    name: { x: '浏览器种类', y: '数量', series: '数量' },
    data: {}
  },
]

const Statistics: React.FC<StatisticsProps> = (props) => {
  const [statistics, setStatistics] = useState([])
  useEffect(() => {
    statisticsMap.forEach((stat) => {
      getCacheable(stat.api).then(res => {
        // @ts-ignore
        stat.data = res.data
      })
    })
    setStatistics(statisticsMap)
  }, [])

  return <>
    {
      statistics.map((stat) => {
        return <FieldStatistics key={stat.api} name={stat.name} data={stat.data}/>
      })
    }

    <PageViewStatistics/>
  </>
}

export default Statistics;
