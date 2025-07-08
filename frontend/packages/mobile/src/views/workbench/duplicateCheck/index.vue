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

    <van-collapse v-model="activeNames" :border="false">
      <van-collapse-item v-show="showResult" name="customer" :border="false">
        <template #title>
          <div class="text-[18px] font-semibold">
            {{
              validatePhone(keyword)
                ? t('workbench.duplicateCheck.contactResult')
                : t('workbench.duplicateCheck.result')
            }}
          </div>
        </template>
        <template #right-icon>
          <CrmIcon
            :name="activeNames.includes('customer') ? 'iconicon_chevron_up' : 'iconicon_chevron_right'"
            width="24px"
            height="24px"
          />
        </template>
        <RelatedList
          ref="customerRelatedListRef"
          v-model="customerList"
          :keyword="keyword"
          :description-list="validatePhone(keyword) ? contactDescriptionList : customerDescriptionList"
          :api="validatePhone(keyword) ? getRepeatContactList : GetRepeatCustomerList"
          is-return-native-response
        />
      </van-collapse-item>
      <van-collapse-item v-show="showClue" name="clue" :border="false">
        <template #title>
          <div class="text-[18px] font-semibold">{{ t('workbench.duplicateCheck.relatedClues') }}</div>
        </template>
        <template #right-icon>
          <CrmIcon
            :name="activeNames.includes('clue') ? 'iconicon_chevron_up' : 'iconicon_chevron_right'"
            width="24px"
            height="24px"
          />
        </template>
        <RelatedList
          ref="clueRelatedListRef"
          v-model="clueList"
          :keyword="keyword"
          :description-list="clueDescriptionList"
          :api="GetRepeatClueList"
          is-return-native-response
        />
      </van-collapse-item>
    </van-collapse>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { validatePhone } from '@lib/shared/method/validate';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import RelatedList from './components/relatedList.vue';

  import { GetRepeatClueList, getRepeatContactList, GetRepeatCustomerList } from '@/api/modules';
  import { clueDescriptionList, contactDescriptionList, customerDescriptionList } from '@/config/workbench';

  import { WorkbenchRouteEnum } from '@/enums/routeEnum';

  defineOptions({
    name: WorkbenchRouteEnum.WORKBENCH_DUPLICATE_CHECK,
  });

  const { t } = useI18n();

  const keyword = ref('');
  const activeNames = ref(['customer', 'clue']);

  const noDuplicateCustomers = ref(false);
  const showResult = ref(false);
  const showClue = ref(false);

  const customerList = ref([]);
  const clueList = ref([]);

  onBeforeMount(() => {
    keyword.value = '';
    showResult.value = false;
    showClue.value = false;
    noDuplicateCustomers.value = false;
  });

  const customerRelatedListRef = ref<InstanceType<typeof RelatedList>>();
  const clueRelatedListRef = ref<InstanceType<typeof RelatedList>>();

  async function searchData() {
    nextTick(() => {
      customerRelatedListRef.value?.loadList().finally(() => {
        showResult.value = !!customerList.value.length || customerRelatedListRef.value?.code === 101003;
        noDuplicateCustomers.value = !showResult.value && !showClue.value;
      });
      clueRelatedListRef.value?.loadList().finally(() => {
        showClue.value = !!clueList.value.length || clueRelatedListRef.value?.code === 101003;
        noDuplicateCustomers.value = !showResult.value && !showClue.value;
      });
    });
  }
</script>

<style lang="less" scoped>
  :deep(.van-collapse) {
    .van-cell--clickable:active {
      background: transparent;
    }
  }
</style>
