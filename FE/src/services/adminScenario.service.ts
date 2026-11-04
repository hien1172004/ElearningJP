import { axiosClient } from './api/axiosClient'
import type {
  AdminScenario,
  AdminScenarioListParams,
  AdminScenarioPayload,
  ApiResponse,
  PageResponse,
} from '@/types'

/**
 * API quản lý kịch bản luyện nói — BE: AdminScenarioController `/admin/scenarios`.
 * ADMIN thao tác mọi kịch bản; TEACHER chỉ kịch bản do mình tạo (BE trả 403 nếu không phải).
 */
export const adminScenarioService = {
  getScenarios: async (
    params: AdminScenarioListParams,
  ): Promise<ApiResponse<PageResponse<AdminScenario>>> => {
    return axiosClient.get('/admin/scenarios', { params })
  },

  getScenario: async (id: number): Promise<ApiResponse<AdminScenario>> => {
    return axiosClient.get(`/admin/scenarios/${id}`)
  },

  createScenario: async (payload: AdminScenarioPayload): Promise<ApiResponse<AdminScenario>> => {
    return axiosClient.post('/admin/scenarios', payload)
  },

  updateScenario: async (
    id: number,
    payload: AdminScenarioPayload,
  ): Promise<ApiResponse<AdminScenario>> => {
    return axiosClient.put(`/admin/scenarios/${id}`, payload)
  },

  deleteScenario: async (id: number): Promise<ApiResponse<void>> => {
    return axiosClient.delete(`/admin/scenarios/${id}`)
  },
}
