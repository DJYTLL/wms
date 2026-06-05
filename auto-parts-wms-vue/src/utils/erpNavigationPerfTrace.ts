export const ERP_PERF_TRACE_STORAGE_KEY = 'erpPerfTrace';

type PerfTraceDetail = Record<string, unknown>;

type PerfTraceEntry = {
  label: string;
  atMs: number;
  deltaMs: number;
  sinceStartMs: number;
  detail?: PerfTraceDetail;
};

const getNow = () => {
  if (typeof performance !== 'undefined' && typeof performance.now === 'function') {
    return performance.now();
  }
  return Date.now();
};

const isErpNavigationPerfEnabled = () => {
  if (typeof window === 'undefined') {
    return false;
  }
  return window.localStorage.getItem(ERP_PERF_TRACE_STORAGE_KEY) === '1';
};

const getTraceEntries = (): PerfTraceEntry[] => {
  const globalWindow = window as typeof window & { __erpNavigationPerfTrace?: PerfTraceEntry[] };
  if (!globalWindow.__erpNavigationPerfTrace) {
    globalWindow.__erpNavigationPerfTrace = [];
  }
  return globalWindow.__erpNavigationPerfTrace;
};

export const resetErpNavigationPerfTrace = () => {
  if (typeof window === 'undefined') {
    return;
  }
  (window as typeof window & { __erpNavigationPerfTrace?: PerfTraceEntry[] }).__erpNavigationPerfTrace = [];
};

export const markErpNavigationPerf = (label: string, detail: PerfTraceDetail = {}) => {
  if (!isErpNavigationPerfEnabled()) {
    return;
  }

  const entries = getTraceEntries();
  const atMs = getNow();
  const firstAtMs = entries[0]?.atMs ?? atMs;
  const previousAtMs = entries[entries.length - 1]?.atMs ?? atMs;
  const entry: PerfTraceEntry = {
    label,
    atMs: Number(atMs.toFixed(2)),
    deltaMs: Number((atMs - previousAtMs).toFixed(2)),
    sinceStartMs: Number((atMs - firstAtMs).toFixed(2)),
    detail
  };
  entries.push(entry);

  console.log(`[ERP perf] ${label}`, {
    deltaMs: entry.deltaMs,
    sinceStartMs: entry.sinceStartMs,
    ...detail
  });
};

export const timeErpNavigationPerf = <T>(label: string, detail: PerfTraceDetail, action: () => T): T => {
  markErpNavigationPerf(`${label}:start`, detail);
  try {
    const result = action();
    markErpNavigationPerf(`${label}:end`, detail);
    return result;
  } catch (error) {
    markErpNavigationPerf(`${label}:error`, {
      ...detail,
      error: error instanceof Error ? error.message : String(error)
    });
    throw error;
  }
};

export const printErpNavigationPerfTrace = () => {
  if (!isErpNavigationPerfEnabled()) {
    return;
  }
  const entries = getTraceEntries();
  console.table(entries.map((entry) => ({
    label: entry.label,
    deltaMs: entry.deltaMs,
    sinceStartMs: entry.sinceStartMs,
    detail: JSON.stringify(entry.detail || {})
  })));
};
