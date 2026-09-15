import request from './request'
import type { ApiResult } from './types'

/**
 * OA 对接配置 / OA 同步 API。
 * 对应后端 OaConfigController、OaSyncController。
 */

export interface OaConfig {
  oaBaseUrl: string
  username: string
  /** 是否已配置密码（后端不回明文） */
  hasPassword: boolean
  devProjectName: string
  leaveProjectName: string
  autoSync: boolean
}

export interface OaConfigSaveReq {
  oaBaseUrl: string
  username: string
  /** 已配置后留空表示不修改 */
  password?: string
  devProjectName: string
  leaveProjectName: string
  autoSync: boolean
}

/** 同步状态: 1运行中 2成功 3失败（与后端 OaSyncStatus 对应） */
export interface OaSyncLog {
  id: number
  weekStart: string
  triggerType: number
  status: number
  step: string | null
  errorMsg: string | null
  /** OA 工时提交截止时间（同步时从表单读取） */
  deadline: string | null
  /** 同步时 OA 端该周工时是否已填报 */
  oaSubmitted: boolean | null
  startTime: string
  endTime: string | null
}

export const getOaConfig = () =>
  request.get<any, ApiResult<OaConfig | null>>('/api/oa-config')

export const saveOaConfig = (data: OaConfigSaveReq) =>
  request.put<any, ApiResult<OaConfig>>('/api/oa-config', data)

export const triggerOaSync = (date: string) =>
  request.post<any, ApiResult<OaSyncLog>>(`/api/oa-sync/weeks/${date}/trigger`)

export const getOaSyncStatus = (date: string) =>
  request.get<any, ApiResult<OaSyncLog | null>>(`/api/oa-sync/weeks/${date}/status`)

export const OA_SYNC_STATUS = {
  RUNNING: 1,
  SUCCESS: 2,
  FAILED: 3
} as const
