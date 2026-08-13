<template>
  <van-popup
    v-model:show="show"
    class="crm-comment-mention-user-select"
    position="right"
    teleport="body"
    destroy-on-close
    @closed="emit('closed')"
  >
    <div class="crm-comment-mention-user-select-page">
      <div class="crm-comment-mention-user-select-header">
        <CrmIcon name="iconicon_chevron_left" width="24px" height="24px" color="var(--text-n1)" @click="handleCancel" />
        <div class="crm-comment-mention-user-select-title">{{ props.title || '@' }}</div>
        <div class="crm-comment-mention-user-select-header-placeholder"></div>
      </div>

      <div class="crm-comment-mention-user-select-content">
        <van-search
          v-model="keyword"
          shape="round"
          :placeholder="props.searchPlaceholder || t('common.pleaseInput')"
          @search="search"
          @clear="search"
        />

        <div class="crm-comment-mention-user-select-list">
          <CrmSelectList
            ref="crmSelectListRef"
            v-model:value="value"
            v-model:selected-rows="selectedRows"
            v-model:loading="loading"
            :keyword="keyword"
            :list-params="listParams"
            :load-list-api="getUserOptions"
            :transform="transformUser"
            multiple
            no-page-nation
          >
            <template #label="{ item }">
              <div class="crm-comment-mention-user-select-option">
                <span class="crm-comment-mention-user-select-name">{{ item.name }}</span>
                <CrmTag
                  v-if="item.enable === false"
                  :tag="t('crmComment.disabledUser')"
                  bg-color="var(--text-n8)"
                  text-color="var(--text-n4)"
                  class="flex-none"
                />
              </div>
            </template>
          </CrmSelectList>
        </div>
      </div>

      <div class="crm-comment-mention-user-select-footer">
        <van-button
          type="default"
          class="crm-button-primary--secondary !rounded-[var(--border-radius-small)] !text-[16px]"
          :disabled="loading"
          block
          @click="handleCancel"
        >
          {{ t('common.cancel') }}
        </van-button>
        <van-button
          class="!rounded-[var(--border-radius-small)] !text-[16px]"
          block
          type="primary"
          :loading="loading"
          @click="handleConfirm"
        >
          {{ t('common.confirm') }}
        </van-button>
      </div>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { FollowCommentUser } from '@lib/shared/models/follow';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmSelectList from '@/components/business/crm-select-list/index.vue';

  import { getUserOptions } from '@/api/modules';

  const props = defineProps<{
    selectedUsers?: FollowCommentUser[];
    title?: string;
    searchPlaceholder?: string;
  }>();

  const emit = defineEmits<{
    (e: 'confirm', users: FollowCommentUser[]): void;
    (e: 'closed'): void;
  }>();

  const { t } = useI18n();

  const show = defineModel<boolean>('show', {
    default: false,
  });

  const keyword = ref('');
  const value = ref<string[]>([]);
  const selectedRows = ref<Record<string, any>[]>([]);
  const loading = ref(false);
  const crmSelectListRef = ref<InstanceType<typeof CrmSelectList>>();
  const listParams = {
    includeDisabled: true,
  };

  const selectedUserIds = computed(() => new Set((props.selectedUsers || []).map((user) => user.id)));

  function transformUser(item: Record<string, any>) {
    return {
      ...item,
      checked: selectedUserIds.value.has(item.id),
    };
  }

  function search() {
    nextTick(() => {
      crmSelectListRef.value?.filterListByKeyword('name');
    });
  }

  function handleCancel() {
    show.value = false;
  }

  function handleConfirm() {
    const users = selectedRows.value.map((item) => ({
      id: item.id,
      name: item.name,
      avatar: item.avatar,
      enable: item.enable,
    }));
    emit('confirm', users);
    show.value = false;
  }

  watch(
    () => props.selectedUsers,
    (users) => {
      value.value = (users || []).map((user) => user.id);
      selectedRows.value = [...(users || [])];
    },
    { immediate: true }
  );

  watch(
    () => show.value,
    (val) => {
      if (val) {
        keyword.value = '';
        search();
      }
    }
  );
</script>

<style scoped lang="less">
  .crm-comment-mention-user-select {
    @apply h-full w-full;
  }
  .crm-comment-mention-user-select-page {
    display: flex;
    flex-direction: column;
    height: 100%;
    background: var(--text-n10);
  }
  .crm-comment-mention-user-select-header {
    position: relative;
    z-index: 1;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 12px;
    height: 48px;
    background: var(--text-n10);
    flex: none;
  }
  .crm-comment-mention-user-select-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-n1);
  }
  .crm-comment-mention-user-select-header-placeholder {
    width: 24px;
    height: 24px;
  }
  .crm-comment-mention-user-select-content {
    display: flex;
    flex: 1;
    flex-direction: column;
    min-height: 0;
  }
  .crm-comment-mention-user-select-list {
    overflow: hidden;
    padding: 0 16px;
    min-height: 0;
    flex: 1;
  }
  .crm-comment-mention-user-select-option {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
    min-width: 0;
    gap: 8px;
  }
  .crm-comment-mention-user-select-name {
    overflow: hidden;
    font-size: 14px;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: var(--text-n1);
  }
  .crm-comment-mention-user-select-footer {
    display: flex;
    flex: none;
    gap: 12px;
    padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
    background: var(--text-n10);
  }
  .crm-comment-mention-user-select-cancel {
    border: 0;
    border-radius: var(--border-radius-small);
    color: var(--primary-8);
    background: var(--primary-1);
  }
  .crm-comment-mention-user-select-confirm {
    border: 0;
    border-radius: var(--border-radius-small);
    background: var(--primary-8);
  }
</style>
