<template>
  <CrmCard no-content-padding hide-footer :special-height="licenseStore.expiredDuring ? 128 : 64">
    <div class="message-settings-layout">
      <aside class="message-settings-sidebar">
        <CrmTab
          v-model:active-tab="activeGroup"
          class="message-settings-group-tabs"
          type="segment"
          no-content
          :tab-list="groupTabs"
        />
        <CrmList
          v-model:active-item-key="activeCategory"
          :data="categoryList"
          class="message-settings-category-list"
          virtual-scroll-height="100%"
          key-field="key"
          item-class="gap-[8px] px-[8px]"
          active-item-class="bg-[var(--primary-7)]"
          mode="static"
          @item-click="handleCategoryClick"
        >
          <template #title="{ item }">
            <div class="one-line-text" :class="activeCategory === item.key ? 'text-[var(--primary-8)]' : ''">
              {{ item.label }}
            </div>
          </template>
          <template #itemRight="{ item }">
            <span class="message-settings-category-count">{{ item.count }}</span>
          </template>
        </CrmList>
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
          @batch-action="handleBatchAction"
        >
          <template #actionLeft>
            <n-button v-permission="['SYSTEM_NOTICE:ADD']" type="primary" @click="openCreateScene">
              {{ t('system.message.settings.addScene') }}
            </n-button>
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
          <span class="text-[var(--text-n4)]">- {{ getCategoryLabel(editor.taskType) }}</span>
        </div>
      </template>
      <n-form :model="editor" class="message-scene-form" label-placement="top">
        <n-form-item :label="t('system.message.settings.sceneName')" required>
          <n-input v-model:value="editor.name" maxlength="100" :disabled="!editorEditable" />
        </n-form-item>
        <div class="message-scene-basic-fields">
          <n-form-item :label="t('system.message.settings.category')" required>
            <n-select
              v-model:value="editor.taskType"
              :options="editorCategoryOptions"
              :disabled="editorMode !== 'copy' || !editorEditable"
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
        <n-form-item :label="t('system.message.settings.triggerType')" required>
          <n-radio-group
            v-model:value="editor.triggerType"
            class="message-trigger-options"
            :disabled="!editorTriggerEditable"
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
          required
        >
          <n-select
            v-model:value="editor.actionTypeConfig"
            :options="actionOptions"
            :disabled="!editorTriggerEditable"
          />
        </n-form-item>
        <n-form-item
          v-else-if="editor.triggerType === MessageNotificationTriggerEnum.SCHEDULE"
          :label="t('system.message.settings.triggerCondition')"
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
                format="HH:mm"
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
        <n-form-item v-else :label="t('system.message.settings.triggerCondition')" required>
          <FilterContent
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
                {{ editor.name || getCategoryLabel(editor.taskType) }}
                <span class="message-preview-dot"></span>
              </span>
            </div>
            <div class="message-preview-content">{{ scenePreviewText }}</div>
            <div class="message-preview-footer">
              <span>{{ getCategoryLabel(editor.taskType) }}</span>
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
    useDialog,
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
  import { SpecialColumnEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
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
  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import type { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import type { FormCreateField } from '@/components/business/crm-form-create/types';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import MessageReceiverSelector from './messageReceiverSelector.vue';

  import {
    addMessageTask,
    deleteMessageTask,
    enableMessageTask,
    getClueFormConfig,
    getContractFormConfig,
    getCustomerFollowRecordFormConfig,
    getCustomerFormConfig,
    getCustomFormDetail,
    getMessageTaskCustomFormTypes,
    getMessageTaskPage,
    getMessageTaskSystemTypes,
    getOptFormConfig,
    getOrderFormConfig,
    updateMessageTask,
  } from '@/api/modules/index.js';
  import useLicenseStore from '@/store/modules/setting/license';
  import { hasAnyPermission } from '@/utils/permission';

  type SceneGroup = 'standard' | 'custom';
  type EditorMode = 'create' | 'update' | 'copy';

  interface SceneRow extends MessageTaskResponse {
    group: SceneGroup;
    builtin: boolean;
    enabled: boolean;
    channelKeys: string[];
  }

  interface SceneCategoryOption {
    key: string;
    label: string;
    count: number;
  }

  interface SceneActionOption {
    value: MessageNotificationEventEnum | 'create' | 'update' | 'delete';
    labelKey: string;
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
  const dialog = useDialog();
  const licenseStore = useLicenseStore();
  const canUpdate = computed(() => hasAnyPermission(['SYSTEM_NOTICE:UPDATE']));
  const canAdd = computed(() => hasAnyPermission(['SYSTEM_NOTICE:ADD']));
  const activeGroup = ref<SceneGroup>('standard');
  const activeCategory = ref('');
  const selectedIds = ref<string[]>([]);
  const enableLoadingIds = ref<Set<string>>(new Set());
  const showBatchChannelModal = ref(false);
  const batchChannelKeys = ref<string[]>([]);
  const showEditor = ref(false);
  const editorMode = ref<EditorMode>('update');
  const editorSource = ref<SceneRow | null>(null);
  const editor = reactive<SceneEditor>({
    name: '',
    taskType: '',
    group: 'standard',
    triggerType: MessageNotificationTriggerEnum.ACTION,
    config: {},
    actionTypeConfig: 'create',
    scheduleFieldId: 'until_time',
    scheduleDay: 0,
    scheduleTime: 8 * 60 * 60 * 1000,
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
  const triggerFields = ref<FormCreateField[]>([]);

  const groupTabs = computed(() => [
    { name: 'standard', tab: t('system.message.settings.standardFeatures') },
    { name: 'custom', tab: t('system.message.settings.customForms') },
  ]);

  const systemCategoryLabelKeyMap: Record<string, string> = {
    [MessageNotificationCategoryEnum.CUSTOMER]: 'customer',
    [MessageNotificationCategoryEnum.APPROVAL]: 'todo',
    [MessageNotificationCategoryEnum.CLUE]: 'lead',
    [MessageNotificationCategoryEnum.OPPORTUNITY]: 'opportunity',
    BUSINESS: 'opportunity',
    [MessageNotificationCategoryEnum.CONTRACT]: 'contract',
    [MessageNotificationCategoryEnum.ORDER]: 'order',
    [MessageNotificationCategoryEnum.FOLLOW_UP]: 'follow',
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
  const editorCategoryOptions = computed(() => {
    const categories = editor.group === 'standard' ? systemCategories.value : customFormCategories.value;
    const availableCategories =
      editorMode.value === 'create'
        ? categories.filter((category) => category.key === activeCategory.value)
        : categories;
    return availableCategories.map((category) => ({
      label: category.label,
      value: category.key,
    }));
  });

  function getCategoryLabel(taskType: string) {
    const category = categoryList.value.find((item) => item.key === taskType);
    if (category?.label) return category.label;
    const key = systemCategoryLabelKeyMap[taskType.toUpperCase()];
    return key ? t(`system.message.settings.category.${key}`) : taskType;
  }

  function formatScheduleTime(value: number | null) {
    const totalMinutes = Math.floor(((value ?? 8 * 60 * 60 * 1000) % 86400000) / 60000);
    const hours = String(Math.floor(totalMinutes / 60)).padStart(2, '0');
    const minutes = String(totalMinutes % 60).padStart(2, '0');
    return `${hours}:${minutes}`;
  }

  function getConditionValueLabel(value: unknown) {
    if (value === null || value === undefined || value === '') return t('system.message.settings.preview.fieldValue');
    if (Array.isArray(value)) return value.map((item) => String(item)).join(', ');
    if (typeof value === 'object') {
      const option = value as Record<string, unknown>;
      return String(option.label ?? option.name ?? option.value ?? t('system.message.settings.preview.fieldValue'));
    }
    return String(value);
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
  const customActionOptions: SceneActionOption[] = [
    { value: 'create', labelKey: 'create' },
    { value: 'update', labelKey: 'update' },
    { value: 'delete', labelKey: 'delete' },
  ];
  const actionOptions = computed(() => {
    const categoryKey = systemCategoryLabelKeyMap[editor.taskType.toUpperCase()] ?? editor.taskType.toLowerCase();
    const category = getCategoryLabel(editor.taskType);
    const definitions = editor.group === 'custom' ? customActionOptions : categoryActionOptions[categoryKey] ?? [];
    const options: { value: string; label: string }[] = definitions.map((option) => {
      let label: string;
      if (option.categoryLabelKey) {
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
    return actionOptions.value[0]?.value ?? 'create';
  }
  const conditionFilterConfigList = computed<FilterFormItem[]>(() => {
    if (triggerFields.value.length) {
      return triggerFields.value.map((field) => ({
        dataIndex: field.id,
        title: field.name,
        type: field.type,
      }));
    }
    return [
      {
        dataIndex: 'followResult',
        title: 'system.message.settings.conditionField.followResult',
        type: FieldTypeEnum.INPUT,
      },
      { dataIndex: 'status', title: 'system.message.settings.conditionField.status', type: FieldTypeEnum.INPUT },
    ];
  });
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
    const options = [{ label: t('system.message.settings.reminderFrequency.once'), value: 'ONLY_ONE' }];
    if (editor.remindType && !options.some((option) => option.value === editor.remindType)) {
      options.unshift({ label: editor.remindType, value: editor.remindType });
    }
    return options;
  });
  const scenePreviewText = computed(() => {
    const category = getCategoryLabel(editor.taskType);
    if (editor.triggerType === MessageNotificationTriggerEnum.ACTION) {
      const templateValues = editor.templateValue.filter((value) => value.trim());
      if (templateValues.length) {
        const resourceTypeName =
          editorCategoryOptions.value.find((option) => option.value === editor.taskType)?.label ?? category;
        const resourceName = `${resourceTypeName}${t('system.message.settings.preview.nameSuffix')}`;
        return templateValues
          .map((value) =>
            value
              .replace(/\$\{name\}/gi, `「${resourceName}」`)
              .replace(/\$\{operator\}/gi, t('system.message.settings.preview.operator'))
          )
          .join('\n');
      }
      if (
        editor.taskType.toUpperCase() === 'CUSTOMER' &&
        [
          MessageNotificationEventEnum.CUSTOMER_AUTOMATIC_MOVE_HIGH_SEAS,
          MessageNotificationEventEnum.CUSTOMER_MOVED_HIGH_SEAS,
        ].includes(editor.actionTypeConfig as MessageNotificationEventEnum)
      ) {
        return t('system.message.settings.preview.customerMovedToPublicPool');
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
      fieldValue: getConditionValueLabel(condition?.value),
    });
  });
  const editorEditable = computed(() =>
    editorMode.value === 'create' || editorMode.value === 'copy' ? canAdd.value : canUpdate.value
  );
  const isBuiltinSceneUpdate = computed(() => editorMode.value === 'update' && Boolean(editorSource.value?.builtin));
  const editorTriggerEditable = computed(() => editorEditable.value && !isBuiltinSceneUpdate.value);

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

  function parseScheduleTime(value?: string) {
    const [hours = 8, minutes = 0] = (value ?? '08:00:00').replace(/：/g, ':').split(':').map(Number);
    return (hours * 60 + minutes) * 60 * 1000;
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
      builtin: task.type === MessageNotificationTypeEnum.SYSTEM,
      enabled: receiveMethods.length > 0,
      channelKeys,
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
    setLoadListParams({ taskType: activeCategory.value });
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
    editor.taskType = task.taskType;
    editor.group = task.group;
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
      name: mode === 'copy' ? `${task.name}${t('system.message.settings.copySuffix', { id: 1 })}` : task.name,
      triggerType: task.triggerType,
      config,
      actionTypeConfig: config.actionTypeConfig ?? 'create',
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
      await loadList();
      return true;
    } catch {
      Message.error(t('common.saveFailed'));
      return false;
    }
  }

  async function saveSceneScopes(task: SceneRow, scopeIds: string[]) {
    try {
      await updateMessageTask({
        ...getTaskSaveParams(task, task.channelKeys),
        scopeId: [...new Set(scopeIds)],
        id: task.id,
      });
      await loadList();
      return true;
    } catch {
      Message.error(t('common.saveFailed'));
      return false;
    }
  }

  function getSceneOperationGroup(task: SceneRow): ActionsItem[] {
    return [
      { label: t('system.message.settings.configure'), key: 'configure', permission: ['SYSTEM_NOTICE:UPDATE'] },
      { label: t('system.message.settings.copy'), key: 'copy', permission: ['SYSTEM_NOTICE:ADD'] },
      ...(task.group === 'custom'
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
    systemCategories.value = (systemTypes ?? []).map((item) => {
      const categoryKey = systemCategoryLabelKeyMap[item.taskType.toUpperCase()];
      return {
        key: item.taskType,
        label: categoryKey ? t(`system.message.settings.category.${categoryKey}`) : item.name || item.taskType,
        count: Number(item.count ?? 0),
      };
    });
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
    dialog.warning({
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
      width: 50,
      render: (row) =>
        h(NSwitch, {
          'value': row.enabled,
          'disabled': !canUpdate.value || enableLoadingIds.value.has(row.id),
          'loading': enableLoadingIds.value.has(row.id),
          'size': 'small',
          'rubberBand': false,
          'beforeChange': async () => {
            if (enableLoadingIds.value.has(row.id)) return false;
            const loadingIds = new Set(enableLoadingIds.value);
            loadingIds.add(row.id);
            enableLoadingIds.value = loadingIds;

            try {
              await enableMessageTask(row.id);
              return true;
            } catch {
              Message.error(t('common.saveFailed'));
              return false;
            } finally {
              const finishedIds = new Set(enableLoadingIds.value);
              finishedIds.delete(row.id);
              enableLoadingIds.value = finishedIds;
            }
          },
          'onUpdate:value': (enabled: boolean) => {
            row.enabled = enabled;
          },
        }),
    },
    {
      title: t('system.message.notificationScenario'),
      key: 'name',
      width: 136,
      ellipsis: { tooltip: true },
      render: (row) =>
        h('div', { class: 'flex min-w-0 items-center gap-[8px]' }, [
          h(
            'span',
            { class: `message-scene-source ${row.builtin ? 'builtin' : 'custom'}` },
            t(row.builtin ? 'system.message.settings.builtin' : 'system.message.settings.custom')
          ),
          h(
            NTooltip,
            {},
            {
              trigger: () => h('span', { class: 'one-line-text' }, row.name),
              default: () => row.name,
            }
          ),
        ]),
    },
    {
      title: t('system.message.settings.category'),
      key: 'taskType',
      width: 84,
      ellipsis: { tooltip: true },
      render: (row) => getCategoryLabel(row.taskType) || '-',
    },
    {
      title: t('system.message.settings.triggerType'),
      key: 'triggerType',
      width: 100,
      render: (row) =>
        triggerOptions.value.find((option) => option.value === row.triggerType)?.label ?? row.triggerType,
    },
    {
      title: t('system.message.settings.receivers'),
      key: 'members',
      width: 160,
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
      width: 130,
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
      title: t('common.operation'),
      key: 'operation',
      width: 116,
      fixed: 'right',
      render: (row) =>
        h(CrmOperationButton, {
          groupList: getSceneOperationGroup(row),
          onSelect: (key: string) => handleSceneOperation(row, key),
        }),
    },
  ]);

  tableProps.value.columns = columns.value;

  async function loadTaskCategories() {
    try {
      const [systemTypes, customTypes] = await Promise.all([
        getMessageTaskSystemTypes(),
        getMessageTaskCustomFormTypes(),
      ]);
      systemCategories.value = (systemTypes ?? []).map((item: MessageTaskCountDTO) => {
        const categoryKey = systemCategoryLabelKeyMap[item.taskType.toUpperCase()];
        return {
          key: item.taskType,
          label: categoryKey ? t(`system.message.settings.category.${categoryKey}`) : item.name || item.taskType,
          count: Number(item.count ?? 0),
        };
      });
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

  function openCreateScene() {
    const taskType = activeCategory.value || categoryList.value[0]?.key;
    if (!taskType) {
      Message.warning(t('common.pleaseSelect'));
      return;
    }
    editorSource.value = null;
    editorMode.value = 'create';
    Object.assign(editor, {
      name: '',
      taskType,
      group: activeGroup.value,
      triggerType: MessageNotificationTriggerEnum.ACTION,
      config: {},
      scheduleFieldId: 'until_time',
      scheduleDay: 0,
      scheduleTime: 8 * 60 * 60 * 1000,
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
    editor.actionTypeConfig = getDefaultActionTypeConfig();
    showEditor.value = true;
  }

  function handleEditorCategoryChange(taskType: string) {
    editor.taskType = taskType;
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
    const rows = (tableProps.value.data as SceneRow[]).filter((row) => selectedIds.value.includes(row.id));
    const results = await Promise.all(
      rows.map(async (row) => {
        try {
          await updateMessageTask({ ...getTaskSaveParams(row, batchChannelKeys.value), id: row.id });
          return true;
        } catch {
          return false;
        }
      })
    );
    if (results.every(Boolean)) {
      showBatchChannelModal.value = false;
      Message.success(t('system.message.settings.batchChannelsSuccess'));
    } else {
      Message.error(t('common.saveFailed'));
    }
    await loadList();
  }

  async function saveEditor() {
    const source = editorSource.value;
    const name = editor.name.trim();
    if (!name) {
      Message.warning(t('system.message.settings.sceneNameRequired'));
      return;
    }
    if (!editor.channelKeys.length) {
      Message.warning(t('system.message.settings.receiveMethods'));
      return;
    }
    if (!editor.taskType) {
      Message.warning(t('common.pleaseSelect'));
      return;
    }

    if (editor.triggerType === MessageNotificationTriggerEnum.ACTION && !editor.actionTypeConfig) {
      Message.warning(t('system.message.settings.sceneActionRequired'));
      return;
    }
    if (
      editor.triggerType === MessageNotificationTriggerEnum.SCHEDULE &&
      (!editor.scheduleFieldId || editor.scheduleDay < 0 || editor.scheduleTime === null || !editor.remindType)
    ) {
      Message.warning(t('system.message.settings.sceneConditionRequired'));
      return;
    }
    if (
      editor.triggerType === MessageNotificationTriggerEnum.FIELD &&
      !editor.conditionFilter.list.some((condition) => {
        const emptyOperator = [OperatorEnum.EMPTY, OperatorEnum.NOT_EMPTY, OperatorEnum.NEW_NOT_EQUALS_OLD].includes(
          condition.operator as OperatorEnum
        );
        return condition.dataIndex && condition.operator && (emptyOperator || condition.value !== null);
      })
    ) {
      Message.warning(t('system.message.settings.sceneConditionRequired'));
      return;
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
      triggerType: isBuiltinSceneUpdate.value && source ? source.triggerType : editor.triggerType,
      config: isBuiltinSceneUpdate.value && source ? source.config : getEditorConfig(),
      template: isBuiltinSceneUpdate.value && source ? source.template : editor.template || '[]',
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
  }
  .message-settings-category-list {
    min-height: 0;
    flex: 1;
  }
  :deep(.message-settings-category-list .v-vl-visible-items) {
    padding-right: 0 !important;
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
