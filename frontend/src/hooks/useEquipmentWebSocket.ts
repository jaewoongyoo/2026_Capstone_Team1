import { useEffect, type Dispatch, type SetStateAction } from "react";

import { subscribeToEquipmentGateway } from "../api/socket";
import type { DashboardItem } from "../types/dashboard";
import type { SensorData, UniversalEquipment } from "../types/equipment";

type GatewaySensor = {
  sensorId?: string | number;
  sensorName?: string;
  name?: string;
  dataType?: "FLOAT" | "DOUBLE" | "BOOLEAN" | "INTEGER" | "INT" | "STRING";
  value?: unknown;
  currentValue?: unknown;
  numericValue?: unknown;
  unit?: string;
};

type GatewayPayload = {
  equipmentId?: string | number;
  equipmentEntityId?: string | number;
  equipmentName?: string;
  timestamp?: string;
  status?: string;
  sensors: GatewaySensor[];
};

type PayloadObject = Record<string, unknown>;

function getSensorStatus(payloadStatus?: string): SensorData["status"] {
  return payloadStatus === "ERROR" ? "CRITICAL" : "NORMAL";
}

function mapSensorIdToLabel(sensorId: string, sensorName?: string): string {
  if (sensorName) return sensorName;
  if (sensorId.startsWith("Temp_Sensor_")) return "Temperature";
  if (sensorId.startsWith("Power_Status_")) return "Power";
  if (sensorId.startsWith("Cycle_Count_")) return "Cycle Count";
  return sensorId;
}

function normalizeSensorValue(value: unknown, dataType?: string): number | string {
  if (dataType === "STRING") return String(value ?? "");
  if (typeof value === "number") return value;
  if (typeof value === "boolean") return value ? 1 : 0;
  if (value && typeof value === "object") {
    const payload = value as Record<string, unknown>;
    return normalizeSensorValue(payload.value ?? payload.currentValue ?? payload.numericValue ?? payload.data, dataType);
  }

  const numericText = String(value ?? "").replace(/,/g, "").match(/-?\d+(\.\d+)?/)?.[0] ?? "";
  const numericValue = Number(numericText);
  return Number.isFinite(numericValue) ? numericValue : String(value ?? "");
}

function getSensorMergeKey(sensor: SensorData) {
  return sensor.sensorId ?? sensor.label;
}

function mergeSensorData(previousSensors: SensorData[], nextSensors: SensorData[]) {
  const sensorByKey = new Map(previousSensors.map((sensor) => [getSensorMergeKey(sensor), sensor]));

  nextSensors.forEach((sensor) => {
    sensorByKey.set(getSensorMergeKey(sensor), sensor);
  });

  return Array.from(sensorByKey.values());
}

function isGatewayPayload(value: unknown): value is GatewayPayload {
  if (!value || typeof value !== "object") return false;

  const payload = value as Partial<GatewayPayload>;

  return (
    Array.isArray(payload.sensors)
  );
}

function firstObject(value: unknown): unknown {
  return Array.isArray(value) ? value.find((item) => item && typeof item === "object") : value;
}

function unwrapGatewayPayload(value: unknown): GatewayPayload | null {
  const payload = firstObject(value);

  if (isGatewayPayload(payload)) {
    return payload;
  }

  if (!payload || typeof payload !== "object") {
    return null;
  }

  const objectPayload = payload as PayloadObject;
  const nestedCandidates = [
    objectPayload.data,
    objectPayload.payload,
    objectPayload.current,
    objectPayload.message,
    objectPayload.body,
  ];

  for (const candidate of nestedCandidates) {
    const unwrapped = unwrapGatewayPayload(candidate);
    if (unwrapped) return unwrapped;
  }

  if (Array.isArray(objectPayload.widgets)) {
    for (const widget of objectPayload.widgets) {
      const unwrapped = unwrapGatewayPayload(widget);
      if (unwrapped) return unwrapped;
    }
  }

  return null;
}

function mapGatewayToDashboard(
  prev: UniversalEquipment,
  payload: GatewayPayload,
): UniversalEquipment {
  const mappedSensors: SensorData[] = payload.sensors.map((sensor, index) => {
    const sensorId = String(sensor.sensorId ?? sensor.sensorName ?? sensor.name ?? `sensor-${index}`);
    const rawValue = sensor.value ?? sensor.currentValue ?? sensor.numericValue;

    return {
    sensorId,
    label: mapSensorIdToLabel(sensorId, sensor.sensorName ?? sensor.name),
    value: normalizeSensorValue(rawValue, sensor.dataType),
    unit: sensor.unit ?? "",
    dataType: sensor.dataType,
    status: getSensorStatus(payload.status),
  };
  });

  const equipmentId = String(payload.equipmentId ?? payload.equipmentEntityId ?? prev.id);
  const isSameEquipment =
    prev.id === equipmentId ||
    Boolean(payload.equipmentName && prev.name === payload.equipmentName);

  console.debug("[Equipment WebSocket] Sensor payload received", {
    equipmentId,
    sensorCount: mappedSensors.length,
    sensorIds: mappedSensors.map((sensor) => sensor.sensorId ?? sensor.label),
  });

  return {
    ...prev,
    id: equipmentId,
    name: payload.equipmentName ?? equipmentId,
    status: payload.status === "ERROR" ? "IDLE" : "RUNNING",
    lastUpdate: payload.timestamp ?? new Date().toISOString(),
    sensors: isSameEquipment ? mergeSensorData(prev.sensors, mappedSensors) : mappedSensors,
    metrics: {
      ...prev.metrics,
      oee: prev.metrics.oee,
      availability: prev.metrics.availability,
      performance: prev.metrics.performance,
      quality: prev.metrics.quality,
    },
  };
}

export function useEquipmentWebSocket(
  setEquipment: Dispatch<SetStateAction<UniversalEquipment>>,
  layouts: DashboardItem[] = [],
) {
  useEffect(() => {
    const equipmentEntityIds = Array.from(new Set(
      layouts
        .flatMap((widget) => Array.isArray(widget.dataKey) ? widget.dataKey : [widget.dataKey])
        .map((dataKey) => String(dataKey).split("::")[0])
        .filter((equipmentId) => equipmentId && Number.isInteger(Number(equipmentId))),
    ));

    if (equipmentEntityIds.length === 0) {
      console.info("[Equipment WebSocket] No equipment topics to subscribe");
      return;
    }

    const subscriptions = equipmentEntityIds.map((equipmentEntityId) =>
      subscribeToEquipmentGateway<GatewayPayload>((message) => {
        const payload = unwrapGatewayPayload(message.body);

        if (!payload) {
          console.debug("[Equipment WebSocket] Ignored non-sensor payload", {
            body: message.body,
            rawBody: message.rawBody,
            parseError: message.parseError,
          });
          return;
        }

        setEquipment((prev) => mapGatewayToDashboard(prev, payload));
      }, { mode: "equipment-entity-widgets", equipmentEntityId }),
    );

    return () => {
      subscriptions.forEach(({ unsubscribe }) => unsubscribe());
    };
  }, [layouts, setEquipment]);
}
