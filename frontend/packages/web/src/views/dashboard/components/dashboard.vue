<template>
  <div ref="fullRef" class="flex h-full flex-col gap-[16px] bg-[var(--text-n10)] p-[24px]">
    <div class="flex items-center justify-between">
      <div class="flex flex-1 items-center gap-[8px]">
        <favoriteIcon
          :value="props.isFavorite"
          class="cursor-pointer text-[var(--primary-8)]"
          @click="emit('toggleFavorite')"
        />
        <div class="flex-1 font-semibold">{{ props.title }}</div>
      </div>
      <div
        v-if="!props.isFullPage"
        class="cursor-pointer text-right !text-[var(--color-text-4)]"
        @click="toggleFullScreen"
      >
        <CrmIcon v-if="isFullScreen" type="iconicon_off_screen" />
        <CrmIcon v-else type="iconicon_full_screen_one" />
      </div>
    </div>
    <n-spin class="block flex-1" :show="loading" content-class="h-full">
      <iframe
        id="iframe-dashboard-view"
        style="width: 100%; height: 100%; border: 0"
        :src="props.resourceUrl ?? iframeSrc"
      ></iframe>
      <n-empty v-if="isError" size="large" :description="t('dashboard.loadFailed')"> </n-empty>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
  import { NEmpty, NSpin } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import favoriteIcon from './favoriteIcon.vue';

  import { getDEToken } from '@/api/modules';
  import useFullScreen from '@/hooks/useFullScreen';

  const props = defineProps<{
    title: string;
    dashboardId?: string; // 模块资源id
    isFavorite: boolean;
    isFullPage?: boolean;
    resourceUrl?: string; // 外链嵌入url
  }>();
  const emit = defineEmits<{
    (e: 'toggleFavorite'): void;
  }>();

  const { t } = useI18n();

  const loading = ref(false);
  const isError = ref(false);
  // 用于全屏的容器 ref
  const fullRef = ref<HTMLElement | null>();
  const { isFullScreen, toggleFullScreen } = useFullScreen(fullRef);

  const iframeSrc = ref('');
  const params = {
    // 固定写法：dashboard 仪表板、dataV 数据大屏
    'busiFlag': 'dashboard',
    'dvId': props?.dashboardId,
    // 固定写法
    'type': 'Dashboard',
    //  JWT token 认证。
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

  onUnmounted(() => {
    window.removeEventListener('message', onMessage, false);
  });

  watch(
    () => props.dashboardId,
    async (newVal) => {
      if (newVal) {
        params.dvId = newVal;
        await init();
        const iframe = document.getElementById('iframe-dashboard-view');
        const { contentWindow } = iframe as HTMLIFrameElement;
        contentWindow?.postMessage(params, '*');
        loading.value = false;
      }
    }
  );

  onBeforeMount(() => {
    window.addEventListener('message', onMessage, false);
    if (!props.resourceUrl) {
      init();
    }
  });
</script>

<style lang="less" scoped></style>
