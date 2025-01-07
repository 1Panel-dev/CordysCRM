<template>
  <n-popover trigger="click" placement="bottom-end" @update:show="handleUpdateShow">
    <template #trigger>
      <CrmIcon type="icon-icon-setting" class="cursor-pointer" />
    </template>
    <div class="flex w-[175px] items-center justify-between text-[12px]">
      <div class="font-medium text-[var(--text-n1)]">{{ t('crmTable.columnSetting.tableHeaderDisplaySettings') }}</div>
      <n-button text size="tiny" :disabled="!hasChange" @click="handleReset">
        {{ t('crmTable.columnSetting.resetDefault') }}
      </n-button>
    </div>
    <VueDraggable v-model="cachedColumns" handle=".sort-handle" @change="handleChange">
      <div
        v-for="element in cachedColumns"
        :key="element.key"
        class="flex w-[175px] items-center justify-between py-[6px]"
      >
        <div class="flex items-center">
          <CrmIcon type="icon-icon_drag" class="sort-handle cursor-move text-[var(--text-n4)]" :size="12" />
          <span class="one-line-text ml-[8px] text-[12px]">
            {{ t(element.title as string) }}
          </span>
        </div>
        <n-switch v-model:value="element.showInTable" size="small" @update:value="handleChange" />
      </div>
    </VueDraggable>
  </n-popover>
</template>

<script setup lang="ts">
  import { NButton, NPopover, NSwitch } from 'naive-ui';

  import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';

  import { useI18n } from '@/hooks/useI18n';
  import { useTableStore } from '@/store';

  import type { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { VueDraggable } from 'vue-draggable-plus';

  const props = defineProps<{
    tableKey: TableKeyEnum;
  }>();

  const emit = defineEmits<{
    (e: 'changeColumnsSetting'): void; //  数据发生变化
  }>();

  const { t } = useI18n();
  const tableStore = useTableStore();

  const hasChange = ref(false); // 是否有改动
  const cachedColumns = ref<CrmDataTableColumn[]>([]);

  async function getCachedColumns() {
    const columns = await tableStore.getCanSetColumns(props.tableKey);
    cachedColumns.value = columns;
  }

  onBeforeMount(() => {
    if (props.tableKey) {
      getCachedColumns();
    }
  });

  function handleReset() {
    getCachedColumns();
    hasChange.value = false;
  }

  function handleChange() {
    hasChange.value = true;
  }

  async function handleUpdateShow(show: boolean) {
    if (!show) {
      if (hasChange.value) {
        await tableStore.setColumns(props.tableKey, [...cachedColumns.value]);
        emit('changeColumnsSetting');
        hasChange.value = false;
      }
    }
  }
</script>
