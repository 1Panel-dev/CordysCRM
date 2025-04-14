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
            <van-radio-group v-model="relationType" direction="horizontal" :disabled="hasGroup === 'Y'">
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
          :label="t('customer.selectCustomer')"
          :multiple="false"
          :disabled-selection="disabledSelection"
          :disabled="!!route.query.id"
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
          :disabled="customerId.length === 0"
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
  import { sleep } from '@lib/shared/method';

  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmDataSource from '@/components/business/crm-datasource/index.vue';

  import { addCustomerRelationItem, updateCustomerRelationItem } from '@/api/modules';

  const route = useRoute();
  const router = useRouter();
  const { t } = useI18n();

  const lastPageParams = window.history.state.params ? JSON.parse(window.history.state.params) : null; // 获取上个页面带过来的参数
  const { relation, hasGroup } = lastPageParams || {};

  const formRef = ref<FormInstance>();
  const relationType = ref(relation?.relationType || (hasGroup === 'Y' ? 'SUBSIDIARY' : 'GROUP'));
  const customerId = ref(relation?.customerId || []);
  const selectedRows = ref(relation ? relation.customerName : []);
  const loading = ref(false);

  function disabledSelection(row: Record<string, any>) {
    return row.id === route.query.sourceId;
  }

  async function save() {
    try {
      await formRef.value?.validate();
      loading.value = true;
      if (route.query.id) {
        await updateCustomerRelationItem(route.query.sourceId as string, {
          id: route.query.id as string,
          customerId: customerId.value[0],
          relationType: relationType.value,
        });
        showSuccessToast(t('common.updateSuccess'));
      } else {
        await addCustomerRelationItem(route.query.sourceId as string, {
          customerId: customerId.value[0],
          relationType: relationType.value,
        });
        showSuccessToast(t('common.addSuccess'));
      }
      await sleep(300);
      router.back();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }
</script>

<style lang="less" scoped></style>
