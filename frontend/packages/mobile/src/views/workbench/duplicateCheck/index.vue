<template>
  <CrmPageWrapper :title="t('workbench.duplicateCheck')">
    <van-search
      v-model="keyword"
      shape="round"
      :placeholder="t('workbench.searchPlaceholder')"
      @clear="searchData"
      @search="searchData"
    />
    <div v-show="noDuplicateCustomers" class="text-center text-[var(--text-n4)]">
      {{ t('workbench.duplicateCheck.noDuplicateCustomers') }}
    </div>

    <van-collapse v-if="keyword.length" v-model="activeNames" :border="false">
      <van-collapse-item v-for="item of configList" :key="item.value" :name="item.value" :border="false">
        <template #icon>
          <CrmIcon
            :name="activeNames.includes(item.value) ? 'iconicon_chevron_up' : 'iconicon_chevron_right'"
            width="24px"
            height="24px"
          />
        </template>
        <template #title>
          <div class="ml-[4px] text-[18px] font-semibold">
            {{
              t('workbench.duplicateCheck.searchTypeTitle', {
                title: item.label,
              })
            }}
          </div>
        </template>
        <template #right-icon> </template>
        <div style="overflow: hidden; height: calc(100vh - 224px)">
          <RelatedList
            :ref="(el) => setRef(el, item.value)"
            v-model="searchResultMap[item.value].list"
            :keyword="keyword"
            :description-list="searchResultMap[item.value as SearchTableKey]?.describe??[]"
            :api="getSearchListApiMap[item.value as SearchTableKey]"
          />
        </div>
      </van-collapse-item>
    </van-collapse>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { debounce } from 'lodash-es';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import RelatedList from './components/relatedList.vue';

  import { WorkbenchRouteEnum } from '@/enums/routeEnum';

  import { SearchTableKey } from './config';
  import useSearchFormConfig from './useSearchFormConfig';

  const {
    initSearchFormConfig,
    searchResultMap,
    getSearchListApiMap,
    configList,
    initSearchDetail,
    initSearchListConfig,
  } = useSearchFormConfig();

  defineOptions({
    name: WorkbenchRouteEnum.WORKBENCH_DUPLICATE_CHECK,
  });

  const { t } = useI18n();

  const keyword = ref('');
  const activeNames = ref<FormDesignKeyEnum[]>([]);

  const noDuplicateCustomers = ref(false);
  const showResult = ref(false);
  const showClue = ref(false);

  onBeforeMount(() => {
    keyword.value = '';
    showResult.value = false;
    showClue.value = false;
    noDuplicateCustomers.value = false;
  });

  const relatedListRefs = ref<Record<string, any>>({});
  function setRef(el: any, key: string) {
    if (el) {
      relatedListRefs.value[key] = el;
    } else {
      delete relatedListRefs.value[key]; // 组件销毁时清除
    }
  }

  const searchData = debounce(() => {
    nextTick(() => {
      Object.values(relatedListRefs).forEach((comp) => {
        comp?.loadList?.();
      });
    });
  }, 300);

  watch(
    () => keyword.value,
    () => {
      searchData();
    }
  );

  onMounted(async () => {
    await initSearchFormConfig();
    await initSearchDetail();
    initSearchListConfig();
  });
</script>

<style lang="less" scoped>
  :deep(.van-collapse) {
    .van-cell--clickable:active {
      background: transparent;
    }
  }
</style>
