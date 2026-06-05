import type { UniversalEquipment } from "../../types/equipment";

export function SensorGridContent({ sensors }: { sensors: UniversalEquipment['sensors'] }) {
  return (
    <div className="grid grid-cols-2 gap-3 h-full overflow-y-auto pr-1 custom-scrollbar">
      {sensors.map((s, i) => (
        <div key={i} className="bg-slate-900/40 p-3 rounded-xl border border-slate-800/60 flex flex-col justify-between group hover:border-indigo-500/50 transition-colors">
          <div className="flex justify-between items-start">
            <span className="text-[10px] text-slate-500 uppercase font-bold tracking-tighter">{s.label}</span>
            <div className={`w-1.5 h-1.5 rounded-full ${s.status === 'CRITICAL' ? 'bg-rose-500 animate-pulse' : s.status === 'CAUTION' ? 'bg-amber-500' : 'bg-emerald-500'}`} />
          </div>
          <div className="mt-2 flex items-baseline gap-1">
            <span className={`text-xl font-black font-mono ${s.status === 'CRITICAL' ? 'text-rose-400' : 'text-indigo-300'}`}>{s.value}</span>
            <span className="text-[10px] text-slate-600 font-medium">{s.unit}</span>
          </div>
        </div>
      ))}
    </div>
  );
}

export default SensorGridContent;