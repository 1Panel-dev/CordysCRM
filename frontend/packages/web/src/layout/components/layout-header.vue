<template>
  <n-layout-header class="flex" bordered>
    <div class="flex w-[180px] items-center gap-[4px] px-[30px] py-[12px]">
      <img :src="props.logo ?? '/logo.png'" width="32px" height="32px" />
      <n-tooltip :delay="300">
        <template #trigger>
          <div class="one-line-text text-[20px] font-semibold leading-[32px]">
            {{ props.name ?? 'Cordys' }}
          </div>
        </template>
        {{ props.name ?? 'Cordys' }}
      </n-tooltip>
    </div>
    <div class="flex flex-1 items-center justify-between px-[16px]">
      <!-- <n-breadcrumb>
        <n-breadcrumb-item>
          <div class="text-[14px] leading-[22px]">一级页面</div>
        </n-breadcrumb-item>
        <n-breadcrumb-item>
          <div class="text-[14px] leading-[22px]">二级页面</div>
        </n-breadcrumb-item>
        <n-breadcrumb-item>
          <div class="text-[14px] leading-[22px]">三级页面</div>
        </n-breadcrumb-item>
      </n-breadcrumb> -->
      <div class="flex items-center gap-[8px]">
        <n-popselect
          v-model:value="currentLocale"
          :options="LOCALE_OPTIONS"
          trigger="hover"
          @update-value="changeLanguage"
        >
          <n-button class="p-[8px]" quaternary>
            <template #icon>
              <CrmIconFont type="icon-icon_translation" :size="14" class="text-[var(--text-n4)]" />
            </template>
          </n-button>
        </n-popselect>
      </div>
    </div>
  </n-layout-header>
</template>

<script setup lang="ts">
  import { NButton, NLayoutHeader, NPopselect, NTooltip } from 'naive-ui';

  import { LocaleType } from '@lib/shared/types/global';

  import CrmIconFont from '@/components/pure/crm-icon-font/index.vue';

  import { LOCALE_OPTIONS } from '@/locale/index';
  import useLocale from '@/locale/useLocale';

  const props = defineProps<{
    logo?: string;
    name?: string;
  }>();

  const { changeLocale, currentLocale } = useLocale();

  function changeLanguage(locale: LocaleType) {
    changeLocale(locale);
  }
</script>

<style lang="less" scoped></style>
