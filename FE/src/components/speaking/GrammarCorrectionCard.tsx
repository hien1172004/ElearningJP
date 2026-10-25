import React from 'react'
import { CheckCircle2, AlertCircle } from 'lucide-react'
import type { GrammarCorrection } from '@/types/speaking.types'

interface GrammarCorrectionCardProps {
  correction: GrammarCorrection
}

export const GrammarCorrectionCard: React.FC<GrammarCorrectionCardProps> = ({ correction }) => {
  if (!correction || (!correction.fixed && !correction.original)) return null

  return (
    <div className="mt-3 p-3 bg-amber-50/90 border border-amber-200/80 rounded-xl text-left shadow-xs transition-all">
      <div className="flex items-center gap-1.5 text-xs font-semibold text-amber-800 uppercase tracking-wider mb-2">
        <AlertCircle className="w-4 h-4 text-amber-600 shrink-0" />
        <span>Góp ý chỉnh sửa ngữ pháp</span>
      </div>

      <div className="space-y-1.5 text-xs sm:text-sm">
        {correction.original && (
          <div className="flex items-baseline gap-2">
            <span className="text-slate-400 font-medium shrink-0">Bạn nói:</span>
            <span className="text-rose-600 line-through decoration-rose-400 font-medium">
              {correction.original}
            </span>
          </div>
        )}

        {correction.fixed && (
          <div className="flex items-baseline gap-2">
            <span className="text-emerald-700 font-medium shrink-0 flex items-center gap-1">
              <CheckCircle2 className="w-3.5 h-3.5 inline text-emerald-600" /> Chuẩn hơn:
            </span>
            <span className="text-emerald-900 font-semibold">{correction.fixed}</span>
          </div>
        )}

        {correction.note_vi && (
          <div className="pt-1.5 mt-1 border-t border-amber-200/60 text-slate-600 text-xs italic">
            💡 {correction.note_vi}
          </div>
        )}
      </div>
    </div>
  )
}
