import { NavLink } from "react-router-dom";

import AppLogo from "../AppLogo";

export const navItems = [
  {
    to: "/dashboard",
    label: "대시보드",
    description: "실시간 설비 모니터링",
    icon: "M4 13h6V4H4v9Zm10 7h6V4h-6v16ZM4 20h6v-5H4v5Z",
  },
  {
    to: "/equipment",
    label: "설비 관리",
    description: "장비 등록 및 센서 관리",
    icon: "M4 7h16v3H4V7Zm2 5h12v7H6v-7Zm2 2v3h2v-3H8Zm5 0v3h2ZM7 4h10v2H7V4Z",
  },
];

type AppSidebarProps = {
  isSidebarOpen: boolean;
  isMobileSidebarOpen: boolean;
  onToggleSidebar: () => void;
  onCloseMobileSidebar: () => void;
};

function SidebarItem({
  to,
  label,
  description,
  icon,
  open,
}: {
  to: string;
  label: string;
  description: string;
  icon: string;
  open: boolean;
}) {
  return (
    <NavLink
      to={to}
      title={!open ? label : undefined}
      className={({ isActive }) =>
        [
          "group flex items-center gap-3 rounded-lg border px-3 py-3 text-sm font-semibold transition-colors",
          isActive
            ? "border-cyan-400/40 bg-cyan-400/10 text-white"
            : "border-transparent text-slate-400 hover:border-slate-700 hover:bg-slate-800/70 hover:text-slate-100",
          open ? "justify-start" : "justify-center",
        ].join(" ")
      }
    >
      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-slate-900/80 text-cyan-300 ring-1 ring-slate-700/80 group-hover:ring-cyan-400/50">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <path d={icon} />
        </svg>
      </span>
      {open && (
        <span className="min-w-0">
          <span className="block truncate">{label}</span>
          <span className="mt-0.5 block truncate text-[11px] font-medium text-slate-500">
            {description}
          </span>
        </span>
      )}
    </NavLink>
  );
}

export default function AppSidebar({
  isSidebarOpen,
  isMobileSidebarOpen,
  onToggleSidebar,
  onCloseMobileSidebar,
}: AppSidebarProps) {
  const isOpen = isSidebarOpen || isMobileSidebarOpen;

  return (
    <aside
      className={[
        "fixed inset-y-0 left-0 z-50 flex h-screen w-64 shrink-0 flex-col border-r border-slate-800 bg-[#0D1117]/95 backdrop-blur transition-transform duration-300 ease-in-out md:sticky md:z-auto md:translate-x-0 md:transition-[width]",
        isMobileSidebarOpen ? "translate-x-0" : "-translate-x-full",
        isSidebarOpen ? "md:w-64" : "md:w-20",
      ].join(" ")}
    >
      <div className="flex h-16 items-center justify-between border-b border-slate-800 px-4">
        <div className="flex min-w-0 items-center gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-cyan-500 text-slate-950 shadow-lg shadow-cyan-500/15">
            <AppLogo className="h-7 w-7" />
          </div>

          {isSidebarOpen && (
            <div className="min-w-0">
              <div className="truncate text-sm font-black uppercase tracking-tight text-white">
                Nexus OS
              </div>
              <div className="truncate text-[10px] font-mono text-slate-500">
                제어 패널
              </div>
            </div>
          )}
        </div>

        <button
          type="button"
          onClick={onToggleSidebar}
          className="ml-2 hidden rounded-md px-2 py-1 text-sm text-slate-400 transition-colors hover:bg-slate-800 hover:text-white md:block"
          aria-label={isSidebarOpen ? "사이드바 접기" : "사이드바 펼치기"}
        >
          {isSidebarOpen ? "<" : ">"}
        </button>
        <button
          type="button"
          onClick={onCloseMobileSidebar}
          className="ml-2 rounded-md px-2 py-1 text-sm text-slate-400 transition-colors hover:bg-slate-800 hover:text-white md:hidden"
          aria-label="사이드바 닫기"
        >
          x
        </button>
      </div>

      <nav className="flex-1 space-y-2 p-3">
        {navItems.map((item) => (
          <div key={item.to} onClick={onCloseMobileSidebar}>
            <SidebarItem {...item} open={isOpen} />
          </div>
        ))}
      </nav>

      <div className="border-t border-slate-800 p-3">
        <div
          className={[
            "flex items-center gap-3 rounded-lg border border-emerald-500/20 bg-emerald-500/10 p-3",
            isSidebarOpen ? "justify-start" : "justify-center",
          ].join(" ")}
        >
          <div className="h-2 w-2 shrink-0 rounded-full bg-emerald-400 shadow-[0_0_14px_rgba(52,211,153,0.7)]" />
          {isSidebarOpen && (
            <div className="min-w-0">
              <div className="truncate text-[10px] font-bold uppercase tracking-wider text-emerald-400">
                Live Connection
              </div>
              <div className="truncate text-[10px] text-slate-500">OPC-UA v2.1</div>
            </div>
          )}
        </div>
      </div>
    </aside>
  );
}
