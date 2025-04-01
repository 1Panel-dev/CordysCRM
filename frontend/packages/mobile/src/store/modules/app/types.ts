import type { ModuleNavBaseInfoItem } from '@lib/shared/models/system/module';

export interface AppState {
  pageSize: number;
  showSizePicker: boolean;
  showQuickJumper: boolean;
  orgId: string;
  moduleConfigList: ModuleNavBaseInfoItem[]; // 模块配置列表
}
