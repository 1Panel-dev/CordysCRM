<template>
  <div class="flex h-full flex-col overflow-hidden bg-[var(--text-n9)]">
    <div class="flex-1 p-[16px]">
      <div v-for="item in relations" :key="item.id" class="flex items-center gap-[8px] bg-[var(--text-n10)] p-[16px]">
        <CrmTag
          v-if="item.relationType === 'GROUP'"
          :tag="t('customer.group')"
          bg-color="var(--primary-7)"
          text-color="var(--primary-8)"
        />
        <CrmTag v-else :tag="t('customer.subsidiary')" bg-color="var(--success-5)" text-color="var(--success-green)" />
        <div class="one-line-text">{{ item.customerName[0].name }}</div>
        <CrmTextButton
          icon="iconicon_delete"
          icon-size="16px"
          color="var(--error-red)"
          @click="() => handleDelete(item.id)"
        />
      </div>
    </div>
    <div class="bg-[var(--text-n10)] p-[16px]">
      <van-button type="primary" class="rounded-[var(--border-radius-small)]" block plain @click="goRelation">
        {{ t('customer.addRelation') }}
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { closeToast, showLoadingToast, showSuccessToast } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmTag from '@/components/pure/crm-tag/index.vue';

  import { getCustomerRelationList } from '@/api/modules';

  import { CustomerRouteEnum } from '@/enums/routeEnum';

  const props = defineProps<{
    sourceId: string;
  }>();

  const { t } = useI18n();
  const router = useRouter();

  const relations = ref<Record<string, any>[]>([]);

  async function initList() {
    try {
      showLoadingToast(t('common.loading'));
      const res = await getCustomerRelationList(props.sourceId);
      relations.value = res.map((item) => ({
        ...item,
        customerId: item.customerId ? [item.customerId] : [],
        customerName: item.customerName
          ? [
              {
                id: item.customerId,
                name: item.customerName,
              },
            ]
          : [],
      }));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      closeToast();
    }
  }

  async function handleDelete(id: string) {
    try {
      showSuccessToast(t('common.deleteSuccess'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function goRelation() {
    router.push({
      name: CustomerRouteEnum.CUSTOMER_RELATION,
      query: {
        sourceId: props.sourceId,
      },
    });
  }

  onBeforeMount(() => {
    initList();
  });
</script>

<style lang="less" scoped></style>
