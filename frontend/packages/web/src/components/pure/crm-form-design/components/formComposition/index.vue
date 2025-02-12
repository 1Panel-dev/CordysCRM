<template>
  <div class="crm-form-design--composition">
    <VueDraggable
      v-model="list"
      :disabled="disabled"
      :animation="150"
      ghost-class="crm-form-design--composition-item-ghost"
      group="crmFormDesign"
      class="grid h-full grid-cols-4 rounded-[var(--border-radius-small)]"
      :class="list.length > 0 ? '' : 'border border-dashed'"
      @start="onStart"
    >
      <div
        v-for="item in list"
        :key="item.id"
        class="crm-form-design--composition-item"
        :class="activeItem?.id === item.id ? 'crm-form-design--composition-item--active' : ''"
        :style="{ gridColumn: `span ${item.grid}` }"
        @click="() => handleItemClick(item)"
      >
        {{ t(item.name) }}
      </div>
      <div
        v-if="list.length === 0"
        class="col-span-4 flex h-full items-center justify-center text-[var(--text-n4)]"
        draggable="false"
      >
        {{ t('crmFormDesign.emptyTip') }}
      </div>
    </VueDraggable>
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@/hooks/useI18n';
  import { getGenerateId } from '@/utils';

  import { FieldTypeEnum } from '../../enum';
  import { VueDraggable } from 'vue-draggable-plus';

  interface CompositionItem {
    id: string;
    name: string;
    grid: number;
    type: FieldTypeEnum;
  }

  const { t } = useI18n();

  const list = ref<CompositionItem[]>([]);
  const activeItem = ref<Record<string, any>>();

  const disabled = ref(false);

  function onStart(e: any) {
    activeItem.value = e.data as CompositionItem;
  }

  function handleItemClick(item: Record<string, any>) {
    activeItem.value = item;
  }

  function addItem(type: FieldTypeEnum, name: string) {
    list.value.push({
      id: getGenerateId(),
      name: t(name),
      grid: 4,
      type,
    });
  }

  defineExpose({
    addItem,
  });
</script>

<style lang="less">
  .crm-form-design--composition {
    @apply h-full;

    padding: 24px;
    .crm-form-design--composition-item {
      @apply cursor-move;

      padding: 16px;
      border: 1px solid transparent;
      border-radius: var(--border-radius-small);
      &:hover {
        background-color: var(--text-n9);
      }
    }
    .crm-form-design--composition-item--active {
      border: 1px solid var(--primary-8);
      background-color: var(--primary-7);
    }
    .crm-form-design--composition-item-ghost {
      @apply flex items-center;

      padding: 6px 12px;
      border: 1px solid var(--primary-8);
      border-radius: var(--border-radius-small);
      background-color: var(--primary-7);
      gap: 8px;
      grid-column: span 4;
      line-height: 22px;
    }
  }
</style>
