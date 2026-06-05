import type { EquipmentMaster } from "../types/equipment";

export const generateMassiveMockData = () => {
  const massiveData: EquipmentMaster[] = [];
  const types = ['CVD', 'ETCH', 'PVD', 'DIFFUSION', 'CLEANING'];

  for (let i = 1; i <= 100; i++) {
    const type = types[i % types.length];
    massiveData.push({
      id: `EQ-${String(i).padStart(3, '0')}`,
      name: `${type} System-${String(i).padStart(3, '0')}`,
      type: type,
      sensors: Array.from({ length: 50 }, (_, j) => {

        const dataType = j % 3 === 0 ? 'FLOAT' : j % 3 === 1 ? 'BOOLEAN' : 'INTEGER';

        // 타입에 어울리는 라벨과 유닛 설정
        let label = "";
        let unit = "";
        if (dataType === 'FLOAT') {
          label = `Temp_Sensor_${j}`;
          unit = '°C';
        } else if (dataType === 'BOOLEAN') {
          label = `Power_Status_${j}`;
          unit = 'BOOL';
        } else {
          label = `Cycle_Count_${j}`;
          unit = 'cnt';
        }

        return {
          id: `sns-${i}-${j}`,
          label: label,
          unit: unit,
          dataType: dataType // 👈 이 값이 위젯 빌더의 'AI 추천' 로직으로 전달됩니다!
        };
      })
    });
  }
  return massiveData;
};

export default generateMassiveMockData;
