import { computed, type MaybeRefOrGetter, ref, toValue } from 'vue';

import { OperatorEnum } from '@lib/shared/enums/commonEnum';
import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import type { OpportunityStageConfig } from '@lib/shared/models/opportunity';

import { operatorOptionsMap } from '@/components/pure/crm-advance-filter';
import type { FilterFormItem } from '@/components/pure/crm-advance-filter/type';
import { getFormConfigApiMap } from '@/components/business/crm-form-create/config';
import type { FormCreateField } from '@/components/business/crm-form-create/types';
import { resolveFieldId } from '@/components/business/crm-formula-editor/utils';

import { getContractStatusConfig, getFieldDeptTree, getOrderStatusConfig, getUserOptions } from '@/api/modules';
import { baseFilterConfigList } from '@/config/clue';
import { quotationStatus } from '@/config/opportunity';
import { processStatusOptions } from '@/config/process';
import useFormCreateFilter from '@/hooks/useFormCreateAdvanceFilter';

const fieldChangedOperatorOption = {
  label: 'advanceFilter.operator.newNotEqualOld',
  value: OperatorEnum.NEW_NOT_EQUALS_OLD,
};

function appendFieldChangedOperator(item: FilterFormItem): FilterFormItem {
  const operatorOptions = item.operatorOption?.length ? item.operatorOption : operatorOptionsMap[item.type] ?? [];
  return {
    ...item,
    operatorOption: [
      ...operatorOptions,
      ...(!operatorOptions.some((option) => option.value === OperatorEnum.NEW_NOT_EQUALS_OLD)
        ? [fieldChangedOperatorOption]
        : []),
    ],
  };
}

function flattenDepartmentOptions(options: any[] = []): Array<{ id: string; name: string }> {
  return options.flatMap((item) => [
    { id: item.id, name: item.name },
    ...flattenDepartmentOptions(item.children ?? []),
  ]);
}

export default function useConditionFilterConfig(options: {
  formType: MaybeRefOrGetter<string>;
  optionMap?: MaybeRefOrGetter<Record<string, any[]> | undefined>;
}) {
  const { t } = useI18n();
  const { getFilterListConfig } = useFormCreateFilter();

  const loading = ref(false);
  const filterConfigList = ref<FilterFormItem[]>([]);
  const customFieldsFilterConfig = ref<FilterFormItem[]>([]);
  const departmentOptions = ref<Array<{ id: string; name: string }>>([]);
  const userOptions = ref<Array<{ id: string; name: string }>>([]);
  const businessStageConfig = ref<OpportunityStageConfig | null>(null);

  // 已保存条件里可能存 dataIndex，也可能存字段 id；两种 key 都指向同一份字段配置，避免回显 raw id。
  const fieldConfigMap = computed<Record<string, FilterFormItem>>(() =>
    [...filterConfigList.value, ...customFieldsFilterConfig.value].reduce<Record<string, FilterFormItem>>(
      (map, item) => {
        if (item.dataIndex) {
          map[item.dataIndex] = item;
        }
        if (item.id) {
          map[item.id] = item;
        }
        return map;
      },
      {}
    )
  );

  // 给条件卡片描述用：fieldConfigMap 负责字段名/类型，optionMap 负责把选项值翻译成显示名。
  const descriptionContext = computed(() => ({
    optionMap: {
      ...(toValue(options.optionMap) ?? {}),
      departmentId: departmentOptions.value,
      createUser: userOptions.value,
      updateUser: userOptions.value,
    },
    fieldConfigMap: fieldConfigMap.value,
  }));

  function createDepartmentFilterItem(): FilterFormItem {
    return {
      title: t('opportunity.department'),
      dataIndex: 'departmentId',
      type: FieldTypeEnum.TREE_SELECT,
      treeSelectProps: {
        labelField: 'name',
        keyField: 'id',
        multiple: true,
        clearFilterAfterSelect: false,
        checkable: true,
        showContainChildModule: true,
      },
    };
  }

  function createApprovalStatusFilterItem(title: string): FilterFormItem {
    return {
      title,
      dataIndex: 'approvalStatus',
      type: FieldTypeEnum.SELECT_MULTIPLE,
      selectProps: {
        options: processStatusOptions,
      },
    };
  }

  function createOrderStatusFilterItem(): FilterFormItem {
    return {
      title: t('order.status'),
      dataIndex: 'stage',
      type: FieldTypeEnum.SELECT_MULTIPLE,
      selectProps: {
        options:
          businessStageConfig.value?.stageConfigList.map((item) => ({
            label: item.name,
            value: item.id,
          })) ?? [],
      },
    };
  }

  const formTypeConfigMap: Partial<Record<FormDesignKeyEnum, () => FilterFormItem[]>> = {
    [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: () => [
      {
        title: t('common.status'),
        dataIndex: 'invalid',
        type: FieldTypeEnum.SELECT_MULTIPLE,
        selectProps: {
          options: quotationStatus as any,
        },
      },
      createApprovalStatusFilterItem(t('common.approvalStatus')),
      createDepartmentFilterItem(),
      ...baseFilterConfigList,
    ],

    [FormDesignKeyEnum.CONTRACT]: () => [
      createDepartmentFilterItem(),
      {
        title: t('contract.status'),
        dataIndex: 'stage',
        type: FieldTypeEnum.SELECT_MULTIPLE,
        selectProps: {
          options:
            businessStageConfig.value?.stageConfigList.map((item) => ({
              label: item.name,
              value: item.id,
            })) ?? [],
        },
      },
      createApprovalStatusFilterItem(t('contract.approvalStatus')),
      ...baseFilterConfigList,
    ],

    [FormDesignKeyEnum.INVOICE]: () => [
      createDepartmentFilterItem(),
      createApprovalStatusFilterItem(t('contract.approvalStatus')),
      ...baseFilterConfigList,
    ],

    [FormDesignKeyEnum.ORDER]: () => [
      createDepartmentFilterItem(),
      createOrderStatusFilterItem(),
      createApprovalStatusFilterItem(t('common.approvalStatus')),
      ...baseFilterConfigList,
    ],
  };

  function createSystemFilterConfigList(formType: FormDesignKeyEnum): FilterFormItem[] {
    return formTypeConfigMap[formType]?.() ?? [...baseFilterConfigList];
  }

  function getFieldConfigProps(field: FormCreateField) {
    if (
      [FieldTypeEnum.SELECT, FieldTypeEnum.SELECT_MULTIPLE, FieldTypeEnum.RADIO, FieldTypeEnum.CHECKBOX].includes(
        field.type
      )
    ) {
      return {
        selectProps: {
          options: field.options,
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

    return {};
  }

  function createSubTableFilterConfigList(fields: FormCreateField[] = []) {
    // 子表格条件用“父字段.子字段”作为唯一 key，避免同一子表格下多个列共用父字段 id。
    return fields.flatMap((field) => {
      if (![FieldTypeEnum.SUB_PRICE, FieldTypeEnum.SUB_PRODUCT].includes(field.type) || !field.subFields?.length) {
        return [];
      }

      return field.subFields
        .filter(
          (subField) =>
            ![
              FieldTypeEnum.TEXTAREA,
              FieldTypeEnum.PICTURE,
              FieldTypeEnum.DIVIDER,
              FieldTypeEnum.SUB_PRICE,
              FieldTypeEnum.SUB_PRODUCT,
            ].includes(subField.type)
        )
        .map((subField) => {
          const dataIndex = `${field.id}.${resolveFieldId(subField, true)}`;
          return {
            id: dataIndex,
            title: `${field.name}.${subField.name}`,
            dataIndex,
            type: subField.type,
            ...getFieldConfigProps(subField),
          };
        }) as FilterFormItem[];
    });
  }

  async function loadOptionalOptions() {
    // 部门/用户只影响描述回显；失败时不能拖垮字段配置，否则抽屉第一列会显示原始 id。
    const [departmentResult, userResult] = await Promise.allSettled([getFieldDeptTree(), getUserOptions()]);

    departmentOptions.value =
      departmentResult.status === 'fulfilled' ? flattenDepartmentOptions(departmentResult.value) : [];
    userOptions.value =
      userResult.status === 'fulfilled' ? userResult.value.map((item: any) => ({ id: item.id, name: item.name })) : [];
  }

  // 每次加载字段配置都生成一个编号；旧请求晚回来时，编号对不上就丢弃，避免覆盖当前业务类型。
  let latestLoadId = 0;

  async function loadFilterConfig() {
    const loadId = ++latestLoadId;
    loading.value = true;

    try {
      const formType = toValue(options.formType) as FormDesignKeyEnum;
      const api = getFormConfigApiMap[formType];
      const stageConfigApiMap: Partial<Record<FormDesignKeyEnum, () => Promise<OpportunityStageConfig>>> = {
        [FormDesignKeyEnum.CONTRACT]: getContractStatusConfig,
        [FormDesignKeyEnum.ORDER]: getOrderStatusConfig,
      };
      const stageConfigApi = stageConfigApiMap[formType];

      const [stageConfig, formConfig] = await Promise.all([
        stageConfigApi?.() ?? Promise.resolve(null),
        api?.() ?? Promise.resolve({ fields: [] }),
      ]);
      // 例如先加载报价、马上切到发票：报价请求如果晚回来，不能再写入发票页面的描述上下文
      if (loadId !== latestLoadId) {
        return;
      }

      businessStageConfig.value = stageConfig;
      filterConfigList.value = createSystemFilterConfigList(formType).map(appendFieldChangedOperator);
      customFieldsFilterConfig.value = [
        ...getFilterListConfig(formConfig, true),
        ...createSubTableFilterConfigList(formConfig.fields),
      ].map(appendFieldChangedOperator);
      await loadOptionalOptions();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      filterConfigList.value = [];
      customFieldsFilterConfig.value = [];
    } finally {
      loading.value = false;
    }
  }

  return {
    loading,
    filterConfigList,
    customFieldsFilterConfig,
    descriptionContext,
    loadFilterConfig,
  };
}
