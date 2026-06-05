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
        :left-fields="realFieldList"
        :right-fields="realRightFields"
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
  import { CustomFormItem } from '@lib/shared/models/customForm';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import {
    getCustomDataSourceName,
    getDataSourceFormKey,
    isCustomDataSourceType,
  } from '@/components/business/crm-data-source-select/utils';
  import { dataSourceFilterFormKeyMap } from '@/components/business/crm-form-create/config';
  import { DataSourceFilterCombine, FormCreateField } from '@/components/business/crm-form-create/types';
  import FilterContent from './filterContent.vue';

  import { processStatusOptions } from '@/config/process.js';
  import useFormCreateApi from '@/hooks/useFormCreateApi';

  const visible = defineModel<boolean>('visible', { required: true });

  const { t } = useI18n();

  const props = defineProps<{
    searchMode?: 'AND' | 'OR';
    fieldConfig: FormCreateField;
    formFields: FormCreateField[];
    formKey: FormDesignKeyEnum;
    customDataSourceForms: CustomFormItem[];
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
  const dataSourceType = computed(() => props.fieldConfig.dataSourceType);
  const isCustomForm = computed(() => isCustomDataSourceType(dataSourceType.value));
  const formKey = computed<FormDesignKeyEnum>(
    () => getDataSourceFormKey(dataSourceType.value, dataSourceFilterFormKeyMap, FormDesignKeyEnum.CUSTOMER)!
  );

  const { fieldList, initFormConfig } = useFormCreateApi({
    formKey,
    customFormId: computed(() => (isCustomForm.value ? dataSourceType.value : undefined)),
  });
  const systemSpecialFieldList = [
    {
      id: 'approvalStatus',
      name: t('contract.approvalStatus'),
      type: FieldTypeEnum.APPROVAL_STATUS,
      businessKey: 'approvalStatus',
      icon: '',
      fieldWidth: 1,
      showLabel: true,
      description: '',
      readable: true,
      editable: false,
      mobile: true,
      rules: [],
      options: processStatusOptions,
    },
  ];
  const realFieldList = computed(() => {
    if (
      [
        FormDesignKeyEnum.CONTRACT,
        FormDesignKeyEnum.INVOICE,
        FormDesignKeyEnum.OPPORTUNITY_QUOTATION,
        FormDesignKeyEnum.ORDER,
      ].includes(formKey.value)
    ) {
      // 合同、发票、商机报价单、订单等需要加审批状态字段
      return [...fieldList.value, ...systemSpecialFieldList];
    }
    return fieldList.value;
  });
  const realRightFields = computed(() => {
    if (
      [
        FormDesignKeyEnum.CONTRACT,
        FormDesignKeyEnum.INVOICE,
        FormDesignKeyEnum.OPPORTUNITY_QUOTATION,
        FormDesignKeyEnum.ORDER,
      ].includes(props.formKey)
    ) {
      // 合同、发票、商机报价单、订单等需要加审批状态字段
      return [...props.formFields, ...systemSpecialFieldList];
    }
    return props.formFields;
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
      type: isCustomForm.value
        ? getCustomDataSourceName(dataSourceType.value, props.customDataSourceForms) || t('module.customForm')
        : t(`crmFormCreate.drawer.${formKey.value}`),
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
            matchType: 'MATCH_FIELD',
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
