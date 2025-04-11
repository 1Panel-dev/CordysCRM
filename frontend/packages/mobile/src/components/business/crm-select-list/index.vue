<template>
  <CrmList
    v-if="props.multiple"
    v-model:model-value="list"
    :keyword="props.keyword"
    :list-params="props.listParams"
    :load-list-api="props.loadListApi"
    :transform="props.transform"
  >
    <template #item="{ item }">
      <van-checkbox v-model="item.checked" :disabled="item.disabled" @change="handleChange">
        <slot name="label">{{ item.name }}</slot>
      </van-checkbox>
    </template>
  </CrmList>
  <van-radio-group v-else v-model:model-value="value" shape="dot" @change="handleChange">
    <CrmList
      ref="crmListRef"
      v-model:model-value="list"
      :keyword="props.keyword"
      :list-params="props.listParams"
      :load-list-api="props.loadListApi"
      :no-page-nation="props.noPageNation"
      :transform="props.transform"
    >
      <template #item="{ item }">
        <van-radio :name="item.id" :disabled="item.disabled">
          <slot name="label">{{ item.name }}</slot>
        </van-radio>
      </template>
    </CrmList>
  </van-radio-group>
</template>

<script setup lang="ts">
  import type { CommonList, TableQueryParams } from '@lib/shared/models/common';

  import CrmList from '@/components/pure/crm-list/index.vue';

  const props = defineProps<{
    keyword?: string;
    multiple?: boolean;
    listParams?: Record<string, any>;
    noPageNation?: boolean;
    loadListApi: (params: TableQueryParams) => Promise<CommonList<Record<string, any>>>;
    transform?: (item: Record<string, any>, optionMap?: Record<string, any[]>) => Record<string, any>;
  }>();

  const value = defineModel<string | string[]>('value', {
    default: '',
  });
  const selectedRows = defineModel<Record<string, any>[]>('selectedRows', {
    default: [],
  });

  const list = ref<Record<string, any>[]>([]);
  const crmListRef = ref<InstanceType<typeof CrmList>>();

  function handleChange() {
    if (props.multiple) {
      selectedRows.value = list.value.filter((e) => e.checked);
      value.value = selectedRows.value.map((e) => e.id);
    } else {
      selectedRows.value = list.value.filter((e) => e.id === value.value);
    }
  }

  onMounted(() => {
    if (!props.multiple) {
      crmListRef.value?.loadList(true);
    }
  });
</script>

<style lang="less" scoped>
  :deep(.van-radio__label) {
    @apply w-full;
    .half-px-border-bottom();

    padding: 16px 16px 16px 0;
  }
</style>
