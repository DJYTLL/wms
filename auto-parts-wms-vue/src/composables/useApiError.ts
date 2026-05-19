import type { AxiosError } from 'axios';
import { ElMessage } from 'element-plus';
import { useI18n } from 'vue-i18n';

type ApiErrorPayload = {
  message?: string;
};

type ErrorMessageFallbacks = {
  network: string;
  server: string;
  timeout: string;
};

const RECENT_ERROR_TTL_MS = 3000;
const recentErrorMessages = new Map<string, number>();

const showErrorOnce = (message: string) => {
  const now = Date.now();
  const lastShownAt = recentErrorMessages.get(message);
  if (lastShownAt && now - lastShownAt < RECENT_ERROR_TTL_MS) {
    return;
  }
  recentErrorMessages.set(message, now);
  window.setTimeout(() => {
    if (recentErrorMessages.get(message) === now) {
      recentErrorMessages.delete(message);
    }
  }, RECENT_ERROR_TTL_MS);
  ElMessage.error(message);
};

const resolveErrorMessage = (
  error: unknown,
  fallback: string,
  fallbacks: ErrorMessageFallbacks,
) => {
  if (!error) return fallback;
  if (typeof error === 'string') return error;

  const axiosError = error as AxiosError<ApiErrorPayload> & {
    response?: { data?: any };
  };
  const apiMessage = axiosError.response?.data?.message;
  if (apiMessage) return apiMessage;
  const apiErrorMessage = axiosError.response?.data?.errorMessage;
  if (apiErrorMessage) return apiErrorMessage;
  if (typeof axiosError.response?.data === 'string' && axiosError.response?.data.trim()) {
    return axiosError.response?.data;
  }

  if (axiosError.code === 'ECONNABORTED' || axiosError.message?.toLowerCase().includes('timeout')) {
    return fallbacks.timeout;
  }

  if (axiosError.response?.status && axiosError.response.status >= 500) {
    return fallbacks.server;
  }

  if (!axiosError.response && axiosError.request) {
    return fallbacks.network;
  }

  if (error instanceof Error && error.message) return error.message;

  return fallback;
};

export const useApiError = () => {
  const { t } = useI18n();

  const notifySuccess = (message?: string) => {
    ElMessage.success(message || t('message.success'));
  };

  const notifyWarning = (message?: string) => {
    ElMessage.warning(message || t('message.required'));
  };

  const notifyError = (error: unknown, fallbackKey = 'message.networkError') => {
    const fallback = t(fallbackKey);
    const message = resolveErrorMessage(error, fallback, {
      network: t('message.serverUnavailable'),
      server: t('message.serverError'),
      timeout: t('message.requestTimeout'),
    });
    showErrorOnce(message);
  };

  return {
    notifySuccess,
    notifyWarning,
    notifyError,
  };
};
