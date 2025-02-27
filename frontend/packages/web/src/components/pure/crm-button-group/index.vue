<template>
  <div class="crm-button-group">
    <div v-for="(item, index) of props.list" :key="item.key" class="crm-button-item flex items-center">
      <slot :name="item.slotName" :item="item" :index="index">
        <template v-if="item.popConfirmProps">
          <CrmPopConfirm
            :show="item.popShow as boolean"
            placement="bottom-end"
            v-bind="item.popConfirmProps"
            @confirm="emit('select', `pop-${item.key}` as string)"
            @cancel="emit('cancel')"
          >
            <n-button v-bind="item" type="primary" text class="!p-0" @click="() => (item.popShow = true)">
              {{ item.label }}
            </n-button>
            <template #content>
              <slot :key="item.key" :name="item.popSlotContent"></slot>
            </template>
          </CrmPopConfirm>
        </template>
        <template v-else>
          <n-button v-bind="item" type="primary" text class="!p-0" @click="() => emit('select', item.key as string)">
            {{ item.label }}
          </n-button>
        </template>
      </slot>
      <n-divider v-if="list[index + 1]" class="!mx-[8px]" vertical />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NButton, NDivider } from 'naive-ui';

  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmPopConfirm from '@/components/pure/crm-pop-confirm/index.vue';

  const props = defineProps<{
    list: ActionsItem[]; // 按钮组
  }>();

  const emit = defineEmits<{
    (e: 'select', key: string): void;
    (e: 'cancel'): void;
  }>();
</script>

<style scoped lang="less">
  .crm-button-group {
    @apply flex flex-nowrap items-center;
  }
</style>
