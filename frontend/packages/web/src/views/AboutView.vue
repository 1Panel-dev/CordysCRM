<template>
  <div class="p-[24px]">
    <CrmCard title="按钮类型demo" sub-title="subTitle" content-max-height="100px" no-content-padding>
      <!-- themeOverridesConfig 主题vars配置 -->
      <n-config-provider :theme-overrides="themeOverridesConfig">
        <!-- TODO 按钮类型demo 暂时提供参考 you can delete it  ^_^  -->
        <div class="flex gap-[16px]">
          <NButton type="primary">主要按钮</NButton>
          <NButton type="tertiary">次要按钮</NButton>
          <NButton type="default" class="outline--secondary">次要外框按钮</NButton>
          <NButton strong secondary>次要背景按钮</NButton>
          <NButton type="success"> 成功背景按钮 </NButton>
          <NButton type="warning"> 警告背景按钮 </NButton>
          <NButton type="error"> 错误背景按钮 </NButton>
        </div>
        <div class="mt-[24px] flex gap-[16px]">
          <n-button quaternary class="text-btn-secondary"> 文本次要按钮 </n-button>
          <n-button quaternary type="primary" class="text-btn-primary"> 文本主题按钮 </n-button>
          <n-button quaternary type="info" class="text-btn-info"> 文本info按钮 </n-button>
          <n-button quaternary type="success" class="text-btn-success"> 文本success按钮 </n-button>
          <n-button quaternary type="warning" class="text-btn-warning"> 文本warning按钮 </n-button>
          <n-button quaternary type="error" class="text-btn-error"> 文本Error按钮 </n-button>
        </div>
        <div class="mt-[24px] flex gap-[16px]">
          <n-button type="primary" ghost> Primary </n-button>
          <n-button type="info" ghost class="n-btn-outline-info"> Info </n-button>
          <n-button type="success" ghost class="n-btn-outline-success"> Success </n-button>
          <n-button type="warning" ghost class="n-btn-outline-warning"> Warning </n-button>
          <n-button type="error" ghost class="n-btn-outline-error"> Error </n-button>
        </div>
        <div class="mt-[24px] flex gap-[16px]">
          <n-button type="primary" ghost> Primary </n-button>
          <n-button type="info" ghost> Info </n-button>
          <n-button type="success" ghost> Success </n-button>
          <n-button type="warning" ghost> Warning </n-button>
          <n-button type="error" ghost> Error </n-button>
        </div>
      </n-config-provider>
    </CrmCard>
  </div>
</template>

<script setup lang="ts">
  import CrmCard from '@/components/pure/crm-card/index.vue';

  import { getThemeOverrides } from '@/utils/themeOverrides';

  import type { GlobalThemeOverrides } from 'naive-ui';
  import { NButton, NConfigProvider } from 'naive-ui';
  // TODO 待完善
  const themeOverridesConfig = ref<GlobalThemeOverrides>(getThemeOverrides());
</script>

<style>
  @media (min-width: 1024px) {
    .about {
      display: flex;
      align-items: center;
      min-height: 100vh;
    }
  }
</style>
