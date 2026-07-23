export interface AgentChatStreamParams {
  message: string;
  conversationId?: string;
  mcpNames?: string[];
}

export interface AgentChatStreamOptions {
  signal?: AbortSignal; // 浏览器侧中断连接
  onSession?: (sessionId: string, conversationId?: string) => void;
}

export interface AgentChatCancelParams {
  conversationId: string;
  sessionId: string;
}

export interface AgentChatStreamEvent {
  type: 'session' | 'chunk' | 'error' | 'done';
  content?: string;
  conversationId?: string;
  sessionId?: string;
  errorMessage?: string;
  raw?: unknown;
}
