export interface MasterOptionItem {
  id: number;
  name: string;
}

export interface LocationMasterOptionItem extends MasterOptionItem {
  warehouseId?: number;
}

export const MASTER_DATA_CODE_PATTERN = /^[A-Z0-9_/-]+$/;

export const MASTER_DATA_CODE_HINT = '编码只能包含字母、数字、短横线、下划线或斜杠';

export const normalizeMasterCode = (value: string) => value.trim().toUpperCase();

export const isValidMasterCode = (value: string) => MASTER_DATA_CODE_PATTERN.test(normalizeMasterCode(value));

export const mergeOptionById = <T extends MasterOptionItem>(options: T[], candidate?: T | null) => {
  if (!candidate) return options;
  if (options.some(item => item.id === candidate.id)) {
    return options;
  }
  return [...options, candidate];
};
