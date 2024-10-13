import { http } from '@/utils'

export async function queryOptions() {
  return http.get('/api/options')
}

export async function doUpdateOptions(options) {
  console.log(options)
  return http.put('/api/options', options)
}
