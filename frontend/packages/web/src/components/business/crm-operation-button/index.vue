<template>
  <!-- 按钮组 -->
  <CrmButtonGroup :list="buttonGroupList" @select="handleSelect" @cancel="emit('cancel')">
    <template v-if="props.moreList?.length" #more>
      <!-- 更多操作 -->
      <CrmMoreAction :options="props.moreList" @select="handleMoreSelect">
        <template #default>
          <n-button text type="primary">
            {{ t('common.more') }}
          </n-button>
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
  import { NButton } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmButtonGroup from '@/components/pure/crm-button-group/index.vue';
  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import { ActionsItem } from '@/components/pure/crm-more-action/type';

  import { hasAllPermission, hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    groupList: ActionsItem[]; // 按钮组数据
    moreList?: ActionsItem[]; // 更多操作下拉选项
  }>();

  const emit = defineEmits<{
    (e: 'select', key: string, done?: () => void): void;
    (e: 'cancel'): void;
  }>();

  const { t } = useI18n();

  function handleSelect(key: string, done?: () => void) {
    emit('select', key, done);
  }

  function handleMoreSelect(item: ActionsItem) {
    emit('select', item.key as string);
  }

  const hasPopContentSlot = computed(() => {
    return props.groupList.filter((e) => e.popSlotContent) as ActionsItem & { popSlotContent: string; key: string }[];
  });

  const hasPermissionMore = computed(
    () =>
      (props.moreList || []).filter((e) =>
        e.allPermission ? hasAllPermission(e.permission) : hasAnyPermission(e.permission)
      ).length > 0
  );

  const buttonGroupList = computed(() =>
    props.groupList.filter((e) =>
      hasPermissionMore.value ? hasAllPermission(e.permission) : hasAllPermission(e.permission) && e.slotName !== 'more'
    )
  );
</script>
