import { useState, type Dispatch, type SetStateAction } from "react";

import type { EquipmentMaster, SelectedData } from "../types/dashboard";
import { buildSensorDataKey, type WidgetConfig } from "./dashboardStateUtils";

type UseWidgetBuilderParams = {
  allEquipments: EquipmentMaster[];
  closeWidgetModal: () => void;
  setTempSelection: Dispatch<SetStateAction<{ eqId: string; sensorId: string }>>;
  setSearchTerm: Dispatch<SetStateAction<string>>;
  tempSelection: { eqId: string; sensorId: string };
};

export function useWidgetBuilder({
  allEquipments,
  closeWidgetModal,
  setTempSelection,
  setSearchTerm,
  tempSelection,
}: UseWidgetBuilderParams) {
  const [newWidgetConfig, setNewWidgetConfig] = useState<WidgetConfig>({
    type: "GAUGE",
    dataKey: "",
    title: "New Widget",
  });
  const [builderStep, setBuilderStep] = useState<1 | 2>(1);
  const [selectedDataCart, setSelectedDataCart] = useState<SelectedData[]>([]);

  const resetWidgetBuilder = () => {
    closeWidgetModal();
    setBuilderStep(1);
    setSelectedDataCart([]);
    setTempSelection({ eqId: "", sensorId: "" });
    setSearchTerm("");
  };

  const addSelectedSensorToCart = () => {
    if (!tempSelection.eqId || !tempSelection.sensorId) {
      alert("장비와 센서를 모두 선택해주세요!");
      return;
    }

    const equipment = allEquipments.find((item) => item.id === tempSelection.eqId);
    const eqName = equipment?.name || "";
    const sensor = equipment?.sensors.find((item) => item.id === tempSelection.sensorId);
    const sensorKey = buildSensorDataKey(tempSelection.eqId, tempSelection.sensorId);

    const isExist = selectedDataCart.some(
      (item) =>
        item.eqId === tempSelection.eqId && item.sensorId === tempSelection.sensorId,
    );

    if (isExist) {
      alert("이미 장바구니에 담긴 데이터입니다!");
      return;
    }

    setSelectedDataCart((prev) => [
      ...prev,
      {
        ...tempSelection,
        eqName,
        sensorLabel: sensor?.label ?? tempSelection.sensorId,
        sensorKey,
        dataType: sensor?.dataType,
      },
    ]);
  };

  const removeSelectedSensorFromCart = (index: number) => {
    setSelectedDataCart((prev) => prev.filter((_, itemIndex) => itemIndex !== index));
  };

  const goToBuilderStep2 = () => {
    if (selectedDataCart.length === 0) {
      alert("최소 1개의 데이터를 담아주세요!");
      return;
    }

    const dataTypes = new Set(selectedDataCart.map((item) => item.dataType ?? "FLOAT"));
    const isMulti = selectedDataCart.length > 1;
    const isBooleanOnly = dataTypes.size === 1 && dataTypes.has("BOOLEAN");
    const numericTypes = new Set(["FLOAT", "DOUBLE", "INTEGER", "INT"]);
    const isNumericOnly = [...dataTypes].every((dataType) => numericTypes.has(dataType));

    if (isMulti && isNumericOnly) {
      setNewWidgetConfig((prev) => ({ ...prev, type: "TREND" }));
    } else if (isMulti && isBooleanOnly) {
      setNewWidgetConfig((prev) => ({ ...prev, type: "DONUT" }));
    } else if (isBooleanOnly) {
      setNewWidgetConfig((prev) => ({ ...prev, type: "STATUS" }));
    } else if (isNumericOnly) {
      setNewWidgetConfig((prev) => ({ ...prev, type: "GAUGE" }));
    } else {
      setNewWidgetConfig((prev) => ({ ...prev, type: "LOG" }));
    }

    setBuilderStep(2);
  };

  return {
    newWidgetConfig,
    builderStep,
    selectedDataCart,
    setNewWidgetConfig,
    setBuilderStep,
    setSelectedDataCart: setSelectedDataCart as Dispatch<SetStateAction<SelectedData[]>>,
    resetWidgetBuilder,
    addSelectedSensorToCart,
    removeSelectedSensorFromCart,
    goToBuilderStep2,
  };
}
