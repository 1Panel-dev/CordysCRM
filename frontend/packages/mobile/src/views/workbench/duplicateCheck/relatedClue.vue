<template>
  <CrmPageWrapper :title="customerName">
    <div class="p-[16px]">
      <div class="mb-[16px] text-[18px] font-semibold">
        {{
          detailType === 'opportunity'
            ? t('workbench.duplicateCheck.relatedOpportunity')
            : t('workbench.duplicateCheck.relatedClue')
        }}
      </div>
      <RelatedList
        :id="id"
        ref="customerRelatedListRef"
        :keyword="keyword"
        :name-key="detailType === 'opportunity' ? 'customerName' : undefined"
        :description-list="detailType === 'opportunity' ? opportunityDescriptionList : clueDescriptionList"
        :api="detailType === 'opportunity' ? GetRepeatOpportunityDetailList : GetRepeatClueDetailList"
      />
    </div>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import RelatedList from './components/relatedList.vue';

  import { GetRepeatClueDetailList, GetRepeatOpportunityDetailList } from '@/api/modules';
  import { clueDescriptionList, opportunityDescriptionList } from '@/config/workbench';

  const { t } = useI18n();
  const route = useRoute();

  const detailType = computed(() => route.query.detailType?.toString());
  const keyword = computed(() => route.query.keyword?.toString() ?? '');
  const customerName = computed(() => route.query.customerName?.toString() ?? '');
  const id = computed(() => route.query.id?.toString() ?? '');
</script>
