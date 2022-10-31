import React, { useEffect, useState } from 'react';
import { getCacheable } from "@/utils";
import ReactEcharts from "echarts-for-react";

export default () => {
  return <>
    <Statistics name={{ x: '日期', y: '数量', series: '数量' }}/>
  </>;
};

const defaultData = {
  ip: [],
  uv: [],
  pv: []
}

type StatisticsProps = {}

// function applyData(map: any, data: any, dataKey: string) {
//   Object.entries(data).forEach((entry) => {
//     const [key, value] = entry
//     if (map[key]) {
//       const old = map[key]
//       old[dataKey] = value
//     }
//     else {
//       const newValue = { uv: 0, ip: 0, pv: 0 }
//       newValue[dataKey] = value
//       map[key] = newValue
//     }
//   })
// }

const Statistics: React.FC<StatisticsProps> = (props) => {

  const [data, setData] = useState(defaultData);
  const [xAxisData, setXAxisData] = useState([]);

  useEffect(() => {
    const ip: any[] = []
    const uv: any[] = []
    const pv: any[] = []

    getCacheable("/api/statistics/pv").then(res => {
      // @ts-ignore
      const { data } = res

      for (const value of Object.values(data)) {
        ip.push(value.ip)
        uv.push(value.uv)
        pv.push(value.pv)
      }

      // @ts-ignore
      setData({ ip, uv, pv })

      // @ts-ignore
      setXAxisData(Array.from(Object.keys(data)))
    })

  }, [])

  // ip, pv, uv

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#283b56'
        }
      }
    },
    legend: {
      data: ['PV', 'UV', 'IP']
    },
    toolbox: {
      show: true,
      feature: {
        dataView: { readOnly: false },
        restore: {},
        saveAsImage: {}
      }
    },
    dataZoom: [{}, {
      type: 'inside'
    }],
    xAxis: [ // x轴刻度
      {
        type: 'category',
        boundaryGap: true,
        data: xAxisData,
        name: props.name.x
      },
    ],
    yAxis: [
      {
        type: 'value',
        scale: true,
        name: props.name.y,
        // max: 30,
        // min: 0,
        // boundaryGap: [0.2, 0.2]
      },
    ],
    series: [
      {
        name: 'PV',
        type: 'line',
        data: data.pv,
      },
      {
        name: 'UV',
        type: 'line',
        data: data.uv
      },
      {
        name: 'IP',
        type: 'line',
        data: data.ip
      }
    ],
  };

  return <>
    <ReactEcharts option={option} style={{ height: 400 }}/>
  </>
}

