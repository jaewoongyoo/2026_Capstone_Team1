import { useMemo, useState } from "react";
import { useOutletContext } from "react-router-dom";

import PageWrapper from "../components/PageWrapper";
import type { DashboardState } from "../hooks/useDashboardState";

const settings = [
  {
    title: "Auto arrange widgets",
    description: "Automatically compact empty spaces after moving or resizing widgets.",
    enabled: true,
  },
  {
    title: "Realtime alerts",
    description: "Show alerts immediately when equipment status crosses a threshold.",
    enabled: true,
  },
  {
    title: "Low power mode",
    description: "Reduce background chart refresh work.",
    enabled: false,
  },
];

export default function SettingsPage() {
  const {
    dashboardId,
    dashboardName,
    isDashboardPublic,
    dashboardShareToken,
    enableShareLink,
    disableShareLink,
  } = useOutletContext<DashboardState>();
  const [isUpdatingShare, setIsUpdatingShare] = useState(false);
  const [copyStatus, setCopyStatus] = useState("");

  const shareUrl = useMemo(() => {
    if (!dashboardShareToken) return "";

    const url = new URL("/public/dashboards", window.location.origin);
    url.searchParams.set("token", dashboardShareToken);
    return url.toString();
  }, [dashboardShareToken]);

  const handleToggleShare = async () => {
    if (!dashboardId || isUpdatingShare) return;

    setIsUpdatingShare(true);
    setCopyStatus("");

    try {
      if (isDashboardPublic) {
        await disableShareLink();
      } else {
        await enableShareLink();
      }
    } catch (error) {
      alert(error instanceof Error ? error.message : "공유 설정을 변경하지 못했습니다.");
    } finally {
      setIsUpdatingShare(false);
    }
  };

  const handleCopyShareUrl = async () => {
    if (!shareUrl) return;

    try {
      await navigator.clipboard.writeText(shareUrl);
      setCopyStatus("Copied");
    } catch {
      setCopyStatus("Copy failed");
    }
  };

  return (
    <PageWrapper
      eyebrow="Preferences"
      title="Settings"
      description="Manage dashboard behavior and read-only sharing."
    >
      <div className="grid gap-4 lg:grid-cols-[0.8fr_1.2fr]">
        <section className="rounded-lg border border-slate-800 bg-[#111827] p-6">
          <h3 className="text-sm font-black uppercase tracking-wide text-white">Profile</h3>
          <div className="mt-6 flex items-center gap-4">
            <div className="flex h-14 w-14 items-center justify-center rounded-lg bg-cyan-500/15 text-lg font-black text-cyan-300 ring-1 ring-cyan-400/30">
              DB
            </div>
            <div>
              <p className="font-bold text-white">{dashboardName || "Dashboard user"}</p>
              <p className="mt-1 text-xs text-slate-500">
                {dashboardId ? `Dashboard #${dashboardId}` : "Loading dashboard"}
              </p>
            </div>
          </div>
        </section>

        <section className="rounded-lg border border-slate-800 bg-[#111827] p-6">
          <h3 className="text-sm font-black uppercase tracking-wide text-white">
            Read-only share link
          </h3>
          <div className="mt-5 flex flex-col gap-5">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <p className="font-bold text-white">External sharing</p>
                <p className="mt-1 text-sm text-slate-500">
                  Anyone with the link can view this dashboard without logging in.
                </p>
              </div>
              <button
                type="button"
                onClick={handleToggleShare}
                disabled={!dashboardId || isUpdatingShare}
                className={[
                  "relative h-7 w-12 shrink-0 rounded-full transition-colors disabled:cursor-not-allowed disabled:opacity-50",
                  isDashboardPublic ? "bg-cyan-500" : "bg-slate-700",
                ].join(" ")}
                aria-pressed={isDashboardPublic}
              >
                <span
                  className={[
                    "absolute top-1 h-5 w-5 rounded-full bg-white transition-transform",
                    isDashboardPublic ? "translate-x-6" : "translate-x-1",
                  ].join(" ")}
                />
              </button>
            </div>

            {isDashboardPublic && shareUrl ? (
              <div className="rounded-lg border border-slate-800 bg-slate-950/50 p-4">
                <label className="mb-2 block text-[10px] font-bold uppercase tracking-widest text-slate-500">
                  Public URL
                </label>
                <div className="flex flex-col gap-2 sm:flex-row">
                  <input
                    value={shareUrl}
                    readOnly
                    className="h-10 min-w-0 flex-1 rounded-lg border border-slate-700 bg-slate-900 px-3 text-xs text-slate-200 outline-none"
                  />
                  <button
                    type="button"
                    onClick={handleCopyShareUrl}
                    className="h-10 rounded-lg bg-cyan-600 px-4 text-xs font-bold text-white transition-colors hover:bg-cyan-500"
                  >
                    Copy
                  </button>
                </div>
                {copyStatus && <p className="mt-2 text-xs text-slate-500">{copyStatus}</p>}
              </div>
            ) : null}
          </div>
        </section>

        <section className="rounded-lg border border-slate-800 bg-[#111827] p-6 lg:col-span-2">
          <h3 className="text-sm font-black uppercase tracking-wide text-white">
            Dashboard Options
          </h3>
          <div className="mt-5 divide-y divide-slate-800">
            {settings.map((setting) => (
              <div
                key={setting.title}
                className="flex flex-col gap-4 py-5 first:pt-0 last:pb-0 sm:flex-row sm:items-center sm:justify-between"
              >
                <div>
                  <p className="font-bold text-white">{setting.title}</p>
                  <p className="mt-1 text-sm text-slate-500">{setting.description}</p>
                </div>
                <button
                  type="button"
                  className={[
                    "relative h-7 w-12 shrink-0 rounded-full transition-colors",
                    setting.enabled ? "bg-cyan-500" : "bg-slate-700",
                  ].join(" ")}
                  aria-pressed={setting.enabled}
                >
                  <span
                    className={[
                      "absolute top-1 h-5 w-5 rounded-full bg-white transition-transform",
                      setting.enabled ? "translate-x-6" : "translate-x-1",
                    ].join(" ")}
                  />
                </button>
              </div>
            ))}
          </div>
        </section>
      </div>
    </PageWrapper>
  );
}
