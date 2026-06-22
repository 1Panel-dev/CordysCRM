<template>
  <CrmModal
    v-model:show="show"
    :title="t('crmFormDesign.dataSourceDisplayField')"
    footer
    @confirm="handleConfirm"
    @cancel="handleCancel"
  >
    <n-scrollbar class="max-h-[60vh]">
      <FieldSection
        v-if="customList.length"
        v-model:selected-ids="selectedCustomIds"
        :items="customList"
        class="px-0 pt-0"
        :title="t('common.formFields')"
        @select-part="(ids) => updateSelectedList(ids, customList)"
        @select-item="(meta) => selectItem(meta, customList)"
      />
      <FieldSection
        v-if="systemList.length"
        v-model:selected-ids="selectedSystemIds"
        :items="systemList"
        class="px-0 pt-0"
        :title="t('common.systemFields')"
        @select-part="(ids) => updateSelectedList(ids, systemList)"
        @select-item="(meta) => selectItem(meta, systemList)"
      />
      <FieldSection
        v-if="subTableList.length"
        v-model:selected-ids="selectedSubTableIds"
        :items="subTableList"
        class="px-0 pt-0"
        :title="t('crmFormDesign.subTableField')"
        @select-part="(ids) => updateSelectedList(ids, subTableList)"
        @select-item="(meta) => selectItem(meta, subTableList)"
      />
    </n-scrollbar>
  </CrmModal>
</template>

<script lang="ts" setup>
  import { NScrollbar } from 'naive-ui';

  import { ColumnTypeEnum } from '@lib/shared/enums/commonEnum';
  import { FieldDataSourceTypeEnum, FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { ExportTableColumnItem } from '@lib/shared/models/common';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import { getDataSourceFormKey, isCustomDataSourceType } from '@/components/business/crm-data-source-select/utils';
  import { dataSourceFilterFormKeyMap } from '@/components/business/crm-form-create/config';
  import { FormCreateField } from '@/components/business/crm-form-create/types';
  import FieldSection from '@/components/business/crm-table-export-modal/components/fieldSection.vue';

  import {
    getBusinessTitleModuleForm,
    getContractStatusConfig,
    getFieldDisplayList,
    getOrderStatusConfig,
  } from '@/api/modules';
  import { quotationStatus } from '@/config/opportunity';
  import { processStatusOptions } from '@/config/process';

  const { t } = useI18n();

  const show = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const props = defineProps<{
    fieldConfig: FormCreateField;
    isSubTableField?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'save', selectedIdList: string[], selectedList: any[]): void;
  }>();

  type DisplayFieldOption = {
    label: string;
    value: string | number | boolean;
  };

  type DisplayFieldItem = ExportTableColumnItem &
    Omit<Partial<FormCreateField>, 'options'> & {
      name?: string;
      options?: DisplayFieldOption[];
    };

  const dataSourceType = computed(() => props.fieldConfig.dataSourceType);
  const isCustomForm = computed(() => isCustomDataSourceType(dataSourceType.value));
  const formKey = computed<FormDesignKeyEnum | undefined>(() =>
    getDataSourceFormKey(dataSourceType.value, dataSourceFilterFormKeyMap)
  );
  const isBusinessTitleSource = computed(
    () => props.fieldConfig.dataSourceType === FieldDataSourceTypeEnum.BUSINESS_TITLE
  );
  const contractStageOptions = ref<{ label: string; value: string }[]>([]);
  const orderStageOptions = ref<{ label: string; value: string }[]>([]);
  const customList = ref<DisplayFieldItem[]>([]);
  const subTableList = ref<DisplayFieldItem[]>([]);
  const systemColumnMap = computed<Record<string, DisplayFieldItem[]>>(() => ({
    [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: [
      {
        id: 'invalid',
        key: 'invalid',
        title: t('common.status'),
        name: t('common.status'),
        columnType: ColumnTypeEnum.CUSTOM,
        type: FieldTypeEnum.SELECT,
        fieldWidth: 1,
        showLabel: true,
        options: quotationStatus,
      },
      {
        id: 'approvalStatus',
        key: 'approvalStatus',
        title: t('common.approvalStatus'),
        name: t('common.approvalStatus'),
        columnType: ColumnTypeEnum.CUSTOM,
        type: FieldTypeEnum.SELECT,
        fieldWidth: 1,
        showLabel: true,
        options: processStatusOptions,
      },
    ],
    [FormDesignKeyEnum.CONTRACT]: [
      {
        id: 'stage',
        key: 'stage',
        title: t('contract.status'),
        name: t('contract.status'),
        columnType: ColumnTypeEnum.CUSTOM,
        type: FieldTypeEnum.SELECT,
        fieldWidth: 1,
        showLabel: true,
        options: contractStageOptions.value,
      },
      {
        id: 'approvalStatus',
        key: 'approvalStatus',
        title: t('contract.approvalStatus'),
        name: t('contract.approvalStatus'),
        columnType: ColumnTypeEnum.CUSTOM,
        type: FieldTypeEnum.SELECT,
        fieldWidth: 1,
        showLabel: true,
        options: processStatusOptions,
      },
    ],
    [FormDesignKeyEnum.ORDER]: [
      {
        id: 'stage',
        key: 'stage',
        title: t('order.status'),
        name: t('order.status'),
        columnType: ColumnTypeEnum.CUSTOM,
        type: FieldTypeEnum.SELECT,
        fieldWidth: 1,
        showLabel: true,
        options: orderStageOptions.value,
      },
      {
        id: 'approvalStatus',
        key: 'approvalStatus',
        title: t('common.approvalStatus'),
        name: t('common.approvalStatus'),
        columnType: ColumnTypeEnum.CUSTOM,
        type: FieldTypeEnum.SELECT,
        fieldWidth: 1,
        showLabel: true,
        options: processStatusOptions,
      },
    ],
    [FormDesignKeyEnum.INVOICE]: [
      {
        id: 'approvalStatus',
        key: 'approvalStatus',
        title: t('common.approvalStatus'),
        name: t('common.approvalStatus'),
        columnType: ColumnTypeEnum.CUSTOM,
        type: FieldTypeEnum.SELECT,
        fieldWidth: 1,
        showLabel: true,
        options: processStatusOptions,
      },
    ],
  }));
  const systemList = computed<DisplayFieldItem[]>(() => {
    return systemColumnMap.value[formKey.value as FormDesignKeyEnum] || [];
  });

  async function getDisplayList() {
    try {
      contractStageOptions.value = [];
      orderStageOptions.value = [];
      customList.value = [];
      subTableList.value = [];

      if (formKey.value === FormDesignKeyEnum.CONTRACT) {
        const contractStageConfig = await getContractStatusConfig();
        contractStageOptions.value = contractStageConfig.stageConfigList.map((item) => ({
          label: item.name,
          value: item.id,
        }));
      }

      if (formKey.value === FormDesignKeyEnum.ORDER) {
        const orderStageConfig = await getOrderStatusConfig();
        orderStageOptions.value = orderStageConfig.stageConfigList.map((item) => ({
          label: item.name,
          value: item.id,
        }));
      }

      let res;
      if (isCustomForm.value) {
        res = await getFieldDisplayList(dataSourceType.value as string);
      } else if (isBusinessTitleSource.value) {
        res = await getBusinessTitleModuleForm();
      } else {
        res = await getFieldDisplayList(formKey.value as FormDesignKeyEnum);
      }

      let fieldColumns: DisplayFieldItem[] = [];
      if (isBusinessTitleSource.value) {
        fieldColumns = res.fields.map((item) => ({
          ...item,
          key: item.id,
          title: t(item.name),
          columnType: ColumnTypeEnum.CUSTOM,
          name: t(item.name),
          type: FieldTypeEnum.INPUT,
          fieldWidth: 1,
          showLabel: true,
        }));
      } else {
        fieldColumns = res.fields.map((item) => ({
          key: item.id,
          title: item.name,
          columnType: item.subTableFieldId ? ColumnTypeEnum.SUB_TABLE : ColumnTypeEnum.CUSTOM,
          ...item,
        }));
      }

      customList.value = fieldColumns.filter((item) => item.columnType === ColumnTypeEnum.CUSTOM);
      subTableList.value = props.isSubTableField
        ? fieldColumns.filter((item) => item.columnType === ColumnTypeEnum.SUB_TABLE)
        : [];
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => show.value,
    async (val) => {
      if (val) {
        await getDisplayList();
      }
    },
    {
      immediate: true,
    }
  );

  const selectedList = ref<any[]>([]);

  const selectedSubTableIds = computed(() =>
    selectedList.value.filter((e) => e.columnType === ColumnTypeEnum.SUB_TABLE).map((e) => e.key)
  );

  const selectedCustomIds = computed(() =>
    selectedList.value.filter((e) => customList.value.some((item) => item.key === e.key)).map((e) => e.key)
  );
  const selectedSystemIds = computed(() =>
    selectedList.value.filter((e) => systemList.value.some((item) => item.key === e.key)).map((e) => e.key)
  );

  const updateSelectedList = (ids: string[], sourceList: any[]) => {
    const newItems = sourceList.filter((item) => ids.includes(item.key));
    const remainingItems = selectedList.value.filter((item) => !sourceList.some((src) => src.key === item.key));
    selectedList.value = [...remainingItems, ...newItems];
  };

  function selectItem(meta: { actionType: 'check' | 'uncheck'; value: string | number }, sourceList: any[]) {
    if (meta.actionType === 'check') {
      const itemToAdd = sourceList.find((i) => i.key === meta.value);
      if (itemToAdd) {
        selectedList.value.push(itemToAdd);
      }
    } else {
      // 移除取消选中的项
      selectedList.value = selectedList.value.filter((item) => item.key !== meta.value);
    }
  }

  function handleConfirm() {
    show.value = false;
    emit(
      'save',
      [...selectedCustomIds.value, ...selectedSystemIds.value, ...selectedSubTableIds.value],
      selectedList.value
    );
  }

  function handleCancel() {
    selectedList.value = props.fieldConfig?.showFields ? [...props.fieldConfig.showFields] : [];
    show.value = false;
  }

  watch(
    [() => props.fieldConfig?.showFields, () => customList.value, () => systemList.value, () => subTableList],
    () => {
      const allSelectedOptions = [...customList.value, ...systemList.value, ...subTableList.value];

      if (props.fieldConfig?.showFields && allSelectedOptions.length) {
        selectedList.value = allSelectedOptions.filter((item) => props.fieldConfig?.showFields?.includes(item.key));
      } else {
        selectedList.value = [];
      }
    }
  );
</script>
