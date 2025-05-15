<template>
  <CrmDrawer
    v-model:show="visible"
    :width="800"
    :title="props.title"
    :ok-text="t('common.save')"
    :loading="loading"
    :footer="false"
  >
    <CrmBatchForm
      ref="batchFormRef"
      :models="formItemModel"
      :default-list="form.list"
      :add-text="t('module.businessManage.addRules')"
      validate-when-add
      @delete-row="handleDelete"
      @save-row="handleSave"
    ></CrmBatchForm>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
  import { MemberSelectTypeEnum, ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmBatchForm from '@/components/business/crm-batch-form/index.vue';
  import type { FormItemModel } from '@/components/business/crm-batch-form/types';

  import { addCapacity, deleteCapacity, getCapacityPage, updateCapacity } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  const { t } = useI18n();
  const { openModal } = useModal();

  const props = defineProps<{
    title: string;
    type: ModuleConfigEnum;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const loading = ref(false);

  const batchFormRef = ref<InstanceType<typeof CrmBatchForm>>();

  const form = ref<any>({ list: [] });

  const formItemModel: Ref<FormItemModel[]> = ref([
    {
      path: 'members',
      type: FieldTypeEnum.USER_TAG_SELECTOR,
      label: t('module.capacitySet.departmentOrMember'),
      rule: [
        {
          required: true,
          message: t('common.notNull', { value: t('module.capacitySet.departmentOrMember') }),
        },
        { notRepeat: true, message: t('module.capacitySet.repeatMsg') },
      ],
      userTagSelectorProps: {
        memberTypes: [
          {
            label: t('menu.settings.org'),
            value: MemberSelectTypeEnum.ORG,
          },
          {
            label: t('role.role'),
            value: MemberSelectTypeEnum.ROLE,
          },
        ],
      },
    },
    {
      path: 'capacity',
      type: FieldTypeEnum.INPUT_NUMBER,
      label: t('module.capacitySet.Maximum'),
      formItemClass: 'w-[120px] flex-initial',
      numberProps: {
        min: 0,
        placeholder: t('module.capacitySet.MaximumPlaceholder'),
      },
    },
  ]);

  function handleDelete(index: number, id: string, done: () => void) {
    openModal({
      type: 'error',
      title: t('module.confirmDeleteCapacity'),
      content: t('module.confirmDeleteCapacityContent'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteCapacity(id, props.type);
          done();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  async function getCapacity() {
    try {
      loading.value = true;
      form.value.list = await getCapacityPage(props.type);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  async function handleSave(element: any, done: () => void) {
    try {
      const params = {
        id: element.id,
        scopeIds: element.members.map((m: any) => m.id),
        capacity: element.capacity,
      };

      if (element.id) {
        await updateCapacity(params, props.type);
        done();
      } else {
        await addCapacity(params, props.type);
        done();
        await getCapacity();
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  watch(
    () => visible.value,
    (newVal) => {
      if (newVal) {
        getCapacity();
      } else {
        form.value.list = [];
      }
    }
  );
</script>
