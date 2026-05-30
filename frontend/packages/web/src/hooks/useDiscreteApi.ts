import { createDiscreteApi } from 'naive-ui';

import { useI18n } from '@lib/shared/hooks/useI18n';

import type {
  ConfigProviderProps,
  DialogApi,
  DialogOptions,
  LoadingBarApi,
  MessageApi,
  MessageProviderProps,
  NotificationApi,
  NotificationOptions,
} from 'naive-ui';

interface MessageOpt {
  loadingBar: LoadingBarApi;
  message: MessageApi; // 信息message
  notification: NotificationApi; // 通知
  dialog: DialogApi;
  confirm: DialogApi;
  destroyAllDialog: () => void;
  destroyAllNotification: () => void;
}

// 用于检测代理方法是否在可代理的类型列表中
const typeMap = ['create', 'error', 'info', 'success', 'warning'];

// 用于存储状态的对象
let state = {} as MessageOpt;
// 标志位，表示是否已经初始化
let isInitialization = false;

// 创建代理函数，用于包装 API 调用，合并传入的配置和参数
function createApi<T extends Record<string | symbol, any>, O>(coreApi: T, opt: O) {
  return new Proxy(coreApi, {
    get(target, propKey) {
      if (typeMap.includes(propKey as string)) {
        return new Proxy(target[propKey], {
          apply(funTarget, ctx, args) {
            return Reflect.apply(funTarget, ctx, [{ ...opt, ...args[0] }]);
          },
        });
      }
      // 否则直接返回目标方法
      return Reflect.get(target, propKey);
    },
  });
}

// 创建 Dialog ，合并额外配置
function createDialog(dialog: DialogApi, opt: DialogOptions) {
  // 给对话框的按钮配置提供默认值类型和尺寸
  const defaultButtonProps = {
    positiveButtonProps: {
      size: 'medium',
    },
    negativeButtonProps: {
      size: 'medium',
      secondary: true,
      strong: true,
      ghost: false,
    },
  };

  // 合并默认配置和传入的配置
  const mergedOpt = { ...opt, ...defaultButtonProps };

  return createApi(dialog, mergedOpt);
}
// 创建 Notification ，合并额外配置
function createNotification(
  notification: NotificationApi,
  defaultOptions: NotificationOptions & { maxCount?: number } = {} // 限制通知最大弹出数量
): NotificationApi {
  const queue: Array<ReturnType<NotificationApi['create']>> = [];
  const defaultMax = defaultOptions.maxCount ?? 3;

  // 创建代理对象拦截通知方法调用
  return new Proxy(notification, {
    get(target, key: keyof NotificationApi) {
      const originalFn = target[key];

      if (typeof originalFn === 'function' && typeMap.includes(key as string)) {
        return (options: NotificationOptions & { maxCount?: number } = {}) => {
          const maxCount = options.maxCount ?? defaultMax;

          // 超出最大数量，移除最早的通知
          if (queue.length >= maxCount) {
            queue.shift()?.destroy();
          }

          // 移除 maxCount，防止多余参数传给组件
          const { maxCount: _, ...rest } = options;

          // 创建通知，合并默认参数和当前参数
          const instance = originalFn({ ...defaultOptions, ...rest });
          if (instance) {
            queue.push(instance);
          }
          return instance;
        };
      }

      // 非通知方法直接返回
      return originalFn;
    },
  });
}

/**
 * @description: 该函数用于创建和管理不同的 API（如消息、通知、对话框等）消息n-XXX-provider容器，并返回相应的实例
 */
export default function useDiscreteApi(props?: {
  configProviderProps?: ConfigProviderProps;
  messageProviderProps?: MessageProviderProps;
}) {
  if (!isInitialization) {
    // 使用 createDiscreteApi 获取 Naive UI 提供的四个 API 实例
    const { message, notification, dialog, loadingBar } = createDiscreteApi(
      ['message', 'dialog', 'notification', 'loadingBar'],
      props
    );

    // 使用 i18n 钩子获取国际化文本
    const { t } = useI18n();

    try {
      // 初始化 state，存储每个 API 的实例，并加入额外的配置信息
      state = {
        loadingBar, // 加载条 API
        message, // 消息 API
        notification: createNotification(notification, { duration: 3000 }), // 通知 API，设置通知持续时间
        // TODO: 取消按钮禁用还未补充
        dialog: createDialog(dialog, {
          positiveText: t('common.confirm'),
          maskClosable: false,
        }), // 对话框 API，设置 OK 文本
        confirm: createDialog(dialog, {
          // 确认对话框 API，设置 OK 和 Cancel 文本
          positiveText: t('common.confirm'),
          negativeText: t('common.cancel'),
        }),
        destroyAllDialog: dialog.destroyAll, // 销毁所有对话框的方法
        destroyAllNotification: notification.destroyAll, // 销毁所有通知的方法
      };

      // 初始化完成
      isInitialization = true;
    } catch (error) {
      // 捕获并输出初始化错误
      // eslint-disable-next-line no-console
      console.warn(`useDiscreteApi init failed: ${error}`);
    }
  }

  // 返回初始化后的状态对象，包含所有的 API 方法和配置
  return state;
}
