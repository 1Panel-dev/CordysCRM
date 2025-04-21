<template>
  <div class="flex h-full flex-col overflow-hidden bg-[var(--text-n9)]">
    <div class="top-bar">
      <van-button plain icon="plus" type="primary" size="small" @click="goCreate"> </van-button>
      <van-search
        v-model="keyword"
        shape="round"
        :placeholder="t('customer.searchContactPlaceholder')"
        class="flex-1 !p-0"
      />
    </div>
    <CrmList
      ref="crmListRef"
      :list-params="listParams"
      :keyword="keyword"
      :load-list-api="getCustomerContactList"
      class="p-[16px]"
      :item-gap="16"
      :transform="transformFormData"
    >
      <template #item="{ item }">
        <div
          class="flex w-full items-center gap-[16px] rounded-[var(--border-radius-small)] bg-[var(--text-n10)] p-[16px]"
          @click="goDetail(item)"
        >
          <van-image round width="40px" height="40px" src="https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg" />
          <div class="flex flex-1 flex-col gap-[2px]">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-[8px]">
                <div class="text-[16px] text-[var(--text-n1)]">{{ item.name }}</div>
                <van-tag
                  color="var(--success-5)"
                  text-color="var(--success-green)"
                  class="rounded-[var(--border-radius-small)] !p-[2px_6px]"
                >
                  {{ t('common.normal') }}
                </van-tag>
              </div>
              <CrmTextButton
                icon="iconicon_delete"
                icon-size="16px"
                color="var(--error-red)"
                @click="() => handleDelete(item.id)"
              />
            </div>
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-[4px]">
                <CrmIcon name="iconicon_mobile" width="12px" height="12px" color="var(--text-n2)" />
                <div class="text-[12px] text-[var(--text-n2)]">
                  {{ item.phone?.replace(/(\d{3})(\d{4})(\d{4})/, '$1 $2 $3') }}
                </div>
                <CrmIcon
                  name="iconicon_file_copy"
                  width="12px"
                  height="12px"
                  color="var(--primary-8)"
                  @click.stop="() => handleCopy(item.phone)"
                />
              </div>
            </div>
          </div>
        </div>
      </template>
    </CrmList>
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { useClipboard } from '@vueuse/core';
  import { showConfirmDialog, showFailToast, showSuccessToast } from 'vant';

  import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { sleep } from '@lib/shared/method';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmList from '@/components/pure/crm-list/index.vue';
  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';

  import { deleteCustomerContact, getCustomerContactList } from '@/api/modules';
  import useFormCreateTransform from '@/hooks/useFormCreateTransform';

  import { CommonRouteEnum } from '@/enums/routeEnum';

  const { t } = useI18n();
  const router = useRouter();
  const { copy, isSupported } = useClipboard({ legacy: true });

  const { transformFormData } = await useFormCreateTransform(FormDesignKeyEnum.CUSTOMER_CONTACT);

  const crmListRef = ref<InstanceType<typeof CrmList>>();
  const keyword = ref('');
  const activeFilter = ref(CustomerSearchTypeEnum.ALL);
  const listParams = computed(() => {
    return {
      searchType: activeFilter.value,
      keyword: keyword.value,
    };
  });

  function handleCopy(val: string) {
    if (isSupported) {
      copy(val);
      showSuccessToast(t('common.copySuccess'));
    } else {
      showFailToast(t('common.copyNotSupport'));
    }
  }

  function handleDelete(id: string) {
    showConfirmDialog({
      title: t('contact.deleteTitle'),
      message: t('contact.deleteTip'),
      confirmButtonText: t('common.confirmDelete'),
      confirmButtonColor: 'var(--error-red)',
      beforeClose: async (action) => {
        if (action === 'confirm') {
          try {
            await deleteCustomerContact(id);
            showSuccessToast(t('common.deleteSuccess'));
            await sleep(300);
            crmListRef.value?.loadList(true);
            return Promise.resolve(true);
          } catch (error) {
            // eslint-disable-next-line no-console
            console.log(error);
            return Promise.resolve(false);
          }
        } else {
          return Promise.resolve(true);
        }
      },
    });
  }

  function goCreate() {
    router.push({
      name: CommonRouteEnum.FORM_CREATE,
      query: {
        formKey: FormDesignKeyEnum.CONTACT,
      },
    });
  }

  function goDetail(item: any) {
    router.push({
      name: CommonRouteEnum.CONTACT_DETAIL,
      query: {
        id: item.id,
        name: item.name,
        needInitDetail: 'Y',
      },
    });
  }

  onActivated(() => {
    crmListRef.value?.loadList(true);
  });
</script>

<style lang="less" scoped></style>
