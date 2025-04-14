<template>
  <div class="flex h-full flex-col overflow-hidden bg-[var(--text-n9)]">
    <div class="top-bar">
      <van-button plain icon="plus" type="primary" size="small" @click="goCreate"> </van-button>
      <van-search v-model="keyword" shape="round" :placeholder="t('common.pleaseInputKeyword')" class="flex-1 !p-0" />
    </div>
    <div class="flex-1 overflow-hidden">
      <CrmList
        ref="crmListRef"
        :keyword="keyword"
        :load-list-api="loadListApi[props.type]"
        class="p-[16px]"
        :list-params="{
          sourceId: props.sourceId,
          status: CustomerFollowPlanStatusEnum.ALL,
        }"
        :item-gap="16"
      >
        <template #item="{ item }">
          <listItem
            :item="item"
            type="plan"
            @click="goDetail(item)"
            @delete="handleDelete(item)"
            @edit="handleEdit(item)"
            @cancel="handleCancel(item)"
          />
        </template>
      </CrmList>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { showSuccessToast } from 'vant';

  import { CustomerFollowPlanStatusEnum } from '@lib/shared/enums/customerEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import listItem from './components/listItem.vue';

  import { getClueFollowPlanList, getCustomerFollowPlanList } from '@/api/modules';

  import { CommonRouteEnum } from '@/enums/routeEnum';

  const props = defineProps<{
    type: FormDesignKeyEnum.FOLLOW_PLAN_CUSTOMER | FormDesignKeyEnum.FOLLOW_PLAN_CLUE;
    sourceId: string;
  }>();

  const { t } = useI18n();
  const keyword = ref('');
  const router = useRouter();

  const crmListRef = ref<InstanceType<typeof CrmList>>();

  const loadListApi = {
    [FormDesignKeyEnum.FOLLOW_PLAN_CUSTOMER]: getCustomerFollowPlanList,
    [FormDesignKeyEnum.FOLLOW_PLAN_CLUE]: getClueFollowPlanList,
  };

  async function handleDelete(item: any) {
    try {
      // TODO: delete customer
      showSuccessToast(t('common.deleteSuccess'));
      crmListRef.value?.loadList(true);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function handleCancel(item: any) {
    try {
      // TODO: delete customer
      showSuccessToast(t('common.cancelSuccess'));
      crmListRef.value?.loadList(true);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function goCreate() {
    router.push({
      name: CommonRouteEnum.FORM_CREATE,
      query: {
        formKey: props.type,
      },
    });
  }

  function handleEdit(item: any) {
    router.push({
      name: CommonRouteEnum.FORM_CREATE,
      query: {
        formKey: props.type,
        id: item.id,
        needInitDetail: 'Y',
      },
    });
  }

  function goDetail(item: any) {
    router.push({
      name: CommonRouteEnum.FOLLOW_DETAIL,
      query: {
        formKey: props.type,
        id: item.id,
        name: item.name,
        needInitDetail: 'Y',
      },
    });
  }
</script>

<style lang="less" scoped></style>
