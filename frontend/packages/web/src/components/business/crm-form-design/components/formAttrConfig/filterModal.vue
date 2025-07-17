<template>
  <CrmModal
    v-model:show="visible"
    size="large"
    :title="t('crmFormDesign.dataSourceFilterSetting')"
    :positive-text="t('common.save')"
    footer
    @confirm="saveFilter"
    @cancel="handleCancel"
  >
    <n-scrollbar class="max-h-[60vh]">
      <FilterContent
        ref="filterContentRef"
        v-model:form-model="formModel"
        :left-fields="fieldList"
        :right-fields="props.formFields"
        :data-index-placeholder="dataIndexPlaceholder"
        :self-id="props.fieldConfig.id"
      />
    </n-scrollbar>
  </CrmModal>
</template>

<script lang="ts" setup>
  import { NScrollbar } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { FieldDataSourceTypeEnum, FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import { dataSourceFilterFormKeyMap } from '@/components/business/crm-form-create/config';
  import { DataSourceFilterCombine, FormCreateField } from '@/components/business/crm-form-create/types';
  import FilterContent from './filterContent.vue';

  import useFormCreateApi from '@/hooks/useFormCreateApi';

  const visible = defineModel<boolean>('visible', { required: true });

  const { t } = useI18n();

  const props = defineProps<{
    searchMode?: 'AND' | 'OR';
    fieldConfig: FormCreateField;
    formFields: FormCreateField[];
  }>();

  const emit = defineEmits<{
    (e: 'save', value: DataSourceFilterCombine): void;
  }>();

  const defaultFormModel: DataSourceFilterCombine = {
    searchMode: 'OR',
    conditions: [],
  };

  const formModel = ref<DataSourceFilterCombine>(
    cloneDeep(props.fieldConfig.combineSearch) || cloneDeep(defaultFormModel)
  );
  const formKey = computed<FormDesignKeyEnum>(() => {
    return dataSourceFilterFormKeyMap[
      props.fieldConfig.dataSourceType || FieldDataSourceTypeEnum.CUSTOMER
    ] as FormDesignKeyEnum;
  });

  const { fieldList, initFormConfig } = useFormCreateApi({
    formKey,
  });

  const filterContentRef = ref<InstanceType<typeof FilterContent>>();

  function saveFilter() {
    filterContentRef.value?.formRef?.validate((errors) => {
      if (!errors) {
        visible.value = false;
        emit('save', cloneDeep(formModel.value));
      }
    });
  }

  const dataIndexPlaceholder = computed(() => {
    return t('crmFormDesign.dataSourceFilterDataIndexPlaceholder', {
      type: t(
        `crmFormCreate.drawer.${
          dataSourceFilterFormKeyMap[props.fieldConfig.dataSourceType || FieldDataSourceTypeEnum.CUSTOMER]
        }`
      ),
    });
  });

  watch(
    () => visible.value,
    async (val) => {
      if (val) {
        await initFormConfig();
        formModel.value.conditions = cloneDeep(props.fieldConfig.combineSearch?.conditions) || [
          {
            leftFieldId: undefined,
            leftFieldType: FieldTypeEnum.INPUT,
            operator: undefined,
            rightFieldId: undefined,
            rightFieldCustom: false,
            rightFieldCustomValue: '',
            rightFieldType: FieldTypeEnum.INPUT, // 默认右侧字段类型为输入框
          },
        ];
      }
    },
    {
      immediate: true,
    }
  );

  function handleCancel() {
    formModel.value = cloneDeep(defaultFormModel);
  }
</script>
