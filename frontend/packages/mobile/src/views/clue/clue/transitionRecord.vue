<template>
  <div class="h-full overflow-auto bg-[var(--text-n9)]">
    <van-pull-refresh v-model="refreshing" @refresh="loadTransitionRecordList">
      <div class="p-[16px]">
        <van-loading v-if="loading" class="py-[24px]" vertical />
        <van-empty v-else-if="!transitionRecords.length" :description="t('common.noData')" />
        <template v-else-if="hasPermission">
          <CrmListCommonItem
            v-for="item of customerList"
            :key="item.id"
            class="mb-[16px] last:mb-0"
            :item="item"
            :actions="getCustomerActions(item)"
            @click="goCustomerDetail(item)"
          />
        </template>
        <div v-else class="rounded-[var(--border-radius-small)] bg-[var(--text-n10)] px-[20px] py-[16px]">
          <div v-for="item of transitionRecords" :key="item.id" class="transition-record-item">
            <div v-for="field of getRecordFields(item)" :key="field.label" class="flex gap-[16px]">
              <div class="w-[96px] shrink-0 text-[14px] text-[var(--text-n4)]">{{ field.label }}</div>
              <div class="break-all text-[14px] text-[var(--text-n1)]">{{ field.value }}</div>
            </div>
          </div>
        </div>
      </div>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
  import dayjs from 'dayjs';
  import { useRouter } from 'vue-router';
  import { showConfirmDialog, showSuccessToast } from 'vant';

  import CrmListCommonItem from '@/components/pure/crm-list-common-item/index.vue';

  import { deleteCustomer, getClueTransitionCustomerRecordList } from '@/api/modules';
  import useApprovalOperation from '@/hooks/useApprovalOperation';
  import useApprovalResourceAction from '@/hooks/useApprovalResourceAction';
  import { ClueRouteEnum, CommonRouteEnum, CustomerRouteEnum } from '@/enums/routeEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ReasonTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CustomerListItem } from '@lib/shared/models/customer';

  const props = defineProps<{
    sourceId: string;
  }>();
  const { t } = useI18n();
  const router = useRouter();
  const loading = ref(false);
  const refreshing = ref(false);
  const hasPermission = ref(false);
  const transitionRecords = ref<CustomerListItem[]>([]);
  const { reviewByResourceId, revokeByResourceId } = useApprovalResourceAction({
    formKey: FormDesignKeyEnum.CUSTOMER,
  });
  const { resolveRowActions, hasApprovalScopedPermission, initApprovalPermission, deleteExecute } =
    useApprovalOperation<CustomerListItem>({
      formType: FormDesignKeyEnum.CUSTOMER,
      onReview: (row) => reviewByResourceId(row.id, { onSuccess: loadTransitionRecordList }),
      onRevoke: (row) => revokeByResourceId(row.id, { onSuccess: loadTransitionRecordList }),
    });

  const customerList = computed(() =>
    transitionRecords.value.map((item) => ({
      ...item,
      description: [
        {
          label: t('common.head'),
          value: item.ownerName || '-',
        },
        {
          label: t('common.remainingBelong'),
          value:
            item.reservedDays === null || item.reservedDays === undefined
              ? '-'
              : `${item.reservedDays}${t('common.dayUnit')}`,
        },
      ],
    }))
  );

  function getRecordFields(item: CustomerListItem) {
    return [
      { label: t('clue.transitionCustomerName'), value: item.name || '-' },
      { label: t('common.head'), value: item.ownerName || '-' },
      { label: t('common.department'), value: item.departmentName || '-' },
      {
        label: t('customer.lastFollowUpDate'),
        value: item.latestFollowUpTime ? dayjs(item.latestFollowUpTime).format('YYYY-MM-DD') : '-',
      },
      { label: t('common.createTime'), value: item.createTime ? dayjs(item.createTime).format('YYYY-MM-DD') : '-' },
    ];
  }

  const actions = [
    {
      key: 'edit',
      label: t('common.edit'),
      icon: 'iconicon_handwritten_signature',
      permission: ['CUSTOMER_MANAGEMENT:UPDATE'],
      action: (item: CustomerListItem) => {
        router.push({
          name: CommonRouteEnum.FORM_CREATE,
          query: {
            id: item.id,
            formKey: FormDesignKeyEnum.CUSTOMER,
            needInitDetail: 'Y',
          },
        });
      },
    },
    {
      key: 'transfer',
      label: t('common.transfer'),
      icon: 'iconicon_jump',
      permission: ['CUSTOMER_MANAGEMENT:TRANSFER'],
      action: (item: CustomerListItem) => {
        router.push({
          name: CustomerRouteEnum.CUSTOMER_TRANSFER,
          query: {
            id: item.id,
            apiKey: FormDesignKeyEnum.CUSTOMER,
            approved: item.approved ? 'Y' : 'N',
          },
        });
      },
    },
    {
      key: 'moveToPool',
      label: t('customer.moveToOpenSea'),
      icon: 'iconicon_swap',
      permission: ['CUSTOMER_MANAGEMENT:RECYCLE'],
      action: (item: CustomerListItem) => {
        router.push({
          name: ClueRouteEnum.MOVE_TO_POOL,
          query: {
            id: item.id,
            name: item.name,
            reasonKey: ReasonTypeEnum.CUSTOMER_POOL_RS,
          },
        });
      },
    },
    {
      key: 'writeRecord',
      label: t('common.writeRecord'),
      icon: 'iconicon_edit1',
      permission: ['CUSTOMER_MANAGEMENT:UPDATE'],
      action: (item: CustomerListItem) => {
        router.push({
          name: CommonRouteEnum.FORM_CREATE,
          query: {
            id: item.id,
            formKey: FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER,
            initialSourceName: item.name,
          },
        });
      },
    },
    {
      key: 'delete',
      label: t('common.delete'),
      icon: 'iconicon_delete',
      permission: ['CUSTOMER_MANAGEMENT:DELETE'],
      action: (item: CustomerListItem) => {
        showConfirmDialog({
          title: t('customer.deleteTitle'),
          message: t('customer.deleteTip'),
          confirmButtonText: deleteExecute.value ? t('crm.approval.confirmAndSubmitReview') : t('common.confirmDelete'),
          confirmButtonColor: 'var(--error-red)',
          beforeClose: async (action) => {
            if (action !== 'confirm') {
              return Promise.resolve(true);
            }

            try {
              await deleteCustomer(item.id);
              showSuccessToast(deleteExecute.value ? t('common.reviewSuccess') : t('common.deleteSuccess'));
              await loadTransitionRecordList();
              return Promise.resolve(true);
            } catch (error) {
              // eslint-disable-next-line no-console
              console.log(error);
              return Promise.resolve(false);
            }
          },
        });
      },
    },
  ];

  function getCustomerActions(item: CustomerListItem) {
    if (item.collaborationType === 'READ_ONLY') {
      return [];
    }
    return resolveRowActions(item, actions);
  }

  async function loadTransitionRecordList() {
    try {
      loading.value = true;
      const result = await getClueTransitionCustomerRecordList(props.sourceId);
      hasPermission.value = result.hasPermission;
      transitionRecords.value = result.list;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
      refreshing.value = false;
    }
  }

  function goCustomerDetail(item: CustomerListItem) {
    if (!hasApprovalScopedPermission(item, ['CUSTOMER_MANAGEMENT:READ'])) {
      return;
    }

    router.push({
      name: CustomerRouteEnum.CUSTOMER_DETAIL,
      query: {
        id: item.id,
        name: item.name,
        needInitDetail: 'Y',
      },
    });
  }

  onBeforeMount(() => {
    initApprovalPermission();
    loadTransitionRecordList();
  });

  onActivated(() => {
    initApprovalPermission();
    loadTransitionRecordList();
  });
</script>

<style lang="less" scoped>
  .transition-record-item {
    @apply flex flex-col;

    gap: 8px;
    &:not(:last-child) {
      margin-bottom: 16px;
      padding-bottom: 16px;
      .half-px-border-bottom();
    }
  }
</style>
