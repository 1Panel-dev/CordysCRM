<template>
  <div class="h-full w-full p-[24px]">
    <CrmTable
      v-model:checked-row-keys="checkedRowKeys"
      v-bind="propsRes"
      :action-config="actionConfig"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
      @batch-action="handleBatchAction"
    >
      <template #actionLeft>
        <div class="flex">
          <n-tooltip trigger="hover" :disabled="!isSyncFromThirdChecked">
            <template #trigger>
              <n-button
                v-permission="['SYS_ORGANIZATION:ADD']"
                :disabled="props.isSyncFromThirdChecked"
                class="mr-[12px]"
                type="primary"
                @click="() => addOrEditMember()"
              >
                {{ t('org.addMember') }}
              </n-button>
            </template>
            {{ t('org.checkSyncUserHoverTip') }}
          </n-tooltip>
          <CrmMoreAction :options="moreActions" trigger="click" @select="selectMoreActions">
            <n-button type="default" class="outline--secondary">
              {{ t('common.more') }}
              <CrmIcon class="ml-[8px]" type="iconicon_chevron_down" :size="16" />
            </n-button>
          </CrmMoreAction>
        </div>
      </template>
      <template #actionRight>
        <CrmSearchInput
          v-model:value="keyword"
          class="!w-[240px]"
          :placeholder="t('org.searchByNameAndPhone')"
          @search="searchData"
        />
      </template>
    </CrmTable>

    <AddMember
      v-model:show="showDrawer"
      :user-id="currentUserId"
      :active-dep-id="activeNode as string"
      :is-sync-from-third-checked="props.isSyncFromThirdChecked"
      @brash="brashHandler"
      @close="cancelHandler"
    />
    <EditIntegrationModal
      v-model:show="showSyncWeChatModal"
      :title="t('system.business.WE_COM')"
      :integration="currentIntegration"
      @init-sync="initIntegration()"
    />
    <MemberDetail
      ref="memberDetailRef"
      v-model:show="showDetailModal"
      :user-id="currentUserId"
      @edit="addOrEditMember"
      @cancel="cancelHandler"
    />
    <batchEditModal
      v-model:show="showEditModal"
      :is-sync-from-third-checked="props.isSyncFromThirdChecked"
      :user-ids="checkedRowKeys"
      @load-list="handleLoadList"
    />
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
      @save="importUser"
      @close="importModal = false"
    />
    <!-- 导入结束 -->
  </div>
</template>

<script setup lang="ts">
  import { ref, RendererElement } from 'vue';
  import { DataTableRowKey, NButton, NSwitch, NTooltip, useMessage } from 'naive-ui';

  import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';
  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit, getCityPath } from '@lib/shared/method';
  import type { ConfigSynchronization } from '@lib/shared/models/system/business';
  import type { MemberItem, ValidateInfo } from '@lib/shared/models/system/org';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmNameTooltip from '@/components/pure/crm-name-tooltip/index.vue';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig, CrmDataTableColumn } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import CrmTableButton from '@/components/pure/crm-table-button/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import type { CrmFileItem } from '@/components/pure/crm-upload/types';
  import CrmEditableText from '@/components/business/crm-editable-text/index.vue';
  import ImportModal from '@/components/business/crm-import-button/components/importModal.vue';
  import ValidateModal from '@/components/business/crm-import-button/components/validateModal.vue';
  import ValidateResult from '@/components/business/crm-import-button/components/validateResult.vue';
  import CrmOperationButton from '@/components/business/crm-operation-button/index.vue';
  import AddMember from './addMember.vue';
  import batchEditModal from './batchEditModal.vue';
  import MemberDetail from './memberDetail.vue';
  import EditIntegrationModal from '@/views/system/business/components/editIntegrationModal.vue';

  import {
    batchResetUserPassword,
    batchToggleStatusUser,
    deleteUser,
    deleteUserCheck,
    getConfigSynchronization,
    getUserList,
    importUserPreCheck,
    importUsers,
    resetUserPassword,
    syncOrg,
    updateOrgUserName,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import useProgressBar from '@/hooks/useProgressBar';
  import useUserStore from '@/store/modules/user';
  import { hasAnyPermission } from '@/utils/permission';

  const userStore = useUserStore();

  const Message = useMessage();

  const { openModal } = useModal();

  const { t } = useI18n();

  const props = defineProps<{
    activeNode: string | number;
    offspringIds: string[];
    isSyncFromThirdChecked: boolean;
  }>();

  const emit = defineEmits<{
    (e: 'addSuccess'): void;
  }>();

  /**
   * 设置同步微信
   */
  const showSyncWeChatModal = ref<boolean>(false);
  const currentIntegration = ref<ConfigSynchronization>({
    type: CompanyTypeEnum.WECOM,
    corpId: '',
    agentId: '',
    appSecret: '',
    syncEnable: true,
    qrcodeEnable: true,
  });

  async function settingWeChat(e: MouseEvent) {
    e.stopPropagation();
    showSyncWeChatModal.value = true;
  }

  const tableRefreshId = ref(0);

  const actionConfig: BatchActionConfig = {
    baseAction: [
      {
        label: t('common.enable'),
        key: 'enabled',
        permission: ['SYS_ORGANIZATION:UPDATE'],
      },
      {
        label: t('common.disable'),
        key: 'disable',
        permission: ['SYS_ORGANIZATION:UPDATE'],
      },
      {
        label: t('common.edit'),
        key: 'batchEdit',
        permission: ['SYS_ORGANIZATION:UPDATE'],
      },
      {
        label: t('org.resetPassWord'),
        key: 'resetPassWord',
        permission: ['SYS_ORGANIZATION_USER:RESET_PASSWORD'],
      },
    ],
    // TODO 不上
    // moreAction: [
    //   {
    //     label: t('common.export'),
    //     key: 'export',
    //   },
    // ],
  };

  const checkedRowKeys = ref<DataTableRowKey[]>([]);

  // 批量编辑
  const showEditModal = ref<boolean>(false);
  function batchEditMember() {
    showEditModal.value = true;
  }
  // 批量启用
  function batchToggleStatusMember(enable: boolean) {
    const title = t('org.batchToggleStatusTip', {
      enable: enable ? t('common.enable') : t('common.disable'),
      number: checkedRowKeys.value.length,
    });

    const content = t(enable ? 'org.enabledUserTipContent' : 'org.disabledUserTipContent');
    const positiveText = t(enable ? 'common.confirmStart' : 'common.confirmDisable');

    openModal({
      type: enable ? 'default' : 'error',
      title,
      content,
      positiveText,
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await batchToggleStatusUser({ ids: checkedRowKeys.value, enable });
          checkedRowKeys.value = [];
          tableRefreshId.value += 1;
          Message.success(t(enable ? 'common.opened' : 'common.disabled'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  // 批量导出
  function batchExportMember() {
    try {
      // TODO
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  // 批量重置密码
  function batchResetPassWord() {
    openModal({
      type: 'warning',
      title: t('org.batchResetPsdTip', { number: checkedRowKeys.value.length }),
      content: t('org.resetPassWordContent'),
      positiveText: t('org.confirmReset'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await batchResetUserPassword({
            ids: checkedRowKeys.value,
          });
          checkedRowKeys.value = [];
          tableRefreshId.value += 1;
          Message.success(t('org.resetPassWordSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  function handleBatchAction(item: ActionsItem) {
    switch (item.key) {
      case 'batchEdit':
        batchEditMember();
        break;
      case 'enabled':
        batchToggleStatusMember(true);
        break;
      case 'disable':
        batchToggleStatusMember(false);
        break;
      case 'export':
        batchExportMember();
        break;
      case 'resetPassWord':
        batchResetPassWord();
        break;
      default:
        break;
    }
  }

  const groupList: ActionsItem[] = [
    {
      label: t('common.edit'),
      key: 'edit',
      permission: ['SYS_ORGANIZATION:UPDATE'],
    },
    {
      label: t('org.resetPassWord'),
      key: 'resetPassWord',
      permission: ['SYS_ORGANIZATION_USER:RESET_PASSWORD'],
    },
    {
      label: 'more',
      key: 'more',
      slotName: 'more',
    },
  ];

  const moreOperationList: ActionsItem[] = [
    {
      label: t('common.delete'),
      key: 'delete',
      danger: true,
      permission: ['SYS_ORGANIZATION:DELETE'],
    },
  ];

  /**
   * 添加&编辑用户
   */
  const showDrawer = ref<boolean>(false);

  const currentUserId = ref<string>('');
  function addOrEditMember(id?: string) {
    showDrawer.value = true;
    currentUserId.value = id ?? '';
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
          tableRefreshId.value += 1;
          Message.success(t('org.resetPassWordSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  // 删除员工
  async function deleteMember(row: MemberItem) {
    try {
      let hasNotMoved = false;
      hasNotMoved = await deleteUserCheck(row.id);

      const title = hasNotMoved
        ? t('common.deleteConfirmTitle', { name: characterLimit(row.userName) })
        : t('org.deleteHasNotMovedTipContent');

      const content = hasNotMoved ? t('org.deleteMemberTipContent') : '';
      const positiveText = hasNotMoved ? t('common.confirm') : '';

      openModal({
        type: 'error',
        title,
        content,
        positiveText,
        negativeText: t('common.cancel'),
        onPositiveClick: async () => {
          try {
            if (hasNotMoved) {
              await deleteUser(row.id);
              Message.success(t('common.deleteSuccess'));
              tableRefreshId.value += 1;
            }
          } catch (error) {
            // eslint-disable-next-line no-console
            console.log(error);
          }
        },
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
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
  async function handleToggleStatus(row: MemberItem) {
    const enable = !row.enable;

    const title = t(enable ? 'common.confirmEnableTitle' : 'common.confirmDisabledTitle', {
      name: characterLimit(row.userName),
    });

    const content = t(enable ? 'org.enabledUserTipContent' : 'org.disabledUserTipContent');

    openModal({
      type: enable ? 'default' : 'error',
      title,
      content,
      positiveText: t(enable ? 'common.confirmStart' : 'common.confirmDisable'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await batchToggleStatusUser({ ids: [row.id], enable });
          tableRefreshId.value += 1;
          Message.success(t(enable ? 'common.opened' : 'common.disabled'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  function handleActionSelect(row: MemberItem, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        addOrEditMember(row.id);
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
      await updateOrgUserName({
        userId: row.userId,
        name: newVal,
      });
      tableRefreshId.value += 1;
      userStore.isLogin();
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
      fixed: 'left',
    },
    {
      title: t('org.userName'),
      key: 'userName',
      width: 200,
      fixed: 'left',
      sortOrder: false,
      sorter: true,
      columnSelectorDisabled: true,
      render: (row: MemberItem) => {
        return h(
          CrmEditableText,
          {
            value: row.userName,
            permission: ['SYS_ORGANIZATION:UPDATE'],
            onHandleEdit: async (val: string, done?: () => void) => {
              const res = await updateUserName(row, val);
              if (res) {
                done?.();
              }
            },
          },
          {
            default: () => {
              return h(
                'div',
                {
                  class: 'flex items-center one-line-text',
                },
                [
                  h(
                    'div',
                    {
                      class: 'one-line-text  inline-block',
                    },
                    [
                      h(
                        CrmTableButton,
                        {
                          class: 'inline-block',
                          onClick: () => showDetail(row.id),
                        },
                        { default: () => row.userName, trigger: () => row.userName }
                      ),
                    ]
                  ),
                  row.commander
                    ? h(
                        CrmTag,
                        {
                          type: 'primary',
                          theme: 'lightOutLine',
                          class: 'ml-[8px]',
                        },
                        { default: () => t('common.head') }
                      )
                    : null,
                ]
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
      filterOptions: [
        {
          label: t('common.enable'),
          value: true,
        },
        {
          label: t('common.disable'),
          value: false,
        },
      ],
      filter: true,
      render: (row: MemberItem) => {
        return h(NSwitch, {
          value: row.enable,
          disabled: !hasAnyPermission(['SYS_ORGANIZATION:UPDATE']),
          onClick: () => {
            if (!hasAnyPermission(['SYS_ORGANIZATION:UPDATE'])) return;
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
          value: false,
        },
        {
          label: t('org.female'),
          value: true,
        },
      ],
      filter: 'default',
      render: (row: MemberItem) => {
        return row.gender ? t('org.female') : t('org.male');
      },
    },
    {
      title: t('common.phoneNumber'),
      key: 'phone',
      ellipsis: {
        tooltip: true,
      },
      width: 120,
    },
    {
      title: t('org.userEmail'),
      key: 'email',
      ellipsis: {
        tooltip: true,
      },
      width: 120,
    },
    {
      title: t('org.department'),
      key: 'departmentName',
      ellipsis: {
        tooltip: true,
      },
      width: 120,
    },
    {
      title: t('org.directSuperior'),
      key: 'supervisorId',
      ellipsis: {
        tooltip: true,
      },
      width: 120,
      showInTable: false,
      render: (row: MemberItem) => {
        return h(CrmNameTooltip, { text: row.supervisorName });
      },
    },
    {
      title: t('org.role'),
      key: 'roles',
      width: 150,
      isTag: true,
      tagGroupProps: {
        labelKey: 'name',
      },
    },
    {
      title: t('org.employeeNumber'),
      key: 'employeeId',
      width: 120,
      ellipsis: {
        tooltip: true,
      },
      showInTable: false,
    },
    {
      title: t('org.position'),
      key: 'positionName',
      width: 120,
      ellipsis: {
        tooltip: true,
      },
      showInTable: false,
    },
    {
      title: t('org.employeeType'),
      key: 'employeeType',
      width: 120,
      ellipsis: {
        tooltip: true,
      },
      showInTable: false,
    },
    {
      title: t('org.workingCity'),
      key: 'workCityName',
      width: 120,
      ellipsis: {
        tooltip: true,
      },
    },
    // {
    //   title: t('org.userGroup'),
    //   key: 'userGroup',
    //   width: 100,
    //   ellipsis: {
    //     tooltip: true,
    //   },
    // },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 120,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
    },
    {
      title: t('common.creator'),
      key: 'createUser',
      width: 120,
      render: (row: MemberItem) => {
        return h(CrmNameTooltip, { text: row.createUserName });
      },
    },
    {
      title: t('common.updateTime'),
      key: 'updateTime',
      width: 120,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
    },
    {
      title: t('common.updateUserName'),
      key: 'updateUser',
      width: 120,
      render: (row: MemberItem) => {
        return h(CrmNameTooltip, { text: row.updateUserName });
      },
    },
    {
      key: 'operation',
      width: 170,
      fixed: 'right',
      render: (row: MemberItem) =>
        h(CrmOperationButton, {
          groupList,
          moreList:
            props.isSyncFromThirdChecked || row.userId === userStore.userInfo.id ? undefined : moreOperationList,
          onSelect: (key: string) => handleActionSelect(row, key),
        }),
    },
  ];

  const getEmployeeType = (value: string) => {
    switch (value) {
      case 'formal':
        return t('org.formalUser');
      case 'internship':
        return t('org.internshipUser');
      case 'outsourcing':
        return t('org.outsourcingUser');
      default:
        return '-';
    }
  };

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(
    getUserList,
    {
      tableKey: TableKeyEnum.SYSTEM_ORG_TABLE,
      showSetting: true,
      columns,
      maxHeight: 'calc(100vh - 296px)',
      permission: ['SYS_ORGANIZATION:UPDATE', 'SYS_ORGANIZATION_USER:RESET_PASSWORD'],
    },
    (row: MemberItem) => {
      return {
        ...row,
        positionName: row.position || '-',
        departmentName: row.departmentName || '-',
        workCityName: getCityPath(row.workCity) || '-',
        phone: row.phone || '-',
        email: row.email || '-',
        employeeType: getEmployeeType(row.employeeType ?? ''),
      };
    }
  );

  async function handleSyncFromThird() {
    try {
      await syncOrg(CompanyTypeEnum.WECOM);
      Message.success(t('org.syncSuccess'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  // 同步二次确认
  function handleSyncConfirm() {
    const content = props.isSyncFromThirdChecked ? t('org.firstSyncUserTipContent') : t('org.syncUserTipContent');
    openModal({
      type: 'warning',
      title: t('org.syncUserTipTitle'),
      content,
      positiveText: t('common.confirm'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await handleSyncFromThird();
          emit('addSuccess');
          tableRefreshId.value += 1;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }
  const isHasConfigPermission = computed(() => hasAnyPermission(['SYSTEM_SETTING:UPDATE'])); // 有配置权限
  const isHasConfig = ref<boolean>(false); // 已配置
  const renderSyncResult = ref<VNode<RendererElement, { [key: string]: any }> | null>(null);

  const moreActions = computed(() => {
    return [
      ...(hasAnyPermission(['SYS_ORGANIZATION:SYNC'])
        ? [
            {
              label: t('org.enterpriseWhatSync'),
              key: 'sync',
              render: renderSyncResult.value,
              disabled: !isHasConfig.value,
            },
          ]
        : []),
      {
        label: t('common.import'),
        key: 'import',
        tooltipContent: props.isSyncFromThirdChecked ? t('org.checkSyncUserHoverTip') : '',
        disabled: props.isSyncFromThirdChecked,
        permission: ['SYS_ORGANIZATION:IMPORT'],
      },
      // TOTO  不上
      // {
      //   label: t('common.export'),
      //   key: 'export',
      // },
    ];
  });

  async function handleSync() {
    if (!isHasConfig.value) {
      return false;
    }

    if (propsRes.value.data.length) {
      handleSyncConfirm();
    } else {
      handleSyncFromThird();
    }
  }

  const keyword = ref('');

  function initOrgList() {
    setLoadListParams({ keyword: keyword.value, departmentIds: [props.activeNode, ...props.offspringIds] });
    loadList();
  }

  const memberDetailRef = ref<InstanceType<typeof MemberDetail>>();
  function brashHandler() {
    initOrgList();
    memberDetailRef.value?.getDetail();
    userStore.isLogin();
  }

  function searchData(val: string) {
    keyword.value = val;
    initOrgList();
  }

  watch(
    () => tableRefreshId.value,
    () => {
      initOrgList();
    }
  );

  function handleLoadList() {
    checkedRowKeys.value = [];
    initOrgList();
    emit('addSuccess');
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

  const validateInfo = ref<ValidateInfo>({
    failCount: 0,
    successCount: 0,
    errorMessages: [],
  });

  const { progress, start, finish } = useProgressBar();
  // 校验导入模板
  async function validateTemplate(files: CrmFileItem[]) {
    fileList.value = files;
    validateLoading.value = true;
    try {
      validateModal.value = true;
      start();

      const result = await importUserPreCheck(fileList.value[0].file as File);
      validateInfo.value = result.data;
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

  // 导入模板
  async function importUser() {
    try {
      importLoading.value = true;
      await importUsers(fileList.value[0].file as File);
      Message.success(t('common.importSuccess'));
      initOrgList();
      emit('addSuccess');
      validateResultModal.value = false;
      importModal.value = false;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      importLoading.value = false;
    }
  }

  function renderSync() {
    if (!hasAnyPermission(['SYS_ORGANIZATION:SYNC']) && !isHasConfigPermission.value) {
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

  // 初始化企业微信配置
  async function initIntegration() {
    try {
      const res = await getConfigSynchronization();
      if (res) {
        const weChatConfig = res.find((item) => item.type === CompanyTypeEnum.WECOM);
        currentIntegration.value = { ...currentIntegration.value, ...weChatConfig };
        isHasConfig.value = !!weChatConfig && weChatConfig.syncEnable && !!weChatConfig.verify;
        renderSyncResult.value = renderSync();
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  onBeforeMount(() => {
    if (isHasConfigPermission.value) {
      initIntegration();
    }
  });

  onMounted(() => {
    renderSyncResult.value = renderSync(); // 在组件挂载后执行渲染函数
  });

  watch(
    () => props.activeNode,
    () => {
      initOrgList();
    }
  );

  defineExpose({
    initOrgList,
  });
</script>

<style scoped></style>
