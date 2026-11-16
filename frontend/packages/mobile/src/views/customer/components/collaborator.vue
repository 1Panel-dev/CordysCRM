<template>
  <div class="flex h-full flex-col overflow-hidden bg-[var(--text-n9)]">
    <div class="flex-1 p-[16px]">
      <div
        v-for="item in collaborators"
        :key="item.id"
        class="mb-[16px] flex w-full items-center gap-[16px] rounded-[var(--border-radius-small)] bg-[var(--text-n10)] p-[16px]"
      >
        <CrmAvatar :text="item.userName" />
        <div class="flex flex-1 flex-col gap-[2px] overflow-hidden">
          <div class="flex items-center justify-between">
            <div class="one-line-text flex-1 text-[16px] text-[var(--text-n1)]">{{ item.userName }}</div>
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
                @click="() => handleEdit(item)"
              />
            </div>
          </div>
          <div class="flex items-center gap-[4px] overflow-hidden">
            <CrmTag
              :tag="collaborationTypeMap[item.collaborationType].label"
              :icon="collaborationTypeMap[item.collaborationType].icon"
              bg-color="var(--text-n9)"
            />
            <CrmTag
              v-if="item.departmentName"
              :tag="item.departmentName"
              bg-color="var(--text-n9)"
              class="flex-1 overflow-hidden"
            />
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
  import type { CollaborationItem, CollaborationType } from '@lib/shared/models/customer';

  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmAvatar from '@/components/business/crm-avatar/index.vue';

  import { deleteCustomerCollaboration } from '@/api/modules';
  import useCustomerCollaborators from '@/hooks/useCustomerCollaborators';

  import { CustomerRouteEnum } from '@/enums/routeEnum';

  const props = defineProps<{
    sourceId: string;
  }>();

  const { t } = useI18n();
  const router = useRouter();

  const { collaborators, refresh: refreshCollaborators } = useCustomerCollaborators(computed(() => props.sourceId));
  const collaborationTypeMap = computed<Record<CollaborationType, { label: string; icon: string }>>(() => ({
    READ_ONLY: {
      label: t('customer.readOnly'),
      icon: 'iconicon_browse',
    },
    COLLABORATION: {
      label: t('customer.customerInfoOnly'),
      icon: 'iconicon_handwritten_signature',
    },
    JOINT_FOLLOW: {
      label: t('customer.jointFollow'),
      icon: 'iconicon_handwritten_signature',
    },
  }));

  async function initList() {
    try {
      showLoadingToast(t('common.loading'));
      await refreshCollaborators();
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
      await initList();
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

  function handleEdit(item: CollaborationItem) {
    router.push({
      name: CustomerRouteEnum.CUSTOMER_COLLABORATOR,
      query: {
        sourceId: props.sourceId,
        id: item.id,
      },
      state: {
        params: JSON.stringify({
          userIds: collaborators.value.map((e) => e.userId),
          collaborationType: item.collaborationType,
          expireTime: item.expireTime,
          collaborator: {
            id: item.userId,
            name: item.userName,
          },
        }),
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
