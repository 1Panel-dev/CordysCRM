<template>
  <div class="p-[16px]">
    <n-checkbox :checked="isAllSelected" :indeterminate="isIndeterminate" @update:checked="handleMasterChange">
      <span class="font-semibold">
        {{ title }}
      </span>
    </n-checkbox>

    <n-checkbox-group :value="selectedIds" class="mt-[16px]" @update:value="handleSelectionChange">
      <div class="flex flex-row flex-wrap gap-[16px]">
        <n-checkbox v-for="item in items" :key="item.key" :value="item.key" class="w-[94px]">
          <n-tooltip placement="top" :delay="300">
            <template #trigger>
              <div class="one-line-text max-w-[80px]">{{ item.title }}</div>
            </template>
            {{ item.title }}
          </n-tooltip>
        </n-checkbox>
      </div>
    </n-checkbox-group>
  </div>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import { NCheckbox, NCheckboxGroup, NTooltip } from 'naive-ui';

  const props = defineProps<{
    title: string;
    items: any[];
  }>();

  const selectedIds = defineModel<string[]>('selectedIds', { required: true });

  const isAllSelected = computed(() => {
    return props.items.length > 0 && selectedIds.value.length === props.items.length;
  });

  const isIndeterminate = computed(() => {
    return selectedIds.value.length > 0 && selectedIds.value.length < props.items.length;
  });

  const handleMasterChange = (checked: boolean) => {
    if (checked) {
      selectedIds.value = props.items.map((item) => item.key);
    } else {
      selectedIds.value = [];
    }
  };

  const handleSelectionChange = (newSelection: (string | number)[]) => {
    selectedIds.value = newSelection as string[];
  };
</script>
