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
  applicableModel?: string;
  enable: boolean;
}

export interface AgentTaskItem extends AgentTaskParams {
  id: string;
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
  executeTime: number;
  taskId: string;
  taskName: string;
  executeReason: string;
  status: AgentTaskExecutionRecordStatusEnum;
  result: string;
}
