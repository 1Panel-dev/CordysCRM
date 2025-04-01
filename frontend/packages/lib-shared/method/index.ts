import JSEncrypt from 'jsencrypt';

import { isObject } from './is';
import { getLocalStorage } from '@lib/shared/method/local-storage';

/**
 * 递归深度合并
 * @param src 源对象
 * @param target 待合并的目标对象
 * @returns 合并后的对象
 */
export const deepMerge = <T = any>(src: any = {}, target: any = {}): T => {
  Object.keys(target).forEach((key) => {
    src[key] = isObject(src[key]) ? deepMerge(src[key], target[key]) : (src[key] = target[key]);
  });
  return src;
};

/**
 * 遍历对象属性并一一添加到 url 地址参数上
 * @param baseUrl 需要添加参数的 url
 * @param obj 参数对象
 * @returns 拼接后的 url
 */
export function setObjToUrlParams(baseUrl: string, obj: any): string {
  let parameters = '';
  Object.keys(obj).forEach((key) => {
    parameters += `${key}=${encodeURIComponent(obj[key])}&`;
  });
  parameters = parameters.replace(/&$/, '');
  return /\?$/.test(baseUrl) ? baseUrl + parameters : baseUrl.replace(/\/?$/, '?') + parameters;
}

/**
 * 加密
 * @param input 输入的字符串
 * @param publicKey 公钥
 * @returns
 */
export function encrypted(input: string) {
  const publicKey = getLocalStorage('publicKey') || '';
  const encrypt = new JSEncrypt({ default_key_size: '1024' });
  encrypt.setPublicKey(publicKey);

  return encrypt.encrypt(input);
}

/**
 * 休眠
 * @param ms 睡眠时长，单位毫秒
 * @returns
 */
export function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(() => resolve(), ms);
  });
}

export function getQueryVariable(variable: string) {
  const urlString = window.location.href;
  const queryIndex = urlString.indexOf('?');
  if (queryIndex !== -1) {
    const query = urlString.substring(queryIndex + 1);

    // 分割查询参数
    const params = query.split('&');
    // 遍历参数，找到 _token 参数的值
    let variableValue;
    params.forEach((param) => {
      const equalIndex = param.indexOf('=');
      const variableName = param.substring(0, equalIndex);
      if (variableName === variable) {
        variableValue = param.substring(equalIndex + 1);
      }
    });
    return variableValue;
  }
}

export function getUrlParameterWidthRegExp(name: string) {
  const url = window.location.href;
  name = name.replace(/[[\]]/g, '\\$&');
  const regex = new RegExp(`[?&]${name}(=([^&#]*)|&|#|$)`);
  const results = regex.exec(url);
  if (!results) return null;
  if (!results[2]) return '';
  return decodeURIComponent(results[2].replace(/\+/g, ' '));
}

/**
 * 建立 SSE 连接
 * @param url 连接地址
 * @param host 连接主机
 * @returns EventSource 实例
 */
export const apiSSE = (url: string, host?: string): EventSource => {
  let protocol = 'http://';

  // 判断是否使用 HTTPS
  if (!host?.startsWith('http') && (window.location.protocol === 'https:' || host?.startsWith('https'))) {
    protocol = 'https://';
  }

  // 解析 URL，自动适配 host
  const uri = protocol + (host?.split('://')[1] || window.location.host) + url;

  return new EventSource(uri);
};

/**
 * 获取 SSE 连接
 * @param sseUrl，自定义 SSE 地址
 * @param host 自定义主机
 * @returns EventSource 实例
 */
export function getSSE(sseUrl: string, params: Record<string, string>, host?: string): EventSource {
  const queryString = new URLSearchParams(params).toString();
  return apiSSE(`${sseUrl}?${queryString}`, host);
}
