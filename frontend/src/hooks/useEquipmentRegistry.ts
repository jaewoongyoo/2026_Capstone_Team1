import { useCallback, useState, type Dispatch, type SetStateAction } from "react";

import {
  applyEquipmentDiscovery,
  deleteEquipment,
  getDashboardEquipment,
  getEquipmentCurrent,
  getMyEquipmentCurrent,
  searchEquipmentSensors,
  searchMyEquipment,
} from "../api/client";
import type { EquipmentCurrentResponse } from "../api/client";
import type { UniversalEquipment } from "../types/equipment";
import type { DashboardItem, EquipmentMaster } from "../types/dashboard";
import {
  EMPTY_EQUIPMENT,
  mapAppliedEquipmentToMaster,
  mapCurrentResponseToMaster,
  mapCurrentResponseToEquipment,
  mapEquipmentResponseToMaster,
  mapEquipmentToMaster,
  mapSensorResponseToMeta,
} from "./dashboardStateUtils";

type TempSelection = {
  eqId: string;
  sensorId: string;
};

type UseEquipmentRegistryParams = {
  dashboardId: number | null;
  tempSelection: TempSelection;
  setTempSelection: Dispatch<SetStateAction<TempSelection>>;
  updateLayouts: (updater: (items: DashboardItem[]) => DashboardItem[]) => void;
};

const mergeEquipmentMasters = (groups: EquipmentMaster[][]) => {
  const equipmentById = new Map<string, EquipmentMaster>();

  groups.flat().forEach((equipment) => {
    const previous = equipmentById.get(equipment.id);

    equipmentById.set(equipment.id, {
      ...previous,
      ...equipment,
      type: equipment.type || previous?.type || "UNKNOWN",
      sensors: equipment.sensors.length > 0 ? equipment.sensors : previous?.sensors ?? [],
      sensorsLoaded: equipment.sensorsLoaded ?? previous?.sensorsLoaded,
    });
  });

  return Array.from(equipmentById.values());
};

export function useEquipmentRegistry({
  dashboardId,
  tempSelection,
  setTempSelection,
  updateLayouts,
}: UseEquipmentRegistryParams) {
  const [allEquipments, setAllEquipments] = useState<EquipmentMaster[]>([]);
  const [isEqModalOpen, setIsEqModalOpen] = useState(false);
  const [equipment, setEquipmentState] = useState<UniversalEquipment>(EMPTY_EQUIPMENT);
  const [equipmentById, setEquipmentById] = useState<Record<string, UniversalEquipment>>({});
  const [isNetworkScanning, setIsNetworkScanning] = useState(false);
  const [loadingSensorEquipmentId, setLoadingSensorEquipmentId] = useState<string | null>(null);

  const upsertEquipmentMaster = useCallback((nextEquipment: UniversalEquipment) => {
    const nextMaster = mapEquipmentToMaster(nextEquipment);

    setAllEquipments((prev) => {
      const existingIndex = prev.findIndex((item) => item.id === nextMaster.id);

      if (existingIndex === -1) {
        return [...prev, nextMaster];
      }

      return prev.map((item, index) => (index === existingIndex ? nextMaster : item));
    });
  }, []);

  const setEquipment: Dispatch<SetStateAction<UniversalEquipment>> = useCallback((value) => {
    setEquipmentState((prev) => {
      const nextEquipment = typeof value === "function" ? value(prev) : value;
      upsertEquipmentMaster(nextEquipment);
      setEquipmentById((previous) => ({
        ...previous,
        [nextEquipment.id]: nextEquipment,
      }));
      return nextEquipment;
    });
  }, [upsertEquipmentMaster]);

  const applyCurrentEquipment = useCallback((response: EquipmentCurrentResponse) => {
    setEquipment((prev) => mapCurrentResponseToEquipment(response, prev));
  }, [setEquipment]);

  const loadEquipmentCurrent = useCallback(async (equipmentId: string | number) => {
    try {
      const response = await getEquipmentCurrent(equipmentId);

      if (response.data) {
        applyCurrentEquipment(response.data);
      }
    } catch (error) {
      console.error("[Equipment Current] Failed to load equipment current", error);
    }
  }, [applyCurrentEquipment]);

  const loadInitialEquipmentCurrent = useCallback(async () => {
    if (!dashboardId) {
      return;
    }

    try {
      const response = await getMyEquipmentCurrent();
      const equipments = (response.data ?? []).filter(
        (item) => item.dashboardId === dashboardId,
      );

      if (equipments.length > 0) {
        equipments.forEach(applyCurrentEquipment);
      }
    } catch (error) {
      console.error("[Equipment Current] Failed to load current sensor values", error);
    }
  }, [applyCurrentEquipment, dashboardId]);

  const loadEquipmentSensors = useCallback(async (equipmentId: string | number, keyword = "", force = false) => {
    const equipmentKey = String(equipmentId);
    const currentEquipment = allEquipments.find((item) => item.id === equipmentKey);
    const liveSensorNames = new Set(
      (equipmentById[equipmentKey]?.sensors ?? []).map((sensor) => sensor.sensorId ?? sensor.label),
    );

    if (!force && !keyword && currentEquipment?.sensorsLoaded) {
      return currentEquipment.sensors;
    }

    setLoadingSensorEquipmentId(equipmentKey);

    try {
      const sensorsResponse = await searchEquipmentSensors(equipmentId, keyword);
      const sensors = (sensorsResponse.data ?? [])
        .map(mapSensorResponseToMeta)
        .filter((sensor) => liveSensorNames.size === 0 || liveSensorNames.has(sensor.label));

      setAllEquipments((prev) =>
        prev.map((item) =>
          item.id === equipmentKey
            ? {
              ...item,
              sensors,
              sensorsLoaded: true,
            }
            : item,
        ),
      );

      return sensors;
    } catch (error) {
      console.error("[Sensors] Failed to load equipment sensors", error);
      alert(error instanceof Error ? error.message : "센서 목록을 불러오지 못했습니다.");
      return [];
    } finally {
      setLoadingSensorEquipmentId((prev) => (prev === equipmentKey ? null : prev));
    }
  }, [allEquipments, equipmentById]);

  const selectEquipmentForDiscovery = useCallback((equipmentId: string) => {
    setTempSelection({ eqId: equipmentId, sensorId: "" });

    if (equipmentId) {
      void loadEquipmentSensors(equipmentId);
      void loadEquipmentCurrent(equipmentId);
    }
  }, [loadEquipmentCurrent, loadEquipmentSensors, setTempSelection]);

  const startNetworkScan = async () => {
    setIsNetworkScanning(true);

    try {
      const [dashboardEquipmentResponse, searchedEquipmentResponse, currentEquipmentResponse] =
        await Promise.all([
          dashboardId ? getDashboardEquipment(dashboardId) : Promise.resolve(undefined),
          searchMyEquipment(),
          getMyEquipmentCurrent(),
        ]);
      const dashboardEquipmentMasters =
        (dashboardEquipmentResponse?.data ?? []).map(mapEquipmentResponseToMaster);
      const searchedEquipmentMasters =
        (searchedEquipmentResponse.data ?? []).map(mapEquipmentResponseToMaster);
      const currentEquipments = (currentEquipmentResponse.data ?? [])
        .filter((item) => !dashboardId || item.dashboardId === dashboardId);
      const currentEquipmentMasters = currentEquipments.map(mapCurrentResponseToMaster);
      const equipmentMasters = mergeEquipmentMasters([
        searchedEquipmentMasters,
        dashboardEquipmentMasters,
        currentEquipmentMasters,
      ]);

      setAllEquipments(equipmentMasters);
      currentEquipments.forEach(applyCurrentEquipment);
      setTempSelection((prev) => {
        if (!prev.eqId || equipmentMasters.some((item) => item.id === prev.eqId)) {
          return prev;
        }

        return { eqId: "", sensorId: "" };
      });

      if (tempSelection.eqId && equipmentMasters.some((item) => item.id === tempSelection.eqId)) {
        void loadEquipmentSensors(tempSelection.eqId, "", true);
      }
    } catch (error) {
      console.error("[Network Scan] Failed to load equipment", error);
      alert(error instanceof Error ? error.message : "장비 목록을 불러오지 못했습니다.");
    } finally {
      setIsNetworkScanning(false);
    }
  };

  const closeEquipmentModal = () => {
    setIsEqModalOpen(false);
  };

  const applyEquipmentRegistration = async () => {
    if (!dashboardId) {
      alert("대시보드를 불러온 뒤 장비를 등록할 수 있습니다.");
      return;
    }

    const assets = allEquipments.map((equipment) => ({
      equipmentName: equipment.name,
      field: equipment.type,
      tags: equipment.sensors.map((sensor) => ({
        sensorName: sensor.label || sensor.id,
      })),
    }));

    if (assets.length === 0) {
      alert("등록할 장비가 없습니다.");
      return;
    }

    setIsNetworkScanning(true);

    try {
      const response = await applyEquipmentDiscovery({
        dashboardId,
        assets,
      });
      const appliedEquipment = response.data?.equipment ?? [];
      const nextEquipments = appliedEquipment.map(mapAppliedEquipmentToMaster);

      if (nextEquipments.length > 0) {
        setAllEquipments(nextEquipments);
        setTempSelection((prev) => {
          if (!prev.eqId || nextEquipments.some((item) => item.id === prev.eqId)) {
            return prev;
          }

          return { eqId: "", sensorId: "" };
        });
      }

      setIsEqModalOpen(false);
      alert("선택된 모든 자산과 태그가 성공적으로 동기화되었습니다.");
    } catch (error) {
      console.error("[Equipment Discovery] Failed to apply equipment discovery", error);
      alert(error instanceof Error ? error.message : "장비 등록에 실패했습니다.");
    } finally {
      setIsNetworkScanning(false);
    }
  };

  const removeEquipment = async (equipmentId: string) => {
    if (!equipmentId) return;

    const target = allEquipments.find((item) => item.id === equipmentId);

    if (!target) {
      return;
    }

    try {
      await deleteEquipment(equipmentId);

      setAllEquipments((prev) => prev.filter((item) => item.id !== equipmentId));
      setEquipmentById((prev) => {
        const next = { ...prev };
        delete next[equipmentId];
        return next;
      });
      setTempSelection((prev) => (prev.eqId === equipmentId ? { eqId: "", sensorId: "" } : prev));

      setEquipmentState((prev) =>
        prev.id === equipmentId
          ? {
            ...EMPTY_EQUIPMENT,
            lastUpdate: new Date().toISOString(),
          }
          : prev,
      );

      updateLayouts((items) =>
        items.filter((widget) =>
          String(widget.equipmentEntityId ?? widget.equipmentName ?? "") !== equipmentId &&
          widget.equipmentName !== target.name,
        ),
      );
    } catch (error) {
      console.error("[Equipment] Failed to delete equipment", error);
      alert(error instanceof Error ? error.message : "장비 삭제에 실패했습니다.");
    }
  };

  return {
    allEquipments,
    equipment,
    equipmentById,
    isEqModalOpen,
    isNetworkScanning,
    loadingSensorEquipmentId,
    setAllEquipments,
    setEquipment,
    setIsEqModalOpen,
    loadInitialEquipmentCurrent,
    loadEquipmentCurrent,
    loadEquipmentSensors,
    selectEquipmentForDiscovery,
    startNetworkScan,
    closeEquipmentModal,
    applyEquipmentRegistration,
    removeEquipment,
  };
}
