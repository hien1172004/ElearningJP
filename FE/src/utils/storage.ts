const TOKEN_KEY = 'elearning_access_token'

export const storage = {
  getToken: (): string | null => {
    try {
      return localStorage.getItem(TOKEN_KEY)
    } catch {
      return null
    }
  },

  setToken: (token: string): void => {
    try {
      localStorage.setItem(TOKEN_KEY, token)
    } catch {
      // ignore
    }
  },

  clearToken: (): void => {
    try {
      localStorage.removeItem(TOKEN_KEY)
    } catch {
      // ignore
    }
  },
}
