import type {
  AddAgentModuleParams,
  AddAgentParams,
  AgentApplicationScript,
  AgentModuleRenameParams,
  AgentModuleTreeNode,
  AgentRenameParams,
  AgentTableQueryParams,
  ApplicationScriptParams,
  UpdateAgentParams,
} from '../../models/agent';
import type { ModuleDragParams, TableQueryParams } from '../../models/common';
import {
  addAgentUrl,
  agentApplicationUrl,
  agentCollectPageUrl,
  agentCollectUrl,
  agentDeleteUrl,
  agentDetailUrl,
  agentModuleAddUrl,
  agentModuleCountUrl,
  agentModuleDeleteUrl,
  agentModuleMoveUrl,
  agentModuleRenameUrl,
  agentModuleTreeUrl,
  agentOptionUrl,
  agentPageUrl,
  agentScriptUrl,
  agentWorkspaceUrl,
  renameAgentUrl,
  unCollectAgentUrl,
  updateAgentUrl,
} from '../requrls/agent';
import type { CrmTreeNodeData } from '@cordys/web/src/components/pure/crm-tree/type';
import type { CordysAxios } from '@lib/shared/api/http/Axios';

export default function useAgentApi(CDR: CordysAxios) {
  // 智能体模块重命名
  function agentModuleRename(data: AgentModuleRenameParams) {
    return CDR.post({ url: agentModuleRenameUrl, data });
  }

  // 智能体模块移动
  function agentModuleMove(data: ModuleDragParams) {
    return CDR.post({ url: agentModuleMoveUrl, data });
  }

  // 智能体模块删除
  function agentModuleDelete(ids: string[]) {
    return CDR.post({ url: agentModuleDeleteUrl, data: ids });
  }

  // 添加智能体模块
  function agentModuleAdd(data: AddAgentModuleParams) {
    return CDR.post({ url: agentModuleAddUrl, data });
  }

  // 获取智能体模块树
  function getAgentModuleTree() {
    return CDR.get<CrmTreeNodeData<AgentModuleTreeNode>[]>({ url: agentModuleTreeUrl });
  }

  // 获取智能体模块树数量
  function getAgentModuleTreeCount() {
    return CDR.get<Record<string, number>>({ url: agentModuleCountUrl });
  }

  // 更新智能体
  function updateAgent(data: UpdateAgentParams) {
    return CDR.post({ url: updateAgentUrl, data });
  }

  // 智能体重命名
  function agentRename(data: AgentRenameParams) {
    return CDR.post({ url: renameAgentUrl, data });
  }

  // 获取智能体列表
  function getAgentPage(data: AgentTableQueryParams) {
    return CDR.post({ url: agentPageUrl, data });
  }

  // 获取智能体收藏列表
  function getAgentCollectPage(data: TableQueryParams) {
    return CDR.post({ url: agentCollectPageUrl, data });
  }

  // 添加智能体
  function addAgent(data: AddAgentParams) {
    return CDR.post({ url: addAgentUrl, data });
  }

  // 取消收藏智能体
  function unCollectAgent(id: string) {
    return CDR.get({ url: `${unCollectAgentUrl}/${id}` });
  }

  // 获取智能体详情
  function getAgentDetail(id: string) {
    return CDR.get({ url: `${agentDetailUrl}/${id}` });
  }

  // 删除智能体
  function agentDelete(id: string) {
    return CDR.get({ url: `${agentDeleteUrl}/${id}` });
  }

  // 收藏智能体
  function agentCollect(id: string) {
    return CDR.get({ url: `${agentCollectUrl}/${id}` });
  }

  // 获取智能体选项
  function getAgentOptions() {
    return CDR.get({ url: agentOptionUrl });
  }

  // 获取智能体应用
  function agentApplicationOptions(workspaceId: string) {
    return CDR.get<AgentModuleRenameParams[]>({ url: `${agentApplicationUrl}/${workspaceId}` });
  }

  // 获取工作空间
  function agentWorkspaceOptions() {
    return CDR.get<AgentModuleRenameParams[]>({ url: agentWorkspaceUrl });
  }

  // 获取工作空间应用脚本
  function getApplicationScript(data: ApplicationScriptParams) {
    return CDR.post<AgentApplicationScript>({ url: agentScriptUrl, data });
  }

  return {
    agentModuleRename,
    agentModuleMove,
    agentModuleDelete,
    agentModuleAdd,
    getAgentModuleTree,
    getAgentModuleTreeCount,
    updateAgent,
    agentRename,
    getAgentPage,
    getAgentCollectPage,
    addAgent,
    unCollectAgent,
    getAgentDetail,
    agentDelete,
    agentCollect,
    getAgentOptions,
    agentApplicationOptions,
    agentWorkspaceOptions,
    getApplicationScript,
  };
}
