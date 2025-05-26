<template>
  <div class="flex h-full flex-col overflow-hidden bg-[var(--text-n9)]">
    <div class="bg-[var(--text-n10)] p-[8px_16px]">
      <van-search
        v-model="keyword"
        shape="round"
        :placeholder="t('customer.searchPlaceholder')"
        class="crm-search"
        @search="search"
        @clear="search"
      />
    </div>
    <CrmList
      ref="crmListRef"
      :keyword="keyword"
      :list-params="{ sourceId: props.sourceId }"
      :load-list-api="loadListApi"
      class="p-[16px]"
      no-page-nation
      :item-gap="16"
    >
      <template #item="{ item }">
        <div
          class="flex w-full items-center gap-[16px] rounded-[var(--border-radius-small)] bg-[var(--text-n10)] p-[16px]"
        >
          <CrmAvatar :text="item.ownerName" />
          <div class="flex flex-1 flex-col gap-[2px] overflow-hidden">
            <div class="flex justify-between">
              <div class="one-line-text flex-1 text-[16px] text-[var(--text-n1)]">{{ item.ownerName }}</div>
              <van-tag
                v-show="item.departmentName?.length"
                color="var(--text-n9)"
                text-color="var(--text-n1)"
                class="rounded-[var(--border-radius-small)] !p-[2px_6px]"
              >
                {{ item.departmentName }}
              </van-tag>
            </div>
            <div>
              <span class="text-[12px] text-[var(--text-n4)]"> {{ t('header.attributionPeriod') }}</span>
              <span class="ml-[8px] text-[12px] text-[var(--text-n2)]">
                {{
                  `${dayjs(item.collectionTime).format('YYYY-MM-DD')} ${t('common.to')} ${dayjs(item.endTime).format(
                    'YYYY-MM-DD'
                  )} `
                }}
              </span>
            </div>
          </div>
        </div>
      </template>
    </CrmList>
  </div>
</template>

<script setup lang="ts">
  import dayjs from 'dayjs';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CustomerContractTableParams, HeaderHistoryItem } from '@lib/shared/models/customer';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';

  const props = defineProps<{
    loadListApi: (data: CustomerContractTableParams) => Promise<HeaderHistoryItem>;
    sourceId: string; // 资源id
  }>();

  const { t } = useI18n();

  const crmListRef = ref<InstanceType<typeof CrmList>>();
  const keyword = ref('');

  function search() {
    nextTick(() => {
      crmListRef.value?.filterListByKeyword('ownerName');
    });
  }

  onMounted(() => {
    search();
  });
</script>

<style lang="less" scoped></style>
