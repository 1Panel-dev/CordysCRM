<template>
  <CrmModal
    v-model:show="showModal"
    :title="t('system.business.configType', { type: props.title })"
    class="crm-form-modal"
  >
    <n-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-placement="left"
      require-mark-placement="left"
      :label-width="form.type === CompanyTypeEnum.DATA_EASE ? 120 : 80"
    >
      <!-- 应用 key 第一版没有 -->
      <!-- <n-form-item v-if="['DINGTALK'].includes(form?.type)" path="appKey" :label="t('system.business.appKey')">
        <n-input
          v-model:value="form.appKey"
          type="password"
          show-password-on="click"
          :input-props="{ autocomplete: 'new-password' }"
          :placeholder="t('common.pleaseInput')"
        />
      </n-form-item> -->

      <!-- 企业 ID -->
      <template v-if="[CompanyTypeEnum.WECOM].includes(form?.type)">
        <n-form-item path="corpId" :label="t('system.business.corpId')">
          <n-input v-model:value="form.corpId" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
      </template>
      <!-- DE 地址 -->
      <template v-if="[CompanyTypeEnum.DATA_EASE].includes(form?.type)">
        <n-form-item path="redirectUrl" :label="t('system.business.DE.url')">
          <n-input v-model:value="form.redirectUrl" :placeholder="t('system.business.DE.urlPlaceholder')" />
        </n-form-item>
      </template>
      <!-- 应用 ID -->
      <template
        v-if="
          [
            CompanyTypeEnum.WECOM,
            CompanyTypeEnum.DINGTALK,
            CompanyTypeEnum.LARK,
            CompanyTypeEnum.INTERNAL,
            CompanyTypeEnum.DATA_EASE,
          ].includes(form?.type)
        "
      >
        <n-form-item
          path="agentId"
          :label="form.type === CompanyTypeEnum.DATA_EASE ? 'APP ID' : t('system.business.agentId')"
        >
          <n-input
            v-model:value="form.agentId"
            :placeholder="
              form.type === CompanyTypeEnum.DATA_EASE ? t('system.business.DE.idPlaceholder') : t('common.pleaseInput')
            "
          />
        </n-form-item>
      </template>

      <!-- 应用密钥 -->
      <n-form-item
        v-if="form.type !== CompanyTypeEnum.SQLBot"
        path="appSecret"
        :label="form.type === CompanyTypeEnum.DATA_EASE ? 'APP Secret' : t('system.business.appSecret')"
      >
        <n-input
          v-model:value="form.appSecret"
          type="password"
          show-password-on="click"
          :input-props="{ autocomplete: 'new-password' }"
          :placeholder="
            form.type === CompanyTypeEnum.DATA_EASE
              ? t('system.business.DE.secretPlaceholder')
              : t('common.pleaseInput')
          "
        />
      </n-form-item>
      <n-form-item v-else path="appSecret" :label="t('system.business.SQLBot.embeddedScript')">
        <n-input
          v-model:value="form.appSecret"
          type="textarea"
          :placeholder="`${t('common.pleaseInput')}${t('system.business.SQLBot.embeddedScript')}`"
        />
        <div class="text-[var(--primary-8)]">
          {{
            t('system.business.SQLBot.example', {
              url: '&lt;script async deferid="XXXXXX" src="XXXXXX"&gt;&lt;/script&gt;',
            })
          }}
        </div>
      </n-form-item>
      <!-- DE账号 -->
      <template v-if="form.type === CompanyTypeEnum.DATA_EASE">
        <n-form-item path="deEmbedType" :label="t('system.business.DE.embedType')">
          <n-checkbox-group v-model:value="form.deEmbedType">
            <n-space item-style="display: flex;">
              <n-checkbox
                v-model:checked="form.deModuleEmbedding"
                value="module"
                :label="t('system.business.DE.embedModule')"
              />
              <n-checkbox v-model:checked="form.deLinkIntegration" value="link" :label="t('system.business.DE.link')" />
            </n-space>
          </n-checkbox-group>
        </n-form-item>
        <template v-if="form.deEmbedType?.includes('module')">
          <n-form-item path="deAutoSync" :label="t('system.business.DE.autoSync')" class="autoSyncItem">
            <n-switch v-model:value="form.deAutoSync" />
            <div class="w-full text-[12px] text-[var(--text-n4)]">{{ t('system.business.DE.autoSyncTip') }}</div>
          </n-form-item>
          <n-form-item path="deAccessKey" label="Access Key">
            <n-input
              v-model:value="form.deAccessKey"
              type="password"
              show-password-on="click"
              :placeholder="t('common.pleaseInput')"
              @change="fetchDEOrgList"
            />
          </n-form-item>
          <n-form-item path="deSecretKey" label="Secret Key">
            <n-input
              v-model:value="form.deSecretKey"
              type="password"
              show-password-on="click"
              :placeholder="t('common.pleaseInput')"
              @change="fetchDEOrgList"
            />
          </n-form-item>
          <n-form-item path="deOrgID" :label="t('system.business.DE.org')">
            <n-tooltip :disabled="!!form.deAccessKey && !!form.deSecretKey">
              <template #trigger>
                <n-select
                  v-model:value="form.deOrgID"
                  size="medium"
                  :options="DEOrgList"
                  label-field="name"
                  value-field="id"
                  :disabled="!form.deAccessKey || !form.deSecretKey"
                  :loading="orgListLoading"
                  filterable
                />
              </template>
              {{ t('system.business.DE.orgTip') }}
            </n-tooltip>
          </n-form-item>
        </template>
        <n-form-item
          v-if="form.deEmbedType?.includes('link')"
          path="deAccount"
          :label="t('system.business.DE.account')"
        >
          <n-input
            v-model:value="form.deAccount"
            type="text"
            :placeholder="t('system.business.DE.accountPlaceholder')"
          />
        </n-form-item>
      </template>
    </n-form>
    <template #footer>
      <div class="flex w-full items-center justify-end">
        <n-button :disabled="loading" secondary @click="cancel">
          {{ t('common.cancel') }}
        </n-button>
        <n-button
          :loading="linkLoading"
          type="primary"
          ghost
          class="n-btn-outline-primary mx-[12px]"
          @click="continueLink"
        >
          {{ t('system.business.mailSettings.testLink') }}
        </n-button>
        <n-button :loading="loading" type="primary" @click="confirmHandler">
          {{ t('common.confirm') }}
        </n-button>
      </div>
    </template>
  </CrmModal>
</template>

<script setup lang="ts">
  import {
    FormInst,
    FormRules,
    NButton,
    NCheckbox,
    NCheckboxGroup,
    NForm,
    NFormItem,
    NInput,
    NSelect,
    NSpace,
    NSwitch,
    NTooltip,
    useMessage,
  } from 'naive-ui';

  import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ConfigSynchronization, DEOrgItem } from '@lib/shared/models/system/business';

  import CrmModal from '@/components/pure/crm-modal/index.vue';

  import { getDEOrgList, testConfigSynchronization, updateConfigSynchronization } from '@/api/modules';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    integration?: ConfigSynchronization;
    title: string;
  }>();

  const showModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const emit = defineEmits<{
    (e: 'initSync'): void;
  }>();

  const form = ref<ConfigSynchronization>({
    corpId: '',
    agentId: '',
    appSecret: '',
    syncEnable: true,
    qrcodeEnable: true,
    type: CompanyTypeEnum.WECOM,
    redirectUrl: '',
    deAccount: '',
    deBoardEnable: false, // DE看板是否开启
    verify: undefined,
    deEmbedType: [],
    deOrgID: '',
  });
  const DEOrgList = ref<DEOrgItem[]>([]);
  const orgListLoading = ref(false);

  async function fetchDEOrgList() {
    try {
      orgListLoading.value = true;
      DEOrgList.value = await getDEOrgList(form.value);
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    } finally {
      orgListLoading.value = false;
    }
  }

  watch(
    () => props.integration,
    (val) => {
      form.value = { ...(val as ConfigSynchronization) };
      form.value.deEmbedType = [val?.deModuleEmbedding ? 'module' : '', val?.deLinkIntegration ? 'link' : ''].filter(
        Boolean
      );
      if (showModal.value && props.integration?.type === CompanyTypeEnum.DATA_EASE) {
        fetchDEOrgList();
      }
    },
    { deep: true }
  );

  const getAppSecretText = computed(() => {
    if (props.integration?.type === CompanyTypeEnum.DATA_EASE) return 'APP Secret';
    if (props.integration?.type === CompanyTypeEnum.SQLBot) return t('system.business.SQLBot.embeddedScript');
    return t('system.business.appSecret');
  });

  const rules = computed<FormRules>(() => ({
    corpId: [{ required: true, message: t('common.notNull', { value: `${t('system.business.corpId')} ` }) }],
    agentId: [
      {
        required: true,
        message: t('common.notNull', {
          value: `${props.integration?.type === CompanyTypeEnum.DATA_EASE ? 'APP ID' : t('system.business.agentId')} `,
        }),
      },
    ],
    appKey: [{ required: true, message: t('common.notNull', { value: `${t('system.business.appKey')} ` }) }],
    appSecret: [
      {
        required: true,
        message: t('common.notNull', {
          value: getAppSecretText.value,
        }),
      },
    ],
    redirectUrl: [{ required: true, message: t('common.notNull', { value: `${t('system.business.DE.url')} ` }) }],
    deAccount: [{ required: true, message: t('common.notNull', { value: `${t('system.business.DE.account')} ` }) }],
    deEmbedType: [{ required: true, message: t('system.business.DE.embedTypeTip'), type: 'array' }],
    deAccessKey: [{ required: true, message: t('common.notNull', { value: 'deAccessKey' }) }],
    deSecretKey: [{ required: true, message: t('common.notNull', { value: 'deSecretKey' }) }],
    deOrgID: [{ required: true, message: t('common.notNull', { value: 'system.business.DE.org' }) }],
  }));

  const formRef = ref<FormInst | null>(null);
  function cancel() {
    showModal.value = false;
  }

  /** *
   * 保存
   */
  const loading = ref(false);
  function confirmHandler() {
    formRef.value?.validate(async (error) => {
      if (!error) {
        try {
          loading.value = true;
          await updateConfigSynchronization({
            ...form.value,
            deModuleEmbedding: form.value.deEmbedType?.includes('module'),
            deLinkIntegration: form.value.deEmbedType?.includes('link'),
          });
          Message.success(t('common.updateSuccess'));
          showModal.value = false;
          emit('initSync');
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          loading.value = false;
        }
      }
    });
  }
  /** *
   * 测试连接
   */
  const linkLoading = ref<boolean>(false);
  function continueLink() {
    formRef.value?.validate(async (error) => {
      if (!error) {
        try {
          linkLoading.value = true;
          const result = await testConfigSynchronization(form.value);
          const isSuccess = result.data.data;
          form.value.verify = result.data.data;
          if (isSuccess) {
            Message.success(t('org.testConnectionSuccess'));
          } else {
            Message.error(t('org.testConnectionError'));
          }
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        } finally {
          linkLoading.value = false;
        }
      }
    });
  }
</script>

<style lang="less">
  .autoSyncItem {
    .n-form-item-label {
      align-items: start;
    }
  }
</style>
