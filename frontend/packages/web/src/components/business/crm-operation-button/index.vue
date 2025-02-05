<template>
  <!-- 按钮组 -->
  <CrmButtonGroup :list="props.groupList" @select="handleSelect">
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
  }>();

  function handleSelect(key: string) {
    emit('select', key);
  }

  function handleMoreSelect(item: ActionsItem) {
    emit('select', item.key as string);
  }
</script>
