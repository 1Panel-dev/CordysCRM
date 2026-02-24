import { ref } from 'vue';

/** *
 *
 * @description 用例模拟进度条进度，采用逐渐减速的曲线，返回的 progress 为整数
 * @step 初始衰减速率因子（百分比，表示每次更新增加剩余部分的百分之几），默认2
 */
export default function useProgressBar(step: number = 2) {
  const intervalId = ref<any>(null);
  const progress = ref<number>(0); // 对外暴露的整数进度
  let internalProgress = 0; // 内部浮点数进度，用于精确计算
  const decayRate = ref<number>(step / 100); // 衰减率（小数形式）

  // 更新进度：每次增加剩余部分的 decayRate 倍，并对 internalProgress 取整后更新 progress
  function updateProgress() {
    const remaining = 100 - internalProgress;
    const increment = remaining * decayRate.value;
    internalProgress = Math.min(internalProgress + increment, 100);
    progress.value = Math.floor(internalProgress);
  }

  // 完成进度，立即设置 100% 并清理定时器
  function finish() {
    clearInterval(intervalId.value);
    progress.value = 100;
    internalProgress = 100;
  }

  // 启动进度条
  function start(newStep: number = step) {
    // 清除之前的定时器
    if (intervalId.value) {
      clearInterval(intervalId.value);
    }
    progress.value = 0;
    internalProgress = 0;
    decayRate.value = newStep / 100;
    intervalId.value = setInterval(() => {
      updateProgress();
    }, 100);
  }

  return {
    progress,
    start,
    finish,
  };
}
