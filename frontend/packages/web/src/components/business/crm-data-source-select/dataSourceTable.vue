<template>
  <div
    class="crm-data-source-table relative bg-[var(--text-n10)]"
    :style="{
      height: isFullScreen ? 'calc(100vh - 138px)' : '60vh',
    }"
  >
    <CrmTable
      ref="crmTableRef"
      v-model:checked-row-keys="selectedKeys"
      v-bind="propsRes"
      :fullscreen-target-ref="props.fullscreenTargetRef"
      :childrenKey="subFieldKey"
      :columns="columns"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
      @row-key-change="handleRowKeyChange"
      @refresh="searchData"
    >
      <template #tableTop>
        <CrmSearchInput
          v-model:value="keyword"
          class="crm-data-source-search-input !w-[240px]"
          :placeholder="
            props.sourceType === FieldDataSourceTypeEnum.CONTACT
              ? t('common.searchByNamePhone')
              : t('common.searchByName')
          "
          @search="searchData"
        />
      </template>
    </CrmTable>
  </div>
</template>

<script setup lang="ts">
  import { DataTableRowKey, NImage, NImageGroup } from 'naive-ui';

  import { PreviewPictureUrl } from '@lib/shared/api/requrls/system/module';
  import { FieldDataSourceTypeEnum, FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { transformData } from '@lib/shared/method/formCreate';

  import { FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';

  import useFormCreateApi from '@/hooks/useFormCreateApi';

  import type { FormCreateField } from '../crm-form-create/types';
  import { formKeyMap, sourceApi } from './config';
  import { InternalRowData, RowData } from 'naive-ui/es/data-table/src/interface';

  const props = withDefaults(
    defineProps<{
      sourceType: FieldDataSourceTypeEnum;
      multiple?: boolean;
      disabledSelection?: (row: RowData) => boolean;
      filterParams?: FilterResult;
      fullscreenTargetRef?: HTMLElement | null;
      fieldConfig?: FormCreateField;
    }>(),
    {
      multiple: true,
    }
  );

  const emit = defineEmits<{
    (e: 'toggleFullScreen', value: boolean): void;
  }>();

  const { t } = useI18n();

  const selectedKeys = defineModel<DataTableRowKey[]>('selectedKeys', {
    required: true,
  });
  const selectedRows = defineModel<InternalRowData[]>('selectedRows', {
    default: [],
  });

  const columns = ref<CrmDataTableColumn[]>([
    {
      type: 'selection',
      multiple: props.multiple,
      width: 46,
      disabled(row: RowData) {
        return props.disabledSelection ? props.disabledSelection(row) : false;
      },
      resizable: false,
      fixed: 'left',
    },
    {
      title: t('common.name'),
      key: 'name',
      ellipsis: {
        tooltip: true,
      },
      resizable: false,
      fixed: 'left',
    },
  ]);

  if (props.sourceType === FieldDataSourceTypeEnum.CONTACT) {
    columns.value.push(
      {
        title: t('crmFormDesign.phone'),
        key: 'phone',
        resizable: false,
      },
      {
        title: t('crmFormDesign.customer'),
        key: 'customerName',
        ellipsis: {
          tooltip: true,
        },
        resizable: true,
      }
    );
  }

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const { fieldList, initFormConfig } = useFormCreateApi({
    formKey: computed(() => formKeyMap[props.sourceType] as FormDesignKeyEnum),
  });
  const subField = computed(() =>
    fieldList.value.find((field) => [FieldTypeEnum.SUB_PRICE, FieldTypeEnum.SUB_PRODUCT].includes(field.type))
  );
  // 计算子表格字段的key
  const subFieldKey = computed(() => {
    if (
      formKeyMap[props.sourceType] === FormDesignKeyEnum.PRICE &&
      props.fieldConfig?.showFields?.some((sf) => subField.value?.subFields?.some((sub) => sub.id === sf))
    ) {
      const field = fieldList.value.find((e) => e.type === FieldTypeEnum.SUB_PRODUCT);
      return field?.businessKey || field?.id;
    }
    return undefined;
  });

  watch(
    () => subFieldKey.value,
    (val) => {
      if (val) {
        columns.value = [
          ...columns.value.map((col) => {
            if (col.type === 'selection') {
              col.multiple = true;
              return col;
            }
            if (col.key === 'name') {
              col.width = 250;
              return col;
            }
            return col;
          }),
          ...(subField.value?.subFields || []).map((field) => ({
            title: field.name,
            key: field.id,
            width: 120,
            ellipsis: {
              tooltip: true,
            },
            resizable: true,
            render:
              field.type === FieldTypeEnum.PICTURE
                ? (row: any) =>
                    h(
                      'div',
                      {
                        class: 'flex items-center',
                      },
                      [
                        h(
                          NImageGroup,
                          {},
                          {
                            default: () =>
                              row[field.businessKey || field.id]?.length
                                ? (row[field.businessKey || field.id] || []).map((_key: string) =>
                                    h(NImage, {
                                      class: 'h-[40px] w-[40px] mr-[4px]',
                                      src: `${PreviewPictureUrl}/${_key}`,
                                    })
                                  )
                                : '-',
                          }
                        ),
                      ]
                    )
                : undefined,
          })),
        ];
      }
    },
    { immediate: true }
  );

  const { propsRes, propsEvent, loadList, setAdvanceFilter, setLoadListParams } = useTable(
    sourceApi[props.sourceType],
    {
      columns: columns.value,
      showSetting: false,
      crmPagination: {
        showSizePicker: false,
      },
      containerClass: '.crm-data-source-table',
      childrenKey: subFieldKey.value,
      rowClassName: (row) => {
        if (subFieldKey.value && row[subFieldKey.value]?.length) {
          return 'crm-data-source-has-subfields';
        }
        return '';
      },
    },
    (item, originalData) => {
      return transformData({
        item,
        originalData,
        fields: fieldList.value,
        needParseSubTable: true,
      });
    }
  );

  const keyword = ref('');

  function searchData(_keyword?: string) {
    if (props.filterParams) {
      setAdvanceFilter(props.filterParams);
    }
    setLoadListParams({ keyword: _keyword !== undefined ? _keyword : keyword.value });
    loadList();
    crmTableRef.value?.scrollTo({ top: 0 });
  }

  function handleRowKeyChange(keys: DataTableRowKey[], _rows: InternalRowData[]) {
    selectedKeys.value = keys;
    selectedRows.value = _rows;
  }

  const isFullScreen = computed(() => crmTableRef.value?.isFullScreen);

  onBeforeMount(async () => {
    await initFormConfig();
    searchData();
  });

  watch(
    () => isFullScreen.value,
    (val) => {
      emit('toggleFullScreen', val ?? false);
    }
  );
</script>

<style lang="less">
  .crm-data-source-table {
    .n-checkbox--disabled .n-checkbox--checked {
      .check-icon {
        opacity: 1 !important;
        transform: scale(1) !important;
      }
    }
    .n-radio--disabled .n-checkbox--checked {
      .n-radio__dot::before {
        opacity: 1 !important;
        transform: scale(1) !important;
      }
    }
    .crm-data-source-has-subfields {
      .n-data-table-td--selection {
        .n-checkbox,
        .n-radio {
          @apply hidden;
        }
      }
    }
    .n-data-table-td {
      @apply whitespace-nowrap;
      .n-ellipsis {
        width: calc(100% - 24px);
      }
    }
  }
  .crm-data-source-search-input {
    --n-box-shadow-focus-error: 0 0 0 2px rgb(var(--primary-8) 0.2) !important;
    &.n-input {
      .n-input__border {
        &:focus {
          border: 1px solid var(--primary-8) !important;
        }
      }
      .n-input__state-border {
        border: 1px solid var(--primary-8) !important;
      }
      &:not(.n-input--disabled) .n-input__input-el {
        caret-color: var(--primary-8) !important;
      }
    }
  }
</style>
