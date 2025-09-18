<template>
  <CrmDrawer v-model:show="visible" :footer="false" width="80vw">
    <template #titleLeft>
      <n-tooltip trigger="hover" :disabled="agentList.length > 0">
        <template #trigger>
          <n-select
            v-model:value="activeAgent"
            :options="agentList"
            :placeholder="t('common.pleaseSelect')"
            class="w-[200px]"
            :disabled="agentList.length === 0"
            :loading="loading"
            label-field="name"
            value-field="id"
            @update-value="handleAgentChange"
          />
        </template>
        {{ t('agentDrawer.unAddedAgent') }}
      </n-tooltip>
    </template>
    <div class="h-full">
      <n-empty v-if="agentList.length === 0" :show-icon="false">
        <div class="flex h-[200px] items-center justify-between gap-[8px]">
          {{ t('agentDrawer.noAgent') }}
          <n-button v-permission="['AGENT:READ']" type="primary" text @click="jump">
            {{ t('agentDrawer.addAgent') }}
          </n-button>
        </div>
      </n-empty>
      <div v-else class="h-full" v-html="activeAgentScript"></div>
    </div>
  </CrmDrawer>
</template>

<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import { NButton, NEmpty, NSelect, NTooltip } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';

  import { getAgentOptions } from '@/api/modules';
  import useUserStore from '@/store/modules/user';

  import { AgentRouteEnum } from '@/enums/routeEnum';

  const router = useRouter();
  const { t } = useI18n();
  const userStore = useUserStore();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const activeAgent = ref<string | undefined>(localStorage.getItem('crm-agent-drawer-active-agent') || undefined);
  const agentList = ref<Record<string, any>[]>([]);
  const firstValidApiKey = computed(() => userStore.apiKeyList.find((key) => !key.isExpire));
  const activeAgentScript = computed(() => {
    const script = (agentList.value.find((agent) => agent.id === activeAgent.value)?.script as string) || '';
    let result = script.replace(/\$\{ak\}/g, firstValidApiKey.value?.accessKey || '');
    result = result.replace(/\$\{sk\}/g, firstValidApiKey.value?.secretKey || '');
    result = result.replace(/\$\{username\}/g, userStore.userInfo.name);
    return result;
  });

  function jump() {
    visible.value = false;
    router.push({ name: AgentRouteEnum.AGENT_INDEX, query: { showAdd: 'Y' } });
  }

  const loading = ref(false);
  async function initAgentList() {
    try {
      loading.value = true;
      agentList.value = await getAgentOptions();
      if (
        (!activeAgent.value && agentList.value.length > 0) ||
        (activeAgent.value && !agentList.value.find((agent) => agent.id === activeAgent.value))
      ) {
        activeAgent.value = agentList.value[0].id;
        localStorage.setItem('crm-agent-drawer-active-agent', agentList.value[0].id);
      } else if (agentList.value.length === 0) {
        activeAgent.value = undefined;
        localStorage.removeItem('crm-agent-drawer-active-agent');
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  function handleAgentChange(val: string) {
    localStorage.setItem('crm-agent-drawer-active-agent', val);
  }

  watch(
    () => visible.value,
    (val) => {
      if (val) {
        initAgentList();
      }
    }
  );
</script>

<style lang="less" scoped></style>
