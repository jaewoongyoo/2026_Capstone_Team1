import ReactECharts from "echarts-for-react";

type SelectedSensor = {
  key: string;
  label: string;
  value: number;
};

type TrendChartContentProps = {
  sensors: SelectedSensor[];
  color?: string;
};

type TrendPoint = {
  t: string;
  [key: string]: string | number;
};

export function TrendChartContent({ sensors, color = "#818cf8" }: TrendChartContentProps) {
  const chartColors = [color, "#34d399", "#fbbf24", "#f87171", "#a78bfa", "#f472b6"];
  const hasLiveSensors = sensors.length > 0;
  const points: TrendPoint[] = Array.from({ length: 12 }, (_, index) => ({
    t: `${index + 1}`,
    ...Object.fromEntries(sensors.map((sensor) => [sensor.key, sensor.value])),
  }));
  const seriesKeys = sensors.map((sensor) => sensor.key);
  const labelByKey = new Map(sensors.map((sensor) => [sensor.key, sensor.label]));

  if (!hasLiveSensors) {
    return (
      <div className="mt-2 flex min-h-[150px] w-full flex-grow items-center justify-center rounded-lg border border-slate-800 bg-slate-900/40 text-xs font-semibold text-slate-500">
        No live sensor data
      </div>
    );
  }

  const trendChartOption = {
    tooltip: {
      trigger: "axis",
      backgroundColor: "#1e293b",
      borderColor: "#334155",
      textStyle: { color: "#f8fafc", fontSize: 10 },
    },
    legend: {
      show: true,
      textStyle: { color: "#64748b", fontSize: 9 },
      top: 0,
    },
    grid: { top: 30, right: 10, bottom: 20, left: 35, containLabel: true },
    xAxis: {
      type: "category",
      data: points.map((point) => point.t),
      axisLine: { lineStyle: { color: "#334155" } },
      axisLabel: { color: "#64748b", fontSize: 9 },
    },
    yAxis: {
      type: "value",
      splitLine: { lineStyle: { color: "#1e293b", type: "dashed" } },
      axisLabel: { color: "#64748b", fontSize: 9 },
    },
    series: seriesKeys.map((key, index) => ({
      name: labelByKey.get(key) ?? key,
      type: "line",
      smooth: true,
      symbol: "none",
      data: points.map((point) => Number(point[key] ?? 0)),
      itemStyle: { color: chartColors[index % chartColors.length] },
      lineStyle: { width: 2 },
      areaStyle: index === 0
        ? {
          color: {
            type: "linear",
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: `${chartColors[index % chartColors.length]}44` },
              { offset: 1, color: `${chartColors[index % chartColors.length]}00` },
            ],
          },
        }
        : undefined,
    })),
  };

  return (
    <div className="mt-2 min-h-[150px] w-full flex-grow">
      <ReactECharts
        option={trendChartOption}
        style={{ height: "100%", width: "100%" }}
        opts={{ renderer: "svg" }}
      />
    </div>
  );
}

export default TrendChartContent;
