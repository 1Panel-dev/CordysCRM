import type { ModuleNavBaseInfoItem } from '@lib/shared/models/system/module';
import type { MessageInfo } from '@lib/shared/models/user';

export interface AppState {
  pageSize: number;
  showSizePicker: boolean;
  showQuickJumper: boolean;
  orgId: string;
  moduleConfigList: ModuleNavBaseInfoItem[]; // 模块配置列表
  messageInfo: MessageInfo; // 消息通知和公告
  eventSource: null | EventSource; // 事件流资源
  cacheRoutes: Set<string>; // 缓存路由列表
}
