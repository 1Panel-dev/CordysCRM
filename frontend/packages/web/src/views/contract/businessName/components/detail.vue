<template>
  <CrmDrawer v-model:show="visible" width="1000" :title="detailInfo?.name ?? ''">
    <template #titleLeft>
      <div>
        <businessNameStatus :status="detailInfo?.status" />
        <div class="text-[14px] font-normal"> {{ detailInfo?.name ?? '' }} </div>
      </div>
    </template>
    <template #titleRight>
      <CrmButtonGroup class="gap-[12px]" :list="buttonList" not-show-divider @select="handleButtonClick" />
    </template>
    <n-scrollbar>
      <CrmDescription
        :one-line-label="false"
        class="p-[8px]"
        :descriptions="descriptions"
        :column="2"
        label-align="end"
        label-width="100px"
      >
      </CrmDescription>
    </n-scrollbar>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { useMessage } from 'naive-ui';

  import { ContractBusinessNameStatusEnum } from '@lib/shared/enums/contractEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { characterLimit } from '@lib/shared/method';

  import CrmButtonGroup from '@/components/pure/crm-button-group/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import businessNameStatus from './businessNameStatus.vue';

  import { deleteBusinessName, getBusinessNameDetail, revokeBusinessName } from '@/api/modules';
  import useModal from '@/hooks/useModal';
  import { useUserStore } from '@/store';

  const { t } = useI18n();
  const Message = useMessage();
  const useStore = useUserStore();
  const { openModal } = useModal();

  const props = defineProps<{
    sourceId: string;
  }>();

  const emit = defineEmits<{
    (e: 'load'): void;
    (e: 'edit', id: string): void;
  }>();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const detailInfo = ref();

  const buttonList = computed(() => {
    if (detailInfo.value?.status === ContractBusinessNameStatusEnum.APPROVING) {
      return [
        {
          label: t('common.pass'),
          key: 'pass',
          text: false,
          ghost: true,
          class: 'n-btn-outline-primary',
          permission: [],
        },
        {
          label: t('common.unPass'),
          key: 'unPass',
          danger: true,
          text: false,
          ghost: true,
          class: 'n-btn-outline-primary',
          permission: [],
        },
        ...(detailInfo.value?.createUser === useStore.userInfo.id
          ? [
              {
                label: t('common.revoke'),
                key: 'revoke',
                text: false,
                ghost: true,
                class: 'n-btn-outline-primary',
              },
            ]
          : []),
        {
          label: t('common.delete'),
          key: 'delete',
          text: false,
          ghost: true,
          danger: true,
          class: 'n-btn-outline-primary',
          permission: [],
        },
      ];
    }
    if (detailInfo.value?.status === ContractBusinessNameStatusEnum.APPROVED) {
      return [
        {
          label: t('common.delete'),
          key: 'delete',
          text: false,
          ghost: true,
          danger: true,
          class: 'n-btn-outline-primary',
          permission: [],
        },
      ];
    }
    return [
      {
        key: 'edit',
        label: t('common.edit'),
        permission: [],
        text: false,
        ghost: true,
        class: 'n-btn-outline-primary',
      },
      {
        label: t('common.delete'),
        key: 'delete',
        text: false,
        ghost: true,
        danger: true,
        class: 'n-btn-outline-primary',
        permission: [],
      },
    ];
  });

  const descriptions = ref([]);

  async function initDetail() {
    try {
      const result = await getBusinessNameDetail(props.sourceId);
      detailInfo.value = result;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  function handleDelete() {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: characterLimit(detailInfo.value?.name ?? '') }),
      content: t('contract.businessName.deleteContent'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteBusinessName(props.sourceId);
          Message.success(t('common.deleteSuccess'));
          emit('load');
          visible.value = false;
        } catch (error) {
          // eslint-disable-next-line no-console
          console.error(error);
        }
      },
    });
  }

  async function handleApproval(approval = false) {
    try {
      // todo: 调用审批接口
      Message.success(approval ? t('common.approvedSuccess') : t('common.unApprovedSuccess'));
      emit('load');
      initDetail();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function handleRevoke() {
    try {
      await revokeBusinessName(props.sourceId);
      Message.success(t('common.revokeSuccess'));
      emit('load');
      initDetail();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }

  async function handleButtonClick(actionKey: string) {
    switch (actionKey) {
      case 'pass':
        handleApproval(true);
        break;
      case 'unPass':
        handleApproval();
        break;
      case 'edit':
        emit('edit', props.sourceId);
        break;
      case 'revoke':
        handleRevoke();
        break;
      case 'delete':
        handleDelete();
        break;
      default:
        break;
    }
  }

  watch(
    () => visible.value,
    (newVal) => {
      if (newVal) {
        initDetail();
      }
    }
  );
</script>

<style scoped></style>
