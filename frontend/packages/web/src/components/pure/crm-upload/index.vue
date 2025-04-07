<template>
  <n-upload
    v-bind="{ ...props }"
    v-model:file-list="innerFileList"
    :accept="
      [UploadAcceptEnum.none, UploadAcceptEnum.unknown].includes(UploadAcceptEnum[props.accept])
        ? '*'
        : UploadAcceptEnum[props.accept]
    "
    :multiple="props.multiple"
    :class="getAllScreenClass"
    class="crm-upload-wrapper"
    @change="({ file, fileList }) => handleChange(file as CrmFileItem, fileList as CrmFileItem[])"
    @before-upload="({ file, fileList })=>beforeUpload(file as CrmFileItem, fileList as CrmFileItem[])"
  >
    <slot>
      <n-upload-dragger class="crm-upload-dragger-wrapper">
        <div class="crm-upload-area-wrapper">
          <div
            class="crm-upload-area"
            :class="[
              props.isAllScreen && showDropArea
                ? 'crm-upload-area-dotted-border h-[100vh]'
                : 'crm-upload-area-thin-border h-[154px]',
            ]"
          >
            <div class="crm-upload-icon-box">
              <CrmIcon
                v-if="props.accept !== UploadAcceptEnum.none"
                :type="fileIconType"
                :size="32"
                class="crm-upload-icon text-[var(--text-n4)]"
              />
              <div v-else class="crm-upload-icon crm-upload-icon--default"></div>
            </div>
            <template v-if="innerFileList.length === 0 || props.multiple">
              <div class="crm-upload-main-text mb-[4px]">
                {{ t(props.mainText || 'crm.upload.importModalDragText') }}
              </div>
              <div v-if="showSubText" class="crm-upload-sub-text">
                <slot name="subText">
                  {{
                    t(props.subText || 'crm.upload.importModalFileTip', {
                      type: UploadAcceptEnum[props.accept],
                      size: props.maxSize || 50,
                    })
                  }}
                </slot>
              </div>
            </template>
            <template v-else>
              <div class="crm-upload-main-text w-full">
                <n-tooltip :content="innerFileList[0]?.name">
                  <template #trigger>
                    <span class="one-line-text w-[80%] text-center"> {{ innerFileList[0]?.name }}</span>
                  </template>
                  <span class="one-line-text w-[80%] text-center"> {{ innerFileList[0]?.name }}</span>
                </n-tooltip>
              </div>
              <div class="crm-upload-sub-text">{{ formatFileSize(innerFileList[0]?.file?.size || 0) }}</div>
            </template>
          </div>
        </div>
      </n-upload-dragger>
    </slot>
  </n-upload>
</template>

<script setup lang="ts">
  import { NTooltip, NUpload, NUploadDragger, UploadFileInfo, useMessage } from 'naive-ui';

  import { UploadAcceptEnum, UploadStatus } from '@lib/shared/enums/uploadEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { formatFileSize } from '@lib/shared/method';

  import { FileIconMap, getFileEnum, getFileIcon } from '@/components/pure/crm-upload/iconMap';

  import type { CrmFileItem, UploadType } from './types';

  const Message = useMessage();

  const { t } = useI18n();

  type UploadProps = Partial<{
    fileList: CrmFileItem[]; // 文件列表
    mainText: string; // 主要标题文案
    subText: string; // 次要文案
    showSubText: boolean; // 是否显示次要文案
    multiple: boolean; // 上传多个
    disabled: boolean; // 禁用
    showFileList: boolean; // 是否显示文件列表
    maxSize: number; // 文件大小限制，单位 MB
    sizeUnit: 'MB' | 'KB'; // 文件大小单位
    isLimit: boolean; // 是否限制文件大小
    directoryDnd: boolean; // 是否允许拖拽文件
    fileTypeTip?: string; // 上传文件类型错误提示
    limit: number; // 限制上传文件数量
    allowRepeat?: boolean; // 自定义上传文件框，是否允许重复文件名替换
    isAllScreen?: boolean; // 是否是全屏显示拖拽上传
  }> & {
    accept: UploadType;
  };

  const props = withDefaults(defineProps<UploadProps>(), {
    showSubText: true,
    isLimit: true,
    isAllScreen: false,
    allowRepeat: false,
    sizeUnit: 'MB',
    showFileList: false,
  });

  const emit = defineEmits<{
    (e: 'change', fileList: Array<CrmFileItem>, file: CrmFileItem): void;
  }>();

  const innerFileList = defineModel<CrmFileItem[]>('fileList', {
    default: () => [],
  });

  const showDropArea = ref(!props.isAllScreen);

  // 文件icon类型
  const fileIconType = computed(() => {
    if (innerFileList.value.length > 0 && !props.multiple) {
      return getFileIcon(innerFileList.value[0], UploadStatus.done);
    }
    return FileIconMap[props.accept][UploadStatus.init];
  });

  const getAllScreenClass = computed(() => {
    return props.isAllScreen && showDropArea.value
      ? ['!fixed', 'right-0', 'left-0', 'bottom-0', 'top-0', 'm-auto', 'z-[999]', 'opacity-90']
      : [];
  });

  function handleChange(file: CrmFileItem, fileList: Array<CrmFileItem>) {
    file.local = true;
    emit('change', fileList, file);
  }

  // 判断文件是否重复
  function isFileRepeat(file: CrmFileItem, fileList: CrmFileItem[], allowRepeat: boolean): boolean {
    if (!allowRepeat) {
      const isRepeat = fileList.some((item: CrmFileItem) => item.name === file.name && item.local);
      return isRepeat;
    }
    return false;
  }

  // 判断文件大小
  function isFileSizeValid(file: UploadFileInfo, maxSize: number, sizeUnit: string, isLimit: boolean): boolean {
    if (isLimit && file.file?.size) {
      const maxSizeInBytes = sizeUnit === 'MB' ? maxSize * 1024 * 1024 : maxSize * 1024;
      return file.file.size <= maxSizeInBytes;
    }
    return true;
  }

  // 判断文件类型
  function isFileTypeValid(file: UploadFileInfo, accept: string): boolean {
    const fileFormatMatch = file.name.match(/\.([a-zA-Z0-9]+)$/);
    const fileFormatType = fileFormatMatch ? fileFormatMatch[1] : 'none';
    return accept === getFileEnum(fileFormatType) || accept === 'none';
  }

  async function beforeUpload(file: CrmFileItem, _fileList: Array<CrmFileItem>) {
    const maxSize = props.maxSize || 50;

    //  附件上传校验名称重复
    if (innerFileList.value.length > 0 && isFileRepeat(file, innerFileList.value, props.allowRepeat)) {
      Message.warning(t('crm.upload.repeatFileTip'));
      return false; // 文件重复，返回 false 以阻止上传
    }

    //  校验文件大小
    if (!isFileSizeValid(file, maxSize, props.sizeUnit, props.isLimit)) {
      Message.warning(t('crm.upload.overSize', { size: maxSize, unit: props.sizeUnit }));
      return false; // 文件大小不符合要求，返回 false
    }

    //  单文件上传时清空之前文件
    if (!props.multiple) {
      innerFileList.value = [];
    }

    //  校验文件类型
    if (!isFileTypeValid(file, props.accept)) {
      Message.error(props.fileTypeTip || t('crm.upload.fileTypeValidate', { type: props.accept }));
      return false;
    }

    return true;
  }

  watch(
    () => props.isAllScreen,
    (val) => {
      showDropArea.value = !val;
    },
    { immediate: true }
  );

  const resizeObserver = ref();
  const targetElement = ref();
  const wrapperWidth = ref<number>();

  function init() {
    const ele = document.querySelector('body');
    targetElement.value = document.querySelector('.n-config-provider');
    resizeObserver.value = new ResizeObserver((entries) => {
      entries.forEach((item) => {
        wrapperWidth.value = item.contentRect.width;
      });
    });

    resizeObserver.value.observe(targetElement.value);
    wrapperWidth.value = targetElement.value.getBoundingClientRect().width;
    if (ele) {
      ele.addEventListener('dragenter', (event) => {
        const { dataTransfer } = event;
        if (dataTransfer && dataTransfer.types.includes('Files')) {
          // 处理拖拽的文件
          showDropArea.value = true;
        }
      });
      // 拖后放
      ele.addEventListener('dragleave', (e: any) => {
        if (
          e.target.nodeName === 'HTML' ||
          e.target === e.explicitOriginalTarget ||
          (!e.fromElement &&
            (e.clientX <= 0 || e.clientY <= 0 || e.clientX >= window.innerWidth || e.clientY >= window.innerHeight))
        ) {
          showDropArea.value = false;
        }
      });
      // 拖离
      ele.addEventListener('drop', (e) => {
        showDropArea.value = false;
        e.preventDefault();
      });
    }
  }

  // 禁用默认拖拽事件
  function disableDefaultEvents() {
    const doc = document.querySelector('body');
    if (doc) {
      doc.addEventListener('dragleave', (e) => e.preventDefault()); // 拖离
      doc.addEventListener('drop', (e) => e.preventDefault()); // 拖后放
      doc.addEventListener('dragenter', (e) => e.preventDefault()); // 拖进
      doc.addEventListener('dragover', (e) => e.preventDefault()); // 结束拖拽
    }
  }

  onMounted(() => {
    if (props.isAllScreen) {
      disableDefaultEvents();
      init();
    }
  });

  onBeforeUnmount(() => {
    if (props.isAllScreen) resizeObserver.value?.disconnect();
  });
</script>

<style lang="less" scoped>
  .crm-upload-area {
    border-color: var(--text-n7);
    border-radius: var(--border-radius-small);
    background-color: var(--text-n9);

    @apply flex flex-col items-center justify-center;
    .crm-upload-icon-box {
      @apply rounded-full;

      margin-bottom: 16px;
      padding: 8px;
      width: 48px;
      height: 48px;
      background-color: var(--text-n10);
      .crm-upload-icon {
        @apply h-full w-full bg-cover bg-no-repeat;
        &--default {
          background-image: url('@/assets/svg/uploadfile.svg');
        }
      }
    }
    .crm-upload-main-text {
      @apply flex items-center justify-center gap-1;

      color: var(--text-n1);
    }
    .crm-upload-sub-text {
      margin-bottom: 6px;
      font-size: 12px;
      color: var(--text-n4);
      line-height: 16px;
    }
  }
  .crm-upload-area-thin-border {
    border: 1px dashed var(--text-n7);
  }
  .crm-upload-area-dotted-border {
    border: 4px dashed var(--primary-8);
  }
</style>

<style lang="less">
  .crm-upload-wrapper {
    border: none !important;
    .n-upload-trigger {
      width: 100% !important;
      .crm-upload-dragger-wrapper {
        padding: 0;
        border: none !important;
      }
    }
  }
</style>
