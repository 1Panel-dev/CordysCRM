<template>
  <CrmList
    ref="crmListRef"
    v-model="list"
    :is-return-native-response="props.isReturnNativeResponse"
    :list-params="{
      name: props.keyword,
      ...(props.id ? { id: props.id } : {}),
    }"
    :error-text="t('workbench.duplicateCheck.moduleNotEnabled')"
    :load-list-api="api"
    :item-gap="16"
  >
    <template #item="{ item: listItem }">
      <div class="rounded-[var(--border-radius-small)] !bg-[var(--text-n9)] p-[16px]">
        <div class="mb-[4px] font-semibold">{{ listItem[props.nameKey ?? 'name'] }}</div>
        <CrmDescription :description="getDescriptions(listItem)" class="!m-0 !bg-[var(--text-n9)] !p-0">
          <template #count="{ item }">
            <CrmTextButton
              :color="
                !item.value || !listItem[item.key.includes('clue') ? 'clueModuleEnable' : 'opportunityModuleEnable']
                  ? 'var(--text-n1)'
                  : 'var(--primary-8)'
              "
              :text="String(item.value)"
              @click="toDetail(listItem, item)"
            />
          </template>
          <template #createTime="{ item }">
            {{ dayjs(item.createTime).format('YYYY-MM-DD HH:mm:ss') }}
          </template>
          <template #render="{ item }">
            {{ item.render(listItem) }}
          </template>
        </CrmDescription>
      </div>
    </template>
  </CrmList>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import dayjs from 'dayjs';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { CommonList } from '@lib/shared/models/common';

  import CrmDescription, { CrmDescriptionItem } from '@/components/pure/crm-description/index.vue';
  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';

  import { WorkbenchRouteEnum } from '@/enums/routeEnum';

  const props = defineProps<{
    keyword: string;
    id?: string;
    descriptionList?: CrmDescriptionItem[];
    api: (data: any) => Promise<CommonList<any>>;
    isReturnNativeResponse?: boolean;
    nameKey?: string; // 取数据里的nameKey字段显示在标题上
  }>();

  const { t } = useI18n();
  const router = useRouter();

  const crmListRef = ref<InstanceType<typeof CrmList>>();
  const list = defineModel<any[]>({
    default: () => [],
  });

  const code = computed(() => crmListRef.value?.code);
  async function loadList() {
    await crmListRef.value?.loadList(true);
  }

  function getDescriptions(item: Record<string, any>) {
    return props.descriptionList?.map((column) => ({
      ...column,
      value: item[column.key as string],
    })) as CrmDescriptionItem[];
  }

  function toDetail(listItem: Record<string, any>, item: Record<string, any>) {
    if (!item.value || !listItem[item.key.includes('clue') ? 'clueModuleEnable' : 'opportunityModuleEnable']) return;
    router.push({
      name: WorkbenchRouteEnum.WORKBENCH_DUPLICATE_CHECK_DETAIL,
      query: {
        keyword: listItem.name,
        id: listItem.id,
        detailType: item.key.includes('clue') ? 'clue' : 'opportunity',
        customerName: listItem.name,
      },
    });
  }

  defineExpose({
    loadList,
    code,
  });
</script>

<style lang="less" scoped>
  :deep(.crm-description-item) {
    .crm-description-label {
      font-size: 12px;
      color: var(--text-n4);
    }
    .crm-description-value {
      font-size: 12px;
    }
  }
</style>
