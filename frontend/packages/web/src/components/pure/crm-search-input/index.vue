<template>
  <n-input
    v-model:value="keyword"
    :placeholder="t(props.placeholder || 'common.searchByName')"
    clearable
    :class="props.class || '!w-[240px]'"
    @keydown="handleKeyDown"
    @input="handleInput"
    @clear="() => emit('search', '')"
  >
    <template #suffix>
      <n-icon>
        <Search />
      </n-icon>
    </template>
  </n-input>
</template>

<script setup lang="ts">
  import { NIcon, NInput } from 'naive-ui';
  import { Search } from '@vicons/ionicons5';
  import { debounce } from 'lodash-es';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  const props = defineProps<{
    autoSearch?: boolean;
    debounceTime?: number;
    placeholder?: string;
    class?: string;
  }>();
  const emit = defineEmits<{
    (e: 'search', value: string): void;
  }>();

  const { t } = useI18n();

  const keyword = defineModel<string>('value', {
    required: true,
  });

  function handleKeyDown(e: KeyboardEvent) {
    if (e.key === 'Enter') {
      emit('search', keyword.value);
    }
  }

  const handleInput = debounce(() => {
    if (props.autoSearch) {
      emit('search', keyword.value);
    }
  }, props.debounceTime || 300);
</script>

<style lang="less" scoped></style>
