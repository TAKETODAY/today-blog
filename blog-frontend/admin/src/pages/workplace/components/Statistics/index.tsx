import React from 'react';
import FieldStatistics from "../FieldStatistics";
import PageViewStatistics from "../PageViewStatistics";
import { getCacheable } from "@/utils";

type StatisticsProps = {}

const statistics = [
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

statistics.forEach((stat) => {
  getCacheable(stat.api).then(res => {
    // @ts-ignore
    stat.data = res.data
  })
})

const Statistics: React.FC<StatisticsProps> = (props) => {

  return <>
    {
      statistics.map((stat) => {
        return <FieldStatistics key={stat.api} name={stat.name} data={stat.data}/>
      })
    }

    <PageViewStatistics />
  </>
}

export default Statistics;
