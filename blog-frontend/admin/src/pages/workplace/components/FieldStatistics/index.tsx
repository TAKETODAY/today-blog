import React from 'react';
import ReactEcharts from "echarts-for-react";

type FieldStatisticsProps = {
  data: {
    [key: string]: any
  }
  name: {
    x: string;
    y: string;
    series: string
  },
  option?: {
    [key: string]: any
  }
}

const FieldStatistics: React.FC<FieldStatisticsProps> = (props) => {

  const x: any = []
  const y: any = []

  Object.entries(props.data).forEach((entry) => {
    const [key, value] = entry
    x.push(key)
    y.push(value)
  })

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
    toolbox: {
      show: true,
      feature: {
        dataView: { readOnly: false },
        restore: {},
        saveAsImage: {}
      }
    },
    dataZoom: {
      show: false,
      start: 0,
      end: 100
    },
    xAxis: [ // x轴刻度
      {
        type: 'category',
        boundaryGap: true,
        data: x,
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
        boundaryGap: [0.2, 0.2]
      },
    ],
    series: [
      {
        name: props.name.series,
        type: 'bar',
        data: y
      }
    ],
    ...props.option
  };

  return <>
    <ReactEcharts option={option} style={{ height: 400 }}/>
  </>
}

export default FieldStatistics;
