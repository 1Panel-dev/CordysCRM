<template>
  <div class="h-full overflow-auto" :class="props.class">
    <van-pull-refresh v-model="refreshing" class="h-full" @refresh="handleRefresh">
      <van-empty v-if="list.length === 0 && !loading" :description="t('common.noData')" />
      <van-list
        v-model:loading="loading"
        v-model:error="error"
        :error-text="t('common.listLoadErrorTip')"
        :finished="finished"
        :finished-text="list.length === 0 ? '' : t('common.listFinishedTip')"
        class="flex h-full flex-col overflow-auto"
        :class="`gap-[${itemGap}px]`"
        @load="loadList"
      >
        <template v-for="item in list" :key="item.id">
          <slot name="item" :item="item"></slot>
        </template>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
  import { showSuccessToast } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  const props = defineProps<{
    keyword?: string;
    class?: string;
    listParams?: Record<string, any>;
    itemGap?: number;
    loadListApi: (...args: any) => Promise<Record<string, any>>;
    transform?: (item: Record<string, any>, optionMap?: Record<string, any[]>) => Record<string, any>;
    noPagination?: boolean; // 不分页
  }>();

  const { t } = useI18n();

  const loading = ref(false);
  const error = ref(false);
  const refreshing = ref(false);
  const finished = ref(false);
  const list = defineModel<any[]>({
    default: () => [],
  });
  const currentPage = ref(0);

  async function loadList(refresh = false) {
    try {
      loading.value = true;
      if (refresh) {
        currentPage.value = 1;
        finished.value = false;
      } else {
        currentPage.value += 1;
      }
      const res = await props.loadListApi({
        ...props.listParams,
        keyword: props.keyword,
        pageSize: 10,
        current: currentPage.value,
      });
      let dataList;
      if (props.noPagination) {
        dataList = res;
      } else {
        dataList = props.transform ? res.list.map((e: any) => props.transform!(e, res.optionMap)) : res.list;
      }
      if (refresh) {
        list.value = dataList;
      } else {
        list.value = list.value.concat(dataList);
      }
      finished.value = res.total <= currentPage.value * 10;
      error.value = false;
    } catch (_error) {
      // eslint-disable-next-line no-console
      console.log(_error);
      error.value = true;
    } finally {
      loading.value = false;
      refreshing.value = false;
    }
  }

  function filterListByKeyword(keywordKey: string) {
    if (props.keyword?.length) {
      const lowerCaseVal = props.keyword.toLowerCase();
      list.value = list.value.filter((item) => {
        return item[keywordKey]?.toLowerCase().includes(lowerCaseVal);
      });
    } else {
      loadList(true);
    }
  }

  async function handleRefresh() {
    refreshing.value = true;
    await loadList(true);
    showSuccessToast(t('common.refreshSuccess'));
  }

  defineExpose({
    loadList,
    filterListByKeyword,
  });
</script>

<style lang="less" scoped></style>
