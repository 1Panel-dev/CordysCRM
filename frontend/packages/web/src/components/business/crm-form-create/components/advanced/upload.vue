<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :show-label="props.fieldConfig.showLabel"
    :path="props.path"
    :rule="props.fieldConfig.rules"
  >
    <div
      v-if="props.fieldConfig.description"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <n-upload
      v-model:file-list="fileList"
      :max="props.fieldConfig.uploadLimit || 10"
      :accept="props.fieldConfig.type === FieldTypeEnum.PICTURE ? 'image/*' : '*/*'"
      :list-type="props.fieldConfig.pictureShowType === 'card' ? 'image-card' : 'text'"
      :custom-request="customRequest"
      multiple
      directory-dnd
      @before-upload="beforeUpload"
      @update-file-list="() => emit('change', fileKeys, fileList)"
      @remove="handleFileRemove"
    >
      <n-upload-dragger>
        <div class="flex items-center gap-[8px] px-[8px] py-[4px]">
          <CrmIcon type="iconicon_add" :size="16" class="text-[var(--primary-8)]" />
          <div v-if="props.fieldConfig.pictureShowType === 'list'" class="text-[var(--text-n4)]">
            {{ t('crmFormCreate.advanced.uploadTip', { size: props.fieldConfig.uploadSizeLimit }) }}
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

  import { PreviewPictureUrl } from '@lib/shared/api/requrls/system/module';
  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import { uploadTempFile } from '@/api/modules';

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
    const maxSize = props.fieldConfig.uploadSizeLimit || 20;
    const _maxSize = maxSize * 1024 * 1024;
    if (file.file && file.file.size > _maxSize) {
      Message.warning(t('crmFormCreate.advanced.overSize', { size: maxSize }));
      return Promise.resolve(false);
    }
    if (props.fieldConfig.uploadLimit === 1) {
      // 单文件上传时，清空之前的文件（得放到校验文件大小之后，避免文件大小限制后文件丢失）
      fileList.value = [];
      fileKeys.value = [];
    }
    return Promise.resolve(true);
  }

  async function customRequest({ file, onFinish, onError, onProgress }: UploadCustomRequestOptions) {
    let timer: NodeJS.Timeout | null = null;
    try {
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
      const res = await uploadTempFile(file.file);
      onProgress({ percent: 100 });
      clearInterval(timer as unknown as number);
      onFinish();
      fileKeys.value.push(...res.data);
      [fileKeysMap.value[file.id]] = res.data;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
      Message.error(t('crm.upload.uploadFail'));
      clearInterval(timer as unknown as number);
      onError();
    }
  }

  function handleFileRemove({ file }: { file: UploadSettledFileInfo }) {
    const index = fileList.value.findIndex((item) => item.id === file.id);
    if (index !== -1) {
      fileKeys.value = fileKeys.value.filter((key) => key !== fileKeysMap.value[file.id]);
      delete fileKeysMap.value[file.id];
    }
  }

  onBeforeMount(() => {
    fileList.value = [];
  });

  watch(
    () => fileKeys.value,
    (keys: string[]) => {
      if (Object.keys(fileKeysMap.value).length === 0) {
        keys.forEach((key) => {
          fileKeysMap.value[key] = key;
          fileList.value.push({
            id: key,
            thumbnailUrl: `${PreviewPictureUrl}/${key}`,
            url: `${PreviewPictureUrl}/${key}`,
            name: key,
            status: 'finished',
            type: 'image/*',
          });
        });
      }
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
</style>
