import type { AiChatAttachment, AiFileKind } from '../types';

const mib = 1024 * 1024;

// AI 对话附件的限制
export const agentChatAttachmentLimits = {
  maxCount: 5,
  maxFileSize: 10 * mib,
  maxTotalSize: 20 * mib,
};

export const agentChatImageMimeTypes = ['image/png', 'image/jpeg', 'image/gif', 'image/webp'];
export const agentChatImageExtensions = ['.png', '.jpg', '.jpeg', '.gif', '.webp'];

// 明确支持的扩展名仅用于避免浏览器 MIME 误判导致误拦截；未知扩展名仍放行给后端按内容识别。
const documentExtensions = [
  '.doc',
  '.docx',
  '.pdf',
  '.xlsx',
  '.pptx',
  '.odt',
  '.ods',
  '.odp',
  '.odg',
  '.rtf',
  '.epub',
];
const textExtensions = ['.txt', '.md', '.csv', '.json', '.xml', '.html', '.htm'];

// 产品明确暂不支持的类型，前端可以直接拦截，减少无效上传。
const unsupportedExtensions = [
  '.xls',
  '.ppt',
  '.bmp',
  '.heic',
  '.heif',
  '.tif',
  '.tiff',
  '.mp3',
  '.wav',
  '.m4a',
  '.aac',
  '.flac',
  '.ogg',
  '.mp4',
  '.mov',
  '.avi',
  '.mkv',
  '.webm',
  '.zip',
  '.rar',
  '.7z',
  '.tar',
  '.gz',
  '.bz2',
];

// 这些 MIME 可能来自无扩展名或未知扩展名文件；若扩展名已明确支持，会优先放行。
const unsupportedMimeTypes = [
  'application/vnd.ms-excel',
  'application/vnd.ms-powerpoint',
  'application/zip',
  'application/x-zip-compressed',
  'application/x-rar-compressed',
  'application/x-7z-compressed',
  'application/x-tar',
  'application/gzip',
];

const imageMimeTypeSet = new Set(agentChatImageMimeTypes);
const imageExtensionSet = new Set(agentChatImageExtensions);
const supportedExtensionSet = new Set([...agentChatImageExtensions, ...documentExtensions, ...textExtensions]);
const unsupportedMimeTypeSet = new Set(unsupportedMimeTypes);
const unsupportedExtensionSet = new Set(unsupportedExtensions);

export type AgentChatAttachmentValidationError =
  | 'duplicate'
  | 'max-count'
  | 'max-file-size'
  | 'max-total-size'
  | 'unsupported-type';

export interface AgentChatFileValidationResult {
  file: File;
  valid: boolean;
  error?: AgentChatAttachmentValidationError;
}

export function getAgentChatFileExtension(fileName: string): string {
  const extensionIndex = fileName.lastIndexOf('.');

  return extensionIndex >= 0 ? fileName.slice(extensionIndex).toLowerCase() : '';
}

export function isAgentChatImageFile(file: Pick<File, 'name' | 'type'>): boolean {
  const extension = getAgentChatFileExtension(file.name);

  return imageMimeTypeSet.has(file.type) || imageExtensionSet.has(extension);
}

export function getAgentChatFileKind(file: Pick<File, 'name' | 'type'>): AiFileKind {
  return isAgentChatImageFile(file) ? 'image' : 'file';
}

function isExplicitlyUnsupported(file: File): boolean {
  const extension = getAgentChatFileExtension(file.name);

  // 旧版 Office、音视频和压缩包是明确不支持项，无需等待后端识别。
  if (unsupportedExtensionSet.has(extension) || file.type.startsWith('audio/') || file.type.startsWith('video/')) {
    return true;
  }

  // 图片只允许 Pi 可作为原生图片输入的几类格式。
  if (file.type.startsWith('image/') && !imageMimeTypeSet.has(file.type)) {
    return true;
  }

  // 有些浏览器会把 CSV 识别成 Excel，或把 docx/xlsx/pptx/epub 识别成 zip；支持扩展名优先放行。
  if (supportedExtensionSet.has(extension)) {
    return false;
  }

  return unsupportedMimeTypeSet.has(file.type);
}

function isSupportedAgentChatFile(file: File): boolean {
  return !isExplicitlyUnsupported(file);
}

export function validateAgentChatFiles(
  files: File[],
  attachments: Pick<AiChatAttachment, 'name' | 'size'>[]
): AgentChatFileValidationResult[] {
  // 同一次选择可能包含多个文件，逐个累计能尽量保留前面仍满足限制的文件。
  const selectedFiles: File[] = [];
  let totalSize = attachments.reduce((sum, attachment) => sum + (attachment.size ?? 0), 0);

  const results: AgentChatFileValidationResult[] = [];

  for (const file of files) {
    if (
      attachments.some((attachment) => attachment.name === file.name) ||
      selectedFiles.some((item) => item.name === file.name)
    ) {
      results.push({ file, valid: false, error: 'duplicate' });
      continue;
    }

    if (attachments.length + selectedFiles.length >= agentChatAttachmentLimits.maxCount) {
      results.push({ file, valid: false, error: 'max-count' });
      continue;
    }

    if (file.size > agentChatAttachmentLimits.maxFileSize) {
      results.push({ file, valid: false, error: 'max-file-size' });
      continue;
    }

    if (totalSize + file.size > agentChatAttachmentLimits.maxTotalSize) {
      results.push({ file, valid: false, error: 'max-total-size' });
      continue;
    }

    if (!isSupportedAgentChatFile(file)) {
      results.push({ file, valid: false, error: 'unsupported-type' });
      continue;
    }

    selectedFiles.push(file);
    totalSize += file.size;
    results.push({ file, valid: true });
  }

  return results;
}
