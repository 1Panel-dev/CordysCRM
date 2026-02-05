<template>
  <CrmCard hide-footer auto-height class="mb-[16px]">
    <div class="title">
      <div class="title-name">{{ t('workbench.quickAccess') }}</div>
      <div class="title-right flex items-center gap-[4px]" @click="handleShowCustom">
        <CrmIcon type="iconicon_set_up" :size="16" />
        {{ t('common.custom') }}
      </div>
    </div>
    <div class="flex justify-between gap-[16px]">
      <div
        v-for="item in displayQuickAccessList"
        :key="item.key"
        v-permission="item.permission"
        class="flex w-[114px] cursor-pointer flex-col items-center gap-[8px] py-[8px]"
        @click="handleActionSelect(item.key)"
      >
        <CrmSvg width="40px" height="40px" :name="item.icon" />
        {{ item.label }}
      </div>
      <div v-if="!quickAccessPermissionList.length" class="w-full p-[24px] text-center text-[var(--text-n4)]">
        {{ t('common.noPermission') }}
      </div>
    </div>
  </CrmCard>
  <CrmFormCreateDrawer
    v-model:visible="formCreateDrawerVisible"
    :form-key="activeFormKey"
    @saved="emit('refresh', activeFormKey)"
  />

  <CrmModal v-model:show="customVisible" :width="600" :positive-text="t('common.save')" @confirm="handleSave">
    <template #title>
      <div class="flex gap-[4px] overflow-hidden">
        <div class="text-[var(--text-n1)]">{{ t('common.custom') }}</div>
        <div class="flex text-[var(--text-n4)]">
          (
          <div>{{ t('workbench.quickAccess.customRules') }}</div>
          )
        </div>
      </div>
    </template>

    <div>
      <div class="font-semibold text-[var(--text-n2)]">
        {{ t('workbench.quickAccess.selectedFunctions') }}
      </div>
      <div class="mt-[16px] flex flex-wrap gap-x-[44px] gap-y-[16px]">
        <div
          v-for="item in selectedQuickAccessList"
          :key="item.key"
          class="function-item"
          @click="removeQuickAccess(item.key)"
        >
          <div class="function-svg">
            <CrmIcon
              v-show="selectedQuickAccessList.length > 1"
              type="iconicon_block_filled"
              :size="16"
              color="var(--error-red)"
              class="action-icon"
            />
            <CrmSvg width="60px" height="60px" :name="item.icon" />
          </div>
          <div class="text-[var(--text-n1)]">{{ item.label }}</div>
        </div>
      </div>

      <div class="mt-[24px] font-semibold text-[var(--text-n2)]">
        {{ t('workbench.quickAccess.functionToBeAdded') }}
      </div>
      <div class="mt-[16px] flex flex-wrap gap-x-[44px] gap-y-[16px]">
        <div
          v-for="item in availableQuickAccessList"
          :key="item.key"
          class="function-item"
          @click="addQuickAccess(item.key)"
        >
          <div class="function-svg">
            <CrmIcon
              v-show="selectedQuickAccessList.length < 5"
              type="iconicon_add_one"
              :size="16"
              color="var(--success-green)"
              class="action-icon"
            />
            <CrmSvg width="60px" height="60px" :name="item.icon" />
          </div>
          <div class="text-[var(--text-n1)]">{{ item.label }}</div>
        </div>
      </div>
    </div>
  </CrmModal>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue';
  import { useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import CrmSvg from '@/components/pure/crm-svg/index.vue';
  import CrmFormCreateDrawer from '@/components/business/crm-form-create-drawer/index.vue';

  import { type QuickAccessItem, quickAccessList } from '@/config/workbench';
  import useLocalForage from '@/hooks/useLocalForage';
  import { hasAnyPermission } from '@/utils/permission';

  const emit = defineEmits<{
    (e: 'refresh', activeFormKey: FormDesignKeyEnum): void;
  }>();

  const { setItem, getItem } = useLocalForage();
  const Message = useMessage();
  const { t } = useI18n();

  const activeFormKey = ref(FormDesignKeyEnum.CUSTOMER);
  const formCreateDrawerVisible = ref(false);

  function handleActionSelect(actionKey: FormDesignKeyEnum) {
    activeFormKey.value = actionKey;
    formCreateDrawerVisible.value = true;
  }

  const displayQuickAccessList = ref<QuickAccessItem[]>([]);
  const quickAccessPermissionList = computed<QuickAccessItem[]>(() =>
    quickAccessList.filter((item) => hasAnyPermission(item.permission))
  );

  onMounted(async () => {
    const savedKeys = (await getItem<FormDesignKeyEnum[]>('QUICK_ACCESS_STORAGE_KEY')) ?? [];
    const savePermissionList = savedKeys
      .map((key) => quickAccessPermissionList.value.find((item) => item.key === key))
      .filter(Boolean) as QuickAccessItem[];
    if (savePermissionList.length) {
      displayQuickAccessList.value = savePermissionList;
    } else {
      displayQuickAccessList.value = quickAccessPermissionList.value.slice(0, 1);
    }
  });

  const customVisible = ref(false);
  const selectedQuickAccessKeys = ref<FormDesignKeyEnum[]>([]);

  const selectedQuickAccessList = computed(
    () =>
      selectedQuickAccessKeys.value
        .map((key) => quickAccessPermissionList.value.find((item) => item.key === key))
        .filter(Boolean) as QuickAccessItem[]
  );

  const availableQuickAccessList = computed(() =>
    quickAccessPermissionList.value.filter((item) => !selectedQuickAccessKeys.value.includes(item.key))
  );

  function handleShowCustom() {
    selectedQuickAccessKeys.value = [...displayQuickAccessList.value.map((item) => item.key)];
    customVisible.value = true;
  }

  function addQuickAccess(key: FormDesignKeyEnum) {
    if (selectedQuickAccessKeys.value.length >= 5) {
      Message.warning(t('workbench.quickAccess.customRulesTip2'));
      return;
    }
    selectedQuickAccessKeys.value.push(key);
  }

  function removeQuickAccess(key: FormDesignKeyEnum) {
    if (selectedQuickAccessKeys.value.length <= 1) {
      Message.warning(t('workbench.quickAccess.customRulesTip1'));
      return;
    }
    selectedQuickAccessKeys.value = selectedQuickAccessKeys.value.filter((k) => k !== key);
  }

  async function handleSave() {
    displayQuickAccessList.value = selectedQuickAccessKeys.value
      .map((key) => quickAccessPermissionList.value.find((item) => item.key === key))
      .filter(Boolean) as QuickAccessItem[];
    await setItem('QUICK_ACCESS_STORAGE_KEY', selectedQuickAccessKeys.value);
    customVisible.value = false;
  }
</script>

<style lang="less" scoped>
  .title {
    display: flex;
    justify-content: space-between;
    margin-bottom: 16px;
    .title-name {
      font-weight: 600;
    }
    .title-right {
      color: var(--text-n4);
      cursor: pointer;
      &:hover {
        color: var(--text-n1);
      }
    }
  }
  .function-item {
    width: 75px;
    gap: 8px;
    @apply flex cursor-pointer flex-col items-center;
    .function-svg {
      position: relative;
      border: 1px solid var(--text-n8);
      border-radius: 2px;
    }
    .action-icon {
      position: absolute;
      top: -8px;
      right: -8px;
      z-index: 1;
    }
  }
</style>
