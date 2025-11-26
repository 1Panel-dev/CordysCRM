<template>
  <CrmDrawer v-model:show="visible" resizable no-padding width="800" :footer="false" :title="title">
    <template #titleLeft>
      <div class="text-[14px] font-normal">
        <ContractStatus :status="detailInfo.status" />
      </div>
    </template>
    <template v-if="detailInfo.status !== ContractStatusEnum.VOID" #titleRight>
      <CrmButtonGroup :list="buttonList" not-show-divider @select="handleButtonClick" />
    </template>
    <div class="h-full bg-[var(--text-n9)] px-[16px] pt-[16px]">
      <CrmCard hide-footer>
        <div class="flex-1">
          <CrmFormDescription
            :form-key="FormDesignKeyEnum.CONTRACT"
            :source-id="props.sourceId"
            :column="2"
            :refresh-key="refreshKey"
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
      :form-key="FormDesignKeyEnum.CONTRACT"
      :source-id="props.sourceId"
      need-init-detail
      :link-form-key="FormDesignKeyEnum.CONTRACT"
      @saved="handleSaved"
    />
  </CrmDrawer>
</template>

<script lang="ts" setup>
  import { useMessage } from 'naive-ui';

  import { ArchiveStatusEnum, ContractStatusEnum } from '@lib/shared/enums/contractEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import type { ContractItem } from '@lib/shared/models/contract';
  import { CollaborationType } from '@lib/shared/models/customer';

  import CrmButtonGroup from '@/components/pure/crm-button-group/index.vue';
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import ContractStatus from '@/views/contract/contract/components/contractStatus.vue';

  import { archivedContract, deleteContract, voidedContract } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  const props = defineProps<{
    sourceId: string;
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

  const buttonList = computed(() => {
    const info = detailInfo.value;
    const isArchived = info.archivedStatus === ArchiveStatusEnum.ARCHIVED;
    if (isArchived) {
      return [
        {
          key: 'unarchive',
          label: t('common.unarchive'),
          text: false,
          ghost: true,
          class: 'n-btn-outline-primary',
          permission: ['PRODUCT_MANAGEMENT:UPDATE'],
        },
      ];
    }
    return [
      {
        key: 'edit',
        label: t('common.edit'),
        permission: ['PRODUCT_MANAGEMENT:UPDATE'],
        text: false,
        ghost: true,
        class: 'n-btn-outline-primary',
      },
      {
        key: 'archive',
        label: t('common.archive'),
        permission: ['PRODUCT_MANAGEMENT:UPDATE'],
        text: false,
        ghost: true,
        class: 'n-btn-outline-primary',
      },
      {
        key: 'voided',
        label: t('common.voided'),
        permission: ['PRODUCT_MANAGEMENT:UPDATE'],
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
        permission: ['CUSTOMER_MANAGEMENT:DELETE'],
      },
    ];
  });

  function handleInit(type?: CollaborationType, name?: string, detail?: Record<string, any>) {
    title.value = name || '';
    detailInfo.value = detail ?? {};
  }

  const formCreateDrawerVisible = ref(false);
  function handleEdit() {
    formCreateDrawerVisible.value = true;
  }

  const refreshKey = ref(0);
  function handleSaved() {
    refreshKey.value += 1;
    emit('refresh');
  }

  function handleDelete(row: ContractItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(row.name) }),
      content: t('common.deleteConfirmContent'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteContract(row.id);
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

  function handleVoided(row: ContractItem) {
    openModal({
      type: 'error',
      title: t('contract.voidedConfirmTitle', { name: characterLimit(row.name) }),
      content: t('contract.voidedConfirmContent'),
      positiveText: t('common.confirmVoid'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await voidedContract(row.id);
          Message.success(t('common.voidSuccess'));
          handleSaved();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  async function handleArchive(id: string, status: string) {
    try {
      await archivedContract(id, status);
      if (status === ArchiveStatusEnum.UN_ARCHIVED) {
        Message.success(t('common.batchArchiveSuccess'));
      } else {
        Message.success(`${t('common.unArchive')}${t('common.success')}`);
      }
      handleSaved();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function handleButtonClick(actionKey: string) {
    switch (actionKey) {
      case 'edit':
        handleEdit();
        break;
      case 'unarchive':
        handleArchive(props.sourceId, detailInfo.value.archivedStatus);
        break;
      case 'archive':
        handleArchive(props.sourceId, detailInfo.value.archivedStatus);
        break;
      case 'voided':
        handleVoided(detailInfo.value);
        break;
      case 'delete':
        handleDelete(detailInfo.value);
        break;
      default:
        break;
    }
  }
</script>
