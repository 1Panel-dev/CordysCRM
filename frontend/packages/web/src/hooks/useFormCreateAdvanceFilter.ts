import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
import type { FormDesignConfigDetailParams } from '@lib/shared/models/system/module';

import type { FilterFormItem } from '@/components/pure/crm-advance-filter/type';
import type { FormCreateField } from '@/components/business/crm-form-create/types';

export default function useFormCreateFilter() {
  const customFieldsFilterConfig = ref<FilterFormItem[]>([]);
  // 获取配置属性
  function getFilterListConfig(res: FormDesignConfigDetailParams, addDefaultKeyAsId = false) {
    // 筛选下拉的第三列展示类型: 公式字段按计算结果格式区分文本/数字;
    // 统计字段的值由后端聚合得出, 本身就是数字, 因此与数字字段一致按数字输入框渲染,
    // 否则渲染成文本输入框, 提交给后端的 value 会是字符串而不是数字
    const getFilterDisplayType = (field: FormCreateField) => {
      if (field.type === FieldTypeEnum.FORMULA) {
        return field.formulaResultFormat === 'number' ? FieldTypeEnum.INPUT_NUMBER : FieldTypeEnum.INPUT;
      }
      if (field.type === FieldTypeEnum.STATISTIC) {
        return FieldTypeEnum.INPUT_NUMBER;
      }
      return field.type;
    };

    const getConfigProps = (field: FormCreateField) => {
      if (
        [FieldTypeEnum.SELECT, FieldTypeEnum.SELECT_MULTIPLE, FieldTypeEnum.RADIO, FieldTypeEnum.CHECKBOX].includes(
          field.type
        )
      ) {
        return {
          selectProps: {
            options: field.options?.map(({ disabled: _disabled, ...option }) => option),
            multiple: true,
          },
        };
      }
      if ([FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(field.type)) {
        return {
          dataSourceProps: {
            dataSourceType: field.dataSourceType,
            maxTagCount: 'responsive',
          },
        };
      }
      // TODO: 其他类型
      return {};
    };
    return (res.fields || []).reduce((acc: FilterFormItem[], field: FormCreateField) => {
      if (
        ![
          FieldTypeEnum.TEXTAREA,
          FieldTypeEnum.PICTURE,
          FieldTypeEnum.DIVIDER,
          FieldTypeEnum.SUB_PRICE,
          FieldTypeEnum.SUB_PRODUCT,
        ].includes(field.type)
      ) {
        let key = field.businessKey || field.id;
        if (field.resourceFieldId) {
          // 数据源引用字段用 id作为 key
          key = field.id;
        }
        acc.push({
          title: field.name,
          dataIndex: key,
          type: field.type,
          filterDisplayType: getFilterDisplayType(field),
          ...(addDefaultKeyAsId ? { id: field.id } : {}),
          ...getConfigProps(field),
        } as FilterFormItem);
      }
      return acc;
    }, []);
  }

  return {
    getFilterListConfig,
    customFieldsFilterConfig,
  };
}
