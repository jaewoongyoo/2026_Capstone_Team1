import ReactECharts from 'echarts-for-react';

export function GaugeChartWidget({ 
  value, 
  min = 0, 
  max = 100, 
  unit = "", 
  label = "",
  color = "#818cf8",
}: { 
  value: number; 
  min?: number; 
  max?: number; 
  unit?: string; 
  label?: string; 
  color?: string;
}) {
  const option = {
    series: [{
      type: 'gauge',
      center: ['50%', '60%'],
      startAngle: 200,
      endAngle: -20,
      min,
      max,
      splitNumber: 5,
      itemStyle: { color },
      progress: { show: true, width: 10 },
      pointer: { show: true, length: '60%', width: 4 },
      axisLine: { lineStyle: { width: 10, color: [[1, '#1e293b']] } },
      axisTick: { show: false },
      splitLine: { distance: -15, length: 8, lineStyle: { color: '#334155', width: 2 } },
      axisLabel: { distance: -30, color: '#64748b', fontSize: 10 },
      anchor: { show: false },
      title: { show: false },
      detail: {
        valueAnimation: true,
        offsetCenter: [0, '20%'],
        fontSize: 22,
        fontWeight: '900',
        formatter: `{value}${unit}`,
        color: '#f8fafc',
        fontFamily: 'monospace'
      },
      data: [{ value }]
    }]
  };

  return (
    <div className="h-full w-full flex flex-col items-center justify-center">
      <div className="flex-grow w-full h-[180px]">
        <ReactECharts option={option} style={{ height: '100%', width: '100%' }} />
      </div>
      <div className="text-[10px] text-slate-500 font-bold uppercase tracking-widest -mt-4 pb-2">
        {label}
      </div>
    </div>
  );
}

export default GaugeChartWidget;
