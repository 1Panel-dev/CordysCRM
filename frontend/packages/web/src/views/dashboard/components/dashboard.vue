<template>
  <div ref="fullRef" class="flex h-full flex-col gap-[16px] bg-[var(--text-n10)] p-[24px]">
    <div class="flex items-center justify-between">
      <div class="flex flex-1 items-center gap-[8px]">
        <favoriteIcon :value="props.isFavorite" class="cursor-pointer text-[var(--primary-8)]" />
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
    <div class="flex-1">
      <iframe id="iframe-dashboard-view" style="width: 100%; height: 100%; border: 0" :src="iframeSrc"></iframe>
    </div>
  </div>
</template>

<script setup lang="ts">
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import favoriteIcon from './favoriteIcon.vue';

  import { getDEToken } from '@/api/modules';
  import useFullScreen from '@/hooks/useFullScreen';

  const props = defineProps<{
    title: string;
    dashboardId: string;
    isFavorite: boolean;
    isFullPage?: boolean;
  }>();

  // 用于全屏的容器 ref
  const fullRef = ref<HTMLElement | null>();
  const { isFullScreen, toggleFullScreen } = useFullScreen(fullRef);

  const iframeSrc = ref('');
  const params = {
    // 固定写法：dashboard 仪表板、dataV 数据大屏
    'busiFlag': 'dashboard',
    'dvId': '',
    // 固定写法
    'type': 'Dashboard',
    //  JWT token 认证。
    'embeddedToken': '',
    // 固定写法
    'de-embedded': true,
  };

  async function init() {
    try {
      const token = await getDEToken();
      // params.embeddedToken = token;
      // iframeSrc.value = `${window.location.origin}/dashboard?${new URLSearchParams(params).toString()}`;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error('Error initializing dashboard:', error);
    }
  }

  onBeforeMount(() => {
    init();
  });
</script>

<style lang="less" scoped></style>
