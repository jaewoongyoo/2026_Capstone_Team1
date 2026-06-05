export interface TrendPoint {
  t: string;
  [key: string]: number | string;
}

export interface DonutItem {
  name: string;
  value: number;
  color: string;
}

export interface AlertItem {
  id: number;
  time: string;
  sev: 'critical' | 'warning';
  eq: string;
  msg: string;
}