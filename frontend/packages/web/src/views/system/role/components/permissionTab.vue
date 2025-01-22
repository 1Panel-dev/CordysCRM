<template>
  <div>
    <div class="flex items-center justify-between pb-[16px] pt-[4px]">
      <n-input-group class="flex-1">
        <n-input-group-label class="text-[var(--text-n4)]">
          {{ t('role.dataPermission') }}
        </n-input-group-label>
        <n-select
          v-model:value="dataPermission"
          class="permission-select"
          :options="dataPermissionOptions"
          @update-value="handleDataPermissionChange"
        />
      </n-input-group>
      <n-cascader
        v-if="dataPermission === 'specifiedDepartment'"
        v-model:value="departments"
        :placeholder="t('role.pleaseSelectDepartment')"
        expand-trigger="click"
        :options="departmentOptions"
        :cascade="true"
        :show-path="false"
        :filterable="false"
        check-strategy="parent"
        max-tag-count="responsive"
        class="department-cascader"
        multiple
        clearable
        @update-value="initPermissionTable"
      />
    </div>
    <n-data-table
      :single-line="false"
      :columns="columns"
      :data="data"
      :paging="false"
      :pagination="false"
      :loading="loading"
    />
  </div>
</template>

<script setup lang="ts">
  import { VNodeChild } from 'vue';
  import { DataTableColumn, NCascader, NCheckbox, NDataTable, NInputGroup, NInputGroupLabel, NSelect } from 'naive-ui';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  const dataPermission = ref('all');
  const dataPermissionOptions = [
    { label: t('role.dataPermissionAll'), value: 'all' },
    { label: t('role.specifiedDepartmentData'), value: 'specifiedDepartment' },
    { label: t('role.departmentData'), value: 'department' },
    { label: t('role.personalData'), value: 'personal' },
  ];
  const departments = ref([]);
  const departmentOptions = ref([
    {
      label: '一级 1',
      value: '1',
      children: [
        {
          label: '二级 1-1',
          value: '1-1',
          children: [
            {
              label: '三级 1-1-1',
              value: '1-1-1',
            },
            {
              label: '三级 1-1-2',
              value: '1-1-2',
            },
            {
              label: '三级 1-1-3',
              value: '1-1-3',
            },
          ],
        },
      ],
    },
    {
      label: '一级 2',
      value: '2',
      children: [
        {
          label: '二级 2-1',
          value: '2-1',
          children: [
            {
              label: '三级 2-1-1',
              value: '2-1-1',
            },
          ],
        },
      ],
    },
  ]);

  const loading = ref(false);
  const data = ref<Record<string, any>[]>([]);

  async function initPermissionTable() {
    try {
      loading.value = true;
      [
        {
          id: '11',
          name: '客户管理',
          enable: false,
          license: true,
          children: [
            {
              id: '111',
              name: '公海',
              enable: false,
              license: true,
              permissions: [
                {
                  id: '1111',
                  name: '查看',
                  license: true,
                  enable: false,
                },
                {
                  id: '1112',
                  name: '新增',
                  license: true,
                  enable: false,
                },
                {
                  id: '1113',
                  name: '编辑',
                  license: true,
                  enable: false,
                },
                {
                  id: '1114',
                  name: '删除',
                  enable: false,
                },
              ],
            },
            {
              id: '112',
              name: '客户',
              enable: false,
              permissions: [
                {
                  id: '1111',
                  name: '查看',
                  enable: false,
                },
                {
                  id: '1112',
                  name: '新增',
                  enable: false,
                },
                {
                  id: '1113',
                  name: '编辑',
                  enable: false,
                },
                {
                  id: '1114',
                  name: '删除',
                  license: true,
                  enable: false,
                },
              ],
            },
          ],
        },
      ].forEach((item) => {
        item.children.forEach((child, index) => {
          data.value.push({
            id: child.id,
            feature: item.name,
            operator: child.name,
            rowSpan: index === 0 ? item.children?.length || 1 : undefined,
            permissions: child.permissions,
            enable: child.enable,
          });
        });
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  function handleDataPermissionChange(value: string) {
    if (value !== 'specifiedDepartment') {
      departments.value = [];
    }
    initPermissionTable();
  }

  const permissionAllChecked = computed({
    get: () => data.value.every((item) => item.enable),
    set: (value) => value,
  });
  const columns: DataTableColumn[] = [
    {
      title: t('role.feature'),
      key: 'feature',
      width: 150,
      className: 'feature-column',
      rowSpan: (rowData, rowIndex) => (rowIndex === 0 ? (rowData.rowSpan as number) : 1),
    },
    {
      title: t('role.operator'),
      key: 'operator',
      width: 150,
    },
    {
      title: t('role.permission'),
      key: 'permission',
      render: (rowData) => {
        const children: VNodeChild[] = [];
        ((rowData.permissions as []) || []).forEach((item: any) => {
          children.push(
            h(
              NCheckbox,
              {
                checked: item.enable as boolean,
                onUpdateChecked: (value: boolean) => {
                  item.enable = value;
                  // 判断当前功能对象所有的权限是否全部选中/取消选中，并设置当前行的选中状态
                  if (((rowData.permissions as []) || []).every((permission: any) => permission.enable)) {
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
        return h('div', { class: 'flex item-center gap-[16px]' }, children);
      },
    },
    {
      key: 'enable',
      width: 60,
      title: () =>
        h(NCheckbox, {
          checked: permissionAllChecked.value,
          indeterminate: data.value.some((item) => item.enable || item.indeterminate) && !permissionAllChecked.value,
          onUpdateChecked: (value: boolean) => {
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
          onUpdateChecked: (value: boolean) => {
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

  onBeforeMount(() => {
    initPermissionTable();
  });
</script>

<style lang="less" scoped>
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
</style>
