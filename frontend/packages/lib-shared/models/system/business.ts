import type { CompanyTypeEnum } from '../../enums/commonEnum';
import type { TableQueryParams } from '../common';
import { PersonalExportStatusEnum } from '@lib/shared/enums/systemEnum';

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
}

// 同步组织设置
export interface ConfigSynchronization {
  type: CompanyTypeEnum; // 类型
  corpId?: string; // 企业ID
  agentId?: string; // 应用ID
  appSecret?: string; // 应用密钥
  syncEnable?: boolean; // 同步组织架构是否开启
  qrcodeEnable?: boolean; // 扫码登录是否开启
  verify?: boolean; // 是否验证通过
  redirectUrl?: string; // DE URL
  deAccount?: string; // DE 账号
  weComEnable?: boolean; // 开启企业微信通知
  deBoardEnable?: boolean; // DE看板是否开启
}

// 同步组织和扫码卡片数据类型
export interface IntegrationItem {
  type: string; // 类型
  title: string;
  description: string;
  logo: string;
  hasConfig: boolean;
  response: ConfigSynchronization;
}

// 三方类型集合设置
export interface OptionDTO {
  id: string; // key
  name: string; // value
}

// 认证设置
export interface Auth {
  description: string; // 描述
  name: string; // 名称
  type: string; // 类型 OAUTH2, LDAP, OIDC, CAS
  enable: boolean; // 是否启用
}

export interface AuthForm extends Auth {
  id?: string; // 认证源ID
  configuration: Record<string, any>;
}

export interface AuthUpdateParams extends Auth {
  id?: string; // 认证源ID
  configuration: string; // 认证源配置
}

export interface AuthItem extends Auth {
  id: string; // ID
  createUser: string; // 创建人
  updateUser: string; // 修改人
  createTime: number; // 创建时间
  updateTime: number; // 更新时间
  configId: string; // 配置id
  content: string; // 配置内容
}

export interface AuthTableQueryParams extends TableQueryParams {
  configId: string; // 认证设置id
}

// 个人中心
export interface PersonalInfoRequest {
  email: string;
  phone: string;
}

export interface PersonalPassword {
  email: string;
  code: string;
  password: string;
  confirmPassword: string;
}

export interface SendEmailDTO {
  email: string;
}

export interface RepeatClueParams extends TableQueryParams {
  name: string;
  id?: string;
}

export interface RepeatCustomerItem {
  id: string;
  name: string;
  owner: string;
  ownerName: string;
  createTime: number;
  repeatType: string;
  clueCount: number;
  clueModuleEnable: boolean;
  opportunityCount: number;
  opportunityModuleEnable: boolean;
}

export interface RepeatContactItem {
  id: string;
  name: string;
  customerName: string;
  ownerName: string;
  createTime: number;
  phone: string;
  enable: boolean;
  opportunityCount: number;
}

export interface RepeatClueItem {
  id: string;
  name: string;
  owner: string;
  stage: string;
  ownerName: string;
  contact: string;
  phone: string;
}

export interface RepeatOpportunityItem {
  id: string;
  name: string;
  customerId: string;
  customerName: string;
  stage: string;
  products: string[];
  productNames: string[];
  owner: string;
  ownerName: string;
  productNameList:string[]
}

export interface ExportCenterListParams {
  keyword: string;
  exportType: string;
  exportStatus: string;
}

export interface ExportCenterItem {
  id: string;
  createUser: string;
  updateUser: string;
  createTime: number;
  updateTime: number;
  fileName: string;
  resourceType: string;
  fileId: string;
  status: PersonalExportStatusEnum;
  organizationId: string;
}
