import React, { useEffect, useState, useRef } from 'react'
import {
  Card,
  Tabs,
  Form,
  Input,
  Button,
  Select,
  DatePicker,
  InputNumber,
  Tag,
  Spin,
  Alert,
} from 'antd'
import {
  UserOutlined,
  LockOutlined,
  BookOutlined,
  CameraOutlined,
  SafetyCertificateOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import {
  useUserProfile,
  useUpdateProfile,
  useChangeAvatar,
  useChangePassword,
} from '@/hooks'
import type {
  ChangePasswordRequest,
  LearnerLevel,
  JlptLevel,
  UpdateProfileRequest,
} from '@/types'

export const ProfilePage: React.FC = () => {
  const { data: profile, isLoading, error } = useUserProfile()
  const { mutate: updateProfile, isPending: isUpdatingProfile } = useUpdateProfile()
  const { mutate: changeAvatar, isPending: isUploadingAvatar } = useChangeAvatar()
  const { mutate: changePassword, isPending: isChangingPassword } = useChangePassword()

  const [personalForm] = Form.useForm()
  const [learningForm] = Form.useForm()
  const [passwordForm] = Form.useForm()

  const fileInputRef = useRef<HTMLInputElement>(null)
  const [previewAvatar, setPreviewAvatar] = useState<string | null>(null)

  // Điền dữ liệu vào form khi profile load xong
  useEffect(() => {
    if (profile) {
      personalForm.setFieldsValue({
        fullName: profile.fullName || '',
        phoneNumber: profile.phoneNumber || '',
        bio: profile.bio || '',
      })

      learningForm.setFieldsValue({
        currentLevel: profile.currentLevel || undefined,
        targetLevel: profile.targetLevel || undefined,
        targetExamDate: profile.targetExamDate ? dayjs(profile.targetExamDate) : undefined,
        dailyStudyTimeMinutes: profile.dailyStudyTimeMinutes || 30,
      })
    }
  }, [profile, personalForm, learningForm])

  // Xử lý upload avatar
  const handleAvatarChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    if (!file.type.startsWith('image/')) {
      alert('Vui lòng chọn tệp hình ảnh!')
      return
    }

    if (file.size > 5 * 1024 * 1024) {
      alert('Kích thước ảnh tối đa là 5MB!')
      return
    }

    // Hiển thị preview ngay
    const previewUrl = URL.createObjectURL(file)
    setPreviewAvatar(previewUrl)

    // Gọi API đổi avatar
    changeAvatar(file, {
      onError: () => {
        setPreviewAvatar(null)
      },
    })
  }

  // Submit cập nhật thông tin cá nhân
  const onSavePersonalInfo = (values: { fullName: string; phoneNumber?: string; bio?: string }) => {
    if (!profile) return
    const payload: UpdateProfileRequest = {
      fullName: values.fullName,
      phoneNumber: values.phoneNumber || undefined,
      bio: values.bio || undefined,
      currentLevel: profile.currentLevel,
      targetLevel: profile.targetLevel,
      targetExamDate: profile.targetExamDate,
      dailyStudyTimeMinutes: profile.dailyStudyTimeMinutes,
    }
    updateProfile(payload)
  }

  // Submit cập nhật mục tiêu học tập
  const onSaveLearningGoals = (values: {
    currentLevel?: LearnerLevel
    targetLevel?: JlptLevel
    targetExamDate?: dayjs.Dayjs
    dailyStudyTimeMinutes?: number
  }) => {
    if (!profile) return
    const payload: UpdateProfileRequest = {
      fullName: profile.fullName,
      phoneNumber: profile.phoneNumber,
      bio: profile.bio,
      currentLevel: values.currentLevel,
      targetLevel: values.targetLevel,
      targetExamDate: values.targetExamDate ? values.targetExamDate.format('YYYY-MM-DD') : undefined,
      dailyStudyTimeMinutes: values.dailyStudyTimeMinutes,
    }
    updateProfile(payload)
  }

  // Submit đổi mật khẩu
  const onSavePassword = (values: ChangePasswordRequest) => {
    changePassword(values, {
      onSuccess: () => {
        passwordForm.resetFields()
      },
    })
  }

  if (isLoading) {
    return (
      <div className="flex justify-center items-center py-20">
        <Spin size="large" tip="Đang tải thông tin cá nhân..." />
      </div>
    )
  }

  if (error || !profile) {
    return (
      <div className="max-w-3xl mx-auto py-10">
        <Alert
          message="Không thể tải thông tin cá nhân"
          description="Vui lòng kiểm tra lại kết nối mạng hoặc thử đăng nhập lại."
          type="error"
          showIcon
        />
      </div>
    )
  }

  const currentAvatar = previewAvatar || profile.avatarUrl

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Banner & Header Profile */}
      <Card className="shadow-xs border-gray-200 overflow-hidden">
        <div className="flex flex-col sm:flex-row items-center sm:items-start gap-6">
          {/* Avatar container */}
          <div className="relative group">
            <div className="w-24 h-24 sm:w-28 sm:h-28 rounded-full overflow-hidden border-4 border-indigo-50 shadow-sm bg-gray-100 flex items-center justify-center">
              {currentAvatar ? (
                <img
                  src={currentAvatar}
                  alt={profile.fullName || 'User'}
                  className="w-full h-full object-cover"
                />
              ) : (
                <UserOutlined className="text-4xl text-gray-400" />
              )}
            </div>

            {/* Nút upload avatar */}
            <button
              type="button"
              disabled={isUploadingAvatar}
              onClick={() => fileInputRef.current?.click()}
              className="absolute bottom-0 right-0 bg-indigo-600 text-white p-2 rounded-full shadow-md hover:bg-indigo-700 transition cursor-pointer"
              title="Đổi ảnh đại diện"
            >
              {isUploadingAvatar ? <Spin size="small" /> : <CameraOutlined className="text-sm" />}
            </button>
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleAvatarChange}
              accept="image/*"
              className="hidden"
            />
          </div>

          {/* User Brief Info */}
          <div className="flex-1 text-center sm:text-left space-y-2">
            <div className="flex flex-col sm:flex-row sm:items-center gap-2">
              <h1 className="text-2xl font-bold text-gray-900">{profile.fullName || 'Chưa cập nhật tên'}</h1>
              <div className="flex justify-center sm:justify-start gap-1.5">
                {profile.roles?.map((role) => (
                  <Tag color="indigo" key={role} className="font-semibold uppercase">
                    {role}
                  </Tag>
                ))}
                <Tag color={profile.status === 'ACTIVE' ? 'success' : 'default'}>
                  {profile.status === 'ACTIVE' ? 'Đang hoạt động' : profile.status}
                </Tag>
              </div>
            </div>

            <p className="text-sm text-gray-500">{profile.email}</p>

            {profile.bio && (
              <p className="text-sm text-gray-600 italic bg-gray-50 p-2.5 rounded-md border border-gray-100 max-w-xl">
                "{profile.bio}"
              </p>
            )}

            <div className="flex flex-wrap justify-center sm:justify-start gap-4 text-xs text-gray-500 pt-1">
              <span>Trình độ: <strong className="text-gray-700">{profile.currentLevel || 'Chưa thiết lập'}</strong></span>
              <span>•</span>
              <span>Mục tiêu: <strong className="text-indigo-600">{profile.targetLevel || 'Chưa thiết lập'}</strong></span>
              {profile.dailyStudyTimeMinutes && (
                <>
                  <span>•</span>
                  <span>Học mỗi ngày: <strong className="text-emerald-600">{profile.dailyStudyTimeMinutes} phút</strong></span>
                </>
              )}
            </div>
          </div>
        </div>
      </Card>

      {/* Main Tabs */}
      <Card className="shadow-xs border-gray-200">
        <Tabs
          defaultActiveKey="personal"
          items={[
            {
              key: 'personal',
              label: (
                <span className="flex items-center gap-2">
                  <UserOutlined />
                  Thông tin cá nhân
                </span>
              ),
              children: (
                <div className="py-2 max-w-xl">
                  <Form
                    form={personalForm}
                    layout="vertical"
                    onFinish={onSavePersonalInfo}
                    requiredMark={false}
                  >
                    <Form.Item label="Email">
                      <Input value={profile.email} disabled className="!bg-gray-50 text-gray-500" />
                    </Form.Item>

                    <Form.Item
                      name="fullName"
                      label="Họ và tên"
                      rules={[
                        { required: true, message: 'Vui lòng nhập họ và tên' },
                        { max: 100, message: 'Họ và tên tối đa 100 ký tự' },
                      ]}
                    >
                      <Input placeholder="Nhập họ và tên đầy đủ" size="large" />
                    </Form.Item>

                    <Form.Item
                      name="phoneNumber"
                      label="Số điện thoại"
                      rules={[
                        {
                          pattern: /^(0|\+84)[3|5|7|8|9][0-9]{8}$/,
                          message: 'Số điện thoại không đúng định dạng (VD: 0912345678)',
                        },
                      ]}
                    >
                      <Input placeholder="Số điện thoại liên hệ" size="large" />
                    </Form.Item>

                    <Form.Item
                      name="bio"
                      label="Tiểu sử / Giới thiệu bản thân"
                      rules={[{ max: 500, message: 'Tiểu sử tối đa 500 ký tự' }]}
                    >
                      <Input.TextArea
                        rows={4}
                        placeholder="Một vài dòng giới thiệu về kinh nghiệm, động lực học tiếng Nhật..."
                        maxLength={500}
                        showCount
                      />
                    </Form.Item>

                    <Form.Item className="pt-2">
                      <Button
                        type="primary"
                        htmlType="submit"
                        size="large"
                        loading={isUpdatingProfile}
                        icon={<CheckCircleOutlined />}
                        className="bg-indigo-600 hover:bg-indigo-700"
                      >
                        Lưu thông tin cá nhân
                      </Button>
                    </Form.Item>
                  </Form>
                </div>
              ),
            },
            {
              key: 'learning',
              label: (
                <span className="flex items-center gap-2">
                  <BookOutlined />
                  Kế hoạch & Mục tiêu học tập
                </span>
              ),
              children: (
                <div className="py-2 max-w-xl">
                  <Form
                    form={learningForm}
                    layout="vertical"
                    onFinish={onSaveLearningGoals}
                    requiredMark={false}
                  >
                    <Form.Item name="currentLevel" label="Trình độ hiện tại">
                      <Select
                        size="large"
                        placeholder="Chọn trình độ hiện tại của bạn"
                        options={[
                          { value: 'BEGINNER', label: 'Sơ cấp (Beginner - N5, N4)' },
                          { value: 'INTERMEDIATE', label: 'Trung cấp (Intermediate - N3)' },
                          { value: 'ADVANCED', label: 'Cao cấp (Advanced - N2, N1)' },
                        ]}
                      />
                    </Form.Item>

                    <Form.Item name="targetLevel" label="Mục tiêu JLPT">
                      <Select
                        size="large"
                        placeholder="Chọn cấp độ JLPT bạn muốn chinh phục"
                        options={[
                          { value: 'N5', label: 'JLPT N5 (Cơ bản)' },
                          { value: 'N4', label: 'JLPT N4 (Sơ cấp)' },
                          { value: 'N3', label: 'JLPT N3 (Trung cấp)' },
                          { value: 'N2', label: 'JLPT N2 (Trung - Cao cấp)' },
                          { value: 'N1', label: 'JLPT N1 (Thành thạo)' },
                        ]}
                      />
                    </Form.Item>

                    <Form.Item name="targetExamDate" label="Ngày thi dự kiến">
                      <DatePicker
                        size="large"
                        className="w-full"
                        format="DD/MM/YYYY"
                        placeholder="Chọn ngày thi dự kiến"
                        disabledDate={(current) => current && current < dayjs().startOf('day')}
                      />
                    </Form.Item>

                    <Form.Item
                      name="dailyStudyTimeMinutes"
                      label="Thời gian học mục tiêu mỗi ngày"
                      rules={[
                        {
                          type: 'number',
                          min: 5,
                          max: 480,
                          message: 'Thời gian học từ 5 đến 480 phút mỗi ngày',
                        },
                      ]}
                    >
                      <InputNumber
                        size="large"
                        min={5}
                        max={480}
                        step={5}
                        addonAfter="phút / ngày"
                        className="w-full"
                      />
                    </Form.Item>

                    <Form.Item className="pt-2">
                      <Button
                        type="primary"
                        htmlType="submit"
                        size="large"
                        loading={isUpdatingProfile}
                        icon={<CheckCircleOutlined />}
                        className="bg-indigo-600 hover:bg-indigo-700"
                      >
                        Lưu mục tiêu học tập
                      </Button>
                    </Form.Item>
                  </Form>
                </div>
              ),
            },
            {
              key: 'security',
              label: (
                <span className="flex items-center gap-2">
                  <LockOutlined />
                  Bảo mật & Mật khẩu
                </span>
              ),
              children: (
                <div className="py-2 max-w-xl">
                  <Form
                    form={passwordForm}
                    layout="vertical"
                    onFinish={onSavePassword}
                    requiredMark={false}
                  >
                    <Form.Item
                      name="oldPassword"
                      label="Mật khẩu hiện tại"
                      rules={[{ required: true, message: 'Vui lòng nhập mật khẩu hiện tại' }]}
                    >
                      <Input.Password
                        size="large"
                        placeholder="Nhập mật khẩu đang dùng"
                        prefix={<LockOutlined className="text-gray-400" />}
                      />
                    </Form.Item>

                    <Form.Item
                      name="newPassword"
                      label="Mật khẩu mới"
                      rules={[
                        { required: true, message: 'Vui lòng nhập mật khẩu mới' },
                        { min: 8, message: 'Mật khẩu mới phải có tối thiểu 8 ký tự' },
                      ]}
                    >
                      <Input.Password
                        size="large"
                        placeholder="Tối thiểu 8 ký tự"
                        prefix={<SafetyCertificateOutlined className="text-gray-400" />}
                      />
                    </Form.Item>

                    <Form.Item
                      name="confirmPassword"
                      label="Xác nhận mật khẩu mới"
                      dependencies={['newPassword']}
                      rules={[
                        { required: true, message: 'Vui lòng xác nhận mật khẩu mới' },
                        ({ getFieldValue }) => ({
                          validator(_, value) {
                            if (!value || getFieldValue('newPassword') === value) {
                              return Promise.resolve()
                            }
                            return Promise.reject(new Error('Mật khẩu xác nhận không khớp!'))
                          },
                        }),
                      ]}
                    >
                      <Input.Password
                        size="large"
                        placeholder="Nhập lại mật khẩu mới"
                        prefix={<SafetyCertificateOutlined className="text-gray-400" />}
                      />
                    </Form.Item>

                    <Form.Item className="pt-2">
                      <Button
                        type="primary"
                        htmlType="submit"
                        size="large"
                        loading={isChangingPassword}
                        className="bg-indigo-600 hover:bg-indigo-700"
                      >
                        Cập nhật mật khẩu
                      </Button>
                    </Form.Item>
                  </Form>
                </div>
              ),
            },
          ]}
        />
      </Card>
    </div>
  )
}
