<template>
  <CrmPageWrapper :title="t('customer.transferCustomer')">
    <div class="flex h-full flex-col">
      <van-search v-model="keyword" shape="round" :placeholder="t('customer.searchNamePlaceholder')" @search="search" />
      <div class="flex-1 overflow-hidden px-[16px]">
        <CrmSelectList
          ref="crmSelectListRef"
          v-model:value="value"
          v-model:selected-rows="selectedRows"
          :keyword="keyword"
          :load-list-api="getUserOptions"
          :multiple="false"
          no-page-nation
        ></CrmSelectList>
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
          v-permission="['CUSTOMER_MANAGEMENT:UPDATE']"
          type="primary"
          :loading="loading"
          :disabled="!selectedRows.length"
          class="!rounded-[var(--border-radius-small)] !text-[16px]"
          block
          @click="onConfirm"
        >
          {{ t('customer.transfer') }}
        </van-button>
      </div>
    </template>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute, useRouter } from 'vue-router';
  import { closeToast, showLoadingToast, showSuccessToast } from 'vant';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { sleep } from '@lib/shared/method';

  import CrmSelectList from '@/components/business/crm-select-list/index.vue';

  import { batchTransferClue, batchTransferCustomer, getUserOptions, transferOpt } from '@/api/modules';

  const route = useRoute();
  const router = useRouter();
  const { t } = useI18n();

  const keyword = ref('');
  const value = ref<string | string[]>([]);
  const selectedRows = ref<Record<string, any>[]>([]);
  const loading = ref(false);
  const crmSelectListRef = ref<InstanceType<typeof CrmSelectList>>();

  const loadListApi: Record<string, (data: any) => Promise<any>> = {
    [FormDesignKeyEnum.CUSTOMER]: batchTransferCustomer,
    [FormDesignKeyEnum.CLUE]: batchTransferClue,
    [FormDesignKeyEnum.BUSINESS]: transferOpt,
  };

  async function onConfirm() {
    try {
      loading.value = true;
      showLoadingToast(t('common.transferring'));
      await loadListApi[route.query.apiKey as string]({
        ids: [route.query.id as string],
        owner: value.value as string,
      });
      showSuccessToast(t('customer.transferSuccess'));
      await sleep(300);
      router.back();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      loading.value = false;
      closeToast();
    }
  }

  function search() {
    crmSelectListRef.value?.loadList(true);
  }
</script>

<style lang="less" scoped></style>
