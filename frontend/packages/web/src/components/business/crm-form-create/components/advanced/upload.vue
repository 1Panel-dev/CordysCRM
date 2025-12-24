<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :path="props.path"
    :rule="props.fieldConfig.rules"
    :show-label="(props.fieldConfig.showLabel && !props.isSubTableRender) || props.isSubTableField"
    :required="props.fieldConfig.rules.some((rule) => rule.key === 'required')"
    :label-placement="props.isSubTableField || props.isSubTableRender ? 'top' : props.formConfig?.labelPos"
    :validation-status="fileKeys.length > 0 ? 'success' : 'error'"
  >
    <template #label>
      <div v-if="props.fieldConfig.showLabel" class="flex h-[22px] items-center gap-[4px] whitespace-nowrap">
        <div class="one-line-text">{{ props.fieldConfig.name }}</div>
        <CrmIcon v-if="props.fieldConfig.resourceFieldId" type="iconicon_correlation" />
      </div>
      <div v-else-if="props.isSubTableField || props.isSubTableRender" class="h-[22px]"></div>
    </template>
    <div
      v-if="props.fieldConfig.description"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <n-divider v-if="props.isSubTableField && !props.isSubTableRender" class="!my-0" />
    <n-upload
      v-model:file-list="fileList"
      :max="props.fieldConfig.uploadLimit || 10"
      :accept="props.fieldConfig.type === FieldTypeEnum.PICTURE ? 'image/*' : '*/*'"
      :list-type="props.fieldConfig.pictureShowType === 'card' ? 'image-card' : 'text'"
      :custom-request="customRequest"
      :disabled="props.fieldConfig.editable === false || !!props.fieldConfig.resourceFieldId"
      :file-list-class="props.isSubTableRender ? 'crm-upload--subtable-file-list' : ''"
      :trigger-style="getTriggerStyle"
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
    NDivider,
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
  import type { FormConfig } from '@lib/shared/models/system/module';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import { uploadTempFile } from '@/api/modules';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
    formConfig?: FormConfig;
    isSubTableField?: boolean; // 是否是子表字段
    isSubTableRender?: boolean; // 是否是子表渲染
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

  const getTriggerStyle = computed(() => {
    if (props.isSubTableField) {
      return {
        width: '32px',
        height: '32px',
      };
    }
    if (props.isSubTableRender) {
      return {
        width: '100px',
        height: '100px',
      };
    }
    return undefined;
  });

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
    if ((file.type && !['jpeg', 'png', 'jpg', 'gif', 'bmp', 'webp'].includes(file.type.split('/')[1])) || !file.type) {
      Message.warning(t('crmFormCreate.advanced.typeNotValid'));
      return Promise.resolve(false);
    }
    const maxSize = props.fieldConfig.uploadSizeLimit || 20;
    const _maxSize = maxSize * 1024 * 1024;
    if (file.file && file.file.size > _maxSize) {
      Message.warning(t('crmFormCreate.advanced.overSize', { size: `${maxSize}MB` }));
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
      // Message.error(t('crm.upload.uploadFail'));
      clearInterval(timer as unknown as number);
      // clear error file
      const index = fileList.value.findIndex((item) => item.id === file.id);
      if (index !== -1) {
        fileList.value.splice(index, 1);
      }
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

  watch(
    () => fileKeys.value,
    (keys: string[]) => {
      if (keys.length === 0) {
        fileList.value = [];
        fileKeysMap.value = {};
      } else if (Object.keys(fileKeysMap.value).length === 0) {
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

<style lang="less">
  .crm-upload--subtable-file-list {
    grid-auto-flow: column;
    grid-template-columns: repeat(auto-fill, 100px) !important; /* 自动填充 100px 宽的列 */
    align-items: start !important;
    .n-upload-file--image-card-type {
      width: 100px !important;
      height: 100px !important;
    }
  }
</style>

<style lang="less" scoped>
  .n-upload-dragger {
    @apply p-0;
  }
</style>
