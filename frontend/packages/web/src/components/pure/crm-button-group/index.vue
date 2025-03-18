<template>
  <div class="crm-button-group">
    <div v-for="(item, index) of props.list" :key="item.key" class="crm-button-item flex items-center">
      <slot :name="item.slotName" :item="item" :index="index">
        <template v-if="item.popConfirmProps">
          <CrmPopConfirm
            v-model:show="popShow"
            placement="bottom-end"
            v-bind="item.popConfirmProps"
            @confirm="emit('select', `pop-${item.key}` as string, cancel)"
            @cancel="emit('cancel')"
          >
            <n-button
              text
              v-bind="item"
              type="primary"
              :class="item.text === false ? '' : '!p-0'"
              @click="() => (popShow = true)"
            >
              {{ item.label }}
            </n-button>
            <template #content>
              <slot :key="item.key" :name="item.popSlotContent"></slot>
            </template>
          </CrmPopConfirm>
        </template>
        <template v-else>
          <n-button
            text
            v-bind="item"
            :class="item.text === false ? '' : '!p-0'"
            type="primary"
            @click="() => emit('select', item.key as string)"
          >
            {{ item.label }}
          </n-button>
        </template>
      </slot>
      <n-divider v-if="list[index + 1] && !props.notShowDivider" class="!mx-[8px]" vertical />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NButton, NDivider } from 'naive-ui';

  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmPopConfirm from '@/components/pure/crm-pop-confirm/index.vue';

  const props = defineProps<{
    list: ActionsItem[]; // 按钮组
    notShowDivider?: boolean; // 不显示分割线
  }>();

  const popShow = ref(false);

  const emit = defineEmits<{
    (e: 'select', key: string, done?: () => void): void;
    (e: 'cancel'): void;
  }>();

  function cancel() {
    popShow.value = false;
  }
</script>

<style scoped lang="less">
  .crm-button-group {
    @apply flex flex-nowrap items-center;
  }
</style>
