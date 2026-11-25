<template>
  <CrmCard no-content-padding hide-footer :special-height="licenseStore.expiredDuring ? 64 : 0">
    <div class="message-settings-layout">
      <aside class="message-settings-sidebar">
        <CrmTab
          v-model:active-tab="activeGroup"
          class="message-settings-group-tabs"
          type="segment"
          no-content
          :tab-list="groupTabs"
        />
        <div class="message-settings-category-list">
          <button
            v-for="category in categoryList"
            :key="category.key"
            type="button"
            class="message-settings-category-item"
            :class="{ active: activeCategory === category.key }"
            @click="handleCategoryClick(category)"
          >
            <span class="one-line-text">{{ category.label }}</span>
            <span class="message-settings-category-count">{{ category.count }}</span>
          </button>
        </div>
      </aside>

      <main class="message-settings-main">
        <CrmTable
          v-bind="tableProps"
          v-model:checked-row-keys="selectedIds"
          class="message-settings-table"
          :action-config="{
            baseAction: [
              {
                label: t('system.message.settings.batchChannels'),
                key: 'batchChannels',
                permission: ['SYSTEM_NOTICE:UPDATE'],
              },
            ],
          }"
          @page-change="propsEvent.pageChange"
          @page-size-change="propsEvent.pageSizeChange"
          @sorter-change="propsEvent.sorterChange"
          @filter-change="handleTableFilterChange"
          @batch-action="handleBatchAction"
        >
          <template #actionLeft>
            <n-button v-permission="['SYSTEM_NOTICE:ADD']" type="primary" @click="openCreateScene">
              {{ t('system.message.settings.addScene') }}
            </n-button>
          </template>
          <template #actionRight>
            <CrmSearchInput v-model:value="keyword" :placeholder="t('common.searchByName')" @search="searchScenes" />
          </template>
        </CrmTable>
      </main>
    </div>

    <CrmModal
      v-model:show="showBatchChannelModal"
      class="message-batch-channel-modal"
      size="small"
      :width="600"
      :positive-text="t('system.message.settings.applyChannels')"
      :negative-text="t('common.cancel')"
      @confirm="applyBatchChannels"
    >
      <template #title>
        <div class="crm-modal-title one-line-text flex items-center">
          <span>{{ t('system.message.settings.batchChannelsTitle') }}</span>
          <span class="ml-[4px] text-[var(--text-n4)]">
            {{ t('system.message.settings.batchChannelsSelectedCount', { count: selectedIds.length }) }}
          </span>
        </div>
      </template>
      <div class="message-channel-field">
        <div class="message-channel-label">{{ t('system.message.settings.receiveMethods') }}</div>
        <n-checkbox-group v-model:value="batchChannelKeys" class="message-channel-options">
          <n-checkbox v-for="channel in channelOptions" :key="channel.value" :value="channel.value">
            {{ channel.label }}
          </n-checkbox>
        </n-checkbox-group>
      </div>
    </CrmModal>

    <CrmModal
      v-model:show="showEditor"
      class="message-scene-modal"
      size="large"
      :width="960"
      :positive-text="t('common.save')"
      :negative-text="t('common.cancel')"
      @confirm="saveEditor"
    >
      <template #title>
        <div class="crm-modal-title one-line-text flex items-center gap-[8px]">
          <span>{{
            t(
              editorMode === 'update'
                ? 'system.message.settings.updateSceneTitle'
                : 'system.message.settings.addSceneTitle'
            )
          }}</span>
          <span class="text-[var(--text-n4)]">- {{ getEditorCategoryLabel() }}</span>
        </div>
      </template>
      <n-form
        ref="sceneFormRef"
        :model="editor"
        :rules="sceneFormRules"
        class="message-scene-form"
        label-placement="top"
      >
        <n-form-item :label="t('system.message.settings.sceneName')" path="name" required>
          <n-input v-model:value="editor.name" maxlength="100" :disabled="!editorEditable" />
        </n-form-item>
        <div class="message-scene-basic-fields">
          <n-form-item :label="t('system.message.settings.category')" path="taskType" required>
            <n-select
              v-model:value="editorCategoryType"
              :options="editorCategoryOptions"
              :disabled="
                (editor.group === 'custom' ? editorMode !== 'copy' : editorMode === 'update') || !editorEditable
              "
              :placeholder="t('common.pleaseSelect')"
              @update:value="handleEditorCategoryChange"
            />
          </n-form-item>
          <n-form-item :label="t('system.message.settings.receivers')">
            <MessageReceiverSelector
              v-model:receiver-keys="editor.scopeKeys"
              v-model:receiver-member-ids="editor.receiverMemberIds"
              v-model:receiver-members="editor.receiverMembers"
              v-model:receiver-role-ids="editor.receiverRoleIds"
              v-model:receiver-roles="editor.receiverRoles"
              :groups="scopeOptionGroups"
              :disabled="!editorEditable"
              :placeholder="t('common.pleaseSelect')"
              :specified-users-label="t('system.message.settings.receiver.specifiedUsers')"
              :specified-roles-label="t('system.message.settings.receiver.specifiedRoles')"
            />
          </n-form-item>
          <n-form-item :label="t('system.message.settings.receiveMethods')">
            <MessageReceiverSelector
              v-model:receiver-keys="editor.channelKeys"
              :groups="channelGroups"
              :disabled="!editorEditable"
              :placeholder="t('common.pleaseSelect')"
            />
          </n-form-item>
        </div>
        <n-form-item :label="t('system.message.settings.triggerType')" path="triggerType" required>
          <n-radio-group
            v-model:value="editor.triggerType"
            class="message-trigger-options"
            :disabled="!editorTriggerEditable"
            @update:value="handleTriggerTypeChange"
          >
            <n-radio
              v-for="option in triggerOptions"
              :key="option.value"
              :value="option.value"
              class="message-trigger-card"
              :class="{ selected: editor.triggerType === option.value }"
            >
              <span class="message-trigger-copy">
                <span class="message-trigger-title">{{ option.label }}</span>
                <span class="message-trigger-description">{{ option.description }}</span>
              </span>
            </n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item
          v-if="editor.triggerType === MessageNotificationTriggerEnum.ACTION"
          :label="t('system.message.settings.basisAction')"
          path="actionTypeConfig"
          required
        >
          <n-select
            v-model:value="editor.actionTypeConfig"
            :options="actionOptions"
            :disabled="!editorTriggerEditable"
            @update:value="handleActionTypeChange"
          />
        </n-form-item>
        <n-form-item
          v-else-if="editor.triggerType === MessageNotificationTriggerEnum.SCHEDULE"
          :label="t('system.message.settings.triggerCondition')"
          path="scheduleFieldId"
          required
        >
          <div class="message-rule-panel message-reminder-rule">
            <n-select
              v-model:value="editor.scheduleFieldId"
              class="message-reminder-date"
              :options="dateFieldOptions"
              :disabled="!editorTriggerEditable"
            />
            <span>{{ t('system.message.settings.advanceLabel') }}</span>
            <n-input-number
              v-model:value="editor.scheduleDay"
              class="message-reminder-number"
              :min="0"
              :precision="0"
              :disabled="!editorTriggerEditable"
            />
            <span>{{ t('system.message.settings.reminderUnit.day') }}</span>
            <div class="message-reminder-time-range">
              <n-time-picker
                v-model:value="editor.scheduleTime"
                class="message-reminder-time"
                format="HH"
                :placeholder="t('system.message.settings.startTime')"
                :disabled="!editorTriggerEditable"
                :bordered="false"
                :show-icon="false"
              />
            </div>
            <n-select
              v-model:value="editor.remindType"
              class="message-reminder-frequency"
              :options="reminderTypeOptions"
              :disabled="!editorTriggerEditable"
            />
          </div>
        </n-form-item>
        <n-form-item v-else :label="t('system.message.settings.triggerCondition')" path="conditionFilter" required>
          <FilterContent
            ref="conditionFilterRef"
            v-model:form-model="editor.conditionFilter"
            class="message-scene-condition-filter"
            :config-list="conditionFilterConfigList"
            :max-filter-field-number="1"
            hide-logic-operator
            hide-actions-when-single
            keep-one-line
            :readonly="!editorTriggerEditable"
          />
        </n-form-item>
        <div class="message-preview-section">
          <div class="message-preview-label">{{ t('system.message.settings.notificationPreview') }}</div>
          <div class="message-preview-card">
            <div class="message-preview-heading">
              <CrmTag theme="light" :type="getTriggerTagType(editor.triggerType)">
                {{ triggerOptions.find((option) => option.value === editor.triggerType)?.label }}
              </CrmTag>
              <span class="message-preview-name">
                {{ editor.name || getEditorCategoryLabel() }}
                <span class="message-preview-dot"></span>
              </span>
            </div>
            <div class="message-preview-content">{{ scenePreviewText }}</div>
            <div class="message-preview-footer">
              <span>{{ getEditorCategoryLabel() }}</span>
              <span>{{ dayjs().format('YYYY-MM-DD HH:mm:ss') }}</span>
            </div>
          </div>
        </div>
      </n-form>
    </CrmModal>
  </CrmCard>
</template>

<script lang="ts" setup>
  import {
    type FormRules,
    NButton,
    NCheckbox,
    NCheckboxGroup,
    NForm,
    NFormItem,
    NInput,
    NInputNumber,
    NRadio,
    NRadioGroup,
    NSelect,
    NSwitch,
    NTimePicker,
    NTooltip,
    useMessage,
  } from 'naive-ui';
  import dayjs from 'dayjs';

  import { OperatorEnum } from '@lib/shared/enums/commonEnum';
  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import {
    MessageNotificationCategoryEnum,
    MessageNotificationChannelEnum,
    MessageNotificationEventEnum,
    MessageNotificationReceiverEnum,
    MessageNotificationTriggerEnum,
    MessageNotificationTypeEnum,
  } from '@lib/shared/enums/messageNotificationEnum';
  import { MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { ProcessStatusEnum } from '@lib/shared/enums/process';
  import { SpecialColumnEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { formatTimeValue, getCityPath, getIndustryPath } from '@lib/shared/method';
  import type {
    MessageTaskCountDTO,
    MessageTaskPageParams,
    MessageTaskResponse,
    MessageTaskSaveParams,
    MessageTaskTriggerConfig,
  } from '@lib/shared/models/system/message';
  import type { SelectedUsersItem } from '@lib/shared/models/system/module';

  import FilterContent from '@/components/pure/crm-advance-filter/components/filterContent.vue';
  import type { FilterForm, FilterFormItem } from '@/components/pure/crm-advance-filter/type';
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import type { FormCreateField } from '@/components/business/crm-form-create/types';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import { timeOptions, unitOptions } from '@/components/business/crm-time-range-picker/config';
  import MessageReceiverSelector from './messageReceiverSelector.vue';

  import {
    addMessageTask,
    batchUpdateMessageTaskReceiveMethod,
    deleteMessageTask,
    enableMessageTask,
    getClueFormConfig,
    getContractFormConfig,
    getContractStatusConfig,
    getCustomerFollowRecordFormConfig,
    getCustomerFormConfig,
    getCustomFormDetail,
    getFieldDeptTree,
    getMessageTaskCustomFormTypes,
    getMessageTaskPage,
    getMessageTaskSystemTypes,
    getOpportunityStageConfig,
    getOptFormConfig,
    getOrderFormConfig,
    getOrderStatusConfig,
    getUserOptions,
    updateMessageTask,
  } from '@/api/modules/index.js';
  import { getNotificationTriggerFilterConfigList } from '@/config/messageFilterConfig';
  import { processStatusOptions } from '@/config/process';
  import useModal from '@/hooks/useModal';
  import useLicenseStore from '@/store/modules/setting/license';
  import { hasAnyPermission } from '@/utils/permission';

  type SceneGroup = 'standard' | 'custom';
  type EditorMode = 'create' | 'update' | 'copy';

  interface SceneRow extends MessageTaskResponse {
    group: SceneGroup;
    enabled: boolean;
    channelKeys: string[];
    sceneCategoryType: string;
  }

  interface SceneCategoryOption {
    key: string;
    label: string;
    count: number;
  }

  interface SceneActionOption {
    value: MessageNotificationEventEnum | 'CUSTOM_FORM_ADD' | 'CUSTOM_FORM_UPDATE' | 'CUSTOM_FORM_DELETE';
    labelKey?: string;
    categoryLabelKey?: string;
  }

  interface SceneEditor {
    name: string;
    taskType: string;
    group: SceneGroup;
    triggerType: string;
    config: MessageTaskTriggerConfig;
    actionTypeConfig: string;
    scheduleFieldId: string;
    scheduleDay: number;
    scheduleTime: number | null;
    remindType: string;
    conditionFilter: FilterForm;
    template: string;
    templateValue: string[];
    scopeKeys: string[];
    receiverMemberIds: string[];
    receiverMembers: SelectedUsersItem[];
    receiverRoleIds: string[];
    receiverRoles: SelectedUsersItem[];
    preservedScopeIds: string[];
    channelKeys: string[];
    preservedReceiveMethods: string[];
  }

  const { t } = useI18n();
  const Message = useMessage();
  const { openModal } = useModal();
  const licenseStore = useLicenseStore();
  const canUpdate = computed(() => hasAnyPermission(['SYSTEM_NOTICE:UPDATE']));
  const canAdd = computed(() => hasAnyPermission(['SYSTEM_NOTICE:ADD']));
  const activeGroup = ref<SceneGroup>('standard');
  const activeCategory = ref('');
  const editorCategoryType = ref('');
  type StageModule = 'opportunity' | 'contract' | 'order';
  type StageFilterOptions = NonNullable<FilterFormItem['selectProps']>['options'];
  const stageOptionsByModule = reactive<Partial<Record<StageModule, StageFilterOptions>>>({});
  const stageOptionsLoading = new Map<StageModule, Promise<void>>();
  const keyword = ref('');
  const selectedIds = ref<string[]>([]);
  const enableLoadingIds = ref<Set<string>>(new Set());
  const showBatchChannelModal = ref(false);
  const batchChannelKeys = ref<string[]>([]);
  const showEditor = ref(false);
  const editorMode = ref<EditorMode>('update');
  const editorSource = ref<SceneRow | null>(null);
  const isCustomScene = ref(true);
  function parseScheduleTime(value?: string) {
    const [hours = 8, minutes = 0] = (value ?? '08:00:00').replace(/：/g, ':').split(':').map(Number);
    const date = new Date();
    date.setHours(hours, minutes, 0, 0);
    return date.getTime();
  }

  const editor = reactive<SceneEditor>({
    name: '',
    taskType: '',
    group: 'standard',
    triggerType: MessageNotificationTriggerEnum.ACTION,
    config: {},
    actionTypeConfig: '',
    scheduleFieldId: 'until_time',
    scheduleDay: 0,
    scheduleTime: parseScheduleTime(),
    remindType: 'ONLY_ONE',
    conditionFilter: {
      searchMode: 'AND',
      list: [{ dataIndex: null, type: FieldTypeEnum.INPUT, value: null }],
    },
    template: '[]',
    templateValue: [],
    scopeKeys: [],
    receiverMemberIds: [],
    receiverMembers: [],
    receiverRoleIds: [],
    receiverRoles: [],
    preservedScopeIds: [],
    channelKeys: [],
    preservedReceiveMethods: [],
  });
  const sceneFormRef = ref<InstanceType<typeof NForm> | null>(null);
  const conditionFilterRef = ref<InstanceType<typeof FilterContent> | null>(null);
  const sceneFormRules = computed<FormRules>(() => ({
    name: [
      {
        required: true,
        whitespace: true,
        message: t('system.message.settings.sceneNameRequired'),
        trigger: 'blur',
      },
    ],
    taskType: [{ required: true, message: t('common.pleaseSelect'), trigger: 'change' }],
    triggerType: [{ required: true, message: t('common.pleaseSelect'), trigger: 'change' }],
    actionTypeConfig: [
      {
        validator: () =>
          editor.triggerType !== MessageNotificationTriggerEnum.ACTION || Boolean(editor.actionTypeConfig),
        message: t('system.message.settings.sceneActionRequired'),
        trigger: 'change',
      },
    ],
    scheduleFieldId: [
      {
        validator: () =>
          editor.triggerType !== MessageNotificationTriggerEnum.SCHEDULE ||
          Boolean(
            editor.scheduleFieldId && editor.scheduleDay >= 0 && editor.scheduleTime !== null && editor.remindType
          ),
        message: t('system.message.settings.sceneConditionRequired'),
        trigger: 'change',
      },
    ],
    conditionFilter: [
      {
        validator: () =>
          editor.triggerType !== MessageNotificationTriggerEnum.FIELD ||
          editor.conditionFilter.list.some((condition) => {
            const emptyOperator = [
              OperatorEnum.EMPTY,
              OperatorEnum.NOT_EMPTY,
              OperatorEnum.NEW_NOT_EQUALS_OLD,
            ].includes(condition.operator as OperatorEnum);
            return condition.dataIndex && condition.operator && (emptyOperator || condition.value !== null);
          }),
        message: t('system.message.settings.sceneConditionRequired'),
        trigger: 'change',
      },
    ],
  }));
  const triggerFields = ref<FormCreateField[]>([]);

  const groupTabs = computed(() => [
    { name: 'standard', tab: t('system.message.settings.standardFeatures') },
    { name: 'custom', tab: t('system.message.settings.customForms') },
  ]);

  const systemCategoryLabelKeyMap: Record<string, string> = {
    [MessageNotificationCategoryEnum.CLUE]: 'lead',
    [MessageNotificationCategoryEnum.CUSTOMER]: 'customer',
    CUSTOMER_CONTACT: 'contact',
    CUSTOMER_CONCAT: 'contact',
    [MessageNotificationCategoryEnum.OPPORTUNITY]: 'opportunity',
    BUSINESS: 'opportunity',
    BUSINESS_QUOTATION: 'quotation',
    QUOTATION: 'quotation',
    [MessageNotificationCategoryEnum.CONTRACT]: 'contract',
    CONTRACT_PAYMENT_RECORD: 'paymentRecord',
    CONTRACT_PAYMENT_PLAN: 'paymentPlan',
    CONTRACT_INVOICE: 'invoice',
    [MessageNotificationCategoryEnum.ORDER]: 'order',
    [MessageNotificationCategoryEnum.APPROVAL]: 'todo',
    [MessageNotificationCategoryEnum.FOLLOW_UP]: 'follow',
    FOLLOW_RECORD: 'followRecord',
    FOLLOW_PLAN: 'followPlan',
  };
  const systemSubcategoryMap: Record<string, string[]> = {
    customer: ['CUSTOMER', 'CUSTOMER_CONTACT'],
    opportunity: ['OPPORTUNITY', 'BUSINESS_QUOTATION'],
    contract: ['CONTRACT', 'CONTRACT_PAYMENT_RECORD', 'CONTRACT_PAYMENT_PLAN', 'CONTRACT_INVOICE'],
    follow: ['FOLLOW_RECORD', 'FOLLOW_PLAN'],
  };
  const sceneCategoryFilterKey = 'sceneCategoryType';
  const systemCategoryActionKeyMap: Record<string, string> = {
    CUSTOMER_CONTACT: 'customer',
    CUSTOMER_CONCAT: 'customer',
    BUSINESS_QUOTATION: 'opportunity',
    QUOTATION: 'opportunity',
    CONTRACT_PAYMENT_RECORD: 'contract',
    CONTRACT_PAYMENT_PLAN: 'contract',
    CONTRACT_INVOICE: 'contract',
    FOLLOW_RECORD: 'follow',
    FOLLOW_PLAN: 'follow',
  };
  const systemCategories = ref<SceneCategoryOption[]>([]);
  const customFormCategories = ref<SceneCategoryOption[]>([]);
  const specialScopeIds = new Set<string>([
    MessageNotificationReceiverEnum.OWNER,
    MessageNotificationReceiverEnum.CREATE_USER,
    MessageNotificationReceiverEnum.COMMANDER,
    MessageNotificationReceiverEnum.SUPERVISOR,
  ]);
  const channelKeyToEnum: Record<string, MessageNotificationChannelEnum> = {
    inApp: MessageNotificationChannelEnum.SYSTEM,
    email: MessageNotificationChannelEnum.EMAIL,
    wecom: MessageNotificationChannelEnum.WECOM,
    dingtalk: MessageNotificationChannelEnum.DINGTALK,
    lark: MessageNotificationChannelEnum.LARK,
  };
  const channelOptions = computed(() => [
    { label: t('system.message.settings.channel.inApp'), value: 'inApp' },
    { label: t('system.message.settings.channel.email'), value: 'email' },
    { label: t('system.message.settings.channel.wecom'), value: 'wecom' },
    { label: t('system.message.settings.channel.dingtalk'), value: 'dingtalk' },
    { label: t('system.message.settings.channel.lark'), value: 'lark' },
  ]);
  const channelGroups = computed(() => [
    { key: 'channels', label: t('system.message.settings.receiveMethods'), options: channelOptions.value },
  ]);
  function getCategoryActionKey(taskType: string) {
    const normalizedTaskType = taskType.toUpperCase();
    return (
      systemCategoryActionKeyMap[normalizedTaskType] ??
      systemCategoryLabelKeyMap[normalizedTaskType] ??
      taskType.toLowerCase()
    );
  }
  const receiverOptions = computed(() => [
    { label: t('system.message.settings.receiver.owner'), value: MessageNotificationReceiverEnum.OWNER },
    { label: t('system.message.settings.receiver.creator'), value: MessageNotificationReceiverEnum.CREATE_USER },
    { label: t('system.message.settings.receiver.manager'), value: MessageNotificationReceiverEnum.SUPERVISOR },
    { label: t('system.message.settings.receiver.departmentLead'), value: MessageNotificationReceiverEnum.COMMANDER },
  ]);
  const customReceiverOptions = computed(() =>
    triggerFields.value
      .filter((field) =>
        [
          FieldTypeEnum.MEMBER,
          FieldTypeEnum.MEMBER_MULTIPLE,
          FieldTypeEnum.USER_TAG_SELECTOR,
          FieldTypeEnum.USER_SELECT,
        ].includes(field.type)
      )
      .map((field) => ({ label: field.name, value: field.id }))
  );
  const scopeOptionGroups = computed(() => [
    {
      key: 'system',
      label: t('system.message.settings.receiverGroup.system'),
      options: receiverOptions.value,
    },
    {
      key: 'custom',
      label: t('system.message.settings.receiverGroup.custom'),
      options: customReceiverOptions.value,
    },
    {
      key: 'specified',
      label: t('system.message.settings.receiverGroup.specified'),
      options: [
        { label: t('system.message.settings.receiver.specifiedUsers'), value: 'specifiedUsers' },
        { label: t('system.message.settings.receiver.specifiedRoles'), value: 'specifiedRoles' },
      ],
    },
  ]);
  const categoryList = computed(() =>
    activeGroup.value === 'standard' ? systemCategories.value : customFormCategories.value
  );
  function getCategoryLabel(taskType: string) {
    const category = categoryList.value.find((item) => item.key === taskType);
    if (category?.label) return category.label;
    const key = systemCategoryLabelKeyMap[taskType.toUpperCase()];
    return key ? t(`system.message.settings.category.${key}`) : taskType;
  }

  const sceneCategoryFilterOptions = computed(() => {
    if (activeGroup.value !== 'standard') return [];
    return (systemSubcategoryMap[getCategoryActionKey(activeCategory.value)] ?? []).map((taskType) => ({
      label: getCategoryLabel(taskType),
      value: taskType,
    }));
  });

  function getEditorCategoryLabel() {
    return getCategoryLabel(editorCategoryType.value || editor.taskType);
  }

  function getDefaultEditorCategoryType(taskType: string) {
    return systemSubcategoryMap[getCategoryActionKey(taskType)]?.[0] ?? taskType;
  }

  const editorCategoryOptions = computed(() => {
    if (editor.group === 'custom') {
      const categories =
        editorMode.value === 'create'
          ? customFormCategories.value.filter((category) => category.key === activeCategory.value)
          : customFormCategories.value;
      return categories.map((category) => ({ label: category.label, value: category.key }));
    }

    const categoryKey = getCategoryActionKey(editor.taskType);
    const subcategories = systemSubcategoryMap[categoryKey] ?? [editor.taskType];
    return subcategories.map((taskType) => ({ label: getCategoryLabel(taskType), value: taskType }));
  });

  function mapSystemCategories(items: MessageTaskCountDTO[] = []) {
    return items.map((item) => {
      const taskType = item.taskType.toUpperCase();
      const categoryKey = systemCategoryLabelKeyMap[taskType];
      return {
        key: item.taskType,
        label: categoryKey ? t(`system.message.settings.category.${categoryKey}`) : item.name || item.taskType,
        count: Number(item.count ?? 0),
      };
    });
  }

  function formatScheduleTime(value: number | null) {
    const date = new Date(value ?? parseScheduleTime());
    const hours = String(date.getHours()).padStart(2, '0');
    return `${hours}:00`;
  }

  function getScheduleReminderTooltip(row: SceneRow) {
    const schedule = row.config?.scheduleTypeConfig;
    const dateFieldId = schedule?.fieldId ?? 'until_time';
    const dateFieldLabelMap: Record<string, string> = {
      until_time: t('system.message.settings.dateField.expiration'),
      nextFollowUp: t('system.message.settings.dateField.nextFollowUp'),
      createdAt: t('system.message.settings.dateField.createdAt'),
    };
    const frequencyMap: Record<string, string> = {
      ONLY_ONE: t('system.message.settings.reminderFrequency.once'),
      EVERY_DAY: t('system.message.settings.reminderFrequency.daily'),
    };
    const time = schedule?.time?.slice(0, 5) || '--:--';

    return t('system.message.settings.reminderScheduleTooltip', {
      dateField: dateFieldLabelMap[dateFieldId] ?? dateFieldId,
      days: Number(schedule?.day ?? 0),
      dayUnit: t('system.message.settings.reminderUnit.day'),
      time,
      frequency: frequencyMap[schedule?.remindType ?? ''] ?? schedule?.remindType ?? '',
    });
  }

  const userNameById = ref<Record<string, string>>({});
  const departmentNameById = ref<Record<string, string>>({});

  function getConditionValueLabel(
    value: unknown,
    conditionField?: FilterFormItem,
    selectedRows: unknown[] = [],
    selectedUserList: SelectedUsersItem[] = []
  ) {
    if (value === null || value === undefined || value === '') return t('system.message.settings.preview.fieldValue');
    const dynamicField = triggerFields.value.find((field) => field.id === String(conditionField?.dataIndex));
    const fieldType = conditionField?.type ?? dynamicField?.type;
    const flattenTreeOptions = (items: Record<string, any>[]): Record<string, unknown>[] =>
      items.flatMap((item) => [item, ...flattenTreeOptions((item.children ?? []) as Record<string, any>[])]);
    const options = [
      ...(conditionField?.selectProps?.options ?? []),
      ...(dynamicField?.options ?? []),
      ...(dynamicField?.initialOptions ?? []),
      ...Object.entries(userNameById.value).map(([id, name]) => ({ id, name })),
      ...Object.entries(departmentNameById.value).map(([id, name]) => ({ id, name })),
      ...selectedRows,
      ...selectedUserList,
      ...flattenTreeOptions((conditionField?.treeSelectProps?.options ?? []) as Record<string, any>[]),
    ] as Record<string, unknown>[];

    function formatDateValue(item: unknown): string {
      const dateValue = String(item);
      if (fieldType === FieldTypeEnum.TIME_RANGE_PICKER && conditionField?.operator === OperatorEnum.DYNAMICS) {
        const [rangeType, amount, unit] = dateValue.split(',');
        if (rangeType === 'CUSTOM') {
          const unitLabel = unitOptions.find((option) => option.value === unit)?.label ?? unit;
          return `${t('common.custom')} (${amount} ${unitLabel})`;
        }
        return timeOptions.find((option) => option.value === rangeType)?.label ?? dateValue;
      }

      return dateValue
        .split(',')
        .map((date) => {
          const timestamp = Number(date);
          if (Number.isFinite(timestamp) && date.trim()) {
            return formatTimeValue(timestamp, dynamicField?.dateType);
          }
          const parsedDate = dayjs(date);
          if (!parsedDate.isValid()) return date;
          if (dynamicField?.dateType === 'month') return parsedDate.format('YYYY-MM');
          if (dynamicField?.dateType === 'date') return parsedDate.format('YYYY-MM-DD');
          return parsedDate.format('YYYY-MM-DD HH:mm:ss');
        })
        .join(' - ');
    }

    function formatOptionValue(item: unknown): string {
      if (item === null || item === undefined) return '';
      const option = typeof item === 'object' ? (item as Record<string, unknown>) : undefined;
      const optionValue = option?.value ?? option?.id ?? option?.key ?? item;
      if (fieldType === FieldTypeEnum.LOCATION) {
        const cityPath = getCityPath(String(optionValue), dynamicField?.scope);
        if (cityPath) return cityPath;
      }
      if (fieldType === FieldTypeEnum.INDUSTRY) {
        const industryPath = getIndustryPath(String(optionValue));
        if (industryPath) return industryPath;
      }
      if ([FieldTypeEnum.DATE_TIME, FieldTypeEnum.TIME_RANGE_PICKER].includes(fieldType as FieldTypeEnum)) {
        return formatDateValue(optionValue);
      }

      const matchedOption = options.find((candidate) => {
        const candidateValue = candidate.value ?? candidate.id ?? candidate.key ?? candidate.code;
        return candidateValue !== undefined && String(candidateValue) === String(optionValue);
      });
      const label = matchedOption?.label ?? matchedOption?.name ?? matchedOption?.title ?? matchedOption?.text;
      if (label !== null && label !== undefined) return String(label);
      if (option) {
        return String(
          option.label ?? option.name ?? option.title ?? option.text ?? option.value ?? option.id ?? option.key ?? ''
        );
      }
      return String(item);
    }

    if (Array.isArray(value)) {
      const formattedValues = value.map(formatOptionValue).filter(Boolean);
      return [FieldTypeEnum.DATE_TIME, FieldTypeEnum.TIME_RANGE_PICKER].includes(fieldType as FieldTypeEnum)
        ? formattedValues.join(' - ')
        : formattedValues.join(', ');
    }
    if (typeof value === 'object') {
      const option = value as Record<string, unknown>;
      return formatOptionValue(option);
    }
    return formatOptionValue(value);
  }

  const triggerOptions = computed(() => [
    {
      label: t('system.message.settings.trigger.action'),
      value: MessageNotificationTriggerEnum.ACTION,
      description: t('system.message.settings.triggerDescription.action'),
    },
    {
      label: t('system.message.settings.trigger.reminder'),
      value: MessageNotificationTriggerEnum.SCHEDULE,
      description: t('system.message.settings.triggerDescription.reminder'),
    },
    {
      label: t('system.message.settings.trigger.condition'),
      value: MessageNotificationTriggerEnum.FIELD,
      description: t('system.message.settings.triggerDescription.condition'),
    },
  ]);
  function getCrudActionOptions(
    createEvent: MessageNotificationEventEnum,
    updateEvent: MessageNotificationEventEnum,
    deleteEvent: MessageNotificationEventEnum,
    categoryLabelKey?: string
  ): SceneActionOption[] {
    return [
      { value: createEvent, labelKey: 'createCategory', categoryLabelKey },
      { value: updateEvent, labelKey: 'updateCategory', categoryLabelKey },
      { value: deleteEvent, labelKey: 'deleteCategory', categoryLabelKey },
    ];
  }

  const categoryActionOptions: Record<string, SceneActionOption[]> = {
    lead: [
      ...getCrudActionOptions(
        MessageNotificationEventEnum.CLUE_ADD,
        MessageNotificationEventEnum.CLUE_UPDATE,
        MessageNotificationEventEnum.CLUE_DELETED
      ),
      { value: MessageNotificationEventEnum.CLUE_AUTOMATIC_MOVE_POOL, labelKey: 'moveLeadPoolSystem' },
      { value: MessageNotificationEventEnum.CLUE_MOVED_POOL, labelKey: 'moveLeadPoolManual' },
      { value: MessageNotificationEventEnum.CLUE_CONVERT_CUSTOMER, labelKey: 'convertToCustomer' },
      { value: MessageNotificationEventEnum.CLUE_CONVERT_BUSINESS, labelKey: 'convertToOpportunity' },
      { value: MessageNotificationEventEnum.TRANSFER_CLUE, labelKey: 'transferLead' },
      { value: MessageNotificationEventEnum.CLUE_DISTRIBUTED, labelKey: 'assignLead' },
      { value: MessageNotificationEventEnum.CLUE_RECEIVE, labelKey: 'claimLead' },
    ],
    customer: [
      ...getCrudActionOptions(
        MessageNotificationEventEnum.CUSTOMER_ADD,
        MessageNotificationEventEnum.CUSTOMER_UPDATE,
        MessageNotificationEventEnum.CUSTOMER_DELETED
      ),
      { value: MessageNotificationEventEnum.CUSTOMER_COLLABORATION_ADD, labelKey: 'createCollaboratorSystem' },
      {
        value: MessageNotificationEventEnum.CUSTOMER_COLLABORATION_ADD_MANUAL,
        labelKey: 'createCollaboratorManual',
      },
      { value: MessageNotificationEventEnum.CUSTOMER_TRANSFERRED_CUSTOMER, labelKey: 'transferCustomer' },
      {
        value: MessageNotificationEventEnum.CUSTOMER_AUTOMATIC_MOVE_HIGH_SEAS,
        labelKey: 'moveCustomerToPublicPoolSystem',
      },
      {
        value: MessageNotificationEventEnum.CUSTOMER_MOVED_HIGH_SEAS,
        labelKey: 'moveCustomerToPublicPoolManual',
      },
      { value: MessageNotificationEventEnum.CUSTOMER_RECEIVE, labelKey: 'claimCustomer' },
      { value: MessageNotificationEventEnum.HIGH_SEAS_CUSTOMER_DISTRIBUTED, labelKey: 'assignCustomer' },
    ],
    opportunity: [
      ...getCrudActionOptions(
        MessageNotificationEventEnum.BUSINESS_ADD,
        MessageNotificationEventEnum.BUSINESS_UPDATE,
        MessageNotificationEventEnum.BUSINESS_DELETED
      ),
      { value: MessageNotificationEventEnum.BUSINESS_TRANSFER, labelKey: 'transferOpportunity' },
    ],
    contract: getCrudActionOptions(
      MessageNotificationEventEnum.CONTRACT_ADD,
      MessageNotificationEventEnum.CONTRACT_UPDATE,
      MessageNotificationEventEnum.CONTRACT_DELETE
    ),
    order: getCrudActionOptions(
      MessageNotificationEventEnum.ORDER_ADD,
      MessageNotificationEventEnum.ORDER_UPDATE,
      MessageNotificationEventEnum.ORDER_DELETE
    ),
    follow: [
      ...getCrudActionOptions(
        MessageNotificationEventEnum.FOLLOW_UP_RECORD_ADD,
        MessageNotificationEventEnum.FOLLOW_UP_RECORD_UPDATE,
        MessageNotificationEventEnum.FOLLOW_UP_RECORD_DELETE,
        'system.message.settings.category.followRecord'
      ),
      { value: MessageNotificationEventEnum.FOLLOW_UP_RECORD_COMMENT, labelKey: 'comment' },
    ],
    todo: [
      {
        value: MessageNotificationEventEnum.APPROVAL_TODO,
        labelKey: 'createCategory',
        categoryLabelKey: 'system.message.settings.category.todo',
      },
      { value: MessageNotificationEventEnum.APPROVAL_CC, labelKey: 'cc' },
      { value: MessageNotificationEventEnum.APPROVAL_COMMENT, labelKey: 'comment' },
    ],
  };
  const categoryEventPrefixes: Record<string, string[]> = {
    lead: ['CLUE_', 'TRANSFER_CLUE'],
    customer: ['CUSTOMER_', 'HIGH_SEAS_CUSTOMER_'],
    opportunity: ['BUSINESS_'],
    contract: ['CONTRACT_', 'INVOICE_'],
    order: ['ORDER_'],
    follow: ['FOLLOW_UP_'],
    todo: ['APPROVAL_'],
  };
  const categorySpecificEventPrefixes: Record<string, string[]> = {
    CUSTOMER_CONTACT: ['CUSTOMER_CONCAT_'],
    CUSTOMER_CONCAT: ['CUSTOMER_CONCAT_'],
    BUSINESS_QUOTATION: ['BUSINESS_QUOTATION_'],
    QUOTATION: ['BUSINESS_QUOTATION_'],
    CONTRACT_PAYMENT_RECORD: ['CONTRACT_RECORD_'],
    CONTRACT_PAYMENT_PLAN: ['CONTRACT_PAYMENT_'],
    CONTRACT_INVOICE: ['INVOICE_'],
    FOLLOW_RECORD: ['FOLLOW_UP_RECORD_'],
    FOLLOW_PLAN: ['FOLLOW_UP_PLAN_'],
  };
  const categoryExcludedEventPrefixes: Record<string, string[]> = {
    CUSTOMER: ['CUSTOMER_CONCAT_'],
    OPPORTUNITY: ['BUSINESS_QUOTATION_'],
    CONTRACT: ['CONTRACT_PAYMENT_', 'CONTRACT_RECORD_', 'INVOICE_'],
  };
  function isCategoryEvent(event: string, categoryType: string) {
    const normalizedCategory = categoryType.toUpperCase();
    const categoryKey = getCategoryActionKey(categoryType);
    const prefixes = categorySpecificEventPrefixes[normalizedCategory] ?? categoryEventPrefixes[categoryKey] ?? [];
    const excludedPrefixes = categoryExcludedEventPrefixes[normalizedCategory] ?? [];
    return (
      prefixes.some((prefix) => event.startsWith(prefix)) &&
      !excludedPrefixes.some((prefix) => event.startsWith(prefix))
    );
  }
  function getSceneCategoryType(taskType: string, actionTypeConfig?: string) {
    const action = actionTypeConfig?.toUpperCase() ?? '';
    const subcategory = Object.entries(categorySpecificEventPrefixes).find(([, prefixes]) =>
      prefixes.some((prefix) => action.startsWith(prefix))
    )?.[0];
    return subcategory ?? taskType;
  }

  function getSceneCategoryLabel(task: SceneRow) {
    return getCategoryLabel(getSceneCategoryType(task.taskType, task.config?.actionTypeConfig));
  }

  const customActionOptions: SceneActionOption[] = [
    { value: 'CUSTOM_FORM_ADD', labelKey: 'create' },
    { value: 'CUSTOM_FORM_UPDATE', labelKey: 'update' },
    { value: 'CUSTOM_FORM_DELETE', labelKey: 'delete' },
  ];
  const actionOptions = computed(() => {
    const selectedCategoryType = editorCategoryType.value || editor.taskType;
    const categoryKey = getCategoryActionKey(selectedCategoryType);
    const category = getCategoryLabel(selectedCategoryType);
    const definitions =
      editor.group === 'custom'
        ? customActionOptions
        : Object.values(MessageNotificationEventEnum)
            .filter((event) => isCategoryEvent(event, selectedCategoryType))
            .map((value) => {
              const definition = categoryActionOptions[categoryKey]?.find((option) => option.value === value);
              return definition ?? { value, labelKey: undefined };
            });
    const options: { value: string; label: string }[] = definitions.map((option) => {
      let label: string;
      if (!option.labelKey) {
        label = t(`system.message.settings.action.event.${option.value}`);
      } else if (option.categoryLabelKey) {
        label = t(`system.message.settings.action.${option.labelKey}`, {
          category: t(option.categoryLabelKey),
        });
      } else if (option.labelKey.endsWith('Category')) {
        label = t(`system.message.settings.action.${option.labelKey}`, { category });
      } else {
        label = t(`system.message.settings.action.${option.labelKey}`);
      }
      return {
        value: option.value,
        label,
      };
    });
    if (editor.actionTypeConfig && !options.some((option) => option.value === editor.actionTypeConfig)) {
      options.unshift({ value: editor.actionTypeConfig, label: editor.actionTypeConfig });
    }
    return options;
  });
  function getDefaultActionTypeConfig() {
    if (editor.group === 'custom') {
      return customActionOptions[0]?.value ?? '';
    }
    const selectedCategoryType = editorCategoryType.value || editor.taskType;
    return (
      Object.values(MessageNotificationEventEnum).find((event) => isCategoryEvent(event, selectedCategoryType)) ?? ''
    );
  }
  function getStageModule(categoryType: string): StageModule | undefined {
    switch (categoryType.toUpperCase()) {
      case 'OPPORTUNITY':
      case 'BUSINESS':
        return 'opportunity';
      case 'CONTRACT':
        return 'contract';
      case 'ORDER':
        return 'order';
      default:
        return undefined;
    }
  }
  async function loadNotificationStageOptions(categoryType: string) {
    const module = getStageModule(categoryType);
    if (!module || stageOptionsByModule[module]) return;
    const pendingRequest = stageOptionsLoading.get(module);
    if (pendingRequest) return pendingRequest;

    const request = (async () => {
      try {
        let config;
        if (module === 'opportunity') {
          config = await getOpportunityStageConfig();
        } else if (module === 'contract') {
          config = await getContractStatusConfig();
        } else {
          config = await getOrderStatusConfig();
        }
        stageOptionsByModule[module] =
          config.stageConfigList?.map((stage) => ({ label: stage.name, value: stage.id })) || [];
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    })();
    stageOptionsLoading.set(module, request);
    await request;
    stageOptionsLoading.delete(module);
  }
  const conditionFilterConfigList = computed<FilterFormItem[]>(() => {
    const fields: FilterFormItem[] = triggerFields.value.length
      ? triggerFields.value
          .filter(
            (field) =>
              ![
                FieldTypeEnum.DIVIDER,
                FieldTypeEnum.SERIAL_NUMBER,
                FieldTypeEnum.SUB_PRICE,
                FieldTypeEnum.SUB_PRODUCT,
              ].includes(field.type)
          )
          .map((field) => {
            let filterDisplayType = field.type;
            if (field.type === FieldTypeEnum.FORMULA) {
              filterDisplayType =
                field.formulaResultFormat === 'number' ? FieldTypeEnum.INPUT_NUMBER : FieldTypeEnum.INPUT;
            } else if (field.type === FieldTypeEnum.STATISTIC) {
              filterDisplayType = FieldTypeEnum.INPUT_NUMBER;
            }
            const selectProps = [
              FieldTypeEnum.SELECT,
              FieldTypeEnum.SELECT_MULTIPLE,
              FieldTypeEnum.RADIO,
              FieldTypeEnum.CHECKBOX,
            ].includes(field.type)
              ? {
                  options: field.options?.map(({ disabled: _disabled, ...option }) => option),
                  multiple: true,
                }
              : undefined;
            const dataSourceProps = [FieldTypeEnum.DATA_SOURCE, FieldTypeEnum.DATA_SOURCE_MULTIPLE].includes(field.type)
              ? { dataSourceType: field.dataSourceType, maxTagCount: 'responsive' as const }
              : undefined;

            return {
              dataIndex: field.id,
              title: field.name,
              type: field.type,
              filterDisplayType,
              ...(selectProps ? { selectProps } : {}),
              ...(dataSourceProps ? { dataSourceProps } : {}),
            };
          })
      : [];

    const selectedCategoryType = editorCategoryType.value || editor.taskType;
    const stageModule = getStageModule(selectedCategoryType);
    const builtInFields =
      editor.group === 'standard'
        ? getNotificationTriggerFilterConfigList(selectedCategoryType, {
            approvalStatusOptions: processStatusOptions.filter((option) => option.value !== ProcessStatusEnum.NONE),
            stageOptions: stageModule ? stageOptionsByModule[stageModule] : undefined,
          })
        : [];
    const fieldMap = new Map<string, FilterFormItem>();
    [...builtInFields, ...fields].forEach((field) => {
      if (field.dataIndex) fieldMap.set(String(field.dataIndex), field);
    });
    return [...fieldMap.values()];
  });
  let userOptionsLoaded = false;
  let userOptionsLoading = false;
  let departmentOptionsLoaded = false;
  let departmentOptionsLoading = false;

  async function loadUserNameOptions() {
    if (userOptionsLoaded || userOptionsLoading) return;
    userOptionsLoading = true;
    try {
      const users = await getUserOptions({ includeDisabled: true });
      userNameById.value = Object.fromEntries(
        users.map((user: { id: string | number; name: string }) => [String(user.id), user.name])
      );
      userOptionsLoaded = true;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      userOptionsLoading = false;
    }
  }

  async function loadDepartmentNameOptions() {
    if (departmentOptionsLoaded || departmentOptionsLoading) return;
    departmentOptionsLoading = true;
    try {
      const tree = await getFieldDeptTree();
      const nameMap: Record<string, string> = {};
      const collectNames = (nodes: { id?: string | number; name?: string; children?: unknown[] }[]) => {
        nodes.forEach((node) => {
          if (node.id !== undefined && node.name) nameMap[String(node.id)] = node.name;
          collectNames((node.children ?? []) as { id?: string | number; name?: string; children?: unknown[] }[]);
        });
      };
      collectNames(tree as unknown as { id?: string | number; name?: string; children?: unknown[] }[]);
      departmentNameById.value = nameMap;
      departmentOptionsLoaded = true;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      departmentOptionsLoading = false;
    }
  }

  watch(
    () => {
      const condition = editor.conditionFilter.list[0];
      return (
        editor.triggerType === MessageNotificationTriggerEnum.FIELD &&
        conditionFilterConfigList.value.find((field) => field.dataIndex === condition?.dataIndex)?.type ===
          FieldTypeEnum.USER_SELECT
      );
    },
    (isUserCondition) => {
      if (isUserCondition) loadUserNameOptions();
    }
  );
  watch(
    () => {
      const condition = editor.conditionFilter.list[0];
      const conditionField = conditionFilterConfigList.value.find((field) => field.dataIndex === condition?.dataIndex);
      return (
        editor.triggerType === MessageNotificationTriggerEnum.FIELD &&
        conditionField?.type === FieldTypeEnum.TREE_SELECT
      );
    },
    (isDepartmentCondition) => {
      if (isDepartmentCondition) loadDepartmentNameOptions();
    }
  );
  const dateFieldOptions = computed(() => {
    const options = triggerFields.value
      .filter((field) => [FieldTypeEnum.DATE_TIME, FieldTypeEnum.TIME_RANGE_PICKER].includes(field.type))
      .map((field) => ({ label: field.name, value: field.id }));
    if (editor.scheduleFieldId && !options.some((option) => option.value === editor.scheduleFieldId)) {
      options.unshift({
        label:
          editor.scheduleFieldId === 'until_time'
            ? t('system.message.settings.dateField.expiration')
            : editor.scheduleFieldId,
        value: editor.scheduleFieldId,
      });
    }
    return options.length
      ? options
      : [{ label: t('system.message.settings.dateField.expiration'), value: 'until_time' }];
  });
  const reminderTypeOptions = computed(() => {
    const options = [
      { label: t('system.message.settings.reminderFrequency.once'), value: 'ONLY_ONE' },
      { label: t('system.message.settings.reminderFrequency.daily'), value: 'EVERY_DAY' },
    ];
    if (editor.remindType && !options.some((option) => option.value === editor.remindType)) {
      options.unshift({ label: editor.remindType, value: editor.remindType });
    }
    return options;
  });
  const scenePreviewText = computed(() => {
    const selectedCategoryType = editorCategoryType.value || editor.taskType;
    const category = getCategoryLabel(selectedCategoryType);
    const templateValues = editor.templateValue.filter((value) => value.trim());
    const firstCondition = editor.conditionFilter.list[0];
    const approvalCondition = firstCondition?.dataIndex === 'approvalStatus' ? firstCondition : undefined;
    const isApprovalAction = editor.actionTypeConfig.endsWith('_APPROVAL');
    const isApprovalStatusCondition =
      editor.triggerType === MessageNotificationTriggerEnum.FIELD && Boolean(approvalCondition);

    if (isApprovalAction || isApprovalStatusCondition) {
      let approvalStateOptions: unknown[] = [];
      if (Array.isArray(approvalCondition?.value)) {
        approvalStateOptions = approvalCondition.value;
      } else if (approvalCondition?.value !== null && approvalCondition?.value !== undefined) {
        approvalStateOptions = [approvalCondition.value];
      }
      const approvalState = approvalStateOptions
        .map((value) => processStatusOptions.find((option) => option.value === value)?.label ?? String(value))
        .join('、');
      const approvalStateLabel =
        approvalState ||
        processStatusOptions.find((option) => option.value === ProcessStatusEnum.APPROVED)?.label ||
        '';
      const resourceTypeName =
        editorCategoryOptions.value.find((option) => option.value === selectedCategoryType)?.label ?? category;
      const resourceName = `${resourceTypeName}${t('system.message.settings.preview.nameSuffix')}`;

      if (editorMode.value === 'create') {
        return t('system.message.settings.preview.approvalResult', {
          operator: t('system.message.settings.preview.operator'),
          type: category,
          name: resourceName,
          state: approvalStateLabel,
        });
      }

      return templateValues
        .map((value) =>
          value
            .replace(/\$\{name\}/gi, `「${resourceName}」`)
            .replace(/\$\{operator\}/gi, t('system.message.settings.preview.operator'))
            .replace(/\$\{type\}/gi, category)
            .replace(/\$\{state\}/gi, approvalStateLabel)
        )
        .join('\n');
    }

    if (editor.triggerType === MessageNotificationTriggerEnum.ACTION) {
      if (templateValues.length) {
        const resourceTypeName =
          editorCategoryOptions.value.find((option) => option.value === selectedCategoryType)?.label ?? category;
        const resourceName = `${resourceTypeName}${t('system.message.settings.preview.nameSuffix')}`;
        return templateValues
          .map((value) =>
            value
              .replace(/\$\{name\}/gi, `「${resourceName}」`)
              .replace(/\$\{operator\}/gi, t('system.message.settings.preview.operator'))
          )
          .join('\n');
      }
      const action = actionOptions.value.find((option) => option.value === editor.actionTypeConfig)?.label ?? '';
      return t('system.message.settings.preview.action', { category, action });
    }
    if (editor.triggerType === MessageNotificationTriggerEnum.SCHEDULE) {
      const dateField = dateFieldOptions.value.find((option) => option.value === editor.scheduleFieldId)?.label ?? '';
      const time = formatScheduleTime(editor.scheduleTime);
      return t('system.message.settings.preview.customReminder', {
        recordName: `${category}${t('system.message.settings.preview.recordNameSuffix')}`,
        dateField,
        fieldValue: `${editor.scheduleDay} ${t('system.message.settings.reminderUnit.day')}，${time}`,
      });
    }
    const condition = editor.conditionFilter.list[0];
    const conditionField = conditionFilterConfigList.value.find((field) => field.dataIndex === condition?.dataIndex);
    return t('system.message.settings.preview.customCondition', {
      recordName: `${category}${t('system.message.settings.preview.recordNameSuffix')}`,
      conditionField: conditionField?.title
        ? t(conditionField.title)
        : t('system.message.settings.preview.conditionField'),
      fieldValue: getConditionValueLabel(
        condition?.value,
        conditionField,
        condition?.selectedRows ?? [],
        condition?.selectedUserList ?? []
      ),
    });
  });
  const editorEditable = computed(() =>
    editorMode.value === 'create' || editorMode.value === 'copy' ? canAdd.value : canUpdate.value
  );
  const editorTriggerEditable = computed(
    () => editorEditable.value && !(editorMode.value === 'update' && editorSource.value?.sysEnable)
  );

  function parseJsonArray(value: string | string[] | null | undefined): string[] {
    if (Array.isArray(value)) return value;
    if (!value) return [];
    try {
      const parsed = JSON.parse(value);
      return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === 'string') : [];
    } catch {
      return [];
    }
  }

  function getTriggerTagType(triggerType: string): 'default' | 'warning' | 'success' {
    if (triggerType === MessageNotificationTriggerEnum.SCHEDULE) return 'warning';
    if (triggerType === MessageNotificationTriggerEnum.FIELD) return 'success';
    return 'default';
  }

  const standardFormConfigApis: Partial<Record<string, () => Promise<{ fields: FormCreateField[] }>>> = {
    CUSTOMER: getCustomerFormConfig,
    CLUE: getClueFormConfig,
    OPPORTUNITY: getOptFormConfig,
    BUSINESS: getOptFormConfig,
    CONTRACT: getContractFormConfig,
    ORDER: getOrderFormConfig,
    FOLLOW_UP: getCustomerFollowRecordFormConfig,
  };

  async function loadTriggerFields() {
    try {
      const formConfig =
        editor.group === 'custom'
          ? await getCustomFormDetail(editor.taskType)
          : await standardFormConfigApis[editor.taskType.toUpperCase()]?.();
      triggerFields.value = formConfig?.fields ?? [];
    } catch {
      triggerFields.value = [];
    }
  }

  watch([showEditor, () => editor.taskType], async ([visible]) => {
    if (visible) await loadTriggerFields();
  });

  function mapTaskRow(task: MessageTaskResponse): SceneRow {
    const group = task.type === MessageNotificationTypeEnum.CUSTOM ? 'custom' : 'standard';
    const receiveMethods = parseJsonArray(task.receiveMethod ?? '[]');
    const channelKeys = Object.entries(channelKeyToEnum)
      .filter(([, channel]) => receiveMethods.includes(channel))
      .map(([key]) => key);
    return {
      ...task,
      members: task.members ?? [],
      config: task.config ?? {},
      template: task.template ?? '[]',
      templateValue: task.templateValue ?? [],
      group,
      enabled: receiveMethods.length > 0,
      channelKeys,
      sceneCategoryType:
        group === 'standard' ? getSceneCategoryType(task.taskType, task.config?.actionTypeConfig) : task.taskType,
    };
  }

  async function fetchMessageTasks(params?: Record<string, any>) {
    const request = params as MessageTaskPageParams;
    const result = await getMessageTaskPage({
      current: Number(request?.current ?? 1),
      pageSize: Number(request?.pageSize ?? 20),
      taskType: String(request?.taskType ?? activeCategory.value),
      sort: request?.sort ?? {},
      combineSearch: request?.combineSearch ?? { searchMode: 'AND', conditions: [] },
      keyword: String(request?.keyword ?? ''),
      viewId: String(request?.viewId ?? 'ALL'),
      filters: Array.isArray(request?.filters) ? request.filters : [],
    });
    return { ...result, list: result.list.map(mapTaskRow) };
  }

  const {
    propsRes: tableProps,
    propsEvent,
    loadList,
    setLoadListParams,
  } = useTable<SceneRow>(fetchMessageTasks, {
    bordered: false,
    showPagination: true,
    notVirtualScroll: true,
    data: [],
    tableRowKey: 'id',
    permission: ['SYSTEM_NOTICE:READ'],
    hiddenTotal: ref(true),
    hiddenRefresh: true,
    hiddenAllScreen: true,
    containerClass: '.message-settings-table',
  });

  function handleTableFilterChange(filters: Parameters<typeof propsEvent.value.filterChange>[0]) {
    propsEvent.value.filterChange(filters);
  }

  function searchScenes(value?: string) {
    if (value !== undefined) keyword.value = value;
    setLoadListParams({ taskType: activeCategory.value, keyword: keyword.value });
    loadList();
  }

  async function loadTasksForCategory() {
    selectedIds.value = [];
    if (!activeCategory.value) {
      tableProps.value.data = [];
      if (tableProps.value.crmPagination) {
        tableProps.value.crmPagination.itemCount = 0;
        tableProps.value.crmPagination.page = 1;
      }
      return;
    }
    setLoadListParams({ taskType: activeCategory.value, keyword: keyword.value });
    await loadList();
  }

  function getReceiveMethods(channelKeys: string[], preservedMethods: string[] = []) {
    return [...new Set([...channelKeys.map((key) => channelKeyToEnum[key]).filter(Boolean), ...preservedMethods])];
  }

  function getScopeEditorState(task: SceneRow) {
    const scopeIds = parseJsonArray(task.scopeId ?? '[]');
    const members = task.members ?? [];
    const customReceiverIds = new Set(customReceiverOptions.value.map((option) => option.value));
    const specialKeys = scopeIds.filter((id) => specialScopeIds.has(id));
    const customKeys = scopeIds.filter((id) => customReceiverIds.has(id));
    const userMembers = members.filter(
      (member) =>
        member.scope.toUpperCase() === MemberSelectTypeEnum.MEMBER &&
        !specialScopeIds.has(member.id) &&
        !customReceiverIds.has(member.id)
    );
    const roleMembers = members.filter(
      (member) => member.scope.toUpperCase() === MemberSelectTypeEnum.ROLE && !customReceiverIds.has(member.id)
    );
    const mappedIds = new Set([
      ...specialKeys,
      ...customKeys,
      ...userMembers.map((member) => member.id),
      ...roleMembers.map((member) => member.id),
    ]);
    return {
      scopeKeys: [
        ...specialKeys,
        ...customKeys,
        ...(userMembers.length ? ['specifiedUsers'] : []),
        ...(roleMembers.length ? ['specifiedRoles'] : []),
      ],
      receiverMemberIds: userMembers.map((member) => member.id),
      receiverMembers: userMembers.map((member) => ({
        id: member.id,
        name: member.name,
        scope: MemberSelectTypeEnum.MEMBER,
      })),
      receiverRoleIds: roleMembers.map((member) => member.id),
      receiverRoles: roleMembers.map((member) => ({
        id: member.id,
        name: member.name,
        scope: MemberSelectTypeEnum.ROLE,
      })),
      preservedScopeIds: scopeIds.filter((id) => !mappedIds.has(id)),
    };
  }

  async function openEditor(task: SceneRow, mode: EditorMode) {
    editorSource.value = task;
    editorMode.value = mode;
    isCustomScene.value = mode === 'copy' || !task.sysEnable;
    editor.taskType = task.taskType;
    editor.group = task.group;
    editorCategoryType.value =
      task.group === 'custom' ? task.taskType : getSceneCategoryType(task.taskType, task.config?.actionTypeConfig);
    loadNotificationStageOptions(editorCategoryType.value);
    await loadTriggerFields();

    const config = task.config ?? {};
    const fieldConditions = (config.fieldTypeConfig ?? []).map((condition) => ({
      dataIndex: condition.name,
      operator: condition.operator as OperatorEnum,
      type: (condition.type as FieldTypeEnum) ?? FieldTypeEnum.INPUT,
      value: condition.value,
      containChildIds: condition.containChildIds,
    }));
    const scopeState = getScopeEditorState(task);
    const receiveMethods = parseJsonArray(task.receiveMethod ?? '[]');

    Object.assign(editor, {
      name: mode === 'copy' ? `${task.name}${t('system.message.settings.copySuffix', { id: task.id })}` : task.name,
      triggerType: task.triggerType,
      config,
      actionTypeConfig: config.actionTypeConfig ?? getDefaultActionTypeConfig(),
      scheduleFieldId: config.scheduleTypeConfig?.fieldId ?? 'until_time',
      scheduleDay: Number(config.scheduleTypeConfig?.day ?? 0),
      scheduleTime: parseScheduleTime(config.scheduleTypeConfig?.time),
      remindType: config.scheduleTypeConfig?.remindType ?? 'ONLY_ONE',
      conditionFilter: {
        searchMode: 'AND',
        list: fieldConditions.length ? fieldConditions : [{ dataIndex: null, type: FieldTypeEnum.INPUT, value: null }],
      },
      template: task.template || '[]',
      templateValue: task.templateValue ?? [],
      ...scopeState,
      channelKeys: Object.entries(channelKeyToEnum)
        .filter(([, channel]) => receiveMethods.includes(channel))
        .map(([key]) => key),
      preservedReceiveMethods: receiveMethods.filter(
        (method) => !Object.values(channelKeyToEnum).includes(method as MessageNotificationChannelEnum)
      ),
    });
    showEditor.value = true;
  }

  function getTaskSaveParams(
    task: SceneRow,
    channelKeys = task.channelKeys,
    preserveUnknownMethods = true
  ): MessageTaskSaveParams {
    const currentReceiveMethods = parseJsonArray(task.receiveMethod ?? '[]');
    const preservedMethods = preserveUnknownMethods
      ? currentReceiveMethods.filter(
          (method) => !Object.values(channelKeyToEnum).includes(method as MessageNotificationChannelEnum)
        )
      : [];
    return {
      name: task.name,
      taskType: task.taskType,
      scopeId: parseJsonArray(task.scopeId),
      receiveMethod: JSON.stringify(getReceiveMethods(channelKeys, preservedMethods)),
      triggerType: task.triggerType,
      config: task.config ?? {},
      template: task.template || '[]',
      type: task.type,
    };
  }

  async function saveSceneChannels(task: SceneRow, channelKeys: string[], preserveUnknownMethods = true) {
    try {
      await updateMessageTask({ ...getTaskSaveParams(task, channelKeys, preserveUnknownMethods), id: task.id });
      Message.success(t('common.saveSuccess'));
      await loadList();
    } catch {
      Message.error(t('common.saveFailed'));
    }
  }

  async function saveSceneScopes(task: SceneRow, scopeIds: string[]) {
    try {
      await updateMessageTask({
        ...getTaskSaveParams(task, task.channelKeys),
        scopeId: [...new Set(scopeIds)],
        id: task.id,
      });
      Message.success(t('common.saveSuccess'));
      await loadList();
    } catch {
      Message.error(t('common.saveFailed'));
    }
  }

  function getSceneOperationGroup(task: SceneRow): ActionsItem[] {
    return [
      { label: t('system.message.settings.configure'), key: 'configure', permission: ['SYSTEM_NOTICE:UPDATE'] },
      { label: t('system.message.settings.copy'), key: 'copy', permission: ['SYSTEM_NOTICE:ADD'] },
      ...(!task.sysEnable
        ? [
            {
              label: t('system.message.settings.delete'),
              key: 'delete',
              danger: true,
              permission: ['SYSTEM_NOTICE:DELETE'],
            },
          ]
        : []),
    ];
  }

  async function refreshAfterMutation() {
    const [systemTypes, customTypes] = await Promise.all([
      getMessageTaskSystemTypes(),
      getMessageTaskCustomFormTypes(),
    ]);
    systemCategories.value = mapSystemCategories(systemTypes ?? []);
    customFormCategories.value = (customTypes ?? []).map((item) => ({
      key: item.taskType,
      label: item.name || item.taskType,
      count: Number(item.count ?? 0),
    }));
    if (!categoryList.value.some((item) => item.key === activeCategory.value)) {
      activeCategory.value = categoryList.value[0]?.key ?? '';
    }
    await loadTasksForCategory();
  }

  function deleteScene(task: SceneRow) {
    openModal({
      type: 'error',
      title: t('system.message.settings.deleteConfirm', { name: task.name }),
      content: t('system.message.settings.deleteConfirmContent'),
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteMessageTask(task.id);
          Message.success(t('system.message.settings.sceneDeleted'));
          await refreshAfterMutation();
        } catch {
          Message.error(t('common.saveFailed'));
        }
      },
    });
  }

  function handleSceneOperation(task: SceneRow, key: string) {
    if (key === 'configure') openEditor(task, 'update');
    if (key === 'copy') openEditor(task, 'copy');
    if (key === 'delete') deleteScene(task);
  }

  const columns = computed<CrmDataTableColumn<SceneRow>[]>(() => [
    {
      key: SpecialColumnEnum.SELECTION,
      type: SpecialColumnEnum.SELECTION,
      width: 20,
      fixed: 'left',
      disabled: () => !canUpdate.value,
      selectTooltip: { showTooltip: () => false },
    },
    {
      title: t('system.message.settings.status'),
      key: 'enabled',
      fixed: 'left',
      width: 50,
      render: (row) =>
        h(NSwitch, {
          'value': row.enabled,
          'disabled': !canUpdate.value || enableLoadingIds.value.has(row.id),
          'loading': enableLoadingIds.value.has(row.id),
          'size': 'small',
          'rubberBand': false,
          'onUpdate:value': async (enabled: boolean) => {
            if (enableLoadingIds.value.has(row.id)) return;
            const loadingIds = new Set(enableLoadingIds.value);
            loadingIds.add(row.id);
            enableLoadingIds.value = loadingIds;

            try {
              await enableMessageTask(row.id);
              row.enabled = enabled;
            } catch {
              Message.error(t('common.saveFailed'));
            } finally {
              const finishedIds = new Set(enableLoadingIds.value);
              finishedIds.delete(row.id);
              enableLoadingIds.value = finishedIds;
            }
          },
        }),
    },
    {
      title: t('system.message.notificationScenario'),
      key: 'name',
      fixed: 'left',
      width: 220,
      ellipsis: { tooltip: true },
      render: (row) =>
        h('div', { class: 'flex min-w-0 items-center gap-[8px]' }, [
          h(
            'span',
            { class: `message-scene-source ${row.sysEnable ? 'builtin' : 'custom'}` },
            t(row.sysEnable ? 'system.message.settings.builtin' : 'system.message.settings.custom')
          ),
          h(
            NTooltip,
            {},
            {
              trigger: () => h('span', { class: 'one-line-text' }, row.name),
              default: () => row.name,
            }
          ),
          row.triggerType === MessageNotificationTriggerEnum.SCHEDULE
            ? h(
                NTooltip,
                {},
                {
                  trigger: () => h(CrmIcon, { type: 'iconicon_time', size: 16, class: 'message-reminder-icon' }),
                  default: () => getScheduleReminderTooltip(row),
                }
              )
            : null,
        ]),
    },
    {
      title: t('system.message.settings.category'),
      key: sceneCategoryFilterKey,
      width: 84,
      ellipsis: { tooltip: true },
      filter: sceneCategoryFilterOptions.value.length > 1,
      filterOptions: sceneCategoryFilterOptions.value,
      render: (row) => getCategoryLabel(row.sceneCategoryType) || '-',
    },
    {
      title: t('system.message.settings.triggerType'),
      key: 'triggerType',
      width: 100,
      filter: true,
      filterOptions: triggerOptions.value.map(({ label, value }) => ({ label, value })),
      render: (row) =>
        h(
          CrmTag,
          { theme: 'light', type: getTriggerTagType(row.triggerType) },
          {
            default: () =>
              triggerOptions.value.find((option) => option.value === row.triggerType)?.label ?? row.triggerType,
          }
        ),
    },
    {
      title: t('system.message.settings.receivers'),
      key: 'members',
      width: 170,
      render: (row) => {
        const scopeState = getScopeEditorState(row);
        return h(MessageReceiverSelector, {
          groups: scopeOptionGroups.value,
          variant: 'table',
          disabled: !canUpdate.value,
          placeholder: t('common.pleaseSelect'),
          specifiedUsersLabel: t('system.message.settings.receiver.specifiedUsers'),
          specifiedRolesLabel: t('system.message.settings.receiver.specifiedRoles'),
          receiverKeys: scopeState.scopeKeys,
          receiverMemberIds: scopeState.receiverMemberIds,
          receiverMembers: scopeState.receiverMembers,
          receiverRoleIds: scopeState.receiverRoleIds,
          receiverRoles: scopeState.receiverRoles,
          onChangeOnClose: (value: { keys: string[]; memberIds: string[]; roleIds: string[] }) => {
            const customReceiverIds = new Set(customReceiverOptions.value.map((option) => option.value));
            const scopeIds = [
              ...value.keys.filter((key) => specialScopeIds.has(key) || customReceiverIds.has(key)),
              ...value.memberIds,
              ...value.roleIds,
              ...scopeState.preservedScopeIds,
            ];
            saveSceneScopes(row, [...new Set(scopeIds)]);
          },
        });
      },
    },
    {
      title: t('system.message.settings.receiveMethods'),
      key: 'channelKeys',
      width: 170,
      render: (row) =>
        h(MessageReceiverSelector, {
          groups: [
            {
              key: 'channels',
              label: t('system.message.settings.receiveMethods'),
              options: channelOptions.value,
            },
          ],
          variant: 'table',
          disabled: !canUpdate.value,
          placeholder: t('common.pleaseSelect'),
          receiverKeys: row.channelKeys,
          onChangeOnClose: (value: { keys: string[] }) => {
            saveSceneChannels(row, value.keys);
          },
        }),
    },
    {
      title: t('common.creator'),
      key: 'createUser',
      width: 120,
      sorter: true,
      ellipsis: { tooltip: true },
      render: (row) => row.createUserName || '-',
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 160,
      sorter: true,
    },
    {
      title: t('common.updateUserName'),
      key: 'updateUser',
      width: 120,
      sorter: true,
      ellipsis: { tooltip: true },
      render: (row) => row.updateUserName || '-',
    },
    {
      title: t('common.updateTime'),
      key: 'updateTime',
      width: 160,
      sorter: true,
    },
    {
      title: t('common.operation'),
      key: 'operation',
      width: 140,
      fixed: 'right',
      render: (row) =>
        h(CrmOperationButton, {
          groupList: getSceneOperationGroup(row),
          onSelect: (key: string) => handleSceneOperation(row, key),
        }),
    },
  ]);

  watch(
    columns,
    (value) => {
      tableProps.value.columns = value;
    },
    { immediate: true }
  );

  async function loadTaskCategories() {
    try {
      const [systemTypes, customTypes] = await Promise.all([
        getMessageTaskSystemTypes(),
        getMessageTaskCustomFormTypes(),
      ]);
      systemCategories.value = mapSystemCategories(systemTypes ?? []);
      customFormCategories.value = (customTypes ?? []).map((item: MessageTaskCountDTO) => ({
        key: item.taskType,
        label: item.name || item.taskType,
        count: Number(item.count ?? 0),
      }));
      const categories = categoryList.value;
      if (!categories.some((item) => item.key === activeCategory.value)) {
        activeCategory.value = categories[0]?.key ?? '';
      }
      await loadTasksForCategory();
    } catch {
      Message.error(t('api.apiRequestFailed'));
    }
  }

  watch(activeGroup, async () => {
    activeCategory.value = categoryList.value[0]?.key ?? '';
    await loadTasksForCategory();
  });

  async function handleCategoryClick(category: Record<string, any>) {
    activeCategory.value = String(category.key);
    await loadTasksForCategory();
  }

  onMounted(loadTaskCategories);

  function fillCustomTemplateForAction() {
    if (editor.triggerType !== MessageNotificationTriggerEnum.ACTION) {
      return;
    }

    const matchedScene = tableProps.value.data.find(
      (task) =>
        task.sysEnable &&
        task.taskType === editor.taskType &&
        task.config?.actionTypeConfig === editor.actionTypeConfig &&
        task.templateValue?.length
    );
    editor.template = matchedScene?.template || '[]';
    editor.templateValue = matchedScene?.templateValue ? [...matchedScene.templateValue] : [];
  }

  function openCreateScene() {
    const taskType = activeCategory.value || categoryList.value[0]?.key;
    if (!taskType) {
      if (activeGroup.value === 'custom' && customFormCategories.value.length === 0) {
        Message.warning(t('system.message.settings.customFormRequired'));
        return;
      }
      Message.warning(t('common.pleaseSelect'));
      return;
    }
    editorSource.value = null;
    editorMode.value = 'create';
    isCustomScene.value = true;
    Object.assign(editor, {
      name: '',
      taskType,
      group: activeGroup.value,
      triggerType: MessageNotificationTriggerEnum.ACTION,
      config: {},
      actionTypeConfig: '',
      scheduleFieldId: 'until_time',
      scheduleDay: 0,
      scheduleTime: parseScheduleTime(),
      remindType: 'ONLY_ONE',
      conditionFilter: {
        list: [{ dataIndex: null, type: FieldTypeEnum.INPUT, value: null }],
      },
      template: '[]',
      templateValue: [],
      scopeKeys: [],
      receiverMemberIds: [],
      receiverMembers: [],
      receiverRoleIds: [],
      receiverRoles: [],
      preservedScopeIds: [],
      channelKeys: [],
      preservedReceiveMethods: [],
    });
    editorCategoryType.value = activeGroup.value === 'custom' ? taskType : getDefaultEditorCategoryType(taskType);
    loadNotificationStageOptions(editorCategoryType.value);
    editor.actionTypeConfig = getDefaultActionTypeConfig();
    fillCustomTemplateForAction();
    showEditor.value = true;
  }

  function handleTriggerTypeChange() {
    fillCustomTemplateForAction();
  }

  function handleActionTypeChange() {
    fillCustomTemplateForAction();
  }

  function handleEditorCategoryChange(categoryType: string) {
    editorCategoryType.value = categoryType;
    loadNotificationStageOptions(categoryType);
    if (editor.group === 'custom') editor.taskType = categoryType;
    editor.config = {};
    editor.templateValue = [];
    editor.actionTypeConfig = getDefaultActionTypeConfig();
    editor.scheduleFieldId = 'until_time';
    editor.conditionFilter = {
      searchMode: 'AND',
      list: [{ dataIndex: null, type: FieldTypeEnum.INPUT, value: null }],
    };
  }

  function getScopeIdsFromEditor() {
    const customReceiverIds = new Set(customReceiverOptions.value.map((option) => option.value));
    return [
      ...new Set([
        ...editor.scopeKeys.filter((key) => specialScopeIds.has(key) || customReceiverIds.has(key)),
        ...editor.receiverMemberIds,
        ...editor.receiverRoleIds,
        ...editor.preservedScopeIds,
      ]),
    ];
  }

  function getEditorConfig(): MessageTaskTriggerConfig {
    const config: MessageTaskTriggerConfig = { ...editor.config };
    if (editor.triggerType === MessageNotificationTriggerEnum.ACTION) {
      config.actionTypeConfig = editor.actionTypeConfig;
    }
    if (editor.triggerType === MessageNotificationTriggerEnum.SCHEDULE) {
      config.scheduleTypeConfig = {
        fieldId: editor.scheduleFieldId,
        day: String(editor.scheduleDay),
        time: `${formatScheduleTime(editor.scheduleTime)}:00`,
        remindType: editor.remindType,
      };
    }
    if (editor.triggerType === MessageNotificationTriggerEnum.FIELD) {
      const fieldConditions: NonNullable<MessageTaskTriggerConfig['fieldTypeConfig']> = [];
      editor.conditionFilter.list.forEach((condition) => {
        if (condition.dataIndex && condition.operator) {
          fieldConditions.push({
            name: String(condition.dataIndex),
            value: condition.value,
            operator: String(condition.operator),
            multipleValue: Boolean(condition.type?.endsWith('_MULTIPLE') || condition.selectProps?.multiple),
            type: condition.type,
            containChildIds: condition.containChildIds,
          });
        }
      });
      config.fieldTypeConfig = fieldConditions;
    }
    return config;
  }

  function openBatchChannelModal() {
    batchChannelKeys.value = [];
    showBatchChannelModal.value = true;
  }

  function handleBatchAction(action: ActionsItem) {
    if (action.key === 'batchChannels') {
      openBatchChannelModal();
    }
  }

  async function applyBatchChannels() {
    try {
      await batchUpdateMessageTaskReceiveMethod({
        ids: selectedIds.value,
        receiveMethod: JSON.stringify(getReceiveMethods(batchChannelKeys.value)),
      });
      selectedIds.value = [];
      showBatchChannelModal.value = false;
      Message.success(t('system.message.settings.batchChannelsSuccess'));
    } catch {
      Message.error(t('common.saveFailed'));
    }
    await loadList();
  }

  async function saveEditor() {
    const source = editorSource.value;
    const name = editor.name.trim();
    try {
      await sceneFormRef.value?.validate();
    } catch {
      return;
    }
    if (editor.triggerType === MessageNotificationTriggerEnum.FIELD && editorTriggerEditable.value) {
      const filterIsValid = await conditionFilterRef.value?.validateForm();
      if (!filterIsValid) return;
    }

    let saveType: MessageTaskSaveParams['type'];
    if (editorMode.value === 'update' && source) {
      saveType = source.type;
    } else if (editor.group === 'custom') {
      saveType = MessageNotificationTypeEnum.CUSTOM;
    } else {
      saveType = MessageNotificationTypeEnum.SYSTEM;
    }
    const params: MessageTaskSaveParams = {
      name,
      taskType: editor.taskType,
      scopeId: getScopeIdsFromEditor(),
      receiveMethod: JSON.stringify(getReceiveMethods(editor.channelKeys, editor.preservedReceiveMethods)),
      triggerType: editorMode.value === 'update' && source?.sysEnable ? source.triggerType : editor.triggerType,
      config: editorMode.value === 'update' && source?.sysEnable ? source.config : getEditorConfig(),
      template: editorMode.value === 'update' && source?.sysEnable ? source.template : editor.template || '[]',
      ...(isCustomScene.value ? { templateValue: editor.templateValue } : {}),
      type: saveType,
    };
    try {
      if (editorMode.value === 'create' || editorMode.value === 'copy') {
        await addMessageTask(params);
      } else if (source) {
        await updateMessageTask({ ...params, id: source.id });
      }
      showEditor.value = false;
      Message.success(t('system.message.settings.sceneSaved'));
      await refreshAfterMutation();
    } catch {
      Message.error(t('common.saveFailed'));
    }
  }
</script>

<style lang="less" scoped>
  .message-settings-layout {
    display: grid;
    width: 100%;
    height: 100%;
    min-height: 420px;
    grid-template-columns: 220px minmax(0, 1fr);
    gap: 0;
  }
  .message-settings-sidebar {
    display: flex;
    padding: 16px;
    min-height: 0;
    border-radius: var(--border-radius-small);
    background: var(--text-n10);
    flex-direction: column;
  }
  .message-settings-group-tabs {
    margin-bottom: 12px;
    width: 100%;
    :deep(.n-tabs-nav),
    :deep(.n-tabs-rail) {
      width: 100%;
    }
    :deep(.n-tabs-tab-wrapper) {
      min-width: 0;
      flex: 1 1 50%;
    }
    :deep(.n-tabs-tab) {
      justify-content: center;
      width: 100%;
      box-sizing: border-box;
    }
  }
  .message-settings-category-count {
    font-size: inherit;
    font-weight: 400;
    color: var(--text-n4);
  }
  :deep(.message-reminder-icon) {
    flex: none;
    color: var(--text-n4);
    cursor: help;
    transition: color 120ms ease;
    &:hover {
      color: var(--primary-8);
    }
  }
  .message-settings-category-list {
    overflow-y: auto;
    min-height: 0;
    flex: 1;
  }
  .message-settings-category-item {
    display: flex;
    align-items: center;
    padding: 8px;
    width: 100%;
    min-width: 0;
    border: 0;
    border-radius: var(--border-radius-small);
    text-align: left;
    color: var(--text-n2);
    background: transparent;
    cursor: pointer;
    gap: 8px;
    &:hover {
      background: var(--text-n9);
    }
    &.active {
      color: var(--primary-8);
      background: var(--primary-7);
    }
    .one-line-text {
      min-width: 0;
      flex: 1;
    }
  }
  .message-settings-main {
    overflow: hidden;
    padding: 16px;
    min-width: 0;
    min-height: 0;
    border-left: 1px solid var(--text-n8);
  }
  .message-settings-table {
    min-width: 0;
    height: 100%;
  }
  :deep(.message-scene-source) {
    padding: 2px 6px;
    font-size: 12px;
    border-radius: 2px;
    flex: none;
    line-height: 18px;
    &.builtin {
      color: var(--text-n1);
      background: var(--text-n8);
    }
    &.custom {
      color: var(--info-blue);
      background: var(--info-5);
    }
  }
  .message-channel-options {
    display: grid;
    width: 100%;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 8px;
  }
  :deep(.message-channel-options .n-checkbox) {
    display: flex;
    justify-content: flex-start;
    align-items: center;
    padding: 8px;
    min-width: 0;
    border: 1px solid var(--text-n7);
    border-radius: var(--border-radius-small);
    white-space: nowrap;
    background: var(--text-n10);
  }
  :deep(.message-channel-options .n-checkbox--checked) {
    border-color: var(--primary-8) !important;
    background: var(--primary-7);
  }
  :deep(.message-channel-options .n-checkbox__label) {
    overflow: hidden;
    padding-left: 6px;
    text-overflow: ellipsis;
  }
  .message-channel-field {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .message-channel-label {
    font-size: 14px;
    color: var(--text-n1);
    line-height: 22px;
  }
  .message-scene-modal {
    .message-scene-form {
      overflow-y: auto;
      padding: 2px 4px 8px;
      max-height: min(68vh, 680px);
      :deep(.n-form-item-label) {
        font-weight: 500;
        color: var(--text-n1);
      }
      :deep(.n-form-item:last-of-type .n-form-item-feedback-wrapper) {
        height: 24px;
      }
    }
  }
  .message-scene-basic-fields {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 16px;
  }
  .message-trigger-options {
    display: grid;
    width: 100%;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
  }
  .message-trigger-card {
    align-items: flex-start;
    padding: 8px;
    width: 100%;
    min-width: 0;
    border: 1px solid var(--text-n7);
    border-radius: var(--border-radius-small);
    text-align: left;
    color: var(--text-n1);
    background: var(--text-n10);
    transition: border-color 120ms ease, background-color 120ms ease;
    gap: 10px;
    cursor: pointer;
    &:hover {
      border-color: var(--primary-6);
    }
    &.selected {
      border-color: var(--primary-8);
      background: var(--primary-7);
    }
    :deep(.n-radio__label) {
      min-width: 0;
      flex: 1;
    }
  }
  .message-trigger-copy {
    display: flex;
    min-width: 0;
    flex-direction: column;
    gap: 4px;
  }
  .message-trigger-title {
    font-size: 14px;
    font-weight: 500;
    line-height: 20px;
  }
  .message-trigger-description {
    font-size: 12px;
    color: var(--text-n4);
    line-height: 18px;
  }
  .message-rule-panel {
    display: flex;
    align-items: center;
    padding: 16px;
    width: 100%;
    min-width: 0;
    border-radius: var(--border-radius-small);
    background: var(--text-n9);
    gap: 10px;
  }
  .message-reminder-rule {
    display: grid;
    grid-template-columns: minmax(180px, 1.2fr) auto 132px auto minmax(200px, 1.2fr) minmax(170px, 1.2fr);
    gap: 8px;
    padding: 8px;
    > span {
      flex: none;
      color: var(--text-n3);
    }
  }
  .message-reminder-date,
  .message-reminder-number {
    width: 100%;
  }
  .message-reminder-time-range {
    display: flex;
    align-items: center;
    padding: 0 8px;
    min-width: 0;
    height: 34px;
    border: 1px solid var(--text-n7);
    border-radius: var(--border-radius-small);
    background: var(--text-n10);
    gap: 6px;
    > span {
      font-size: 13px;
      color: var(--text-n4);
      flex: none;
    }
  }
  .message-reminder-time {
    width: 0;
    min-width: 0;
    flex: 1 1 0;
  }
  .message-reminder-frequency {
    width: 100%;
  }
  .message-preview-label {
    margin-bottom: 10px;
    font-size: 14px;
    font-weight: 500;
    color: var(--text-n1);
  }
  .message-preview-card {
    padding: 16px;
    border: 0;
    border-radius: var(--border-radius-small);
    background: var(--text-n9);
  }
  .message-preview-heading,
  .message-preview-footer {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .message-preview-name {
    position: relative;
    overflow: hidden;
    padding-right: 12px;
    min-width: 0;
    font-weight: 500;
    text-overflow: ellipsis;
    white-space: nowrap;
    .message-preview-dot {
      position: absolute;
      top: 0;
      right: 0;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--error-red);
    }
  }
  .message-preview-content {
    margin: 12px 0;
    white-space: pre-line;
    color: var(--text-n2);
    line-height: 20px;
  }
  .message-preview-footer {
    justify-content: space-between;
    font-size: 12px;
    color: var(--text-n4);
    line-height: 18px;
  }

  @media (max-width: 760px) {
    .message-scene-basic-fields,
    .message-trigger-options {
      grid-template-columns: minmax(0, 1fr);
    }
    .message-channel-options {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
    .message-reminder-rule {
      display: flex;
      flex-wrap: wrap;
    }
    .message-reminder-date,
    .message-reminder-time-range,
    .message-reminder-frequency {
      width: 100%;
    }
    .message-reminder-number {
      width: 112px;
    }
    .message-reminder-time-range {
      flex: 1 1 100%;
    }
  }
</style>
