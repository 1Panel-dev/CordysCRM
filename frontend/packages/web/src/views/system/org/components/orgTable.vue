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
      <n-input v-model:value="keyword" :placeholder="t('common.searchByName')" clearable class="!w-[240px]">
        <template #suffix>
          <n-icon>
            <Search />
          </n-icon>
        </template>
      </n-input>
    </div>
    <CrmTable
      v-bind="propsRes"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
    />
    <AddMember v-model:show="showDrawer" @add-success="emit('addSuccess')" />
    <SyncWeChat v-model:show="showSyncWeChatModal" />
    <MemberDetail v-model:show="showDetailModal" :rows-data="rowsData" @edit="addOrEditMember(true)" />
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
  import { NButton, NIcon, NInput, useMessage } from 'naive-ui';
  import { Search } from '@vicons/ionicons5';

  import CrmButtonGroup from '@/components/pure/crm-button-group/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { CrmDataTableColumn, CrmTableDataItem } from '@/components/pure/crm-table/type';
  import useTable from '@/components/pure/crm-table/useTable';
  import type { CrmFileItem } from '@/components/pure/crm-upload/types';
  import AddMember from './addMember.vue';
  import MemberDetail from './memberDetail.vue';
  import SyncWeChat from './syncWeChat.vue';
  import ImportModal from '@/views/system/org/components/import/importModal.vue';
  import ValidateModal from '@/views/system/org/components/import/validateModal.vue';
  import ValidateResult from '@/views/system/org/components/import/validateResult.vue';

  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';
  import useProgressBar from '@/hooks/useProgressBar';

  import { TableKeyEnum } from '@lib/shared/enums/tableEnum';
  import type { CommonList } from '@lib/shared/models/common';

  const Message = useMessage();

  const { openModal } = useModal();

  const { t } = useI18n();

  const emit = defineEmits<{
    (e: 'addSuccess'): void;
  }>();

  /**
   * 设置同步微信
   */
  const isHasSetting = ref<boolean>(false);
  const showSyncWeChatModal = ref<boolean>(false);
  function settingWeChat(e: MouseEvent) {
    e.stopPropagation();
    showSyncWeChatModal.value = true;
  }

  function renderSync() {
    return h(
      'div',
      {
        class: `flex items-center ${isHasSetting.value ? 'text-[var(--text-n1)]' : 'text-[var(--text-n6)]'}`,
      },
      [
        t('org.enterpriseWhatSync'),
        h(CrmIcon, {
          type: 'iconicon_set_up',
          size: 16,
          class: 'ml-2 text-[var(--primary-8)]',
          onClick: (e: MouseEvent) => settingWeChat(e),
        }),
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

  function initData() {
    const data: CommonList<CrmTableDataItem<any>> = {
      total: 11,
      pageSize: 10,
      current: 1,
      list: [
        {
          id: '11',
          num: 'string',
          title: 'string',
          status: 'string',
          updateTime: null,
          createTime: null,
        },
        {
          id: '22',
          num: '232324323',
          title: '222',
          status: 'aaaa',
          updateTime: null,
          createTime: null,
        },
      ],
    };
    return new Promise<CommonList<CrmTableDataItem<any>>>((resolve) => {
      setTimeout(() => {
        resolve(data);
      }, 200);
    });
  }

  /**
   * 添加&编辑用户
   */
  const showDrawer = ref<boolean>(false);
  // TODO xxw 类型
  const rowsData = ref<Record<string, any>>({
    enabled: true,
    name: '标题标题标题标题标题标题标题标题标题标题标题标题标题标题标题标题标题标题标题标题',
  });
  function addOrEditMember(isEdit: boolean, row?: any) {
    showDrawer.value = true;
    if (isEdit) {
      rowsData.value = { ...row };
    }
  }

  // 重置密码
  function handleResetPassWord(row: any) {
    openModal({
      type: 'warning',
      title: t('org.resetPassWordTip', { name: row.name }),
      content: t('org.resetPassWordContent'),
      positiveText: t('org.confirmReset'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          Message.success(t('org.resetPassWordSuccess'));
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  }

  // TODO 类型
  function handleActionSelect(row: any, actionKey: string) {
    switch (actionKey) {
      case 'edit':
        addOrEditMember(true);
        break;
      case 'resetPassWord':
        handleResetPassWord(row);
        break;
      default:
        break;
    }
  }

  // 删除员工 TODO 工作交接中 文案未定
  function deleteMember(row: any) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: row.name }),
      content: t('org.resetPassWordContent'),
      positiveText: t('common.confirm'),
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

  // TODO 未调整样式
  const columns: CrmDataTableColumn[] = [
    {
      type: 'selection',
    },
    {
      title: t('org.userName'),
      key: 'userName',
      width: 100,
      sortOrder: false,
      sorter: true,
      showInTable: true,
    },
    {
      title: t('common.status'),
      key: 'status',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      sortOrder: false,
      sorter: true,
      showInTable: true,
      filterOptions: [
        {
          label: '222',
          value: '222',
        },
        {
          label: 'string',
          value: 'string',
        },
      ],
      filter: true,
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
          value: 'male',
        },
        {
          label: t('org.female'),
          value: 'female',
        },
      ],
      filter: 'default',
      showInTable: true,
    },
    {
      title: t('org.phoneNumber'),
      key: 'phoneNumber',
      ellipsis: {
        tooltip: true,
      },
      width: 100,
    },
    {
      title: t('org.userEmail'),
      key: 'userEmail',
      ellipsis: {
        tooltip: true,
      },
      width: 100,
      showInTable: true,
    },
    {
      title: t('org.department'),
      key: 'department',
      ellipsis: {
        tooltip: true,
      },
      width: 100,
      showInTable: true,
    },
    {
      title: t('org.directSuperior'),
      key: 'directSuperior',
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
      key: 'employeeNumber',
      width: 100,
      ellipsis: {
        tooltip: true,
      },
      showInTable: false,
    },
    {
      title: t('org.Position'),
      key: 'Position',
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
      key: 'workingCity',
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
      width: 100,
      fixed: 'right',
      // TODO xxw 类型
      render: (row: any) => {
        return h(
          CrmButtonGroup,
          {
            list: groupList.value,
            onSelect: (key: string) => handleActionSelect(row, key),
          },
          {
            more: () => {
              return h(
                CrmMoreAction,
                {
                  options: [
                    {
                      label: t('common.delete'),
                      key: 'delete',
                      danger: true,
                    },
                  ],
                  onSelect: () => deleteMember(row),
                },
                {
                  default: () => {
                    return h('div', { class: 'flex items-center justify-center' }, [
                      h(CrmIcon, {
                        class: 'cursor-pointer',
                        type: 'iconicon_ellipsis',
                        size: 16,
                      }),
                    ]);
                  },
                }
              );
            },
          }
        );
      },
    },
  ];

  const { propsRes, propsEvent, loadList, setLoadListParams } = useTable(initData, {
    tableKey: TableKeyEnum.SYSTEM_ORG_TABLE,
    showSetting: true,
    columns,
    scrollX: 1600,
  });

  const keyword = ref('');

  function initOrgList() {
    setLoadListParams({ keyword: '' });
    loadList();
  }

  /**
   * 用户详情
   */
  const showDetailModal = ref<boolean>(false);
  function showDetail() {
    showDetailModal.value = true;
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

  onMounted(() => {
    initOrgList();
  });
</script>

<style scoped></style>
