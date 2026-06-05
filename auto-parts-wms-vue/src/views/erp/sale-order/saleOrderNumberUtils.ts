import type { SaleOrderItem } from './saleOrderTypes';

export const parseDecimal = (value: string | number | undefined, scale: number) => {
  if (value == null || value === '') return 0;
  const raw = String(value);
  const normalized = raw.endsWith('.') ? raw.slice(0, -1) : raw;
  if (!normalized) return 0;
  const regex = new RegExp(`^\\d+(\\.\\d{1,${scale}})?$`);
  if (!regex.test(normalized)) {
    return null;
  }
  return Number(normalized);
};

export const parseAmount = (value: string) => parseDecimal(value, 2);

export const formatQuickDecimal = (value: number, scale = 4) => {
  if (!Number.isFinite(value)) return '';
  return Number(value.toFixed(scale)).toString();
};

export const parsePositiveDecimal = (value: string | number | undefined, scale = 4) => {
  const parsed = parseDecimal(value, scale);
  return parsed && parsed > 0 ? parsed : null;
};

export const calcLineAmount = (row: SaleOrderItem) => {
  const qty = parseDecimal(row.qty, 4);
  const price = parseDecimal(row.price, 4);
  if (qty == null || price == null) return null;
  return qty * price;
};

export const formatMoney = (value: number | string | null) => {
  const num = typeof value === 'string' ? Number(value) : value;
  if (num == null || Number.isNaN(num)) return '-';
  return num.toFixed(2);
};

export const formatPlainNumber = (value: number | string | null | undefined) => {
  if (value == null || value === '') return '-';
  const num = Number(value);
  if (Number.isNaN(num)) return String(value);
  return String(num);
};

export const formatRate = (value: number | null) => {
  if (value == null || Number.isNaN(value)) return '-';
  return `${(value * 100).toFixed(2)}%`;
};
