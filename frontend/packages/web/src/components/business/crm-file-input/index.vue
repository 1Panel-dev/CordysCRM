<template>
  <div class="crm-file-input-wrapper">
    <div
      class="z-10 flex h-[24px] w-full items-center rounded-[3px_3px_0_0] border border-b-0 border-[var(--text-n7)] bg-[var(--text-n10)] px-[8px]"
    >
      <n-tooltip trigger="hover" placement="top-start">
        <template #trigger>
          <n-upload
            v-model:file-list="fileList"
            :custom-request="customRequest"
            :multiple="props.multiple"
            class="crm-file-input-upload"
            :show-file-list="false"
            :max="10"
            directory-dnd
            @change="({ file, fileList }) => handleFileChange(file as CrmFileItem, fileList as CrmFileItem[])"
            @before-upload="({ file, fileList }) => beforeUpload(file as CrmFileItem, fileList as CrmFileItem[])"
            @update-file-list="handleFileListChange"
          >
            <CrmIcon type="iconicon_link1" :size="16" class="text-[var(--text-n4)]" />
          </n-upload>
        </template>
        {{ fileList.length === 10 ? t('crm.approval.fileLimitTip') : t('crmFormDesign.file') }}
      </n-tooltip>
    </div>
    <n-input
      v-model:value="value"
      type="textarea"
      :maxlength="300"
      :autosize="{
        minRows: 3,
      }"
      :status="valueStatus"
      class="crm-file-input"
      resizable
      clearable
    />
    <div class="flex justify-end text-[var(--text-n4)]">{{ value.length }}/300</div>
    <span v-if="valueStatus === 'error'" class="text-[var(--error-red)]">
      {{ t('common.notNull', { value: props.name }) }}
    </span>
    <n-scrollbar :content-style="{ maxHeight: '400px' }" class="mt-[8px]">
      <CrmFileList
        v-if="fileList.length > 0"
        :files="fileList as unknown as AttachmentInfo[]"
        @deleteFile="handleDeleteFile"
      />
    </n-scrollbar>
  </div>
</template>

<script setup lang="ts">
  import {
    NInput,
    NScrollbar,
    NTooltip,
    NUpload,
    type UploadCustomRequestOptions,
    type UploadFileInfo,
    useMessage,
  } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import { getFileEnum } from '@/components/pure/crm-upload/iconMap';
  import type { CrmFileItem } from '@/components/pure/crm-upload/types';
  import CrmFileList from '../crm-file-list/index.vue';

  import { uploadTempAttachment } from '@/api/modules';

  import type { AttachmentInfo } from '../crm-form-create/types';

  const props = withDefaults(
    defineProps<{
      multiple?: boolean;
      allowRepeat?: boolean;
      maxSize?: number;
      sizeUnit?: 'MB' | 'KB'; // 文件大小单位
      isLimit?: boolean; // 是否限制文件大小
      accept?: string; // 接受的文件类型
      fileTypeTip?: string; // 文件类型不合法提示
      required?: boolean;
      name?: string;
    }>(),
    {
      multiple: true,
      allowRepeat: false,
      sizeUnit: 'MB',
      isLimit: true,
      accept: 'none',
    }
  );
  const emit = defineEmits<{
    (e: 'change', value: string, fileList: UploadFileInfo[]): void;
  }>();

  const { t } = useI18n();
  const Message = useMessage();

  const value = defineModel<string>('value', {
    required: true,
  });
  const fileList = defineModel<UploadFileInfo[]>('fileList', {
    default: [],
  });
  const valueStatus = ref();

  function handleFileChange(file: CrmFileItem, fs: Array<CrmFileItem>) {
    const lastFileList = fs.map((e: any) => {
      return {
        ...e,
        url: URL.createObjectURL(e.file),
        size: e.file.size,
      };
    });
    file.local = true;
    file.url = URL.createObjectURL(file.file as Blob);
    file.size = file.file?.size;
    emit('change', value.value, lastFileList);
  }

  // 判断文件大小
  function isFileSizeValid(file: UploadFileInfo, maxSize: number, sizeUnit: string, isLimit: boolean): boolean {
    if (isLimit && file.file?.size) {
      const maxSizeInBytes = sizeUnit === 'MB' ? maxSize * 1024 * 1024 : maxSize * 1024;
      return file.file.size <= maxSizeInBytes;
    }
    return true;
  }

  // 判断文件类型
  function isFileTypeValid(file: UploadFileInfo, accept?: string): boolean {
    const fileFormatMatch = file.name.match(/\.([a-zA-Z0-9]+)$/);
    const fileFormatType = fileFormatMatch ? fileFormatMatch[1] : 'none';
    return accept === getFileEnum(fileFormatType) || accept === 'none';
  }

  async function beforeUpload(file: CrmFileItem, _fileList: Array<CrmFileItem>) {
    const maxSize = props.maxSize || 50;

    //  附件上传校验名称重复
    if (fileList.value.length > 0) {
      // 附件上传校验名称重复
      const isRepeat = fileList.value.filter((item) => item.name === file.name).length >= 1;
      if (isRepeat) {
        Message.warning(t('crm.upload.repeatFileTip'));
        return false;
      }
    }

    //  校验文件大小
    if (!isFileSizeValid(file, maxSize, props.sizeUnit, props.isLimit)) {
      Message.warning(t('crm.upload.overSize', { size: maxSize, unit: props.sizeUnit }));
      return false; // 文件大小不符合要求，返回 false
    }

    //  单文件上传时清空之前文件
    if (!props.multiple) {
      fileList.value = [];
    }

    //  校验文件类型
    if (!isFileTypeValid(file, props.accept)) {
      Message.error(props.fileTypeTip || t('crm.upload.fileTypeValidate', { type: props.accept }));
      return false;
    }

    return true;
  }

  function handleDeleteFile(fileId: string) {
    fileList.value = fileList.value.filter((file: UploadFileInfo) => file.id !== fileId);
  }

  async function customRequest({ file, onFinish, onError, onProgress }: UploadCustomRequestOptions) {
    let timer: NodeJS.Timeout | null = null;
    try {
      file.status = 'uploading';
      // 模拟上传进度
      let upLoadProgress = 0;
      timer = setInterval(() => {
        if (upLoadProgress < 50) {
          // 进度在0-50%之间较快
          const randomIncrement = Math.floor(Math.random() * 10) + 1; // 随机增加 5-10 的百分比
          upLoadProgress += randomIncrement;
          onProgress({ percent: upLoadProgress });
        } else if (upLoadProgress < 100) {
          // 进度在50%-100%之间较慢
          const randomIncrement = Math.floor(Math.random() * 10) + 1; // 随机增加 1-5 的百分比
          upLoadProgress = Math.min(upLoadProgress + randomIncrement, 99);
          onProgress({ percent: upLoadProgress });
        }
      }, 100); // 定时器间隔为 100 毫秒
      const res = await uploadTempAttachment(file.file);
      onProgress({ percent: 100 });
      clearInterval(timer as unknown as number);
      onFinish();
      fileList.value = fileList.value.map((f) => {
        if (f.id === file.id) {
          return {
            ...f,
            id: res.data[0],
            local: false,
          };
        }
        return f;
      });
      emit('change', value.value, fileList.value);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
      clearInterval(timer as unknown as number);
      file.status = 'error';
      onError();
    }
  }

  function validate() {
    if (props.required && value.value.trim() === '') {
      valueStatus.value = 'error';
      return false;
    }
    valueStatus.value = '';
    return true;
  }

  function handleFileListChange(files: UploadFileInfo[]) {
    emit('change', value.value, files);
  }

  defineExpose({
    validate,
  });
</script>

<style lang="less" scoped>
  .crm-file-input-wrapper {
    @apply flex w-full flex-col;
    .crm-file-input-upload {
      :deep(.n-upload-trigger) {
        @apply flex cursor-pointer items-center;
      }
    }
    .crm-file-input {
      border-radius: 0 0 3px 3px;
    }
    :deep(.n-input--focus) {
      box-shadow: none;
    }
  }
</style>
