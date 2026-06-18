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

  import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { CustomFormItem } from '@lib/shared/models/customForm';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import {
    getCustomDataSourceName,
    getDataSourceFormKey,
    isCustomDataSourceType,
  } from '@/components/business/crm-data-source-select/utils';
  import { dataSourceFilterFormKeyMap } from '@/components/business/crm-form-create/config';
  import {
    DataSourceFilterCombine,
    FormCreateField,
    FormCreateFieldOption,
  } from '@/components/business/crm-form-create/types';
  import FilterContent from './filterContent.vue';

  import { getContractStatusConfig, getOrderStatusConfig } from '@/api/modules';
  import { quotationStatus } from '@/config/opportunity';
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
  const contractStageOptions = ref<{ label: string; value: string }[]>([]);
  const orderStageOptions = ref<{ label: string; value: string }[]>([]);

  const systemApprovalFieldList = [
    {
      id: 'approvalStatus',
      name: t('common.approvalStatus'),
      type: FieldTypeEnum.SELECT,
      isSystemField: true,
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
  const systemQuotationStatusFieldList = [
    {
      id: 'invalid',
      name: t('common.status'),
      type: FieldTypeEnum.SELECT,
      isSystemField: true,
      businessKey: 'invalid',
      icon: '',
      fieldWidth: 1,
      showLabel: true,
      description: '',
      readable: true,
      editable: false,
      mobile: true,
      rules: [],
      options: quotationStatus as unknown as FormCreateFieldOption[],
    },
  ];
  const systemFieldMap = computed<Partial<Record<FormDesignKeyEnum, FormCreateField[]>>>(() => ({
    [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: [...systemQuotationStatusFieldList, ...systemApprovalFieldList],
    [FormDesignKeyEnum.CONTRACT]: [
      {
        id: 'stage',
        name: t('contract.status'),
        type: FieldTypeEnum.SELECT,
        isSystemField: true,
        businessKey: 'stage',
        icon: '',
        fieldWidth: 1,
        showLabel: true,
        description: '',
        readable: true,
        editable: false,
        mobile: true,
        rules: [],
        options: contractStageOptions.value,
      } as FormCreateField,
      ...systemApprovalFieldList,
    ],
    [FormDesignKeyEnum.ORDER]: [
      {
        id: 'stage',
        name: t('order.status'),
        type: FieldTypeEnum.SELECT,
        isSystemField: true,
        businessKey: 'stage',
        icon: '',
        fieldWidth: 1,
        showLabel: true,
        description: '',
        readable: true,
        editable: false,
        mobile: true,
        rules: [],
        options: orderStageOptions.value,
      } as FormCreateField,
      ...systemApprovalFieldList,
    ],
    [FormDesignKeyEnum.INVOICE]: [...systemApprovalFieldList],
  }));

  const realFieldList = computed(() => {
    const systemSpecialFieldList = systemFieldMap.value[formKey.value] || [];
    return [...fieldList.value, ...systemSpecialFieldList];
  });
  const realRightFields = computed(() => {
    const systemSpecialFieldList = systemFieldMap.value[formKey.value] || [];
    return [...props.formFields, ...systemSpecialFieldList];
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

  async function initStageOptions() {
    contractStageOptions.value = [];
    orderStageOptions.value = [];

    if ([formKey.value, props.formKey].includes(FormDesignKeyEnum.CONTRACT)) {
      const contractStageConfig = await getContractStatusConfig();
      contractStageOptions.value = contractStageConfig.stageConfigList.map((item) => ({
        label: item.name,
        value: item.id,
      }));
    }

    if ([formKey.value, props.formKey].includes(FormDesignKeyEnum.ORDER)) {
      const orderStageConfig = await getOrderStatusConfig();
      orderStageOptions.value = orderStageConfig.stageConfigList.map((item) => ({
        label: item.name,
        value: item.id,
      }));
    }
  }

  watch(
    () => visible.value,
    async (val) => {
      if (val) {
        await initFormConfig();
        await initStageOptions();
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
