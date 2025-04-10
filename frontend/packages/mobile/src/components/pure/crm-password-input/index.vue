<template>
  <van-field v-model="value" :type="visible ? 'text' : 'password'" v-bind="$attrs">
    <template #right-icon>
      <CrmIcon
        :name="visible ? 'iconicon_browse' : 'iconicon_browse_off'"
        width="20px"
        height="20px"
        color="var(--text-n4)"
        @click="() => (visible = !visible)"
      />
    </template>
  </van-field>
</template>

<script lang="ts" setup>
  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  const value = defineModel<string>('value', {
    required: true,
  });

  const visible = ref(false);
</script>

<style lang="less" scoped></style>
