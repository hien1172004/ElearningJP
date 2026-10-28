export type LearnerLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'

export type JlptLevel = 'N5' | 'N4' | 'N3' | 'N2' | 'N1'

export interface UserProfile {
  id: number
  email: string
  fullName: string
  avatarUrl?: string
  roles: string[]
  status: string
  phoneNumber?: string
  bio?: string
  currentLevel?: LearnerLevel
  targetLevel?: JlptLevel
  targetExamDate?: string
  dailyStudyTimeMinutes?: number
  timezone?: string
  createdAt?: string
  updatedAt?: string
}

export interface UpdateProfileRequest {
  fullName: string
  phoneNumber?: string
  bio?: string
  currentLevel?: LearnerLevel
  targetLevel?: JlptLevel
  targetExamDate?: string
  dailyStudyTimeMinutes?: number
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}
