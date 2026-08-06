<template>
  <div class="crm-comment-mention-input">
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
      to="body"
      @search="handleMentionSearch"
      @select="handleMentionSelect"
      @blur="syncMentionUserIds"
    />
    <div class="mt-[8px] flex justify-end gap-[8px]">
      <n-button :disabled="props.loading" secondary @click="handleCancel">
        {{ t('common.cancel') }}
      </n-button>
      <n-button type="primary" :disabled="!content.trim()" :loading="props.loading" @click="handleSubmit">
        {{ props.submitText || t('crmComment.reply') }}
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NButton, NCheckbox, NMention } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { FOLLOW_COMMENT_MAX_LENGTH } from '@lib/shared/method/comment';

  import CrmTag from '@/components/pure/crm-tag/index.vue';

  import { getUserOptions } from '@/api/modules';

  import type { MentionOption as NaiveMentionOption } from 'naive-ui/es/mention/src/interface';

  const { t } = useI18n();

  interface MentionUser {
    id: string;
    name: string;
    enable?: boolean;
  }

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
    }>(),
    {
      includeDisabled: false,
    }
  );

  const emit = defineEmits<{
    (event: 'submit', value: { content: string; mentionUserIds: string[] }): void;
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
  const selectedMentionUsers = ref<MentionUser[]>([]);
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
    selectedMentionUsers.value = selectedMentionUsers.value.filter((user) => content.value.includes(`@${user.name}`));
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
      })) as MentionUser[];

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
    emit('cancel');
  }

  function handleSubmit() {
    const trimmedContent = content.value.trim();
    if (!trimmedContent) {
      return;
    }

    emit('submit', {
      content: trimmedContent,
      mentionUserIds: getMentionUserIds(),
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
</style>
