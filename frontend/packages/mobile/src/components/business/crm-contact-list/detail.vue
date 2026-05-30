<template>
  <CrmPageWrapper :title="route.query.name?.toString() || ''">
    <div class="relative h-full bg-[var(--text-n9)] pt-[16px]">
      <CrmDescription :description="descriptions" />
    </div>
    <template v-if="route.query.readonly?.toString() === 'N'" #footer>
      <CrmActionButtons
        :show-edit-button="hasAllPermission(['CUSTOMER_MANAGEMENT_CONTACT:UPDATE'])"
        :actions="actions"
        @select="handleMoreSelect"
      />
    </template>
  </CrmPageWrapper>
</template>

<script setup lang="ts">
  import { useRoute, useRouter } from 'vue-router';
  import { showConfirmDialog, showSuccessToast } from 'vant';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { sleep } from '@lib/shared/method';

  import CrmDescription from '@/components/pure/crm-description/index.vue';
  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';

  import { deleteCustomerContact } from '@/api/modules';
  import useFormCreateApi from '@/hooks/useFormCreateApi';
  import { hasAllPermission } from '@/utils/permission';

  import { CommonRouteEnum } from '@/enums/routeEnum';

  const route = useRoute();
  const router = useRouter();
  const { t } = useI18n();

  const { descriptions, initFormConfig, initFormDescription } = useFormCreateApi({
    formKey: FormDesignKeyEnum.CUSTOMER_CONTACT,
    sourceId: ref(route.query.id?.toString() || ''),
    needInitDetail: true,
  });
  const loading = ref(false);

  function handleDelete() {
    showConfirmDialog({
      title: t('contact.deleteTitle'),
      message: t('contact.deleteTip'),
      confirmButtonText: t('common.confirmDelete'),
      confirmButtonColor: 'var(--error-red)',
      beforeClose: async (action) => {
        if (action === 'confirm') {
          try {
            loading.value = true;
            await deleteCustomerContact(route.query.id?.toString() || '');
            showSuccessToast(t('common.deleteSuccess'));
            await sleep(300);
            router.back();
            return Promise.resolve(true);
          } catch (error) {
            // eslint-disable-next-line no-console
            console.log(error);
            return Promise.resolve(false);
          } finally {
            loading.value = false;
          }
        } else {
          return Promise.resolve(true);
        }
      },
    });
  }

  const actions = ref([
    {
      key: 'delete',
      text: t('common.delete'),
      color: 'var(--error-red)',
      permission: ['CUSTOMER_MANAGEMENT_CONTACT:DELETE'],
    },
  ]);

  function handleEdit(id: string) {
    router.push({
      name: CommonRouteEnum.FORM_CREATE,
      query: {
        id,
        formKey: id ? FormDesignKeyEnum.CUSTOMER_CONTACT : FormDesignKeyEnum.CONTACT,
        needInitDetail: 'Y',
      },
    });
  }

  function handleMoreSelect(key: string) {
    switch (key) {
      case 'edit':
        handleEdit(route.query.id?.toString() || '');
        break;
      case 'delete':
        handleDelete();
        break;
      default:
        break;
    }
  }

  onBeforeMount(async () => {
    await initFormConfig();
    initFormDescription();
  });
</script>

<style lang="less" scoped></style>
