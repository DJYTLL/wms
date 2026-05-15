import { useI18n } from 'vue-i18n';

export const useValidationMessage = () => {
  const { t } = useI18n();

  const requiredFieldMessage = (field: string) => t('message.requiredWithField', { field });

  const requiredRowFieldMessage = (row: number, field: string) => t('message.requiredWithFieldRow', { row, field });

  const positiveRowFieldMessage = (row: number, field: string) => t('message.mustBePositiveWithFieldRow', { row, field });

  const invalidRowFieldMessage = (row: number, field: string) => t('message.invalidNumberWithFieldRow', { row, field });

  return {
    requiredFieldMessage,
    requiredRowFieldMessage,
    positiveRowFieldMessage,
    invalidRowFieldMessage,
  };
};
