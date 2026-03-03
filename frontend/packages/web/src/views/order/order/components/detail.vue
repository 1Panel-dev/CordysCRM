<template>
  <CrmDrawer v-model:show="visible" resizable no-padding :width="800" :footer="false">
    <template #titleRight>
      <n-button
        v-if="!props.readonly"
        v-permission="['CONTRACT_PAYMENT_PLAN:UPDATE']"
        type="primary"
        ghost
        class="n-btn-outline-primary"
        @click="handleEdit()"
      >
        {{ t('common.edit') }}
      </n-button>
      <n-button
        v-permission="['CONTRACT_PAYMENT_PLAN:UPDATE']"
        type="primary"
        ghost
        class="n-btn-outline-primary"
        @click="handleDownload(props.sourceId)"
      >
        {{ t('common.download') }}
      </n-button>
      <n-button
        v-if="!props.readonly"
        v-permission="['CONTRACT_PAYMENT_PLAN:DELETE']"
        type="primary"
        danger
        ghost
        class="n-btn-outline-primary ml-[12px]"
        @click="handleDelete(detailInfo)"
      >
        {{ t('common.delete') }}
      </n-button>
    </template>
    <div class="h-full bg-[var(--text-n9)] px-[16px] pt-[16px]">
      <!-- TODO lmy 订单状态 -->
      <!-- <CrmWorkflowCard
        v-model:stage="currentStatus"
        :show-confirm-status="true"
        class="mb-[16px]"
        :stage-config-list="stageConfig?.stageConfigList || []"
        is-limit-back
        :failure-reason="lastFailureReason"
        :back-stage-permission="['OPPORTUNITY_MANAGEMENT:UPDATE', 'OPPORTUNITY_MANAGEMENT:RESIGN']"
        :source-id="sourceId"
        :operation-permission="['OPPORTUNITY_MANAGEMENT:UPDATE']"
        :update-api="updateOptStage"
        :afoot-roll-back="stageConfig?.afootRollBack"
        :end-roll-back="stageConfig?.endRollBack"
        @load-detail="refreshList"
      /> -->
      <CrmCard hide-footer>
        <div class="flex-1">
          <CrmFormDescription
            :form-key="FormDesignKeyEnum.ORDER_SNAPSHOT"
            :source-id="props.sourceId"
            :column="2"
            :refresh-key="refreshKey"
            label-width="auto"
            value-align="start"
            tooltip-position="top-start"
            @init="handleInit"
            @open-contract-detail="emit('openContractDrawer', $event)"
          />
        </div>
      </CrmCard>
    </div>

    <CrmFormCreateDrawer
      v-model:visible="formCreateDrawerVisible"
      :form-key="FormDesignKeyEnum.ORDER"
      :source-id="props.sourceId"
      need-init-detail
      :link-form-key="FormDesignKeyEnum.ORDER"
      @saved="() => handleSaved()"
    />
  </CrmDrawer>
</template>

<script lang="ts" setup>
  import { NButton, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import { CollaborationType } from '@lib/shared/models/customer';
  import { OrderItem } from '@lib/shared/models/order';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmWorkflowCard from '@/components/business/crm-workflow-card/index.vue';

  import { deleteOrder } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  const props = defineProps<{
    sourceId: string;
    readonly?: boolean;
  }>();
  const emit = defineEmits<{
    (e: 'refresh'): void;
    (e: 'openContractDrawer', params: { id: string }): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const Message = useMessage();
  const { openModal } = useModal();
  const { t } = useI18n();
  const detailInfo = ref();

  function handleInit(type?: CollaborationType, name?: string, detail?: Record<string, any>) {
    detailInfo.value = detail;
  }

  const refreshKey = ref(0);
  function handleSaved() {
    refreshKey.value += 1;
    emit('refresh');
  }

  async function handleDelete(row: OrderItem) {
    // TODO lmy 判断是否被合同关联
    const hasData = true;
    const content = hasData ? t('order.deleteContent') : t('common.deleteConfirmContent');
    const positiveText = hasData ? t('opportunity.gotIt') : t('common.confirmDelete');

    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content,
      positiveText,
      negativeText: t('common.cancel'),
      positiveButtonProps: {
        type: hasData ? 'primary' : 'error',
        size: 'medium',
      },
      onPositiveClick: async () => {
        if (!hasData) {
          try {
            await deleteOrder(row.id);
            Message.success(t('common.deleteSuccess'));
            visible.value = false;
            emit('refresh');
          } catch (error) {
            // eslint-disable-next-line no-console
            console.error(error);
          }
        }
      },
      onNegativeClick: () => {
        if (hasData) {
          visible.value = false;
        }
      },
    });
  }

  const formCreateDrawerVisible = ref(false);
  function handleEdit() {
    formCreateDrawerVisible.value = true;
  }

  function handleDownload(id: string) {
    // TODO lmy 下载
  }
</script>
