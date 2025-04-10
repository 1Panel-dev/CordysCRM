<template>
  <CrmList v-model:model-value="list" :keyword="props.keyword">
    <template #item="{ item }">
      <van-checkbox v-if="props.multiple" v-model="item.checked" :disabled="item.disabled" @change="handleChange">
        <slot name="label">{{ item.name }}</slot>
      </van-checkbox>
      <van-radio-group v-else v-model="item.checked" :disabled="item.disabled" @change="handleChange">
        <van-radio :name="true">
          <slot name="label">{{ item.name }}</slot>
        </van-radio>
      </van-radio-group>
    </template>
  </CrmList>
</template>

<script setup lang="ts">
  import CrmList from '@/components/pure/crm-list/index.vue';

  const props = defineProps<{
    keyword?: string;
    multiple?: boolean;
  }>();

  const value = defineModel<string | string[]>('value', {
    default: '',
  });
  const selectedRows = defineModel<Record<string, any>[]>('selectedRows', {
    default: [],
  });

  const list = ref<Record<string, any>[]>([]);

  function handleChange() {
    if (props.multiple) {
      selectedRows.value = list.value.filter((item) => item.checked);
      value.value = selectedRows.value.map((item) => item.id);
    } else {
      selectedRows.value = list.value.filter((item) => item.checked);
      value.value = selectedRows.value[0]?.id || '';
    }
  }
</script>

<style lang="less" scoped>
  :deep(.van-radio__label) {
    @apply w-full;
    .half-px-border-bottom();

    padding: 16px 16px 16px 0;
  }
</style>
