<template>
  <n-select
    v-model:value="modelValue"
    filterable
    multiple
    tag
    :placeholder="t('common.pleaseSelect')"
    :render-tag="renderTag"
    :show-arrow="false"
    :show="false"
    @click="handleShowAddAdmin"
  />
  <!-- TODO lmy MemberApiTypeEnum和params 联调 -->
  <CrmSelectUserDrawer
    ref="crmSelectUserDrawerRef"
    v-model:visible="showSelectAdminDrawer"
    :loading="false"
    :title="props.drawerTitle || t('role.addMember')"
    :api-type-key="MemberApiTypeEnum.SYSTEM_ROLE"
    :base-params="{ roleId: 'org_admin' }"
    :disabled-list="modelValue"
    :multiple="props.multiple"
    :ok-text="props.okText"
    :member-types="props.memberTypes"
    @confirm="handleAddAdminConfirm"
  />
</template>

<script setup lang="ts">
  import { NSelect, SelectOption } from 'naive-ui';

  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmSelectUserDrawer from '@/components/business/crm-select-user-drawer/index.vue';
  import type { Option } from '@/components/business/crm-select-user-drawer/type';

  import { useI18n } from '@/hooks/useI18n';

  import { MemberApiTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { SelectedUsersItem } from '@lib/shared/models/system/module';

  const { t } = useI18n();

  const props = withDefaults(
    defineProps<{
      userErrorTagIds?: string[];
      multiple?: boolean;
      drawerTitle?: string;
      okText?: string;
      memberTypes?: Option[];
    }>(),
    {
      multiple: true,
    }
  );
  const selectedList = defineModel<SelectedUsersItem[]>('selectedList', {
    required: true,
  });

  const emit = defineEmits<{
    (e: 'deleteTag'): void;
  }>();

  const modelValue = ref<string[]>([]);

  const showSelectAdminDrawer = ref(false);
  const crmSelectUserDrawerRef = ref<InstanceType<typeof CrmSelectUserDrawer>>();
  function handleShowAddAdmin() {
    showSelectAdminDrawer.value = true;
  }

  function handleAddAdminConfirm(params: SelectedUsersItem[]) {
    if (props.multiple) {
      selectedList.value = [...selectedList.value, ...params];
    } else {
      selectedList.value = params;
    }
    showSelectAdminDrawer.value = false;
  }
  const renderTag = ({ option, handleClose }: { option: SelectOption; handleClose: () => void }) => {
    return h(
      CrmTag,
      {
        type: props.userErrorTagIds?.includes(option.value as string) ? 'error' : 'default',
        theme: 'light',
        closable: true,
        onClose: () => {
          handleClose();
          selectedList.value = selectedList.value.filter((item) => item.id !== option.value);
          emit('deleteTag');
        },
      },
      {
        default: () => selectedList.value.find((item) => item.id === option.value)?.name,
      }
    );
  };

  watch(
    () => selectedList.value,
    (newVal) => {
      modelValue.value = newVal.map((item) => item.id);
    },
    {
      immediate: true,
    }
  );
</script>
