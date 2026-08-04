<template>
  <div class="crm-comment-header">
    <div class="crm-comment-header__left">
      <CrmIcon name="iconicon_comment" width="16px" height="16px" color="var(--text-n1)" />
      <div class="crm-comment-header__title">{{ titleText }}</div>
      <CrmTag :tag="displayCount" />
    </div>

    <van-button
      v-if="props.showAdd"
      class="crm-comment-header__add"
      icon="plus"
      plain
      size="small"
      type="default"
      @click="emit('add')"
    >
      {{ t('crmComment.addComment') }}
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
    }>(),
    {
      count: 0,
      showAdd: true,
    }
  );

  const emit = defineEmits<{
    (event: 'add'): void;
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
    padding: 12px 20px;
    width: 100%;
    background: var(--text-n10);
    gap: 12px;
    .half-px-border-bottom();
  }
  .crm-comment-header__left {
    display: flex;
    align-items: center;
    padding: 0;
    min-width: 0;
    border: 0;
    background: transparent;
    gap: 6px;
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
</style>
