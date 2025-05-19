<template>
  <div class="w-full">
    <n-select
      v-model:value="modelValue"
      filterable
      multiple
      tag
      :placeholder="t('common.pleaseSelect')"
      :render-tag="renderTag"
      :show-arrow="false"
      :show="false"
      :disabled="props.disabled"
      max-tag-count="responsive"
      @click="handleShowSelectDrawer"
    />
    <CrmSelectUserDrawer
      ref="crmSelectUserDrawerRef"
      v-model:visible="showSelectDrawer"
      :loading="false"
      :title="props.drawerTitle || t('role.addMember')"
      :api-type-key="props.apiTypeKey"
      :disabled-list="modelValue"
      :multiple="props.multiple"
      :ok-text="props.okText"
      :member-types="props.memberTypes"
      :disabled-node-types="props.disabledNodeTypes"
      @confirm="handleSelectConfirm"
    />
  </div>
</template>

<script setup lang="ts">
  import { NSelect, SelectOption } from 'naive-ui';

  import { MemberApiTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { SelectedUsersItem } from '@lib/shared/models/system/module';

  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmSelectUserDrawer from '@/components/business/crm-select-user-drawer/index.vue';
  import type { Option } from '@/components/business/crm-select-user-drawer/type';

  const { t } = useI18n();

  export type UserTagSelectorProps = {
    apiTypeKey?: MemberApiTypeEnum; // 要配置对应的key
    disabledNodeTypes?: DeptNodeTypeEnum[]; // 禁用的节点类型
    userErrorTagIds?: string[];
    multiple?: boolean;
    drawerTitle?: string;
    okText?: string;
    memberTypes?: Option[];
    disabled?: boolean;
  };
  const props = withDefaults(defineProps<UserTagSelectorProps>(), {
    multiple: true,
    apiTypeKey: MemberApiTypeEnum.MODULE_ROLE,
  });
  const selectedList = defineModel<SelectedUsersItem[]>('selectedList', {
    required: false,
  });
  const modelValue = defineModel<string[]>('value', {
    default: [],
  });

  const emit = defineEmits<{
    (e: 'deleteTag'): void;
  }>();

  const showSelectDrawer = ref(false);
  const crmSelectUserDrawerRef = ref<InstanceType<typeof CrmSelectUserDrawer>>();
  function handleShowSelectDrawer() {
    if (props.disabled) return;
    showSelectDrawer.value = true;
  }

  function handleSelectConfirm(params: SelectedUsersItem[]) {
    if (props.multiple) {
      selectedList.value = [...(selectedList.value || []), ...params];
    } else {
      selectedList.value = params;
    }
    showSelectDrawer.value = false;
  }
  const renderTag = ({ option, handleClose }: { option: SelectOption; handleClose: () => void }) => {
    return h(
      CrmTag,
      {
        type: props.userErrorTagIds?.includes(option.value as string) ? 'error' : 'default',
        theme: 'light',
        closable: !props.disabled,
        onClose: () => {
          handleClose();
          selectedList.value = selectedList.value?.filter((item) => item.id !== option.value);
          emit('deleteTag');
        },
      },
      {
        default: () => selectedList.value?.find((item) => item.id === option.value)?.name,
      }
    );
  };

  watch(
    () => selectedList.value,
    (newVal) => {
      if (newVal) {
        modelValue.value = newVal?.map((item) => item.id);
      }
    },
    {
      immediate: true,
    }
  );
</script>
