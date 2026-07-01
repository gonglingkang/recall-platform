/**
 * 后端统一响应结构（对应 com.recall.common.api.Result）。
 */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

/** 业务错误码（与后端 ResultCode 对应） */
export const ResultCode = {
  SUCCESS: 200,
  UNAUTHORIZED: 401,
} as const
