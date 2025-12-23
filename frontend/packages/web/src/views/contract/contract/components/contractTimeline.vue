<template>
  <CrmCard no-content-bottom-padding hide-footer>
    <div v-if="statisticInfo" class="mb-[16px] rounded-[var(--border-radius-mini)] bg-[var(--text-n9)] p-[12px]">
      <div v-for="item in statisticInfo" :key="item" class="flex items-center">
        <span>{{ item.label }}</span>
        <span class="ml-[8px] font-semibold">
          {{ item.value.toLocaleString('en-US') }}
        </span>
      </div>
    </div>
    <CrmList
      v-if="data.length"
      v-model:data="data"
      :loading="loading"
      virtual-scroll-height="calc(100vh - 270px)"
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
              {{ dayjs(item.createTime).format('YYYY-MM-DD') }}
            </div>

            <div class="crm-follow-record-base-info">
              <CrmDetailCard :description="getDescription(item)">
                <template #name>
                  <CrmTableButton @click="goDetail(item.id)">
                    {{ item.name }}
                    <template #trigger> {{ item.name }} </template>
                  </CrmTableButton>
                </template>
                <template #contractName="{ item: decItem }">
                  <CrmTableButton @click="goDetail(item.contractId)">
                    {{ decItem.value }}
                    <template #trigger> {{ decItem.value }} </template>
                  </CrmTableButton>
                </template>
                <template #stage>
                  {{ contractStatusOptions.find((i) => i.value === item.stage)?.label }}
                </template>
                <template #planStatus>
                  <ContractStatus :status="item?.planStatus ?? ContractStatusEnum.SIGNED" />
                </template>
                <template #createTime="{ item: decItem }">
                  <div class="flex items-center gap-[8px]">
                    {{ dayjs(decItem.value).format('YYYY-MM-DD HH:mm:ss') }}
                  </div>
                </template>
                <template #planEndTime="{ item: decItem }">
                  <div class="flex items-center gap-[8px]">
                    {{ dayjs(decItem.value).format('YYYY-MM-DD HH:mm:ss') }}
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

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDetailCard from '@/components/pure/crm-detail-card/index.vue';
  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import ContractStatus from '@/views/contract/contractPaymentPlan/components/contractPaymentStatus.vue';

  import { contractStatusOptions } from '@/config/contract';
  import type { TimelineType } from '@/hooks/useContractTimeline';
  import useContractTimeline from '@/hooks/useContractTimeline';
  import useOpenNewPage from '@/hooks/useOpenNewPage';

  import { ContractRouteEnum } from '@/enums/routeEnum';

  const props = defineProps<{
    sourceId: string;
    formKey: TimelineType;
  }>();

  const { t } = useI18n();
  const { openNewPage } = useOpenNewPage();

  const { data, loading, statisticInfo, getDescription, loadList, getStatistic, handleReachBottom } =
    useContractTimeline(props.formKey, props.sourceId);

  function goDetail(id: string) {
    openNewPage(ContractRouteEnum.CONTRACT_INDEX, {
      id,
    });
  }

  onMounted(() => {
    loadList();
    getStatistic();
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
