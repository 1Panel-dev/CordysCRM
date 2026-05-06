<template>
  <n-scrollbar>
    <n-timeline>
      <n-timeline-item>
        <template #icon>
          <CrmIcon type="iconicon_add" />
        </template>
        <template #header>
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-[8px]">
              <div class="font-semibold">审批节点</div>
              <CrmTag type="info">{{ t(`common.${ProcessTypeEnum.AND_APPROVAL}`) }}</CrmTag>
              <CrmApprovalStatus :status="ProcessStatusEnum.APPROVED" isTag />
            </div>
            <div class="text-[var(--text-n4)]">2026-2-32 12:32:22</div>
          </div>
        </template>
        <div class="bg-[var(--text-n9)] p-[8px]"></div>
        <CrmFileList
          v-if="currentFileList.length > 0"
          :files="currentFileList as unknown as AttachmentInfo[]"
          class="mt-[8px]"
          readonly
        />
        <n-collapse>
          <n-collapse-item :title="t('common.copyTo')" name="copyTo">
            <template #header>
              <div class="flex items-center gap-[8px]">
                <CrmIcon type="iconicon_send" />
                <div>{{ t('common.copyTo') }}</div>
              </div>
            </template>
            <template #header-extra="{ collapsed }">
              <CrmIcon :type="collapsed ? 'iconicon_arrow_right' : 'iconicon_arrow_down'" />
            </template>
            <div class="bg-[var(--text-n9)] p-[8px]"></div>
          </n-collapse-item>
        </n-collapse>
      </n-timeline-item>
    </n-timeline>
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { NCollapse, NCollapseItem, NScrollbar, NTimeline, NTimelineItem } from 'naive-ui';

  import { ProcessStatusEnum, ProcessTypeEnum } from '@lib/shared/enums/process';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import type { CrmFileItem } from '@/components/pure/crm-upload/types';
  import CrmApprovalStatus from '@/components/business/crm-approval-status/index.vue';
  import CrmFileList from '@/components/business/crm-file-list/index.vue';

  import type { AttachmentInfo } from '../../crm-form-create/types';

  const { t } = useI18n();

  const currentFileList = ref<CrmFileItem[]>([]);
</script>

<style lang="less" scoped></style>
