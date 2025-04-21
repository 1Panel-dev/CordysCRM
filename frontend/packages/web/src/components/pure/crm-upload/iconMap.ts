import { UploadAcceptEnum, UploadStatus } from '@lib/shared/enums/uploadEnum';

import type { CrmFileItem } from './types';

// 使用映射类型和索引签名
export type FileIconMapping = {
  [key in keyof typeof UploadAcceptEnum]: any;
};

// TODO 暂时缺少以下类型，需要再上传
// jar: 未上传
// json： 已上传
// jmx： 已上传
// har: 已上传
export const FileIconMap: FileIconMapping = {
  csv: {
    [UploadStatus.init]: 'iconicon_file-CSV_colorful_ash',
    [UploadStatus.done]: 'iconicon_file-CSV_colorful1',
  },
  excel: {
    [UploadStatus.init]: 'iconicon_file-excel_colorful_ash',
    [UploadStatus.done]: 'iconicon_file-excel_colorful',
  },
  image: {
    [UploadStatus.init]: 'iconicon_file-image_colorful_ash',
    [UploadStatus.done]: 'iconicon_file-image_colorful1',
  },
  jar: {
    [UploadStatus.init]: 'icona-icon_file-jar_colorful',
    [UploadStatus.done]: 'icona-icon_file-jar_colorful',
  },
  pdf: {
    [UploadStatus.init]: 'iconicon_file-pdf_colorful_ash',
    [UploadStatus.done]: 'icon-icon_file-pdf_colorful1',
  },
  sql: {
    [UploadStatus.init]: 'iiconicon_file-sql_colorful_ash',
    [UploadStatus.done]: 'iconicon_file-sql_colorful1',
  },
  // TOTO
  plain: {
    [UploadStatus.init]: 'icon-icon_file-text_colorful_ash',
    [UploadStatus.done]: 'icon-icon_file-text_colorful1',
  },
  txt: {
    [UploadStatus.init]: 'iconicon_file-text_colorful_ash',
    [UploadStatus.done]: 'iconicon_file-text_colorful1',
  },
  word: {
    [UploadStatus.init]: 'iconicon_file-word_colorful_ash',
    [UploadStatus.done]: 'iconicon_file-word_colorful1',
  },
  video: {
    [UploadStatus.init]: 'iconicon_file-vedio_colorful_ash',
    [UploadStatus.done]: 'iconicon_file-vedio_colorful1',
  },
  xmind: {
    [UploadStatus.init]: 'iconicon_file-xmind_colorful_ash',
    [UploadStatus.done]: 'iconicon_file-xmind_colorful1',
  },
  zip: {
    [UploadStatus.init]: 'icona-icon_file-compressed_colorful_ash2',
    [UploadStatus.done]: 'icona-icon_file-compressed_colorful1',
  },
  sketch: {
    [UploadStatus.init]: 'iconicon_file-sketch_colorful_ash',
    [UploadStatus.done]: 'iconicon_file-sketch_colorful1',
  },
  ppt: {
    [UploadStatus.init]: 'iconicon_file-ppt_colorful_ash',
    [UploadStatus.done]: 'iconicon_file-ppt_colorful1',
  },
  unknown: {
    [UploadStatus.init]: 'iconicon_file-unknow_colorful1',
    [UploadStatus.done]: 'iconicon_file-unknow_colorful1',
  },
  none: {
    [UploadStatus.init]: 'iconicon_file-unknow_colorful1',
    [UploadStatus.done]: 'iconicon_file-unknow_colorful1',
  },
  json: {
    [UploadStatus.init]: 'icona-icon_file-json',
    [UploadStatus.done]: 'icon-a-icon_file-json',
  },
  jmx: {
    [UploadStatus.init]: 'icona-icon_file-JMX',
    [UploadStatus.done]: 'icon-a-icon_file-JMX',
  },
  har: {
    [UploadStatus.init]: 'iconicon_file_har',
    [UploadStatus.done]: 'icon-icon_file_har',
  },
};

/**
 * 获取文件类型枚举
 * @param fileType 文件类型
 */
export function getFileEnum(fileType?: string): keyof typeof UploadAcceptEnum {
  if (fileType) {
    const keys = Object.keys(UploadAcceptEnum);
    for (let i = 0; i < keys.length; i++) {
      const key = keys[i] as unknown as keyof typeof UploadAcceptEnum;
      if (UploadAcceptEnum[key].includes(fileType)) {
        return key;
      }
    }
  }
  return 'unknown' as keyof typeof UploadAcceptEnum;
}

/**
 * 获取文件图标
 * @param item 文件项
 * @param status 文件状态
 */
export function getFileIcon(item: CrmFileItem, status?: UploadStatus) {
  const fileType = item.file?.name?.split('.').pop(); // 通过文件后缀判断文件类型
  const _status = status || item.status;
  if (_status === UploadStatus.done) {
    return FileIconMap[getFileEnum(fileType)]?.[_status] ?? FileIconMap.unknown[UploadStatus.done];
  }
  return FileIconMap[getFileEnum(fileType)]?.[_status || UploadStatus.init] ?? FileIconMap.unknown[UploadStatus.done];
}
