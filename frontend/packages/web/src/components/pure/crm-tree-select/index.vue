<template>
  <div ref="treeSelectWrapperRef" class="crm-select-tree w-full">
    <n-tree-select
      v-bind="attrs"
      v-model:half-checked-keys="halfCheckedKeys"
      v-model:value="checkedKeys"
      :render-suffix="renderSuffix"
      :options="treeData"
      filterable
      clearable
      max-tag-count="responsive"
      :render-switcher-icon="renderSwitcherIconDom"
      :render-label="renderLabel"
      :node-props="nodeProps"
      :show="showSelectOptionsMenu"
      :disabled="props.disabled"
      checkable
      @update:show="handleUpdateShow"
      @update:value="updateCheckedKeys"
      @clear="handleClear"
      @click="handleShowPop"
    />
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton, NCheckbox, NTooltip, NTreeSelect, TreeOption, TreeSelectOption, TreeSelectProps } from 'naive-ui';
  import { cloneDeep, isEqual } from 'lodash-es';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { findNodeByKey, mapTree } from '@lib/shared/method';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';

  import { getFieldDeptTree } from '@/api/modules';

  import useTreeSelection, { CheckedNodes } from './useTreeSelection';

  const props = defineProps<{
    showContainChildModule?: boolean;
    options?: TreeSelectOption[];
    disabled?: boolean;
    type?: 'department' | 'custom';
  }>();

  const attrs = useAttrs();

  const { t } = useI18n();

  const treeData = ref<any[]>(cloneDeep(props.options || []));
  const containChildIds = defineModel<string[]>('containChildIds', {
    default: [],
  });

  async function initDepartList() {
    try {
      const tree = await getFieldDeptTree();
      containChildIds.value = [];
      treeData.value = mapTree(tree, (node) => ({
        ...node,
        containChildModule: !!containChildIds.value.includes(node.id as string),
        disabled: false,
      }));
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const selectValue = defineModel<string | number | (string | number)[] | null>('value', {
    required: true,
    default: [],
  });

  const selectedModuleProps = ref({
    modulesTree: treeData.value,
    moduleCount: {},
    value: selectValue.value,
  });

  const { selectedModulesMaps, selectParent, checkNode, halfCheckedKeys, checkedKeys, clearSelector } =
    useTreeSelection(selectedModuleProps.value);

  const showSelectOptionsMenu = ref(false);
  function handleUpdateShow(show: boolean) {
    if (!attrs.checkable) {
      showSelectOptionsMenu.value = show;
    }
  }
  const focusNodeKeys = ref(new Set());

  function handleShowPop() {
    if (attrs.checkable && !props.disabled) {
      showSelectOptionsMenu.value = !showSelectOptionsMenu.value;
    }
  }

  const keyField = computed(() => (attrs as TreeSelectProps)?.keyField ?? 'value');
  const labelField = computed(() => (attrs as TreeSelectProps)?.labelField ?? 'label');

  // 设置子节点的 containChildModule 和 disabled 属性值
  function updateChildNodesState(node: TreeSelectOption, state: boolean) {
    if (node.children) {
      node.children.forEach((child: TreeSelectOption) => {
        child.containChildModule = state;
        child.disabled = state;
        updateChildNodesState(child, state);
      });
    }
  }

  function handleCheck(_checkedKeys: Array<string | number>, checkedNodes: CheckedNodes) {
    if (props.showContainChildModule) {
      if (checkedNodes.node?.disabled) return;
      const realNode = findNodeByKey<TreeSelectOption>(treeData.value, checkedNodes.id as string, 'id');
      if (!realNode) return;
      if (checkedNodes.checked) {
        // 父级勾选，且父级“包含新增子模块”勾选，那么下面所有子级：禁用和勾选“包含新增子模块”
        if (realNode.containChildModule) {
          updateChildNodesState(realNode, true);
        }
      } else {
        // 父级取消勾选，父级和所有子级“包含新增子模块”取消勾选，所有子级取消禁用
        realNode.containChildModule = false;
        updateChildNodesState(realNode, false);
      }
    }
    checkNode(_checkedKeys, checkedNodes);
  }

  function handleClear() {
    if (props.showContainChildModule) {
      treeData.value = mapTree<TreeSelectOption>(treeData.value, (node) => ({
        ...node,
        containChildModule: false,
        disabled: false,
      }));
    }
    clearSelector();
  }

  function updateCheckedKeys(
    value: string | number | Array<string | number> | null,
    option: TreeSelectOption | null | Array<TreeSelectOption | null>,
    meta: { node: TreeOption | null; action: 'select' | 'unselect' | 'delete' | 'clear' }
  ) {
    if (attrs.checkable) {
      const { node, action } = meta;
      if (action === 'clear') {
        handleClear();
      }

      if (node) {
        const checkNodes: CheckedNodes = {
          id: node.id as string,
          checked: action === 'select',
          node,
        };
        handleCheck(value as (string | number)[], checkNodes);
      }
    } else {
      selectValue.value = value;
    }
  }

  // 当前节点“包含新增子模块”取消勾选，下面一层的子级取消禁用
  function updateNodeState(nodeId: string | number) {
    const realNode = findNodeByKey<TreeSelectOption>(treeData.value, nodeId, 'id');
    if (!realNode) return;
    realNode.containChildModule = false;
    realNode.children?.forEach((child) => {
      child.disabled = false;
    });
  }

  // 选择当前|取消当前
  function handleSelectCurrent(option: TreeSelectOption) {
    const selectValueKeys = selectValue.value as string[];
    const optionId = option[keyField.value] as string;
    if (props.showContainChildModule && selectValueKeys.includes(optionId)) {
      // 取消当前
      updateNodeState(optionId);
    }
    selectParent(option, !!selectValueKeys.includes(optionId));
  }

  function handleContainChildModule(nodeData: TreeSelectOption, containChildModule: boolean) {
    const realNode = findNodeByKey<TreeSelectOption>(treeData.value, nodeData.id as string, 'id');
    if (!realNode) return;
    realNode.containChildModule = containChildModule;
    if (containChildModule) {
      handleCheck(selectValue.value as string[], { checked: true, node: realNode, id: realNode.id });
      containChildIds.value.push(realNode.id as string);
    } else {
      realNode.children?.forEach((child) => {
        child.disabled = false;
      });
      containChildIds.value = containChildIds.value.filter((id) => id !== realNode.id);
    }
  }

  function renderSwitcherIconDom(nodeSwitchProps: { option: TreeSelectOption; expanded: boolean; selected: boolean }) {
    const { option } = nodeSwitchProps;
    return h(CrmIcon, {
      size: 16,
      type: option.children?.length ? 'iconicon_caret_right_small' : '',
      class: `text-[var(--text-n2)]`,
    });
  }

  function renderContainChildModule(option: TreeSelectOption) {
    return h(
      'div',
      {
        class: `flex items-center`,
        onClick: (e: Event) => {
          e.stopPropagation();
        },
      },
      [
        h(
          NCheckbox,
          {
            checked: option.containChildModule as boolean,
            onUpdateChecked: (val: boolean) => {
              option.containChildModule = val;
              handleContainChildModule(option, val);
            },
            onClick: (e: Event) => {
              e.stopPropagation();
            },
          },
          {
            default: () => t('crmTreeSelect.containNewChildModule'),
          }
        ),
        h('div', { class: 'flex items-center gap-[4px]' }, [
          h(
            NTooltip,
            {
              delay: 300,
              flip: true,
            },
            {
              trigger: () =>
                h(CrmIcon, {
                  type: 'iconicon_help_circle',
                  class: 'cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]',
                }),
              default: () => t('crmTreeSelect.containNewChildModuleTip'),
            }
          ),
        ]),
      ]
    );
  }

  function renderSuffix(info: { option: TreeSelectOption; checked: boolean; selected: boolean }) {
    if (!attrs.checkable && !attrs.multiple) return null;

    const selectValueKeys = selectValue.value as string[];
    const { option } = info;
    const suffixChildren: VNode[] = [];
    const optionId = option[keyField.value] as string;
    // “选择当前”/“取消当前”按钮
    if (option.children && option.children.length && !option.disabled) {
      const isSelected = selectValueKeys.includes(optionId);

      suffixChildren.push(
        h(
          NButton,
          {
            text: true,
            size: 'tiny',
            type: 'primary',
            onClick: (e: Event) => {
              e.stopPropagation();
              handleSelectCurrent(option);
            },
          },
          () => (isSelected ? t('crmTreeSelect.cancelCurrent') : t('crmTreeSelect.selectCurrent'))
        )
      );
    }

    // “包含新增子模块”更多菜单
    if (props.showContainChildModule && !option.disabled && option.id !== 'root') {
      const moreVNode = h(CrmMoreAction, {
        options: [
          {
            label: t('crmTreeSelect.containNewChildModule'),
            key: 'contain',
            render: renderContainChildModule(option),
          },
        ],
        size: 'tiny',
        onUpdateShow: (show: boolean) => {
          if (show) {
            focusNodeKeys.value.add(optionId);
          } else {
            focusNodeKeys.value.clear();
          }
        },
      });

      suffixChildren.push(moreVNode);
    }

    return h(
      'div',
      {
        class: 'flex items-center gap-[8px] crm-tree-node-extra',
        onClick: (e: Event) => {
          e.stopPropagation();
        },
      },
      suffixChildren
    );
  }

  function renderLabel(info: { option: TreeSelectOption; checked: boolean; selected: boolean }) {
    const { option } = info;
    const optionLabel = option[labelField.value] as string | number;
    return h(
      NTooltip,
      {
        delay: 300,
        flip: true,
      },
      {
        trigger: () => {
          return h('div', { class: 'flex-1 m-w-0 one-line-text max-w-[300px] crm-tree-node-title' }, optionLabel);
        },
        default: () => optionLabel,
      }
    );
  }

  // TODO 先不用这个，有自带的面板提示
  // const tooltipText = computed(() => {
  //   const values = Array.isArray(checkedKeys.value) ? checkedKeys.value : [checkedKeys.value];
  //   if (!values.length || !treeData.value) return '';
  //   return values
  //     .map((valueItem: string | number) => {
  //       const optItem = findNodeByKey<TreeSelectOption>(
  //         treeData.value as TreeSelectOption[],
  //         valueItem,
  //         keyField.value
  //       );
  //       return optItem ? optItem[labelField.value] : '';
  //     })
  //     .filter(Boolean)
  //     .join('，');
  // });

  function nodeProps(info: { option: TreeSelectOption }) {
    const { option } = info;
    const optionId = option[keyField.value] as string | number;
    return {
      class: `${focusNodeKeys.value.has(optionId) ? 'crm-tree-focus-node' : ''}`,
    };
  }

  const treeSelectWrapperRef = ref<HTMLElement>();

  function handleClickOutside(e: MouseEvent) {
    const treeSelectWrapper = treeSelectWrapperRef.value;
    const dropdownPanel = document.querySelector('.n-tree-select-menu');
    const moreActionPanel = document.querySelector('.crm-dropdown');
    const target = e.target as Node;

    const clickedOutside =
      treeSelectWrapper &&
      !treeSelectWrapper.contains(target) &&
      dropdownPanel &&
      !dropdownPanel.contains(target) &&
      (!moreActionPanel || !moreActionPanel.contains(target));

    if (clickedOutside) {
      focusNodeKeys.value.clear();
      showSelectOptionsMenu.value = false;
    }
  }

  onMounted(() => {
    if (props.type === 'department') {
      initDepartList();
    }
    document.addEventListener('mousedown', handleClickOutside);
  });

  onBeforeUnmount(() => {
    document.removeEventListener('mousedown', handleClickOutside);
  });

  const skipSelectValueWatch = ref(false);
  watch(
    () => selectValue.value,
    (newValue) => {
      if (attrs.multiple) {
        if (!skipSelectValueWatch.value && !isEqual(checkedKeys.value, newValue)) {
          clearSelector();
          ((newValue as string[]) ?? []).forEach((id) => {
            selectedModulesMaps.value[id] = {
              selectAll: true,
              selectIds: new Set(),
              excludeIds: new Set(),
              count: 0,
            };
          });
        }
        skipSelectValueWatch.value = false;
      }
    },
    {
      immediate: true,
    }
  );

  watch(
    () => checkedKeys.value,
    (newValue) => {
      if (attrs.multiple) {
        if (!isEqual(selectValue.value, newValue)) {
          // 赋值selectValue，但是不触发watch selectValue
          skipSelectValueWatch.value = true;
          selectValue.value = [...newValue];
        }
      }
    }
  );
</script>

<style lang="less">
  .n-tree-select-menu {
    .n-tree {
      .n-tree-node-wrapper {
        .n-tree-node {
          @apply flex items-center;
          .n-tree-node-content {
            min-width: 100px;
            .n-tree-node-content__text {
              @apply flex w-full items-center overflow-hidden;

              gap: 4px;
              .crm-tree-node-title {
                @apply flex-1 overflow-hidden;

                line-height: 22px;
              }
            }
            // 后缀
            .n-tree-node-content__suffix {
              @apply sticky right-0;

              background-color: var(--primary-7);
              transition: background-color 0.3s var(--n-bezier);
              .crm-tree-node-extra {
                gap: 4px;
                @apply invisible flex w-0 items-center;
                .crm-suffix-btn {
                  width: 24px;
                  height: 24px;
                  background-color: var(--primary-7);
                  @apply flex items-center justify-center;
                  &:hover {
                    background-color: var(--primary-6);
                  }
                }
                &:hover {
                  @apply visible w-auto;
                }
              }
            }
          }
          &.crm-tree-focus-node {
            .n-tree-node-content__suffix {
              .crm-tree-node-extra {
                @apply visible w-auto;
              }
            }
          }
          // hover样式
          &:hover {
            .n-tree-node-content__text {
              .crm-tree-node-count {
                @apply hidden;
              }
            }
            .n-tree-node-content__suffix {
              .crm-tree-node-extra {
                @apply visible w-auto;
              }
            }
          }
        }
      }
      .n-scrollbar-rail--horizontal {
        bottom: 0;
      }
      .n-scrollbar-rail--vertical {
        right: 0;
      }
    }
  }
</style>
