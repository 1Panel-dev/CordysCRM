<template>
  <div class="flex h-full flex-col overflow-hidden bg-[var(--text-n9)]">
    <div class="top-bar">
      <van-button v-if="!props.readonly" plain icon="plus" type="primary" size="small" @click="goCreate"> </van-button>
      <van-search v-model="keyword" shape="round" :placeholder="t('common.pleaseInputKeyword')" class="flex-1 !p-0" />
    </div>
    <div class="flex-1 overflow-hidden">
      <CrmList
        ref="crmListRef"
        :keyword="keyword"
        class="p-[16px]"
        :item-gap="16"
        :load-list-api="loadListApi[props.type]"
      >
        <template #item="{ item }">
          <listItem
            :item="item"
            type="record"
            :readonly="props.readonly"
            @click="goDetail(item)"
            @delete="handleDelete(item)"
            @edit="handleEdit(item)"
          />
        </template>
      </CrmList>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { showSuccessToast } from 'vant';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import listItem from './components/listItem.vue';

  import { getClueFollowRecordList, getCustomerFollowRecordList } from '@/api/modules';

  import { CommonRouteEnum } from '@/enums/routeEnum';

  const props = defineProps<{
    type: FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER | FormDesignKeyEnum.FOLLOW_RECORD_CLUE;
    readonly?: boolean;
  }>();

  const { t } = useI18n();
  const keyword = ref('');
  const router = useRouter();

  const crmListRef = ref<InstanceType<typeof CrmList>>();
  const loadListApi = {
    [FormDesignKeyEnum.FOLLOW_RECORD_CUSTOMER]: getCustomerFollowRecordList,
    [FormDesignKeyEnum.FOLLOW_RECORD_CLUE]: getClueFollowRecordList,
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
