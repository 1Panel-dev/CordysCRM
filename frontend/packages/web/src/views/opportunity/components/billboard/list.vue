<template>
  <div
    class="flex h-full flex-col gap-[8px] overflow-hidden rounded-[var(--border-radius-small)] bg-[var(--text-n9)] p-[16px]"
  >
    <div class="flex items-center justify-between">
      <CrmTag :type="getStageColor" theme="dark">{{ props.stageConfig.name }}</CrmTag>
      <div class="font-semibold">
        {{
          `${abbreviateNumber(statisticInfo?.amount, '').value} ${abbreviateNumber(statisticInfo?.amount, '').unit} / ${
            pageNation.total
          }`
        }}
      </div>
    </div>
    <n-progress
      type="line"
      color="var(--primary-8)"
      rail-color="var(--text-n8)"
      :percentage="Number(props.stageConfig.rate)"
    />
    <n-spin :show="loading" content-class="h-full">
      <VueDraggable
        v-model="list"
        :animation="150"
        ghost-class="opportunity-billboard-item-ghost"
        :group="{ name: 'opportunity', pull: 'clone', put: true }"
        target=".n-scrollbar-content"
        class="h-full"
        @add="handleAddItem"
        @move="handleMove"
        @update="handleUpdate"
        @remove="handleRemove"
      >
        <n-scrollbar :content-class="`${props.stageConfig.id} h-full`" @scroll="handleReachBottom">
          <div v-for="item in list" :key="item.id" class="opportunity-billboard-item">
            <div class="flex items-center justify-between">
              <CrmTableButton v-if="item.name" @click="jumpToDetail('opportunity', item.id)">
                <template #trigger>{{ item.name }}</template>
                {{ item.name }}
              </CrmTableButton>
              <div v-else>-</div>
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
              <div class="opportunity-billboard-item-desc-value">
                {{
                  formatNumberValue(
                    item.amount,
                    (fieldList.find((field) => field.businessKey === 'amount') as FormCreateField) || {}
                  )
                }}
              </div>
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
                <CrmTagGroup v-if="item.products.length > 0" :tags="getProductNames(item.products)" />
                <div v-else>-</div>
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
                <CrmTableButton
                  v-if="item.customerName"
                  size="small"
                  class="text-[14px]"
                  @click="jumpToDetail('customer', item.customerId)"
                >
                  <template #trigger>{{ item.customerName }}</template>
                  {{ item.customerName }}
                </CrmTableButton>
                <div v-else>-</div>
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
                :class="{ '!text-[var(--error-red)]': dayjs(item.expectedEndTime).isSame(dayjs(), 'M') }"
              >
                {{ item.expectedEndTime ? dayjs(item.expectedEndTime).format('YYYY-MM-DD') : '-' }}
              </div>
            </div>
          </div>
          <div v-if="list.length === 0 && !loading" class="flex h-[300px] flex-1 items-center justify-center">
            <n-empty :description="t('common.noData')"> </n-empty>
          </div>
        </n-scrollbar>
      </VueDraggable>
    </n-spin>
  </div>
</template>

<script setup lang="ts">
  import { NEmpty, NProgress, NScrollbar, NSpin, NTooltip } from 'naive-ui';
  import dayjs from 'dayjs';
  import { VueDraggable } from 'vue-draggable-plus';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { abbreviateNumber, formatNumberValue } from '@lib/shared/method';
  import { OpportunityItem, StageConfigItem } from '@lib/shared/models/opportunity';

  import { FilterResult } from '@/components/pure/crm-advance-filter/type';
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
    stageConfig: StageConfigItem;
    index: number;
    fieldList: FormCreateField[];
    enableReason?: boolean;
    failId?: string;
    stageIds: string[];
    viewId?: string;
  }>();
  const emit = defineEmits<{
    (e: 'fail', item: OpportunityItem): void;
    (e: 'change', stage: string): void;
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
  const isSuccess = computed(() => props.stageConfig?.type === 'END' && props.stageConfig?.rate === '100');
  const isFail = computed(() => props.stageConfig?.type === 'END' && props.stageConfig?.rate === '0');
  const getStageColor = computed(() => {
    if (isSuccess.value) {
      return 'success';
    }
    if (isFail.value) {
      return 'error';
    }
    if (props.index === 0) {
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
            value: props.stageConfig.id,
            multipleValue: false,
            operator: 'EQUALS',
          },
        ],
        board: true,
        viewId: props.viewId,
      });
      if (res) {
        if (refresh) {
          list.value = [];
        }
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
            value: props.stageConfig.id,
            multipleValue: false,
            operator: 'EQUALS',
          },
        ],
        viewId: props.viewId,
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

  function handleReachBottom(e: Event) {
    const el = e.target as HTMLElement;
    if (el.scrollHeight - el.scrollTop <= el.clientHeight) {
      pageNation.value.current += 1;
      if (pageNation.value.current > Math.ceil(pageNation.value.total / pageNation.value.pageSize)) {
        return;
      }
      loadOpportunityList(false);
    }
  }

  function refreshList() {
    loadOpportunityList();
    getStatistic();
  }

  watch([() => props.refreshTimeStamp, () => props.viewId], () => {
    refreshList();
  });

  async function handleUpdate(item: any) {
    try {
      loading.value = true;
      const lastItem = list.value[item.newIndex - 1];
      const nextItem = list.value[item.newIndex + 1];
      await sortOpportunity({
        dropNodeId: nextItem?.id || lastItem?.id || '',
        dragNodeId: item.data.id,
        dropPosition: nextItem ? -1 : 1,
        stage: props.stageConfig.id || '',
      });
      refreshList();
      if (item.to.className !== item.from.className) {
        emit('change', item.from.className.split(' ')[1]);
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  function handleRemove(item: any) {
    // 从当前列表移除
    list.value = list.value.filter((i) => i.id !== item.data.id);
    emit('change', item.from.className.split(' ')[1]);
  }

  async function handleAddItem(item: any) {
    // 开启失败原因需要填写失败原因
    if (item.data.stage !== props.stageConfig.id && isFail.value && props.enableReason) {
      emit('fail', item);
      list.value = list.value.filter((i) => i.id !== item.data.id);
      return;
    }
    try {
      loading.value = true;
      const lastItem = list.value[item.newIndex - 1];
      const nextItem = list.value[item.newIndex + 1];
      await sortOpportunity({
        dropNodeId: nextItem?.id || lastItem?.id || '',
        dragNodeId: item.data.id,
        dropPosition: nextItem ? -1 : 1,
        stage: props.stageConfig.id || '',
      });
      refreshList();
      if (item.to.className !== item.from.className) {
        emit('change', item.from.className.split(' ')[1]);
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  async function sortItem(item: any) {
    try {
      loading.value = true;
      const lastItem = list.value[item.newIndex - 1];
      const nextItem = list.value[item.newIndex + 1];
      await sortOpportunity({
        dropNodeId: nextItem?.id || lastItem?.id || '',
        dragNodeId: item.data.id,
        dropPosition: nextItem ? -1 : 1,
        stage: props.stageConfig.id || '',
      });
      refreshList();
      emit('change', item.from.className.split(' ')[1]);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  function handleMove(evt: any) {
    // 同一列表随意拖拽
    if (evt.to.className.includes(props.stageConfig.id) && evt.from.className.includes(props.stageConfig.id)) {
      return true;
    }
    // 完结状态更改需要返签权限，或者开启回退才能回退
    if (isSuccess.value) {
      return (
        (props.stageConfig.endRollBack && !evt.to.className.includes(props.failId)) ||
        (hasAnyPermission(['OPPORTUNITY_MANAGEMENT:RESIGN']) && evt.to.className.includes(props.failId))
      );
    }
    // 失败状态更改需要开启回退权限
    if (isFail.value) {
      return props.stageConfig.endRollBack;
    }
    // 非完结状态只能向后拖拽，开启回退权限可以向前拖拽
    if (props.stageIds.findIndex((id) => id === evt.to.className.split(' ')[1]) < props.index) {
      return props.stageConfig.afootRollBack;
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

  defineExpose({
    refreshList,
    sortItem,
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
