/**
 * 生产环境在 Vercel「环境变量」中设置 VITE_API_BASE_URL，须以 /api 结尾或包含路径前缀，
 * 例如：https://你的ECS公网或域名:8080/api
 */
export function getApiBaseUrl() {
  const apiBase = import.meta.env.VITE_API_BASE_URL
  if (apiBase && String(apiBase).trim() !== '') {
    return String(apiBase).replace(/\/$/, '')
  }
  return '/api'
}
