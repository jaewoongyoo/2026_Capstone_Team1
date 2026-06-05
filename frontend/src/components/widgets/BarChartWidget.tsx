import ReactECharts from "echarts-for-react";

type SelectedSensor = {
  label: string;
  value: number;
  unit?: string;
};

export function BarChartWidget({
  direction = "vertical",
  sensors,
  color = "#818cf8",
}: {
  direction: "vertical" | "horizontal";
  sensors: SelectedSensor[];
  color?: string;
}) {
  const chartColors = [color, "#34d399", "#fbbf24", "#f87171", "#a78bfa", "#f472b6"];
  const data = sensors;

  if (data.length === 0) {
    return (
      <div className="flex h-full min-h-[120px] w-full items-center justify-center rounded-lg border border-slate-800 bg-slate-900/40 px-4 text-center text-xs font-semibold text-slate-500">
        No live sensor data
      </div>
    );
  }

  const categoryData = data.map((sensor) => sensor.label.split(" - ").pop() ?? sensor.label);
  const values = data.map((sensor) => sensor.value);

  const option = {
    tooltip: {
      trigger: "axis",
      axisPointer: { type: "shadow" },
      backgroundColor: "#1e293b",
      borderColor: "#334155",
      textStyle: { color: "#f8fafc", fontSize: 11 },
    },
    grid: {
      top: 24,
      right: direction === "vertical" ? 18 : 36,
      bottom: direction === "vertical" ? 46 : 24,
      left: direction === "vertical" ? 46 : 92,
      containLabel: true,
    },
    xAxis: direction === "vertical"
      ? {
        type: "category",
        data: categoryData,
        axisTick: { alignWithLabel: true },
        axisLabel: {
          color: "#64748b",
          fontSize: 10,
          interval: 0,
          overflow: "truncate",
          width: 72,
        },
      }
      : {
        type: "value",
        axisLabel: { color: "#64748b", fontSize: 10 },
        splitLine: { lineStyle: { color: "#1e293b", type: "dashed" } },
      },
    yAxis: direction === "vertical"
      ? {
        type: "value",
        axisLabel: { color: "#64748b", fontSize: 10 },
        splitLine: { lineStyle: { color: "#1e293b", type: "dashed" } },
      }
      : {
        type: "category",
        data: categoryData,
        axisTick: { alignWithLabel: true },
        axisLabel: {
          color: "#64748b",
          fontSize: 10,
          overflow: "truncate",
          width: 84,
        },
      },
    series: [
      {
        name: "Current",
        type: "bar",
        barWidth: data.length > 2 ? "45%" : "30%",
        itemStyle: {
          color: (params: { dataIndex: number }) => chartColors[params.dataIndex % chartColors.length],
          borderRadius: direction === "vertical" ? [3, 3, 0, 0] : [0, 3, 3, 0],
        },
        label: {
          show: true,
          position: direction === "vertical" ? "top" : "right",
          color: "#cbd5e1",
          fontSize: 10,
          formatter: (params: { value: number; dataIndex: number }) => {
            const sensor = data[params.dataIndex];
            const unit = "unit" in sensor && sensor.unit ? sensor.unit : "";
            return `${params.value}${unit}`;
          },
        },
        data: values,
      },
    ],
  };

  return (
    <div className="h-full w-full pt-2">
      <ReactECharts option={option} style={{ height: "100%", width: "100%" }} />
    </div>
  );
}

export default BarChartWidget;
