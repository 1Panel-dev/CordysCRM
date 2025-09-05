<template>
  <CrmList
    ref="crmListRef"
    v-model="list"
    not-show-loading-toast
    class="max-h-[calc(100vh-224px)]"
    :list-params="{
      keyword: props.keyword,
      ...(props.id ? { id: props.id } : {}),
    }"
    :load-list-api="api"
    :transform="props.transformData"
    :item-gap="16"
  >
    <template #item="{ item: listItem }">
      <div
        class="rounded-[var(--border-radius-small)] !bg-[var(--text-n9)] p-[16px]"
        @click="goDetail(listItem, props.searchTableKey === FormDesignKeyEnum.SEARCH_ADVANCED_CONTACT)"
      >
        <CrmDescription :description="getDescriptions(listItem)" class="!m-0 !bg-[var(--text-n9)] !p-0">
          <template #createTime="{ item }">
            {{ dayjs(item.createTime).format('YYYY-MM-DD HH:mm:ss') }}
          </template>
          <template #render="{ item }">
            <div v-if="typeof item.render === 'function'">
              {{ item.render(listItem) }}
            </div>
          </template>
          <template #customerName>
            <CrmTextButton
              :color="listItem.hasPermission ? 'var(--primary-8)' : 'var(--text-n1)'"
              :text="listItem.customerName"
              @click="goDetail(listItem, true)"
            />
          </template>
          <template #name>
            <CrmTextButton
              :color="listItem.hasPermission ? 'var(--primary-8)' : 'var(--text-n1)'"
              :text="listItem.name"
              @click="goDetail(listItem)"
            />
          </template>
        </CrmDescription>
      </div>
    </template>
  </CrmList>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import dayjs from 'dayjs';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CommonList } from '@lib/shared/models/common';

  import CrmDescription, { CrmDescriptionItem } from '@/components/pure/crm-description/index.vue';
  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';

  import { ClueRouteEnum, CustomerRouteEnum, OpportunityRouteEnum } from '@/enums/routeEnum';

  import { SearchTableKey } from '../config';

  const props = defineProps<{
    keyword: string;
    searchTableKey: SearchTableKey;
    id?: string;
    descriptionList?: CrmDescriptionItem[];
    api: (data: any) => Promise<CommonList<any>>;
    nameKey?: string; // 取数据里的nameKey字段显示在标题上
    transformData?: (i: any) => Record<string, any>;
  }>();

  const { t } = useI18n();
  const router = useRouter();

  const crmListRef = ref<InstanceType<typeof CrmList>>();
  const list = defineModel<any[]>({
    default: () => [],
  });

  const code = computed(() => crmListRef.value?.code);
  async function loadList() {
    await crmListRef.value?.loadList(true);
  }

  function getDescriptions(item: Record<string, any>) {
    return props.descriptionList?.map((column) => ({
      ...column,
      value: item[column.key as string],
    })) as CrmDescriptionItem[];
  }

  function goCustomerDetail(item: Record<string, any>) {
    router.push({
      name: CustomerRouteEnum.CUSTOMER_DETAIL,
      query: {
        id: item.id,
        name: item.name,
        needInitDetail: 'Y',
      },
    });
  }

  function goOptOrAccountDetail(item: Record<string, any>, isAccount = false) {
    if (isAccount) {
      router.push({
        name:
          item.inSharedPool || item.inCustomerPool
            ? CustomerRouteEnum.CUSTOMER_OPENSEA_DETAIL
            : CustomerRouteEnum.CUSTOMER_DETAIL,
        query: {
          id: item.customerId,
          name: item.customerName,
          needInitDetail: 'Y',
        },
      });
    } else {
      const { id, name } = item;
      router.push({
        name: OpportunityRouteEnum.OPPORTUNITY_DETAIL,
        query: {
          id,
          name,
        },
      });
    }
  }

  function goLeadDetail(item: Record<string, any>) {
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

  function goLeadPoolDetail(item: Record<string, any>) {
    router.push({
      name: ClueRouteEnum.CLUE_POOL_DETAIL,
      query: {
        id: item.id,
        name: item.name,
        needInitDetail: 'Y',
      },
    });
  }

  function goPublicPoolDetail(item: Record<string, any>) {
    router.push({
      name: CustomerRouteEnum.CUSTOMER_OPENSEA_DETAIL,
      query: {
        id: item.id,
        name: item.name,
        needInitDetail: 'Y',
      },
    });
  }

  const goDetailMap: Record<SearchTableKey, (data: any, value?: any) => void> = {
    [FormDesignKeyEnum.SEARCH_ADVANCED_CLUE]: goLeadDetail,
    [FormDesignKeyEnum.SEARCH_ADVANCED_CUSTOMER]: goCustomerDetail,
    [FormDesignKeyEnum.SEARCH_ADVANCED_CONTACT]: goOptOrAccountDetail,
    [FormDesignKeyEnum.SEARCH_ADVANCED_PUBLIC]: goPublicPoolDetail,
    [FormDesignKeyEnum.SEARCH_ADVANCED_CLUE_POOL]: goLeadPoolDetail,
    [FormDesignKeyEnum.SEARCH_ADVANCED_OPPORTUNITY]: goOptOrAccountDetail,
  };

  function goDetail(listItem: Record<string, any>, isCustomerName = false) {
    if (!listItem.hasPermission) return;
    goDetailMap[props.searchTableKey](listItem, isCustomerName);
  }

  defineExpose({
    loadList,
    code,
  });
</script>

<style lang="less" scoped>
  :deep(.crm-description) {
    padding: 0;
    gap: 4px;
  }
  :deep(.crm-description-item) {
    .crm-description-label {
      font-size: 12px;
      color: var(--text-n4);
    }
    .crm-description-value {
      font-size: 12px;
    }
  }
</style>
