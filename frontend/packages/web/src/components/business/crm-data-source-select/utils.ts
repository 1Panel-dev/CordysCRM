import { type SelectOption } from 'naive-ui';

import { FieldDataSourceTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { CustomFormItem } from '@lib/shared/models/customForm';

export interface DataSourceOption extends SelectOption {
  formKey?: FormDesignKeyEnum;
  dataSourceType?: FieldDataSourceTypeEnum | string;
}

export function getCustomDataSourceName(dataSourceType: string | undefined, customDataSourceForms: CustomFormItem[]) {
  if (!dataSourceType) {
    return '';
  }

  return customDataSourceForms.find((item) => item.id === dataSourceType)?.name || '';
}

export function getDataSourceType(
  formKey: FormDesignKeyEnum | undefined | string,
  map: Partial<Record<FieldDataSourceTypeEnum, FormDesignKeyEnum>>,
  fallback?: FieldDataSourceTypeEnum
) {
  if (!formKey) {
    return fallback;
  }

  return (
    (Object.entries(map).find(([, mappedFormKey]) => mappedFormKey === formKey)?.[0] as
      | FieldDataSourceTypeEnum
      | undefined) || fallback
  );
}
