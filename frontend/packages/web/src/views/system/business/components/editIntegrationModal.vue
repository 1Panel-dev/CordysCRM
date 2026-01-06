<template>
  <CrmModal
    v-model:show="showModal"
    :title="t('system.business.configType', { type: props.title })"
    :mask-closable="false"
  >
    <n-form
      ref="formRef"
      :model="form.config"
      :rules="rules"
      label-placement="left"
      require-mark-placement="left"
      :label-width="getLabelWidth"
    >
      <!-- 应用 key 第一版没有 -->
      <!-- <n-form-item v-if="['DINGTALK'].includes(form?.type)" path="appKey" :label="t('system.business.appKey')">
        <n-input
          v-model:value="form.config.appKey"
          type="password"
          show-password-on="click"
          :input-props="{ autocomplete: 'new-password' }"
          :placeholder="t('common.pleaseInput')"
        />
      </n-form-item> -->
      <!-- MaxKB -->
      <template v-if="[CompanyTypeEnum.MAXKB].includes(form?.type)">
        <n-form-item path="mkAddress" :label="t('system.business.agent.agentMaxKBUrl')">
          <n-input v-model:value="form.config.mkAddress" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
      </template>

      <!-- 企业 ID -->
      <template v-if="platformType.includes(form?.type)">
        <n-form-item path="corpId" :label="t('system.business.corpId')">
          <n-input v-model:value="form.config.corpId" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
      </template>
      <!-- DE 地址 -->
      <template v-if="[CompanyTypeEnum.DATA_EASE].includes(form?.type)">
        <n-form-item path="redirectUrl" :label="t('system.business.DE.url')">
          <n-input v-model:value="form.config.redirectUrl" :placeholder="t('system.business.DE.urlPlaceholder')" />
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
            v-model:value="form.config.agentId"
            :placeholder="
              form.type === CompanyTypeEnum.DATA_EASE ? t('system.business.DE.idPlaceholder') : t('common.pleaseInput')
            "
          />
        </n-form-item>
      </template>

      <template v-if="[CompanyTypeEnum.QCC].includes(form?.type)">
        <n-form-item path="qccAddress" :label="t('system.business.qichachaAddress')">
          <n-input v-model:value="form.config.qccAddress" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
        <n-form-item path="qccAccessKey" label="Access Key">
          <n-input v-model:value="form.config.qccAccessKey" :placeholder="t('common.pleaseInput')" />
        </n-form-item>
        <n-form-item path="qccSecretKey" label="Secret Key">
          <n-input
            v-model:value="form.config.qccSecretKey"
            type="password"
            show-password-on="click"
            :placeholder="t('common.pleaseInput')"
          />
        </n-form-item>
      </template>

      <!-- 应用密钥 -->
      <n-form-item
        v-if="![CompanyTypeEnum.SQLBot, CompanyTypeEnum.QCC].includes(form.type)"
        path="appSecret"
        :label="getAppSecretText"
      >
        <n-input
          v-model:value="form.config.appSecret"
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

      <n-form-item
        v-if="[CompanyTypeEnum.SQLBot].includes(form.type)"
        path="appSecret"
        :label="t('system.business.SQLBot.embeddedScript')"
      >
        <n-input
          v-model:value="form.config.appSecret"
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

      <n-form-item
        v-if="form.type === CompanyTypeEnum.DINGTALK"
        path="appId"
        :label="t('system.business.authenticationSettings.innerAppId')"
      >
        <n-input
          v-model:value="form.config.appId"
          :placeholder="t('system.business.authenticationSettings.innerAppIdPlaceholder')"
        />
      </n-form-item>

      <n-form-item
        v-if="form.type === CompanyTypeEnum.LARK"
        path="redirectUrl"
        :label="t('system.business.authenticationSettings.callbackUrl')"
      >
        <n-input
          v-model:value="form.config.redirectUrl"
          :placeholder="t('system.business.authenticationSettings.callbackUrlPlaceholder')"
        />
      </n-form-item>

      <!-- DE账号 -->
      <template v-if="form.type === CompanyTypeEnum.DATA_EASE">
        <n-form-item path="deAutoSync" :label="t('system.business.DE.autoSync')" class="autoSyncItem">
          <n-switch v-model:value="form.config.deAutoSync" />
          <div class="w-full text-[12px] text-[var(--text-n4)]">{{ t('system.business.DE.autoSyncTip') }}</div>
        </n-form-item>
        <n-form-item path="deAccessKey" label="Access Key">
          <n-input
            v-model:value="form.config.deAccessKey"
            type="password"
            show-password-on="click"
            :placeholder="t('common.pleaseInput')"
            @change="fetchDEOrgList"
          />
        </n-form-item>
        <n-form-item path="deSecretKey" label="Secret Key">
          <n-input
            v-model:value="form.config.deSecretKey"
            type="password"
            show-password-on="click"
            :placeholder="t('common.pleaseInput')"
            @change="fetchDEOrgList"
          />
        </n-form-item>
        <n-form-item path="deOrgID" :label="t('system.business.DE.org')">
          <n-tooltip :disabled="!!form.config.deAccessKey && !!form.config.deSecretKey">
            <template #trigger>
              <n-select
                v-model:value="form.config.deOrgID"
                size="medium"
                :options="DEOrgList"
                label-field="name"
                value-field="id"
                :disabled="!form.config.deAccessKey || !form.config.deSecretKey"
                :loading="orgListLoading"
                filterable
              />
            </template>
            {{ t('system.business.DE.orgTip') }}
          </n-tooltip>
        </n-form-item>
      </template>
    </n-form>
    <template #footer>
      <div class="flex w-full items-center justify-between">
        <div v-if="platformType.includes(form.type)" class="ml-[4px] flex items-center gap-[8px]">
          <n-tooltip :disabled="form.verify">
            <template #trigger>
              <n-switch v-model:value="form.config.startEnable" :rubber-band="false" :disabled="!form.verify" />
            </template>
            {{ t('system.business.notConfiguredTip') }}
          </n-tooltip>

          <div class="text-[12px] text-[var(--text-n1)]">
            {{ t('system.business.authenticationSettings.syncUser') }}
          </div>
          <n-tooltip trigger="hover">
            <template #trigger>
              <CrmIcon
                type="iconicon_help_circle"
                :size="16"
                class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
              />
            </template>
            <template #default>
              <div>
                <div>{{ t('system.business.authenticationSettings.syncUsersToolTitle', { type: props.title }) }}</div>
                <div>{{ t('system.business.authenticationSettings.syncUsersOpenTip', { type: props.title }) }}</div>
                <div>{{ t('system.business.authenticationSettings.syncUsersTipContent', { type: props.title }) }}</div>
              </div>
            </template>
          </n-tooltip>
        </div>
        <div class="flex flex-1 items-center justify-end">
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
  import { cloneDeep } from 'lodash-es';

  import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { DEOrgItem, ThirdPartyDEConfig, ThirdPartyResourceConfig } from '@lib/shared/models/system/business';

  import CrmModal from '@/components/pure/crm-modal/index.vue';

  import { getDEOrgList, testConfigSynchronization, updateConfigSynchronization } from '@/api/modules';
  import { defaultThirdPartyConfigMap, platformType } from '@/config/business';
  import useModal from '@/hooks/useModal';

  const { t } = useI18n();
  const Message = useMessage();

  const props = defineProps<{
    integration?: ThirdPartyResourceConfig;
    title: string;
  }>();

  const showModal = defineModel<boolean>('show', {
    required: true,
    default: false,
  });
  const { openModal } = useModal();
  const emit = defineEmits<{
    (e: 'initSync'): void;
  }>();

  const form = ref<ThirdPartyResourceConfig>({
    type: CompanyTypeEnum.WECOM,
    verify: false,
    config: defaultThirdPartyConfigMap[CompanyTypeEnum.WECOM],
  });
  const DEOrgList = ref<DEOrgItem[]>([]);
  const orgListLoading = ref(false);

  const getAppSecretText = computed(() => {
    if (props.integration?.type === CompanyTypeEnum.DATA_EASE) return 'APP Secret';
    if (props.integration?.type === CompanyTypeEnum.SQLBot) return t('system.business.SQLBot.embeddedScript');
    if (props.integration?.type === CompanyTypeEnum.MAXKB) return 'API Key';
    return t('system.business.appSecret');
  });

  const rules = computed<FormRules>(() => ({
    corpId: [
      {
        trigger: ['input', 'blur'],
        required: true,
        message: t('common.notNull', { value: `${t('system.business.corpId')} ` }),
      },
    ],
    agentId: [
      {
        trigger: ['input', 'blur'],
        required: true,
        message: t('common.notNull', {
          value: `${props.integration?.type === CompanyTypeEnum.DATA_EASE ? 'APP ID' : t('system.business.agentId')} `,
        }),
      },
    ],
    appKey: [
      {
        trigger: ['input', 'blur'],
        required: true,
        message: t('common.notNull', { value: `${t('system.business.appKey')} ` }),
      },
    ],
    appSecret: [
      {
        trigger: ['input', 'blur'],
        required: true,
        message: t('common.notNull', {
          value: getAppSecretText.value,
        }),
      },
    ],
    appId: [
      {
        trigger: ['input', 'blur'],
        required: true,
        message: t('system.business.authenticationSettings.innerAppIdPlaceholder'),
      },
    ],
    // 判断redirectUrl 如果form.type是LARK 则提示回调地址不能为空
    redirectUrl:
      props.integration?.type === CompanyTypeEnum.LARK
        ? [
            {
              trigger: ['input', 'blur'],
              required: true,
              message: t('common.notNull', { value: `${t('system.business.authenticationSettings.callbackUrl')} ` }),
            },
          ]
        : [
            {
              trigger: ['input', 'blur'],
              required: true,
              message: t('common.notNull', { value: `${t('system.business.DE.url')} ` }),
            },
          ],
    deAccessKey: [
      { trigger: ['input', 'blur'], required: true, message: t('common.notNull', { value: 'deAccessKey' }) },
    ],
    deSecretKey: [
      { trigger: ['input', 'blur'], required: true, message: t('common.notNull', { value: 'deSecretKey' }) },
    ],
    deOrgID: [
      {
        trigger: ['input', 'blur'],
        required: true,
        message: t('common.notNull', { value: t('system.business.DE.org') }),
      },
    ],
    mkAddress: [
      {
        trigger: ['input', 'blur'],
        required: true,
        message: t('common.notNull', { value: t('system.business.agent.agentMaxKBUrl') }),
      },
    ],
  }));

  const formRef = ref<FormInst | null>(null);
  function cancel() {
    showModal.value = false;
  }

  function makeParams() {
    const { type, verify, config } = form.value;
    const thirdConfigKeys = Object.keys(defaultThirdPartyConfigMap[type as CompanyTypeEnum]);
    const params: Record<string, any> = {};
    thirdConfigKeys.forEach((configKey: string) => {
      params[configKey] = config[configKey];
    });
    return {
      verify,
      type,
      config: params,
    };
  }

  async function fetchDEOrgList() {
    try {
      orgListLoading.value = true;
      const params = makeParams().config;
      DEOrgList.value = await getDEOrgList(params as ThirdPartyDEConfig);
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    } finally {
      orgListLoading.value = false;
    }
  }

  /** *
   * 保存
   */
  const loading = ref(false);
  async function handleSave() {
    try {
      loading.value = true;
      await updateConfigSynchronization(makeParams());
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

  const isChangeCorpId = computed(
    () => props.integration?.config?.corpId && props.integration?.config?.corpId !== form.value.config?.corpId
  );

  function handleThirdConfig() {
    if (isChangeCorpId.value) {
      openModal({
        type: 'error',
        title: t('common.updateConfirmTitle'),
        content: t('system.business.authenticationSettings.changeCorpIdTip'),
        positiveText: t('common.confirm'),
        negativeText: t('common.cancel'),

        onPositiveClick: async () => {
          try {
            await handleSave();
          } catch (error) {
            // eslint-disable-next-line no-console
            console.log(error);
          }
        },
      });
    } else {
      handleSave();
    }
  }

  function confirmHandler() {
    formRef.value?.validate((error) => {
      if (!error) {
        if (platformType.includes(form.value.type)) {
          handleThirdConfig();
        } else {
          handleSave();
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
          const result = await testConfigSynchronization(makeParams());
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

  const getLabelWidth = computed(() => {
    if ([CompanyTypeEnum.DATA_EASE, CompanyTypeEnum.QCC].includes(form.value.type)) return 120;
    if ([...platformType, CompanyTypeEnum.MAXKB].includes(form.value.type)) return 100;

    return 80;
  });

  watch(
    () => props.integration,
    (val) => {
      form.value = cloneDeep(val as ThirdPartyResourceConfig);
      if (showModal.value && props.integration?.type === CompanyTypeEnum.DATA_EASE) {
        fetchDEOrgList();
      }
    },
    { deep: true }
  );
</script>

<style lang="less">
  .autoSyncItem {
    .n-form-item-label {
      align-items: start;
    }
  }
</style>
