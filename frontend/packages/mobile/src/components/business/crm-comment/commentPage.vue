<template>
  <CrmPageWrapper :title="t('crmComment.title')">
    <div class="h-full bg-[var(--text-n9)]">
      <CrmComment
        :type="commentType"
        :source-id="commentSourceId"
        :count="initialCommentCount"
        :default-reply-count="1"
      />
    </div>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmComment from './index.vue';

  import type { MobileCommentResourceType } from './useCommentResource';

  const route = useRoute();
  const { t } = useI18n();

  const commentSourceId = computed(() => String(route.query.id || ''));
  const commentType = computed<MobileCommentResourceType>(() => (route.query.type === 'plan' ? 'plan' : 'record'));

  function getInitialCommentCount() {
    const count = Number(route.query.commentCount);
    return Number.isFinite(count) ? count : 0;
  }

  const initialCommentCount = computed(() => getInitialCommentCount());
</script>

<style scoped lang="less"></style>
