type LogSensor = {
  key: string;
  label: string;
  value: number | string;
  unit: string;
  status: "NORMAL" | "CAUTION" | "CRITICAL";
};

export function LogContent({ sensors }: { sensors: LogSensor[] }) {
  if (sensors.length === 0) {
    return (
      <div className="flex h-full min-h-[120px] items-center justify-center rounded-lg border border-slate-800 bg-slate-900/40 px-4 text-center text-xs font-semibold text-slate-500">
        No live sensor data
      </div>
    );
  }

  const time = new Date().toLocaleTimeString();

  return (
    <div className="flex h-full flex-col gap-2 overflow-y-auto pr-2 text-[11px] custom-scrollbar">
      {sensors.map((sensor) => {
        const level = sensor.status === "CRITICAL" ? "ERROR" : sensor.status === "CAUTION" ? "WARN" : "INFO";
        const levelColor = level === "ERROR" ? "text-rose-400" : level === "WARN" ? "text-amber-400" : "text-emerald-400";

        return (
          <div key={sensor.key} className="flex gap-3 rounded-r-lg border-l-2 border-indigo-500 bg-slate-900/50 p-2">
            <span className="shrink-0 font-mono text-slate-500">{time}</span>
            <span className={`shrink-0 font-bold ${levelColor}`}>
              [{level}]
            </span>
            <span className="truncate text-slate-300">
              {sensor.label}: {String(sensor.value)}{sensor.unit}
            </span>
          </div>
        );
      })}
    </div>
  );
}

export default LogContent;
