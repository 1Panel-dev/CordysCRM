import type { TableQueryParams } from '../common';

export interface TermCategoryItem {
  id: string;
  name: string;
  termCount: number;
  isNew?: boolean;
}

export interface TermCategoryParams {
  id?: string;
  name: string;
}

export interface TermParams {
  id?: string;
  catalogId?: string;
  standardTerm: string;
  alsoCalled: string; // 同义词
  avoidThese: string;
  useCase: string;
  systemReference: string;
  enable: boolean;
}

export interface TermItem extends TermParams {
  id: string;
  catalogName: string;
  updateUserName: string;
  createUserName: string;
  createTime: number;
  updateTime: number;
}

export interface TermListParams extends TableQueryParams {
  catalogId?: string;
}

export interface TermDiscoveryItem {
  id: string;
  freeTerm: string;
  source: string;
  reference: string;
  createTime: number;
}

export interface TermDiscoveryAdoptParams extends TermParams {
  id: string;
  catalogId: string;
}
