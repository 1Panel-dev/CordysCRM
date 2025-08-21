import type { MessageCenterItem } from '@lib/shared/models/system/message';

export interface UserInfo {
  id: string;
  name: string;
  email: string;
  password: string;
  enable: boolean;
  createTime: number;
  updateTime: number;
  language: string;
  lastOrganizationId: string;
  phone: string;
  source: string;
  createUser: string;
  updateUser: string;
  platformInfo: string;
  avatar: string;
  permissionIds: string[];
  organizationIds: string[];
  csrfToken: string;
  sessionId: string;
  roles: string[];
  departmentId: string;
  departmentName: string;
  defaultPwd: boolean;
}

export interface MessageInfo {
  read: boolean;
  notificationDTOList: MessageCenterItem[];
  announcementDTOList: MessageCenterItem[];
}
