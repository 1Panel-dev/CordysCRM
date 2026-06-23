<template>
  <CrmDrawer
    v-model:show="show"
    :width="1000"
    :min-width="600"
    :ok-text="t('common.confirm')"
    :ok-disabled="loading"
    :title="t('process.process.flow.setTriggerCondition')"
    :footer="!props.readonly"
    @confirm="handleConfirm"
  >
    <n-spin :show="loading">
      <div class="flex flex-col gap-[16px]">
        <n-form ref="formRef" :model="form" label-placement="top" require-mark-placement="right">
          <n-form-item
            :label="t('process.process.flow.conditionName')"
            path="name"
            :rule="[
              {
                required: true,
                message: t('common.notNull', { value: t('process.process.flow.conditionName') }),
                trigger: ['input', 'blur'],
              },
            ]"
          >
            <n-input
              v-model:value="form.name"
              :disabled="props.readonly"
              :maxlength="255"
              type="text"
              :placeholder="t('common.pleaseInput')"
            />
          </n-form-item>
        </n-form>

        <FilterContent
          ref="filterContentRef"
          v-model:form-model="form.conditionConfig"
          no-filter-option
          :readonly="props.readonly"
          :config-list="filterConfigList"
          :custom-list="customFieldsFilterConfig"
        >
          <template #header>
            <div class="mb-[16px] flex items-center justify-between">
              <div>{{ t('process.process.flow.conditionRule') }}</div>
              <n-select
                v-model:value="form.sort"
                class="w-[100px]"
                size="small"
                :disabled="props.readonly"
                :options="props.priorityOptions ?? []"
              />
            </div>
          </template>
        </FilterContent>
      </div>
    </n-spin>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref, watch } from 'vue';
  import { FormInst, NForm, NFormItem, NInput, NSelect, NSpin } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ApprovalConditionBranch } from '@lib/shared/models/system/process';

  import FilterContent from '@/components/pure/crm-advance-filter/components/filterContent.vue';
  import {
    type ConditionsItem,
    type FilterForm,
    type FilterFormItem,
    filterOptionKeyMap,
  } from '@/components/pure/crm-advance-filter/type';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import { multipleValueTypeList } from '@/components/business/crm-form-create/config';

  import useConditionFilterConfig from './flow/useConditionFilterConfig';

  defineOptions({
    name: 'SetConditionDrawer',
  });

  const props = defineProps<{
    branch: ApprovalConditionBranch | null;
    formType: string;
    optionMap?: Record<string, any[]>;
    sort?: number;
    priorityOptions?: Array<{ label: string; value: number }>;
    readonly?: boolean;
  }>();

  const emit = defineEmits<{
    (
      e: 'confirm',
      payload: {
        name: string;
        sort: number;
        conditionConfig: FilterForm;
      }
    ): void;
  }>();

  const show = defineModel<boolean>('show', {
    required: true,
  });

  const { t } = useI18n();
  const { loading, filterConfigList, customFieldsFilterConfig, loadFilterConfig } = useConditionFilterConfig({
    formType: () => props.formType,
    optionMap: () => props.optionMap,
  });

  const formRef = ref<FormInst | null>(null);
  const filterContentRef = ref<InstanceType<typeof FilterContent> | null>(null);

  function createDefaultFormModel(): FilterForm {
    return {
      searchMode: 'AND',
      list: [{ dataIndex: null, operator: undefined, value: null, type: FieldTypeEnum.INPUT }],
    };
  }

  const form = ref<{
    name: string;
    sort: number;
    conditionConfig: FilterForm;
  }>({
    name: '',
    sort: 1,
    conditionConfig: createDefaultFormModel(),
  });

  function normalizeConditionList(conditionConfig: FilterForm) {
    const sourceList = conditionConfig.list?.length
      ? cloneDeep(conditionConfig.list)
      : conditionConfig.conditions?.map((item) => ({
          ...item,
          dataIndex: item.name ?? null,
          type: item.type ?? FieldTypeEnum.INPUT,
        })) ?? [];

    const configMap = [...filterConfigList.value, ...customFieldsFilterConfig.value].reduce((map, item) => {
      if (item.dataIndex) {
        map.set(item.dataIndex, item);
      }
      if (item.id) {
        map.set(item.id, item);
      }
      return map;
    }, new Map<string, FilterFormItem>());

    return sourceList.map((sourceItem): FilterFormItem => {
      const item = cloneDeep(sourceItem) as FilterFormItem;
      const configItem = item.dataIndex ? configMap.get(item.dataIndex) : undefined;
      const optionKey = filterOptionKeyMap[item.type];

      if (optionKey && item.dataIndex) {
        const values = Array.isArray(item.value) ? item.value : [item.value];
        item[optionKey] =
          props.optionMap?.[item.dataIndex]?.filter((option: { id: string }) => values.includes(option.id)) ?? [];
      }

      return {
        ...cloneDeep(configItem),
        ...item,
        dataIndex: configItem?.dataIndex ?? item.dataIndex,
        type: configItem?.type ?? item.type,
      };
    });
  }

  function initDraft(branch: ApprovalConditionBranch | null) {
    form.value = {
      name: branch?.name ?? '',
      sort: props.sort ?? 1,
      conditionConfig: branch?.conditionConfig
        ? {
            ...branch.conditionConfig,
            list: normalizeConditionList(branch.conditionConfig),
          }
        : createDefaultFormModel(),
    };
  }

  async function initialize() {
    await loadFilterConfig();
    initDraft(props.branch);
  }

  watch(
    () => [show.value, props.branch?.id, props.formType, props.sort],
    async ([visible]) => {
      if (visible) {
        await initialize();
      }
    }
  );

  function getParams() {
    const conditions: ConditionsItem[] = form.value.conditionConfig.list.map((item: any) => ({
      value: item.value,
      operator: item.operator,
      name: item.dataIndex ?? '',
      multipleValue: multipleValueTypeList.includes(item.type),
      type: item.type,
      containChildIds: item.containChildIds || [],
    }));

    return {
      list: form.value.conditionConfig.list,
      searchMode: form.value.conditionConfig.searchMode,
      conditions,
    };
  }

  async function handleConfirm() {
    try {
      await formRef.value?.validate();
      await filterContentRef.value?.formRef?.validate();

      emit('confirm', {
        name: form.value.name.trim(),
        sort: form.value.sort,
        conditionConfig: getParams(),
      });

      show.value = false;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
</script>

<style lang="less" scoped>
  :deep(.list-operator) {
    width: 140px;
  }
</style>
