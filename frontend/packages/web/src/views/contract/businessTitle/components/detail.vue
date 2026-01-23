<template>
  <CrmDrawer
    v-model:show="visible"
    :width="1000"
    no-padding
    :footer="false"
    :title="detailInfo?.name ?? ''"
    @cancel="handleCancel"
  >
    <template #titleLeft>
      <CrmBusinessNamePrefix v-if="detailInfo?.type" :type="detailInfo?.type" />
    </template>
    <template #titleRight>
      <CrmButtonGroup class="gap-[12px]" :list="buttonList" not-show-divider @select="handleButtonClick" />
    </template>
    <div class="h-full bg-[var(--text-n9)] px-[16px] pt-[16px]">
      <CrmCard hide-footer>
        <div class="mb-[16px] font-medium text-[var(--text-n1)]">{{ t('module.businessTitle') }}</div>
        <div class="flex-1">
          <CrmDescription
            :one-line-label="false"
            :descriptions="descriptions"
            :column="2"
            label-width="auto"
            value-align="start"
            tooltip-position="top-start"
          >
            <template #createTimeSlot="{ item }">
              {{ dayjs(item.value as string).format('YYYY-MM-DD HH:mm:ss') }}
            </template>
            <template #updateTimeSlot="{ item }">
              {{ dayjs(item.value as string).format('YYYY-MM-DD HH:mm:ss') }}
            </template>
          </CrmDescription>
        </div>
        <businessTitleDrawer
          v-model:visible="businessNameDrawerVisible"
          :source-id="detailInfo?.id ?? ''"
          @load="handleRefresh"
        />
      </CrmCard>
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { useMessage } from 'naive-ui';
  import dayjs from 'dayjs';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import { BusinessTitleItem } from '@lib/shared/models/contract';

  import CrmButtonGroup from '@/components/pure/crm-button-group/index.vue';
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDescription, { Description } from '@/components/pure/crm-description/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmBusinessNamePrefix from '@/components/business/crm-business-name-prefix/index.vue';
  import businessTitleDrawer from './businessTitleDrawer.vue';

  import {
    deleteBusinessTitle,
    getBusinessTitleDetail,
    getBusinessTitleInvoiceCheck,
    revokeBusinessTitle,
  } from '@/api/modules';
  import { businessTitleFormConfigList } from '@/config/contract';
  import useModal from '@/hooks/useModal';

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();

  const props = defineProps<{
    sourceId: string;
  }>();

  const emit = defineEmits<{
    (e: 'load'): void;
    (e: 'cancel'): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const detailInfo = ref();

  const buttonList = [
    {
      key: 'edit',
      label: t('common.edit'),
      permission: ['CONTRACT_BUSINESS_TITLE:UPDATE'],
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
    },
    {
      label: t('common.delete'),
      key: 'delete',
      text: false,
      ghost: true,
      danger: true,
      class: 'n-btn-outline-primary',
      permission: ['CONTRACT_BUSINESS_TITLE:DELETE'],
    },
  ];

  const descriptions = ref<Description[]>([]);
  const systemFields = ref<Description[]>([
    {
      label: t('common.createTime'),
      value: 'createTime',
      valueSlotName: 'createTimeSlot',
    },
    {
      label: t('common.creator'),
      value: 'createUserName',
    },
    {
      label: t('common.updateTime'),
      value: 'updateTime',
      valueSlotName: 'updateTimeSlot',
    },
    {
      label: t('common.updateUserName'),
      value: 'updateUserName',
    },
  ]);

  async function initDetail() {
    try {
      const result = await getBusinessTitleDetail(props.sourceId);
      detailInfo.value = result;
      descriptions.value = [...businessTitleFormConfigList, ...systemFields.value].map((item: any) => {
        return {
          ...item,
          label: item.label,
          value: result[item.value as keyof BusinessTitleItem] ?? '-',
        } as Description;
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function handleCancel() {
    emit('cancel');
    visible.value = false;
  }

  async function handleDelete() {
    try {
      const isInvoiceChecked = await getBusinessTitleInvoiceCheck(props.sourceId);
      const content = isInvoiceChecked
        ? t('contract.businessTitle.deleteInvoiceContent')
        : t('contract.businessTitle.deleteContent');
      const positiveText = isInvoiceChecked ? t('common.gotIt') : t('common.confirmDelete');
      const type = isInvoiceChecked ? 'default' : 'error';
      openModal({
        type,
        title: t('common.deleteConfirmTitle', { name: characterLimit(detailInfo.value?.name) }),
        content,
        positiveText,
        negativeText: t('common.cancel'),
        onPositiveClick: async () => {
          try {
            if (isInvoiceChecked) return;
            await deleteBusinessTitle(props.sourceId);
            Message.success(t('common.deleteSuccess'));
            emit('load');
            visible.value = false;
          } catch (error) {
            // eslint-disable-next-line no-console
            console.error(error);
          }
        },
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function handleApproval(approval = false) {
    try {
      // todo: 这版本不上xinxinwu
      Message.success(approval ? t('common.approvedSuccess') : t('common.unApprovedSuccess'));
      emit('load');
      initDetail();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function handleRevoke() {
    try {
      await revokeBusinessTitle(props.sourceId);
      Message.success(t('common.revokeSuccess'));
      emit('load');
      initDetail();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  const businessNameDrawerVisible = ref(false);
  async function handleButtonClick(actionKey: string) {
    switch (actionKey) {
      case 'pass':
        handleApproval(true);
        break;
      case 'unPass':
        handleApproval();
        break;
      case 'edit':
        businessNameDrawerVisible.value = true;
        break;
      case 'revoke':
        handleRevoke();
        break;
      case 'delete':
        handleDelete();
        break;
      default:
        break;
    }
  }

  function handleRefresh() {
    initDetail();
    emit('load');
  }

  watch(
    () => visible.value,
    (newVal) => {
      if (newVal) {
        initDetail();
      }
    },
    { immediate: true }
  );
</script>

<style scoped></style>
