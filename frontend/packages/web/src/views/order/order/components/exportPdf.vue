<template>
  <div class="bg-[var(--text-n9)]">
    <div class="detail-container">
      <div id="detail">
        <CrmFormDescription
          :form-key="FormDesignKeyEnum.ORDER_SNAPSHOT"
          :source-id="sourceId"
          :loadingDescription="t('order.exportingPdf')"
          :column="2"
          :refresh-key="refreshId"
          label-width="auto"
          :one-line-value="false"
          :one-line-label="false"
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
  import { useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { sleep } from '@lib/shared/method';
  import exportPDF from '@lib/shared/method/exportPdf';
  import { CollaborationType } from '@lib/shared/models/customer';

  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';

  import { downloadOrder } from '@/api/modules';

  const { t } = useI18n();

  const loading = ref(true);

  const Message = useMessage();
  const route = useRoute();
  const sourceId = ref(route.query.id as string);
  const refreshId = ref(0);
  const title = ref('');
  const detailInfo = ref<Record<string, any>>();

  const formLoading = ref(true);
  async function handleInit(type?: CollaborationType, name?: string, detail?: Record<string, any>) {
    title.value = name || '';
    detailInfo.value = detail;
    formLoading.value = false;
    await nextTick();
    await sleep(300);
    exportPDF(detailInfo.value?.name ?? t('module.order'), 'detail', async () => {
      loading.value = false;
      try {
        await downloadOrder(sourceId.value);
        Message.success(t('opportunity.quotation.exportPdfSuccess'));
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    });
  }
</script>

<style scoped lang="less">
  .detail-container {
    @apply flex justify-center;
    #detail {
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
