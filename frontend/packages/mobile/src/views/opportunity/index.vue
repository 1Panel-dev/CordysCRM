<template>
  <div class="flex h-full flex-col overflow-hidden">
    <div
      class="flex items-center justify-center bg-[var(--text-n10)] p-[11px_16px] text-[18px] font-semibold text-[var(--text-n1)]"
    >
      {{ t('common.opportunity') }}
    </div>
    <div class="flex items-center gap-[12px] bg-[var(--text-n10)] p-[8px_16px]">
      <van-button plain icon="plus" type="primary" size="small" @click="goCreate"> </van-button>
      <van-search
        v-model="keyword"
        shape="round"
        :placeholder="t('opportunity.searchPlaceholder')"
        class="flex-1 !p-0"
        @search="search"
        @clear="clear"
      />
    </div>
    <div class="filter-buttons">
      <van-button
        v-for="item of filterButtons"
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
  import { OpportunitySearchTypeEnum, StageResultEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { OpportunityItem } from '@lib/shared/models/opportunity';

  import CrmList from '@/components/pure/crm-list/index.vue';

  import { deleteOpt, getOpportunityList } from '@/api/modules';
  import useFormCreateTransform from '@/hooks/useFormCreateTransform';

  import { CommonRouteEnum, CustomerRouteEnum, OpportunityRouteEnum } from '@/enums/routeEnum';

  const router = useRouter();
  const { t } = useI18n();

  const keyword = ref('');
  const crmListRef = ref<InstanceType<typeof CrmList>>();

  const activeFilter = ref(OpportunitySearchTypeEnum.ALL);

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
      name: OpportunitySearchTypeEnum.DEAL,
      tab: t('opportunity.convertedOpportunities'),
    },
  ];

  const listParams = computed(() => {
    return {
      searchType: activeFilter.value,
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

  const actions = computed(() => {
    return (row: OpportunityItem) => {
      const showAction = row.stage !== StageResultEnum.FAIL && row.stage !== StageResultEnum.SUCCESS;
      const transferAction = [
        {
          label: t('common.transfer'),
          icon: 'iconicon_jump',
          permission: ['OPPORTUNITY_MANAGEMENT:UPDATE'],
          action: (item: OpportunityItem) => {
            handleTransfer(item.id);
          },
        },
      ];
      return showAction
        ? [
            {
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
            {
              label: t('common.writeRecord'),
              icon: 'iconicon_handwritten_signature',
              permission: ['OPPORTUNITY_MANAGEMENT:UPDATE'],
              action: (item: OpportunityItem) => {
                router.push({
                  name: CommonRouteEnum.FORM_CREATE,
                  query: {
                    id: item.id,
                    formKey: FormDesignKeyEnum.FOLLOW_RECORD_BUSINESS,
                    initialSourceName: item.name,
                  },
                });
              },
            },
            ...transferAction,
            {
              label: t('common.delete'),
              icon: 'iconicon_delete',
              permission: ['OPPORTUNITY_MANAGEMENT:DELETE'],
              action: (item: OpportunityItem) => {
                showConfirmDialog({
                  title: t('customer.deleteTitle'),
                  message: t('opportunity.deleteContentTip'),
                  confirmButtonText: t('common.confirmDelete'),
                  confirmButtonColor: 'var(--error-red)',
                  beforeClose: async (action) => {
                    if (action === 'confirm') {
                      try {
                        await deleteOpt(item.id);
                        showSuccessToast(t('common.deleteSuccess'));
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
          ]
        : transferAction;
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
