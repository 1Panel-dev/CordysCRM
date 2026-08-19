import type { CommonList, TableQueryParams } from './common';

export interface AgentChatStreamParams {
  message: string;
  conversationId?: string;
  mcpIds?: string[];
  attachments?: string[];
}

export interface SmartFocusParams {
  focus: string;
}

export interface AgentChatStreamOptions {
  signal?: AbortSignal; // 浏览器侧中断连接
  onSession?: (sessionId: string, conversationId?: string) => void;
}

export interface AgentChatCancelParams {
  conversationId: string;
  sessionId: string;
}

export interface AgentChatRunData {
  conversationId: string;
  runId: string;
  userMessageId?: string;
  assistantMessageId?: string;
}

export interface AgentChatConfirmData {
  dialogId: string;
  conversationId?: string;
  orgId?: string;
  sessionId?: string;
  userId?: string;
  items: AgentChatConfirmItem[];
  createdAt?: number;
}

export interface AgentChatConfirmItem {
  prompt: string;
  title: string;
  selectionType: 'SINGLE' | 'MULTIPLE';
  options?: AgentChatConfirmOption[];
}

export interface AgentChatConfirmOption {
  label: string;
  description?: string;
}

export interface AgentChatProgressData {
  schemaVersion?: number;
  sequence: number;
  actionId: string;
  stage: string;
  status: string;
  title: string;
  description?: string;
  timestamp?: number;
  details?: {
    input?: string;
    output?: string;
    [key: string]: unknown;
  };
}

export interface AgentChatDoneData {
  runId?: string;
  output?: number;
  conversationId?: string;
  input?: number;
  assistantMessageId?: string;
  totalTokens?: number; // Tokens 消耗
}

export interface AgentChatStreamEvent {
  type: 'run' | 'progress' | 'chunk' | 'confirm' | 'error' | 'done';
  content?: string;
  conversationId?: string;
  sessionId?: string;
  run?: AgentChatRunData;
  progress?: AgentChatProgressData;
  confirm?: AgentChatConfirmData;
  data?: AgentChatDoneData;
  errorMessage?: string;
  raw?: unknown;
}

export type AgentConversationQueryRequest = TableQueryParams;

export interface AgentConversationItem {
  id: string;
  createUser?: string;
  updateUser?: string;
  createTime?: number;
  updateTime?: number;
  organizationId?: string;
  userId?: string;
  title: string;
}

export type AgentConversationPageResult = CommonList<AgentConversationItem>;

export interface AgentConversationMessage {
  id: string;
  createUser?: string;
  updateUser?: string;
  createTime?: number;
  updateTime?: number;
  content: string;
  inputTokens?: number | null;
  outputTokens?: number | null;
  totalTokens?: number | null;
  organizationId?: string;
  role: 'USER' | 'ASSISTANT';
  conversationId: string;
  runId?: string;
}

export interface AgentConversationDetail {
  messages: AgentConversationMessage[];
  conversation: AgentConversationItem;
}

export interface AgentMcpConfigItem {
  id: string;
  name: string;
  description?: string;
}

export interface AgentActionSuggestionItem {
  id: string;
  organizationId?: string;
  createTime?: number;
  summary?: string;
  createUser?: string;
  content?: string;
  topic?: string;
  userId?: string;
  actions?: string;
  priority?: number;
}

export interface AgentActionApproveItem {
  id: string;
  summary?: string;
  topic?: string;
  createUser?: string;
  type?: string;
  userId?: string;
  organizationId?: string;
  createTime?: number;
  content?: string;
}
