<template>
  <CrmModal
    v-model:show="show"
    :title="props.dashboardId ? t('dashboard.updateDashboard') : t('dashboard.addDashboard')"
    :ok-loading="loading"
    :positive-text="props.dashboardId ? t('common.update') : t('common.add')"
    size="large"
    :show-continue="!props.dashboardId"
    @confirm="handleConfirm"
    @cancel="handleCancel"
    @continue="handleConfirm(true)"
  >
    <n-spin :show="detailLoading" class="h-full w-full">
      <n-form
        ref="formRef"
        :model="form"
        label-placement="left"
        :rules="{
          name: [
            {
              required: true,
              message: t('common.notNull', { value: t('dashboard.dashboardName') }),
              trigger: 'blur',
            },
          ],
          resourceId: [
            {
              required: true,
              message: t('common.notNull', { value: t('dashboard.dashboardId') }),
              trigger: 'blur',
            },
          ],
          dashboardModuleId: [
            {
              required: true,
              message: t('common.notNull', { value: t('dashboard.folder') }),
              trigger: 'blur',
            },
          ],
        }"
        label-width="100"
      >
        <n-form-item :label="t('dashboard.dashboardName')" path="name">
          <n-input v-model:value="form.name" :maxlength="255" />
        </n-form-item>
        <n-form-item :label="t('dashboard.dashboardId')" path="resourceId">
          <n-input v-model:value="form.resourceId" :maxlength="50" />
          <a
            href="https://rtykuh4z44.feishu.cn/docx/QN1TdHfxjofZXXxZ0toc9oIpnWd"
            class="text-[var(--primary-8)]"
            target="_blank"
            >https://rtykuh4z44.feishu.cn/docx/QN1TdHfxjofZXXxZ0toc9oIpnWd</a
          >
        </n-form-item>
        <n-form-item :label="t('dashboard.folder')" path="dashboardModuleId">
          <n-tree-select
            v-model:value="form.dashboardModuleId"
            :options="props.folderTree"
            label-field="name"
            key-field="id"
          />
        </n-form-item>
        <n-form-item :label="t('dashboard.members')" path="scopeIds">
          <CrmUserTagSelector
            v-model:selected-list="form.scopeIds"
            :api-type-key="MemberApiTypeEnum.FORM_FIELD"
            :member-types="memberTypes"
          />
        </n-form-item>
        <n-form-item :label="t('common.desc')" path="description">
          <n-input v-model:value="form.description" type="textarea" :maxlength="500" />
        </n-form-item>
      </n-form>
    </n-spin>
  </CrmModal>
</template>

<script setup lang="ts">
  import { NForm, NFormItem, NInput, NSpin, NTreeSelect, TreeSelectOption, useMessage } from 'naive-ui';

  import { MemberApiTypeEnum, MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { SelectedUsersItem } from '@lib/shared/models/system/module';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import type { Option } from '@/components/business/crm-select-user-drawer/type';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  import { dashboardAdd, dashboardDetail, dashboardUpdate } from '@/api/modules';

  const props = defineProps<{
    dashboardId?: string;
    folderTree: TreeSelectOption[];
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
    resourceId: '',
    scopeIds: [] as SelectedUsersItem[],
    description: '',
    dashboardModuleId: '',
  });
  const formRef = ref<InstanceType<typeof NForm>>();

  const memberTypes: Option[] = [
    {
      label: t('menu.settings.org'),
      value: MemberSelectTypeEnum.ORG,
    },
  ];

  function handleCancel() {
    form.value = {
      name: '',
      resourceId: '',
      scopeIds: [],
      description: '',
      dashboardModuleId: '',
    };
  }

  function handleConfirm(isContinue = false) {
    formRef.value?.validate(async (errors) => {
      if (errors) return;
      loading.value = true;
      try {
        if (props.dashboardId) {
          await dashboardUpdate({
            ...form.value,
            id: props.dashboardId,
            scopeIds: form.value.scopeIds.map((item) => item.id),
          });
          message.success(t('common.updateSuccess'));
        } else {
          await dashboardAdd({
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
        console.error('Failed to add dashboard:', error);
      } finally {
        loading.value = false;
      }
    });
  }

  const detailLoading = ref(false);
  async function initDetail() {
    try {
      detailLoading.value = true;
      const res = await dashboardDetail(props.dashboardId!);
      form.value = {
        ...res,
        scopeIds: res.members,
      };
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error('Failed to initialize dashboard detail:', error);
    } finally {
      detailLoading.value = false;
    }
  }

  watch(
    () => show.value,
    (val) => {
      if (val && props.dashboardId) {
        initDetail();
      } else {
        form.value.dashboardModuleId = (props.folderTree[0]?.id as string) || '';
      }
    },
    { immediate: true }
  );
</script>

<style lang="less" scoped></style>
