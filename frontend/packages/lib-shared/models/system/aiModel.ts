export interface AiModelParams {
  temperature?: number | null;
  max_tokens?: number | null;
  top_p?: number | null;
}

export interface AiModelSaveParams {
  id?: string;
  displayName: string;
  modelName?: string;
  provider: string;
  apiUrl?: string;
  apiKey?: string;
  enable: boolean;
  globalDailyLimit?: number;
  userDailyLimit?: number;
  modelParams?: string;
}

export interface AiModelItem extends AiModelSaveParams {
  id: string;
  dailyTotal: number;
  updateUserName: string;
  createUserName: string;
  createTime: number;
  updateTime: number;
}

export interface AiModelStatusParams {
  id: string;
}

export interface AiModelOption {
  id: string;
  name: string;
  idAsString: string;
}

export interface AiModelRouteStrategy {
  id?: string;
  chatModels: string[];
  taskModels: string[];
  fallback: boolean;
}
