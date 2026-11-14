import React from 'react'
import { Modal } from 'antd'
import { ArrowRight, Sparkles, Check, X, Globe } from 'lucide-react'

interface PlanChangeConfirmModalProps {
  open: boolean
  itemType: 'course' | 'exam'
  title: string
  level?: string
  targetPremium: boolean
  isLoading?: boolean
  onConfirm: () => void | Promise<void>
  onCancel: () => void
}

export const PlanChangeConfirmModal: React.FC<PlanChangeConfirmModalProps> = ({
  open,
  itemType,
  title,
  level,
  targetPremium,
  isLoading = false,
  onConfirm,
  onCancel,
}) => {
  const itemLabel = itemType === 'course' ? 'khóa học' : 'đề thi'

  return (
    <Modal
      open={open}
      onCancel={isLoading ? undefined : onCancel}
      footer={null}
      centered
      width={480}
      closeIcon={null}
      className="[&_.ant-modal-content]:!p-0 [&_.ant-modal-content]:!rounded-2xl [&_.ant-modal-content]:!overflow-hidden [&_.ant-modal-content]:!bg-surface [&_.ant-modal-content]:!border [&_.ant-modal-content]:!border-suna [&_.ant-modal-content]:!shadow-xl"
    >
      <div className="bg-surface text-sumi">
        {/* Header Section */}
        <div
          className={`p-5 pb-4 border-b border-suna/60 relative ${
            targetPremium
              ? 'bg-gradient-to-b from-amber-500/10 via-amber-500/5 to-transparent'
              : 'bg-gradient-to-b from-indigo-500/10 via-indigo-500/5 to-transparent'
          }`}
        >
          <div className="flex items-center justify-between">
            <span
              className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-bold font-mono tracking-wide ${
                targetPremium
                  ? 'bg-amber-500/15 text-amber-800 dark:text-amber-300 border border-amber-500/30'
                  : 'bg-indigo-500/15 text-indigo-800 dark:text-indigo-300 border border-indigo-500/30'
              }`}
            >
              {targetPremium ? (
                <>
                  <Sparkles className="w-3 h-3 text-amber-600 dark:text-amber-400" />
                  <span>NÂNG CẤP GÓI PRO</span>
                </>
              ) : (
                <>
                  <Globe className="w-3 h-3 text-indigo-600 dark:text-indigo-400" />
                  <span>CÔNG KHAI MIỄN PHÍ</span>
                </>
              )}
            </span>

            <button
              type="button"
              disabled={isLoading}
              onClick={onCancel}
              className="text-nibi hover:text-sumi transition p-1 rounded-lg hover:bg-kinari cursor-pointer"
              aria-label="Đóng"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          <h3 className="text-base sm:text-lg font-bold text-sumi mt-2.5">
            {targetPremium
              ? `Chuyển ${itemLabel} sang gói PRO`
              : `Chuyển ${itemLabel} sang Miễn phí`}
          </h3>
          <p className="text-xs text-nibi mt-0.5">
            Thay đổi thiết lập quyền truy cập cho học viên trên hệ thống
          </p>
        </div>

        {/* Content Body */}
        <div className="p-5 space-y-4 text-xs">
          {/* Target Item & Plan Transition Card */}
          <div className="bg-kinari/60 border border-suna rounded-xl p-3.5 space-y-2.5">
            <div className="flex items-center justify-between gap-2">
              <span className="font-bold text-sumi text-sm truncate flex-1">
                {title}
              </span>
              {level && (
                <span className="shrink-0 text-[11px] font-mono font-bold px-2 py-0.5 rounded bg-surface border border-suna text-ai">
                  {level}
                </span>
              )}
            </div>

            <div className="flex items-center justify-between pt-2 border-t border-suna/60 text-[11px]">
              <div className="flex items-center gap-1.5 text-nibi">
                <span>Hiện tại:</span>
                <span className="font-semibold text-sumi">
                  {targetPremium ? 'Miễn phí' : 'Hội viên PRO'}
                </span>
              </div>

              <ArrowRight className="w-3.5 h-3.5 text-nibi" />

              <div className="flex items-center gap-1.5">
                <span className="text-nibi">Sau khi đổi:</span>
                <span
                  className={`font-bold px-2 py-0.5 rounded ${
                    targetPremium
                      ? 'bg-amber-500/20 text-amber-800 dark:text-amber-300 font-mono'
                      : 'bg-emerald-500/20 text-emerald-800 dark:text-emerald-300 font-mono'
                  }`}
                >
                  {targetPremium ? '✦ PRO' : 'Miễn phí'}
                </span>
              </div>
            </div>
          </div>

          {/* Impact Specifications List */}
          <div className="space-y-2 pt-1 text-nibi">
            <div className="flex items-start gap-2.5">
              <div className="mt-0.5 w-4 h-4 rounded-full bg-ai/10 text-ai flex items-center justify-center shrink-0">
                <Check className="w-2.5 h-2.5" />
              </div>
              <span className="text-sumi leading-relaxed">
                {targetPremium
                  ? `Chỉ học viên sở hữu gói PRO đang có hiệu lực mới được truy cập ${itemLabel} này.`
                  : `Mọi học viên (bao gồm tài khoản miễn phí) đều có thể tự do truy cập ${itemLabel} này.`}
              </span>
            </div>

            <div className="flex items-start gap-2.5">
              <div className="mt-0.5 w-4 h-4 rounded-full bg-ai/10 text-ai flex items-center justify-center shrink-0">
                <Check className="w-2.5 h-2.5" />
              </div>
              <span className="leading-relaxed">
                {targetPremium
                  ? 'Những học viên đã ghi danh từ trước vẫn được bảo lưu tiến độ và học tập bình thường.'
                  : 'Không áp dụng giới hạn gói và không tiêu hao hạn ngạch lượt truy cập.'}
              </span>
            </div>
          </div>
        </div>

        {/* Footer Actions */}
        <div className="p-4 bg-kinari/40 border-t border-suna flex items-center justify-end gap-2.5">
          <button
            type="button"
            disabled={isLoading}
            onClick={onCancel}
            className="px-4 py-2 rounded-xl text-xs font-semibold text-nibi hover:text-sumi bg-surface border border-suna hover:border-nibi/40 transition cursor-pointer disabled:opacity-50"
          >
            Hủy bỏ
          </button>

          <button
            type="button"
            disabled={isLoading}
            onClick={onConfirm}
            className={`px-4 sm:px-5 py-2 rounded-xl text-xs font-bold text-white transition flex items-center gap-1.5 shadow-xs cursor-pointer disabled:opacity-50 ${
              targetPremium
                ? 'bg-amber-600 hover:bg-amber-700'
                : 'bg-ai hover:bg-ai/90'
            }`}
          >
            {targetPremium ? (
              <Sparkles className="w-3.5 h-3.5" />
            ) : (
              <Globe className="w-3.5 h-3.5" />
            )}
            <span>
              {isLoading
                ? 'Đang lưu...'
                : targetPremium
                ? 'Xác nhận chuyển PRO'
                : 'Xác nhận mở Miễn phí'}
            </span>
          </button>
        </div>
      </div>
    </Modal>
  )
}
