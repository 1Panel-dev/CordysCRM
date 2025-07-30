import { NImage, NImageGroup } from 'naive-ui';
import dayjs from 'dayjs';

import { PreviewPictureUrl } from '@lib/shared/api/requrls/system/module';
import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { formatTimeValue, getCityPath } from '@lib/shared/method';
import type { ModuleField } from '@lib/shared/models/customer';

import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
import useTable from '@/components/pure/crm-table/useTable';
import {
  getFormConfigApiMap,
  getFormListApiMap,
  multipleValueTypeList,
} from '@/components/business/crm-form-create/config';
import type { FormCreateField } from '@/components/business/crm-form-create/types';

import { lastOpportunitySteps } from '@/config/opportunity';
import useFormCreateAdvanceFilter from '@/hooks/useFormCreateAdvanceFilter';
import useReasonConfig from '@/hooks/useReasonConfig';

type FormKey =
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
  | FormDesignKeyEnum.CLUE_TRANSITION_CUSTOMER;

export interface FormCreateTableProps {
  formKey: FormKey;
  disabledSelection?: (row: any) => boolean;
  operationColumn?: CrmDataTableColumn;
  specialRender?: Record<string, (row: any) => void>;
  showPagination?: boolean;
  excludeFieldIds?: string[]; // 规避某些字段的文字替换
  permission?: string[];
  readonly?: boolean;
  radio?: boolean; // 是否单选
}

export default async function useFormCreateTable(props: FormCreateTableProps) {
  const { t } = useI18n();
  const { getFilterListConfig, customFieldsFilterConfig } = useFormCreateAdvanceFilter();
  const { reasonOptions, initReasonConfig } = useReasonConfig(props.formKey);
  const loading = ref(false);
  const showPagination = props.showPagination ?? true;
  let columns: CrmDataTableColumn[] = [];
  const columnsSorter = showPagination ? true : 'default';
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
  };
  const noPaginationKey = [FormDesignKeyEnum.CUSTOMER_CONTACT];
  // 存储地址类型字段集合
  const addressFieldIds = ref<string[]>([]);
  // 业务字段集合
  const businessFieldIds = ref<string[]>([]);
  // 数据源字段集合
  const dataSourceFieldIds = ref<string[]>([]);

  // 静态列和高级筛选增加原因配置筛选
  await initReasonConfig();
  const opportunityInternalColumns: CrmDataTableColumn[] = [
    {
      title: t('org.department'),
      width: 120,
      key: 'departmentId',
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
      render: (row: any) => row.departmentName || '-',
    },
    {
      title: t('opportunity.stage'),
      width: 150,
      key: 'stage',
      ellipsis: {
        tooltip: true,
      },
      filter: true,
      sortOrder: false,
      sorter: true,
      filterOptions: lastOpportunitySteps,
      render: props.specialRender?.stage,
    },
    {
      title: t('customer.lastFollowUps'),
      width: 150,
      key: 'followerName',
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
    },
    {
      title: t('customer.lastFollowUpDate'),
      width: 160,
      key: 'followTime',
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
      render: (row: any) => (row.followTime ? dayjs(row.followTime).format('YYYY-MM-DD') : '-'),
    },
    {
      title: t('customer.remainingVesting'),
      width: 120,
      key: 'reservedDays',
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
          label: t('common.open'),
          value: true,
        },
        {
          label: t('common.close'),
          value: false,
        },
      ],
      filter: true,
      sortOrder: false,
      sorter: true,
      render: props.specialRender?.status,
    },
    {
      title: t('opportunity.actualEndTime'),
      width: 160,
      key: 'actualEndTime',
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
      render: (row: any) => (row.actualEndTime ? dayjs(row.actualEndTime).format('YYYY-MM-DD') : '-'),
    },
    {
      title: t('opportunity.failureReason'),
      width: 120,
      key: 'failureReason',
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
      filterOptions: reasonOptions.value,
      filter: true,
      render: props.specialRender?.failureReason,
    },
  ];
  const customerInternalColumns: CrmDataTableColumn[] = [
    {
      title: t('org.department'),
      width: 120,
      key: 'departmentId',
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
      render: (row: any) => row.departmentName || '-',
    },
    {
      title: t('customer.collectionTime'),
      width: 160,
      key: 'collectionTime',
      sortOrder: false,
      sorter: true,
      render: (row: any) => (row.collectionTime ? dayjs(row.collectionTime).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: t('customer.recycleOpenSea'),
      width: 120,
      key: 'recyclePoolName',
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('customer.remainingVesting'),
      width: 120,
      key: 'reservedDays',
      ellipsis: {
        tooltip: true,
      },
      render: (row: any) => (row.reservedDays ? `${row.reservedDays}${t('common.dayUnit')}` : '-'),
    },
    {
      title: t('customer.lastFollowUps'),
      width: 150,
      key: 'follower',
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
      render: (row: any) => row.followerName || '-',
    },
    {
      title: t('customer.lastFollowUpDate'),
      width: 160,
      key: 'followTime',
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
      render: (row: any) => (row.followTime ? dayjs(row.followTime).format('YYYY-MM-DD') : '-'),
    },
  ];
  const internalColumnMap: Record<FormKey, CrmDataTableColumn[]> = {
    [FormDesignKeyEnum.CUSTOMER]: customerInternalColumns,
    [FormDesignKeyEnum.CONTACT]: [
      {
        title: t('common.status'),
        width: 120,
        key: 'enable',
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
        filter: true,
        render: props.specialRender?.status,
      },
      {
        title: t('customer.disableReason'),
        width: 120,
        key: 'disableReason',
        sortOrder: false,
        sorter: true,
        ellipsis: {
          tooltip: true,
        },
      },
      {
        title: t('org.department'),
        width: 120,
        key: 'departmentId',
        ellipsis: {
          tooltip: true,
        },
        sortOrder: false,
        sorter: true,
        filter: true,
        render: (row: any) => row.departmentName || '-',
      },
    ],
    [FormDesignKeyEnum.CUSTOMER_CONTACT]: [
      {
        title: t('common.status'),
        width: 120,
        key: 'enable',
        ellipsis: {
          tooltip: true,
        },
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
        key: 'departmentId',
        ellipsis: {
          tooltip: true,
        },
        sortOrder: false,
        sorter: 'default',
        render: (row: any) => row.departmentName || '-',
      },
    ],
    [FormDesignKeyEnum.BUSINESS_CONTACT]: [
      {
        title: t('common.status'),
        width: 120,
        key: 'enable',
        ellipsis: {
          tooltip: true,
        },
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
        key: 'departmentId',
        ellipsis: {
          tooltip: true,
        },
        sortOrder: false,
        sorter: 'default',
        render: (row: any) => row.departmentName || '-',
      },
    ],
    [FormDesignKeyEnum.BUSINESS]: opportunityInternalColumns,
    [FormDesignKeyEnum.CLUE]: [
      {
        title: t('org.department'),
        width: 120,
        key: 'departmentId',
        ellipsis: {
          tooltip: true,
        },
        sortOrder: false,
        sorter: true,
        render: (row: any) => row.departmentName || '-',
      },
      {
        title: t('customer.collectionTime'),
        width: 160,
        key: 'collectionTime',
        sortOrder: false,
        sorter: true,
        render: (row: any) => (row.collectionTime ? dayjs(row.collectionTime).format('YYYY-MM-DD HH:mm:ss') : '-'),
      },
      {
        title: t('clue.recyclePool'),
        width: 120,
        key: 'recyclePoolName',
        ellipsis: {
          tooltip: true,
        },
      },
      {
        title: t('customer.remainingVesting'),
        width: 120,
        key: 'reservedDays',
        ellipsis: {
          tooltip: true,
        },
      },
      {
        title: t('customer.lastFollowUps'),
        width: 120,
        key: 'follower',
        ellipsis: {
          tooltip: true,
        },
        sortOrder: false,
        sorter: true,
        render: (row: any) => row.followerName || '-',
      },
      {
        title: t('customer.lastFollowUpDate'),
        width: 120,
        key: 'followTime',
        ellipsis: {
          tooltip: true,
        },
        sortOrder: false,
        sorter: true,
        render: (row: any) => (row.followTime ? dayjs(row.followTime).format('YYYY-MM-DD') : '-'),
      },
    ],
    [FormDesignKeyEnum.PRODUCT]: [],
    [FormDesignKeyEnum.CUSTOMER_OPEN_SEA]: [
      {
        title: t('customer.recycleReason'),
        width: 120,
        key: 'reasonId',
        ellipsis: {
          tooltip: true,
        },
        sortOrder: false,
        sorter: true,
        filterOptions: reasonOptions.value,
        filter: true,
        render: props.specialRender?.recycleReason,
      },
    ],
    [FormDesignKeyEnum.CLUE_POOL]: [
      {
        title: t('customer.recycleReason'),
        width: 120,
        key: 'reasonId',
        ellipsis: {
          tooltip: true,
        },
        sortOrder: false,
        sorter: true,
        filterOptions: reasonOptions.value,
        filter: true,
        render: props.specialRender?.recycleReason,
      },
    ],
    [FormDesignKeyEnum.CUSTOMER_OPPORTUNITY]: opportunityInternalColumns,
    [FormDesignKeyEnum.CLUE_TRANSITION_CUSTOMER]: customerInternalColumns,
  };
  const staticColumns: CrmDataTableColumn[] = [
    {
      title: t('common.creator'),
      key: 'createUser',
      width: 120,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: columnsSorter,
      render: (row: any) => row.createUserName || '-',
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
      key: 'updateUser',
      width: 120,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: columnsSorter,
      render: (row: any) => row.updateUserName || '-',
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

  function formatNumberValue(value: string | number, field: FormCreateField) {
    if (field.numberFormat === 'percent') {
      return value ? `${value}%` : '-';
    }
    if (field.showThousandsSeparator) {
      return value ? Number(value).toLocaleString('en-us') : '-';
    }
    return value;
  }

  async function initFormConfig() {
    try {
      const sorter = noPaginationKey.includes(props.formKey) ? 'default' : true;
      loading.value = true;
      const res = await getFormConfigApiMap[props.formKey]();
      columns = res.fields
        .filter(
          (e) =>
            e.type !== FieldTypeEnum.DIVIDER &&
            e.type !== FieldTypeEnum.TEXTAREA &&
            !(
              e.businessKey === 'owner' &&
              [FormDesignKeyEnum.CLUE_POOL, FormDesignKeyEnum.CUSTOMER_OPEN_SEA].includes(props.formKey)
            )
        )
        .map((field) => {
          const noSorterType = [
            FieldTypeEnum.DIVIDER,
            FieldTypeEnum.PICTURE,
            FieldTypeEnum.TEXTAREA,
            FieldTypeEnum.INPUT_MULTIPLE,
            FieldTypeEnum.MEMBER_MULTIPLE,
            FieldTypeEnum.SELECT_MULTIPLE,
            FieldTypeEnum.DATA_SOURCE_MULTIPLE,
            FieldTypeEnum.USER_TAG_SELECTOR,
            FieldTypeEnum.CHECKBOX,
          ];
          if (field.type === FieldTypeEnum.PICTURE) {
            return {
              title: field.name,
              width: 200,
              key: field.businessKey || field.id,
              fieldId: field.id,
              filedType: FieldTypeEnum.PICTURE,
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
                          row[field.businessKey || field.id]?.length
                            ? (row[field.businessKey || field.id] || []).map((key: string) =>
                                h(NImage, {
                                  class: 'h-[40px] w-[40px] mr-[4px]',
                                  src: `${PreviewPictureUrl}/${key}`,
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
              key: field.businessKey || field.id,
              fieldId: field.id,
              ellipsis: {
                tooltip: true,
              },
              isTag: field.type === FieldTypeEnum.CHECKBOX || field.type === FieldTypeEnum.SELECT_MULTIPLE,
              filterOptions: field.options || field.initialOptions?.map((e: any) => ({ label: e.name, value: e.id })),
              filter: true,
              sortOrder: false,
              sorter: !noSorterType.includes(field.type),
              filterMultipleValue: multipleValueTypeList.includes(field.type),
              filedType: field.type,
            };
          }
          if (
            field.businessKey === 'name' &&
            ![FormDesignKeyEnum.CUSTOMER_CONTACT, FormDesignKeyEnum.BUSINESS_CONTACT].includes(props.formKey)
          ) {
            return {
              title: field.name,
              width: 200,
              key: field.businessKey,
              fieldId: field.id,
              sortOrder: false,
              sorter,
              fixed: 'left',
              columnSelectorDisabled: true,
              filedType: field.type,
              render: props.specialRender?.[field.businessKey],
            };
          }

          if (field.businessKey === 'customerId') {
            return {
              title: field.name,
              width: 200,
              key: field.businessKey,
              fieldId: field.id,
              sortOrder: false,
              sorter,
              filedType: field.type,
              render: props.specialRender?.[field.businessKey],
            };
          }

          if (field.businessKey === 'owner') {
            return {
              title: field.name,
              width: 200,
              key: field.businessKey,
              fieldId: field.id,
              sortOrder: false,
              sorter,
              ellipsis: {
                tooltip: true,
              },
              filedType: field.type,
              render: props.specialRender?.[field.businessKey],
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
              key: field.businessKey || field.id,
              fieldId: field.id,
              isTag: true,
              filedType: field.type,
            };
          }
          if (field.type === FieldTypeEnum.DATE_TIME) {
            return {
              title: field.name,
              width: 180,
              key: field.businessKey || field.id,
              fieldId: field.id,
              render: (row: any) => formatTimeValue(row[field.businessKey || field.id], field.dateType),
              sortOrder: false,
              sorter,
              filedType: field.type,
            };
          }
          if (field.type === FieldTypeEnum.INPUT_NUMBER) {
            return {
              title: field.name,
              width: 150,
              key: field.businessKey || field.id,
              fieldId: field.id,
              render: (row: any) => formatNumberValue(row[field.businessKey || field.id], field),
              sortOrder: false,
              sorter,
              filedType: field.type,
            };
          }
          if ([FieldTypeEnum.MEMBER, FieldTypeEnum.DEPARTMENT].includes(field.type)) {
            return {
              title: field.name,
              width: 150,
              key: field.businessKey || field.id,
              fieldId: field.id,
              ellipsis: {
                tooltip: true,
              },
              sortOrder: false,
              sorter: !noSorterType.includes(field.type),
              filedType: field.type,
            };
          }
          return {
            title: field.name,
            width: 150,
            key: field.businessKey || field.id,
            fieldId: field.id,
            ellipsis: {
              tooltip: true,
            },
            sortOrder: false,
            sorter: !noSorterType.includes(field.type) ? sorter : false,
            filedType: field.type,
          };
        });
      columns = [...columns, ...(internalColumnMap[props.formKey] || []), ...staticColumns];
      if (!props.readonly) {
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
        width: 56,
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
    },
    (item, originalData) => {
      const businessFieldAttr: Record<string, any> = {};
      const customFieldAttr: Record<string, any> = {};
      businessFieldIds.value.forEach((fieldId) => {
        const options = originalData?.optionMap?.[fieldId]?.map((e) => ({
          ...e,
          name: e.name || t('common.optionNotExist'),
        }));
        if (addressFieldIds.value.includes(fieldId)) {
          // 地址类型字段，解析代码替换成省市区
          const address = item[fieldId]?.split('-') || [];
          const value = `${getCityPath(address[0])}${address[1] ? `-${address[1]}` : ''}`;
          businessFieldAttr[fieldId] = value;
        } else if (options && options.length > 0) {
          let name: string | string[] = '';
          if (dataSourceFieldIds.value.includes(fieldId)) {
            // 处理数据源字段，需要赋值为数组
            if (typeof item[fieldId] === 'string' || typeof item[fieldId] === 'number') {
              // 单选
              name = [options?.find((e) => e.id === item[fieldId])?.name || t('common.optionNotExist')];
            } else {
              // 多选
              name = options?.filter((e) => item[fieldId].includes(e.id)).map((e) => e.name) || [
                t('common.optionNotExist'),
              ];
            }
          } else if (typeof item[fieldId] === 'string' || typeof item[fieldId] === 'number') {
            // 若值是单个字符串/数字
            name = options?.find((e) => e.id === item[fieldId])?.name;
          } else {
            // 若值是数组
            name = options?.filter((e) => item[fieldId]?.includes(e.id)).map((e) => e.name) || [
              t('common.optionNotExist'),
            ];
            if (Array.isArray(name) && name.length === 0) {
              name = [t('common.optionNotExist')];
            }
          }
          businessFieldAttr[fieldId] = name || t('common.optionNotExist');
        }
      });
      item.moduleFields?.forEach((field: ModuleField) => {
        const options = originalData?.optionMap?.[field.fieldId]?.map((e) => ({
          ...e,
          name: e.name || t('common.optionNotExist'),
        }));
        if (addressFieldIds.value.includes(field.fieldId)) {
          // 地址类型字段，解析代码替换成省市区
          const address = (field.fieldValue as string)?.split('-') || [];
          const value = `${getCityPath(address[0])}${address[1] ? `-${address[1]}` : ''}`;
          customFieldAttr[field.fieldId] = value;
        } else if (options && options.length > 0) {
          let name: string | string[] = '';
          if (dataSourceFieldIds.value.includes(field.fieldId)) {
            // 处理数据源字段，需要赋值为数组
            if (typeof field.fieldValue === 'string' || typeof field.fieldValue === 'number') {
              // 单选
              name = [options.find((e) => e.id === field.fieldValue)?.name || t('common.optionNotExist')];
            } else {
              // 多选
              name = options.filter((e) => field.fieldValue?.includes(e.id)).map((e) => e.name);
            }
          } else if (typeof field.fieldValue === 'string' || typeof field.fieldValue === 'number') {
            // 若值是单个字符串/数字
            name = options.find((e) => e.id === field.fieldValue)?.name || t('common.optionNotExist');
          } else {
            // 若值是数组
            name = options.filter((e) => field.fieldValue?.includes(e.id)).map((e) => e.name);
            if (Array.isArray(name) && name.length === 0) {
              name = [t('common.optionNotExist')];
            }
          }
          customFieldAttr[field.fieldId] = name || [t('common.optionNotExist')];
        } else {
          // 其他类型字段，直接赋值
          customFieldAttr[field.fieldId] = field.fieldValue;
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
    reasonOptions,
  };
}
