<template>
  <CrmPageWrapper :title="route.query.id ? t('customer.updateCollaborator') : t('customer.addCollaborator')">
    <van-form ref="formRef" class="crm-form">
      <van-cell-group inset>
        <CrmDataSource
          id="collaborator"
          v-model:value="form.member"
          v-model:selected-rows="selectedRows"
          :data-source-type="FieldDataSourceTypeEnum.USER_OPTIONS"
          :label="t('customer.member')"
          :multiple="false"
          required
          :rules="[{ required: true, message: t('common.notNull', { value: t('customer.member') }) }]"
          :disabled-selection="disabledSelection"
          :disabled="!!route.query.id"
          no-page-nation
          class="!text-[16px]"
        >
        </CrmDataSource>
      </van-cell-group>

      <section class="p-[16px]">
        <div class="mb-[8px] text-[16px] leading-[24px] text-[var(--text-n1)]">
          {{ t('customer.permission') }}
        </div>
        <van-radio-group v-model="form.permission" class="flex flex-col gap-[12px]">
          <div
            v-for="option of collaborationOptions"
            :key="option.value"
            class="collaboration-permission-card"
            :class="{ 'collaboration-permission-card--active': form.permission === option.value }"
            @click="form.permission = option.value"
          >
            <van-radio :name="option.value" />
            <div class="min-w-0 flex-1">
              <div class="text-[16px] leading-[24px] text-[var(--text-n1)]">{{ option.label }}</div>
              <div class="mt-[4px] text-[14px] leading-[22px] text-[var(--text-n4)]">{{ option.description }}</div>
            </div>
          </div>
        </van-radio-group>
      </section>

      <van-cell-group inset>
        <van-field
          :model-value="expireTimeLabel"
          :placeholder="t('customer.longTerm')"
          readonly
          is-link
          :label="t('customer.validity')"
          @click="openExpireTimePicker"
        />
        <div class="validity-tip flex items-center gap-[8px] px-[16px] py-[4px] text-[12px] text-[var(--text-n4)]">
          <van-icon name="info-o" />
          <span>{{ t('customer.validityTip') }}</span>
        </div>
      </van-cell-group>
    </van-form>
    <van-popup v-model:show="showExpireTimePicker" destroy-on-close round position="bottom">
      <van-button v-if="form.expireTime" plain size="small" class="expire-time-picker__clear" @click="clearExpireTime">
        {{ t('common.clear') }}</van-button
      >
      <van-picker-group
        :title="t('customer.validity')"
        :tabs="[t('formCreate.pickDate'), t('formCreate.pickTime')]"
        :next-step-text="t('formCreate.next')"
        @confirm="confirmExpireTime"
        @cancel="showExpireTimePicker = false"
      >
        <van-date-picker v-model="currentDate" />
        <van-time-picker v-model="currentTime" :columns-type="['hour', 'minute', 'second']" />
      </van-picker-group>
    </van-popup>
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
  import dayjs from 'dayjs';

  import { FieldDataSourceTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { sleep } from '@lib/shared/method';
  import type { CollaborationType } from '@lib/shared/models/customer';

  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmDataSource from '@/components/business/crm-datasource/index.vue';

  import { addCustomerCollaboration, updateCustomerCollaboration } from '@/api/modules';
  import { refreshCustomerCollaborators } from '@/hooks/useCustomerCollaborators';

  const route = useRoute();
  const router = useRouter();
  const { t } = useI18n();

  const formRef = ref<FormInstance>();
  const lastPageParams = window.history.state.params ? JSON.parse(window.history.state.params) : null; // 获取上个页面带过来的参数
  const form = ref<{
    id: string;
    member: string;
    permission: CollaborationType;
    expireTime?: number | null;
  }>({
    id: route.query.id as string,
    member: lastPageParams.collaborator?.id || '',
    permission: lastPageParams.collaborationType || 'READ_ONLY',
    expireTime: lastPageParams.expireTime || null,
  });
  const selectedRows = ref(lastPageParams.collaborator ? [lastPageParams.collaborator] : []);
  const loading = ref(false);
  const showExpireTimePicker = ref(false);
  const currentDate = ref<string[]>([]);
  const currentTime = ref<string[]>([]);
  const expireTimeLabel = computed(() =>
    form.value.expireTime ? dayjs(form.value.expireTime).format('YYYY-MM-DD HH:mm:ss') : ''
  );
  const collaborationOptions = computed(() => [
    {
      value: 'COLLABORATION' as const,
      label: t('customer.customerInfoOnly'),
      description: t('customer.collaborationDescription.infoOnly'),
    },
    {
      value: 'READ_ONLY' as const,
      label: t('customer.readOnly'),
      description: t('customer.collaborationDescription.readOnly'),
    },
    {
      value: 'JOINT_FOLLOW' as const,
      label: t('customer.jointFollow'),
      description: t('customer.collaborationDescription.jointFollow'),
    },
  ]);

  function disabledSelection(row: Record<string, any>) {
    return lastPageParams.userIds?.includes(row.id);
  }

  function openExpireTimePicker() {
    const date = form.value.expireTime ? dayjs(form.value.expireTime) : dayjs();
    currentDate.value = date.format('YYYY-MM-DD').split('-');
    currentTime.value = date.format('HH:mm:ss').split(':');
    showExpireTimePicker.value = true;
  }

  function confirmExpireTime() {
    form.value.expireTime = dayjs(`${currentDate.value.join('-')} ${currentTime.value.join(':')}`).valueOf();
    showExpireTimePicker.value = false;
  }

  function clearExpireTime() {
    form.value.expireTime = null;
    showExpireTimePicker.value = false;
  }

  async function save() {
    try {
      await formRef.value?.validate();
      loading.value = true;
      if (route.query.id) {
        await updateCustomerCollaboration({
          id: form.value.id,
          collaborationType: form.value.permission,
          expireTime: form.value.expireTime || null,
        });
        showSuccessToast(t('common.updateSuccess'));
      } else {
        await addCustomerCollaboration({
          customerId: route.query.sourceId as string,
          userId: form.value.member,
          collaborationType: form.value.permission,
          expireTime: form.value.expireTime || null,
        });
        showSuccessToast(t('common.addSuccess'));
      }
      await sleep(300);
      await refreshCustomerCollaborators(route.query.sourceId?.toString() || '');
      router.back();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }
</script>

<style lang="less" scoped>
  .collaboration-permission-card {
    display: flex;
    gap: 12px;
    align-items: flex-start;
    padding: 14px 12px;
    border: 1px solid var(--van-border-color);
    border-radius: var(--border-radius-small);
    background-color: var(--text-n10);
    &--active {
      border-color: var(--primary-1);
      background-color: var(--primary-7);
    }
    :deep(.van-radio) {
      margin-top: 2px;
    }
  }
  .expire-time-picker__clear {
    position: absolute;
    top: 6px;
    left: 64px;
    z-index: 1;
    padding: 0 8px;
    font-size: 14px;
    border: 0;
    color: var(--text-n4);
  }
</style>
