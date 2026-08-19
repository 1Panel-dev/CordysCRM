<template>
  <CrmSplitPanel class="relative h-full bg-[var(--text-n10)]" :max="0.5" :min="0.25" :default-size="0.25">
    <template #1>
      <div class="flex h-full flex-col overflow-hidden">
        <div class="mb-[8px] flex items-center justify-between gap-[8px] px-[24px] pt-[24px]">
          <CrmSearchInput v-model:value="keyword" :placeholder="t('common.searchByName')" class="flex-1" />
          <n-tooltip trigger="hover" :delay="300">
            <template #trigger>
              <n-button type="primary" ghost class="n-btn-outline-primary p-[8px]" @click="emit('new')">
                <CrmIcon type="iconicon_add" :size="16" />
              </n-button>
            </template>
            {{ t('aiChat.newConversation') }}
          </n-tooltip>
        </div>

        <div class="flex-1 overflow-hidden px-[24px] pb-[24px]">
          <n-empty
            v-if="props.historyItems.length === 0"
            :description="t('aiChat.noConversation')"
            :show-icon="false"
            class="flex h-[38px] flex-col items-center justify-center bg-[var(--text-n9)]"
          />
          <CrmList
            v-show="props.historyItems.length > 0"
            v-model:focus-item-key="focusHistoryId"
            :active-item-key="props.activeHistoryId"
            :data="props.historyItems"
            virtual-scroll-height="100%"
            key-field="id"
            :item-more-actions="getHistoryMoreActions"
            :loading="props.historyLoading"
            :no-more-data="props.historyNoMore"
            item-class="gap-[8px] px-[4px]"
            activeItemClass="bg-[var(--text-n9)]"
            mode="remote"
            @item-click="handleHistoryClick"
            @more-action-select="handleHistoryActionSelect"
            @reach-bottom="emit('historyReachBottom')"
          >
            <template #title="{ item }">
              <n-input
                v-if="editingHistoryId === item.id"
                ref="historyInputInstRef"
                v-model:value="historyForm.title"
                size="small"
                :maxlength="255"
                :placeholder="t('common.pleaseInputToEnter')"
                :loading="savingHistoryId === item.id"
                @blur="handleSaveHistory(item)"
                @keydown="handleHistoryKeyDown(item, $event)"
                @click.stop
                @compositionstart="handleCompositionStart"
                @compositionend="handleCompositionEnd"
              />
              <n-tooltip v-else trigger="hover">
                <template #trigger>
                  <div
                    class="one-line-text"
                    :class="props.activeHistoryId === item.id ? 'text-[var(--primary-8)]' : ''"
                  >
                    {{ item.title }}
                  </div>
                </template>
                {{ item.title }}
              </n-tooltip>
            </template>
          </CrmList>
        </div>
      </div>
    </template>

    <template #2>
      <main class="h-full min-h-0 min-w-0">
        <AiChatProvider :runtime="runtime">
          <AiChatContent>
            <template #composer>
              <AiComposer
                :placeholder="props.placeholder || t('aiChat.inputPlaceholder')"
                :mcp-options="props.mcpOptions"
                @mcp-updated="emit('mcpUpdated')"
              />
            </template>
          </AiChatContent>
        </AiChatProvider>
      </main>
    </template>
  </CrmSplitPanel>
</template>

<script setup lang="ts">
  import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';
  import { InputInst, NButton, NEmpty, NInput, NTooltip } from 'naive-ui';

  import type { AiChatMcp, AiChatRuntime } from '@lib/shared/ai-chat';
  import { AiChatProvider, createAiChatRuntime } from '@lib/shared/ai-chat';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmList from '@/components/pure/crm-list/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import AiChatContent from './components/AiChatContent.vue';
  import AiComposer from './components/AiComposer.vue';

  interface AiChatHistoryItem {
    id: string;
    title: string;
    active?: boolean;
  }

  const props = withDefaults(
    defineProps<{
      runtime?: AiChatRuntime;
      historyItems?: AiChatHistoryItem[];
      activeHistoryId?: string;
      historyLoading?: boolean;
      historyNoMore?: boolean;
      mcpOptions?: AiChatMcp[];
      placeholder?: string;
    }>(),
    {
      historyItems: () => [],
      activeHistoryId: '',
      historyLoading: false,
      historyNoMore: true,
      mcpOptions: () => [],
      placeholder: '',
    }
  );

  const { t } = useI18n();
  const emit = defineEmits<{
    (e: 'new'): void;
    (e: 'searchHistory', keyword: string): void;
    (e: 'historyReachBottom'): void;
    (e: 'historyClick', id: string): void;
    (e: 'historyDelete', id: string): void;
    (e: 'historyRename', id: string, title: string): void;
    (e: 'mcpUpdated'): void;
  }>();

  const keyword = ref('');
  const focusHistoryId = ref('');
  const editingHistoryId = ref('');
  const savingHistoryId = ref('');
  const historyForm = ref({ title: '' });
  const historyInputInstRef = ref<InputInst | null>(null);
  let searchTimer: ReturnType<typeof setTimeout> | undefined;

  const historyMoreActions: ActionsItem[] = [
    {
      label: t('common.rename'),
      key: 'rename',
    },
    {
      label: t('common.delete'),
      key: 'delete',
      danger: true,
    },
  ];

  function getHistoryMoreActions(item: Record<string, unknown>): ActionsItem[] {
    return editingHistoryId.value === item.id ? [] : historyMoreActions;
  }

  function handleHistoryClick(item: Record<string, unknown>): void {
    if (editingHistoryId.value === item.id) {
      return;
    }

    emit('historyClick', String(item.id));
  }

  function editHistory(item: Record<string, unknown>): void {
    editingHistoryId.value = String(item.id);
    historyForm.value.title = String(item.title ?? '');
    focusHistoryId.value = '';
    nextTick(() => {
      historyInputInstRef.value?.focus();
    });
  }

  function cancelEditHistory(): void {
    editingHistoryId.value = '';
    savingHistoryId.value = '';
    historyForm.value.title = '';
  }

  function handleSaveHistory(item: Record<string, unknown>): void {
    const historyId = String(item.id);

    if (editingHistoryId.value !== historyId || savingHistoryId.value === historyId) {
      return;
    }

    const title = historyForm.value.title.trim();

    if (!title || title === String(item.title ?? '')) {
      cancelEditHistory();
      return;
    }

    savingHistoryId.value = historyId;
    emit('historyRename', historyId, title);
  }

  function finishHistoryRename(id: string): void {
    if (editingHistoryId.value === id) {
      cancelEditHistory();
    }
  }

  function resetHistoryRenameLoading(id?: string): void {
    if (!id || savingHistoryId.value === id) {
      savingHistoryId.value = '';
    }
  }

  const isComposing = ref(false);
  function handleCompositionStart(): void {
    isComposing.value = true;
  }

  function handleCompositionEnd(): void {
    isComposing.value = false;
  }

  function handleHistoryKeyDown(item: Record<string, unknown>, event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      if (isComposing.value) return;
      event.stopPropagation();
      event.preventDefault();
      handleSaveHistory(item);
    } else if (event.key === 'Escape') {
      event.stopPropagation();
      event.preventDefault();
      cancelEditHistory();
    }
  }

  function handleHistoryActionSelect(action: ActionsItem, item: Record<string, unknown>): void {
    const historyId = String(item.id);

    switch (action.key) {
      case 'rename':
        editHistory(item);
        break;
      case 'delete':
        emit('historyDelete', historyId);
        break;
      default:
        break;
    }
  }

  const innerRuntime = createAiChatRuntime();
  const runtime = computed(() => props.runtime ?? innerRuntime);

  watch(keyword, (value) => {
    if (searchTimer) {
      clearTimeout(searchTimer);
    }

    searchTimer = setTimeout(() => {
      emit('searchHistory', value.trim());
    }, 300);
  });

  onBeforeUnmount(() => {
    if (searchTimer) {
      clearTimeout(searchTimer);
    }

    if (!props.runtime) {
      runtime.value.clear();
    }
  });

  defineExpose({
    finishHistoryRename,
    resetHistoryRenameLoading,
  });
</script>
