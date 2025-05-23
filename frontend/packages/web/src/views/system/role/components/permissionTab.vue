<template>
  <div class="relative flex h-full flex-col">
    <n-scrollbar
      x-scrollable
      class="flex-1 px-[24px] pb-[24px]"
      :content-style="{ 'min-width': '600px', 'width': '100%' }"
    >
      <div class="group-title">{{ t('role.dataPermission') }}</div>
      <div class="mb-[24px] flex h-[32px] items-center gap-[8px]">
        <n-radio-group v-model:value="dataPermission" :disabled="isDisabled" @update-value="handleDataPermissionChange">
          <n-space>
            <n-radio v-for="item in dataPermissionOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </n-radio>
          </n-space>
        </n-radio-group>
        <n-tree-select
          v-if="dataPermission === 'DEPT_CUSTOM'"
          v-model:value="departments"
          :options="departmentOptions"
          :consistent-menu-width="false"
          max-tag-count="responsive"
          multiple
          class="w-[240px]"
          :placeholder="t('role.pleaseSelectDepartment')"
          :loading="departmentLoading"
          :disabled="isDisabled"
          :render-label="renderLabel"
          key-field="id"
          label-field="name"
          @update-value="() => (unsave = true)"
        />
      </div>
      <div class="group-title">{{ t('role.featurePermission') }}</div>
      <n-data-table
        :single-line="false"
        :columns="columns"
        :data="data"
        :paging="false"
        :pagination="false"
        :loading="loading"
        :scroll-x="500"
      />
    </n-scrollbar>
    <div class="tab-footer">
      <n-button
        v-permission="['SYSTEM_ROLE:ADD', 'SYSTEM_ROLE:UPDATE']"
        :disabled="loading || (!props.isNew && !unsave)"
        secondary
        @click="handleCancel"
      >
        {{ t(props.isNew ? 'common.cancel' : 'common.revokeChange') }}
      </n-button>
      <n-button
        v-permission="['SYSTEM_ROLE:ADD', 'SYSTEM_ROLE:UPDATE']"
        :loading="loading"
        type="primary"
        :disabled="isDisabled"
        @click="handleSave"
      >
        {{ t(props.isNew ? 'common.create' : 'common.update') }}
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { VNodeChild } from 'vue';
  import {
    DataTableColumn,
    NButton,
    NCheckbox,
    NDataTable,
    NRadio,
    NRadioGroup,
    NScrollbar,
    NSpace,
    NTooltip,
    NTreeSelect,
    TreeOption,
    useMessage,
  } from 'naive-ui';
  import { cloneDeep } from 'lodash-es';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { sleep } from '@lib/shared/method';
  import {
    DeptTreeNode,
    PermissionItem,
    PermissionTreeNode,
    RoleDetail,
    RolePermissionItem,
  } from '@lib/shared/models/system/role';

  import { createRole, getPermissions, getRoleDeptTree, getRoleDetail, updateRole } from '@/api/modules';
  import { hasAnyPermission } from '@/utils/permission';

  const props = defineProps<{
    activeRoleId: string;
    roleName?: string;
    isNew: boolean;
    isCopy: boolean;
    copyFrom?: string;
  }>();
  const emit = defineEmits<{
    (e: 'cancelCreate'): void;
    (e: 'createSuccess', id: string): void;
    (e: 'unsaveChange', value: boolean): void;
  }>();

  const { t } = useI18n();
  const message = useMessage();

  const dataPermission = ref('ALL');
  const dataPermissionOptions = [
    { label: t('role.dataPermissionAll'), value: 'ALL' },
    { label: t('role.departmentData'), value: 'DEPT_AND_CHILD' },
    { label: t('role.personalData'), value: 'SELF' },
    { label: t('role.specifiedDepartmentData'), value: 'DEPT_CUSTOM' },
  ];
  const departments = ref<string[]>([]);
  const departmentOptions = ref<DeptTreeNode[]>([]);

  const loading = ref(false);
  const unsave = ref<boolean>(false);
  const backupDetail = ref<RoleDetail>();
  const data = ref<Record<string, any>[]>([]);

  const departmentLoading = ref(false);
  async function initDept() {
    try {
      departmentLoading.value = true;
      departmentOptions.value = await getRoleDeptTree();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      departmentLoading.value = false;
    }
  }

  const isDisabled = computed(() => {
    if (props.activeRoleId === 'org_admin') {
      return true;
    }
    if (props.isNew) {
      return !hasAnyPermission(['SYSTEM_ROLE:ADD']);
    }
    return !hasAnyPermission(['SYSTEM_ROLE:UPDATE']);
  });

  function renderLabel({ option }: { option: TreeOption; checked: boolean; selected: boolean }) {
    return h(
      NTooltip,
      {
        delay: 300,
      },
      {
        default: () => h('div', {}, { default: () => option.name }),
        trigger: () =>
          h(
            'div',
            {
              class: 'one-line-text max-w-[200px]',
            },
            { default: () => option.name }
          ),
      }
    );
  }

  async function init() {
    try {
      loading.value = true;
      data.value = [];
      if (props.isCopy && props.copyFrom) {
        const res = await getRoleDetail(props.copyFrom);
        dataPermission.value = res.dataScope || 'ALL';
        departments.value = res.deptIds || [];
        res.permissions.forEach((item) => {
          item.children.forEach((child) => {
            data.value.push({
              id: child.id,
              feature: item.name,
              operator: child.name,
              rowSpan: item.children?.length || 1,
              permissions: child.permissions,
              enable: child.enable,
            });
          });
        });
      } else if (props.isNew) {
        const res = await getPermissions();
        dataPermission.value = 'ALL';
        departments.value = [];
        res.forEach((item) => {
          item.children.forEach((child) => {
            data.value.push({
              id: child.id,
              feature: item.name,
              operator: child.name,
              rowSpan: item.children?.length || 1,
              permissions: child.permissions,
              enable: false,
            });
          });
        });
      } else {
        const res = await getRoleDetail(props.activeRoleId);
        backupDetail.value = res;
        dataPermission.value = res.dataScope || 'ALL';
        departments.value = res.deptIds || [];
        res.permissions.forEach((item) => {
          item.children.forEach((child) => {
            data.value.push({
              id: child.id,
              feature: item.name,
              operator: child.name,
              rowSpan: item.children?.length || 1,
              permissions: child.permissions,
              enable: child.enable,
            });
          });
        });
        backupDetail.value.permissions = cloneDeep(data.value as PermissionTreeNode[]);
      }
      nextTick(() => {
        unsave.value = false;
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  function handleDataPermissionChange(value: string) {
    unsave.value = true;
    if (value !== 'DEPT_CUSTOM') {
      departments.value = [];
    }
  }

  const permissionAllChecked = computed({
    get: () => data.value.every((item) => item.enable),
    set: (value) => value,
  });
  const columns: DataTableColumn[] = [
    {
      title: t('role.feature'),
      key: 'feature',
      width: 90,
      className: 'feature-column',
      fixed: 'left',
      rowSpan: (rowData) => rowData.rowSpan as number,
      render: (rowData) => {
        return h(
          'div',
          {
            class: 'font-medium',
          },
          {
            default: () => rowData.feature,
          }
        );
      },
    },
    {
      title: t('role.operator'),
      key: 'operator',
      fixed: 'left',
      width: 150,
    },
    {
      title: t('role.permission'),
      key: 'permission',
      render: (rowData) => {
        const children: VNodeChild[] = [];
        ((rowData.permissions as PermissionItem[]) || []).forEach((item: PermissionItem) => {
          children.push(
            h(
              NCheckbox,
              {
                class: 'mr-[8px]',
                checked: item.enable as boolean,
                disabled: isDisabled.value,
                onUpdateChecked: (value: boolean) => {
                  unsave.value = true;
                  item.enable = value;
                  if (item.id.includes('READ') && !value) {
                    // 取消查看权限，则取消其他所有操作权限
                    ((rowData.permissions as PermissionItem[]) || []).forEach((permission: any) => {
                      permission.enable = value;
                    });
                    rowData.enable = false;
                    rowData.indeterminate = false;
                  } else if (!item.id.includes('READ') && value) {
                    // 勾选操作权限，则必须带上查看权限
                    ((rowData.permissions as PermissionItem[]) || []).forEach((permission: any) => {
                      if (permission.id.includes('READ')) {
                        permission.enable = value;
                      }
                    });
                  }
                  if (((rowData.permissions as []) || []).every((permission: any) => permission.enable)) {
                    // 判断当前功能对象所有的权限是否全部选中/取消选中，并设置当前行的选中状态
                    rowData.enable = true;
                    rowData.indeterminate = false;
                  } else {
                    rowData.enable = false;
                    rowData.indeterminate = ((rowData.permissions as []) || []).some(
                      (permission: any) => permission.enable
                    );
                  }
                },
              },
              () => item.name
            )
          );
        });
        return h('div', { class: 'flex item-center flex-wrap py-[6px] gap-[6px]' }, children);
      },
    },
    {
      key: 'enable',
      width: 40,
      fixed: 'right',
      title: () =>
        h(NCheckbox, {
          checked: permissionAllChecked.value,
          indeterminate: data.value.some((item) => item.enable || item.indeterminate) && !permissionAllChecked.value,
          disabled: isDisabled.value,
          onUpdateChecked: (value: boolean) => {
            unsave.value = true;
            permissionAllChecked.value = value;
            // 全表格全选/取消全选
            data.value.forEach((item) => {
              item.enable = value;
              ((item.permissions as []) || []).forEach((permission: any) => {
                permission.enable = value;
              });
            });
          },
        }),
      render: (rowData) =>
        h(NCheckbox, {
          checked: rowData.enable as boolean,
          indeterminate: ((rowData.permissions as []) || []).some((item: any) => item.enable) && !rowData.enable,
          disabled: isDisabled.value,
          onUpdateChecked: (value: boolean) => {
            unsave.value = true;
            rowData.enable = value;
            // 设置当前功能对象所有的权限选中状态
            if (value) {
              ((rowData.permissions as []) || []).forEach((item: any) => {
                item.enable = true;
              });
            } else {
              ((rowData.permissions as []) || []).forEach((item: any) => {
                item.enable = false;
              });
            }
          },
        }),
    },
  ];

  function handleCancel() {
    if (props.isNew) {
      emit('cancelCreate');
    } else if (unsave.value) {
      dataPermission.value = backupDetail.value?.dataScope || 'ALL';
      departments.value = backupDetail.value?.deptIds || [];
      data.value = cloneDeep(backupDetail.value?.permissions as PermissionTreeNode[]);
      unsave.value = false;
    }
  }

  async function handleSave() {
    await sleep(100);
    try {
      loading.value = true;
      const permissions: RolePermissionItem[] = [];
      data.value.forEach((e) => {
        e.permissions?.forEach((ele: RolePermissionItem) => {
          permissions.push({
            id: ele.id,
            enable: ele.enable,
          });
        });
      });
      if (props.isNew) {
        const res = await createRole({
          name: props.roleName || '',
          dataScope: dataPermission.value,
          deptIds: departments.value,
          permissions,
        });
        emit('createSuccess', res.id);
        message.success(t('common.addSuccess'));
      } else {
        await updateRole({
          id: props.activeRoleId,
          name: props.roleName || '',
          dataScope: dataPermission.value,
          deptIds: departments.value,
          permissions,
        });
        message.success(t('common.saveSuccess'));
        init();
      }
      unsave.value = false;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  watch(
    () => props.activeRoleId,
    () => {
      init();
    },
    { immediate: true }
  );

  watch(
    () => unsave.value,
    () => {
      emit('unsaveChange', unsave.value);
    }
  );

  onBeforeMount(() => {
    initDept();
  });
</script>

<style lang="less" scoped>
  .group-title {
    margin-bottom: 16px;
    font-weight: 600;
    color: var(--text-n1);
  }
  :deep(.permission-select) {
    width: 160px;
    height: 32px !important;
  }
  .department-cascader {
    width: 240px !important;
  }
  :deep(.feature-column),
  :deep(.n-data-table-th) {
    color: var(--text-n2);
    background-color: var(--text-n9);
  }
  .tab-footer {
    @apply flex w-full items-center justify-end;

    padding: 24px;
    background-color: var(--text-n10);
    box-shadow: var(--tw-ring-offset-shadow, 0 0 #00000000), var(--tw-ring-shadow, 0 0 #00000000), var(--tw-shadow);
    gap: 16px;

    --tw-shadow: 0 -1px 4px rgb(2 2 2 / 10%);
    --tw-shadow-colored: 0 -1px 4px var(--tw-shadow-color);
  }
  :deep(.n-data-table-th--fixed-left) {
    background-color: var(--text-n9) !important;
  }
  :deep(.n-data-table-td--fixed-right),
  :deep(.n-data-table-th--fixed-right) {
    padding: 0 16px;
  }
</style>
