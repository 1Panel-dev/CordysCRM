<template>
  <n-tree
    ref="crmTreeRef"
    v-model:checked-keys="checkedKeys"
    v-model:selected-keys="selectedKeys"
    v-model:expanded-keys="expandedKeys"
    v-model:indeterminate-keys="indeterminateKeys"
    :virtual-scroll="props.virtualScrollProps?.virtualScroll"
    :style="{
      height: `${props.virtualScrollProps?.virtualScrollHeight}`,
    }"
    :class="['crm-tree', containerStatusClass]"
    v-bind="{ ...props, ...props.fieldNames }"
    :data="filterTreeData"
    :node-props="nodeProps"
    :allow-drop="handleAllowDrop"
    :render-label="renderLabelDom"
    :render-prefix="renderPrefixDom"
    :render-suffix="renderSuffixDom"
    :render-switcher-icon="renderSwitcherIconDom"
    @update:selected-keys="select"
    @update:expanded-keys="expanded"
    @update:checked-keys="checkNode"
    @drag-start="onDragStart"
    @drag-end="onDragEnd"
    @drop="onDrop"
  >
    <template #empty>
      <slot>
        <div
          v-show="filterTreeData.length === 0"
          class="mt-[16px] rounded-[var(--border-radius-small)] p-[8px] text-center text-[12px] leading-[16px] text-[var(--text-n4)]"
        >
          {{ props.emptyText || t('common.noData') }}
        </div>
      </slot>
    </template>
  </n-tree>
</template>

<script setup lang="ts">
  import { ref, VNodeChild } from 'vue';
  import { FormItemRule, NTree } from 'naive-ui';
  import { debounce } from 'lodash-es';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { findNodeByKey, getAllParentNodeIds, traverseTree } from '@lib/shared/method';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmMoreAction from '@/components/pure/crm-more-action/index.vue';
  import type { ActionsItem } from '@/components/pure/crm-more-action/type';

  import useContainerShadow from '@/hooks/useContainerShadow';

  import type {
    CrmInfoNode,
    CrmTreeFieldNames,
    CrmTreeNodeData,
    CrmTreeRenderData,
    CrmTreeRenderIconData,
    VirtualScrollPropsType,
  } from './type';
  import useRenameNode from './useRenameNode';
  import { DropPosition } from 'naive-ui/es/tree/src/interface';

  const { t } = useI18n();

  const props = withDefaults(
    defineProps<{
      keyword?: string; // 搜索关键字
      searchDebounce?: number; // 搜索防抖 crm 数
      draggable?: boolean; // 是否可拖拽
      showLine?: boolean; // 是否展示连接线
      fieldNames?: CrmTreeFieldNames;
      blockNode?: boolean; // 节点名称整行撑开
      blockLine?: boolean; // 节点整行撑开
      selectable?: boolean; // 是否可选中
      nodeMoreActions?: ActionsItem[]; // 节点展示在省略号按钮内的更多操作
      multiple?: boolean; // 是否允许多选
      nodeMoreActionSize?: 'medium' | 'mini' | 'small' | 'large'; // 更多操作按钮大小
      emptyText?: string; // 空数据时的文案
      checkable?: boolean; // 是否可选中
      checkStrategy?: 'all' | 'parent' | 'child'; // 选中节点时的策略
      cascade?: boolean; // 是否取消父子节点关联
      virtualScrollProps?: VirtualScrollPropsType; // 虚拟滚动
      disabledTitleTooltip?: boolean; // 是否禁用标题 tooltip
      renderLabel?: (info: CrmInfoNode) => VNodeChild; // 自定义label
      editRules?: FormItemRule | Array<FormItemRule>;
      renderPrefix?: (info: CrmTreeRenderData) => VNodeChild; // 节点前缀的渲染函数
      renderSuffix?: (info: CrmTreeRenderData) => VNodeChild; // 节点后缀的渲染函数
      renderExtra?: (info: CrmTreeRenderData) => VNodeChild; // 节点后缀前的额外内容渲染
      renderSwitcherIcon?: (info: CrmTreeRenderIconData) => VNodeChild; // 渲染icon
      nodeHighlightClass?: string; // 节点高亮背景色
      handleDrop?: boolean; // 节点高亮背景色
      cancelable?: boolean; // 选中可取消
      titleTooltipPosition?:
        | 'top-start'
        | 'top'
        | 'top-end'
        | 'right-start'
        | 'right'
        | 'right-end'
        | 'bottom-start'
        | 'bottom'
        | 'bottom-end'
        | 'left-start'
        | 'left'
        | 'left-end'; // 标题 tooltip 的位置
      allowDrop?: (info: { dropPosition: DropPosition; node: CrmTreeNodeData; phase: 'drag' | 'drop' }) => boolean; // 是否允许放置
      filterMoreActionFunc?: (items: ActionsItem[], node: CrmTreeNodeData) => any[]; // 过滤更多操作按钮
      titleClass?: string;
      renameApi?: (node: CrmTreeNodeData) => Promise<boolean>;
      renameStatic?: boolean; // 是否是静态重命名，不调用接口
    }>(),
    {
      searchDebounce: 300,
      selectable: true,
      draggable: false,
      titleTooltipPosition: 'top',
      fieldNames: () => ({
        keyField: 'key',
        labelField: 'label',
        childrenField: 'children',
        disabledField: 'disabled',
      }),
      multiple: false,
      disabledTitleTooltip: false,
      blockLine: true,
      cancelable: false,
    }
  );

  const emit = defineEmits<{
    (
      e: 'select',
      selectedKeys: Array<string | number>,
      option: Array<CrmTreeNodeData | null> | CrmTreeNodeData,
      meta: { node: CrmTreeNodeData | null; action: 'select' | 'unselect' }
    ): void;
    (
      e: 'check',
      checkedKeys: Array<string | number>,
      option: Array<CrmTreeNodeData | null> | CrmTreeNodeData,
      meta: { node: CrmTreeNodeData | null; action: 'check' | 'uncheck' }
    ): void;
    (
      e: 'expand',
      _expandKeys: Array<string | number>,
      option: Array<CrmTreeNodeData | null> | CrmTreeNodeData,
      meta: { node: CrmTreeNodeData | null; action: 'expand' | 'collapse' | 'filter' }
    ): void;
    (e: 'moreActionSelect', item: ActionsItem, option: CrmTreeNodeData): void;
    (e: 'click', info: { option: CrmTreeNodeData; checked: boolean; selected: boolean }): void;
    (
      e: 'drop',
      tree: CrmTreeNodeData[],
      dragNode: CrmTreeNodeData, // 被拖拽的节点
      dropNode: CrmTreeNodeData, // 放入的节点
      dropPosition: 'before' | 'inside' | 'after' // 位置
    ): void;
  }>();

  const data = defineModel<CrmTreeNodeData[]>('data', {
    required: true,
  });

  const defaultExpandAllKeys = defineModel<boolean>('defaultExpandAll', {
    default: false,
  });
  const selectedKeys = defineModel<Array<string | number>>('selectedKeys', {
    default: [],
  });

  const checkedKeys = defineModel<Array<string | number>>('checkedKeys', {
    default: [],
  });

  const indeterminateKeys = defineModel<Array<string | number>>('indeterminate-keys', {
    default: [],
  });

  const expandedKeys = defineModel<Array<string | number>>('expanded-keys', {
    default: [],
  });

  const filterTreeData = ref<CrmTreeNodeData[]>([]); // 初始化时全量的树数据或在非搜索情况下更新后的全量树数据

  const { toggleEdit, createEditInput, editingKey } = useRenameNode(
    props.renameApi,
    toRefs(props).renameStatic,
    props.fieldNames
  );

  /**
   * 选中节点事件
   */
  function select(
    _selectedKeys: Array<string | number>,
    _option: Array<CrmTreeNodeData | null>,
    meta: { node: CrmTreeNodeData | null; action: 'select' | 'unselect' }
  ) {
    const options = props.multiple ? _option : (meta.node as CrmTreeNodeData);
    emit('select', _selectedKeys, options, meta);
  }

  /**
   * 展开折叠
   */
  function expanded(
    _expandKeys: Array<string | number>,
    _option: Array<CrmTreeNodeData | null>,
    meta: { node: CrmTreeNodeData | null; action: 'expand' | 'collapse' | 'filter' }
  ) {
    const options = props.multiple ? _option : (meta.node as CrmTreeNodeData);
    emit('expand', _expandKeys, options, meta);
  }
  function getSiblingLabels(option: CrmTreeNodeData): string[] {
    // 获取同层节点列表（树结构 or 列表结构）
    const siblings = option.parentId
      ? findNodeByKey<CrmTreeNodeData>(data.value, option.parentId, props.fieldNames.keyField)?.[
          props.fieldNames.childrenField
        ] ?? []
      : data.value;
    // 排除自己节点在内
    return siblings.reduce((siblingsLabels: string[], node: CrmTreeNodeData) => {
      if (node[props.fieldNames.keyField] !== option[props.fieldNames.keyField]) {
        siblingsLabels.push(node[props.fieldNames.labelField]);
      }
      return siblingsLabels;
    }, []);
  }

  /**
   * 渲染label
   */
  function renderLabelDom(info: { option: CrmTreeNodeData; checked: boolean; selected: boolean }) {
    const { option, selected, checked } = info;
    const siblingLabels = getSiblingLabels(option);
    return createEditInput(
      { option, selected, checked },
      {
        name: option[props.fieldNames.labelField],
        rules: props.editRules,
      },
      siblingLabels,
      props.renderLabel,
      { titleTooltipPosition: props.titleTooltipPosition, titleClass: props.titleClass }
    );
  }

  /**
   * 渲染前缀 (一般会放置标签Tag)
   */
  function renderPrefixDom(info: { option: CrmTreeNodeData; checked: boolean; selected: boolean }) {
    // 如果提供了 renderPrefix
    if (props.renderPrefix && typeof props.renderPrefix === 'function') {
      return props.renderPrefix(info);
    }
  }

  // 更多操作
  function selectMoreAction(actionItem: ActionsItem, option: CrmTreeNodeData) {
    const nodeKey = option[props.fieldNames.keyField];
    if (actionItem.key === 'rename') {
      option.hideMoreAction = true;
      toggleEdit(nodeKey);
    } else {
      emit('moreActionSelect', actionItem, option);
    }
  }

  function renderExtraDom(info: { option: CrmTreeNodeData; checked: boolean; selected: boolean }) {
    const { option, checked, selected } = info;
    if (props.renderExtra && typeof props.renderExtra === 'function') {
      return props.renderExtra({ option, selected, checked });
    }
  }

  /** *
   * 操作后缀
   */
  const focusNodeKeys = ref(new Set());

  function renderSuffixDom(info: { option: CrmTreeNodeData; checked: boolean; selected: boolean }) {
    const { option, checked, selected } = info;
    if (option.hideMoreAction || editingKey.value === option[props.fieldNames.keyField]) {
      return null;
    }

    // 如果自定义 renderSuffix 则显示自定义内容
    if (props.renderSuffix && typeof props.renderSuffix === 'function') {
      return props.renderSuffix({ option, selected, checked });
    }

    const filteredActions =
      typeof props.filterMoreActionFunc === 'function'
        ? props.filterMoreActionFunc(props.nodeMoreActions || [], option)
        : props.nodeMoreActions || [];

    const moreActionNode =
      filteredActions.length > 0
        ? h(CrmMoreAction, {
            options: filteredActions,
            onSelect: (actionItem: ActionsItem) => selectMoreAction(actionItem, option),
            onUpdateShow: (show: boolean) => {
              focusNodeKeys.value.clear();
              if (show) {
                focusNodeKeys.value.add(option[props.fieldNames.keyField]);
              }
            },
          })
        : null;

    return h('div', { class: 'crm-tree-node-extra' }, [moreActionNode, renderExtraDom(info)]);
  }

  /**
   * 渲染展开折叠图标
   */
  function renderSwitcherIconDom(nodeSwitchProps: { option: CrmTreeNodeData; expanded: boolean; selected: boolean }) {
    const { option } = nodeSwitchProps;
    if (props.renderSwitcherIcon && typeof props.renderSwitcherIcon === 'function') {
      return props.renderSwitcherIcon(nodeSwitchProps);
    }

    return h(CrmIcon, {
      size: 16,
      type: option.children?.length ? 'iconicon_caret_right_small' : '',
      class: `text-[var(--text-n2)]`,
    });
  }

  /**
   * 返回HTML属性
   */
  function nodeProps(info: { option: CrmTreeNodeData }) {
    const { option } = info;
    const key = option[props.fieldNames.keyField];
    return {
      class: `${focusNodeKeys.value.has(key) ? 'crm-tree-focus-node' : ''}`,
    };
  }

  /**
   * checked
   */
  function checkNode(
    _checkedKeys: Array<string | number>,
    _option: Array<CrmTreeNodeData | null>,
    meta: { node: CrmTreeNodeData | null; action: 'check' | 'uncheck' }
  ) {
    const options = props.multiple ? _option : (meta.node as CrmTreeNodeData);
    emit('check', _checkedKeys, options, meta);
  }

  /**
   * 根据关键字过滤树节点
   * @param keyword 搜索关键字
   */
  function searchData(keyword: string) {
    const search = (_data: CrmTreeNodeData[]) => {
      const result: CrmTreeNodeData[] = [];
      _data.forEach((item) => {
        // 判断当前节点是否符合搜索关键字
        const titleMatches = item[props.fieldNames.labelField].toLowerCase().includes(keyword.toLowerCase());
        // 递归搜索子节点
        let filteredChildren: CrmTreeNodeData[] = [];
        if (item[props.fieldNames.childrenField]) {
          filteredChildren = search(item[props.fieldNames.childrenField]);
        }
        // 当前节点符合关键字，或有符合关键字的子节点
        if (titleMatches || filteredChildren.length > 0) {
          result.push({
            ...item,
            expanded: true,
            [props.fieldNames.childrenField]: filteredChildren,
          });
        }
      });
      return result;
    };

    return search(data.value);
  }

  // 防抖搜索
  const updateDebouncedSearch = debounce(() => {
    if (props.keyword) {
      filterTreeData.value = searchData(props.keyword);
    }
  }, props.searchDebounce);

  /** *
   * 是否允许 drop
   */
  function handleAllowDrop(info: { dropPosition: DropPosition; node: CrmTreeNodeData; phase: 'drag' | 'drop' }) {
    if (props.allowDrop) {
      return props.allowDrop(info);
    }
    return true;
  }

  /** *
   * 开始拖拽
   */
  const tempDragNode = ref<CrmTreeNodeData | null>(null);
  function onDragStart(dataInfo: { node: CrmTreeNodeData; event: DragEvent }) {
    const { node } = dataInfo;
    tempDragNode.value = node;
  }

  /** *
   * 结束拖拽
   */
  function onDragEnd() {
    tempDragNode.value = null;
  }

  function loop(
    _data: CrmTreeNodeData[],
    key: string | number | undefined,
    callback: (item: CrmTreeNodeData, index: number, arr: CrmTreeNodeData[]) => void
  ) {
    _data.some((item, index, arr) => {
      if (item[props.fieldNames.keyField] === key) {
        callback(item, index, arr);
        return true;
      }
      if (item[props.fieldNames.childrenField]) {
        return loop(item[props.fieldNames.childrenField], key, callback);
      }
      return false;
    });
  }

  /**
   * 处理拖拽结束
   */
  function onDrop(dropDataInfo: {
    node: CrmTreeNodeData;
    dragNode: CrmTreeNodeData;
    dropPosition: 'before' | 'inside' | 'after';
    event: DragEvent;
  }) {
    const { node: dropNode, dragNode, dropPosition } = dropDataInfo;

    if (props.handleDrop) {
      emit('drop', data.value, dragNode, dropNode, dropPosition);
      return;
    }
    loop(data.value, dragNode.key, (item, index, arr) => {
      arr.splice(index, 1);
    });

    if (dropPosition === 'inside') {
      // 放入节点内
      loop(data.value, dropNode.key, (item) => {
        item.children = item.children || [];
        item.children.push(dragNode);
      });
      dropNode.isLeaf = false;
      if (props.showLine) {
        delete dropNode.switcherIcon; // 放入的节点的 children 放入了被拖拽的节点，需要删除 switcherIcon 以展示默认的折叠图标
      }
    } else {
      // 放入节点前或后
      loop(data.value, dropNode.key, (item, index, arr) => {
        arr.splice(['after', 'before'].includes(dropPosition) ? index : index + 1, 0, dragNode);
      });
    }
    emit('drop', data.value, dragNode, dropNode, dropPosition);
  }

  function expandAllNodes() {
    if (!defaultExpandAllKeys.value) return;

    const expandAllKeysSet = new Set(expandedKeys.value);
    traverseTree(data.value, (node) => {
      expandAllKeysSet.add(node[props.fieldNames.keyField]);
    });
    expandedKeys.value = Array.from(expandAllKeysSet);
  }

  watch(
    () => props.keyword,
    (val) => {
      if (val?.length) {
        updateDebouncedSearch();
      } else {
        expandAllNodes();
        filterTreeData.value = data.value;
      }
    }
  );

  function toggleExpand(expandAll: boolean) {
    expandedKeys.value = expandAll ? getAllParentNodeIds(data.value) : [];
  }

  watch(
    () => data.value,
    () => {
      if (props.keyword) {
        filterTreeData.value = searchData(props.keyword);
      } else {
        filterTreeData.value = data.value;
      }
    }
  );

  onBeforeMount(() => {
    filterTreeData.value = data.value;
  });

  // TODO 未生效xxw
  const crmTreeRef = ref<InstanceType<typeof NTree>>();
  const { containerStatusClass } = useContainerShadow({
    overHeight: 34,
    containerClassName: 'crm-tree',
  });

  watch(
    () => defaultExpandAllKeys.value,
    (val) => {
      toggleExpand(val);
    }
  );

  defineExpose({
    toggleEdit,
  });
</script>

<style lang="less">
  .crm-tree {
    .crm-container--shadow-y();
    .v-vl-visible-items {
      padding-right: 10px;
    }
    &.n-tree {
      .n-tree-node-wrapper {
        .n-tree-node {
          height: 34px !important;
          .n-tree-node-content {
            .n-tree-node-content__text {
              gap: 4px;
              @apply flex w-full items-center overflow-hidden;
              .crm-tree-node-title {
                @apply flex-1 overflow-hidden;

                line-height: 22px;
                .crm-tree-node-count {
                  margin-right: 4px;
                  white-space: nowrap;
                  color: var(--text-n4);
                }
              }
            }
            // 后缀
            .n-tree-node-content__suffix {
              .crm-tree-node-extra {
                margin-left: -4px;
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
          &.n-tree-node--selected {
            .n-tree-node-content__prefix {
              color: var(--primary-8);
            }
            .n-tree-node-content {
              .n-tree-node-content__text {
                width: 60%;
                color: var(--primary-8);
                .crm-tree-node-count {
                  color: var(--primary-8);
                }
              }
            }
          }
          .n-tree-node-switcher {
            height: 100%;
            .n-tree-node-switcher__icon {
              width: 16px;
              height: 16px;
            }
          }
          .n-tree-node-checkbox {
            height: 100%;
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
    }
  }
</style>
