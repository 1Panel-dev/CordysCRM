import { ref } from 'vue';

export default function useHighlight(selectorArray: string[]) {
  const keyword = ref('');
  const defaultHighlightClass = `bg-[var(--primary-8)] text-[var(--text-n10)] rounded-sm`;
  // 高亮文本
  function highlightContent() {
    if (!keyword.value) return;

    const regex = new RegExp(`(${keyword.value})`, 'gi'); // 关键字匹配

    selectorArray.forEach((selector) => {
      const elements = document.querySelectorAll(selector);
      elements.forEach((element) => {
        if (element.innerHTML !== element.textContent) return;
        element.innerHTML = element.textContent.replace(regex, `<span class="${defaultHighlightClass}">$1</span>`);
      });
    });
  }

  // 清空高亮
  function resetHighlight() {
    selectorArray.forEach((selector) => {
      const elements = document.querySelectorAll(selector);
      elements.forEach((element) => {
        const originalText = element.innerHTML;
        element.innerHTML = originalText.replace(/<span class="[^"]+">([^<]+)<\/span>/gi, '$1');
      });
    });
  }

  return {
    keyword,
    highlightContent,
    resetHighlight,
  };
}
