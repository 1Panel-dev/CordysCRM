<template>
  <div class="px-[12px] pb-[12px]">
    <div v-if="isEditing" class="mb-[8px] rounded-[8px] bg-[var(--primary-7)] px-[12px] py-[8px] text-[var(--text-n2)]">
      <div class="flex items-center justify-between">
        <span>{{ t('aiChat.editingMessage') }}</span>
        <CrmIcon
          name="iconicon_close"
          width="16px"
          height="16px"
          color="var(--text-n3)"
          @click="runtime.cancelEditMessage"
        />
      </div>
    </div>

    <div class="overflow-hidden rounded-[30px] bg-[var(--text-n10)] shadow-[0_4px_10px_-1px_#6467671A]">
      <div v-if="!isEditing && attachments.length" class="border-b border-[var(--text-n8)] p-[12px]">
        <AiMobileAttachmentList
          :attachments="attachments"
          removable
          show-add
          @add="showAttachmentPopover = true"
          @remove="removeAttachment"
        />
      </div>
      <div class="flex items-center gap-[8px] p-[16px]">
        <input
          ref="imageInputRef"
          type="file"
          :accept="agentChatImageAccept"
          multiple
          class="hidden"
          @change="handleFileInputChange"
        />
        <input ref="fileInputRef" type="file" multiple class="hidden" @change="handleFileInputChange" />
        <van-popover
          v-if="!isEditing"
          v-model:show="showAttachmentPopover"
          placement="top-start"
          :show-arrow="false"
          :actions="attachmentActions"
          @select="handleAttachmentSelect"
        >
          <template #reference>
            <CrmIcon name="iconicon_link1" width="24px" height="24px" color="var(--text-n1)" />
          </template>
        </van-popover>
        <van-field
          ref="composerFieldRef"
          v-model="composerValue"
          :autosize="{ maxHeight: 140 }"
          type="textarea"
          rows="1"
          :border="false"
          :placeholder="props.placeholder || t('aiChat.inputPlaceholder')"
          class="flex-1 !bg-transparent !p-0"
          @keypress.enter.prevent="handleSubmit"
        />
        <van-button
          v-if="runtime.state.canStop.value"
          class="ai-mobile-composer__button"
          round
          size="mini"
          @click="runtime.stop"
        >
          <span class="block h-[8px] w-[8px] rounded-[2px] bg-[var(--text-n10)]" />
        </van-button>
        <van-button
          v-else
          class="ai-mobile-composer__button"
          :disabled="!canSubmit"
          round
          size="mini"
          @click="handleSubmit"
        >
          <CrmIcon name="iconicon_send" width="16px" height="16px" color="var(--text-n10)" />
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, nextTick, ref, watch } from 'vue';
  import { type PopoverAction, showFailToast } from 'vant';

  import type {
    AgentChatAttachmentValidationError,
    AiChatAttachment,
    AiChatModel,
    AiComposerSubmitPayload,
  } from '@lib/shared/ai-chat';
  import {
    agentChatAttachmentLimits,
    agentChatImageMimeTypes,
    getAgentChatFileKind,
    useAiChatRuntime,
    validateAgentChatFiles,
  } from '@lib/shared/ai-chat';
  import { PreviewPictureUrl } from '@lib/shared/api/requrls/system/module';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import AiMobileAttachmentList from './AiMobileAttachmentList.vue';

  import { uploadAgentChatFile } from '@/api/modules';

  const props = withDefaults(
    defineProps<{
      placeholder?: string;
      submitMode?: 'runtime' | 'emit';
      model?: AiChatModel | null;
    }>(),
    {
      placeholder: '',
      submitMode: 'runtime',
      model: null,
    }
  );

  const emit = defineEmits<{
    (e: 'submit', payload: AiComposerSubmitPayload): void;
  }>();

  const { t } = useI18n();
  const runtime = useAiChatRuntime();

  const composerFieldRef = ref<{ focus: () => void }>();
  const imageInputRef = ref<HTMLInputElement | null>(null);
  const fileInputRef = ref<HTMLInputElement | null>(null);
  const inputValue = ref(runtime.state.input.value);
  const showAttachmentPopover = ref(false);
  const agentChatImageAccept = agentChatImageMimeTypes.join(',');

  const isEditing = computed(() => Boolean(runtime.state.editingMessageId.value));
  const attachments = computed(() => runtime.state.attachments.value);
  const composerValue = computed({
    get: () => (isEditing.value ? runtime.state.editingContent.value : inputValue.value),
    set: (value: string) => {
      if (isEditing.value) {
        runtime.setEditingContent(value);
      } else {
        inputValue.value = value;
      }
    },
  });
  const hasUnavailableAttachment = computed(() =>
    attachments.value.some((attachment) => attachment.status === 'uploading' || attachment.status === 'error')
  );
  const canSubmit = computed(() =>
    isEditing.value
      ? runtime.state.canSubmitEdit.value
      : !runtime.state.loading.value &&
        !hasUnavailableAttachment.value &&
        (inputValue.value.trim().length > 0 || attachments.value.length > 0)
  );
  const attachmentActions = computed<PopoverAction[]>(() => [
    { text: t('aiChat.uploadImage'), key: 'image' },
    { text: t('aiChat.uploadFile'), key: 'file' },
  ]);

  function createLocalAttachment(file: File, status: AiChatAttachment['status'] = 'uploading'): AiChatAttachment {
    const kind = getAgentChatFileKind(file);
    const previewUrl = kind === 'image' ? URL.createObjectURL(file) : undefined;

    return {
      id: `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
      name: file.name,
      mimeType: file.type,
      size: file.size,
      kind,
      status,
      url: previewUrl,
      metadata: {
        file,
        previewUrl,
      },
    };
  }

  function updateAttachment(attachmentId: string, attachment: AiChatAttachment): void {
    runtime.setAttachments(
      attachments.value.map((item) => {
        if (item.id !== attachmentId) {
          return item;
        }

        return attachment;
      })
    );
  }

  function showAttachmentValidationToast(error: AgentChatAttachmentValidationError): void {
    const messages: Record<AgentChatAttachmentValidationError, string> = {
      'duplicate': t('formCreate.upload.repeatFileTip'),
      'max-count': t('aiChat.attachmentMaxCount', { count: agentChatAttachmentLimits.maxCount }),
      'max-file-size': t('formCreate.advanced.overSize', { size: 10 }),
      'max-total-size': t('aiChat.attachmentMaxTotalSize', { size: 20 }),
      'unsupported-type': t('aiChat.attachmentUnsupportedType'),
    };

    showFailToast(messages[error]);
  }

  function toUploadedAttachment(file: File, id: string, previewUrl?: string): AiChatAttachment {
    const kind = getAgentChatFileKind(file);

    return {
      id,
      name: file.name,
      mimeType: file.type,
      size: file.size,
      kind,
      status: 'done',
      url: kind === 'image' ? previewUrl || `${PreviewPictureUrl}/${id}` : undefined,
      metadata: {
        fileId: id,
        previewUrl: kind === 'image' ? previewUrl : undefined,
      },
    };
  }

  async function addFiles(files: FileList | File[] | null | undefined) {
    const validationResults = validateAgentChatFiles(Array.from(files ?? []), attachments.value);
    const validFiles = validationResults.filter((result) => result.valid).map((result) => result.file);
    const validationError = validationResults.find((result) => !result.valid)?.error;

    if (validationError) {
      showAttachmentValidationToast(validationError);
    }

    if (!validFiles.length) {
      return;
    }

    const localAttachments = validFiles.map((file) => createLocalAttachment(file));

    runtime.setAttachments([...attachments.value, ...localAttachments]);

    try {
      const res = await uploadAgentChatFile(validFiles);
      const uploadedAttachments = validFiles.map((file, index) =>
        toUploadedAttachment(file, res.data[index], localAttachments[index].metadata?.previewUrl as string | undefined)
      );

      if (uploadedAttachments.some((attachment) => !attachment.id)) {
        throw new Error('Upload response id is empty');
      }

      localAttachments.forEach((localAttachment, index) => {
        updateAttachment(localAttachment.id, uploadedAttachments[index]);
      });
    } catch {
      localAttachments.forEach((localAttachment) => {
        updateAttachment(localAttachment.id, {
          ...localAttachment,
          status: 'error',
        });
      });
    }
  }

  function revokePreviewUrl(attachment: AiChatAttachment | undefined): void {
    const previewUrl = attachment?.metadata?.previewUrl;

    if (typeof previewUrl === 'string') {
      URL.revokeObjectURL(previewUrl);
    }
  }

  function removeAttachment(attachmentId: string): void {
    const attachment = attachments.value.find((item) => item.id === attachmentId);

    revokePreviewUrl(attachment);
    runtime.removeAttachment(attachmentId);
  }

  function handleAttachmentSelect(action: PopoverAction): void {
    showAttachmentPopover.value = false;

    if (action.key === 'image') {
      imageInputRef.value?.click();
      return;
    }

    fileInputRef.value?.click();
  }

  function resetFileInput(input: HTMLInputElement | null): void {
    if (input) {
      input.value = '';
    }
  }

  async function handleFileInputChange(event: Event) {
    const input = event.target as HTMLInputElement;

    await addFiles(input.files);
    resetFileInput(input);
  }

  async function handleSubmit(): Promise<void> {
    const content = composerValue.value.trim();
    const submitPayload: AiComposerSubmitPayload = {
      content,
      attachments: [...attachments.value],
      options: { model: props.model ?? undefined },
    };

    if ((!content && !attachments.value.length) || runtime.state.loading.value) {
      return;
    }

    if (isEditing.value) {
      await runtime.submitEditMessage();
    } else if (props.submitMode === 'emit') {
      emit('submit', submitPayload);
    } else {
      await runtime.submit(submitPayload);
    }
  }

  watch(inputValue, (value) => {
    runtime.setInput(value);
  });

  watch(runtime.state.input, (value) => {
    if (value !== inputValue.value) {
      inputValue.value = value;
    }
  });

  watch(
    isEditing,
    async (value) => {
      if (!value) {
        return;
      }

      await nextTick();
      composerFieldRef.value?.focus();
    },
    { flush: 'post' }
  );
</script>

<style scoped lang="less">
  .ai-mobile-composer__button {
    width: 24px;
    border: 0;
    color: var(--text-n10);
    background: var(--primary-8);
  }
  :deep(.van-popover__wrapper) {
    display: flex;
    align-items: center;
  }
</style>
