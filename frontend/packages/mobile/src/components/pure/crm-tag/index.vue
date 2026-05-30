<template>
  <template v-if="Array.isArray(props.tag)">
    <div class="flex flex-wrap gap-[4px]">
      <van-tag
        v-for="item of props.tag"
        :key="item"
        :color="props.bgColor"
        :text-color="props.textColor"
        class="!p-[2px_6px]"
      >
        {{ item }}
      </van-tag>
    </div>
  </template>
  <van-tag
    v-else
    :color="props.bgColor"
    :text-color="props.textColor"
    :size="props.size"
    :round="props.round"
    :plain="props.plain"
    class="flex items-center gap-[2px] !p-[2px_6px]"
  >
    <CrmIcon
      v-if="props.icon"
      :name="props.icon"
      :color="props.iconColor"
      :width="props.iconWidth"
      :height="props.iconHeight"
    />
    <div class="one-line-text">{{ props.tag }}</div>
  </van-tag>
</template>

<script setup lang="ts">
  import { TagSize } from 'vant';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  const props = withDefaults(
    defineProps<{
      tag: (string | number) | (string | number)[];
      bgColor?: string;
      textColor?: string;
      size?: TagSize;
      round?: boolean;
      plain?: boolean;
      icon?: string;
      iconWidth?: string;
      iconHeight?: string;
      iconColor?: string;
    }>(),
    {
      bgColor: 'var(--text-n8)',
      textColor: 'var(--text-n1)',
    }
  );
</script>

<style lang="less" scoped></style>
