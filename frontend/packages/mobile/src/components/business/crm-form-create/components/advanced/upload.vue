<template>
  <van-field :label="props.fieldConfig.showLabel ? props.fieldConfig.name : ''" :required="required">
    <template #input>
      <van-uploader
        v-model="fileList"
        :before-read="handleBeforeRead"
        :before-delete="handleBeforeDelete"
        :after-read="handleAfterRead"
      />
    </template>
  </van-field>
</template>

<script setup lang="ts">
  import { showToast, UploaderFileListItem } from 'vant';

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

  function handleBeforeRead(file: File | File[]) {
    if (fileList.value.length > 0) {
      // 附件上传校验名称重复
      const isRepeat = fileList.value.filter((item) => item.file?.name === (file as File).name).length >= 1;
      if (isRepeat) {
        showToast(t('formCreate.upload.repeatFileTip'));
        return false;
      }
    }
    const maxSize = props.fieldConfig.uploadSizeLimit || 20;
    const _maxSize = maxSize * 1024 * 1024;
    if ((file as File).size > _maxSize) {
      showToast(t('formCreate.advanced.overSize', { size: maxSize }));
      return false;
    }
    if (props.fieldConfig.uploadLimit === 1) {
      // 单文件上传时，清空之前的文件（得放到校验文件大小之后，避免文件大小限制后文件丢失）
      fileList.value = [];
      fileKeys.value = [];
    }
    return true;
  }

  async function handleAfterRead(file: UploaderFileListItem | UploaderFileListItem[]) {
    try {
      if ((file as UploaderFileListItem).file) {
        (file as UploaderFileListItem).status = 'uploading';
        const res = await uploadTempFile((file as UploaderFileListItem).file!);
        fileKeys.value.push(...res.data);
        (file as UploaderFileListItem).status = 'done';
        (file as UploaderFileListItem).content = `${PreviewPictureUrl}/${res.data[0]}`;
        emit('change', fileKeys.value, fileList.value);
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      (file as UploaderFileListItem).status = 'failed';
    }
  }

  function handleBeforeDelete(file: UploaderFileListItem) {
    const index = fileList.value.findIndex((item) => item.content === file.content);
    if (index !== -1) {
      fileKeys.value = fileKeys.value.filter((key) => `${PreviewPictureUrl}/${key}` !== file.content);
    }
  }

  watch(
    () => fileKeys.value,
    () => {
      if (fileKeys.value.length === 0) {
        fileList.value = [];
      } else if (fileList.value.length !== fileKeys.value.length) {
        fileKeys.value.forEach((key) => {
          if (!fileList.value.some((item) => item.content === key)) {
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
