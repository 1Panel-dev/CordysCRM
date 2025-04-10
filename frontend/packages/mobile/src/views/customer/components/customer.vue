<template>
  <div class="flex h-full flex-col overflow-hidden">
    <div class="flex items-center gap-[12px] bg-[var(--text-n10)] p-[8px_16px]">
      <van-button plain icon="plus" type="primary" size="small" @click="goCreate"> </van-button>
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
    <div class="flex-1 overflow-hidden">
      <CrmList
        ref="crmListRef"
        :keyword="keyword"
        :list-params="listParams"
        class="p-[16px]"
        :item-gap="16"
        :load-list-api="getCustomerList"
        :transform="transformFormData"
      >
        <template #item="{ item }">
          <CrmListCommonItem :item="item" :actions="actions" name-key="ownerName" @click="goDetail"></CrmListCommonItem>
        </template>
      </CrmList>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { closeToast, showConfirmDialog, showLoadingToast, showSuccessToast } from 'vant';

  import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmListCommonItem from '@/components/pure/crm-list-common-item/index.vue';

  import { getCustomerList } from '@/api/modules';
  import useFormCreateTransform from '@/hooks/useFormCreateTransform';

  import { CommonRouteEnum, CustomerRouteEnum } from '@/enums/routeEnum';

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
      tab: t('customer.mine'),
    },
    {
      name: CustomerSearchTypeEnum.DEPARTMENT,
      tab: t('customer.deptCustomer'),
    },
    {
      name: CustomerSearchTypeEnum.VISIBLE,
      tab: t('customer.cooperationCustomer'),
    },
  ];
  const listParams = computed(() => {
    return {
      searchType: activeFilter.value,
      keyword: keyword.value,
    };
  });

  const { transformFormData } = await useFormCreateTransform(FormDesignKeyEnum.CUSTOMER);

  const actions = [
    {
      label: t('common.edit'),
      icon: 'iconicon_handwritten_signature',
      permission: [],
      action: (item: any) => {
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
      label: t('common.transfer'),
      icon: 'iconicon_jump',
      permission: [],
      action: (item: any) => {
        router.push({
          name: CustomerRouteEnum.CUSTOMER_TRANSFER,
          query: {
            id: item.id,
          },
        });
      },
    },
    {
      label: t('common.writeRecord'),
      icon: 'iconicon_edit1',
      permission: [],
      action: (item: any) => {
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
      label: t('common.delete'),
      icon: 'iconicon_delete',
      permission: [],
      action: (item: any) => {
        showConfirmDialog({
          title: t('customer.deleteTitle'),
          message: t('customer.deleteTip'),
          confirmButtonText: t('common.confirmDelete'),
          confirmButtonColor: 'var(--error-red)',
          beforeClose: (action) => {
            if (action === 'confirm') {
              try {
                // TODO: delete customer
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
  ];

  watch(
    () => activeFilter.value,
    () => {
      nextTick(() => {
        crmListRef.value?.loadList(true);
      });
    }
  );

  async function search() {
    showLoadingToast(t('common.searching'));
    await crmListRef.value?.loadList(true);
    closeToast();
  }

  function goCreate() {
    router.push({
      name: CommonRouteEnum.FORM_CREATE,
      query: {
        formKey: FormDesignKeyEnum.CUSTOMER,
      },
    });
  }

  function goDetail(item: any) {
    router.push({
      name: CustomerRouteEnum.CUSTOMER_DETAIL,
      query: {
        id: item.id,
        name: item.name,
        needInitDetail: 'Y',
      },
    });
  }
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
