<template>
  <div class="flex h-full flex-col overflow-hidden bg-[var(--text-n9)]">
    <div class="top-bar">
      <van-button
        v-if="hasAnyPermission(['CUSTOMER_MANAGEMENT_CONTACT:ADD']) && !props.readonly"
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
        :placeholder="t('customer.searchContactPlaceholder')"
        class="flex-1 !p-0"
        @search="searchList"
        @clear="searchList"
      />
    </div>
    <div v-if="!props.sourceId" class="filter-buttons">
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
    <CrmList
      ref="crmListRef"
      :list-params="listParams"
      :keyword="keyword"
      :load-list-api="loadApiMap[props.formKey]"
      class="p-[16px]"
      :item-gap="16"
      :transform="transformFormData"
      :no-page-nation="!!props.sourceId"
    >
      <template #item="{ item }">
        <div
          class="flex w-full items-center gap-[16px] overflow-hidden rounded-[var(--border-radius-small)] bg-[var(--text-n10)] p-[16px]"
          @click="goDetail(item)"
        >
          <CrmAvatar :text="item.name" />
          <div class="flex flex-1 flex-col gap-[2px] overflow-hidden">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-[8px] overflow-hidden">
                <div class="one-line-text text-[16px] text-[var(--text-n1)]">{{ item.name }}</div>
                <van-tag
                  color="var(--success-5)"
                  text-color="var(--success-green)"
                  class="min-w-[36px] rounded-[var(--border-radius-small)] !p-[2px_6px]"
                >
                  {{ t('common.normal') }}
                </van-tag>
              </div>
              <CrmTextButton
                v-if="hasAnyPermission(['CUSTOMER_MANAGEMENT_CONTACT:DELETE']) && !props.readonly"
                icon="iconicon_delete"
                icon-size="16px"
                color="var(--error-red)"
                @click="() => handleDelete(item.id)"
              />
            </div>
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-[4px]">
                <a :href="`tel:${item.phone}`" class="flex items-center" @click.stop>
                  <CrmIcon name="iconicon_phone_outgoing" width="14px" height="14px" color="var(--primary-8)" />
                </a>
                <a :href="`tel:${item.phone}`" class="text-[12px] text-[var(--primary-8)]" @click.stop>
                  {{ item.phone?.replace(/(\d{3})(\d{4})(\d{4})/, '$1 $2 $3') }}
                </a>
                <CrmIcon
                  name="iconicon_file_copy"
                  width="12px"
                  height="12px"
                  color="var(--primary-8)"
                  @click.stop="() => handleCopy(item.phone)"
                />
              </div>
            </div>
          </div>
        </div>
      </template>
    </CrmList>
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { useClipboard } from '@vueuse/core';
  import { showConfirmDialog, showFailToast, showSuccessToast } from 'vant';

  import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { sleep } from '@lib/shared/method';
  import { CommonList } from '@lib/shared/models/common';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';

  import {
    deleteCustomerContact,
    getContactListUnderCustomer,
    getCustomerContactList,
    getOpportunityContactList,
  } from '@/api/modules';
  import useFormCreateTransform from '@/hooks/useFormCreateTransform';
  import useHiddenTab from '@/hooks/useHiddenTab';
  import { hasAnyPermission } from '@/utils/permission';

  import { CommonRouteEnum } from '@/enums/routeEnum';

  export type ContactFormKey =
    | FormDesignKeyEnum.CUSTOMER_CONTACT
    | FormDesignKeyEnum.CONTACT
    | FormDesignKeyEnum.BUSINESS_CONTACT;

  const props = defineProps<{
    sourceId?: string;
    customerName?: string;
    formKey: ContactFormKey;
    readonly?: boolean;
  }>();

  const { t } = useI18n();
  const router = useRouter();
  const { copy, isSupported } = useClipboard({ legacy: true });

  const loadApiMap: Record<
    ContactFormKey,
    (...args: any) => Promise<CommonList<Record<string, any>> | Record<string, any>>
  > = {
    [FormDesignKeyEnum.CUSTOMER_CONTACT]: getContactListUnderCustomer,
    [FormDesignKeyEnum.CONTACT]: getCustomerContactList,
    [FormDesignKeyEnum.BUSINESS_CONTACT]: getOpportunityContactList,
  };

  const { transformFormData } = await useFormCreateTransform(
    props.sourceId ? FormDesignKeyEnum.CUSTOMER_CONTACT : FormDesignKeyEnum.CONTACT
  );

  const filterButtons = [
    {
      name: CustomerSearchTypeEnum.ALL,
      tab: t('contact.all'),
    },
    {
      name: CustomerSearchTypeEnum.SELF,
      tab: t('contact.mine'),
    },
    {
      name: CustomerSearchTypeEnum.DEPARTMENT,
      tab: t('contact.department'),
    },
  ];

  const { tabList, activeFilter } = useHiddenTab(
    filterButtons,
    !props.sourceId ? FormDesignKeyEnum.CONTACT : undefined
  );

  const crmListRef = ref<InstanceType<typeof CrmList>>();
  const keyword = ref('');
  const listParams = computed(() => {
    return {
      searchType: activeFilter.value,
      keyword: keyword.value,
      id: props.sourceId,
    };
  });

  function handleCopy(val: string) {
    if (isSupported) {
      copy(val);
      showSuccessToast(t('common.copySuccess'));
    } else {
      showFailToast(t('common.copyNotSupport'));
    }
  }

  function handleDelete(id: string) {
    showConfirmDialog({
      title: t('contact.deleteTitle'),
      message: t('contact.deleteTip'),
      confirmButtonText: t('common.confirmDelete'),
      confirmButtonColor: 'var(--error-red)',
      beforeClose: async (action) => {
        if (action === 'confirm') {
          try {
            await deleteCustomerContact(id);
            showSuccessToast(t('common.deleteSuccess'));
            await sleep(300);
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

  function goCreate() {
    router.push({
      name: CommonRouteEnum.FORM_CREATE,
      query: {
        id: props.sourceId,
        formKey: props.sourceId ? FormDesignKeyEnum.CUSTOMER_CONTACT : FormDesignKeyEnum.CONTACT,
        initialSourceName: props.customerName,
      },
    });
  }

  function goDetail(item: any) {
    router.push({
      name: CommonRouteEnum.CONTACT_DETAIL,
      query: {
        id: item.id,
        name: item.name,
        needInitDetail: 'Y',
        readonly: props.readonly !== false ? 'Y' : 'N',
      },
    });
  }

  function loadList() {
    crmListRef.value?.loadList(true);
  }

  function searchList() {
    nextTick(() => {
      if (props.sourceId) {
        crmListRef.value?.filterListByKeyword('name');
      } else {
        loadList();
      }
    });
  }

  onActivated(() => {
    loadList();
  });

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
