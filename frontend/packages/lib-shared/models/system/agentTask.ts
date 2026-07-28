export enum AgentTaskTriggerTypeEnum {
  MANUAL = 'manual',
  CRON = 'cron',
}

export enum AgentTaskConfirmationLevelEnum {
  ONLY_ANALYSIS = 'only-analysis',
  ASK = 'ask',
  AUTO = 'auto',
}

export interface AgentTaskParams {
  id?: string;
  name: string;
  triggerType: AgentTaskTriggerTypeEnum;
  executionCondition: string;
  executionAction: string;
  confirmationLevel: AgentTaskConfirmationLevelEnum;
  applicableRoles: string;
  applicableModel?: string;
  enable: boolean;
}

export interface AgentTaskItem extends Omit<AgentTaskParams, 'applicableRoles'> {
  id: string;
  applicableRoles: string | string[];
  createUser?: string;
  updateUser?: string;
  createTime?: number;
  updateTime?: number;
  createUserName?: string;
  updateUserName?: string;
}
