<template>
  <van-field
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :required="required"
    :rules="props.fieldConfig.rules as FieldRule[]"
    :disabled="!props.fieldConfig.editable || !!props.fieldConfig.resourceFieldId"
  >
    <template #input>
      <van-uploader
        v-model="fileList"
        :before-read="handleBeforeRead"
        :before-delete="handleBeforeDelete"
        :after-read="handleAfterRead"
        :accept="props.fieldConfig.accept || '*/*'"
        :max-count="props.fieldConfig.onlyOne ? 1 : 10"
        multiple
      >
        <div class="flex items-center gap-[8px]">
          <div class="flex h-[80px] w-[80px] items-center justify-center bg-[var(--text-n9)]">
            <CrmIconFont name="iconicon_add" />
          </div>
          <div class="flex-1 text-[12px] text-[var(--text-n4)]">
            {{
              `${t('formCreate.advanced.uploadFileTip', {
                type: props.fieldConfig.accept || t('formCreate.advanced.anyType'),
                size: props.fieldConfig.limitSize || '20MB',
              })}${t('formCreate.advanced.limitCount', { count: props.fieldConfig.onlyOne ? 1 : 10 })}`
            }}
          </div>
        </div>
      </van-uploader>
    </template>
  </van-field>
</template>

<script setup lang="ts">
  import { FieldRule, showToast, UploaderFileListItem } from 'vant';

  import { PreviewAttachmentUrl } from '@lib/shared/api/requrls/system/module';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import CrmIconFont from '@/components/pure/crm-icon-font/index.vue';

  import { uploadTempAttachment } from '@/api/modules';

  import { FormCreateField } from '@cordys/web/src/components/business/crm-form-create/types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
  }>();
  const emit = defineEmits<{
    (e: 'change', value: string[], files: UploaderFileListItem[]): void;
  }>();

  const { t } = useI18n();

  const fileKeys = defineModel<string[]>('value', {
    default: [],
  });
  const fileList = ref<UploaderFileListItem[]>([]);
  const required = computed(() => props.fieldConfig.rules.some((rule) => rule.key === 'required'));

  function handleBeforeRead(file: File | File[]) {
    if (fileList.value.length > 0) {
      // 附件上传校验名称重复
      const isRepeat =
        fileList.value.filter((item) =>
          Array.isArray(file) ? file.some((e) => e.name === item.file?.name) : item.file?.name === file.name
        ).length >= 1;
      if (isRepeat) {
        showToast(t('formCreate.upload.repeatFileTip'));
        return false;
      }
    }
    const maxSize = parseInt(props.fieldConfig.limitSize || '', 10) || 20;
    const _maxSize = props.fieldConfig.limitSize?.includes('KB') ? maxSize * 1024 : maxSize * 1024 * 1024;
    if (Array.isArray(file) ? file.some((f) => f.size > _maxSize) : file.size > _maxSize) {
      showToast(t('formCreate.advanced.overSize', { size: props.fieldConfig.limitSize || '20' }));
      return false;
    }
    if (props.fieldConfig.onlyOne) {
      // 单文件上传时，清空之前的文件（得放到校验文件大小之后，避免文件大小限制后文件丢失）
      fileList.value = [];
      fileKeys.value = [];
    }
    return true;
  }

  function removeFailedFile(file: UploaderFileListItem) {
    const index = fileList.value.findIndex((item) => item === file || item.file === file.file);
    if (index !== -1) {
      fileList.value.splice(index, 1);
    }
  }

  async function handleAfterRead(file: UploaderFileListItem | UploaderFileListItem[]) {
    if (Array.isArray(file)) {
      await Promise.all(
        file.map(async (item) => {
          if (!item.file) {
            return;
          }

          try {
            item.status = 'uploading';
            const res = await uploadTempAttachment(item.file);
            fileKeys.value.push(...res.data);
            item.status = 'done';
            item.content = `${PreviewAttachmentUrl}/${res.data[0]}`;
          } catch (error) {
            // eslint-disable-next-line no-console
            console.log(error);
            removeFailedFile(item);
          }
        })
      );
      emit('change', fileKeys.value, fileList.value);
    } else {
      try {
        if (file.file) {
          file.status = 'uploading';
          const res = await uploadTempAttachment(file.file!);
          fileKeys.value.push(...res.data);
          file.status = 'done';
          file.content = `${PreviewAttachmentUrl}/${res.data[0]}`;
          emit('change', fileKeys.value, fileList.value);
        }
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
        removeFailedFile(file);
      }
    }
  }

  function handleBeforeDelete(file: UploaderFileListItem) {
    const index = fileList.value.findIndex((item) => item.content === file.content);
    if (index !== -1) {
      fileKeys.value = fileKeys.value.filter((key) => `${PreviewAttachmentUrl}/${key}` !== file.content);
      fileList.value.splice(index, 1);
    }
  }

  watch(
    () => props.fieldConfig.initialOptions,
    (arr) => {
      fileList.value =
        (arr?.map((e) => ({
          ...e,
          status: 'finished',
          url: `${PreviewAttachmentUrl}/${e.id}`,
          content: `${PreviewAttachmentUrl}/${e.id}`,
          isImage: /(jpg|jpeg|png|gif|bmp|webp|svg)$/i.test(e.type),
        })) as UploaderFileListItem[]) || [];
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less" scoped></style>
