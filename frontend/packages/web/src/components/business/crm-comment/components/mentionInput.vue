<template>
  <div class="crm-comment-mention-input">
    <div
      v-if="props.enableAttachment"
      class="flex h-[24px] items-center rounded-[3px_3px_0_0] border border-b-0 border-[var(--text-n7)] px-[8px]"
    >
      <n-tooltip trigger="hover" placement="top-start">
        <template #trigger>
          <n-upload
            v-model:file-list="fileList"
            class="crm-file-input-upload"
            :custom-request="uploadAttachment"
            :show-file-list="false"
            :max="10"
            directory-dnd
            @before-upload="beforeUpload"
            @finish="handleAttachmentFinish"
          >
            <CrmIcon type="iconicon_link1" :size="16" class="text-[var(--text-n4)]" />
          </n-upload>
        </template>
        {{ fileList.length === 10 ? t('crm.approval.fileLimitTip') : t('crmFormDesign.file') }}
      </n-tooltip>
    </div>
    <n-mention
      v-model:value="mentionContent"
      type="textarea"
      :autosize="{ minRows: 3, maxRows: 6 }"
      :disabled="props.disabled"
      :filter="() => true"
      :loading="loading"
      :options="mentionOptions"
      :placeholder="placeholder"
      :render-label="renderMentionLabel"
      :class="{ 'crm-comment-mention-input__mention--with-attachment': props.enableAttachment }"
      to="body"
      @search="handleMentionSearch"
      @select="handleMentionSelect"
      @blur="syncMentionUserIds"
    />
    <CrmFileList
      v-if="props.enableAttachment && fileList.length"
      :files="fileList as unknown as AttachmentInfo[]"
      class="mt-[8px]"
      @delete-file="deleteAttachment"
    />
    <div class="mt-[8px] flex justify-end gap-[8px]">
      <n-button :disabled="props.loading" secondary @click="handleCancel">
        {{ t('common.cancel') }}
      </n-button>
      <n-button
        type="primary"
        :disabled="!content.trim() || hasPendingAttachment"
        :loading="props.loading"
        @click="handleSubmit"
      >
        {{ props.submitText || t('crmComment.reply') }}
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import {
    NButton,
    NCheckbox,
    NMention,
    NTooltip,
    NUpload,
    type UploadCustomRequestOptions,
    type UploadFileInfo,
    useMessage,
  } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { filterMentionUsers, FOLLOW_COMMENT_MAX_LENGTH } from '@lib/shared/method/comment';
  import type { FollowCommentUser } from '@lib/shared/models/follow';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmFileList from '@/components/business/crm-file-list/index.vue';
  import type { AttachmentInfo } from '@/components/business/crm-form-create/types';

  import { getUserOptions, uploadTempAttachment } from '@/api/modules';

  import type { MentionOption as NaiveMentionOption } from 'naive-ui/es/mention/src/interface';

  const { t } = useI18n();
  const message = useMessage();

  interface MentionOption extends NaiveMentionOption {
    id?: string;
    name?: string;
    enable?: boolean;
    value: string;
    label: string;
    isHeader?: boolean;
  }

  const INCLUDE_DISABLED_OPTION_VALUE = '__include_disabled_header__';

  const props = withDefaults(
    defineProps<{
      replyUserName?: string;
      submitText?: string;
      disabled?: boolean;
      loading?: boolean;
      includeDisabled?: boolean;
      initialMentionUsers?: FollowCommentUser[];
      initialAttachments?: AttachmentInfo[];
      enableAttachment?: boolean;
    }>(),
    {
      includeDisabled: false,
    }
  );

  const emit = defineEmits<{
    (event: 'submit', value: { content: string; mentionUserIds: string[]; attachmentIds: string[] }): void;
    (event: 'cancel'): void;
  }>();

  const content = defineModel<string>('value', {
    default: '',
  });

  const mentionContent = computed({
    get: () => content.value,
    set: (value: string) => {
      content.value = value.slice(0, FOLLOW_COMMENT_MAX_LENGTH);
    },
  });

  const loading = ref(false);
  const includeDisabled = ref(props.includeDisabled);
  const rawUserOptions = ref<MentionOption[]>([]);
  const selectedMentionUsers = ref<FollowCommentUser[]>([]);
  const fileList = ref<UploadFileInfo[]>([]);
  const temporaryAttachmentIds = ref<Record<string, string>>({});
  const uploadingAttachmentIds = ref(new Set<string>());
  const hasPendingAttachment = computed(
    () => uploadingAttachmentIds.value.size > 0 || fileList.value.some((file) => file.status === 'error')
  );
  const lastKeyword = ref('');
  const userRequestId = ref(0);
  const hasLoadedUsers = ref(false);
  const loadedIncludeDisabled = ref(false);

  const userOptions = computed(() => {
    const keyword = lastKeyword.value.trim();
    return rawUserOptions.value.filter((user) => {
      if (!includeDisabled.value && user.enable === false) {
        return false;
      }
      return !keyword || user.name?.includes(keyword);
    });
  });

  const mentionOptions = computed<MentionOption[]>(() => [
    {
      value: INCLUDE_DISABLED_OPTION_VALUE,
      label: '',
      isHeader: true,
      disabled: true,
    },
    ...userOptions.value,
  ]);

  const placeholder = computed(() => {
    if (props.replyUserName) {
      return t('crmComment.replyPlaceholder', { name: props.replyUserName });
    }
    return `${t('crmComment.commentPlaceholder')}@${t('crmComment.commentPlaceholderOthers')}`;
  });

  function syncMentionUserIds() {
    selectedMentionUsers.value = filterMentionUsers(content.value, selectedMentionUsers.value);
  }

  async function loadUsers() {
    if (hasLoadedUsers.value && (!includeDisabled.value || loadedIncludeDisabled.value)) {
      return;
    }

    const currentRequestId = userRequestId.value + 1;
    userRequestId.value = currentRequestId;
    const needIncludeDisabled = includeDisabled.value;

    try {
      loading.value = true;
      const users = (await getUserOptions({
        ...(needIncludeDisabled ? { includeDisabled: true } : {}),
      })) as FollowCommentUser[];

      if (currentRequestId !== userRequestId.value) {
        return;
      }

      rawUserOptions.value = users.map((user) => ({
        ...user,
        label: user.name,
        value: user.name,
      }));
      hasLoadedUsers.value = true;
      loadedIncludeDisabled.value = needIncludeDisabled;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      if (currentRequestId === userRequestId.value) {
        loading.value = false;
      }
    }
  }

  function handleMentionSearch(keyword: string) {
    lastKeyword.value = keyword;
    loadUsers();
  }

  function getCurrentMentionKeyword(value: string) {
    const matched = value.match(/(?:^|\s)@([^\s@\n\r]*)$/);
    return matched ? matched[1] : undefined;
  }

  function handleMentionSelect(option: NaiveMentionOption) {
    const user = option as MentionOption;
    if (user.isHeader || !user.id || !user.name) {
      return;
    }
    if (!selectedMentionUsers.value.some((item) => item.id === user.id)) {
      selectedMentionUsers.value.push({
        id: user.id,
        name: user.name,
        enable: user.enable,
      });
    }
  }

  function getMentionUserIds() {
    syncMentionUserIds();
    return selectedMentionUsers.value.map((user) => user.id);
  }

  function handleCancel() {
    content.value = '';
    selectedMentionUsers.value = [];
    fileList.value = [];
    temporaryAttachmentIds.value = {};
    uploadingAttachmentIds.value.clear();
    emit('cancel');
  }

  function resetAttachments() {
    fileList.value = (props.initialAttachments || []).map((attachment) => ({
      ...attachment,
      status: 'finished' as const,
    }));
    temporaryAttachmentIds.value = {};
    uploadingAttachmentIds.value.clear();
  }

  function handleSubmit() {
    const trimmedContent = content.value.trim();
    if (!trimmedContent) {
      return;
    }

    emit('submit', {
      content: trimmedContent,
      mentionUserIds: getMentionUserIds(),
      attachmentIds: fileList.value.map((file) => temporaryAttachmentIds.value[file.id] || file.id),
    });
  }

  function beforeUpload({ file }: { file: UploadFileInfo }) {
    if (fileList.value.some((item) => item.name === file.name)) {
      message.warning(t('crm.upload.repeatFileTip'));
      return false;
    }
    if (file.file?.size && file.file.size > 50 * 1024 * 1024) {
      message.warning(t('crm.upload.overSize', { size: 50, unit: 'MB' }));
      return false;
    }
    return true;
  }

  async function uploadAttachment({ file, onFinish, onError }: UploadCustomRequestOptions) {
    uploadingAttachmentIds.value.add(file.id);
    try {
      const result = await uploadTempAttachment(file.file);
      const [temporaryAttachmentId] = result.data;
      temporaryAttachmentIds.value[file.id] = temporaryAttachmentId;
      onFinish();
    } catch {
      onError();
    } finally {
      uploadingAttachmentIds.value.delete(file.id);
    }
  }

  function handleAttachmentFinish({ file }: { file: UploadFileInfo }) {
    const temporaryId = temporaryAttachmentIds.value[file.id];
    return temporaryId ? { ...file, id: temporaryId } : file;
  }

  function deleteAttachment(fileId: string) {
    fileList.value = fileList.value.filter((file) => file.id !== fileId);
    Object.entries(temporaryAttachmentIds.value).forEach(([localId, temporaryId]) => {
      if (localId === fileId || temporaryId === fileId) {
        delete temporaryAttachmentIds.value[localId];
        uploadingAttachmentIds.value.delete(localId);
      }
    });
  }

  function renderDisabledTag(option: NaiveMentionOption) {
    if ((option as MentionOption).enable !== false) {
      return null;
    }

    return h(
      CrmTag,
      {
        theme: 'light',
        size: 'small',
        tooltipDisabled: true,
        class: 'ml-[8px] shrink-0',
      },
      { default: () => t('common.disabled') }
    );
  }

  function handleIncludeDisabledChange(value: boolean) {
    includeDisabled.value = value;
    loadUsers();
  }

  function renderIncludeDisabledHeader() {
    return h(
      'div',
      {
        class: 'crm-comment-mention-header',
        onMousedown: (event: MouseEvent) => {
          event.preventDefault();
          event.stopPropagation();
        },
        onClick: (event: MouseEvent) => event.stopPropagation(),
      },
      [
        h(
          NCheckbox,
          {
            'checked': includeDisabled.value,
            'onUpdate:checked': handleIncludeDisabledChange,
          },
          {
            default: () => h('span', { class: 'text-[var(--text-n2)]' }, t('common.showDisabledUsers')),
          }
        ),
      ]
    );
  }

  function renderMentionLabel(option: NaiveMentionOption) {
    if ((option as MentionOption).isHeader) {
      return renderIncludeDisabledHeader();
    }

    return h('div', { class: 'flex w-full items-center justify-between' }, [
      h('span', { class: 'one-line-text min-w-0' }, option.label as string),
      renderDisabledTag(option),
    ]);
  }

  watch(content, (value) => {
    const keyword = getCurrentMentionKeyword(value);
    if (keyword === undefined || keyword === lastKeyword.value) {
      return;
    }
    lastKeyword.value = keyword;
    loadUsers();
  });

  watch(
    () => props.initialMentionUsers,
    (users) => {
      selectedMentionUsers.value = users ? [...users] : [];
    },
    { immediate: true }
  );

  watch(
    () => props.initialAttachments,
    () => {
      resetAttachments();
    },
    { immediate: true }
  );
</script>

<style scoped lang="less">
  :deep(.crm-comment-mention-header) {
    @apply flex w-full items-center;
  }
  // 适配 mention 组件宽度
  :global(.n-mention-menu) {
    width: 200px !important;
    min-width: 200px !important;
    max-width: 200px;
  }
  :deep(.n-base-select-menu) {
    .n-base-select-option.n-base-select-option--show-checkmark {
      padding-right: 12px;
    }
  }
  .crm-file-input-upload {
    :deep(.n-upload-trigger) {
      @apply flex cursor-pointer items-center;
    }
  }
  .crm-comment-mention-input__mention--with-attachment {
    :deep(.n-input),
    :deep(.n-input-wrapper) {
      border-top-left-radius: 0;
      border-top-right-radius: 0;
    }
  }
</style>
