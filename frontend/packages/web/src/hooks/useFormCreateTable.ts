import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
import type { ModuleField } from '@lib/shared/models/customer';

import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
import useTable from '@/components/pure/crm-table/useTable';
import { getFormConfigApiMap, getFormListApiMap } from '@/components/business/crm-form-create/config';

import { useI18n } from './useI18n';

export interface FormCreateTableProps {
  formKey: FormDesignKeyEnum;
  operationColumn?: CrmDataTableColumn;
  specialRender?: Record<string, (row: any) => void>;
}

export default async function useFormCreateTable(props: FormCreateTableProps) {
  const { t } = useI18n();

  const loading = ref(false);
  let columns: CrmDataTableColumn[] = [];
  const tableKeyMap = {
    [FormDesignKeyEnum.CUSTOMER]: TableKeyEnum.CUSTOMER,
    [FormDesignKeyEnum.CONTACT]: TableKeyEnum.CUSTOMER_CONTRACT,
    [FormDesignKeyEnum.BUSINESS]: TableKeyEnum.BUSINESS,
    [FormDesignKeyEnum.FOLLOW_PLAN]: TableKeyEnum.CUSTOMER_FOLLOW_PLAN,
    [FormDesignKeyEnum.FOLLOW_RECORD]: TableKeyEnum.CUSTOMER_FOLLOW_RECORD,
    [FormDesignKeyEnum.CLUE]: TableKeyEnum.CLUE,
    [FormDesignKeyEnum.PRODUCT]: TableKeyEnum.PRODUCT,
  };

  const internalColumnMap: Record<FormDesignKeyEnum, CrmDataTableColumn[]> = {
    [FormDesignKeyEnum.CUSTOMER]: [
      {
        title: t('customer.recycleOpenSea'),
        width: 120,
        key: 'recycleOpenSea',
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
        sortOrder: false,
        sorter: true,
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
        sortOrder: false,
        sorter: true,
      },
      {
        title: t('common.head'),
        width: 120,
        key: 'head',
        ellipsis: {
          tooltip: true,
        },
        sortOrder: false,
        sorter: true,
      },
      {
        title: t('org.department'),
        width: 120,
        key: 'department',
        ellipsis: {
          tooltip: true,
        },
        sortOrder: false,
        sorter: true,
      },
    ],
    [FormDesignKeyEnum.BUSINESS]: [
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
        sortOrder: false,
        sorter: true,
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
            value: true,
          },
          {
            label: t('common.disable'),
            value: false,
          },
        ],
        sortOrder: false,
        sorter: true,
      },
    ],
    [FormDesignKeyEnum.FOLLOW_PLAN]: [],
    [FormDesignKeyEnum.FOLLOW_RECORD]: [],
    [FormDesignKeyEnum.CLUE]: [
      {
        title: t('clue.recyclePool'),
        width: 120,
        key: 'recyclePool',
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
        sortOrder: false,
        sorter: true,
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
      sorter: true,
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 160,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
    },
    {
      title: t('common.updateUserName'),
      key: 'updateUser',
      width: 120,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
    },
    {
      title: t('common.updateTime'),
      key: 'updateTime',
      width: 160,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
    },
  ];

  async function initFormConfig() {
    try {
      loading.value = true;
      const res = await getFormConfigApiMap[props.formKey]();
      columns = res.fields
        .filter((e) => e.type !== FieldTypeEnum.DIVIDER)
        .map((field) => {
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
              filterOptions: field.options || field.initialOptions?.map((e) => ({ label: e.name, value: e.id })),
              filter: true,
            };
          }
          if (field.businessKey === 'name') {
            return {
              title: field.name,
              width: 150,
              key: field.businessKey,
              ellipsis: {
                tooltip: true,
              },
              sortOrder: false,
              sorter: true,
              render: props.specialRender?.[field.businessKey],
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
            sorter: true,
          };
        });
      columns = [...columns, ...internalColumnMap[props.formKey], ...staticColumns];
      if (props.operationColumn) {
        columns.push(props.operationColumn);
      }
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
      columns,
      scrollX: 1000,
    },
    (item) => {
      const customFieldAttr: Record<string, any> = {};
      item.moduleFields.forEach((field: ModuleField) => {
        customFieldAttr[field.fieldId] = field.fieldValue; // TODO:多值字段处理
      });
      return {
        ...item,
        ...customFieldAttr,
      };
    }
  );

  return {
    loading,
    useTableRes,
  };
}
