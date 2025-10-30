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
          workspaceId: [
            {
              required: true,
              message: t('common.notNull', { value: t('agent.agentWorkSpace') }),
              trigger: 'blur',
            },
          ],
          applicationId: [
            {
              required: true,
              message: t('common.notNull', { value: t('agent.agentSelected') }),
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
        <n-form-item v-if="isShowAddMethod" :label="t('agent.addMethod')" path="method">
          <n-radio-group v-model:value="form.type" name="radiogroup" class="flex" @update:value="changeType">
            <n-radio value="SCRIPT" class="flex-1 text-center">
              {{ t('system.business.SQLBot.embeddedScript') }}
            </n-radio>
            <n-radio value="LIST" class="flex-1 text-center">
              {{ t('agent.addMethodListSelect') }}
            </n-radio>
          </n-radio-group>
        </n-form-item>
        <template v-if="form.type === 'LIST'">
          <n-form-item :label="t('agent.agentWorkSpace')" path="workspaceId">
            <n-select
              v-model:value="form.workspaceId"
              filterable
              :options="workSpaceOptions"
              :disabled="!isEnableConfig || props.isDetail"
              @update:value="changeWorkSpace"
            />
          </n-form-item>

          <n-form-item :label="t('agent.agentSelected')" path="applicationId">
            <n-select
              v-model:value="form.applicationId"
              filterable
              clearable
              :options="agentOptions"
              :disabled="!isEnableConfig || props.isDetail"
              @update:value="changeApplicationId"
            />
          </n-form-item>
        </template>

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
    NRadio,
    NRadioGroup,
    NSelect,
    NSpin,
    NTooltip,
    NTreeSelect,
    SelectOption,
    TreeOption,
    TreeSelectOption,
    useMessage,
  } from 'naive-ui';

  import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';
  import { MemberApiTypeEnum, MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { SelectedUsersItem } from '@lib/shared/models/system/module';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import type { Option } from '@/components/business/crm-select-user-drawer/type';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  import {
    addAgent,
    agentApplicationOptions,
    agentWorkspaceOptions,
    getAgentDetail,
    getApplicationScript,
    getConfigSynchronization,
    getMkAgentVersion,
    updateAgent,
  } from '@/api/modules';

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
    type: 'SCRIPT',
    agentModuleId: '',
    workspaceId: '',
    applicationId: '',
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
      type: 'SCRIPT',
      agentModuleId: '',
      workspaceId: '',
      applicationId: '',
    };
  }

  function handleConfirm(isContinue = false) {
    formRef.value?.validate(async (errors) => {
      if (errors) return;
      loading.value = true;
      try {
        const { workspaceId, applicationId } = form.value;
        if (props.agentId) {
          await updateAgent({
            ...form.value,
            id: props.agentId,
            workspaceId: form.value.type === 'LIST' ? workspaceId : '',
            applicationId: form.value.type === 'LIST' ? applicationId : '',
            scopeIds: form.value.scopeIds.map((item) => item.id),
          });
          message.success(t('common.updateSuccess'));
        } else {
          await addAgent({
            ...form.value,
            scopeIds: form.value.scopeIds.map((item) => item.id),
            workspaceId: form.value.type === 'LIST' ? workspaceId : '',
            applicationId: form.value.type === 'LIST' ? applicationId : '',
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
  const originType = ref('SCRIPT');
  async function initDetail() {
    try {
      detailLoading.value = true;
      const res = await getAgentDetail(props.agentId!);
      form.value = {
        ...res,
        scopeIds: res.members,
      };
      originType.value = res.type;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error('Failed to initialize agent detail:', error);
    } finally {
      detailLoading.value = false;
    }
  }

  const agentOptions = ref<SelectOption[]>([]);
  async function loadApplication(newVal: string) {
    try {
      const res = await agentApplicationOptions(newVal);
      agentOptions.value = res.map((e) => ({ value: e.id, label: e.name }));
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  }

  async function loadApplicationScript(newVal: string) {
    try {
      const res = await getApplicationScript({
        applicationId: newVal,
        workspaceId: form.value.workspaceId,
      });

      form.value.script = '';
      if (res) {
        const scriptString = res.parameters?.map((item) => `${item.parameter}=\${${item.value ?? ''}}`).join('&');
        const scriptParams = scriptString.length > 0 ? `?${scriptString}` : '';
        const script = `<iframe src="${res.src}${scriptParams}" style="width: 100%; height: 100%;" frameborder="0" allow="microphone"></iframe>`;
        form.value.script = script;
      }
    } catch (e) {
      // eslint-disable-next-line no-console
      console.log(e);
    }
  }

  const workSpaceOptions = ref<SelectOption[]>([]);
  const defaultWorkSpaceOptions = [
    {
      value: 'default',
      label: t('agent.agentDefaultWorkspace'),
    },
  ];

  const isDefaultWorkspace = ref(true);
  async function initMkVersion() {
    try {
      const res = await getMkAgentVersion();
      isDefaultWorkspace.value = res === 'PE';
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  async function initWorkSpaceOptions(isInit = false) {
    try {
      if (isDefaultWorkspace.value) {
        workSpaceOptions.value = defaultWorkSpaceOptions;
      } else {
        const res = await agentWorkspaceOptions();
        workSpaceOptions.value = res.map((e) => ({ value: e.id, label: e.name }));
      }

      if (isInit) {
        form.value.workspaceId = (workSpaceOptions.value[0]?.value ?? '') as string;
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error('Failed to get work space options:', error);
    }
  }

  function changeType(val: string) {
    if (val === 'LIST') {
      initWorkSpaceOptions(originType.value === 'SCRIPT');
    }
  }

  const isEnableConfig = ref(false);
  async function initAgentStatus() {
    try {
      const res = await getConfigSynchronization();
      if (res) {
        const mkAgentConfig = res.find((item) => item.type === CompanyTypeEnum.MAXKB);
        isEnableConfig.value = !!mkAgentConfig && !!mkAgentConfig.mkEnable;
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const isShowAddMethod = computed(
    () => isEnableConfig.value || (!!(props.agentId || props.isDetail) && originType.value !== 'SCRIPT')
  );

  function changeWorkSpace(_: string) {
    form.value.applicationId = '';
    form.value.script = '';
  }

  function changeApplicationId(val: string) {
    if (val) {
      loadApplicationScript(val);
    }
  }

  watch(
    () => form.value.workspaceId,
    (val) => {
      if (val) {
        loadApplication(val);
      }
    }
  );

  watch(
    () => show.value,
    (val) => {
      if (val) {
        initAgentStatus();
        initWorkSpaceOptions(!props.agentId);
        if (props.agentId) {
          initDetail();
        } else {
          form.value.agentModuleId =
            ((props.activeFolder && !['favorite', 'all'].includes(props.activeFolder)
              ? props.activeFolder
              : props.folderTree[0]?.id) as string) || '';
          originType.value = 'SCRIPT';
        }
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

  onBeforeMount(() => {
    initMkVersion();
  });
</script>

<style lang="less" scoped></style>
