<template>
  <CrmPageWrapper :title="t('workbench.duplicateCheck')">
    <van-search v-model="keyword" shape="round" :placeholder="t('workbench.searchPlaceholder')" @search="refresh" />
    <div
      v-show="keyword.length && !loading && !displayConfigList.length"
      class="mt-[80px] text-center text-[var(--text-n4)]"
    >
      {{ t('workbench.duplicateCheck.noSearchData') }}
    </div>

    <van-collapse v-if="keyword.length" v-model="activeNames" :border="false">
      <van-collapse-item v-for="item of displayConfigList" :key="item.value" :name="item.value" :border="false">
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
            <span class="text-[var(--text-n4)]"> ({{ moduleCount?.[item.value] }}) </span>
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
    loading,
    formModel,
    moduleCount,
    getCountList,
    initSearchDetail,
    initSearchListConfig,
  } = useSearchFormConfig();

  defineOptions({
    name: WorkbenchRouteEnum.WORKBENCH_DUPLICATE_CHECK,
  });

  const { t } = useI18n();

  const keyword = ref('');
  const activeNames = ref<FormDesignKeyEnum[]>([]);

  const displayConfigList = computed(() => {
    if (!formModel.value.resultDisplay) {
      return configList.value;
    }
    return configList.value.filter((item) => moduleCount.value?.[item.value]);
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
      Object.values(relatedListRefs.value).forEach((comp) => {
        comp?.loadList?.();
      });
    });
  }, 300);

  async function refresh(val: string) {
    await getCountList(val);
    searchData();
  }

  watch(
    () => keyword.value,
    (val: string) => {
      if (val) {
        refresh(val);
      }
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
