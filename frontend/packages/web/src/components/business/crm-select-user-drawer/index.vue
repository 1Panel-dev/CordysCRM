<template>
  <CrmDrawer
    v-model:show="visible"
    :title="props.title"
    :width="800"
    :ok-disabled="addMembers.length === 0"
    :loading="props.loading"
    :ok-text="props.okText"
    @cancel="handleCancelAdd"
    @confirm="handleAddConfirm"
  >
    <div class="flex h-full w-full flex-col gap-[16px]">
      <div v-if="addMemberTypes.length > 1" class="flex items-center gap-[16px]">
        <div class="whitespace-nowrap">{{ t('role.addMemberType') }}</div>
        <n-tabs
          v-model:value="addMemberType"
          type="segment"
          class="no-content"
          animated
          @update-value="handleTypeChange"
        >
          <n-tab-pane v-for="item of addMemberTypes" :key="item.value" :name="item.value" :tab="item.label">
          </n-tab-pane>
        </n-tabs>
      </div>
      <n-transfer
        v-model:value="addMembers"
        :options="flattenTree(options as unknown as Option[])"
        :render-source-list="renderSourceList"
        :render-target-label="renderTargetLabel"
        source-filterable
        class="addMemberTransfer"
        :class="props.multiple ? '' : 'addMemberTransfer--single'"
        @update-value="handleUpdateValue"
      />
    </div>
    <template #footer>
      <div class="flex w-full items-center gap-[12px]">
        <n-button :disabled="addMembers.length === 0" :loading="props.loading" type="primary" @click="handleAddConfirm">
          {{ t(props.okText || 'common.add') }}
        </n-button>
        <n-button secondary @click="handleCancelAdd">
          {{ t('common.cancel') }}
        </n-button>
      </div>
    </template>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import {
    NButton,
    NSkeleton,
    NTabPane,
    NTabs,
    NTooltip,
    NTransfer,
    NTree,
    TransferOption,
    TransferRenderSourceList,
  } from 'naive-ui';

  import { MemberApiTypeEnum, MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { mapTree } from '@lib/shared/method';
  import { SelectedUsersItem } from '@lib/shared/models/system/module';
  import { DeptTreeNode, RoleItem } from '@lib/shared/models/system/role';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import { Option } from '@/components/business/crm-select-user-drawer/type';
  import roleTreeNodePrefix from './roleTreeNodePrefix.vue';

  import { getDataFunc } from './utils';

  const { t } = useI18n();

  const props = withDefaults(
    defineProps<{
      title?: string;
      loading: boolean;
      apiTypeKey: MemberApiTypeEnum; // 要配置对应的key
      fetchOrgParams?: Record<string, any>; // 组织架构入参
      fetchRoleParams?: Record<string, any>; // 角色入参
      fetchMemberParams?: Record<string, any>; // 成员入参
      baseParams?: Record<string, any>; // 基础公共入参
      disabledList?: string[]; // 需要禁用掉的选项
      memberTypes?: Option[];
      multiple?: boolean;
      okText?: string;
      disabledNodeTypes?: DeptNodeTypeEnum[]; // 需要禁用掉的节点类型
    }>(),
    {
      multiple: true,
    }
  );

  const emit = defineEmits<{
    (e: 'confirm', params: any[], offspringNodes: any[]): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const addMemberType = ref<MemberSelectTypeEnum>(
    (props.memberTypes?.[0]?.value as MemberSelectTypeEnum) || MemberSelectTypeEnum.ORG
  );
  const addMemberTypes = ref(
    props.memberTypes || [
      {
        label: t('menu.settings.org'),
        value: MemberSelectTypeEnum.ORG,
      },
      {
        label: t('role.role'),
        value: MemberSelectTypeEnum.ROLE,
      },
    ]
  );

  const addMembers = ref<string[]>([]);
  const selectedNodes = ref<SelectedUsersItem[]>([]);

  function handleCancelAdd() {
    visible.value = false;
    addMembers.value = [];
    selectedNodes.value = [];
    addMemberType.value = (props.memberTypes?.[0]?.value as MemberSelectTypeEnum) || MemberSelectTypeEnum.ORG;
  }

  function flattenTree(list: undefined | Option[]): Option[] {
    const result: Option[] = [];
    function flatten(_list: Option[] = []) {
      _list.forEach((item) => {
        result.push(item);
        flatten(item.children);
      });
    }
    flatten(list);
    return result;
  }

  const roleOptions = ref<Option[]>([]);
  const userOptions = ref<RoleItem[]>([]);
  const departmentOptions = ref<DeptTreeNode[]>([]);
  const treeLoading = ref(false);

  async function loadData(value: MemberSelectTypeEnum) {
    try {
      treeLoading.value = true;
      departmentOptions.value = [];
      roleOptions.value = [];
      userOptions.value = [];
      let params = { ...props.baseParams };
      switch (value) {
        case MemberSelectTypeEnum.ORG:
        case MemberSelectTypeEnum.ONLY_ORG:
          params = { ...params, ...props.fetchOrgParams };
          departmentOptions.value = await getDataFunc(props.apiTypeKey, value, params);
          break;
        case MemberSelectTypeEnum.ROLE:
          params = { ...params, ...props.fetchRoleParams };
          roleOptions.value = await getDataFunc(props.apiTypeKey, value, params);
          break;
        default:
          params = { ...params, ...props.fetchMemberParams };
          userOptions.value = await getDataFunc(props.apiTypeKey, value, params);
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log('Error loading data:', error);
    } finally {
      treeLoading.value = false;
    }
  }

  function handleTypeChange(value: string) {
    loadData(value as MemberSelectTypeEnum);
    addMembers.value = [];
  }

  const options = computed<Record<string, any>[]>(() => {
    if ([MemberSelectTypeEnum.ORG, MemberSelectTypeEnum.ONLY_ORG].includes(addMemberType.value)) {
      return mapTree(departmentOptions.value, (item) => {
        return {
          label: item.name,
          value: item.id,
          ...item,
          children: item.children?.length ? item.children : undefined,
        };
      });
    }
    if (addMemberType.value === MemberSelectTypeEnum.ROLE) {
      return mapTree(roleOptions.value, (item) => {
        return {
          label: item.name,
          value: item.id,
          disabled: !item.enabled,
          ...item,
          children: item.children?.length ? item.children : undefined,
        };
      });
    }
    return userOptions.value.map((item) => {
      return {
        label: item.name,
        value: item.id,
        disabled: !item.enabled,
        ...item,
      };
    });
  });

  async function handleAddConfirm() {
    const _selectedNodes: any[] = [];
    mapTree(options.value, (item) => {
      if (addMembers.value.includes(item.id)) {
        _selectedNodes.push(item);
      }
      delete item.parent;
      return item;
    });
    const offspringNodes: any[] = [];
    _selectedNodes.forEach((item) => {
      mapTree(item, (node) => {
        offspringNodes.push(node);
        return node;
      });
    });
    emit('confirm', _selectedNodes, offspringNodes);
  }

  const defaultExpandedKeys = computed(() => {
    if ([MemberSelectTypeEnum.ORG, MemberSelectTypeEnum.ONLY_ORG].includes(addMemberType.value)) {
      return [options.value[0]?.value];
    }
    return [];
  });

  function renderTargetLabel({ option }: { option: TransferOption }) {
    return h(
      NTooltip,
      {
        delay: 300,
      },
      {
        default: () => {
          return h(
            'div',
            {},
            {
              default: () => option.label,
            }
          );
        },
        trigger: () => {
          return h(
            'div',
            {
              class: 'one-line-text',
            },
            {
              default: () => option.label,
            }
          );
        },
      }
    );
  }

  const renderSourceList: TransferRenderSourceList = ({ onCheck, pattern }) => {
    return h(
      NTree,
      {
        keyField: 'value',
        blockLine: true,
        blockNode: true,
        multiple: props.multiple,
        selectable: true,
        data: options.value,
        pattern,
        selectedKeys: addMembers.value,
        showIrrelevantNodes: false,
        class: 'px-[16px] overflow-hidden',
        defaultExpandedKeys: defaultExpandedKeys.value,
        overrideDefaultNodeClickBehavior: ({ option }: { option: CrmTreeNodeData }) => {
          if (!props.disabledList?.includes(option.id) && !props.disabledNodeTypes?.includes(option.nodeType)) {
            return 'toggleSelect';
          }
          return 'toggleExpand';
        },
        renderPrefix(node: { option: CrmTreeNodeData; checked: boolean; selected: boolean }) {
          if (node.option.internal) {
            return h(roleTreeNodePrefix, {
              text: t('role.sys'),
              tooltip: t('role.systemInit'),
            });
          }
          if (node.option.nodeType === DeptNodeTypeEnum.ORG && node.option.parentId === 'NONE') {
            return h(CrmIcon, {
              type: 'iconicon_enterprise',
              class: 'text-[var(--primary-8)]',
              size: 16,
            });
          }
          if ([DeptNodeTypeEnum.ORG, DeptNodeTypeEnum.ROLE].includes(node.option.nodeType)) {
            return h(CrmIcon, {
              type: 'iconicon_file1',
              class: 'text-[var(--text-n4)]',
              size: 16,
            });
          }
        },
        renderLabel({ option }) {
          return h(
            NTooltip,
            {
              trigger: 'hover',
              placement: 'top',
              showArrow: true,
              content: option.label,
              delay: 300,
            },
            {
              default: () => {
                return h(
                  'div',
                  {},
                  {
                    default: () => option.label,
                  }
                );
              },
              trigger: () => {
                return h(
                  'div',
                  { class: 'w-full flex items-center' },
                  {
                    default: () => [
                      h(
                        'div',
                        {
                          class: 'one-line-text',
                        },
                        {
                          default: () => option.label,
                        }
                      ),
                      option.commander
                        ? h(
                            'div',
                            {},
                            {
                              default: () => [
                                h(
                                  CrmTag,
                                  {
                                    type: 'primary',
                                    theme: 'lightOutLine',
                                    class: 'ml-[8px]',
                                    size: 'small',
                                    tooltipDisabled: true,
                                  },
                                  { default: () => t('common.head') }
                                ),
                              ],
                            }
                          )
                        : null,
                    ],
                  }
                );
              },
            }
          );
        },
        onUpdateSelectedKeys: (selectedKeys: Array<string | number>, nodes) => {
          onCheck(selectedKeys);
          selectedNodes.value = [];

          nodes.forEach((node) => {
            if (!node) return;

            let type: MemberSelectTypeEnum;
            if (node.nodeType === DeptNodeTypeEnum.USER || addMemberType.value === MemberSelectTypeEnum.MEMBER) {
              type = MemberSelectTypeEnum.MEMBER;
            } else if (node.nodeType === DeptNodeTypeEnum.ROLE) {
              type = MemberSelectTypeEnum.ROLE;
            } else {
              type = MemberSelectTypeEnum.ORG;
            }

            selectedNodes.value.push({
              id: node.value as string,
              name: node.label as string,
              scope: type,
            });
          });
        },
      },
      {
        empty: () => {
          return h(
            'div',
            {
              class: 'px-[12px] h-full',
            },
            [
              treeLoading.value
                ? h(NSkeleton, {
                    text: true,
                    repeat: 20,
                    height: 24,
                  })
                : t('common.noData'),
            ]
          );
        },
      }
    );
  };

  function handleUpdateValue(value: Array<string | number>) {
    selectedNodes.value = selectedNodes.value.filter((e) => value.includes(e.id));
  }

  watch(
    () => visible.value,
    (val) => {
      if (val) {
        // TODO: 别的接口
        loadData(addMemberType.value);
      }
    }
  );
</script>

<style lang="less" scoped>
  .addMemberTransfer {
    @apply flex-1;
    :deep(.n-transfer-list) {
      @apply h-full;
      .n-transfer-list-header {
        padding: 16px;
        .n-transfer-list-header__button {
          padding: 2px 8px;
          height: auto;
          .n-button__content {
            line-height: 20px;
          }
        }
        .n-transfer-list-header__extra {
          font-size: 14px;
        }
      }
      .n-transfer-filter {
        padding: 0 16px 16px;
        .n-input {
          padding: 4px 8px;
          font-size: 14px;
          line-height: 22px;
        }
      }
      .n-tree-node-wrapper {
        padding: 1px 0;
        .n-tree-node {
          padding: 5px 0;
        }
        .n-tree-node--selected {
          .n-tree-node-content__text {
            color: var(--primary-8);
          }
        }
        .n-tree-node-content__text {
          @apply overflow-hidden;
        }
      }
      .n-transfer-list-item--target {
        margin-bottom: 8px;
        .n-transfer-list-item__background {
          background-color: var(--text-n9) !important;
        }
        .n-transfer-list-item__close {
          font-size: 14px;
          opacity: 1 !important;
        }
      }
    }
    :deep(.n-transfer-list--target) {
      .n-transfer-list-body {
        padding: 0 16px;
      }
    }
  }
  .addMemberTransfer--single {
    :deep(.n-transfer-list-header__button) {
      @apply hidden;
    }
  }
</style>
