import { OEEContent } from "./widgets/OEEContent";
import { SensorGridContent } from "./widgets/SensorGridContent";
import { TrendChartContent } from "./widgets/TrendChartContent";
import { AlertsContent } from "./widgets/AlertsContent";
import { GaugeChartWidget } from "./widgets/GaugeChartWidget";
import { DonutChartWidget } from "./widgets/DonutChartWidget";
import { StatusWidget } from "./widgets/StatusWidget";
import { LogContent } from "./widgets/LogContent";
import { BarChartWidget } from "./widgets/BarChartWidget";

import type { SensorData, UniversalEquipment } from "../types/equipment";
import type { AlertItem, DashboardItem } from "../types/dashboard";

type SelectedSensor = {
  key: string;
  equipmentId?: string;
  sensorId: string;
  label: string;
  value: number | string;
  unit: string;
  dataType?: SensorData["dataType"];
  status: SensorData["status"];
};

type Props = {
  widget: DashboardItem;
  equipment: UniversalEquipment;
  equipmentById?: Record<string, UniversalEquipment>;
  alerts: AlertItem[];
};

const widgetColorMap: Record<string, string> = {
  "bg-indigo-500": "#818cf8",
  "bg-cyan-500": "#06b6d4",
  "bg-emerald-500": "#10b981",
  "bg-amber-500": "#f59e0b",
  "bg-rose-500": "#f43f5e",
  "bg-violet-500": "#8b5cf6",
  "bg-sky-500": "#0ea5e9",
  "bg-pink-500": "#ec4899",
};

function getWidgetChartColor(widget: DashboardItem) {
  return widgetColorMap[widget.color] ?? "#818cf8";
}

function EmptyWidgetState({ message = "No live sensor data" }: { message?: string }) {
  return (
    <div className="flex h-full min-h-[120px] items-center justify-center rounded-lg border border-slate-800 bg-slate-900/40 px-4 text-center text-xs font-semibold text-slate-500">
      {message}
    </div>
  );
}

function parseDataKey(dataKey: string) {
  const [equipmentId, sensorId] = dataKey.includes("::") ? dataKey.split("::") : ["", dataKey];
  return { equipmentId, sensorId };
}

function resolveEquipment(dataKey: string, fallback: UniversalEquipment, equipmentById?: Record<string, UniversalEquipment>) {
  const { equipmentId, sensorId } = parseDataKey(dataKey);
  const equipment = equipmentId ? equipmentById?.[equipmentId] : fallback;

  return {
    equipment,
    equipmentId,
    sensorId,
  };
}

function resolveSensor(
  dataKey: string,
  fallbackEquipment: UniversalEquipment,
  equipmentById?: Record<string, UniversalEquipment>,
  widget?: DashboardItem,
): SelectedSensor | undefined {
  const { equipment, equipmentId, sensorId } = resolveEquipment(dataKey, fallbackEquipment, equipmentById);

  if (!equipment) {
    return undefined;
  }

  if (equipmentId && equipment.id !== equipmentId && !equipmentById?.[equipmentId]) {
    return undefined;
  }

  const sensor = equipment.sensors.find(
    (item) =>
      item.sensorId === sensorId ||
      item.label === sensorId ||
      item.sensorId === widget?.sensorId ||
      item.label === widget?.sensorId ||
      item.sensorId === widget?.sensorName ||
      item.label === widget?.sensorName ||
      String(item.sensorId ?? "") === String(widget?.sensorEntityId ?? "") ||
      String(item.sensorId ?? "").endsWith(sensorId) ||
      sensorId.endsWith(String(item.sensorId ?? "")) ||
      String(item.label ?? "").endsWith(sensorId) ||
      sensorId.endsWith(String(item.label ?? "")),
  );

  if (!sensor) return undefined;

  return {
    key: dataKey,
    equipmentId: equipmentId || equipment.id,
    sensorId: sensor.sensorId ?? sensor.label,
    label: `${equipment.name} - ${sensor.label}`,
    value: sensor.value,
    unit: sensor.unit,
    dataType: sensor.dataType,
    status: sensor.status,
  };
}

function isNumericSensor(sensor: SelectedSensor) {
  return typeof sensor.value === "number" && Number.isFinite(sensor.value);
}

function resolveSensorsForWidget(
  widget: DashboardItem,
  equipment: UniversalEquipment | undefined,
  equipmentById?: Record<string, UniversalEquipment>,
) {
  if (!equipment) return [];

  const dataKey = widget.dataKey;
  const keys = Array.isArray(dataKey) ? dataKey : [dataKey];
  return keys
    .map((key) => resolveSensor(key, equipment, equipmentById, widget))
    .filter((sensor): sensor is SelectedSensor => Boolean(sensor));
}

function getPrimaryEquipment(widget: DashboardItem, fallback: UniversalEquipment, equipmentById?: Record<string, UniversalEquipment>) {
  const firstKey = Array.isArray(widget.dataKey) ? widget.dataKey[0] : widget.dataKey;
  const { equipmentId } = parseDataKey(firstKey);
  const serverEquipmentId = widget.equipmentEntityId ? String(widget.equipmentEntityId) : "";
  const resolvedEquipmentId = equipmentId || serverEquipmentId;
  return resolvedEquipmentId ? equipmentById?.[resolvedEquipmentId] : fallback;
}

export function WidgetRenderer({ widget, equipment, equipmentById, alerts }: Props) {
  const chartColor = getWidgetChartColor(widget);
  const widgetEquipment = getPrimaryEquipment(widget, equipment, equipmentById);
  const selectedSensors = resolveSensorsForWidget(widget, widgetEquipment, equipmentById);
  const numericSensors = selectedSensors.filter(isNumericSensor) as Array<SelectedSensor & { value: number }>;
  const targetSensor = selectedSensors[0];
  const targetNumericSensor = numericSensors[0];
  const val = targetNumericSensor?.value ?? 0;
  const unit = targetSensor?.unit ?? "";
  const label = targetSensor?.label ?? widget.title;

  switch (widget.type) {
    case "OEE":
      if (!widgetEquipment) return <EmptyWidgetState />;
      return <OEEContent data={widgetEquipment} />;

    case "SENSORS":
      if (!widgetEquipment) return <EmptyWidgetState />;
      if (widgetEquipment.sensors.length === 0) return <EmptyWidgetState />;
      return <SensorGridContent sensors={widgetEquipment.sensors} />;

    case "TREND":
      return <TrendChartContent sensors={numericSensors} color={chartColor} />;

    case "ALERTS":
      return <AlertsContent alerts={alerts} />;

    case "GAUGE":
      if (!targetNumericSensor) return <EmptyWidgetState />;
      return (
        <GaugeChartWidget
          value={val}
          unit={unit}
          label={label}
          min={0}
          max={1200}
          color={chartColor}
        />
      );

    case "DONUT":
      if (!widgetEquipment) return <EmptyWidgetState />;
      if (widgetEquipment.sensors.length === 0) return <EmptyWidgetState />;
      return (
        <DonutChartWidget
          title={widget.title}
          data={[
            { name: "Normal", value: widgetEquipment.sensors.filter((sensor) => sensor.status === "NORMAL").length, color: chartColor },
            { name: "Caution", value: widgetEquipment.sensors.filter((sensor) => sensor.status === "CAUTION").length, color: "#f59e0b" },
            { name: "Critical", value: widgetEquipment.sensors.filter((sensor) => sensor.status === "CRITICAL").length, color: "#ef4444" },
          ]}
        />
      );

    case "STATUS":
      if (!targetSensor) return <EmptyWidgetState />;
      return (
        <StatusWidget
          status={targetSensor?.status === "CRITICAL" ? "ALARM" : targetSensor?.status === "CAUTION" ? "CAUTION" : "RUNNING"}
          label={label}
          subText={targetSensor ? `${String(targetSensor.value)}${targetSensor.unit}` : "No data"}
        />
      );

    case "LOG":
      return <LogContent sensors={selectedSensors} />;

    case "BAR_V":
      if (numericSensors.length === 0) return <EmptyWidgetState />;
      return <BarChartWidget direction="vertical" sensors={numericSensors} color={chartColor} />;

    case "BAR_H":
      if (numericSensors.length === 0) return <EmptyWidgetState />;
      return <BarChartWidget direction="horizontal" sensors={numericSensors} color={chartColor} />;

    default:
      return null;
  }
}

export default WidgetRenderer;
