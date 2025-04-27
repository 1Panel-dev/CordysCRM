<template>
  <div class="flex h-full flex-col overflow-hidden">
    <div class="flex items-center gap-[12px] bg-[var(--text-n10)] p-[8px_16px]">
      <van-button
        v-permission="['CLUE_MANAGEMENT:ADD']"
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
        :placeholder="t('customer.searchPlaceholder')"
        class="flex-1 !p-0"
        @search="search"
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
    <CrmList
      ref="crmListRef"
      :keyword="keyword"
      :list-params="{
        searchType: activeFilter,
      }"
      class="p-[16px]"
      :item-gap="16"
      :load-list-api="getClueList"
      :transform="transformFormData"
    >
      <template #item="{ item }">
        <CrmListCommonItem :item="item" :actions="actions(item)" @click="goDetail"></CrmListCommonItem>
      </template>
    </CrmList>
    <van-action-sheet
      v-model:show="moreActionShow"
      :actions="moreActions"
      :cancel-text="hasAnyPermission(['CLUE_MANAGEMENT:DELETE']) ? t('common.delete') : ''"
      @cancel="handleDelete"
    />
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { ActionSheetAction, showConfirmDialog, showSuccessToast } from 'vant';

  import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ClueListItem } from '@lib/shared/models/clue';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmListCommonItem from '@/components/pure/crm-list-common-item/index.vue';

  import { deleteClue, getClueList } from '@/api/modules';
  import useFormCreateTransform from '@/hooks/useFormCreateTransform';
  import { hasAllPermission, hasAnyPermission } from '@/utils/permission';

  import { ClueRouteEnum, CommonRouteEnum, CustomerRouteEnum } from '@/enums/routeEnum';

  const { t } = useI18n();
  const router = useRouter();

  const crmListRef = ref<InstanceType<typeof CrmList>>();
  const keyword = ref('');
  const activeFilter = ref(CustomerSearchTypeEnum.ALL);
  const filterButtons = [
    {
      name: CustomerSearchTypeEnum.ALL,
      tab: t('customer.all'),
    },
    {
      name: CustomerSearchTypeEnum.SELF,
      tab: t('menu.mine'),
    },
    {
      name: CustomerSearchTypeEnum.DEPARTMENT,
      tab: t('clue.department'),
    },
    {
      name: CustomerSearchTypeEnum.CUSTOMER_TRANSITION,
      tab: t('clue.convertedToCustomer'),
    },
    {
      name: CustomerSearchTypeEnum.OPPORTUNITY_TRANSITION,
      tab: t('clue.convertedToOpportunity'),
    },
  ];

  const activeClue = ref<ClueListItem>();
  const activeClueId = computed(() => activeClue.value?.id ?? '');
  const { transformFormData } = await useFormCreateTransform(FormDesignKeyEnum.CLUE);

  const moreActionShow = ref(false);

  function handleEdit(id: string) {
    router.push({
      name: CommonRouteEnum.FORM_CREATE,
      query: {
        id,
        formKey: FormDesignKeyEnum.CLUE,
        needInitDetail: 'Y',
      },
    });
  }

  function handleTransfer(id: string) {
    router.push({
      name: CustomerRouteEnum.CUSTOMER_TRANSFER,
      query: {
        id,
        apiKey: FormDesignKeyEnum.CLUE,
      },
    });
  }

  function convertTo(id: string, formKey: FormDesignKeyEnum) {
    router.push({
      name: CommonRouteEnum.FORM_CREATE,
      query: {
        id: '',
        formKey,
        initialSourceName: activeClue.value?.name,
      },
      state: {
        params: JSON.stringify({ clueId: id }),
      },
    });
  }

  function handleDelete() {
    showConfirmDialog({
      title: t('clue.deleteTitle'),
      message: t('clue.batchDeleteContentTip'),
      confirmButtonText: t('common.confirmDelete'),
      confirmButtonColor: 'var(--error-red)',
      beforeClose: async (action) => {
        if (action === 'confirm') {
          try {
            await deleteClue(activeClueId.value);
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
  }

  const moreActions: ActionSheetAction[] = [
    {
      name: t('common.edit'),
      permission: ['CLUE_MANAGEMENT:UPDATE'],
      callback: () => {
        handleEdit(activeClueId.value);
      },
    },
    {
      name: t('common.transfer'),
      permission: ['CLUE_MANAGEMENT:UPDATE'],
      callback: () => {
        handleTransfer(activeClueId.value);
      },
    },
    {
      name: t('common.convertToOpportunity'),
      permission: ['CLUE_MANAGEMENT:READ', 'OPPORTUNITY_MANAGEMENT:ADD'],
      allPermission: true,
      callback: () => {
        convertTo(activeClueId.value, FormDesignKeyEnum.CLUE_TRANSITION_BUSINESS);
      },
    },
    {
      name: t('common.convertToCustomer'),
      permission: ['CLUE_MANAGEMENT:READ', 'CUSTOMER_MANAGEMENT:ADD'],
      allPermission: true,
      callback: () => {
        convertTo(activeClueId.value, FormDesignKeyEnum.CLUE_TRANSITION_CUSTOMER);
      },
    },
  ].filter((e) => (e.allPermission ? hasAllPermission(e.permission) : hasAnyPermission(e.permission)));

  const actions = computed(() => {
    return (row: ClueListItem) => {
      if (row.transitionType && ['CUSTOMER', 'OPPORTUNITY'].includes(row.transitionType)) return [];
      return [
        {
          label: t('common.edit'),
          icon: 'iconicon_handwritten_signature',
          permission: ['CLUE_MANAGEMENT:UPDATE'],
          action: (item: ClueListItem) => {
            handleEdit(item.id);
          },
        },
        {
          label: t('common.transfer'),
          icon: 'iconicon_jump',
          permission: ['CLUE_MANAGEMENT:UPDATE'],
          action: (item: ClueListItem) => {
            handleTransfer(item.id);
          },
        },
        {
          label: t('common.convertToOpportunity'),
          icon: 'iconicon_edit1',
          permission: ['CLUE_MANAGEMENT:READ', 'OPPORTUNITY_MANAGEMENT:ADD'],
          allPermission: true,
          action: (item: ClueListItem) => {
            activeClue.value = item;
            convertTo(item.id, FormDesignKeyEnum.CLUE_TRANSITION_BUSINESS);
          },
        },
        ...(moreActions.length
          ? [
              {
                label: t('common.more'),
                icon: 'iconicon_ellipsis',
                permission: [],
                action: (item: ClueListItem) => {
                  moreActionShow.value = true;
                  activeClue.value = item;
                },
              },
            ]
          : []),
      ];
    };
  });

  function search() {
    crmListRef.value?.loadList(true);
  }

  watch(
    () => activeFilter.value,
    () => {
      nextTick(() => {
        search();
      });
    }
  );

  function goCreate() {
    router.push({
      name: CommonRouteEnum.FORM_CREATE,
      query: {
        formKey: FormDesignKeyEnum.CLUE,
      },
    });
  }

  function goDetail(item: Record<string, any>) {
    router.push({
      name: ClueRouteEnum.CLUE_DETAIL,
      query: {
        id: item.id,
        name: item.name,
        transitionType: item.transitionType,
        needInitDetail: 'Y',
      },
    });
  }
</script>

<style lang="less" scoped>
  .filter-buttons {
    @apply flex;

    overflow: auto;
    padding: 8px 4px;
    min-height: 48px;
    white-space: nowrap;
    background-color: var(--text-n10);
    gap: 8px;
    &::-webkit-scrollbar {
      display: none;
    }
    .half-px-border-bottom();
  }
  :deep(.van-action-sheet__cancel) {
    color: var(--text-n1);
  }
</style>
