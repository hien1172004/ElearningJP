import React, { useState } from 'react'
import { Select } from 'antd'
import { Link, useNavigate } from 'react-router-dom'
import { Button, LoadingState } from '@/components/common'
import { useProgressGoal, useUpdateProgressGoal } from '@/hooks/useProgressGoal'
import { cn } from '@/utils/cn'
import {
  CUSTOM_WEEKLY_GOALS,
  GOAL_PRESETS,
  dailyGoalFromWeekly,
} from '@/utils/progressGoal'

const MASCOT_SRC = '/mascot/standing.svg'

const IntroStep: React.FC<{ onStart: () => void }> = ({ onStart }) => (
  <div className="px-4 py-10 sm:py-14 flex justify-center">
    <div className="w-full max-w-2xl rounded-xl bg-surface border border-suna p-6 sm:p-10 flex flex-col sm:flex-row items-center gap-6 sm:gap-10 shadow-xs">
      <img src={MASCOT_SRC} alt="" className="w-36 h-36 sm:w-44 sm:h-44 shrink-0" />
      <div className="text-center sm:text-left">
        <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 leading-snug">
          Bắt đầu hành trình tốt hơn mỗi ngày
        </h1>
        <p className="text-gray-700 mt-3">
          Chinh phục mục tiêu từ vựng một cách trực quan và theo dõi sự tiến bộ của bạn!
        </p>
        <Button className="mt-5" onClick={onStart}>
          Bắt đầu
        </Button>
      </div>
    </div>
  </div>
)

interface OptionRowProps {
  selected: boolean
  onSelect: () => void
  badge: string
  badgeClass: string
  title: string
  hint?: string
}

const OptionRow: React.FC<OptionRowProps> = ({
  selected,
  onSelect,
  badge,
  badgeClass,
  title,
  hint,
}) => (
  <label
    className={cn(
      'flex items-center gap-4 rounded-xl border px-4 py-3 cursor-pointer transition-colors',
      selected ? 'border-indigo-500 bg-indigo-50/50' : 'border-gray-200 hover:border-gray-300',
    )}
  >
    <input
      type="radio"
      name="weekly-goal"
      checked={selected}
      onChange={onSelect}
      className="w-4 h-4 accent-indigo-600 shrink-0"
    />
    <span className="min-w-0">
      <span className={cn('inline-block rounded-full px-3 py-0.5 text-xs font-medium', badgeClass)}>
        {badge}
      </span>
      <span className="block font-semibold text-gray-900 mt-1">{title}</span>
      {hint && <span className="block text-xs text-gray-500 mt-0.5">{hint}</span>}
    </span>
  </label>
)

const CUSTOM = 'custom' as const
type Selection = number | typeof CUSTOM | null

interface StepProps {
  isSaving: boolean
  onSave: (weeklyGoalWords: number) => void
}

const ChooseStep: React.FC<
  StepProps & { current: number | null; onCustom: () => void; cancelable: boolean }
> = ({ current, isSaving, onSave, onCustom, cancelable }) => {
  const [selection, setSelection] = useState<Selection>(
    current === null ? null : current >= CUSTOM_WEEKLY_GOALS[0] ? CUSTOM : current,
  )

  const handleConfirm = () => {
    if (selection === CUSTOM) onCustom()
    else if (selection !== null) onSave(selection)
  }

  return (
    <div className="rounded-2xl bg-surface border border-gray-200/80 px-4 py-8 sm:py-10">
      <div className="max-w-2xl mx-auto">
        <h1 className="flex items-center justify-center gap-3 text-xl sm:text-2xl font-bold text-gray-900 text-center">
          <img src={MASCOT_SRC} alt="" className="w-10 h-10 shrink-0" />
          Bạn sẵn sàng học bao nhiêu từ vựng/tuần?
        </h1>

        <div className="space-y-3 mt-6">
          {GOAL_PRESETS.map((preset) => (
            <OptionRow
              key={preset.weeklyGoalWords}
              selected={selection === preset.weeklyGoalWords}
              onSelect={() => setSelection(preset.weeklyGoalWords)}
              badge={preset.label}
              badgeClass={preset.badgeClass}
              title={`${preset.weeklyGoalWords} từ/tuần`}
              hint={`Bạn sẽ cần học ${dailyGoalFromWeekly(preset.weeklyGoalWords)} từ/ngày`}
            />
          ))}
          <OptionRow
            selected={selection === CUSTOM}
            onSelect={() => setSelection(CUSTOM)}
            badge="Lão làng"
            badgeClass="bg-indigo-50 text-indigo-700"
            title="Tùy chỉnh"
          />
        </div>

        <div className="flex justify-center gap-3 mt-6">
          {cancelable && (
            <Link to="/progress">
              <Button variant="outline">Hủy</Button>
            </Link>
          )}
          <Button disabled={selection === null} isLoading={isSaving} onClick={handleConfirm}>
            Xác nhận
          </Button>
        </div>
      </div>
    </div>
  )
}

const CustomStep: React.FC<StepProps & { current: number | null; onBack: () => void }> = ({
  current,
  isSaving,
  onSave,
  onBack,
}) => {
  const [weekly, setWeekly] = useState<number | undefined>(
    current !== null && CUSTOM_WEEKLY_GOALS.includes(current) ? current : undefined,
  )

  return (
    <div className="rounded-2xl bg-surface border border-gray-200/80 px-4 py-8 sm:py-10">
      <div className="max-w-2xl mx-auto">
        <h1 className="flex items-center justify-center gap-3 text-xl sm:text-2xl font-bold text-gray-900 text-center">
          <img src={MASCOT_SRC} alt="" className="w-10 h-10 shrink-0" />
          Bắt đầu hành trình tốt hơn mỗi ngày
        </h1>
        <p className="text-sm italic text-gray-500 text-center mt-2">
          Chọn số từ phù hợp với khả năng và thời gian học mỗi ngày để duy trì động lực lâu dài.
        </p>

        <Select
          size="large"
          className="w-full mt-6"
          placeholder="Chọn số từ cần học"
          value={weekly}
          onChange={setWeekly}
          options={CUSTOM_WEEKLY_GOALS.map((value) => ({
            value,
            label: `${value} từ/tuần (~${dailyGoalFromWeekly(value)} từ/ngày)`,
          }))}
        />

        <div className="flex justify-center gap-3 mt-6">
          <Button variant="outline" onClick={onBack}>
            Quay lại
          </Button>
          <Button
            disabled={weekly === undefined}
            isLoading={isSaving}
            onClick={() => weekly !== undefined && onSave(weekly)}
          >
            Xác nhận
          </Button>
        </div>
      </div>
    </div>
  )
}

type Step = 'intro' | 'choose' | 'custom'

const GoalSteps: React.FC<{ current: number | null }> = ({ current }) => {
  const navigate = useNavigate()
  const updateGoal = useUpdateProgressGoal()
  const cancelable = current !== null
  // Đổi mục tiêu thì không cần màn giới thiệu.
  const [step, setStep] = useState<Step>(cancelable ? 'choose' : 'intro')

  const handleSave = (weeklyGoalWords: number) => {
    updateGoal.mutate(weeklyGoalWords, {
      onSuccess: () => navigate('/progress', { replace: true }),
    })
  }

  if (step === 'intro') return <IntroStep onStart={() => setStep('choose')} />
  if (step === 'custom') {
    return (
      <CustomStep
        current={current}
        isSaving={updateGoal.isPending}
        onSave={handleSave}
        onBack={() => setStep('choose')}
      />
    )
  }
  return (
    <ChooseStep
      current={current}
      isSaving={updateGoal.isPending}
      onSave={handleSave}
      onCustom={() => setStep('custom')}
      cancelable={cancelable}
    />
  )
}

/**
 * Trang chọn mục tiêu số từ mới mỗi tuần (`/progress/goal`).
 * Lần đầu: giới thiệu → chọn mức → (tùy chỉnh). Đổi mục tiêu: vào thẳng bước chọn mức.
 */
export const ProgressGoalPage: React.FC = () => {
  const { data, isLoading } = useProgressGoal()

  if (isLoading) return <LoadingState fullHeight />

  return (
    <div className="pb-12">
      <GoalSteps current={data?.weeklyGoalWords ?? null} />
    </div>
  )
}

export default ProgressGoalPage
