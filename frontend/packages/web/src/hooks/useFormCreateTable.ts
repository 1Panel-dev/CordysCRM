import { NImage, NImageGroup } from 'naive-ui';

import { PreviewPictureUrl } from '@lib/shared/api/requrls/system/module';
import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { formatNumberValueToString, getFieldItemId, transformData } from '@lib/shared/method/formCreate';
import type { StageConfigItem } from '@lib/shared/models/opportunity';

import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
import useTable from '@/components/pure/crm-table/useTable';
import {
  getFormConfigApiMap,
  getFormListApiMap,
  multipleValueTypeList,
} from '@/components/business/crm-form-create/config';
import type { FormCreateField } from '@/components/business/crm-form-create/types';

import useFormCreateAdvanceFilter from '@/hooks/useFormCreateAdvanceFilter';

import useFormCreateSystemColumns from './useFormCreateSystemColumns';

export type FormKey =
  | FormDesignKeyEnum.CUSTOMER
  | FormDesignKeyEnum.CONTACT
  | FormDesignKeyEnum.BUSINESS
  | FormDesignKeyEnum.CLUE
  | FormDesignKeyEnum.PRODUCT
  | FormDesignKeyEnum.CUSTOMER_OPEN_SEA
  | FormDesignKeyEnum.CLUE_POOL
  | FormDesignKeyEnum.CUSTOMER_CONTACT
  | FormDesignKeyEnum.BUSINESS_CONTACT
  | FormDesignKeyEnum.CUSTOMER_OPPORTUNITY
  | FormDesignKeyEnum.CLUE_TRANSITION_CUSTOMER
  | FormDesignKeyEnum.FOLLOW_RECORD
  | FormDesignKeyEnum.FOLLOW_PLAN
  | FormDesignKeyEnum.SEARCH_ADVANCED_CLUE
  | FormDesignKeyEnum.SEARCH_ADVANCED_CUSTOMER
  | FormDesignKeyEnum.SEARCH_ADVANCED_CONTACT
  | FormDesignKeyEnum.SEARCH_ADVANCED_PUBLIC
  | FormDesignKeyEnum.SEARCH_ADVANCED_CLUE_POOL
  | FormDesignKeyEnum.SEARCH_ADVANCED_OPPORTUNITY
  | FormDesignKeyEnum.OPPORTUNITY_QUOTATION
  | FormDesignKeyEnum.CONTRACT
  | FormDesignKeyEnum.CONTRACT_PAYMENT
  | FormDesignKeyEnum.CONTRACT_CONTRACT_PAYMENT
  | FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD
  | FormDesignKeyEnum.PRICE
  | FormDesignKeyEnum.INVOICE
  | FormDesignKeyEnum.CONTRACT_INVOICE
  | FormDesignKeyEnum.ORDER
  | FormDesignKeyEnum.CONTRACT_ORDER
  | FormDesignKeyEnum.CUSTOMER_ORDER;

export interface FormCreateTableProps {
  formKey: FormKey;
  containerClass: string; // 容器元素类名
  disabledSelection?: (row: any) => boolean;
  operationColumn?: CrmDataTableColumn;
  specialRender?: Record<string, (row: any) => void>;
  showPagination?: boolean;
  excludeFieldIds?: string[]; // 规避某些字段的文字替换
  permission?: string[]; // 表格勾选的权限
  readonly?: boolean;
  radio?: boolean; // 是否单选
  hiddenTotal?: Ref<boolean>;
  opportunityStage?: StageConfigItem[]; // 商机阶段筛选项
  orderStage?: StageConfigItem[];
  hiddenAllScreen?: boolean;
  hiddenRefresh?: boolean;
}

export default async function useFormCreateTable(props: FormCreateTableProps) {
  const { t } = useI18n();
  const { getFilterListConfig, customFieldsFilterConfig } = useFormCreateAdvanceFilter();
  const { internalColumnMap, staticColumns, reasonOptions, dicApprovalEnable, noSorterType } =
    await useFormCreateSystemColumns(props);
  const loading = ref(false);
  const showPagination = props.showPagination ?? true;
  let columns: CrmDataTableColumn[] = [];
  const fieldList = ref<FormCreateField[]>([]);
  const tableKeyMap = {
    [FormDesignKeyEnum.CUSTOMER]: TableKeyEnum.CUSTOMER,
    [FormDesignKeyEnum.CONTACT]: TableKeyEnum.CUSTOMER_CONTRACT,
    [FormDesignKeyEnum.CUSTOMER_CONTACT]: TableKeyEnum.CUSTOMER_CONTRACT,
    [FormDesignKeyEnum.BUSINESS_CONTACT]: TableKeyEnum.BUSINESS_CONTRACT,
    [FormDesignKeyEnum.BUSINESS]: TableKeyEnum.BUSINESS,
    [FormDesignKeyEnum.CLUE]: TableKeyEnum.CLUE,
    [FormDesignKeyEnum.CLUE_POOL]: TableKeyEnum.CLUE_POOL,
    [FormDesignKeyEnum.PRODUCT]: TableKeyEnum.PRODUCT,
    [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: TableKeyEnum.CUSTOMER_OPEN_SEA,
    [FormDesignKeyEnum.CUSTOMER_OPPORTUNITY]: TableKeyEnum.BUSINESS,
    [FormDesignKeyEnum.CLUE_TRANSITION_CUSTOMER]: undefined,
    [FormDesignKeyEnum.FOLLOW_PLAN]: TableKeyEnum.FOLLOW_PLAN,
    [FormDesignKeyEnum.FOLLOW_RECORD]: TableKeyEnum.FOLLOW_RECORD,
    [FormDesignKeyEnum.SEARCH_ADVANCED_CLUE]: TableKeyEnum.SEARCH_ADVANCED_CLUE,
    [FormDesignKeyEnum.SEARCH_ADVANCED_CUSTOMER]: TableKeyEnum.SEARCH_ADVANCED_CUSTOMER,
    [FormDesignKeyEnum.SEARCH_ADVANCED_CONTACT]: TableKeyEnum.SEARCH_ADVANCED_CONTACT,
    [FormDesignKeyEnum.SEARCH_ADVANCED_PUBLIC]: TableKeyEnum.SEARCH_ADVANCED_PUBLIC,
    [FormDesignKeyEnum.SEARCH_ADVANCED_CLUE_POOL]: TableKeyEnum.SEARCH_ADVANCED_CLUE_POOL,
    [FormDesignKeyEnum.SEARCH_ADVANCED_OPPORTUNITY]: TableKeyEnum.SEARCH_ADVANCED_OPPORTUNITY,
    [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: TableKeyEnum.OPPORTUNITY_QUOTATION,
    [FormDesignKeyEnum.CONTRACT]: TableKeyEnum.CONTRACT,
    [FormDesignKeyEnum.CONTRACT_PAYMENT]: TableKeyEnum.CONTRACT_PAYMENT,
    [FormDesignKeyEnum.CONTRACT_CONTRACT_PAYMENT]: TableKeyEnum.CONTRACT_PAYMENT,
    [FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD]: TableKeyEnum.CONTRACT_PAYMENT_RECORD,
    [FormDesignKeyEnum.PRICE]: TableKeyEnum.PRICE,
    [FormDesignKeyEnum.INVOICE]: TableKeyEnum.INVOICE,
    [FormDesignKeyEnum.CONTRACT_INVOICE]: TableKeyEnum.CONTRACT_INVOICE,
    [FormDesignKeyEnum.ORDER]: TableKeyEnum.ORDER,
    [FormDesignKeyEnum.CONTRACT_ORDER]: TableKeyEnum.CONTRACT_ORDER,
    [FormDesignKeyEnum.CUSTOMER_ORDER]: TableKeyEnum.ORDER,
  };
  const noPaginationKey = [FormDesignKeyEnum.CUSTOMER_CONTACT];
  // 存储地址类型字段集合
  const addressFieldIds = ref<string[]>([]);
  // 存储行业类型字段集合
  const industryFieldIds = ref<string[]>([]);
  // 业务字段集合
  const businessFieldIds = ref<string[]>([]);
  // 数据源字段集合
  const dataSourceFieldIds = ref<string[]>([]);

  const remoteFilterBusinessKey = ['products'];

  function disableFilterAndSorter(cols: CrmDataTableColumn[]) {
    return cols.map((c) => ({
      ...c,
      filter: false,
      sorter: false,
    })) as CrmDataTableColumn[];
  }

  function getFollowColumn(fields: FormCreateField[]): CrmDataTableColumn[] {
    if (props.formKey === FormDesignKeyEnum.FOLLOW_PLAN || props.formKey === FormDesignKeyEnum.FOLLOW_RECORD) {
      const customerField = fields.find((item) => item.businessKey === 'customerId');
      const clueField = fields.find((item) => item.businessKey === 'clueId');

      const baseColumns: CrmDataTableColumn[] = [
        {
          title: `${customerField?.name}/${clueField?.name}`,
          width: 200,
          key: 'name',
          render: props.specialRender?.name,
          fixed: 'left',
          fieldId: (customerField ?? clueField)?.id,
          filedType: (customerField ?? clueField)?.type,
          columnSelectorDisabled: true,
        },
      ];

      // FOLLOW_PLAN 才有状态列
      if (props.formKey === FormDesignKeyEnum.FOLLOW_PLAN) {
        baseColumns.push({
          title: t('common.status'),
          width: 120,
          key: 'status',
          render: props.specialRender?.status,
        });
      }

      return baseColumns;
    }

    return [];
  }

  async function initFormConfig() {
    try {
      const sorter = noPaginationKey.includes(props.formKey) ? 'default' : true;
      loading.value = true;
      const res = await getFormConfigApiMap[props.formKey]();
      fieldList.value = res.fields;

      const isFollowModule = [FormDesignKeyEnum.FOLLOW_PLAN, FormDesignKeyEnum.FOLLOW_RECORD].includes(props.formKey);
      columns = res.fields
        .filter(
          (e) =>
            e.type !== FieldTypeEnum.DIVIDER &&
            e.type !== FieldTypeEnum.TEXTAREA &&
            e.type !== FieldTypeEnum.ATTACHMENT &&
            e.type !== FieldTypeEnum.SUB_PRICE &&
            e.type !== FieldTypeEnum.SUB_PRODUCT &&
            !(
              e.businessKey === 'owner' &&
              [FormDesignKeyEnum.CLUE_POOL, FormDesignKeyEnum.CUSTOMER_OPEN_SEA].includes(props.formKey)
            ) &&
            e.readable &&
            !(isFollowModule && ['clueId', 'customerId'].includes(e.businessKey as string))
        )
        .map((field) => {
          let key = field.businessKey || field.id;
          if (field.resourceFieldId) {
            // 数据源引用字段用 id作为 key
            key = getFieldItemId(field);
          }
          if (field.type === FieldTypeEnum.PICTURE) {
            return {
              title: field.name,
              width: 200,
              key,
              fieldId: field.id,
              filedType: FieldTypeEnum.PICTURE,
              resourceFieldId: field.resourceFieldId,
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
                          row[key]?.length
                            ? (row[key] || []).map((_key: string) =>
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
          if (field.type === FieldTypeEnum.LOCATION) {
            addressFieldIds.value.push(field.businessKey || field.id);
          } else if (field.type === FieldTypeEnum.INDUSTRY) {
            industryFieldIds.value.push(field.businessKey || field.id);
          } else if (
            [FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(field.type) &&
            !props.excludeFieldIds?.includes(field.businessKey || field.id)
          ) {
            dataSourceFieldIds.value.push(field.businessKey || field.id);
          }
          if (field.businessKey && !props.excludeFieldIds?.includes(field.businessKey)) {
            businessFieldIds.value.push(field.businessKey);
          }
          if (
            [FieldTypeEnum.RADIO, FieldTypeEnum.CHECKBOX, FieldTypeEnum.SELECT, FieldTypeEnum.SELECT_MULTIPLE].includes(
              field.type
            )
          ) {
            // 带筛选的列
            return {
              title: field.name,
              width: 150,
              key,
              fieldId: field.id,
              ellipsis: ![FieldTypeEnum.CHECKBOX, FieldTypeEnum.SELECT_MULTIPLE].includes(field.type)
                ? {
                    tooltip: true,
                  }
                : undefined,
              isTag: field.type === FieldTypeEnum.CHECKBOX || field.type === FieldTypeEnum.SELECT_MULTIPLE,
              filterOptions: field.options || field.initialOptions?.map((e: any) => ({ label: e.name, value: e.id })),
              filter: !field.resourceFieldId,
              sortOrder: false,
              sorter: !noSorterType.includes(field.type) && !field.resourceFieldId,
              filterMultipleValue: multipleValueTypeList.includes(field.type),
              filedType: field.type,
              resourceFieldId: field.resourceFieldId,
            };
          }
          if (
            field.businessKey === 'name' &&
            ![FormDesignKeyEnum.CUSTOMER_CONTACT, FormDesignKeyEnum.BUSINESS_CONTACT].includes(props.formKey) &&
            !field.resourceFieldId
          ) {
            return {
              title: field.name,
              width: 200,
              key,
              fieldId: field.id,
              sortOrder: false,
              sorter: sorter && !field.resourceFieldId,
              fixed: 'left',
              columnSelectorDisabled: true,
              filedType: field.type,
              render: props.specialRender?.[field.businessKey],
              resourceFieldId: field.resourceFieldId,
            };
          }
          if (field.businessKey === 'businessTitleId' && !field.resourceFieldId) {
            return {
              title: field.name,
              width: 200,
              key,
              fieldId: field.id,
              sortOrder: false,
              sorter: sorter && !field.resourceFieldId,
              filedType: field.type,
              render: props.specialRender?.[field.businessKey],
              resourceFieldId: field.resourceFieldId,
            };
          }
          if (
            !field.resourceFieldId &&
            (field.businessKey === 'customerId' ||
              ([
                FormDesignKeyEnum.CONTRACT_PAYMENT,
                FormDesignKeyEnum.CONTRACT_CONTRACT_PAYMENT,
                FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD,
                FormDesignKeyEnum.INVOICE,
                FormDesignKeyEnum.CONTRACT_INVOICE,
                FormDesignKeyEnum.ORDER,
                FormDesignKeyEnum.CUSTOMER_ORDER,
                FormDesignKeyEnum.CONTRACT_ORDER,
              ].includes(props.formKey) &&
                field.businessKey === 'contractId') ||
              field.businessKey === 'paymentPlanId')
          ) {
            return {
              title: field.name,
              width: 200,
              key,
              fieldId: field.id,
              sortOrder: false,
              sorter: sorter && !field.resourceFieldId,
              filedType: field.type,
              render: props.specialRender?.[field.businessKey],
              resourceFieldId: field.resourceFieldId,
            };
          }

          if (isFollowModule && field.businessKey === 'opportunityId') {
            return {
              title: field.name,
              width: 200,
              key,
              fieldId: field.id,
              sortOrder: false,
              sorter: sorter && !field.resourceFieldId,
              filedType: field.type,
              ellipsis: {
                tooltip: true,
              },
              render: (row: any) => row.opportunityName ?? '-',
              resourceFieldId: field.resourceFieldId,
            };
          }

          if (
            [FormDesignKeyEnum.OPPORTUNITY_QUOTATION].includes(props.formKey) &&
            field.businessKey === 'opportunityId'
          ) {
            return {
              title: field.name,
              width: 200,
              key,
              fieldId: field.id,
              sortOrder: false,
              sorter: sorter && !field.resourceFieldId,
              filedType: field.type,
              ellipsis: {
                tooltip: true,
              },
              render: props.specialRender?.[field.businessKey],
              resourceFieldId: field.resourceFieldId,
            };
          }

          if (field.businessKey === 'owner') {
            return {
              title: field.name,
              width: 200,
              key,
              fieldId: field.id,
              sortOrder: false,
              sorter: sorter && !field.resourceFieldId,
              ellipsis: {
                tooltip: true,
              },
              filedType: field.type,
              render: (row: any) => row.ownerName || '-',
              resourceFieldId: field.resourceFieldId,
            };
          }

          if (field.businessKey === 'contactId') {
            return {
              title: field.name,
              width: 200,
              key,
              fieldId: field.id,
              sortOrder: false,
              sorter: sorter && !field.resourceFieldId,
              ellipsis: {
                tooltip: true,
              },
              filedType: field.type,
              render: (row: any) => row.contactName || '-',
              resourceFieldId: field.resourceFieldId,
            };
          }

          if (
            field.businessKey &&
            remoteFilterBusinessKey.includes(field.businessKey) &&
            props.formKey === FormDesignKeyEnum.BUSINESS
          ) {
            return {
              title: field.name,
              width: 150,
              key,
              fieldId: field.id,
              isTag: true,
              filter: !field.resourceFieldId,
              filterOptions: [],
              remoteFilterApiKey: field.businessKey,
              filedType: field.type,
              resourceFieldId: field.resourceFieldId,
            };
          }

          if (
            [
              FieldTypeEnum.DATA_SOURCE_MULTIPLE,
              FieldTypeEnum.MEMBER_MULTIPLE,
              FieldTypeEnum.DEPARTMENT_MULTIPLE,
            ].includes(field.type) ||
            field.type === FieldTypeEnum.INPUT_MULTIPLE
          ) {
            return {
              title: field.name,
              width: 150,
              key,
              fieldId: field.id,
              isTag: true,
              filedType: field.type,
              resourceFieldId: field.resourceFieldId,
            };
          }
          if (field.type === FieldTypeEnum.DATE_TIME) {
            return {
              title: field.name,
              width: 180,
              key,
              fieldId: field.id,
              ellipsis: {
                tooltip: true,
              },
              render: (row: any) => row[key] ?? '-',
              sortOrder: false,
              sorter: sorter && !field.resourceFieldId,
              filedType: field.type,
              resourceFieldId: field.resourceFieldId,
            };
          }
          if (field.type === FieldTypeEnum.INPUT_NUMBER) {
            return {
              title: field.name,
              width: 150,
              key,
              fieldId: field.id,
              render: (row: any) => formatNumberValueToString(row[key], field),
              sortOrder: false,
              sorter: sorter && !field.resourceFieldId,
              filedType: field.type,
              resourceFieldId: field.resourceFieldId,
            };
          }
          if ([FieldTypeEnum.MEMBER, FieldTypeEnum.DEPARTMENT].includes(field.type)) {
            return {
              title: field.name,
              width: 150,
              key,
              fieldId: field.id,
              ellipsis: {
                tooltip: true,
              },
              sortOrder: false,
              sorter: !noSorterType.includes(field.type) && !field.resourceFieldId,
              filedType: field.type,
              resourceFieldId: field.resourceFieldId,
            };
          }
          return {
            title: field.name,
            width: 150,
            key,
            fieldId: field.id,
            ellipsis: {
              tooltip: true,
            },
            sortOrder: false,
            sorter: !noSorterType.includes(field.type) && !field.resourceFieldId ? sorter : false,
            filedType: field.type,
            resourceFieldId: field.resourceFieldId,
          };
        });

      columns = [
        ...getFollowColumn(res.fields),
        ...columns,
        ...(internalColumnMap[props.formKey] || []),
        ...staticColumns,
      ];
      if (isFollowModule) {
        columns = disableFilterAndSorter(columns);
      }
      if (
        !props.readonly &&
        ![FormDesignKeyEnum.FOLLOW_PLAN, FormDesignKeyEnum.FOLLOW_RECORD].includes(props.formKey)
      ) {
        columns.unshift({
          type: 'selection',
          fixed: 'left',
          width: 46,
          multiple: !props.radio,
          disabled(row) {
            return props.disabledSelection ? props.disabledSelection(row) : false;
          },
          ...(props.formKey === FormDesignKeyEnum.CLUE
            ? {
                selectTooltip: {
                  showTooltip(row) {
                    return props.disabledSelection ? props.disabledSelection(row) : false;
                  },
                  tooltipText: t('clue.disabledTooltipText'),
                },
              }
            : {}),
        });
      }
      columns.unshift({
        fixed: 'left',
        title: t('crmTable.order'),
        width: 50,
        key: SpecialColumnEnum.ORDER,
        resizable: false,
        columnSelectorDisabled: true,
        render: (row: any, rowIndex: number) => rowIndex + 1,
      });
      if (props.operationColumn) {
        columns.push(props.operationColumn);
      }
      customFieldsFilterConfig.value = getFilterListConfig(res);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  await initFormConfig();

  const useTableRes = useTable(
    getFormListApiMap[props.formKey],
    {
      tableKey: tableKeyMap[props.formKey],
      showSetting: !!tableKeyMap[props.formKey],
      showPagination,
      columns,
      permission: props.permission,
      // virtualScrollX: props.formKey !== FormDesignKeyEnum.PRODUCT, // TODO:横向滚动有问题
      containerClass: props.containerClass,
      hiddenTotal: props.hiddenTotal,
      hiddenAllScreen: props.hiddenAllScreen,
      hiddenRefresh: props.hiddenRefresh,
    },
    (item, originalData) => {
      return transformData({
        item,
        originalData,
        fields: fieldList.value,
        excludeFieldIds: props.excludeFieldIds,
      });
    }
  );

  return {
    loading,
    useTableRes,
    customFieldsFilterConfig,
    reasonOptions,
    dicApprovalEnable,
    fieldList,
  };
}
