import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';

import type { FilterFormItem } from '@/components/pure/crm-advance-filter/type';

import { baseFilterConfigList } from '@/config/clue';
import { contractPaymentPlanStatusOptions } from '@/config/contract';
import { quotationStatus } from '@/config/opportunity';

const { t } = useI18n();

type FilterOptions = NonNullable<FilterFormItem['selectProps']>['options'];

type ModuleFilterOptions = {
  approvalStatusOptions?: FilterOptions;
  stageOptions?: FilterOptions;
  failureReasonOptions?: FilterOptions;
  quotationStatusOptions?: FilterOptions;
  paymentPlanStatusOptions?: FilterOptions;
  recycleReasonOptions?: FilterOptions;
  selectionOperators?: FilterFormItem['operatorOption'];
};

function departmentFilterConfig(): FilterFormItem {
  return {
    title: t('opportunity.department'),
    dataIndex: 'departmentId',
    type: FieldTypeEnum.TREE_SELECT,
    treeSelectProps: {
      labelField: 'name',
      keyField: 'id',
      multiple: true,
      clearFilterAfterSelect: false,
      checkable: true,
      showContainChildModule: true,
    },
  };
}

function approvalStatusFilterConfig(title = t('common.approvalStatus'), options?: FilterOptions): FilterFormItem {
  return {
    title,
    dataIndex: 'approvalStatus',
    type: FieldTypeEnum.SELECT_MULTIPLE,
    selectProps: { options },
  };
}

function auditFilterConfigs(): FilterFormItem[] {
  return baseFilterConfigList.map((item) => ({ ...item }));
}

function followFilterConfigs(): FilterFormItem[] {
  return [
    { title: t('customer.lastFollowUps'), dataIndex: 'follower', type: FieldTypeEnum.USER_SELECT },
    { title: t('customer.lastFollowUpDate'), dataIndex: 'followTime', type: FieldTypeEnum.TIME_RANGE_PICKER },
  ];
}

function moduleFilterConfigs(module: string, options: ModuleFilterOptions = {}): FilterFormItem[] {
  const approvalOptions = options.approvalStatusOptions;
  const department = departmentFilterConfig();
  const audit = auditFilterConfigs();
  const approval = approvalOptions ? [approvalStatusFilterConfig(undefined, approvalOptions)] : [];
  const { selectionOperators } = options;

  switch (module) {
    case 'customer':
      return [...approval, department, ...followFilterConfigs(), ...audit];
    case 'contact':
      return [department, ...audit];
    case 'customerPool':
      return [
        ...approval,
        {
          title: t('customer.recycleReason'),
          dataIndex: 'reasonId',
          type: FieldTypeEnum.SELECT_MULTIPLE,
          selectProps: { options: options.recycleReasonOptions },
        },
        ...audit,
      ];
    case 'opportunity':
      return [
        ...approval,
        {
          title: t('opportunity.opportunityStage'),
          dataIndex: 'stage',
          type: FieldTypeEnum.SELECT_MULTIPLE,
          selectProps: { options: options.stageOptions },
        },
        {
          title: t('opportunity.failureReason'),
          dataIndex: 'failureReason',
          type: FieldTypeEnum.SELECT_MULTIPLE,
          selectProps: { options: options.failureReasonOptions },
        },
        department,
        ...followFilterConfigs(),
        { title: t('opportunity.actualEndTime'), dataIndex: 'actualEndTime', type: FieldTypeEnum.TIME_RANGE_PICKER },
        ...audit,
      ];
    case 'quotation':
      return [
        ...approval,
        {
          title: t('common.status'),
          dataIndex: 'invalid',
          type: FieldTypeEnum.SELECT_MULTIPLE,
          selectProps: { options: options.quotationStatusOptions ?? (quotationStatus as unknown as FilterOptions) },
        },
        department,
        ...audit,
      ];
    case 'contract':
      return [
        department,
        {
          title: t('contract.status'),
          dataIndex: 'stage',
          type: FieldTypeEnum.SELECT_MULTIPLE,
          operatorOption: selectionOperators,
          selectProps: { options: options.stageOptions },
        },
        { title: t('contract.voidReason'), dataIndex: 'voidReason', type: FieldTypeEnum.INPUT },
        ...(approvalOptions
          ? [
              {
                ...approvalStatusFilterConfig(t('contract.approvalStatus'), approvalOptions),
                operatorOption: selectionOperators,
              },
            ]
          : []),
        ...audit,
      ];
    case 'paymentPlan':
      return [
        department,
        {
          title: t('contract.planStatus'),
          dataIndex: 'planStatus',
          type: FieldTypeEnum.SELECT_MULTIPLE,
          operatorOption: selectionOperators,
          selectProps: { options: options.paymentPlanStatusOptions ?? contractPaymentPlanStatusOptions },
        },
        ...audit,
      ];
    case 'paymentRecord':
      return [department, ...audit];
    case 'invoice':
      return [
        department,
        ...(approvalOptions
          ? [
              {
                ...approvalStatusFilterConfig(t('contract.approvalStatus'), approvalOptions),
                operatorOption: selectionOperators,
              },
            ]
          : []),
        ...audit,
      ];
    case 'follow':
      return [
        ...audit,
        {
          dataIndex: 'followResult',
          title: 'system.message.settings.conditionField.followResult',
          type: FieldTypeEnum.INPUT,
        },
        { dataIndex: 'status', title: 'system.message.settings.conditionField.status', type: FieldTypeEnum.INPUT },
      ];
    case 'clue':
      return [
        ...approval,
        department,
        ...followFilterConfigs(),
        { title: t('customer.collectionTime'), dataIndex: 'collectionTime', type: FieldTypeEnum.TIME_RANGE_PICKER },
        ...audit,
      ];
    case 'order':
      return [
        department,
        {
          title: t('order.status'),
          dataIndex: 'stage',
          type: FieldTypeEnum.SELECT_MULTIPLE,
          selectProps: { options: options.stageOptions },
        },
        ...approval,
        ...audit,
      ];
    default:
      return [...approval, ...audit];
  }
}

export function getModuleFilterConfigList(module: string, options?: ModuleFilterOptions) {
  return moduleFilterConfigs(module, options);
}

export function getNotificationTriggerFilterConfigList(
  categoryType: string,
  options?: ModuleFilterOptions
): FilterFormItem[] {
  const normalizedType = categoryType.toUpperCase();
  const categoryMap: Record<string, string> = {
    CUSTOMER: 'customer',
    CUSTOMER_CONTACT: 'contact',
    OPPORTUNITY: 'opportunity',
    BUSINESS: 'opportunity',
    BUSINESS_QUOTATION: 'quotation',
    OPPORTUNITY_QUOTATION: 'quotation',
    QUOTATION: 'quotation',
    CONTRACT: 'contract',
    CONTRACT_PAYMENT_RECORD: 'paymentRecord',
    CONTRACT_RECORD: 'paymentRecord',
    CONTRACT_PAYMENT_PLAN: 'paymentPlan',
    CONTRACT_PAYMENT: 'paymentPlan',
    CONTRACT_INVOICE: 'invoice',
    FOLLOW_UP: 'follow',
    FOLLOW_RECORD: 'follow',
    FOLLOW_UP_RECORD: 'follow',
    FOLLOW_PLAN: 'follow',
    FOLLOW_UP_PLAN: 'follow',
    CLUE: 'clue',
    ORDER: 'order',
  };
  return moduleFilterConfigs(categoryMap[normalizedType] ?? normalizedType.toLowerCase(), options);
}
