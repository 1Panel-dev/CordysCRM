<template>
  <n-auto-complete
    v-model:value="value"
    :input-props="{ autocomplete: 'disabled' }"
    :clear-after-select="false"
    :options="options"
    :loading="loading"
    show-empty
    clearable
    :placeholder="props.placeholder ?? t('common.pleaseSelect')"
    @scroll="handleScroll"
    @select="handleSelect"
    @update:value="handleUpdateValue"
  />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NAutoComplete } from 'naive-ui';
  import { debounce } from 'lodash-es';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CommonList, TableQueryParams } from '@lib/shared/models/common';

  import { AutoCompleteOption } from 'naive-ui/es/auto-complete/src/interface';

  const { t } = useI18n();

  const props = withDefaults(
    defineProps<{
      fetch: (params: TableQueryParams) => Promise<CommonList<Record<string, any>>>;
      placeholder?: string;
      pageSize?: number;
      labelKey?: string;
      valueKey?: string;
    }>(),
    {
      labelKey: 'label',
      valueKey: 'value',
    }
  );

  const emit = defineEmits<{
    (e: 'select', item: any): void;
  }>();

  const value = defineModel<string | undefined>('value', {
    default: undefined,
  });

  const options = ref<AutoCompleteOption[]>([]);

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
        label: typeof item === 'string' ? item : item[props.labelKey],
        value: typeof item === 'string' ? item : item[props.valueKey],
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

  const handleSearch = debounce(async (val: string) => {
    keyword.value = val.trim();
    options.value = [];
    current.value = 1;
    hasMore.value = true;

    if (keyword.value.length <= 2) {
      return;
    }
    await loadData();
  }, 300);

  function handleScroll(e: Event) {
    if (loading.value || !hasMore.value) return;

    const currentTarget = e.currentTarget as HTMLElement;
    const reachBottom = currentTarget.scrollTop + currentTarget.offsetHeight >= currentTarget.scrollHeight;

    if (reachBottom) {
      loadData();
    }
  }

  function handleSelect(val: string) {
    value.value = val;
    const selectedItem = options.value.find((option) => option.value === val);
    if (selectedItem && selectedItem.raw) {
      emit('select', selectedItem.raw);
    }
  }

  function handleUpdateValue(val?: string) {
    value.value = val;
    handleSearch(val ?? '');
  }
</script>

<style scoped></style>
