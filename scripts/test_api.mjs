import assert from 'node:assert/strict'

globalThis.window = new EventTarget()
let csrfCalls = 0
const calls = []
let replyStatus = 200
globalThis.fetch = async (url, options = {}) => {
  calls.push({url, ...options})
  if (url === '/api/auth/csrf') return new Response(JSON.stringify({data: {token: 'token-' + ++csrfCalls}}))
  if (replyStatus === 204) return new Response(null, {status: 204})
  return new Response(JSON.stringify({data: {ok: true}, message: '测试错误'}), {status: replyStatus})
}
const {api} = await import('../web/src/api.js')
await api.post('/api/auth/login', {username: 'test'})
assert.equal(calls.at(-1).headers['X-CSRF-Token'], 'token-1')
const form = new FormData()
form.append('file', new Blob(['image']), 'test.png')
await api.post('/api/uploads/image', form)
assert.equal(calls.at(-1).headers['X-CSRF-Token'], 'token-2')
assert.equal(calls.at(-1).headers['Content-Type'], undefined)
assert.equal(calls.at(-1).credentials, 'same-origin')
await api.get('/api/goods', {params: {page: 1, keyword: '教材', unused: undefined}})
assert.match(calls.at(-1).url, /page=1&keyword=/)
assert.ok(!calls.at(-1).url.includes('unused'))
replyStatus = 204
assert.equal((await api.put('/api/notifications/1/read')).data.data, null)
for (const status of [401, 403, 409, 503]) {
  replyStatus = status
  await assert.rejects(api.get('/api/goods/mine'), error => error.response.status === status && error.isApiError)
}
console.log('Fetch self-check: CSRF rotation, Cookie mode, FormData, query strings, 204 and 401/403/409/503 passed')
