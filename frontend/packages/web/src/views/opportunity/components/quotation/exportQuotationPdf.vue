<template>
  <div class="bg-[var(--text-n9)]">
    <div class="quotation-detail-container">
      <div id="quotation-detail">
        <CrmFormDescription
          :form-key="FormDesignKeyEnum.OPPORTUNITY_QUOTATION_SNAPSHOT"
          :source-id="sourceId"
          :loadingDescription="t('opportunity.quotation.exportingPdf')"
          :column="2"
          :refresh-key="refreshId"
          label-width="auto"
          value-align="start"
          tooltip-position="top-start"
          @init="handleInit"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { useRoute } from 'vue-router';
  import { NSpin, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import exportPDF from '@lib/shared/method/exportPdf';
  import { CollaborationType } from '@lib/shared/models/customer';

  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';

  const { t } = useI18n();

  const loading = ref(true);

  const Message = useMessage();
  const route = useRoute();
  const sourceId = ref(route.query.id as string);
  const refreshId = ref(0);
  const title = ref('');
  const quotationDetail = ref<Record<string, any>>();

  function handleInit(type?: CollaborationType, name?: string, detail?: Record<string, any>) {
    title.value = name || '';
    quotationDetail.value = detail;
    exportPDF(detail?.name ?? t('opportunity.quotation'), 'quotation-detail', () => {
      loading.value = false;
      Message.success(t('opportunity.quotation.exportPdfSuccess'));
    });
  }
</script>

<style scoped lang="less">
  .quotation-detail-container {
    @apply flex justify-center;
    #quotation-detail {
      padding: 16px;
      width: 1190px;
      min-height: 100vh;
      background: var(--text-n10);
      page-break-inside: avoid;
      @apply overflow-x-auto;
      .crm-scroll-bar();
    }
  }
</style>
