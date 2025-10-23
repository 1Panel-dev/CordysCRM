<template>
  <div class="absolute right-[16px] top-[16px] z-10 flex items-center gap-[12px]">
    <n-button type="default" class="outline--secondary px-[6px]" size="small" @click="emit('refresh')">
      <CrmIcon type="iconicon_refresh" />
    </n-button>
    <n-button type="default" class="outline--secondary px-[6px]" size="small" @click="emit('download')">
      <CrmIcon type="iconicon_download" />
    </n-button>
    <n-button type="default" class="outline--secondary px-[6px]" size="small" @click="toggleFullScreen">
      <CrmIcon :type="isFullScreen ? 'iconicon_off_screen' : 'iconicon_full_screen_one'" />
    </n-button>
  </div>
</template>

<script setup lang="ts">
  import { NButton } from 'naive-ui';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import useFullScreen from '@/hooks/useFullScreen';

  const props = defineProps<{
    containerRef?: Element;
  }>();
  const emit = defineEmits<{
    (e: 'refresh'): void;
    (e: 'download'): void;
    (e: 'toggleFullScreen', isFullScreen: boolean): void;
  }>();

  const { toggleFullScreen, isFullScreen } = useFullScreen(props.containerRef);

  watch(isFullScreen, (val) => {
    emit('toggleFullScreen', val);
  });
</script>

<style lang="less" scoped></style>
