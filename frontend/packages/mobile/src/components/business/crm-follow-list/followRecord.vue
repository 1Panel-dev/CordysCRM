<template>
  <div class="flex h-full flex-col overflow-hidden bg-[var(--text-n9)]">
    <div class="top-bar">
      <van-button plain icon="plus" type="primary" size="small" @click="goCreate"> </van-button>
      <van-search v-model="keyword" shape="round" :placeholder="t('common.pleaseInputKeyword')" class="flex-1 !p-0" />
    </div>
    <div class="flex-1 overflow-hidden">
      <CrmList :keyword="keyword" class="p-[16px]" :item-gap="16">
        <template #item="{ item }">
          <listItem :item="item" type="record" @click="goDetail(item)" />
        </template>
      </CrmList>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmList from '@/components/pure/crm-list/index.vue';
  import listItem from './components/listItem.vue';

  import { CommonRouteEnum } from '@/enums/routeEnum';

  const props = defineProps<{
    type: FormDesignKeyEnum;
  }>();

  const { t } = useI18n();
  const keyword = ref('');
  const router = useRouter();

  function goCreate() {
    router.push({
      name: CommonRouteEnum.FORM_CREATE,
      query: {
        formKey: props.type,
      },
    });
  }

  function goDetail(item: any) {
    router.push({
      name: CommonRouteEnum.FOLLOW_DETAIL,
      query: {
        id: item.id,
        name: item.name,
        type: 'record',
      },
    });
  }
</script>

<style lang="less" scoped></style>
