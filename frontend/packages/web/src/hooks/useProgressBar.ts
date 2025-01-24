import { ref } from 'vue';

/** *
 *
 * @description 用例模拟进度条进度
 * @step 增加的步长
 */
export default function useProgressBar(step: number = 2) {
  const intervalId = ref<any>(null);
  const progress = ref<number>(0);
  const increment = ref<number>(step);

  // 更新进度
  function updateProgress() {
    progress.value = Math.floor(progress.value + increment.value);
    if (progress.value >= 100) {
      progress.value = 100;
    }
  }

  // 完成进度，清理定时器
  function finish() {
    clearInterval(intervalId.value);
    progress.value = 100;
    updateProgress();
  }

  // 启动进度条
  function start(newStep: number = increment.value) {
    progress.value = 0;
    increment.value = newStep;
    intervalId.value = setInterval(() => {
      if (progress.value >= 100) {
        finish();
      } else {
        updateProgress();
      }
    }, 100);
  }

  return {
    progress,
    start,
    finish,
  };
}
