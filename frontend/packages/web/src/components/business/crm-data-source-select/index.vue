<template>
  <n-select
    v-model:value="value"
    filterable
    multiple
    tag
    :placeholder="t('common.pleaseSelect')"
    :render-tag="renderTag"
    :show-arrow="false"
    :show="false"
    :disabled="props.disabled"
    :max-tag-count="props.maxTagCount"
    @click="showDataSourcesModal"
  />
  <CrmModal
    v-model:show="dataSourcesModalVisible"
    :title="
      t('crmFormDesign.selectDataSource', { type: props.dataSourceType ? t(typeLocaleMap[props.dataSourceType]) : '' })
    "
    :positive-text="t('common.confirm')"
    :class="`crm-data-source-select-modal ${fullScreenModal ? 'crm-full-modal' : ''}`"
    @confirm="handleDataSourceConfirm"
    @cancel="handleDataSourceCancel"
  >
    <dataSourceTable
      v-model:selected-keys="selectedKeys"
      v-model:selected-rows="selectedRows"
      :multiple="props.multiple"
      :source-type="props.dataSourceType"
      :disabled-selection="props.disabledSelection ? props.disabledSelection : undefined"
      :filter-params="filterParams"
      :fullscreen-target-ref="fullscreenTargetRef"
      :fieldConfig="props.fieldConfig"
      :isSubTableRender="props.hideChildTag"
      @init-form="handleFormInit"
      @toggle-full-screen="(val) => (fullScreenModal = val)"
    />
  </CrmModal>
</template>

<script setup lang="ts">
  import { DataTableRowKey, NSelect, SelectOption } from 'naive-ui';

  import { FieldDataSourceTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import dataSourceTable from './dataSourceTable.vue';

  import type { FormCreateField } from '../crm-form-create/types';
  import { InternalRowData, RowData, RowKey } from 'naive-ui/es/data-table/src/interface';

  interface DataSourceTableProps {
    dataSourceType: FieldDataSourceTypeEnum;
    multiple?: boolean;
    disabled?: boolean;
    disabledSelection?: (row: RowData) => boolean;
    maxTagCount?: number | 'responsive';
    filterParams?: FilterResult;
    fieldConfig?: FormCreateField;
    hideChildTag?: boolean;
  }

  const props = withDefaults(defineProps<DataSourceTableProps>(), {
    multiple: true,
  });
  const emit = defineEmits<{
    (
      e: 'change',
      value: (string | number)[],
      source: Record<string, any>[],
      dataSourceFormFields: FormCreateField[]
    ): void;
  }>();

  const { t } = useI18n();

  const typeLocaleMap = {
    [FieldDataSourceTypeEnum.CUSTOMER]: 'crmFormDesign.customer',
    [FieldDataSourceTypeEnum.CONTACT]: 'crmFormDesign.contract',
    [FieldDataSourceTypeEnum.BUSINESS]: 'crmFormDesign.opportunity',
    [FieldDataSourceTypeEnum.PRODUCT]: 'crmFormDesign.product',
    [FieldDataSourceTypeEnum.CLUE]: 'crmFormDesign.clue',
    [FieldDataSourceTypeEnum.CUSTOMER_OPTIONS]: 'crmFormDesign.customer',
    [FieldDataSourceTypeEnum.USER_OPTIONS]: '',
    [FieldDataSourceTypeEnum.PRICE]: 'crmFormCreate.drawer.price',
    [FieldDataSourceTypeEnum.CONTRACT]: 'crmFormCreate.drawer.contract',
    [FieldDataSourceTypeEnum.QUOTATION]: 'crmFormCreate.drawer.quotation',
    [FieldDataSourceTypeEnum.CONTRACT_PAYMENT]: 'crmFormCreate.drawer.contractPaymentPlan',
    [FieldDataSourceTypeEnum.BUSINESS_TITLE]: 'crmFormCreate.drawer.businessTitle',
  };

  const value = defineModel<DataTableRowKey[]>('value', {
    required: true,
    default: [],
  });
  const rows = defineModel<InternalRowData[]>('rows', {
    default: [],
  });

  const selectedRows = ref<InternalRowData[]>(rows.value);
  const selectedKeys = ref<DataTableRowKey[]>(value.value);

  const dataSourcesModalVisible = ref(false);
  const dataSourceFormFields = ref<FormCreateField[]>([]);

  function handleFormInit(fields: FormCreateField[]) {
    dataSourceFormFields.value = fields;
  }

  function handleDataSourceConfirm() {
    const newRows = selectedRows.value;
    rows.value = newRows;
    value.value = newRows.map((e) => e.id) as RowKey[];
    nextTick(() => {
      emit('change', value.value, newRows, dataSourceFormFields.value);
    });
    dataSourcesModalVisible.value = false;
  }

  function handleDataSourceCancel() {
    selectedKeys.value = [];
    dataSourcesModalVisible.value = false;
  }

  const renderTag = ({ option, handleClose }: { option: SelectOption; handleClose: () => void }) => {
    const _row = (rows.value || []).find((item) => item?.id === option.value);
    return props.hideChildTag && _row?.parentId
      ? null
      : h(
          CrmTag,
          {
            type: 'default',
            theme: 'light',
            closable: !props.disabled,
            onClose: () => {
              handleClose();
              rows.value = rows.value.filter((item) => item.id !== option.value);
              value.value = value.value.filter((key) => key !== option.value);
              nextTick(() => {
                emit('change', value.value, rows.value, dataSourceFormFields.value);
              });
            },
          },
          {
            default: () => {
              return _row?.name || t('common.optionNotExist');
            },
          }
        );
  };

  function showDataSourcesModal() {
    if (!props.disabled) {
      selectedKeys.value = value.value;
      dataSourcesModalVisible.value = true;
    }
  }

  watch(
    () => value.value,
    () => {
      selectedKeys.value = value.value;
      selectedRows.value = rows.value.filter((item) => value.value.includes(item.id as DataTableRowKey));
    },
    { immediate: true }
  );

  const fullscreenTargetRef = ref();
  const fullScreenModal = ref(false);

  function setFullWrapperFullScreenRef() {
    nextTick(() => {
      const wrapper = document.querySelector('.n-modal-body-wrapper .crm-data-source-select-modal');
      fullscreenTargetRef.value = wrapper;
    });
  }

  watch(
    () => dataSourcesModalVisible.value,
    (v) => {
      if (v) {
        setFullWrapperFullScreenRef();
      } else {
        fullScreenModal.value = false;
      }
    }
  );

  watch(
    () => fullScreenModal.value,
    () => {
      setFullWrapperFullScreenRef();
    }
  );
</script>

<style lang="less">
  .crm-data-source-select-modal {
    .n-dialog__title {
      @apply justify-between;
    }
  }
  .crm-full-modal {
    max-width: 100% !important;
    .n-dialog__content {
      height: calc(100vh - 136px);
      .n-scrollbar {
        height: 100% !important;
        max-height: none !important;
        .n-scrollbar-content {
          height: 100%;
        }
      }
    }
  }
</style>
