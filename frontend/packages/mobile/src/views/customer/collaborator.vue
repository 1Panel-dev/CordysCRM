<template>
  <CrmPageWrapper :title="route.query.id ? t('customer.updateCollaborator') : t('customer.addCollaborator')">
    <van-form ref="formRef" class="crm-form" required>
      <van-cell-group inset>
        <van-field
          v-model="form.permission"
          name="permission"
          :label="t('customer.collaborator')"
          :placeholder="t('common.pleaseInput')"
          :rules="[{ required: true, message: t('customer.customerNameNotNull') }]"
          class="!text-[16px]"
        >
          <template #input>
            <van-radio-group v-model="form.permission" direction="horizontal">
              <van-radio name="READ_ONLY">{{ t('customer.readOnly') }}</van-radio>
              <van-radio name="COLLABORATION">{{ t('customer.cooperation') }}</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <CrmDataSource
          id="collaborator"
          v-model:value="form.member"
          v-model:selected-rows="selectedRows"
          :data-source-type="FieldDataSourceTypeEnum.USER_OPTIONS"
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
          v-permission="['CUSTOMER_MANAGEMENT:UPDATE']"
          type="primary"
          class="!rounded-[var(--border-radius-small)] !text-[16px]"
          :loading="loading"
          :disabled="form.member.length === 0"
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
  import { CollaborationType } from '@lib/shared/models/customer';

  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmDataSource from '@/components/business/crm-datasource/index.vue';

  import { addCustomerCollaboration, updateCustomerCollaboration } from '@/api/modules';

  const route = useRoute();
  const router = useRouter();
  const { t } = useI18n();

  const formRef = ref<FormInstance>();
  const lastPageParams = window.history.state.params ? JSON.parse(window.history.state.params) : null; // 获取上个页面带过来的参数
  const form = ref<{
    id: string;
    member: string;
    permission: CollaborationType;
  }>({
    id: route.query.id as string,
    member: lastPageParams.collaborator?.id || '',
    permission: 'READ_ONLY',
  });
  const selectedRows = ref(lastPageParams.collaborator ? [lastPageParams.collaborator] : []);
  const loading = ref(false);

  function disabledSelection(row: Record<string, any>) {
    return lastPageParams.userIds?.includes(row.id);
  }

  async function save() {
    try {
      await formRef.value?.validate();
      loading.value = true;
      if (route.query.id) {
        await updateCustomerCollaboration({
          id: form.value.id,
          collaborationType: form.value.permission,
        });
        showSuccessToast(t('common.updateSuccess'));
      } else {
        await addCustomerCollaboration({
          customerId: route.query.sourceId as string,
          userId: form.value.member,
          collaborationType: form.value.permission,
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
