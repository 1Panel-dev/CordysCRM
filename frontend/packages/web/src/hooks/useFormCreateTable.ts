import { NImage, NImageGroup } from 'naive-ui';
import dayjs from 'dayjs';

import { PreviewPictureUrl } from '@lib/shared/api/requrls/system/module';
import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
import type { ModuleField } from '@lib/shared/models/customer';

import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
import useTable from '@/components/pure/crm-table/useTable';
import { getFormConfigApiMap, getFormListApiMap } from '@/components/business/crm-form-create/config';
import type { FormCreateField, FormCreateFieldDateType } from '@/components/business/crm-form-create/types';

import { lastOpportunitySteps } from '@/config/opportunity';
import useFormCreateAdvanceFilter from '@/hooks/useFormCreateAdvanceFilter';
import { getCityPath } from '@/utils';

import { useI18n } from './useI18n';

type FormKey =
  | FormDesignKeyEnum.CUSTOMER
  | FormDesignKeyEnum.CONTACT
  | FormDesignKeyEnum.BUSINESS
  | FormDesignKeyEnum.CLUE
  | FormDesignKeyEnum.PRODUCT
  | FormDesignKeyEnum.CUSTOMER_OPEN_SEA
  | FormDesignKeyEnum.CLUE_POOL
  | FormDesignKeyEnum.CUSTOMER_CONTACT;

export interface FormCreateTableProps {
  formKey: FormKey;
  disabledSelection?: (row: any) => boolean;
  operationColumn?: CrmDataTableColumn;
  specialRender?: Record<string, (row: any) => void>;
  showPagination?: boolean;
}

export default async function useFormCreateTable(props: FormCreateTableProps) {
  const { t } = useI18n();
  const { getFilterListConfig, customFieldsFilterConfig } = useFormCreateAdvanceFilter(props.formKey);
  const loading = ref(false);
  const showPagination = props.showPagination ?? true;
  let columns: CrmDataTableColumn[] = [];
  const columnsSorter = showPagination ? true : 'default';
  const tableKeyMap = {
    [FormDesignKeyEnum.CUSTOMER]: TableKeyEnum.CUSTOMER,
    [FormDesignKeyEnum.CONTACT]: TableKeyEnum.CUSTOMER_CONTRACT,
    [FormDesignKeyEnum.CUSTOMER_CONTACT]: TableKeyEnum.CUSTOMER_CONTRACT,
    [FormDesignKeyEnum.BUSINESS]: TableKeyEnum.BUSINESS,
    [FormDesignKeyEnum.CLUE]: TableKeyEnum.CLUE,
    [FormDesignKeyEnum.CLUE_POOL]: TableKeyEnum.CLUE_POOL,
    [FormDesignKeyEnum.PRODUCT]: TableKeyEnum.PRODUCT,
    [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: TableKeyEnum.CUSTOMER_OPEN_SEA,
  };
  // 存储地址类型字段集合
  const addressFieldIds = ref<string[]>([]);
  // 业务字段集合
  const businessFieldIds = ref<string[]>([]);
  // 数据源字段集合
  const dataSourceFieldIds = ref<string[]>([]);

  const internalColumnMap: Record<FormKey, CrmDataTableColumn[]> = {
    [FormDesignKeyEnum.CUSTOMER]: [
      {
        title: t('customer.recycleOpenSea'),
        width: 120,
        key: 'recycleOpenSea',
        ellipsis: {
          tooltip: true,
        },
      },
      {
        title: t('customer.remainingVesting'),
        width: 120,
        key: 'remainingVesting',
        ellipsis: {
          tooltip: true,
        },
      },
      {
        title: t('customer.lastFollowUps'),
        width: 150,
        key: 'lastFollowUps',
        ellipsis: {
          tooltip: true,
        },
        sortOrder: false,
        sorter: true,
      },
      {
        title: t('customer.lastFollowUpDate'),
        width: 160,
        key: 'lastFollowUpDate',
        ellipsis: {
          tooltip: true,
        },
        sortOrder: false,
        sorter: true,
      },
    ],
    [FormDesignKeyEnum.CONTACT]: [
      {
        title: t('common.status'),
        width: 120,
        key: 'status',
        ellipsis: {
          tooltip: true,
        },
        filterOptions: [
          {
            label: t('common.enable'),
            value: true,
          },
          {
            label: t('common.disable'),
            value: false,
          },
        ],
        sortOrder: false,
        sorter: true,
        render: props.specialRender?.status,
      },
      {
        title: t('customer.disableReason'),
        width: 120,
        key: 'disableReason',
        ellipsis: {
          tooltip: true,
        },
      },
      {
        title: t('org.department'),
        width: 120,
        key: 'departmentName',
        ellipsis: {
          tooltip: true,
        },
        sortOrder: false,
        sorter: true,
      },
    ],
    [FormDesignKeyEnum.CUSTOMER_CONTACT]: [
      {
        title: t('common.status'),
        width: 120,
        key: 'status',
        ellipsis: {
          tooltip: true,
        },
        filterOptions: [
          {
            label: t('common.enable'),
            value: true,
          },
          {
            label: t('common.disable'),
            value: false,
          },
        ],
        sortOrder: false,
        sorter: 'default',
        render: props.specialRender?.status,
      },
      {
        title: t('customer.disableReason'),
        width: 120,
        key: 'disableReason',
        ellipsis: {
          tooltip: true,
        },
      },
      {
        title: t('org.department'),
        width: 120,
        key: 'department',
        ellipsis: {
          tooltip: true,
        },
        sortOrder: false,
        sorter: 'default',
      },
    ],
    [FormDesignKeyEnum.BUSINESS]: [
      {
        title: t('opportunity.stage'),
        width: 150,
        key: 'stage',
        ellipsis: {
          tooltip: true,
        },
        filterOptions: lastOpportunitySteps,
        render: props.specialRender?.stage,
      },
      {
        title: t('customer.lastFollowUps'),
        width: 150,
        key: 'lastFollowUps',
        ellipsis: {
          tooltip: true,
        },
        sortOrder: false,
        sorter: true,
      },
      {
        title: t('customer.lastFollowUpDate'),
        width: 160,
        key: 'lastFollowUpDate',
        ellipsis: {
          tooltip: true,
        },
        sortOrder: false,
        sorter: true,
      },
      {
        title: t('customer.remainingVesting'),
        width: 120,
        key: 'remainingVesting',
        ellipsis: {
          tooltip: true,
        },
      },
      {
        title: t('common.status'),
        width: 120,
        key: 'status',
        ellipsis: {
          tooltip: true,
        },
        filterOptions: [
          {
            label: t('common.enable'),
            value: '1',
          },
          {
            label: t('common.disable'),
            value: '0',
          },
        ],
        sortOrder: false,
        sorter: true,
        render: props.specialRender?.status,
      },
    ],
    [FormDesignKeyEnum.CLUE]: [
      {
        title: t('clue.recyclePool'),
        width: 120,
        key: 'recyclePool',
        ellipsis: {
          tooltip: true,
        },
      },
      {
        title: t('customer.remainingVesting'),
        width: 120,
        key: 'remainingVesting',
        ellipsis: {
          tooltip: true,
        },
      },
      {
        title: t('customer.lastFollowUps'),
        width: 120,
        key: 'lastFollowUps',
        ellipsis: {
          tooltip: true,
        },
        sortOrder: false,
        sorter: true,
      },
      {
        title: t('customer.lastFollowUpDate'),
        width: 120,
        key: 'lastFollowUpDate',
        ellipsis: {
          tooltip: true,
        },
        sortOrder: false,
        sorter: true,
      },
    ],
    [FormDesignKeyEnum.PRODUCT]: [],
    [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: [],
    [FormDesignKeyEnum.CLUE_POOL]: [],
  };
  const staticColumns: CrmDataTableColumn[] = [
    {
      title: t('common.creator'),
      key: 'createUserName',
      width: 120,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: columnsSorter,
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 160,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: columnsSorter,
    },
    {
      title: t('common.updateUserName'),
      key: 'updateUserName',
      width: 120,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: columnsSorter,
    },
    {
      title: t('common.updateTime'),
      key: 'updateTime',
      width: 160,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: columnsSorter,
    },
  ];

  function formatTimeValue(value: string | number, type?: FormCreateFieldDateType) {
    if (value) {
      const date = dayjs(Number(value));
      switch (type) {
        case 'month':
          return date.format('YYYY-MM');
        case 'date':
          return date.format('YYYY-MM-DD');
        case 'datetime':
        default:
          return date.format('YYYY-MM-DD HH:mm:ss');
      }
    }
    return '-';
  }

  function formatNumberValue(value: string | number, field: FormCreateField) {
    if (field.numberFormat === 'percent') {
      return value ? `${value}%` : '-';
    }
    if (field.showThousandsSeparator) {
      return value ? Number(value).toLocaleString('en-us') : '-';
    }
  }

  async function initFormConfig() {
    try {
      loading.value = true;
      const res = await getFormConfigApiMap[props.formKey]();
      columns = res.fields
        .filter((e) => e.type !== FieldTypeEnum.DIVIDER && e.type !== FieldTypeEnum.TEXTAREA)
        .map((field) => {
          if (field.type === FieldTypeEnum.PICTURE) {
            return {
              title: field.name,
              width: 200,
              key: field.businessKey || field.id,
              render: (row: any) =>
                h(
                  NImageGroup,
                  {},
                  {
                    default: () =>
                      row[field.businessKey || field.id]?.length
                        ? (row[field.businessKey || field.id] || []).map((key: string) =>
                            h(NImage, {
                              width: 40,
                              height: 40,
                              src: `${PreviewPictureUrl}/${key}`,
                            })
                          )
                        : '-',
                  }
                ),
            };
          }
          if (field.type === FieldTypeEnum.LOCATION) {
            addressFieldIds.value.push(field.businessKey || field.id);
          } else if (field.type === FieldTypeEnum.DATA_SOURCE) {
            dataSourceFieldIds.value.push(field.businessKey || field.id);
          }
          if (field.businessKey) {
            businessFieldIds.value.push(field.businessKey);
          }
          if (
            [
              FieldTypeEnum.RADIO,
              FieldTypeEnum.CHECKBOX,
              FieldTypeEnum.SELECT,
              FieldTypeEnum.MEMBER,
              FieldTypeEnum.DEPARTMENT,
            ].includes(field.type)
          ) {
            // 带筛选的列
            return {
              title: field.name,
              width: 150,
              key: field.businessKey || field.id,
              ellipsis: {
                tooltip: true,
              },
              isTag:
                field.type === FieldTypeEnum.CHECKBOX ||
                (field.type === FieldTypeEnum.SELECT && field.multiple) ||
                (field.type === FieldTypeEnum.MEMBER && field.multiple) ||
                (field.type === FieldTypeEnum.DEPARTMENT && field.multiple),
              filterOptions: field.options || field.initialOptions?.map((e: any) => ({ label: e.name, value: e.id })),
              filter: true,
            };
          }
          if (field.businessKey === 'name') {
            return {
              title: field.name,
              width: 200,
              key: field.businessKey,
              sortOrder: false,
              sorter: true,
              fixed: 'left',
              render: props.specialRender?.[field.businessKey],
            };
          }

          if (field.businessKey === 'customerId') {
            return {
              title: field.name,
              width: 200,
              key: field.businessKey,
              sortOrder: false,
              sorter: true,
              render: props.specialRender?.[field.businessKey],
            };
          }

          if (field.type === FieldTypeEnum.DATA_SOURCE || field.type === FieldTypeEnum.MULTIPLE_INPUT) {
            return {
              title: field.name,
              width: 150,
              key: field.businessKey || field.id,
              isTag: true,
            };
          }
          if (field.type === FieldTypeEnum.DATE_TIME) {
            return {
              title: field.name,
              width: 180,
              key: field.businessKey || field.id,
              render: (row: any) => formatTimeValue(row[field.businessKey || field.id], field.dateType),
            };
          }
          if (field.type === FieldTypeEnum.INPUT_NUMBER) {
            return {
              title: field.name,
              width: 100,
              key: field.businessKey || field.id,
              render: (row: any) => formatNumberValue(row[field.businessKey || field.id], field),
            };
          }
          return {
            title: field.name,
            width: 150,
            key: field.businessKey || field.id,
            ellipsis: {
              tooltip: true,
            },
            sortOrder: false,
            sorter: ![FieldTypeEnum.TEXTAREA, FieldTypeEnum.LOCATION, FieldTypeEnum.PICTURE].includes(field.type),
          };
        });
      columns = [
        {
          type: 'selection',
          fixed: 'left',
          disabled(row) {
            return props.disabledSelection ? props.disabledSelection(row) : false;
          },
        },
        ...columns,
        ...(internalColumnMap[props.formKey] || []),
        ...staticColumns,
      ];
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
      showSetting: true,
      showPagination,
      columns,
      scrollX: columns.reduce((prev, curr) => prev + (curr.width as number), 0) + 46,
      maxHeight: 600,
    },
    (item, originalData) => {
      const businessFieldAttr: Record<string, any> = {};
      const customFieldAttr: Record<string, any> = {};
      businessFieldIds.value.forEach((fieldId) => {
        const options = originalData?.optionMap?.[fieldId];
        let name: string | string[] = '';
        if (dataSourceFieldIds.value.includes(fieldId)) {
          if (typeof item[fieldId] === 'string') {
            name = [options?.find((e) => e.id === item[fieldId])?.name];
          } else {
            name = options?.filter((e) => item[fieldId].includes(e.id)).map((e) => e.name) || [];
          }
        } else {
          name = options?.find((e) => e.id === item[fieldId])?.name;
        }
        if (name) {
          businessFieldAttr[fieldId] = name;
        } else if (addressFieldIds.value.includes(fieldId)) {
          // 地址类型字段，解析代码替换成省市区
          const address = item[fieldId]?.split('-');
          const value = `${getCityPath(address[0])}-${address[1]}`;
          businessFieldAttr[fieldId] = value;
        }
      });
      item.moduleFields?.forEach((field: ModuleField) => {
        const options = originalData?.optionMap?.[field.fieldId];
        if (options) {
          // 若字段值是选项值，则取选项值的name
          if (Array.isArray(field.fieldValue)) {
            customFieldAttr[field.fieldId] = options.filter((e) => field.fieldValue.includes(e.id)).map((e) => e.name);
          } else {
            customFieldAttr[field.fieldId] = options.find((e) => e.id === field.fieldValue)?.name;
          }
        } else if (addressFieldIds.value.includes(field.fieldId)) {
          // 地址类型字段，解析代码替换成省市区
          const address = (field?.fieldValue as string)?.split('-');
          const value = `${getCityPath(address[0])}-${address[1]}`;
          customFieldAttr[field.fieldId] = value;
        } else {
          customFieldAttr[field.fieldId] = field.fieldValue;
        }
        // 数据源字段赋值
        if (dataSourceFieldIds.value.includes(field.fieldId)) {
          customFieldAttr[field.fieldId] = Array.isArray(field.fieldValue) ? field.fieldValue : [field.fieldValue];
        }
      });
      // 数据源字段赋值
      dataSourceFieldIds.value.forEach((fieldId) => {
        if (!customFieldAttr[fieldId] && !businessFieldAttr[fieldId]) {
          customFieldAttr[fieldId] = dataSourceFieldIds.value.includes(fieldId) ? [item[fieldId]] : item[fieldId];
        }
      });
      return {
        ...item,
        ...customFieldAttr,
        ...businessFieldAttr,
      };
    }
  );

  return {
    loading,
    useTableRes,
    customFieldsFilterConfig,
  };
}
