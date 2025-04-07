<template>
  <n-popover
    v-model:show="popoverVisible"
    trigger="click"
    placement="bottom-end"
    class="crm-table-column-setting-popover"
    @update:show="handleUpdateShow"
  >
    <template #trigger>
      <CrmIcon
        type="iconicon_set_up"
        :class="`cursor-pointer ${popoverVisible ? 'text-[var(--primary-8)]' : ''}`"
        :size="16"
      />
    </template>
    <n-scrollbar class="max-h-[416px] px-[12px] py-[4px]">
      <div class="mb-[4px] flex h-[24px] w-[175px] items-center justify-between text-[12px]">
        <div class="font-medium text-[var(--text-n1)]">
          {{ t('crmTable.columnSetting.tableHeaderDisplaySettings') }}
        </div>
        <n-button text type="primary" size="tiny" :disabled="!hasChange" @click="handleReset">
          {{ t('crmTable.columnSetting.resetDefault') }}
        </n-button>
      </div>
      <VueDraggable v-model="cachedColumns" handle=".sort-handle" @change="handleChange">
        <div
          v-for="element in cachedColumns"
          :key="element.key"
          class="mb-[4px] flex w-[175px] items-center justify-between py-[3px]"
        >
          <div class="flex flex-1 items-center overflow-hidden">
            <CrmIcon type="iconicon_move" class="sort-handle cursor-move text-[var(--text-n4)]" :size="12" />
            <span class="one-line-text ml-[8px] text-[12px]">
              {{ t(element.title as string) }}
            </span>
          </div>
          <n-switch v-model:value="element.showInTable" size="small" @update:value="handleChange" />
        </div>
      </VueDraggable>
    </n-scrollbar>
  </n-popover>
</template>

<script setup lang="ts">
  import { NButton, NPopover, NScrollbar, NSwitch } from 'naive-ui';
  import { VueDraggable } from 'vue-draggable-plus';

  import type { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';

  import useTableStore from '@/hooks/useTableStore';

  const props = defineProps<{
    tableKey: TableKeyEnum;
  }>();

  const emit = defineEmits<{
    (e: 'changeColumnsSetting'): void; //  数据发生变化
  }>();

  const { t } = useI18n();
  const tableStore = useTableStore();

  const popoverVisible = ref(false);
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

<style lang="less">
  .crm-table-column-setting-popover {
    padding: 0 !important;
  }
</style>
