import { useCallback, useState } from "react";

import {
  disableDashboardShare,
  enableDashboardShare,
  updateDashboard,
  type DashboardResponse,
} from "../api/client";

export function useDashboardMeta() {
  const [dashboardId, setDashboardId] = useState<number | null>(null);
  const [dashboardName, setDashboardName] = useState("");
  const [isDashboardPublic, setIsDashboardPublic] = useState(false);
  const [dashboardShareToken, setDashboardShareToken] = useState<string | null>(null);

  const setDashboardFromResponse = useCallback((dashboard: DashboardResponse) => {
    setDashboardId(dashboard.dashboardId);
    setDashboardName(dashboard.dashboardName);
    setIsDashboardPublic(Boolean(dashboard.isPublic));
    setDashboardShareToken(dashboard.shareToken ?? null);
  }, []);

  const enableShareLink = useCallback(async () => {
    if (!dashboardId) {
      throw new Error("대시보드를 불러온 뒤 공유 링크를 활성화할 수 있습니다.");
    }

    const response = await enableDashboardShare(dashboardId);

    if (!response.success || !response.data) {
      throw new Error(response.message ?? "공유 링크 활성화에 실패했습니다.");
    }

    setDashboardName(response.data.dashboardName);
    setIsDashboardPublic(response.data.isPublic);
    setDashboardShareToken(response.data.shareToken);

    return response.data;
  }, [dashboardId]);

  const disableShareLink = useCallback(async () => {
    if (!dashboardId) {
      throw new Error("대시보드를 불러온 뒤 공유 링크를 비활성화할 수 있습니다.");
    }

    const response = await disableDashboardShare(dashboardId);

    if (!response.success || !response.data) {
      throw new Error(response.message ?? "공유 링크 비활성화에 실패했습니다.");
    }

    setDashboardName(response.data.dashboardName);
    setIsDashboardPublic(response.data.isPublic);
    setDashboardShareToken(response.data.shareToken);

    return response.data;
  }, [dashboardId]);

  const updateDashboardTitle = useCallback(async (nextTitle: string) => {
    const dashboardTitle = nextTitle.trim();

    if (!dashboardTitle) {
      throw new Error("대시보드 제목을 입력해주세요.");
    }

    if (!dashboardId) {
      setDashboardName(dashboardTitle);
      return;
    }

    const response = await updateDashboard(dashboardId, { dashboardName: dashboardTitle });

    if (!response.success || !response.data) {
      throw new Error(response.message ?? "대시보드 제목 수정에 실패했습니다.");
    }

    setDashboardName(response.data.dashboardName);
  }, [dashboardId]);

  return {
    dashboardId,
    dashboardName,
    isDashboardPublic,
    dashboardShareToken,
    setDashboardFromResponse,
    enableShareLink,
    disableShareLink,
    updateDashboardTitle,
  };
}
