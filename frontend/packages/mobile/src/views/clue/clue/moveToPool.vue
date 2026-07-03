<template>
  <CrmPageWrapper :title="t('common.moveInReason')">
    <div class="flex h-full flex-col">
      <van-notice-bar wrapable :scrollable="false" :text="contentTip" />
      <div v-if="isPoolReason" class="flex-1 overflow-hidden px-[16px]">
        <div class="p-[16px_0_8px] text-[14px] text-[var(--text-n3)]">{{ poolLabel }}</div>
        <CrmSelectList
          v-model:value="poolValue"
          v-model:selected-rows="selectedPoolRows"
          :data="poolList"
          :multiple="false"
          no-page-nation
        ></CrmSelectList>
      </div>
      <div v-if="enableReason" class="flex-1 overflow-hidden px-[16px]">
        <div v-if="isPoolReason" class="p-[16px_0_8px] text-[14px] text-[var(--text-n3)]">
          {{ t('common.moveInReason') }}
        </div>
        <CrmSelectList
          v-model:value="value"
          v-model:selected-rows="selectedRows"
          :data="reasonList"
          :multiple="false"
          no-page-nation
        ></CrmSelectList>
      </div>
      <div v-else-if="!isPoolReason" class="mx-auto mt-[32px]">
        {{ t('common.noReason') }}
      </div>
    </div>
    <template #footer>
      <div class="flex items-center gap-[16px]">
        <van-button
          type="default"
          class="crm-button-primary--secondary !rounded-[var(--border-radius-small)] !text-[16px]"
          :disabled="loading"
          block
          @click="router.back"
        >
          {{ t('common.cancel') }}
        </van-button>
        <van-button
          type="primary"
          :loading="loading"
          :disabled="confirmDisabled"
          class="!rounded-[var(--border-radius-small)] !text-[16px]"
          block
          @click="handleSave"
        >
          {{ t('common.confirmMoveIn') }}
        </van-button>
      </div>
    </template>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute, useRouter } from 'vue-router';
  import { showFailToast, showSuccessToast } from 'vant';

  import { ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { sleep } from '@lib/shared/method';
  import { MoveToPublicPoolParams } from '@lib/shared/models/customer';

  import CrmSelectList from '@/components/business/crm-select-list/index.vue';

  import {
    getOpenSeaOptions,
    getPoolOptions,
    getReasonConfig,
    moveCustomerToPool,
    moveToLeadPool,
  } from '@/api/modules';

  import { ClueRouteEnum, CustomerRouteEnum } from '@/enums/routeEnum';

  const route = useRoute();
  const router = useRouter();
  const { t } = useI18n();

  const value = ref<string>('');
  const selectedRows = ref<Record<string, any>[]>([]);
  const poolValue = ref<string>('');
  const selectedPoolRows = ref<Record<string, any>[]>([]);
  const poolList = ref<Record<string, any>[]>([]);
  const loading = ref(false);

  export type ReasonKey = ReasonTypeEnum.CLUE_POOL_RS | ReasonTypeEnum.CUSTOMER_POOL_RS;

  const enableReason = ref(false);
  const reasonList = ref<Record<string, any>[]>([]);
  const reasonKey = computed<ReasonKey>(
    () => (route.query.reasonKey?.toString() as ReasonKey) ?? ReasonTypeEnum.CLUE_POOL_RS
  );
  const isCluePoolReason = computed(() => reasonKey.value === ReasonTypeEnum.CLUE_POOL_RS);
  const isCustomerPoolReason = computed(() => reasonKey.value === ReasonTypeEnum.CUSTOMER_POOL_RS);
  const isPoolReason = computed(() => isCluePoolReason.value || isCustomerPoolReason.value);
  const poolLabel = computed(() => (isCluePoolReason.value ? t('clue.moveIntoCluePool') : t('customer.moveToOpenSea')));
  const confirmDisabled = computed(() => {
    if (enableReason.value && !selectedRows.value.length) {
      return true;
    }
    return isPoolReason.value && !selectedPoolRows.value.length;
  });

  const moveApiMap: Record<ReasonKey, (params: MoveToPublicPoolParams) => Promise<any>> = {
    [ReasonTypeEnum.CLUE_POOL_RS]: moveToLeadPool,
    [ReasonTypeEnum.CUSTOMER_POOL_RS]: moveCustomerToPool,
  };

  const contentTip = computed(() =>
    reasonKey.value === ReasonTypeEnum.CLUE_POOL_RS ? t('clue.moveToLeadPoolTip') : t('customer.batchMoveContentTip')
  );

  async function initReasonConfig() {
    try {
      const { dictList, enable } = await getReasonConfig(reasonKey.value);
      enableReason.value = enable;
      reasonList.value = dictList.filter((e) => e.id !== 'system');
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  }

  async function initPoolOptions() {
    if (!isPoolReason.value) {
      return;
    }
    try {
      poolValue.value = '';
      selectedPoolRows.value = [];
      poolList.value = [];
      poolList.value = isCluePoolReason.value ? await getPoolOptions() : await getOpenSeaOptions();
      const defaultPool = [...poolList.value].sort((prev, next) => next.createTime - prev.createTime)[0];
      poolValue.value = defaultPool?.id || '';
      selectedPoolRows.value = defaultPool ? [defaultPool] : [];
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  }

  function resetForm() {
    value.value = '';
    selectedRows.value = [];
    poolValue.value = '';
    selectedPoolRows.value = [];
    poolList.value = [];
    reasonList.value = [];
    enableReason.value = false;
  }

  onBeforeMount(() => {
    resetForm();
    initReasonConfig();
    initPoolOptions();
  });

  async function handleSave() {
    try {
      loading.value = true;
      const { fail } = await moveApiMap[reasonKey.value]({
        id: route.query.id?.toString() ?? '',
        reasonId: value.value,
        poolId: isPoolReason.value ? poolValue.value : null,
      });
      if (fail > 0) {
        showFailToast(t('common.transferFailed'));
      } else {
        showSuccessToast(t('common.transferSuccess'));
      }
      await sleep(800);
      // 返回列表
      if (reasonKey.value === ReasonTypeEnum.CLUE_POOL_RS) {
        router.push({
          name: ClueRouteEnum.CLUE_INDEX,
        });
      } else {
        router.push({
          name: CustomerRouteEnum.CUSTOMER_INDEX,
        });
      }
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    } finally {
      loading.value = false;
    }
  }
</script>

<style lang="less" scoped></style>
