import type { TableQueryParams } from './common';

export interface AgentModuleRenameParams {
  id: string;
  name: string;
}

export interface AgentRenameParams {
  id: string;
  name: string;
  agentModuleId: string;
}

export interface AddAgentModuleParams {
  name: string;
  parentId: string;
}

export interface AgentModuleTreeNode {
  id: string;
  name: string;
  parentId: string;
  organizationId: string;
  children: AgentModuleTreeNode[];
}

export interface AddAgentParams {
  name: string;
  agentModuleId: string;
  scopeIds: string[];
  script: string;
  type: string; // 添加方式
  workspaceId: string; // 工作空间
  applicationId: string; // 对应工作空间应用id
  description: string;
}

export interface UpdateAgentParams extends AddAgentParams {
  id: string;
}

export interface AgentTableQueryParams extends TableQueryParams {
  agentModuleIds: string[];
}

export interface AgentMember {
  id: string;
  scope: string;
  name: string;
}
export interface AgentDetail {
  id: string;
  name: string;
  agentModuleId: string;
  agentModuleName: string;
  scopeId: string;
  members: AgentMember[];
  script: string;
  description: string;
}

export type ApplicationScriptParams = Pick<AddAgentParams, 'applicationId' | 'workspaceId'>;

export interface AgentApplicationScript {
  parameters: { parameter: string; value: string }[];
  src: string;
}
