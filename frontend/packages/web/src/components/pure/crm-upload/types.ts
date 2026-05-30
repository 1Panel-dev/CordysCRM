import { UploadFileInfo } from 'naive-ui';

import { UploadAcceptEnum, UploadStatus } from '@lib/shared/enums/uploadEnum';

// 上传类型
export type UploadType = keyof typeof UploadAcceptEnum;

// CRM 文件类型
export type CrmFileItem = UploadFileInfo & {
  status?: keyof typeof UploadStatus;
  enable?: boolean; // jar类型文件是否可用
  uploadedTime?: string | number; // 上传完成时间
  errMsg?: string; // 上传失败的错误信息
  delete?: boolean; // 是否删除
  local?: boolean;
  [key: string]: any;
};
