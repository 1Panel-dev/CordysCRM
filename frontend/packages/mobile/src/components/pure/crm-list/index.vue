<template>
  <div class="h-full overflow-auto" :class="props.class">
    <van-pull-refresh v-model="refreshing" @refresh="handleRefresh">
      <van-empty v-if="list.length === 0 && !loading && !error" :description="t('common.noData')" />
      <van-list
        v-model:loading="loading"
        v-model:error="error"
        :error-text="props.errorText ?? t('common.listLoadErrorTip')"
        :finished="finished"
        :finished-text="list.length === 0 ? '' : t('common.listFinishedTip')"
        class="flex flex-col"
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
  import { closeToast, showLoadingToast, showSuccessToast } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { CommonList } from '@lib/shared/models/common';

  const props = defineProps<{
    keyword?: string;
    class?: string;
    listParams?: Record<string, any>;
    itemGap?: number;
    noPageNation?: boolean;
    errorText?: string;
    isReturnNativeResponse?: boolean;
    loadListApi?: (...args: any) => Promise<CommonList<Record<string, any>> | Record<string, any>>;
    transform?: (item: any, optionMap?: Record<string, any[]>) => Record<string, any>;
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

  const code = ref(0); // 状态码

  async function loadList(refresh = false) {
    try {
      if (!props.loadListApi) {
        list.value = props.transform ? list.value.map((e: any) => props.transform!(e)) : list.value;
        finished.value = true;
        return;
      }

      loading.value = true;
      if (refresh) {
        currentPage.value = 1;
        finished.value = false;
      } else {
        currentPage.value += 1;
      }
      if (currentPage.value === 1 && !refreshing.value) {
        showLoadingToast(t('common.loading'));
      }
      const res = await props.loadListApi({
        keyword: props.keyword,
        ...props.listParams,
        pageSize: 10,
        current: currentPage.value,
      });
      if (props?.isReturnNativeResponse) {
        code.value = res.data.code;
      }
      const data = props?.isReturnNativeResponse ? res.data.data : res;
      const resList = props.noPageNation && !data.list ? data : data.list;
      const dataList = props.transform
        ? resList.map((e: Record<string, any>) => props.transform!(e, data.optionMap))
        : resList;
      if (refresh) {
        list.value = dataList;
      } else {
        list.value = list.value.concat(dataList);
      }
      finished.value = props.noPageNation || data.total <= currentPage.value * 10;
      error.value = false;
    } catch (_error: any) {
      // eslint-disable-next-line no-console
      console.log(_error);
      error.value = true;
      if (props?.isReturnNativeResponse) {
        code.value = _error.code;
      }
    } finally {
      loading.value = false;
      refreshing.value = false;
      closeToast();
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
    code,
  });
</script>

<style lang="less" scoped></style>
