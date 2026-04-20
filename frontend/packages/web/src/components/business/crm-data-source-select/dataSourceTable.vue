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
      :class="subFieldKey ? 'crm-datasource-table--hasSubField' : ''"
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
  import { DataTableRowKey, NImage, NImageGroup, NSwitch } from 'naive-ui';

  import { PreviewPictureUrl } from '@lib/shared/api/requrls/system/module';
  import { ContractPaymentPlanEnum, ContractStatusEnum } from '@lib/shared/enums/contractEnum';
  import { FieldDataSourceTypeEnum, FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { QuotationStatusEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { transformData } from '@lib/shared/method/formCreate';
  import type { ContractItem, PaymentPlanItem } from '@lib/shared/models/contract';
  import { CustomerContractListItem } from '@lib/shared/models/customer';
  import { OpportunityItem, OpportunityStageConfig, QuotationItem } from '@lib/shared/models/opportunity';
  import { OrderItem } from '@lib/shared/models/order';

  import { FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmApprovalStatus from '@/components/business/crm-approval-status/index.vue';
  import CrmBusinessNamePrefix from '@/components/business/crm-business-name-prefix/index.vue';
  import StatusTagSelect from '@/components/business/crm-follow-detail/statusTagSelect.vue';
  import ContractStatus from '@/views/contract/contractPaymentPlan/components/contractPaymentStatus.vue';

  import { getOpportunityStageConfig, getOrderStatusConfig } from '@/api/modules';
  import { contractPaymentPlanStatusOptions, contractStatusOptions } from '@/config/contract';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import useFormCreateSystemColumns from '@/hooks/useFormCreateSystemColumns';
  import { FormKey } from '@/hooks/useFormCreateTable';

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
      isSubTableRender?: boolean;
    }>(),
    {
      multiple: true,
    }
  );

  const emit = defineEmits<{
    (e: 'initForm', fields: FormCreateField[]): void;
    (e: 'toggleFullScreen', value: boolean): void;
  }>();

  const { t } = useI18n();

  const selectedKeys = defineModel<DataTableRowKey[]>('selectedKeys', {
    required: true,
  });
  const selectedRows = defineModel<InternalRowData[]>('selectedRows', {
    default: [],
  });

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const { fieldList, initFormConfig } = useFormCreateApi({
    formKey: computed(() => formKeyMap[props.sourceType] as FormDesignKeyEnum),
  });
  const subField = computed(() =>
    fieldList.value.find((field) => [FieldTypeEnum.SUB_PRICE, FieldTypeEnum.SUB_PRODUCT].includes(field.type))
  );
  // 计算子表格字段的key
  const subFieldKey = computed(() => {
    if (formKeyMap[props.sourceType] === FormDesignKeyEnum.PRICE && props.isSubTableRender) {
      const field = fieldList.value.find((e) => e.type === FieldTypeEnum.SUB_PRODUCT);
      return field?.businessKey || field?.id;
    }
    return undefined;
  });

  const dataSourceSpecialRenderMap: Record<FieldDataSourceTypeEnum, Record<string, (row: any) => any>> = {
    [FieldDataSourceTypeEnum.CUSTOMER]: {},
    [FieldDataSourceTypeEnum.CONTACT]: {
      status: (row: CustomerContractListItem) => {
        return h(NSwitch, {
          value: row.enable,
          disabled: true,
        });
      },
    },
    [FieldDataSourceTypeEnum.BUSINESS]: {
      stage: (row: OpportunityItem) => {
        return row.stageName || '-';
      },
    },
    [FieldDataSourceTypeEnum.PRODUCT]: {},
    [FieldDataSourceTypeEnum.CLUE]: {},
    [FieldDataSourceTypeEnum.PRICE]: {},
    [FieldDataSourceTypeEnum.QUOTATION]: {
      approvalStatus: (row: QuotationItem) =>
        h(CrmApprovalStatus, {
          status: row.approvalStatus,
        }),
      status: (row: QuotationItem) =>
        h(
          CrmTag,
          {
            type: row.status === QuotationStatusEnum.VOIDED ? 'default' : 'info',
            theme: 'light',
          },
          {
            default: () => (row.status === QuotationStatusEnum.VOIDED ? t('common.voided') : t('common.normal')),
          }
        ),
    },
    [FieldDataSourceTypeEnum.CONTRACT]: {
      stage: (row: ContractItem) => {
        return h(StatusTagSelect, {
          status: row.stage as ContractStatusEnum,
          noRender: true,
          disabled: true,
          statusOptions: contractStatusOptions,
        });
      },
      approvalStatus: (row: ContractItem) =>
        h(CrmApprovalStatus, {
          status: row.approvalStatus,
        }),
    },
    [FieldDataSourceTypeEnum.CONTRACT_PAYMENT]: {
      status: (row: PaymentPlanItem) =>
        h(StatusTagSelect, {
          'status': row.planStatus as ContractPaymentPlanEnum,
          'disabled': true,
          'statusTagComponent': ContractStatus,
          'onUpdate:status': (val) => {
            row.planStatus = val;
          },
          'statusOptions': contractPaymentPlanStatusOptions,
        }),
    },
    [FieldDataSourceTypeEnum.CONTRACT_PAYMENT_RECORD]: {
      status: (row: PaymentPlanItem) =>
        h(StatusTagSelect, {
          'status': row.planStatus as ContractPaymentPlanEnum,
          'disabled': true,
          'statusTagComponent': ContractStatus,
          'onUpdate:status': (val) => {
            row.planStatus = val;
          },
          'statusOptions': contractPaymentPlanStatusOptions,
        }),
    },
    [FieldDataSourceTypeEnum.ORDER]: {
      stage: (row: OrderItem) => {
        return row.stageName || '-';
      },
      approvalStatus: (row: ContractItem) =>
        h(CrmApprovalStatus, {
          status: row.approvalStatus,
        }),
    },
    [FieldDataSourceTypeEnum.CUSTOMER_OPTIONS]: {},
    [FieldDataSourceTypeEnum.USER_OPTIONS]: {},
    [FieldDataSourceTypeEnum.BUSINESS_TITLE]: {},
  };

  const stageConfig = ref<OpportunityStageConfig>();

  async function initStageConfig() {
    try {
      if (props.sourceType === FieldDataSourceTypeEnum.ORDER) {
        stageConfig.value = await getOrderStatusConfig();
      } else if (props.sourceType === FieldDataSourceTypeEnum.BUSINESS) {
        stageConfig.value = await getOpportunityStageConfig();
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const formKey = computed(() => formKeyMap[props.sourceType] as FormDesignKeyEnum);

  await initStageConfig();

  const { internalColumnMap, staticColumns, noSorterType } = await useFormCreateSystemColumns({
    formKey: formKey.value as FormKey,
    containerClass: '',
    specialRender: {
      ...dataSourceSpecialRenderMap[props.sourceType],
    },
    ...{
      [props.sourceType === FieldDataSourceTypeEnum.ORDER ? 'orderStage' : 'opportunityStage']:
        stageConfig.value?.stageConfigList || [],
    },
  });

  const defaultInternalNameKeyMap: Record<string, string> = {
    [FormDesignKeyEnum.CUSTOMER]: 'customerName',
    [FormDesignKeyEnum.CLUE]: 'clueName',
    [FormDesignKeyEnum.BUSINESS]: 'opportunityName',
    [FormDesignKeyEnum.CONTACT]: 'contactName',
    [FormDesignKeyEnum.PRODUCT]: 'productName',
    [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: 'quotationName',
    [FormDesignKeyEnum.ORDER]: 'orderName',
    [FormDesignKeyEnum.CONTRACT]: 'contractName',
    [FormDesignKeyEnum.CONTRACT_PAYMENT]: 'contractPaymentPlanName',
    [FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD]: 'contractPaymentRecordName',
    [FormDesignKeyEnum.PRICE]: 'priceName',
    [FormDesignKeyEnum.BUSINESS_TITLE]: 'name',
  };

  function mapColumnKey(columnKey: string): string {
    const keyMap: Record<string, string> = {
      customerId: 'customerName',
      owner: 'ownerName',
      stage: 'stageName',
      contactId: 'contactName',
      contractId: 'contractName',
      paymentPlanId: 'paymentPlanName',
      opportunityId: 'opportunityName',
    };
    return keyMap[columnKey] || columnKey;
  }

  function getFieldColumnKey(field: FormCreateField) {
    let key = field.businessKey || field.id;
    if (field.resourceFieldId) {
      key = field.id;
    }
    return mapColumnKey(key);
  }

  const defaultInternalKey = computed(() => {
    return defaultInternalNameKeyMap[formKeyMap[props.sourceType] as FormDesignKeyEnum];
  });

  const defaultDisplayField = computed<FormCreateField | undefined>(() => {
    return fieldList.value.find((field) => field.internalKey === defaultInternalKey.value);
  });

  const selectedDisplayFields = computed<string[]>(() => {
    const defaultFormColumn =
      formKey.value === FormDesignKeyEnum.BUSINESS_TITLE ? [] : internalColumnMap[formKey.value] || [];
    const fixedFieldIds = [...defaultFormColumn, ...staticColumns].map((column) => String(column.key));
    const allFields = [...fieldList.value.map((e) => e.id), ...fixedFieldIds];
    const savedFieldIds = props.fieldConfig?.listDisplayFields || [];

    const matchedFieldsIds = savedFieldIds
      .map((fieldId) => allFields.find((id) => id === fieldId))
      .filter((field): field is string => !!field);

    if (matchedFieldsIds.length === 0) {
      return defaultDisplayField.value ? [defaultDisplayField.value.id] : [];
    }

    if (!defaultDisplayField.value) {
      return matchedFieldsIds;
    }

    return [
      defaultDisplayField.value.id,
      ...matchedFieldsIds.filter((fieldId) => fieldId !== defaultDisplayField.value?.id),
    ];
  });

  function buildFieldColumn(field: FormCreateField): CrmDataTableColumn {
    const isTag = [
      FieldTypeEnum.DATA_SOURCE_MULTIPLE,
      FieldTypeEnum.MEMBER_MULTIPLE,
      FieldTypeEnum.DEPARTMENT_MULTIPLE,
      FieldTypeEnum.INPUT_MULTIPLE,
      FieldTypeEnum.SELECT_MULTIPLE,
      FieldTypeEnum.CHECKBOX,
    ].includes(field.type);

    const hasFilter = [
      FieldTypeEnum.RADIO,
      FieldTypeEnum.CHECKBOX,
      FieldTypeEnum.SELECT,
      FieldTypeEnum.SELECT_MULTIPLE,
    ];

    const columnKey = getFieldColumnKey(field);

    const baseColumn: CrmDataTableColumn = {
      title: field.name,
      key: columnKey,
      ellipsis: {
        tooltip: true,
      },
      resizable: true,
      width: field.internalKey === defaultInternalKey.value ? 220 : 150,
      isTag,
      fixed: defaultDisplayField.value?.id === field.id && selectedDisplayFields.value.length > 2 ? 'left' : undefined,
      sortOrder: false,
      filter: !field.resourceFieldId && hasFilter.includes(field.type),
      filterOptions: field.options || field.initialOptions?.map((e: any) => ({ label: e.name, value: e.id })),
      sorter: !noSorterType.includes(field.type) && !field.resourceFieldId,
      filedType: field.type,
      resourceFieldId: field.resourceFieldId,
    };
    if (field.type === FieldTypeEnum.PICTURE) {
      return {
        ...baseColumn,
        render: (row: any) =>
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
                    row[columnKey]?.length
                      ? (row[columnKey] || []).map((_key: string) =>
                          h(NImage, {
                            class: 'h-[40px] w-[40px] mr-[4px]',
                            src: `${PreviewPictureUrl}/${_key}`,
                          })
                        )
                      : '-',
                }
              ),
            ]
          ),
      };
    }

    if (props.sourceType === FieldDataSourceTypeEnum.BUSINESS_TITLE && field.businessKey === 'name') {
      return {
        ...baseColumn,
        render: (row: RowData) =>
          h(
            CrmNameTooltip,
            { text: row[columnKey] as string },
            {
              prefix: () => h(CrmBusinessNamePrefix, { type: row.type }),
            }
          ),
      };
    }

    return baseColumn;
  }

  function buildSubFieldColumn(field: FormCreateField): CrmDataTableColumn {
    const columnKey = field.businessKey || field.id;
    return {
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
                        row[columnKey]?.length
                          ? (Array.isArray(row[columnKey]) ? row[columnKey] : []).map((_key: string) =>
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
    };
  }

  const visibleColumns = computed<CrmDataTableColumn[]>(() => {
    const allFields = fieldList.value || [];
    const savedFieldKeys = props.fieldConfig?.listDisplayFields || [];
    const resolvedColumns: CrmDataTableColumn[] = [];
    const appendedKeys = new Set<string>();
    const defaultFieldId = defaultDisplayField.value?.id;
    const defaultColumn = defaultDisplayField.value ? buildFieldColumn(defaultDisplayField.value) : undefined;

    if (defaultColumn && defaultFieldId) {
      resolvedColumns.push(defaultColumn);
      appendedKeys.add(defaultFieldId);
    }

    const allSystemColumns = [...(internalColumnMap[formKey.value] || []), ...staticColumns];
    const systemColumnMap = new Map(allSystemColumns.map((column) => [String(column.key), column] as const));

    savedFieldKeys.forEach((fieldKey) => {
      const matchedField = allFields.find((field) => field.id === fieldKey);
      if (matchedField) {
        if (!appendedKeys.has(matchedField.id)) {
          resolvedColumns.push(buildFieldColumn(matchedField));
          appendedKeys.add(matchedField.id);
        }
        return;
      }

      const matchedSystemColumn = systemColumnMap.get(String(fieldKey));
      if (matchedSystemColumn && !appendedKeys.has(String(matchedSystemColumn.key))) {
        resolvedColumns.push(matchedSystemColumn);
        appendedKeys.add(String(matchedSystemColumn.key));
      }
    });

    if (resolvedColumns.length === 0 && defaultColumn) {
      return [defaultColumn];
    }

    return resolvedColumns;
  });

  const columns = computed<CrmDataTableColumn[]>(() => {
    const selectionColumn: CrmDataTableColumn = {
      type: 'selection',
      multiple: subFieldKey.value ? true : props.multiple,
      width: 46,
      disabled(row: RowData) {
        if (subFieldKey.value) {
          return (!row[subFieldKey.value] || row[subFieldKey.value]?.length === 0) && !row.parentId;
        }
        return props.disabledSelection ? props.disabledSelection(row) : false;
      },
      resizable: false,
      fixed: 'left',
    };

    const subColumns = subFieldKey.value
      ? (subField.value?.subFields || []).map((field) => buildSubFieldColumn(field))
      : [];

    return [selectionColumn, ...visibleColumns.value, ...subColumns];
  });

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
      cascade: false,
      rowClassName: (row) => {
        if (subFieldKey.value && row[subFieldKey.value]?.length) {
          return 'crm-data-source-has-subfields';
        }
        return '';
      },
    },
    (item, originalData) => {
      const transformItem = transformData({
        item,
        originalData,
        fields: fieldList.value,
        needParseSubTable: true,
      });
      if (subFieldKey.value) {
        transformItem[subFieldKey.value] = transformItem[subFieldKey.value]?.map((subItem: any) => ({
          ...subItem,
          parentId: item.id,
          parentName: item.name,
        }));
      }
      return transformItem;
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
    if (subFieldKey.value) {
      const parentIds = _rows.map((r) => r.parentId as DataTableRowKey);
      selectedKeys.value = Array.from(new Set(keys.concat(parentIds)));
      const parentRows =
        propsRes.value.data.filter(
          (r) => parentIds.includes(r.id as DataTableRowKey) && !_rows.some((row) => row.id === r.id)
        ) || [];
      selectedRows.value = _rows.concat(parentRows);
    } else {
      selectedKeys.value = keys;
      selectedRows.value = _rows;
    }
  }

  const isFullScreen = computed(() => crmTableRef.value?.isFullScreen);

  onBeforeMount(async () => {
    await initFormConfig();
    emit('initForm', fieldList.value);
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
  .crm-datasource-table--hasSubField {
    .n-data-table-thead {
      .n-checkbox {
        @apply hidden;
      }
    }
  }
</style>
