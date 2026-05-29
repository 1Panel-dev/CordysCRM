<template>
  <n-scrollbar class="p-[16px]" @pointerdown.capture="handleUserInteraction" @keydown.capture="handleUserInteraction">
    <n-form
      :rules="rules"
      class="process-setting-form"
      require-mark-placement="right"
      :model="nodeConfig"
      label-placement="top"
    >
      <n-form-item path="approvalType" :label="t('process.process.flow.approvalType')">
        <n-select
          v-model:value="nodeConfig.approvalType"
          :disabled="props.readonly"
          :options="approvalTypeOptions"
          :placeholder="t('common.pleaseSelect')"
          @update:value="handleApprovalTypeUpdate"
        />
      </n-form-item>

      <n-form-item path="name" :label="t('process.process.flow.nodeName')">
        <n-input
          v-model:value="nodeConfig.name"
          :disabled="props.readonly"
          :maxlength="255"
          type="text"
          :placeholder="t('common.pleaseInput')"
          @update:value="clearCurrentNodeInvalid"
        />
      </n-form-item>

      <template v-if="nodeConfig.approvalType === ApprovalTypeEnum.MANUAL">
        <!-- 审批人 -->
        <n-form-item path="approverType" :label="t('process.process.flow.approver')">
          <n-select
            v-model:value="nodeConfig.approverType"
            :disabled="props.readonly"
            :options="approverTypeOptions"
            :placeholder="t('common.pleaseSelect')"
            @update:value="handleApproverTypeUpdate"
          />
        </n-form-item>

        <!-- 添加成员/角色 -->
        <n-form-item
          v-if="isMemberOrRole(nodeConfig.approverType)"
          path="approverList"
          :show-label="false"
          :show-feedback="false"
        >
          <ApprovalMemberSelector
            :key="nodeConfig.approverType ?? 'none'"
            v-model:value="nodeConfig.approverList"
            v-model:selected-list="nodeConfig.approverSelectedList"
            class="mb-[24px]"
            :label="nodeConfig.approverType === ApproverTypeEnum.ROLE ? t('role.role') : t('org.addMember')"
            :add-text="nodeConfig.approverType === ApproverTypeEnum.ROLE ? t('role.addRole') : t('role.addMember')"
            :limit-label="
              nodeConfig.approverType === ApproverTypeEnum.ROLE ? t('role.role') : t('process.process.flow.member')
            "
            :api-type-key="
              nodeConfig.approverType === ApproverTypeEnum.ROLE
                ? MemberApiTypeEnum.MODULE_ROLE
                : MemberApiTypeEnum.FORM_FIELD
            "
            :member-types="nodeConfig.approverType === ApproverTypeEnum.ROLE ? roleMemberTypes : userMemberTypes"
            :disabled-node-types="
              nodeConfig.approverType === ApproverTypeEnum.ROLE ? undefined : disabledMemberNodeTypes
            "
            required
            :max-count="approverMaxCount"
            :disabled="props.readonly"
            @update:value="clearCurrentNodeInvalid"
          />
        </n-form-item>

        <!-- 指定层级/终点 -->
        <n-form-item v-if="approverLevelConfig" path="approverList" class="specified-level-form-item">
          <template #label>
            <span class="inline-flex items-center gap-[8px]">
              {{ approverLevelConfig.label }}
              <n-tooltip trigger="hover" :delay="300">
                <template #trigger>
                  <CrmIcon
                    :size="16"
                    type="iconicon_help_circle"
                    class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
                  />
                </template>
                {{ approverLevelConfig.tooltip }}
              </n-tooltip>
            </span>
            <ApprovalLevelExamplePopover
              v-if="approverLevelConfig.exampleItems"
              :items="approverLevelConfig.exampleItems"
              :tip="approverLevelConfig.exampleTip"
            />
          </template>

          <div class="flex w-full gap-[8px]">
            <n-select
              v-if="approverLevelConfig.showDirection"
              v-model:value="approverDirection"
              class="w-[120px]"
              :disabled="props.readonly"
              :options="levelDirectionOptions"
              @update:value="clearCurrentNodeInvalid"
            />
            <n-select
              v-model:value="approverLevel"
              class="flex-1"
              :disabled="props.readonly"
              :options="approverLevelConfig.options"
              @update:value="clearCurrentNodeInvalid"
            />
          </div>
        </n-form-item>

        <!-- 多人审批 -->
        <n-form-item path="multiApproverMode" :label="t('process.process.flow.multiApprovalType')">
          <n-radio-group v-model:value="nodeConfig.multiApproverMode" :disabled="props.readonly">
            <n-space vertical :size="8">
              <n-radio v-for="item in multiApproverModeOptions" :key="item.value" :value="item.value">
                {{ item.label }}
                <span v-if="item.description" class="text-[var(--text-n4)]">{{ item.description }}</span>
              </n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>

        <!-- 异常处理 -->
        <div>
          <div class="mb-[8px] inline-flex items-center gap-[8px] font-semibold">
            {{ t('process.process.flow.exceptionHandling') }}

            <n-tooltip
              trigger="hover"
              :delay="300"
              :theme-overrides="{
                color: 'var(--text-n10)',
                textColor: 'var(--text-n1)',
              }"
            >
              <template #trigger>
                <CrmIcon
                  :size="16"
                  type="iconicon_help_circle"
                  class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
                />
              </template>

              <div class="flex flex-col gap-[4px]">
                <div>{{ t('process.process.flow.exceptionHandlingTip.disabledApprover') }}</div>
                <div>
                  {{ t('process.process.flow.exceptionHandlingTip.duplicateApproverPrefix') }}
                  <span class="cursor-pointer text-[var(--primary-8)]" @click.stop="emit('switchMoreSetting')">
                    {{ t('process.processDesign.moreSetting') }}
                  </span>
                  {{ t('process.process.flow.exceptionHandlingTip.duplicateApproverSuffix') }}
                </div>
              </div>
            </n-tooltip>
          </div>

          <CrmTab
            v-model:active-tab="activeExceptionTab"
            no-content
            :tab-list="exceptionTabList"
            type="segment"
            class="approval-exception-tabs mb-[8px]"
            :disabled="props.readonly"
          />

          <template v-if="activeExceptionTab === 'emptyApprover'">
            <n-form-item path="emptyApproverAction" :show-label="false">
              <n-radio-group
                v-model:value="nodeConfig.emptyApproverAction"
                :disabled="props.readonly"
                @update:value="handleEmptyApproverActionUpdate"
              >
                <n-space vertical :size="8">
                  <n-radio :value="EmptyApproverActionEnum.AUTO_PASS">
                    {{ t('process.process.flow.exceptionHandling.autoPass') }}
                  </n-radio>
                  <n-radio :value="EmptyApproverActionEnum.ASSIGN_SPECIFIC">
                    {{ t('process.process.flow.exceptionHandling.toUser') }}
                  </n-radio>
                  <n-radio :value="EmptyApproverActionEnum.ASSIGN_ADMIN">
                    {{ t('process.process.flow.exceptionHandling.toAdmin') }}
                  </n-radio>
                </n-space>
              </n-radio-group>
            </n-form-item>

            <n-form-item
              v-if="nodeConfig.emptyApproverAction === EmptyApproverActionEnum.ASSIGN_SPECIFIC"
              path="fallbackApprover"
              :show-label="false"
              :show-feedback="false"
            >
              <ApprovalMemberSelector
                v-model:value="fallbackApproverList"
                v-model:selected-list="nodeConfig.emptyApproverSelectedList"
                class="mb-[24px]"
                :label="t('org.addMember')"
                :add-text="t('role.addMember')"
                :api-type-key="MemberApiTypeEnum.FORM_FIELD"
                :limit-label="t('process.process.flow.member')"
                :member-types="userMemberTypes"
                :disabled-node-types="disabledMemberNodeTypes"
                :max-count="fallbackApproverMaxCount"
                required
                :disabled="props.readonly"
                @update:value="clearCurrentNodeInvalid"
              />
            </n-form-item>

            <n-form-item
              v-if="nodeConfig.emptyApproverAction === EmptyApproverActionEnum.ASSIGN_ADMIN"
              path="fallbackApprover"
              :label="t('process.process.flow.selectAdmin')"
              required
            >
              <n-select
                v-model:value="nodeConfig.fallbackApprover"
                :disabled="props.readonly"
                :options="approvalAdminOptions"
                :placeholder="t('common.pleaseSelect')"
                @update:value="clearCurrentNodeInvalid"
              />
            </n-form-item>
          </template>

          <template v-else>
            <n-form-item path="sameSubmitterAction" :show-label="false">
              <n-radio-group v-model:value="nodeConfig.sameSubmitterAction" :disabled="props.readonly">
                <n-space vertical :size="8">
                  <n-radio :value="SameSubmitterActionEnum.ALLOW">
                    {{ t('process.process.flow.sameSubmitter.selfApprove') }}
                  </n-radio>
                  <n-radio :value="SameSubmitterActionEnum.SKIP">
                    {{ t('process.process.flow.exceptionHandling.autoPass') }}
                  </n-radio>
                  <n-radio :value="SameSubmitterActionEnum.ASSIGN_SUPERIOR">
                    {{ t('process.process.flow.sameSubmitter.transferSupervisor') }}
                  </n-radio>
                </n-space>
              </n-radio-group>
            </n-form-item>
          </template>
        </div>

        <!-- 抄送人 -->
        <n-form-item path="ccType" :label="t('process.process.flow.ccMember')">
          <n-select
            v-model:value="nodeConfig.ccType"
            :disabled="props.readonly"
            :options="approverTypeOptions"
            clearable
            :placeholder="t('common.pleaseSelect')"
            @update:value="handleCcTypeUpdate"
          />
        </n-form-item>

        <n-form-item
          v-if="nodeConfig.ccType && isMemberOrRole(nodeConfig.ccType)"
          path="ccList"
          :show-label="false"
          :show-feedback="false"
        >
          <ApprovalMemberSelector
            :key="nodeConfig.ccType ?? 'none'"
            v-model:value="nodeConfig.ccList"
            v-model:selected-list="nodeConfig.ccSelectedList"
            :label="nodeConfig.ccType === ApproverTypeEnum.ROLE ? t('role.role') : t('org.addMember')"
            :add-text="nodeConfig.ccType === ApproverTypeEnum.ROLE ? t('role.addRole') : t('role.addMember')"
            :limit-label="
              nodeConfig.ccType === ApproverTypeEnum.ROLE ? t('role.role') : t('process.process.flow.member')
            "
            :api-type-key="
              nodeConfig.ccType === ApproverTypeEnum.ROLE ? MemberApiTypeEnum.MODULE_ROLE : MemberApiTypeEnum.FORM_FIELD
            "
            :member-types="nodeConfig.ccType === ApproverTypeEnum.ROLE ? roleMemberTypes : userMemberTypes"
            :disabled-node-types="nodeConfig.ccType === ApproverTypeEnum.ROLE ? undefined : disabledMemberNodeTypes"
            required
            :max-count="ccMaxCount"
            :disabled="props.readonly"
            @update:value="clearCurrentNodeInvalid"
          />
        </n-form-item>

        <n-form-item v-if="ccLevelConfig" path="ccList" class="specified-level-form-item">
          <template #label>
            <span class="inline-flex items-center gap-[8px]">
              {{ ccLevelConfig.label }}
              <n-tooltip trigger="hover" :delay="300">
                <template #trigger>
                  <CrmIcon
                    :size="16"
                    type="iconicon_help_circle"
                    class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
                  />
                </template>
                {{ ccLevelConfig.tooltip }}
              </n-tooltip>
            </span>

            <ApprovalLevelExamplePopover
              v-if="ccLevelConfig.exampleItems"
              :items="ccLevelConfig.exampleItems"
              :tip="ccLevelConfig.exampleTip"
            />
          </template>

          <div class="flex w-full gap-[8px]">
            <n-select
              v-if="ccLevelConfig.showDirection"
              v-model:value="ccDirection"
              class="w-[120px]"
              :disabled="props.readonly"
              :options="levelDirectionOptions"
              @update:value="clearCurrentNodeInvalid"
            />
            <n-select
              v-model:value="ccLevel"
              class="flex-1"
              :disabled="props.readonly"
              :options="ccLevelConfig.options"
              @update:value="clearCurrentNodeInvalid"
            />
          </div>
        </n-form-item>
      </template>
    </n-form>
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue';
  import {
    type FormRules,
    NForm,
    NFormItem,
    NInput,
    NRadio,
    NRadioGroup,
    NScrollbar,
    NSelect,
    NSpace,
    NTooltip,
  } from 'naive-ui';

  import { MemberApiTypeEnum, MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import {
    ApprovalLevelDirectionEnum,
    ApprovalTypeEnum,
    ApproverTypeEnum,
    EmptyApproverActionEnum,
    MultiApproverModeEnum,
    SameSubmitterActionEnum,
  } from '@lib/shared/enums/process';
  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { ApprovalActionNode } from '@lib/shared/models/system/process';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import ApprovalLevelExamplePopover from './approvalLevelExamplePopover.vue';
  import ApprovalMemberSelector from './approvalMemberSelector.vue';

  import { getAdminOptions } from '@/api/modules';
  import {
    approvalTypeOptions,
    approverLevelOptions,
    approverTypeOptions,
    continuousApproverLevelOptions,
    continuousDepartmentLevelOptions,
    departmentLevelOptions,
    resolveApprovalActionNodeDescription,
  } from '@/config/process';

  import { canClearInvalidState, clearInvalidState, unlockInvalidClearState } from '../../flow/validation';

  defineOptions({
    name: 'ApproverSettingTab',
  });

  const props = defineProps<{
    readonly?: boolean;
  }>();

  const nodeConfig = defineModel<ApprovalActionNode>('nodeConfig', {
    required: true,
  });

  const emit = defineEmits<{
    (event: 'switchMoreSetting'): void;
  }>();

  const { t } = useI18n();

  const activeExceptionTab = ref<'emptyApprover' | 'sameSubmitter'>('emptyApprover'); // 异常处理的tab

  const approverMaxCount = 15;
  const ccMemberMaxCount = 100;
  const ccRoleMaxCount = 15;
  const fallbackApproverMaxCount = 1;

  const ccMaxCount = computed(() =>
    nodeConfig.value.ccType === ApproverTypeEnum.ROLE ? ccRoleMaxCount : ccMemberMaxCount
  );

  const supervisorLevelApproverTypes = [ApproverTypeEnum.DIRECT_SUPERVISOR, ApproverTypeEnum.CONTINUOUS_SUPERVISOR];

  const departmentLevelApproverTypes = [
    ApproverTypeEnum.SPECIFIED_DEPARTMENT_LEADER,
    ApproverTypeEnum.CONTINUOUS_DEPARTMENT_LEADER,
  ];

  const endpointApproverTypes = [ApproverTypeEnum.CONTINUOUS_SUPERVISOR, ApproverTypeEnum.CONTINUOUS_DEPARTMENT_LEADER];

  const defaultLevelDirection = ApprovalLevelDirectionEnum.BOTTOM_UP;
  const levelDirectionOptions = [
    {
      label: t('process.process.flow.levelDirection.bottomUp'),
      value: ApprovalLevelDirectionEnum.BOTTOM_UP,
    },
    {
      label: t('process.process.flow.levelDirection.topDown'),
      value: ApprovalLevelDirectionEnum.TOP_DOWN,
    },
  ];

  const roleMemberTypes = [
    {
      label: t('role.role'),
      value: MemberSelectTypeEnum.ROLE,
    },
  ];

  const userMemberTypes = [
    {
      label: t('menu.settings.org'),
      value: MemberSelectTypeEnum.ORG,
    },
  ];

  const disabledMemberNodeTypes = [DeptNodeTypeEnum.ORG, DeptNodeTypeEnum.ROLE];

  const exceptionTabList = [
    {
      name: 'emptyApprover',
      tab: t('process.process.flow.exceptionHandling.emptyApprover'),
    },
    {
      name: 'sameSubmitter',
      tab: t('process.process.flow.exceptionHandling.sameSubmitter'),
    },
  ];

  const directSupervisorExampleItems = [
    {
      level: t('process.process.flow.levelExample.thirdLevelSupervisor'),
      name: t('process.process.flow.levelExample.supervisorD'),
    },
    {
      level: t('process.process.flow.levelExample.secondLevelSupervisor'),
      name: t('process.process.flow.levelExample.supervisorC'),
    },
    {
      level: t('org.directSuperior'),
      name: t('process.process.flow.levelExample.supervisorB'),
    },
    {
      level: t('process.process.flow.levelExample.applicant'),
      name: t('process.process.flow.levelExample.employeeA'),
    },
  ];

  const departmentLeaderExampleItems = [
    {
      level: t('process.process.flow.levelExample.fourthLevelDepartment'),
      name: t('process.process.flow.levelExample.departmentD'),
    },
    {
      level: t('process.process.flow.levelExample.thirdLevelDepartment'),
      name: t('process.process.flow.levelExample.departmentC'),
    },
    {
      level: t('process.process.flow.levelExample.secondLevelDepartment'),
      name: t('process.process.flow.levelExample.departmentB'),
    },
    {
      level: t('process.process.flow.levelExample.directDepartment'),
      name: t('process.process.flow.levelExample.departmentA'),
    },
    {
      level: t('process.process.flow.levelExample.applicant'),
      name: t('process.process.flow.levelExample.departmentApplicant'),
    },
  ];

  const approverLevel = computed({
    get() {
      return nodeConfig.value.approverList[0] ?? '1';
    },
    set(value: string) {
      nodeConfig.value.approverList = [value];
    },
  });

  const approverDirection = computed({
    get() {
      return nodeConfig.value.approverDirection ?? defaultLevelDirection;
    },
    set(value: ApprovalLevelDirectionEnum) {
      nodeConfig.value.approverDirection = value;
    },
  });

  const ccLevel = computed({
    get() {
      return nodeConfig.value.ccList[0] ?? '1';
    },
    set(value: string) {
      nodeConfig.value.ccList = [value];
    },
  });

  const ccDirection = computed({
    get() {
      return nodeConfig.value.ccDirection ?? defaultLevelDirection;
    },
    set(value: ApprovalLevelDirectionEnum) {
      nodeConfig.value.ccDirection = value;
    },
  });

  const fallbackApproverList = computed({
    get() {
      return nodeConfig.value.fallbackApprover ? [nodeConfig.value.fallbackApprover] : [];
    },
    set(value: string[]) {
      nodeConfig.value.fallbackApprover = value[0] ?? null;
    },
  });

  const multiApproverModeOptions: Array<{
    value: MultiApproverModeEnum;
    label: string;
    description: string;
  }> = [
    {
      value: MultiApproverModeEnum.ALL,
      label: t('process.process.flow.multiApprovalType.all'),
      description: t('process.process.flow.multiApprovalType.all.description'),
    },
    {
      value: MultiApproverModeEnum.ANY,
      label: t('process.process.flow.multiApprovalType.majority'),
      description: t('process.process.flow.multiApprovalType.majority.description'),
    },
    {
      value: MultiApproverModeEnum.SEQUENTIAL,
      label: t('process.process.flow.multiApprovalType.sequential'),
      description: t('process.process.flow.multiApprovalType.sequential.description'),
    },
  ];

  function isMemberOrRole(type?: ApproverTypeEnum | null) {
    return !!type && [ApproverTypeEnum.SPECIFIED_MEMBER, ApproverTypeEnum.ROLE].includes(type);
  }

  // 当前节点被保存校验打红后，只有用户真的开始编辑右侧表单，才允许清掉红框
  function clearCurrentNodeInvalid() {
    if (props.readonly || !canClearInvalidState()) {
      return;
    }

    clearInvalidState(nodeConfig.value);
  }

  // 用 capture 提前感知用户交互，先解锁“自动清红框”，再让各个 update 事件继续执行
  function handleUserInteraction() {
    if (props.readonly) {
      return;
    }

    unlockInvalidClearState();
  }

  function hasSelectedItems(value: unknown[]) {
    return Array.isArray(value) && value.some((item) => item !== null && item !== undefined && String(item).trim());
  }

  const rules: FormRules = {
    name: [
      {
        required: true,
        message: t('common.notNull', {
          value: t('process.process.flow.nodeName'),
        }),
        trigger: ['blur'],
      },
    ],
    approverType: [
      {
        required: true,
        message: t('common.notNull', {
          value: t('process.process.flow.approver'),
        }),
        trigger: ['change', 'blur'],
      },
    ],
    approverList: [
      {
        trigger: ['change', 'blur'],
        validator: (_rule, value: unknown[]) => {
          if (!isMemberOrRole(nodeConfig.value.approverType)) {
            return true;
          }

          if (hasSelectedItems(value) && value.length <= approverMaxCount) {
            return true;
          }

          return new Error(
            t('process.process.flow.addMemberLimitTip', {
              count: approverMaxCount,
              target:
                nodeConfig.value.approverType === ApproverTypeEnum.ROLE
                  ? t('role.role')
                  : t('process.process.flow.member'),
            })
          );
        },
      },
    ],
    ccList: [
      {
        trigger: ['change', 'blur'],
        validator: (_rule, value: unknown[]) => {
          if (!nodeConfig.value.ccType || !isMemberOrRole(nodeConfig.value.ccType)) {
            return true;
          }

          if (hasSelectedItems(value) && value.length <= ccMaxCount.value) {
            return true;
          }

          const target =
            nodeConfig.value.ccType === ApproverTypeEnum.ROLE ? t('role.role') : t('process.process.flow.ccMember');

          return new Error(t('process.process.flow.addMemberLimitTip', { count: ccMaxCount.value, target }));
        },
      },
    ],
    fallbackApprover: [
      {
        trigger: ['change', 'blur'],
        validator: (_rule, value: string | null) => {
          if (activeExceptionTab.value !== 'emptyApprover') {
            return true;
          }

          if (nodeConfig.value.emptyApproverAction === EmptyApproverActionEnum.ASSIGN_SPECIFIC) {
            return (
              !!value ||
              new Error(
                t('process.process.flow.addMemberLimitTip', {
                  count: fallbackApproverMaxCount,
                  target: t('process.process.flow.member'),
                })
              )
            );
          }

          if (nodeConfig.value.emptyApproverAction === EmptyApproverActionEnum.ASSIGN_ADMIN) {
            return !!value || new Error(t('common.notNull', { value: t('process.process.flow.selectAdmin') }));
          }

          return true;
        },
      },
    ],
  };

  function getApproverLevelTooltip(type: ApproverTypeEnum) {
    const tooltipMap: Partial<Record<ApproverTypeEnum, string>> = {
      [ApproverTypeEnum.DIRECT_SUPERVISOR]: t('process.process.flow.directSupervisorTip'),
      [ApproverTypeEnum.CONTINUOUS_SUPERVISOR]: t('process.process.flow.continuousSupervisorTip'),
      [ApproverTypeEnum.SPECIFIED_DEPARTMENT_LEADER]: t('process.process.flow.departmentLeaderTip'),
      [ApproverTypeEnum.CONTINUOUS_DEPARTMENT_LEADER]: t('process.process.flow.continuousDepartmentLeaderTip'),
    };

    return tooltipMap[type] ?? '';
  }

  function createLevelConfig(type: ApproverTypeEnum) {
    const isSupervisorLevel = supervisorLevelApproverTypes.includes(type);
    const isDepartmentLevel = departmentLevelApproverTypes.includes(type);
    const isEndpoint = endpointApproverTypes.includes(type);

    if (!isSupervisorLevel && !isDepartmentLevel) {
      return null;
    }

    let options = isDepartmentLevel ? departmentLevelOptions : approverLevelOptions;
    if (isEndpoint) {
      options = isDepartmentLevel ? continuousDepartmentLevelOptions : continuousApproverLevelOptions;
    }

    return {
      label: isEndpoint ? t('process.process.flow.specifiedEndpoint') : t('process.process.flow.specifiedLevel'),
      tooltip: getApproverLevelTooltip(type),
      options,
      showDirection: isEndpoint,
      exampleItems: isDepartmentLevel ? departmentLeaderExampleItems : directSupervisorExampleItems,
      exampleTip: isDepartmentLevel ? t('process.process.flow.levelExample.departmentTip') : undefined,
    };
  }

  const approverLevelConfig = computed(() =>
    nodeConfig.value.approverType ? createLevelConfig(nodeConfig.value.approverType) : null
  );

  const ccLevelConfig = computed(() => {
    return nodeConfig.value.ccType ? createLevelConfig(nodeConfig.value.ccType) : null;
  });

  function resetLevelList(type: ApproverTypeEnum | null) {
    return type && [...supervisorLevelApproverTypes, ...departmentLevelApproverTypes].includes(type) ? ['1'] : [];
  }

  function handleApprovalTypeUpdate(type: ApprovalTypeEnum) {
    if (props.readonly) {
      return;
    }

    nodeConfig.value.description = resolveApprovalActionNodeDescription(type, nodeConfig.value.approverType);
    clearCurrentNodeInvalid();
  }

  function handleApproverTypeUpdate(type: ApproverTypeEnum | null) {
    if (props.readonly) {
      return;
    }

    nodeConfig.value.approverSelectedList = [];
    nodeConfig.value.approverList = resetLevelList(type);
    nodeConfig.value.approverDirection = defaultLevelDirection;
    nodeConfig.value.description = resolveApprovalActionNodeDescription(nodeConfig.value.approvalType, type);
    clearCurrentNodeInvalid();
  }

  function handleCcTypeUpdate(type: ApproverTypeEnum | null) {
    if (props.readonly) {
      return;
    }

    nodeConfig.value.ccSelectedList = [];
    nodeConfig.value.ccList = resetLevelList(type);
    nodeConfig.value.ccDirection = defaultLevelDirection;
    clearCurrentNodeInvalid();
  }

  function handleEmptyApproverActionUpdate() {
    if (props.readonly) {
      return;
    }

    nodeConfig.value.fallbackApprover = null;
    nodeConfig.value.emptyApproverSelectedList = [];
    clearCurrentNodeInvalid();
  }

  // 管理员
  const approvalAdminOptions = ref<Array<{ label: string; value: string }>>([]);
  async function getApprovalAdminOptions() {
    try {
      const options = await getAdminOptions();

      approvalAdminOptions.value = options.map((item) => ({
        label: item.name,
        value: item.id,
      }));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  onMounted(() => {
    getApprovalAdminOptions();
  });
</script>

<style scoped lang="less">
  .specified-level-form-item {
    :deep(.n-form-item-label__text) {
      display: flex;
      justify-content: space-between;
      width: 100%;
    }
  }
  .approval-exception-tabs {
    :deep(.n-tabs-rail) {
      width: 100%;
    }
  }
</style>
