export type DashboardWidgetType =
  | 'OEE'
  | 'SENSORS'
  | 'TREND'
  | 'ALERTS'
  | 'GAUGE'
  | 'DONUT'
  | 'STATUS'
  | 'LOG'
  | 'BAR_V'
  | 'BAR_H';

export interface DashboardItem {
  i: string;
  serverWidgetId?: number;
  equipmentEntityId?: number;
  equipmentName?: string;
  sensorEntityId?: number;
  sensorId?: string;
  sensorName?: string;
  x: number;
  y: number;
  w: number;
  h: number;
  minW?: number;
  minH?: number;
  maxW?: number;
  maxH?: number;
  static?: boolean;
  isDraggable?: boolean;
  isResizable?: boolean;
  type: DashboardWidgetType;
  dataKey: string | string[];
  title: string;
  color: string;
  backgroundColor?: string;
  pinned?: boolean;
}

export type SensorMetaDataType = 'FLOAT' | 'DOUBLE' | 'BOOLEAN' | 'INTEGER' | 'INT' | 'STRING';

export interface SensorMeta {
  id: string;
  label: string;
  unit: string;
  dataType?: SensorMetaDataType;
}

export interface EquipmentMaster {
  id: string;
  name: string;
  type: string;
  sensors: SensorMeta[];
  sensorsLoaded?: boolean;
}

export interface SelectedData {
  eqId: string;
  eqName: string;
  sensorId: string;
  sensorLabel?: string;
  sensorKey?: string;
  dataType?: SensorMeta['dataType'];
}

export interface AlertItem {
  id: number;
  time: string;
  sev: string;
  eq: string;
  msg: string;
  status?: string;
}
