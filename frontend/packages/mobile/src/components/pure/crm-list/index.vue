<template>
  <div class="h-full overflow-auto" :class="props.class">
    <van-pull-refresh v-model="refreshing" @refresh="onLoad">
      <van-list
        v-model:loading="loading"
        v-model:error="error"
        :error-text="t('common.listLoadErrorTip')"
        :finished="finished"
        :finished-text="t('common.listFinishedTip')"
        class="flex h-full flex-col overflow-auto"
        :class="`gap-[${itemGap}px]`"
        @load="onLoad"
      >
        <template v-for="item in list" :key="item.id">
          <slot name="item" :item="item"></slot>
        </template>
        <van-empty v-if="list.length === 0" :description="t('common.noData')" />
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';

  const props = defineProps<{
    keyword?: string;
    class?: string;
    listParams?: Record<string, any>;
    itemGap?: number;
  }>();

  const { t } = useI18n();

  const loading = ref(false);
  const error = ref(false);
  const refreshing = ref(false);
  const finished = ref(false);
  const list = ref<Record<string, any>[]>([
    {
      id: 1,
      name: '张三',
      time: '2025-09-09 12:12:00',
      tag: '重要客户',
      tagBgColor: 'var(--text-n8)',
      tagColor: 'var(--text-n1)',
      content: '你负责的客户 赵胖胖文化有限公司 已被移入公海，并踢了你一脚和扇了一巴掌，请知悉！',
      phone: '13800138000',
      email: 'sagja@sas.csi',
      description: [
        {
          label: '商机名称',
          value: '赵胖胖文化有限公司',
          fullLine: true,
        },
        {
          label: '客户经理',
          value: '张三',
        },
        {
          label: '客户状态',
          value: '公海',
        },
      ],
    },
    {
      id: 2,
      name: '李四',
      time: '2025-09-09 12:12:00',
      tag: '重要客户',
      tagBgColor: 'var(--text-n8)',
      tagColor: 'var(--text-n1)',
      content: '你负责的客户 赵胖胖文化有限公司 已被移入公海，并踢了你一脚和扇了一巴掌，请知悉！',
      description: [
        {
          label: '客户经理',
          value: '张三',
        },
        {
          label: '客户状态',
          value: '公海',
        },
      ],
    },
    {
      id: 3,
      name: '王五',
      time: '2025-09-09 12:12:00',
      tag: '重要客户',
      tagBgColor: 'var(--text-n8)',
      tagColor: 'var(--text-n1)',
      content: '你负责的客户 赵胖胖文化有限公司 已被移入公海，并踢了你一脚和扇了一巴掌，请知悉！',
    },
    {
      id: 4,
      name: '赵六',
      time: '2025-09-09 12:12:00',
      tag: '重要客户',
      tagBgColor: 'var(--info-5)',
      tagColor: 'var(--info-blue)',
      content: '你负责的客户 赵胖胖文化有限公司 已被移入公海，并踢了你一脚和扇了一巴掌，请知悉！',
    },
    {
      id: 5,
      name: '钱七',
      time: '2025-09-09 12:12:00',
      tag: '重要客户',
      tagBgColor: 'var(--info-5)',
      tagColor: 'var(--info-blue)',
      content: '你负责的客户 赵胖胖文化有限公司 已被移入公海，并踢了你一脚和扇了一巴掌，请知悉！',
    },
    {
      id: 6,
      name: '孙八',
      time: '2025-09-09 12:12:00',
      tag: '重要客户',
      tagBgColor: 'var(--info-5)',
      tagColor: 'var(--info-blue)',
      content: '你负责的客户 赵胖胖文化有限公司 已被移入公海，并踢了你一脚和扇了一巴掌，请知悉！',
    },
    {
      id: 7,
      name: '周九',
      time: '2025-09-09 12:12:00',
      tag: '重要客户',
      tagBgColor: 'var(--info-5)',
      tagColor: 'var(--info-blue)',
      content: '你负责的客户 赵胖胖文化有限公司 已被移入公海，并踢了你一脚和扇了一巴掌，请知悉！',
    },
    {
      id: 8,
      name: '吴十',
      time: '2025-09-09 12:12:00',
      tag: '重要客户',
      tagBgColor: 'var(--info-5)',
      tagColor: 'var(--info-blue)',
      content: '你负责的客户 赵胖胖文化有限公司 已被移入公海，并踢了你一脚和扇了一巴掌，请知悉！',
    },
    {
      id: 9,
      name: '郑十一',
      time: '2025-09-09 12:12:00',
      tag: '重要客户',
      tagBgColor: 'var(--info-5)',
      tagColor: 'var(--info-blue)',
      content: '你负责的客户 赵胖胖文化有限公司 已被移入公海，并踢了你一脚和扇了一巴掌，请知悉！',
    },
    {
      id: 10,
      name: '冯十二',
      time: '2025-09-09 12:12:00',
      tag: '重要客户',
      tagBgColor: 'var(--info-5)',
      tagColor: 'var(--info-blue)',
      content: '你负责的客户 赵胖胖文化有限公司 已被移入公海，并踢了你一脚和扇了一巴掌，请知悉！',
    },
    {
      id: 11,
      name: '陈十三',
      time: '2025-09-09 12:12:00',
      tag: '重要客户',
      tagBgColor: 'var(--info-5)',
      tagColor: 'var(--info-blue)',
      content: '你负责的客户 赵胖胖文化有限公司 已被移入公海，并踢了你一脚和扇了一巴掌，请知悉！',
    },
    {
      id: 12,
      name: '张十四',
      time: '2025-09-09 12:12:00',
      tag: '重要客户',
      tagBgColor: 'var(--info-5)',
      tagColor: 'var(--info-blue)',
      content: '你负责的客户 赵胖胖文化有限公司 已被移入公海，并踢了你一脚和扇了一巴掌，请知悉！',
    },
  ]);
  const onLoad = () => {
    loading.value = true;
    setTimeout(() => {
      finished.value = true;
      refreshing.value = false;
      loading.value = false;
      error.value = false;
    }, 2000);
  };
</script>

<style lang="less" scoped></style>
