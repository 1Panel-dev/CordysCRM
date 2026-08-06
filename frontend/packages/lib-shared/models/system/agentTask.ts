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

export enum AgentTaskExecutionRecordStatusEnum {
  RUNNING = 'RUNNING',
  COMPLETED = 'COMPLETED',
  STOPPED = 'STOPPED',
}

export interface AgentTaskExecutionRecordItem {
  id: string;
  executionTime: number;
  taskId: string;
  taskName: string;
  triggerReason: string;
  status: AgentTaskExecutionRecordStatusEnum;
  result: string;
  confirmUser: string;
  confirmUserName: string;
}
