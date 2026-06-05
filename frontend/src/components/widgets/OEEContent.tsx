import type { UniversalEquipment } from "../../types/equipment";

export function OEEContent({ data }: { data: UniversalEquipment }) {
  return (
    <div className="h-full flex flex-col justify-between">
      <div>
        <div className="flex items-baseline gap-2 mt-1">
          <span className="text-5xl font-black text-white font-mono tracking-tighter leading-none">
            {data.metrics.oee}
          </span>
          <span className="text-xl text-slate-500 font-bold">%</span>
        </div>
        <div className="flex items-center gap-2 mt-3">
          <span className="text-emerald-400 text-sm font-bold">↑ +2.1%</span>
          <span className="text-slate-600 text-[10px] uppercase tracking-wider">vs. prev. shift</span>
        </div>
      </div>
      <div className="grid grid-cols-3 gap-2 mt-4">
        {[
          { label: "Availability", value: data.metrics.availability, color: "text-emerald-400" },
          { label: "Performance", value: data.metrics.performance, color: "text-blue-400" },
          { label: "Quality", value: data.metrics.quality, color: "text-indigo-400" }
        ].map(m => (
          <div key={m.label} className="bg-slate-900/50 p-2 rounded-lg border border-slate-800/50">
            <div className="text-[8px] text-slate-500 uppercase mb-1">{m.label}</div>
            <div className={`text-sm font-bold font-mono ${m.color}`}>{m.value}%</div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default OEEContent;