<template>
  <div class="ai-chat-composer relative flex items-start gap-[12px] bg-[var(--text-n10)] p-[16px]">
    <div class="flex min-w-0 flex-1 flex-col gap-[6px]" :class="{ 'mb-[28px]': props.showFooter }">
      <!-- 附件 -->
      <div v-if="props.showAttachments && attachments?.length" class="flex flex-wrap gap-[6px]">
        <div
          v-for="attachment in attachments"
          :key="attachment.id"
          class="inline-flex h-[26px] max-w-[220px] items-center gap-[8px] rounded-[4px] border border-[var(--text-n8)] bg-[var(--text-n9)] px-[8px] text-[12px] text-[var(--text-n1)]"
        >
          <span class="min-w-0 overflow-hidden truncate">{{ attachment.name }}</span>
          <span v-if="attachment.size" class="flex-none text-[var(--text-n4)]">
            {{ formatFileSize(attachment.size) }}
          </span>
          <n-button text @click="removeAttachment(attachment.id)"> × </n-button>
        </div>
      </div>

      <div
        ref="editorRef"
        class="ai-chat-composer__input min-w-0 flex-1"
        contenteditable="true"
        :data-placeholder="props.placeholder || t('aiChat.inputPlaceholder')"
        @input="syncEditorValue"
        @keydown="handleKeydown"
      ></div>
    </div>

    <div
      v-if="props.showFooter"
      class="absolute bottom-[16px] left-[16px] right-[16px] flex min-h-[22px] items-center justify-between"
    >
      <div class="flex items-center">
        <n-upload
          v-model:file-list="uploadFileList"
          :custom-request="handleUploadRequest"
          :max="maxFiles"
          multiple
          :show-file-list="false"
          class="crm-file-input-upload w-fit"
          @before-upload="handleBeforeUpload"
        >
          <CrmIcon type="iconicon_link1" :size="16" />
        </n-upload>
        <n-divider vertical class="!mx-[12px]" />
        <n-dropdown
          v-model:show="mcpDropdownShow"
          trigger="click"
          placement="top-start"
          :options="mcpDropdownOptions"
          @select="handleMcpSelect"
        >
          <n-button class="ai-chat-mcp-button" :class="{ 'ai-chat-mcp-button--active': mcpDropdownShow }" text>
            <CrmIcon type="iconicon_mcp" :size="16" />
            <span>MCP</span>
            <CrmIcon
              :type="mcpDropdownShow ? 'iconicon_chevron_up' : 'iconicon_chevron_down'"
              :size="16"
              color="var(--text-n4)"
            />
          </n-button>
        </n-dropdown>
      </div>

      <n-button v-if="canStop" circle size="small" type="primary" @click="runtime.stop()">
        <template #icon>
          <span class="block h-[8px] w-[8px] rounded-[2px] bg-[var(--text-n10)]" />
        </template>
      </n-button>
      <n-button
        v-else
        circle
        size="small"
        type="primary"
        :loading="isLoading"
        :disabled="!canSubmit"
        @click="handleSubmit"
      >
        <template #icon>
          <CrmIcon type="iconicon_send" :size="14" color="var(--text-n10)" />
        </template>
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, h, nextTick, onMounted, ref, watch } from 'vue';
  import { NButton, NDivider, NDropdown, NUpload } from 'naive-ui';

  import type { AiChatAttachment, AiChatMcp, AiComposerSubmitPayload, AiFileKind } from '@lib/shared/ai-chat';
  import { useAiChatRuntime } from '@lib/shared/ai-chat';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { formatFileSize } from '@lib/shared/method';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import type { DropdownOption, UploadCustomRequestOptions, UploadFileInfo } from 'naive-ui';

  const props = withDefaults(
    defineProps<{
      placeholder?: string;
      uploadFile?: (file: File) => AiChatAttachment | Promise<AiChatAttachment>;
      mcpOptions?: AiChatMcp[];
      submitMode?: 'runtime' | 'emit';
      initialContent?: string;
      initialMcps?: AiChatMcp[];
      showAttachments?: boolean;
      showFooter?: boolean;
      syncRuntime?: boolean;
    }>(),
    {
      placeholder: '',
      mcpOptions: () => [],
      submitMode: 'runtime',
      initialContent: '',
      initialMcps: () => [],
      showAttachments: true,
      showFooter: true,
      syncRuntime: true,
    }
  );

  const emit = defineEmits<{
    (e: 'submit', payload: AiComposerSubmitPayload): void;
    (e: 'change', payload: AiComposerSubmitPayload): void;
    (e: 'importMcp'): void;
  }>();

  const { t } = useI18n();
  const runtime = useAiChatRuntime();

  const editorRef = ref<HTMLElement | null>(null);
  const inputValue = ref(props.initialContent || runtime.state.input.value);

  const uploadFileList = ref<UploadFileInfo[]>([]);
  const attachments = computed(() => runtime.state.attachments.value);
  const submitAttachments = computed(() => (props.showAttachments ? attachments.value : []));
  const isLoading = computed(() => runtime.state.loading.value);
  const canStop = computed(() => runtime.state.canStop.value);
  const canSubmit = computed(() => inputValue.value.trim().length > 0 || submitAttachments.value.length > 0);
  const maxFiles = 10;

  const mcpDropdownShow = ref(false);

  function focusInput(): void {
    nextTick(() => {
      editorRef.value?.focus();
    });
  }

  // 找当前要插入的位置
  function getEditorRange(): Range {
    const editor = editorRef.value;
    const selection = window.getSelection();

    // contenteditable 没有 selectionStart/selectionEnd，只能通过 Selection/Range 找当前插入点。
    if (selection?.rangeCount && editor?.contains(selection.anchorNode)) {
      return selection.getRangeAt(0);
    }

    // 下拉选择 MCP 时焦点可能已经离开输入框，兜底插入到当前内容末尾。
    const range = document.createRange();
    if (editor) {
      range.selectNodeContents(editor);
      range.collapse(false);
    }
    return range;
  }

  // 插入后把光标放到正确位置
  function setCaretAfter(node: Node): void {
    // DOM 插入后浏览器不会自动把光标放到期望位置，需要手动重建选区
    const range = document.createRange();
    const selection = window.getSelection();

    range.setStartAfter(node);
    range.collapse(true);
    selection?.removeAllRanges();
    selection?.addRange(range);
  }

  function createMcpNode(mcp: AiChatMcp): HTMLElement {
    const node = document.createElement('span');
    const icon = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    const use = document.createElementNS('http://www.w3.org/2000/svg', 'use');
    const text = document.createElement('span');

    // MCP 在输入框里是一个不可编辑的整体节点，避免用户只删掉名称或图标的一部分。
    node.className = 'ai-chat-mcp-token';
    node.contentEditable = 'false';
    node.dataset.mcpId = mcp.id;
    node.dataset.mcpName = mcp.name;

    icon.setAttribute('width', '16');
    icon.setAttribute('height', '16');
    icon.setAttribute('aria-hidden', 'true');
    use.setAttributeNS('http://www.w3.org/1999/xlink', 'xlink:href', '#iconicon_mcp');
    icon.appendChild(use);

    text.textContent = mcp.name;
    node.append(icon, text);

    return node;
  }

  function isMcpNode(node: Node | null): node is HTMLElement {
    return node instanceof HTMLElement && node.classList.contains('ai-chat-mcp-token');
  }

  function getEditorText(): string {
    const editor = editorRef.value;
    if (!editor) {
      return '';
    }

    // 提交给后端的 content 仍是纯文本；MCP token 用名称参与文本拼接。
    return Array.from(editor.childNodes)
      .map((node) => {
        if (isMcpNode(node)) {
          return node.dataset.mcpName ?? '';
        }
        return node.textContent ?? '';
      })
      .join('');
  }

  function getEditorMcps(): AiChatMcp[] {
    // MCP 列表只以 token 节点为准，避免普通文本里刚好包含同名 MCP 时被误判为已选。
    const ids = new Set(
      Array.from(editorRef.value?.querySelectorAll<HTMLElement>('.ai-chat-mcp-token') ?? [])
        .map((node) => node.dataset.mcpId)
        .filter(Boolean)
    );

    return props.mcpOptions.filter((mcp) => ids.has(mcp.id));
  }

  function getSubmitPayload(): AiComposerSubmitPayload {
    return {
      content: inputValue.value,
      attachments: [...submitAttachments.value],
      options: {
        mcps: getEditorMcps(),
      },
    };
  }

  function syncEditorValue(): void {
    // contenteditable 负责展示富文本，runtime 仍只接收文本内容和选中的 MCP 列表。
    inputValue.value = getEditorText();
    if (props.syncRuntime) {
      runtime.setSelectedMcps(getEditorMcps());
    }
    emit('change', getSubmitPayload());
  }

  function isEditorEmpty(): boolean {
    const editor = editorRef.value;
    if (!editor) {
      return true;
    }

    // 只看 innerText 不够，空输入聚焦后可能只有 br；有 MCP token 时也不能判空。
    return !getEditorText().trim() && !editor.querySelector('.ai-chat-mcp-token');
  }

  function getRangeSideText(range: Range, direction: 'before' | 'after'): string {
    const editor = editorRef.value;
    if (!editor) {
      return '';
    }

    const sideRange = range.cloneRange();
    // 插入 MCP 前后各取一段文本，用来判断是否需要自动补空格，避免 token 和文字粘连。
    if (direction === 'before') {
      sideRange.selectNodeContents(editor);
      sideRange.setEnd(range.startContainer, range.startOffset);
    } else {
      sideRange.selectNodeContents(editor);
      sideRange.setStart(range.endContainer, range.endOffset);
    }

    return sideRange.toString();
  }

  function insertMcp(mcp: AiChatMcp): void {
    if (isEditorEmpty()) {
      // 空 contenteditable 聚焦后浏览器可能插入 br，直接替换子节点可避免 MCP 跑到第二行。
      const tokenNode = createMcpNode(mcp);
      const suffixNode = document.createTextNode(' ');

      editorRef.value?.replaceChildren(tokenNode, suffixNode);
      setCaretAfter(suffixNode);
      syncEditorValue();
      focusInput();
      return;
    }

    const range = getEditorRange();
    const fragment = document.createDocumentFragment();
    const tokenNode = createMcpNode(mcp);
    const beforeText = getRangeSideText(range, 'before');
    const afterText = getRangeSideText(range, 'after');
    const prefixNode = beforeText && !/\s$/.test(beforeText) ? document.createTextNode(' ') : null;
    const suffixNode = afterText && /^\s/.test(afterText) ? null : document.createTextNode(' ');

    // 使用 fragment 一次性插入 “前置空格 + MCP token + 后置空格”，减少 DOM 光标错位。
    range.deleteContents();
    if (prefixNode) {
      fragment.appendChild(prefixNode);
    }
    fragment.appendChild(tokenNode);
    if (suffixNode) {
      fragment.appendChild(suffixNode);
    }
    range.insertNode(fragment);
    setCaretAfter(suffixNode ?? tokenNode);
    syncEditorValue();
    focusInput();
  }

  function handleImportMcp(): void {
    mcpDropdownShow.value = false;
    emit('importMcp');
  }

  function handleMcpSelect(key: string | number): void {
    const mcp = props.mcpOptions.find((item) => item.id === String(key));

    if (mcp) {
      insertMcp(mcp);
    }
  }

  const mcpDropdownOptions = computed<DropdownOption[]>(() => [
    {
      key: 'mcp-import',
      type: 'render',
      render: () =>
        h('div', { class: 'ai-chat-mcp-dropdown-header' }, [
          h(
            NButton,
            {
              text: true,
              type: 'primary',
              onClick: handleImportMcp,
            },
            {
              icon: () => h(CrmIcon, { type: 'iconicon_add', size: 16 }),
              default: () => t('aiChat.importMcp'),
            }
          ),
        ]),
    },
    ...(props.mcpOptions.length
      ? [
          {
            type: 'divider',
            key: 'mcp-divider',
          },
        ]
      : []),
    ...props.mcpOptions.map((mcp) => ({
      label: mcp.name,
      key: mcp.id,
    })),
  ]);

  function getMatchedMcp(text: string, index: number, mcps: AiChatMcp[]): AiChatMcp | undefined {
    return mcps.find((mcp) => text.startsWith(mcp.name, index));
  }

  function renderEditorValue(value: string, mcps: AiChatMcp[] = []): void {
    const editor = editorRef.value;
    if (!editor) {
      return;
    }

    const targetEditor = editor;
    const sortedMcps = [...mcps].sort((a, b) => b.name.length - a.name.length);
    targetEditor.innerHTML = '';
    let currentText = '';
    let index = 0;

    function appendText(): void {
      if (!currentText) {
        return;
      }

      targetEditor.appendChild(document.createTextNode(currentText));
      currentText = '';
    }

    while (index < value.length) {
      const mcp = getMatchedMcp(value, index, sortedMcps);

      if (mcp) {
        appendText();
        targetEditor.appendChild(createMcpNode(mcp));
        index += mcp.name.length;
      } else {
        currentText += value[index];
        index += 1;
      }
    }

    appendText();
  }

  watch(inputValue, (value) => {
    if (props.syncRuntime) {
      runtime.setInput(value);
    }
  });
  watch(runtime.state.input, (value) => {
    if (!props.syncRuntime) {
      return;
    }

    if (value !== inputValue.value) {
      inputValue.value = value;
      renderEditorValue(value);
      runtime.setSelectedMcps(getEditorMcps());
    }
  });

  watch(
    () => props.mcpOptions,
    () => {
      if (props.syncRuntime) {
        runtime.setSelectedMcps(getEditorMcps());
      }
    }
  );

  function removeAttachment(attachmentId: string): void {
    const targetAttachment = attachments.value.find((attachment) => attachment.id === attachmentId);
    const uploadFileId = targetAttachment?.metadata?.uploadFileId;

    uploadFileList.value = uploadFileList.value.filter((file) => file.id !== attachmentId && file.id !== uploadFileId);
    runtime.removeAttachment(attachmentId);
  }

  function getFileKind(file: File): AiFileKind {
    if (file.type.startsWith('image/')) {
      return 'image';
    }

    if (file.type.startsWith('audio/')) {
      return 'audio';
    }

    if (file.type.startsWith('video/')) {
      return 'video';
    }

    return 'file';
  }

  function createLocalAttachment(file: File, uploadFileId: string): AiChatAttachment {
    return {
      id: uploadFileId,
      name: file.name,
      mimeType: file.type,
      size: file.size,
      kind: getFileKind(file),
      metadata: {
        file,
        uploadFileId,
      },
    };
  }

  function handleBeforeUpload({ file }: { file: UploadFileInfo }): boolean {
    if (attachments.value.length >= maxFiles) {
      return false;
    }

    return Boolean(file.file);
  }

  async function handleUploadRequest({ file, onFinish, onError }: UploadCustomRequestOptions): Promise<void> {
    if (!file.file) {
      onError();
      return;
    }

    try {
      const uploadedAttachment = props.uploadFile
        ? await props.uploadFile(file.file)
        : createLocalAttachment(file.file, file.id);
      const attachment: AiChatAttachment = {
        ...uploadedAttachment,
        metadata: {
          ...uploadedAttachment.metadata,
          uploadFileId: file.id,
        },
      };

      runtime.setAttachments([...attachments.value, attachment]);
      onFinish();
    } catch {
      onError();
    }
  }

  /**
   * Composer 只负责输入交互。
   * 真正的消息追加、请求发送、流式更新都交给 Runtime。
   */
  async function handleSubmit(): Promise<void> {
    if (!canSubmit.value) {
      return;
    }

    const payload = getSubmitPayload();

    if (props.submitMode === 'emit') {
      emit('submit', payload);
    } else {
      await runtime.submit(payload);
    }
  }

  function removeAdjacentMcp(event: KeyboardEvent): boolean {
    // MCP token 是 contenteditable=false，手动接管前后删除，保证按一次删除整个 token。
    if (event.key !== 'Backspace' && event.key !== 'Delete') {
      return false;
    }

    const selection = window.getSelection();
    if (!selection?.rangeCount || !selection.isCollapsed || !editorRef.value?.contains(selection.anchorNode)) {
      return false;
    }

    const range = selection.getRangeAt(0);
    const container = range.startContainer;
    const offset = range.startOffset;
    let targetNode: Node | null;

    if (event.key === 'Backspace') {
      targetNode = offset === 0 ? container.previousSibling : container.childNodes[offset - 1];
    } else {
      targetNode = container.childNodes[offset] ?? container.nextSibling;
    }

    if (!isMcpNode(targetNode)) {
      return false;
    }

    event.preventDefault();
    const nextCaretNode = document.createTextNode('');
    targetNode.replaceWith(nextCaretNode);
    setCaretAfter(nextCaretNode);
    syncEditorValue();
    return true;
  }

  async function handleKeydown(event: KeyboardEvent): Promise<void> {
    if (removeAdjacentMcp(event)) {
      return;
    }

    if (event.key !== 'Enter' || event.shiftKey || event.metaKey || event.ctrlKey) {
      return;
    }

    event.preventDefault();
    await handleSubmit();
  }

  onMounted(() => {
    renderEditorValue(inputValue.value, props.initialMcps);
    emit('change', getSubmitPayload());
  });

  defineExpose({
    getSubmitPayload,
  });
</script>

<style scoped lang="scss">
  .ai-chat-composer {
    box-shadow: 0 4px 15px 2px #6467671a;
  }
  .ai-chat-composer__input {
    overflow-y: auto;
    min-width: 0;
    max-height: 132px;
    white-space: pre-wrap;
    color: var(--text-n1);
    outline: none;
    line-height: 26px;
    word-break: break-word;
    &:empty::before {
      color: var(--text-n4);
      content: attr(data-placeholder);
      pointer-events: none;
    }
  }
  :global(.ai-chat-mcp-token) {
    display: inline-flex;
    align-items: center;
    padding: 0 6px;
    height: 26px;
    border-radius: 4px;
    color: var(--primary-8);
    background: var(--primary-7);
    gap: 4px;
    line-height: 26px;
    vertical-align: top;
    user-select: all;
  }
  :global(.ai-chat-mcp-token svg) {
    flex: none;
    fill: currentcolor;
  }
  .crm-file-input-upload {
    :deep(.n-upload-trigger) {
      @apply flex cursor-pointer items-center;
    }
  }
  .ai-chat-mcp-button {
    padding: 2px 4px;
    height: 26px !important;
    border-radius: 4px;
    cursor: pointer;
    &:hover,
    &--active {
      background: var(--text-n9);
    }
    :deep(.n-button__content) {
      gap: 4px;
    }
  }
</style>
