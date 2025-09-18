<template>
  <CrmList
    v-if="props.multiple"
    ref="crmListRef"
    v-model:model-value="list"
    :keyword="props.keyword"
    :list-params="props.listParams"
    :load-list-api="props.loadListApi"
    :transform="props.transform"
  >
    <template #item="{ item }">
      <van-checkbox v-model="item.checked" :disabled="item.disabled" @change="handleChange">
        <slot name="label" :item="item">{{ item.name }}</slot>
      </van-checkbox>
    </template>
  </CrmList>
  <van-radio-group v-else v-model:model-value="value" shape="dot" class="h-full" @change="handleChange">
    <CrmList
      ref="crmListRef"
      v-model:model-value="list"
      v-model:loading="loading"
      :keyword="props.keyword"
      :list-params="props.listParams"
      :load-list-api="props.loadListApi"
      :no-page-nation="props.noPageNation"
      :transform="props.transform"
    >
      <template #item="{ item }">
        <van-radio :name="item.id" :disabled="item.disabled">
          <slot name="label" :item="item">{{ item.name }}</slot>
        </van-radio>
      </template>
    </CrmList>
  </van-radio-group>
</template>

<script setup lang="ts">
  import type { CommonList, TableQueryParams } from '@lib/shared/models/common';

  import CrmList from '@/components/pure/crm-list/index.vue';

  const props = defineProps<{
    data?: Record<string, any>[];
    keyword?: string;
    multiple?: boolean;
    listParams?: Record<string, any>;
    noPageNation?: boolean;
    loadListApi?: (params: TableQueryParams) => Promise<CommonList<Record<string, any>>>;
    transform?: (item: Record<string, any>, optionMap?: Record<string, any[]>) => Record<string, any>;
  }>();

  const value = defineModel<string | string[]>('value', {
    default: '',
  });
  const selectedRows = defineModel<Record<string, any>[]>('selectedRows', {
    default: [],
  });
  const loading = defineModel<boolean>('loading', {
    default: false,
  });

  const list = ref<Record<string, any>[]>([]);
  const crmListRef = ref<InstanceType<typeof CrmList>>();

  const selectedMap = ref<Map<string, Record<string, any>>>(new Map()); // 用 Map 来存储所有已选行，避免被过滤掉

  function handleChange() {
    if (props.multiple) {
      list.value.forEach((item) => {
        if (item.checked) {
          selectedMap.value.set(item.id, item);
        } else {
          selectedMap.value.delete(item.id);
        }
      });
      selectedRows.value = Array.from(selectedMap.value.values());
      value.value = selectedRows.value.map((e) => e.id);
    } else {
      selectedRows.value = list.value.filter((e) => e.id === value.value);
    }
  }

  function loadList(refresh = false) {
    crmListRef.value?.loadList(refresh);
  }

  function filterListByKeyword(keywordKey: string) {
    crmListRef.value?.filterListByKeyword(keywordKey);
  }

  onBeforeMount(() => {
    if (!props.multiple && Array.isArray(value.value)) {
      [value.value] = value.value;
    }
  });

  onMounted(() => {
    if (!props.multiple) {
      // 单选容器包裹 list 组件导致创建 list 组件时不会触发组件初始化加载，需要手动调用一下
      loadList(true);
    }
  });

  watch(
    () => props.data,
    (newData) => {
      list.value = newData || [];

      list.value.forEach((item) => {
        item.checked = selectedMap.value.has(item.id);
      });

      nextTick(() => {
        loadList(true);
      });
    },
    { immediate: true }
  );

  defineExpose({
    loadList,
    filterListByKeyword,
  });
</script>

<style lang="less" scoped>
  :deep(.van-radio):not(:first-child) {
    .half-px-border-top();
  }
  :deep(.van-radio) {
    @apply overflow-visible;
    .van-radio__label,
    .van-checkbox__label {
      @apply w-full;

      padding: 16px 16px 16px 0;
    }
  }
  :deep(.van-checkbox) {
    &:not(:first-child) {
      .half-px-border-top();
    }
    .van-checkbox__label {
      @apply w-full;

      padding: 16px 16px 16px 0;
    }
  }
</style>
