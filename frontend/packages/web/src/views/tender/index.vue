<template>
  <div class="flex h-full flex-col gap-[8px]">
    <div class="flex w-full items-center gap-[12px] bg-[var(--text-n10)] p-[8px]">
      <CrmTag
        v-for="item of tenderList"
        :key="item.key"
        :type="activeTender === item.key ? 'primary' : 'default'"
        theme="outline"
        class="cursor-pointer !px-[12px]"
        size="large"
        tooltip-disabled
        @click="clickTag(item.key)"
      >
        <div class="flex items-center gap-[8px]">
          <CrmSvgIcon :name="item.icon" width="14px" height="14px" />
          {{ item.label }}
        </div>
      </CrmTag>
    </div>
    <n-spin class="block flex-1" :show="loading" content-class="h-full">
      <n-empty v-if="isError" size="large" :description="t('dashboard.loadFailed')"> </n-empty>
      <iframe
        v-else
        id="iframe-tender-view"
        style="width: 100%; height: 100%; border: 0"
        :src="tenderConfig?.tenderAddress"
      ></iframe>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
  import { NEmpty, NSpin } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { ConfigSynchronization } from '@lib/shared/models/system/business';

  import CrmSvgIcon from '@/components/pure/crm-svg/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';

  import { getThirdPartyConfig } from '@/api/modules';
  import useLocalForage from '@/hooks/useLocalForage';

  const { t } = useI18n();
  const { setItem, getItem } = useLocalForage();
  const tenderList = [
    {
      key: 'dadan',
      label: '大单网',
      icon: 'dadan',
    },
  ];

  const loading = ref(false);
  const isError = ref(false);
  const tenderConfig = ref<ConfigSynchronization>();
  async function init() {
    try {
      isError.value = false;
      loading.value = true;
      tenderConfig.value = await getThirdPartyConfig('TENDER');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      isError.value = true;
    } finally {
      loading.value = false;
    }
  }

  const activeTender = ref(tenderList[0].key);
  const activeTenderKey = 'tender-active-tab';
  async function clickTag(key: string) {
    activeTender.value = key;
    await setItem(activeTenderKey, key);
  }

  async function loadActiveTender() {
    const currentActiveKey = await getItem(activeTenderKey);
    if (typeof currentActiveKey === 'string' && tenderList.some((item) => item.key === currentActiveKey)) {
      activeTender.value = currentActiveKey;
    } else {
      activeTender.value = tenderList[0]?.key ?? '';
      await setItem(activeTenderKey, activeTender.value);
    }
  }

  onBeforeMount(async () => {
    await init();
    loadActiveTender();
  });
</script>

<style lang="less" scoped>
  .tender-card {
    padding: 24px;
    border: 1px solid var(--text-n8);
    border-radius: 6px;
    .tender-card-header {
      @apply flex items-start justify-between;

      margin-bottom: 8px;
      padding-bottom: 16px;
    }
    .tender-card-bottom {
      @apply flex items-center;

      gap: 12px;
    }
  }
</style>
