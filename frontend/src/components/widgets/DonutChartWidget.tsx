import ReactECharts from 'echarts-for-react';

export function DonutChartWidget({ 
  data, 
  title 
}: { 
  data: { name: string, value: number, color: string }[], 
  title?: string 
}) {
  const option = {
    tooltip: { trigger: 'item', backgroundColor: '#1e293b', borderColor: '#334155', textStyle: { color: '#f8fafc', fontSize: 11 } },
    legend: { show: false },
    series: [
      {
        name: title || 'Status Ratio',
        type: 'pie',
        radius: ['48%', '78%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 6, borderColor: '#161B26', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold', color: '#f8fafc' } },
        labelLine: { show: false },
        data: data.map(d => ({ value: d.value, name: d.name, itemStyle: { color: d.color } }))
      }
    ]
  };

  return (
    <div className="flex h-full w-full flex-col items-center justify-center gap-3 py-2">
      <div className="min-h-0 w-full flex-1">
        <ReactECharts option={option} style={{ height: '100%', width: '100%' }} />
      </div>
      <div className="grid w-full max-w-[220px] shrink-0 grid-cols-1 gap-1 text-[10px] font-medium text-slate-500">
        {data.map((item) => (
          <div key={item.name} className="flex items-center gap-2">
            <span
              className="h-2 w-2 shrink-0 rounded-sm"
              style={{ backgroundColor: item.color }}
              aria-hidden="true"
            />
            <span className="truncate">{item.name}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

export default DonutChartWidget;
