<template>
  <CrmPageWrapper :title="route.query.id ? t('common.editCustomer') : t('common.newCustomer')">
    <van-form ref="formRef" class="border-b border-[var(--text-n8)]" required>
      <van-cell-group inset>
        <van-field
          v-model="name"
          name="name"
          :label="t('customer.customerName')"
          :placeholder="t('common.pleaseInput')"
          :rules="[{ required: true, message: t('customer.customerNameNotNull') }]"
          class="!text-[16px]"
        />
      </van-cell-group>
    </van-form>
    <template #footer>
      <div class="flex items-center gap-[16px]">
        <van-button
          type="default"
          class="crm-button-primary--secondary !rounded-[var(--border-radius-small)] !text-[16px]"
          block
          :disabled="loading"
          @click="router.back"
        >
          {{ t('common.cancel') }}
        </van-button>
        <van-button
          type="primary"
          class="!rounded-[var(--border-radius-small)] !text-[16px]"
          :loading="loading"
          :disabled="!name"
          block
          @click="save"
        >
          {{ route.query.id ? t('common.update') : t('common.create') }}
        </van-button>
      </div>
    </template>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute, useRouter } from 'vue-router';
  import { FormInstance, showToast } from 'vant';

  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';

  import { useI18n } from '@cordys/web/src/hooks/useI18n';

  const route = useRoute();
  const router = useRouter();
  const { t } = useI18n();

  const formRef = ref<FormInstance>();
  const name = ref('');
  const loading = ref(false);

  async function save() {
    try {
      await formRef.value?.validate();
      if (route.query.id) {
        // update
        showToast(t('common.updateSuccess'));
      } else {
        // create
        showToast(t('common.createSuccess'));
      }
      router.back();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
</script>

<style lang="less" scoped></style>
