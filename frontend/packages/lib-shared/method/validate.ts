// 邮箱校验
export const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
// 密码校验，8-32位
export const passwordLengthRegex = /^.{8,32}$/;
// 密码校验，必须包含数字和字母，特殊符号范围校验
export const passwordWordRegex = /^(?=.*\d)(?=.*[a-zA-Z])[0-9a-zA-Z!@#$%^&*()_+.]+$/;
// Git地址校验
export const gitRepositoryUrlRegex = /\.git$/;

/**
 * 校验邮箱
 * @param email 邮箱
 * @returns boolean
 */
export function validateEmail(email: string): boolean {
  return emailRegex.test(email);
}

/**
 * 校验手机号（通用验证）
 * 支持以下格式：
 * - 中国11位手机号：1xxxxxxxxxx
 * - E.164 格式：+xxxxxxxxxxx
 * @param phone 手机号
 * @returns boolean
 */
export function validatePhone(phone: string): boolean {
  if (!phone) return false;

  const cleaned = cleanPhoneNumber(phone);

  // 中国11位手机号（以1开头）- 直接验证，无需转换
  if (/^1\d{10}$/.test(cleaned)) {
    return true;
  }

  // E.164 格式 - 使用 E.164 验证
  if (cleaned.startsWith('+')) {
    return validateE164Phone(cleaned) === undefined;
  }

  return false;
}

/**
 * 校验密码长度
 * @param password 密码
 * @returns boolean
 */
export function validatePasswordLength(password: string): boolean {
  return passwordLengthRegex.test(password);
}

/**
 * 校验密码组成
 * @param password 密码
 * @returns boolean
 */
export function validateWordPassword(password: string): boolean {
  return passwordWordRegex.test(password);
}

/**
 * 校验密码
 * @param password 密码
 * @returns boolean
 */
export function validatePassword(password: string): boolean {
  return validatePasswordLength(password) && validateWordPassword(password);
}

export function getPatternByAreaCode(code: string): RegExp | null {
  switch (code) {
    case '+86': // 中国大陆
      return /^\d{10,12}$/;
    case '+852': // 香港
      return /^\d{8}$/;
    case '+853': // 澳门
      return /^\d{8}$/;
    case '+886': // 台湾
      return /^\d{8,11}$/;
    default: // 其他
      return /^\d+$/;
  }
}

/**
 * E.164 标准正则表达式
 * 格式：+[1-3位国家代码][电话号码]，总长度不超过15位数字（不包括+号）
 */
export const e164Regex = /^\+[1-9]\d{1,14}$/;

/**
 * 清理手机号：去除空格、连字符、括号等字符
 * @param phone 手机号
 * @returns 清理后的手机号
 */
export function cleanPhoneNumber(phone: string): string {
  if (!phone) return '';
  return phone.replace(/[\s\uFEFF\xA0\-()（）]/g, '');
}

/**
 * 格式化手机号
 * @param phone 手机号
 * @param format 格式类型，'11' 表示启用 E.164 严格模式
 * @returns 格式化后的手机号
 */
export function normalizeToE164(phone: string, format?: string): string {
  if (!phone) return '';

  const cleaned = cleanPhoneNumber(phone);

  // 只有 format='11' 时才启用 E.164 模式
  if (format === '11') {
    // 如果是11位数字且以1开头，自动添加 +86（中国手机号格式）
    if (/^1\d{10}$/.test(cleaned)) {
      return `+86${cleaned}`;
    }

    // 如果已经是 E.164 格式（以+开头），直接返回
    if (cleaned.startsWith('+')) {
      return cleaned;
    }

    // 如果以数字开头，添加 + 前缀
    if (/^\d/.test(cleaned)) {
      return `+${cleaned}`;
    }
  }

  // format != '11' 时，只做基本清理，不强制 E.164 格式
  return cleaned;
}

/**
 * 验证 E.164 格式手机号
 * E.164 标准：
 * - 必须以 + 开头
 * - 后跟国家代码（1-3位数字，不能以0开头）
 * - 然后是电话号码（纯数字）
 * - 总长度不超过15位数字（不包括+号）
 * - 格式：+[1-3位国家代码][电话号码]
 * @param phone 手机号（必须是 E.164 格式，即必须以+开头）
 * @returns 错误信息 key，如果验证通过返回 undefined
 */
export function validateE164Phone(phone: string): string | undefined {
  if (!phone) return undefined;
  
  const cleaned = cleanPhoneNumber(phone);
  
  // E.164 标准：必须以 + 开头
  if (!cleaned.startsWith('+')) {
    return 'phone.formatValidator';
  }
  
  // 提取国家代码和号码部分
  // E.164 国家代码：1-3位数字，不能以0开头
  const e164Match = cleaned.match(/^\+([1-9]\d{0,2})(\d+)$/);
  if (!e164Match) {
    return 'phone.formatValidator';
  }
  
  const countryCode = e164Match[1];
  const phoneNumber = e164Match[2];
  
  // 总长度验证：国家代码 + 电话号码不超过15位（不包括+号）
  const totalLength = countryCode.length + phoneNumber.length;
  if (totalLength < 7 || totalLength > 15) {
    return 'phone.formatValidator';
  }
  
  // 电话号码部分至少4位（E.164 要求）
  if (phoneNumber.length < 4) {
    return 'phone.formatValidator';
  }
  
  // 如果国家代码在已知列表中，使用对应的验证规则
  const areaCode = `+${countryCode}`;
  const pattern = getPatternByAreaCode(areaCode);
  if (pattern) {
    if (!pattern.test(phoneNumber)) {
      return 'phone.formatValidator';
    }
  }
  
  return undefined;
}
