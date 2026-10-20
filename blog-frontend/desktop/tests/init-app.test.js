import initApp from '../src/init-app'
import { articleService } from '../src/services'
import { getCacheable, http, applySEO, setDefaultSEODescription, setDefaultSEOKeywords } from 'core'

vi.mock('core', () => ({
  getCacheable: vi.fn(),
  http: {
    get: vi.fn(),
  },
  applySEO: vi.fn(),
  setDefaultSEODescription: vi.fn(),
  setDefaultSEOKeywords: vi.fn(),
}))

vi.mock('../src/services', () => ({
  articleService: {
    popularArticles: vi.fn(),
  },
}))

const initStore = initApp[0]

describe('initStore', () => {
  const options = {
    'site.copyright': 'Copyright TODAY',
    'site.keywords': 'today,blog',
    'site.description': 'TODAY Blog',
  }
  const categories = [{ name: '技术' }]
  const labels = [{ name: 'JavaScript' }]
  const popularArticles = [{ id: 1, title: '测试文章' }]

  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    http.get.mockResolvedValue({ data: { id: 1, name: '访客' } })
    getCacheable.mockImplementation((url) => {
      const data = {
        '/api/options': options,
        '/api/categories': categories,
        '/api/tags': labels,
      }[url]
      return Promise.resolve({ data })
    })
    articleService.popularArticles.mockResolvedValue({ data: popularArticles })
  })

  test('initializes the store and SEO metadata from successful requests', async () => {
    const store = { dispatch: vi.fn() }
    const log = vi.spyOn(globalThis.console, 'log').mockImplementation(() => {})

    initStore(store)

    await vi.waitFor(() => expect(store.dispatch).toHaveBeenCalledTimes(5))

    expect(localStorage.getItem('session')).toBe(JSON.stringify({ id: 1, name: '访客' }))
    expect(store.dispatch).toHaveBeenCalledWith({ type: 'update-user-session', data: { id: 1, name: '访客' } })
    expect(store.dispatch).toHaveBeenCalledWith({ type: 'update-options', data: options })
    expect(store.dispatch).toHaveBeenCalledWith({ type: 'update-categories', data: categories })
    expect(store.dispatch).toHaveBeenCalledWith({ type: 'update-labels', data: labels })
    expect(store.dispatch).toHaveBeenCalledWith({ type: 'update-popular', data: popularArticles })
    expect(setDefaultSEOKeywords).toHaveBeenCalledWith(options['site.keywords'])
    expect(setDefaultSEODescription).toHaveBeenCalledWith(options['site.description'])
    expect(applySEO).toHaveBeenCalledOnce()
    expect(log).toHaveBeenCalledWith(
      expect.stringContaining('TODAY BLOG'),
      expect.stringContaining('font-size:96px'),
      expect.stringContaining('font-size:12px'),
    )

    log.mockRestore()
  })

  test('treats a 204 session probe as anonymous and clears stale storage', async () => {
    const store = { dispatch: vi.fn() }
    localStorage.setItem('session', '{"id":1}')
    http.get.mockResolvedValue({ status: 204, data: '' })

    initStore(store)

    await vi.waitFor(() => expect(store.dispatch).toHaveBeenCalledTimes(5))
    expect(localStorage.getItem('session')).toBeNull()
    expect(store.dispatch).toHaveBeenCalledWith({ type: 'update-user-session', data: null })
  })

  test('handles a failed session probe without an unhandled rejection', async () => {
    const store = { dispatch: vi.fn() }
    http.get.mockRejectedValue(new Error('network unavailable'))

    initStore(store)

    await vi.waitFor(() => expect(store.dispatch).toHaveBeenCalledTimes(5))
    expect(store.dispatch).toHaveBeenCalledWith({ type: 'update-user-session', data: null })
  })

  test('dispatches an error for each failed content request', async () => {
    const store = { dispatch: vi.fn() }
    const errors = {
      '/api/options': Object.assign(new Error('options unavailable'), { response: { status: 503 } }),
      '/api/categories': new Error('categories unavailable'),
      '/api/tags': new Error('tags unavailable'),
    }
    getCacheable.mockImplementation((url) => Promise.reject(errors[url]))
    articleService.popularArticles.mockRejectedValue(new Error('popular unavailable'))

    initStore(store)

    await vi.waitFor(() => expect(store.dispatch).toHaveBeenCalledTimes(5))

    expect(store.dispatch).toHaveBeenCalledWith({
      type: 'updateHttpErrorMessage',
      data: { options: { status: 503, subTitle: 'options unavailable' } },
    })
    expect(store.dispatch).toHaveBeenCalledWith({
      type: 'updateHttpErrorMessage',
      data: { categories: { status: 500, subTitle: 'categories unavailable' } },
    })
    expect(store.dispatch).toHaveBeenCalledWith({
      type: 'updateHttpErrorMessage',
      data: { tags: { status: 500, subTitle: 'tags unavailable' } },
    })
    expect(store.dispatch).toHaveBeenCalledWith({
      type: 'updateHttpErrorMessage',
      data: { popularArticles: { status: 500, subTitle: 'popular unavailable' } },
    })
    expect(applySEO).not.toHaveBeenCalled()
  })
})
