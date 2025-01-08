<template>
  <div class="p-[24px]">
    <CrmCard title="按钮类型demo" sub-title="subTitle" no-content-padding>
      <div class="flex flex-col gap-4 p-4">
        <!-- TODO 按钮类型demo 暂时提供参考 you can delete it  ^_^  -->
        <div class="flex gap-[16px]">
          <NButton type="primary">主要按钮</NButton>
          <NButton type="tertiary">次要按钮</NButton>
          <NButton type="default" class="outline--secondary">次要外框按钮</NButton>
          <NButton strong secondary>次要背景按钮</NButton>
          <NButton type="success"> 成功背景按钮 </NButton>
          <NButton type="warning"> 警告背景按钮 </NButton>
          <NButton type="error"> 错误背景按钮 </NButton>
        </div>
        <div class="mt-[24px] flex gap-[16px]">
          <n-button quaternary class="text-btn-secondary"> 文本次要按钮 </n-button>
          <n-button quaternary type="primary" class="text-btn-primary"> 文本主题按钮 </n-button>
          <n-button quaternary type="info" class="text-btn-info"> 文本info按钮 </n-button>
          <n-button quaternary type="success" class="text-btn-success"> 文本success按钮 </n-button>
          <n-button quaternary type="warning" class="text-btn-warning"> 文本warning按钮 </n-button>
          <n-button quaternary type="error" class="text-btn-error"> 文本Error按钮 </n-button>
        </div>
        <div class="mt-[24px] flex gap-[16px]">
          <n-button type="primary" ghost> Primary </n-button>
          <n-button type="info" ghost class="n-btn-outline-info" @click="info"> Info </n-button>
          <n-button type="success" ghost class="n-btn-outline-success" @click="success"> Success </n-button>
          <n-button type="warning" ghost class="n-btn-outline-warning" @click="warning"> Warning </n-button>
          <n-button type="error" ghost class="n-btn-outline-error" @click="error"> Error </n-button>
        </div>
        <div class="mt-[24px] flex gap-[16px]">
          <n-button type="primary" ghost @click="loading"> Primary </n-button>
          <n-button type="info" ghost @click="notify"> Info </n-button>
          <n-button type="success" ghost @click="dialogHandler"> Success </n-button>
          <n-button type="warning" ghost> Warning </n-button>
          <n-button type="error" ghost> Error </n-button>
        </div>
      </div>
    </CrmCard>

    <TableDemo class="my-[16px]" />
  </div>
</template>

<script setup lang="ts">
  import { NButton } from 'naive-ui';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import TableDemo from './TableDemo.vue';

  import useDiscreteApi from '@/hooks/useDiscreteApi';
  // 暂时提供参考 you can delete it  ^_^
  const { message, notification, dialog } = useDiscreteApi();

  function info() {
    message.info("I don't know why nobody told you how to unfold your love", {
      keepAliveOnHover: true,
    });
  }
  function error() {
    message.error('Once upon a time you dressed so fine', {
      duration: 100000,
    });
  }
  function warning() {
    message.warning('How many roads must a man walk down');
  }
  function success() {
    message.success("'Cause you walked hand in hand With another man in my place");
  }
  function loading() {
    message.loading('If I were you, I will realize that I love you more than any other guy');
  }

  function notify() {
    notification.info({
      title: 'hahahahha',
      content: `（一般是）从浏览器顶部降下来的神谕。`,
      duration: 103000,
    });
  }

  function dialogHandler() {
    const d = dialog.success({
      title: '想说点啥',
      content: '但是好像也没有说的,怎么样都是要离开的',
      positiveText: '离开',
      negativeText: '算了',
      onPositiveClick: async () => {
        d.loading = true;
        try {
          await new Promise((resolve) => {
            setTimeout(resolve, 2000);
          });
          message.success('删除成功');
          d.loading = false;
        } catch (e) {
          message.error('删除失败，请重试');
        }
      },
      onNegativeClick: () => {
        message.error('不确定');
      },
    });
  }
</script>

<style>
  @media (min-width: 1024px) {
    .about {
      display: flex;
      align-items: center;
      min-height: 100vh;
    }
  }
</style>
