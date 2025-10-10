<template>
  <div
    class="flex h-full flex-col gap-[8px] overflow-hidden rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]"
  >
    <div class="flex items-center justify-between">
      <CrmTag :type="getStageColor" theme="dark">{{ getStageText }}</CrmTag>
      <div class="font-semibold">
        {{
          `${abbreviateNumber(statisticInfo?.amount, '').value} ${abbreviateNumber(statisticInfo?.amount, '').unit} / ${
            pageNation.total
          }`
        }}
      </div>
    </div>
    <n-progress type="line" color="var(--primary-8)" rail-color="var(--text-n8)" :percentage="getStagePercentage" />
    <CrmList
      v-show="list.length > 0 || loading"
      v-model:data="list"
      virtual-scroll-height="100%"
      key-field="id"
      :item-height="268"
      :loading="loading"
      mode="remote"
      ignore-item-resize
      class="mb-[24px] !flex-1"
      @reach-bottom="handleReachBottom"
    >
      <template #item="{ item }">
        <VueDraggable
          :id="item.id"
          v-model="list"
          :animation="150"
          ghost-class="opportunity-billboard-item-ghost"
          group="opportunity-billboard"
          handle=".handle"
          :class="`${props.stage}-draggable`"
          @update="onUpdate"
          @add="onAdd"
          @move="handleMove"
        >
          <div class="opportunity-billboard-item handle">
            <div class="flex items-center justify-between">
              <CrmTableButton @click="jumpToDetail('opportunity', item.id)">
                <template #trigger>{{ item.name }}</template>
                {{ item.name }}
              </CrmTableButton>
            </div>
            <div class="opportunity-billboard-item-desc">
              <n-tooltip trigger="hover" :delay="300">
                <template #trigger>
                  <div class="opportunity-billboard-item-desc-label">
                    {{ fieldLabelMap.amount }}
                  </div>
                </template>
                {{ fieldLabelMap.amount }}
              </n-tooltip>
              <div class="opportunity-billboard-item-desc-value">{{ item.amount }}</div>
            </div>
            <div class="opportunity-billboard-item-desc">
              <n-tooltip trigger="hover" :delay="300">
                <template #trigger>
                  <div class="opportunity-billboard-item-desc-label">
                    {{ fieldLabelMap.products }}
                  </div>
                </template>
                {{ fieldLabelMap.products }}
              </n-tooltip>
              <div class="opportunity-billboard-item-desc-value">
                <CrmTagGroup :tags="getProductNames(item.products)" />
              </div>
            </div>
            <div class="opportunity-billboard-item-desc">
              <n-tooltip trigger="hover" :delay="300">
                <template #trigger>
                  <div class="opportunity-billboard-item-desc-label">
                    {{ fieldLabelMap.customerId }}
                  </div>
                </template>
                {{ fieldLabelMap.customerId }}
              </n-tooltip>
              <div class="opportunity-billboard-item-desc-value">
                <CrmTableButton size="small" class="text-[14px]" @click="jumpToDetail('customer', item.customerId)">
                  <template #trigger>{{ item.customerName }}</template>
                  {{ item.customerName }}
                </CrmTableButton>
              </div>
            </div>
            <div class="opportunity-billboard-item-desc">
              <n-tooltip trigger="hover" :delay="300">
                <template #trigger>
                  <div class="opportunity-billboard-item-desc-label">
                    {{ fieldLabelMap.owner }}
                  </div>
                </template>
                {{ fieldLabelMap.owner }}
              </n-tooltip>
              <div class="opportunity-billboard-item-desc-value">{{ item.ownerName }}</div>
            </div>
            <div class="opportunity-billboard-item-desc">
              <n-tooltip trigger="hover" :delay="300">
                <template #trigger>
                  <div class="opportunity-billboard-item-desc-label">
                    {{ fieldLabelMap.expectedEndTime }}
                  </div>
                </template>
                {{ fieldLabelMap.expectedEndTime }}
              </n-tooltip>
              <div
                class="opportunity-billboard-item-desc-value"
                :class="{ 'text-[var(--error-red)]': dayjs(item.expectedEndTime).isSame(dayjs(), 'M') }"
              >
                {{ dayjs(item.expectedEndTime).format('YYYY-MM-DD') }}
              </div>
            </div>
          </div>
        </VueDraggable>
      </template>
    </CrmList>
    <div v-if="list.length === 0 && !loading" class="flex h-full flex-1 items-center justify-center">
      <n-empty :description="t('common.noData')"> </n-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NEmpty, NProgress, NTooltip } from 'naive-ui';
  import dayjs from 'dayjs';
  import { VueDraggable } from 'vue-draggable-plus';

  import { OpportunityStatusEnum, StageResultEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { abbreviateNumber } from '@lib/shared/method';

  import { FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmTagGroup from '@/components/pure/crm-tag-group/index.vue';
  import { FormCreateField } from '@/components/business/crm-form-create/types';

  import { getOpportunityList, getOptStatistic, sortOpportunity } from '@/api/modules';
  import useOpenNewPage from '@/hooks/useOpenNewPage';
  import { hasAnyPermission } from '@/utils/permission';

  import { CustomerRouteEnum, OpportunityRouteEnum } from '@/enums/routeEnum';

  const props = defineProps<{
    keyword?: string;
    refreshTimeStamp?: number;
    advanceFilter?: FilterResult;
    stage: string[];
    fieldList: FormCreateField[];
  }>();
  const emit = defineEmits<{
    (e: 'change'): void;
  }>();

  const { t } = useI18n();
  const { openNewPage } = useOpenNewPage();

  const fieldLabelMap = computed(() => {
    const map: Record<string, string> = {};
    props.fieldList.forEach((field) => {
      if (field.businessKey) {
        map[field.businessKey] = field.name;
      }
    });
    return map;
  });
  const getStagePercentage = computed(() => {
    if (props.stage.length === 0 || props.stage[0] === StageResultEnum.FAIL) {
      return 0;
    }
    if (props.stage[0] === OpportunityStatusEnum.CREATE) {
      return 10;
    }
    if (props.stage[0] === OpportunityStatusEnum.CLEAR_REQUIREMENTS) {
      return 30;
    }
    if (props.stage[0] === OpportunityStatusEnum.SCHEME_VALIDATION) {
      return 50;
    }
    if (props.stage[0] === OpportunityStatusEnum.PROJECT_PROPOSAL_REPORT) {
      return 70;
    }
    if (props.stage[0] === OpportunityStatusEnum.BUSINESS_PROCUREMENT) {
      return 90;
    }
    if (props.stage[0] === StageResultEnum.SUCCESS) {
      return 100;
    }
    return 0;
  });
  const getStageText = computed(() => {
    if (props.stage[0] === StageResultEnum.SUCCESS) {
      return `${t('opportunity.end')}/${t('common.success')}`;
    }
    if (props.stage[0] === StageResultEnum.FAIL) {
      return `${t('opportunity.end')}/${t('common.fail')}`;
    }
    if (props.stage[0] === OpportunityStatusEnum.CREATE) {
      return t('common.create');
    }
    if (props.stage[0] === OpportunityStatusEnum.CLEAR_REQUIREMENTS) {
      return t('opportunity.clearRequirements');
    }
    if (props.stage[0] === OpportunityStatusEnum.SCHEME_VALIDATION) {
      return t('opportunity.schemeValidation');
    }
    if (props.stage[0] === OpportunityStatusEnum.PROJECT_PROPOSAL_REPORT) {
      return t('opportunity.projectProposalReport');
    }
    if (props.stage[0] === OpportunityStatusEnum.BUSINESS_PROCUREMENT) {
      return t('opportunity.businessProcurement');
    }
    return '';
  });
  const getStageColor = computed(() => {
    if (props.stage[0] === StageResultEnum.SUCCESS) {
      return 'success';
    }
    if (props.stage[0] === StageResultEnum.FAIL) {
      return 'error';
    }
    if (props.stage[0] === OpportunityStatusEnum.CREATE) {
      return 'info';
    }
    return 'primary';
  });

  const list = ref<any[]>([]);
  const pageNation = ref({
    total: 0,
    pageSize: 10,
    current: 1,
  });
  const optionMap = ref<Record<string, any>>({});
  const loading = ref(false);
  const finished = ref(false);
  async function loadOpportunityList(refresh = true) {
    try {
      loading.value = true;
      if (refresh) {
        finished.value = false;
        pageNation.value.current = 1;
      }
      const res = await getOpportunityList({
        current: pageNation.value.current || 1,
        pageSize: pageNation.value.pageSize,
        keyword: props.keyword,
        combineSearch: props.advanceFilter,
        filters: [
          {
            name: 'stage',
            value: props.stage,
            multipleValue: false,
            operator: 'IN',
          },
        ],
        board: true,
        viewId: 'ALL',
      });
      if (res) {
        list.value = [];
        list.value = list.value.concat(res.list);
        pageNation.value.total = res.total;
        optionMap.value = res.optionMap || {};
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
      finished.value = true;
    }
  }

  const statisticInfo = ref({ amount: 0, averageAmount: 0 });
  async function getStatistic() {
    try {
      const res = await getOptStatistic({
        keyword: props.keyword,
        combineSearch: props.advanceFilter,
        filters: [
          {
            name: 'stage',
            value: props.stage,
            multipleValue: false,
            operator: 'IN',
          },
        ],
        viewId: 'ALL',
      });
      statisticInfo.value = res;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  function getProductNames(ids: string[]) {
    const products = optionMap.value.products || [];
    return products.filter((product: any) => ids.includes(product.id)).map((product: any) => product.name);
  }

  function handleReachBottom() {
    pageNation.value.current += 1;
    if (pageNation.value.current > Math.ceil(pageNation.value.total / pageNation.value.pageSize)) {
      return;
    }
    loadOpportunityList(false);
  }

  function refreshList() {
    loadOpportunityList();
    getStatistic();
  }

  watch(
    () => props.refreshTimeStamp,
    () => {
      refreshList();
    }
  );

  async function onUpdate(item: any) {
    try {
      loading.value = true;
      await sortOpportunity({
        dropNodeId: item.to.id,
        dragNodeId: item.data.id,
        dropPosition: -1,
        stage: props.stage[0] || '',
      });
      refreshList();
      emit('change');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  async function onAdd(item: any) {
    try {
      loading.value = true;
      await sortOpportunity({
        dropNodeId: item.to.id,
        dragNodeId: item.data.id,
        dropPosition: -1,
        stage: props.stage[0] || '',
      });
      refreshList();
      emit('change');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  function handleMove(evt: any) {
    // 禁止拖拽到不可放置的位置
    if (evt.to.className.includes(props.stage[0]) && evt.from.className.includes(props.stage[0])) {
      return true;
    }
    if (evt.data.stage === StageResultEnum.SUCCESS || evt.data.stage === StageResultEnum.FAIL) {
      return hasAnyPermission(['OPPORTUNITY_MANAGEMENT:RESIGN']);
    }
    return true;
  }

  function jumpToDetail(type: 'customer' | 'opportunity', id: string) {
    if (type === 'customer') {
      openNewPage(CustomerRouteEnum.CUSTOMER_INDEX, {
        id,
      });
    } else if (type === 'opportunity') {
      openNewPage(OpportunityRouteEnum.OPPORTUNITY_OPT, {
        id,
      });
    }
  }

  onBeforeMount(() => {
    getStatistic();
    loadOpportunityList();
  });
</script>

<style lang="less" scoped>
  .opportunity-billboard-item {
    @apply flex cursor-move flex-col;

    margin-bottom: 8px;
    padding: 16px;
    border-radius: var(--border-radius-small);
    background-color: var(--text-n10);
    gap: 8px;
    .opportunity-billboard-item-desc {
      @apply flex items-center;

      gap: 16px;
      .opportunity-billboard-item-desc-label {
        @apply overflow-hidden overflow-ellipsis whitespace-nowrap;

        width: 60px;
        color: var(--text-n4);
        line-height: 24px;
        word-break: keep-all;
      }
      .opportunity-billboard-item-desc-value {
        @apply flex-1 overflow-hidden;

        color: var(--text-n1);
      }
    }
  }
  .opportunity-billboard-item-ghost {
    @apply flex flex-col;

    padding: 16px;
    border: 1px solid var(--primary-8);
    border-radius: var(--border-radius-small);
    background-color: var(--primary-7);
    gap: 8px;
  }
  :deep(.n-progress-icon--as-text) {
    word-break: keep-all;
  }
  :deep(.n-spin-container) {
    height: calc(100% - 48px);
  }
</style>
