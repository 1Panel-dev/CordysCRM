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
