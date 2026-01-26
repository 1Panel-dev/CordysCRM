<template>
  <div class="crm-button-group">
    <div v-for="(item, index) of buttonGroupList" :key="item.key" class="crm-button-item flex items-center">
      <slot :name="item.slotName" :item="item" :index="index">
        <template v-if="item.popConfirmProps">
          <CrmPopConfirm
            v-model:show="popShow[item.key as string]"
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
              @click="() => (popShow[item.key as string] = true)"
            >
              {{ item.label }}
            </n-button>
            <template #content>
              <slot :key="item.key" :name="item.popSlotContent"></slot>
            </template>
          </CrmPopConfirm>
        </template>
        <template v-else>
          <slot>
            <n-tooltip :delay="300" :disabled="!item.tooltipContent">
              <template #trigger>
                <n-button
                  text
                  v-bind="item"
                  :class="item.text === false ? '' : '!p-0'"
                  :type="item.danger ? 'error' : 'primary'"
                  @click="() => emit('select', item.key as string)"
                >
                  {{ item.label }}
                </n-button>
              </template>
              {{ item.tooltipContent }}
            </n-tooltip>
          </slot>
        </template>
      </slot>
      <n-divider v-if="buttonGroupList[index + 1] && !props.notShowDivider" class="!mx-[8px]" vertical />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, watch } from 'vue';
  import { NButton, NDivider, NTooltip } from 'naive-ui';

  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmPopConfirm from '@/components/pure/crm-pop-confirm/index.vue';

  import { hasAllPermission } from '@/utils/permission';

  const props = defineProps<{
    list: ActionsItem[]; // 按钮组
    notShowDivider?: boolean; // 不显示分割线
  }>();

  const popShow = ref<Record<string, boolean>>({});

  // 初始化
  watch(
    () => props.list,
    (newList) => {
      newList.forEach((item) => {
        if (item.popConfirmProps) {
          popShow.value[item.key as string] = false;
        }
      });
    },
    { immediate: true }
  );

  const emit = defineEmits<{
    (e: 'select', key: string, done?: () => void): void;
    (e: 'cancel'): void;
  }>();

  function cancel() {
    // 关闭所有弹出框
    Object.keys(popShow.value).forEach((key) => {
      popShow.value[key] = false;
    });
  }

  const buttonGroupList = computed(() => {
    return props.list.filter((e) => hasAllPermission(e.permission));
  });
</script>

<style scoped lang="less">
  .crm-button-group {
    @apply flex h-full flex-nowrap items-center;
  }
</style>
