<template>
  <div class="ai-chat-composer relative flex items-start gap-[12px] bg-[var(--text-n10)] p-[16px]">
    <div class="mb-[28px] flex min-w-0 flex-1 flex-col gap-[6px]">
      <!-- 附件 -->
      <div v-if="attachments?.length" class="flex flex-wrap gap-[6px]">
        <div
          v-for="attachment in attachments"
          :key="attachment.id"
          class="inline-flex h-[26px] max-w-[220px] items-center rounded-[4px] border border-[#edf0f2] bg-[#f7f8fa] px-[8px] text-[12px] text-[#1f2329]"
        >
          <span class="min-w-0 overflow-hidden truncate">{{ attachment.name }}</span>
          <span v-if="attachment.size" class="ml-[6px] flex-none text-[#86909c]">
            {{ formatFileSize(attachment.size) }}
          </span>
          <n-button text @click="removeAttachment(attachment.id)"> × </n-button>
        </div>
      </div>

      <n-input
        v-model:value="inputValue"
        class="ai-chat-composer__input"
        type="textarea"
        :autosize="props.autosize"
        :placeholder="props.placeholder || t('aiChat.inputPlaceholder')"
        @keydown="handleKeydown"
      />
    </div>

    <div class="absolute bottom-[16px] left-[16px] right-[16px] flex min-h-[22px] items-center justify-between">
      <div class="flex items-center">
        <n-upload
          v-if="props.allowFile"
          v-model:file-list="uploadFileList"
          :accept="props.accept"
          :custom-request="handleUploadRequest"
          :max="props.maxFiles"
          :multiple="props.multiple"
          :show-file-list="false"
          class="crm-file-input-upload w-fit"
          @before-upload="handleBeforeUpload"
        >
          <CrmIcon type="iconicon_link1" :size="16" />
        </n-upload>

        <n-divider vertical class="!mx-[12px]" />

        <!-- TODO lmy mcp -->

        <span v-if="currentModelName" class="text-[12px] text-[var(--text-n4)]">
          {{ currentModelName }}
        </span>
      </div>

      <!-- TODO lmy icon -->
      <n-button v-if="canStop" type="primary" circle @click="runtime.stop()"> ■ </n-button>
      <n-button
        v-else
        circle
        size="small"
        type="primary"
        :loading="isLoading"
        :disabled="props.disabled || !canSubmit"
        @click="handleSubmit"
      >
        <template #icon>
          <CrmIcon type="iconicon_send" :size="14" color="var(--text-n10)" />
        </template>
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { NButton, NDivider, NInput, NUpload } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { formatFileSize } from '@lib/shared/method';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import { useAiChatRuntime } from '../runtime/useAiChatRuntime';
  import type { AiChatAttachment, AiChatMcp, AiComposerSubmitPayload, AiFileKind } from '../types';
  import type { UploadCustomRequestOptions, UploadFileInfo } from 'naive-ui';

  const props = withDefaults(
    defineProps<{
      placeholder?: string;
      disabled?: boolean;
      allowFile?: boolean;
      accept?: string;
      multiple?: boolean;
      maxFiles?: number;
      uploadFile?: (file: File) => AiChatAttachment | Promise<AiChatAttachment>;
      modelName?: string;
      mcpOptions?: AiChatMcp[];
      submitMode?: 'runtime' | 'emit';
      autosize?: {
        minRows?: number;
        maxRows?: number;
      };
    }>(),
    {
      placeholder: '',
      disabled: false,
      allowFile: true,
      accept: '',
      multiple: true,
      maxFiles: 10,
      modelName: '',
      mcpOptions: () => [],
      submitMode: 'runtime',
      autosize: () => ({
        minRows: 1,
        maxRows: 6,
      }),
    }
  );

  const emit = defineEmits<{
    (e: 'submit', payload: AiComposerSubmitPayload): void;
  }>();

  const { t } = useI18n();
  const runtime = useAiChatRuntime();

  const inputValue = ref(runtime.state.input.value);

  const uploadFileList = ref<UploadFileInfo[]>([]);
  const attachments = computed(() => runtime.state.attachments.value);
  const selectedMcps = computed(() => runtime.state.selectedMcps.value);
  const currentModelName = computed(() => props.modelName || runtime.state.modelName.value);
  const isLoading = computed(() => runtime.state.loading.value);
  const canStop = computed(() => runtime.state.canStop.value);
  const canSubmit = computed(() => inputValue.value.trim().length > 0 || attachments.value.length > 0);

  watch(
    () => props.modelName,
    (value) => {
      if (value) {
        runtime.setModelName(value);
      }
    },
    { immediate: true }
  );

  watch(inputValue, (value) => {
    runtime.setInput(value);
  });
  watch(runtime.state.input, (value) => {
    if (value !== inputValue.value) {
      inputValue.value = value;
    }
  });

  function removeAttachment(attachmentId: string): void {
    const targetAttachment = attachments.value.find((attachment) => attachment.id === attachmentId);
    const uploadFileId = targetAttachment?.metadata?.uploadFileId;

    uploadFileList.value = uploadFileList.value.filter((file) => file.id !== attachmentId && file.id !== uploadFileId);
    runtime.removeAttachment(attachmentId);
  }

  function getFileKind(file: File): AiFileKind {
    if (file.type.startsWith('image/')) {
      return 'image';
    }

    if (file.type.startsWith('audio/')) {
      return 'audio';
    }

    if (file.type.startsWith('video/')) {
      return 'video';
    }

    return 'file';
  }

  function createLocalAttachment(file: File, uploadFileId: string): AiChatAttachment {
    return {
      id: uploadFileId,
      name: file.name,
      mimeType: file.type,
      size: file.size,
      kind: getFileKind(file),
      metadata: {
        file,
        uploadFileId,
      },
    };
  }

  function handleBeforeUpload({ file }: { file: UploadFileInfo }): boolean {
    if (attachments.value.length >= props.maxFiles) {
      return false;
    }

    if (!props.multiple) {
      uploadFileList.value = [];
      runtime.setAttachments([]);
    }

    return Boolean(file.file);
  }

  async function handleUploadRequest({ file, onFinish, onError }: UploadCustomRequestOptions): Promise<void> {
    if (!file.file) {
      onError();
      return;
    }

    try {
      const uploadedAttachment = props.uploadFile
        ? await props.uploadFile(file.file)
        : createLocalAttachment(file.file, file.id);
      const attachment: AiChatAttachment = {
        ...uploadedAttachment,
        metadata: {
          ...uploadedAttachment.metadata,
          uploadFileId: file.id,
        },
      };

      runtime.setAttachments(props.multiple ? [...attachments.value, attachment] : [attachment]);
      onFinish();
    } catch {
      onError();
    }
  }

  /**
   * Composer 只负责输入交互。
   * 真正的消息追加、请求发送、流式更新都交给 Runtime。
   */
  async function handleSubmit(): Promise<void> {
    if (props.disabled || !canSubmit.value) {
      return;
    }

    if (props.submitMode === 'emit') {
      emit('submit', {
        content: inputValue.value,
        attachments: [...attachments.value],
        options: {
          model: currentModelName.value,
          mcps: [...selectedMcps.value],
        },
      });
    } else {
      await runtime.submit({
        content: inputValue.value,
        attachments: [...attachments.value],
        options: {
          model: currentModelName.value,
          mcps: selectedMcps.value,
        },
      });
    }
  }

  async function handleKeydown(event: KeyboardEvent): Promise<void> {
    if (event.key !== 'Enter' || event.shiftKey || event.metaKey || event.ctrlKey) {
      return;
    }

    event.preventDefault();
    await handleSubmit();
  }
</script>

<style scoped lang="scss">
  .ai-chat-composer {
    box-shadow: 0 4px 15px 2px #6467671a;
  }
  .ai-chat-composer__input {
    min-width: 0;
    :deep(.n-input) {
      background: transparent;
    }
    :deep(.n-input-wrapper) {
      padding: 0;
    }
    :deep(.n-input__border),
    :deep(.n-input__state-border) {
      display: none;
    }
  }
  .crm-file-input-upload {
    :deep(.n-upload-trigger) {
      @apply flex cursor-pointer items-center;
    }
  }
</style>
