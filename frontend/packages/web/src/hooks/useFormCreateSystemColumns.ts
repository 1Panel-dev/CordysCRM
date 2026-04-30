import dayjs from 'dayjs';

import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';

import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';

import { contractPaymentPlanStatusOptions, contractStatusOptions } from '@/config/contract';
import { quotationStatus } from '@/config/opportunity';
import { processStatusOptions } from '@/config/process';
import useReasonConfig from '@/hooks/useReasonConfig';

import { FormCreateTableProps } from './useFormCreateTable';
import { FilterOption } from 'naive-ui/es/data-table/src/interface';

interface FormCreateSystemColumnsResult {
  internalColumnMap: Record<string, CrmDataTableColumn[]>;
  staticColumns: CrmDataTableColumn[];
  reasonOptions: Ref<FilterOption[]>;
  noSorterType: FieldTypeEnum[];
}

export default async function useFormCreateSystemColumns(
  props: FormCreateTableProps
): Promise<FormCreateSystemColumnsResult> {
  const { t } = useI18n();

  const { reasonOptions, initReasonConfig } = useReasonConfig(props.formKey);
  const showPagination = props.showPagination ?? true;
  const columnsSorter = showPagination ? true : 'default';
  const enableApproval = props.enableApproval?.value ?? false;

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
    FieldTypeEnum.LINK,
    FieldTypeEnum.DEPARTMENT_MULTIPLE,
  ];

  // 静态列和高级筛选增加原因配置筛选
  await initReasonConfig();

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
      render: (row: any) => row.reasonName || '-',
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
  const contactInternalColumns: CrmDataTableColumn[] = [
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
      render: (row: any) => row.departmentName || '-',
    },
  ];

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
      filterOptions:
        props.opportunityStage?.map((e) => ({
          label: e.name,
          value: e.id,
        })) || [],
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
    },
  ];

  const recordInternalColumns: CrmDataTableColumn[] = [
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
      key: 'phone',
      title: t('common.phoneNumber'),
      width: 120,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      key: 'content',
      title: t('eventDrawer.record.content'),
      width: 120,
      ellipsis: {
        tooltip: true,
      },
    },
  ];

  const planInternalColumns: CrmDataTableColumn[] = [
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
      key: 'phone',
      title: t('common.phoneNumber'),
      width: 120,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('eventDrawer.record.converted'),
      width: 120,
      key: 'converted',
      render: (row: any) => (row.converted ? t('common.yes') : t('common.no')),
    },
    {
      key: 'content',
      title: t('eventDrawer.plan.content'),
      width: 120,
      ellipsis: {
        tooltip: true,
      },
    },
  ];

  const paymentInternalColumns: CrmDataTableColumn[] = [
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
      title: t('contract.planStatus'),
      width: 120,
      key: 'planStatus',
      filterOptions: contractPaymentPlanStatusOptions,
      sortOrder: false,
      sorter: true,
      filter: true,
      render: props.specialRender?.status,
    },
  ];
  // 审批状态
  const approvalStatusColumn = (
    enableApproval
      ? [
          {
            title: t('contract.approvalStatus'),
            width: 120,
            key: 'approvalStatus',
            filterOptions: processStatusOptions,
            sortOrder: false,
            sorter: true,
            filter: true,
            render: props.specialRender?.approvalStatus,
          },
        ]
      : []
  ) as CrmDataTableColumn[];

  const invoiceInternalColumns: CrmDataTableColumn[] = [
    ...approvalStatusColumn,
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
  ];

  const orderInternalColumns: CrmDataTableColumn[] = [
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
      title: t('order.status'),
      width: 150,
      key: 'stage',
      ellipsis: {
        tooltip: true,
      },
      filter: true,
      sortOrder: false,
      sorter: true,
      filterOptions:
        props.orderStage?.map((e) => ({
          label: e.name,
          value: e.id,
        })) || [],
      render: props.specialRender?.stage,
    },
    ...approvalStatusColumn,
  ];

  const internalColumnMap: Record<string, CrmDataTableColumn[]> = {
    [FormDesignKeyEnum.CUSTOMER]: customerInternalColumns,
    [FormDesignKeyEnum.CONTACT]: contactInternalColumns,
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
        width: 180,
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
        render: (row: any) => row.reasonName || '-',
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
        render: (row: any) => row.reasonName || '-',
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
        render: (row: any) => row.reasonName || '-',
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
    [FormDesignKeyEnum.CUSTOMER_OPPORTUNITY]: opportunityInternalColumns,
    [FormDesignKeyEnum.CLUE_TRANSITION_CUSTOMER]: customerInternalColumns,
    [FormDesignKeyEnum.FOLLOW_RECORD]: recordInternalColumns,
    [FormDesignKeyEnum.FOLLOW_PLAN]: planInternalColumns,
    [FormDesignKeyEnum.SEARCH_ADVANCED_CLUE]: [],
    [FormDesignKeyEnum.SEARCH_ADVANCED_CUSTOMER]: [
      {
        title: t('workbench.duplicateCheck.relatedOpportunity'),
        key: 'opportunityCount',
        width: 60,
        render: props.specialRender?.opportunityCount,
      },
      {
        title: t('workbench.duplicateCheck.relatedClue'),
        key: 'clueCount',
        width: 60,
        render: props.specialRender?.clueCount,
      },
      ...customerInternalColumns,
    ],
    [FormDesignKeyEnum.SEARCH_ADVANCED_CONTACT]: contactInternalColumns,
    [FormDesignKeyEnum.SEARCH_ADVANCED_PUBLIC]: [
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
        render: (row: any) => row.reasonName || '-',
      },
      {
        title: t('customer.recycleOpenSeaName'),
        width: 120,
        key: 'poolId',
        ellipsis: {
          tooltip: true,
        },
        sortOrder: false,
        sorter: true,
        render: (row: any) => row.poolName || '-',
      },
    ],
    [FormDesignKeyEnum.SEARCH_ADVANCED_CLUE_POOL]: [
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
        render: (row: any) => row.reasonName || '-',
      },
      {
        title: t('clue.belongingCluePool'),
        width: 120,
        key: 'poolId',
        ellipsis: {
          tooltip: true,
        },
        sortOrder: false,
        sorter: true,
        render: (row: any) => row.poolName || '-',
      },
    ],
    [FormDesignKeyEnum.SEARCH_ADVANCED_OPPORTUNITY]: opportunityInternalColumns,
    [FormDesignKeyEnum.OPPORTUNITY_QUOTATION]: [
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
        title: t('common.status'),
        width: 120,
        key: 'status',
        filterOptions: quotationStatus,
        sortOrder: false,
        sorter: true,
        filter: true,
        render: props.specialRender?.status,
      },
      ...approvalStatusColumn,
    ],
    [FormDesignKeyEnum.CONTRACT]: [
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
        title: t('contract.status'),
        width: 120,
        key: 'stage',
        filterOptions: contractStatusOptions,
        sortOrder: false,
        sorter: true,
        filter: true,
        render: props.specialRender?.stage,
      },
      {
        title: t('contract.voidReason'),
        width: 120,
        key: 'voidReason',
        ellipsis: {
          tooltip: true,
        },
      },
      {
        title: t('contract.alreadyPayAmount'),
        width: 120,
        key: 'alreadyPayAmount',
        sortOrder: false,
        sorter: true,
      },
      ...approvalStatusColumn,
    ],
    [FormDesignKeyEnum.CONTRACT_PAYMENT]: paymentInternalColumns,
    [FormDesignKeyEnum.CONTRACT_CONTRACT_PAYMENT]: paymentInternalColumns,
    [FormDesignKeyEnum.CONTRACT_PAYMENT_RECORD]: [
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
    [FormDesignKeyEnum.PRICE]: [],
    [FormDesignKeyEnum.INVOICE]: invoiceInternalColumns,
    [FormDesignKeyEnum.CONTRACT_INVOICE]: invoiceInternalColumns,
    [FormDesignKeyEnum.ORDER]: orderInternalColumns,
    [FormDesignKeyEnum.CONTRACT_ORDER]: orderInternalColumns,
    [FormDesignKeyEnum.CUSTOMER_ORDER]: orderInternalColumns,
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
  return {
    internalColumnMap,
    staticColumns,
    reasonOptions,
    noSorterType,
  };
}
