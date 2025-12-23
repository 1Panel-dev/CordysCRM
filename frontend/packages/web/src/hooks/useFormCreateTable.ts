import { NImage, NImageGroup, NSwitch } from 'naive-ui';
import dayjs from 'dayjs';

import { PreviewPictureUrl } from '@lib/shared/api/requrls/system/module';
import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { QuotationStatusEnum } from '@lib/shared/enums/opportunityEnum';
import { SpecialColumnEnum, TableKeyEnum } from '@lib/shared/enums/tableEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { formatTimeValue, getCityPath, getIndustryPath } from '@lib/shared/method';
import { formatNumberValue, transformData } from '@lib/shared/method/formCreate';
import type { CommonList } from '@lib/shared/models/common';
import type { ModuleField } from '@lib/shared/models/customer';
import type { StageConfigItem } from '@lib/shared/models/opportunity';

import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
import useTable from '@/components/pure/crm-table/useTable';
import {
  getFormConfigApiMap,
  getFormListApiMap,
  multipleValueTypeList,
} from '@/components/business/crm-form-create/config';
import type { FormCreateField } from '@/components/business/crm-form-create/types';

import { contractPaymentPlanStatusOptions, contractStatusOptions } from '@/config/contract';
import { quotationStatusOptions } from '@/config/opportunity';
import useFormCreateAdvanceFilter from '@/hooks/useFormCreateAdvanceFilter';
import useReasonConfig from '@/hooks/useReasonConfig';

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
  | FormDesignKeyEnum.PRICE;

export interface FormCreateTableProps {
  formKey: FormKey;
  disabledSelection?: (row: any) => boolean;
  operationColumn?: CrmDataTableColumn;
  specialRender?: Record<string, (row: any) => void>;
  showPagination?: boolean;
  excludeFieldIds?: string[]; // 规避某些字段的文字替换
  permission?: string[]; // 表格勾选的权限
  readonly?: boolean;
  radio?: boolean; // 是否单选
  containerClass: string; // 容器元素类名
  hiddenTotal?: Ref<boolean>;
  opportunityStage?: StageConfigItem[]; // 商机阶段筛选项
  hiddenAllScreen?: boolean;
  hiddenRefresh?: boolean;
}

export default async function useFormCreateTable(props: FormCreateTableProps) {
  const { t } = useI18n();
  const { getFilterListConfig, customFieldsFilterConfig } = useFormCreateAdvanceFilter();
  const { reasonOptions, initReasonConfig } = useReasonConfig(props.formKey);
  const loading = ref(false);
  const showPagination = props.showPagination ?? true;
  let columns: CrmDataTableColumn[] = [];
  const fieldList = ref<FormCreateField[]>([]);
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
    [FormDesignKeyEnum.PRICE]: TableKeyEnum.PRICE,
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

  const internalColumnMap: Record<FormKey, CrmDataTableColumn[]> = {
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
        key: 'approvalStatus',
        filterOptions: quotationStatusOptions,
        sortOrder: false,
        sorter: true,
        filter: true,
        render: props.specialRender?.approvalStatus,
      },
      {
        title: t('opportunity.quotation.amount'),
        width: 120,
        key: 'amount',
        sortOrder: false,
        sorter: true,
        ellipsis: {
          tooltip: true,
        },
        render: props.specialRender?.amount,
      },
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
        title: t('opportunity.quotation.amount'),
        width: 120,
        key: 'amount',
        sortOrder: false,
        sorter: true,
      },
      {
        title: t('contract.approvalStatus'),
        width: 120,
        key: 'approvalStatus',
        filterOptions: quotationStatusOptions.filter((item) =>
          [QuotationStatusEnum.APPROVED, QuotationStatusEnum.UNAPPROVED, QuotationStatusEnum.APPROVING].includes(
            item.value
          )
        ),
        sortOrder: false,
        sorter: true,
        filter: true,
        render: props.specialRender?.approvalStatus,
      },
    ],
    [FormDesignKeyEnum.CONTRACT_PAYMENT]: paymentInternalColumns,
    [FormDesignKeyEnum.CONTRACT_CONTRACT_PAYMENT]: paymentInternalColumns,
    [FormDesignKeyEnum.PRICE]: [],
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
            key = field.id;
          }
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
          ];
          if (field.type === FieldTypeEnum.PICTURE) {
            return {
              title: field.name,
              width: 200,
              key,
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
            };
          }
          if (
            (field.businessKey === 'name' &&
              ![FormDesignKeyEnum.CUSTOMER_CONTACT, FormDesignKeyEnum.BUSINESS_CONTACT].includes(props.formKey) &&
              !field.resourceFieldId) ||
            ([FormDesignKeyEnum.CONTRACT_PAYMENT, FormDesignKeyEnum.CONTRACT_CONTRACT_PAYMENT].includes(
              props.formKey
            ) &&
              field.businessKey === 'contractId')
          ) {
            return {
              title: field.name,
              width: 200,
              key,
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
              key,
              fieldId: field.id,
              sortOrder: false,
              sorter,
              filedType: field.type,
              render: props.specialRender?.[field.businessKey],
            };
          }

          if (isFollowModule && field.businessKey === 'opportunityId') {
            return {
              title: field.name,
              width: 200,
              key,
              fieldId: field.id,
              sortOrder: false,
              sorter,
              filedType: field.type,
              ellipsis: {
                tooltip: true,
              },
              render: (row: any) => row.opportunityName ?? '-',
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
              sorter,
              filedType: field.type,
              ellipsis: {
                tooltip: true,
              },
              render: props.specialRender?.[field.businessKey],
            };
          }

          if (field.businessKey === 'owner') {
            return {
              title: field.name,
              width: 200,
              key,
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
              sorter,
              filedType: field.type,
            };
          }
          if (field.type === FieldTypeEnum.INPUT_NUMBER) {
            return {
              title: field.name,
              width: 150,
              key,
              fieldId: field.id,
              render: (row: any) => formatNumberValue(row[key], field),
              sortOrder: false,
              sorter,
              filedType: field.type,
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
    fieldList,
  };
}
