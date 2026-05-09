import type { AxiosError } from 'axios';
import { ElMessage } from 'element-plus';
import { useI18n } from 'vue-i18n';

type ApiErrorPayload = {
  message?: string;
};

const resolveErrorMessage = (error: unknown, fallback: string) => {
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
    const message = resolveErrorMessage(error, fallback);
    ElMessage.error(message);
  };

  return {
    notifySuccess,
    notifyWarning,
    notifyError,
  };
};
