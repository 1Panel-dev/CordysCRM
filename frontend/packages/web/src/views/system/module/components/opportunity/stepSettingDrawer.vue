<template>
  <CrmDrawer v-model:show="show" :width="1000" :footer="false" :title="t('module.businessManage.businessStepSet')">
    <div class="mb-[16px] text-[16px] font-semibold">{{ t('module.businessManage.businessStepConfig') }}</div>
    <CrmBatchForm
      ref="batchFormRef"
      :models="formItemModel"
      :default-list="form.list"
      validate-when-add
      draggable
      @save-row="handleSave"
      @drag="dragEnd"
    >
      <template #extra="{ element }">
        <CrmMoreAction
          :options="getDropdownOptions(element)"
          placement="bottom"
          @select="handleMoreSelect($event, element)"
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
    <div class="mb-[16px] mt-[24px] text-[16px] font-semibold">
      {{ t('module.businessManage.businessStepRollbackConfig') }}
    </div>
    <div class="flex items-center gap-[8px]">
      <n-switch v-model:value="form.runningStageRollback" @update-value="handleSwitchChange" />
      {{ t('module.businessManage.runningStageRollback') }}
      <n-tooltip trigger="hover" placement="right">
        <template #trigger>
          <CrmIcon
            type="iconicon_help_circle"
            :size="16"
            class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
          />
        </template>
        {{ t('module.businessManage.runningStageRollbackTip') }}
      </n-tooltip>
    </div>
    <div class="mt-[16px] flex items-center gap-[8px]">
      <n-switch v-model:value="form.completedStageRollback" @update-value="handleSwitchChange" />
      {{ t('module.businessManage.completedStageRollback') }}
      <n-tooltip trigger="hover" placement="right">
        <template #trigger>
          <CrmIcon
            type="iconicon_help_circle"
            :size="16"
            class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
          />
        </template>
        {{ t('module.businessManage.completedStageRollbackTip') }}
      </n-tooltip>
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { NButton, NSwitch, NTooltip, useMessage } from 'naive-ui';

  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getGenerateId } from '@lib/shared/method';
  import { StageConfigItem } from '@lib/shared/models/opportunity';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmBatchForm from '@/components/business/crm-batch-form/index.vue';
  import { FormItemModel } from '@/components/business/crm-batch-form/types';

  import {
    addOpportunityStage,
    deleteOpportunityStage,
    getOpportunityStageConfig,
    sortOpportunityStage,
    updateOpportunityStage,
    updateOpportunityStageRollback,
  } from '@/api/modules';

  const { t } = useI18n();
  const Message = useMessage();

  const show = defineModel<boolean>('visible', {
    required: true,
  });

  const batchFormRef = ref<InstanceType<typeof CrmBatchForm>>();
  const form = ref<any>({
    runningStageRollback: true,
    completedStageRollback: false,
    list: [],
  });

  const formItemModel = ref<FormItemModel[]>([
    {
      path: 'name',
      type: FieldTypeEnum.INPUT,
      formItemClass: 'w-full flex-initial',
      label: t('opportunity.stage'),
      inputProps: {
        maxlength: 16,
      },
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: '' }),
        },
      ],
    },
    {
      path: 'rate',
      type: FieldTypeEnum.INPUT_NUMBER,
      formItemClass: 'w-full flex-initial',
      label: t('opportunity.win'),
      numberProps: {
        min: 0,
        max: 100,
        precision: 0,
        disabledFunction(row) {
          return row.type === 'END';
        },
      },
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: '' }),
        },
      ],
    },
    {
      path: 'type',
      type: FieldTypeEnum.SELECT,
      formItemClass: 'w-full flex-initial',
      label: t('opportunity.stageType'),
      selectProps: {
        disabledFunction: () => true,
        disabledTooltipFunction: () => t('opportunity.stageTypeDisabledChange'),
        options: [
          { label: t('common.inProgress'), value: 'AFOOT' },
          { label: t('common.complete'), value: 'END' },
        ],
      },
    },
  ]);

  function getDropdownOptions(element: Record<string, any>): ActionsItem[] {
    if (element.type === 'END') {
      return [];
    }
    if (form.value.list.some((item: any) => item.type === 'AFOOT').length === 1) {
      // 至少保留一个进行中的状态
      return [
        { label: t('module.businessManage.insetBefore'), key: 'before' },
        { label: t('module.businessManage.insetAfter'), key: 'after' },
      ];
    }
    if (form.value.list.length === 10) {
      return [
        {
          label: t('common.delete'),
          key: 'delete',
        },
      ];
    }
    return [
      { label: t('module.businessManage.insetBefore'), key: 'before' },
      { label: t('module.businessManage.insetAfter'), key: 'after' },
      { type: 'divider' },
      {
        label: t('common.delete'),
        key: 'delete',
        danger: true,
        disabled: element.stageHasData,
        tooltipContent: t('module.businessManage.stageHasData'),
      },
    ];
  }

  async function handleMoreSelect(action: ActionsItem, element: Record<string, any>) {
    const index = form.value.list.findIndex((item: any) => item._key === element._key);
    const id = getGenerateId();
    if (action.key === 'before') {
      batchFormRef.value?.formValidate(() => {
        form.value.list.splice(index, 0, { id, _key: id, name: '', rate: null, type: 'AFOOT', editing: true });
      });
    } else if (action.key === 'after') {
      batchFormRef.value?.formValidate(() => {
        form.value.list.splice(index + 1, 0, {
          id,
          _key: id,
          name: '',
          rate: null,
          type: 'AFOOT',
          editing: true,
        });
      });
    } else if (action.key === 'delete') {
      await deleteOpportunityStage(element.id);
      form.value.list.splice(index, 1);
    }
  }

  async function handleSave(element: Record<string, any>, done: () => void, index: number) {
    try {
      const params = {
        id: element?.id,
        name: element.name,
        rate: element.rate,
      };
      if (element.id) {
        await updateOpportunityStage(params);
        done();
      } else {
        const id = await addOpportunityStage({
          ...params,
          type: element.type,
          dropPosition: form.value.list[index - 1] ? 1 : -1,
          targetId: form.value.list[index - 1]?.id || form.value.list[index + 1]?.id,
        });
        element.id = id;
        done();
      }
      Message.success(t('common.operationSuccess'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function dragEnd(event: any) {
    if (form.value.list.length === 1) return;
    try {
      const { newIndex, oldIndex, data } = event;
      if (newIndex === oldIndex) return;
      await sortOpportunityStage({
        dragId: data.id,
        end: newIndex + 1,
        start: data.pos,
      });
      Message.success(t('common.operationSuccess'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function handleSwitchChange() {
    try {
      await updateOpportunityStageRollback({
        afootRollBack: form.value.runningStageRollback,
        endRollBack: form.value.completedStageRollback,
      });
      Message.success(t('common.operationSuccess'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function init() {
    try {
      const res = await getOpportunityStageConfig();
      form.value = {
        runningStageRollback: res.afootRollBack,
        completedStageRollback: res.endRollBack,
        list: res.stageConfigList.map((item: StageConfigItem) => ({
          ...item,
          _key: item.id,
          rate: Number(item.rate),
          editing: false,
          draggable: item.type !== 'END',
        })),
      };
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
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
