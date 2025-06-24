<template>
  <n-select
    v-model:value="innerValue"
    v-bind="{
      valueField: props.valueField,
      labelField: props.labelField,
      ...$attrs,
    }"
    filterable
    clearable
    max-tag-count="responsive"
    :options="sortedOptions"
    :placeholder="props.placeholder || t('common.pleaseSelect')"
    @search="handleSearch"
    @update:value="change"
  />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NSelect } from 'naive-ui';
  import { debounce } from 'lodash-es';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import useLocalForage from '@/hooks/useLocalForage';

  import { SelectBaseOption, SelectMixedOption } from 'naive-ui/es/select/src/interface';

  const { t } = useI18n();
  const { setItem, getItem } = useLocalForage();

  export interface CrmUserSelectProps {
    mode?: 'remote' | 'static';
    labelField?: string;
    valueField?: string;
    placeholder?: string;
    options?: SelectMixedOption[];
    fetchApi?: (params: Record<string, any>) => Promise<Record<string, any>[]>;
    params?: Record<string, any>;
    disabledIds?: string[];
  }

  const props = withDefaults(defineProps<CrmUserSelectProps>(), {
    mode: 'static',
    labelField: 'label',
    valueField: 'value',
  });

  const emit = defineEmits<{
    (
      e: 'change',
      value: Array<string | number> | string | number | null,
      option: SelectBaseOption | null | SelectBaseOption[]
    ): void;
  }>();

  const innerValue = defineModel<string | number | Array<string | number> | null>('value', {
    required: true,
    default: null,
  });

  const recentlyUserIds = ref<string[]>([]);
  const optionsList = ref<SelectMixedOption[]>([]);

  const loadUsers = async (keyword = '') => {
    if (props.mode !== 'remote' || !props.fetchApi) return;

    try {
      const res = await props.fetchApi({ keyword, ...props.params });
      optionsList.value = res.map((user) => ({
        [props.labelField]: user[props.labelField],
        [props.valueField]: user[props.valueField],
        disabled: props.disabledIds?.includes(user[props.valueField]),
      }));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  };

  const handleSearch = debounce(async (query: string) => {
    if (props.mode === 'remote' && props.fetchApi) {
      await loadUsers(query);
    }
  }, 100);

  function updateRecentlyUserIds(value: string | number) {
    if (!value) return;
    const newIds = [String(value), ...recentlyUserIds.value.filter((id) => id !== String(value))];
    recentlyUserIds.value = newIds.slice(0, 5); // 保持最近5个用户
    setItem('recentlyUserIds', recentlyUserIds.value);
  }

  function change(
    value: Array<string | number> | string | number | null,
    option: SelectBaseOption | null | SelectBaseOption[]
  ) {
    innerValue.value = value;
    updateRecentlyUserIds(value as string | number);
    emit('change', value, option);
  }

  const computedOptions = computed<SelectMixedOption[]>(() => {
    return props.mode === 'static'
      ? (props.options || []).map((item) => ({
          ...item,
          [props.labelField]: item[props.labelField],
          [props.valueField]: item[props.valueField],
          disabled: props.disabledIds?.includes(item[props.valueField] as string),
        }))
      : optionsList.value;
  });

  const sortedOptions = computed<SelectMixedOption[]>(() => {
    const sorted = [...computedOptions.value];
    recentlyUserIds.value.forEach((id) => {
      const index = sorted.findIndex((item) => item[props.valueField] === id);
      if (index !== -1) {
        const [item] = sorted.splice(index, 1);
        sorted.push(item);
      }
    });
    return sorted;
  });

  onMounted(async () => {
    loadUsers();
    const ids = await getItem<string[]>('recentlyUserIds');
    if (ids) {
      recentlyUserIds.value = ids;
    }
  });

  defineExpose({
    loadUsers,
  });
</script>

<style scoped></style>
