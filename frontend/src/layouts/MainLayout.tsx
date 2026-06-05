import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { getMe, logoutSession } from "../api/client";
import { useDashboardState } from "../hooks/useDashboardState";
import { useEquipmentWebSocket } from "../hooks/useEquipmentWebSocket";
import { getAccessToken, isLocalTestSession, logout } from "../utils/Auth";

import DashboardModals from "../components/dashboard/DashboardModals";
import AppSidebar, { navItems } from "../components/layout/AppSidebar";
import FloatingChatbot from "../components/layout/FloatingChatbot";
import { ALERTS_DATA } from "../components/mocks/dashboardMockData";
import AppLogo from "../components/AppLogo";

import "react-grid-layout/css/styles.css";
import "react-resizable/css/styles.css";

const INACTIVITY_WARNING_MS = 30 * 60 * 1000;
const INACTIVITY_LOGOUT_GRACE_MS = 60 * 1000;

export default function MainLayout() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  const [isMobileSidebarOpen, setIsMobileSidebarOpen] = useState(false);
  const [isTitleEditing, setIsTitleEditing] = useState(false);
  const [draftDashboardTitle, setDraftDashboardTitle] = useState("");
  const [isSavingDashboardTitle, setIsSavingDashboardTitle] = useState(false);
  const inactivityWarningTimer = useRef<number | null>(null);
  const inactivityLogoutTimer = useRef<number | null>(null);
  const [canEditDashboard, setCanEditDashboard] = useState(false);
  const [isAuthVerified, setIsAuthVerified] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();

  const dashboardState = useDashboardState({
    alertsData: ALERTS_DATA,
  });

  const {
    autoArrange,
    time,
    dashboardName,
    equipment,
    layouts,
    setEquipment,
    setIsModalOpen,
    setAutoArrange,
    arrangeWidgets,
    saveDashboardState,
    updateDashboardTitle,
    isDashboardDirty,
    isSavingDashboard,
    lastDashboardSavedAt,
    dashboardSaveError,
  } = dashboardState;

  useEquipmentWebSocket(setEquipment, layouts);

  const clearInactivityTimers = useCallback(() => {
    if (inactivityWarningTimer.current !== null) {
      window.clearTimeout(inactivityWarningTimer.current);
      inactivityWarningTimer.current = null;
    }

    if (inactivityLogoutTimer.current !== null) {
      window.clearTimeout(inactivityLogoutTimer.current);
      inactivityLogoutTimer.current = null;
    }
  }, []);

  const handleLogout = useCallback(async () => {
    const accessToken = getAccessToken();

    try {
      if (accessToken) {
        await logoutSession(accessToken);
      }
    } catch (error) {
      console.error("[Auth] Logout request failed", error);
    } finally {
      logout();
      navigate("/login", { replace: true });
    }
  }, [navigate]);

  useEffect(() => {
    let isActive = true;
    const accessToken = getAccessToken();

    if (!accessToken) {
      logout();
      navigate("/login", { replace: true });
      return undefined;
    }

    if (isLocalTestSession()) {
      setCanEditDashboard(true);
      setIsAuthVerified(true);
      return undefined;
    }

    setIsAuthVerified(false);
    getMe(accessToken)
      .then((response) => {
        if (!isActive) {
          return;
        }

        if (!response.success || !response.data) {
          throw new Error(response.message ?? "Failed to verify session.");
        }

        setCanEditDashboard(true);
        setIsAuthVerified(true);
      })
      .catch((error) => {
        if (!isActive) {
          return;
        }

        console.error("[Auth] Session verification failed", error);
        logout();
        navigate("/login", { replace: true });
      });

    return () => {
      isActive = false;
    };
  }, [navigate]);

  useEffect(() => {
    const activityEvents = [
      "mousedown",
      "mousemove",
      "keydown",
      "scroll",
      "touchstart",
      "click",
    ] as const;

    const resetInactivityTimers = () => {
      clearInactivityTimers();

      inactivityWarningTimer.current = window.setTimeout(() => {
        alert("장시간 활동이 없어 1분 후 자동 로그아웃됩니다.");
        inactivityLogoutTimer.current = window.setTimeout(() => {
          void handleLogout();
        }, INACTIVITY_LOGOUT_GRACE_MS);
      }, INACTIVITY_WARNING_MS);
    };

    resetInactivityTimers();
    activityEvents.forEach((eventName) => {
      window.addEventListener(eventName, resetInactivityTimers, { passive: true });
    });

    return () => {
      clearInactivityTimers();
      activityEvents.forEach((eventName) => {
        window.removeEventListener(eventName, resetInactivityTimers);
      });
    };
  }, [clearInactivityTimers, handleLogout]);

  const pageTitle = useMemo(() => {
    const current = navItems.find((item) => location.pathname.startsWith(item.to));
    return current?.label ?? "대시보드";
  }, [location.pathname]);

  const displayedDashboardTitle = dashboardName || `${equipment.name} 대시보드`;

  const startTitleEdit = useCallback(() => {
    setDraftDashboardTitle(displayedDashboardTitle);
    setIsTitleEditing(true);
  }, [displayedDashboardTitle]);

  const cancelTitleEdit = useCallback(() => {
    setDraftDashboardTitle("");
    setIsTitleEditing(false);
  }, []);

  const saveDashboardTitle = useCallback(async () => {
    const nextTitle = draftDashboardTitle.trim();

    if (!nextTitle) {
      alert("대시보드 제목을 입력해주세요.");
      return;
    }

    setIsSavingDashboardTitle(true);

    try {
      await updateDashboardTitle(nextTitle);
      setIsTitleEditing(false);
      setDraftDashboardTitle("");
    } catch (error) {
      alert(error instanceof Error ? error.message : "대시보드 제목 수정에 실패했습니다.");
    } finally {
      setIsSavingDashboardTitle(false);
    }
  }, [draftDashboardTitle, updateDashboardTitle]);

  if (!isAuthVerified) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#0B0F1A] text-sm font-semibold text-slate-300">
        Verifying session...
      </div>
    );
  }

  return (
    <div className="flex min-h-screen bg-[#0B0F1A] text-slate-200">
      {isMobileSidebarOpen && (
        <button
          type="button"
          className="fixed inset-0 z-40 bg-black/70 md:hidden"
          aria-label="사이드바 닫기"
          onClick={() => setIsMobileSidebarOpen(false)}
        />
      )}

      <AppSidebar
        isSidebarOpen={isSidebarOpen}
        isMobileSidebarOpen={isMobileSidebarOpen}
        onToggleSidebar={() => setIsSidebarOpen((prev) => !prev)}
        onCloseMobileSidebar={() => setIsMobileSidebarOpen(false)}
      />

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-50 flex min-h-16 flex-wrap items-center justify-between gap-3 border-b border-slate-800/60 bg-[#0D1117]/90 px-4 py-3 backdrop-blur md:px-6">
          <div className="flex min-w-0 items-center gap-4">
            <button
              type="button"
              onClick={() => setIsMobileSidebarOpen(true)}
              className="flex h-10 w-10 items-center justify-center rounded-lg border border-slate-700 bg-slate-900 text-slate-300 md:hidden"
              aria-label="사이드바 열기"
            >
              메뉴
            </button>
            <div className="hidden h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-cyan-500 text-slate-950 shadow-lg shadow-cyan-500/15 md:flex">
              <AppLogo className="h-7 w-7" />
            </div>
            <div className="min-w-0">
              {pageTitle === "대시보드" && canEditDashboard && isTitleEditing ? (
                <form
                  className="flex min-w-0 items-center gap-2"
                  onSubmit={(event) => {
                    event.preventDefault();
                    void saveDashboardTitle();
                  }}
                >
                  <input
                    value={draftDashboardTitle}
                    onChange={(event) => setDraftDashboardTitle(event.target.value)}
                    onKeyDown={(event) => {
                      if (event.key === "Escape") {
                        event.preventDefault();
                        cancelTitleEdit();
                      }
                    }}
                    disabled={isSavingDashboardTitle}
                    className="h-9 min-w-0 max-w-[320px] rounded-lg border border-slate-700 bg-slate-900 px-3 text-sm font-black text-white outline-none transition-colors focus:border-cyan-400 disabled:opacity-60"
                    autoFocus
                  />
                  <button
                    type="submit"
                    disabled={isSavingDashboardTitle}
                    className="rounded-lg bg-cyan-600 px-3 py-2 text-xs font-bold text-white transition-colors hover:bg-cyan-500 disabled:opacity-60"
                  >
                    저장
                  </button>
                  <button
                    type="button"
                    onClick={cancelTitleEdit}
                    disabled={isSavingDashboardTitle}
                    className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-xs font-bold text-slate-300 transition-colors hover:border-slate-500 hover:text-white disabled:opacity-60"
                  >
                    취소
                  </button>
                </form>
              ) : (
                <div className="flex min-w-0 items-center gap-2">
                  <h1 className="truncate text-sm font-black uppercase tracking-tight text-white">
                    {pageTitle === "대시보드" ? displayedDashboardTitle : pageTitle}
                  </h1>
                  {pageTitle === "대시보드" && canEditDashboard && (
                    <button
                      type="button"
                      onClick={startTitleEdit}
                      className="rounded-md p-1 text-slate-500 transition-colors hover:bg-slate-800 hover:text-white"
                      aria-label="대시보드 제목 수정"
                      title="대시보드 제목 수정"
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                        <path d="m4 16-.8 4 4-.8L18.5 7.9l-3.2-3.2L4 16Zm13.3-12.1 2.8 2.8" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                      </svg>
                    </button>
                  )}
                </div>
              )}
              <p className="mt-0.5 truncate text-[10px] font-mono text-slate-500">
                {time.toLocaleDateString()}
                <span className="ml-1 text-slate-400">{time.toLocaleTimeString()}</span>
              </p>
            </div>
          </div>

          <div className="hidden min-w-0 flex-1 md:block" />

          {canEditDashboard && location.pathname.startsWith("/dashboard") && (
            <div className="flex shrink-0 items-center gap-2">
              <button
                type="button"
                onClick={() => setIsModalOpen(true)}
                className="rounded-lg bg-cyan-600 px-4 py-2 text-xs font-bold text-white shadow-lg shadow-cyan-500/20 transition-colors hover:bg-cyan-500"
              >
                + 위젯 추가
              </button>
              <button
                type="button"
                onClick={() => setAutoArrange(!autoArrange)}
                className={[
                  "rounded-lg px-4 py-2 text-xs font-bold transition-colors",
                  autoArrange ? "bg-blue-600 text-white" : "bg-slate-800 text-slate-400",
                ].join(" ")}
              >
                자동 정렬: {autoArrange ? "켜짐" : "꺼짐"}
              </button>
              <button
                type="button"
                onClick={arrangeWidgets}
                className="rounded-lg border border-slate-700 bg-slate-900 px-4 py-2 text-xs font-bold text-slate-300 transition-colors hover:border-cyan-400 hover:text-white"
              >
                지금 정렬
              </button>
              <button
                type="button"
                onClick={() => void saveDashboardState()}
                disabled={isSavingDashboard}
                className={[
                  "rounded-lg px-4 py-2 text-xs font-bold transition-colors disabled:cursor-not-allowed disabled:opacity-60",
                  isDashboardDirty
                    ? "bg-amber-500 text-slate-950 hover:bg-amber-400"
                    : "bg-slate-800 text-slate-300 hover:bg-slate-700",
                ].join(" ")}
                title={dashboardSaveError ?? undefined}
              >
                {isSavingDashboard ? "저장 중..." : "저장"}
              </button>
              <span
                className={[
                  "hidden text-[10px] font-semibold md:inline",
                  dashboardSaveError
                    ? "text-rose-400"
                    : isDashboardDirty
                      ? "text-amber-300"
                      : "text-slate-500",
                ].join(" ")}
                title={dashboardSaveError ?? undefined}
              >
                {dashboardSaveError
                  ? "저장 실패"
                  : isDashboardDirty
                    ? "저장되지 않음"
                    : lastDashboardSavedAt
                      ? `저장됨 ${lastDashboardSavedAt.toLocaleTimeString()}`
                      : "Saved locally"}
              </span>
            </div>
          )}
          <button
            type="button"
            onClick={() => void handleLogout()}
            className="rounded-lg border border-slate-700 bg-slate-900 px-4 py-2 text-xs font-bold text-slate-300 transition-colors hover:border-rose-400 hover:text-rose-200"
          >
            로그아웃
          </button>
        </header>

        <main className="min-w-0 flex-1 overflow-x-hidden">
          <Outlet context={{ ...dashboardState, canEditDashboard }} />
        </main>
        {canEditDashboard && <DashboardModals state={dashboardState} />}
      </div>
      <FloatingChatbot />
    </div>
  );
}

