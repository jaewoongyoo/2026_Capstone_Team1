export type EquipmentStatus = "RUNNING" | "IDLE" | "DOWN" | "MAINTENANCE";

export type SensorValueType = "FLOAT" | "DOUBLE" | "BOOLEAN" | "INTEGER" | "INT" | "STRING";

export interface SensorData {
  sensorId?: string;
  label: string;
  value: number | string;
  unit: string;
  dataType?: SensorValueType;
  status: "NORMAL" | "CAUTION" | "CRITICAL";
}

export interface BaseEquipment {
  id: string;
  name: string;
  type: string;
  status: EquipmentStatus;
  lastUpdate: string;
}

export interface UniversalEquipment extends BaseEquipment {
  metrics: {
    oee: number;
    availability: number;
    performance: number;
    quality: number;
  };
  sensors: SensorData[];
}

export interface SensorMeta {
  id: string;
  label: string;
  unit: string;
  dataType?: SensorValueType;
}

export interface EquipmentMaster {
  id: string;
  name: string;
  type: string;
  sensors: SensorMeta[];
}
