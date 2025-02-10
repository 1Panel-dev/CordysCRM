<template>
  <div class="w-full p-[24px]">
    <div class="mb-[16px] flex items-center justify-between">
      <div class="flex">
        <n-button class="mr-[12px]" type="primary" @click="addOrEditMember(false)">{{ t('org.addStaff') }}</n-button>
        <CrmMoreAction :options="moreActions" trigger="click" @select="selectMoreActions">
          <n-button type="default" class="outline--secondary">
            {{ t('common.more') }}
            <CrmIcon class="ml-[8px]" type="iconicon_chevron_down" :size="16" />
          </n-button>
        </CrmMoreAction>
      </div>
      <CrmSearchInput v-model:value="keyword" class="!w-[240px]" @search="searchData" />
    </div>
    <CrmTable
      v-bind="propsRes"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
    />

    <AddMember v-model:show="showDrawer" :user-id="currentUserId" @brash="initOrgList()" @close="cancelHandler" />
    <EditIntegrationModal
      v-model:show="showSyncWeChatModal"
      :title="t('system.business.syncFrom', { name: t('system.business.WE_COM') })"
      :integration="currentIntegration"
      @init-sync="updateConfig"
    />
    <MemberDetail
      v-model:show="showDetailModal"
      :user-id="currentUserId"
      @edit="addOrEditMember(true)"
      @cancel="cancelHandler"
    />
    <batchEditModal v-model:show="showEditModal" />
    <!-- 导入开始 -->
    <!-- 导入弹窗 -->
    <ImportModal v-model:show="importModal" :confirm-loading="validateLoading" @validate="validateTemplate" />

    <!-- 校验弹窗 -->
    <ValidateModal
      v-model:show="validateModal"
      :percent="progress"
      @cancel="cancelValidate"
      @check-finished="checkFinished"
    />

    <!-- 校验结果弹窗 -->
    <ValidateResult
      v-model:show="validateResultModal"
      :validate-info="validateInfo"
      :import-loading="importLoading"
      @close="importModal = false"
    />
    <!-- 导入结束 -->
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton, NSwitch, NTooltip, useMessage } from 'naive-ui';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import type { CrmFileItem } from '@/components/pure/crm-upload/types';
  import CrmEditableText from '@/components/business/crm-editable-text/index.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import AddMember from './addMember.vue';
  import batchEditModal from './batchEditModal.vue';
  import MemberDetail from './memberDetail.vue';
  import EditIntegrationModal from '@/views/system/business/components/editIntegrationModal.vue';
  import ImportModal from '@/views/system/org/components/import/importModal.vue';
  import ValidateModal from '@/views/system/org/components/import/validateModal.vue';
  import ValidateResult from '@/views/system/org/components/import/validateResult.vue';

  import { getConfigSynchronization } from '@/api/modules/system/business';
  import { getUserList, resetUserPassword, syncOrg, updateUser } from '@/api/modules/system/org';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';
  import useProgressBar from '@/hooks/useProgressBar';
  import { characterLimit, getCityPath } from '@/utils';

  import { CompanyTypeEnum } from '@/enums/commonEnum';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import type { ConfigSynchronization } from '@lib/shared/models/system/business';
  import type { MemberItem } from '@lib/shared/models/system/org';

  const Message = useMessage();

  const { openModal } = useModal();

  const { t } = useI18n();

  const props = defineProps<{
    activeNode: string | number;
  }>();

  const emit = defineEmits<{
    (e: 'addSuccess'): void;
  }>();

  /**
   * 设置同步微信
   */

  const showSyncWeChatModal = ref<boolean>(false);
  const currentIntegration = ref<ConfigSynchronization>({
    id: '',
    type: CompanyTypeEnum.WECOM,
    corpId: '',
    agentId: '',
    appSecret: '',
    enable: true,
  });

  async function settingWeChat(e: MouseEvent) {
    e.stopPropagation();
    const res = await getConfigSynchronization();
    [currentIntegration.value] = res;
    showSyncWeChatModal.value = true;
  }

  const isHasConfigPermission = ref<boolean>(true); // 有配置权限
  const isHasSyncPermission = ref<boolean>(true); // 有同步权限
  const isHasConfig = ref<boolean>(false); // 已配置

  async function handleSync() {
    if (!isHasConfig.value) {
      return false;
    }
    try {
      await syncOrg(CompanyTypeEnum.WECOM);
      Message.success(t('org.syncSuccess'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function renderSync() {
    if (!isHasSyncPermission.value && !isHasConfigPermission.value) {
      return null;
    }
    return h(
      'div',
      {
        class: `flex items-center ${
          isHasConfigPermission.value && isHasConfig.value ? 'text-[var(--text-n1)]' : 'text-[var(--text-n6)]'
        }`,
        onClick: () => handleSync(),
      },
      [
        h(
          NTooltip,
          {
            delay: 300,
            flip: true,
            disabled: isHasConfigPermission.value && isHasConfig.value,
          },
          {
            trigger: () => {
              return t('org.enterpriseWhatSync');
            },
            default: () => t('org.checkIsOpenConfig'),
          }
        ),
        // 有同步微信配置权限则都展示
        isHasConfigPermission.value
          ? h(CrmIcon, {
              type: 'iconicon_set_up',
              size: 16,
              class: 'ml-2 text-[var(--primary-8)]',
              onClick: (e: MouseEvent) => settingWeChat(e),
            })
          : null,
      ]
    );
  }

  const moreActions: ActionsItem[] = [
    {
      label: t('org.enterpriseWhatSync'),
      key: 'sync',
      render: renderSync(),
    },
    {
      label: t('common.import'),
      key: 'import',
    },

    {
      label: t('common.export'),
      key: 'export',
    },
  ];

  // 更新配置
  function updateConfig() {
    isHasConfig.value = true;
    moreActions[0].render = renderSync();
  }

  const groupList = ref([
    {
      label: t('common.edit'),
      key: 'edit',
    },
    {
      label: t('org.resetPassWord'),
      key: 'resetPassWord',
    },
    {
      label: 'more',
      key: 'more',
      slotName: 'more',
    },
  ]);

  const moreOperationList = ref<ActionsItem[]>([
    {
      label: t('common.delete'),
      key: 'delete',
      danger: true,
    },
  ]);

  /**
   * 添加&编辑用户
   */
  const showDrawer = ref<boolean>(false);

  const currentUserId = ref<string>('');
  function addOrEditMember(isEdit: boolean, row?: MemberItem) {
    showDrawer.value = true;
    if (isEdit && row) {
      currentUserId.value = row.id;
    }
  }

  function cancelHandler() {
    showDrawer.value = false;
    currentUserId.value = '';
  }

  // 重置密码
  function handleResetPassWord(row: MemberItem) {
    openModal({
      type: 'warning',
      title: t('org.resetPassWordTip', { name: characterLimit(row.userName) }),
      content: t('org.resetPassWordContent'),
      positiveText: t('org.confirmReset'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await resetUserPassword(row.userId);
          Message.success(t('org.resetPassWordSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  // 删除员工
  function deleteMember(row: MemberItem) {
    const hasNotMoved = false;
    const tipContent = hasNotMoved ? '' : t('org.deleteMemberTipContent');
    const tipTitle = hasNotMoved
      ? t('org.deleteHasNotMovedTipContent')
      : t('common.deleteConfirmTitle', { name: characterLimit(row.userName) });
    openModal({
      type: 'error',
      title: tipTitle,
      content: tipContent,
      positiveText: hasNotMoved ? '' : t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          Message.success(t('common.deleteSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  /**
   * 用户详情
   */
  const showDetailModal = ref<boolean>(false);
  function showDetail(id: string) {
    showDetailModal.value = true;
    currentUserId.value = id;
  }

  // 切换员工状态
  function handleToggleStatus(row: MemberItem) {
    const enable = !row.enable;
    openModal({
      type: enable ? 'default' : 'error',
      title: t(enable ? 'common.confirmEnableTitle' : 'common.confirmDisabledTitle', {
        name: characterLimit(row.userName),
      }),
      content: t(enable ? 'org.enabledUserTipContent' : 'org.disabledUserTipContent'),
      positiveText: t(enable ? 'common.confirmStart' : 'common.confirmDisable'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await updateUser({
            ...row,
            name: row.userName,
            enable,
          });
          Message.success(t(enable ? 'common.opened' : 'common.disabled'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  function handleActionSelect(row: MemberItem, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        addOrEditMember(true, row);
        break;
      case 'resetPassWord':
        handleResetPassWord(row);
        break;
      case 'delete':
        deleteMember(row);
        break;
      default:
        break;
    }
  }

  async function updateUserName(row: MemberItem, newVal: string) {
    try {
      await updateUser({
        ...row,
        name: newVal,
      });
      return Promise.resolve(true);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      return Promise.resolve(false);
    }
  }

  const columns: CrmDataTableColumn[] = [
    {
      type: 'selection',
    },
    {
      title: t('org.userName'),
      key: 'userName',
      width: 200,
      sortOrder: false,
      sorter: true,
      render: (row: MemberItem) => {
        return h(
          CrmEditableText,
          {
            value: row.userName,
            onHandleEdit: (val: string) => {
              updateUserName(row, val);
              row.userName = val;
            },
          },
          {
            default: () => {
              return h(
                NButton,
                {
                  text: true,
                  type: 'primary',
                  onClick: () => showDetail(row.id),
                },
                { default: () => row.userName }
              );
            },
          }
        );
      },
    },
    {
      title: t('common.status'),
      key: 'enable',
      width: 120,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
      showInTable: true,
      filterOptions: [
        {
          label: t('common.enable'),
          value: 1,
        },
        {
          label: t('common.disable'),
          value: 0,
        },
      ],
      filter: true,
      render: (row: MemberItem) => {
        return h(NSwitch, {
          value: row.enable,
          onClick: () => {
            handleToggleStatus(row);
          },
        });
      },
    },
    {
      title: t('org.gender'),
      key: 'gender',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      filterOptions: [
        {
          label: t('org.male'),
          value: 0,
        },
        {
          label: t('org.female'),
          value: 1,
        },
      ],
      filter: 'default',
    },
    {
      title: t('org.phoneNumber'),
      key: 'phone',
      ellipsis: {
        tooltip: true,
      },
      width: 100,
    },
    {
      title: t('org.userEmail'),
      key: 'email',
      ellipsis: {
        tooltip: true,
      },
      width: 100,
      showInTable: true,
    },
    {
      title: t('org.department'),
      key: 'departmentName',
      ellipsis: {
        tooltip: true,
      },
      width: 100,
      showInTable: true,
    },
    {
      title: t('org.directSuperior'),
      key: 'supervisorId',
      ellipsis: {
        tooltip: true,
      },
      width: 100,
      showInTable: false,
    },
    {
      title: t('org.role'),
      key: 'role',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      showInTable: false,
    },
    {
      title: t('org.employeeNumber'),
      key: 'employeeId',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      showInTable: false,
    },
    {
      title: t('org.Position'),
      key: 'position',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      showInTable: false,
    },
    {
      title: t('org.employeeType'),
      key: 'employeeType',
      width: 80,
      ellipsis: {
        tooltip: true,
      },
      showInTable: false,
    },
    {
      title: t('org.workingCity'),
      key: 'workCity',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('org.userGroup'),
      key: 'userGroup',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.creator'),
      key: 'creator',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.updateTime'),
      key: 'updateTime',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      title: t('common.updateUserName'),
      key: 'updateUserName',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
    },
    {
      key: 'operation',
      width: 150,
      fixed: 'right',
      render: (row: MemberItem) =>
        h(CrmOperationButton, {
          groupList: groupList.value,
          moreList: moreOperationList.value,
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(
    getUserList,
    {
      tableKey: TableKeyEnum.SYSTEM_ORG_TABLE,
      showSetting: true,
      columns,
      scrollX: 1600,
    },
    (row: MemberItem) => {
      return {
        ...row,
        gender: row.gender ? t('org.female') : t('org.male'),
        position: row.position || '-',
        departmentName: row.departmentName || '-',
        workCity: getCityPath(row.workCity) || '-',
      };
    }
  );

  const keyword = ref('');

  function initOrgList() {
    setLoadListParams({ keyword: keyword.value, departmentId: props.activeNode });
    loadList();
  }

  function searchData(val: string) {
    keyword.value = val;
    initOrgList();
  }

  /**
   * 导入用户
   */
  const importModal = ref<boolean>(false);
  const validateLoading = ref<boolean>(false);
  function handleImportUser() {
    importModal.value = true;
  }

  const validateModal = ref<boolean>(false);
  function cancelValidate() {
    validateModal.value = false;
  }

  const fileList = ref<CrmFileItem[]>([]);
  // TODO类型 xxw
  const validateInfo = ref<any>({
    failCount: 10,
    successCount: 0,
    errorMessages: [
      {
        rowNum: 13,
        errMsg: '第 14 行出错：[名称]不能为空; ',
      },
      {
        rowNum: 14,
        errMsg: '第 15 行出错：[名称]不能为空; ',
      },
      {
        rowNum: 15,
        errMsg: '第 16 行出错：[名称]不能为空; ',
      },
      {
        rowNum: 16,
        errMsg: '第 17 行出错：[名称]不能为空; ',
      },
      {
        rowNum: 13,
        errMsg: '第 14 行出错：[名称]不能为空; ',
      },
      {
        rowNum: 14,
        errMsg: '第 15 行出错：[名称]不能为空; ',
      },
      {
        rowNum: 15,
        errMsg: '第 16 行出错：[名称]不能为空; ',
      },
      {
        rowNum: 16,
        errMsg: '第 17 行出错：[名称]不能为空; ',
      },
      {
        rowNum: 13,
        errMsg: '第 14 行出错：[名称]不能为空; ',
      },
      {
        rowNum: 14,
        errMsg: '第 15 行出错：[名称]不能为空; ',
      },
      {
        rowNum: 15,
        errMsg: '第 16 行出错：[名称]不能为空; ',
      },
      {
        rowNum: 16,
        errMsg: '第 17 行出错：[名称]不能为空; ',
      },
    ],
  });

  const { progress, start, finish } = useProgressBar();
  // 校验导入模板
  async function validateTemplate(files: CrmFileItem[]) {
    fileList.value = files;
    validateLoading.value = true;
    try {
      validateModal.value = true;
      start();
      await new Promise((resolve) => {
        setTimeout(() => {
          resolve(true);
        }, 10000);
      });
      finish();
    } catch (error) {
      validateModal.value = false;
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      validateLoading.value = false;
    }
  }

  function selectMoreActions(item: ActionsItem) {
    switch (item.key) {
      case 'import':
        handleImportUser();
        break;

      default:
        break;
    }
  }

  const validateResultModal = ref<boolean>(false);
  function checkFinished() {
    validateLoading.value = false;
    validateResultModal.value = true;
  }

  const importLoading = ref<boolean>(false);

  const showEditModal = ref<boolean>(false);

  watch(
    () => props.activeNode,
    () => {
      initOrgList();
    }
  );
</script>

<style scoped></style>
