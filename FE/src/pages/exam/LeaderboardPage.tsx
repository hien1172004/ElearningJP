import React from 'react'
import { LeaderboardWidget } from '@/components'
import { Link } from 'react-router-dom'
import { ChevronLeft } from 'lucide-react'

export const LeaderboardPage: React.FC = () => {
  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <div className="mb-6">
        <Link
          to="/exams"
          className="inline-flex items-center gap-1.5 text-sm font-bold text-slate-500 hover:text-indigo-600 transition"
        >
          <ChevronLeft className="w-4 h-4" />
          <span>Quay lại Phòng thi JLPT</span>
        </Link>
      </div>
      <LeaderboardWidget />
    </div>
  )
}

export default LeaderboardPage
