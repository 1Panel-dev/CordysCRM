<template>
  <div v-if="actionList?.length || props.showEditButton" class="flex items-center justify-center gap-[16px]">
    <van-popover
      v-if="actionList?.length"
      v-model:show="showPopover"
      placement="top"
      :actions="actionList"
      @select="handleSelect"
    >
      <template #reference>
        <div class="flex w-[100px] items-center">
          <CrmTextButton
            color="var(--text-n1)"
            icon="iconicon_ellipsis"
            :text="t('common.more')"
            icon-size="18px"
            direction="column"
            class="flex-1"
            @click="showPopover = true"
          />
        </div>
      </template>
    </van-popover>

    <van-button
      v-if="props.showEditButton"
      type="primary"
      :plain="true"
      class="flex-1 !rounded-[var(--border-radius-small)] !text-[16px]"
      @click="emit('select', 'edit')"
    >
      {{ t('common.edit') }}
    </van-button>
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';

  import { hasAllPermission, hasAnyPermission } from '@/utils/permission';

  import type { PopoverAction } from 'vant';

  export interface CrmActionButtonsItem extends PopoverAction {
    permission: string[];
    key: string;
    allPermission?: boolean;
  }

  const props = defineProps<{
    actions?: CrmActionButtonsItem[]; // 更多操作项
    showEditButton?: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'select', actionKey: string): void;
  }>();

  const { t } = useI18n();

  const actionList = computed(() => {
    return props.actions?.filter((e) =>
      e.allPermission ? hasAllPermission(e.permission) : hasAnyPermission(e.permission)
    );
  });

  const showPopover = ref(false);

  function handleSelect(action: CrmActionButtonsItem) {
    showPopover.value = false;
    emit('select', action.key);
  }
</script>
