<template>
  <CrmModal
    v-model:show="showEditDrawer"
    size="small"
    :title="t('common.batchEdit')"
    :ok-loading="loading"
    @cancel="cancelHandler"
  >
    <n-form ref="formRef" :model="form" label-placement="left" label-width="auto">
      <n-form-item
        :rule="[{ required: true, message: t('common.notNull', { value: `${t('org.attributes')}` }) }]"
        require-mark-placement="left"
        label-placement="left"
        path="attributes"
        :label="t('org.attributes')"
      >
        <n-select
          v-model:value="form.attributes"
          :placeholder="t('common.pleaseSelect')"
          :options="attributesOptions"
        />
      </n-form-item>
      <n-form-item
        v-if="!form.attributes"
        :rule="[{ required: true, message: t('common.value.notNull') }]"
        require-mark-placement="left"
        label-placement="left"
        path="value"
        :label="t('common.batchUpdate')"
      >
        <n-select v-model:value="form.value" :placeholder="t('common.pleaseSelect')" :options="valueOptions" />
      </n-form-item>
      <n-form-item
        v-else-if="form.attributes === 'departmentId'"
        :rule="[{ required: true, message: t('common.value.notNull') }]"
        require-mark-placement="left"
        label-placement="left"
        path="value"
        :label="t('common.batchUpdate')"
      >
        <n-tree-select
          v-model:value="form.value"
          :options="department"
          label-field="name"
          key-field="id"
          filterable
          clearable
          children-field="children"
        />
      </n-form-item>
      <n-form-item
        v-else-if="form.attributes === 'supervisorId'"
        :rule="[{ required: true, message: t('common.value.notNull') }]"
        require-mark-placement="left"
        label-placement="left"
        path="value"
        :label="t('common.batchUpdate')"
      >
        <CrmUserSelect
          v-model:value="form.value"
          value-field="id"
          label-field="name"
          mode="remote"
          filterable
          :fetch-api="getUserOptions"
        />
      </n-form-item>
      <n-form-item
        v-else-if="form.attributes === 'workCity'"
        :rule="[{ required: true, message: t('common.value.notNull') }]"
        require-mark-placement="left"
        label-placement="left"
        path="value"
        :label="t('common.batchUpdate')"
      >
        <CrmCitySelect v-model:value="form.value" />
      </n-form-item>
    </n-form>
    <template #footer>
      <div class="flex w-full items-center justify-end gap-[12px]">
        <n-button :disabled="loading" secondary @click="cancelHandler">
          {{ t('common.cancel') }}
        </n-button>
        <n-button :loading="loading" type="primary" @click="confirmHandler">
          {{ t('common.update') }}
        </n-button>
      </div>
    </template>
  </CrmModal>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import {
    DataTableRowKey,
    FormInst,
    NButton,
    NForm,
    NFormItem,
    NSelect,
    NTreeSelect,
    SelectOption,
    useMessage,
  } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmModal from '@/components/pure/crm-modal/index.vue';
  import type { CrmTreeNodeData } from '@/components/pure/crm-tree/type';
  import CrmCitySelect from '@/components/business/crm-city-select/index.vue';
  import CrmUserSelect from '@/components/business/crm-user-select/index.vue';

  import { batchEditUser, getDepartmentTree, getUserOptions } from '@/api/modules';

  const Message = useMessage();
  const { t } = useI18n();

  const props = defineProps<{
    userIds: DataTableRowKey[];
  }>();

  const emit = defineEmits<{
    (e: 'loadList'): void;
  }>();

  const showEditDrawer = defineModel<boolean>('show', {
    required: true,
    default: false,
  });

  const form = ref<{
    attributes: string | null;
    value: string | null;
  }>({
    attributes: null,
    value: null,
  });

  const attributesOptions = ref<SelectOption[]>([
    {
      value: 'departmentId',
      label: t('org.department'),
    },
    {
      value: 'supervisorId',
      label: t('org.directSuperior'),
    },
    {
      value: 'workCity',
      label: t('org.workingCity'),
    },
  ]);

  const valueOptions = ref([]);

  function cancelHandler() {
    form.value.attributes = null;
    form.value.value = null;
    showEditDrawer.value = false;
  }

  const loading = ref<boolean>(false);
  const formRef = ref<FormInst | null>(null);
  function confirmHandler() {
    formRef.value?.validate(async (error) => {
      if (!error) {
        try {
          const { value, attributes } = form.value;

          if (!attributes) {
            return;
          }
          loading.value = true;
          await batchEditUser({
            ids: props.userIds,
            [attributes]: value,
          });
          cancelHandler();
          emit('loadList');
          Message.success(t('common.updateSuccess'));
        } catch (e) {
          // eslint-disable-next-line no-console
          console.log(e);
        }
      }
    });
  }

  const department = ref<CrmTreeNodeData[]>([]);
  async function initDepartList() {
    try {
      department.value = await getDepartmentTree();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => showEditDrawer.value,
    (val) => {
      if (val) {
        initDepartList();
      }
    }
  );
</script>

<style scoped></style>
