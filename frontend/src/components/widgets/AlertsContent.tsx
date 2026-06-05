import type { AlertItem } from "../../types/dashboard";

export function AlertsContent({ alerts }: { alerts: AlertItem[] }) {
  return (
    <div className="flex-grow space-y-4 overflow-y-auto pr-2 custom-scrollbar">
      {alerts?.map((alert) => (
        <div key={alert.id} className="p-4 bg-slate-900/50 border border-slate-800 rounded-2xl hover:border-rose-500/30 transition-all group/item">
          <div className="flex justify-between items-start mb-2">
            <span className="text-[10px] text-slate-600 font-mono tracking-tighter font-bold">{alert.time}</span>
            <span className={`text-[9px] font-black px-2 py-0.5 rounded uppercase ${alert.sev === 'critical' ? 'bg-rose-500/10 text-rose-500' : 'bg-amber-500/10 text-amber-500'}`}>{alert.sev}</span>
          </div>
          <div className="text-xs font-bold text-slate-300 mb-1">{alert.eq}</div>
          <p className="text-[11px] text-slate-500 leading-relaxed line-clamp-2 group-hover/item:text-slate-400">{alert.msg}</p>
        </div>
      ))}
    </div>
  );
}

export default AlertsContent;
