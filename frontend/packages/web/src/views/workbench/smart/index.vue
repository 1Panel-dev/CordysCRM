<template>
  <n-scrollbar class="h-full">
    <div class="smart-workbench flex min-h-full flex-col gap-[16px]">
      <AiChatProvider :runtime="composerRuntime">
        <AiComposer
          class="rounded-[4px] !shadow-none"
          :mcp-options="mcpOptions"
          submit-mode="emit"
          :placeholder="t('workbench.smart.composerPlaceholder')"
          @submit="handleComposerSubmit"
        />
      </AiChatProvider>
      <CrmCard no-content-padding hide-footer>
        <template #header>
          <div class="text-[14px] font-semibold">{{ t('workbench.dataOverView') }}</div>
        </template>
        <template #header-extra>
          <n-button type="primary" class="mr-[16px]" ghost @click="">{{ t('workbench.smart.reBuild') }}</n-button>
          <button class="gradient-border-button">
            <n-gradient-text
              :style="{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                fontWeight: 400,
              }"
              gradient="linear-gradient(96.9deg, #3370FF 0%, #E22E23 47.65%, #00C261 100%)"
            >
              <CrmIcon
                type="iconicon_star1"
                :size="16"
                color="linear-gradient(130.1deg, #FFA200 -30.47%, #E22E23 42.7%, #00C261 113.44%)"
              />
              {{ t('workbench.smart.AIRead') }}
            </n-gradient-text>
          </button>
        </template>
        <div class="p-[16px_24px]">
          <n-empty v-if="!dataOverviewAIRenderString" :description="t('common.noData')" />
          <div class="h-full w-full" v-html="dataOverviewAIRenderString"> </div>
        </div>
      </CrmCard>
      <div class="flex w-full gap-[16px]">
        <CrmCard class="flex-1" no-content-padding hide-footer>
          <template #header>
            <div class="flex items-center gap-[8px]">
              <CrmIcon type="iconicon_star1" :size="16" color="var(--primary-8)" />
              <div class="text-[14px] font-semibold">{{ t('workbench.smart.AIAction') }}</div>
            </div>
          </template>
          <div class="p-[16px_24px]">
            <n-empty v-if="!AIActionRenderString" :description="t('common.noData')" />
            <div class="h-full w-full" v-html="AIActionRenderString"> </div>
          </div>
        </CrmCard>
        <CrmCard class="flex-1" no-content-padding hide-footer>
          <template #header>
            <div class="flex items-center gap-[8px]">
              <CrmIcon type="iconicon_star1" :size="16" color="var(--primary-8)" />
              <div class="text-[14px] font-semibold">{{ t('workbench.smart.AIActionApproval') }}</div>
            </div>
          </template>
          <div class="p-[16px_24px]">
            <n-empty v-if="!AIActionApprovalRenderString" :description="t('common.noData')" />
            <div class="h-full w-full" v-html="AIActionApprovalRenderString"> </div>
          </div>
        </CrmCard>
      </div>
    </div>
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { onBeforeUnmount, ref } from 'vue';
  import { NButton, NEmpty, NGradientText, NScrollbar } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import {
    type AiChatMcp,
    AiChatProvider,
    AiComposer,
    type AiComposerSubmitPayload,
    createAiChatRuntime,
  } from '@/components/business/ai-chat';

  const { t } = useI18n();

  // TODO lmy 获取后端数据
  const mcpOptions: AiChatMcp[] = [
    { id: 'cordys-crm', name: 'codys-crm', permission: 'read' },
    { id: 'ardot-design-assistant', name: 'mock', permission: 'read' },
  ];
  const dataOverviewAIRenderString = ref('');
  const AIActionRenderString = ref('');
  const AIActionApprovalRenderString = ref('');

  const composerRuntime = createAiChatRuntime();

  function handleComposerSubmit(payload: AiComposerSubmitPayload): void {
    composerRuntime.clear();
    window.dispatchEvent(
      new CustomEvent('crm-ai-chat-floating-open', {
        detail: {
          content: payload.content,
          attachments: payload.attachments,
          mcps: payload.options?.mcps ?? [],
        },
      })
    );
  }

  onBeforeUnmount(() => {
    composerRuntime.clear();
  });
</script>

<style scoped lang="less">
  .gradient-border-button {
    padding: 4px 12px;
    border: 1px solid transparent;
    border-radius: 4px;
    background-clip: padding-box, border-box;

    /* background layer: button fill; border layer: gradient */
    background-image: linear-gradient(var(--primary-7), var(--primary-7)),
      linear-gradient(96.9deg, #3370ff 0%, #e22e23 47.65%, #00c261 100%);
    background-origin: border-box;
    &:hover {
      border: 1px solid transparent;
      background-image: linear-gradient(var(--n-button-color, #ffffff), var(--n-button-color, #ffffff)),
        linear-gradient(96.9deg, #3370ff 0%, #e22e23 47.65%, #00c261 100%) !important;
    }
  }
</style>
