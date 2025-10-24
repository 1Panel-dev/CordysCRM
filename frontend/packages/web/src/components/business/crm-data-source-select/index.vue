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
    class="crm-data-source-select-modal"
    @confirm="handleDataSourceConfirm"
    @cancel="handleDataSourceCancel"
  >
    <dataSourceTable
      v-if="dataSourcesModalVisible"
      v-model:selected-keys="selectedKeys"
      v-model:selected-rows="selectedRows"
      :multiple="props.multiple"
      :source-type="props.dataSourceType"
      :disabled-selection="tableDisabledSelection"
      :filter-params="filterParams"
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

  import { InternalRowData, RowData, RowKey } from 'naive-ui/es/data-table/src/interface';

  interface DataSourceTableProps {
    dataSourceType: FieldDataSourceTypeEnum;
    multiple?: boolean;
    disabled?: boolean;
    disabledSelection?: (row: RowData) => boolean;
    maxTagCount?: number | 'responsive';
    filterParams?: FilterResult;
  }

  const props = withDefaults(defineProps<DataSourceTableProps>(), {
    multiple: true,
  });
  const emit = defineEmits<{
    (e: 'change', value: (string | number)[]): void;
  }>();

  const { t } = useI18n();

  const typeLocaleMap = {
    [FieldDataSourceTypeEnum.CUSTOMER]: 'crmFormDesign.customer',
    [FieldDataSourceTypeEnum.CONTACT]: 'crmFormDesign.contract',
    [FieldDataSourceTypeEnum.BUSINESS]: 'crmFormDesign.business',
    [FieldDataSourceTypeEnum.PRODUCT]: 'crmFormDesign.product',
    [FieldDataSourceTypeEnum.CLUE]: 'crmFormDesign.clue',
    [FieldDataSourceTypeEnum.CUSTOMER_OPTIONS]: 'crmFormDesign.customer',
    [FieldDataSourceTypeEnum.USER_OPTIONS]: '',
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

  function handleDataSourceConfirm() {
    const newRows = selectedRows.value;
    rows.value = newRows;
    value.value = newRows.map((e) => e.id) as RowKey[];
    dataSourcesModalVisible.value = false;
    emit('change', value.value);
  }

  function handleDataSourceCancel() {
    selectedKeys.value = [];
    dataSourcesModalVisible.value = false;
  }

  const renderTag = ({ option, handleClose }: { option: SelectOption; handleClose: () => void }) => {
    return h(
      CrmTag,
      {
        type: 'default',
        theme: 'light',
        closable: !props.disabled,
        onClose: () => {
          handleClose();
          rows.value = rows.value.filter((item) => item.id !== option.value);
          value.value = value.value.filter((key) => key !== option.value);
        },
      },
      {
        default: () => {
          return (rows.value || []).find((item) => item?.id === option.value)?.name;
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

  function tableDisabledSelection(row: RowData) {
    if (props.disabledSelection) {
      return props.disabledSelection(row);
    }
    return false;
  }
</script>

<style lang="less">
  .crm-data-source-select-modal {
    .n-dialog__title {
      @apply justify-between;
    }
  }
</style>
