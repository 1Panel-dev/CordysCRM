<template>
  <n-select
    v-model:value="value"
    filterable
    remote
    clearable
    :options="options"
    :loading="loading"
    :placeholder="props.placeholder ?? t('common.pleaseSelect')"
    @search="handleSearch"
    @scroll="handleScroll"
    @update:value="handleUpdateValue"
  />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NSelect } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CommonList, TableQueryParams } from '@lib/shared/models/common';

  import { SelectMixedOption } from 'naive-ui/es/select/src/interface';

  const { t } = useI18n();

  const props = defineProps<{
    fetch: (params: TableQueryParams) => Promise<CommonList<Record<string, any>>>;
    placeholder?: string;
    pageSize?: number;
  }>();

  const emit = defineEmits<{
    (e: 'select', item: any): void;
  }>();

  const value = defineModel<string | null>('value', {
    default: null,
  });

  const options = ref<SelectMixedOption[]>([]);

  const loading = ref(false);
  const keyword = ref('');
  const current = ref(1);
  const defaultPageSize = 5;
  const hasMore = ref(true);

  async function loadData() {
    if (loading.value || !hasMore.value) return;

    loading.value = true;
    const res = await props.fetch({
      keyword: keyword.value,
      current: current.value,
      pageSize: props.pageSize ?? defaultPageSize,
    });

    const list = res.list || [];

    options.value.push(
      ...list.map((item) => ({
        label: item.name,
        value: item.id,
        raw: item,
      }))
    );

    if (options.value.length >= res.total) {
      hasMore.value = false;
    } else {
      current.value++;
    }

    loading.value = false;
  }

  async function handleSearch(val: string) {
    keyword.value = val.trim();
    options.value = [];
    current.value = 1;
    hasMore.value = true;

    if (keyword.value.length < 2) {
      return;
    }
    await loadData();
  }

  function handleScroll(e: Event) {
    if (loading.value || !hasMore.value) return;

    const currentTarget = e.currentTarget as HTMLElement;
    const reachBottom = currentTarget.scrollTop + currentTarget.offsetHeight >= currentTarget.scrollHeight;

    if (reachBottom) {
      loadData();
    }
  }

  function handleUpdateValue(val: string | null) {
    value.value = val;
    if (!val) return;

    const item = options.value.find((opt) => opt.value === val);
    if (item) {
      emit('select', item.raw);
    }
  }
</script>

<style scoped></style>
