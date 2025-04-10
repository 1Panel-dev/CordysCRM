<template>
  <CrmPageWrapper :title="route.query.name?.toString() || ''">
    <div class="relative h-full bg-[var(--text-n9)] pt-[16px]">
      <CrmDescription :description="description" />
    </div>
    <template #footer>
      <van-button
        type="danger"
        class="!rounded-[var(--border-radius-small)] !text-[16px]"
        block
        plain
        :loading="loading"
        @click="handleDelete"
      >
        {{ t('common.delete') }}
      </van-button>
    </template>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute, useRouter } from 'vue-router';
  import { showConfirmDialog, showSuccessToast } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmDescription, { CrmDescriptionItem } from '@/components/pure/crm-description/index.vue';
  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';

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
  const loading = ref(false);

  function handleDelete() {
    showConfirmDialog({
      title: t('contact.deleteTitle'),
      message: t('contact.deleteTip'),
      confirmButtonText: t('common.confirmDelete'),
      confirmButtonColor: 'var(--error-red)',
      beforeClose: (action) => {
        if (action === 'confirm') {
          try {
            // TODO: delete customer
            showSuccessToast(t('common.deleteSuccess'));
            router.back();
            return Promise.resolve(true);
          } catch (error) {
            // eslint-disable-next-line no-console
            console.log(error);
            return Promise.resolve(false);
          }
        } else {
          return Promise.resolve(true);
        }
      },
    });
  }
</script>

<style lang="less" scoped></style>
