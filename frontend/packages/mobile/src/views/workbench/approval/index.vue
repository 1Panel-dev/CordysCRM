<template>
  <CrmPageWrapper :title="sourceName || ''">
    <van-tabs v-model:active="activeName" border class="approval-tabs">
      <van-tab v-for="tab of tabList" :key="tab.name" :name="tab.name">
        <template #title>
          <div class="text-[16px]" :class="activeName === tab.name ? 'text-[var(--primary-8)]' : ''">
            {{ tab.title }}
          </div>
        </template>
        <div v-if="tab.name === 'detail'" class="relative h-full overflow-auto bg-[var(--text-n9)] py-[16px]">
          <CrmDescription :description="renderDescriptions" :source-id="sourceId">
            <template #approvalStatus>
              <ApprovalStatus v-if="approvalInfo" :status="approvalInfo?.approvalStatus" />
            </template>
            <template #quotationStatus="{ item }">
              <CrmTag :tag="item.value ? t('common.voided') : t('common.normal')" />
            </template>
          </CrmDescription>
        </div>
        <div v-else class="flex h-full bg-[var(--text-n9)] p-[16px]">
          <CrmApprovalLine
            :nodes="approvalInfo?.nodes || []"
            :submitter="{
              submitAvatar: approvalInfo?.submitAvatar,
              submitter: approvalInfo?.submitter,
              submitTime: approvalInfo?.submitTime,
              submitterId: approvalInfo?.submitterId,
              comment: approvalInfo?.comment,
            }"
            :currentApprovalNode="currentApprovalNode"
            :currentApprovalNodeIndex="currentApprovalNodeIndex"
            :finally-result="approvalInfo?.approvalStatus"
          />
        </div>
      </van-tab>
    </van-tabs>
    <template v-if="approvalInfo?.approvalStatus === ProcessStatusEnum.APPROVING" #footer>
      <CrmApprovalRecordActions
        :source-id="sourceId"
        :form-key="approvalFormKey"
        :approval-info="approvalInfo"
        :approval-config="approvalConfig"
        :current-approval-node="currentApprovalNode"
        :current-approval-node-index="currentApprovalNodeIndex"
        @refresh="refresh"
        @approval-success="router.back()"
        @cancel-success="router.back()"
      />
    </template>
  </CrmPageWrapper>
</template>
<script setup lang="ts">
  import { getApprovalConfigDetail, getApprovalResourceDetail } from '@/api/modules';
  import useUserStore from '@/store/modules/user';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { ApprovalFieldPermissionModeEnum, ProcessStatusEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type {
    ApprovalDetail,
    ApprovalFieldPermission,
    ApprovalNode,
    ApprovalProcessDetail,
  } from '@lib/shared/models/system/process';
  import CrmDescription, { type CrmDescriptionItem } from '@/components/pure/crm-description/index.vue';
  import { useRoute } from 'vue-router';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import router from '@/router/index.js';
  import ApprovalStatus from '@/components/business/crm-approval/crm-approval-status.vue';
  import CrmApprovalLine from '@/components/business/crm-approval/crm-approval-line.vue';
  import CrmApprovalRecordActions from '@/components/business/crm-approval/crm-approval-record-actions.vue';

  const { t } = useI18n();
  const userStore = useUserStore();
  const route = useRoute();

  const sourceId = computed(() => route.query.id?.toString() ?? '');
  const routeFormKey = computed(() => route.query.formKey?.toString() as FormDesignKeyEnum);
  const customFormId = computed(() => route.query.customFormId?.toString());
  const approvalFormKey = computed(() =>
    routeFormKey.value === FormDesignKeyEnum.CUSTOM_FORM
      ? customFormId.value || ''
      : routeFormKey.value.replace('Snapshot', '')
  );

  const { sourceName, descriptions, detail, initFormConfig, initFormDescription } = useFormCreateApi({
    formKey: routeFormKey.value,
    sourceId,
    needInitDetail: true,
    otherSaveParams: {
      approvalTaskId: route.query.taskId,
    },
    customFormId,
  });

  const tabList = [
    {
      name: 'detail',
      title: t('common.detail'),
    },
    {
      name: 'record',
      title: t('workbench.approval.record'),
    },
  ];
  const activeName = ref(tabList[0].name);

  const approvalInfo = ref<ApprovalDetail>();
  const approvalConfig = ref<ApprovalProcessDetail>(); // 审批配置详情

  async function initApprovalConfig() {
    try {
      if (approvalFormKey.value) {
        approvalConfig.value = await getApprovalConfigDetail(approvalFormKey.value);
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const currentApprovalNode = ref<ApprovalNode>();
  const currentApprovalNodeIndex = ref(0);
  const currentTaskNode = computed(() => {
    if (!currentApprovalNode.value) {
      return undefined;
    }
    return currentApprovalNode.value.taskNodes?.find(
      (e) => e.approvalStatus === ProcessStatusEnum.APPROVING && e.approverId === userStore.userInfo.id
    );
  });
  // 只有当前审批中的人才展示编辑权限，其他节点展示只读权限
  const filedPermission = computed(() => {
    if (currentTaskNode.value && approvalInfo.value?.currentNodeFieldPermissions) {
      return (JSON.parse(approvalInfo.value?.currentNodeFieldPermissions || '[]') as ApprovalFieldPermission[]) || [];
    }
    return [];
  });
  const hiddenFieldByPermission = computed(
    () =>
      filedPermission.value
        ?.filter((e) => e.permissionType === ApprovalFieldPermissionModeEnum.HIDDEN)
        .map((e) => e.fieldId) || []
  );

  const renderDescriptions = computed(
    () =>
      [
        {
          label: t('workbench.approvalStatus'),
          value: approvalInfo.value?.approvalStatus,
          valueSlotName: 'approvalStatus',
        },
        route.query.formKey === FormDesignKeyEnum.OPPORTUNITY_QUOTATION_SNAPSHOT
          ? {
              label: t('workbench.quotationStatus'),
              value: detail.value.invalid,
              valueSlotName: 'quotationStatus',
            }
          : null,
        ...descriptions.value.filter((e) => e.fieldInfo && !hiddenFieldByPermission.value?.includes(e.fieldInfo.id)),
      ].filter(Boolean) as CrmDescriptionItem[]
  );

  const noApproval = ref(false);
  async function initApprovalDetail() {
    try {
      approvalInfo.value = await getApprovalResourceDetail(route.query.id?.toString() || '');
      if (!approvalInfo.value) {
        noApproval.value = true;
      }
      currentApprovalNodeIndex.value = approvalInfo.value?.nodes.findIndex(
        (node) => node.nodeId === approvalInfo.value?.currentNodeId
      );
      currentApprovalNode.value = approvalInfo.value?.nodes[currentApprovalNodeIndex.value];
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function refresh() {
    initApprovalConfig();
    initFormDescription();
    initApprovalDetail();
  }

  onBeforeMount(async () => {
    initApprovalConfig();
    await initFormConfig();
    initFormDescription();
    initApprovalDetail();
  });
</script>
<style lang="less" scoped>
  .approval-tabs {
    @apply h-full;
    :deep(.van-tabs__content) {
      height: calc(100% - var(--van-tabs-line-height));
      .van-tab__panel {
        @apply h-full;
      }
    }
  }
</style>
