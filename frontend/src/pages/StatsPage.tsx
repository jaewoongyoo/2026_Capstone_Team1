import PageWrapper from "../components/PageWrapper";

const metrics = [
  { label: "Average OEE", value: "86.4%", delta: "+2.8%" },
  { label: "Availability", value: "91.2%", delta: "+1.4%" },
  { label: "Throughput", value: "14,280", delta: "+6.1%" },
  { label: "Critical Alerts", value: "3", delta: "-4" },
];

export default function StatsPage() {
  return (
    <PageWrapper
      eyebrow="Analytics"
      title="운영 통계"
      description="설비 성능, 가동률, 알림 추이를 한 곳에서 확인합니다."
      actions={
        <button className="rounded-lg border border-slate-700 bg-slate-900 px-4 py-2 text-xs font-bold text-slate-300 transition-colors hover:border-cyan-400 hover:text-white">
          보고서 내보내기
        </button>
      }
    >
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {metrics.map((metric) => (
          <article
            key={metric.label}
            className="rounded-lg border border-slate-800 bg-[#111827] p-5 shadow-xl shadow-black/10"
          >
            <p className="text-[11px] font-bold uppercase tracking-wider text-slate-500">
              {metric.label}
            </p>
            <div className="mt-4 flex items-end justify-between gap-3">
              <strong className="text-3xl font-black text-white">{metric.value}</strong>
              <span className="rounded-md bg-emerald-500/10 px-2 py-1 text-xs font-bold text-emerald-400">
                {metric.delta}
              </span>
            </div>
          </article>
        ))}
      </div>

      <div className="grid gap-4 xl:grid-cols-[1.3fr_0.7fr]">
        <section className="rounded-lg border border-slate-800 bg-[#111827] p-6">
          <h3 className="text-sm font-black uppercase tracking-wide text-white">
            Production Trend
          </h3>
          <div className="mt-6 flex h-72 items-end gap-3 border-b border-slate-800 pb-4">
            {[52, 68, 61, 74, 88, 81, 93, 76, 84, 91, 86, 96].map((height, index) => (
              <div key={index} className="flex flex-1 flex-col items-center gap-2">
                <div
                  className="w-full rounded-t-md bg-cyan-500/80"
                  style={{ height: `${height}%` }}
                />
                <span className="text-[10px] text-slate-600">{index + 1}</span>
              </div>
            ))}
          </div>
        </section>

        <section className="rounded-lg border border-slate-800 bg-[#111827] p-6">
          <h3 className="text-sm font-black uppercase tracking-wide text-white">Focus Areas</h3>
          <div className="mt-5 space-y-4">
            {["ETCH-02 압력 편차 확인", "CVD-01 온도 안정화", "Robot Arm-04 정비 예약"].map(
              (item) => (
                <div key={item} className="rounded-lg bg-slate-900/80 p-4 text-sm text-slate-300">
                  {item}
                </div>
              ),
            )}
          </div>
        </section>
      </div>
    </PageWrapper>
  );
}
