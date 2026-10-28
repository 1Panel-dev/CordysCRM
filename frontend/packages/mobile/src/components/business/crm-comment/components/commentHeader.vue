<template>
  <div class="crm-comment-header">
    <div class="crm-comment-header__left">
      <CrmIcon name="iconicon_comment" width="16px" height="16px" color="var(--text-n1)" />
      <div class="crm-comment-header__title shrink-0">{{ titleText }}</div>
      <CrmTag :tag="displayCount" />
    </div>

    <div
      v-if="props.showTimeSort"
      class="crm-comment-header__sort"
      @click="emit('sortChange', props.sortOrder === 'desc' ? 'asc' : 'desc')"
      @keydown.enter.prevent="emit('sortChange', props.sortOrder === 'desc' ? 'asc' : 'desc')"
      @keydown.space.prevent="emit('sortChange', props.sortOrder === 'desc' ? 'asc' : 'desc')"
    >
      {{ t('crmComment.timeSort') }}
      <span class="crm-comment-header__sort-arrows">
        <CrmIcon
          name="iconicon_chevron_up"
          width="14px"
          height="14px"
          class="mb-[-6px]"
          :color="props.sortOrder === 'asc' ? 'var(--primary-8)' : 'var(--text-n4)'"
        />
        <CrmIcon
          name="iconicon_chevron_down"
          width="14px"
          height="14px"
          :color="props.sortOrder === 'desc' ? 'var(--primary-8)' : 'var(--text-n4)'"
        />
      </span>
    </div>
    <van-button
      v-if="props.showAdd"
      class="crm-comment-header__add shrink-0"
      icon="plus"
      plain
      size="small"
      type="default"
      @click="emit('add')"
    >
      {{ t('common.add') }}
    </van-button>
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { formatBadgeCount } from '@lib/shared/method';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';

  const props = withDefaults(
    defineProps<{
      count?: number;
      title?: string;
      showAdd?: boolean;
      showTimeSort?: boolean;
      sortOrder?: 'asc' | 'desc';
    }>(),
    {
      count: 0,
      showAdd: true,
      showTimeSort: false,
      sortOrder: 'desc',
    }
  );

  const emit = defineEmits<{
    (event: 'add'): void;
    (event: 'sortChange', value: 'asc' | 'desc'): void;
  }>();

  const { t } = useI18n();

  const titleText = computed(() => props.title || t('crmComment.title'));
  const displayCount = computed(() => formatBadgeCount(props.count));
</script>

<style scoped lang="less">
  .crm-comment-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    width: 100%;
    background: var(--text-n10);
    gap: 8px;
    .half-px-border-bottom();
  }
  .crm-comment-header__left {
    display: flex;
    align-items: center;
    padding: 0;
    min-width: 0;
    border: 0;
    background: transparent;
    gap: 4px;
  }
  .crm-comment-header__title {
    font-size: 14px;
    font-weight: 500;
    color: var(--text-n1);
  }
  .crm-comment-header__add {
    padding: 0 10px;
    height: 28px;
    border-color: var(--text-n7);
    color: var(--text-n1);
    background-color: var(--text-n10);
  }
  .crm-comment-header__sort {
    display: flex;
    margin-left: auto;
    padding: 2px 6px;
    border-radius: 2px;
    white-space: nowrap;
    color: var(--text-n1);
    background: var(--text-n8);
    cursor: pointer;
    gap: 4px;
  }
  .crm-comment-header__sort-arrows {
    display: flex;
    flex-direction: column;
  }
</style>
