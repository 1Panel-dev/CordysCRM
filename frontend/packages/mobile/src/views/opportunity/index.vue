<template>
  <div class="flex h-full flex-col overflow-hidden">
    <CrmPageHeader :title="t('common.opportunity')" hide-back />
    <div class="mt-[48px] flex items-center gap-[12px] bg-[var(--text-n10)] p-[8px_16px]">
      <van-button
        v-permission="['OPPORTUNITY_MANAGEMENT:ADD']"
        plain
        icon="plus"
        type="primary"
        size="small"
        @click="goCreate"
      >
      </van-button>
      <van-search
        v-model="keyword"
        shape="round"
        :placeholder="t('opportunity.searchPlaceholder')"
        class="crm-search"
        @search="search"
        @clear="clear"
      />
    </div>
    <div class="filter-buttons">
      <van-button
        v-for="item of tabList"
        :key="item.name"
        round
        size="small"
        class="!border-none !px-[16px] !py-[4px] !text-[14px]"
        :class="
          activeFilter === item.name
            ? '!bg-[var(--primary-7)] !text-[var(--primary-8)]'
            : '!bg-[var(--text-n9)] !text-[var(--text-n1)]'
        "
        @click="activeFilter = item.name"
      >
        {{ item.tab }}
      </van-button>
    </div>

    <div class="flex-1 overflow-hidden">
      <CrmList
        ref="crmListRef"
        :close-init-load="!activeFilter"
        :keyword="keyword"
        :load-list-api="getOpportunityList"
        :list-params="listParams"
        class="p-[16px]"
        :item-gap="16"
        :transform="transformFormData"
      >
        <template #item="{ item }">
          <CrmListCommonItem :item="item" :actions="actions(item)" @click="(val)=>goDetail(val as OpportunityItem)" />
        </template>
      </CrmList>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { closeToast, showConfirmDialog, showLoadingToast, showSuccessToast } from 'vant';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { OpportunitySearchTypeEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { OpportunityItem, OpportunityStageConfig } from '@lib/shared/models/opportunity';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmPageHeader from '@/components/pure/crm-page-header/index.vue';

  import { deleteOpt, getOpportunityList, getOpportunityStageConfig } from '@/api/modules';
  import useApprovalOperation from '@/hooks/useApprovalOperation';
  import useApprovalResourceAction from '@/hooks/useApprovalResourceAction';
  import useFormCreateTransform from '@/hooks/useFormCreateTransform';
  import useHiddenTab from '@/hooks/useHiddenTab';
  import { hasAllPermission } from '@/utils/permission';

  import { CommonRouteEnum, CustomerRouteEnum, OpportunityRouteEnum } from '@/enums/routeEnum';

  defineOptions({
    name: OpportunityRouteEnum.OPPORTUNITY_INDEX,
  });
  const router = useRouter();
  const { t } = useI18n();

  const keyword = ref('');
  const crmListRef = ref<InstanceType<typeof CrmList>>();
  const stageConfig = ref<OpportunityStageConfig>();
  async function initStageConfig() {
    try {
      stageConfig.value = await getOpportunityStageConfig();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
  const failureStage = computed(() =>
    stageConfig.value?.stageConfigList.find((e) => e.type === 'END' && e.rate === '0')
  );
  const successStage = computed(() =>
    stageConfig.value?.stageConfigList.find((e) => e.type === 'END' && e.rate === '100')
  );

  const filterButtons = [
    {
      name: OpportunitySearchTypeEnum.ALL,
      tab: t('opportunity.all'),
    },
    {
      name: OpportunitySearchTypeEnum.SELF,
      tab: t('opportunity.mine'),
    },
    {
      name: OpportunitySearchTypeEnum.DEPARTMENT,
      tab: t('opportunity.deptOpportunities'),
    },
    {
      name: OpportunitySearchTypeEnum.OPPORTUNITY_SUCCESS,
      tab: t('opportunity.convertedOpportunities'),
    },
  ];
  const { tabList, activeFilter } = useHiddenTab(filterButtons, FormDesignKeyEnum.BUSINESS);
  const { reviewByResourceId, revokeByResourceId } = useApprovalResourceAction({
    formKey: FormDesignKeyEnum.BUSINESS,
  });

  const refreshList = () => crmListRef.value?.loadList(true);
  const { resolveRowActions, initApprovalPermission, deleteExecute } = useApprovalOperation<OpportunityItem>({
    formType: FormDesignKeyEnum.BUSINESS,
    onReview: (row) => reviewByResourceId(row.id, { onSuccess: refreshList }),
    onRevoke: (row) => revokeByResourceId(row.id, { onSuccess: refreshList }),
  });

  const listParams = computed(() => {
    return {
      viewId: activeFilter.value,
      keyword: keyword.value,
    };
  });

  function handleTransfer(id: string) {
    router.push({
      name: CustomerRouteEnum.CUSTOMER_TRANSFER,
      query: {
        id,
        apiKey: FormDesignKeyEnum.BUSINESS,
      },
    });
  }

  const hasBackStagePermission = computed(() =>
    hasAllPermission(['OPPORTUNITY_MANAGEMENT:UPDATE', 'OPPORTUNITY_MANAGEMENT:RESIGN'])
  );

  const actions = computed(() => {
    return (row: OpportunityItem) => {
      const transferAction = [
        {
          key: 'transfer',
          label: t('common.transfer'),
          icon: 'iconicon_jump',
          permission: ['OPPORTUNITY_MANAGEMENT:TRANSFER'],
          action: (item: OpportunityItem) => {
            handleTransfer(item.id);
          },
        },
      ];

      const editAction = [
        {
          key: 'edit',
          label: t('common.edit'),
          icon: 'iconicon_handwritten_signature',
          permission: ['OPPORTUNITY_MANAGEMENT:UPDATE'],
          action: (item: OpportunityItem) => {
            router.push({
              name: CommonRouteEnum.FORM_CREATE,
              query: {
                id: item.id,
                formKey: FormDesignKeyEnum.BUSINESS,
                needInitDetail: 'Y',
              },
            });
          },
        },
      ];

      if (row.stage === failureStage.value?.id) {
        return resolveRowActions(row, transferAction);
      }

      if (row.stage === successStage.value?.id) {
        return hasBackStagePermission.value ? resolveRowActions(row, editAction) : resolveRowActions(row, []);
      }

      return resolveRowActions(row, [
        ...editAction,
        {
          key: 'writeRecord',
          label: t('common.writeRecord'),
          icon: 'iconicon_handwritten_signature',
          permission: ['OPPORTUNITY_MANAGEMENT:UPDATE'],
          action: (item: OpportunityItem) => {
            const { customerName, customerId, name } = item;
            const initialSourceName = JSON.stringify({
              name,
              customerName,
              customerId,
            });
            router.push({
              name: CommonRouteEnum.FORM_CREATE,
              query: {
                id: item.id,
                formKey: FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS,
                initialSourceName,
              },
            });
          },
        },
        ...transferAction,
        {
          key: 'delete',
          label: t('common.delete'),
          icon: 'iconicon_delete',
          permission: ['OPPORTUNITY_MANAGEMENT:DELETE'],
          action: (item: OpportunityItem) => {
            showConfirmDialog({
              title: t('opportunity.deleteTitle'),
              message: t('opportunity.deleteContentTip'),
              confirmButtonText: deleteExecute.value
                ? t('crm.approval.confirmAndSubmitReview')
                : t('common.confirmDelete'),
              confirmButtonColor: 'var(--error-red)',
              beforeClose: async (action) => {
                if (action === 'confirm') {
                  try {
                    await deleteOpt(item.id);
                    showSuccessToast(deleteExecute.value ? t('common.reviewSuccess') : t('common.deleteSuccess'));
                    crmListRef.value?.loadList(true);
                    return Promise.resolve(true);
                  } catch (error) {
                    // eslint-disable-next-line no-console
                    console.log(error);
                    return Promise.resolve(false);
                  }
                } else {
                  return Promise.resolve(true);
                }
              },
            });
          },
        },
      ]);
    };
  });

  const { transformFormData } = await useFormCreateTransform(FormDesignKeyEnum.CUSTOMER);

  async function search() {
    showLoadingToast(t('common.searching'));
    await crmListRef.value?.loadList(true);
    closeToast();
  }

  function goCreate() {
    router.push({
      name: CommonRouteEnum.FORM_CREATE,
      query: {
        formKey: FormDesignKeyEnum.BUSINESS,
      },
    });
  }

  function goDetail(item: OpportunityItem) {
    const { id, name } = item;
    router.push({
      name: OpportunityRouteEnum.OPPORTUNITY_DETAIL,
      query: {
        id,
        name,
      },
    });
  }

  function clear() {
    nextTick(() => {
      search();
    });
  }

  watch(
    () => activeFilter.value,
    () => {
      nextTick(() => {
        crmListRef.value?.loadList(true);
      });
    }
  );

  onActivated(() => {
    initApprovalPermission();
    initStageConfig();
    crmListRef.value?.loadList(true);
  });

  onBeforeMount(() => {
    initApprovalPermission();
  });
</script>

<style lang="less" scoped>
  .filter-buttons {
    @apply flex;

    gap: 8px;
    padding: 8px 4px;
    background-color: var(--text-n10);
    .half-px-border-bottom();
  }
</style>
