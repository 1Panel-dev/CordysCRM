<template>
  <!-- 按钮组 -->
  <CrmButtonGroup :list="props.groupList" @select="handleSelect" @cancel="emit('cancel')">
    <template v-if="props.moreList?.length" #more>
      <!-- 更多操作 -->
      <CrmMoreAction :options="props.moreList" @select="handleMoreSelect">
        <template #default>
          <div class="flex items-center justify-center">
            <CrmIcon class="cursor-pointer" type="iconicon_ellipsis" :size="16" />
          </div>
        </template>
      </CrmMoreAction>
    </template>
    <!-- pop内容插槽 -->
    <template v-for="group in hasPopContentSlot" :key="group.key" #[group.popSlotContent]="{ key }">
      <slot :key="key" :name="group.popSlotContent"></slot>
    </template>
  </CrmButtonGroup>
</template>

<script setup lang="ts">
  import CrmButtonGroup from '@/components/pure/crm-button-group/index.vue';
  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import { ActionsItem } from '@/components/pure/crm-more-action/type';

  const props = defineProps<{
    groupList: ActionsItem[]; // 按钮组数据
    moreList?: ActionsItem[]; // 更多操作下拉选项
  }>();

  const emit = defineEmits<{
    (e: 'select', key: string): void;
    (e: 'cancel'): void;
  }>();

  function handleSelect(key: string) {
    emit('select', key);
  }

  function handleMoreSelect(item: ActionsItem) {
    emit('select', item.key as string);
  }

  const hasPopContentSlot = computed(() => {
    return props.groupList.filter((e) => e.popSlotContent) as ActionsItem & { popSlotContent: string; key: string }[];
  });
</script>
