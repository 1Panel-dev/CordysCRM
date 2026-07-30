<template>
  <div class="flex h-[calc(100vh-174px)] flex-col">
    <div class="filter-buttons follow-view flex gap-[8px] py-[8px]">
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
      :keyword="keyword"
      :list-params="listParams"
      class="p-[16px]"
      :item-gap="16"
      :close-init-load="!activeFilter"
      :load-list-api="getFollowRecordPage"
      :transform="transformField"
    >
      <template #item="{ item }">
        <listItem
          :item="item"
          type="record"
          :form-key="FormDesignKeyEnum.FOLLOW_RECORD"
          :readonly="props.readonly"
          @click="goDetail(item)"
          @delete="handleDelete(item)"
          @edit="handleEdit(item)"
        />
      </template>
    </CrmList>
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { closeToast, showLoadingToast, showSuccessToast } from 'vant';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { OpportunitySearchTypeEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { FollowDetailItem } from '@lib/shared/models/customer';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import listItem from '@/components/business/crm-follow-list/components/listItem.vue';
  import useFollowApi from '@/components/business/crm-follow-list/useFollowApi';

  import { deleteFollowRecord, getFollowRecordPage } from '@/api/modules';
  import useHiddenTab from '@/hooks/useHiddenTab';

  const { t } = useI18n();

  const props = defineProps<{
    readonly?: boolean;
  }>();
  const keyword = ref('');
  const crmListRef = ref<InstanceType<typeof CrmList>>();

  const { transformField, handleEdit, goDetail } = useFollowApi({
    type: 'followRecord',
    formKey: FormDesignKeyEnum.FOLLOW_RECORD,
    readonly: props.readonly,
  });

  const filterRecordButtons = [
    {
      name: OpportunitySearchTypeEnum.ALL,
      tab: t('workbench.record.all'),
    },
    {
      name: OpportunitySearchTypeEnum.SELF,
      tab: t('workbench.record.personal'),
    },
    {
      name: OpportunitySearchTypeEnum.DEPARTMENT,
      tab: t('workbench.record.depart'),
    },
  ];

  const { tabList, activeFilter } = useHiddenTab(filterRecordButtons, FormDesignKeyEnum.FOLLOW_RECORD);

  const listParams = computed(() => {
    return {
      viewId: activeFilter.value,
      keyword: keyword.value,
    };
  });

  async function handleDelete(item: FollowDetailItem) {
    try {
      await deleteFollowRecord(item.id);
      showSuccessToast(t('common.deleteSuccess'));
      crmListRef.value?.loadList(true);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  onActivated(() => {
    crmListRef.value?.loadList(true);
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

<style scoped lang="less"></style>
