export function StatusWidget({ 
  status, 
  label, 
  subText 
}: { 
  status: 'RUNNING' | 'STOP' | 'CAUTION' | 'ALARM', 
  label: string, 
  subText?: string 
}) {
  // 상태별 색상 및 애니메이션 설정
  const statusMap = {
    RUNNING: { color: 'bg-emerald-500', text: 'Operational', pulse: true },
    STOP:    { color: 'bg-slate-500',    text: 'Stopped',     pulse: false },
    CAUTION: { color: 'bg-amber-500',   text: 'Check Req.',  pulse: true },
    ALARM:   { color: 'bg-rose-500',    text: 'Critical',    pulse: true },
  };

  const config = statusMap[status] || statusMap.STOP;

  return (
    <div className="h-full flex flex-col items-center justify-center bg-slate-900/30 rounded-2xl border border-slate-800/40 p-4">
      <div className="relative mb-3">
        {/* 상태 표시 라이트 */}
        <div className={`w-12 h-12 rounded-full ${config.color} ${config.pulse ? 'animate-pulse' : ''} shadow-[0_0_20px_rgba(0,0,0,0.5)] flex items-center justify-center`}>
          <div className="w-8 h-8 rounded-full bg-white/20 blur-sm" />
        </div>
        {/* 테두리 글로우 효과 */}
        <div className={`absolute inset-0 w-12 h-12 rounded-full ${config.color} opacity-20 blur-md`} />
      </div>
      
      <div className="text-center">
        <div className="text-white font-black text-lg tracking-tight leading-tight">{config.text}</div>
        <div className="text-[10px] text-slate-500 uppercase font-bold mt-1 tracking-widest">{label}</div>
        {subText && <div className="text-[9px] text-indigo-400/70 mt-1 font-mono">{subText}</div>}
      </div>
    </div>
  );
}

export default StatusWidget;