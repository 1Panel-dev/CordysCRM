<template>
  <n-popover
    placement="right"
    :show-arrow="false"
    :theme-overrides="{
      padding: '16px',
      color: 'var(--text-n10)',
      textColor: 'var(--text-n1)',
    }"
  >
    <template #trigger>
      <span class="cursor-pointer font-normal text-[var(--primary-8)]">
        {{ t('process.process.flow.viewExample') }}
      </span>
    </template>

    <div class="w-[440px]">
      <div class="mb-[16px] flex items-center gap-[8px]">
        <span class="font-semibold text-[var(--text-n1)]">{{ directionLabel }}</span>
        <CrmTag theme="light" type="info" tooltip-disabled>
          {{ countRuleText }}
        </CrmTag>
        <CrmTag theme="light" type="primary" tooltip-disabled>
          {{ exampleText }}
        </CrmTag>
      </div>

      <div class="flex flex-col">
        <template v-for="(item, index) in displayNodes" :key="item.key">
          <div
            class="grid grid-cols-[32px_1fr] items-center gap-[16px]"
            :class="{ 'mt-[16px]': index > 0 && !showConnector(index - 1) }"
          >
            <div
              class="flex h-[32px] w-[32px] items-center justify-center rounded-[4px]"
              :class="
                item.state !== 'inactive'
                  ? 'bg-[var(--info-5)] text-[var(--info-blue)]'
                  : 'bg-[var(--text-n9)] text-[var(--text-n2)]'
              "
            >
              {{ item.index }}
            </div>
            <div
              class="flex items-center justify-between gap-[16px] rounded-[4px] px-[12px] py-[8px]"
              :class="item.state !== 'inactive' ? 'bg-[var(--info-5)]' : 'bg-[var(--text-n9)]'"
            >
              <div>
                <div :class="item.state !== 'inactive' ? 'text-[var(--info-blue)]' : 'text-[var(--text-n2)]'">
                  {{ item.name }}
                </div>
                <div class="mt-[2px] text-[12px] text-[var(--text-n4)]">{{ item.description }}</div>
              </div>
              <span v-if="item.state === 'inactive'" class="whitespace-nowrap text-[12px] text-[var(--text-n4)]">
                {{ getNodeAction(item.state) }}
              </span>
              <CrmTag v-else theme="dark" type="info" tooltip-disabled>
                {{ getNodeAction(item.state) }}
              </CrmTag>
            </div>
          </div>

          <CrmIcon
            v-if="showConnector(index)"
            type="iconicon_arrow_up"
            :size="16"
            class="self-center text-[var(--text-n4)]"
          />
        </template>

        <div
          class="grid grid-cols-[32px_1fr] items-center gap-[16px]"
          :class="{ 'mt-[16px]': !showConnector(displayNodes.length - 1) }"
        >
          <div
            class="flex h-[32px] w-[32px] items-center justify-center rounded-[4px] bg-[var(--text-n7)] text-[var(--text-n10)]"
          >
            {{ t('process.process.flow.levelExample.start') }}
          </div>
          <div
            class="flex items-center justify-between gap-[16px] rounded-[4px] bg-[var(--text-n9)] px-[12px] py-[8px]"
          >
            <div>
              <div class="text-[var(--text-n1)]">{{ startNode.name }}</div>
              <div class="mt-[2px] text-[12px] text-[var(--text-n4)]">{{ startNode.description }}</div>
            </div>
            <span class="whitespace-nowrap text-[12px] text-[var(--text-n4)]">
              {{ t('process.process.flow.levelExample.submitApplication') }}
            </span>
          </div>
        </div>

        <div class="mt-[16px] rounded-[4px] bg-[var(--text-n9)] px-[12px] py-[8px] text-[12px] text-[var(--text-n4)]">{{
          pathText
        }}</div>
      </div>
    </div>
  </n-popover>
</template>

<script setup lang="ts">
  import { computed } from 'vue';
  import { NPopover } from 'naive-ui';

  import { ApprovalLevelDirectionEnum, ApproverTypeEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';

  defineOptions({
    name: 'ApprovalLevelExamplePopover',
  });

  type NodeState = 'inactive' | 'endpoint' | 'participant';

  interface ExampleNode {
    key: string;
    name: string;
    description: string;
    bottomUpIndex: string;
    topDownIndex: string;
  }

  interface DisplayNode extends ExampleNode {
    index: string;
    state: NodeState;
  }

  const props = defineProps<{
    type: ApproverTypeEnum;
    direction: ApprovalLevelDirectionEnum;
  }>();

  const { t } = useI18n();

  const isDepartmentType = computed(() =>
    [ApproverTypeEnum.SPECIFIED_DEPARTMENT_LEADER, ApproverTypeEnum.CONTINUOUS_DEPARTMENT_LEADER].includes(props.type)
  );
  const isContinuousType = computed(() =>
    [ApproverTypeEnum.CONTINUOUS_SUPERVISOR, ApproverTypeEnum.CONTINUOUS_DEPARTMENT_LEADER].includes(props.type)
  );
  const isBottomUp = computed(() => props.direction === ApprovalLevelDirectionEnum.BOTTOM_UP);

  const directionLabel = computed(() =>
    isBottomUp.value
      ? t('process.process.flow.levelDirection.bottomUp')
      : t('process.process.flow.levelDirection.topDown')
  );
  const countRuleText = computed(() =>
    isBottomUp.value
      ? t('process.process.flow.levelExample.countFromApplicant')
      : t('process.process.flow.levelExample.countFromTop')
  );
  const exampleText = computed(() =>
    isDepartmentType.value
      ? t('process.process.flow.levelExample.secondDepartment')
      : t('process.process.flow.levelExample.secondSupervisor')
  );

  const supervisorNodes = computed<ExampleNode[]>(() => [
    {
      key: 'supervisor-e',
      name: t('process.process.flow.levelExample.supervisor.name.e'),
      description: t('process.process.flow.levelExample.supervisor.description.e'),
      bottomUpIndex: '3',
      topDownIndex: '1',
    },
    {
      key: 'supervisor-d',
      name: t('process.process.flow.levelExample.supervisor.name.d'),
      description: t('process.process.flow.levelExample.supervisor.description.d'),
      bottomUpIndex: '2',
      topDownIndex: '2',
    },
    {
      key: 'supervisor-c',
      name: t('process.process.flow.levelExample.supervisor.name.c'),
      description: t('process.process.flow.levelExample.supervisor.description.c'),
      bottomUpIndex: '1',
      topDownIndex: '3',
    },
  ]);

  const departmentNodes = computed<ExampleNode[]>(() => [
    {
      key: 'department-province',
      name: t('process.process.flow.levelExample.department.name.province'),
      description: t(
        isBottomUp.value
          ? 'process.process.flow.levelExample.aboveEndpoint'
          : 'process.process.flow.levelExample.orgTop'
      ),
      bottomUpIndex: '4',
      topDownIndex: '1',
    },
    {
      key: 'department-city',
      name: t('process.process.flow.levelExample.department.name.city'),
      description: t(
        isBottomUp.value
          ? 'process.process.flow.levelExample.aboveEndpoint'
          : 'process.process.flow.levelExample.ownerApprovedStop'
      ),
      bottomUpIndex: '3',
      topDownIndex: '2',
    },
    {
      key: 'department-area',
      name: t('process.process.flow.levelExample.department.name.area'),
      description: t(
        isBottomUp.value
          ? 'process.process.flow.levelExample.ownerApprovedStop'
          : 'process.process.flow.levelExample.owner'
      ),
      bottomUpIndex: '2',
      topDownIndex: '3',
    },
    {
      key: 'department-dept',
      name: t('process.process.flow.levelExample.department.name.dept'),
      description: t('process.process.flow.levelExample.owner'),
      bottomUpIndex: '1',
      topDownIndex: '4',
    },
  ]);

  const nodes = computed(() => (isDepartmentType.value ? departmentNodes.value : supervisorNodes.value));
  const endpointIndex = computed(() => {
    if (isDepartmentType.value) {
      return isBottomUp.value ? 2 : 1;
    }

    return 1;
  });

  const displayNodes = computed<DisplayNode[]>(() => {
    return nodes.value.map((node, index) => {
      let state: NodeState = 'inactive';

      if (index === endpointIndex.value) {
        state = 'endpoint';
      } else if (isContinuousType.value && index > endpointIndex.value) {
        state = 'participant';
      }

      return {
        ...node,
        index: isBottomUp.value ? node.bottomUpIndex : node.topDownIndex,
        state,
      };
    });
  });

  const startNode = computed(() => {
    if (isDepartmentType.value) {
      return {
        name: t('process.process.flow.levelExample.department.name.applicant'),
        description: t('process.process.flow.levelExample.applicantDepartment'),
      };
    }

    return {
      name: t('process.process.flow.levelExample.supervisor.name.applicant'),
      description: t('process.process.flow.levelExample.applicant'),
    };
  });

  const pathText = computed(() => {
    const activeNames = displayNodes.value
      .filter((item) => item.state !== 'inactive')
      .reverse()
      .map((item) => item.name);

    if (!isContinuousType.value) {
      if (!isDepartmentType.value) {
        return t('process.process.flow.levelExample.applicantNoUpgrade');
      }

      const endpointName = activeNames[activeNames.length - 1];

      return t('process.process.flow.levelExample.approvalPath', {
        path: t('process.process.flow.levelExample.departmentOwner', { department: endpointName }),
      });
    }

    if (isDepartmentType.value) {
      return t('process.process.flow.levelExample.approvalPath', {
        path: [
          t('process.process.flow.levelExample.applicant'),
          ...activeNames.map((name) =>
            t('process.process.flow.levelExample.departmentOwner', {
              department: name,
            })
          ),
        ].join(' → '),
      });
    }

    return t('process.process.flow.levelExample.approvalSequence', {
      path: [
        t('process.process.flow.levelExample.applicantStart', { applicant: startNode.value.name }),
        ...activeNames.map((name) => t('process.process.flow.levelExample.supervisorApprove', { supervisor: name })),
        t('process.process.flow.levelExample.end'),
      ].join(' → '),
    });
  });

  function getNodeAction(state: NodeState): string {
    if (state === 'endpoint') {
      return t('process.process.flow.levelExample.endpointStop');
    }

    if (state === 'participant') {
      return t('process.process.flow.levelExample.participateApproval');
    }

    return isDepartmentType.value
      ? t('process.process.flow.levelExample.notParticipateReview')
      : t('process.process.flow.levelExample.notParticipateApproval');
  }

  function showConnector(index: number): boolean {
    if (!isContinuousType.value) {
      return false;
    }

    return displayNodes.value[index]?.state !== 'inactive' && displayNodes.value[index + 1]?.state !== 'inactive';
  }
</script>
