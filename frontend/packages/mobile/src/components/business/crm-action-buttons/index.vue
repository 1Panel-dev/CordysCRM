<template>
  <div v-if="buttonList.length" class="flex items-center justify-center gap-[16px]">
    <van-button
      v-for="button of visibleButtonList"
      :key="button.key"
      :type="button.danger ? 'danger' : 'primary'"
      :plain="true"
      class="flex-1 !rounded-[var(--border-radius-small)] !text-[16px]"
      @click="emit('select', button.key)"
    >
      {{ button.text }}
    </van-button>
    <van-popover
      v-if="moreButtonList.length"
      v-model:show="showPopover"
      placement="top"
      :actions="moreButtonList"
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
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';

  import { hasAllPermission, hasAnyPermission } from '@/utils/permission';

  import type { PopoverAction } from 'vant';

  export interface CrmActionButtonsItem extends PopoverAction {
    permission?: string[];
    key: string;
    allPermission?: boolean;
  }
  interface CrmActionButtonRenderItem extends CrmActionButtonsItem {
    text: string;
    danger: boolean;
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
    return (props.actions || []).filter((e) => {
      if (!e.permission?.length) {
        return true;
      }

      return e.allPermission ? hasAllPermission(e.permission) : hasAnyPermission(e.permission);
    });
  });
  const buttonList = computed<CrmActionButtonRenderItem[]>(() => [
    ...(props.showEditButton ? [{ key: 'edit', text: t('common.edit'), danger: false }] : []),
    ...actionList.value.map((action) => ({
      ...action,
      text: action.text || action.name || '',
      danger: action.color === 'var(--error-red)',
    })),
  ]);
  const visibleButtonList = computed(() => {
    if (buttonList.value.length <= 3) {
      return buttonList.value;
    }
    return buttonList.value.slice(0, 2);
  });
  const moreButtonList = computed(() => {
    if (buttonList.value.length <= 3) {
      return [];
    }
    return buttonList.value.slice(2);
  });

  const showPopover = ref(false);

  function handleSelect(action: CrmActionButtonsItem & { text?: string }) {
    showPopover.value = false;
    emit('select', action.key);
  }
</script>
