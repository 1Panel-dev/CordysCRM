<template>
  <CrmProcessDrawer
    v-model:visible="visible"
    v-model:active-tab="activeTab"
    :tabList="tabList"
    @save="handleSave"
    @next-step="handleNextStep"
    @cancel="() => emit('cancel')"
  >
    <template #title>
      <div class="process-name-header flex max-w-full flex-1 overflow-hidden">
        <CrmEditableText
          :status="errorStatus"
          size="small"
          :value="form.basicConfig.name"
          :permission="['APPROVAL_FLOW:UPDATE']"
          click-to-edit
          :emptyTextTip="t('common.notNull', { value: t('process.process.processName') })"
          @handle-edit="handleEditName"
          @input="handleInput"
          @cancel="handleCancelEditName"
        >
          <n-tooltip trigger="hover" :delay="300" :disabled="!form.basicConfig.name">
            <template #trigger>
              <div class="process-name one-line-text">
                {{ form.basicConfig.name ?? '-' }}
              </div>
            </template>
            {{ form.basicConfig.name ?? '-' }}
          </n-tooltip>
        </CrmEditableText>
      </div>
    </template>
    <template v-if="visible">
      <ApprovalFlowDesign v-if="activeTab === 'process'" v-model:basicConfig="form.basicConfig" />
      <moreSetting v-if="activeTab === 'moreSetting'" v-model:moreConfig="form.moreConfig" />
    </template>
  </CrmProcessDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NTooltip } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmEditableText from '@/components/business/crm-editable-text/index.vue';
  import CrmProcessDrawer from '@/components/business/crm-process-drawer/index.vue';
  import ApprovalFlowDesign from './approvalFlowDesign.vue';
  import moreSetting from './moreSetting.vue';

  import { defaultBasicForm, defaultMoreConfig } from '@/config/process';

  const { t } = useI18n();

  const props = defineProps<{
    sourceId?: string;
  }>();

  const emit = defineEmits<{
    (e: 'cancel'): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const activeTab = ref('process');

  const form = ref({
    basicConfig: defaultBasicForm,
    moreConfig: defaultMoreConfig,
  });

  const editingName = ref('');

  const tabList = [
    {
      name: 'process',
      tab: t('process.processDesign'),
    },
    {
      name: 'moreSetting',
      tab: t('process.processDesign.moreSetting'),
    },
  ];

  function handleNextStep() {
    const index = tabList.findIndex((item) => item.name === activeTab.value);
    if (index === tabList.length - 1) {
      return;
    }
    activeTab.value = tabList[index + 1].name;
  }

  function handleSave() {}

  function handleEditName(value: string, done?: () => void) {
    form.value.basicConfig.name = value;
    editingName.value = value;
    done?.();
  }

  function handleCancelEditName() {
    editingName.value = form.value.basicConfig.name ?? '';
  }

  const errorStatus = computed(() => (editingName.value.trim().length ? '' : 'error'));
  function handleInput(value: string) {
    editingName.value = value;
  }
</script>

<style lang="less">
  .process-name-header {
    min-width: 0;
    > * {
      min-width: 0;
      max-width: 100%;
      flex: 1 1 auto;
    }
    .table-row-edit {
      @apply invisible;
    }
    &:hover {
      .table-row-edit {
        color: var(--primary-8);
        @apply visible;
      }
    }
    .process-name {
      overflow: hidden;
      min-width: 0;
      max-width: 100%;
      font-size: 14px;
      font-weight: 400;
      border-bottom: 2px solid var(--text-n6);
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
</style>
