<template>
  <CrmSplitPanel class="relative h-full bg-[var(--text-n10)]" :max="0.5" :min="0.25" :default-size="0.25">
    <template #1>
      <div class="flex h-full flex-col overflow-hidden">
        <div class="mb-[8px] flex items-center justify-between gap-[8px] px-[24px] pt-[24px]">
          <CrmSearchInput v-model:value="keyword" :placeholder="t('common.searchByName')" class="flex-1" />
          <n-tooltip trigger="hover" :delay="300">
            <template #trigger>
              <n-button type="primary" ghost class="n-btn-outline-primary p-[8px]">
                <CrmIcon type="iconicon_add" :size="16" />
              </n-button>
            </template>
            {{ t('aiChat.newConversation') }}
          </n-tooltip>
        </div>

        <div class="flex-1 overflow-hidden px-[24px] pb-[24px]">
          <n-empty
            v-if="filteredHistoryItems.length === 0"
            :description="t('aiChat.noConversation')"
            :show-icon="false"
            class="flex h-[38px] flex-col items-center justify-center bg-[var(--text-n9)]"
          />
          <CrmList
            v-show="filteredHistoryItems.length > 0"
            v-model:active-item-key="activeHistoryId"
            v-model:focus-item-key="focusHistoryId"
            :data="filteredHistoryItems"
            virtual-scroll-height="100%"
            key-field="id"
            :item-more-actions="historyMoreActions"
            item-class="gap-[8px] px-[4px]"
            activeItemClass="bg-[var(--text-n9)]"
            mode="static"
            @item-click="handleHistoryClick"
            @more-action-select="handleHistoryActionSelect"
          >
            <template #title="{ item }">
              <n-input
                v-if="renamingHistoryId === item.id"
                v-model:value="renamingHistoryTitle"
                size="small"
                autofocus
                @blur="finishRenameHistory"
                @keydown.enter.stop.prevent="finishRenameHistory"
                @keydown.esc.stop.prevent="cancelRenameHistory"
                @click.stop
              />
              <n-tooltip v-else trigger="hover">
                <template #trigger>
                  <div class="one-line-text" :class="activeHistoryId === item.id ? 'text-[var(--primary-8)]' : ''">
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
                :model-name="props.modelName"
                :mcp-options="props.mcpOptions"
              />
            </template>
          </AiChatContent>
        </AiChatProvider>
      </main>
    </template>
  </CrmSplitPanel>
</template>

<script setup lang="ts">
  import { computed, onBeforeUnmount, ref } from 'vue';
  import { NButton, NEmpty, NInput, NTooltip } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmList from '@/components/pure/crm-list/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import AiChatContent from './components/AiChatContent.vue';
  import AiChatProvider from './components/AiChatProvider.vue';
  import AiComposer from './components/AiComposer.vue';

  import createAiChatRuntime from './runtime/createAiChatRuntime.js';
  import type { AiChatRuntime } from './runtime/types.js';
  import type { AiChatMcp } from './types.js';

  interface AiChatHistoryItem {
    id: string;
    title: string;
    active?: boolean;
  }

  const props = withDefaults(
    defineProps<{
      runtime?: AiChatRuntime;
      historyItems?: AiChatHistoryItem[];
      modelName?: string;
      mcpOptions?: AiChatMcp[];
      placeholder?: string;
    }>(),
    {
      historyItems: () => [],
      modelName: '',
      mcpOptions: () => [],
      placeholder: '',
    }
  );

  const { t } = useI18n();

  const keyword = ref('');
  const activeHistoryId = ref(props.historyItems[0]?.id ?? '');
  const focusHistoryId = ref('');
  const renamingHistoryId = ref('');
  const renamingHistoryTitle = ref('');

  const historyItems = ref<AiChatHistoryItem[]>([...props.historyItems]);
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
  const filteredHistoryItems = computed(() => {
    const value = keyword.value.trim();

    if (!value) {
      return historyItems.value;
    }

    return historyItems.value.filter((item) => item.title.includes(value));
  });

  function handleHistoryClick(item: Record<string, unknown>): void {
    activeHistoryId.value = String(item.id);
  }

  function handleHistoryActionSelect(action: ActionsItem, item: Record<string, unknown>): void {
    const historyId = String(item.id);

    if (action.key === 'rename') {
      renamingHistoryId.value = historyId;
      renamingHistoryTitle.value = String(item.title ?? '');
      return;
    }

    if (action.key === 'delete') {
      historyItems.value = historyItems.value.filter((history) => history.id !== historyId);

      if (activeHistoryId.value === historyId) {
        activeHistoryId.value = historyItems.value[0]?.id ?? '';
      }
    }
  }

  function cancelRenameHistory(): void {
    renamingHistoryId.value = '';
    renamingHistoryTitle.value = '';
  }

  function finishRenameHistory(): void {
    const title = renamingHistoryTitle.value.trim();

    if (renamingHistoryId.value && title) {
      historyItems.value = historyItems.value.map((history) =>
        history.id === renamingHistoryId.value
          ? {
              ...history,
              title,
            }
          : history
      );
    }

    cancelRenameHistory();
  }

  const innerRuntime = createAiChatRuntime({
    initialModelName: props.modelName,
  });
  const runtime = props.runtime ?? innerRuntime;

  onBeforeUnmount(() => {
    if (!props.runtime) {
      runtime.clear();
    }
  });
</script>
