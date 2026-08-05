<template>
  <van-popup v-model:show="show" position="left" :style="{ width: '70%', height: '100%' }">
    <div class="flex h-full flex-col bg-[var(--text-n10)]">
      <div class="flex flex-none items-center gap-[12px] px-[12px] py-[8px]">
        <van-search
          v-model="keyword"
          shape="round"
          :placeholder="t('common.searchByName')"
          class="min-w-0 flex-1 !p-0"
          @update:model-value="handleSearchChange"
        />
        <CrmIcon
          name="iconicon_handwritten_signature"
          width="24px"
          height="24px"
          color="var(--text-n1)"
          @click="handleNew"
        />
      </div>

      <van-list
        :loading="loading"
        :finished="noMore"
        :finished-text="items.length ? t('common.listFinishedTip') : ''"
        class="flex-1 overflow-y-auto"
        @load="emit('reachBottom')"
      >
        <van-empty v-if="items.length === 0 && !loading" :description="t('aiChat.noConversation')" />

        <van-swipe-cell v-for="item in items" :key="item.id">
          <div
            class="px-[16px] py-[12px]"
            :class="{ '!bg-[var(--primary-7)]': activeId === item.id }"
            @click="handleClick(item.id)"
          >
            <div
              class="truncate text-[14px] text-[var(--text-n1)]"
              :class="{ '!text-[var(--primary-8)]': activeId === item.id }"
            >
              {{ item.title }}
            </div>
          </div>
          <template #right>
            <van-button square type="primary" class="h-full" @click="openRename(item)">
              {{ t('aiChat.renameConversation') }}
            </van-button>
            <van-button square type="danger" class="h-full" @click="handleDelete(item.id)">
              {{ t('common.delete') }}
            </van-button>
          </template>
        </van-swipe-cell>
      </van-list>
    </div>
  </van-popup>

  <van-popup v-model:show="showRename" position="right" :style="{ width: '100%', height: '100%' }">
    <div class="flex h-full flex-col bg-[var(--text-n10)]">
      <van-nav-bar :border="false" :title="t('aiChat.renameConversation')" class="ai-mobile-history-rename__header">
        <template #left>
          <CrmIcon name="iconicon_chevron_left" width="24px" height="24px" @click="closeRename" />
        </template>
      </van-nav-bar>

      <div class="flex-1 overflow-y-auto">
        <van-cell-group>
          <van-field
            v-model="renameTitle"
            :label="t('aiChat.conversationTitle')"
            :placeholder="t('aiChat.renameConversationPlaceholder')"
            maxlength="255"
            clearable
            autofocus
            class="ai-mobile-history-rename__field !text-[16px]"
          />
        </van-cell-group>
      </div>

      <div class="p-[16px]">
        <div class="flex items-center gap-[16px]">
          <van-button
            type="default"
            class="crm-button-primary--secondary !rounded-[var(--border-radius-small)] !text-[16px]"
            block
            :disabled="renameSaving"
            @click="closeRename"
          >
            {{ t('common.cancel') }}
          </van-button>
          <van-button
            type="primary"
            class="!rounded-[var(--border-radius-small)] !text-[16px]"
            :loading="renameSaving"
            block
            @click="handleRenameConfirm"
          >
            {{ t('common.confirm') }}
          </van-button>
        </div>
      </div>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
  import { onBeforeUnmount, ref, watch } from 'vue';
  import { debounce } from 'lodash-es';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { AgentConversationItem } from '@lib/shared/models/ai';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  const props = withDefaults(
    defineProps<{
      items?: AgentConversationItem[];
      activeId?: string;
      loading?: boolean;
      noMore?: boolean;
    }>(),
    {
      items: () => [],
      activeId: '',
      loading: false,
      noMore: true,
    }
  );

  const emit = defineEmits<{
    (e: 'search', keyword: string): void;
    (e: 'reachBottom'): void;
    (e: 'click', id: string): void;
    (e: 'delete', id: string): void;
    (e: 'rename', id: string, title: string, done: () => void): void;
    (e: 'new'): void;
  }>();

  const show = defineModel<boolean>('show', { required: true });
  const { t } = useI18n();

  const keyword = ref('');

  const handleSearchChange = debounce(() => {
    emit('search', keyword.value.trim());
  }, 300);

  function handleClick(id: string): void {
    emit('click', id);
    show.value = false;
  }

  function handleDelete(id: string): void {
    emit('delete', id);
  }

  function handleNew(): void {
    emit('new');
  }

  const showRename = ref(false);
  const renameSaving = ref(false);
  const renameId = ref('');
  const renameTitle = ref('');

  function openRename(item: AgentConversationItem): void {
    renameId.value = item.id;
    renameTitle.value = item.title;
    showRename.value = true;
  }

  function closeRename(): void {
    if (renameSaving.value) {
      return;
    }

    showRename.value = false;
  }

  function handleRenameConfirm(): void {
    const title = renameTitle.value.trim();

    if (!renameId.value || !title) {
      return;
    }

    renameSaving.value = true;
    emit('rename', renameId.value, title, () => {
      renameSaving.value = false;
      showRename.value = false;
    });
  }

  watch(show, (value) => {
    if (value) {
      emit('search', keyword.value.trim());
    }
  });

  onBeforeUnmount(() => {
    handleSearchChange.cancel();
  });
</script>

<style scoped lang="less">
  .ai-mobile-history-rename__header {
    height: 48px;
    :deep(.van-nav-bar__left) {
      padding: 0 12px;
    }
  }
  .ai-mobile-history-rename__field {
    :deep(.van-field__label) {
      width: 80px;
    }
  }
</style>
