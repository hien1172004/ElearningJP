import { axiosClient } from './api/axiosClient'

export interface CloudinaryUploadResult {
  publicId: string
  url: string
  secureUrl: string
  format?: string
  resourceType?: string
  bytes?: number
}

export const mediaService = {
  uploadFile: async (file: File, folder = 'elearningJP/exam'): Promise<string> => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('folder', folder)

    const res: any = await axiosClient.post('/media/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })

    const payload = res?.data?.data || res?.data || res?.result || res
    return payload?.secureUrl || payload?.url || (typeof payload === 'string' ? payload : '')
  },
}
