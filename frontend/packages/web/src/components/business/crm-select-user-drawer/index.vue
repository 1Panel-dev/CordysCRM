<template>
  <CrmDrawer
    v-model:show="visible"
    :title="props.title"
    :width="800"
    :ok-disabled="addMembers.length === 0"
    :loading="props.loading"
    @cancel="handleCancelAdd"
    @confirm="handleAddConfirm"
  >
    <div class="flex h-full w-full flex-col gap-[16px]">
      <div class="flex items-center gap-[16px]">
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
        source-filterable
        class="addMemberTransfer"
      />
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue';
  import { NTabPane, NTabs, NTransfer, NTree, TransferRenderSourceList } from 'naive-ui';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import { Option } from '@/components/business/crm-select-user-drawer/type';
  import roleTreeNodePrefix from './roleTreeNodePrefix.vue';

  import { useI18n } from '@/hooks/useI18n';
  import { mapTree } from '@/utils';

  import { getDataFunc } from './utils';
  import { MemberApiTypeEnum, MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { SelectedUsersItem } from '@lib/shared/models/system/module';
  import { DeptTreeNode, RoleItem } from '@lib/shared/models/system/role';

  const { t } = useI18n();

  const props = defineProps<{
    title?: string;
    loading: boolean;
    apiTypeKey: MemberApiTypeEnum; // 要配置对应的key
    fetchOrgParams?: Record<string, any>; // 组织架构入参
    fetchRoleParams?: Record<string, any>; // 角色入参
    fetchMemberParams?: Record<string, any>; // 成员入参
    baseParams?: Record<string, any>; // 基础公共入参
    disabledList?: string[]; // 需要禁用掉的选项
  }>();

  const emit = defineEmits<{
    (e: 'confirm', params: SelectedUsersItem[]): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const addMemberType = ref<MemberSelectTypeEnum>(MemberSelectTypeEnum.ORG);
  const addMemberTypes = [
    {
      label: t('menu.settings.org'),
      value: MemberSelectTypeEnum.ORG,
    },
    {
      label: t('role.role'),
      value: MemberSelectTypeEnum.ROLE,
    },
    {
      label: t('role.member'),
      value: MemberSelectTypeEnum.MEMBER,
    },
  ];

  const addMembers = ref<string[]>([]);
  const selectedNodes = ref<SelectedUsersItem[]>([]);

  function handleCancelAdd() {
    addMembers.value = [];
    selectedNodes.value = [];
    addMemberType.value = MemberSelectTypeEnum.ORG;
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

  async function loadData(value: MemberSelectTypeEnum) {
    let params = { ...props.baseParams };
    switch (value) {
      case MemberSelectTypeEnum.ORG:
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
  }

  function handleTypeChange(value: string) {
    loadData(value as MemberSelectTypeEnum);
    addMembers.value = [];
  }

  async function handleAddConfirm() {
    emit('confirm', selectedNodes.value);
  }

  const options = computed(() => {
    if (addMemberType.value === MemberSelectTypeEnum.ORG) {
      return mapTree(departmentOptions.value, (item) => {
        return {
          label: item.name,
          value: item.id,
          disabled: !item.enabled || props.disabledList?.includes(item.id),
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
          disabled: !item.enabled || props.disabledList?.includes(item.id),
          ...item,
          children: item.children?.length ? item.children : undefined,
        };
      });
    }
    return userOptions.value.map((item) => {
      return {
        label: item.name,
        value: item.id,
        disabled: !item.enabled || props.disabledList?.includes(item.id),
        ...item,
      };
    });
  });

  const renderSourceList: TransferRenderSourceList = ({ onCheck, pattern }) => {
    return h(NTree, {
      keyField: 'value',
      blockLine: true,
      multiple: true,
      selectable: true,
      data: options.value,
      pattern,
      selectedKeys: addMembers.value,
      showIrrelevantNodes: false,
      renderPrefix(node: { option: CrmTreeNodeData; checked: boolean; selected: boolean }) {
        if (node.option.internal) {
          return h(roleTreeNodePrefix);
        }
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
    });
  };

  onMounted(() => {
    loadData(MemberSelectTypeEnum.ORG);
  });
</script>

<style scoped></style>
