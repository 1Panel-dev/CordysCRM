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
    @click="showDataSourcesModal"
    @update-value="($event) => emit('change', $event)"
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
    />
  </CrmModal>
</template>

<script setup lang="ts">
  import { DataTableRowKey, NSelect, SelectOption } from 'naive-ui';

  import { FieldDataSourceTypeEnum } from '@lib/shared/enums/formDesignEnum';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import dataSourceTable from './dataSourceTable.vue';

  import { useI18n } from '@/hooks/useI18n';

  import { InternalRowData } from 'naive-ui/es/data-table/src/interface';

  const { t } = useI18n();

  const props = withDefaults(
    defineProps<{
      dataSourceType?: FieldDataSourceTypeEnum;
      multiple?: boolean;
      disabled?: boolean;
    }>(),
    {
      multiple: true,
    }
  );
  const emit = defineEmits<{
    (e: 'change', value: (string | number)[]): void;
  }>();

  const typeLocaleMap = {
    [FieldDataSourceTypeEnum.CUSTOMER]: 'crmFormDesign.customer',
    [FieldDataSourceTypeEnum.CONTACT]: 'crmFormDesign.contract',
    [FieldDataSourceTypeEnum.BUSINESS]: 'crmFormDesign.business',
    [FieldDataSourceTypeEnum.PRODUCT]: 'crmFormDesign.product',
    [FieldDataSourceTypeEnum.CLUE]: 'crmFormDesign.clue',
  };

  const value = defineModel<DataTableRowKey[]>('value', {
    required: true,
  });
  const rows = defineModel<InternalRowData[]>('rows', {
    default: [],
  });

  const selectedRows = ref<InternalRowData[]>([]);
  const selectedKeys = ref<DataTableRowKey[]>([]);

  const dataSourcesModalVisible = ref(false);

  function handleDataSourceConfirm() {
    value.value = selectedKeys.value;
    rows.value = selectedRows.value;
    dataSourcesModalVisible.value = false;
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
        closable: true,
        onClose: () => {
          handleClose();
          rows.value = rows.value.filter((item) => item.id !== option.value);
          value.value = value.value.filter((key) => key !== option.value);
        },
      },
      {
        default: () => rows.value.find((item) => item.id === option.value)?.name,
      }
    );
  };

  function showDataSourcesModal() {
    dataSourcesModalVisible.value = true;
    selectedKeys.value = value.value;
  }
</script>

<style lang="less">
  .crm-data-source-select-modal {
    .n-dialog__title {
      @apply justify-between;
    }
  }
</style>
