<template>
  <CrmPageWrapper :title="route.query.id ? t('customer.updateRelation') : t('customer.addRelation')">
    <van-form ref="formRef" class="crm-form" required>
      <van-cell-group inset>
        <van-field
          v-model="relationType"
          name="relationType"
          :label="t('customer.relation')"
          :placeholder="t('common.pleaseInput')"
          :rules="[{ required: true, message: t('customer.customerNameNotNull') }]"
          class="!text-[16px]"
        >
          <template #input>
            <van-radio-group v-model="relationType" direction="horizontal">
              <van-radio name="GROUP">{{ t('customer.group') }}</van-radio>
              <van-radio name="SUBSIDIARY">{{ t('customer.subsidiary') }}</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <CrmDataSource
          id="customerId"
          v-model:value="customerId"
          v-model:selected-rows="selectedRows"
          :data-source-type="FieldDataSourceTypeEnum.CUSTOMER_OPTIONS"
          :label="t('common.pleaseSelect')"
          :multiple="false"
          :disabled-selection="disabledSelection"
          no-page-nation
          class="!text-[16px]"
        >
        </CrmDataSource>
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
          :disabled="!customerId"
          block
          @click="save"
        >
          {{ route.query.id ? t('common.update') : t('common.add') }}
        </van-button>
      </div>
    </template>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute, useRouter } from 'vue-router';
  import { FormInstance, showSuccessToast } from 'vant';

  import { FieldDataSourceTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmDataSource from '@/components/business/crm-datasource/index.vue';

  const route = useRoute();
  const router = useRouter();
  const { t } = useI18n();

  const formRef = ref<FormInstance>();
  const relationType = ref('GROUP');
  const customerId = ref('');
  const selectedRows = ref([]);
  const loading = ref(false);

  function disabledSelection(row: Record<string, any>) {
    return row.id === route.query.sourceId;
  }

  async function save() {
    try {
      await formRef.value?.validate();
      if (route.query.id) {
        // update
        showSuccessToast(t('common.updateSuccess'));
      } else {
        // create
        showSuccessToast(t('common.addSuccess'));
      }
      router.back();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }
</script>

<style lang="less" scoped></style>
