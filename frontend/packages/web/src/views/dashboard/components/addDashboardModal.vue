<template>
  <CrmModal
    v-model:show="show"
    :title="t('dashboard.addDashboard')"
    :ok-loading="loading"
    :positive-text="t('common.add')"
    size="large"
    show-continue
    @confirm="handleConfirm"
  >
    <n-form
      ref="formRef"
      :model="form"
      label-placement="left"
      :rules="{
        name: [
          {
            required: true,
            message: t('common.notNull', { value: t('dashboard.dashboardName') }),
          },
        ],
        id: [
          {
            required: true,
            message: t('common.notNull', { value: t('dashboard.dashboardId') }),
          },
        ],
        folder: [
          {
            required: true,
            message: t('common.notNull', { value: t('dashboard.folder') }),
          },
        ],
      }"
      label-width="100"
    >
      <n-form-item :label="t('dashboard.dashboardName')" path="name">
        <n-input v-model="form.name" :maxlength="255" />
      </n-form-item>
      <n-form-item :label="t('dashboard.dashboardId')" path="id">
        <n-input v-model="form.id" :maxlength="255" />
        <a
          href="https://rtykuh4z44.feishu.cn/docx/QN1TdHfxjofZXXxZ0toc9oIpnWd"
          class="text-[var(--primary-8)]"
          target="_blank"
          >https://rtykuh4z44.feishu.cn/docx/QN1TdHfxjofZXXxZ0toc9oIpnWd</a
        >
      </n-form-item>
      <n-form-item :label="t('dashboard.folder')" path="folder">
        <n-tree-select v-model:value="form.folder" :options="props.folderTree" />
      </n-form-item>
      <n-form-item :label="t('dashboard.members')" path="members">
        <CrmUserTagSelector
          v-model:selected-list="form.members"
          :api-type-key="MemberApiTypeEnum.FORM_FIELD"
          :member-types="memberTypes"
        />
      </n-form-item>
      <n-form-item :label="t('common.desc')" path="description">
        <n-input v-model="form.description" type="textarea" :maxlength="500" />
      </n-form-item>
    </n-form>
  </CrmModal>
</template>

<script setup lang="ts">
  import { NForm, NFormItem, NInput, NTreeSelect, TreeSelectOption } from 'naive-ui';

  import { MemberApiTypeEnum, MemberSelectTypeEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import type { Option } from '@/components/business/crm-select-user-drawer/type';
  import CrmUserTagSelector from '@/components/business/crm-user-tag-selector/index.vue';

  const props = defineProps<{
    folderTree: TreeSelectOption[];
  }>();

  const { t } = useI18n();

  const show = defineModel('show', {
    type: Boolean,
    default: false,
  });

  const loading = ref(false);
  const form = ref({
    name: '',
    id: '',
    members: [],
    description: '',
    folder: '',
  });
  const formRef = ref<InstanceType<typeof NForm>>();

  const memberTypes: Option[] = [
    {
      label: t('menu.settings.org'),
      value: MemberSelectTypeEnum.ORG,
    },
  ];

  function handleConfirm() {
    formRef.value?.validate(async (errors) => {
      if (errors) return;
      loading.value = true;
      try {
        show.value = false;
        form.value = {
          name: '',
          id: '',
          members: [],
          description: '',
          folder: '',
        };
      } catch (error) {
        // eslint-disable-next-line no-console
        console.error('Failed to add dashboard:', error);
      } finally {
        loading.value = false;
      }
    });
  }
</script>

<style lang="less" scoped></style>
