<template>
  <div class="flex h-full flex-col overflow-hidden">
    <div class="top-bar">
      <van-button plain icon="plus" type="primary" size="small" class="!rounded-[var(--border-radius-medium)]">
      </van-button>
      <van-search v-model="keyword" shape="round" :placeholder="t('customer.searchPlaceholder')" class="flex-1 !p-0" />
    </div>
    <div class="flex-1 overflow-hidden">
      <CrmList :list-params="listParams" class="p-[16px]" :item-gap="16">
        <template #item="{ item }">
          <div
            class="flex w-full items-center gap-[16px] rounded-[var(--border-radius-small)] bg-[var(--text-n10)] p-[16px]"
          >
            <van-image round width="40px" height="40px" src="https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg" />
            <div class="flex flex-1 flex-col gap-[2px]">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-[8px]">
                  <div class="text-[16px] text-[var(--text-n1)]">{{ item.name }}</div>
                  <van-tag
                    color="var(--success-5)"
                    text-color="var(--success-green)"
                    class="rounded-[var(--border-radius-small)] !p-[2px_6px]"
                  >
                    {{ t('common.normal') }}
                  </van-tag>
                </div>
                <CrmTextButton icon="iconicon_delete" icon-size="16px" color="var(--error-red)" />
              </div>
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-[4px]">
                  <CrmIcon name="iconicon_mobile" width="12px" height="12px" color="var(--text-n2)" />
                  <div class="text-[12px] text-[var(--text-n2)]">
                    {{ item.phone?.replace(/(\d{3})(\d{4})(\d{4})/, '$1 $2 $3') }}
                  </div>
                  <CrmIcon name="iconicon_file_copy" width="12px" height="12px" color="var(--primary-8)" />
                </div>
                <div class="flex items-center gap-[4px]">
                  <CrmIcon name="iconicon_mail1" width="12px" height="12px" color="var(--text-n2)" />
                  <div class="text-[12px] text-[var(--text-n2)]">{{ item.email }}</div>
                </div>
              </div>
            </div>
          </div>
        </template>
      </CrmList>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';

  import { useI18n } from '@cordys/web/src/hooks/useI18n';

  const { t } = useI18n();
  const keyword = ref('');
  const activeFilter = ref(CustomerSearchTypeEnum.ALL);
  const listParams = computed(() => {
    return {
      searchType: activeFilter.value,
      keyword: keyword.value,
    };
  });
</script>

<style lang="less" scoped>
  .top-bar {
    @apply flex items-center;

    gap: 12px;
    padding: 8px 16px;
    background-color: var(--text-n10);
    .half-px-border-bottom();
  }
</style>
