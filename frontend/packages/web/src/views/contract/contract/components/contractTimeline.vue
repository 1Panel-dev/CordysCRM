<template>
  <CrmCard no-content-bottom-padding hide-footer>
    <!-- TODO lmy -->
    <div class="mb-[16px] rounded-[var(--border-radius-mini)] bg-[var(--text-n9)] p-[12px]"> </div>
    <CrmList
      v-if="data.length"
      v-model:data="data"
      :loading="loading"
      virtual-scroll-height="calc(100vh - 250px)"
      key-field="id"
      mode="remote"
      @reach-bottom="handleReachBottom"
    >
      <template #item="{ item }">
        <div class="crm-follow-record-item">
          <div class="crm-follow-time-line">
            <div class="crm-follow-time-dot"></div>
            <div class="crm-follow-time-line"></div>
          </div>
          <div class="mb-[24px] flex w-full flex-col gap-[16px]">
            <div class="crm-follow-record-title h-[32px]">
              <!-- TODO lmy -->
              {{ dayjs(item.createTime).format('YYYY-MM-DD') }}
            </div>

            <div class="crm-follow-record-base-info">
              <CrmDetailCard :description="getDescriptionFun(item)">
                <template #name>
                  <CrmTableButton @click="goDetail(item)">
                    {{ item.name }}
                    <template #trigger> {{ item.name }} </template>
                  </CrmTableButton>
                </template>
                <template #status>
                  <ContractStatus :status="item?.status ?? ContractStatusEnum.SIGNED" />
                </template>
                <template #createTime>
                  <div class="flex items-center gap-[8px]">
                    {{ dayjs(item.createTime).format('YYYY-MM-DD HH:mm:ss') }}
                  </div>
                </template>
              </CrmDetailCard>
            </div>
          </div>
        </div>
      </template>
    </CrmList>
    <n-empty v-else :description="t('common.noData')"> </n-empty>
  </CrmCard>
</template>

<script setup lang="ts">
  import { NEmpty } from 'naive-ui';
  import dayjs from 'dayjs';

  import { ContractStatusEnum } from '@lib/shared/enums/contractEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ContractItem } from '@lib/shared/models/contract';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import type { Description } from '@/components/pure/crm-detail-card/index.vue';
  import CrmDetailCard from '@/components/pure/crm-detail-card/index.vue';
  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import ContractStatus from '@/views/contract/contract/components/contractStatus.vue';

  import { getAccountContract } from '@/api/modules';
  import useOpenNewPage from '@/hooks/useOpenNewPage';

  import { ContractRouteEnum } from '@/enums/routeEnum';

  const props = defineProps<{
    sourceId: string;
  }>();

  const { t } = useI18n();
  const { openNewPage } = useOpenNewPage();

  function getDescriptionFun(item: ContractItem) {
    const lastDescriptionList = [
      {
        key: 'ownerName',
        label: t('contract.applicant'),
        value: 'ownerName',
      },
      {
        key: 'name',
        label: t('contract.invoicedContract'),
        value: 'name',
      },
      {
        key: 'amount',
        label: t('contract.contractAmount'),
        value: 'amount',
      },
      {
        key: 'status',
        label: t('contract.status'),
        value: 'status',
      },
      {
        key: 'createTime',
        label: t('contract.applicationTime'),
        value: 'createTime',
      },
    ];

    return (lastDescriptionList.map((desc: Description) => ({
      ...desc,
      value: item[desc.key as keyof ContractItem],
    })) || []) as Description[];
  }

  const data = ref<ContractItem[]>([]);

  const pageNation = ref({
    total: 0,
    pageSize: 10,
    current: 1,
  });

  const loading = ref(false);

  function transformField(item: ContractItem, optionMap?: Record<string, any>) {
    const methodKey = 'contractId';
    let name;
    if (optionMap) {
      name = optionMap[methodKey]?.find((e: any) => e.id === item[methodKey as keyof ContractItem])?.name || '-';
    }
    return {
      ...item,
      [methodKey]: name,
    };
  }

  async function loadList(refresh = true) {
    try {
      loading.value = true;
      if (refresh) {
        pageNation.value.current = 1;
      }
      const params = {
        customerId: props.sourceId,
        current: pageNation.value.current || 1,
        pageSize: pageNation.value.pageSize,
      };
      const res = await getAccountContract(params);
      if (refresh) {
        data.value = [];
      }
      if (res) {
        const newList = res.list.map((item: ContractItem) => transformField(item, res?.optionMap));
        data.value = data.value.concat(newList);
        pageNation.value.total = res.total;
      }
    } catch (err) {
      // eslint-disable-next-line no-console
      console.log(err);
    } finally {
      loading.value = false;
    }
  }

  function handleReachBottom() {
    pageNation.value.current += 1;
    if (pageNation.value.current > Math.ceil(pageNation.value.total / pageNation.value.pageSize)) {
      return;
    }
    loadList(false);
  }

  function goDetail(item: ContractItem) {
    openNewPage(ContractRouteEnum.CONTRACT_INDEX, {
      id: item.id,
    });
  }

  onMounted(() => {
    loadList();
  });
</script>

<style scoped lang="less">
  .crm-follow-record-item {
    @apply flex gap-4;
    .crm-follow-time-line {
      padding-top: 12px;
      width: 8px;

      @apply flex flex-col items-center justify-center gap-2;
      .crm-follow-time-dot {
        width: 8px;
        height: 8px;
        border: 2px solid var(--text-n7);
        border-color: var(--primary-8);
        border-radius: 50%;
        flex-shrink: 0;
      }
      .crm-follow-time-line {
        width: 2px;
        background: var(--text-n8);
        @apply h-full;
      }
    }
    .crm-follow-record-title {
      @apply flex items-center justify-between gap-4;
      .crm-follow-record-method {
        color: var(--text-n1);
        @apply font-medium;
      }
    }
  }
</style>
