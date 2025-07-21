<template>
  <CrmDrawer
    v-model:show="visible"
    :width="props.type === ModuleConfigEnum.CUSTOMER_MANAGEMENT ? 1000 : 800"
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

  import { IN, NOT_IN } from '@/components/pure/crm-advance-filter/index';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmBatchForm from '@/components/business/crm-batch-form/index.vue';
  import type { FormItemModel } from '@/components/business/crm-batch-form/types';

  import { addCapacity, deleteCapacity, getCapacityPage, updateCapacity } from '@/api/modules';
  import { opportunityBaseSteps, opportunityResultSteps } from '@/config/opportunity';
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

  const formItemModel: Ref<FormItemModel[]> = computed(() => {
    return [
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
      ...(props.type === ModuleConfigEnum.CUSTOMER_MANAGEMENT
        ? ([
            {
              path: 'column',
              type: FieldTypeEnum.SELECT,
              label: t('module.capacitySet.notIncluded'),
              labelTooltip: t('module.capacitySet.notIncludedTip'),
              formItemClass: 'w-[150px] flex-initial',
              selectProps: {
                options: [{ value: 'stage', label: t('opportunity.stage') }],
                disabledFunction: (row: any) => !row.capacity,
                disabledTooltipFunction: () => t('module.capacitySet.notIncludedDisabled'),
              },
            },
            {
              path: 'operator',
              type: FieldTypeEnum.SELECT,
              label: ' ',
              formItemClass: 'w-[120px] flex-initial',
              selectProps: {
                options: [IN, NOT_IN].map((item) => ({ ...item, label: t(item.label) })),
                disabledFunction: (row: any) => !row.capacity,
                disabledTooltipFunction: () => t('module.capacitySet.notIncludedDisabled'),
              },
            },
            {
              path: 'value',
              type: FieldTypeEnum.SELECT_MULTIPLE,
              label: ' ',
              formItemClass: 'w-[220px] flex-initial',
              selectProps: {
                options: [...opportunityBaseSteps, ...opportunityResultSteps],
                disabledFunction: (row: any) => {
                  if (!row.column || !row.operator) {
                    row.value = null;
                  }
                  return !row.capacity || !row.column || !row.operator;
                },
                maxTagCount: 'responsive',
                disabledTooltipFunction: (row: any) =>
                  !row.capacity
                    ? t('module.capacitySet.notIncludedDisabled')
                    : t('module.capacitySet.notIncludedValueDisabled'),
              },
            },
          ] as FormItemModel[])
        : []),
    ];
  });

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
      const res = await getCapacityPage(props.type);
      form.value.list =
        props.type === ModuleConfigEnum.CUSTOMER_MANAGEMENT
          ? res.map((item) => ({ ...item, ...item.filters?.[0] }))
          : res;
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
        ...(props.type === ModuleConfigEnum.CUSTOMER_MANAGEMENT
          ? {
              filters: [
                {
                  column: element.column,
                  operator: element.operator,
                  value: element.value,
                },
              ],
            }
          : {}),
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
