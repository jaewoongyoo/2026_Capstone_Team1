export type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
  statusCode?: number;
};

export type LoginResponse = {
  accessToken: string;
  refreshToken?: string;
  tokenType?: string;
  expiresIn?: number;
};

export type Dashboard = {
  dashboardId: number;
  dashboardName: string;
  description?: string;
  userId?: number;
  createdAt?: string;
  updatedAt?: string;
};

export type Equipment = {
  equipmentId: number;
  equipmentName: string;
  field?: string;
  dashboardId: number;
};

export type EquipmentCurrent = {
  equipmentId: number;
  equipmentName: string;
  field?: string;
  dashboardId?: number;
  current?: {
    equipmentEntityId?: number;
    equipmentId?: string;
    timestamp?: string;
    status?: string;
    sensors?: SensorCurrent[];
  };
};

export type SensorCurrent = {
  sensorId: string;
  dataType: string;
  value: unknown;
  unit?: string;
};

export type Sensor = {
  sensorId: number;
  sensorName: string;
  equipmentId: number;
};

export type WidgetRequest = {
  dashboardId?: number;
  equipmentId?: string;
  equipmentEntityId?: number;
  widgetType: string;
  title: string;
  sensorId?: string;
  sensorEntityId?: number;
  chartType?: string;
  dataType?: string;
  unit?: string;
  posX: number;
  posY: number;
  width: number;
  height: number;
  configJson?: string;
};

export type Widget = WidgetRequest & {
  id: number;
  userId?: number;
  dashboardName?: string;
  equipmentName?: string;
  sensorName?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type WidgetLayoutUpdate = {
  layouts: Array<{
    widgetId: number;
    posX: number;
    posY: number;
    width: number;
    height: number;
  }>;
};
