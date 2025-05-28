<template>
  <CrmModal v-model:show="visible" size="large" :title="t('advanceFilter.advancedFilter')">
    <n-scrollbar class="max-h-[60vh]">
      <FilterContent
        ref="filterContentRef"
        v-model:form-model="formModel"
        :config-list="props.configList"
        :custom-list="props.customList"
      />
    </n-scrollbar>
    <template #footer>
      <div class="mb-[22px] flex items-center gap-[12px]">
        <n-button type="default" class="outline--secondary" @click="handleReset">
          {{ t('common.reset') }}
        </n-button>
        <n-button class="mr-[12px]" type="primary" @click="handleFilter">
          {{ t('advanceFilter.filter') }}
        </n-button>
      </div>
    </template>
  </CrmModal>
</template>

<script lang="ts" setup>
  import { NButton, NScrollbar } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import { multipleValueTypeList } from '@/components/business/crm-form-create/config';
  import FilterContent from './components/filterContent.vue';

  import { ConditionsItem, FilterForm, FilterFormItem, FilterResult } from './type';

  const visible = defineModel<boolean>('visible', { required: true });

  const { t } = useI18n();

  const props = defineProps<{
    configList: FilterFormItem[];
    customList?: FilterFormItem[];
  }>();

  const emit = defineEmits<{
    (e: 'handleFilter', value: FilterResult): void;
    (e: 'refreshViewList'): void;
    (e: 'reset'): void;
  }>();

  const defaultFormModel: FilterForm = {
    searchMode: 'AND',
    list: [{ dataIndex: null, operator: undefined, value: null, type: FieldTypeEnum.INPUT }],
  };
  const formModel = ref<FilterForm>(cloneDeep(defaultFormModel));

  const savedFormModel = ref(cloneDeep(formModel.value));
  const filterContentRef = ref<InstanceType<typeof FilterContent>>();

  // 重置
  function handleReset() {
    filterContentRef.value?.formRef?.restoreValidation();
    formModel.value = JSON.parse(JSON.stringify(savedFormModel.value));
  }

  function getParams(): FilterResult {
    const conditions: ConditionsItem[] = formModel.value.list.map((item: any) => ({
      value: item.value,
      operator: item.operator,
      name: item.dataIndex ?? '',
      multipleValue: multipleValueTypeList.includes(item.type),
    }));

    return {
      searchMode: formModel.value.searchMode,
      conditions,
    };
  }

  // 过滤
  function handleFilter() {
    filterContentRef.value?.formRef?.validate((errors) => {
      if (!errors) {
        visible.value = false;
        emit('handleFilter', getParams());
      }
    });
  }

  defineExpose({
    handleReset,
  });
</script>
