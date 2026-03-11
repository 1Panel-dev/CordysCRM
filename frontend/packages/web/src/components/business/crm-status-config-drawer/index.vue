<template>
  <CrmDrawer v-model:show="show" :width="1000" :footer="false" :title="textConfig.title">
    <div class="mb-[16px] text-[16px] font-semibold">{{ textConfig.sectionTitle }}</div>

    <div class="bg-[var(--text-n9)] p-[16px]">
      <div class="flex items-center gap-[8px]">
        <div class="w-[12px]"></div>
        <div v-for="(ele, index) of textConfig.columnTitles" :key="`ele-${index}`" class="w-full flex-1">
          {{ ele }}
        </div>
        <div class="w-[68px]"></div>
      </div>

      <CrmBatchForm
        ref="batchFormRef"
        :models="formItemModel"
        :default-list="form.list"
        validate-when-add
        draggable
        class="!p-0"
        :move="handleMove"
        @save-row="handleSave"
        @drag="dragEnd"
        @cancel-row="handleCancelRow"
      >
        <template #extra="{ element }">
          <CrmMoreAction
            :options="getDropdownOptions(element)"
            placement="bottom"
            @select="handleActionSelect($event, element)"
          >
            <n-button ghost class="px-[7px]">
              <template #icon>
                <CrmIcon type="iconicon_ellipsis" :size="16" />
              </template>
            </n-button>
          </CrmMoreAction>

          <div v-if="getDropdownOptions(element).length === 0" class="w-[32px]"></div>
        </template>
      </CrmBatchForm>
    </div>

    <div class="mb-[16px] mt-[24px] text-[16px] font-semibold">
      {{ textConfig.rollbackTitle }}
    </div>

    <div
      v-for="item in textConfig.switches"
      :key="item.key"
      :class="[item.key === 'completedStageRollback' ? 'mt-[16px]' : '', 'flex items-center gap-[8px]']"
    >
      <n-switch v-model:value="form[item.key]" @update-value="handleSwitchChange" />
      {{ item.label }}
      <n-tooltip trigger="hover" placement="right">
        <template #trigger>
          <CrmIcon
            type="iconicon_help_circle"
            :size="16"
            class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
          />
        </template>
        {{ item.tip }}
      </n-tooltip>
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { NButton, NSwitch, NTooltip } from 'naive-ui';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmBatchForm from '@/components/business/crm-batch-form/index.vue';

  import type { StatusBizType, StatusRowItem } from './types';
  import useStageConfig from './useStageConfig';

  const props = defineProps<{
    type: StatusBizType;
  }>();

  const show = defineModel<boolean>('visible', {
    required: true,
  });

  const batchFormRef = ref<InstanceType<typeof CrmBatchForm>>();

  const {
    textConfig,
    formItemModel,
    form,
    init,
    handleSave,
    handleCancelRow,
    handleSwitchChange,
    handleMoreSelect,
    dragEnd,
    handleMove,
    getDropdownOptions,
  } = useStageConfig(props.type);

  async function handleActionSelect(action: ActionsItem, element: StatusRowItem) {
    await handleMoreSelect(action, element, batchFormRef.value);
  }

  watch(
    () => show.value,
    (val) => {
      if (val) {
        init();
      }
    }
  );
</script>

<style lang="less" scoped></style>
