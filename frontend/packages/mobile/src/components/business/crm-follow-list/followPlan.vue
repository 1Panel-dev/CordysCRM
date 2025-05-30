<template>
  <div class="flex h-full flex-col overflow-hidden bg-[var(--text-n9)]">
    <div class="top-bar">
      <van-button v-if="!props.readonly" plain icon="plus" type="primary" size="small" @click="goCreate"> </van-button>
      <van-search
        v-model="keyword"
        shape="round"
        :placeholder="t('common.pleaseInputKeyword')"
        class="crm-search"
        @search="loadList"
        @clear="loadList"
      />
    </div>
    <CrmList
      ref="crmListRef"
      :keyword="keyword"
      :load-list-api="followPlanApiMap.list[props.type]"
      class="p-[16px]"
      :list-params="{
        sourceId: props.sourceId,
        status: CustomerFollowPlanStatusEnum.ALL,
      }"
      :item-gap="16"
      :transform="transformField"
    >
      <template #item="{ item }">
        <listItem
          v-model="item.status"
          :item="item"
          type="plan"
          :readonly="props.readonly"
          @click="goDetail(item)"
          @delete="handleDelete(item)"
          @edit="handleEdit(item)"
          @change="handleChangeStatus(item)"
        />
      </template>
    </CrmList>
  </div>
</template>

<script setup lang="ts">
  import { showSuccessToast } from 'vant';

  import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CustomerFollowPlanListItem, FollowDetailItem, StatusTagKey } from '@lib/shared/models/customer';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import listItem from './components/listItem.vue';

  import { followPlanApiMap, PlanEnumType } from '@/config/follow';

  import useFollowApi from './useFollowApi';

  const props = defineProps<{
    type: PlanEnumType;
    sourceId: string;
    readonly?: boolean;
    initialSourceName?: string;
  }>();

  const { t } = useI18n();
  const keyword = ref('');

  const crmListRef = ref<InstanceType<typeof CrmList>>();

  const { transformField, goCreate, handleEdit, goDetail } = useFollowApi({
    type: 'followPlan',
    formKey: props.type,
    sourceId: props.sourceId,
    initialSourceName: props.initialSourceName,
    readonly: props.readonly,
  });

  async function handleDelete(item: FollowDetailItem) {
    try {
      await followPlanApiMap.delete?.[props.type]?.(item.id);
      showSuccessToast(t('common.deleteSuccess'));
      crmListRef.value?.loadList(true);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function handleChangeStatus(item: FollowDetailItem) {
    try {
      await followPlanApiMap.changeStatus?.[props.type]?.({
        id: item.id,
        status: (item as CustomerFollowPlanListItem).status,
      });
      showSuccessToast(t('common.operationSuccess'));
      crmListRef.value?.loadList(true);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function loadList() {
    nextTick(() => {
      crmListRef.value?.loadList(true);
    });
  }

  defineExpose({
    loadList,
  });
</script>

<style lang="less" scoped></style>
