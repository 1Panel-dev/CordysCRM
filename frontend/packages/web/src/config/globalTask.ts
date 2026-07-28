import { useI18n } from '@lib/shared/hooks/useI18n';
import { AgentTaskConfirmationLevelEnum, AgentTaskTriggerTypeEnum } from '@lib/shared/models/system/agentTask';

import type { SelectOption } from 'naive-ui';

const { t } = useI18n();

export const triggerTypeOptions: SelectOption[] = [
  {
    label: t('system.business.globalTask.triggerTypeEvent'),
    value: AgentTaskTriggerTypeEnum.MANUAL,
  },
  {
    label: t('system.business.globalTask.triggerTypeScheduled'),
    value: AgentTaskTriggerTypeEnum.CRON,
  },
];

export const confirmationLevelOptions = [
  {
    tableLabel: t('system.business.globalTask.confirmationManualShort'),
    label: `${t('system.business.globalTask.confirmationManualShort')}（${t(
      'system.business.globalTask.confirmationManualType'
    )}）`,
    description: t('system.business.globalTask.confirmationManualTip'),
    value: AgentTaskConfirmationLevelEnum.ASK,
  },
  {
    tableLabel: t('system.business.globalTask.confirmationAutoShort'),
    label: `${t('system.business.globalTask.confirmationAutoShort')}（${t(
      'system.business.globalTask.confirmationAutoType'
    )}）`,
    description: t('system.business.globalTask.confirmationAutoTip'),
    value: AgentTaskConfirmationLevelEnum.AUTO,
  },
  {
    tableLabel: t('system.business.globalTask.confirmationNotifyShort'),
    label: `${t('system.business.globalTask.confirmationNotifyShort')}（${t(
      'system.business.globalTask.confirmationNotifyType'
    )}）`,
    description: t('system.business.globalTask.confirmationNotifyTip'),
    value: AgentTaskConfirmationLevelEnum.ONLY_ANALYSIS,
  },
];
