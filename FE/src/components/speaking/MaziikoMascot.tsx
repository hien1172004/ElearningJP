import React, { useState } from 'react'

export type MascotVariant = 'maneki-neko' | 'shiba'

interface MaziikoMascotProps {
  size?: number
  className?: string
  showAura?: boolean
  variant?: MascotVariant
  showBadge?: boolean
}

export const MaziikoMascot: React.FC<MaziikoMascotProps> = ({
  size = 80,
  className = '',
  showAura = false,
  variant = 'maneki-neko',
  showBadge = false,
}) => {
  const [imageError, setImageError] = useState(false)

  const imgSrc =
    variant === 'shiba' ? '/mascot/shiba_inu.jpg' : '/mascot/maneki_neko.jpg'

  return (
    <div
      className={`relative inline-flex items-center justify-center select-none ${className}`}
      style={{ width: size, height: size }}
    >
      {/* 1. Hào quang phát sáng kiểu Nhật (Sakura & Indigo Glow) */}
      {showAura && (
        <>
          <div
            style={{ width: size * 1.45, height: size * 1.45 }}
            className="absolute -inset-4 rounded-full bg-linear-to-tr from-pink-400/30 via-indigo-500/25 to-sky-400/30 blur-xl pointer-events-none animate-pulse"
          />
          <div
            style={{ width: size * 1.2, height: size * 1.2 }}
            className="absolute rounded-full bg-linear-to-b from-rose-200/40 via-purple-300/20 to-transparent blur-md pointer-events-none"
          />
        </>
      )}

      {/* 2. Khung viền Avatar gradient Nhật Bản cao cấp */}
      <div
        style={{ width: size, height: size }}
        className="relative rounded-full p-[3px] bg-linear-to-tr from-amber-400 via-rose-400 to-indigo-500 shadow-lg shadow-indigo-500/20 transition-transform duration-300 hover:scale-105"
      >
        <div className="w-full h-full rounded-full overflow-hidden bg-white flex items-center justify-center relative">
          {!imageError ? (
            <img
              src={imgSrc}
              alt={variant === 'shiba' ? 'Maziiko Shiba Inu' : 'Maziiko Maneki-neko'}
              className="w-full h-full object-cover rounded-full"
              onError={() => setImageError(true)}
              loading="eager"
            />
          ) : (
            // Fallback SVG phong cách mèo thần tài Nhật Bản nếu chưa tải ảnh
            <div className="w-full h-full rounded-full bg-linear-to-b from-amber-50 to-rose-50 flex items-center justify-center text-rose-500 font-bold text-xs">
              🐱 Maziiko
            </div>
          )}
        </div>

        {/* 3. Huy hiệu nhỏ hoa anh đào (Sakura Badge) hoặc Chuông Nhật Bản */}
        {showBadge && size >= 48 && (
          <div className="absolute -bottom-1 -right-1 w-6 h-6 rounded-full bg-white shadow-md border border-rose-100 flex items-center justify-center text-xs animate-bounce">
            🌸
          </div>
        )}
      </div>
    </div>
  )
}
