import React, { useEffect, useRef, useState } from 'react';
import ReactEcharts from "echarts-for-react";


const defaultOption = {
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
    data: ['最新成交价', '预购队列']
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
  xAxis: [
    {
      type: 'category',
      boundaryGap: true,
      data: (function () {
        let now = new Date();
        const res = [];
        let len = 10;
        while (len--) {
          res.unshift(now.toLocaleTimeString().replace(/^\D*/, ''));
          now = new Date(now - 2000);
        }
        return res;
      })()
    },
    {
      type: 'category',
      boundaryGap: true,
      data: (function () {
        var res = [];
        var len = 10;
        while (len--) {
          res.push(10 - len - 1);
        }
        return res;
      })()
    }
  ],
  yAxis: [
    {
      type: 'value',
      scale: true,
      name: '价格',
      max: 30,
      min: 0,
      boundaryGap: [0.2, 0.2]
    },
    {
      type: 'value',
      scale: true,
      name: '预购量',
      max: 1200,
      min: 0,
      boundaryGap: [0.2, 0.2]
    }
  ],
  series: [
    {
      name: '预购队列',
      type: 'bar',
      xAxisIndex: 1,
      yAxisIndex: 1,
      data: []
    },
    {
      name: '最新成交价',
      type: 'line',
      data: []
    }
  ]
};

var app = {};
app.count = 11;

const FieldStatistics: React.FC<{ data: { [key: string]: any } }> = (props) => {
  const [options, setOptions] = useState(defaultOption)

  // const data: { key: string; value: any; }[] = []
  // Object.entries(props.data).forEach((entry) => {
  //   const [key, value] = entry
  //   data.push({ key, value: value })
  // })
  // console.log(data)
  const instance = useRef(null);

  function fetchNewData() {
    const axisData = (new Date()).toLocaleTimeString().replace(/^\D*/, '');
    const newOption = { ...options }
    // newOption.title.text = 'Hello Echarts-for-react.' + new Date().getSeconds();
    const data0 = newOption.series[0].data;
    const data1 = newOption.series[1].data;
    data0.shift();
    data0.push(Math.round(Math.random() * 1000));
    data1.shift();
    data1.push((Math.random() * 10 + 5).toFixed(1) - 0);

    // 移动
    newOption.xAxis[0].data.shift();
    newOption.xAxis[0].data.push(axisData);
    newOption.xAxis[1].data.shift();
    newOption.xAxis[1].data.push(app.count++);

    // instance.current?.ech`arts?.setOption(newOption)
    // instance.current?.setOption(newOption)
    instance?.current.getEchartsInstance().setOption(newOption)
    // setOptions(newOption);
  }

  useEffect(() => {
    const timer = setInterval(() => {
      fetchNewData()
      console.log(instance)
    }, 1500);

    return () => clearInterval(timer)
  }, []);

  return <>
    <ReactEcharts
        ref={instance}
        option={options}
        style={{ height: 400 }}
    />
  </>;
};

export default FieldStatistics;
