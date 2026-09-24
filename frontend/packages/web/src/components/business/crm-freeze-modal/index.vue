<template>
  <CrmModal
    v-model:show="show"
    :ok-loading="loading"
    :positive-text="props.type === 'freeze' ? t('common.freeze') : t('common.unfreeze')"
    @confirm="confirmHandler"
  >
    <template #title>
      <div class="crm-modal-title one-line-text flex items-center">
        {{ props.type === 'freeze' ? t('common.freeze') : t('common.unfreeze') }}
        <n-tooltip trigger="hover">
          <template #trigger>
            <div class="one-line-text text-[var(--text-n4)]">({{ props.resourceName }})</div>
          </template>
          {{ props.resourceName }}
        </n-tooltip>
      </div>
    </template>
    <template v-if="props.type === 'freeze'">
      <n-alert type="warning">
        {{ alertTip }}
      </n-alert>
      <n-form ref="formRef" :model="form" label-placement="top" require-mark-placement="left">
        <n-form-item label="" path="freezeType">
          <n-tabs v-model:value="form.freezeType" type="segment" class="no-content" animated>
            <n-tab-pane name="custom" :tab="t('common.customTime')"> </n-tab-pane>
            <n-tab-pane name="freezeForever" :tab="t('common.freezeForever')"> </n-tab-pane>
          </n-tabs>
        </n-form-item>
        <n-form-item
          v-if="form.freezeType === 'custom'"
          :label="t('common.freezeTime')"
          path="time"
          :rule="[
            {
              required: true,
              message: t('common.notNull', { value: t('common.freezeTime') }),
              trigger: 'blur',
              type: 'number',
            },
          ]"
          required
        >
          <template #label>
            <div class="flex items-center gap-[8px]">
              {{ t('common.freezeTime') }}
              <n-tooltip trigger="hover">
                <template #trigger>
                  <CrmIcon
                    type="iconicon_help_circle"
                    class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
                    size="16px"
                  />
                </template>
                {{ t('common.freezeBiggestDay') }}
              </n-tooltip>
            </div>
          </template>
          <n-input-group>
            <CrmInputNumber v-model:value="form.time" :min="1" :max="1000" :precision="0" :step="1" />
            <div
              class="flex items-center rounded-[var(--border-radius-small)] border border-l-0 bg-[var(--text-n9)] p-[4px_8px] leading-[22px]"
            >
              {{ t('common.dayUnit') }}
            </div>
          </n-input-group>
          <div class="text-[12px] text-[var(--primary-8)]">
            {{ t('common.autoUnfreezeTime', { time: dayjs().add(form.time, 'day').format('YYYY-MM-DD HH:mm:ss') }) }}
          </div>
        </n-form-item>
        <n-form-item
          :label="t('common.freezeReason')"
          path="reason"
          :rule="[
            { required: true, message: t('common.notNull', { value: t('common.freezeReason') }), trigger: 'blur' },
          ]"
          required
        >
          <n-input v-model:value="form.reason" type="textarea" :maxlength="300" show-count clearable />
        </n-form-item>
      </n-form>
    </template>
    <template v-else>
      <n-alert type="default" class="mb-[16px]">
        <template #icon>
          <CrmIcon type="iconicon_info_circle_filled" class="text-[var(--primary-8)]" />
        </template>
        {{ alertTip }}
      </n-alert>
      <n-form-item :label="t('common.unfreezeReason')" path="reason">
        <n-input v-model:value="form.reason" type="textarea" :maxlength="300" show-count clearable />
      </n-form-item>
    </template>
  </CrmModal>
</template>

<script setup lang="ts">
  import {
    type FormInst,
    NAlert,
    NForm,
    NFormItem,
    NInput,
    NInputGroup,
    NTabPane,
    NTabs,
    NTooltip,
    useMessage,
  } from 'naive-ui';
  import dayjs from 'dayjs';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmInputNumber from '@/components/pure/crm-input-number/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';

  import { freezeClue, freezeOpenSeaCustomer, unfreezeClue, unfreezeOpenSeaCustomer } from '@/api/modules';

  const props = defineProps<{
    type: 'freeze' | 'unfreeze';
    resourceType: 'customer' | 'lead';
    resourceName: string;
    resourceId: string;
    poolId: string | number;
  }>();

  const { t } = useI18n();

  const show = defineModel<boolean>('show', {
    required: true,
  });
  const emit = defineEmits<{
    (e: 'success', id: string): void;
  }>();
  const Message = useMessage();
  const loading = ref(false);
  const alertTip = computed(() => {
    const sourceTypeLocale = props.resourceType === 'customer' ? t('menu.customer') : t('menu.clue');
    return props.type === 'freeze'
      ? t('common.freezeTip', { type: sourceTypeLocale })
      : t('common.unfreezeTip', { type: sourceTypeLocale });
  });
  const form = ref({
    freezeType: 'custom',
    time: 7,
    reason: '',
  });
  const formRef = ref<FormInst>();

  function resetForm() {
    form.value = {
      freezeType: 'custom',
      time: 7,
      reason: '',
    };
    formRef.value?.restoreValidation();
  }

  async function confirmHandler() {
    if (props.type === 'freeze') {
      formRef.value?.validate(async (errors) => {
        if (!errors) {
          try {
            loading.value = true;
            const data = {
              id: props.resourceId,
              freezeDays: form.value.freezeType === 'freezeForever' ? 0 : form.value.time,
              reason: form.value.reason,
            };
            if (props.resourceType === 'customer') {
              await freezeOpenSeaCustomer(data);
            } else {
              await freezeClue(data);
            }
            Message.success(t('common.operationSuccess'));
            resetForm();
            show.value = false;
            emit('success', props.resourceId);
          } catch (error) {
            // eslint-disable-next-line no-console
            console.error(error);
          } finally {
            loading.value = false;
          }
        }
      });
    } else {
      try {
        loading.value = true;
        const data = { id: props.resourceId, reason: form.value.reason };
        if (props.resourceType === 'customer') {
          await unfreezeOpenSeaCustomer(data);
        } else {
          await unfreezeClue(data);
        }
        Message.success(t('common.operationSuccess'));
        resetForm();
        show.value = false;
        emit('success', props.resourceId);
      } catch (error) {
        // eslint-disable-next-line no-console
        console.error(error);
      } finally {
        loading.value = false;
      }
    }
  }

  watch(
    () => show.value,
    (val) => {
      if (!val) {
        resetForm();
      }
    }
  );
</script>

<style lang="less" scoped></style>
