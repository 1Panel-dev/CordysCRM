<template>
  <CrmDrawer v-model:show="visible" resizable no-padding width="800" :footer="false" :title="title">
    <template #titleLeft>
      <div class="text-[14px] font-normal">
        <ContractStatus :status="detailInfo.planStatus" />
      </div>
    </template>
    <template #titleRight>
      <n-button
        v-permission="['PRODUCT_MANAGEMENT:UPDATE']"
        type="primary"
        ghost
        class="n-btn-outline-primary"
        @click="handleEdit(props.sourceId)"
      >
        {{ t('common.edit') }}
      </n-button>
      <n-button
        v-permission="['PRODUCT_MANAGEMENT:UPDATE']"
        type="primary"
        danger
        ghost
        class="n-btn-outline-primary"
        @click="handleDelete(detailInfo)"
      >
        {{ t('common.delete') }}
      </n-button>
    </template>
    <div class="h-full bg-[var(--text-n9)] px-[16px] pt-[16px]">
      <CrmCard hide-footer>
        <div class="flex-1">
          <CrmFormDescription
            :form-key="FormDesignKeyEnum.CONTRACT_PAYMENT"
            :source-id="props.sourceId"
            :column="2"
            :refresh-key="props.refreshId"
            label-width="auto"
            value-align="start"
            tooltip-position="top-start"
            @init="handleInit"
          />
        </div>
      </CrmCard>
    </div>

    <CrmFormCreateDrawer
      v-model:visible="formCreateDrawerVisible"
      :form-key="FormDesignKeyEnum.CONTRACT_PAYMENT"
      :source-id="props.sourceId"
      need-init-detail
      :link-form-key="FormDesignKeyEnum.CONTRACT_PAYMENT"
      @saved="handleSaved"
    />
  </CrmDrawer>
</template>

<script lang="ts" setup>
  import { NButton, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import { CollaborationType } from '@lib/shared/models/customer';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import ContractStatus from '@/views/contract/contract/components/contractStatus.vue';

  import { deletePaymentPlan } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  const props = defineProps<{
    sourceId: string;
    refreshId?: number;
  }>();
  const emit = defineEmits<{
    (e: 'refresh'): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const Message = useMessage();
  const { openModal } = useModal();
  const { t } = useI18n();
  const title = ref('');
  const detailInfo = ref();

  function handleInit(type?: CollaborationType, name?: string, detail?: Record<string, any>) {
    title.value = name || '';
    detailInfo.value = detail;
  }

  const refreshKey = ref(0);
  function handleSaved() {
    refreshKey.value += 1;
    emit('refresh');
  }

  function handleDelete(row: any) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('common.deleteConfirmContent'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deletePaymentPlan(row.id);
          Message.success(t('common.deleteSuccess'));
          visible.value = false;
          handleSaved();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  const formCreateDrawerVisible = ref(false);
  function handleEdit(id: string) {
    formCreateDrawerVisible.value = true;
  }
</script>
