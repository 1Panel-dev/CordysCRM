<template>
  <div class="flex h-full flex-col overflow-hidden bg-[var(--text-n9)]">
    <div class="top-bar">
      <van-button v-if="!props.readonly" plain icon="plus" type="primary" size="small" @click="goCreate"> </van-button>
      <van-search v-model="keyword" shape="round" :placeholder="t('common.pleaseInputKeyword')" class="flex-1 !p-0" />
    </div>
    <CrmList
      ref="crmListRef"
      :keyword="keyword"
      class="p-[16px]"
      :item-gap="16"
      :list-params="{
        sourceId: props.sourceId,
      }"
      :load-list-api="followRecordApiMap.list[props.type]"
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
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { showSuccessToast } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { FollowDetailItem } from '@lib/shared/models/customer';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import listItem from './components/listItem.vue';

  import { followRecordApiMap, RecordEnumType } from '@/config/follow';

  import { CommonRouteEnum } from '@/enums/routeEnum';

  const props = defineProps<{
    type: RecordEnumType;
    readonly?: boolean;
    sourceId: string;
    initialSourceName?: string;
  }>();

  const { t } = useI18n();
  const keyword = ref('');
  const router = useRouter();

  const crmListRef = ref<InstanceType<typeof CrmList>>();

  async function handleDelete(item: FollowDetailItem) {
    try {
      await followRecordApiMap.delete?.[props.type]?.(item.id);
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
        id: props.sourceId,
        initialSourceName: props.initialSourceName,
      },
    });
  }

  function handleEdit(item: FollowDetailItem) {
    router.push({
      name: CommonRouteEnum.FORM_CREATE,
      query: {
        formKey: props.type,
        id: item.id,
        needInitDetail: 'Y',
      },
    });
  }
  function goDetail(item: FollowDetailItem) {
    router.push({
      name: CommonRouteEnum.FOLLOW_DETAIL,
      query: {
        formKey: props.type,
        id: item.id,
        needInitDetail: 'Y',
      },
    });
  }

  defineExpose({
    loadList: () => crmListRef.value?.loadList(true),
  });
</script>

<style lang="less" scoped></style>
