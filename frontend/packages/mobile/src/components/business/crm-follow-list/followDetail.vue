<template>
  <CrmPageWrapper :title="route.query.name?.toString() || ''">
    <div class="h-full bg-[var(--text-n9)] p-[16px]">
      <CrmDescription :description="description" />
    </div>
    <template #footer>
      <div class="flex items-center gap-[16px]">
        <div class="flex w-[100px] items-center">
          <CrmTextButton
            v-if="route.query.formKey?.includes('plan')"
            color="var(--text-n1)"
            icon="iconicon_minus_circle1"
            :text="t('common.cancelPlan')"
            icon-size="18px"
            direction="column"
            class="flex-1"
            @click="handleCancel"
          />
          <CrmTextButton
            color="var(--text-n1)"
            icon="iconicon_delete"
            :text="t('common.delete')"
            icon-size="18px"
            direction="column"
            class="flex-1"
            @click="handleDelete"
          />
        </div>
        <van-button
          type="primary"
          class="flex-1 !rounded-[var(--border-radius-small)] !text-[16px]"
          plain
          @click="handleEdit"
        >
          {{ t('common.edit') }}
        </van-button>
      </div>
    </template>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute, useRouter } from 'vue-router';
  import { showSuccessToast } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmDescription, { CrmDescriptionItem } from '@/components/pure/crm-description/index.vue';
  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';

  import { CommonRouteEnum } from '@/enums/routeEnum';

  const route = useRoute();
  const router = useRouter();
  const { t } = useI18n();

  const description: CrmDescriptionItem[] = [
    {
      label: '基本信息',
      isTitle: true,
    },
    {
      label: t('customer.customerName'),
      value: '张三',
    },
    {
      label: t('customer.customerType'),
      value: 'VIP客户',
    },
    {
      label: t('customer.customerLevel'),
      value: 'VIP客户',
    },
    {
      label: t('customer.customerSource'),
      value: '市场活动',
    },
    {
      label: t('customer.customerStatus'),
      value: '潜在客户',
    },
  ];

  async function handleDelete() {
    try {
      // TODO: delete customer
      showSuccessToast(t('common.deleteSuccess'));
      router.back();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function handleCancel() {
    try {
      // TODO: delete customer
      showSuccessToast(t('common.cancelSuccess'));
      router.back();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function handleEdit() {
    router.push({
      name: CommonRouteEnum.FORM_CREATE,
      query: {
        formKey: route.query.formKey,
        id: route.query.id,
        needInitDetail: 'Y',
      },
    });
  }
</script>

<style lang="less" scoped>
  :deep(.crm-page-content) {
    @apply !overflow-hidden;
  }
</style>
