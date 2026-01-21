<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :path="props.path"
    :rule="props.fieldConfig.rules"
    :required="props.fieldConfig.rules.some((rule) => rule.key === 'required')"
  >
    <template #label>
      <div v-if="props.fieldConfig.showLabel" class="flex h-[22px] items-center gap-[4px] whitespace-nowrap">
        <div class="one-line-text">{{ props.fieldConfig.name }}</div>
      </div>
      <div v-else class="h-[22px]"></div>
    </template>
    <div
      v-if="props.fieldConfig.description"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <n-upload
      v-model:file-list="fileList"
      :max="props.fieldConfig.onlyOne ? 1 : 10"
      :accept="props.fieldConfig.accept || '*/*'"
      :custom-request="customRequest"
      :disabled="props.fieldConfig.editable === false || !!props.fieldConfig.resourceFieldId"
      list-type="image"
      multiple
      directory-dnd
      @before-upload="beforeUpload"
      @update-file-list="handleFileListChange"
    >
      <n-upload-dragger>
        <div class="flex flex-col items-center justify-center p-[24px]">
          <div class="flex h-[48px] w-[48px] items-center justify-center rounded-full bg-[var(--text-n10)]">
            <CrmSvg name="uploadfile" width="32px" height="32px" />
          </div>
          <div class="mt-[16px]">
            {{ t('crm.upload.importModalDragText') }}
          </div>
          <div class="text-[12px] text-[var(--text-n4)]">
            {{
              `${t('crmFormCreate.advanced.uploadFileTip', {
                type: props.fieldConfig.accept || t('crmFormCreate.advanced.anyType'),
                size: props.fieldConfig.limitSize || '20MB',
              })}${t('crmFormCreate.advanced.limitCount', { count: props.fieldConfig.onlyOne ? 1 : 10 })}`
            }}
          </div>
        </div>
      </n-upload-dragger>
    </n-upload>
  </n-form-item>
</template>

<script setup lang="ts">
  import {
    NFormItem,
    NUpload,
    NUploadDragger,
    UploadCustomRequestOptions,
    UploadFileInfo,
    UploadSettledFileInfo,
    useMessage,
  } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmSvg from '@/components/pure/crm-svg/index.vue';

  import { uploadTempAttachment } from '@/api/modules';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string[], files: UploadFileInfo[]): void;
  }>();

  const { t } = useI18n();
  const Message = useMessage();

  const fileKeys = defineModel<string[]>('value', {
    default: [],
  });
  const fileList = ref<UploadFileInfo[]>([]);
  const fileKeysMap = ref<Record<string, string>>({});

  async function beforeUpload({
    file,
  }: {
    file: UploadSettledFileInfo;
    fileList: UploadSettledFileInfo[];
  }): Promise<boolean> {
    if (fileList.value.length > 0) {
      // 附件上传校验名称重复
      const isRepeat = fileList.value.filter((item) => item.name === file.name).length >= 1;
      if (isRepeat) {
        Message.warning(t('crm.upload.repeatFileTip'));
        return Promise.resolve(false);
      }
    }
    if (props.fieldConfig.accept) {
      const acceptedTypes = props.fieldConfig.accept.split(',').map((type) => type.trim().replace('.', ''));
      const fileExtension = file.name.split('.').pop();
      if ((file.type && !acceptedTypes.includes(fileExtension || '')) || !file.type) {
        Message.warning(t('crmFormCreate.advanced.typeNotValid'));
        return Promise.resolve(false);
      }
    }
    const maxSize = parseInt(props.fieldConfig.limitSize || '', 10) || 20;
    const _maxSize = props.fieldConfig.limitSize?.includes('KB') ? maxSize * 1024 : maxSize * 1024 * 1024;
    if (file.file && file.file.size > _maxSize) {
      Message.warning(
        t('crmFormCreate.advanced.overSize', {
          size: props.fieldConfig.limitSize || '20MB',
        })
      );
      return Promise.resolve(false);
    }
    if (props.fieldConfig.onlyOne) {
      // 单文件上传时，清空之前的文件（得放到校验文件大小之后，避免文件大小限制后文件丢失）
      fileList.value = [];
      fileKeys.value = [];
    }
    return Promise.resolve(true);
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
      fileKeys.value.push(...res.data);
      [fileKeysMap.value[file.id]] = res.data;
      emit('change', fileKeys.value, fileList.value);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
      clearInterval(timer as unknown as number);
      file.status = 'error';
      onError();
    }
  }

  function handleFileListChange(files: UploadFileInfo[]) {
    if (fileKeys.value.length > files.length) {
      fileKeys.value = fileKeys.value.filter((key) => files.some((file) => file.id === key));
      emit('change', fileKeys.value, fileList.value);
    }
  }

  watch(
    () => props.fieldConfig.initialOptions,
    (arr) => {
      fileList.value =
        (arr?.map((e) => ({
          ...e,
          status: 'finished',
        })) as UploadFileInfo[]) || [];
      fileKeysMap.value =
        arr?.reduce((acc, cur) => {
          acc[cur.id] = cur.id;
          return acc;
        }, {} as Record<string, string>) || {};
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less" scoped>
  .n-upload-dragger {
    @apply p-0;
  }
  :deep(.n-upload-file-info__thumbnail) {
    background-color: var(--text-n9);
  }
  :deep(.n-upload-file--image-type) {
    border: 1px solid var(--text-n8);
  }
  :deep(.n-upload-file-list) {
    @apply flex flex-col;

    gap: 8px;
  }
</style>
