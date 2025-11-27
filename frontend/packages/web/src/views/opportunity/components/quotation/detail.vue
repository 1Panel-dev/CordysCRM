<template>
  <CrmDrawer v-model:show="visible" resizable no-padding :width="800" :footer="false" :title="props.detail?.name ?? ''">
    <template #titleLeft>
      <div class="text-[14px] font-normal">
        <quotationStatus v-if="props.detail?.approvalStatus" :status="props.detail?.approvalStatus" />
      </div>
    </template>
    <template #titleRight>
      <div class="flex items-center gap-[8px]">
        <n-button
          v-for="item of buttonList"
          :key="item.key"
          :type="item.danger ? 'error' : 'primary'"
          ghost
          :class="`n-btn-outline-${item.danger ? 'error' : 'primary'}`"
          @click="handleSelect(item.key as string)"
        >
          {{ item.label }}
        </n-button>
        <CrmMoreAction
          :options="buttonMoreList"
          trigger="click"
          @select="(item:ActionsItem)=>handleSelect(item.key as string)"
        >
          <n-button type="primary" ghost class="n-btn-outline-primary">
            {{ t('common.more') }}
            <CrmIcon class="ml-[8px]" type="iconicon_chevron_down" :size="16" />
          </n-button>
        </CrmMoreAction>
      </div>
    </template>
    <CrmFormDescription
      :form-key="FormDesignKeyEnum.OPPORTUNITY_QUOTATION_SNAPSHOT"
      :source-id="sourceId"
      :column="2"
      :refresh-key="props.refreshId"
      label-width="auto"
      value-align="start"
      tooltip-position="top-start"
      class="p-[8px]"
      @init="handleInit"
    />
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { QuotationStatusEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';
  import { CollaborationType } from '@lib/shared/models/customer';
  import { QuotationItem } from '@lib/shared/models/opportunity';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import quotationStatus from './quotationStatus.vue';

  import { approvalQuotation, deleteQuotation, revokeQuotation, voidQuotation } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import useOpenNewPage from '@/hooks/useOpenNewPage';
  import { useUserStore } from '@/store';
  import { hasAnyPermission } from '@/utils/permission';

  import { FullPageEnum } from '@/enums/routeEnum';

  const { openModal } = useModal();
  const { openNewPage } = useOpenNewPage();

  const useStore = useUserStore();
  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    refreshId?: number;
    detail?: Partial<QuotationItem>;
  }>();

  const emit = defineEmits<{
    (e: 'edit', sourceId: string): void;
    (e: 'refresh'): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const sourceId = computed(() => props.detail?.id ?? '');

  const title = ref('');
  function handleInit(type?: CollaborationType, name?: string) {
    title.value = name || '';
  }

  const isShowApproval = computed(
    () =>
      hasAnyPermission(['OPPORTUNITY_QUOTATION:APPROVAL']) &&
      props.detail?.approvalStatus === QuotationStatusEnum.APPROVING
  );

  function handleDownload() {
    openNewPage(FullPageEnum.FULL_PAGE_EXPORT_QUOTATION, { id: sourceId.value });
  }

  const commonActions = [
    {
      label: t('common.pass'),
      key: 'pass',
      permission: ['OPPORTUNITY_QUOTATION:APPROVAL'],
    },
    {
      label: t('common.unPass'),
      key: 'unPass',
      danger: true,
      permission: ['OPPORTUNITY_QUOTATION:APPROVAL'],
    },
    {
      label: t('common.edit'),
      key: 'edit',
      permission: ['OPPORTUNITY_QUOTATION:UPDATE'],
    },
    {
      label: t('common.download'),
      key: 'download',
      permission: ['OPPORTUNITY_QUOTATION:EXPORT'],
    },
  ];

  const deleteActions = [
    {
      label: t('common.delete'),
      key: 'delete',
      danger: true,
      permission: ['OPPORTUNITY_QUOTATION:DELETE'],
    },
  ];

  const moreActions = [
    {
      label: t('common.voided'),
      key: 'voided',
      permission: ['OPPORTU NITY_QUOTATION:VOIDED'],
    },
    {
      label: t('common.revoke'),
      key: 'revoke',
    },
  ];

  async function handleApproval(approval = false) {
    const approvalStatus = approval ? QuotationStatusEnum.APPROVED : QuotationStatusEnum.UNAPPROVED;
    try {
      await approvalQuotation({
        id: sourceId.value,
        name: props.detail?.name ?? '',
        approvalStatus,
        opportunityId: props.detail?.opportunityId ?? '',
      });
      Message.success(approval ? t('common.approvedSuccess') : t('common.unApprovedSuccess'));
      visible.value = false;
      emit('refresh');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  function handleVoid() {
    const { name } = props.detail ?? {};
    openModal({
      type: 'error',
      title: t('opportunity.quotation.voidTitleTip', { name: characterLimit(name) }),
      content: t('opportunity.quotation.invalidContentTip'),
      positiveText: t('common.confirmVoid'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await voidQuotation(sourceId.value);
          Message.success(t('common.voidSuccess'));
          visible.value = false;
          emit('refresh');
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  async function handleRevoke() {
    try {
      await revokeQuotation(sourceId.value);
      Message.success(t('common.revokeSuccess'));
      visible.value = false;
      emit('refresh');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  function handleDelete() {
    const { name } = props.detail ?? {};
    openModal({
      type: 'error',
      title: t('opportunity.quotation.deleteTitleTip', { name: characterLimit(name) }),
      content: t('opportunity.quotation.deleteContentTip'),
      positiveText: t('common.confirmVoid'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteQuotation(sourceId.value);
          Message.success(t('common.deleteSuccess'));
          visible.value = false;
          emit('refresh');
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function handleSelect(key: string) {
    switch (key) {
      case 'edit':
        emit('edit', sourceId.value);
        break;
      case 'pass':
        handleApproval(true);
        break;
      case 'unPass':
        handleApproval();
        break;
      case 'voided':
        handleVoid();
        break;
      case 'revoke':
        handleRevoke();
        break;
      case 'download':
        handleDownload();
        break;
      case 'delete':
        handleDelete();
        break;
      default:
        break;
    }
  }

  const buttonList = computed<ActionsItem[]>(() => {
    const { approvalStatus } = props.detail ?? {};
    switch (approvalStatus) {
      case QuotationStatusEnum.APPROVING:
        return isShowApproval.value ? commonActions.filter((item) => ['pass', 'unPass'].includes(item.key)) : [];
      case QuotationStatusEnum.APPROVED:
        return commonActions.filter((item) => ['download'].includes(item.key));
      case QuotationStatusEnum.UNAPPROVED:
      case QuotationStatusEnum.REVOKE:
        return commonActions.filter((item) => ['edit'].includes(item.key));
      case QuotationStatusEnum.VOIDED:
        return deleteActions;
      default:
        return [];
    }
  });

  const buttonMoreList = computed(() => {
    const allActions = [...commonActions, ...moreActions, ...deleteActions];
    const commonActionsKeys = ['voided', 'delete'];
    const { approvalStatus, createUser } = props.detail ?? {};
    const getActions = (keys: string[]) => allActions.filter((e) => keys.includes(e.key));
    switch (approvalStatus) {
      case QuotationStatusEnum.APPROVED:
        const successStatusGroups = isShowApproval ? commonActionsKeys : ['download', ...commonActionsKeys];
        return getActions(successStatusGroups);
      case QuotationStatusEnum.UNAPPROVED:
      case QuotationStatusEnum.REVOKE:
        const revokeStatusGroups = isShowApproval ? commonActionsKeys : ['edit', 'download', ...commonActionsKeys];
        return getActions(revokeStatusGroups);
      case QuotationStatusEnum.APPROVING:
        const reviewStatusGroups =
          createUser === useStore.userInfo.id ? ['revoke', ...commonActionsKeys] : commonActionsKeys;
        return getActions(reviewStatusGroups);
      default:
        return [];
    }
  });
</script>

<style scoped></style>
