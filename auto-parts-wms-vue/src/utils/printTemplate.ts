import request from '@/utils/request';

export interface PrintTemplateConfig {
  headerFields: string[];
  detailColumns: string[];
  showTotals: boolean;
  columnWidths: Record<string, number>;
}

export interface PrintTemplatePreviewPayload {
  headerTitle?: string;
  subTitle?: string;
  footerNote?: string;
  fieldConfig?: string;
}

const PREVIEW_STORAGE_PREFIX = 'erp:print-template-preview:';

export const resolveTemplateId = (value: unknown): number | null => {
  const raw = Array.isArray(value) ? value[0] : value;
  if (raw === null || raw === undefined || raw === '') return null;
  const numeric = Number(raw);
  return Number.isInteger(numeric) && numeric > 0 ? numeric : null;
};

export const resolvePreviewConfigKey = (value: unknown): string | null => {
  const raw = Array.isArray(value) ? value[0] : value;
  if (typeof raw !== 'string') return null;
  const key = raw.trim();
  return key ? key : null;
};

export const normalizeColumnWidths = (
  input: Record<string, number> | undefined,
  columns: string[],
  defaults: Record<string, number>
): Record<string, number> => {
  const next: Record<string, number> = {};
  columns.forEach((key) => {
    const raw = input?.[key];
    const fallback = defaults[key] ?? 10;
    const numeric = Number(raw);
    next[key] = Number.isFinite(numeric) ? Math.max(4, Math.min(40, Math.round(numeric))) : fallback;
  });
  return next;
};

export const parsePrintTemplateConfig = (
  config: string | undefined,
  defaults: PrintTemplateConfig
): PrintTemplateConfig => {
  if (!config) {
    return {
      headerFields: [...defaults.headerFields],
      detailColumns: [...defaults.detailColumns],
      showTotals: defaults.showTotals,
      columnWidths: { ...defaults.columnWidths }
    };
  }

  try {
    const parsed = JSON.parse(config);
    const detailColumns = Array.isArray(parsed.detailColumns) ? parsed.detailColumns : defaults.detailColumns;
    return {
      headerFields: Array.isArray(parsed.headerFields) ? parsed.headerFields : defaults.headerFields,
      detailColumns,
      showTotals: parsed.showTotals !== undefined ? Boolean(parsed.showTotals) : defaults.showTotals,
      columnWidths: normalizeColumnWidths(parsed.columnWidths, detailColumns, defaults.columnWidths)
    };
  } catch {
    return {
      headerFields: [...defaults.headerFields],
      detailColumns: [...defaults.detailColumns],
      showTotals: defaults.showTotals,
      columnWidths: { ...defaults.columnWidths }
    };
  }
};

export const fetchPrintTemplate = async (docType: string, templateId?: number | null) => {
  const endpoint = templateId ? `/erp/print-templates/${templateId}` : '/erp/print-templates/default';
  const options = templateId ? undefined : { params: { docType } };
  const res: any = await request.get(endpoint, options);
  return res.data.data || null;
};

export const savePrintTemplatePreview = (key: string, payload: PrintTemplatePreviewPayload) => {
  if (typeof window === 'undefined' || !key) return;
  window.localStorage.setItem(`${PREVIEW_STORAGE_PREFIX}${key}`, JSON.stringify(payload));
};

export const readPrintTemplatePreview = (key?: string | null): PrintTemplatePreviewPayload | null => {
  if (typeof window === 'undefined' || !key) return null;
  const raw = window.localStorage.getItem(`${PREVIEW_STORAGE_PREFIX}${key}`);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as PrintTemplatePreviewPayload;
  } catch {
    return null;
  }
};
