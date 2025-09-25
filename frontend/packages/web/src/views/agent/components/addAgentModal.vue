<template>
  <CrmModal
    v-model:show="show"
    :title="title"
    :ok-loading="loading"
    :positive-text="props.agentId ? t('common.update') : t('common.add')"
    size="large"
    :show-continue="!props.agentId"
    :footer="!props.isDetail"
    @confirm="handleConfirm"
    @cancel="handleCancel"
    @continue="handleConfirm(true)"
  >
    <n-spin :show="detailLoading" class="h-full w-full">
      <n-form
        ref="formRef"
        :model="form"
        label-placement="left"
        :disabled="props.isDetail"
        :rules="{
          name: [
            {
              required: true,
              message: t('common.notNull', { value: t('agent.agentName') }),
              trigger: 'input',
            },
          ],
          agentModuleId: [
            {
              required: true,
              message: t('common.notNull', { value: t('agent.folder') }),
              trigger: 'blur',
            },
          ],
          scopeIds: [
            {
              required: true,
              message: t('common.notNull', { value: t('agent.members') }),
              trigger: 'blur',
              type: 'array',
            },
          ],
          script: [
            {
              required: true,
              message: t('common.notNull', { value: t('agent.script') }),
              trigger: 'blur',
            },
          ],
        }"
        label-width="100"
      >
        <n-form-item :label="t('agent.agentName')" path="name">
          <n-input v-model:value="form.name" :maxlength="255" />
        </n-form-item>
        <n-form-item :label="t('agent.folder')" path="agentModuleId">
          <n-tree-select
            v-model:value="form.agentModuleId"
            :options="props.folderTree"
            :render-label="renderLabel"
            label-field="name"
            key-field="id"
            filterable
          />
        </n-form-item>
        <n-form-item :label="t('agent.members')" path="scopeIds">
          <CrmUserTagSelector
            v-model:selected-list="form.scopeIds"
            :api-type-key="MemberApiTypeEnum.FORM_FIELD"
            :member-types="memberTypes"
            :disabled="props.isDetail"
          />
        </n-form-item>
        <n-form-item :label="t('agent.script')" path="script">
          <n-input v-model:value="form.script" type="textarea" :maxlength="500" />
          <div class="text-[12px] text-[var(--text-n4)]">
            {{ '<iframe src="https://xxx?ak=${ak}&sk=${sk}&username=${username}" />' }}
          </div>
        </n-form-item>
        <n-form-item :label="t('common.desc')" path="description">
          <n-input v-model:value="form.description" type="textarea" :maxlength="500" />
        </n-form-item>
      </n-form>
    </n-spin>
  </CrmModal>
</template>

<script setup lang="ts">
  import {
    NForm,
    NFormItem,
    NInput,
    NSpin,
    NTooltip,
    NTreeSelect,
    TreeOption,
    TreeSelectOption,
    useMessage,
  } from 'naive-ui';

  import { MemberApiTypeEnum, MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { SelectedUsersItem } from '@lib/shared/models/system/module';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import type { Option } from '@/components/business/crm-select-user-drawer/type';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  import { addAgent, getAgentDetail, updateAgent } from '@/api/modules';

  const props = defineProps<{
    agentId?: string;
    folderTree: TreeSelectOption[];
    isDetail?: boolean;
    activeFolder?: string;
  }>();
  const emit = defineEmits<{
    (e: 'finish'): void;
  }>();

  const { t } = useI18n();
  const message = useMessage();

  const show = defineModel('show', {
    type: Boolean,
    default: false,
  });

  const loading = ref(false);
  const form = ref({
    name: '',
    script: '',
    scopeIds: [] as SelectedUsersItem[],
    description: '',
    agentModuleId: '',
  });
  const formRef = ref<InstanceType<typeof NForm>>();

  const memberTypes: Option[] = [
    {
      label: t('menu.settings.org'),
      value: MemberSelectTypeEnum.ORG,
    },
  ];
  const title = computed(() => {
    if (props.isDetail) {
      return t('agent.agentDetail');
    }
    if (props.agentId) {
      return t('agent.updateAgent');
    }
    return t('agent.addAgent');
  });

  function renderLabel({ option }: { option: TreeOption; checked: boolean; selected: boolean }) {
    return h(
      NTooltip,
      {
        delay: 300,
      },
      {
        default: () => h('div', {}, { default: () => option.name }),
        trigger: () =>
          h(
            'div',
            {
              class: 'one-line-text max-w-[200px]',
            },
            { default: () => option.name }
          ),
      }
    );
  }

  function handleCancel() {
    form.value = {
      name: '',
      script: '',
      scopeIds: [],
      description: '',
      agentModuleId: '',
    };
  }

  function handleConfirm(isContinue = false) {
    formRef.value?.validate(async (errors) => {
      if (errors) return;
      loading.value = true;
      try {
        if (props.agentId) {
          await updateAgent({
            ...form.value,
            id: props.agentId,
            scopeIds: form.value.scopeIds.map((item) => item.id),
          });
          message.success(t('common.updateSuccess'));
        } else {
          await addAgent({
            ...form.value,
            scopeIds: form.value.scopeIds.map((item) => item.id),
          });
          message.success(t('common.addSuccess'));
        }
        emit('finish');
        if (!isContinue) {
          show.value = false;
        }
        handleCancel();
      } catch (error) {
        // eslint-disable-next-line no-console
        console.error('Failed to add agent:', error);
      } finally {
        loading.value = false;
      }
    });
  }

  const detailLoading = ref(false);
  async function initDetail() {
    try {
      detailLoading.value = true;
      const res = await getAgentDetail(props.agentId!);
      form.value = {
        ...res,
        scopeIds: res.members,
      };
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error('Failed to initialize agent detail:', error);
    } finally {
      detailLoading.value = false;
    }
  }

  watch(
    () => show.value,
    (val) => {
      if (val && props.agentId) {
        initDetail();
      } else {
        form.value.agentModuleId =
          ((props.activeFolder && !['favorite', 'all'].includes(props.activeFolder)
            ? props.activeFolder
            : props.folderTree[0]?.id) as string) || '';
      }
    },
    { immediate: true }
  );

  watch(
    () => props.activeFolder,
    (val) => {
      form.value.agentModuleId =
        ((val && !['favorite', 'all'].includes(val) ? val : props.folderTree[0]?.id) as string) || '';
    }
  );
</script>

<style lang="less" scoped></style>
