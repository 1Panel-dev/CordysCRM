export interface AiModelParams {
  temperature?: number;
  max_tokens?: number;
  top_p?: number;
}

export interface AiModelSaveParams {
  id?: string;
  displayName: string;
  modelName?: string;
  provider: string;
  apiUrl?: string;
  apiKey?: string;
  enable: boolean;
  globalDailyLimit: number;
  userDailyLimit: number;
  modelParams?: string;
}

export interface AiModelItem extends AiModelSaveParams {
  id: string;
  updateUserName: string;
  createUserName: string;
  createTime: number;
  updateTime: number;
}

export interface AiModelStatusParams {
  id: string;
}

export interface AiModelRouteStrategy {
  defaultModelId?: string;
  insightModelId?: string;
  classifyModelId?: string;
  autoFallback: boolean;
}
