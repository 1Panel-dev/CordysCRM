<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :show-label="props.fieldConfig.showLabel"
    :path="props.path"
    :rule="props.fieldConfig.rules"
  >
    <div v-if="props.fieldConfig.description" class="n-form-item-desc" v-html="props.fieldConfig.description"></div>
    <n-upload
      v-model:file-list="fileList"
      :max="props.fieldConfig.uploadLimit"
      :accept="props.fieldConfig.type === FieldTypeEnum.PICTURE ? 'image/*' : '*/*'"
      multiple
      directory-dnd
      @before-upload="beforeUpload"
    >
      <n-upload-dragger>
        <div class="flex items-center gap-[8px] px-[8px] py-[4px]">
          <CrmIcon type="iconicon_add" :size="16" class="text-[var(--primary-8)]" />
          <div class="text-[var(--text-n4)]">
            {{ t('crmFormCreate.advanced.uploadTip', { size: props.fieldConfig.uploadSizeLimit }) }}
          </div>
        </div>
      </n-upload-dragger>
    </n-upload>
  </n-form-item>
</template>

<script setup lang="ts">
  import { NFormItem, NUpload, NUploadDragger, UploadFileInfo, UploadSettledFileInfo, useMessage } from 'naive-ui';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import { FieldTypeEnum } from '@/components/business/crm-form-create/enum';

  import { useI18n } from '@/hooks/useI18n';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    path: string;
  }>();

  const { t } = useI18n();
  const Message = useMessage();

  const fileList = defineModel<UploadFileInfo[]>('value', {
    default: [],
  });

  async function beforeUpload({
    file,
  }: {
    file: UploadSettledFileInfo;
    fileList: UploadSettledFileInfo[];
  }): Promise<boolean> {
    const maxSize = props.fieldConfig.uploadSizeLimit || 20;
    const _maxSize = maxSize * 1024 * 1024;
    if (file.file && file.file.size > _maxSize) {
      Message.warning(t('crmFormCreate.advanced.overSize', { size: maxSize }));
      return Promise.resolve(false);
    }
    return Promise.resolve(true);
  }
</script>

<style lang="less" scoped>
  .n-upload-dragger {
    @apply p-0;
  }
</style>
