<template>
  <CrmModal v-model:show="showModal" :title="t('system.business.settings')" :mask-closable="false" :width="620">
    <n-tabs v-model:value="activeSettingTab" type="segment" class="no-content mb-[4px]">
      <n-tab-pane name="platform" :tab="t('system.business.platformConfigTab', { type: props.title })"></n-tab-pane>
      <n-tab-pane name="sync" :tab="t('system.business.syncSettings')"></n-tab-pane>
    </n-tabs>

    <n-form
      v-show="activeSettingTab === 'platform'"
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
      <n-form-item v-if="![CompanyTypeEnum.QCC].includes(form.type)" path="appSecret" :label="getAppSecretText">
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

    <n-spin :show="syncSettingsLoading">
      <n-form
        v-show="activeSettingTab === 'sync'"
        ref="syncFormRef"
        :model="syncForm"
        :rules="syncRules"
        label-placement="top"
      >
        <div class="mb-[16px] flex items-center gap-[8px]">
          <n-switch v-model:value="syncForm.enable" :rubber-band="false" />
          <span class="text-[var(--text-n1)]">
            {{ t('system.business.syncSettings.enableSchedule') }}
          </span>
        </div>

        <template v-if="syncForm.enable">
          <n-form-item path="syncFrequency" :label="t('system.business.syncSettings.period')">
            <template #label>
              <div class="flex items-center gap-[4px]">
                <span>{{ t('system.business.syncSettings.period') }}</span>
                <n-tooltip trigger="hover">
                  <template #trigger>
                    <CrmIcon
                      type="iconicon_help_circle"
                      :size="16"
                      class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-8)]"
                    />
                  </template>
                  {{ t('system.business.syncSettings.periodTip') }}
                </n-tooltip>
              </div>
            </template>
            <div class="w-full">
              <div class="flex gap-[8px]">
                <n-select
                  v-model:value="syncForm.syncFrequency"
                  :options="syncFrequencyOptions"
                  :placeholder="t('common.pleaseSelect')"
                />
                <n-select
                  v-if="syncForm.syncFrequency === 'WEEKLY'"
                  v-model:value="syncForm.syncWeekday"
                  :options="syncWeekdayOptions"
                  :placeholder="t('common.pleaseSelect')"
                />
              </div>
              <div v-if="syncNextTimeText" class="mt-[4px] text-[12px] text-[var(--primary-8)]">
                {{ syncNextTimeText }}
              </div>
            </div>
          </n-form-item>

          <n-form-item path="syncDepartmentIds" :label="t('system.business.syncSettings.scope')">
            <template #label>
              <div class="flex items-center gap-[4px]">
                <span>{{ t('system.business.syncSettings.scope') }}</span>
                <n-tooltip trigger="hover">
                  <template #trigger>
                    <CrmIcon
                      type="iconicon_help_circle"
                      :size="16"
                      class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
                    />
                  </template>
                  {{ t('system.business.syncSettings.scopeTip') }}
                </n-tooltip>
              </div>
            </template>
            <div class="w-full">
              <n-radio-group v-model:value="syncForm.syncScope">
                <n-radio-button value="ALL">
                  {{ t('system.business.syncSettings.allCompany') }}
                </n-radio-button>
                <n-radio-button value="DEPARTMENT">
                  {{ t('system.business.syncSettings.department') }}
                </n-radio-button>
              </n-radio-group>
              <CrmUserTagSelector
                v-if="syncForm.syncScope === 'DEPARTMENT'"
                v-model:value="syncForm.syncDepartmentIds"
                v-model:selected-list="selectedDepartments"
                class="mt-[8px]"
                :api-type-key="MemberApiTypeEnum.FORM_FIELD"
                :fetch-org-api="fetchSyncDepartmentTree"
                :member-types="departmentMemberTypes"
                :disabled-node-types="[DeptNodeTypeEnum.USER]"
                :drawer-title="t('system.business.syncSettings.selectDepartmentTitle')"
                :placeholder="t('system.business.syncSettings.selectDepartment')"
              />
            </div>
          </n-form-item>
        </template>
      </n-form>
    </n-spin>

    <template #footer>
      <div class="flex w-full items-center justify-between">
        <div
          v-if="activeSettingTab === 'platform' && platformType.includes(form.type)"
          class="ml-[4px] flex items-center gap-[8px]"
        >
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
        <div class="flex flex-1 items-center justify-end gap-[12px]">
          <n-button :disabled="loading" secondary @click="cancel">
            {{ t('common.cancel') }}
          </n-button>
          <n-button
            v-if="activeSettingTab === 'platform'"
            :loading="linkLoading"
            type="primary"
            ghost
            class="n-btn-outline-primary"
            @click="continueLink"
          >
            {{ t('common.testLink') }}
          </n-button>
          <n-button v-if="activeSettingTab === 'platform'" :loading="loading" type="primary" @click="confirmHandler">
            {{ t('common.confirm') }}
          </n-button>
          <n-button
            v-if="activeSettingTab === 'sync'"
            :disabled="!hasAnyPermission(['SYS_ORGANIZATION:SYNC'])"
            :loading="loading"
            type="primary"
            @click="handleSaveSyncSettings"
          >
            {{ t('common.save') }}
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
    NForm,
    NFormItem,
    NInput,
    NRadioButton,
    NRadioGroup,
    NSelect,
    NSpin,
    NSwitch,
    NTabPane,
    NTabs,
    NTooltip,
    SelectOption,
    useMessage,
  } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';
  import dayjs from 'dayjs';

  import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';
  import { MemberApiTypeEnum, MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { DEOrgItem, ThirdPartyDEConfig, ThirdPartyResourceConfig } from '@lib/shared/models/system/business';
  import type { SelectedUsersItem } from '@lib/shared/models/system/module';
  import type {
    SyncCycle,
    SyncFrequency,
    SyncUserScheduleForm,
    SyncWeekday,
    ThirdDepartmentNode,
  } from '@lib/shared/models/system/org';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import type { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  import {
    getDEOrgList,
    getSyncScheduleConfig,
    getSyncThirdOrg,
    saveSyncScheduleConfig,
    testConfigSynchronization,
    updateConfigSynchronization,
  } from '@/api/modules';
  import { defaultThirdPartyConfigMap, platformType } from '@/config/business';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

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
  const activeSettingTab = ref<'platform' | 'sync'>('platform');

  const getAppSecretText = computed(() => {
    if (props.integration?.type === CompanyTypeEnum.DATA_EASE) return 'APP Secret';
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
    qccAddress: [
      {
        trigger: ['input', 'blur'],
        required: true,
        message: t('common.notNull', { value: t('system.business.qichachaAddress') }),
      },
    ],
    qccAccessKey: [
      {
        trigger: ['input', 'blur'],
        required: true,
        message: t('common.notNull', { value: 'Access Key' }),
      },
    ],
    qccSecretKey: [
      {
        trigger: ['input', 'blur'],
        required: true,
        message: t('common.notNull', { value: 'Secret Key' }),
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

  function getDefaultSyncForm(): SyncUserScheduleForm {
    return {
      enable: false,
      syncFrequency: 'DAY',
      syncWeekday: 'MONDAY',
      syncScope: 'ALL',
      syncDepartmentIds: [],
    };
  }
  const syncForm = ref<SyncUserScheduleForm>(getDefaultSyncForm());

  const syncFrequencyOptions = computed<SelectOption[]>(() => [
    { label: t('system.business.syncSettings.hourly'), value: 'HOUR' },
    { label: t('system.business.syncSettings.every6Hours'), value: 'SIX_HOUR' },
    { label: t('system.business.syncSettings.every12Hours'), value: 'TWELVE_HOUR' },
    { label: t('system.business.syncSettings.daily'), value: 'DAY' },
    { label: t('system.business.syncSettings.weekly'), value: 'WEEKLY' },
  ]);
  const syncWeekdayOptions = computed<SelectOption[]>(() => [
    { label: t('system.business.syncSettings.monday'), value: 'MONDAY' },
    { label: t('system.business.syncSettings.tuesday'), value: 'TUESDAY' },
    { label: t('system.business.syncSettings.wednesday'), value: 'WEDNESDAY' },
    { label: t('system.business.syncSettings.thursday'), value: 'THURSDAY' },
    { label: t('system.business.syncSettings.friday'), value: 'FRIDAY' },
    { label: t('system.business.syncSettings.saturday'), value: 'SATURDAY' },
    { label: t('system.business.syncSettings.sunday'), value: 'SUNDAY' },
  ]);
  const departmentMemberTypes = computed(() => [
    {
      label: t('menu.settings.org'),
      value: MemberSelectTypeEnum.ONLY_ORG,
    },
  ]);
  const intervalHoursMap: Partial<Record<SyncFrequency, number>> = {
    HOUR: 1,
    SIX_HOUR: 6,
    TWELVE_HOUR: 12,
  };

  function formatSyncTime(time: dayjs.Dayjs) {
    if (time.isSame(dayjs(), 'day')) {
      return t('system.business.syncSettings.todayAt', { time: time.format('HH:mm') });
    }
    if (time.isSame(dayjs().add(1, 'day'), 'day')) {
      return t('system.business.syncSettings.tomorrowAt', { time: time.format('HH:mm') });
    }
    return time.format('YYYY-MM-DD HH:mm');
  }

  // 计算“多久后”
  function formatSyncTimeDistance(time: dayjs.Dayjs) {
    const minutes = Math.max(time.diff(dayjs(), 'minute'), 1);
    if (minutes < 60) {
      return t('system.business.syncSettings.minutesLater', { count: minutes });
    }
    return t('system.business.syncSettings.hoursLater', { count: Math.ceil(minutes / 60) });
  }

  function getNextIntervalSyncTime(interval: number) {
    const now = dayjs();
    const dayStart = now.startOf('day');
    const elapsedMinutes = now.diff(dayStart, 'minute');
    const intervalMinutes = interval * 60;
    const nextSlotIndex = Math.floor(elapsedMinutes / intervalMinutes) + 1;
    return dayStart.add(nextSlotIndex * intervalMinutes, 'minute');
  }

  const syncNextTimeText = computed(() => {
    if (!syncForm.value.enable) return '';
    if (['DAY', 'WEEKLY'].includes(syncForm.value.syncFrequency)) {
      return t('system.business.syncSettings.dawnExecution');
    }
    const interval = intervalHoursMap[syncForm.value.syncFrequency];
    if (!interval) return '';
    const nextTime = getNextIntervalSyncTime(interval);
    return t('system.business.syncSettings.nextTimeWithDistance', {
      time: formatSyncTime(nextTime),
      distance: formatSyncTimeDistance(nextTime),
    });
  });

  function getSyncCycle() {
    if (syncForm.value.syncFrequency === 'WEEKLY') {
      return syncForm.value.syncWeekday;
    }
    return syncForm.value.syncFrequency as SyncCycle;
  }

  // 转成系统组织架构选择器能识别的树节点格式
  function mapThirdDepartmentTree(nodes: ThirdDepartmentNode[]): CrmTreeNodeData[] {
    return nodes.map((node) => ({
      ...node,
      label: node.name,
      value: node.id,
      nodeType: DeptNodeTypeEnum.ORG,
      children: node.children?.length ? mapThirdDepartmentTree(node.children) : undefined,
    }));
  }

  async function fetchSyncDepartmentTree() {
    const tree = await getSyncThirdOrg(form.value.type);
    return mapThirdDepartmentTree(tree);
  }

  // 用于回显
  function findSelectedDepartments(nodes: CrmTreeNodeData[], ids: string[]) {
    const selected: SelectedUsersItem[] = [];
    const idSet = new Set(ids);
    const walk = (tree: CrmTreeNodeData[]) => {
      tree.forEach((node) => {
        const nodeId = node.id ? String(node.id) : '';
        if (nodeId && idSet.has(nodeId)) {
          selected.push({
            id: nodeId,
            name: String(node.name || node.label || nodeId),
          });
        }
        if (node.children?.length) {
          walk(node.children as CrmTreeNodeData[]);
        }
      });
    };
    walk(nodes);
    return selected;
  }

  const syncSettingsLoading = ref(false);
  const selectedDepartments = ref<SelectedUsersItem[]>([]);
  const syncFormRef = ref<FormInst | null>(null);
  const syncRules = computed<FormRules>(() => ({
    syncDepartmentIds: [
      {
        trigger: ['change', 'blur'],
        validator() {
          if (
            syncForm.value.enable &&
            syncForm.value.syncScope === 'DEPARTMENT' &&
            !syncForm.value.syncDepartmentIds.length
          ) {
            return new Error(t('system.business.syncSettings.selectDepartment'));
          }
          return true;
        },
      },
    ],
  }));

  async function patchSelectedDepartments(ids: string[]) {
    if (!ids.length) {
      selectedDepartments.value = [];
      return;
    }
    try {
      const departments = await fetchSyncDepartmentTree();
      selectedDepartments.value = findSelectedDepartments(departments, ids);
    } catch (e) {
      selectedDepartments.value = ids.map((id) => ({ id, name: id }));
      // eslint-disable-next-line no-console
      console.log(e);
    }
  }

  async function fetchSyncSettings() {
    if (!platformType.includes(form.value.type)) {
      syncForm.value = getDefaultSyncForm();
      selectedDepartments.value = [];
      return;
    }
    try {
      syncSettingsLoading.value = true;
      const config = await getSyncScheduleConfig(form.value.type);
      const syncConfig = config.syncConfig ?? [];
      const isWeekly = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'].includes(
        config.syncCycle || ''
      );
      syncForm.value = {
        enable: !!config.enable,
        syncFrequency: isWeekly ? 'WEEKLY' : ((config.syncCycle || 'DAY') as SyncFrequency),
        syncWeekday: isWeekly ? (config.syncCycle as SyncWeekday) : 'MONDAY',
        syncScope: syncConfig.length ? 'DEPARTMENT' : 'ALL',
        syncDepartmentIds: syncConfig,
      };
      await patchSelectedDepartments(syncConfig);
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    } finally {
      syncSettingsLoading.value = false;
    }
  }

  /** *
   * 保存
   */
  const loading = ref(false);

  async function handleSaveSyncSettings() {
    try {
      await syncFormRef.value?.validate();
      loading.value = true;
      const syncScope = syncForm.value.syncScope === 'DEPARTMENT' ? syncForm.value.syncDepartmentIds : [];
      await saveSyncScheduleConfig({
        enable: syncForm.value.enable,
        syncCycle: getSyncCycle(),
        syncScope,
        resourceType: form.value.type,
      });
      Message.success(t('common.updateSuccess'));
      await fetchSyncSettings();
      showModal.value = false;
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    } finally {
      loading.value = false;
    }
  }

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
      activeSettingTab.value = 'platform';
      syncForm.value = getDefaultSyncForm();
      selectedDepartments.value = [];
      if (showModal.value && props.integration?.type === CompanyTypeEnum.DATA_EASE) {
        fetchDEOrgList();
      }
    },
    { deep: true }
  );

  watch(
    () => activeSettingTab.value,
    (tab) => {
      if (tab === 'sync' && showModal.value) {
        fetchSyncSettings();
      }
    }
  );
</script>

<style lang="less">
  .autoSyncItem {
    .n-form-item-label {
      align-items: start;
    }
  }
</style>
