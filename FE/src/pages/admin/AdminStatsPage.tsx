import React, { useState } from 'react'
import { BarChart3 } from 'lucide-react'
import { Button as AntdButton, Segmented, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PageHeader, LoadingState, EmptyState } from '@/components/common'
import { ChartCard, DailyLineChart, SimpleBarChart, StatTile } from '@/components/admin'
import { useAdminDashboard } from '@/hooks'
import type { DashboardLevelStats, DashboardRangeDays } from '@/types'

const RANGE_OPTIONS: { label: string; value: DashboardRangeDays }[] = [
  { label: '7 ngày', value: 7 },
  { label: '30 ngày', value: 30 },
  { label: '90 ngày', value: 90 },
]

const fmtNumber = (n: number) => n.toLocaleString('vi-VN')

/** yyyy-MM-dd → dd/MM */
const fmtDay = (iso: string) => {
  const [, m, d] = iso.split('-')
  return d && m ? `${d}/${m}` : iso
}

/** yyyy-MM-dd → dd/MM/yyyy */
const fmtFullDay = (iso: string) => {
  const [y, m, d] = iso.split('-')
  return y && m && d ? `${d}/${m}/${y}` : iso
}

const fmtPercent = (ratio: number | null | undefined) =>
  ratio == null ? '—' : `${Math.round(ratio * 1000) / 10}%`

/** Giây → "12,5 giờ" hoặc "45 phút". */
const fmtDuration = (seconds: number) => {
  if (seconds >= 3600) {
    return `${(Math.round((seconds / 3600) * 10) / 10).toLocaleString('vi-VN')} giờ`
  }
  return `${Math.round(seconds / 60).toLocaleString('vi-VN')} phút`
}

const levelColumns: ColumnsType<DashboardLevelStats> = [
  { title: 'Cấp độ', dataIndex: 'level', key: 'level' },
  {
    title: 'Bài đã nộp',
    dataIndex: 'submitted',
    key: 'submitted',
    align: 'right',
    render: (v: number) => <span className="tabular-nums">{fmtNumber(v)}</span>,
  },
  {
    title: 'Đỗ / có kết quả',
    key: 'passed',
    align: 'right',
    render: (_, r) => (
      <span className="tabular-nums">
        {fmtNumber(r.passed)} / {fmtNumber(r.graded)}
      </span>
    ),
  },
  {
    title: 'Tỉ lệ đỗ',
    dataIndex: 'passRate',
    key: 'passRate',
    align: 'right',
    render: (v: number | null) => <span className="tabular-nums">{fmtPercent(v)}</span>,
  },
  {
    title: 'Điểm TB',
    dataIndex: 'avgScaledScore',
    key: 'avgScaledScore',
    align: 'right',
    render: (v: number | null) => (
      <span className="tabular-nums">{v == null ? '—' : v.toLocaleString('vi-VN')}</span>
    ),
  },
]

export const AdminStatsPage: React.FC = () => {
  const [days, setDays] = useState<DashboardRangeDays>(30)
  const { data, isLoading, isError, isFetching, refetch } = useAdminDashboard(days)

  const header = (
    <PageHeader
      // badge="Quản trị"
      title="Thống kê tổng quan"
      subtitle={
        data
          ? `Từ ${fmtFullDay(data.from)} đến ${fmtFullDay(data.to)} · so sánh với ${data.days} ngày liền trước`
          : 'Người dùng, mức độ hoạt động học tập và kết quả thi thử JLPT.'
      }
      variant="indigo"
      actions={
        <Segmented<DashboardRangeDays>
          options={RANGE_OPTIONS}
          value={days}
          onChange={setDays}
          disabled={isFetching && !data}
        />
      }
    />
  )

  if (isLoading) {
    return (
      <div className="max-w-6xl mx-auto px-4 py-6 space-y-5">
        {header}
        <LoadingState message="Đang tải số liệu thống kê..." />
      </div>
    )
  }

  if (isError || !data) {
    return (
      <div className="max-w-6xl mx-auto px-4 py-6 space-y-5">
        {header}
        <EmptyState
          icon={<BarChart3 className="w-10 h-10 text-rose-400" />}
          title="Không tải được số liệu thống kê"
          description="Kiểm tra kết nối hoặc quyền truy cập rồi thử lại."
          action={<AntdButton onClick={() => refetch()}>Thử lại</AntdButton>}
        />
      </div>
    )
  }

  const { users, activity, exams, daily } = data

  return (
    <div
      className="max-w-6xl mx-auto px-4 py-6 space-y-6 transition-opacity"
      style={{ opacity: isFetching ? 0.6 : 1 }}
      aria-busy={isFetching}
    >
      {header}

      {/* ======================== Người dùng ======================== */}
      <section className="space-y-3">
        <h2 className="text-base font-semibold text-slate-800">Người dùng</h2>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          <StatTile
            label="Tổng người dùng"
            value={fmtNumber(users.total)}
            hint={`Email ${fmtNumber(users.localAccounts)} · Google ${fmtNumber(users.googleAccounts)}`}
          />
          <StatTile
            label={`Người dùng mới (${data.days} ngày)`}
            value={fmtNumber(users.newInRange)}
            current={users.newInRange}
            previous={users.newPrevious}
          />
          <StatTile label="Tài khoản bị khóa" value={fmtNumber(users.banned)} />
          <StatTile label="Tài khoản vô hiệu hóa" value={fmtNumber(users.inactive)} />
        </div>
        <ChartCard title="Người dùng mới theo ngày">
          <SimpleBarChart
            data={daily}
            xKey="date"
            yKey="newUsers"
            valueLabel="Người dùng mới"
            formatX={fmtDay}
          />
        </ChartCard>
      </section>

      {/* ======================== Hoạt động học ======================== */}
      <section className="space-y-3">
        <h2 className="text-base font-semibold text-slate-800">Mức độ hoạt động</h2>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          <StatTile label="Đang học hôm nay" value={fmtNumber(activity.activeToday)} />
          <StatTile
            label="Người học trong kỳ"
            value={fmtNumber(activity.activeInRange)}
            current={activity.activeInRange}
            previous={activity.activePrevious}
            hint={`Trung bình ${activity.avgDailyActive.toLocaleString('vi-VN')} người/ngày`}
          />
          <StatTile
            label="Tổng thời gian học"
            value={fmtDuration(activity.studySecondsInRange)}
            current={activity.studySecondsInRange}
            previous={activity.studySecondsPrevious}
          />
          <StatTile
            label="Lượt đạt mục tiêu ngày"
            value={fmtNumber(activity.goalMetCount)}
            hint="Mỗi người mỗi ngày tính 1 lượt"
          />
        </div>
        <div className="grid lg:grid-cols-3 gap-3">
          <ChartCard title="Số người học mỗi ngày" className="lg:col-span-2">
            <DailyLineChart
              data={daily}
              xKey="date"
              yKey="activeUsers"
              valueLabel="Người học"
              formatX={fmtDay}
              formatTooltipLabel={fmtFullDay}
            />
          </ChartCard>
          <ChartCard
            title="Phân bố chuỗi ngày học (streak)"
            subtitle="Số liệu ước tính, tính theo độ dài streak hiện tại (ngày)"
          >
            <SimpleBarChart
              data={activity.streakBuckets}
              xKey="label"
              yKey="users"
              valueLabel="Người dùng"
              formatX={(v) => `${v} ngày`}
            />
          </ChartCard>
        </div>
      </section>

      {/* ======================== Thi thử JLPT ======================== */}
      <section className="space-y-3">
        <h2 className="text-base font-semibold text-slate-800">Thi thử JLPT</h2>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          <StatTile
            label="Bài đã nộp"
            value={fmtNumber(exams.submittedInRange)}
            current={exams.submittedInRange}
            previous={exams.submittedPrevious}
          />
          <StatTile
            label="Tỉ lệ đỗ"
            value={fmtPercent(exams.passRate)}
            hint="Trên các bài có kết quả đỗ/trượt"
          />
          <StatTile
            label="Điểm trung bình"
            value={exams.avgScaledScore == null ? '—' : exams.avgScaledScore.toLocaleString('vi-VN')}
            hint="Điểm quy đổi"
          />
          <StatTile
            label="Bài đang làm dở"
            value={fmtNumber(exams.inProgress)}
            hint="Bắt đầu trong kỳ nhưng chưa nộp"
          />
        </div>
        <div className="grid lg:grid-cols-2 gap-3">
          <ChartCard title="Tỉ lệ đỗ theo cấp độ">
            <SimpleBarChart
              data={exams.byLevel}
              xKey="level"
              yKey="passRate"
              valueLabel="Tỉ lệ đỗ"
              yDomain={[0, 1]}
              formatY={(v) => `${Math.round(v * 100)}%`}
              formatValue={(v) => fmtPercent(v)}
            />
          </ChartCard>
          <ChartCard title="Chi tiết theo cấp độ">
            <Table<DashboardLevelStats>
              rowKey="level"
              size="small"
              columns={levelColumns}
              dataSource={exams.byLevel}
              pagination={false}
            />
          </ChartCard>
        </div>
      </section>
    </div>
  )
}
