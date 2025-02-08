// 邮件设置
export interface ConfigEmailParams {
  host: string; // SMTP 主机
  port: string; // SMTP 端口
  account: string; // SMTP 账号
  password: string; // SMTP 密码
  from: string; // 指定发件人
  recipient: string; // 指定收件人
  ssl: string; // SSL 开关
  tsl: string; // TSL 开关
  id?: string; // ID
}

// 同步组织设置
export interface ConfigSynchronization {
  type: string; // 类型
  corpId: string; // 企业ID
  agentId: string; // 应用ID
  appSecret: string; // 应用密钥
  id: string; // ID
  enable: boolean; // 是否开启
}

// 同步组织和扫码卡片数据类型
export interface IntegrationItem extends ConfigSynchronization {
  title: string;
  description: string;
  logo: string;
  hasConfig: boolean;
}
