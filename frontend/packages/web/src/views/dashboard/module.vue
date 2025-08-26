<template>
  <CrmCard :special-height="26" no-content-padding hide-footer>
    <n-spin class="block flex-1" :show="loading" content-class="h-full">
      <iframe id="iframe-dashboard-view" style="width: 100%; height: 100%; border: 0" :src="iframeSrc"></iframe>
      <n-empty v-if="isError" size="large" :description="t('dashboard.loadFailed')"> </n-empty>
    </n-spin>
  </CrmCard>
</template>

<script setup lang="ts">
  import { NEmpty, NSpin } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';

  import { getDEToken } from '@/api/modules';

  const { t } = useI18n();
  const loading = ref(false);
  const isError = ref(false);
  const iframeSrc = ref('');
  const params = {
    // 固定写法 DashboardPanel 仪表板模块。
    'type': 'DashboardPanel',
    //  JWT token 认证
    'embeddedToken': '',
    // 固定写法
    'de-embedded': true,
  };

  const onMessage = (event: any) => {
    const iframe = document.getElementById('iframe-dashboard-view');
    if (event.data?.msgOrigin === 'de-fit2cloud') {
      const { contentWindow } = iframe as HTMLIFrameElement;
      contentWindow?.postMessage(params, '*');
      loading.value = false;
    }
  };

  async function init() {
    try {
      loading.value = true;
      isError.value = false;
      const res = await getDEToken();
      params.embeddedToken = res.token;
      iframeSrc.value = `${res.url}/#/chart-view`;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error('Error initializing dashboard:', error);
      loading.value = false;
      isError.value = true;
    }
  }

  onBeforeMount(() => {
    window.addEventListener('message', onMessage, false);
    init();
  });

  onUnmounted(() => {
    window.removeEventListener('message', onMessage, false);
  });
</script>

<style lang="less" scoped></style>
