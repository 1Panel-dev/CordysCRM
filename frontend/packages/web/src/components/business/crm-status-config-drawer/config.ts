import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';

import type { FormItemModel } from '@/components/business/crm-batch-form/types';

import {
  addOpportunityStage,
  addOrderStatus,
  deleteOpportunityStage,
  deleteOrderStatus,
  getOpportunityStageConfig,
  getOrderStatusConfig,
  sortOpportunityStage,
  sortOrderStatus,
  updateOpportunityStage,
  updateOpportunityStageRollback,
  updateOrderStatus,
  updateOrderStatusRollback,
} from '@/api/modules';

import type { StatusApiConfig, StatusBizType, StatusRowItem, StatusStrategyConfig, StatusTextConfig } from './types';

function pick<T extends Record<string, any>>(obj: T, keys: string[]) {
  return keys.reduce((acc, key) => {
    acc[key] = obj[key];
    return acc;
  }, {} as Record<string, any>);
}

function buildDefaultCreateParams(row: StatusRowItem, list: StatusRowItem[], index: number, keys: string[]) {
  return {
    ...pick(row, keys),
    dropPosition: list[index - 1] ? 1 : -1,
    targetId: list[index - 1]?.id || list[index + 1]?.id,
  };
}

function buildDefaultUpdateParams(row: StatusRowItem, keys: string[]) {
  return pick(row, keys);
}

export function useStatusTextConfig(): Record<StatusBizType, StatusTextConfig> {
  const { t } = useI18n();

  return {
    [FormDesignKeyEnum.BUSINESS]: {
      title: t('module.businessManage.businessStepSet'),
      sectionTitle: t('module.businessManage.businessStepConfig'),
      columnTitles: [t('opportunity.stage'), t('opportunity.win'), t('opportunity.stageType')],
      rollbackTitle: t('module.businessManage.businessStepRollbackConfig'),
      switches: [
        {
          key: 'runningStageRollback',
          label: t('crmStatusConfigDrawer.runningStageRollback'),
          tip: t('crmStatusConfigDrawer.runningStageRollbackTip', { name: t('module.businessManagement') }),
        },
        {
          key: 'completedStageRollback',
          label: t('crmStatusConfigDrawer.completedStageRollback'),
          tip: t('crmStatusConfigDrawer.completedStageRollbackTip', { name: t('module.businessManagement') }),
        },
      ],
      stageHasDataTip: t('module.businessManage.stageHasData'),
    },
    [FormDesignKeyEnum.ORDER]: {
      title: t('module.order.stateFlowSet'),
      sectionTitle: t('module.order.stateConfig'),
      columnTitles: [t('module.order.state'), t('module.order.stateType')],
      rollbackTitle: t('module.order.stateBackConfig'),
      switches: [
        {
          key: 'runningStageRollback',
          label: t('crmStatusConfigDrawer.runningStageRollback'),
          tip: t('crmStatusConfigDrawer.runningStageRollbackTip', { name: t('module.order') }),
        },
        {
          key: 'completedStageRollback',
          label: t('crmStatusConfigDrawer.completedStageRollback'),
          tip: t('crmStatusConfigDrawer.completedStageRollbackTip', { name: t('module.order') }),
        },
      ],
      stageHasDataTip: t('module.order.stageHasData'),
    },
  };
}

export function useStatusFormItemModelConfig(): Record<StatusBizType, FormItemModel[]> {
  const { t } = useI18n();

  return {
    [FormDesignKeyEnum.BUSINESS]: [
      {
        path: 'name',
        type: FieldTypeEnum.INPUT,
        formItemClass: 'w-full flex-initial',
        inputProps: { maxlength: 16 },
        rule: [
          { required: true, message: t('common.notNull', { value: '' }) },
          { notRepeat: true, message: t('module.capacitySet.repeatMsg') },
        ],
      },
      {
        path: 'rate',
        type: FieldTypeEnum.INPUT_NUMBER,
        formItemClass: 'w-full flex-initial',
        numberProps: {
          min: 0,
          max: 100,
          precision: 0,
          disabledFunction(row) {
            return row.type === 'END';
          },
        },
        rule: [{ required: true, message: t('common.notNull', { value: t('opportunity.stage') }) }],
      },
      {
        path: 'type',
        type: FieldTypeEnum.SELECT,
        formItemClass: 'w-full flex-initial',
        selectProps: {
          disabledFunction: () => true,
          disabledTooltipFunction: () => t('opportunity.stageTypeDisabledChange'),
          options: [
            { label: t('common.inProgress'), value: 'AFOOT' },
            { label: t('common.complete'), value: 'END' },
          ],
        },
      },
    ],
    [FormDesignKeyEnum.ORDER]: [
      {
        path: 'name',
        type: FieldTypeEnum.INPUT,
        formItemClass: 'w-full flex-initial',
        inputProps: { maxlength: 16 },
        rule: [
          {
            required: true,
            message: t('common.notNull', { value: t('order.status') }),
          },
          { notRepeat: true, message: t('module.capacitySet.repeatMsg') },
        ],
      },
      {
        path: 'type',
        type: FieldTypeEnum.SELECT,
        formItemClass: 'w-full flex-initial',
        selectProps: {
          disabledFunction: () => true,
          disabledTooltipFunction: () => t('opportunity.stageTypeDisabledChange'),
          options: [
            { label: t('common.inProgress'), value: 'AFOOT' },
            { label: t('common.complete'), value: 'END' },
          ],
        },
      },
    ],
  };
}

export function useStatusApiConfig(): Record<StatusBizType, StatusApiConfig> {
  return {
    [FormDesignKeyEnum.BUSINESS]: {
      load: getOpportunityStageConfig,
      create: addOpportunityStage,
      update: updateOpportunityStage,
      remove: deleteOpportunityStage,
      sort: sortOpportunityStage,
      rollback: updateOpportunityStageRollback,
    },
    [FormDesignKeyEnum.ORDER]: {
      load: getOrderStatusConfig,
      create: addOrderStatus,
      update: updateOrderStatus,
      remove: deleteOrderStatus,
      sort: sortOrderStatus,
      rollback: updateOrderStatusRollback,
    },
  };
}

export function useStatusStrategyConfig(): Record<StatusBizType, StatusStrategyConfig> {
  const formItemModelMap = useStatusFormItemModelConfig();

  return {
    [FormDesignKeyEnum.BUSINESS]: {
      formItemModel: formItemModelMap[FormDesignKeyEnum.BUSINESS],
      buildCreateParams: (row, { list, index }) => buildDefaultCreateParams(row, list, index, ['name', 'rate', 'type']),
      buildUpdateParams: (row) => buildDefaultUpdateParams(row, ['id', 'name', 'rate']),
      normalizeItem: (item) => ({
        rate: Number(item.rate),
      }),
    },
    [FormDesignKeyEnum.ORDER]: {
      formItemModel: formItemModelMap[FormDesignKeyEnum.ORDER],
      buildCreateParams: (row, { list, index }) => buildDefaultCreateParams(row, list, index, ['name', 'type']),
      buildUpdateParams: (row) => buildDefaultUpdateParams(row, ['id', 'name']),
    },
  };
}
