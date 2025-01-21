<template>
  <CrmCard no-content-padding>
    <CrmSplitPanel class="h-[500px]" :max="0.5" :min="0.25" :default-size="0.25"></CrmSplitPanel>
  </CrmCard>
</template>

<script lang="ts" setup>
  import { NTree } from 'naive-ui';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmSplitPanel from '@/components/pure/crm-split-panel/index.vue';

  import type { TransferRenderSourceList, TreeOption } from 'naive-ui';
  import { repeat } from 'seemly';

  function createLabel(level: number): string {
    if (level === 4) return '道生一';
    if (level === 3) return '一生二';
    if (level === 2) return '二生三';
    if (level === 1) return '三生万物';
    return '';
  }

  interface Option {
    label: string;
    value: string;
    children?: Option[];
  }

  function createData(level = 4, baseKey = ''): Option[] | undefined {
    if (!level) return undefined;
    return repeat(6 - level, undefined).map((_, index) => {
      const value = `${baseKey}${level}${index}`;
      return {
        label: createLabel(level),
        value,
        children: createData(level - 1, value),
      };
    });
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
  const treeData = createData();
  const value = ref<Array<string | number>>([]);
  const renderSourceList: TransferRenderSourceList = ({ onCheck, pattern }) => {
    return h(
      'div',
      {
        class: 'flex',
      },
      [
        h(NTree, {
          style: 'margin: 0 4px;',
          keyField: 'value',
          checkable: true,
          selectable: false,
          blockLine: true,
          checkOnClick: true,
          cascade: true,
          data: treeData as unknown as TreeOption[],
          pattern,
          checkedKeys: value.value,
          checkStrategy: 'parent',
          onUpdateCheckedKeys: (checkedKeys: Array<string | number>) => {
            onCheck(checkedKeys);
          },
        }),
        h(NTree, {
          style: 'margin: 0 4px;',
          keyField: 'value',
          checkable: true,
          selectable: false,
          blockLine: true,
          checkOnClick: true,
          cascade: true,
          data: treeData as unknown as TreeOption[],
          pattern,
          checkedKeys: value.value,
          checkStrategy: 'parent',
          onUpdateCheckedKeys: (checkedKeys: Array<string | number>) => {
            onCheck(checkedKeys);
          },
        }),
      ]
    );
  };

  const options = flattenTree(createData());
</script>
