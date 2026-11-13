<template>
  <van-field
    :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''"
    :required="required"
    :rules="uploadRules"
    :disabled="!props.fieldConfig.editable || !!props.fieldConfig.resourceFieldId"
  >
    <template #input>
      <van-uploader
        v-model="fileList"
        :before-read="handleBeforeRead"
        :before-delete="handleBeforeDelete"
        :after-read="handleAfterRead"
        :max-count="props.fieldConfig.uploadLimit || 10"
        multiple
      />
    </template>
  </van-field>
</template>

<script setup lang="ts">
  import { FieldRule, showToast, UploaderFileListItem } from 'vant';

  import { PreviewPictureUrl } from '@lib/shared/api/requrls/system/module';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { uploadTempFile } from '@/api/modules';

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
  const uploadRules = computed<FieldRule[]>(() => [
    ...(props.fieldConfig.rules as FieldRule[]),
    {
      validator: () => {
        if (fileList.value.some((file) => file.status === 'uploading')) {
          return t('formCreate.advanced.waitForUpload');
        }
        return !fileList.value.some((file) => file.status === 'failed') || t('formCreate.advanced.uploadFailed');
      },
    },
  ]);

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
    const maxSize = props.fieldConfig.uploadSizeLimit || 20;
    const _maxSize = maxSize * 1024 * 1024;
    if (Array.isArray(file) ? file.some((f) => f.size > _maxSize) : file.size > _maxSize) {
      showToast(t('formCreate.advanced.overSize', { size: maxSize }));
      return false;
    }
    return true;
  }

  async function handleAfterRead(file: UploaderFileListItem | UploaderFileListItem[]) {
    if (Array.isArray(file)) {
      await Promise.all(file.map(handleAfterRead));
    } else {
      try {
        if (file.file) {
          file.status = 'uploading';
          const res = await uploadTempFile(file.file!);
          if (!fileList.value.some((item) => item.file === file.file)) return;
          fileKeys.value.push(...res.data);
          file.status = 'done';
          file.content = `${PreviewPictureUrl}/${res.data[0]}`;
          emit('change', fileKeys.value, fileList.value);
        }
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
        file.status = 'failed';
      }
    }
  }

  function handleBeforeDelete(file: UploaderFileListItem) {
    const index = fileList.value.findIndex(
      (item) => item === file || (file.file ? item.file === file.file : !!file.content && item.content === file.content)
    );
    if (index !== -1) {
      fileKeys.value = fileKeys.value.filter((key) => `${PreviewPictureUrl}/${key}` !== file.content);
      fileList.value.splice(index, 1);
    }
  }

  watch(
    () => fileKeys.value,
    () => {
      if (fileKeys.value.length === 0) {
        fileList.value = fileList.value.filter((file) => ['uploading', 'failed'].includes(file.status || ''));
      } else if (fileList.value.length !== fileKeys.value.length) {
        fileKeys.value.forEach((key) => {
          if (!fileList.value.some((item) => item.content === `${PreviewPictureUrl}/${key}`)) {
            fileList.value.push({
              url: `${PreviewPictureUrl}/${key}`,
              content: `${PreviewPictureUrl}/${key}`,
              isImage: true,
            });
          }
        });
      }
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less" scoped></style>
