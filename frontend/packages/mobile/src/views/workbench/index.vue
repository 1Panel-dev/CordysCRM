<template>
  <div class="flex h-full flex-col gap-[16px] overflow-hidden">
    <div class="flex items-center gap-[12px] bg-[var(--text-n10)] px-[12px] py-[4px]">
      <van-image
        round
        width="40px"
        height="40px"
        src="https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg"
        @click="goMine"
      />
      <van-search
        v-model="keyword"
        shape="round"
        :placeholder="t('workbench.searchPlaceholder')"
        class="flex-1 !p-0"
        @click="goDuplicateCheck"
      />
      <CrmIcon name="iconicon_notification" width="21px" height="21px" @click="goMineMessage" />
    </div>
    <van-cell-group inset class="py-[16px]">
      <van-cell :border="false" class="!py-0">
        <template #title>
          <div class="font-semibold text-[var(--text-n1)]">{{ t('workbench.quickEntry') }}</div>
        </template>
      </van-cell>
      <div class="flex flex-wrap">
        <div v-for="card of entryCardList" :key="card.name" class="quick-entry-card" @click="goCardRoute(card.name)">
          <CrmIcon :name="card.icon" width="30px" height="30px" />
          <div class="text-[12px] text-[var(--text-n1)]">{{ card.label }}</div>
        </div>
      </div>
    </van-cell-group>
    <van-cell-group inset class="flex-1 py-[16px]">
      <van-cell :border="false" class="!py-0">
        <template #title>
          <div class="font-semibold text-[var(--text-n1)]">{{ t('common.message') }}</div>
        </template>
        <template #value>
          <div class="text-[var(--text-n4)]" @click="goMineMessage">
            {{ t('common.checkMore') }}
          </div>
        </template>
      </van-cell>
      <CrmList ref="crmListRef" :load-list-api="getHomeMessageList" no-page-nation>
        <template #item="{ item }">
          <CrmMessageItem :item="item" @load-list="() => initMessageList()" />
        </template>
      </CrmList>
    </van-cell-group>
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmMessageItem from '@/components/business/crm-message-item/index.vue';

  import { getHomeMessageList } from '@/api/modules';

  import { CommonRouteEnum, MineRouteEnum, WorkbenchRouteEnum } from '@/enums/routeEnum';

  const { t } = useI18n();
  const router = useRouter();

  const keyword = ref('');
  const entryCardList = [
    {
      icon: 'icon-newCustomer',
      label: t('common.newCustomer'),
      name: FormDesignKeyEnum.CUSTOMER,
    },
    {
      icon: 'icon-newContact',
      label: t('common.newContact'),
      name: FormDesignKeyEnum.CUSTOMER_CONTACT,
    },
    {
      icon: 'icon-newOpportunity',
      label: t('common.newOpportunity'),
      name: FormDesignKeyEnum.BUSINESS,
    },
    {
      icon: 'icon-newClue',
      label: t('common.newClue'),
      name: FormDesignKeyEnum.CLUE,
    },
    // {
    //   icon: 'icon-newRecord',
    //   label: t('common.newFollowRecord'),
    //   name: 'followRecord',
    // },
    // {
    //   icon: 'icon-newPlan',
    //   label: t('common.newFollowPlan'),
    //   name: 'followPlan',
    // },
  ];

  function goMine() {
    router.push({ name: MineRouteEnum.MINE_INDEX });
  }

  function goMineMessage() {
    router.push({ name: MineRouteEnum.MINE_MESSAGE });
  }

  function goDuplicateCheck() {
    router.push({ name: WorkbenchRouteEnum.WORKBENCH_DUPLICATE_CHECK });
  }

  function goCardRoute(formKey: FormDesignKeyEnum) {
    router.push({ name: CommonRouteEnum.FORM_CREATE, query: { formKey } });
  }

  const crmListRef = ref<InstanceType<typeof CrmList>>();

  function initMessageList() {
    crmListRef.value?.loadList();
  }
</script>

<style lang="less" scoped>
  .quick-entry-card {
    @apply flex basis-1/4 flex-col items-center;

    gap: 8px;
    padding-top: 16px;
    padding-bottom: 8px;
    &:active {
      background-color: var(--text-n9);
    }
  }
</style>
