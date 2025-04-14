<template>
  <div class="flex gap-[16px] overflow-hidden">
    <div class="flex flex-col items-center gap-[4px]">
      <div class="flex h-[22px] w-[8px] items-center">
        <div class="h-[8px] w-full rounded-[999px] border border-[var(--primary-8)]"></div>
      </div>
      <div class="flex flex-1 justify-center">
        <div class="h-full w-[1px] bg-[var(--primary-8)]"></div>
      </div>
    </div>
    <div class="flex flex-1 flex-col gap-[8px] overflow-hidden">
      <div class="flex items-center gap-[16px]">
        <div class="flex items-center gap-[8px]">
          <CrmTag
            v-if="isPlan"
            :tag="item.status"
            icon="iconicon_block_filled"
            icon-color="var(--text-n4)"
            plain
          ></CrmTag>
          <div>{{ getShowTime(item) }} </div>
        </div>
        <div class="text-[14px] font-semibold text-[var(--text-n1)]">
          {{ (!isPlan ? item.recordMethod : item.planMethod) ?? '-' }}
        </div>
      </div>
      <div class="flex flex-1 flex-col gap-[12px] rounded-[var(--border-radius-large)] bg-[var(--text-n10)] p-[16px]">
        <div class="flex items-center justify-between gap-[16px]">
          <van-image round width="40px" height="40px" src="https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg" />
          <div class="flex flex-1 flex-wrap items-center overflow-hidden">
            <div class="one-line-text flex-1 text-[16px] font-semibold">{{ item.ownerName }}</div>
            <div v-if="!props.readonly" class="flex items-center gap-[16px]">
              <CrmTextButton icon="iconicon_delete" color="var(--error-red)" icon-size="16px" @click="emit('delete')" />
              <CrmTextButton
                v-if="isPlan"
                icon="iconicon_minus_circle1"
                color="var(--primary-8)"
                icon-size="16px"
                @click="emit('cancel')"
              />
              <CrmTextButton
                icon="iconicon_handwritten_signature"
                color="var(--primary-8)"
                icon-size="16px"
                @click="emit('edit')"
              />
            </div>
          </div>
        </div>
        <div class="rounded-[var(--border-radius-mini)] bg-[var(--text-n9)] p-[12px] text-[12px]">
          {{ item.content }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import dayjs from 'dayjs';

  import type { FollowDetailItem } from '@lib/shared/models/customer';

  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import CrmTextButton from '@/components/pure/crm-text-button/index.vue';

  const props = defineProps<{
    item: any;
    type: 'plan' | 'record';
    readonly?: boolean;
  }>();
  const emit = defineEmits<{
    (e: 'edit'): void;
    (e: 'delete'): void;
    (e: 'cancel'): void;
  }>();

  const isPlan = computed(() => {
    return props.type === 'plan';
  });

  function getShowTime(item: FollowDetailItem) {
    const time = 'estimatedTime' in item ? item.estimatedTime : item.followTime;
    return time ? dayjs(time).format('YYYY-MM-DD') : '-';
  }
</script>

<style lang="less" scoped></style>
