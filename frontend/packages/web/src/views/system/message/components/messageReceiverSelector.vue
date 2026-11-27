<template>
  <div class="message-receiver-selector">
    <NPopover
      :show="popoverVisible"
      trigger="click"
      placement="bottom-start"
      :width="300"
      :show-arrow="false"
      :class="['message-receiver-popover', `message-receiver-popover-${variant}`, '!p-[6px]']"
      @update:show="handlePopoverVisibleChange"
    >
      <template #trigger>
        <button
          class="message-receiver-trigger"
          :class="[`message-receiver-trigger-${variant}`, { active: popoverVisible }]"
          type="button"
          :disabled="disabled"
        >
          <span class="message-receiver-trigger-tags">
            <span v-if="selectedOptions.length" class="message-receiver-trigger-tag">
              {{ selectedOptions[0].label }}
            </span>
            <NPopover
              v-if="selectedOptions.length > 1"
              trigger="hover"
              placement="top-start"
              :show-arrow="false"
              :class="['message-receiver-overflow-popover', '!p-[8px]']"
            >
              <template #trigger>
                <span class="message-receiver-trigger-count">+{{ selectedOptions.length - 1 }}</span>
              </template>
              <div class="message-receiver-overflow-tags">
                <NTag v-for="option in selectedOptions" :key="option.value" :bordered="false">
                  {{ option.label }}
                </NTag>
              </div>
            </NPopover>
            <span v-if="!selectedOptions.length" class="message-receiver-trigger-placeholder">
              {{ placeholder }}
            </span>
          </span>
          <span class="message-receiver-trigger-arrow" :class="{ active: popoverVisible }"></span>
        </button>
      </template>

      <NScrollbar class="message-receiver-dropdown" style="max-height: min(56vh, 480px)">
        <section v-for="group in visibleGroups" :key="group.key" class="message-receiver-group">
          <div v-if="visibleGroups.length > 1" class="message-receiver-group-title">{{ group.label }}</div>
          <div v-for="option in group.options" :key="option.value" class="message-receiver-option">
            <div class="message-receiver-option-row" :class="{ selected: receiverKeys.includes(option.value) }">
              <NCheckbox
                :checked="receiverKeys.includes(option.value)"
                @update:checked="(checked) => toggleReceiverOption(option.value, checked)"
              >
                {{ option.label }}
              </NCheckbox>
            </div>
            <CrmUserTagSelector
              v-if="option.value === 'specifiedUsers' && receiverKeys.includes(option.value)"
              v-model:value="receiverMemberIds"
              v-model:selected-list="receiverMembers"
              class="message-receiver-option-selector"
              :api-type-key="MemberApiTypeEnum.SYSTEM_ROLE"
              :member-types="memberOnlyTypes"
              :disabled-node-types="[DeptNodeTypeEnum.ORG, DeptNodeTypeEnum.ROLE]"
              :drawer-title="specifiedUsersLabel"
              :disabled="disabled"
              @select-drawer-visible-change="handleSelectDrawerVisibleChange"
            />
            <CrmUserTagSelector
              v-if="option.value === 'specifiedRoles' && receiverKeys.includes(option.value)"
              v-model:value="receiverRoleIds"
              v-model:selected-list="receiverRoles"
              class="message-receiver-option-selector"
              :api-type-key="MemberApiTypeEnum.SYSTEM_ROLE"
              :member-types="roleOnlyTypes"
              :disabled-node-types="[DeptNodeTypeEnum.ORG, DeptNodeTypeEnum.USER]"
              :drawer-title="specifiedRolesLabel"
              :disabled="disabled"
              @select-drawer-visible-change="handleSelectDrawerVisibleChange"
            />
          </div>
        </section>
      </NScrollbar>
    </NPopover>
  </div>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { NCheckbox, NPopover, NScrollbar, NTag } from 'naive-ui';

  import { MemberApiTypeEnum, MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { DeptNodeTypeEnum } from '@lib/shared/enums/systemEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import type { SelectedUsersItem } from '@lib/shared/models/system/module';

  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  interface ReceiverOption {
    label: string;
    value: string;
  }

  interface ReceiverOptionGroup {
    key: string;
    label: string;
    options: ReceiverOption[];
  }

  const props = withDefaults(
    defineProps<{
      groups: ReceiverOptionGroup[];
      disabled?: boolean;
      placeholder: string;
      formType?: string;
      beforeOpen?: (formType: string) => Promise<void>;
      specifiedUsersLabel?: string;
      specifiedRolesLabel?: string;
      variant?: 'dialog' | 'table';
    }>(),
    {
      disabled: false,
      formType: '',
      beforeOpen: undefined,
      specifiedUsersLabel: '',
      specifiedRolesLabel: '',
      variant: 'dialog',
    }
  );

  const emit = defineEmits<{
    (event: 'selectDrawerVisibleChange', visible: boolean): void;
    (event: 'changeOnClose', value: { keys: string[]; memberIds: string[]; roleIds: string[] }): void;
  }>();

  const receiverKeys = defineModel<string[]>('receiverKeys', { required: true });
  const receiverMemberIds = defineModel<string[]>('receiverMemberIds', { default: () => [] });
  const receiverMembers = defineModel<SelectedUsersItem[]>('receiverMembers', { default: () => [] });
  const receiverRoleIds = defineModel<string[]>('receiverRoleIds', { default: () => [] });
  const receiverRoles = defineModel<SelectedUsersItem[]>('receiverRoles', { default: () => [] });
  const popoverVisible = defineModel<boolean>('show', { default: false });
  const drawerVisible = ref(false);
  const initialReceiverKeys = ref<string[]>([]);
  const initialReceiverMemberIds = ref<string[]>([]);
  const initialReceiverRoleIds = ref<string[]>([]);
  let loadedFormType = '';

  const { t } = useI18n();
  const visibleGroups = computed(() => props.groups.filter((group) => group.options.length > 0));
  const memberOnlyTypes = computed(() => [{ label: t('role.member'), value: MemberSelectTypeEnum.MEMBER }]);
  const roleOnlyTypes = computed(() => [{ label: t('role.role'), value: MemberSelectTypeEnum.ROLE }]);
  const selectedOptions = computed(() =>
    props.groups.flatMap((group) => group.options).filter((option) => receiverKeys.value.includes(option.value))
  );

  function toggleReceiverOption(value: string, checked: boolean) {
    if (checked && !receiverKeys.value.includes(value)) {
      receiverKeys.value = [...receiverKeys.value, value];
    } else if (!checked) {
      receiverKeys.value = receiverKeys.value.filter((key) => key !== value);
      if (value === 'specifiedUsers') {
        receiverMemberIds.value = [];
        receiverMembers.value = [];
      } else if (value === 'specifiedRoles') {
        receiverRoleIds.value = [];
        receiverRoles.value = [];
      }
    }
  }

  async function handlePopoverVisibleChange(visible: boolean) {
    if (!visible && drawerVisible.value) return;
    if (visible) {
      if (props.formType && props.formType !== loadedFormType) {
        await props.beforeOpen?.(props.formType);
        loadedFormType = props.formType;
      }
      initialReceiverKeys.value = [...receiverKeys.value];
      initialReceiverMemberIds.value = [...receiverMemberIds.value];
      initialReceiverRoleIds.value = [...receiverRoleIds.value];
    } else {
      const keysChanged =
        receiverKeys.value.length !== initialReceiverKeys.value.length ||
        receiverKeys.value.some((key) => !initialReceiverKeys.value.includes(key));
      const membersChanged =
        receiverMemberIds.value.length !== initialReceiverMemberIds.value.length ||
        receiverMemberIds.value.some((id) => !initialReceiverMemberIds.value.includes(id));
      const rolesChanged =
        receiverRoleIds.value.length !== initialReceiverRoleIds.value.length ||
        receiverRoleIds.value.some((id) => !initialReceiverRoleIds.value.includes(id));
      if (keysChanged || membersChanged || rolesChanged) {
        emit('changeOnClose', {
          keys: [...receiverKeys.value],
          memberIds: [...receiverMemberIds.value],
          roleIds: [...receiverRoleIds.value],
        });
      }
    }
    popoverVisible.value = visible;
  }

  function handleSelectDrawerVisibleChange(visible: boolean) {
    drawerVisible.value = visible;
    emit('selectDrawerVisibleChange', visible);
    if (visible) popoverVisible.value = true;
  }
</script>

<style lang="less" scoped>
  .message-receiver-selector {
    width: 100%;
  }
  .message-receiver-trigger {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 4px 8px;
    width: 100%;
    min-width: 0;
    height: 32px;
    border: 1px solid var(--text-n7);
    border-radius: var(--border-radius-small);
    text-align: left;
    color: var(--text-n1);
    background: var(--text-n10);
    gap: 8px;
    cursor: pointer;
    &:hover:not(:disabled),
    &:focus-visible {
      border-color: var(--primary-8);
      outline: none;
    }
    &:disabled {
      color: var(--text-n4);
      background: var(--text-n9);
      cursor: not-allowed;
    }
  }
  .message-receiver-trigger-table {
    border-color: transparent;
    background: transparent;
    &:hover:not(:disabled),
    &:focus-visible,
    &.active {
      border-color: var(--primary-8);
      background: var(--text-n10);
      outline: none;
    }
    &:disabled {
      border-color: transparent;
      background: transparent;
    }
  }
  .message-receiver-trigger-tags {
    display: flex;
    align-items: center;
    overflow: hidden;
    min-width: 0;
    gap: 6px;
  }
  .message-receiver-trigger-tag,
  .message-receiver-trigger-count {
    padding: 2px 8px;
    font-size: 12px;
    border-radius: var(--border-radius-small);
    background: var(--text-n8);
    flex: none;
    line-height: 22px;
  }
  .message-receiver-trigger-tag {
    overflow: hidden;
    max-width: 180px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .message-receiver-trigger-count {
    color: var(--text-n2);
  }
  .message-receiver-trigger-placeholder {
    color: var(--text-n4);
  }
  .message-receiver-trigger-arrow {
    margin: -4px 2px 0 0;
    width: 8px;
    height: 8px;
    border-right: 1.5px solid var(--text-n4);
    border-bottom: 1.5px solid var(--text-n4);
    transition: transform 120ms ease;
    flex: none;
    transform: rotate(45deg);
    &.active {
      margin-top: 4px;
      transform: rotate(225deg);
    }
  }
  :global(.message-receiver-popover .message-receiver-group-title) {
    padding: 4px 8px;
    font-size: 14px;
    color: var(--text-n4);
    line-height: 22px;
  }
  :global(.message-receiver-popover .message-receiver-option-row) {
    padding: 4px 8px;
    border-radius: var(--border-radius-small);
    color: var(--text-n1);
  }
  :global(.message-receiver-popover .message-receiver-option-row.selected) {
    color: var(--primary-8);
    background: var(--primary-7);
  }
  :global(.message-receiver-popover .message-receiver-option-row .n-checkbox) {
    width: 100%;
  }
  :global(.message-receiver-popover .message-receiver-option-row .n-checkbox__label) {
    flex: 1;
  }
  :global(.message-receiver-popover .message-receiver-option-selector) {
    display: block;
    margin: 0;
    height: 32px;
    .n-base-selection {
      height: 32px;
      min-height: 32px;
      box-sizing: border-box;
    }
  }
  :global(.message-receiver-overflow-popover .message-receiver-overflow-tags) {
    display: flex;
    max-width: 320px;
    flex-direction: column;
    gap: 8px;
  }
</style>
