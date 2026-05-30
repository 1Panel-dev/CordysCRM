<template>
  <div class="flex h-full flex-col overflow-hidden">
    <CrmPageHeader :title="selectedAgentName">
      <template #rightSlot>
        <CrmIcon name="iconview-list" width="24px" height="24px" color="var(--text-n2)" @click="selectAgent" />
      </template>
    </CrmPageHeader>
    <div class="mt-[48px] flex flex-1 flex-col">
      <div class="h-full" v-html="activeAgentScript"></div>
    </div>

    <van-popup v-model:show="showAgent" position="right" :style="{ width: '75%', height: '100%' }">
      <CrmPageWrapper :title="t('workbench.agent.select')" hide-back>
        <div class="flex h-full flex-col overflow-hidden">
          <div class="flex-1 overflow-hidden px-[16px]">
            <CrmSelectList
              v-model:value="tempActiveAgent"
              v-model:selected-rows="selectedRows"
              :data="agentList"
              :multiple="false"
              no-page-nation
            />
          </div>
        </div>
        <template #footer>
          <div class="flex items-center gap-[16px]">
            <van-button
              type="primary"
              plain
              class="!rounded-[var(--border-radius-small)] !text-[16px]"
              block
              :disabled="!selectedRows.length"
              @click="onConfirm"
            >
              {{ t('common.confirm') }}
            </van-button>
          </div>
        </template>
      </CrmPageWrapper>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmPageWrapper from '@/components/pure/crm-page-wrapper/index.vue';
  import CrmSelectList from '@/components/business/crm-select-list/index.vue';

  import { getAgentOptions } from '@/api/modules';
  import useUserStore from '@/store/modules/user';

  const { t } = useI18n();

  const userStore = useUserStore();
  const showAgent = ref(false);

  const activeAgent = ref<string>(localStorage.getItem('crm-agent-drawer-active-agent') || '');
  const tempActiveAgent = ref<string>(activeAgent.value);
  const agentList = ref<Record<string, any>[]>([]);
  const selectedRows = ref<Record<string, any>[]>([]);
  const firstValidApiKey = computed(() => userStore.apiKeyList.find((key) => !key.isExpire && key.enable));
  const selectedAgentName = computed(() => agentList.value.find((agent) => agent.id === activeAgent.value)?.name ?? '');
  const activeAgentScript = computed(() => {
    const script = (agentList.value.find((agent) => agent.id === activeAgent.value)?.script as string) || '';
    let result = script.replace(/\$\{ak\}/g, firstValidApiKey.value?.accessKey || '');
    result = result.replace(/\$\{sk\}/g, firstValidApiKey.value?.secretKey || '');
    result = result.replace(/\$\{username\}/g, userStore.userInfo.name);
    return result;
  });

  function selectAgent() {
    tempActiveAgent.value = activeAgent.value;
    selectedRows.value = agentList.value.filter((agent) => agent.id === tempActiveAgent.value);
    showAgent.value = true;
  }

  const loading = ref(false);
  async function initAgentList() {
    try {
      loading.value = true;
      agentList.value = await getAgentOptions();

      const hasAgentId = agentList.value.find((agent) => agent.id === activeAgent.value);
      if ((!activeAgent.value && agentList.value.length > 0) || (activeAgent.value && !hasAgentId)) {
        activeAgent.value = agentList.value[0]?.id ?? '';
        if (activeAgent.value) {
          localStorage.setItem('crm-agent-drawer-active-agent', activeAgent.value);
        } else {
          localStorage.removeItem('crm-agent-drawer-active-agent');
        }
      } else if (agentList.value.length === 0) {
        localStorage.removeItem('crm-agent-drawer-active-agent');
        activeAgent.value = '';
      }

      selectedRows.value = agentList.value.filter((agent) => agent.id === activeAgent.value);
      tempActiveAgent.value = activeAgent.value;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  function onConfirm() {
    activeAgent.value = tempActiveAgent.value;
    localStorage.setItem('crm-agent-drawer-active-agent', activeAgent.value ?? '');
    showAgent.value = false;
  }

  function initAPIKeyConfig() {
    userStore.initApiKeyList();
    initAgentList();
  }

  watch(
    () => showAgent.value,
    (val) => {
      if (val) {
        initAPIKeyConfig();
      }
    }
  );

  onBeforeMount(() => {
    initAPIKeyConfig();
  });
</script>

<style scoped></style>
