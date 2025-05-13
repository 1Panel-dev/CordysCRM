<template>
  <CrmCard :loading="loading" hide-footer no-content-padding>
    <CrmSplitPanel class="h-full" :max="0.5" :min="0.25" :default-size="0.25">
      <template #1>
        <div class="flex h-full flex-col overflow-hidden">
          <div class="mb-[8px] flex items-center justify-between gap-[8px] px-[24px] pt-[24px]">
            <CrmSearchInput v-model:value="keyword" :placeholder="t('common.searchByName')" />
            <n-tooltip trigger="hover" :delay="300">
              <template #trigger>
                <n-button
                  v-permission="['SYSTEM_ROLE:ADD']"
                  type="primary"
                  ghost
                  class="n-btn-outline-primary px-[7px]"
                  @click="addRole"
                >
                  <template #icon>
                    <n-icon><Add /></n-icon>
                  </template>
                </n-button>
              </template>
              {{ t('role.addRole') }}
            </n-tooltip>
          </div>
          <n-scrollbar class="px-[24px] pb-[24px]">
            <CrmTree
              ref="roleTreeRef"
              v-model:selected-keys="selectedKeys"
              v-model:data="roles"
              :keyword="keyword"
              :render-prefix="renderPrefix"
              :node-more-actions="nodeMoreActions"
              title-tooltip-position="top-start"
              :filter-more-action-func="filterMoreActionFunc"
              :field-names="{ keyField: 'id', labelField: 'name', childrenField: 'children' }"
              :rename-api="updateRoleName"
              :rename-static="renameStatic"
              :selectable="roleTreeSelectable"
              @more-action-select="handleMoreActionSelect"
            />
          </n-scrollbar>
        </div>
      </template>
      <template #2>
        <div class="h-full pt-[13px]">
          <CrmTab v-model:active-tab="activeTab" :tab-list="tabList" type="line">
            <template #permission>
              <permissionTab
                v-if="activeRole"
                :active-role-id="selectedKeys[0]"
                :is-new="!!activeRole.isNew"
                :is-copy="!!activeRole.isCopy"
                :copy-from="activeRole.copyFrom"
                :role-name="activeRole.name"
                @create-success="handleCreated"
                @cancel-create="handleCancelCreate"
                @unsave-change="handleUnsaveChange"
              />
            </template>
            <template #member>
              <memberTab v-if="activeRole" :active-role-id="selectedKeys[0]" />
            </template>
          </CrmTab>
        </div>
      </template>
    </CrmSplitPanel>
  </CrmCard>
</template>

<script lang="ts" setup>
  import { NButton, NIcon, NScrollbar, NTooltip, TabPaneProps, useMessage } from 'naive-ui';
  import { Add } from '@vicons/ionicons5';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { getGenerateId } from '@lib/shared/method';
  import type { RoleItem } from '@lib/shared/models/system/role';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmSearchInput from '@/components/pure/crm-search-input/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmTree from '@/components/pure/crm-tree/index.vue';
  import { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import roleTreeNodePrefix from '@/components/business/crm-select-user-drawer/roleTreeNodePrefix.vue';
  import memberTab from './components/memberTab.vue';
  import permissionTab from './components/permissionTab.vue';

  import { deleteRole, getRoles, updateRole } from '@/api/modules';
  import useLeaveUnSaveTip from '@/hooks/useLeaveUnSaveTip';
  import useModal from '@/hooks/useModal';
  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();
  const { openModal } = useModal();
  const message = useMessage();
  const { setIsSave } = useLeaveUnSaveTip();

  const loading = ref(false);
  const keyword = ref('');
  const roles = ref<RoleItem[]>([]);
  const selectedKeys = ref<string[]>([]);
  const roleTreeRef = ref<InstanceType<typeof CrmTree> | null>(null);

  function renderPrefix(node: { option: CrmTreeNodeData; checked: boolean; selected: boolean }) {
    if (node.option.internal) {
      return h(roleTreeNodePrefix);
    }
  }

  function updateRoleName(node: CrmTreeNodeData) {
    return Promise.resolve(
      updateRole({
        id: node.id,
        name: node.name,
      })
    );
  }

  const activeRole = computed(() => roles.value.find((e) => e.id === selectedKeys.value[0]));
  const nodeMoreActions: ActionsItem[] = [
    {
      key: 'rename',
      label: t('common.rename'),
      permission: ['SYSTEM_ROLE:UPDATE'],
    },
    {
      key: 'copy',
      label: t('common.copy'),
      permission: ['SYSTEM_ROLE:ADD'],
    },
    {
      type: 'divider',
    },
    {
      key: 'delete',
      label: t('common.delete'),
      danger: true,
      permission: ['SYSTEM_ROLE:DELETE'],
    },
  ];

  function filterMoreActionFunc(items: ActionsItem[], node: CrmTreeNodeData) {
    if (node.internal || !hasAnyPermission(['SYSTEM_ROLE:UPDATE', 'SYSTEM_ROLE:ADD', 'SYSTEM_ROLE:DELETE'])) {
      return [];
    }
    if (activeRole.value?.isNew) {
      return node.id === activeRole.value?.id
        ? [
            {
              key: 'rename',
              label: t('common.rename'),
            },
          ]
        : [];
    }
    return items.filter((item) => {
      const { permission } = item;
      if (permission && !hasAnyPermission(permission)) {
        return false;
      }
      return true;
    });
  }

  const roleTreeSelectable = computed(() => !roles.value.some((role) => role.isNew || role.unsave));

  function handleMoreActionSelect(item: ActionsItem, node: CrmTreeNodeData) {
    switch (item.key) {
      case 'rename':
        break;
      case 'copy':
        if (!roleTreeSelectable.value) {
          message.warning(t('role.saveFirst'));
          return;
        }
        const id = getGenerateId();
        roles.value.push({
          ...roles.value[roles.value.length - 1],
          name: `${node.name}Copy`,
          internal: false,
          isNew: true,
          isCopy: true,
          copyFrom: node.id,
          id,
        });
        selectedKeys.value = [id];
        setIsSave(false);
        break;
      case 'delete':
        openModal({
          type: 'error',
          title: t('common.deleteConfirmTitle', { name: node.name }),
          content: t('role.deleteConfirmContent'),
          positiveText: t('common.confirmDelete'),
          negativeText: t('common.cancel'),
          onPositiveClick: async () => {
            try {
              loading.value = true;
              await deleteRole(node.id);
              roles.value = roles.value.filter((role) => role.id !== node.id);
              if (selectedKeys.value.includes(node.id)) {
                selectedKeys.value = [roles.value[0].id];
              }
              message.success(t('common.deleteSuccess'));
            } catch (error) {
              // eslint-disable-next-line no-console
              console.log(error);
            } finally {
              loading.value = false;
            }
          },
        });
        break;
      default:
        break;
    }
  }

  const renameStatic = computed(() => activeRole.value?.isNew);

  const activeTab = ref('permission');

  function addRole() {
    if (!roleTreeSelectable.value) {
      message.warning(t('role.saveFirst'));
      return;
    }
    activeTab.value = 'permission';
    const id = getGenerateId();
    roles.value.push({
      id,
      name: t('role.newRole'),
      internal: false,
      dataScope: 'ALL',
      description: '',
      isNew: true,
    });
    selectedKeys.value = [id];
    setIsSave(false);
    nextTick(() => {
      roleTreeRef.value?.toggleEdit(id);
    });
  }

  const tabList = computed<TabPaneProps[]>(() => {
    if (activeRole.value?.isNew) {
      return [
        {
          name: 'permission',
          tab: t('role.permission'),
        },
      ];
    }
    return [
      {
        name: 'permission',
        tab: t('role.permission'),
      },
      {
        name: 'member',
        tab: t('role.member'),
      },
    ];
  });

  async function init() {
    loading.value = true;
    try {
      roles.value = await getRoles();
      selectedKeys.value = roles.value[0] ? [roles.value[0].id] : [];
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    } finally {
      loading.value = false;
    }
  }

  async function handleCreated(id: string) {
    if (activeRole.value) {
      roles.value = await getRoles();
      selectedKeys.value = [id];
      activeRole.value.isNew = false;
      activeRole.value.unsave = false;
      setIsSave(true);
    }
  }

  function handleCancelCreate() {
    roles.value = roles.value.filter((role) => role.id !== selectedKeys.value[0]);
    selectedKeys.value = roles.value[0] ? [roles.value[0].id] : [];
  }

  function handleUnsaveChange(val: boolean) {
    if (activeRole.value) {
      setIsSave(!val);
      activeRole.value.unsave = val;
    }
  }

  onBeforeMount(() => {
    init();
  });
</script>

<style lang="less" scoped>
  :deep(.n-tree-node-switcher) {
    @apply hidden;
  }
  :deep(.n-tree-node-wrapper) {
    padding: 0;
  }
  :deep(.n-tabs-nav--line-type) {
    padding: 0 24px;
  }
  :deep(.n-tabs),
  :deep(.n-tab-pane) {
    @apply h-full overflow-hidden;
  }
</style>
