<template>
  <n-select
    v-model:value="innerValue"
    v-bind="{
      valueField: props.valueField,
      labelField: props.labelField,
      ...$attrs,
    }"
    :options="computedOptions"
    :placeholder="props.placeholder || t('common.pleaseSelect')"
    @search="handleSearch"
    @update:value="change"
  />
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NSelect, SelectOption } from 'naive-ui';
  import { debounce } from 'lodash-es';

  import { useI18n } from '@/hooks/useI18n';

  import { SelectBaseOption } from 'naive-ui/es/select/src/interface';

  const { t } = useI18n();

  const props = withDefaults(
    defineProps<{
      value: string | number | Array<string | number> | null;
      mode?: 'remote' | 'static';
      labelField?: string;
      valueField?: string;
      placeholder?: string;
      options?: SelectOption[];
      fetchApi?: (params: Record<string, any>) => Promise<Record<string, any>[]>;
      params?: Record<string, any>;
    }>(),
    {
      mode: 'static',
      labelField: 'label',
      valueField: 'value',
    }
  );

  const emit = defineEmits<{
    (
      e: 'change',
      value: Array<string | number> | string | number | null,
      option: SelectBaseOption | null | SelectBaseOption[]
    ): void;
  }>();

  const innerValue = ref<string | number | Array<string | number> | null>(props.value ?? null);

  const innerOptions = ref<SelectOption[]>([]);

  const loadUsers = async (keyword = '') => {
    if (props.mode !== 'remote' || !props.fetchApi) return;

    try {
      const res = await props.fetchApi({ keyword, ...props.params });
      innerOptions.value = res.map((user) => ({
        [props.labelField]: user[props.labelField],
        [props.valueField]: user[props.valueField],
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

  function change(
    value: Array<string | number> | string | number | null,
    option: SelectBaseOption | null | SelectBaseOption[]
  ) {
    innerValue.value = value;
    emit('change', value, option);
  }

  const computedOptions = computed(() => {
    return props.mode === 'static'
      ? (props.options || []).map((item) => ({
          ...item,
          [props.labelField]: item[props.labelField],
          [props.valueField]: item[props.valueField],
        }))
      : innerOptions.value;
  });

  onMounted(() => {
    loadUsers();
  });

  watch(
    () => props.value,
    (newValue) => {
      innerValue.value = newValue;
    }
  );
</script>

<style scoped></style>
