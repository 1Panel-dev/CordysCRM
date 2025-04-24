<template>
  <div class="flex h-full flex-col overflow-hidden bg-[var(--text-n9)]">
    <div class="flex-1 p-[16px]">
      <div
        v-for="item in collaborators"
        :key="item.id"
        class="mb-[16px] flex w-full items-center gap-[16px] rounded-[var(--border-radius-small)] bg-[var(--text-n10)] p-[16px]"
      >
        <CrmAvatar :text="item.userName" />
        <div class="flex flex-1 flex-col gap-[2px]">
          <div class="flex items-center justify-between">
            <div class="flex-1 text-[16px] text-[var(--text-n1)]">{{ item.userName }}</div>
            <div class="flex items-center gap-[16px]">
              <CrmTextButton
                v-permission="['CUSTOMER_MANAGEMENT:UPDATE']"
                icon="iconicon_delete"
                icon-size="16px"
                color="var(--error-red)"
                @click="() => handleDelete(item.id)"
              />
              <CrmTextButton
                v-permission="['CUSTOMER_MANAGEMENT:UPDATE']"
                icon="iconicon_handwritten_signature"
                color="var(--primary-8)"
                icon-size="16px"
                @click="() => handleEdit(item.id)"
              />
            </div>
          </div>
          <div class="flex items-center gap-[4px]">
            <CrmTag
              :tag="item.collaborationType === 'READ_ONLY' ? t('customer.readOnly') : t('customer.cooperation')"
              :icon="item.collaborationType === 'READ_ONLY' ? 'iconicon_browse' : 'iconicon_handwritten_signature'"
              bg-color="var(--text-n9)"
            />
            <CrmTag :tag="item.departmentName" bg-color="var(--text-n9)" />
          </div>
        </div>
      </div>
    </div>
    <div v-permission="['CUSTOMER_MANAGEMENT:UPDATE']" class="bg-[var(--text-n10)] p-[16px]">
      <van-button type="primary" class="rounded-[var(--border-radius-small)]" block plain @click="goCollaborator">
        {{ t('customer.addCollaborator') }}
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { closeToast, showLoadingToast, showSuccessToast } from 'vant';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { sleep } from '@lib/shared/method';
  import { CollaborationItem } from '@lib/shared/models/customer';

  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';

  import { deleteCustomerCollaboration, getCustomerCollaborationList } from '@/api/modules';

  import { CustomerRouteEnum } from '@/enums/routeEnum';

  const props = defineProps<{
    sourceId: string;
  }>();

  const { t } = useI18n();
  const router = useRouter();

  const collaborators = ref<CollaborationItem[]>([]);

  async function initList() {
    try {
      showLoadingToast(t('common.loading'));
      collaborators.value = await getCustomerCollaborationList({ customerId: props.sourceId });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      closeToast();
    }
  }

  async function handleDelete(id: string) {
    try {
      showLoadingToast(t('common.deleting'));
      await deleteCustomerCollaboration(id);
      showSuccessToast(t('common.deleteSuccess'));
      initList();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      await sleep(300);
      closeToast();
    }
  }

  function goCollaborator() {
    router.push({
      name: CustomerRouteEnum.CUSTOMER_COLLABORATOR,
      query: {
        sourceId: props.sourceId,
      },
      state: {
        params: JSON.stringify({ userIds: collaborators.value.map((e) => e.userId) }),
      },
    });
  }

  function handleEdit(id: string) {
    router.push({
      name: CustomerRouteEnum.CUSTOMER_COLLABORATOR,
      query: {
        sourceId: props.sourceId,
        id,
      },
      state: {
        params: JSON.stringify({ userIds: collaborators.value.map((e) => e.userId) }),
      },
    });
  }

  onBeforeMount(() => {
    initList();
  });

  defineExpose({
    initList,
  });
</script>

<style lang="less" scoped></style>
