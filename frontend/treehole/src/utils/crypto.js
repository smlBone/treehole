/**
 * 密码加密工具 - 使用 SHA-256 哈希
 * 前端将密码加密后再传给后端
 */
export async function hashPassword(password) {
  const encoder = new TextEncoder()
  const data = encoder.encode(password + '_treehole_salt_2024')
  const hashBuffer = await crypto.subtle.digest('SHA-256', data)
  const hashArray = Array.from(new Uint8Array(hashBuffer))
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('')
}
