import { type SelectOption } from 'naive-ui';

import { FieldDataSourceTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { CustomFormItem } from '@lib/shared/models/customForm';

export interface DataSourceOption extends SelectOption {
  formKey?: FormDesignKeyEnum;
  dataSourceType?: FieldDataSourceTypeEnum | string;
}

export function isCustomDataSourceType(dataSourceType?: FieldDataSourceTypeEnum | string) {
  if (!dataSourceType) {
    return false;
  }

  return !Object.values(FieldDataSourceTypeEnum).includes(dataSourceType as FieldDataSourceTypeEnum);
}

export function getCustomDataSourceName(dataSourceType: string | undefined, customDataSourceForms: CustomFormItem[]) {
  if (!dataSourceType) {
    return '';
  }

  return customDataSourceForms.find((item) => item.id === dataSourceType)?.name || '';
}

export function getDataSourceFormKey(
  dataSourceType: FieldDataSourceTypeEnum | string | undefined,
  map: Partial<Record<FieldDataSourceTypeEnum, FormDesignKeyEnum>>,
  fallback?: FormDesignKeyEnum
) {
  if (isCustomDataSourceType(dataSourceType)) {
    return FormDesignKeyEnum.CUSTOM_FORM;
  }

  if (!dataSourceType) {
    return fallback;
  }

  return map[dataSourceType as FieldDataSourceTypeEnum] || fallback;
}
