import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { Button } from '@/components'

export const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    // Placeholder - sẽ kết nối API ở feature tiếp theo
    console.log('Login with:', { email, password })
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="your-email@example.com"
          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
          required
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Mật khẩu</label>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="••••••••"
          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
          required
        />
      </div>

      <Button type="submit" className="w-full mt-2">
        Đăng nhập
      </Button>

      <div className="text-center text-sm text-gray-500 pt-2">
        Chưa có tài khoản?{' '}
        <Link to="/" className="text-indigo-600 hover:underline">
          Đăng ký ngay
        </Link>
      </div>
    </form>
  )
}
