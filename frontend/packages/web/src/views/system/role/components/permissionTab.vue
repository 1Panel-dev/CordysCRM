<template>
  <div class="relative h-full px-[24px]">
    <n-scrollbar
      x-scrollable
      :content-style="{ 'min-width': '600px', 'width': '100%', 'min-height': '500px', 'margin-bottom': '96px' }"
    >
      <div class="group-title">{{ t('role.dataPermission') }}</div>
      <div class="mb-[24px] flex h-[32px] items-center gap-[8px]">
        <n-radio-group v-model:value="dataPermission" @update-value="handleDataPermissionChange">
          <n-space>
            <n-radio v-for="item in dataPermissionOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </n-radio>
          </n-space>
        </n-radio-group>
        <n-tree-select
          v-if="dataPermission === 'specifiedDepartment'"
          v-model:value="departments"
          :options="departmentOptions"
          :consistent-menu-width="false"
          max-tag-count="responsive"
          multiple
          class="w-[240px]"
          :placeholder="t('role.pleaseSelectDepartment')"
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
        :scroll-x="800"
        :max-height="500"
      />
    </n-scrollbar>
    <div class="tab-footer">
      <n-button :disabled="loading" secondary @click="handleCancel">
        {{ t('common.cancel') }}
      </n-button>
      <n-button :loading="loading" type="primary" @click="handleSave">
        {{ t(isEdit ? 'common.update' : 'common.create') }}
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
    NTreeSelect,
    useMessage,
  } from 'naive-ui';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();
  const message = useMessage();

  const dataPermission = ref('all');
  const dataPermissionOptions = [
    { label: t('role.dataPermissionAll'), value: 'all' },
    { label: t('role.departmentData'), value: 'department' },
    { label: t('role.personalData'), value: 'personal' },
    { label: t('role.specifiedDepartmentData'), value: 'specifiedDepartment' },
  ];
  const departments = ref([]);
  const departmentOptions = ref([
    {
      label: '一级 1',
      key: '1',
      children: [
        {
          label: '二级 1-1',
          key: '1-1',
          children: [
            {
              label: '三级 1-1-1',
              key: '1-1-1',
            },
            {
              label: '三级 1-1-2',
              key: '1-1-2',
            },
            {
              label: '三级 1-1-3',
              key: '1-1-3',
            },
          ],
        },
      ],
    },
    {
      label: '一级 2',
      key: '2',
      children: [
        {
          label: '二级 2-1',
          key: '2-1',
          children: [
            {
              label: '三级 2-1-1',
              key: '2-1-1',
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
        {
          id: '12',
          name: '订单管理',
          enable: false,
          children: [
            {
              id: '121',
              name: '订单',
              enable: false,
              permissions: [
                {
                  id: '1211',
                  name: '查看',
                  enable: false,
                },
                {
                  id: '1212',
                  name: '新增',
                  enable: false,
                },
                {
                  id: '1213',
                  name: '编辑',
                  enable: false,
                },
                {
                  id: '1214',
                  name: '删除',
                  enable: false,
                },
              ],
            },
          ],
        },
        {
          id: '13',
          name: '产品管理',
          enable: false,
          children: [
            {
              id: '131',
              name: '产品',
              enable: false,
              permissions: [
                {
                  id: '1311',
                  name: '查看',
                  enable: false,
                },
                {
                  id: '1312',
                  name: '新增',
                  enable: false,
                },
                {
                  id: '1313',
                  name: '编辑',
                  enable: false,
                },
                {
                  id: '1314',
                  name: '删除',
                  enable: false,
                },
              ],
            },
          ],
        },
        {
          id: '14',
          name: '统计分析',
          enable: false,
          children: [
            {
              id: '141',
              name: '销售统计',
              enable: false,
              permissions: [
                {
                  id: '1411',
                  name: '查看',
                  enable: false,
                },
              ],
            },
          ],
        },
        {
          id: '15',
          name: '系统设置',
          enable: false,
          children: [
            {
              id: '151',
              name: '角色管理',
              enable: false,
              permissions: [
                {
                  id: '1511',
                  name: '查看',
                  enable: false,
                },
                {
                  id: '1512',
                  name: '新增',
                  enable: false,
                },
                {
                  id: '1513',
                  name: '编辑',
                  enable: false,
                },
                {
                  id: '1514',
                  name: '删除',
                  enable: false,
                },
              ],
            },
            {
              id: '152',
              name: '用户管理',
              enable: false,
              permissions: [
                {
                  id: '1521',
                  name: '查看',
                  enable: false,
                },
                {
                  id: '1522',
                  name: '新增',
                  enable: false,
                },
                {
                  id: '1523',
                  name: '编辑',
                  enable: false,
                },
                {
                  id: '1524',
                  name: '删除',
                  enable: false,
                },
              ],
            },
          ],
        },
        {
          id: '16',
          name: '系统监控',
          enable: false,
          children: [
            {
              id: '161',
              name: '日志监控',
              enable: false,
              permissions: [
                {
                  id: '1611',
                  name: '查看',
                  enable: false,
                },
              ],
            },
          ],
        },
        {
          id: '17',
          name: '系统工具',
          enable: false,
          children: [
            {
              id: '171',
              name: '代码生成',
              enable: false,
              permissions: [
                {
                  id: '1711',
                  name: '查看',
                  enable: false,
                },
              ],
            },
          ],
        },
        {
          id: '18',
          name: '系统帮助',
          enable: false,
          children: [
            {
              id: '181',
              name: '文档中心',
              enable: false,
              permissions: [
                {
                  id: '1811',
                  name: '查看',
                  enable: false,
                },
              ],
            },
          ],
        },
        {
          id: '19',
          name: '系统设置',
          enable: false,
          children: [
            {
              id: '191',
              name: '系统设置',
              enable: false,
              permissions: [
                {
                  id: '1911',
                  name: '查看',
                  enable: false,
                },
              ],
            },
          ],
        },
        {
          id: '20',
          name: '系统设置',
          enable: false,
          children: [
            {
              id: '201',
              name: '系统设置',
              enable: false,
              permissions: [
                {
                  id: '2011',
                  name: '查看',
                  enable: false,
                },
              ],
            },
          ],
        },
        {
          id: '21',
          name: '系统设置',
          enable: false,
          children: [
            {
              id: '211',
              name: '系统设置',
              enable: false,
              permissions: [
                {
                  id: '2111',
                  name: '查看',
                  enable: false,
                },
              ],
            },
          ],
        },
        {
          id: '22',
          name: '系统设置',
          enable: false,
          children: [
            {
              id: '221',
              name: '系统设置',
              enable: false,
              permissions: [
                {
                  id: '2211',
                  name: '查看',
                  enable: false,
                },
              ],
            },
          ],
        },
        {
          id: '23',
          name: '系统设置',
          enable: false,
          children: [
            {
              id: '231',
              name: '系统设置',
              enable: false,
              permissions: [
                {
                  id: '2311',
                  name: '查看',
                  enable: false,
                },
              ],
            },
          ],
        },
        {
          id: '24',
          name: '系统设置',
          enable: false,
          children: [
            {
              id: '241',
              name: '系统设置',
              enable: false,
              permissions: [
                {
                  id: '2411',
                  name: '查看',
                  enable: false,
                },
              ],
            },
          ],
        },
        {
          id: '25',
          name: '系统设置',
          enable: false,
          children: [
            {
              id: '251',
              name: '系统设置',
              enable: false,
              permissions: [
                {
                  id: '2511',
                  name: '查看',
                  enable: false,
                },
              ],
            },
          ],
        },
        {
          id: '26',
          name: '系统设置',
          enable: false,
          children: [
            {
              id: '261',
              name: '系统设置',
              enable: false,
              permissions: [
                {
                  id: '2611',
                  name: '查看',
                  enable: false,
                },
              ],
            },
          ],
        },
        {
          id: '27',
          name: '系统设置',
          enable: false,
          children: [
            {
              id: '271',
              name: '系统设置',
              enable: false,
              permissions: [
                {
                  id: '2711',
                  name: '查看',
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
      fixed: 'left',
      rowSpan: (rowData, rowIndex) => (rowIndex === 0 ? (rowData.rowSpan as number) : 1),
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
      fixed: 'right',
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

  const isEdit = ref(false);

  function handleCancel() {}

  async function handleSave() {
    try {
      loading.value = true;
      message.success(t('common.saveSuccess'));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  onBeforeMount(() => {
    initPermissionTable();
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
    @apply absolute z-10 flex w-full items-center justify-end;

    bottom: 0;
    left: 0;
    padding: 24px;
    background-color: var(--text-n10);
    box-shadow: var(--tw-ring-offset-shadow, 0 0 #00000000), var(--tw-ring-shadow, 0 0 #00000000), var(--tw-shadow);
    gap: 16px;

    --tw-shadow: 0 -1px 4px rgb(2 2 2 / 10%);
    --tw-shadow-colored: 0 -1px 4px var(--tw-shadow-color);
  }
</style>
