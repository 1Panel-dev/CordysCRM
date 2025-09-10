<template>
  <CrmCard hide-footer :special-height="64">
    <div class="flex h-full flex-col gap-[16px]">
      <div class="flex items-center">
        <div class="font-medium text-[var(--text-n1)]">{{ t('system.personal.apiKey') }}</div>
        <n-tooltip placement="right">
          <template #trigger>
            <CrmIcon
              type="iconicon_help_circle"
              :size="16"
              class="ml-[4px] cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-8)]"
            />
          </template>
          {{ t('system.personal.apiKeyTip') }}
        </n-tooltip>
      </div>
      <div class="flex items-center">
        <n-tooltip placement="right" :disabled="apiKeyList.length < 5">
          <template #trigger>
            <n-button
              v-permission="['PERSONAL_API_KEY:ADD']"
              type="primary"
              ghost
              class="n-btn-outline-primary"
              :loading="newLoading"
              :disabled="apiKeyList.length >= 5"
              @click="newApiKey"
            >
              {{ t('common.new') }}
            </n-button>
          </template>
          {{ t('system.personal.maxTip') }}
        </n-tooltip>
      </div>

      <n-spin class="h-full w-full" :show="loading">
        <div class="api-list-content">
          <div v-for="item of apiKeyList" :key="item.id" class="api-item">
            <div class="mb-[8px] border-b border-solid border-[var(--text-n8)]">
              <div class="px-[16px]">
                <div class="api-item-label">Access Key</div>
                <div class="api-item-value-strong">
                  {{ item.accessKey }}
                  <CrmTag v-if="item.isExpire" size="small" class="mx-[4px] px-[4px]" theme="light" type="warning">
                    {{ t('system.personal.expired') }}
                  </CrmTag>
                  <CrmIcon type="iconicon_file_copy" class="copy-icon" @click="handleCopy(item.accessKey)" />
                </div>
                <div class="api-item-label">Secret Key</div>
                <div class="api-item-value-strong">
                  {{ item.desensitization ? item.secretKey.replace(/./g, '*') : item.secretKey }}
                  <CrmIcon
                    :type="item.desensitization ? 'iconicon_browse_off' : 'iconicon_browse'"
                    class="eye-icon"
                    @click="desensitization(item)"
                  />
                  <CrmIcon type="iconicon_file_copy" class="copy-icon" @click="handleCopy(item.secretKey)" />
                </div>
              </div>
            </div>
            <div class="px-[16px]">
              <div class="api-item-label">{{ t('common.desc') }}</div>
              <n-input
                v-if="item.showDescInput"
                v-model:value="item.description"
                type="textarea"
                :autosize="{
                  maxRows: 2,
                  minRows: 2,
                }"
                :placeholder="t('common.pleaseInput')"
                :maxlength="1000"
                @blur="handleDescChange(item)"
              />
              <div v-else class="desc-line api-item-value">
                <n-tooltip trigger="hover" flip :disabled="!item.description">
                  <template #trigger>
                    <div class="one-line-text w-[300px]">{{ item.description || '-' }}</div>
                  </template>
                  {{ item.description }}
                </n-tooltip>
                <CrmIcon
                  v-permission="['PERSONAL_API_KEY:UPDATE']"
                  type="iconicon_edit1"
                  class="edit-icon"
                  @click="handleEditClick(item)"
                />
              </div>
              <div class="api-item-label mt-[4px]">{{ t('common.createTime') }}</div>
              <div class="api-item-value">
                {{ dayjs(item.createTime).format('YYYY-MM-DD HH:mm:ss') }}
              </div>
              <div class="api-item-label">{{ t('system.personal.validTime') }}</div>
              <div class="api-item-value">
                {{ item.forever ? t('system.personal.forever') : dayjs(item.expireTime).format('YYYY-MM-DD HH:mm:ss') }}
                <n-tooltip v-if="item.isExpire" :disabled="!item.description">
                  <template #trigger>
                    <CrmIcon type="iconicon_info_circle_filled" class="ml-[4px] text-[rgb(var(--warning-6))]" />
                  </template>
                  {{ t('system.personal.expiredTip') }}
                </n-tooltip>
              </div>
            </div>
            <div class="flex items-center justify-between px-[16px]">
              <div class="flex items-center gap-[8px]">
                <n-button
                  v-permission="['PERSONAL_API_KEY:UPDATE']"
                  size="small"
                  type="default"
                  class="outline--secondary mr-[8px] px-[8px]"
                  @click="handleSetValidTime(item)"
                >
                  {{ t('system.personal.validTime') }}
                </n-button>
                <n-button
                  v-permission="['PERSONAL_API_KEY:DELETE']"
                  type="error"
                  size="small"
                  ghost
                  class="n-btn-outline-error"
                  @click="handleDeleteApiKey(item)"
                >
                  {{ t('common.delete') }}
                </n-button>
              </div>
              <n-switch
                :disabled="!hasAnyPermission(['PERSONAL_API_KEY:UPDATE'])"
                :value="item.enable"
                size="small"
                :rubber-band="false"
                @click="handleBeforeEnableChange(item)"
              />
            </div>
          </div>
          <div v-if="apiKeyList.length === 0" class="col-span-8 flex w-full items-center justify-center p-[44px]">
            {{ hasCratePermission ? t('system.personal.noData') : t('system.personal.empty') }}
            <n-button v-if="hasCratePermission" text class="ml-[8px]" type="primary" @click="newApiKey">
              {{ t('common.new') }}
            </n-button>
          </div>
        </div>
      </n-spin>
    </div>
    <CrmModal
      v-model:show="timeModalVisible"
      :title="t('system.personal.setValidTime')"
      :positive-text="t('common.save')"
      class="crm-form-modal"
      @confirm="handleTimeConfirm"
      @cancel="handleTimeClose"
    >
      <n-form ref="timeFormRef" :model="timeForm" layout="vertical">
        <n-form-item path="activeTimeType" :label="t('system.personal.timeSetting')">
          <n-radio-group v-model:value="timeForm.activeTimeType" name="activeTimeType">
            <n-radio-button value="forever" class="show-type-icon" :label="t('system.personal.forever')" />
            <n-radio-button value="custom" class="show-type-icon" :label="t('system.personal.custom')" />
          </n-radio-group>
        </n-form-item>
        <n-form-item
          v-if="timeForm.activeTimeType === 'custom'"
          path="time"
          :label="t('system.personal.timeSetting')"
          :rule="[{ required: true, message: t('common.notNull', { value: `${t('system.personal.expiredTime')}` }) }]"
        >
          <n-date-picker v-model:value="timeForm.time" class="w-[240px]" type="datetime" clearable />
        </n-form-item>
        <n-form-item path="desc" :label="t('system.personal.accessKeyDesc')">
          <n-input
            v-model:value="timeForm.desc"
            type="textarea"
            :placeholder="t('system.personal.accessKeyDescPlaceholder')"
            :maxlength="1000"
          />
        </n-form-item>
      </n-form>
    </CrmModal>
  </CrmCard>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { useClipboard } from '@vueuse/core';
  import {
    FormInst,
    NButton,
    NDatePicker,
    NForm,
    NFormItem,
    NInput,
    NRadioButton,
    NRadioGroup,
    NSpin,
    NSwitch,
    NTooltip,
    useMessage,
  } from 'naive-ui';
  import dayjs from 'dayjs';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { ApiKeyItem, DefaultTimeForm } from '@lib/shared/models/system/business';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';

  import { addApiKey, deleteApiKey, disableApiKey, enableApiKey, getApiKeyList, updateApiKey } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const { openModal } = useModal();

  const { copy, isSupported } = useClipboard({ legacy: true });

  const Message = useMessage();

  const { t } = useI18n();

  const hasCratePermission = hasAnyPermission(['PERSONAL_API_KEY:ADD']);
  const apiKeyList = ref<ApiKeyItem[]>([]);
  const loading = ref(false);

  async function initApiKeys() {
    try {
      loading.value = true;
      const res = await getApiKeyList();
      apiKeyList.value = res.map((item) => ({
        ...item,
        isExpire: item.forever ? false : item.expireTime < Date.now(),
        desensitization: true,
        showDescInput: false,
      }));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  const newLoading = ref(false);
  async function newApiKey() {
    try {
      newLoading.value = true;
      await addApiKey();
      Message.success(t('common.newSuccess'));
      initApiKeys();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      newLoading.value = false;
    }
  }

  function handleCopy(val: string) {
    if (isSupported) {
      copy(val);
      Message.success(t('common.copySuccess'));
    } else {
      Message.warning(t('common.copyNotSupport'));
    }
  }

  function desensitization(item: ApiKeyItem) {
    item.desensitization = !item.desensitization;
  }

  async function handleDescChange(item: ApiKeyItem) {
    try {
      loading.value = true;
      await updateApiKey({
        id: item.id || '',
        description: item.description,
      });
      item.showDescInput = false;
      Message.success(t('common.updateSuccess'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  function handleEditClick(item: ApiKeyItem) {
    item.showDescInput = true;
    nextTick(() => {
      document.querySelector<HTMLInputElement>('.n-input__textarea-el')?.focus();
    });
  }

  const defaultTimeForm: DefaultTimeForm = {
    activeTimeType: 'forever',
    time: undefined,
    desc: '',
  };

  const timeForm = ref({ ...defaultTimeForm });
  const activeKey = ref<ApiKeyItem>();
  const timeModalVisible = ref(false);

  function handleSetValidTime(apiKey: ApiKeyItem) {
    activeKey.value = apiKey;
    timeForm.value = {
      activeTimeType: apiKey.forever ? 'forever' : 'custom',
      time: apiKey.expireTime ?? undefined,
      desc: apiKey.description,
    };
    timeModalVisible.value = true;
  }

  function handleDeleteApiKey(item: ApiKeyItem) {
    openModal({
      type: 'error',
      title: t('system.personal.confirmDelete'),
      content: t('system.personal.deleteTip'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteApiKey(item.id);
          Message.success(t('common.deleteSuccess'));
          initApiKeys();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  async function handleBeforeEnableChange(item: ApiKeyItem) {
    if (!hasAnyPermission(['PERSONAL_API_KEY:UPDATE'])) return;
    if (item.enable) {
      openModal({
        type: 'error',
        title: t('common.confirmClose'),
        content: t('system.personal.closeTip'),
        positiveText: t('common.confirmClose'),
        negativeText: t('common.cancel'),
        onPositiveClick: async () => {
          try {
            await disableApiKey(item.id);
            Message.success(t('common.closeSuccess'));
            item.enable = false;
          } catch (error) {
            // eslint-disable-next-line no-console
            console.log(error);
          }
        },
      });
    } else {
      try {
        await enableApiKey(item.id);
        Message.success(t('common.enableSuccess'));
        item.enable = true;
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    }
  }

  function handleTimeClose() {
    timeForm.value = { ...defaultTimeForm };
    timeModalVisible.value = false;
  }

  const timeFormRef = ref<FormInst | null>(null);
  function handleTimeConfirm() {
    timeFormRef.value?.validate(async (errors) => {
      if (!errors) {
        try {
          await updateApiKey({
            id: activeKey.value?.id || '',
            description: timeForm.value.desc,
            expireTime: timeForm.value.activeTimeType === 'forever' ? 0 : dayjs(timeForm.value.time).valueOf(),
            forever: timeForm.value.activeTimeType === 'forever',
          });
          Message.success(t('common.updateSuccess'));
          handleTimeClose();
          initApiKeys();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      }
    });
  }

  onBeforeMount(() => {
    initApiKeys();
  });
</script>

<style scoped lang="less">
  .api-list-content {
    @apply grid h-full overflow-auto;
    .crm-scroll-bar();

    gap: 16px;
    grid-template-columns: repeat(auto-fill, minmax(318px, 2fr));
    padding: 16px;
    border-radius: var(--border-radius-small);
    background-color: var(--text-n9);
  }
  .api-item {
    padding: 16px 0;
    height: 335px;
    border-radius: var(--border-radius-small);
    background-color: var(--text-n10);
    .api-item-label {
      font-size: 12px;
      line-height: 16px;
      color: var(--text-n4);
    }
    .api-item-value {
      @apply flex items-center;

      margin-bottom: 16px;
      font-size: 14px;
      color: var(--text-n1);
    }
    .api-item-value-strong {
      @apply flex items-center;

      margin-bottom: 8px;
      font-size: 14px;
      font-weight: 500;
      color: var(--text-n1);
      &:hover {
        .copy-icon {
          @apply visible;
        }
      }
      .copy-icon,
      .eye-icon {
        @apply cursor-pointer;

        margin-left: 4px;
        color: var(--text-n4);
        &:hover {
          color: var(--primary-8);
        }
      }
      .copy-icon {
        @apply invisible;
      }
    }
  }
  .desc-line {
    gap: 4px;
    &:hover {
      .edit-icon {
        @apply visible;
      }
    }
    .edit-icon {
      @apply invisible cursor-pointer;

      color: var(--primary-8);
    }
  }
</style>
